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
import com.sun.c1x.C1XCompilation;

import java.util.List;
import java.util.ArrayList;

/**
 * The <code>IRScope</code> class represents an inlining context in the compilation
 * of a method.
 *
 * @author Ben L. Titzer
 */
public class IRScope {

    public final IRScope caller;
    public final CiMethod method;
    public final int level;

    final C1XCompilation compilation; // XXX: is this necessary?
    final int callerBCI;
    final List<IRScope> callees;

    ValueStack callerState;
    int numberOfLocks;
    BlockBegin start;

    int lockStackSize;

    BitMap storesInLoops;

    public IRScope(C1XCompilation compilation, IRScope caller, int callerBCI, CiMethod method, int osrBCI) {
        this.compilation = compilation;
        this.caller = caller;
        this.callerBCI = callerBCI;
        this.method = method;
        this.level = caller == null ? 0 : 1 + caller.level;
        this.callees = new ArrayList<IRScope>();
    }

    /**
     * Sets the minimum number of locks that are necessary for this context.
     * @param size the number of locks required
     */
    public void setMinimumNumberOfLocks(int size) {
        if (size > numberOfLocks) {
            numberOfLocks = size;
        }
    }

    /**
     * Gets the number of locks in this IR scope.
     * @return the number of locks
     */
    public final int numberOfLocks() {
        return numberOfLocks;
    }

    /**
     * Gets the bytecode index of the callsite that called this method.
     * @return the call site's bytecode index
     */
    public final int callerBCI() {
        return callerBCI;
    }

    /**
     * Gets the value stack at the caller of this scope.
     * @return the value stack at the point of this call
     */
    public final ValueStack callerState() {
        return callerState;
    }

    /**
     * Returns whether this IR scope is the top scope (i.e. has no caller).
     * @return <code>true</code> if this inlining scope has no parent
     */
    public final boolean isTopScope() {
        return caller == null;
    }

    /**
     * Gets the block corresponding to the start of this method.
     * @return the block for the start of this method
     */
    public final BlockBegin start() {
        return start;
    }

    /**
     * Gets the phi bitmap for this IR scope. The phi bitmap stores
     * whether a phi instruction is required for each local variable.
     * @return the phi bitmap for this IR scope
     */
    public final BitMap getStoresInLoops() {
        return storesInLoops;
    }

    /**
     * Checks whether this IR scope is valid (i.e. it has had its start block computed).
     * @return <code>true</code> if this IR scope is valid
     */
    public final boolean isValid() {
        return start != null;
    }

    /**
     * Add a called IRScope to this IRScope's list of callees.
     * @param callee the callee to add
     */
    public final void addCallee(IRScope callee) {
        callees.add(callee);
    }

    /**
     * Sets the caller state for this IRScope.
     * @param callerState the new caller state
     */
    public final void setCallerState(ValueStack callerState) {
        this.callerState = callerState;
    }

    public final void setStoresInLoops(BitMap storesInLoops) {
        this.storesInLoops = storesInLoops;
    }

    /**
     * Gets the number of callees of this IR scope.
     * @return the number of callees
     */
    public final int numberOfCallees() {
        return callees.size();
    }

    /**
     * Gets the callee at the specified position.
     * @param i the index of the callee
     * @return the callee at the specified index
     */
    public final IRScope calleeAt(int i) {
        return callees.get(i);
    }

    @Override
    public String toString() {
        if (caller == null) {
            return "root-scope: " + method;
        } else {
            return "inline-scope @ " + callerBCI + ": " + method;
        }
    }

    /**
     * Gets the caller bytecode index of the top scope.
     * @return the bytecode index of the caller of the top scope
     */
    public final int topScopeBCI() {
        assert caller != null;
        IRScope scope = this;
        while (scope.caller != null) {
            scope = scope.caller;
        }
        return scope.callerBCI;
    }

    public final void computeLockStackSize() {
        if (!C1XOptions.InlineMethodsWithExceptionHandlers) {
            lockStackSize = 0;
            return;
        }
        IRScope curScope = this;
        // TODO: should this calculation be done in ScopeData (because of synchronized handler)?
        while (curScope != null && curScope.method.exceptionHandlers().size() > 0) {
            curScope = curScope.caller;
        }
        lockStackSize = (curScope == null ? 0 :
                          (curScope.callerState() == null ? 0 :
                           curScope.callerState().stackSize()));
    }

    public int lockStackSize() {
        assert lockStackSize >= 0;
        return lockStackSize;
    }
}
