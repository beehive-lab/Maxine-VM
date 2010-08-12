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
import com.sun.cri.bytecode.*;

/**
 * Atomic update of a value in memory. Implements the {@link Bytecodes#PCMPSWP} family of instructions.
 *
 * Compares a suspected value with the actual value in a memory location.
 * Iff they are same, a new value is placed into the location and the expected value is returned.
 * Otherwise, the actual value is returned.
 *
 * @author Doug Simon
 */
public final class CompareAndSwap extends PointerOp {

    /**
     * The value to store.
     */
    Value expectedValue;

    Value newValue;

    /**
     * Creates an instruction for a pointer store. If {@code displacement != null}, the effective of the address of the store is
     * computed as the pointer plus a byte displacement plus a scaled index. Otherwise, the effective address is computed as the
     * pointer plus a byte offset.
     * @param pointer the value producing the pointer
     * @param offset the value producing the byte offset
     * @param expectedValue the value that must currently being in memory location for the swap to occur
     * @param newValue the new value to store if the precondition is satisfied
     * @param stateBefore the state before
     * @param isVolatile {@code true} if the access is volatile
     */
    public CompareAndSwap(int opcode, Value pointer, Value offset, Value expectedValue, Value newValue, FrameState stateBefore, boolean isVolatile) {
        super(expectedValue.kind, opcode, pointer, null, offset, stateBefore, isVolatile);
        assert offset != null;
        this.expectedValue = expectedValue;
        this.newValue = newValue;
        setFlag(Flag.LiveStore);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitCompareAndSwap(this);
    }

    public Value expectedValue() {
        return expectedValue;
    }

    public Value newValue() {
        return newValue;
    }

    @Override
    public void inputValuesDo(ValueClosure closure) {
        super.inputValuesDo(closure);
        expectedValue = closure.apply(expectedValue);
        newValue = closure.apply(newValue);
    }
}
