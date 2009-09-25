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
import com.sun.c1x.xir.*;

public class LIRXirInstruction extends LIRInstruction {

    public final LIROperand[] originalOperands;
    public final XirSnippet snippet;

    public LIRXirInstruction(XirSnippet snippet, LIROperand[] originalOperands, LIROperand outputOperand, int inputTempCount, int tempCount, LIROperand[] operands) {
        super(LIROpcode.Xir, outputOperand, null, false, null, inputTempCount, tempCount, operands);
        this.snippet = snippet;
        this.originalOperands = originalOperands;
    }

    public LIROperand[] getOperands() {
        final LIROperand[] result = new LIROperand[snippet.template.parameters.length];

        int inputParameterIndex = 0;
        for (int i = 0; i < result.length; i++) {
            if (i == snippet.template.getResultParameterIndex()) {
                result[i] = this.result();
            } else {
                if (snippet.arguments[i] != null) {
                    if (snippet.arguments[i].constant == null) {
                        result[i] = operand(inputParameterIndex++);
                    } else {
                        result[i] = originalOperands[i];
                    }
                }
            }
        }

        return result;
    }

    /**
     * Emits target assembly code for this instruction.
     *
     * @param masm the target assembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitXir(this);
    }

     /**
     * Prints this instruction.
     *
     * @param out the output stream
     */
    @Override
    public void printInstruction(LogStream out) {
        out.print("LIRXIR");

        for (LIROperand op : getOperands()) {
            if (op != null) {
                out.print(" | ");
                out.print(op.toString());
            }
        }
    }
}
