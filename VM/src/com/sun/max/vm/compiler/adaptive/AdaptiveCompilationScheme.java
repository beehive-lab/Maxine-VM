/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.compiler.adaptive;

import static com.sun.max.vm.VMOptions.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.jit.JitInstrumentation;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.prototype.*;
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
     * Stores the default threshold at which a recompilation is triggered from optimized code to more highly optimized
     * code.
     */
    public static int defaultRecompilationThreshold1 = DEFAULT_RECOMPILATION_THRESHOLD;

    /**
     * A queue of pending compilations.
     */
    protected final LinkedList<Compilation> pending = new LinkedList<Compilation>();

    /**
     * The compiler that is used as the default while bootstrapping.
     */
    @HOSTED_ONLY
    protected final BootstrapCompilerScheme bootstrapCompiler;

    /**
     * The baseline (JIT) compiler.
     */
    protected final RuntimeCompilerScheme jitCompiler;

    /**
     * The C1X compiler.
     */
    protected RuntimeCompilerScheme c1xCompiler;

    /**
     * The optimizing compiler, if any.
     */
    protected final BootstrapCompilerScheme optimizingCompiler;

    /**
     * List of attached Compilation observers.
     */
    @RESET
    protected LinkedList<CompilationObserver> observers;

    private static final VMOption intOption = register(new VMOption("-Xint",
                    "Interpreted mode execution only."), MaxineVM.Phase.STARTING);
    private static final VMOption jitOption = register(new VMOption("-Xjit",
                    "Selects JIT only mode, with no recompilation."), MaxineVM.Phase.STARTING);
    private static final VMOption optOption = register(new VMOption("-Xopt",
                    "Selects optimized only mode."), MaxineVM.Phase.STARTING);
    private static final VMIntOption thresholdOption = register(new VMIntOption("-XX:RCT=", DEFAULT_RECOMPILATION_THRESHOLD,
                    "In mixed mode, sets the recompilation threshold for methods."), MaxineVM.Phase.STARTING);
    static final VMBooleanXXOption gcOnCompileOption = register(new VMBooleanXXOption("-XX:-GCOnCompilation",
                    "When specified, the compiler will request GC before every compilation operation, " +
                    "which is useful for testing corner cases in the compiler/GC interactions."), MaxineVM.Phase.STARTING);
    private static final VMBooleanXXOption gcOnRecompileOption = register(new VMBooleanXXOption("-XX:-GCOnRecompilation",
                    "When specified, the compiler will request GC before every re-compilation operation, " +
                    "which is useful for testing corner cases in the compiler/GC interactions."), MaxineVM.Phase.STARTING);
    private static final VMBooleanXXOption failoverOption = register(new VMBooleanXXOption("-XX:-FailOverCompilation",
                    "When specified, the compiler will attempt to use a different compiler if compilation fails " +
                    "with the first compiler."), MaxineVM.Phase.STARTING);

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
    public AdaptiveCompilationScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        bootstrapCompiler = vmConfiguration.compilerScheme();
        optimizingCompiler = bootstrapCompiler;
        jitCompiler = vmConfiguration.jitScheme();
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
        if (MaxineVM.isHosted()) {
            if (BACKGROUND_COMPILATION) {
                // launch a compiler thread if background compilation is supported (currently no)
                final CompilationThread compilationThread = new CompilationThread();
                compilationThread.setDaemon(true);
                compilationThread.start();
            }
        } else if (phase == MaxineVM.Phase.STARTING) {
            if (jitOption.isPresent()) {
                defaultRecompilationThreshold0 = RECOMPILATION_DISABLED;
                defaultRecompilationThreshold1 = RECOMPILATION_DISABLED;
                setMode(Mode.JIT);
            } else if (intOption.isPresent()) {
                defaultRecompilationThreshold0 = RECOMPILATION_DISABLED;
                defaultRecompilationThreshold1 = RECOMPILATION_DISABLED;
                setMode(Mode.INTERPRETED);
            } else if (optOption.isPresent()) {
                defaultRecompilationThreshold0 = RECOMPILATION_DISABLED;
                defaultRecompilationThreshold1 = RECOMPILATION_DISABLED;
                setMode(Mode.OPTIMIZED);
            } else {
                defaultRecompilationThreshold0 = thresholdOption.getValue();
                JitInstrumentation.enable();
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
        return synchronousCompileHelper(classMethodActor, null, null);
    }

    public TargetMethod synchronousCompile(ClassMethodActor classMethodActor, RuntimeCompilerScheme compiler) {
        return synchronousCompileHelper(classMethodActor, compiler, null);
    }

    private TargetMethod synchronousCompileHelper(ClassMethodActor classMethodActor, RuntimeCompilerScheme recommendedCompiler, RuntimeCompilerScheme prohibitedCompiler) {
        Compilation compilation;
        synchronized (classMethodActor) {
            assert !(classMethodActor.isNative() && classMethodActor.isJniFunction()) : "cannot compile JNI functions that are native";
            Object targetState = classMethodActor.targetState;
            if (targetState == null) {
                // this is the first compilation.
                RuntimeCompilerScheme compiler = selectCompiler(classMethodActor, true, recommendedCompiler, prohibitedCompiler);
                compilation = new Compilation(this, compiler, classMethodActor, targetState, Thread.currentThread());
                classMethodActor.targetState = compilation;
            } else if (targetState instanceof Compilation) {
                // the method is currently being compiled, wait for the result
                compilation = (Compilation) targetState;

                try {
                    return compilation.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                // this method has already been compiled
                RuntimeCompilerScheme compiler = selectCompiler(classMethodActor, classMethodActor.targetMethodCount() == 0, recommendedCompiler, prohibitedCompiler);
                TargetMethod targetMethod = classMethodActor.currentTargetMethod();
                if (targetMethod != null && targetMethod.compilerScheme == compiler) {
                    return targetMethod;
                }
                compilation = new Compilation(this, compiler, classMethodActor, targetState, Thread.currentThread());
                classMethodActor.targetState = compilation;
            }
        }

        final RuntimeCompilerScheme compilerScheme = compilation.compilerScheme;
        try {
            return compilation.compile(observers);
        } catch (Throwable t) {
            Trace.line(1, "Exception occurred during compilation of method " + classMethodActor.toString() + ": " + t.toString());
            Trace.line(1, "Compiler scheme is: " + compilerScheme.toString() + " - trying different compiler scheme...");
            if (failoverOption.getValue()) {
                return synchronousCompileHelper(classMethodActor, null, compilerScheme);
            }
            throw new RuntimeException("Error compiling: " + classMethodActor, t);
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
     * @param prohibitedCompiler the compiler not to use (because it failed)
     * @return the compiler that should be used to perform the next compilation of the method
     */
    RuntimeCompilerScheme selectCompiler(ClassMethodActor classMethodActor, boolean firstCompile, RuntimeCompilerScheme recommendedCompiler, RuntimeCompilerScheme prohibitedCompiler) {

        if (prohibitedCompiler == optimizingCompiler) {
            throw new RuntimeException("Must use the optimizing compiler, if compilation fails!");
        } else if (prohibitedCompiler != null) {
            return optimizingCompiler;
        }

        if (classMethodActor.isUnsafe()) {
            // the JIT cannot handle unsafe features
            return optimizingCompiler;
        }

        if (MaxineVM.isHosted()) {
            // if we are bootstrapping, then always use the prototype compiler
            // unless forced to use the JIT (e.g. for testing purposes)
            if (CompiledPrototype.jitCompile(classMethodActor)) {
                return jitCompiler;
            } else if (CompiledPrototype.c1xCompile(classMethodActor)) {
                synchronized (this) {
                    if (c1xCompiler == null) {
                        c1xCompiler = new C1XCompilerScheme(vmConfiguration());
                        c1xCompiler.initialize(Phase.BOOTSTRAPPING);
                    }
                    return c1xCompiler;
                }
            }

            if (classMethodActor.isSynthetic() || classMethodActor.holder().packageName().startsWith(new com.sun.max.unsafe.Package().name()) || classMethodActor.holder().packageName().startsWith("com.sun.max")) {
                return bootstrapCompiler;
            }

            if (mode == Mode.PROTOTYPE_JIT) {
                return jitCompiler;
            }

            return bootstrapCompiler;
        }

        // templates should only be compiled while bootstrapping
        assert !classMethodActor.isTemplate();

        if (recommendedCompiler != null) {
            return recommendedCompiler;
        }

        if (mode == Mode.INTERPRETED) {
            if (!classMethodActor.isSynthetic() && !classMethodActor.isNative()) {
                return vmConfiguration().interpreterStubCompiler;
            }
        }

        if (firstCompile) {
            if (classMethodActor.isSynthetic() && !classMethodActor.isNative()) {
                // we must at first use the JIT for reflective invocation stubs, otherwise meta-evaluation may not
                // terminate
                return jitCompiler;
            }
            if (mode == Mode.OPTIMIZED) {
                return optimizingCompiler;
            }
        } else if (mode != Mode.JIT) {
            return optimizingCompiler;
        }
        return jitCompiler;
    }

    /**
     * This method provides a hint to the adaptive compilation system that it should increase the optimization level for
     * a particular method, potentially recompiling it if it has already been compiled. This method is typically called
     * in response to dynamic feedback mechanisms such as a method invocation counter.
     *
     * @param classMethodActor the method for which to increase the optimization level
     * @param synchronous a boolean indicating whether any recompilation should be performed synchronously (i.e.
     */
    public static void increaseOptimizationLevel(ClassMethodActor classMethodActor, boolean synchronous) {
        final CompilationScheme compilationScheme = VMConfiguration.target().compilationScheme();
        if (compilationScheme instanceof AdaptiveCompilationScheme) {
            ((AdaptiveCompilationScheme) compilationScheme).reoptimize(classMethodActor, synchronous);
        }
    }

    /**
     * This method reoptimizes the specified method with a higher level of optimization, choosing a more optimizing
     * compiler and/or more aggressive optimizations. This method can be used in either synchronous mode (i.e.
     * compilation is performed right away) or asynchronous mode (i.e. compilation is performed in the background).
     *
     * @param classMethodActor the method to be reoptimized
     * @param synchronous a boolean indicating whether the compilation should be performed immediately or queued for
     */
    public void reoptimize(ClassMethodActor classMethodActor, boolean synchronous) {
        synchronousCompile(classMethodActor, optimizingCompiler);
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
            if (gcOnRecompileOption.getValue()) {
                System.gc();
            }
            compilation.compile(observers);
            compilation = null;
        }
    }
}
