/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.target;

import static com.sun.max.vm.VMOptions.*;

import java.util.*;
import java.util.concurrent.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * This class represents an ongoing or completed compilation.
 *
 * @author Ben L. Titzer
 */
public class Compilation implements Future<TargetMethod> {

    /**
     * Used to detect re-entrant compilation which indicates the boot image closure was not incomplete.
     */
    private static final ObjectThreadLocal<Compilation> COMPILATION = new ObjectThreadLocal<Compilation>("COMPILATION", "current compilation");

    private static final VMBooleanXXOption GC_ON_COMPILE_OPTION = register(new VMBooleanXXOption("-XX:-GCOnCompilation",
        "When specified, the compiler will request GC before every compilation operation."), MaxineVM.Phase.STARTING);

    public static final VMBooleanXXOption TIME_COMPILATION = register(new VMBooleanXXOption("-XX:-TimeCompilation",
        "Report time spent in compilation.") {
        @Override
        protected void beforeExit() {
            if (getValue()) {
                Log.print("Time spent in compilation: ");
                Log.print(compilationTime);
                Log.println("ms");
            }

        }
    }, MaxineVM.Phase.STARTING);

    @RESET
    private static long compilationTime;

    public final CompilationScheme compilationScheme;
    public final RuntimeCompilerScheme compiler;
    public final ClassMethodActor classMethodActor;
    @INSPECTED
    public final Object previousTargetState;
    public Thread compilingThread;
    public TargetMethod result;

    /**
     * State of this compilation. If {@code true}, then this compilation has finished and the target
     * method is available.
     */
    public boolean done;

    public Compilation(CompilationScheme compilationScheme,
                       RuntimeCompilerScheme compiler,
                       ClassMethodActor classMethodActor,
                       Object previousTargetState,
                       Thread compilingThread) {

        this.compilationScheme = compilationScheme;
        this.compiler = compiler;
        this.classMethodActor = classMethodActor;
        this.previousTargetState = previousTargetState;
        this.compilingThread = compilingThread;
    }

    /**
     * Cancel this compilation. Ignored.
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    /**
     * Checks whether this compilation was canceled. This method always returns {@code false}.
     */
    public boolean isCancelled() {
        return false;
    }

    /**
     * Returns whether this compilation is done.
     */
    public boolean isDone() {
        synchronized (classMethodActor) {
            return done;
        }
    }

    /**
     * Gets the result of this compilation, blocking if necessary.
     *
     * @return the target method that resulted from this compilation
     */
    public TargetMethod get() throws InterruptedException {
        synchronized (classMethodActor) {
            if (!done) {
                if (compilingThread == Thread.currentThread()) {
                    throw new RuntimeException("Compilation of " + classMethodActor.format("%H.%n(%p)") + " is recursive, current compilation scheme: " + this.compilationScheme);
                }

                // the class method actor is used here as the condition variable
                classMethodActor.wait();
            }
            return classMethodActor.currentTargetMethod();
        }
    }

    /**
     * Gets the result of this compilation, blocking for a maximum amount of time.
     *
     * @return the target method that resulted from this compilation
     */
    public TargetMethod get(long timeout, TimeUnit unit) throws InterruptedException {
        synchronized (classMethodActor) {
            if (!done) {
                // the class method actor is used here as the condition variable
                classMethodActor.wait(timeout); // TODO: convert timeout to milliseconds
            }
            return classMethodActor.currentTargetMethod();
        }
    }

