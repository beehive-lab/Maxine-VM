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

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.Mark;
import com.sun.cri.ri.*;
import com.sun.cri.xir.CiXirAssembler.XirMark;

/**
 * This class represents a call instruction; either to a {@linkplain CiRuntimeCall runtime method},
 * a {@linkplain RiMethod Java method}, a native function or a global stub.
 *
 * @author Marcelo Cintra
 */
public class LIRCall extends LIRInstruction {

    /**
     * The target of the call. This will be a {@link CiRuntimeCall}, {@link RiMethod} or {@link CiValue}
     * object denoting a call to the runtime, a Java method or a native function respectively.
     */
    public final Object target;
    /**
     * The call site needs to be marked if this is non-null.
     */
    public final Map<XirMark, Mark> marks;

    private final int targetAddressIndex;


    private static CiValue[] toArray(List<CiValue> arguments) {
        return arguments.toArray(new CiValue[arguments.size()]);
    }

    public LIRCall(LIROpcode opcode, Object target, CiValue result, List<CiValue> arguments, LIRDebugInfo info, Map<XirMark, Mark> marks, boolean calleeSaved) {
        super(opcode, result, info, !calleeSaved, 0, 0, toArray(arguments));
        this.marks = marks;
        if (opcode == LIROpcode.DirectCall) {
            this.targetAddressIndex = -1;
        } else {
            // The last argument is the operand holding the address for the indirect call
            this.targetAddressIndex = arguments.size() - 1;
        }
        this.target = target;
    }

    /**
     * Emits target assembly code for this instruction.
     *
     * @param masm the target assembler
     */
    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitCall(this);
    }

    /**
     * Returns the receiver for this method call.
     * @return the receiver
     */
    public CiValue receiver() {
        return operand(0);
    }

    public RiMethod method() {
        return (RiMethod) target;
    }

    public CiRuntimeCall runtimeCall() {
        return (CiRuntimeCall) target;
    }

    public CiValue targetAddress() {
        return operand(targetAddressIndex);
    }

    @Override
    public String operationString(OperandFormatter operandFmt) {
        StringBuilder buf = new StringBuilder();
        if (result().isLegal()) {
            buf.append(operandFmt.format(result())).append(" = ");
        }
        String targetAddress = null;
        if (code != LIROpcode.DirectCall) {
            targetAddress = operandFmt.format(targetAddress());
            buf.append(targetAddress);
        }
        buf.append('(');
        boolean first = true;
        for (LIROperand operandSlot : inputAndTempOperands) {
            String operand = operandFmt.format(operandSlot.value(this));
            if (!operand.isEmpty() && !operand.equals(targetAddress)) {
                if (!first) {
                    buf.append(", ");
                } else {
                    first = false;
                }
                buf.append(operand);
            }
        }
        buf.append(')');
        return buf.toString();
    }
}
