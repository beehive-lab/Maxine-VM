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
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;


public class MustInlineAndFoldPhase extends Phase {

    private final MaxRuntime runtime;

    public MustInlineAndFoldPhase(MaxRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    protected void run(StructuredGraph graph) {
        // Apply folding.
        for (InvokeNode invoke : graph.getNodes(InvokeNode.class)) {
            RiResolvedMethod method = invoke.callTarget().targetMethod();
            if (runtime.isFoldable(method)) {
                NodeInputList<ValueNode> arguments = invoke.callTarget().arguments();
                CiConstant[] constantArgs = new CiConstant[arguments.size()];
                for (int i = 0; i < constantArgs.length; ++i) {
                    constantArgs[i] = arguments.get(i).asConstant();
                }
                CiConstant foldResult = runtime.fold(method, constantArgs);
                StructuredGraph foldGraph = new StructuredGraph();
                foldGraph.start().setNext(foldGraph.add(new ReturnNode(ConstantNode.forCiConstant(foldResult, runtime, foldGraph))));
                InliningUtil.inline(invoke, foldGraph);
            }
        }

        // Canonicalize.
        new CanonicalizerPhase(null, runtime, null).apply(graph);

        // Inline all necessary methods.
        for (InvokeNode invoke : graph.getNodes(InvokeNode.class)) {
            RiResolvedMethod method = invoke.callTarget().targetMethod();
            if (runtime.mustInline(method)) {
                RiResolvedMethod method1 = invoke.callTarget().targetMethod();
                StructuredGraph inlineGraph = new StructuredGraph();
                new GraphBuilderPhase(runtime, method1).apply(inlineGraph);
                new MustInlineAndFoldPhase(runtime).apply(inlineGraph);
                InliningUtil.inline(invoke, inlineGraph);
            }
        }

        // Canonicalize.
        new CanonicalizerPhase(null, runtime, null).apply(graph);
    }
}
