/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import com.oracle.max.asm.*;
import com.oracle.max.cri.intrinsics.*;
import com.oracle.max.cri.intrinsics.IntrinsicImpl.Registry;
import com.oracle.max.criutils.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.debug.*;
import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.compiler.phases.PhasePlan.PhasePosition;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.graph.NodeClass.CalcOffset;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.snippets.*;
import com.oracle.max.vm.ext.graal.stubs.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ci.CiCompiler.DebugInfoLevel;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;
import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.deps.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * Integration of the Graal compiler into Maxine's compilation framework.
 */
public class Graal implements RuntimeCompiler {

    /**
     * The Maxine specific implementation of the {@linkplain RiRuntime runtime interface} needed by Graal.
     */
    public final MaxRuntime runtime = MaxRuntime.runtime();

    /**
     * The {@linkplain CiTarget target} environment derived from a Maxine {@linkplain Platform platform} description.
     */
    public final CiTarget target;

    /**
     * The Maxine specific implementation of the {@linkplain RiXirGenerator interface} used by Graal
     * to incorporate runtime specific details when translating bytecode methods.
     */
    public final RiXirGenerator xirGenerator;

    /**
     * The Graal compiler instance configured for the Maxine runtime.
     */
    private GraalCompiler compiler;

    private PhasePlan plan;

    private static final int DEFAULT_OPT_LEVEL = 3;

    @HOSTED_ONLY
    public Graal() {
        this(new MaxXirGenerator(GraalOptions.PrintXirTemplates), platform().target);
    }

    @HOSTED_ONLY
    public Graal(RiXirGenerator xirGenerator, CiTarget target) {
        this.xirGenerator = xirGenerator;
        this.target = target;
    }

    @HOSTED_ONLY
    public static boolean optionsRegistered;

    @HOSTED_ONLY
    public static Map<RiMethod, StructuredGraph> cache = new ConcurrentHashMap<RiMethod, StructuredGraph>();

    @Override
    public void initialize(Phase phase) {
        // TODO(ls) implementation of RiType.fields required to enable escape analysis
        GraalOptions.EscapeAnalysis = false;

        if (isHosted() && !optionsRegistered) {
            runtime.initialize();

            GraalOptions.StackShadowPages = VmThread.STACK_SHADOW_PAGES;
            VMOptions.addFieldOptions("-G:", GraalOptions.class, null);
            VMOptions.addFieldOptions("-ASM:", AsmOptions.class, null);

            optionsRegistered = true;
        }

        if (isHosted() && phase == Phase.HOSTED_COMPILING) {
            Registry intrinsicRegistry = new IntrinsicImpl.Registry();
            GraalMaxineIntrinsicImplementations.initialize(intrinsicRegistry);
            for (Map.Entry<String, IntrinsicImpl> e : intrinsicRegistry) {
                GraalIntrinsicImpl impl = (GraalIntrinsicImpl) e.getValue();
                if (impl.createMethod != null) {
                    CompiledPrototype.registerImageInvocationStub(MethodActor.fromJava(impl.createMethod));
                }
            }
            runtime.setIntrinsicRegistry(intrinsicRegistry);

            GraalContext context = new GraalContext("Virtual Machine Compiler");
            compiler = new GraalCompiler(context, runtime, target, xirGenerator, vm().registerConfigs.compilerStub);
            plan = new PhasePlan();
            plan.addPhase(PhasePosition.AFTER_PARSING, new FoldPhase(runtime));
            plan.addPhase(PhasePosition.AFTER_PARSING, new MaxineIntrinsicsPhase());
            plan.addPhase(PhasePosition.AFTER_PARSING, new MustInlinePhase(runtime, cache, null));

            // Run forced inlining again because high-level optimizations (such as replacing a final field read by a constant) can open up new opportunities.
            plan.addPhase(PhasePosition.HIGH_LEVEL, new FoldPhase(runtime));
            plan.addPhase(PhasePosition.HIGH_LEVEL, new MaxineIntrinsicsPhase());
            plan.addPhase(PhasePosition.HIGH_LEVEL, new MustInlinePhase(runtime, cache, null));

            plan.addPhase(PhasePosition.HIGH_LEVEL, new IntrinsificationPhase(runtime));
            plan.addPhase(PhasePosition.HIGH_LEVEL, new WordTypeRewriterPhase());

            GraalIntrinsics.installIntrinsics(runtime, target, plan);
            NativeStubGraphBuilder.initialize();

            // Ensure all the Node classes used by Maxine have their NodeClass instances in the image.
            for (ClassActor classActor : ClassRegistry.allBootImageClasses()) {
                if (Node.class.isAssignableFrom(classActor.toJava())) {
                    Class<?> nodeClass = classActor.toJava();
                    if (!Modifier.isAbstract(nodeClass.getModifiers())) {
                        NodeClass.get(nodeClass);
                    }
                }
            }
        }

        if (isHosted() && phase == Phase.SERIALIZING_IMAGE) {
            // Don't put boot time observers into the boot image
            compiler.context.observable.clear();

            NodeClass.rescanAllFieldOffsets(new CalcOffset() {
                @Override
                public long getOffset(Field field) {
                    return FieldActor.fromJava(field).offset();
                }
            });
        }
        if (phase == Phase.STARTING) {
            // This makes sure the relevant observers are (re)initialized based on the runtime options
            compiler.context.reset();
        } else if (phase == Phase.TERMINATING) {
            if (GraalOptions.Meter) {
                compiler.context.metrics.print();
                DebugInfo.dumpStats(Log.out);
            }
            if (GraalOptions.Time) {
                compiler.context.timers.print();
            }
        }
    }

