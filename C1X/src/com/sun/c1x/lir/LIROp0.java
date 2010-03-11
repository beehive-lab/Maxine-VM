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

/**
 * The {@code LIROp0} class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIROp0 extends LIRInstruction {

    /**
     * Creates a LIROp0 instruction.
     *
     * @param opcode the opcode of the new instruction
     */
    public LIROp0(LIROpcode opcode) {
        this(opcode, LIROperand.IllegalLocation);
    }

    /**
     * Creates a LIROp0 instruction.
     *
     * @param opcode the opcode of the new instruction
     * @param result the result operand to the new instruction
     */
    public LIROp0(LIROpcode opcode, LIROperand result) {
        this(opcode, result, null);
    }

    /**
     * Creates a LIROp0 instruction.
     *
     * @param opcode the opcode of the new instruction
     * @param result the result operand to the new instruction
     * @param info used to emit debug information associated to this instruction
     */
    public LIROp0(LIROpcode opcode, LIROperand result, LIRDebugInfo info) {
        super(opcode, result, info, false, null, 0, 0);
        assert isInRange(opcode, LIROpcode.BeginOp0, LIROpcode.EndOp0) : "Opcode " + opcode + " is invalid for a LIROP0 instruction";
    }

    /**
     * Emit assembly code for this instruction.
     * @param masm the target assembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitOp0(this);
    }
}
