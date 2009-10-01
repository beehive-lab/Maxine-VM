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
import com.sun.c1x.ri.*;
import com.sun.c1x.xir.*;

public class LIRXirInstruction extends LIRInstruction {

    public final LIROperand[] originalOperands;
    public final int outputOperandIndex;
    public final int[] operandIndices;
    public final XirSnippet snippet;
    public final RiMethod method;

    public LIRXirInstruction(XirSnippet snippet, LIROperand[] originalOperands, LIROperand outputOperand, int inputTempCount, int tempCount, LIROperand[] operands, int[] operandIndices, int outputOperandIndex, CodeEmitInfo info, RiMethod method) {
        super(LIROpcode.Xir, outputOperand, info, false, null, inputTempCount, tempCount, operands);
        this.method = method;
        this.snippet = snippet;
        this.operandIndices = operandIndices;
        this.outputOperandIndex = outputOperandIndex;
        this.originalOperands = originalOperands;
    }

    public LIROperand[] getOperands() {
        for (int i=0; i<operandIndices.length; i++) {
            originalOperands[operandIndices[i]] = operand(i);
        }
        if (outputOperandIndex != -1) {
            originalOperands[outputOperandIndex] = result();
        }
        return originalOperands;
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
