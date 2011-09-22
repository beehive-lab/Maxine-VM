/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.adaptive;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.compiler.RuntimeCompiler.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.RuntimeCompiler.Nature;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.runtime.*;

/**
 * This class implements an adaptive compilation system with multiple compilers with different compilation time / code
 * quality tradeoffs. It encapsulates the necessary infrastructure for recording profiling data, selecting what and when
 * to recompile, etc.
 */
public class AdaptiveCompilationScheme extends AbstractVMScheme implements CompilationScheme {

    /**
     * The threshold at which a recompilation is triggered from the baseline compiler to the next level
     * of optimization. This is typically the number of invocations of the method.
     */
    private static int RCT = 5000;

    /**
     * A queue of pending compilations.
     */
    protected final LinkedList<Compilation> pending = new LinkedList<Compilation>();

    /**
     * The baseline compiler.
     */
    public final RuntimeCompiler baselineCompiler;

    /**
     * The optimizing compiler.
     */
    public final RuntimeCompiler optimizingCompiler;

    private static boolean opt;
    private static boolean GCOnRecompilation;
    private static boolean FailOverCompilation;
    static int PrintCodeCacheMetrics;

    static {
        addFieldOption("-X", "opt", "Select optimizing compiler whenever possible.");
        addFieldOption("-XX:", "RCT", "Set the recompilation threshold for methods. Use 0 to disable recompilation. (default: " + RCT + ").");
        addFieldOption("-XX:", "GCOnRecompilation", "Force GC before every re-compilation.");
        addFieldOption("-XX:", "FailOverCompilation", "Retry failed compilations with another compiler (if available).");
        addFieldOption("-XX:", "PrintCodeCacheMetrics", "Print code cache metrics (0 = disabled, 1 = summary, 2 = verbose).");
    }

    /**
     * The default compiler to use.
     */
    private RuntimeCompiler defaultCompiler = null;

    private static final boolean BACKGROUND_COMPILATION = false;

    public boolean needsAdapters() {
        return baselineCompiler != null;
    }

    public boolean isDeoptSupported() {
        return baselineCompiler != null;
    }

    private static final String OPTIMIZING_COMPILER_PROPERTY = AdaptiveCompilationScheme.class.getSimpleName() + "." + optimizingCompilerOption.getName();
    private static final String BASELINE_COMPILER_PROPERTY = AdaptiveCompilationScheme.class.getSimpleName() + "." + baselineCompilerOption.getName();

    /**
     * Gets the class name of the optimizing compiler that will be configured when an instance of this scheme is instantiated.
     */
    @HOSTED_ONLY
    public static String optName() {
        return configValue(OPTIMIZING_COMPILER_PROPERTY, optimizingCompilerOption, aliases);
    }

    /**
     * Gets the class name of the baseline compiler that will be configured when an instance of this scheme is instantiated.
     */
    @HOSTED_ONLY
    public static String baselineName() {
        return configValue(BASELINE_COMPILER_PROPERTY, baselineCompilerOption, aliases);
    }

    /**
     * The constructor for this class initializes a new adaptive compilation.
     */
    @HOSTED_ONLY
    public AdaptiveCompilationScheme() {
        assert optimizingCompilerOption.getValue() != null;
        String optName = optName();
        String baselineName = baselineName();
        optimizingCompiler = instantiateCompiler(optName);
        assert optimizingCompiler.nature() == Nature.OPT : optimizingCompiler + " is not an optimizing compiler";
        if (baselineName != null) {
            baselineCompiler = instantiateCompiler(baselineName);
            assert baselineCompiler.nature() == Nature.BASELINE : baselineCompiler + " is not a baseline compiler";
            assert baselineCompiler != optimizingCompiler;
        } else {
            baselineCompiler = null;
        }
    }

    @HOSTED_ONLY
    private static RuntimeCompiler instantiateCompiler(String name) {
        try {
            return (RuntimeCompiler) Class.forName(name).newInstance();
        } catch (Exception e) {
            throw FatalError.unexpected("Error instantiating compiler " + name, e);
        }
    }

