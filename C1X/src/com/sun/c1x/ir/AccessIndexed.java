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
 * The {@code AccessIndexed} class is the base class of instructions that read or write
 * elements of an array.
 *
 * @author Ben L. Titzer
 */
public abstract class AccessIndexed extends AccessArray {

    Value index;
    Value length;
    final CiKind elementType;

    /**
     * Create an new AccessIndexed instruction.
     * @param array the instruction producing the array
     * @param index the instruction producing the index
     * @param length the instruction producing the length (used in bounds check elimination?)
     * @param elementType the type of the elements of the array
     * @param stateBefore the state before executing this instruction
     */
    AccessIndexed(Value array, Value index, Value length, CiKind elementType, FrameState stateBefore) {
        super(elementType.stackKind(), array, stateBefore);
        this.index = index;
        this.length = length;
        this.elementType = elementType;
    }

    /**
     * Gets the instruction producing the index into the array.
     * @return the index
     */
    public Value index() {
        return index;
    }

    /**
     * Gets the instruction that produces the length of the array.
     * @return the length
     */
    public Value length() {
        return length;
    }

    /**
     * Gets the element type of the array.
     * @return the element type
     */
    public CiKind elementKind() {
        return elementType;
    }

    /**
     * Computes whether the instruction requires a range check.
     * @return {@code true} if a range check is required for this instruction
     */
    public boolean needsRangeCheck() {
        return !checkFlag(Value.Flag.NoBoundsCheck);
    }

    /**
     * Iterates over the input values of this instruction.
     * @param closure the closure to apply to each of the input values
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        super.inputValuesDo(closure);
        index = closure.apply(index);
        if (length != null) {
            length = closure.apply(length);
        }
    }
}
