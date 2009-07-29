/*
 * Copyright (c) 2008 Sun Microsystems, Inc.  All rights reserved.
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.sun.max.profile.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.adaptive.Compilation.CompilationStatsOption.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

/**
 * This class represents the state for the compilation of a single method, including the method to be compiled, that
 * method's state, and the compiler to use. This class also implements the {@link Future} interface, which allows
 * this compilation to be queried by the clients of the compilation scheme to obtain the target method result later.
 *
 * @author Ben L. Titzer
 * @author Michael Bebenita
 */
class Compilation implements Future<TargetMethod> {

    /**
     * A reference to the enclosing instance of the compilation scheme.
     */
    private final AdaptiveCompilationScheme adaptiveCompilationScheme;

    /**
     * The state of the method this compilation is to compile.
     */
    protected final AdaptiveMethodState methodState;

    /**
     * Compilation options to be passed on to the underlying compiler.
     */
    protected final CompilationDirective compilationDirective;

    /**
     * The compiler to use for this compilation.
     */
    protected DynamicCompilerScheme compiler;

    /**
     * State of this compilation. If {@code true}, then this compilation has finished and the target method is
     * available.
     */
    protected boolean done;

    protected final Thread compilingThread;

    Compilation(AdaptiveCompilationScheme adaptiveCompilationScheme, AdaptiveMethodState methodState, CompilationDirective compilationDirective) {
        this.adaptiveCompilationScheme = adaptiveCompilationScheme;
        this.methodState = methodState;
        this.compilingThread = Thread.currentThread();
        this.compilationDirective = compilationDirective;
    }

    /**
     * Cancel this compilation. Ignored.
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    /**
     * Checks whether this compilation was canceled. This method always returns {@code false}
     */
    public boolean isCancelled() {
        return false;
    }

    /**
     * Returns whether this compilation is done.
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Gets the result of this compilation, blocking if necessary.
     *
     * @return the target method that resulted from this compilation
     */
    public TargetMethod get() throws InterruptedException {
        if (!done) {
            synchronized (methodState) {
                if (compilingThread == Thread.currentThread()) {
                    FatalError.unexpected("Compilation of " + methodState.classMethodActor().format("%H.%n(%p)") + " is recursive");
                }

                // the method state object is used here as the condition variable
                methodState.wait();
                return methodState.currentTargetMethod(compilationDirective);
            }
        }
        return methodState.currentTargetMethod(compilationDirective);
    }

    /**
     * Gets the result of this compilation, blocking for a maximum amount of time.
     *
     * @return the target method that resulted from this compilation
     */
    public TargetMethod get(long timeout, TimeUnit unit) throws InterruptedException {
        if (!done) {
            synchronized (methodState) {
                // the method state object is used here as the condition variable
                methodState.wait(timeout); // TODO: convert timeout to milliseconds
                return methodState.currentTargetMethod(compilationDirective);
            }
        }
        return methodState.currentTargetMethod(compilationDirective);
    }

