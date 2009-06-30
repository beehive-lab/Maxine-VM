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

import java.util.*;


/**
 * The <code>LIRRTCall</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRRTCall extends LIRCall{

    private LIROperand tmp;

    /**
     * Creates a new LIRRTCall instruction.
     *
     * @param address
     * @param tmp
     * @param result
     * @param arguments
     * @param info
     */
    public LIRRTCall(int address, LIROperand tmp, LIROperand result, ArrayList<LIRInstruction> arguments, CodeEmitInfo info) {
        super(LIROpcode.RtCall, address, result, arguments, info);
        this.tmp = tmp;
    }

    /**
     * Gets the temporary operand associated to this call.
     * @return the tmp
     */
    public LIROperand tmp() {
        return tmp;
    }

    /** Emits target assembly code for this instruction.
    *
    * @param masm the target assembler
    */
    @Override
    public void emitCode(LIRAssembler masm) {
        // TODO Not yet implemented.
    }

    /**
     * Verifies this instruction.
     */
    @Override
    public void verify() {
        // TODO Not yet implemented.
    }
}
