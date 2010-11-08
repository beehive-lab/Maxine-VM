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

import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

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
    CiCodePos callerCodeSite;

    /**
     * The frame state at the call site of this scope's caller or {@code null}
     * if this is not a nested scope.
     */
    public final FrameState callerState;

    /**
     * The maximum number of locks held in this scope at any one time
     * (c.f. maxStack and maxLocals of the Code attribute in a class file).
     */
    int maxLocks;

    BitMap storesInLoops;

    public IRScope(IRScope caller, FrameState callerState, RiMethod method, int osrBCI) {
        this.caller = caller;
        this.callerState = callerState;
        this.method = method;
        this.level = caller == null ? 0 : 1 + caller.level;
    }

    /**
     * Updates the maximum number of locks held in this scope at any one time.
     *
     * @param locks a lock count that will replace the current {@linkplain #maxLocks() max locks} for this scope if it is greater
     */
    public void updateMaxLocks(int locks) {
        if (locks > maxLocks) {
            maxLocks = locks;
        }
    }

    /**
     * Gets the number of locks in this IR scope.
     * @return the number of locks
     */
    public final int maxLocks() {
        return maxLocks;
    }

    /**
     * Gets the bytecode index of the call site that called this method.
     * @return the call site's bytecode index
     */
    public final int callerBCI() {
        return callerState == null ? -1 : callerState.bci;
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

    public final void setStoresInLoops(BitMap storesInLoops) {
        this.storesInLoops = storesInLoops;
    }

    @Override
    public String toString() {
        if (caller == null) {
            return "root-scope: " + method;
        } else {
            return "inlined-scope: " + method + " [caller bci: " + callerState.bci + "]";
        }
    }

    public CiCodePos callerCodeSite() {
        if (caller != null && callerCodeSite == null) {
            callerCodeSite = caller.toCodeSite(callerBCI());
        }
        return callerCodeSite;
    }

    public CiCodePos toCodeSite(int bci) {
        return new CiCodePos(callerCodeSite(), method, bci);
    }
}
