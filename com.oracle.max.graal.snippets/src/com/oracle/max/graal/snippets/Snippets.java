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
import com.oracle.max.graal.compiler.graphbuilder.*;
import com.oracle.max.graal.compiler.observer.*;
import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.cri.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.extended.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * Utilities for snippet installation and management.
 */
public class Snippets {

    public static void install(GraalRuntime runtime, CiTarget target, SnippetsInterface obj, boolean plotGraphs, PhasePlan plan) {
        Class<? extends SnippetsInterface> clazz = obj.getClass();
        GraalContext context = new GraalContext("Installing Snippet");
        BoxingMethodPool pool = new BoxingMethodPool(runtime);
        if (clazz.isAnnotationPresent(ClassSubstitution.class)) {
            installSubstitution(runtime, target, plotGraphs, plan, clazz, context, pool, clazz.getAnnotation(ClassSubstitution.class).value());
        } else {
            installSnippets(runtime, target, plotGraphs, plan, clazz, context, pool);
        }
    }

    private static void installSnippets(GraalRuntime runtime, CiTarget target, boolean plotGraphs, PhasePlan plan, Class< ? extends SnippetsInterface> clazz, GraalContext context,
                    BoxingMethodPool pool) {
        for (Method snippet : clazz.getDeclaredMethods()) {
            try {
                StructuredGraph graph = buildSnippetGraph(snippet, runtime, target, context, pool, plotGraphs, plan);
                RiResolvedMethod targetRiMethod = runtime.getRiMethod(snippet);
                targetRiMethod.compilerStorage().put(Graph.class, graph);
            } catch (GraalInternalError error) {
                if (context.isObserved()) {
                    if (error.node() != null) {
                        context.observable.fireCompilationEvent("VerificationError on Node " + error.node(), CompilationEvent.ERROR, error.node().graph());
                    } else if (error.graph() != null) {
                        context.observable.fireCompilationEvent("VerificationError on Graph " + error.graph(), CompilationEvent.ERROR, error.graph());
                    }
                }
                throw error;
            }
        }
    }

    private static void installSubstitution(GraalRuntime runtime, CiTarget target, boolean plotGraphs, PhasePlan plan, Class< ? extends SnippetsInterface> clazz, GraalContext context,
                    BoxingMethodPool pool, Class<?> original) throws GraalInternalError {
        for (Method snippet : clazz.getDeclaredMethods()) {
            try {
                Method method = original.getDeclaredMethod(snippet.getName(), snippet.getParameterTypes());
                if (!method.getReturnType().isAssignableFrom(snippet.getReturnType())) {
                    throw new RuntimeException("Snippet has incompatible return type");
                }
                StructuredGraph graph = buildSnippetGraph(snippet, runtime, target, context, pool, plotGraphs, plan);
                RiResolvedMethod targetRiMethod = runtime.getRiMethod(method);
                targetRiMethod.compilerStorage().put(Graph.class, graph);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Could not resolve method to substitute with: " + snippet.getName(), e);
            } catch (GraalInternalError error) {
                if (context.isObserved()) {
                    if (error.node() != null) {
                        context.observable.fireCompilationEvent("VerificationError on Node " + error.node(), CompilationEvent.ERROR, error.node().graph());
                    } else if (error.graph() != null) {
                        context.observable.fireCompilationEvent("VerificationError on Graph " + error.graph(), CompilationEvent.ERROR, error.graph());
                    }
                }
                throw error;
            }
        }
    }

    public static StructuredGraph buildSnippetGraph(Method snippet, GraalRuntime runtime, CiTarget target, GraalContext context, BoxingMethodPool pool, boolean plotGraphs, PhasePlan plan) {
        int modifiers = snippet.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers)) {
            throw new RuntimeException("Snippet must not be abstract or native");
        }
        RiResolvedMethod snippetRiMethod = runtime.getRiMethod(snippet);
        GraphBuilderConfiguration config = GraphBuilderConfiguration.getDeoptFreeDefault();
        GraphBuilderPhase graphBuilder = new GraphBuilderPhase(runtime, snippetRiMethod, null, config);
        StructuredGraph graph = new StructuredGraph();
        graphBuilder.apply(graph, context);

        if (plotGraphs) {
            IdealGraphPrinterObserver observer = new IdealGraphPrinterObserver(GraalOptions.PrintIdealGraphAddress, GraalOptions.PrintIdealGraphPort);
            observer.printSingleGraph(snippet.getName(), graph);
        }

        new IntrinsificationPhase(runtime).apply(graph, context);
        new SnippetIntrinsificationPhase(runtime, pool).apply(graph, context);

        Collection<Invoke> invokes = new ArrayList<Invoke>();
        for (InvokeNode invoke : graph.getNodes(InvokeNode.class)) {
            invokes.add(invoke);
        }
        for (InvokeWithExceptionNode invoke : graph.getNodes(InvokeWithExceptionNode.class)) {
            invokes.add(invoke);
        }
        new InliningPhase(target, runtime, invokes, null, plan, config).apply(graph, context);
        new IntrinsificationPhase(runtime).apply(graph, context);

        new SnippetIntrinsificationPhase(runtime, pool).apply(graph, context);

        if (plotGraphs) {
            IdealGraphPrinterObserver observer = new IdealGraphPrinterObserver(GraalOptions.PrintIdealGraphAddress, GraalOptions.PrintIdealGraphPort);
            observer.printSingleGraph(snippet.getName(), graph);
        }
        new DeadCodeEliminationPhase().apply(graph, context);

        if (plotGraphs) {
            IdealGraphPrinterObserver observer = new IdealGraphPrinterObserver(GraalOptions.PrintIdealGraphAddress, GraalOptions.PrintIdealGraphPort);
            observer.printSingleGraph(snippet.getName(), graph);
        }

        return graph;
    }

}
