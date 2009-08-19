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

import com.sun.c1x.value.*;

/**
 * The <code>StoreIndexed</code> instruction represents a write to an array element.
 *
 * @author Ben L. Titzer
 */
public class StoreIndexed extends AccessIndexed {

    Instruction value;

    /**
     * Creates a new StoreIndexed instruction.
     * @param array the instruction producing the array
     * @param index the instruction producing the index
     * @param length the instruction producing the length
     * @param elementType the element type
     * @param value the value to store into the array
     * @param stateBefore the state before executing this instruction
     */
    public StoreIndexed(Instruction array, Instruction index, Instruction length, BasicType elementType, Instruction value, ValueStack stateBefore) {
        super(array, index, length, elementType, stateBefore);
        this.value = value;
    }

    /**
     * Gets the instruction that produces the value that is to be stored into the array.
     * @return the value to write into the array
     */
    public Instruction value() {
        return value;
    }

    /**
     * Checks if this instruction needs a write barrier.
     * @return <code>true</code> if this instruction needs a write barrier
     */
    public boolean needsWriteBarrier() {
        return !checkFlag(Flag.NoWriteBarrier);
    }

    /**
     * Checks if this instruction needs a store check.
     * @return <code>true</code> if this instruction needs a store check
     */
    public boolean needsStoreCheck() {
        return !checkFlag(Flag.NoStoreCheck);
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply to each instruction
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        super.inputValuesDo(closure);
        value = closure.apply(value);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitStoreIndexed(this);
    }
}
