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

import com.sun.c1x.util.InstructionVisitor;
import com.sun.c1x.util.InstructionClosure;
import com.sun.c1x.util.Util;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.bytecode.Bytecodes;

/**
 * The <code>NullCheck</code> class represents an explicit null check instruction.
 *
 * @author Ben L. Titzer
 */
public class NullCheck extends Instruction {

    Instruction _object;
    ValueStack _lockStack;

    /**
     * Constructs a new NullCheck instruction.
     * @param obj the instruction producing the object to check against null
     * @param lockStack the lock stack
     */
    public NullCheck(Instruction obj, ValueStack lockStack) {
        super(obj.type().base());
        _object = obj;
        _lockStack = lockStack;
        setFlag(Flag.CanTrap);
        setFlag(Flag.NonNull);
        pin(PinReason.PinExplicitNullCheck);
    }

    /**
     * Gets the instruction that produces the object tested against null.
     * @return the instruction producing the object
     */
    public Instruction object() {
        return _object;
    }

    /**
     * Gets the lock stack.
     * @return the lock stack
     */
    public ValueStack lockStack() {
        return _lockStack;
    }

    /**
     * Sets the lock stack.
     * @param lockStack the lock stack
     */
    public void setLockStack(ValueStack lockStack) {
        _lockStack = lockStack;
    }

    /**
     * Sets whether this instruction can cause a trap.
     * @param canTrap <code>true</code> if this instruction can cause a trap
     */
    public void setCanTrap(boolean canTrap) {
        setFlag(Flag.CanTrap, canTrap);
    }

    /**
     * Checks whether this instruction can cause a trap.
     * @return <code>true</code> if this instruction can cause a trap
     */
    @Override
    public boolean canTrap() {
        return checkFlag(Flag.CanTrap);
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply to each instruction
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        _object = closure.apply(_object);
    }

    /**
     * Iterates over the other values of this instruction.
     * @param closure the closure to apply to each instruction
     */
    @Override
    public void otherValuesDo(InstructionClosure closure) {
        if (_lockStack != null) {
            _lockStack.valuesDo(closure);
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
        return Util.hash1(Bytecodes.IFNONNULL, _object);
    }

    @Override
    public boolean valueEqual(Instruction i) {
        if (i instanceof NullCheck) {
            NullCheck o = (NullCheck) i;
            return _object == o._object;
        }
        return false;
    }

}
