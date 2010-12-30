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

import static com.sun.max.vm.cps.eir.EirLocationCategory.*;

import com.sun.max.asm.amd64.*;
import com.sun.max.asm.x86.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.type.*;

public final class AMD64EirStore extends AMD64EirPointerOperation {

    private static PoolSet<EirLocationCategory> valueLocationCategories(Kind kind) {
        switch (kind.asEnum) {
            case FLOAT:
            case DOUBLE:
                return F;
            case LONG:
            case WORD:
            case REFERENCE:
                return G;
            default:
                return G_I32;
        }
    }

    public AMD64EirStore(EirBlock block, Kind kind, EirValue value, EirValue pointer) {
        super(block, kind, value, EirOperand.Effect.USE, valueLocationCategories(kind), pointer);
    }

    public AMD64EirStore(EirBlock block, Kind kind, EirValue value, EirValue pointer, Kind offsetKind, EirValue offset) {
        super(block, kind, value, EirOperand.Effect.USE, valueLocationCategories(kind), pointer, offsetKind, offset);
    }

    public AMD64EirStore(EirBlock block, Kind kind, EirValue value, EirValue pointer, EirValue index) {
        super(block, kind, value, EirOperand.Effect.USE, valueLocationCategories(kind), pointer, index);
    }

    public AMD64EirStore(EirBlock block, Kind kind, EirValue value, EirValue pointer, EirValue displacement, EirValue index) {
        super(block, kind, value, EirOperand.Effect.USE, valueLocationCategories(kind), pointer, displacement, index);
    }

    public EirOperand valueOperand() {
        return destinationOperand();
    }

    public AMD64EirRegister.General valueGeneralRegister() {
        return destinationGeneralRegister();
    }

    public AMD64EirRegister.XMM valueXMMRegister() {
        return destinationXMMRegister();
    }

    @Override
    public String toString() {
        return "store-" + kind().character + " " + addressString() + " := " + valueOperand();
    }

