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

import com.sun.c1x.*;
import com.sun.c1x.gen.*;
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
    public final List<CiValue> pointerSlots;

    public LIRXirInstruction(XirSnippet snippet,
                             CiValue[] originalOperands,
                             CiValue outputOperand,
                             int inputTempCount,
                             int tempCount,
			     CiValue[] operands,
			     int[] operandIndices,
			     int outputOperandIndex,
			     LIRDebugInfo info,
			     RiMethod method,
			     List<CiValue> pointerSlots) {
        super(LIROpcode.Xir, outputOperand, info, false, inputTempCount, tempCount, operands);
        this.pointerSlots = pointerSlots;
        assert this.pointerSlots == null || this.pointerSlots.size() >= 0;
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
    public String operationString(OperandFormatter operandFmt) {
        return toString(operandFmt);
    }

    @Override
    public String toString(OperandFormatter operandFmt) {
        StringBuilder sb = new StringBuilder();
        sb.append("XIR: ");

        if (result().isLegal()) {
            sb.append(operandFmt.format(result()) + " = ");
        }

        sb.append(snippet.template);
        sb.append("(");
        for (int i = 0; i < snippet.arguments.length; i++) {
            XirArgument a = snippet.arguments[i];
            if (i > 0) {
                sb.append(", ");
            }
            if (a.constant != null) {
                sb.append(operandFmt.format(a.constant));
            } else {
                LIRItem item = (LIRItem) a.object;
                sb.append(operandFmt.format(item.result()));
            }
        }
        sb.append(')');

        if (method != null) {
            sb.append(" method=");
            sb.append(method.toString());
        }


        for (LIRInstruction.OperandMode mode : LIRInstruction.OPERAND_MODES) {
            int n = operandCount(mode);
            if (mode == OperandMode.Output && n <= 1) {
                // Already printed single output (i.e. result())
                continue;
            }
            if (n != 0) {
                sb.append(' ').append(mode.name().toLowerCase()).append("=(");
                HashSet<String> operands = new HashSet<String>();
                for (int i = 0; i < n; i++) {
                    String operand = operandFmt.format(operandAt(mode, i));
                    if (!operands.contains(operand)) {
                        if (!operands.isEmpty()) {
                            sb.append(", ");
                        }
                        operands.add(operand);
                        sb.append(operand);
                    }
                }
                sb.append(')');
            }
        }

        appendDebugInfo(sb, operandFmt, info);

        return sb.toString();
    }
}
