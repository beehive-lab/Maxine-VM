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

import com.sun.c1x.ci.*;
import com.sun.c1x.value.*;

/**
 * This the base class of all array operations.
 *
 * @author Ben L. Titzer
 */
public abstract class AccessArray extends StateSplit {

    Value array;

    /**
     * Creates a new AccessArray instruction.
     * @param kind the type of the result of this instruction
     * @param array the instruction that produces the array object value
     * @param stateBefore the lock stack
     */
    public AccessArray(CiKind kind, Value array, FrameState stateBefore) {
        super(kind, stateBefore);
        this.array = array;
        if (array.isNonNull()) {
            redundantNullCheck();
        }
    }

    /**
     * Gets the instruction that produces the array object.
     * @return the instruction that produces the array object
     */
    public Value array() {
        return array;
    }

    /**
     * Clears the state associated with a null check.
     * @return {@code true} always
     */
    @Override
    public boolean internalClearNullCheck() {
        if (!needsNullCheck() && !needsBoundsCheck()) {
            stateBefore = null;
        }
        return true;
    }

    /**
     * Checks whether this instruction needs a bounds check.
     * @return {@code true} if a bounds check is needed
     */
    public boolean needsBoundsCheck() {
        return !checkFlag(Flag.NoBoundsCheck);
    }

    /**
     * Checks whether this instruction can cause a trap.
     * @return {@code true} if this instruction can cause a trap
     */
    @Override
    public boolean canTrap() {
        return needsNullCheck();
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply to each of the input values.
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        array = closure.apply(array);
    }
}
