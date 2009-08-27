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

import com.sun.c1x.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.value.*;

/**
 * The <code>ArithmeticOp</code> class represents arithmetic operations such as addition, subtraction, etc.
 *
 * @author Ben L. Titzer
 */
public class ArithmeticOp extends Op2 {

    ValueStack stateBefore;

    /**
     * Creates a new arithmetic operation.
     * @param opcode the bytecode opcode
     * @param x the first input instruction
     * @param y the second input instruction
     * @param isStrictFP indicates this operation has strict rounding semantics
     * @param stateBefore the value stack for instructions that may trap
     */
    public ArithmeticOp(int opcode, Instruction x, Instruction y, boolean isStrictFP, ValueStack stateBefore) {
        super(x.type().meet(y.type()), opcode, x, y);
        initFlag(Flag.IsStrictFP, isStrictFP);
        if (stateBefore != null) {
            // state before is only used in the case of a division or remainder,
            // and isn't needed if the zero check is redundant
            if (y.isConstant() && y.asConstant().asLong() != 0) {
                C1XMetrics.ZeroChecksRedundant++;
                setFlag(Flag.NoZeroCheck);
            } else {
                this.stateBefore = stateBefore;
            }
        }
    }

    /**
     * Gets the lock stack for this instruction.
     * @return the lock stack
     */
    @Override
    public ValueStack stateBefore() {
        return stateBefore;
    }

    /**
     * Checks whether this instruction has strict fp semantics.
     * @return <code>true</code> if this instruction has strict fp semantics
     */
    public boolean isStrictFP() {
        return checkFlag(Flag.IsStrictFP);
    }

    /**
     * Checks whether this instruction can cause a trap. For arithmetic operations,
     * only division and remainder operations can cause traps.
     * @return <code>true</code> if this instruction can cause a trap
     */
    @Override
    public boolean canTrap() {
        return stateBefore != null;
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitArithmeticOp(this);
    }

    public boolean isCommutative() {
        return Bytecodes.isCommutative(opcode);
    }
}