    void compile(CompilationDirective directive) {
        if (AdaptiveCompilationScheme.gcOnCompileOption.getValue()) {
            System.gc();
        }
        DynamicCompilerScheme comp = this.compiler;
        if (comp == null) {
            if (directive.jitOnly() || directive.traceInstrument()) {
                assert methodState.classMethodActor().isUnsafe() == false;
                comp = this.adaptiveCompilationScheme.jitCompiler;
            } else {
                comp = this.adaptiveCompilationScheme.selectCompiler(methodState);
            }
        }
        assert methodState.currentCompilation(directive) == this;
        TargetMethod targetMethod = null;

        adaptiveCompilationScheme.observeBeforeCompilation(this, comp);

        try {
            String methodString = null;
            if (verboseOption.verboseCompilation) {
                methodString = methodState.classMethodActor().format("%H.%n(%p)");
                Log.println(comp.name() + ": Compiling " + methodString);
            }
            final CompilerStats stats = statsOptions.start(comp, methodState.classMethodActor());
            targetMethod = IrTargetMethod.asTargetMethod(comp.compile(methodState.classMethodActor(), directive));
            statsOptions.stop(stats);
            if (verboseOption.verboseCompilation) {
                Log.print(comp.name() + ": Compiled  " + methodString + " @ ");
                Log.print(targetMethod.codeStart());
                Log.print(" {code length=" + targetMethod.codeLength() + "}");
                Log.println();
            }
        } finally {
            done = true;

            synchronized (methodState) {
                if (targetMethod != null) {
                    // compilation succeeded and produced a target method
                    methodState.setTargetMethod(targetMethod, directive);
                }
                methodState.setCurrentCompilation(null, directive);
                methodState.notifyAll();
            }

            adaptiveCompilationScheme.observeAfterCompilation(this, comp, targetMethod);
            synchronized (adaptiveCompilationScheme.completionLock) {
                adaptiveCompilationScheme.completed++;
            }
        }
    }

    private static final CompilationStatsOption statsOptions = register(new CompilationStatsOption(), MaxineVM.Phase.STARTING);

    static class CompilationStatsOption extends TimerOption {
        static class CompilerStats {
            CompilerStats(DynamicCompilerScheme compiler) {
                this.compiler = compiler;
            }
            final DynamicCompilerScheme compiler;
            final AtomicLong time = new AtomicLong();
            final AtomicLong bytes = new AtomicLong();
        }

        private CompilerStats[] allCompilerStats;

        CompilationStatsOption() {
            super("-XX:-TimeCompilation", "Compilation", "Time compilation.", new MultiThreadTimer(Clock.SYSTEM_MILLISECONDS));
        }

        CompilerStats start(DynamicCompilerScheme compiler, ClassMethodActor classMethodActor) {
            CompilerStats stats = null;
            if (isPresent()) {
                stats = makeStats(compiler);
                final int bytes = classMethodActor.codeAttribute().code().length;
                stats.bytes.addAndGet(bytes);
                timerMetric.start();
            }
            return stats;
        }

        private CompilerStats makeStats(DynamicCompilerScheme compiler) {
            if (allCompilerStats == null) {
                final CompilerStats stats = new CompilerStats(compiler);
                allCompilerStats = new CompilerStats[] {stats};
                return stats;
            }
            CompilerStats stats = null;
            for (CompilerStats s : allCompilerStats) {
                if (s.compiler == compiler) {
                    stats = s;
                }
            }
            if (stats == null) {
                stats = new CompilerStats(compiler);
                allCompilerStats = Arrays.copyOf(allCompilerStats, allCompilerStats.length + 1);
                allCompilerStats[allCompilerStats.length - 1] = stats;
            }
            return stats;
        }

        void stop(CompilerStats stats) {
            if (isPresent()) {
                timerMetric.stop();
                stats.time.addAndGet(timerMetric.getLastElapsedTime());
            }
        }

        void printStats(String compilerName, long milliseconds, long bytes) {
            Log.print("    ");
            Log.print(compilerName);
            Log.print(" compilation: ");
            Log.print(milliseconds);
            Log.print(" ms, ");
            Log.print(bytes);
            Log.print(" bytes, ");
            final double seconds = (double) milliseconds / 1000;
            if (seconds != 0) {
                Log.print(bytes / seconds);
                Log.print(" bytes/sec");
            }
            Log.println();
        }

        @Override
        protected void beforeExit() {
            if (allCompilerStats != null) {
                long bytesTotal = 0;
                for (CompilerStats stats : allCompilerStats) {
                    printStats(stats.compiler.name(), stats.time.get(), stats.bytes.get());
                    bytesTotal += stats.bytes.get();
                }
                printStats("All", timerMetric.getElapsedTime(), bytesTotal);
            }
        }
    }
}
