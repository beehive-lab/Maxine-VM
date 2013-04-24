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

import java.util.concurrent.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.*;
import com.oracle.graal.compiler.target.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.java.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.OptimisticOptimizations.Optimization;
import com.oracle.graal.phases.PhasePlan.PhasePosition;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.phases.tiers.*;
import com.oracle.graal.printer.*;
import com.oracle.max.vm.ext.graal.amd64.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;

/**
 * Integration of the Graal compiler into Maxine's compilation framework. The compiler must be included in the boot image,
 * as the snippet generation needs access to things that are not available to a VM extension (i.e., {@link HOSTED_ONLY} fields of VM classes).
 *
 * It is necessary to call {@link FieldIntrospection#rescanAllFieldOffsets()} on VM startup as the
 * {@link NodeClass} field offsets calculated during boot image construction are those of the host VM. In principle this
 * could be fixed up during boot image creation, as per the JDK classes, but as Graal provides a method to do it, and
 * the necessary fixup is quite complicated, we do it on VM startup.
 * TODO: this likely isn't sufficient for the future when Graal is the only compiler, and probably will require the offsets to be
 * explicitly fixed up in the boot image.
 *
 * The Graal debug environment similarly needs to be re-established when the VM runs. It is setup for
 * debugging snippet installation in the boot image generation, but the config is stored in a thread local
 * so it does not survive in the boot image.
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

    // Graal doesn't provide an instance that captures all this additioinal state needed
    // for a compilation. Instead these values are passed to compileMethod.
    private MaxRuntime runtime;
    private Backend backend;
    private Replacements replacements;
    private OptimisticOptimizations optimisticOpts;
    private MaxGraphCache cache;
    private Suites suites;

    public MaxGraal() {
        if (singleton == null) {
            singleton = this;
        }
    }

    public static MaxRuntime runtime() {
        assert singleton != null && singleton.runtime != null;
        return singleton.runtime;
    }

    /**
     * See {@link CompilationBroker#initialize(Phase).
     * When an added compiler only called in {@link MaxineVM.Phase#HOSTED_COMPILING} and {@link MaxineVM.Phase#STARTING} phases,
     * <i>after</i> {@link CompilationBroker#addCompiler} has been called.
     */
    @Override
    public void initialize(Phase phase) {
        if (phase == MaxineVM.Phase.HOSTED_COMPILING) {
            MaxGraalOptions.initialize();
            createGraalCompiler(phase);
        } else if (phase == MaxineVM.Phase.STARTING) {
            // This is the obvious place to call rescanAllFieldOffsets() but it is too early in the startup without
            // more classes related to annotations being in the boot image, because bootclasspath is not setup.
            // So it is done lazily when the first compile is initiated.
            //
        } else if (phase == MaxineVM.Phase.RUNNING) {
            // Would be called if the compiler was registered as the default optimizing compiler.
        }
    }

    private static boolean lazyRuntimeInitDone;

    private static void lazyRunTimeInit() {
        if (lazyRuntimeInitDone) {
            return;
        }
        try {
            FieldIntrospection.rescanAllFieldOffsets(new FieldIntrospection.DefaultCalcOffset());
            // This sets up the debug environment for the running VM
            DebugEnvironment.initialize(System.out);
            lazyRuntimeInitDone = true;
        } catch (Throwable t) {
            Log.println("FieldIntrospection.rescanAllFieldOffsets failed: " + t);
            MaxineVM.exit(-1);
        }
    }

    private void createGraalCompiler(Phase phase) {
        // This sets up the debug environment for the boot image build
        DebugEnvironment.initialize(System.out);
        MaxTargetDescription td = new MaxTargetDescription();
        runtime = new MaxRuntime(td);
        backend = new MaxAMD64Backend(runtime, td);
        // We want exception handlers generated
        optimisticOpts = OptimisticOptimizations.ALL.remove(Optimization.UseExceptionProbability, Optimization.UseExceptionProbabilityForOperations);
        cache = new MaxGraphCache();
        changeInlineLimits();
        // Disable for now as it causes problems in DebugInfo
        GraalOptions.PartialEscapeAnalysis = false;
        suites = Suites.createSuites(GraalOptions.CompilerConfiguration);
        replacements = runtime.init();
    }

    /**
     * Graal's inlining limits are very aggressive, too high for the Maxine environment.
     * So we dial them down here.
     */
    private static void changeInlineLimits() {
        GraalOptions.NormalBytecodeSize = 30;
        GraalOptions.NormalComplexity = 30;
        GraalOptions.NormalCompiledCodeSize = 150;
    }

    @NEVER_INLINE
    public static void unimplemented(String methodName) {
        ProgramError.unexpected("unimplemented: " + methodName);
    }

    @Override
    public TargetMethod compile(ClassMethodActor methodActor, boolean isDeopt, boolean install, CiStatistics stats) {
        lazyRunTimeInit();
        PhasePlan phasePlan = new PhasePlan();
        ResolvedJavaMethod method = MaxResolvedJavaMethod.get(methodActor);

        StructuredGraph graph = (StructuredGraph) method.getCompilerStorage().get(Graph.class);
        if (graph == null) {
            GraphBuilderPhase graphBuilderPhase = new MaxGraphBuilderPhase(runtime, GraphBuilderConfiguration.getDefault(), optimisticOpts);
            phasePlan.addPhase(PhasePosition.AFTER_PARSING, graphBuilderPhase);
            // Hosted (boot image) compilation is more complex owing to the need to handle the unsafe features of VM programming,
            // requiring more phases to be run. All this is very similar to what happens in MaxReplacementsImpl
            if (MaxineVM.isHosted()) {
                // In Graal, @Fold is purely a snippet notion; we handle Maxine's more general use with similar code.
                // We do this early to reduce the size of a graph that contains isHosted code.
                phasePlan.addPhase(PhasePosition.AFTER_PARSING, new MaxFoldPhase(runtime));
                // Any folded isHosted code is only removed by a CanonicalizationPhase
                phasePlan.addPhase(PhasePosition.AFTER_PARSING, new CanonicalizerPhase.Instance(runtime, null));
                phasePlan.addPhase(PhasePosition.AFTER_PARSING, new MaxIntrinsicsPhase());
                phasePlan.addPhase(PhasePosition.AFTER_PARSING,
                                new MaxWordTypeRewriterPhase.MakeWordFinalRewriter(runtime, runtime.maxTargetDescription.wordKind));
                // In order to have Maxine's INLINE annotation interpreted, we have to disable the standard inlining phase
                // and substitute a custom phase that checks the method annotation.
                phasePlan.disablePhase(InliningPhase.class);
                phasePlan.addPhase(PhasePosition.HIGH_LEVEL,
                                new MaxHostedInliningPhase(runtime, replacements, new Assumptions(GraalOptions.OptAssumptions), cache, phasePlan, optimisticOpts));
                // Important to remove bogus null checks on Word types
                phasePlan.addPhase(PhasePosition.HIGH_LEVEL, new MaxWordTypeRewriterPhase.MaxNullCheckRewriter(runtime, runtime.maxTargetDescription.wordKind));
                phasePlan.addPhase(PhasePosition.HIGH_LEVEL,
                                new MaxWordTypeRewriterPhase.KindRewriter(runtime, runtime.maxTargetDescription.wordKind));

            }
            graph = new StructuredGraph(method);
        }
        // Graal sometimes calls Class.forName on its own classes using the context class loader
        // so we have to override it as VM classes are ordinarily hidden
        ClassLoader savedContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(VMClassLoader.VM_CLASS_LOADER);
        try {
            CompilationResult result = GraalCompiler.compileMethod(runtime, replacements, backend, runtime.maxTargetDescription,
                            method, graph, cache, phasePlan, optimisticOpts, new SpeculationLog(), suites);
            return GraalMaxTargetMethod.create(methodActor, MaxCiTargetMethod.create(result), true);
        } catch (Throwable ex) {
            throw ex;
        } finally {
            Thread.currentThread().setContextClassLoader(savedContextClassLoader);
        }
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
