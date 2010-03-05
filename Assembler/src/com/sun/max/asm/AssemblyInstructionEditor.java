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
package com.sun.max.asm;

import com.sun.max.lang.*;

/**
 * Interface for target code edition. Currently, used by the JIT only, for template-based code generation.
 * The interface is currently customized to the need of the JIT.
 * 
 * @author Laurent Daynes
 */
public interface AssemblyInstructionEditor {

    /**
     * Returns the displacement in the edited instruction.
     * @param displacementWidth the width of the displacement in the instruction
     * @return  the displacement in the edited instruction
     */
    int getIntDisplacement(WordWidth displacementWidth) throws AssemblyException;

    /**
     * Returns the immediate int value in the edited instruction.
     * @param displacementWidth the width of the displacement in the instruction
     * @return  the displacement in the edited instruction
     */
    int getIntImmediate(WordWidth immediateWidth) throws AssemblyException;

    /**
     * Replaces the value of the immediate displacement in a load/store instruction.
     * The original instruction must have an immediate displacement operand that can hold 8-bit immediate value.
     * @param displacementWidth width of the displacement in the original instruction
     * @param withIndex  indicate if the instruction uses a register index.
     * @param disp8 new value of the displacement.
     */
    void fixDisplacement(WordWidth displacementWidth, boolean withIndex, byte disp8);

    /**
     * Replaces the value of the immediate displacement in a load/store instruction.
     * The original instruction must have an immediate displacement operand that can hold 32-bit immediate value.
     * @param disp32
     * @throws AssemblyException
     *      if the replacement is not allowed (e.g., the instruction is
     *      not a load/store with immediate displacement parameter).
     */
    void fixDisplacement(WordWidth displacementWidth, boolean withIndex, int disp32) throws AssemblyException;

    /**
     * Replaces the value of the immediate source operand of an  instruction.
     * (tested with store instruction only).
     * @param operandWidth the width of the operand being replaced
     * @param imm8 the new immediate value of the operand.
     */
    void fixImmediateOperand(WordWidth operandWidth, byte imm8);

    /**
     * Replaces the value of the immediate source operand of an  instruction.
     * (tested with store instruction only).
     * @param operandWidth the width of the operand being replaced
     * @param imm16 the new immediate value of the operand.
     */
    void fixImmediateOperand(WordWidth operandWidth, short imm16);

    /**
     * Replaces the value of the immediate source operand of an  instruction.
     * (tested with store instruction only).
     * @param operandWidth the width of the operand being replaced
     * @param imm32 the new immediate value of the operand.
     */
    void fixImmediateOperand(WordWidth operandWidth, int imm32);

    /**
     * Replaces the value of the immediate source operand of an  instruction.
     * @param imm64 the new immediate value of the operand.
     */
    void fixImmediateOperand(int imm32);

    /**
     * Replaces the value of the immediate source operand of an  instruction.
     * @param imm64 the new immediate value of the operand.
     */
    void fixImmediateOperand(long imm64);

    /**
     * Fix relative displacement of a branch instruction.
     * 
     * @param originalDisplacementWidth
     * @param displacementWidth width of the relative displacement in the original instruction
     * @param disp32 new relative displacement
     */
    void fixBranchRelativeDisplacement(WordWidth displacementWidth, int disp32) throws AssemblyException;
}
