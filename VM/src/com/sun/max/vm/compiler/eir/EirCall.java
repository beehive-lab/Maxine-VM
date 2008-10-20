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

import java.lang.reflect.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public abstract class EirCall<EirInstructionVisitor_Type extends EirInstructionVisitor, EirTargetEmitter_Type extends EirTargetEmitter>
                      extends EirStop<EirInstructionVisitor_Type, EirTargetEmitter_Type> {

    private EirOperand[] _callerSavedOperands;

    public EirOperand[] callerSavedOperands() {
        return _callerSavedOperands;
    }

    private final EirOperand _function;

    public EirOperand function() {
        return _function;
    }

    private final EirOperand _result;

    public EirOperand result() {
        return _result;
    }

    private final EirOperand[] _arguments;

    public EirOperand[] arguments() {
        return _arguments;
    }

    protected EirCall(EirBlock block,
                      EirABI abi, EirValue result, EirLocation resultLocation,
                      EirValue function, PoolSet<EirLocationCategory> functionLocationCategories,
                      EirValue[] arguments, EirLocation[] argumentLocations,
                      EirMethodGeneration methodGeneration) {
        super(block);
        _function = new EirOperand(this, EirOperand.Effect.USE, functionLocationCategories);
        _function.setEirValue(function);
        if (result == null) {
            _result = null;
        } else {
            _result = new EirOperand(this, EirOperand.Effect.DEFINITION, resultLocation.category().asSet());
            _result.setRequiredLocation(resultLocation);
            _result.setEirValue(result);
        }
        if (arguments == null) {
            _arguments = null;
        } else {
            _arguments = new EirOperand[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                EirLocation location = argumentLocations[i];
                if (location instanceof EirStackSlot) {
                    location = methodGeneration.canonicalizeStackSlot((EirStackSlot) location);
                }
                _arguments[i] = new EirOperand(this, EirOperand.Effect.USE, argumentLocations[i].category().asSet());
                _arguments[i].setRequiredLocation(argumentLocations[i]);
                _arguments[i].setEirValue(arguments[i]);
            }
        }
        final EirRegister[] callerSavedRegisters = abi.callerSavedRegisterArray();
        _callerSavedOperands = new EirOperand[callerSavedRegisters.length];
        for (int i = 0; i < callerSavedRegisters.length; i++) {
            final EirRegister register = callerSavedRegisters[i];
            if (register ==  abi.integerRegisterActingAs(VMRegister.Role.SAFEPOINT_LATCH)) {
                _callerSavedOperands[i] = new EirOperand(this, EirOperand.Effect.DEFINITION, register.category().asSet());
                _callerSavedOperands[i].setEirValue(methodGeneration.safepointLatchVariable());
                continue;
            }
            if (register != resultLocation) {
                _callerSavedOperands[i] = new EirOperand(this, EirOperand.Effect.DEFINITION, register.category().asSet());
                _callerSavedOperands[i].setRequiredLocation(register);
                final EirVariable variable = methodGeneration.makeRegisterVariable(register);
                _callerSavedOperands[i].setEirValue(variable);
            }
        }
    }

    @Override
    public void addStackReferenceMap(WordWidth stackSlotWidth, ByteArrayBitMap map) {
        super.addStackReferenceMap(stackSlotWidth, map);
        if (_arguments != null) {
            for (EirOperand argument : _arguments) {
                if (argument.kind() == Kind.REFERENCE && argument.location() instanceof EirStackSlot) {
                    final EirStackSlot stackSlot = (EirStackSlot) argument.location();
                    final int stackSlotBitIndex = stackSlot.offset() / stackSlotWidth.numberOfBytes();
                    map.set(stackSlotBitIndex);
                }
            }
        }
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        visitor.run(_function);
        if (_result != null) {
            visitor.run(_result);
        }
        if (_arguments != null) {
            for (EirOperand argument : _arguments) {
                visitor.run(argument);
            }
        }
        if (_callerSavedOperands != null) {
            for (EirOperand callerSaveOperand : _callerSavedOperands) {
                if (callerSaveOperand != null) {
                    visitor.run(callerSaveOperand);
                }
            }
        }
        super.visitOperands(visitor);
    }

    @Override
    public String toString() {
        String s = super.toString() + " " + _function;
        if (_result != null) {
            s = _result.toString() + " := " + s;
        }
        if (_arguments != null) {
            s += " (" + Arrays.toString(_arguments) + ")";
        }
        if (_callerSavedOperands != null) {
            s += " [Caller saved: " + Arrays.toString(_callerSavedOperands) + "]";
        }
        return s + " " + javaFrameDescriptor();
    }

    @Override
    public void acceptVisitor(EirInstructionVisitor visitor) throws InvocationTargetException {
        visitor.visit(this);
    }

}
