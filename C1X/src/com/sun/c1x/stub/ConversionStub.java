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
package com.sun.c1x.stub;

import com.sun.c1x.debug.LogStream;
import com.sun.c1x.lir.LIRAssembler;
import com.sun.c1x.lir.LIROperand;
import com.sun.c1x.lir.LIRVisitState;


/**
 * The <code>ConversionStub</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class ConversionStub extends CodeStub {

    private final int opcode;
    private LIROperand input;
    private LIROperand result;

    /**
     * Constructs a new conversion stub.
     *
     * @param opcode
     * @param input
     * @param result
     */
    public ConversionStub(int opcode, LIROperand input, LIROperand result) {
        super();
        this.opcode = opcode;
        this.input = input;
        this.result = result;
    }

    /**
     * Gets the bytecode of this conversion stub.
     *
     * @return the bytecode
     */
    public int bytecode() {
        return opcode;
    }

    /**
     * @return the input
     */
    public LIROperand input() {
        return input;
    }

    /**
     * @return the result
     */
    public LIROperand result() {
        return result;
    }

    @Override
    public void emitCode(LIRAssembler masm) {
        // TODO to be completed later
    }

    @Override
    public void visit(LIRVisitState visitor) {
        visitor.doSlowCase();
        visitor.doInput(input);
        visitor.doOutput(result);
    }

    @Override
    public void printName(LogStream out) {
        out.print("ConversionStub");
    }
}
