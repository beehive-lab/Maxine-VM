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
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public abstract class EirCall<EirInstructionVisitor_Type extends EirInstructionVisitor, EirTargetEmitter_Type extends EirTargetEmitter>
                      extends EirStop<EirInstructionVisitor_Type, EirTargetEmitter_Type> {

    private EirOperand[] callerSavedOperands;

    public EirOperand[] callerSavedOperands() {
        return callerSavedOperands;
    }

    private final EirOperand function;

    public EirOperand function() {
        return function;
    }

    private final EirOperand result;

    public EirOperand result() {
        return result;
    }

    private final EirOperand[] arguments;

    public EirOperand[] arguments() {
        return arguments;
    }

    protected EirCall(EirBlock block,
                      EirABI abi, EirValue result, EirLocation resultLocation,
                      EirValue function, PoolSet<EirLocationCategory> functionLocationCategories,
                      EirValue[] arguments, EirLocation[] argumentLocations,
                      EirMethodGeneration methodGeneration) {
        super(block);
        this.function = new EirOperand(this, EirOperand.Effect.USE, functionLocationCategories);
        this.function.setEirValue(function);
        if (result == null) {
            this.result = null;
        } else {
            this.result = new EirOperand(this, EirOperand.Effect.DEFINITION, resultLocation.category().asSet());
            this.result.setRequiredLocation(resultLocation);
            this.result.setEirValue(result);
        }
        if (arguments == null) {
            this.arguments = null;
        } else {
            this.arguments = new EirOperand[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                EirLocation location = argumentLocations[i];
                if (location instanceof EirStackSlot) {
                    location = methodGeneration.canonicalizeStackSlot((EirStackSlot) location);
                }
                final EirOperand argument = new EirOperand(this, EirOperand.Effect.USE, argumentLocations[i].category().asSet());
                argument.setRequiredLocation(argumentLocations[i]);
                argument.setEirValue(arguments[i]);
                this.arguments[i] = argument;
            }
        }
        final EirRegister[] callerSavedRegisters = abi.callerSavedRegisterArray();
        callerSavedOperands = new EirOperand[callerSavedRegisters.length];
        for (int i = 0; i < callerSavedRegisters.length; i++) {
            final EirRegister register = callerSavedRegisters[i];
            if (register != resultLocation) {
                callerSavedOperands[i] = new EirOperand(this, EirOperand.Effect.DEFINITION, register.category().asSet());
                callerSavedOperands[i].setRequiredLocation(register);
                final EirVariable variable = methodGeneration.makeRegisterVariable(register);
                callerSavedOperands[i].setEirValue(variable);
            }
        }
    }

    @Override
    public void addStackReferenceMap(WordWidth stackSlotWidth, ByteArrayBitMap map) {
        super.addStackReferenceMap(stackSlotWidth, map);
        if (arguments != null) {
            for (EirOperand argument : arguments) {
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
        visitor.run(function);
        if (result != null) {
            visitor.run(result);
        }
        if (arguments != null) {
            for (EirOperand argument : arguments) {
                visitor.run(argument);
            }
        }
        if (callerSavedOperands != null) {
            for (EirOperand callerSaveOperand : callerSavedOperands) {
                if (callerSaveOperand != null) {
                    visitor.run(callerSaveOperand);
                }
            }
        }
        super.visitOperands(visitor);
    }

    @Override
    public String toString() {
        String s = super.toString() + " " + function;
        if (result != null) {
            s = result.toString() + " := " + s;
        }
        if (arguments != null) {
            s += " (" + Arrays.toString(arguments) + ")";
        }
        if (callerSavedOperands != null) {
            s += " [Caller saved: " + Arrays.toString(callerSavedOperands) + "]";
        }
        return s + " " + javaFrameDescriptor();
    }

    @Override
    public void acceptVisitor(EirInstructionVisitor visitor) throws InvocationTargetException {
        visitor.visit(this);
    }

}
