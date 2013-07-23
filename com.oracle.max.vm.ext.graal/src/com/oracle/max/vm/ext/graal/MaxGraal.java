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

import static com.sun.max.vm.VMOptions.*;

import java.lang.reflect.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.api.meta.Kind;
import com.oracle.graal.compiler.*;
import com.oracle.graal.compiler.target.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.java.*;
import com.oracle.graal.lir.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.OptimisticOptimizations.Optimization;
import com.oracle.graal.phases.PhasePlan.PhasePosition;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.phases.tiers.*;
import com.oracle.graal.printer.*;
import com.oracle.graal.replacements.*;
import com.oracle.max.vm.ext.graal.amd64.*;
import com.oracle.max.vm.ext.graal.phases.*;
import com.oracle.max.vm.ext.graal.snippets.*;
import com.oracle.max.vm.ext.graal.stubs.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.config.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

/**
 * Integration of the Graal compiler into Maxine's compilation framework.
 *
 * Graal makes sophisticated use of {@link Unsafe} field offsets in its graph manipulations, and provides
 * a method {@link FieldIntrospection#rescanAllFieldOffsets()} to (re)compute the offsets. Since these differ
 * between the host VM and Maxine, they have to be recomputed. Graal stores the offsets in arrays
 * held in {@link FieldIntrospection} and {@link NodeClass}, so the usual {@link JDKInterceptor} mechanism isn't useful.
 * So the fix up is done in the {@link MaxineVM.Phase#SERIALIZING_IMAGE} phase before the data objects are created,
 * and, importantly, after all compilation is complete!
 *
 * The Graal debug environment similarly needs to be re-established when the VM runs. It is setup for debugging snippet
 * installation in the boot image generation, but the config is stored in a thread local so it does not survive in the
 * boot image. We establish it in the {@link MaxineVM.Phase#STARTING} phase, as compilations can happen after that.
 */
public class MaxGraal extends RuntimeCompiler.DefaultNameAdapter implements RuntimeCompiler {

    private static class MaxGraphCache implements GraphCache {

        private ConcurrentMap<ResolvedJavaMethod, StructuredGraph> cache = new ConcurrentHashMap<ResolvedJavaMethod, StructuredGraph>();

        @Override
        public StructuredGraph get(ResolvedJavaMethod method) {
            return cache.get(method);
        }

        @Override
        public void put(StructuredGraph graph, boolean hasMatureProfilingInfo) {
            cache.put(graph.method(), graph);
        }
    }
    static {
        addFieldOption("-XX:", "MaxGraalForBoot", "use Graal for boot image");
    }

    public static boolean MaxGraalForBoot;

    private static final String NAME = "Graal";

    // Graal doesn't provide an instance that captures all this additional state needed
    // for a compilation. Instead these values are passed to compileMethod.
    protected MaxRuntime runtime;
    private Backend backend;
    private MaxReplacementsImpl replacements;
    private OptimisticOptimizations optimisticOpts;

    @RESET
    private MaxGraphCache cache;
    /**
     * This used to be a Graal option, now subsumed by {@link AOTCompilation}.
     * However, {@link Canonicalizer} still uses {@code canonicalizeReads}.
     */
    public static final boolean canonicalizeReads = true;
    /**
     * Standard configuration used for non boot image compilation.
     */
    private Suites suites;
    /**
     * Customized suites for compiling the boot image.
     */
    private Suites bootSuites;

    /**
     * Gets the {@link MaxRuntime} associated with the compiler that is currently active in the current thread.
     * @return
     */
    public static MaxRuntime runtime() {
        MaxGraal current = stateThreadLocal.get().maxGraal;
        assert current != null;
        return current.runtime;
    }

    /**
     * Allows the {@link RegisterConfig} to be correctly set when compiling the boot image, given
     * that {@link CodeCacheProvider#lookupRegisterConfig()} does not provide the method being compiled.
     */
    static ClassMethodActor methodBeingCompiled() {
        return stateThreadLocal.get().methodActor;
    }

