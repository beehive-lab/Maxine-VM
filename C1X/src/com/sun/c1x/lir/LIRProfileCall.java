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

import com.sun.c1x.ci.*;
import com.sun.c1x.debug.LogStream;


/**
 * The <code>LIRProfileCall</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRProfileCall extends LIRInstruction {

    private CiMethod profiledMethod;
    private int profiledBci;
    LIROperand mdo;
    LIROperand recv;
    LIROperand tmp1;
    private CiType knownHolder;

    /**
     * Constructs a new LIRProfileCall Instruction.
     *
     * @param profiledMethod
     * @param profiledBci
     * @param mdo
     * @param receiver
     * @param tmp1
     */
    public LIRProfileCall(LIROpcode opcode, CiMethod profiledMethod, int profiledBci, LIROperand mdo, LIROperand recv, LIROperand tmp1, CiType knownHolder) {
        super(opcode, LIROperandFactory.IllegalOperand, null);
        this.profiledMethod = profiledMethod;
        this.profiledBci = profiledBci;
        this.mdo = mdo;
        this.recv = recv;
        this.tmp1 = tmp1;
        this.knownHolder = knownHolder;
    }

    /**
     * Gets the profiledMethod of this profile call instruction.
     *
     * @return the profiledMethod
     */
    public CiMethod profiledMethod() {
        return profiledMethod;
    }

    /**
     * Gets the profiledBci of this profile call instruction.
     *
     * @return the profiledBci
     */
    public int profiledBci() {
        return profiledBci;
    }

    /**
     * Gets the mdo of this profile call instruction.
     *
     * @return the mdo
     */
    public LIROperand mdo() {
        return mdo;
    }

    /**
     * Gets the receiver of this profile call instruction.
     *
     * @return the receiver
     */
    public LIROperand recv() {
        return recv;
    }

    /**
     * Gets the tmp1 of this profile call instruction.
     *
     * @return the tmp1
     */
    public LIROperand tmp1() {
        return tmp1;
    }

    /**
     * Gets the knownHolder of this profile call instruction.
     *
     * @return the knownHolder
     */
    public CiType knownHolder() {
        return knownHolder;
    }

    /**
     * Emits target assembly code for this instruction.
     *
     * @param masm the target assembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitProfileCall(this);
    }

    /**
     * Prints this instruction.
     *
     * @param out the output stream
     */
    @Override
    public void printInstruction(LogStream out) {
        out.print(profiledMethod.name().toString()); // TODO: the method name was changed from ciSymbol to String
        out.print(".");
        out.print(profiledMethod.holder().name().toString());
        out.printf(" @ %d", profiledBci);
        mdo.print(out);
        out.print(" ");
        recv.print(out);
        out.print(" ");
        tmp1.print(out);
    }
}
