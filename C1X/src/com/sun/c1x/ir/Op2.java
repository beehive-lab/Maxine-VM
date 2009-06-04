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

import com.sun.c1x.value.ValueType;
import com.sun.c1x.bytecode.Bytecodes;
import com.sun.c1x.util.InstructionClosure;

/**
 * The <code>Op2</code> class is the base of arithmetic and logic operations with two inputs.
 *
 * @author Ben L. Titzer
 */
public abstract class Op2 extends Instruction {
    final int _opcode;
    Instruction _x;
    Instruction _y;

    /**
     * Creates a new Op2 instance.
     * @param type the result type of this instruction
     * @param opcode the bytecode opcode
     * @param x the first input instruction
     * @param y the second input instruction
     */
    public Op2(ValueType type, int opcode, Instruction x, Instruction y) {
        super(type);
        _opcode = opcode;
        _x = x;
        _y = y;
    }

    /**
     * Gets the opcode of this instruction. See {@link com.sun.c1x.bytecode.Bytecodes}.
     * @return the opcode of this instruction
     */
    public int opcode() {
        return _opcode;
    }

    /**
     * Gets the first input to this instruction.
     * @return the first input to this instruction
     */
    public Instruction x() {
        return _x;
    }

    /**
     * Gets the second input to this instruction.
     * @return the second input to this instruction
     */
    public Instruction y() {
        return _y;
    }

    /**
     * Swaps the operands of this instruction. This is only legal for commutative operations.
     */
    public void swapOperands() {
        assert Bytecodes.isCommutative(_opcode);
        Instruction t = _x;
        _x = _y;
        _y = t;
    }

    /**
     * Iterates over the inputs to this instruction.
     * @param closure the closure to apply to each input value
     */
    public void inputValuesDo(InstructionClosure closure) {
        _x = closure.apply(_x);
        _x = closure.apply(_y);
    }

}
