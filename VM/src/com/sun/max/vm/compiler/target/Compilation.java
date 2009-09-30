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
import com.sun.max.vm.runtime.*;

/**
 * This class represents an ongoing or completed compilation.
 *
 * @author Ben L. Titzer
 */
public class Compilation implements Future<TargetMethod> {
    public final CompilationScheme compilationScheme;
    public final RuntimeCompilerScheme compilerScheme;
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
                       RuntimeCompilerScheme compilerScheme,
                       ClassMethodActor classMethodActor,
                       Object previousTargetState,
                       Thread compilingThread) {

        this.compilationScheme = compilationScheme;
        this.compilerScheme = compilerScheme;
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
            synchronized (classMethodActor) {
                if (compilingThread == Thread.currentThread()) {
                    throw new RuntimeException("Compilation of " + classMethodActor.format("%H.%n(%p)") + " is recursive, current compilation scheme: " + this.compilationScheme);
                }

                // the class method actor is used here as the condition variable
                classMethodActor.wait();
                return classMethodActor.currentTargetMethod();
            }
        }
        return classMethodActor.currentTargetMethod();
    }

    /**
     * Gets the result of this compilation, blocking for a maximum amount of time.
     *
     * @return the target method that resulted from this compilation
     */
    public TargetMethod get(long timeout, TimeUnit unit) throws InterruptedException {
        if (!done) {
            synchronized (classMethodActor) {
                // the class method actor is used here as the condition variable
                classMethodActor.wait(timeout); // TODO: convert timeout to milliseconds
                return classMethodActor.currentTargetMethod();
            }
        }
        return classMethodActor.currentTargetMethod();
    }

    /**
     * Perform the compilation, notifying the specified observers.
     *
     * @param observers a list of observers to notify; {@code null} if there are no observers
     * to notify
     * @return the target method that is the result of the compilation
     */
    public TargetMethod compile(List<CompilationObserver> observers) {
        RuntimeCompilerScheme compiler = compilerScheme;
        TargetMethod targetMethod = null;

        // notify any compilation observers
        observeBeforeCompilation(observers, compiler);

        Throwable error = null;
        String methodString = "";
        try {
            // attempt the compilation
            methodString = logBeforeCompilation(compiler);
            targetMethod = compiler.compile(classMethodActor);

            if (targetMethod == null) {
                throw new InternalError(classMethodActor.format("Result of compiling of %H.%n(%p) is null"));
            }

            logAfterCompilation(compiler, targetMethod, methodString);
        } catch (Throwable t) {
            error = t;
        } finally {
            // invariant: (targetMethod != null) != (error != null)
            synchronized (classMethodActor) {
                // update the target state of the class method actor
                // assert classMethodActor.targetState == this;
                if (targetMethod != null) {
                    // compilation succeeded and produced a target method
                    classMethodActor.targetState = TargetState.addTargetMethod(targetMethod, previousTargetState);
                } else {
                    FatalError.check(error != null, "Target method cannot be null if no compilation error occurred");
                    // compilation caused an exception: save it as the target state
                    classMethodActor.targetState = error;
                }
                // compilation finished: this must come after the assignment to classMethodActor.targetState
                done = true;

                // notify any waiters on this compilation
                classMethodActor.notifyAll();
            }

            // notify any compilation observers
            observeAfterCompilation(observers, compiler, targetMethod);
        }

        if (error != null) {

            logCompilationError(error, compiler, targetMethod, methodString);

            throw new RuntimeException(error);
        }

        return targetMethod;
    }

    private void logCompilationError(Throwable error, RuntimeCompilerScheme compiler, TargetMethod targetMethod, String methodString) {
        if (verboseOption.verboseCompilation) {
            Log.printCurrentThread(false);
            Log.print(": " + compiler.name() + ": Compilation failed  " + methodString + " @ ");
            Log.print(error.toString());
            error.printStackTrace(Log.out);
            Log.println();
        }
    }

    private String logBeforeCompilation(RuntimeCompilerScheme compiler) {
        String methodString = null;
        if (verboseOption.verboseCompilation) {
            methodString = classMethodActor.format("%H.%n(%p)");
            Log.printCurrentThread(false);
            Log.println(": " + compiler.name() + ": Compiling " + methodString);
        }
        return methodString;
    }

    private void logAfterCompilation(RuntimeCompilerScheme compiler, TargetMethod targetMethod, String methodString) {
        if (verboseOption.verboseCompilation) {
            Log.printCurrentThread(false);
            Log.print(": " + compiler.name() + ": Compiled  " + methodString + " @ ");
            Log.print(targetMethod.codeStart());
            Log.print(" {code length=" + targetMethod.codeLength() + "}");
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
