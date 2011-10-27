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

import java.lang.reflect.*;
import java.util.*;

import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.debug.*;
import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.cri.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * Utilities for snippet installation and management.
 */
public class Snippets {

    public static void install(GraalRuntime runtime, CiTarget target, SnippetsInterface obj, boolean plotGraphs) {
        Class<? extends SnippetsInterface> clazz = obj.getClass();
        Class<?> original = clazz.getAnnotation(ClassSubstitution.class).value();
        GraalContext context = new GraalContext("Installing Snippet");

        for (Method snippet : clazz.getDeclaredMethods()) {
            try {
                Method method = original.getDeclaredMethod(snippet.getName(), snippet.getParameterTypes());
                if (!method.getReturnType().isAssignableFrom(snippet.getReturnType())) {
                    throw new RuntimeException("Snippet has incompatible return type");
                }

                int modifiers = snippet.getModifiers();
                if (Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers)) {
                    throw new RuntimeException("Snippet must not be abstract or native");
                }
                RiResolvedMethod snippetRiMethod = runtime.getRiMethod(snippet);
                GraphBuilderPhase graphBuilder = new GraphBuilderPhase(context, runtime, snippetRiMethod, null, false, true);
                Graph<EntryPointNode> graph = new Graph<EntryPointNode>(new EntryPointNode(runtime));
                graphBuilder.apply(graph);

                if (plotGraphs) {
                    IdealGraphPrinterObserver observer = new IdealGraphPrinterObserver(GraalOptions.PrintIdealGraphAddress, GraalOptions.PrintIdealGraphPort);
                    observer.printSingleGraph(method.getName(), graph);
                }

                new SnippetIntrinsificationPhase(context, runtime).apply(graph);

                Collection<InvokeNode> invokes = new ArrayList<InvokeNode>();
                for (InvokeNode invoke : graph.getNodes(InvokeNode.class)) {
                    invokes.add(invoke);
                }
                new InliningPhase(context, runtime, target, invokes).apply(graph);

                new SnippetIntrinsificationPhase(context, runtime).apply(graph);

                if (plotGraphs) {
                    IdealGraphPrinterObserver observer = new IdealGraphPrinterObserver(GraalOptions.PrintIdealGraphAddress, GraalOptions.PrintIdealGraphPort);
                    observer.printSingleGraph(method.getName(), graph);
                }
                new DeadCodeEliminationPhase(context).apply(graph);

                if (plotGraphs) {
                    IdealGraphPrinterObserver observer = new IdealGraphPrinterObserver(GraalOptions.PrintIdealGraphAddress, GraalOptions.PrintIdealGraphPort);
                    observer.printSingleGraph(method.getName(), graph);
                }

                RiResolvedMethod targetRiMethod = runtime.getRiMethod(method);
                targetRiMethod.compilerStorage().put(Graph.class, graph);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Could not resolve method to substitute with: " + snippet.getName(), e);
            }
        }
    }

}
