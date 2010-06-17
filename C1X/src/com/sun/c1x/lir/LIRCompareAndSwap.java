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

import com.sun.cri.ci.*;

/**
 * The {@code LIRCompareAndSwap} class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRCompareAndSwap extends LIRInstruction {

    /**
     * Constructs a new LIRCompareAndSwap instruction.
     * @param addr
     * @param expectedValue
     * @param newValue
     */
    public LIRCompareAndSwap(LIROpcode opcode, CiAddress addr, CiValue expectedValue, CiValue newValue) {
        super(opcode, CiValue.IllegalValue, null, false, 0, 0, addr, expectedValue, newValue);
    }

    /**
     * Gets the address of compare and swap.
     *
     * @return the address
     */
    public CiAddress address() {
        return (CiAddress) operand(0);
    }

    /**
     * Gets the cmpValue of this class.
     *
     * @return the cmpValue
     */
    public CiValue expectedValue() {
        return operand(1);
    }

    /**
     * Gets the newValue of this class.
     *
     * @return the newValue
     */
    public CiValue newValue() {
        return operand(2);
    }

    /**
     * Emits target assembly code for this instruction.
     *
     * @param masm the target assembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitCompareAndSwap(this);
    }
}
