/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import com.oracle.max.asm.*;
import com.oracle.max.cri.intrinsics.*;
import com.oracle.max.cri.intrinsics.IntrinsicImpl.Registry;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.ext.*;
import com.oracle.max.graal.compiler.graphbuilder.*;
import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.compiler.phases.PhasePlan.PhasePosition;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.graph.NodeClass.CalcOffset;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.snippets.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.CiCompiler.DebugInfoLevel;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;
import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.deps.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.thread.*;

/**
 * Integration of the Graal compiler into Maxine's compilation framework.
 */
public class Graal implements RuntimeCompiler {

    /**
     * The Maxine specific implementation of the {@linkplain RiRuntime runtime interface} needed by Graal.
     */
    public final MaxRuntime runtime = MaxRuntime.getInstance();

    public final ExtendedBytecodeHandler extendedBytecodeHandler = new ExtendedBytecodeHandler() {
        @Override
        public boolean handle(int opcode, BytecodeStream s, StructuredGraph graph, FrameStateBuilder frameState, GraphBuilderTool graphBuilderTool) {
            if (opcode == Bytecodes.JNICALL) {
                // TODO(tw): Add code for JNI calls. int cpi = s.readCPI();
            }
            return false;
        }
    };

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
            runtime.setIntrinsicRegistry(intrinsicRegistry);

            GraalContext context = new GraalContext("Virtual Machine Compiler");
            compiler = new GraalCompiler(context, runtime, target, xirGenerator, vm().registerConfigs.compilerStub, extendedBytecodeHandler);
            plan = new PhasePlan();
            plan.addPhase(PhasePosition.AFTER_PARSING, new FoldPhase(runtime));
            plan.addPhase(PhasePosition.AFTER_PARSING, new MaxineIntrinsicsPhase(runtime));
            plan.addPhase(PhasePosition.AFTER_PARSING, new MustInlinePhase(runtime, cache, null));

            // Run forced inlining again because high-level optimizations (such as replacing a final field read by a constant) can open up new opportunities.
            plan.addPhase(PhasePosition.HIGH_LEVEL, new FoldPhase(runtime));
            plan.addPhase(PhasePosition.HIGH_LEVEL, new MaxineIntrinsicsPhase(runtime));
            plan.addPhase(PhasePosition.HIGH_LEVEL, new MustInlinePhase(runtime, cache, null));

            plan.addPhase(PhasePosition.HIGH_LEVEL, new IntrinsificationPhase(runtime));
            plan.addPhase(PhasePosition.HIGH_LEVEL, new WordTypeRewriterPhase());

            GraalIntrinsics.installIntrinsics(runtime, target, plan);
        }

        if (isHosted() && phase == Phase.SERIALIZING_IMAGE) {
            NodeClass.rescanAllFieldOffsets(new CalcOffset() {
                @Override
                public long getOffset(Field field) {
                    return FieldActor.fromJava(field).offset();
                }
            });
        }

        if (phase == Phase.TERMINATING) {
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

    public final TargetMethod compile(final ClassMethodActor method, boolean install, CiStatistics stats) {
        CiTargetMethod compiledMethod;
        do {
            if (method.compilee().isNative()) {
                NativeStubCompiler nsc = new NativeStubCompiler(compiler(), method);
                compiledMethod = nsc.compile(plan);
            } else {
                compiledMethod = compiler().compileMethod(method, -1, stats, DebugInfoLevel.FULL, plan).targetMethod();
            }

            Dependencies deps = DependenciesManager.validateDependencies(compiledMethod.assumptions());
            if (deps != Dependencies.INVALID) {
                if (GraalOptions.Time) {
                    compiler().context.timers.startScope("Install Target Method");
                }
                MaxTargetMethod maxTargetMethod = new MaxTargetMethod(method, compiledMethod, install);
                if (GraalOptions.Time) {
                    compiler().context.timers.endScope();
                }
                if (deps != null) {
                    DependenciesManager.registerValidatedTarget(deps, maxTargetMethod);
                    if (DependenciesManager.TraceDeps) {
                        Log.println("DEPS: " + deps.toString(true));
                    }
                }
                return maxTargetMethod;

            }
            // Loop back and recompile.
        } while(true);
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
