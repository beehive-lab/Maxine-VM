/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.eir.amd64;

import static com.sun.max.asm.x86.Scale.*;
import static com.sun.max.vm.cps.eir.EirLocationCategory.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64EirCompareAndSwap extends AMD64EirPointerOperation {

    @CONSTANT
    private EirOperand comparableAndResultOperand;

    private void initialize(EirValue comparableAndResult) {
        comparableAndResultOperand = new EirOperand(this, EirOperand.Effect.UPDATE, G);
        comparableAndResultOperand.setRequiredRegister(AMD64EirRegister.General.RAX);
        comparableAndResultOperand.setEirValue(comparableAndResult);
    }

    public AMD64EirCompareAndSwap(EirBlock block, Kind kind, EirValue newValue, EirValue pointer, EirValue comparableAndResult) {
        super(block, kind, newValue, EirOperand.Effect.USE, G, pointer);
        initialize(comparableAndResult);
    }

    public AMD64EirCompareAndSwap(EirBlock block, Kind kind, EirValue newValue, EirValue pointer, Kind offsetKind, EirValue offset, EirValue comparableAndResult) {
        super(block, kind, newValue, EirOperand.Effect.USE, G, pointer, offsetKind, offset);
        initialize(comparableAndResult);
    }

    public AMD64EirCompareAndSwap(EirBlock block, Kind kind, EirValue newValue, EirValue pointer, EirValue index, EirValue comparableAndResult) {
        super(block, kind, newValue, EirOperand.Effect.USE, G, pointer, index);
        initialize(comparableAndResult);
    }

    public AMD64EirCompareAndSwap(EirBlock block, Kind kind, EirValue newValue, EirValue pointer, EirValue displacement, EirValue index, EirValue comparableAndResult) {
        super(block, kind, newValue, EirOperand.Effect.USE, G, pointer, displacement, index);
        initialize(comparableAndResult);
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        super.visitOperands(visitor);
        visitor.run(comparableAndResultOperand);
    }

    public AMD64GeneralRegister32 newValueRegister32() {
        return destinationGeneralRegister().as32();
    }

    public AMD64GeneralRegister64 newValueRegister64() {
        return destinationGeneralRegister().as64();
    }

    @Override
    protected void translateWithoutOffsetWithoutIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister) {
        emitter.assembler().lock();
        switch (kind().asEnum) {
            case INT: {
                emitter.assembler().cmpxchg(pointerRegister.indirect(), newValueRegister32());
                break;
            }
            case WORD:
            case REFERENCE: {
                emitter.assembler().cmpxchg(pointerRegister.indirect(), newValueRegister64());
                break;
            }
            default: {
                FatalError.unimplemented();
            }
        }
    }

    @Override
    protected void translateWithoutOffsetWithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, AMD64GeneralRegister64 indexRegister) {
        FatalError.unimplemented();
    }

    @Override
    protected void translateWithRegisterOffsetWithoutIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, AMD64GeneralRegister64 offsetRegister) {
        emitter.assembler().lock();
        switch (kind().asEnum) {
            case INT: {
                emitter.assembler().cmpxchg(pointerRegister.base(), offsetRegister.index(), SCALE_1, newValueRegister32());
                break;
            }
            case WORD:
            case REFERENCE: {
                emitter.assembler().cmpxchg(pointerRegister.base(), offsetRegister.index(), SCALE_1, newValueRegister64());
                break;
            }
            default: {
                FatalError.unimplemented();
            }
        }
    }

    @Override
    protected void translateWithRegisterOffsetWithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, AMD64GeneralRegister64 offsetRegister, AMD64GeneralRegister64 indexRegister) {
        FatalError.unimplemented();
    }

    @Override
    protected void translateWithImmediateOffset8WithoutIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, byte offset8) {
        emitter.assembler().lock();
        switch (kind().asEnum) {
            case INT: {
                emitter.assembler().cmpxchg(offset8, pointerRegister.indirect(), newValueRegister32());
                break;
            }
            case WORD:
            case REFERENCE: {
                emitter.assembler().cmpxchg(offset8, pointerRegister.indirect(), newValueRegister64());
                break;
            }
            default: {
                FatalError.unimplemented();
            }
        }
    }

    @Override
    protected void translateWithImmediateOffset8WithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, byte offset8, AMD64GeneralRegister64 indexRegister) {
        FatalError.unimplemented();
    }

    @Override
    protected void translateWithImmediateOffset32WithoutIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, int offset32) {
        emitter.assembler().lock();
        switch (kind().asEnum) {
            case INT: {
                emitter.assembler().cmpxchg(offset32, pointerRegister.indirect(), newValueRegister32());
                break;
            }
            case WORD:
            case REFERENCE: {
                emitter.assembler().cmpxchg(offset32, pointerRegister.indirect(), newValueRegister64());
                break;
            }
            default: {
                FatalError.unimplemented();
            }
        }
    }

    @Override
    protected void translateWithImmediateOffset32WithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, int offset32, AMD64GeneralRegister64 indexRegister) {
        FatalError.unimplemented();
    }

    @Override
    public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

}
