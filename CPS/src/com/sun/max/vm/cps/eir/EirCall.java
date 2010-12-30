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

import java.lang.reflect.*;

import com.sun.max.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.collect.*;

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

    public final EirOperand[] arguments;

    public final boolean isNativeFunctionCall;

    protected EirCall(EirBlock block,
                      EirABI abi, EirValue result, EirLocation resultLocation,
                      EirValue function, PoolSet<EirLocationCategory> functionLocationCategories,
                      EirValue[] arguments, EirLocation[] argumentLocations,
                      boolean isNativeFunctionCall, EirMethodGeneration methodGeneration) {
        super(block);
        this.isNativeFunctionCall = isNativeFunctionCall;
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
                EirLocationCategory category = register.category();
                callerSavedOperands[i] = new EirOperand(this, EirOperand.Effect.DEFINITION, category.asSet());
                callerSavedOperands[i].setRequiredLocation(register);
                final EirVariable variable = methodGeneration.makeRegisterVariable(register);
                callerSavedOperands[i].setEirValue(variable);
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
            s += " (" + Utils.toString(arguments, ", ") + ")";
        }
        if (callerSavedOperands != null) {
            s += " [Caller saved: " + Utils.toString(callerSavedOperands, ", ") + "]";
        }
        s += " " + javaFrameDescriptor();
        if (isNativeFunctionCall) {
            s += " <native function call>";
        }
        return s;
    }

    @Override
    public void acceptVisitor(EirInstructionVisitor visitor) throws InvocationTargetException {
        visitor.visit(this);
    }

    @Override
    public void addFrameReferenceMap(WordWidth stackSlotWidth, ByteArrayBitMap map) {
        super.addFrameReferenceMap(stackSlotWidth, map);
        if (arguments != null) {
            for (EirOperand argument : arguments) {
                if (argument.kind().isReference && argument.location() instanceof EirStackSlot) {
                    final EirStackSlot stackSlot = (EirStackSlot) argument.location();
                    final int stackSlotBitIndex = stackSlot.offset / stackSlotWidth.numberOfBytes;
                    map.set(stackSlotBitIndex);
                }
            }
        }
    }
}
