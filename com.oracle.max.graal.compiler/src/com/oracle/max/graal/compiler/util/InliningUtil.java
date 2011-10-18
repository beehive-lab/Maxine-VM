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
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.DeoptimizeNode.DeoptAction;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.graal.nodes.java.*;
import com.sun.cri.ri.*;

public class InliningUtil {

    public static void inline(InvokeNode invoke, CompilerGraph inlineGraph, Queue<InvokeNode> newInvokes) {
        ValueNode[] parameters = InliningUtil.simplifyParameters(invoke);
        Graph graph = invoke.graph();

        FrameState stateAfter = invoke.stateAfter();
        FixedNode exceptionEdge = invoke.exceptionEdge();
        if (exceptionEdge instanceof PlaceholderNode) {
            exceptionEdge = ((PlaceholderNode) exceptionEdge).next();
        }

        invoke.clearInputs();

        IdentityHashMap<Node, Node> replacements = new IdentityHashMap<Node, Node>();
        ArrayList<Node> nodes = new ArrayList<Node>();
        ArrayList<Node> frameStates = new ArrayList<Node>();
        ReturnNode returnNode = null;
        UnwindNode unwindNode = null;
        EntryPointNode entryPointNode = inlineGraph.start();
        FixedNode firstCFGNode = entryPointNode.next();
        for (Node node : inlineGraph.getNodes()) {
            if (node instanceof EntryPointNode) {
                assert entryPointNode == node;
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

        assert invoke.successors().first() != null : invoke;
        assert invoke.predecessor() != null;

        Map<Node, Node> duplicates = graph.addDuplicate(nodes, replacements);

        FixedNode firstCFGNodeDuplicate = (FixedNode) duplicates.get(firstCFGNode);
        FixedNode invokeReplacement;
        if (invoke.isStatic()) {
            invokeReplacement = firstCFGNodeDuplicate;
        } else {
            FixedGuardNode guard = graph.add(new FixedGuardNode(graph.unique(new IsNonNullNode(parameters[0]))));
            guard.setNext(firstCFGNodeDuplicate);
            invokeReplacement = guard;
        }
        invoke.replaceAtPredecessors(invokeReplacement);

        FrameState stateBefore = null;
        double invokeProbability = invoke.probability();
        for (Node node : duplicates.values()) {
            if (GraalOptions.ProbabilityAnalysis) {
                if (node instanceof FixedNode) {
                    FixedNode fixed = (FixedNode) node;
                    fixed.setProbability(fixed.probability() * invokeProbability);
                }
            }
            if (node instanceof InvokeNode && ((InvokeNode) node).canInline()) {
                if (newInvokes != null) {
                    newInvokes.add((InvokeNode) node);
                }
            } else if (node instanceof FrameState) {
                FrameState frameState = (FrameState) node;
                if (frameState.bci == FrameState.BEFORE_BCI) {
                    if (stateBefore == null) {
                        stateBefore = stateAfter.duplicateModified(invoke.bci, false, invoke.kind, parameters);
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
                if (entry.getValue() instanceof MonitorAddressNode) {
                    MonitorAddressNode address = (MonitorAddressNode) entry.getValue();
                    address.setMonitorIndex(address.monitorIndex() + monitorIndexDelta);
                }
            }
        }

        if (returnNode != null) {
            for (Node usage : invoke.usages().snapshot()) {
                if (returnNode.result() instanceof LocalNode) {
                    usage.replaceFirstInput(invoke, replacements.get(returnNode.result()));
                } else {
                    usage.replaceFirstInput(invoke, duplicates.get(returnNode.result()));
                }
            }
            Node returnDuplicate = duplicates.get(returnNode);
            returnDuplicate.clearInputs();
            Node n = invoke.next();
            invoke.setNext(null);
            returnDuplicate.replaceAndDelete(n);
        }

        if (exceptionEdge != null) {
            if (unwindNode != null) {
                assert unwindNode.predecessor() != null;
                assert exceptionEdge.successors().explicitCount() == 1;
                ExceptionObjectNode obj = (ExceptionObjectNode) exceptionEdge;

                UnwindNode unwindDuplicate = (UnwindNode) duplicates.get(unwindNode);
                for (Node usage : obj.usages().snapshot()) {
                    usage.replaceFirstInput(obj, unwindDuplicate.exception());
                }
                unwindDuplicate.clearInputs();
                Node n = obj.next();
                obj.setNext(null);
                unwindDuplicate.replaceAndDelete(n);
            }
        } else {
            if (unwindNode != null) {
                UnwindNode unwindDuplicate = (UnwindNode) duplicates.get(unwindNode);
                unwindDuplicate.replaceAndDelete(graph.add(new DeoptimizeNode(DeoptAction.InvalidateRecompile)));
            }
        }

        GraphUtil.killCFG(invoke);

        // adjust all frame states that were copied
        if (frameStates.size() > 0) {
            FrameState outerFrameState = stateAfter.duplicateModified(invoke.bci, stateAfter.rethrowException(), invoke.kind);
            for (Node node : frameStates) {
                FrameState frameState = (FrameState) duplicates.get(node);
                if (!frameState.isDeleted()) {
                    frameState.setOuterFrameState(outerFrameState);
                }
            }
        }

    }

    public static ValueNode[] simplifyParameters(InvokeNode invoke) {
        RiMethod method = invoke.target();
        NodeInputList<ValueNode> arguments = invoke.arguments();

        boolean withReceiver = !invoke.isStatic();
        int argumentCount = method.signature().argumentCount(false);
        ValueNode[] parameters = new ValueNode[argumentCount + (withReceiver ? 1 : 0)];
        int slot = withReceiver ? 1 : 0;
        int param = withReceiver ? 1 : 0;
        for (int i = 0; i < argumentCount; i++) {
            parameters[param++] = arguments.get(slot);
            slot++;
            if (slot < arguments.size() && arguments.get(slot) == null) {
                // Second slot of long or double value.
                slot++;
            }
        }
        assert slot == arguments.size() : "missed an argument";
        if (withReceiver) {
            parameters[0] = arguments.get(0);
        }
        return parameters;
    }
}