    private static class TraceDefaultAndSetMaxineFieldOffset extends FieldIntrospection.DefaultCalcOffset {
        @Override
        public long getOffset(Field field) {
            long hostOffset = super.getOffset(field);
            long maxOffset = FieldActor.fromJava(field).offset();
            Trace.line(2, field.getDeclaringClass().getName() + "." + field.getName() + " has host offset: " + hostOffset + ", Maxine offset: " + maxOffset);
            return maxOffset;
        }
    }

    private static class State {
        MaxGraal maxGraal;
        boolean inlineDone;
        boolean bootCompile;
        ClassMethodActor methodActor; // only set when bootCompile==true
    }

    /**
     * A thread local that records relevant state about the compilation for external access.
     */
    private static class StateThreadLocal extends ThreadLocal<State> {
        @Override
        public State initialValue() {
            return new State();
        }
    }

    private static final StateThreadLocal stateThreadLocal = new StateThreadLocal();

    private static class InlineDone extends com.oracle.graal.phases.Phase {

        @Override
        protected void run(StructuredGraph graph) {
            // Checkstyle: stop
            stateThreadLocal.get().inlineDone = true;
            // Checkstyle: resume
        }
    }

    public static boolean inlineDone() {
        return stateThreadLocal.get().inlineDone;
    }

    public static boolean bootCompile() {
        State state = stateThreadLocal.get();
        return state.bootCompile;
    }

    /**
     * Callback to suppress all snippets from being compiled into the boot image.
     */
    @HOSTED_ONLY
    private static class NoSnippetCompilation extends CompiledPrototype.NeedsCompilationCallback {
        @Override
        public boolean needsCompilation(MethodActor methodActor) {
            return methodActor.getAnnotation(Snippet.class) == null;
        }

    }

    @Override
    public void initialize(Phase phase) {
        if (phase == MaxineVM.Phase.HOSTED_COMPILING) {
            MaxGraalOptions.initialize(phase);
            createGraalCompiler(phase);
            forceJDKSubstitutedNatives();
            CompiledPrototype.registerNeedsCompilationCallback(new NoSnippetCompilation());
        } else if (phase == MaxineVM.Phase.SERIALIZING_IMAGE) {
            TraceNodeClasses.scan();
            FieldIntrospection.rescanAllFieldOffsets(new TraceDefaultAndSetMaxineFieldOffset());
            // reset the default
            GraalOptions.OptPushThroughPi.setValue(true);
        } else if (phase == MaxineVM.Phase.RUNNING) {
            cache = new MaxGraphCache();
            // Compilations can occur after this, so set up the debug environment and check options.
            if (MaxGraalOptions.isPresent("Dump") != null) {
                dumpWorkAround();
            }
            // Now we can actually set the Graal options and initialize the Debug environment
            MaxGraalOptions.initialize(phase);
            DebugEnvironment.initialize(System.out);
        }
    }

    /**
     * Metacircularity workaround for {@code -G:Dump}.
     * There is a circularity if {@code -G:Dump} is set, as this will try to open the network printer
     * which will cause native stubs in the JDK network library to be created and compiled, which will
     * try to open the network printer (irrespective of the {@code -G:MethodFilter} setting)...
     * So we check if the option is set and, if so, open a socket to force the stub compilations before
     * initializing the DebugEnvironment. We actually dump a dummy graph to keep IGV happy.
     */
    private static void dumpWorkAround() {
        try {
            // Get either the set values or the Graal defaults
            VMStringOption hostOption = (VMStringOption) MaxGraalOptions.isPresent("PrintIdealGraphAddress");
            String host = hostOption == null ? GraalOptions.PrintIdealGraphAddress.getValue() : hostOption.getValue();
            VMIntOption portOption = (VMIntOption) MaxGraalOptions.isPresent("PrintIdealGraphPort");
            int port = portOption == null ? GraalOptions.PrintIdealGraphPort.getValue() : portOption.getValue();
            BinaryGraphPrinter printer = new BinaryGraphPrinter(SocketChannel.open(new InetSocketAddress(host, port)));
            printer.print(MaxMiscLowerings.dummyGraph(), "dummy", null);
            printer.close();
        } catch (Exception ex) {
        }

    }


