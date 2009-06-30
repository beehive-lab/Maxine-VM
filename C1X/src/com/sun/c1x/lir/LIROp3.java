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
 * The <code>LIROp3</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIROp3 extends LIRInstruction {

    LIROperand opr1;
    LIROperand opr2;
    LIROperand opr3;

    public LIROp3(LIROpcode opcode, LIROperand opr1, LIROperand opr2, LIROperand opr3, LIROperand result, CodeEmitInfo info) {
        super(opcode, result, info);
        this.opr1 = opr1;
        this.opr2 = opr2;
        this.opr3 = opr3;
        assert isInRange(opcode, LIROpcode.BeginOp3, LIROpcode.EndOp3) : "The " + opcode + " is not a valid LIROp3 opcode";
    }

    /**
     * Emits assembly code for this instruction.
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
        opr1.print(out);
        out.print(" ");
        opr2.print(out);
        out.print(" ");
        opr3.print(out);
        out.print(" ");
        result.print(out);
    }
}
