/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.util;

import java.util.*;

import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.graphbuilder.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.DeoptimizeNode.DeoptAction;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.graal.nodes.java.*;
import com.sun.cri.ri.*;

public class InliningUtil {

    public static void inline(Invoke invoke, StructuredGraph inlineGraph) {
        ValueNode[] parameters = InliningUtil.simplifyParameters(invoke);
        StructuredGraph graph = invoke.node().graph();

        FrameState stateAfter = invoke.stateAfter();

        IdentityHashMap<Node, Node> replacements = new IdentityHashMap<Node, Node>();
        ArrayList<Node> nodes = new ArrayList<Node>();
        ArrayList<Node> frameStates = new ArrayList<Node>();
        ReturnNode returnNode = null;
        UnwindNode unwindNode = null;
        BeginNode entryPointNode = inlineGraph.start();
        FixedNode firstCFGNode = entryPointNode.next();
        for (Node node : inlineGraph.getNodes()) {
            if (node == entryPointNode || node == entryPointNode.stateAfter()) {
                // Do nothing.
            } else if (node instanceof LocalNode) {
                replacements.put(node, parameters[((LocalNode) node).index()]);
            } else {
                nodes.add(node);
                if (node instanceof ReturnNode) {
                    returnNode = (ReturnNode) node;
                } else if (node instanceof UnwindNode) {
                    unwindNode = (UnwindNode) node;
                } else if (node instanceof FrameState) {
                    frameStates.add(node);
                }
            }
        }

        assert invoke.node().successors().first() != null : invoke;
        assert invoke.node().predecessor() != null;

        Map<Node, Node> duplicates = graph.addDuplicate(nodes, replacements);

        FixedNode firstCFGNodeDuplicate = (FixedNode) duplicates.get(firstCFGNode);
        FixedNode invokeReplacement;
        MethodCallTargetNode callTarget = invoke.callTarget();
        if (callTarget.isStatic()) {
            invokeReplacement = firstCFGNodeDuplicate;
        } else {
            FixedGuardNode guard = graph.add(new FixedGuardNode(graph.unique(new NullCheckNode(parameters[0], false))));
            guard.setNext(firstCFGNodeDuplicate);
            invokeReplacement = guard;
        }
        invoke.node().replaceAtPredecessors(invokeReplacement);

        FrameState stateBefore = null;
        double invokeProbability = invoke.node().probability();
        for (Node node : duplicates.values()) {
            if (GraalOptions.ProbabilityAnalysis) {
                if (node instanceof FixedNode) {
                    FixedNode fixed = (FixedNode) node;
                    fixed.setProbability(fixed.probability() * invokeProbability);
                }
            }
            if (node instanceof FrameState) {
                FrameState frameState = (FrameState) node;
                if (frameState.bci == FrameState.BEFORE_BCI) {
                    if (stateBefore == null) {
                        stateBefore = stateAfter.duplicateModified(invoke.bci(), false, invoke.node().kind(), parameters);
                    }
                    frameState.replaceAndDelete(stateBefore);
                } else if (frameState.bci == FrameState.AFTER_BCI) {
                    frameState.replaceAndDelete(stateAfter);
                }
            }
        }

        int monitorIndexDelta = stateAfter.locksSize();
        if (monitorIndexDelta > 0) {
            for (Map.Entry<Node, Node> entry : duplicates.entrySet()) {
                if (entry.getValue() instanceof AccessMonitorNode) {
                    AccessMonitorNode access = (AccessMonitorNode) entry.getValue();
                    access.setMonitorIndex(access.monitorIndex() + monitorIndexDelta);
                }
            }
        }

        if (returnNode != null) {
            for (Node usage : invoke.node().usages().snapshot()) {
                if (returnNode.result() instanceof LocalNode) {
                    usage.replaceFirstInput(invoke.node(), replacements.get(returnNode.result()));
                } else {
                    usage.replaceFirstInput(invoke.node(), duplicates.get(returnNode.result()));
                }
            }
            Node returnDuplicate = duplicates.get(returnNode);
            returnDuplicate.clearInputs();
            Node n = invoke.next();
            invoke.setNext(null);
            returnDuplicate.replaceAndDelete(n);
        }

        if (invoke instanceof InvokeWithExceptionNode) {
            InvokeWithExceptionNode invokeWithException = ((InvokeWithExceptionNode) invoke);
            if (unwindNode != null) {
                assert unwindNode.predecessor() != null;
                assert invokeWithException.exceptionEdge().successors().explicitCount() == 1;
                ExceptionObjectNode obj = (ExceptionObjectNode) invokeWithException.exceptionEdge().next();

                UnwindNode unwindDuplicate = (UnwindNode) duplicates.get(unwindNode);
                for (Node usage : obj.usages().snapshot()) {
                    usage.replaceFirstInput(obj, unwindDuplicate.exception());
                }
                unwindDuplicate.clearInputs();
                Node n = obj.next();
                obj.setNext(null);
                unwindDuplicate.replaceAndDelete(n);
            } else {
                FixedNode nodeToDelete = invokeWithException.exceptionEdge();
                invokeWithException.setExceptionEdge(null);
                GraphUtil.killCFG(nodeToDelete);
            }
        } else {
            if (unwindNode != null) {
                UnwindNode unwindDuplicate = (UnwindNode) duplicates.get(unwindNode);
                unwindDuplicate.replaceAndDelete(graph.add(new DeoptimizeNode(DeoptAction.InvalidateRecompile)));
            }
        }

        invoke.node().clearInputs();
        GraphUtil.killCFG(invoke.node());

        // adjust all frame states that were copied
        if (frameStates.size() > 0) {
            FrameState outerFrameState = stateAfter.duplicateModified(invoke.bci(), stateAfter.rethrowException(), invoke.node().kind());
            for (Node node : frameStates) {
                FrameState frameState = (FrameState) duplicates.get(node);
                if (!frameState.isDeleted()) {
                    frameState.setOuterFrameState(outerFrameState);
                }
            }
        }

        if (stateAfter.usages().isEmpty()) {
            stateAfter.delete();
        }

    }

    public static ValueNode[] simplifyParameters(Invoke invoke) {
        MethodCallTargetNode target = invoke.callTarget();
        RiMethod method = target.targetMethod();
        NodeInputList<ValueNode> arguments = target.arguments();

        boolean withReceiver = !target.isStatic();
        int argumentCount = method.signature().argumentCount(false);
        ValueNode[] parameters = new ValueNode[argumentCount + (withReceiver ? 1 : 0)];
        int param = withReceiver ? 1 : 0;
        for (int i = 0; i < argumentCount; i++) {
            parameters[param] = arguments.get(param);
            param++;
        }
        if (withReceiver) {
            parameters[0] = arguments.get(0);
        }
        return parameters;
    }

    public static void inline(RiRuntime runtime, Invoke invoke) {
        RiResolvedMethod method = invoke.callTarget().targetMethod();
        StructuredGraph graph = new StructuredGraph();
        new GraphBuilderPhase(runtime, method).apply(graph);
        inline(invoke, graph);
    }
}
