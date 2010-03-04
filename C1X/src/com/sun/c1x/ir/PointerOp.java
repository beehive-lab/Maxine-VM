/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
 * The base class for pointer access operations.
 *
 * @author Doug Simon
 */
public abstract class PointerOp extends StateSplit {

    public final int opcode;
    protected Value pointer;
    protected Value displacement;
    protected Value offsetOrIndex;
    protected final boolean isVolatile;
    protected final boolean canTrap;
    final boolean isPrefetch;

    /**
     * Creates an instruction for a pointer operation. If {@code displacement != null}, the effective of the address of the operation is
     * computed as the pointer plus a byte displacement plus a scaled index. Otherwise, the effective address is computed as the
     * pointer plus a byte offset.
     *
     * @param kind the kind of value at the address accessed by the pointer operation
     * @param opcode the opcode of the instruction
     * @param pointer the value producing the pointer
     * @param displacement the value producing the displacement. This may be {@code null}.
     * @param offsetOrIndex the value producing the scaled-index of the byte offset depending on whether {@code displacement} is {@code null}
     * @param canTrap {@code true} if the access can cause a trap
     * @param stateBefore the state before
     * @param isVolatile {@code true} if the access is volatile
     */
    public PointerOp(CiKind kind, int opcode, Value pointer, Value displacement, Value offsetOrIndex, boolean canTrap, ValueStack stateBefore, boolean isVolatile) {
        super(kind, stateBefore);
        this.opcode = opcode;
        this.pointer = pointer;
        this.displacement = displacement;
        this.offsetOrIndex = offsetOrIndex;
        this.isVolatile = isVolatile;
        this.canTrap = canTrap;
        this.isPrefetch = false;
    }

    @Override
    public boolean canTrap() {
        return canTrap;
    }

    public Value pointer() {
        return pointer;
    }

    public Value index() {
        return offsetOrIndex;
    }

    public Value offset() {
        return offsetOrIndex;
    }

    public Value displacement() {
        return displacement;
    }

    /**
     * Iterates over the input values to this instruction. In this case,
     * it is only the pointer value.
     * @param closure the closure to apply to each value
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        pointer = closure.apply(pointer);
        offsetOrIndex = closure.apply(offsetOrIndex);
        if (displacement != null) {
            displacement = closure.apply(displacement);
        }
    }
}
