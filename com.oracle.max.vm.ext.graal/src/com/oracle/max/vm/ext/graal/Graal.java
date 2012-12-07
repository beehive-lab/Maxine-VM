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

package com.oracle.max.vm.ext.graal;
import static com.sun.max.vm.MaxineVM.*;

import java.util.concurrent.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.java.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.PhasePlan.PhasePosition;
import com.sun.cri.ci.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Integration of the Graal compiler into Maxine's compilation framework.
 */
public class Graal implements RuntimeCompiler {

    private static class MaxGraphCache implements GraphCache {

        private final ConcurrentMap<ResolvedJavaMethod, StructuredGraph> cache = new ConcurrentHashMap<ResolvedJavaMethod, StructuredGraph>();

        @Override
        public void put(StructuredGraph graph) {
            cache.put(graph.method(), graph);
        }

        @Override
        public StructuredGraph get(ResolvedJavaMethod method) {
            return cache.get(method);
        }
    }

    private static final String NAME = "Graal";
    private GraalCompiler compiler;
    public MaxRuntime runtime;
    private OptimisticOptimizations optimisticOpts;
    private MaxGraphCache cache;

    public static void onLoad(String args) {

    }

    @Override
    public void initialize(Phase phase) {
        if (isHosted() && phase == Phase.HOSTED_COMPILING) {
            createCompiler();
        }

        if (phase == Phase.STARTING && compiler == null) {
            createCompiler();
        }

    }

    private void createCompiler() {
        if (compiler == null) {
            runtime = new MaxRuntime();
            MaxTargetDescription td = new MaxTargetDescription();
            compiler = new GraalCompiler(runtime, td, new MaxAMD64Backend(runtime, td));
            optimisticOpts = OptimisticOptimizations.ALL;
            cache = new MaxGraphCache();
            CompilationBroker.addCompiler(NAME, "com.oracle.max.vm.ext.graal.Graal");
        }
    }

    @Override
    public TargetMethod compile(ClassMethodActor methodActor, boolean isDeopt, boolean install, CiStatistics stats) {
        PhasePlan phasePlan = new PhasePlan();
        ResolvedJavaMethod method = MaxResolvedJavaMethod.get(methodActor);

        StructuredGraph graph = (StructuredGraph) method.getCompilerStorage().get(Graph.class);
        if (graph == null) {
            GraphBuilderPhase graphBuilderPhase = new GraphBuilderPhase(runtime, GraphBuilderConfiguration.getDefault(), optimisticOpts);
            phasePlan.addPhase(PhasePosition.AFTER_PARSING, graphBuilderPhase);
            graph = new StructuredGraph(method);
        }
        compiler.compileMethod(method, graph, cache, phasePlan, optimisticOpts);
        return null;
    }

    @Override
    public Nature nature() {
        return Nature.OPT;
    }

    @Override
    public boolean matches(String compilerName) {
        return compilerName.equals(NAME);
    }

}
