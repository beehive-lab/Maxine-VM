/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.cps.eir;

import java.util.*;

import com.sun.max.*;
import com.sun.max.vm.type.*;

/**
 * This instruction is a placeholder for any actions that occur immediately prior to
 * returning from a method. Typically, this instruction will generate
 * machine code to leave a frame created by an {@link EirPrologue}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class EirEpilogue<EirInstructionVisitor_Type extends EirInstructionVisitor, EirTargetEmitter_Type extends EirTargetEmitter>
                      extends EirInstruction<EirInstructionVisitor_Type, EirTargetEmitter_Type> {

    private final EirMethod eirMethod;

    public EirMethod eirMethod() {
        return eirMethod;
    }

    private final EirOperand[] calleeSavedOperands;

    private final EirOperand resultOperand;

    public void setResultValue(EirValue resultValue) {
        assert resultOperand != null : "Cannot set result value for void method";
        if (resultOperand.eirValue() == resultValue) {
            return;
        }
        assert resultOperand.eirValue() == null;
        resultOperand.setEirValue(resultValue);
    }

    public EirValue resultValue() {
        assert resultOperand != null : "Cannot get result value for void method";
        return resultOperand.eirValue();
    }

    private final List<EirOperand> useOperands = new LinkedList<EirOperand>();

    public void addUse(EirValue usedValue) {
        final EirOperand operand = new EirOperand(this, EirOperand.Effect.USE, usedValue.location().category().asSet());
        operand.setEirValue(usedValue);
        useOperands.add(operand);
    }

    public void addStackSlotUse(EirValue usedValue) {
        final EirOperand operand = new EirOperand(this, EirOperand.Effect.USE, EirLocationCategory.S);
        operand.setEirValue(usedValue);
        useOperands.add(operand);
    }

    public EirEpilogue(EirBlock block, EirMethod eirMethod,
                       EirValue[] calleeSavedValues, EirLocation[] calleeSavedRegisters,
                       EirLocation returnResultLocation) {
        super(block);
        this.eirMethod = eirMethod;

        this.calleeSavedOperands = new EirOperand[calleeSavedValues.length];
        for (int i = 0; i < calleeSavedValues.length; i++) {
            final EirLocation register = calleeSavedRegisters[i];
            calleeSavedOperands[i] = new EirOperand(this, EirOperand.Effect.USE, register.category().asSet());
            calleeSavedOperands[i].setRequiredLocation(register);
            calleeSavedOperands[i].setEirValue(calleeSavedValues[i]);
        }
        if (eirMethod.resultKind() != Kind.VOID) {
            resultOperand = new EirOperand(this, EirOperand.Effect.USE, returnResultLocation.category().asSet());
            resultOperand.setRequiredLocation(returnResultLocation);
        } else {
            resultOperand = null;
        }
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        if (calleeSavedOperands != null) {
            for (EirOperand operand : calleeSavedOperands) {
                visitor.run(operand);
            }
        }
        if (resultOperand != null) {
            visitor.run(resultOperand);
        }
        for (EirOperand operand : useOperands) {
            visitor.run(operand);
        }
    }

    @Override
    public String toString() {
        String s = "epilogue (" + (resultOperand == null ? "" : resultOperand.toString()) + ")";
        if (calleeSavedOperands.length != 0) {
            s += "[Callee saved: " + Utils.toString(calleeSavedOperands, ", ") + "]";
        }
        if (!useOperands.isEmpty()) {
            s += "[Used: " + useOperands + "]";
        }
        if (eirMethod.isGenerated()) {
            s += " frameSize:" + eirMethod.frameSize();
        }
        return s;
    }
}
