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

import com.sun.c1x.*;
import com.sun.c1x.debug.*;


/**
 * The <code>LIRDelay</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRDelay extends LIRInstruction {

    private LIRInstruction operand;

    public LIRDelay(LIRInstruction operand, CodeEmitInfo info) {
        super(LIROpcode.DelaySlot, LIROperandFactory.IllegalOperand, info);
        this.operand = operand;
        assert operand.code == LIROpcode.Nop || C1XOptions.LIRFillDelaySlots : "Should be filling with nops";
    }

    /**
     * Emit target assembly code for this instruction.
     *
     * @param masm the target assembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitDelay(this);
    }

    /**
     * Prints this instruction.
     *
     * @param out the output log stream.
     */
    @Override
    public void printInstruction(LogStream out) {
        operand.printInstruction(out);
    }

    /**
     * Gets the delay operand of this instruction.
     *
     * @return the operand
     */
    public LIRInstruction delayOperand() {
        return operand;
    }

    /**
     * @return the object with information for
     */
    public CodeEmitInfo callInfo() {
        return info();
    }
}
