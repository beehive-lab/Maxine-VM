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
 * The <code>LIRLock</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRLock extends LIRInstruction{

    private LIROperand header;
    private LIROperand object;
    private LIROperand lock;
    private LIROperand scratch;
    private CodeStub stub;

    /**
     * Creates a new LIRLock instruction.
     *
     * @param header
     * @param object
     * @param lock
     * @param scratch
     * @param stub
     */
    public LIRLock(LIROpcode opcode, LIROperand header, LIROperand object, LIROperand lock, LIROperand scratch, CodeStub stub, CodeEmitInfo info) {
        super(opcode, LIROperandFactory.illegalOperand, info);
        this.header = header;
        this.object = object;
        this.lock = lock;
        this.scratch = scratch;
        this.stub = stub;
    }

    /**
     * Gets the header of this class.
     *
     * @return the header
     */
    public LIROperand header() {
        return header;
    }

    /**
     * Gets the object of this class.
     *
     * @return the object
     */
    public LIROperand object() {
        return object;
    }

    /**
     * Gets the lock of this class.
     *
     * @return the lock
     */
    public LIROperand lock() {
        return lock;
    }

    /**
     * Gets the scratch of this class.
     *
     * @return the scratch
     */
    public LIROperand scratch() {
        return scratch;
    }

    /**
     * Gets the stub of this class.
     *
     * @return the stub
     */
    public CodeStub stub() {
        return stub;
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
     * Prints this instruction.
     *
     * @param out the output log stream
     */
    @Override
    public void printInstruction(LogStream out) {
        header.print(out);
        out.print(" ");
        object.print(out);
        out.print(" ");
        lock.print(out);
        if (scratch.isValid()) {
            scratch.print(out);
            out.print(" ");
        }
        out.print("[lbl:0x" + stub.entry);
    }
}
