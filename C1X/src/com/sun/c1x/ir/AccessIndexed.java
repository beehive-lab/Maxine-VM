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

import com.sun.c1x.util.InstructionClosure;
import com.sun.c1x.value.BasicType;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.value.ValueType;

/**
 * The <code>AccessIndexed</code> class is the base class of instructions that read or write
 * elements of an array.
 *
 * @author Ben L. Titzer
 */
public abstract class AccessIndexed extends AccessArray {

    Instruction _index;
    Instruction _length;
    BasicType _elementType;

    /**
     * Create an new AccessIndexed instruction.
     * @param array the instruction producing the array
     * @param index the instruction producing the index
     * @param length the instruction producing the length (used in bounds check elimination?)
     * @param elementType the type of the elements of the array
     * @param lockStack the lock stack
     */
    AccessIndexed(Instruction array, Instruction index, Instruction length, BasicType elementType, ValueStack lockStack) {
        super(ValueType.fromBasicType(elementType), array, lockStack);
        _index = index;
        _length = length;
        _elementType = elementType;
    }

    /**
     * Gets the instruction producing the index into the array.
     * @return the index
     */
    public Instruction index() {
        return _index;
    }

    /**
     * Gets the instruction that produces the length of the array.
     * @return the length
     */
    public Instruction length() {
        return _length;
    }

    /**
     * Gets the element type of the array.
     * @return the element type
     */
    public BasicType elementType() {
        return _elementType;
    }

    /**
     * Computes whether the instruction requires a range check. The conservative assumption
     * is to always require it.
     * @return <code>true</code> if a range check is required for this instruction
     */
    boolean computeNeedsRangeCheck() {
        return true;
    }

    /**
     * Iterates over the input values of this instruction.
     * @param closure the closure to apply to each of the input values
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        super.inputValuesDo(closure);
        _index = closure.apply(_index);
        _length = closure.apply(_length);
    }
}
