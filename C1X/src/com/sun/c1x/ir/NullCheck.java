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

import com.sun.c1x.bytecode.Bytecodes;
import com.sun.c1x.util.Util;
import com.sun.c1x.value.ValueStack;

/**
 * The <code>NullCheck</code> class represents an explicit null check instruction.
 *
 * @author Ben L. Titzer
 */
public class NullCheck extends Instruction {

    Instruction object;
    ValueStack lockStack;

    /**
     * Constructs a new NullCheck instruction.
     * @param obj the instruction producing the object to check against null
     * @param lockStack the lock stack
     */
    public NullCheck(Instruction obj, ValueStack lockStack) {
        super(obj.type().base());
        this.object = obj;
        this.lockStack = lockStack;
        setFlag(Flag.NonNull);
        setNeedsNullCheck(!obj.isNonNull());
        setFlag(Flag.PinExplicitNullCheck);
    }

    /**
     * Gets the instruction that produces the object tested against null.
     * @return the instruction producing the object
     */
    public Instruction object() {
        return object;
    }

    /**
     * Gets the lock stack.
     * @return the lock stack
     */
    @Override
    public ValueStack lockStack() {
        return lockStack;
    }

    /**
     * Sets whether this instruction requires a null check.
     * @param on {@code true} if this instruction requires a null check
     */
    public void setNeedsNullCheck(boolean on) {
        if (on) {
            assert lockStack != null;
            setFlag(Instruction.Flag.NoNullCheck);
        } else {
            lockStack = null;
            clearFlag(Instruction.Flag.NoNullCheck);
        }
    }

    /**
     * Checks whether this instruction can cause a trap.
     * @return <code>true</code> if this instruction can cause a trap
     */
    @Override
    public boolean canTrap() {
        return needsNullCheck();
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply to each instruction
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        object = closure.apply(object);
    }

    /**
     * Iterates over the other values of this instruction.
     * @param closure the closure to apply to each instruction
     */
    @Override
    public void otherValuesDo(InstructionClosure closure) {
        if (lockStack != null) {
            lockStack.valuesDo(closure);
        }
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitNullCheck(this);
    }

    @Override
    public int valueNumber() {
        return Util.hash1(Bytecodes.IFNONNULL, object);
    }

    @Override
    public boolean valueEqual(Instruction i) {
        if (i instanceof NullCheck) {
            NullCheck o = (NullCheck) i;
            return object == o.object;
        }
        return false;
    }

}
