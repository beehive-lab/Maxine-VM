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

import com.sun.c1x.debug.*;
import com.sun.c1x.stub.*;


/**
 * The <code>LIRLock</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRLock extends LIRInstruction {

    /**
     * Creates a new LIRLock instruction.
     *
     * @param hdr
     * @param obj
     * @param lock
     * @param scratch
     * @param stub
     */
    public LIRLock(LIROpcode opcode, LIROperand hdr, LIROperand obj, LIROperand lock, LIROperand scratch, CodeStub stub, CodeEmitInfo info) {
        super(opcode, LIROperandFactory.IllegalLocation, info, false, stub, 0, 3, obj, lock, hdr, scratch);
    }

    /**
     * Gets the lock of this class.
     *
     * @return the lock
     */
    public LIROperand lockOpr() {
        return operand(1);
    }


    /**
     * Gets the header of this class.
     *
     * @return the header
     */
    public LIROperand hdrOpr() {
        return operand(2);
    }

    /**
     * Gets the object of this class.
     *
     * @return the object
     */
    public LIROperand objOpr() {
        return operand(0);
    }

    /**
     * Gets the scratch of this class.
     *
     * @return the scratch
     */
    public LIROperand scratchOpr() {
        return operand(3);
    }

    /**
     * Emits target assembly code for this instruction.
     *
     * @param masm the target assembler
     *
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitLock(this);
        masm.addCodeStub(stub);
    }

    /**
     * Prints this instruction.
     *
     * @param out the output log stream
     */
    @Override
    public void printInstruction(LogStream out) {
        super.printInstruction(out);
        out.printf("[lbl:%s]", stub.entry.toString());
    }
}
