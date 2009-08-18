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
 * The <code>StoreField</code> instruction represents a write to a static or instance field.
 *
 * @author Ben L. Titzer
 */
public class StoreField extends AccessField {
    Instruction value;

    /**
     * Creates a new LoadField instance.
     * @param object the receiver object
     * @param field the compiler interface field
     * @param value the instruction representing the value to store to the field
     * @param isStatic indicates if the field is static
     * @param lockStack the lock stack
     * @param stateBefore the state before the field access
     * @param isLoaded indicates if the class is loaded
     */
    public StoreField(Instruction object, RiField field, Instruction value, boolean isStatic, ValueStack lockStack, ValueStack stateBefore, boolean isLoaded) {
        super(object, field, isStatic, lockStack, stateBefore, isLoaded);
        this.value = value;
        setFlag(Flag.NoWriteBarrier);
    }

    /**
     * Gets the value that is written to the field.
     * @return the value
     */
    public Instruction value() {
        return value;
    }

    /**
     * Checks whether this instruction needs a write barrier.
     * @return <code>true</code> if this instruction needs a write barrier
     */
    public boolean needsWriteBarrier() {
        return checkFlag(Flag.NoWriteBarrier);
    }

    /**
     * Iterates over the input values to this instruction. This implementation applies the closure
     * to the object receiver value and the written value.
     * @param closure the closure to apply to each value
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        super.inputValuesDo(closure);
        value = closure.apply(value);
    }

    /**
     * Implements this instruction's half of the visitor interface.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitStoreField(this);
    }
}
