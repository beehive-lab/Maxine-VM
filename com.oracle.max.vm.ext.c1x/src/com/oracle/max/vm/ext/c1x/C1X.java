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
package com.oracle.max.vm.ext.c1x;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;

import java.io.*;
import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.criutils.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.c1x.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.observer.*;
import com.sun.cri.ci.CiCompiler.DebugInfoLevel;
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

/**
 * Integration of the C1X compiler into Maxine's compilation framework.
 */
public class C1X implements RuntimeCompiler {

    /**
     * The Maxine specific implementation of the {@linkplain RiRuntime runtime interface} needed by C1X.
     */
    public final MaxRuntime runtime = MaxRuntime.runtime();

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
     * The C1X compiler instance configured for the Maxine runtime.
     */
    private C1XCompiler compiler;

    /**
     * Set to true once the C1X options are set (to allow subclasses of this scheme to coexist in the same image).
     */
    @HOSTED_ONLY
    public static boolean optionsRegistered;

    private static final int DEFAULT_OPT_LEVEL = Integer.getInteger("max.c1x.optlevel", 3);

    public static final VMIntOption optLevelOption = VMOptions.register(new VMIntOption("-C1X:OptLevel=", DEFAULT_OPT_LEVEL,
        "Set the optimization level of C1X.") {
            @Override
            public boolean parseValue(com.sun.max.unsafe.Pointer optionValue) {
                boolean result = super.parseValue(optionValue);
                if (result) {
                    C1XOptions.setOptimizationLevel(getValue());
                    return true;
                }
                return false;
            }
        }, MaxineVM.Phase.STARTING);

    /**
     * A map from option field names to some text describing the meaning and
     * usage of the corresponding C1X option.
     */
    private static Map<String, String> helpMap;

    public static Map<String, String> getHelpMap() {
        if (helpMap == null) {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("PrintFilter",
                    "Filter compiler tracing to methods whose fully qualified name " +
                    "matches <arg>. If <arg> starts with \"~\", then <arg> (without " +
                    "the \"~\") is interpreted as a regular expression. Otherwise, " +
                    "<arg> is interpreted as a simple substring.");

            map.put("TraceBytecodeParserLevel",
                    "Trace frontend bytecode parser at level <n> where 0 means no " +
                    "tracing, 1 means instruction tracing and 2 means instruction " +
                    "plus frame state tracing.");

            map.put("DetailedAsserts",
                    "Turn on detailed error checking that has a noticeable performance impact.");

            map.put("GenSpecialDivChecks",
                    "Generate code to check for (Integer.MIN_VALUE / -1) or (Long.MIN_VALUE / -1) " +
                    "instead of detecting these cases via instruction decoding in a trap handler.");

            map.put("UseStackMapTableLiveness",
                    "Use liveness information derived from StackMapTable class file attribute.");

            for (String name : map.keySet()) {
                try {
                    C1XOptions.class.getField(name);
                } catch (Exception e) {
                    throw new InternalError("The name '" + name + "' does not denote a field in " + C1XOptions.class);
                }
            }
            helpMap = Collections.unmodifiableMap(map);
        }
        return helpMap;
    }

    @HOSTED_ONLY
    public C1X() {
        this(new MaxXirGenerator(C1XOptions.PrintXirTemplates), platform().target);
    }

    @HOSTED_ONLY
    public C1X(RiXirGenerator xirGenerator, CiTarget target) {
        this.xirGenerator = xirGenerator;
        this.target = target;
    }

    @Override
    public void initialize(Phase phase) {
        if (isHosted() && !optionsRegistered) {
            runtime.initialize();

            C1XOptions.setOptimizationLevel(optLevelOption.getValue());
            C1XOptions.OptIntrinsify = false; // TODO (ds): remove once intrinisification works for Maxine
            C1XOptions.StackShadowPages = VmThread.STACK_SHADOW_PAGES;
            VMOptions.addFieldOptions("-C1X:", C1XOptions.class, getHelpMap());
            VMOptions.addFieldOptions("-ASM:", AsmOptions.class, null);

            // Speculative opts (UseAssumptions) are the default in the boot image as they are limited
            // to VM classes, which form a closed world.
            optionsRegistered = true;
        }

        if (isHosted() && phase == Phase.HOSTED_COMPILING) {
            // Temporary work-around to support the @ACCESSOR annotation.
            GraphBuilder.setAccessor(ClassActor.fromJava(Accessor.class));

            compiler = new C1XCompiler(runtime, target, xirGenerator, vm().registerConfigs.compilerStub);
            compiler.addCompilationObserver(new WordTypeRewriterObserver());
            MaxineIntrinsicImplementations.initialize(compiler.intrinsicRegistry);
        }

        if (phase == Phase.STARTING) {
            // Speculative opts are ok provided the compilation broker can handle deopt
            C1XOptions.UseAssumptions = vm().compilationBroker.isDeoptSupported() && Deoptimization.UseDeopt;
        } else if (phase == Phase.TERMINATING) {
            if (C1XOptions.PrintMetrics) {
                C1XMetrics.print();
                DebugInfo.dumpStats(Log.out);
            }
            if (C1XOptions.PrintTimers) {
                C1XTimers.print();
            }
        }
    }

