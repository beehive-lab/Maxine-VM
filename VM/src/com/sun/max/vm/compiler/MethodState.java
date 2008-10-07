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
package com.sun.max.vm.compiler;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;

/**
 * The {@code MethodState} class represents the state of a method with respect to
 * compilation and recompilation, and therefore represents a per-method collection
 * of information that is used by a compiler. This class represents the mapping between
 * a {@link ClassMethodActor} and its associated {@linkplain TargetMethod target method}.
 *
 * This class is intended to be a lightweight collection of state that is in a one-to-one
 * mapping with its associated {@link ClassMethodActor}. This makes it suitable for
 * synchronizing per-method compilation operations such as updating the current version
 * of the target code for a method.
 *
 * @author Ben L. Titzer
 * @author Michael Van De Vanter
 * @author Michael Bebenita
 */
public abstract class MethodState {
    /**
     * The method corresponding to this method state.
     */
    @INSPECTED
    protected final ClassMethodActor _classMethodActor;

    /**
     * Compilations for this method, null beyond {@link #_numberOfCompilations} -1 .
     */
    @INSPECTED
    private TargetMethod[] _targetMethodHistory;

    /**
     * Number of compilations of this method.
     */
    @INSPECTED
    private int _numberOfCompilations = 0;

    protected MethodState(ClassMethodActor classMethodActor, int initialHistoryLength) {
        _classMethodActor = classMethodActor;
        _targetMethodHistory = new TargetMethod[initialHistoryLength];
    }

    /**
     * This method returns the associated {@link ClassMethodActor} for this method state.
     * @return the class method actor corresponding to this method state
     */
    @INLINE
    public final ClassMethodActor classMethodActor() {
        return _classMethodActor;
    }

    /**
     * @return the most recent compilation for this method state, {@code null} if never compiled
     */
    public abstract TargetMethod currentTargetMethod();

    /**
     * @return the most recent compilation for this method state, {@code null} if never compiled
     */
    public abstract TargetMethod currentTargetMethod(CompilationDirective compilationDirective);

    /**
     * @return sequential history of compilations of this method, null for positions beyond {@link #numberOfCompilations()} -1.
     * The identity of the array may change as compilations are added.
     */
    protected final TargetMethod[] targetMethodHistory() {
        return _targetMethodHistory;
    }

    /**
     * @return number of times this method has been compiled.
     */
    protected final int numberOfCompilations() {
        return _numberOfCompilations;
    }

    /**
     * @param newTargetMethod
     * Adds a new compilation to the history and make it the current compilation.
     */
    protected void addTargetMethod(TargetMethod newTargetMethod) {
        if (_numberOfCompilations == _targetMethodHistory.length) {
            // History full; extend, even if length is 0.
            _targetMethodHistory = Arrays.extend(_targetMethodHistory, _targetMethodHistory.length * 2 + 1);
        }
        _numberOfCompilations++;
        _targetMethodHistory[_numberOfCompilations - 1 ] = newTargetMethod;
    }

}
