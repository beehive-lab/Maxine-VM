/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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

import com.sun.c1x.util.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;

/**
 * Implements the {@link Bytecodes#LSB} and {@link Bytecodes#MSB} instructions.
 *
 * @author Laurent Daynes
 */
public class SignificantBitOp extends Instruction {
    Value value;

    /**
     * This will be {@link Bytecodes#LSB} or {@link Bytecodes#MSB}.
     */
    public final int op;

    /**
     * Create a a new SignificantBitOp instance.
     *
     * @param value the instruction producing the value that is input to this instruction
     * @param opcodeop either {@link Bytecodes#LSB} or {@link Bytecodes#MSB}
     */
    public SignificantBitOp(Value value, int opcodeop) {
        super(CiKind.Int);
        assert opcodeop == Bytecodes.LSB || opcodeop == Bytecodes.MSB;
        this.value = value;
        this.op = opcodeop;
    }

    /**
     * Gets the instruction producing input to this instruction.
     * @return the instruction that produces this instruction's input
     */
    public Value value() {
        return value;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply to each value
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        value = closure.apply(value);
    }

    @Override
    public void accept(ValueVisitor v) {
        v.visitSignificantBit(this);
    }

    @Override
    public int valueNumber() {
        return Util.hash1(op, value);
    }

    @Override
    public boolean valueEqual(Instruction i) {
        if (i instanceof SignificantBitOp) {
            SignificantBitOp o = (SignificantBitOp) i;
            // FIXME: this is a conservative estimate. If x is a single-bit value
            // (i.e., a power of 2), then the values are equal regardless of the value of the most field.
            return value == o.value && op == o.op;
        }
        return false;
    }
}
