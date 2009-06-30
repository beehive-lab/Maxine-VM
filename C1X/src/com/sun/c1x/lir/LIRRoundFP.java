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


/**
 * The <code>LIRRoundFP</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRRoundFP extends LIROp1 {

    private LIROperand stackLockTmp;

    /**
     * Creates a LIRRoundFP instruction.
     *
     * @param register the register holding the input for this instruction
     * @param stackLockTemp stack lock temporary
     * @param result the resulting operand
     */
    public LIRRoundFP(LIROperand register, LIROperand stackLockTemp, LIROperand result) {
        super(LIROpcode.Roundfp, register, result);
        this.stackLockTmp = stackLockTemp;
    }

    /**
     * Gets the stackLockTmp of this instruction.
     *
     * @return the stackLockTmp
     */
    public LIROperand tackLockTmp() {
        return stackLockTmp;
    }

    /**
     * Prints this instruction to a LogStream.
     *
     * @param out the output stream
     */
    @Override
    public void printInstruction(LogStream out) {
        super.printInstruction(out);
        opr.print(out);
        out.print(" ");
        stackLockTmp.print(out);
        out.print(" ");
        result.print(out);
        out.print(" ");
    }
}