    private State setMaxGraal() {
        State state = stateThreadLocal.get();
        state.maxGraal = this;
        return state;
    }

    @HOSTED_ONLY
    private void createGraalCompiler(Phase phase) {
        State state = setMaxGraal();
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
        if (MaxGraalOptions.isPresent("PartialEscapeAnalysis") == null) {
            GraalOptions.PartialEscapeAnalysis.setValue(false);
        }
        if (MaxGraalOptions.isPresent("OptPushThroughPi") == null) {
            /*
             * When compiling the boot image, this phase (in the MidTier) can replace a genuine object argument to an
             * IsNullNode with a Word type, when the object is the result of an UNSAFE_CAST from a Word type, which then
             * gets rewritten to long, and then the graph does not verify. If there was a PhasePosition.MID_LEVEL hook,
             * we could perhaps fix this by running the KindRewriter before the mid-level suite.
             * There is no way to disable this phase (unlike InliningPhase) so we do it via an option.
             */
            GraalOptions.OptPushThroughPi.setValue(false);
        }
        suites = Suites.createDefaultSuites();
        createBootSuites();
        // For snippet creation we need bootCompile==true for inlining of VM methods
        state.bootCompile = true;
        replacements = MaxSnippets.initialize(runtime);
        addCustomSnippets(replacements);
        GraalBoot.forceValues();
    }

    /**
     * Create the custom suites for boot image compilation.
     * We could do this with a custom {@link CompilerConfiguration} but that involves jar files
     * the {@link ServiceLoader} which doesn't fit the Maxine build model well.
     */
    @HOSTED_ONLY
    private void  createBootSuites() {
        bootSuites = Suites.createDefaultSuites();
        PhaseSuite<MidTierContext> midTier = bootSuites.getMidTier();
        midTier.appendPhase(new MaxWordType.KindRewriterPhase(runtime, runtime.maxTargetDescription().wordKind));
    }
    /**
     * Graal's inlining limits are very aggressive, too high for the Maxine environment.
     * So we dial them down here.
     */
    private static void changeInlineLimits() {
        if (MaxGraalOptions.isPresent("MaximumInliningSize") == null) {
            GraalOptions.MaximumInliningSize.setValue(100);
        }
    }

    @NEVER_INLINE
    public static void unimplemented(String methodName) {
        ProgramError.unexpected("unimplemented: " + methodName);
    }


    @Override
    public TargetMethod compile(ClassMethodActor methodActor, boolean isDeopt, boolean install, CiStatistics stats) {
        PhasePlan phasePlan = new PhasePlan();
        ResolvedJavaMethod method = MaxResolvedJavaMethod.get(methodActor);
        State state = setMaxGraal();
        state.inlineDone = false;
        state.bootCompile = false;
        Suites compileSuites;

        if ((MaxineVM.isHosted() && (MaxGraalForBoot || methodActor.toString().startsWith("jtt.max"))) ||
                        substitutedNativeMethod(methodActor)) {
            // The (temporary) check for MaxGraalForBoot prevents test compilations being treated as boot image code.
            // N.B. Native methods that have been substituted are effectively boot compilations and are not all
            // compiled into the boot image (perhaps they should be).
            compileSuites = bootSuites;
            state.bootCompile = true;
            state.methodActor = methodActor;
        } else {
            compileSuites = suites;
        }

        StructuredGraph graph = (StructuredGraph) method.getCompilerStorage().get(Graph.class);
        Double tailDuplicationProbability = GraalOptions.TailDuplicationProbability.getValue();
        if (graph == null) {
            GraphBuilderPhase graphBuilderPhase = new MaxGraphBuilderPhase(runtime, GraphBuilderConfiguration.getDefault(), optimisticOpts);
            phasePlan.addPhase(PhasePosition.AFTER_PARSING, graphBuilderPhase);
            addCustomPhase(methodActor, phasePlan);
            // Hosted (boot image) compilation is more complex owing to the need to handle the unsafe features of VM programming,
            // requiring more phases to be run. All this is very similar to what happens in MaxReplacementsImpl.
            if (state.bootCompile) {
                // TODO should be done by modifying bootSuites
                addBootPhases(phasePlan);
            }
            if (methodActor.compilee().isNative()) {
                graph = new NativeStubGraphBuilder(methodActor).build();
                GraalOptions.TailDuplicationProbability.setValue(2.0);
            } else {
                graph = new StructuredGraph(method);
            }
        }
        CallingConvention cc = CodeUtil.getCallingConvention(runtime, CallingConvention.Type.JavaCallee, method, false);
        try {
            CompilationResult result = GraalCompiler.compileGraph(graph, cc, method, runtime, replacements, backend,
                        runtime.maxTargetDescription(),
                        cache, phasePlan, optimisticOpts, new SpeculationLog(), compileSuites, new CompilationResult());
            return GraalMaxTargetMethod.create(methodActor, MaxCiTargetMethod.create(result), true);
        } finally {
            GraalOptions.TailDuplicationProbability.setValue(tailDuplicationProbability);
        }
    }