    @Override
    protected void translateWithoutOffsetWithoutIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister) {
        switch (kind().asEnum) {
            case BYTE:
            case BOOLEAN: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(pointerRegister.indirect(), valueGeneralRegister().as8());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movb(pointerRegister.indirect(), valueOperand().location().asImmediate().value().toByte());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case SHORT:
            case CHAR: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(pointerRegister.indirect(), valueGeneralRegister().as16());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movw(pointerRegister.indirect(), valueOperand().location().asImmediate().value().toShort());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case INT: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(pointerRegister.indirect(), valueGeneralRegister().as32());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movl(pointerRegister.indirect(), valueOperand().location().asImmediate().value().toInt());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(pointerRegister.indirect(), valueGeneralRegister().as64());
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(pointerRegister.indirect(), valueXMMRegister().as());
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(pointerRegister.indirect(), valueXMMRegister().as());
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    protected void translateWithoutOffsetWithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, AMD64GeneralRegister64 indexRegister) {
        switch (kind().asEnum) {
            case BYTE:
            case BOOLEAN: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(pointerRegister.base(), indexRegister.index(), Scale.SCALE_1, valueGeneralRegister().as8());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movb(pointerRegister.base(), indexRegister.index(), Scale.SCALE_1, valueOperand().location().asImmediate().value().toByte());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case SHORT:
            case CHAR: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(pointerRegister.base(), indexRegister.index(), Scale.SCALE_2, valueGeneralRegister().as16());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movw(pointerRegister.base(), indexRegister.index(), Scale.SCALE_2, valueOperand().location().asImmediate().value().toShort());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case INT: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(pointerRegister.base(), indexRegister.index(), Scale.SCALE_4, valueGeneralRegister().as32());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movl(pointerRegister.base(), indexRegister.index(), Scale.SCALE_4, valueOperand().location().asImmediate().value().toInt());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(pointerRegister.base(), indexRegister.index(), Scale.SCALE_8, valueGeneralRegister().as64());
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(pointerRegister.base(), indexRegister.index(), Scale.SCALE_4, valueXMMRegister().as());
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(pointerRegister.base(), indexRegister.index(), Scale.SCALE_8, valueXMMRegister().as());
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    protected void translateWithRegisterOffsetWithoutIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, AMD64GeneralRegister64 offsetRegister) {
        switch (kind().asEnum) {
            case BYTE:
            case BOOLEAN: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1, valueGeneralRegister().as8());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movb(pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1, valueOperand().location().asImmediate().value().toByte());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case SHORT:
            case CHAR: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1, valueGeneralRegister().as16());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movw(pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1, valueOperand().location().asImmediate().value().toShort());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case INT: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1, valueGeneralRegister().as32());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movl(pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1, valueOperand().location().asImmediate().value().toInt());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1, valueGeneralRegister().as64());
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1, valueXMMRegister().as());
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1, valueXMMRegister().as());
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    protected void translateWithRegisterOffsetWithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, AMD64GeneralRegister64 offsetRegister, AMD64GeneralRegister64 indexRegister) {
        final AMD64EirRegister.General eirScratchRegister = (AMD64EirRegister.General) emitter.abi().getScratchRegister(Kind.LONG);
        final AMD64GeneralRegister64 scratchRegister = eirScratchRegister.as64();
        emitter.assembler().mov(scratchRegister, pointerRegister);
        emitter.assembler().add(scratchRegister, offsetRegister);
        switch (kind().asEnum) {
            case BYTE:
            case BOOLEAN: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(scratchRegister.base(), indexRegister.index(), Scale.SCALE_1, valueGeneralRegister().as8());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movb(scratchRegister.base(), indexRegister.index(), Scale.SCALE_1, valueOperand().location().asImmediate().value().toByte());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case SHORT:
            case CHAR: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(scratchRegister.base(), indexRegister.index(), Scale.SCALE_2, valueGeneralRegister().as16());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movw(scratchRegister.base(), indexRegister.index(), Scale.SCALE_2, valueOperand().location().asImmediate().value().toShort());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case INT: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(scratchRegister.base(), indexRegister.index(), Scale.SCALE_4, valueGeneralRegister().as32());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movl(scratchRegister.base(), indexRegister.index(), Scale.SCALE_4, valueOperand().location().asImmediate().value().toInt());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(scratchRegister.base(), indexRegister.index(), Scale.SCALE_8, valueGeneralRegister().as64());
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(scratchRegister.base(), indexRegister.index(), Scale.SCALE_4, valueXMMRegister().as());
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(scratchRegister.base(), indexRegister.index(), Scale.SCALE_8, valueXMMRegister().as());
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    protected void translateWithImmediateOffset8WithoutIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, byte offset8) {
        switch (kind().asEnum) {
            case BYTE:
            case BOOLEAN: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(offset8, pointerRegister.indirect(), valueGeneralRegister().as8());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movb(offset8, pointerRegister.indirect(), valueOperand().location().asImmediate().value().toByte());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case SHORT:
            case CHAR: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(offset8, pointerRegister.indirect(), valueGeneralRegister().as16());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movw(offset8, pointerRegister.indirect(), valueOperand().location().asImmediate().value().toShort());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case INT: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(offset8, pointerRegister.indirect(), valueGeneralRegister().as32());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movl(offset8, pointerRegister.indirect(), valueOperand().location().asImmediate().value().toInt());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(offset8, pointerRegister.indirect(), valueGeneralRegister().as64());
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(offset8, pointerRegister.indirect(), valueXMMRegister().as());
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(offset8, pointerRegister.indirect(), valueXMMRegister().as());
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    protected void translateWithImmediateOffset8WithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, byte offset8, AMD64GeneralRegister64 indexRegister) {
        switch (kind().asEnum) {
            case BYTE:
            case BOOLEAN: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_1, valueGeneralRegister().as8());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movb(offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_1, valueOperand().location().asImmediate().value().toByte());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case SHORT:
            case CHAR: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_2, valueGeneralRegister().as16());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movw(offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_2, valueOperand().location().asImmediate().value().toShort());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case INT: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_4, valueGeneralRegister().as32());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movl(offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_4, valueOperand().location().asImmediate().value().toInt());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_8, valueGeneralRegister().as64());
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_4, valueXMMRegister().as());
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_8, valueXMMRegister().as());
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    protected void translateWithImmediateOffset32WithoutIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, int offset32) {
        switch (kind().asEnum) {
            case BYTE:
            case BOOLEAN: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(offset32, pointerRegister.indirect(), valueGeneralRegister().as8());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movb(offset32, pointerRegister.indirect(), valueOperand().location().asImmediate().value().toByte());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case SHORT:
            case CHAR: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(offset32, pointerRegister.indirect(), valueGeneralRegister().as16());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movw(offset32, pointerRegister.indirect(), valueOperand().location().asImmediate().value().toShort());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case INT: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(offset32, pointerRegister.indirect(), valueGeneralRegister().as32());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movl(offset32, pointerRegister.indirect(), valueOperand().location().asImmediate().value().toInt());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(offset32, pointerRegister.indirect(), valueGeneralRegister().as64());
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(offset32, pointerRegister.indirect(), valueXMMRegister().as());
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(offset32, pointerRegister.indirect(), valueXMMRegister().as());
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    protected void translateWithImmediateOffset32WithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, int offset32, AMD64GeneralRegister64 indexRegister) {
        switch (kind().asEnum) {
            case BYTE:
            case BOOLEAN: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_1, valueGeneralRegister().as8());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movb(offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_1, valueOperand().location().asImmediate().value().toByte());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case SHORT:
            case CHAR: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_2, valueGeneralRegister().as16());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movw(offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_2, valueOperand().location().asImmediate().value().toShort());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case INT: {
                switch (valueOperand().location().category()) {
                    case INTEGER_REGISTER: {
                        emitter.assembler().mov(offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_4, valueGeneralRegister().as32());
                        break;
                    }
                    case IMMEDIATE_32: {
                        emitter.assembler().movl(offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_4, valueOperand().location().asImmediate().value().toInt());
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_8, valueGeneralRegister().as64());
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_4, valueXMMRegister().as());
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_8, valueXMMRegister().as());
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
        visitor.visit(this);
    }
}
