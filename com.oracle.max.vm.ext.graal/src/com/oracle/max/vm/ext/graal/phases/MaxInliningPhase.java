/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.max.vm.ext.graal.phases;

import static com.oracle.graal.phases.GraalOptions.*;

import com.oracle.graal.api.code.Assumptions;
import com.oracle.graal.api.code.InfopointReason;
import com.oracle.graal.api.meta.ResolvedJavaMethod;
import com.oracle.graal.graph.Node;
import com.oracle.graal.graph.NodeFlood;
import com.oracle.graal.graph.NodeMap;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.MonitorEnterNode;
import com.oracle.graal.nodes.java.MonitorExitNode;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.phases.common.InliningUtil.*;
import com.oracle.graal.phases.tiers.HighTierContext;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.nodes.MaxInfopointNode;
import com.sun.max.annotate.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.runtime.FatalError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is a Maxine customization for tuning the inlining policy.
 * Since Maxine currently does not support runtime generated probabilities,
 * we use the {@code min(relevance, probability)} in {@link Policy#computeMaximumSize}.
 *
 * We also dial down the {@link GraalOptions#MaximumInliningSize} value here, rather than
 * change the actual option.
 */
public class MaxInliningPhase extends InliningPhase {

    @RESET
    private static int maxInliningSize;

    /**
     * Indicates whether auxiliary infopoints were constructed.
     */
    private boolean maxInfopointsConstructed;

    private static int getMaxInliningSize() {
        if (maxInliningSize == 0) {
            int optionValue = MaximumInliningSize.getValue();
            // If it was set on the command line, assume user knows what they are doing
            if (MaxGraalOptions.isPresent("MaximumInliningSize") == null) {
                if (optionValue > 100) {
                    optionValue = 100;
                }
            }
            maxInliningSize = optionValue;
        }
        return maxInliningSize;
    }

    static {
        JavaPrototype.registerInitializationCompleteCallback(new Callback());
    }


    @HOSTED_ONLY
    private static class Callback implements JavaPrototype.InitializationCompleteCallback {

        public void initializationComplete() {
            // Here you could capture the Actors for specific methods/classes for comparison in isWorthInlining.
        }

    }

    private static class ProbTL extends ThreadLocal<Double> {

    }

    private static final ProbTL probTL = new ProbTL();

    static class Policy extends GreedyInliningPolicy {

        public Policy() {
            super(null);
        }

        /**
         * Maxine-specific computation. N.B. This is highly dependent on the implementation in the supertype.
         * In particular, we assume {@code configuredMaximum == (int) (MaximumInliningSize.getValue() * inliningBonus)}.
         */
        @Override
        protected double computeMaximumSize(double relevance, int configuredMaximum) {
            // recover inliningBonus (some loss of precision due to the original conversion to int is inevitable)
            double inliningBonus = (double) configuredMaximum / (double) MaximumInliningSize.getValue();
            return super.computeMaximumSize(Math.min(relevance, probTL.get()), (int) (getMaxInliningSize() * inliningBonus));
        }

        @Override
        public boolean isWorthInlining(Replacements replacements, InlineInfo info, int inliningDepth, double probability, double relevance, boolean fullyProcessed) {
            probTL.set(probability);
            return super.isWorthInlining(replacements, info, inliningDepth, probability, relevance, fullyProcessed);
        }
    }

    public MaxInliningPhase() {
        super(new Policy());
    }

    private static class MethodListTL extends ThreadLocal<List<MaxResolvedJavaMethod>> {

    }

    private static final MethodListTL methodListTL = new MethodListTL();

    @Override
    protected void run(final StructuredGraph graph, final HighTierContext context) {
        List<MaxResolvedJavaMethod> methodList = new ArrayList<MaxResolvedJavaMethod>();
        methodListTL.set(methodList);
        super.run(graph, context);
        redundantInfopointsElimination(graph);
        for (MaxResolvedJavaMethod m : methodList) {
            m.utilizeCollectedProfilingInfo();
        }
    }

    @Override
    protected StructuredGraph buildGraph(final ResolvedJavaMethod method, final Invoke invoke, final Assumptions assumptions, final HighTierContext context) {
        MaxResolvedJavaMethod callerMethod = (MaxResolvedJavaMethod) invoke.asNode().graph().method();
        if (callerMethod.isCollectedProfilingInfoIgnored() || callerMethod.isOptimizedMethodInBootCodeRegion()) {
            List<MaxResolvedJavaMethod> methodList = methodListTL.get();
            MaxResolvedJavaMethod calleeMethod = (MaxResolvedJavaMethod) method;

            methodList.add(calleeMethod);
            calleeMethod.ignoreCollectedProfilingInfo();
        }
        StructuredGraph resGraph = super.buildGraph(method, invoke, assumptions, context);
        insertInfopointsToMethodBondaries(resGraph);
        return resGraph;
    }

    /**
     * Inserts infopoints at the start and all the return points of the graph to preserve information about inlined
     * methods for their invalidation during deoptimization.
     */
    private void insertInfopointsToMethodBondaries(StructuredGraph graph) {
        InfopointNode maxInfopointNode = null;
        FrameState frameState;

        // insert infopoint node after start node if infopoint node does not exist
        FixedWithNextNode entryFrameStateNode = graph.start();
        frameState = ((StateSplit) entryFrameStateNode).stateAfter();
        if (frameState == null) {
            FatalError.unexpected("Expected non zero frame state at start node");
        }
        if (entryFrameStateNode.next() instanceof ReturnNode) {
            return;
        }
        if (frameState.bci == FrameState.BEFORE_BCI) {
            FixedNode nextAfterStartNode = entryFrameStateNode.next();
            assert nextAfterStartNode instanceof MonitorEnterNode;
            entryFrameStateNode = (FixedWithNextNode) nextAfterStartNode;
            frameState = ((StateSplit) entryFrameStateNode).stateAfter();
            if (entryFrameStateNode.next() instanceof MonitorExitNode) {
                return;
            }
        }
        if (!(entryFrameStateNode.next() instanceof InfopointNode)) {
            maxInfopointNode = graph.add(new MaxInfopointNode(InfopointReason.METHOD_START));

            if (frameState == null && frameState.bci == 0) {
                FatalError.unexpected("Expected non zero frame state with bci 0 at method entry");
            }
            maxInfopointNode.setStateAfter(frameState.duplicate());
            graph.addAfterFixed(entryFrameStateNode, maxInfopointNode);
        }
        List<ValueNode> stack = Collections.emptyList();
        List<ReturnNode> returnNodes = graph.getNodes().filter(ReturnNode.class).snapshot();
        for (ReturnNode returnNode : returnNodes) {
            // for each return node insert infopoint node after start node if infopoint node does not exist
            if (!(returnNode.predecessor() instanceof InfopointNode)) {
                assert maxInfopointNode != null;
                maxInfopointNode = graph.add(new MaxInfopointNode(InfopointReason.METHOD_END));
                frameState = new FrameState(graph.method(), FrameState.AFTER_BCI, new ValueNode[0], stack, new ValueNode[0], false, false);
                maxInfopointNode.setStateAfter(graph.add(frameState));
                graph.addBeforeFixed(returnNode, maxInfopointNode);
            } else {
                assert maxInfopointNode == null;
            }
        }
        if (maxInfopointNode != null) {
            maxInfopointsConstructed = true;
        }
    }

    /**
     * Eliminates redundant infopoints.
     *
     * Precondition: it is assumed that for all inlined graphs either infopoints construction was triggered by
     * {@link com.oracle.graal.java.GraphBuilderConfiguration#eagerInfopointMode} so no auxiliary infopoints were inserted
     * or all infopoints were inserted in {@link #insertInfopointsToMethodBondaries}.
     */
    private void redundantInfopointsElimination(StructuredGraph graph) {
        if (!maxInfopointsConstructed) {
            return;
        }
        redundantBackwardUnreachableInfopointsElimination(graph);
        redundantAdjacentInfopointsElimination(graph);
    }

    /**
     * Eliminates redundant infopoints which are not directly backward reachable from {@link DeoptimizeNode DeoptimizeNodes}.
     */
    private void redundantBackwardUnreachableInfopointsElimination(StructuredGraph graph) {
        NodeFlood flood = graph.createNodeFlood();
        // start graph traversal from deoptimization nodes
        for (DeoptimizeNode node : graph.getNodes(DeoptimizeNode.class)) {
            flood.add(node);
        }
        // traverse nodes by control flow predecessors recursively ignoring back edges until reaching {@link InfopointNode}
        // then traversing outer frame states
        for (Node node : flood) {
            if  (node instanceof MergeNode) {
                MergeNode mergeNode = (MergeNode) node;
                for (Node predNode : mergeNode.cfgPredecessors()) {
                    if (predNode instanceof EndNode) {
                        flood.add(predNode);
                    }
                }
            } else if (node instanceof InfopointNode) {
                flood.add(((AbstractStateSplit) node).stateAfter());
            } else if (node instanceof FrameState) {
                flood.add(((FrameState) node).outerFrameState());
            } else if (!(node instanceof StartNode)) {
                flood.add(node.predecessor());
            }
        }
        // eliminate every {@link MaxInfopointNode} with non-marked frame state
        for (MaxInfopointNode node : graph.getNodes(MaxInfopointNode.class)) {
            if (!flood.isMarked(node.stateAfter())) {
                FrameState frameState = node.stateAfter();
                graph.removeFixed(node);
                if (frameState.usages().isEmpty()) {
                    frameState.safeDelete();
                }
            }
        }
    }

    /**
     * Eliminates redundant adjacent infopoints with identical outer frame states.
     */
    private void redundantAdjacentInfopointsElimination(StructuredGraph graph) {
        FrameState nonCongruentFrameState = new FrameState(FrameState.INVALID_FRAMESTATE_BCI);
        NodeMap<FrameState> nodeIncomingOuterFrameStateMap = graph.createNodeMap();
        NodeFlood flood = graph.createNodeFlood();

        // traverse nodes in RPO numeration propagating outer frame state starting from non congruent frame state
        flood.add(graph.start());
        nodeIncomingOuterFrameStateMap.set(graph.start(), nonCongruentFrameState);
        for (Node node : flood) {
            FrameState nodeIncomingOuterFrameState = nodeIncomingOuterFrameStateMap.get(node);
            if (node instanceof AbstractStateSplit) {
                // if node is {@link InfopointNode} propagate its outer frame state else if it is {@link AbstractStateSplit}
                // propagate non congruent frame state
                if (node instanceof InfopointNode) {
                    nodeIncomingOuterFrameState = ((InfopointNode) node).stateAfter().outerFrameState();
                } else {
                    nodeIncomingOuterFrameState = nonCongruentFrameState;
                }
            }
            if (node instanceof EndNode) {
                EndNode end = (EndNode) node;
                MergeNode mergeNode = end.merge();
                boolean addToFlood = true;
                // if node is {@link MergeNode} then propagate frame states of predecessors if they are equal otherwise
                // propagate non congruent frame state
                for (Node predNode : mergeNode.cfgPredecessors()) {
                    if (predNode instanceof EndNode) {
                        if (!flood.isMarked(predNode)) {
                            addToFlood = false;
                            break;
                        } else {
                            FrameState predIncomingOuterFrameState = nodeIncomingOuterFrameStateMap.get(predNode);
                            if (nodeIncomingOuterFrameState != predIncomingOuterFrameState) {
                                nodeIncomingOuterFrameState = nonCongruentFrameState;
                            }
                        }
                    }
                }
                if (addToFlood) {
                    flood.add(mergeNode);
                    nodeIncomingOuterFrameStateMap.set(mergeNode, nodeIncomingOuterFrameState);
                }
            } else {
                for (Node successor : node.successors()) {
                    // do not propagate by back edge
                    if (!(node instanceof LoopEndNode)) {
                        flood.add(successor);
                        nodeIncomingOuterFrameStateMap.set(successor, nodeIncomingOuterFrameState);
                    }
                }
            }
        }
        // eliminate every {@link MaxInfopointNode} with incoming outer frame state equal to its own outer frame state
        for (MaxInfopointNode node : graph.getNodes(MaxInfopointNode.class)) {
            FrameState incomingOuterFrameState = nodeIncomingOuterFrameStateMap.get(node);
            if (incomingOuterFrameState == node.stateAfter().outerFrameState()) {
                FrameState frameState = node.stateAfter();
                graph.removeFixed(node);
                if (frameState.usages().isEmpty()) {
                    frameState.safeDelete();
                }
            }
        }
    }
}