    private static boolean substitutedNativeMethod(ClassMethodActor methodActor) {
        return methodActor.isNative() && methodActor.compilee() != methodActor;
    }

    /**
     * A hook to add custom phase by a subclass.
     * @param methodActor method being compiled
     * @param phasePlan the pohasePlan to be customized
     */
    protected void addCustomPhase(ClassMethodActor methodActor, PhasePlan phasePlan) {
    }

    /**
     * A hook to add additional snippets by subclass.
     */
    protected void addCustomSnippets(MaxReplacementsImpl maxReplacementsImpl) {
    }

    @HOSTED_ONLY
    public List<com.oracle.graal.phases.Phase> addBootPhases(PhasePlan phasePlan) {
        Kind wordKind = runtime.maxTargetDescription().wordKind;
        ArrayList<com.oracle.graal.phases.Phase> result = new ArrayList<>();
        result.add(addPhase(phasePlan, PhasePosition.AFTER_PARSING, new MaxWordType.CheckWordObjectEqualsPhase(runtime, wordKind)));
        // In Graal, @Fold is purely a snippet notion; we handle Maxine's more general use with similar code.
        // We do this early to reduce the size of a graph that contains isHosted code.
        result.add(addPhase(phasePlan, PhasePosition.AFTER_PARSING, new MaxFoldPhase(runtime)));
        // Any folded isHosted code is only removed by a CanonicalizationPhase
        result.add(addPhase(phasePlan, PhasePosition.AFTER_PARSING, new CanonicalizerPhase.Instance(runtime,
                        new Assumptions(GraalOptions.OptAssumptions.getValue()), canonicalizeReads)));
        result.add(addPhase(phasePlan, PhasePosition.AFTER_PARSING, new MaxIntrinsicsPhase()));
        result.add(addPhase(phasePlan, PhasePosition.AFTER_PARSING,
                        new MaxWordType.MakeWordFinalRewriterPhase(runtime, wordKind)));

        // In order to have Maxine's (must) INLINE annotation interpreted, we have to disable the standard inlining phase
        // and substitute a custom phase that checks the method annotation.
        phasePlan.disablePhase(InliningPhase.class);
        result.add(addPhase(phasePlan, PhasePosition.HIGH_LEVEL,
                        new MaxHostedInliningPhase(runtime, null, replacements, new Assumptions(GraalOptions.OptAssumptions.getValue()),
                                        cache, phasePlan, optimisticOpts)));
        result.add(addPhase(phasePlan, PhasePosition.HIGH_LEVEL, new InlineDone()));
        // Important to remove bogus null checks on Word types
        result.add(addPhase(phasePlan, PhasePosition.HIGH_LEVEL, new MaxWordType.MaxNullCheckRewriterPhase(runtime, wordKind)));
        // intrinsics are (obviously) not inlined, so they are left in the graph and need to be rewritten now
        result.add(addPhase(phasePlan, PhasePosition.HIGH_LEVEL, new MaxIntrinsicsPhase()));
        // The replaces all Word based types with target.wordKind
        result.add(addPhase(phasePlan, PhasePosition.HIGH_LEVEL, new MaxWordType.KindRewriterPhase(runtime, wordKind)));
        // Remove UnsafeCasts rendered irrelevant by previous phase
        result.add(addPhase(phasePlan, PhasePosition.HIGH_LEVEL, new CanonicalizerPhase.Instance(runtime,
                        new Assumptions(GraalOptions.OptAssumptions.getValue()), canonicalizeReads)));
        return result;
    }