    /**
     * Perform the compilation, notifying the specified observers.
     *
     * @param observers a list of observers to notify; {@code null} if there are no observers
     * to notify
     * @return the target method that is the result of the compilation
     */
    public TargetMethod compile(List<CompilationObserver> observers) {
        RuntimeCompilerScheme compiler = this.compiler;
        TargetMethod targetMethod = null;

        // notify any compilation observers
        observeBeforeCompilation(observers, compiler);

        Throwable error = null;
        String methodString = "";
        try {
            methodString = logBeforeCompilation(compiler);
            if (StackReferenceMapPreparer.VerifyRefMaps) {
                StackReferenceMapPreparer.verifyReferenceMapsForThisThread();
            }
            if (GC_ON_COMPILE_OPTION.getValue() && Heap.isInitialized()) {
                System.gc();
                StackReferenceMapPreparer.verifyReferenceMapsForThisThread();
            }
            long startCompile = 0;
            if (TIME_COMPILATION.getValue()) {
                startCompile = System.currentTimeMillis();
            }

            // Check for recursive compilation
            if (COMPILATION.get() != null) {
                FatalError.unexpected("Compilation of " + classMethodActor + " while compiling " + COMPILATION.get().classMethodActor);
            }
            COMPILATION.set(this);

            // attempt the compilation
            targetMethod = compiler.compile(classMethodActor);

            if (targetMethod == null) {
                throw new InternalError(classMethodActor.format("Result of compiling of %H.%n(%p) is null"));
            }
            if (startCompile != 0) {
                compilationTime += System.currentTimeMillis() - startCompile;
            }

            logAfterCompilation(compiler, targetMethod, methodString);
        } catch (RuntimeException t) {
            error = t;
        } catch (Error t) {
            error = t;
        } finally {
            // invariant: (targetMethod != null) != (error != null)
            synchronized (classMethodActor) {
                // update the target state of the class method actor
                // assert classMethodActor.targetState == this;
                if (targetMethod != null) {
                    // compilation succeeded and produced a target method
                    classMethodActor.targetState = TargetState.addTargetMethod(targetMethod, previousTargetState);
                }
                // compilation finished: this must come after the assignment to classMethodActor.targetState
                done = true;

                // notify any waiters on this compilation
                classMethodActor.notifyAll();
            }

            COMPILATION.set((Compilation) null);

            // notify any compilation observers
            observeAfterCompilation(observers, compiler, targetMethod);
        }

        if (error != null) {
            // an error occurred
            logCompilationError(error, compiler, methodString);
        } else if (targetMethod == null) {
            // the compilation didn't produce a target method
            FatalError.unexpected("target method should not be null");
        }

        return targetMethod;
    }

    private void logCompilationError(Throwable error, RuntimeCompilerScheme compiler, String methodString) {
        if (verboseOption.verboseCompilation) {
            Log.printCurrentThread(false);
            Log.print(": ");
            Log.print(compiler.name());
            Log.print(": Failed ");
            Log.println(methodString);
        }
        if (error instanceof Error) {
            throw (Error) error;
        }
        throw (RuntimeException) error;
    }

    private String logBeforeCompilation(RuntimeCompilerScheme compiler) {
        String methodString = null;
        if (verboseOption.verboseCompilation) {
            methodString = classMethodActor.format("%H.%n(%p)");
            Log.printCurrentThread(false);
            Log.print(": ");
            Log.print(compiler.name());
            Log.print(": Compiling ");
            Log.println(methodString);
        }
        return methodString;
    }

    private void logAfterCompilation(RuntimeCompilerScheme compiler, TargetMethod targetMethod, String methodString) {
        if (verboseOption.verboseCompilation) {
            Log.printCurrentThread(false);
            Log.print(": ");
            Log.print(compiler.name());
            Log.print(": Compiled  ");
            Log.print(methodString);
            Log.print(" @ ");
            Log.print(targetMethod.codeStart());
            Log.print(", size = ");
            Log.print(targetMethod.codeLength());
            Log.println();
        }
    }

    private void observeBeforeCompilation(List<CompilationObserver> observers, RuntimeCompilerScheme compiler) {
        if (observers != null) {
            for (CompilationObserver observer : observers) {
                observer.observeBeforeCompilation(classMethodActor, compiler);
            }
        }
    }
    private void observeAfterCompilation(List<CompilationObserver> observers, RuntimeCompilerScheme compiler, TargetMethod result) {
        if (observers != null) {
            for (CompilationObserver observer : observers) {
                observer.observeAfterCompilation(classMethodActor, compiler, result);
            }
        }
    }
}
