/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
