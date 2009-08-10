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

import com.sun.max.vm.compiler.*;
import com.sun.max.vm.actor.member.ClassMethodActor;
import com.sun.max.vm.runtime.FatalError;
import static com.sun.max.vm.VMOptions.verboseOption;
import com.sun.max.vm.Log;
import com.sun.max.annotate.INSPECTED;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * This class represents an ongoing or completed compilation.
 *
 * @author Ben L. Titzer
 */
public class Compilation implements Future<TargetMethod> {
    public final CompilationScheme compilationScheme;
    public final DynamicCompilerScheme compilerScheme;
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
                       DynamicCompilerScheme compilerScheme,
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
                    throw FatalError.unexpected("Compilation of " + classMethodActor.format("%H.%n(%p)") + " is recursive");
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
        DynamicCompilerScheme compiler = compilerScheme;
        TargetMethod targetMethod = null;

        // notify any compilation observers
        observeBeforeCompilation(observers, compiler);

        try {
            // attempt the compilation
            String methodString = logBeforeCompilation(compiler);
            targetMethod = IrTargetMethod.asTargetMethod(compiler.compile(classMethodActor));
            logAfterCompilation(compiler, targetMethod, methodString);
        } finally {
            // compilation finished
            done = true;

            synchronized (classMethodActor) {
                // update the target state of the class method actor
                // assert classMethodActor.targetState == this;
                if (targetMethod != null) {
                    // compilation succeeded and produced a target method
                    classMethodActor.targetState = TargetState.addTargetMethod(targetMethod, previousTargetState);
                } else {
                    // compilation did not produce a target method, reset to previous state
                    classMethodActor.targetState = previousTargetState;
                }
                // notify any waiters on this compilation
                classMethodActor.notifyAll();
            }

            // notify any compilation observers
            observeAfterCompilation(observers, compiler, targetMethod);
        }
        return targetMethod;
    }

    private void logAfterCompilation(DynamicCompilerScheme compiler, TargetMethod targetMethod, String methodString) {
        if (verboseOption.verboseCompilation) {
            Log.print(compiler.name() + ": Compiled  " + methodString + " @ ");
            Log.print(targetMethod.codeStart());
            Log.print(" {code length=" + targetMethod.codeLength() + "}");
            Log.println();
        }
    }

    private String logBeforeCompilation(DynamicCompilerScheme compiler) {
        String methodString = null;
        if (verboseOption.verboseCompilation) {
            methodString = classMethodActor.format("%H.%n(%p)");
            Log.println(compiler.name() + ": Compiling " + methodString);
        }
        return methodString;
    }

    private void observeBeforeCompilation(List<CompilationObserver> observers, DynamicCompilerScheme compiler) {
        if (observers != null) {
            for (CompilationObserver observer : observers) {
                observer.observeBeforeCompilation(classMethodActor, compiler);
            }
        }
    }
    private void observeAfterCompilation(List<CompilationObserver> observers, DynamicCompilerScheme compiler, TargetMethod result) {
        if (observers != null) {
            for (CompilationObserver observer : observers) {
                observer.observeAfterCompilation(classMethodActor, compiler, result);
            }
        }
    }

}