    private com.oracle.graal.phases.Phase addPhase(PhasePlan phasePlan, PhasePosition pos, com.oracle.graal.phases.Phase phase) {
        phasePlan.addPhase(pos, phase);
        return phase;
    }

    @Override
    public Nature nature() {
        return Nature.OPT;
    }

    @Override
    public boolean matches(String compilerName) {
        return compilerName.equals(NAME);
    }

    private static class TraceNodeClasses extends FieldIntrospection {
        TraceNodeClasses() {
            super(null);
        }

        public static void scan() {
            Trace.line(2, "NodeClass.allClasses");
            for (FieldIntrospection nodeClass : allClasses.values()) {
                Trace.line(2, nodeClass);
            }
        }

        @Override
        protected void rescanFieldOffsets(CalcOffset calc) {
        }
    }

    /**
     * With Graal it is not currently possible to compile code at runtime that uses Maxine annotations.
     * Not all the substituted native methods in the JDK are compiled into the boot image because they
     * are not used by boot image code (e.g. several in sun.misc.Unsafe). So we force them in here.
     * (C1X's inlining is such that it "gets away" with not interpreting the Maxine annotations.)
     *
     */
    private void forceJDKSubstitutedNatives() {
        for (ClassActor ca : ClassRegistry.BOOT_CLASS_REGISTRY.bootImageClasses()) {
            forceJDKSubstitutedNatives(ca.toJava());
        }
    }

    private void forceJDKSubstitutedNatives(Class<?> klass) {
        for (Method m : klass.getDeclaredMethods()) {
            if (Modifier.isNative(m.getModifiers())) {
                ClassMethodActor methodActor = ClassMethodActor.fromJava(m);
                ClassMethodActor substitute = METHOD_SUBSTITUTIONS.Static.findSubstituteFor(methodActor);
                if (substitute != null) {
                    CompiledPrototype.registerVMEntryPoint(methodActor);
                }
            }
        }
    }

    /**
     * Since we are not yet compiling the boot image with Graal, a lot of node types are not being included in the
     * image, which causes problems when trying to compile with Graal at runtime early in the bootstrap, so we force
     * them to be included here.
     */
    private static class GraalBoot {

        @SuppressWarnings("unchecked")
        static void forceValues() {
            for (BootImagePackage p : VMConfiguration.activeConfig().bootImagePackages) {
                if (p.name().contains("com.oracle.graal.nodes") || p.name().contains("com.oracle.max.vm.ext.graal.nodes") || p.name().contains("com.oracle.graal.lir")) {
                    String[] classes = p.listClasses(JavaPrototype.javaPrototype().packageLoader().classpath);
                    for (String className : classes) {
                        try {
                            Class< ? > klass = Class.forName(className);
                            if (Node.class.isAssignableFrom(klass)) {
                                NodeClass.get(klass);
                            } else if (LIRInstruction.class.isAssignableFrom(klass) && klass != LIRInstruction.class) {
                                LIRInstructionClass.get((Class< ? extends LIRInstruction>) klass);
                            } else if (CompositeValue.class.isAssignableFrom(klass)) {
                                CompositeValueClass.get((Class< ? extends CompositeValue>) klass);
                            }
                        } catch (Exception ex) {
                            throw ProgramError.unexpected();
                        }
                    }
                }
            }
        }

    }

}
