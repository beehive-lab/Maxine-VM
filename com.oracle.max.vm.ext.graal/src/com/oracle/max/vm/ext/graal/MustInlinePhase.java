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
package com.oracle.max.vm.ext.graal;

import com.oracle.max.graal.compiler.graphbuilder.*;
import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.java.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;


public class MustInlinePhase extends Phase {

    private final MaxRuntime runtime;
    private final RiResolvedType accessor;

    public MustInlinePhase(MaxRuntime runtime, RiResolvedType accessor) {
        this.runtime = runtime;
        this.accessor = accessor;
    }

    @Override
    protected void run(StructuredGraph graph) {
        // Apply folding.
        for (MethodCallTargetNode callTarget : graph.getNodes(MethodCallTargetNode.class)) {
            RiResolvedMethod method = callTarget.targetMethod();
            Invoke invoke = callTarget.invoke();
            if (invoke != null) {
                if (runtime.isFoldable(method)) {
                    NodeInputList<ValueNode> arguments = invoke.callTarget().arguments();
                    CiConstant[] constantArgs = new CiConstant[arguments.size()];
                    for (int i = 0; i < constantArgs.length; ++i) {
                        constantArgs[i] = arguments.get(i).asConstant();
                    }
                    CiConstant foldResult = runtime.fold(method, constantArgs);
                    if (foldResult != null) {
                        StructuredGraph foldGraph = new StructuredGraph();
                        foldGraph.start().setNext(foldGraph.add(new ReturnNode(ConstantNode.forCiConstant(foldResult, runtime, foldGraph))));
                        InliningUtil.inline(invoke, foldGraph, false);
                    }
                }
            }
        }

        // Canonicalize.
        new CanonicalizerPhase(null, runtime, null).apply(graph);

        // Inline all necessary methods.
        for (MethodCallTargetNode callTarget : graph.getNodes(MethodCallTargetNode.class)) {
            Invoke invoke = callTarget.invoke();
            RiResolvedMethod method = callTarget.targetMethod();
            if (invoke != null) {
                boolean mustInline = runtime.mustInline(method);
                if (method.holder().equals(runtime.getType(Accessor.class))) {
                    RiResolvedType curAccessor = getAccessor(invoke, accessor);
                    assert curAccessor != null;
                    method = curAccessor.resolveMethodImpl(method);
                    mustInline = true;
                }
                MethodActor methodActor = (MethodActor) method;
                if (methodActor.intrinsic() != null) {
                    IntrinsificationPhase.tryIntrinsify(invoke, method, runtime);
                } else if (mustInline) {
                    StructuredGraph inlineGraph = (StructuredGraph) method.compilerStorage().get(MustInlinePhase.class);
                    if (inlineGraph == null) {
                        inlineGraph = new StructuredGraph();
                        new GraphBuilderPhase(runtime, method).apply(inlineGraph);
                        RiResolvedType curAccessor = getAccessor(invoke, accessor);
                        new FoldPhase(runtime).apply(inlineGraph);
                        new MustInlinePhase(runtime, curAccessor).apply(inlineGraph);
                        method.compilerStorage().put(MustInlinePhase.class, inlineGraph);
                    }
                    InliningUtil.inline(invoke, inlineGraph, false);
                }
            }
        }

        // Intrinsification.
        new IntrinsificationPhase(runtime).apply(graph);

        // Canonicalize.
        new CanonicalizerPhase(null, runtime, null).apply(graph);
    }

    private RiResolvedType getAccessor(Invoke invoke, RiResolvedType accessor) {
        CiCodePos pos = invoke.stateAfter().toCodePos();
        while (pos != null) {
            RiResolvedType result = pos.method.accessor();
            if (result != null) {
                // Found accessor.
                return result;
            }
            pos = pos.caller;
        }
        return accessor;
    }
}
