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
package com.sun.max.vm.compiler.eir;

import com.sun.max.collect.*;
import com.sun.max.lang.*;

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

    private final EirMethod _eirMethod;

    public EirMethod eirMethod() {
        return _eirMethod;
    }

    private final EirOperand[] _calleeSavedOperands;

    private final AppendableSequence<EirOperand> _resultOperands = new LinkSequence<EirOperand>();

    private final EirLocation _returnResultLocation;

    public void addResultValue(EirValue resultValue) {
        for (EirOperand operand : _resultOperands) {
            if (operand.eirValue() == resultValue) {
                return;
            }
        }
        final EirOperand resultOperand = new EirOperand(this, EirOperand.Effect.USE, _returnResultLocation.category().asSet());
        resultOperand.setRequiredLocation(_returnResultLocation);
        resultOperand.setEirValue(resultValue);
        _resultOperands.append(resultOperand);
    }

    private final AppendableSequence<EirOperand> _useOperands = new LinkSequence<EirOperand>();

    public void addUse(EirValue usedValue) {
        final EirOperand operand = new EirOperand(this, EirOperand.Effect.USE, usedValue.location().category().asSet());
        operand.setEirValue(usedValue);
        _useOperands.append(operand);
    }

    public void addStackSlotUse(EirValue usedValue) {
        final EirOperand operand = new EirOperand(this, EirOperand.Effect.USE, EirLocationCategory.S);
        operand.setEirValue(usedValue);
        _useOperands.append(operand);
    }

    public EirEpilogue(EirBlock block, EirMethod eirMethod,
                       EirValue[] calleeSavedValues, EirLocation[] calleeSavedRegisters,
                       EirLocation returnResultLocation) {
        super(block);
        _eirMethod = eirMethod;

        _calleeSavedOperands = new EirOperand[calleeSavedValues.length];
        for (int i = 0; i < calleeSavedValues.length; i++) {
            final EirLocation register = calleeSavedRegisters[i];
            _calleeSavedOperands[i] = new EirOperand(this, EirOperand.Effect.USE, register.category().asSet());
            _calleeSavedOperands[i].setRequiredLocation(register);
            _calleeSavedOperands[i].setEirValue(calleeSavedValues[i]);
        }

        _returnResultLocation = returnResultLocation;
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        if (_calleeSavedOperands != null) {
            for (EirOperand operand : _calleeSavedOperands) {
                visitor.run(operand);
            }
        }
        for (EirOperand operand : _resultOperands) {
            visitor.run(operand);
        }
        for (EirOperand operand : _useOperands) {
            visitor.run(operand);
        }
    }

    @Override
    public String toString() {
        String s = "epilogue (" + _resultOperands + ")";
        if (_calleeSavedOperands.length != 0) {
            s += "[Callee saved: " + Arrays.toString(_calleeSavedOperands) + "]";
        }
        if (!_useOperands.isEmpty()) {
            s += "[Used: " + _useOperands + "]";
        }
        if (_eirMethod.isGenerated()) {
            s += " frameSize:" + _eirMethod.frameSize();
        }
        return s;
    }
}
