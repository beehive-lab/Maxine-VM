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
import com.sun.c1x.ci.CiKind;

/**
 * The <code>StorePointer</code> instruction represents a write of a pointer.
 * This instruction is part of the HIR support for low-level operations, such as safepoints,
 * stack banging, etc, and does not correspond to a Java operation.
 *
 * @author Ben L. Titzer
 */
public class StorePointer extends StateSplit {

    Value pointer;
    Value value;
    final boolean isVolatile;
    final boolean canTrap;
    final boolean isPrefetch;

    /**
     * Creates a new LoadPointer instance.
     * @param kind the kind of value stored to the pointer
     * @param pointer the value producing the pointer
     * @param value the value to write to the pointer
     * @param canTrap {@code true} if the access can cause a trap
     * @param stateBefore the state before
     * @param isVolatile {@code true} if the access is volatile
     */
    public StorePointer(CiKind kind, Value pointer, Value value, boolean canTrap, ValueStack stateBefore, boolean isVolatile) {
        super(kind, stateBefore);
        this.pointer = pointer;
        this.value = value;
        this.isVolatile = isVolatile;
        this.isPrefetch = false;
        this.canTrap = canTrap;
        setFlag(Flag.LiveStore);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitStorePointer(this);
    }

    @Override
    public boolean canTrap() {
        return canTrap;
    }

    public Value pointer() {
        return pointer;
    }

    public Value value() {
        return value;
    }

    /**
     * Iterates over the input values to this instruction. In this case,
     * it is only the pointer value.
     * @param closure the closure to apply to each value
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        pointer = closure.apply(pointer);
        value = closure.apply(value);
    }
}
