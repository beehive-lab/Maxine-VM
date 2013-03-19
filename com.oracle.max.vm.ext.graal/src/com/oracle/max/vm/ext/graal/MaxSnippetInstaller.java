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
package com.oracle.max.vm.ext.graal;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.java.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.snippets.*;
import com.oracle.max.vm.ext.graal.MaxRuntime.*;
import com.oracle.max.vm.ext.graal.snippets.*;
import com.sun.max.program.*;

class MaxSnippetInstaller extends SnippetInstaller {
    private MaxSnippetGraphBuilderConfiguration maxSnippetGraphBuilderConfiguration;
    Map<Class< ? extends Node>, LoweringProvider> lowerings;

    MaxSnippetInstaller(MetaAccessProvider runtime, Assumptions assumptions, TargetDescription target,
                    MaxSnippetGraphBuilderConfiguration maxSnippetGraphBuilderConfiguration,
                    Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, assumptions, target);
        this.maxSnippetGraphBuilderConfiguration = maxSnippetGraphBuilderConfiguration;
        this.lowerings = lowerings;
    }

    public void installAndRegister(Class< ? extends SnippetLowerings> clazz) {
        installSnippets(clazz);
        // assumption is that it is ok to register the lowerings incrementally
        try {
            Constructor< ? > cons = clazz.getDeclaredConstructor(CodeCacheProvider.class, TargetDescription.class, Assumptions.class, Map.class);
            Object[] args = new Object[] {runtime, target, assumptions, lowerings};
            SnippetLowerings snippetLowerings = (SnippetLowerings) cons.newInstance(args);
            snippetLowerings.registerLowerings((CodeCacheProvider) runtime, target, assumptions, lowerings);
        } catch (Exception ex) {
            ProgramError.unexpected("MaxSnippetInstaller: problem: " + ex + " in: " + clazz);
        }
    }

    @Override
    protected StructuredGraph buildInitialGraph(final ResolvedJavaMethod method) {
        final StructuredGraph graph = new StructuredGraph(method);

        GraphBuilderPhase graphBuilder = new GraphBuilderPhase(runtime, maxSnippetGraphBuilderConfiguration, OptimisticOptimizations.NONE);
        graphBuilder.apply(graph);

        Debug.dump(graph, "%s: %s", method.getName(), GraphBuilderPhase.class.getSimpleName());

        new MaxIntrinsicsPhase().apply(graph);
        new MaxWordTypeRewriterPhase.MaxInvokeRewriter(runtime, target.wordKind).apply(graph);
        new SnippetIntrinsificationPhase(runtime, pool).apply(graph);
        // need constant propagation for folded methods
        new CanonicalizerPhase(runtime, assumptions).apply(graph);

        return graph;
    }

    @Override
    public void afterInline(StructuredGraph caller, StructuredGraph callee) {
        new MaxWordTypeRewriterPhase.MaxNullCheckRewriter(runtime, target.wordKind).apply(caller);
        new CanonicalizerPhase(runtime, assumptions).apply(caller);
        new MaxIntrinsicsPhase().apply(caller);
    }

    @Override
    protected void finalizeGraph(ResolvedJavaMethod method, StructuredGraph graph) {
        new SnippetIntrinsificationPhase(runtime, pool).apply(graph);

        // These only get done once right at the end
        new MaxWordTypeRewriterPhase.MaxUnsafeCastRewriter(runtime, target.wordKind).apply(graph);
        new MaxSlowpathRewriterPhase(runtime).apply(graph);
        new MaxWordTypeRewriterPhase.MaxNullCheckRewriter(runtime, target.wordKind).apply(graph);

        // The replaces all Word based types with long
        new MaxWordTypeRewriterPhase.KindRewriter(runtime, target.wordKind).apply(graph);

        new SnippetFrameStateCleanupPhase().apply(graph);
        new DeadCodeEliminationPhase().apply(graph);

        new InsertStateAfterPlaceholderPhase().apply(graph);
    }

    @Override
    public void afterInlining(StructuredGraph graph) {
        new SnippetIntrinsificationPhase(runtime, pool).apply(graph);

        new DeadCodeEliminationPhase().apply(graph);
        new CanonicalizerPhase(runtime, assumptions).apply(graph);

    }

}
