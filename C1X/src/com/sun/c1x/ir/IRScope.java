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
package com.sun.c1x.ir;

import com.sun.c1x.ci.CiMethod;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.util.BitMap;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.Compilation;

import java.util.List;
import java.util.ArrayList;

/**
 * The <code>IRScope</code> class represents an inlining context in the compilation
 * of a method.
 *
 * @author Ben L. Titzer
 */
public class IRScope {

    final Compilation _compilation; // XXX: is this necessary?
    final IRScope _caller;
    final int _callerBCI;
    ValueStack _callerState;
    final int _level;
    final CiMethod _method;
    final List<IRScope> _callees;

    List<ExceptionHandler> _exceptionHandlers;
    int _numberOfLocks;
    BlockBegin _start;

    int _lockStackSize;

    BitMap _storesInLoops;

    public IRScope(Compilation compilation, IRScope caller, int callerBCI, CiMethod method, int osrBCI) {
        _compilation = compilation;
        _caller = caller;
        _callerBCI = callerBCI;
        _method = method;
        _level = _caller == null ? 0 : 1 + _caller._level;
        _callees = new ArrayList<IRScope>();
    }

    /**
     * Sets the minimum number of locks that are necessary for this context.
     * @param size the number of locks required
     */
    public void setMinimumNumberOfLocks(int size) {
        if (size > _numberOfLocks) {
            _numberOfLocks = size;
        }
    }

    /**
     * Gets the number of locks in this IR scope.
     * @return the number of locks
     */
    public final int numberOfLocks() {
        return _numberOfLocks;
    }

    /**
     * Gets the IR scope of the calling method; <code>null</code> if this
     * scope is not inlined.
     * @return the IR scope of the calling method
     */
    public final IRScope caller() {
        return _caller;
    }

    /**
     * Gets the bytecode index of the callsite that called this method.
     * @return the call site's bytecode index
     */
    public final int callerBCI() {
        return _callerBCI;
    }

    /**
     * Gets the value stack at the caller of this scope.
     * @return the value stack at the point of this call
     */
    public final ValueStack callerState() {
        return _callerState;
    }

    /**
     * Gets the method for this IR scope.
     * @return the method
     */
    public final CiMethod method() {
        return _method;
    }

    /**
     * Gets the inlining level of this IR scope.
     * @return the inlining level
     */
    public final int level() {
        return _level;
    }

    /**
     * Returns whether this IR scope is the top scope (i.e. has no caller).
     * @return <code>true</code> if this inlining scope has no parent
     */
    public final boolean isTopScope() {
        return _caller == null;
    }

    /**
     * Gets the block corresponding to the start of this method.
     * @return the block for the start of this method
     */
    public final BlockBegin start() {
        return _start;
    }

    /**
     * Gets the list of exception handlers for this scope.
     * @return the list of exception handlers
     */
    public List<ExceptionHandler> exceptionHandlers() {
        return _exceptionHandlers;
    }

    /**
     * Gets the phi bitmap for this IR scope. The phi bitmap stores
     * whether a phi instruction is required for each local variable.
     * @return the phi bitmap for this IR scope
     */
    public final BitMap getStoresInLoops() {
        return _storesInLoops;
    }

    /**
     * Checks whether this IR scope is valid (i.e. it has had its start block computed).
     * @return <code>true</code> if this IR scope is valid
     */
    public final boolean isValid() {
        return _start != null;
    }

    /**
     * Add a called IRScope to this IRScope's list of callees.
     * @param callee the callee to add
     */
    public final void addCallee(IRScope callee) {
        _callees.add(callee);
    }

    /**
     * Sets the caller state for this IRScope.
     * @param callerState the new caller state
     */
    public final void setCallerState(ValueStack callerState) {
        _callerState = callerState;
    }

    public final void setStoresInLoops(BitMap storesInLoops) {
        _storesInLoops = storesInLoops;
    }

    /**
     * Gets the number of callees of this IR scope.
     * @return the number of callees
     */
    public final int numberOfCallees() {
        return _callees.size();
    }

    /**
     * Gets the callee at the specified position.
     * @param i the index of the callee
     * @return the callee at the specified index
     */
    public final IRScope calleeAt(int i) {
        return _callees.get(i);
    }

    /**
     * Gets the caller bytecode index of the top scope.
     * @return the bytecode index of the caller of the top scope
     */
    public final int topScopeBCI() {
        assert _caller != null;
        IRScope scope = this;
        while (scope._caller != null) {
            scope = scope._caller;
        }
        return scope._callerBCI;
    }

    public final void computeLockStackSize() {
        if (!C1XOptions.InlineMethodsWithExceptionHandlers) {
            _lockStackSize = 0;
            return;
        }
        IRScope curScope = this;
        while (curScope != null && curScope._exceptionHandlers.size() > 0) {
            curScope = curScope.caller();
        }
        _lockStackSize = (curScope == null ? 0 :
                          (curScope.callerState() == null ? 0 :
                           curScope.callerState().stackSize()));
    }

    public int lockStackSize() {
        assert _lockStackSize >= 0;
        return _lockStackSize;
    }
}
