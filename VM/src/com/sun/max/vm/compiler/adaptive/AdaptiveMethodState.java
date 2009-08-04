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

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.instrument.*;
import com.sun.max.vm.compiler.target.*;

/**
 * The {@code MethodState} class implements a container for a number of pieces of information about a class method
 * actor that are used during compilation and recompilation, including any instrumentation attached to various
 * versions as well as the "current" compiled code.
 *
 * Multiple histories are kept for each {@link CompilationDirective}.
 *
 * @author Ben L. Titzer
 * @author Michael Bebenita
 */
public class AdaptiveMethodState extends MethodState {

    /**
     * The enclosing instance of the compilation scheme.
     */
    private final AdaptiveCompilationScheme adaptiveCompilationScheme;

    protected TargetMethod[][] targetMethods;

    protected MethodInstrumentation methodInstrumentation;
    protected Compilation [] currentCompilations;

    protected AdaptiveMethodState(AdaptiveCompilationScheme adaptiveCompilationScheme, ClassMethodActor classMethodActor) {
        super(classMethodActor, AdaptiveCompilationScheme.DEFAULT_HISTORY_LENGTH);
        this.adaptiveCompilationScheme = adaptiveCompilationScheme;
        targetMethods = new TargetMethod[CompilationDirective.count()][];
        currentCompilations = new Compilation[CompilationDirective.count()];
    }

    /**
     * Constructs the method instrumentation for this method state, if it does not exist already.
     *
     * @return the method instrumentation for this method state
     */
    public synchronized MethodInstrumentation makeMethodInstrumentation() {
        if (methodInstrumentation == null) {
            methodInstrumentation = new MethodInstrumentation(classMethodActor);
        }
        return methodInstrumentation;
    }

    /**
     * Gets the method instrumentation for this method state.
     *
     * @return the method instrumentation if it exists; {@code null} if it does not exist
     */
    public MethodInstrumentation getMethodInstrumentation() {
        return methodInstrumentation;
    }

    /**
     * Gets the currently pending compilation for this method state, if any.
     *
     * @return any currently pending compilation
     */
    @INLINE
    final Compilation currentCompilation(CompilationDirective compilationDirective) {
        return currentCompilations[compilationDirective.ordinal()];
    }

    public void setCurrentCompilation(Compilation compilation, CompilationDirective compilationDirective) {
        currentCompilations[compilationDirective.ordinal()] = compilation;
    }

    /**
     * Update this method state to a new compiled version. If the current target method is {@code null} (e.g. this
     * method has never been compiled), then the update takes effect immediately. If there is already a target
     * method, then the compilation scheme will decide if the new code is a valid update, and if so, perform the
     * necessary code patching to forward calls to the old target method to the new one.
     *
     * @param newTargetMethod the new target method
     */
    public final void setTargetMethod(TargetMethod newTargetMethod, CompilationDirective compilationDirective) {
        final TargetMethod currentTargetMethod = currentTargetMethod(compilationDirective);
        if (currentTargetMethod == null) {
            // no previous code for this method and this compilation directive
            addTargetMethod(newTargetMethod, compilationDirective);
        } else if (currentTargetMethod == newTargetMethod) {
            // nothing to be done.
        } else if (this.adaptiveCompilationScheme.allowUpdate(this, newTargetMethod)) {
            // the method update was allowed, perform code update on previous compilations
            // we only update target methods that were compiled with promotable compilation directives
            for (CompilationDirective promotableDirective : compilationDirective.promotableFrom()) {
                final TargetMethod[] methodHistory = methodHistory(promotableDirective);
                if (methodHistory != null) {
                    for (int i = 0; i < methodHistory.length; i++) {
                        Code.updateTargetMethod(methodHistory[i], newTargetMethod);
                    }
                }
            }
            addTargetMethod(newTargetMethod, compilationDirective);
        } else {
            Code.discardTargetMethod(newTargetMethod);
        }
    }

    private TargetMethod[] methodHistory(CompilationDirective compilationDirective) {
        final TargetMethod[] methodHistory = targetMethods[compilationDirective.ordinal()];
        return methodHistory;
    }

    @Override
    public TargetMethod currentTargetMethod(CompilationDirective compilationDirective) {
        final TargetMethod[] methodHistory = methodHistory(compilationDirective);
        if (methodHistory != null) {
            return methodHistory[methodHistory.length - 1];
        }
        return null;
    }

    private void addTargetMethod(TargetMethod targetMethod, CompilationDirective compilationDirective) {
        TargetMethod[] methodHistory = methodHistory(compilationDirective);
        if (methodHistory != null) {
            methodHistory = Arrays.extend(methodHistory, methodHistory.length + 1);
        } else {
            methodHistory = new TargetMethod[1];
        }
        methodHistory[methodHistory.length - 1] = targetMethod;
        targetMethods[compilationDirective.ordinal()] = methodHistory;
        // TODO: No need to keep track of method history in two places, remove it from the super class.
        // For now we leave it in to make the inspector happy.
        addTargetMethod(targetMethod);
    }

    public final TargetMethod jittedTargetMethod() {
        return currentTargetMethod(CompilationDirective.JIT);
    }

    public final TargetMethod tracedTargetMethod() {
        return currentTargetMethod(CompilationDirective.TRACE_JIT);
    }

    public final boolean isJitted() {
        return jittedTargetMethod() != null;
    }

    public final boolean isTraced() {
        return tracedTargetMethod() != null;
    }

    @Override
    public TargetMethod currentTargetMethod() {
        final TargetMethod targetMethod = currentTargetMethod(CompilationDirective.DEFAULT);
        if (targetMethod != null) {
            return targetMethod;
        }
        return currentTargetMethod(CompilationDirective.JIT);
    }
}
