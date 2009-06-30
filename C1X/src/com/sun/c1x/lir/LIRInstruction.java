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

import com.sun.c1x.ir.*;
import com.sun.c1x.util.*;

/**
 * The <code>LIRInstruction</code> class definition.
 *
 * @author Marcelo Cintra
 */
public abstract class LIRInstruction {

    LIROperand result;      // the result operand for this instruction
    LIROpcode opcode;       // the opcode of this instruction
    LIRMoveKind flags;      // flag that indicate the kind of move
    CodeEmitInfo info;      // used to emit debug information
    int id;                 // value id for register allocation
    int fpuPopCount;
    Instruction source;    // for debugging

    /**
     * Constructs a new Instruction.
     *
     */
    public LIRInstruction() {
        result = LIROperandFactory.illegalOperand;
        opcode = LIROpcode.None;
        info = null;
        flags = LIRMoveKind.Normal;
        fpuPopCount = 0;
        source = null;
        id = -1;
    }

    /**
     * Constructs a new Instruction.
     *
     * @param opcode the opcode of the new instruction
     * @param result the operand that holds operation result of this instruction
     * @param info the object holding information needed to emit debug information
     */
    public LIRInstruction(LIROpcode opcode, LIROperand result, CodeEmitInfo info) {
        this.result = result;
        this.opcode = opcode;
        this.info = info;
        flags = LIRMoveKind.Normal;
        fpuPopCount = 0;
        source = null;
        id = -1;
    }

    /**
     * Gets the lock stack for this instruction.
     *
     * @return return the result operand
     */
    public LIROperand result() {
        return result;
    }

    /**
     * Sets the result operand for this instruction.
     *
     * @param result the result operand
     */
    public void setResult(LIROperand result) {
        this.result = result;
    }

    /**
     * Gets the opcode of this instruction.
     *
     * @return return the instruction's opcode.
     */
    public LIROpcode opcode() {
        return opcode;
    }

    /**
     * Gets the code emission info of this instruction.
     *
     * @return info the object containing additional information to produce generate debug information.
     */
    public CodeEmitInfo info() {
        return info;
    }

    /**
     * Gets the value id of this instruction.
     *
     * @return id the value id.
     */
    public int id() {
        return id;
    }

    /**
     * Sets the value id of this instruction.
     *
     * @param id the value
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the instruction name.
     *
     * @return the name of the enum constant that represents the instruction opcode, exactly as declared in the enum LIROpcode declaration.
     */
    public String name() {
        return opcode.name();
    }

    /**
     * Gets the Fpu pop counter of this instruction.This is a counter to a FPU stack simulation, only used on Intel.
     *
     * @return id the value id.
     */
    public int fpuPopCount() {
        return fpuPopCount;
    }

    /**
     * Sets the Fpu pop counter of this instruction. This is a counter to FPU stack simulation, only used on Intel.
     *
     * @param fpuPopCount the value
     */
    public void setFpuPopCount(int fpuPopCount) {
        this.fpuPopCount = fpuPopCount;
    }

    /**
     * Checks if the Fpu stack is not empty. This FPU stack simulation is only used on Intel.
     *
     * @return <code>true</code> if the Fpu stack is not empty.
     */
    public boolean popFpuStack() {
        return fpuPopCount > 0;
    }

    /**
     * Gets the HIR correspondent for this instruction.
     *
     * @return source the HIR source instruction.
     */
    public Instruction source() {
        return source;
    }

    /**
     * Sets the HIR correspondent for this instruction.
     *
     * @param source the HIR source instruction.
     */
    public void setSource(Instruction source) {
        this.source = source;
    }

    /**
     * Abstract method to be used to emit target code for this instruction.
     *
     * @param masm the target assembler.
     */
    public abstract void emitCode(LIRAssembler masm);

    /**
     * Abstract method to be print information specific to each instruction.
     *
     * @param out the LogStream to print into.
     */
    public abstract void printInstruction(LogStream out);

    /**
     * Abstract method to be print this instruction.
     *
     * @param stream the LogStream to print into.
     */
    public void printOn(LogStream stream) {

    }

    /**
     * Determines if a given opcode is in a given range of valid opcodes.
     *
     * @param opcode the opcode to be tested.
     * @param start the lower bound range limit of valid opcodes
     * @param end the upper bound range limit of valid opcodes
     */
    protected static boolean isInRange(LIROpcode opcode, LIROpcode start, LIROpcode end)  {
        return start.ordinal() < opcode.ordinal() && opcode.ordinal() < end.ordinal();
    }


    public void verify() {

    }

}
