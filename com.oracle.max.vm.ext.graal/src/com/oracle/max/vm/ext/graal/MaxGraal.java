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
import java.util.*;
import java.util.concurrent.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.java.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.PhasePlan.PhasePosition;
import com.oracle.graal.printer.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;

/**
 * Integration of the Graal compiler into Maxine's compilation framework.
 * Currently the compiler is loaded as a VM extension and not included in the boot image.
 * Amongst other things, this means that Graal options cannot be set on the command line,
 * but just be set using {@code -vmextension:jar=args}.
 */
public class MaxGraal implements RuntimeCompiler {

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
    // The singleton instance of this compiler
    private static MaxGraal singleton;

    private GraalCompiler compiler;
    public MaxRuntime runtime;
    private OptimisticOptimizations optimisticOpts;
    private MaxGraphCache cache;

    public static void onLoad(String args) {
        // Graal options are passed as a comma separated list in args
        MaxGraalOptions.initialize(args);
        // Graal uses the context class loader during snippet instrinsification, so
        // we must override the default to be the VM class loader
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(VMClassLoader.VM_CLASS_LOADER);
            createCompiler();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    @Override
    public void initialize(Phase phase) {
        // In extension mode, this is only called in the RUNNING phase,
        // at which point there is nothing else to do.
        MaxGraalOptions.initialize();
    }

    private static void createCompiler() {
        singleton = (MaxGraal) CompilationBroker.addCompiler(NAME, "com.oracle.max.vm.ext.graal.MaxGraal");
        MaxTargetDescription td = new MaxTargetDescription();
        singleton.runtime = new MaxRuntime(td);
        singleton.compiler = new GraalCompiler(singleton.runtime, td, new MaxAMD64Backend(singleton.runtime, td));
        singleton.optimisticOpts = OptimisticOptimizations.ALL;
        singleton.cache = new MaxGraphCache();
        singleton.runtime.init();
    }

    @NEVER_INLINE
    static void unimplemented(String methodName) {
        ProgramError.unexpected("unimplemented: " + methodName);
    }

    private static DebugConfig makeDebugConfig(boolean dump) {
        if (dump) {
            Debug.enable();
            return Debug.fixedConfig(false, true, false, false, Arrays.asList(new GraphPrinterDumpHandler(), new CFGPrinterObserver()), System.out);
        } else {
            return Debug.fixedConfig(false, false, false, false, new ArrayList<DebugDumpHandler>(), System.out);
        }
    }

    @Override
    public TargetMethod compile(ClassMethodActor methodActor, boolean isDeopt, boolean install, CiStatistics stats) {
        try {
            DebugConfig debugConfig = makeDebugConfig(true);
            Debug.setConfig(debugConfig);
            PhasePlan phasePlan = new PhasePlan();
            ResolvedJavaMethod method = MaxResolvedJavaMethod.get(methodActor);

            StructuredGraph graph = (StructuredGraph) method.getCompilerStorage().get(Graph.class);
            if (graph == null) {
                GraphBuilderPhase graphBuilderPhase = new GraphBuilderPhase(runtime, GraphBuilderConfiguration.getDefault(), optimisticOpts);
                phasePlan.addPhase(PhasePosition.AFTER_PARSING, graphBuilderPhase);
                graph = new StructuredGraph(method);
            }
            CompilationResult result = compiler.compileMethod(method, graph, cache, phasePlan, optimisticOpts);
            MaxTargetMethod graalTM = new MaxTargetMethod(methodActor, MaxCiTargetMethod.create(result), true);
            // compile with C1X for comparison
            TargetMethod c1xTM = CompilationBroker.singleton.optimizingCompiler.compile(methodActor, isDeopt, install, stats);
            compare(graalTM, c1xTM);
            return graalTM;
        } catch (Throwable t) {
            ProgramError.unexpected(t);
            return null;
        }
    }

    private static void compare(TargetMethod grallTM, TargetMethod c1xTM) {

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
