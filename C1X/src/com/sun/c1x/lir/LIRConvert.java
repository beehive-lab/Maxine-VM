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

import com.sun.c1x.bytecode.*;
import com.sun.c1x.util.*;


/**
 * The <code>LIRConvert</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRConvert extends LIROp1 {

    private int bytecode;
    private ConversionStub stub;

    /**
     * Constructs a new instruction LIRConvert for a given operand.
     *
     * @param bytecode the opcode of the bytecode for this conversion
     * @param operand the input operand for this instruction
     * @param result the result operand for this instruction
     * @param stub the conversion stub for this instruction
     */
    public LIRConvert(int bytecode, LIROperand operand, LIROperand result, ConversionStub stub) {
        super(LIROpcode.Convert, operand, result);
        this.bytecode = bytecode;
        this.stub = stub;
    }

    /**
     * @return the bytecode
     */
    public int bytecode() {
        return bytecode;
    }

    /**
     * Gets the code stub associated for this instruction.
     *
     * @return the code stub
     */
    public ConversionStub stub() {
        return stub;
    }

    /**
     * Emits target assembly code for this LIRConvert instruction.
     *
     * @param masm the LIRAssembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        // TODO: not yet implemented
    }

    /**
     * Prints this instruction to a LogStream.
     *
     * @param out the output stream
     */
    @Override
    public void printInstruction(LogStream out) {
        super.printInstruction(out);
        out.print(Bytecodes.name(bytecode));
        opr.print(out);
        out.print(" ");
        result.print(out);
        out.print(" ");
    }
}
