/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.VMOptions.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.runtime.*;

/**
 * This class implements an adaptive compilation system with multiple compilers with different compilation time / code
 * quality tradeoffs. It encapsulates the necessary infrastructure for recording profiling data, selecting what and when
 * to recompile, etc.
 *
 * @author Ben L. Titzer
 * @author Michael Van De Vanter
 */
public class AdaptiveCompilationScheme extends AbstractVMScheme implements CompilationScheme {

    /**
     * A constant used to indicated that recompilation is disabled.
     */
    public static final int RECOMPILATION_DISABLED = -1;

    private static final int DEFAULT_RECOMPILATION_THRESHOLD = 5000;

    /**
     * Stores the default threshold at which a recompilation is triggered from the baseline compiler to the next level
     * of optimization. This is typically the number of invocations of the method.
     */
    public static int defaultRecompilationThreshold0 = DEFAULT_RECOMPILATION_THRESHOLD;

    /**
     * A queue of pending compilations.
     */
    protected final LinkedList<Compilation> pending = new LinkedList<Compilation>();

    /**
     * The baseline (JIT) compiler.
     */
    protected final RuntimeCompilerScheme jitCompiler;

    /**
     * The optimizing compiler.
     */
    protected final RuntimeCompilerScheme optCompiler;

    /**
     * List of attached Compilation observers.
     */
    @RESET
    protected LinkedList<CompilationObserver> observers;

    private static boolean jit;
    private static boolean opt;
    private static int RCT = DEFAULT_RECOMPILATION_THRESHOLD;
    private static boolean GCOnRecompilation;
    private static boolean FailOverCompilation = true;

    static {
        addFieldOption("-X", "jit", "Select JIT only mode, with no recompilation.");
        addFieldOption("-X", "opt", "Select optimizing-only compilation.");
        addFieldOption("-XX:", "RCT", "Set the recompilation threshold for methods.");
        addFieldOption("-XX:", "GCOnRecompilation", "Force GC before every re-compilation.");
        addFieldOption("-XX:", "FailOverCompilation", "Retry failed compilations with another compiler (if available).");
    }

    /**
     * The (dynamically selected) compilation mode.
     */
    private Mode mode = Mode.JIT;
    private static final boolean BACKGROUND_COMPILATION = false;

    public Mode mode() {
        return mode;
    }

    /**
     * Set the compilation mode of this compilation scheme, which determines which compilers to select at runtime.
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * The constructor for this class initializes a new adaptive compilation system with the specified VM configuration,
     * configuring itself according to the compiler(s) selected in the VM configuration.
     *
     * @param vmConfiguration the configuration of the virtual machine
     */
    @HOSTED_ONLY
    public AdaptiveCompilationScheme() {
        optCompiler = vmConfig().optCompilerScheme();
        jitCompiler = vmConfig().jitCompilerScheme();
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
        if (isHosted()) {
            if (BACKGROUND_COMPILATION) {
                // launch a compiler thread if background compilation is supported (currently no)
                final CompilationThread compilationThread = new CompilationThread();
                compilationThread.setDaemon(true);
                compilationThread.start();
            }
        } else if (phase == MaxineVM.Phase.STARTING) {
            if (jit) {
                defaultRecompilationThreshold0 = RECOMPILATION_DISABLED;
                setMode(Mode.JIT);
            } else if (opt) {
                defaultRecompilationThreshold0 = RECOMPILATION_DISABLED;
                setMode(Mode.OPTIMIZED);
            } else {
                defaultRecompilationThreshold0 = RCT;
                MethodInstrumentation.enable(defaultRecompilationThreshold0);
                setMode(Mode.MIXED);
            }

            if (BACKGROUND_COMPILATION) {
                // launch a compiler thread if background compilation is supported (currently no)
                final CompilationThread compilationThread = new CompilationThread();
                compilationThread.setDaemon(true);
                compilationThread.start();
            }
        }
    }

    /**
     * Performs a compilation of the specified method, waiting for the compilation to finish.
     *
     * @param classMethodActor the method to compile
     * @return the target method that results from compiling the specified method
     */
    public TargetMethod synchronousCompile(ClassMethodActor classMethodActor) {
        return synchronousCompileHelper(classMethodActor, null);
    }

    public TargetMethod synchronousCompile(ClassMethodActor classMethodActor, RuntimeCompilerScheme compiler) {
        return synchronousCompileHelper(classMethodActor, compiler);
    }

