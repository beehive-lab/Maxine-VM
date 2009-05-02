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

import java.util.concurrent.*;

import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
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
    private final AdaptiveCompilationScheme _adaptiveCompilationScheme;

    /**
     * The state of the method this compilation is to compile.
     */
    protected final AdaptiveMethodState _methodState;

    /**
     * Compilation options to be passed on to the underlying compiler.
     */
    protected final CompilationDirective _compilationDirective;

    /**
     * The compiler to use for this compilation.
     */
    protected DynamicCompilerScheme _compiler;

    /**
     * State of this compilation. If {@code true}, then this compilation has finished and the target method is
     * available.
     */
    protected boolean _done;

    protected final Thread _compilingThread;

    Compilation(AdaptiveCompilationScheme adaptiveCompilationScheme, AdaptiveMethodState methodState, CompilationDirective compilationDirective) {
        this._adaptiveCompilationScheme = adaptiveCompilationScheme;
        _methodState = methodState;
        _compilingThread = Thread.currentThread();
        _compilationDirective = compilationDirective;
    }

    /**
     * Cancel this compilation. Ignored.
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    /**
     * Checks whether this compilation was cancelled. This method always returns {@code false}
     */
    public boolean isCancelled() {
        return false;
    }

    /**
     * Returns whether this compilation is done.
     */
    public boolean isDone() {
        return _done;
    }

    /**
     * Gets the result of this compilation, blocking if necessary.
     *
     * @return the target method that resulted from this compilation
     */
    public TargetMethod get() throws InterruptedException {
        if (!_done) {
            synchronized (_methodState) {
                if (_compilingThread == Thread.currentThread()) {
                    FatalError.unexpected("Compilation of " + _methodState.classMethodActor().format("%H.%n(%p)") + " is recursive");
                }

                // the method state object is used here as the condition variable
                _methodState.wait();
                return _methodState.currentTargetMethod(_compilationDirective);
            }
        }
        return _methodState.currentTargetMethod(_compilationDirective);
    }

    /**
     * Gets the result of this compilation, blocking for a maximum amount of time.
     *
     * @return the target method that resulted from this compilation
     */
    public TargetMethod get(long timeout, TimeUnit unit) throws InterruptedException {
        if (!_done) {
            synchronized (_methodState) {
                // the method state object is used here as the condition variable
                _methodState.wait(timeout); // TODO: convert timeout to milliseconds
                return _methodState.currentTargetMethod(_compilationDirective);
            }
        }
        return _methodState.currentTargetMethod(_compilationDirective);
    }

    void compile(CompilationDirective compilationDirective) {
        if (AdaptiveCompilationScheme._gcOnCompileOption.isPresent()) {
            System.gc();
        }
        DynamicCompilerScheme compiler = _compiler;
        if (compiler == null) {
            if (compilationDirective.jitOnly() || compilationDirective.traceInstrument()) {
                assert _methodState.classMethodActor().isUnsafe() == false;
                compiler = this._adaptiveCompilationScheme._jitCompiler;
            } else {
                compiler = this._adaptiveCompilationScheme.selectCompiler(_methodState);
            }
        }
        assert _methodState.currentCompilation(compilationDirective) == this;
        TargetMethod targetMethod = null;

        _adaptiveCompilationScheme.observeBeforeCompilation(this, compiler);

        try {
            String methodString = null;
            if (VerboseVMOption.verboseCompilation()) {
                methodString = _methodState.classMethodActor().format("%H.%n(%p)");
                Log.println(compiler.name() + ": Compiling " + methodString);
            }
            targetMethod = IrTargetMethod.asTargetMethod(compiler.compile(_methodState.classMethodActor(), compilationDirective));
            if (VerboseVMOption.verboseCompilation()) {
                Log.print(compiler.name() + ": Compiled  " + methodString + " @ ");
                Log.print(targetMethod.codeStart());
                Log.print(" {code length=" + targetMethod.codeLength() + "}");
                Log.println();
            }
        } finally {
            _done = true;

            synchronized (_methodState) {
                if (targetMethod != null) {
                    // compilation succeeded and produced a target method
                    _methodState.setTargetMethod(targetMethod, compilationDirective);
                }
                _methodState.setCurrentCompilation(null, compilationDirective);
                _methodState.notifyAll();
            }

            _adaptiveCompilationScheme.observeAfterCompilation(this, compiler, targetMethod);
            synchronized (_adaptiveCompilationScheme._completionLock) {
                _adaptiveCompilationScheme._completed++;
            }
        }
    }
}
