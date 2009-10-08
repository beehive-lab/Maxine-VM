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

import com.sun.c1x.ci.*;

/**
 * The <code>LIROp1</code> class definition. The LIROp1 instruction has only one input operand.
 *
 * @author Marcelo Cintra
 *
 */
public class LIROp1 extends LIRInstruction {


    public enum LIRMoveKind {
        Normal, Volatile, Unaligned
    }

    protected final CiKind type;          // the operand type
    protected final LIRMoveKind moveKind; // flag that indicate the kind of move

    /**
     * Constructs a new LIROp1 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr the first input operand
     * @param result the operand that holds the result of this instruction
     * @param type the basic type of this instruction
     * @param info the object holding information needed to emit debug information
     */
    public LIROp1(LIROpcode opcode, LIROperand opr, LIROperand result, CiKind type, CodeEmitInfo info) {
        super(opcode, result, info, false, null, 0, 0, opr);
        this.type = type;
        this.moveKind = LIRMoveKind.Normal;
        assert isInRange(opcode, LIROpcode.BeginOp1, LIROpcode.EndOp1) : "The " + opcode + " is not a valid LIROp1 opcode";
    }

    /**
     * Constructs a new LIROp1 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr the first input operand
     * @param result the operand that holds the result of this instruction
     * @param type the basic type of this instruction
     */
    public LIROp1(LIROpcode opcode, LIROperand opr, LIROperand result, CiKind type) {
        this(opcode, opr, result, type, null);
    }

    /**
     * Constructs a new LIROp1 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr the first input operand
     * @param result the operand that holds the result of this instruction
     */
    public LIROp1(LIROpcode opcode, LIROperand opr, LIROperand result) {
        this(opcode, opr, result, CiKind.Illegal);
    }

    /**
     * Constructs a new LIROp1 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr the first input operand
     */
    public LIROp1(LIROpcode opcode, LIROperand opr) {
        this(opcode, opr, LIROperandFactory.IllegalLocation);
    }

    /**
     * Constructs a new LIROp1 instruction.
     *
     * @param moveKind the kind of move the instruction represents
     * @param opr the first input operand
     * @param result the operand that holds the result of this instruction
     * @param type the basic type of this instruction
     * @param info the object holding information needed to emit debug information
     */
    public LIROp1(LIRMoveKind moveKind, LIROperand opr, LIROperand result, CiKind type, CodeEmitInfo info) {
        super(LIROpcode.Move, result, info, false, null, 0, 0, opr);
        this.type = type;
        this.moveKind = moveKind;
    }

    /**
     * Constructs a new LIROp1 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr the first input operand
     * @param info the object holding information needed to emit debug information
     */
    public LIROp1(LIROpcode opcode, LIROperand opr, CodeEmitInfo info) {
        super(opcode, LIROperandFactory.IllegalLocation, info, false, null, 0, 0, opr);
        this.type = CiKind.Illegal;
        this.moveKind = LIRMoveKind.Normal;
        assert isInRange(opcode, LIROpcode.BeginOp1, LIROpcode.EndOp1) : "The " + opcode + " is not a valid LIROp1 opcode";
    }

    /**
     * Gets the input operand of this instruction.
     *
     * @return opr the input operand.
     */
    public LIROperand operand() {
        return operand(0);
    }

    /**
     * Gets the basic type of this this instruction.
     *
     * @return type the instruction's type.
     */
    public CiKind type() {
        return type;
    }

    /**
     * Gets the kind of move of this instruction.
     *
     * @return flags the constant that represents the move kind.
     */
    public LIRMoveKind moveKind() {
        assert code == LIROpcode.Move : "The opcode must be of type LIROpcode.Move in LIROp1";
        return moveKind;
    }

    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitOp1(this);
    }

    @Override
    public String name() {
        if (code == LIROpcode.Move) {
            switch (moveKind()) {
                case Normal:
                    return "move";
                case Unaligned:
                    return "unaligned move";
                case Volatile:
                    return "volatile_move";
                default:
                    assert false : "The " + moveKind() + " is not a valid move";
                    return "IllegalOp";
            }
        } else {
            return super.name();
        }
    }

    @Override
    public boolean verify() {
        switch (code) {
            case Move:
                assert (!operand().isIllegal()) && (!result().isIllegal()) : "Operand and result must be valid in a LIROp1 move instruction.";
                break;
            case NullCheck:
                assert operand().isRegister() : "Operand must be a register in a LIROp1 null check instruction.";
                break;
            case Return:
                assert operand().isRegister() || operand().isIllegal() : "Operand must be (register | illegal) in a LIROp1 return instruction.";
                break;
        }
        return true;
    }
}
