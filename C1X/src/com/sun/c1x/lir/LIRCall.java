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

import com.sun.c1x.util.*;


/**
 * The <code>LIRCall</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRCall extends LIRInstruction {

    protected int address;   // TODO not sure if we should create a new class for address
    protected ArrayList<LIRInstruction> arguments;

    /**
     * Creates a new LIRCall instruction.
     *
     * @param address
     * @param arguments
     */
    public LIRCall(LIROpcode opcode, int address, LIROperand result, ArrayList<LIRInstruction> arguments, CodeEmitInfo info) {
        super(opcode, result, info);
        this.address = address;
        this.arguments = arguments;
    }

    /**
     * Gets the address of this call.
     *
     * @return the address
     */
    public int address() {
        return address;
    }

    /**
     * Gets the arguments of this call.
     *
     * @return the arguments
     */
    public ArrayList<LIRInstruction> arguments() {
        return arguments;
    }

    /**
     * Emits code for this instruction.
     *
     * @param masm the target assembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        // TODO Auto-generated method stub

    }

    /**
     * Prints this instruction.
     *
     * @param out the output log stream.
     */
    @Override
    public void printInstruction(LogStream out) {
        out.print("call: ");
        out.print("[addr: 0x" + address());
    }
}
