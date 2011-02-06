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
import com.sun.max.vm.cps.eir.allocate.*;

/**
 * The first instruction in every EIR method.
 *
 * From the {@linkplain EirAllocator register allocator's} point of view, this instruction
 * is where the parameters become {@linkplain EirOperand#recordDefinition() defined}.
 * If the method containing this instruction requires a stack frame for local variables, then the
 * appropriate machine instruction(s) are emitted when assembling this instruction.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class EirPrologue<EirInstructionVisitor_Type extends EirInstructionVisitor,
                                  EirTargetEmitter_Type extends EirTargetEmitter>
                          extends EirInstruction<EirInstructionVisitor_Type, EirTargetEmitter_Type> {

    private final EirMethod eirMethod;

    public final EirMethod eirMethod() {
        return eirMethod;
    }

    List<EirOperand> calleeSavedOperands = new LinkedList<EirOperand>();
    private final EirOperand[] parameterOperands;

    private final List<EirOperand> definitionOperands = new LinkedList<EirOperand>();

    public void addDefinition(EirValue definedValue) {
        final EirOperand operand = new EirOperand(this, EirOperand.Effect.DEFINITION, definedValue.location().category().asSet());
        operand.setEirValue(definedValue);
        definitionOperands.add(operand);
    }

    protected EirPrologue(EirBlock block, EirMethod eirMethod,
                          EirValue[] calleeSavedValues, EirLocation[] calleeSavedRegisters,
                          BitSet isCalleeSavedParameter,
                          EirValue[] parameters, EirLocation[] parameterLocations) {
        super(block);
        this.eirMethod = eirMethod;

        for (int i = 0; i < calleeSavedValues.length; i++) {
            if (isCalleeSavedParameter.get(i)) {
                // We omit defining this callee-saved variable here,
                // because it is also a parameter and thus it will already be defined further below.
            } else {
                final EirLocation register = calleeSavedRegisters[i];
                final EirOperand operand = new EirOperand(this, EirOperand.Effect.DEFINITION, register.category().asSet());
                operand.setRequiredLocation(register);
                operand.setEirValue(calleeSavedValues[i]);
                calleeSavedOperands.add(operand);
            }
        }

        parameterOperands = new EirOperand[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            final EirLocation location = parameterLocations[i];
            parameterOperands[i] = new EirOperand(this, EirOperand.Effect.DEFINITION, location.category().asSet());
            parameterOperands[i].setRequiredLocation(location);
            parameterOperands[i].setEirValue(parameters[i]);
        }
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        for (EirOperand register : calleeSavedOperands) {
            visitor.run(register);
        }
        for (EirOperand parameter : parameterOperands) {
            visitor.run(parameter);
        }
        for (EirOperand parameter : definitionOperands) {
            visitor.run(parameter);
        }
    }

    @Override
    public String toString() {
        String s = "prologue (" + Utils.toString(parameterOperands, ", ") + ")";
        if (!calleeSavedOperands.isEmpty()) {
            s += "[Callee saved: " + calleeSavedOperands.toString() + "]";
        }
        if (!definitionOperands.isEmpty()) {
            s += "[Defined: " + definitionOperands.toString() + "]";
        }
        if (eirMethod.isGenerated()) {
            s += " frameSize:" + eirMethod.frameSize();
        }
        return s;
    }
}
