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
 * The {@code StorePointer} instruction represents a write of a pointer.
 * This instruction is part of the HIR support for low-level operations, such as safepoints,
 * stack banging, etc, and does not correspond to a Java operation.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public final class StorePointer extends PointerOp {

    Value value;

    /**
     * Creates an instruction for a pointer store. If {@code displacement != null}, the effective of the address of the store is
     * computed as the pointer plus a byte displacement plus a scaled index. Otherwise, the effective address is computed as the
     * pointer plus a byte offset.
     *
     * @param kind the kind of value stored to the pointer
     * @param pointer the value producing the pointer
     * @param displacement the value producing the displacement. This may be {@code null}.
     * @param offsetOrIndex the value producing the scaled-index of the byte offset depending on whether {@code displacement} is {@code null}
     * @param value the value to write to the pointer
     * @param canTrap {@code true} if the access can cause a trap
     * @param stateBefore the state before
     * @param isVolatile {@code true} if the access is volatile
     */
    public StorePointer(CiKind kind, int opcode, Value pointer, Value displacement, Value offsetOrIndex, Value value, boolean canTrap, FrameState stateBefore, boolean isVolatile) {
        super(kind, opcode, pointer, displacement, offsetOrIndex, canTrap, stateBefore, isVolatile);
        this.value = value;
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

    public Value value() {
        return value;
    }

    @Override
    public void inputValuesDo(ValueClosure closure) {
        super.inputValuesDo(closure);
        value = closure.apply(value);
    }
}
