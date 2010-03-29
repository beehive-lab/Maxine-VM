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

import com.sun.max.collect.*;
import com.sun.max.lang.Arrays;
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

    AppendableSequence<EirOperand> calleeSavedOperands = new LinkSequence<EirOperand>();
    private final EirOperand[] parameterOperands;

    private final AppendableSequence<EirOperand> definitionOperands = new LinkSequence<EirOperand>();

    public void addDefinition(EirValue definedValue) {
        final EirOperand operand = new EirOperand(this, EirOperand.Effect.DEFINITION, definedValue.location().category().asSet());
        operand.setEirValue(definedValue);
        definitionOperands.append(operand);
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
                calleeSavedOperands.append(operand);
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
        String s = "prologue (" + Arrays.toString(parameterOperands) + ")";
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
