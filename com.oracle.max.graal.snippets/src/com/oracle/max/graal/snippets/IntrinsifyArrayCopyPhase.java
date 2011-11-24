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
package com.oracle.max.graal.snippets;

import com.oracle.max.criutils.*;
import com.oracle.max.graal.compiler.graphbuilder.*;
import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.java.*;
import com.sun.cri.ri.*;

public class IntrinsifyArrayCopyPhase extends Phase {
    private final RiRuntime runtime;
    private RiResolvedMethod intArrayCopy;
    private RiResolvedMethod arrayCopy;
    private RiResolvedMethod charArrayCopy;
    private RiResolvedMethod longArrayCopy;


    public IntrinsifyArrayCopyPhase(RiRuntime runtime) {
        this.runtime = runtime;
        try {
            intArrayCopy = runtime.getRiMethod(ArrayCopySnippets.class.getDeclaredMethod("arraycopy", int[].class, int.class, int[].class, int.class, int.class));
            charArrayCopy = runtime.getRiMethod(ArrayCopySnippets.class.getDeclaredMethod("arraycopy", char[].class, int.class, char[].class, int.class, int.class));
            longArrayCopy = runtime.getRiMethod(ArrayCopySnippets.class.getDeclaredMethod("arraycopy", long[].class, int.class, long[].class, int.class, int.class));
            arrayCopy = runtime.getRiMethod(System.class.getDeclaredMethod("arraycopy", Object.class, int.class, Object.class, int.class, int.class));
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void run(StructuredGraph graph) {
        for (MethodCallTargetNode methodCallTarget : graph.getNodes(MethodCallTargetNode.class)) {
            RiResolvedMethod targetMethod = methodCallTarget.targetMethod();
            RiResolvedMethod snippetMethod = null;
            if (targetMethod == arrayCopy) {
                RiResolvedType srcDeclaredType = methodCallTarget.arguments().get(0).declaredType();
                RiResolvedType destDeclaredType = methodCallTarget.arguments().get(2).declaredType();
                if (srcDeclaredType != null
                                && srcDeclaredType.isArrayClass()
                                && srcDeclaredType.componentType().toJava().equals(int.class)
                                && destDeclaredType != null
                                && destDeclaredType.isArrayClass()
                                && destDeclaredType.componentType().toJava().equals(int.class)) {
                    snippetMethod = intArrayCopy;
                } else if (srcDeclaredType != null
                                && srcDeclaredType.isArrayClass()
                                && srcDeclaredType.componentType().toJava().equals(char.class)
                                && destDeclaredType != null
                                && destDeclaredType.isArrayClass()
                                && destDeclaredType.componentType().toJava().equals(char.class)) {
                    snippetMethod = charArrayCopy;
                } else if (srcDeclaredType != null
                                && srcDeclaredType.isArrayClass()
                                && srcDeclaredType.componentType().toJava().equals(long.class)
                                && destDeclaredType != null
                                && destDeclaredType.isArrayClass()
                                && destDeclaredType.componentType().toJava().equals(long.class)) {
                    snippetMethod = longArrayCopy;
                }
            }

            if (snippetMethod != null) {
                StructuredGraph snippetGraph = (StructuredGraph) snippetMethod.compilerStorage().get(Graph.class);
                if (snippetGraph == null) {
                    snippetGraph = new StructuredGraph();
                    new GraphBuilderPhase(runtime, snippetMethod).apply(snippetGraph);
                    new PhiSimplificationPhase().apply(snippetGraph);
                    snippetMethod.compilerStorage().put(Graph.class, snippetGraph);
                }
                TTY.println("> use array copy snippet <");
                InliningUtil.inline(methodCallTarget.invoke(), snippetGraph, false);
            }
        }
        new CanonicalizerPhase(null, runtime, null).apply(graph);
    }

}
