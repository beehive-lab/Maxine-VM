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

import com.sun.cri.ci.*;

/**
 * The {@code UnsafeRawOp} class is the base class of all unsafe raw operations.
 *
 * @author Ben L. Titzer
 */
public abstract class UnsafeRawOp extends UnsafeOp {

    Value base;
    Value index;
    int log2Scale;

    /**
     * Creates a new UnsafeRawOp instruction.
     * @param opKind the kind of the operation
     * @param addr the instruction generating the base address (a long)
     * @param isStore {@code true} if this operation is a store
     */
    public UnsafeRawOp(CiKind opKind, Value addr, boolean isStore) {
        super(opKind, isStore);
        assert addr == null || addr.kind == CiKind.Long;
        base = addr;
    }

    /**
     * Creates a new UnsafeRawOp instruction.
     * @param opKind the kind of the operation
     * @param addr the instruction generating the base address (a long)
     * @param index the instruction generating the index
     * @param log2scale the log base 2 of the scaling factor
     * @param isStore {@code true} if this operation is a store
     */
    public UnsafeRawOp(CiKind opKind, Value addr, Value index, int log2scale, boolean isStore) {
        this(opKind, addr, isStore);
        this.base = addr;
        this.index = index;
        this.log2Scale = log2scale;
    }

    /**
     * Gets the instruction generating the base address for this operation.
     * @return the instruction generating the base
     */
    public Value base() {
        return base;
    }

    /**
     * Gets the instruction generating the index for this operation.
     * @return the instruction generating the index
     */
    public Value index() {
        return index;
    }

    /**
     * Checks whether this instruction has an index.
     * @return {@code true} if this instruction has an index
     */
    public boolean hasIndex() {
        return index != null;
    }

    /**
     * Gets the log base 2 of the scaling factor for the index of this instruction.
     * @return the log base 2 of the scaling factor
     */
    public int log2Scale() {
        return log2Scale;
    }

    /**
     * Sets the instruction that generates the base address for this instruction.
     * @param base the instruction generating the base address
     */
    public void setBase(Value base) {
        this.base = base;
    }

    /**
     * Sets the instruction generating the base address for this instruction.
     * @param index the instruction generating the index
     */
    public void setIndex(Value index) {
        this.index = index;
    }

    /**
     * Sets the scaling factor for the index of this instruction.
     * @param log2scale the log base 2 of the scaling factor for this instruction
     */
    public void setLog2Scale(int log2scale) {
        this.log2Scale = log2scale;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        super.inputValuesDo(closure);
        base = closure.apply(base);
        if (index != null) {
            index = closure.apply(index);
        }
    }
}
