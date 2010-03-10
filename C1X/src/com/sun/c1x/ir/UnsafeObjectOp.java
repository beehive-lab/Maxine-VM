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

/**
 * The {@code UnsafeObjectOp} class is the base of all unsafe object instructions.
 *
 * @author Ben L. Titzer
 */
public abstract class UnsafeObjectOp extends UnsafeOp {

    Value object;
    Value offset;
    final boolean isVolatile;

    /**
     * Creates a new UnsafeObjectOp instruction.
     * @param opKind the kind of the operation
     * @param object the instruction generating the object
     * @param offset the instruction generating the index
     * @param isStore {@code true} if this is a store operation
     * @param isVolatile {@code true} if the operation is volatile
     */
    public UnsafeObjectOp(CiKind opKind, Value object, Value offset, boolean isStore, boolean isVolatile) {
        super(opKind, isStore);
        this.object = object;
        this.offset = offset;
        this.isVolatile = isVolatile;
    }

    /**
     * Gets the instruction that generates the object.
     * @return the instruction that produces the object
     */
    public Value object() {
        return object;
    }

    /**
     * Gets the instruction that generates the offset.
     * @return the instruction generating the offset
     */
    public Value offset() {
        return offset;
    }

    /**
     * Checks whether this is a volatile operation.
     * @return {@code true} if this operation is volatile
     */
    public boolean isVolatile() {
        return isVolatile;
    }

    /**
     * Iterates over the input values of this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        super.inputValuesDo(closure);
        object = closure.apply(object);
        offset = closure.apply(offset);
    }
}
