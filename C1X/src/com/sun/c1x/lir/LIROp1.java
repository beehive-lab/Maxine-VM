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

import com.sun.c1x.debug.LogStream;
import com.sun.c1x.util.Util;
import com.sun.c1x.value.BasicType;

/**
 * The <code>LIROp1</code> class definition. The LIROp1 instruction has only one input operand.
 *
 * @author Marcelo Cintra
 *
 */
public class LIROp1 extends LIRInstruction {

    protected LIROperand opr; // the input operand
    protected BasicType type; // the operand type
    protected LIRPatchCode patch; // only required for patching (TODO:NEEDS_CLEANUP: do we want a special instruction
                                  // for patching?)

    protected static void printPatchCode(LogStream out, LIRPatchCode code) {
        switch (code) {
            case PatchNone:
                break;
            case PatchLow:
                out.print("[PatchLow]");
                break;
            case PatchHigh:
                out.print("[PatchHigh]");
                break;
            case PatchNormal:
                out.print("[PatchNormal]");
                break;
            default:
                Util.shouldNotReachHere();
        }
    }

    /**
     * Sets the move kind for this instruction.
     *
     * @param kind the kind
     */
    void setKind(LIRMoveKind kind) {
        assert code == LIROpcode.Move : "Instruction opcode must be of kind LIROpcode.Move";
        flags = kind;
    }

    /**
     * Constructs a new LIROp1 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr the first input operand
     * @param result the operand that holds the result of this instruction
     * @param type the basic type of this instruction
     * @param patch the patching code for this instruction
     * @param info the object holding information needed to emit debug information
     */
    public LIROp1(LIROpcode opcode, LIROperand opr, LIROperand result, BasicType type, LIRPatchCode patch, CodeEmitInfo info) {
        super(opcode, result, info);
        this.opr = opr;
        this.type = type;
        this.patch = patch;
        assert isInRange(opcode, LIROpcode.BeginOp1, LIROpcode.EndOp1) : "The " + opcode + " is not a valid LIROp1 opcode";
    }

    /**
     * Constructs a new LIROp1 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr the first input operand
     * @param result the operand that holds the result of this instruction
     * @param type the basic type of this instruction
     * @param patch the patching code for this instruction
     */
    public LIROp1(LIROpcode opcode, LIROperand opr, LIROperand result, BasicType type, LIRPatchCode patch) {
        this(opcode, opr, result, type, patch, null);
    }

    /**
     * Constructs a new LIROp1 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr the first input operand
     * @param result the operand that holds the result of this instruction
     * @param type the basic type of this instruction
     */
    public LIROp1(LIROpcode opcode, LIROperand opr, LIROperand result, BasicType type) {
        this(opcode, opr, result, type, LIRPatchCode.PatchNone);
    }

    /**
     * Constructs a new LIROp1 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr the first input operand
     * @param result the operand that holds the result of this instruction
     */
    public LIROp1(LIROpcode opcode, LIROperand opr, LIROperand result) {
        this(opcode, opr, result, BasicType.Illegal);
    }

    /**
     * Constructs a new LIROp1 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr the first input operand
     * @param result the operand that holds the result of this instruction
     */
    public LIROp1(LIROpcode opcode, LIROperand opr) {
        this(opcode, opr, LIROperandFactory.IllegalOperand);
    }

    /**
     * Constructs a new LIROp1 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr the first input operand
     * @param result the operand that holds the result of this instruction
     * @param type the basic type of this instruction
     * @param patch the patching code for this instruction
     * @param info the object holding information needed to emit debug information
     * @param unaligned the kind of move the instruction represents
     */
    public LIROp1(LIROpcode opcode, LIROperand opr, LIROperand result, BasicType type, LIRPatchCode patch, CodeEmitInfo info, LIRMoveKind unaligned) {
        super(opcode, result, info);
        this.opr = opr;
        this.type = type;
        this.patch = patch;
        assert opcode == LIROpcode.Move : "The " + opcode + " is not valid on LIROp1. Opcode must be of type LIROpcode.Move";
        setKind(unaligned);
    }

    /**
     * Constructs a new LIROp1 instruction.
     *
     * @param opcode the instruction's opcode
     * @param opr the first input operand
     * @param result the operand that holds the result of this instruction
     * @param type the basic type of this instruction
     * @param patch the patching code for this instruction
     * @param info the object holding information needed to emit debug information
     * @param kind the kind of move the instruction represents
     */
    public LIROp1(LIROpcode opcode, LIROperand opr, CodeEmitInfo info) {
        super(opcode, LIROperandFactory.IllegalOperand, info);
        this.opr = opr;
        this.type = BasicType.Illegal;
        this.patch = LIRPatchCode.PatchNone;
        assert isInRange(opcode, LIROpcode.BeginOp1, LIROpcode.EndOp1) : "The " + opcode + " is not a valid LIROp1 opcode";
    }

    /**
     * Gets the input operand of this instruction.
     *
     * @return opr the input operand.
     */
    public LIROperand operand() {
        return opr;
    }

    /**
     * Gets patch code of this instruction.
     *
     * @return patch the patch code for this instruction.
     */
    public LIRPatchCode patchCode() {
        return patch;
    }

    /**
     * Gets the basic type of this this instruction.
     *
     * @return type the instruction's type.
     */
    public BasicType type() {
        return type;
    }

    /**
     * Gets the kind of move of this instruction.
     *
     * @return flags the constant that represents the move kind.
     */
    public LIRMoveKind moveKind() {
        assert code == LIROpcode.Move : "The opcode must be of type LIROpcode.Move in LIROp1";
        return flags;
    }

    /**
     * Gets the kind of move of this instruction.
     *
     * @return flags the constant that represents the move kind.
     */
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

    /**
     * Sets the input operand of this instruction.
     *
     * @param opr the input operand.
     */
    public void setOperand(LIROperand opr) {
        this.opr = opr;
    }

    @Override
    public void printInstruction(LogStream out) {
        opr.print(out);
        out.print(" ");
        this.result().print(out);
        out.print(" ");
        printPatchCode(out, patchCode());
    }

    @Override
    public boolean verify() {
        switch (code) {
            case Move:
                assert opr.isValid() && result.isValid() : "Operand and result must be valid in a LIROp1 move instruction.";
                break;
            case NullCheck:
                assert opr.isRegister() : "Operand must be a register in a LIROp1 null check instruction.";
                break;
            case Return:
                assert opr.isRegister() || opr.isIllegal() : "Operand must be (register | illegal) in a LIROp1 return instruction.";
                break;
        }
        return true;
    }

    public LIROperand inOpr() {
        return operand();
    }

    public LIROperand resultOpr() {
        return result();
    }
}
