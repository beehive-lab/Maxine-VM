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
import com.oracle.graal.compiler.*;
import com.oracle.graal.compiler.target.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.debug.internal.*;
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
import com.oracle.graal.virtual.phases.ea.*;
import com.oracle.max.vm.ext.graal.amd64.*;
import com.oracle.max.vm.ext.graal.phases.*;
import com.oracle.max.vm.ext.graal.snippets.*;
import com.oracle.max.vm.ext.graal.stubs.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.config.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.deps.*;
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
        addFieldOption("-XX:", "GraalForBoot", MaxGraal.class, "use Graal for boot image");
        addFieldOption("-XX:", "GraalForNative", MaxGraal.class, "compile native code with Graal");
    }

    public static boolean GraalForBoot;
    /**
     * Temporary option to enable/disable native method compilation by Graal.
     */
    public static boolean GraalForNative = true;


    private static final String NAME = "Graal";

    // GraalCompiler doesn't provide an instance that captures all this additional state needed
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
     * Standard configuration used for non-boot image compilation.
     * To honor runtime Graal options, which can affect which phases are run, these may have to be recreated at runtime.
     */
    private Suites defaultSuites;
    /**
     * Customized suites for compiling the boot image with Graal.
     */
    @HOSTED_ONLY
    private Suites bootSuites;

    /**
     * To propagate to multiple compiler threads in boot image generation.
     */
    @HOSTED_ONLY
    private DebugConfig hostedDebugConfig;

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
        boolean bootCompile;
        boolean archWordKind; // only set when bootCompile==true
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

    private static class WordKindRewritten extends com.oracle.graal.phases.Phase {

        @Override
        protected void run(StructuredGraph graph) {
            // Checkstyle: stop
            stateThreadLocal.get().archWordKind = true;
            // Checkstyle: resume
        }
    }

    public static boolean archWordKind() {
        return stateThreadLocal.get().archWordKind;
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
            forceJDKSubstitutedMethods();
            CompiledPrototype.registerNeedsCompilationCallback(new NoSnippetCompilation());
        } else if (phase == MaxineVM.Phase.SERIALIZING_IMAGE) {
            MaxGraalOptions.initialize(phase);
            TraceNodeClasses.scan();
            FieldIntrospection.rescanAllFieldOffsets(new TraceDefaultAndSetMaxineFieldOffset());
            // The above call causes a crash when using toString/hashCode on Graal node instances,
            // due to the field offsets having been rewritten.
            BootImageGenerator.treeStringOption.setValue(false);
        } else if (phase == MaxineVM.Phase.RUNNING) {
            MaxResolvedJavaType.init(phase);
            cache = new MaxGraphCache();
            // Compilations can occur after this, so set up the debug environment and check options.
            if (MaxGraalOptions.isPresent("Dump") != null && GraalForNative) {
                dumpWorkAround();
            }
            // Now we can actually set the Graal options and initialize the Debug environment
            if (MaxGraalOptions.initialize(phase)) {
                defaultSuites = createDefaultSuites();
            }
            DebugEnvironment.initialize(System.out);
        }
    }

    /**
     * Metacircularity workaround for {@code -G:Dump}.
     * There is a circularity if {@code -G:Dump} is set, as this will try to open the network printer
     * which will cause native stubs in the JDK network library to be created and compiled, which will
     * try to open the network printer (irrespective of the {@code -G:MethodFilter} setting)...
     * So we check if the option is set and, if so, open a socket to force the stub compilations before
     * initializing the DebugEnvironment.
     */
    private static void dumpWorkAround() {
        try {
            // Get either the set values or the Graal defaults
            VMStringOption hostOption = (VMStringOption) MaxGraalOptions.isPresent("PrintIdealGraphAddress");
            String host = hostOption == null ? GraalOptions.PrintIdealGraphAddress.getValue() : hostOption.getValue();
            VMIntOption portOption = (VMIntOption) MaxGraalOptions.isPresent("PrintIdealGraphPort");
            int port = portOption == null ? GraalOptions.PrintBinaryGraphPort.getValue() : portOption.getValue();
            BinaryGraphPrinter printer = new BinaryGraphPrinter(SocketChannel.open(new InetSocketAddress(host, port)));
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
        DebugEnvironment.initialize(Trace.stream());
        hostedDebugConfig = DebugScope.getConfig();
        MaxTargetDescription td = new MaxTargetDescription();
        runtime = new MaxRuntime(td);
        backend = new MaxAMD64Backend(runtime, td);
        // We want exception handlers generated
        optimisticOpts = OptimisticOptimizations.ALL.remove(Optimization.UseExceptionProbability, Optimization.UseExceptionProbabilityForOperations);
        cache = new MaxGraphCache();
        // Prevent DeoptimizeNodes since we don't handle them yet
        FixedGuardNode.suppressDeoptimize();
        defaultSuites = createDefaultSuites();
        bootSuites = createBootSuites();
        // For snippet creation we need bootCompile==true for inlining of VM methods
        state.bootCompile = true;
        MaxResolvedJavaType.init(phase);
        replacements = MaxSnippets.initialize(runtime);
        addCustomSnippets(replacements);
        GraalBoot.forceValues();
    }

    /**
     * MaxGraal's default suites which, for now, have some small modifications.
     */
    private Suites createDefaultSuites() {
        Suites suites = Suites.createDefaultSuites();
        PhaseSuite<HighTierContext> highTier = suites.getHighTier();
        // Replace the normal InliningPhase
        ListIterator<BasePhase<? super HighTierContext>> highIter = highTier.findPhase(InliningPhase.class);
        highIter.remove();
        highIter.add(new MaxInliningPhase());
        // TailDuplicationcauses a problem with native methods because the NativeFunctionCallNode gets duplicated
        // from its initial state as the template method. Disabling it completely is overkill but simple.
        highTier.findPhase(TailDuplicationPhase.class).remove();
        // causes problems in Maxine DebugInfo (doesn't understand virtualized objects)
        highTier.findPhase(PartialEscapePhase.class).remove();
        return suites;
    }

    /**
     * Create the custom suites for boot image compilation.
     * We could do this with a custom {@link CompilerConfiguration} but that involves jar files
     * using the {@link ServiceLoader} which doesn't fit the Maxine build model well.
     */
    @HOSTED_ONLY
    private Suites createBootSuites() {
        Suites suites = createDefaultSuites();
        PhaseSuite<HighTierContext> highTier = suites.getHighTier();
        // Replace the normal InliningPhase
        ListIterator<BasePhase<? super HighTierContext>> highIter = highTier.findPhase(InliningPhase.class);
        highIter.remove();
        highIter.add(new MaxHostedInliningPhase());
        highIter = highTier.findPhase(CleanTypeProfileProxyPhase.class);
        // Add the Maxine specific phases that used to run in the old HIGH_LEVEL PhasePosition
        highIter.add(new MaxWordType.MaxNullCheckRewriterPhase());
        highIter.add(new MaxWordType.ReplaceAccessorPhase());
        highIter.add(new MaxIntrinsicsPhase());
        highIter.add(new MaxWordType.KindRewriterPhase());
        highIter.add(new WordKindRewritten());
        highIter.add(new CanonicalizerPhase(canonicalizeReads));

        PhaseSuite<MidTierContext> midTier = suites.getMidTier();
        midTier.appendPhase(new MaxWordType.KindRewriterPhase());
        return suites;
    }

    @HOSTED_ONLY
    public List<com.oracle.graal.phases.Phase> addBootPhases(PhasePlan phasePlan) {
        ArrayList<com.oracle.graal.phases.Phase> result = new ArrayList<>();
        result.add(addPhase(phasePlan, PhasePosition.AFTER_PARSING, new MaxWordType.CheckWordObjectEqualsPhase()));
        // In Graal, @Fold is purely a snippet notion; we handle Maxine's more general use with similar code.
        // We do this early to reduce the size of a graph that contains isHosted code.
        result.add(addPhase(phasePlan, PhasePosition.AFTER_PARSING, new MaxFoldPhase(runtime)));
        // Any folded isHosted code is only removed by a CanonicalizationPhase
        result.add(addPhase(phasePlan, PhasePosition.AFTER_PARSING, new CanonicalizerPhase.Instance(runtime,
                        new Assumptions(GraalOptions.OptAssumptions.getValue()), canonicalizeReads)));
        result.add(addPhase(phasePlan, PhasePosition.AFTER_PARSING, new MaxIntrinsicsPhase()));
        result.add(addPhase(phasePlan, PhasePosition.AFTER_PARSING,
                        new MaxWordType.MakeWordFinalRewriterPhase()));
        return result;
    }

    private com.oracle.graal.phases.Phase addPhase(PhasePlan phasePlan, PhasePosition pos, com.oracle.graal.phases.Phase phase) {
        phasePlan.addPhase(pos, phase);
        return phase;
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
        state.archWordKind = false;
        state.bootCompile = false;
        Suites compileSuites;
        if (MaxineVM.isHosted() && (GraalForBoot || methodActor.toString().startsWith("jtt.max"))) {
            // The (temporary) check for GraalForBoot prevents test compilations being treated as boot image code.
            compileSuites = bootSuites;
            state.bootCompile = true;
            state.methodActor = methodActor;
        } else {
            // all SUBSTITUTEd methods should have been compiled into the boot image
            assert !substitutedMethod(methodActor);
            compileSuites = defaultSuites;
        }

        StructuredGraph graph = (StructuredGraph) method.getCompilerStorage().get(Graph.class);
        if (graph == null) {
            GraphBuilderPhase graphBuilderPhase = new MaxGraphBuilderPhase(runtime, GraphBuilderConfiguration.getDefault(), optimisticOpts);
            phasePlan.addPhase(PhasePosition.AFTER_PARSING, graphBuilderPhase);
            addCustomPhase(methodActor, phasePlan);
            // Hosted (boot image) compilation is more complex owing to the need to handle the unsafe features of VM programming,
            // requiring more phases to be run, all very similar to what happens in MaxReplacementsImpl.
            if (MaxineVM.isHosted() && state.bootCompile) {
                // TODO should be done by modifying bootSuites
                addBootPhases(phasePlan);
                // need to propagate the Debug configuration if this is a new compiler thread
                DebugConfig threadDebugConfig = DebugScope.getConfig();
                if (threadDebugConfig == null) {
                    DebugScope.getInstance().setConfig(hostedDebugConfig);
                }
            }
            if (methodActor.compilee().isNative()) {
                graph = new NativeStubGraphBuilder(methodActor).build();
            } else {
                graph = new StructuredGraph(method);
            }
        }
        CallingConvention cc = CodeUtil.getCallingConvention(runtime, CallingConvention.Type.JavaCallee, method, false);
        do {
            CompilationResult result = GraalCompiler.compileGraph(graph, cc, method, runtime, replacements, backend, runtime.maxTargetDescription(), cache, phasePlan, optimisticOpts,
                            new SpeculationLog(), compileSuites, new CompilationResult());
            Dependencies deps = Dependencies.validateDependencies(MaxAssumptions.toCiAssumptions(methodActor, result.getAssumptions()));
            if (deps != Dependencies.INVALID) {
                MaxTargetMethod maxTargetMethod = GraalMaxTargetMethod.create(methodActor, MaxCiTargetMethod.create(result), true);
                if (deps != null) {
                    Dependencies.registerValidatedTarget(deps, maxTargetMethod);
                }
                return maxTargetMethod;
            }
            // Loop back and recompile.
            graph = new StructuredGraph(method);
        } while (true);
    }

    private static boolean substitutedMethod(ClassMethodActor methodActor) {
        return methodActor.compilee() != methodActor;
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


    @Override
    public Nature nature() {
        return Nature.OPT;
    }

    @Override
    public boolean matches(String compilerName) {
        return compilerName.equals(NAME);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
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
     * With Graal it is not currently possible to compile code at runtime that uses Maxine annotations,
     * as the special boot compilations are not enabled (they could be at additional compile time cost).
     * Not all the substituted methods in the JDK are compiled into the boot image because they
     * are not used by boot image code (e.g. several in sun.misc.Unsafe). So we force them in here.
     * (C1X's inlining is such that it "gets away" with not interpreting the Maxine annotations.)
     *
     */
    private void forceJDKSubstitutedMethods() {
        for (ClassActor ca : ClassRegistry.BOOT_CLASS_REGISTRY.bootImageClasses()) {
            forceJDKSubstitutedMethods(ca.toJava());
        }
    }

    private void forceJDKSubstitutedMethods(Class< ? > klass) {
        for (Method m : klass.getDeclaredMethods()) {
            MethodActor methodActor = MethodActor.fromJava(m);
            if (methodActor instanceof ClassMethodActor) {
                ClassMethodActor classMethodActor = (ClassMethodActor) methodActor;
                ClassMethodActor substitute = METHOD_SUBSTITUTIONS.Static.findSubstituteFor(classMethodActor);
                if (substitute != null) {
                    CompiledPrototype.registerVMEntryPoint(classMethodActor);
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
