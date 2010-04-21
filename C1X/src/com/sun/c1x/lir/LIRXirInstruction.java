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

import com.sun.c1x.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;

public class LIRXirInstruction extends LIRInstruction {

    public final CiValue[] originalOperands;
    public final int outputOperandIndex;
    public final int[] operandIndices;
    public final XirSnippet snippet;
    public final RiMethod method;
    public final int inputTempCount;
    public final int tempCount;
    public final int inputCount;

    public LIRXirInstruction(XirSnippet snippet, CiValue[] originalOperands, CiValue outputOperand, int inputTempCount, int tempCount, CiValue[] operands, int[] operandIndices, int outputOperandIndex, LIRDebugInfo info, RiMethod method) {
        super(LIROpcode.Xir, outputOperand, info, false, null, inputTempCount, tempCount, operands);
        this.method = method;
        this.snippet = snippet;
        this.operandIndices = operandIndices;
        this.outputOperandIndex = outputOperandIndex;
        this.originalOperands = originalOperands;
        this.inputTempCount = inputTempCount;
        this.tempCount = tempCount;
        this.inputCount = operands.length - inputTempCount - tempCount;

        C1XMetrics.LIRXIRInstructions++;
    }

    public CiValue[] getOperands() {
        for (int i = 0; i < operandIndices.length; i++) {
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
     */
    @Override
    public String operationString() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("XIR ");

        sb.append(snippet.toString());

        if (method != null) {
            sb.append(" // ");
            sb.append(method.toString());
        }

        sb.append(" // ");

        int z = 0;

        sb.append(result().toString() + " = ");

        for (LIROperand opSlot : super.inputAndTempOperands) {

            CiValue op = opSlot.value(this);

            if (op != null) {
                sb.append(" ");
                sb.append(op.toString());
            }

            z++;

            if (z == inputCount) {
                sb.append(" | ");
            }

            if (z == inputCount + inputTempCount) {
                sb.append(" | ");
            }
        }

        return sb.toString();
    }
}
