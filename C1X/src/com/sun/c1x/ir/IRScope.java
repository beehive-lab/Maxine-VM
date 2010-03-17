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

import com.sun.c1x.*;
import com.sun.c1x.ci.CiCodePos;
import com.sun.c1x.ri.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The {@code IRScope} class represents an inlining context in the compilation
 * of a method.
 *
 * @author Ben L. Titzer
 */
public class IRScope {

    public final IRScope caller;
    public final RiMethod method;
    public final int level;
    public final C1XCompilation compilation; // TODO: remove this field

    final int callerBCI;
    CiCodePos callerCodeSite;

    ValueStack callerState;
    int numberOfLocks;

    int lockStackSize;

    BitMap storesInLoops;

    public IRScope(C1XCompilation compilation, IRScope caller, int callerBCI, RiMethod method, int osrBCI) {
        this.compilation = compilation;
        this.caller = caller;
        this.callerBCI = callerBCI;
        this.method = method;
        this.level = caller == null ? 0 : 1 + caller.level;
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
     * @return {@code true} if this inlining scope has no parent
     */
    public final boolean isTopScope() {
        return caller == null;
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
     * Sets the caller state for this IRScope.
     * @param callerState the new caller state
     */
    public final void setCallerState(ValueStack callerState) {
        this.callerState = callerState;
    }

    public final void setStoresInLoops(BitMap storesInLoops) {
        this.storesInLoops = storesInLoops;
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
     * Computes the size of the lock stack and saves it in a field of this scope.
     */
    public final void computeLockStackSize() {
        if (!C1XOptions.OptInlineExcept) {
            lockStackSize = 0;
            return;
        }
        IRScope curScope = this;
        // TODO: should this calculation be done in ScopeData (because of synchronized handler)?
        while (curScope != null && curScope.method.exceptionHandlers().size() > 0) {
            curScope = curScope.caller;
        }
        lockStackSize = curScope == null ? 0 : curScope.callerState() == null ? 0 : curScope.callerState().stackSize();
    }

    /**
     * Gets the lock stack size. The method {@link #computeLockStackSize()} has to be called for this value to be valid.
     * @return the lock stack size.
     */
    public int lockStackSize() {
        assert lockStackSize >= 0;
        return lockStackSize;
    }

    public CiCodePos callerCodeSite() {
        if (caller != null && callerCodeSite == null) {
            callerCodeSite = caller.toCodeSite(callerBCI);
        }
        return callerCodeSite;
    }

    public CiCodePos toCodeSite(int bci) {
        return new CiCodePos(callerCodeSite(), method, bci);
    }
}