    private TargetMethod synchronousCompileHelper(ClassMethodActor classMethodActor, RuntimeCompilerScheme recommendedCompiler) {
        boolean retrying = false;
        while (true) {
            Compilation compilation;
            boolean doCompile = true;
            synchronized (classMethodActor) {
                assert !(classMethodActor.isNative() && classMethodActor.isVmEntryPoint()) : "cannot compile JNI functions that are native";
                Object targetState = classMethodActor.targetState;
                if (targetState == null) {
                    // this is the first compilation.
                    RuntimeCompilerScheme compiler = !retrying ? selectCompiler(classMethodActor, true, recommendedCompiler) : optCompiler;
                    compilation = new Compilation(this, compiler, classMethodActor, targetState, Thread.currentThread());
                    classMethodActor.targetState = compilation;
                } else if (targetState instanceof Compilation) {
                    // the method is currently being compiled, just wait for the result
                    compilation = (Compilation) targetState;
                    doCompile = false;
                } else {
                    // this method has already been compiled once
                    RuntimeCompilerScheme compiler = !retrying ? selectCompiler(classMethodActor, classMethodActor.targetMethodCount() == 0, recommendedCompiler) : optCompiler;
                    TargetMethod targetMethod = classMethodActor.currentTargetMethod();
                    if (targetMethod != null && compiler.compiledType() == targetMethod.getClass()) {
                        return targetMethod;
                    }
                    compilation = new Compilation(this, compiler, classMethodActor, targetState, Thread.currentThread());
                    classMethodActor.targetState = compilation;
                }
            }

            try {
                if (doCompile) {
                    return compilation.compile(observers);
                }
                return compilation.get();
            } catch (Throwable t) {
                classMethodActor.targetState = null;
                String errorMessage = "Compilation of " + classMethodActor + " by " + compilation.compiler + " failed";
                Log.println(errorMessage);
                t.printStackTrace(Log.out);
                if (compilation.compiler != optCompiler && FailOverCompilation) {
                    Log.println("Retrying with " + optCompiler + "...");
                    retrying = true;
                } else {
                    throw (InternalError) new InternalError(errorMessage).initCause(t);
                }
            }
        }
    }

    /**
     * This method allows an observer to be notified before the compilation of a method begins.
     */
    public synchronized void addObserver(CompilationObserver observer) {
        if (observers == null) {
            observers = new LinkedList<CompilationObserver>();
        }
        observers.add(observer);
    }

    /**
     * This method allows an observer to be notified after the compilation of a method completes.
     */
    public synchronized void removeObserver(CompilationObserver observer) {
        if (observers != null) {
            observers.remove(observer);
            if (observers.size() == 0) {
                observers = null;
            }
        }
    }

    /**
     * Checks whether there is currently a compilation being performed.
     *
     * @return {@code true} if there is currently a compilation pending or being performed; {@code false} otherwise
     */
    public boolean isCompiling() {
        throw FatalError.unimplemented();
    }

    /**
     * Select the appropriate compiler based on the current state of the method.
     *
     * @param classMethodActor the class method actor to compile
     * @param firstCompile {@code true} if this is the first compilation of this method
     * @param recommendedCompiler the compiler recommended for use
     * @return the compiler that should be used to perform the next compilation of the method
     */
    RuntimeCompilerScheme selectCompiler(ClassMethodActor classMethodActor, boolean firstCompile, RuntimeCompilerScheme recommendedCompiler) {

        int flags = classMethodActor.flags() | classMethodActor.compilee().flags();
        if (Actor.isUnsafe(flags)) {
            if (isHosted() && Builtin.builtinInvocationStubClasses.contains(classMethodActor.holder())) {
                // Invocation stubs for builtins must be compiled with CPS.
                // To satisfy this, we simply compile all invocation stubs with CPS for now.
                return CPSCompiler.Static.compiler();
            }

            if (classMethodActor.isTemplate()) {
                // Templates must be compiled with the CPS compiled
                return CPSCompiler.Static.compiler();
            }
            return optCompiler;
        }

        RuntimeCompilerScheme compiler;

        // use the recommended compiler
        if (recommendedCompiler != null) {
            compiler = recommendedCompiler;
        } else if (isHosted()) {
            if (classMethodActor.getAnnotation(SNIPPET.class) != null) {
                // snippets must be compiled with the opt compiler
                compiler = optCompiler;
            } else if (Builtin.builtinInvocationStubClasses.contains(classMethodActor.holder())) {
                // Invocation stubs for builtins must be compiled with CPS.
                compiler = CPSCompiler.Static.compiler();
            } else if (mode == Mode.PROTOTYPE_JIT) {
                compiler = jitCompiler;
            } else {
                // at prototyping time, default to the opt compiler
                compiler = optCompiler;
            }
        } else {
            // in optimized mode, default to the optimizing compiler
            if (mode == Mode.OPTIMIZED) {
                if (classMethodActor.isSynthetic() && firstCompile) {
                    // we must at first use the JIT for reflective invocation stubs,
                    // otherwise the CPS compiler may not terminate
                    compiler = jitCompiler;
                } else {
                    compiler = optCompiler;
                }
            } else {
                // use the jit if the first compile or in JIT mode
                if (firstCompile || mode == Mode.JIT) {
                    compiler = jitCompiler;
                } else {
                    // not the first compile, use the optimizing compiler
                    compiler = optCompiler;
                }
            }
        }

        return compiler;
    }

    /**
     * This class implements a daemon thread that performs compilations in the background. Depending on the compiler
     * configuration, multiple compilation threads may be working in parallel.
     *
     * @author Ben L. Titzer
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
            compilation.compile(observers);
            compilation = null;
        }
    }
}
