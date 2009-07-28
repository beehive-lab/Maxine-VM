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

import com.sun.c1x.ci.CiType;
import com.sun.c1x.value.ValueStack;

/**
 * The <code>NewMultiArray</code> instruction represents an allocation of a multi-dimensional object
 * array.
 *
 * @author Ben L. Titzer
 */
public class NewMultiArray extends NewArray {
    final CiType elementType;
    final Instruction[] dimensions;

    /**
     * Constructs a new NewMultiArray instruction.
     * @param elementType the element type of the array
     * @param dimensions the instructions which produce the dimensions for this array
     * @param stateBefore the state before this instruction
     */
    public NewMultiArray(CiType elementType, Instruction[] dimensions, ValueStack stateBefore) {
        super(null, stateBefore); // note that this instruction doesn't have a "length" per-se
        this.elementType = elementType;
        this.dimensions = dimensions;
    }

    /**
     * Gets the element type of this array allocation.
     * @return the array element type
     */
    public CiType elementType() {
        return elementType;
    }

    /**
     * Gets the list of instructions which produce input for this instruction.
     * @return the list of instructions which produce input
     */
    public Instruction[] dimensions() {
        return dimensions;
    }

    /**
     * Gets the rank of the array allocated by this instruction, i.e. how many array dimensions.
     * @return the rank of the array allocated
     */
    public int rank() {
        return dimensions.length;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        for (int i = 0; i < dimensions.length; i++) {
            dimensions[i] = closure.apply(dimensions[i]);
        }
    }
    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitNewMultiArray(this);
    }
}
