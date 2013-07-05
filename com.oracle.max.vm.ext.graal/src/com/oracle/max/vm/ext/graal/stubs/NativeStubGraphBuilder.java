/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal.stubs;

import java.util.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.phases.util.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.type.SignatureDescriptor;

/**
 * A utility class for generating a native method stub (as a compiler graph) implementing
 * the transition from Java to native code. This process is very similar to using snippets
 * to lower high-level nodes. However, since the number and types of the arguments to a native method
 * are variable, we cannot just use the snippet instantiation mechanism directly.
 */
public class NativeStubGraphBuilder extends AbstractGraphBuilder {

    /**
     * Creates a native stub graph builder instance for a given native method.
     */
    public NativeStubGraphBuilder(ClassMethodActor nativeMethod) {
        super(nativeMethod);
        assert nativeMethod.isNative();
        StructuredGraph template;
        if (nativeMethod.isCFunction()) {
            if (NativeInterfaces.needsPrologueAndEpilogue(nativeMethod)) {
                template = NativeStubSnippets.cFunctionTemplate;
            } else {
                template = NativeStubSnippets.noPrologueOrEpilogueTemplate;
            }
        } else {
            if (nativeMethod.isSynchronized()) {
                template = NativeStubSnippets.synchronizedTemplate;
            } else {
                template = NativeStubSnippets.normalTemplate;
            }
        }
        setGraph(template.copy(nativeMethod.name()));
    }

    /**
     * Builds the graph for the native method stub.
     */
    public StructuredGraph build(final List<Phase> bootPhases) {
        Debug.scope("NativeStubGraphBuilder", new Object[]{nativeMethod}, new Runnable() {
            public void run() {
                buildGraph(bootPhases);
            }
        });
        return graph;
    }

    private StructuredGraph buildGraph(final List<Phase> bootPhases) {
        SignatureDescriptor sig = nativeMethodActor.descriptor();

        Debug.dump(graph, NativeStubGraphBuilder.class.getSimpleName() + ":" + nativeMethod.getName());

        // Replace template parameters with constants
        Iterator<LocalNode> locals = graph.getNodes(LocalNode.class).iterator();
        LocalNode local0 = locals.next();
        assert local0.kind() == Kind.Object;
        LocalNode local1 = null;

        if (locals.hasNext()) {
            local1 = locals.next();
            assert local1.kind() == Kind.Long;
        }

        // The NativeFunction object
        local0.replaceAtUsages(oconst(nativeMethodActor.nativeFunction));
        local0.safeDelete();

        if (local1 != null) {
            // The MethodID
            local1.replaceAtUsages(ConstantNode.forLong(MethodID.fromMethodActor(nativeMethodActor).asAddress().toLong(), graph));
            local1.safeDelete();
        }

        Debug.dump(graph, "%s: SpecializeLocals", nativeMethod.getName());

        if (nativeMethodActor.isStatic() && nativeMethodActor.isSynchronized()) {
            // The template synchronizes on the NativeStubSnippets class, patch that now
            patchMonitors();
            Debug.dump(graph, "Patch Monitors");
        }

        patchReturn();

        // Add parameters of native method
        boolean isStatic = nativeMethodActor.isStatic();

        for (Node n : GraphOrder.forwardGraph(graph)) {
            if (n instanceof NativeFunctionAdapterNode) {
                NativeFunctionAdapterNode nfNode = (NativeFunctionAdapterNode) n;
                nfNode.setNativeMethod(nativeMethodActor);
                NodeInputList<ValueNode> arguments = nfNode.arguments();
                arguments.addAll(createLocals(0, sig, isStatic));
                MaxGraal.runtime().lower(nfNode, null);
            }
        }

        Debug.dump(graph, "Inlined");

        // No deoptimization points within the stub
        for (Node node : graph.getNodes()) {
            if (node instanceof StateSplit) {
                StateSplit stateSplit = (StateSplit) node;
                FrameState frameState = stateSplit.stateAfter();
                if (frameState != null) {
                    stateSplit.setStateAfter(null);
                    if (frameState.usages().isEmpty()) {
                        GraphUtil.killWithUnusedFloatingInputs(frameState);
                    }
                }
            }
        }
        Debug.dump(graph,  "StateSplit cleanup");

        // There has to be a FrameState at the start (evidently)
        graph.start().setStateAfter(graph.add(new FrameState(nativeMethod, 0, new ArrayList<ValueNode>(), 0, 0, false, false, null)));


        new DeadCodeEliminationPhase().apply(graph);
        Debug.dump(graph, "%s: Final", nativeMethod.getName());

        graph.verify();
        return graph;
    }

    /**
     * Patch the ReturnNode if the method is void.
     */
    private void patchReturn() {
        Kind returnKind = nativeMethod.getSignature().getReturnKind();
        if (returnKind == Kind.Void) {
            ReturnNode returnNode = null;
            for (Node n : graph.getNodes()) {
                if (n instanceof ReturnNode) {
                    returnNode = (ReturnNode) n;
                    break;
                }
            }
            assert returnNode != null;
            // Fix the return node to be void
            ReturnNode voidReturnNode = graph.add(new ReturnNode(null));
            returnNode.replaceAndDelete(voidReturnNode);
            Debug.dump(graph, "Patch Return");

        }
    }

    private void patchMonitors() {
        ValueNode nativeClass = ConstantNode.forObject(nativeMethodActor.holder().toJava(), MaxGraal.runtime(), graph);
        int count = 0;
        ValueNode templateObject = null;
        for (Node n : graph.getNodes()) {
            if (n instanceof AccessMonitorNode) {
                AccessMonitorNode an = (AccessMonitorNode) n;
                templateObject = an.object();
                AccessMonitorNode replacement = null;
                FixedNode successor = an.next();
                if (n instanceof MonitorEnterNode) {
                    replacement = new MonitorEnterNode(nativeClass, 0);
                    n.replaceAndDelete(graph.add(replacement));
                    count++;
                } else if (n instanceof MonitorExitNode) {
                    replacement = new MonitorExitNode(nativeClass, 0);
                    n.replaceAndDelete(graph.add(replacement));
                    count++;
                }
                replacement.setNext(successor);
                if (count >= 2) {
                    break;
                }
            }
        }
        templateObject.safeDelete();
    }

    /**
     * Applies the standard boot phases to a graph before it is inlined into another graph.
     */
    protected void applyPhasesBeforeInlining(StructuredGraph graph, List<Phase> bootPhases) {
        for (Phase phase : bootPhases) {
            phase.apply(graph);
        }
    }

}