    public String description() {
        return "default compiler: " + defaultCompiler;
    }

    @Override
    public String about() {
        return super.about() + " [opt=" + optimizingCompiler + ", baseline=" + baselineCompiler + "]";
    }

    @Override
    public Properties properties() {
        Properties props = new Properties();
        props.put(OPTIMIZING_COMPILER_PROPERTY, optimizingCompiler.getClass().getName());
        if (baselineCompiler != null) {
            props.put(BASELINE_COMPILER_PROPERTY, baselineCompiler.getClass().getName());
        }
        return props;
    }

    /**
     * This method initializes the adaptive compilation system, either while bootstrapping or
     * at VM startup time. This implementation creates daemon threads to handle asynchronous
     * compilations.
     *
     * @param phase the phase of VM starting up.
     */
    @Override
    public void initialize(MaxineVM.Phase phase) {
        optimizingCompiler.initialize(phase);
        if (baselineCompiler != null) {
            baselineCompiler.initialize(phase);
            defaultCompiler = baselineCompiler;
        } else {
            defaultCompiler = optimizingCompiler;
        }

        if (isHosted()) {
            if (BACKGROUND_COMPILATION) {
                // launch a compiler thread if background compilation is supported (currently no)
                final CompilationThread compilationThread = new CompilationThread();
                compilationThread.setDaemon(true);
                compilationThread.start();
            }
        } else if (phase == MaxineVM.Phase.STARTING) {
            if (opt) {
                defaultCompiler = optimizingCompiler;
            }

            if (RCT != 0) {
                MethodInstrumentation.enable(RCT);
            }

            if (BACKGROUND_COMPILATION) {
                // launch a compiler thread if background compilation is supported (currently no)
                final CompilationThread compilationThread = new CompilationThread();
                compilationThread.setDaemon(true);
                compilationThread.start();
            }
        } else if (phase == Phase.RUNNING) {
            if (PrintCodeCacheMetrics != 0) {
                Runtime.getRuntime().addShutdownHook(new Thread("CodeCacheMetricsPrinter") {
                    @Override
                    public void run() {
                        new CodeCacheMetricsPrinter(AdaptiveCompilationScheme.PrintCodeCacheMetrics > 1).printTo(Log.out);
                    }
                });
            }
        }
    }

    /**
     * Performs a compilation of the specified method, waiting for the compilation to finish.
     *
     * @param classMethodActor the method to compile
     * @param nature a mask of {@link Compilations.Attr} values
     * @return the target method that results from compiling the specified method
     */
    public TargetMethod synchronousCompile(ClassMethodActor classMethodActor, Nature nature) {
        RuntimeCompiler retryCompiler = null;
        while (true) {
            Compilation compilation;
            boolean doCompile = true;
            synchronized (classMethodActor) {
                assert !(classMethodActor.isNative() && classMethodActor.isVmEntryPoint()) : "cannot compile JNI functions that are native";
                Object compiledState = classMethodActor.compiledState;
                compilation = compiledState instanceof Compilation ? (Compilation) compiledState : null;
                if (compilation != null && (nature == null || nature == compilation.nature)) {
                    // Only wait for a pending compilation if it is compatible with the current request.
                    // That is, the current request does not specify a special nature (nature == null)
                    // or it specifies the same nature as the pending compilation (nature == compilation.nature)
                    if (retryCompiler != null) {
                        assert compilation.compilingThread == Thread.currentThread();
                        assert nature == null : "cannot retry if specific compilation nature is specified";
                        compilation.compiler = retryCompiler;
                    } else {
                        // the method is currently being compiled, just wait for the result
                        doCompile = false;
                    }
                } else {
                    Compilations prevCompilations = compilation != null ? compilation.prevCompilations :  (Compilations) compiledState;
                    RuntimeCompiler compiler = retryCompiler == null ? selectCompiler(classMethodActor, nature) : retryCompiler;
                    compilation = new Compilation(this, compiler, classMethodActor, prevCompilations, Thread.currentThread(), nature);
                    classMethodActor.compiledState = compilation;
                }
            }

            try {
                if (doCompile) {
                    return compilation.compile();
                }
                return compilation.get();
            } catch (Throwable t) {
                classMethodActor.compiledState = Compilations.EMPTY;
                String errorMessage = "Compilation of " + classMethodActor + " by " + compilation.compiler + " failed";
                if (VMOptions.verboseOption.verboseCompilation) {
                    boolean lockDisabledSafepoints = Log.lock();
                    Log.printCurrentThread(false);
                    Log.print(": ");
                    Log.println(errorMessage);
                    t.printStackTrace(Log.out);
                    Log.unlock(lockDisabledSafepoints);
                }
                if (!FailOverCompilation || retryCompiler != null || (baselineCompiler == null) || isHosted()) {
                    // This is the final failure: no other compilers available or failover is disabled
                    throw (InternalError) new InternalError(errorMessage + " (final attempt)").initCause(t);
                }
                if (compilation.compiler == optimizingCompiler) {
                    retryCompiler = baselineCompiler;
                } else {
                    retryCompiler = optimizingCompiler;
                }
                if (VMOptions.verboseOption.verboseCompilation) {
                    boolean lockDisabledSafepoints = Log.lock();
                    Log.printCurrentThread(false);
                    Log.println(": Retrying with " + retryCompiler + "...");
                    Log.unlock(lockDisabledSafepoints);
                }
            }
        }
    }

