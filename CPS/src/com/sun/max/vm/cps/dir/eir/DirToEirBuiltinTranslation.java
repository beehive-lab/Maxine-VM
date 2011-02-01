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
package com.sun.max.vm.cps.dir.eir;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Platform-independent aspects of translations from DIR to EIR for builtins.
 *
 * @author Bernd Mathiske
 */
public abstract class DirToEirBuiltinTranslation extends BuiltinAdapter<DirValue> {

    protected final DirToEirInstructionTranslation instructionTranslation;
    protected final DirJavaFrameDescriptor javaFrameDescriptor;

    protected DirToEirInstructionTranslation instructionTranslation() {
        return instructionTranslation;
    }

    protected DirToEirMethodTranslation methodTranslation() {
        return instructionTranslation.methodTranslation;
    }

    protected DirToEirBuiltinTranslation(DirToEirInstructionTranslation instructionTranslation, DirJavaFrameDescriptor javaFrameDescriptor) {
        this.instructionTranslation = instructionTranslation;
        this.javaFrameDescriptor = javaFrameDescriptor;
    }

    public EirABI abi() {
        return instructionTranslation.abi();
    }

    public void addInstruction(EirInstruction instruction) {
        instructionTranslation.addInstruction(instruction);
    }

    public EirBlock eirBlock() {
        return instructionTranslation.eirBlock();
    }

    public void setEirBlock(EirBlock eirBlock) {
        instructionTranslation.setBlock(eirBlock);
    }

    public EirValue dirToEirValue(DirValue dirValue) {
        return instructionTranslation.dirToEirValue(dirValue);
    }

    public EirConstant dirToEirConstant(DirConstant dirConstant) {
        return instructionTranslation.dirToEirConstant(dirConstant);
    }

    public EirVariable createEirVariable(Kind kind) {
        return instructionTranslation.createEirVariable(kind);
    }

    public EirConstant createEirConstant(Value value) {
        return instructionTranslation.createEirConstant(value);
    }

    public EirInstruction assign(Kind kind, EirValue destination, EirValue source) {
        return instructionTranslation.assign(kind, destination, source);
    }

    @Override
    public final void visitCall(Call builtin, DirValue dirResult, DirValue[] dirArguments) {
        EirValue address;
        EirValue[] arguments = null;
        EirLocation[] argumentLocations = null;
        switch (dirArguments.length) {
            case 0: {
                // Special direct call that will be linked later (e.g. by the JIT) - provide an arbitrary placeholder callee:
                address = new EirMethodValue(methodTranslation().classMethodActor());
                break;
            }
            case 1: {
                // Regular indirect call.
                address = dirToEirValue(dirArguments[0]);
                break;
            }
            case 2: {
                // Indirect call with receiver.
                address = dirToEirValue(dirArguments[0]);
                arguments = new EirValue[]{dirToEirValue(dirArguments[1])};
                argumentLocations = methodTranslation().abi.getParameterLocations(EirStackSlot.Purpose.LOCAL, Kind.REFERENCE);
                break;
            }
            default: {
                throw ProgramError.unknownCase();
            }
        }
        final EirValue result = dirToEirValue(dirResult);
        final EirLocation resultLocation = result == null ? null : methodTranslation().abi.getResultLocation(result.kind());
        final EirCall instruction = methodTranslation().createCall(eirBlock(), methodTranslation().abi, result, resultLocation, address, arguments, argumentLocations, false);
        addInstruction(instruction);
        if (!methodTranslation().isTemplate()) {
            instruction.setEirJavaFrameDescriptor(methodTranslation().dirToEirJavaFrameDescriptor(javaFrameDescriptor, instruction));
        }
    }

    @Override
    public void visitPause(Pause builtin, DirValue result, DirValue[] arguments) {
    }
}