    public GraalCompiler compiler() {
        if (isHosted() && compiler == null) {
            initialize(Phase.HOSTED_COMPILING);
        }
        return compiler;
    }

    public final TargetMethod compile(final ClassMethodActor method, boolean isDeopt, boolean install, CiStatistics stats) {
        CiTargetMethod compiledMethod;
        do {
            if (method.compilee().isNative()) {
                compiledMethod = compileNativeMethod(method);
            } else {
                compiledMethod = compiler().compileMethod(method, -1, stats, DebugInfoLevel.FULL, plan);
            }

            Dependencies deps = Dependencies.validateDependencies(compiledMethod.assumptions());
            if (deps != Dependencies.INVALID) {
                if (GraalOptions.Time) {
                    compiler().context.timers.startScope("Install Target Method");
                }
                MaxTargetMethod maxTargetMethod = new MaxTargetMethod(method, compiledMethod, install);
                if (GraalOptions.Time) {
                    compiler().context.timers.endScope();
                }
                if (deps != null) {
                    Dependencies.registerValidatedTarget(deps, maxTargetMethod);
                }
                TTY.Filter filter = new TTY.Filter(GraalOptions.PrintFilter, method);
                try {
                    printMachineCode(compiledMethod, maxTargetMethod, false);
                } finally {
                    filter.remove();
                }
                return maxTargetMethod;

            }
            // Loop back and recompile.
        } while(true);
    }

    protected CiTargetMethod compileNativeMethod(final ClassMethodActor method) {
        NativeStubGraphBuilder nativeStubCompiler = new NativeStubGraphBuilder(method);
        TTY.Filter filter = new TTY.Filter(GraalOptions.PrintFilter, method);
        try {
            return compiler.compileMethod(method, nativeStubCompiler.build(), plan);
        } finally {
            filter.remove();
        }
    }

    void printMachineCode(CiTargetMethod ciTM, MaxTargetMethod maxTM, boolean reentrant) {
        if (!GraalOptions.PrintCFGToFile || reentrant || TTY.isSuppressed()) {
            return;
        }
        if (!isHosted() && !isRunning()) {
            // Cannot write to file system at runtime until the VM is in the RUNNING phase
            return;
        }

        ByteArrayOutputStream cfgPrinterBuffer = new ByteArrayOutputStream();
        CFGPrinter cfgPrinter = new CFGPrinter(cfgPrinterBuffer, null, target(), runtime);
        cfgPrinter.printMachineCode(runtime.disassemble(ciTM, maxTM), "After code installation");
        cfgPrinter.flush();

        OutputStream stream = CompilationPrinter.globalOut();
        if (stream != null) {
            synchronized (stream) {
                try {
                    stream.write(cfgPrinter.buffer.toByteArray());
                    stream.flush();
                } catch (IOException e) {
                    TTY.println("WARNING: Error writing CFGPrinter output: %s", e);
                }
            }
        }
    }

    @Override
    public Nature nature() {
        return Nature.OPT;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean matches(String compilerName) {
        return compilerName.equals("Graal");
    }
}