    private static class WordTypeRewriterObserver implements CompilationObserver {
        @Override
        public void compilationEvent(CompilationEvent event) {
            if (event.getLabel() == CompilationEvent.AFTER_PARSING) {
                new WordTypeRewriter().apply(event.getCompilation());
            } else if (event.getLabel() == CompilationEvent.AFTER_REGISTER_ALLOCATION && ((MethodActor) event.getCompilation().method).isTemplate()) {
                processTemplate(event.getCompilation());
            }
        }

        @Override
        public void compilationStarted(CompilationEvent event) { }
        @Override
        public void compilationFinished(CompilationEvent event) { }
    }

    /**
     * T1X templates are compiled in a special way without a return instruction. The control flow just falls through to the
     * next template.  Therefore, we have to make sure that 1) the template has only one exit point, and 2) that this exit point
     * is the last block.
     * Condition 1) can be ensured by careful coding of the templates, i.e., having no return statement in the middle of the
     * template. If one of the fatal errors below triggers, this condition is violated.
     * Condition 2) is guaranteed by this code: we move the return block to the end of the block list.
     *
     * Note that we currently do not check that templates do not have stubs (which would be at the end of the method).
     */
    private static void processTemplate(C1XCompilation compilation) {
        List<BlockBegin> code = compilation.hir().linearScanOrder();
        BlockBegin returnBlock = null;
        for (int i = code.size() - 1; i >= 0; i--) {
            BlockBegin block = code.get(i);

            LIROpcode lastOp = block.lir().at(block.lir().length() - 1).code;
            if (block.numberOfSux() == 0 && lastOp != LIROpcode.Unwind) {
                if (returnBlock != null) {
                    FatalError.unexpected("Template has more than one return instruction");
                }
                returnBlock = block;
                code.remove(i);
            }
        }
        if (returnBlock == null) {
            FatalError.unexpected("Template has no return instruction");
        }
        code.add(returnBlock);
    }


    public C1XCompiler compiler() {
        if (isHosted() && compiler == null) {
            FatalError.unexpected("xxxx");
            initialize(Phase.HOSTED_COMPILING);
        }
        return compiler;
    }

    public TargetMethod compile(final ClassMethodActor method, boolean isDeopt, boolean install, CiStatistics stats) {
        CiTargetMethod compiledMethod;
        do {
            DebugInfoLevel debugInfoLevel = method.isTemplate() ? DebugInfoLevel.REF_MAPS : DebugInfoLevel.FULL;
            compiledMethod = compiler().compileMethod(method, -1, stats, debugInfoLevel).targetMethod();
            Dependencies deps = Dependencies.validateDependencies(compiledMethod.assumptions());
            if (deps != Dependencies.INVALID) {
                if (C1XOptions.PrintTimers) {
                    C1XTimers.INSTALL.start();
                }
                MaxTargetMethod maxTargetMethod = new MaxTargetMethod(method, compiledMethod, install);
                if (C1XOptions.PrintTimers) {
                    C1XTimers.INSTALL.stop();
                }
                if (deps != null) {
                    Dependencies.registerValidatedTarget(deps, maxTargetMethod);
                }
                TTY.Filter filter = new TTY.Filter(C1XOptions.PrintFilter, method);
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

    void printMachineCode(CiTargetMethod ciTM, MaxTargetMethod maxTM, boolean reentrant) {
        if (!C1XOptions.PrintCFGToFile || reentrant || TTY.isSuppressed()) {
            return;
        }
        if (!isHosted() && !isRunning()) {
            // Cannot write to file system at runtime until the VM is in the RUNNING phase
            return;
        }

        ByteArrayOutputStream cfgPrinterBuffer = new ByteArrayOutputStream();
        CFGPrinter cfgPrinter = new CFGPrinter(cfgPrinterBuffer, target());

        cfgPrinter.printMachineCode(runtime.disassemble(ciTM, maxTM), "After code installation");
        cfgPrinter.flush();
        OutputStream cfgFileStream = CompilationPrinter.globalOut();
        if (cfgFileStream != null) {
            synchronized (cfgFileStream) {
                try {
                    cfgFileStream.write(cfgPrinterBuffer.toByteArray());
                } catch (IOException e) {
                    TTY.println("WARNING: Error writing CFGPrinter output for %s to disk: %s", maxTM.classMethodActor, e.getMessage());
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
        return compilerName.equals("C1X");
    }
}
