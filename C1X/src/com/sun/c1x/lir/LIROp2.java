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
package com.sun.c1x.lir;

import com.sun.c1x.util.*;
import com.sun.c1x.value.*;


/**
 * The <code>LIROp2</code> class represents a LIR instruction that performs an operation on two operands.
 *
 * @author Marcelo Cintra
 *
 */
public class LIROp2 extends LIRInstruction {

    private int fpuStackSize;     // used for sin/cos implementation on Intel
    protected LIROperand opr1;
    LIROperand opr2;
    BasicType type;
    LIROperand tmp;
    LIRCondition condition;

    /**
     * Constructs a new LIROp2 instruction.
     *
     * @param opcode the instruction's opcode
     * @param condition the instruction's condition
     * @param opr1 the first input operand
     * @param opr2 the second input operand
     * @param info the object holding information needed to emit debug information
     */
    public LIROp2(LIROpcode opcode, LIRCondition condition, LIROperand opr1, LIROperand opr2, CodeEmitInfo info) {
        super(opcode, LIROperandFactory.illegalOperand, info);
        this.opr1 = opr1;
        this.opr2 = opr2;
        this.type = BasicType.Illegal;
        this.condition = condition;
        this.fpuStackSize = 0;
        this.tmp = LIROperandFactory.illegalOperand;
        assert opcode == LIROpcode.Cmp : "Instruction opcode should be of type LIROpcode.Cmp";
    }

    /**
     * Constructs a new LIROp2 instruction.
     *
     * @param opcode the instruction's opcode
     * @param condition the instruction's condition
     * @param opr1 the first input operand
     * @param opr2 the second input operand
     * @param result the operand that holds the result of this instruction
     */
    public LIROp2(LIROpcode opcode, LIRCondition condition, LIROperand opr1, LIROperand opr2, LIROperand result) {
        super(opcode, result, null);
        this.opr1 = opr1;
        this.opr2 = opr2;
        this.type = BasicType.Illegal;
        this.condition = condition;
        this.fpuStackSize = 0;
        this.tmp = LIROperandFactory.illegalOperand;
        assert opcode == LIROpcode.Cmove : "Instruction opcode should be of type LIROpcode.Cmove";
    }

    /**
     * Constructs a new LIROp2 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr1 the first input operand
     * @param opr2 the second input operand
     * @param result the operand that holds the result of this instruction
     * @param info the object holding information needed to emit debug information
     * @param type
     */
    public LIROp2(LIROpcode opcode, LIROperand opr1, LIROperand opr2, LIROperand result, CodeEmitInfo info, BasicType type) {
        super(opcode, result, info);
        this.opr1 = opr1;
        this.opr2 = opr2;
        this.type = type;
        this.condition = LIRCondition.Unknown;
        this.fpuStackSize = 0;
        this.tmp = LIROperandFactory.illegalOperand;
        assert opcode != LIROpcode.Cmp && isInRange(opcode, LIROpcode.BeginOp2, LIROpcode.EndOp2) : "The " + opcode + " is not a valid LIROp2 opcode";
    }

    /**
     * Constructs a new LIROp2 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr1 the instruction's first operand
     * @param opr2 the instruction's second operand
     * @param result the operand that holds the result of this instruction
     * @param info the object holding information needed to emit debug information
     */
    public LIROp2(LIROpcode opcode, LIROperand opr1, LIROperand opr2, LIROperand result, CodeEmitInfo info) {
        this(opcode, opr1, opr2, result, info, BasicType.Illegal);
    }

    /**
     * Constructs a new LIROp2 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr1 the instruction's first operand
     * @param opr2 the instruction's second operand
     * @param result the operand that holds the result of this instruction
     */
    public LIROp2(LIROpcode opcode, LIROperand opr1, LIROperand opr2, LIROperand result) {
        this(opcode, opr1, opr2, result, (CodeEmitInfo) null);
    }

    /**
     * Constructs a new LIROp2 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr1 the first input operand
     * @param opr2 the instruction's second operand
     */
    public LIROp2(LIROpcode opcode, LIROperand opr1, LIROperand opr2) {
        this(opcode, opr1, opr2, LIROperandFactory.illegalOperand);
    }

    /**
     * Constructs a new LIROp2 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr1 the first input operand
     * @param opr2 the second input operand
     * @param result the operand that holds the result of this instruction
     * @param tmp the temporary operand used by this instruction
     */
    public LIROp2(LIROpcode opcode, LIROperand opr1, LIROperand opr2, LIROperand result, LIROperand tmp) {
        super(opcode, result, null);
        this.opr1 = opr1;
        this.opr2 = opr2;
        this.type = BasicType.Illegal;
        this.condition = LIRCondition.Unknown;
        this.fpuStackSize = 0;
        this.tmp = tmp;
        assert opcode != LIROpcode.Cmp && isInRange(opcode, LIROpcode.BeginOp2, LIROpcode.EndOp2) : "The " + opcode + " is not a valid LIROp2 opcode";
    }

    /**
     * Gets the first input operand.
     *
     * @return opr1 the first input operand
     */
    public LIROperand opr1() {
        return opr1;
    }

    /**
     * Gets the second input operand.
     *
     * @return opr2 the second input operand
     */
    public LIROperand opr2() {
        return opr2;
    }

    /**
     * Gets the resulting type of this instruction.
     *
     * @return type the resulting type
     *
     */
    public BasicType type() {
        return type;
    }

    /**
     * Gets the temporary operand of this instruction.
     *
     * @return tmp the temporary operand of this instruction
     *
     */
    public LIROperand tmp() {
        return tmp;
    }

    /**
     * Gets the condition of this instruction, if it is a Cmp or Cmove LIR instruction
     * .
     * @return condition the condition of this instruction
     */
    public LIRCondition condition() {
        assert code() == LIROpcode.Cmp || code() == LIROpcode.Cmove : "Field access only valid for cmp and cmove";
        return condition;
    }

    public void setFpuStackSize(int fpuStackSize) {
        this.fpuStackSize = fpuStackSize;
    }

    public int fpuStackSize() {
        return fpuStackSize;
    }

    public void setOpr1(LIROperand opr1) {
        this.opr1 = opr1;
    }

    public void setOpr2(LIROperand opr2) {
        this.opr2 = opr2;
    }

    /**
     * Emit target assembly code for this instruction.
     *
     * @param masm the target assembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitOp2(this);
    }

    /**
     * Prints this instruction.
     *
     * @param out the output log stream.
     */
    @Override
    public void printInstruction(LogStream out) {
        if (code() == LIROpcode.Cmove) {
            printCondition(out, condition());
            out.print(" ");
        }
        opr1().print(out);
        out.print(" ");
        opr2().print(out);
        out.print(" ");
        if (tmp().isValid()) {
            tmp().print(out);
            out.print(" ");
        }
        result().print(out);
    }

    public LIROperand inOpr1() {
        return opr1();
    }

    public LIROperand inOpr2() {
        return opr2();
    }

    public LIROperand resultOpr() {
        return result();
    }

    public LIROperand tmpOpr() {
        return tmp();
    }
}

