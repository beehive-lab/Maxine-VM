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
package com.oracle.max.vm.ext.c1xgraal;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;

import java.lang.reflect.*;

import com.oracle.max.asm.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.graph.NodeClass.CalcOffset;
import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.maxri.*;
import com.oracle.max.vm.ext.maxri.MaxXirGenerator.RuntimeCalls;
import com.sun.c1x.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.observer.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;
import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.deps.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * Integration of the C1X + Graal compiler into Maxine's compilation framework.
 */
public class C1XGraal implements RuntimeCompiler {

    /**
     * The Maxine specific implementation of the {@linkplain RiRuntime runtime interface} needed by C1X.
     */
    public final MaxRuntime runtime = MaxRuntime.getInstance();

    /**
     * The {@linkplain CiTarget target} environment derived from a Maxine {@linkplain Platform platform} description.
     */
    public final CiTarget target;

    /**
     * The Maxine specific implementation of the {@linkplain RiXirGenerator interface} used by C1X
     * to incorporate runtime specific details when translating bytecode methods.
     */
    public final RiXirGenerator xirGenerator;

    /**
     * The Graal compiler instance configured for the Maxine runtime.
     */
    private GraalCompiler graalCompiler;

    /**
     * The C1X compiler instance configured for the Maxine runtime.
     */
    private C1XCompiler c1xCompiler;

    /**
     * Set to true once the C1X options are set (to allow subclasses of this scheme to coexist in the same image).
     */
    @HOSTED_ONLY
    public static boolean optionsRegistered;

    @HOSTED_ONLY
    public C1XGraal() {
        this(new MaxXirGenerator(GraalOptions.PrintXirTemplates), platform().target);
    }

    @HOSTED_ONLY
    protected C1XGraal(RiXirGenerator xirGenerator, CiTarget target) {
        this.xirGenerator = xirGenerator;
        this.target = target;
    }

    @Override
    public void initialize(Phase phase) {
        if (isHosted() && !optionsRegistered) {
            GraalOptions.StackShadowPages = VmThread.STACK_SHADOW_PAGES;
            GraalOptions.CallSiteUniquePC = true;
            C1XOptions.OptIntrinsify = false; // TODO (ds): remove once intrinisification works for Maxine
            VMOptions.addFieldOptions("-G:", GraalOptions.class, null);
            VMOptions.addFieldOptions("-C1X:", C1XOptions.class, null);
            VMOptions.addFieldOptions("-ASM:", AsmOptions.class, null);

            // Boot image code may not be safely deoptimizable due to metacircular issues
            // so only enable speculative optimizations at runtime
            C1XOptions.UseAssumptions = false;

            optionsRegistered = true;
        }

        if (isHosted() && phase == Phase.HOSTED_COMPILING) {
            // Temporary work-around to support the @ACCESSOR annotation.
            GraphBuilder.setAccessor(ClassActor.fromJava(Accessor.class));

            GraalContext context = new GraalContext("Virtual Machine Compiler");
            graalCompiler = new GraalCompiler(context, runtime, target, xirGenerator, vm().registerConfigs.compilerStub);

            c1xCompiler = new C1XCompiler(runtime, target, xirGenerator, vm().registerConfigs.compilerStub);
            c1xCompiler.addCompilationObserver(new WordTypeRewriterPhase());
            MaxineIntrinsicImplementations.initialize(c1xCompiler.intrinsicRegistry);

            // search for the runtime call and register critical methods
            for (Method m : RuntimeCalls.class.getDeclaredMethods()) {
                int flags = m.getModifiers();
                if (Modifier.isStatic(flags) && Modifier.isPublic(flags)) {
                    new CriticalMethod(RuntimeCalls.class, m.getName(), SignatureDescriptor.create(m.getReturnType(), m.getParameterTypes()));
                }
            }

            // The direct call made from compiled code for the UNCOMMON_TRAP intrinisic
            // must go through a stub that saves the register state before calling the deopt routine.
            CriticalMethod uncommonTrap = new CriticalMethod(MaxRuntimeCalls.class, "uncommonTrap", null);
            uncommonTrap.classMethodActor.compiledState = new Compilations(null, vm().stubs.genUncommonTrapStub());
        }

        if (isHosted() && phase == Phase.SERIALIZING_IMAGE) {
            NodeClass.rescanAllFieldOffsets(new CalcOffset() {
                @Override
                public int getOffset(Field field) {
                    return FieldActor.fromJava(field).offset();
                }
            });
        }

        if (phase == Phase.STARTING) {
            // Now it is safe to use speculative opts
            C1XOptions.UseAssumptions = vm().compilationBroker.isDeoptSupported() && Deoptimization.UseDeopt;
        } else if (phase == Phase.TERMINATING) {
            if (C1XOptions.PrintMetrics) {
                C1XMetrics.print();
                DebugInfo.dumpStats(Log.out);
            }
            if (C1XOptions.PrintTimers) {
                C1XTimers.print();
            }
            if (GraalOptions.Meter) {
                graalCompiler.context.metrics.print();
            }
            if (GraalOptions.Time) {
                graalCompiler.context.timers.print();
            }
        }
    }

    private static class WordTypeRewriterPhase implements CompilationObserver {
        @Override
        public void compilationEvent(CompilationEvent event) {
            if (event.getLabel() == CompilationEvent.AFTER_PARSING) {
                new WordTypeRewriter().apply(event.getCompilation());
            }
        }

        @Override
        public void compilationStarted(CompilationEvent event) { }
        @Override
        public void compilationFinished(CompilationEvent event) { }
    }

    public GraalCompiler graalCompiler() {
        if (isHosted() && graalCompiler == null) {
            initialize(Phase.HOSTED_COMPILING);
        }
        return graalCompiler;
    }

    public C1XCompiler c1xCompiler() {
        if (isHosted() && c1xCompiler == null) {
            initialize(Phase.HOSTED_COMPILING);
        }
        return c1xCompiler;
    }

    public final TargetMethod compile(final ClassMethodActor method, boolean install, CiStatistics stats) {
        CiTargetMethod compiledMethod;
        do {
            String name = vm().compilationBroker.compilerFor(method);
            if (forceC1X(method) || (name != null && name.equals("C1X"))) {
                compiledMethod = c1xCompiler().compileMethod(method, -1, stats).targetMethod();
            } else {
                compiledMethod = graalCompiler().compileMethod(method, -1, stats).targetMethod();
            }

            Dependencies deps = DependenciesManager.validateDependencies(compiledMethod.assumptions());
            if (deps != Dependencies.INVALID) {
                if (GraalOptions.Time) {
                    graalCompiler().context.timers.startScope("Install Target Method");
                }
                MaxTargetMethod maxTargetMethod = new MaxTargetMethod(method, compiledMethod, install);
                if (GraalOptions.Time) {
                    graalCompiler().context.timers.endScope();
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

    /**
     * Until Graal can compile everything, this is a mechanism to specify
     * what it cannot yet handle.
     */
    boolean forceC1X(final ClassMethodActor method) {
        return method.isNative() || method.isTemplate();
    }

    public Nature nature() {
        return Nature.OPT;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public boolean matches(String compilerName) {
        return compilerName.equals("C1X") || compilerName.equals("Graal");
    }

    /**
     * Factory method for getting a Graal compiler instance. This method is called via reflection.
     */
    public static GraalCompiler getGraalCompiler() {
        return ((C1XGraal) vm().compilationBroker.optimizingCompiler).graalCompiler();
    }
}
