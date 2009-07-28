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

import com.sun.c1x.value.ValueStack;
import com.sun.c1x.value.ValueType;

/**
 * The <code>NewArray</code> class is the base of all instructions that allocate arrays.
 *
 * @author Ben L. Titzer
 */
public abstract class NewArray extends StateSplit {

    Instruction length;
    final ValueStack stateBefore;

    /**
     * Constructs a new NewArray instruction.
     * @param length the instruction that produces the length for this allocation
     * @param stateBefore the state before the allocation
     */
    NewArray(Instruction length, ValueStack stateBefore) {
        super(ValueType.OBJECT_TYPE);
        this.length = length;
        this.stateBefore = stateBefore;
        setFlag(Flag.NonNull);
    }

    /**
     * Gets the value stack which represents the state before this instruction.
     * @return the state before this instruction
     */
    public ValueStack stateBefore() {
        return stateBefore;
    }

    /**
     * Gets the instruction that produces the length of this array.
     * @return the instruction that produces the length
     */
    public Instruction length() {
        return length;
    }

    /**
     * Checks whether this instruction can trap.
     * @return <true>true</code>, conservatively assuming that this instruction can throw such
     * exceptions as <code>OutOfMemoryError</code>
     */
    @Override
    public boolean canTrap() {
        return true;
    }

    /**
     * Applies the specified closure to all input values of this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        length = closure.apply(length);
    }

    /**
     * Applies the specified closure to all the other input values of this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void otherValuesDo(InstructionClosure closure) {
        super.otherValuesDo(closure);
        if (stateBefore != null) {
            stateBefore.valuesDo(closure);
        }
    }
}
