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
import com.sun.c1x.util.InstructionClosure;

/**
 * The <code>AccessArray</code> class is the base class of all array operations.
 *
 * @author Ben L. Titzer
 */
public abstract class AccessArray extends Instruction {

    Instruction _array;
    ValueStack _lockStack;

    /**
     * Creates a new AccessArray instruction.
     * @param type the type of the result of this instruction
     * @param array the instruction that produces the array object value
     * @param lockStack the lock stack
     */
    public AccessArray(ValueType type, Instruction array, ValueStack lockStack) {
        super(type);
        _array = array;
        _lockStack = lockStack;
        pin();
    }

    /**
     * Gets the instruction that produces the array object.
     * @return the instruction that produces the array object
     */
    public Instruction array() {
        return _array;
    }

    /**
     * Gets the lock stack.
     * @return the lock stack
     */
    public ValueStack lockStack() {
        return _lockStack;
    }

    /**
     * Sets the lock stack for this instruction.
     * @param lockStack the lock stack
     */
    public void setLockStack(ValueStack lockStack) {
        _lockStack = lockStack;
    }

    /**
     * Checks whether this instruction can cause a trap.
     * @return <code>true</code> if this instruction can cause a trap
     */
    @Override
    public boolean canTrap() {
        return !checkFlag(Flag.NonNull);
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply to each of the input values.
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        _array = closure.apply(_array);
    }

    /**
     * Iterates over the "other" values of this instruction.
     * @param closure the closure to apply to each of the other values
     */
    @Override
    public void otherValuesDo(InstructionClosure closure) {
        if (_lockStack != null) {
            _lockStack.valuesDo(closure);
        }
    }
}
