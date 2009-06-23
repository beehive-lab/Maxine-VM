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
import com.sun.c1x.bytecode.Bytecodes;
import com.sun.c1x.value.ValueStack;

/**
 * The <code>ArithmeticOp</code> class represents arithmetic operations such as addition, subtraction, etc.
 *
 * @author Ben L. Titzer
 */
public class ArithmeticOp extends Op2 {

    ValueStack lockStack;

    /**
     * Creates a new arithmetic operation.
     * @param opcode the bytecode opcode
     * @param x the first input instruction
     * @param y the second input instruction
     * @param isStrictFP indicates this operation has strict rounding semantics
     * @param lockStack the value stack for instructions that may trap
     */
    public ArithmeticOp(int opcode, Instruction x, Instruction y, boolean isStrictFP, ValueStack lockStack) {
        super(x.type().meet(y.type()), opcode, x, y);
        initFlag(Flag.IsStrictFP, isStrictFP);
        this.lockStack = lockStack;
        if (canTrap()) {
            pin();
        }
    }

    /**
     * Gets the lock stack for this instruction.
     * @return the lock stack
     */
    public ValueStack lockStack() {
        return lockStack;
    }

    /**
     * Sets the lock stack for this instruction.
     * @param lockStack the lock stack
     */
    public void setLockStack(ValueStack lockStack) {
        this.lockStack = lockStack;
    }

    /**
     * Checks whether this instruction has strict fp semantics.
     * @return <code>true</code> if this instruction has strict fp semantics
     */
    public boolean isStrictFP() {
        return checkFlag(Flag.IsStrictFP);
    }

    /**
     * Iterates over the other values in this instruction.
     * @param closure the closure to apply to each instruction
     */
    @Override
    public void otherValuesDo(InstructionClosure closure) {
        if (lockStack != null) {
            lockStack.valuesDo(closure);
        }
    }

    /**
     * Checks whether this instruction can cause a trap. For arithmetic operations,
     * only division and remainder operations can cause traps.
     * @return <code>true</code> if this instruction can cause a trap
     */
    @Override
    public boolean canTrap() {
        switch (opcode) {
            case Bytecodes.IDIV:
            case Bytecodes.IREM:
            case Bytecodes.LDIV:
            case Bytecodes.LREM:
                return true;
        }
        return false;
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitArithmeticOp(this);
    }
}