    @HOSTED_ONLY
    public static final HashSet<Class> compileWithBaseline = new HashSet<Class>();

    /**
     * Select the appropriate compiler based on the current state of the method.
     *
     * @param classMethodActor the class method actor to compile
     * @param nature the specific type of target method required or {@code null} if any target method is acceptable
     * @return the compiler that should be used to perform the next compilation of the method
     */
    public RuntimeCompiler selectCompiler(ClassMethodActor classMethodActor, RuntimeCompiler.Nature nature) {

        if (Actor.isUnsafe(classMethodActor.flags() | classMethodActor.compilee().flags())) {
            assert nature != Nature.BASELINE : "cannot produce baseline version of " + classMethodActor;
            return optimizingCompiler;
        }

        RuntimeCompiler compiler;

        if (isHosted()) {
            if (compileWithBaseline.contains(classMethodActor.holder().javaClass())) {
                compiler = baselineCompiler;
                assert compiler != null;
            } else {
                // at prototyping time, default to the opt compiler
                compiler = optimizingCompiler;
            }
        } else {
            if (nature == Nature.BASELINE) {
                compiler = baselineCompiler;
                assert compiler != null;
            } else if (nature == Nature.OPT) {
                compiler = optimizingCompiler;
            } else {
                compiler = defaultCompiler;
                assert compiler != null;
            }
        }

        return compiler;
    }

    /**
     * This class implements a daemon thread that performs compilations in the background. Depending on the compiler
     * configuration, multiple compilation threads may be working in parallel.
     */
    protected class CompilationThread extends Thread {

        protected CompilationThread() {
            super("compile");
            setDaemon(true);
        }

        /**
         * The current compilation being performed by this thread.
         */
        Compilation compilation;

        /**
         * Continuously polls the compilation queue for work, performing compilations as they are removed from the
         * queue.
         */
        @Override
        public void run() {
            while (true) {
                try {
                    compileOne();
                } catch (InterruptedException e) {
                    // do nothing.
                } catch (Throwable t) {
                    Log.print("Exception during compilation of " + compilation.classMethodActor);
                    t.printStackTrace();
                }
            }
        }

        /**
         * Polls the compilation queue and performs a single compilation.
         *
         * @throws InterruptedException if the thread was interrupted waiting on the queue
         */
        void compileOne() throws InterruptedException {
            compilation = null;
            synchronized (pending) {
                while (compilation == null) {
                    compilation = pending.poll();
                    if (compilation == null) {
                        pending.wait();
                    }
                }
            }
            compilation.compilingThread = Thread.currentThread();
            if (GCOnRecompilation) {
                System.gc();
            }
            compilation.compile();
            compilation = null;
        }
    }
}
