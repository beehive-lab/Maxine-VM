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
package com.sun.max.vm.cps.eir.amd64;

import static com.sun.max.vm.cps.eir.EirLocationCategory.*;

import com.sun.max.asm.amd64.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.amd64.AMD64EirTargetEmitter.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Assignments from one AMD64 EIR location to another.
 * Typically these assemble to "mov" instructions in some form.
 *
 * @author Bernd Mathiske
 */
public class AMD64EirAssignment extends AMD64EirBinaryOperation.Move implements EirAssignment {

    private final Kind kind;
    private Type type = Type.NORMAL;

    public Type type() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Kind kind() {
        return kind;
    }

    public static PoolSet<EirLocationCategory> destinationLocationCategories(Kind kind) {
        switch (kind.asEnum) {
            case INT:
            case LONG:
            case WORD:
            case REFERENCE:
                return G_S;
            case FLOAT:
            case DOUBLE:
                return F_S;
            default:
                ProgramError.unknownCase();
                return null;
        }
    }

    private static PoolSet<EirLocationCategory> sourceLocationCategories(Kind kind) {
        switch (kind.asEnum) {
            case INT:
                return G_I32_L_S;
            case LONG:
            case WORD:
                return B_G_I32_I64_L_S;
            case REFERENCE:
                return G_I32_I64_L_S;
            case FLOAT:
            case DOUBLE:
                return F_L_S;
            default:
                ProgramError.unknownCase();
                return null;
        }
    }

    public AMD64EirAssignment(EirBlock block, Kind kind, EirValue destination, EirValue source) {
        super(block, destination, destinationLocationCategories(kind), source, sourceLocationCategories(kind), false);
        this.kind = kind;
        switch (kind.asEnum) {
            case INT:
            case LONG:
            case WORD:
            case REFERENCE:
                destinationOperand().setPreferredRegister(AMD64EirRegister.General.RAX);
                break;
            default:
                break;
        }
    }

    private static void emit_I32(AMD64EirTargetEmitter emitter, EirLocation destinationLocation, EirLocation sourceLocation) {
        switch (destinationLocation.category()) {
            case INTEGER_REGISTER: {
                final AMD64EirRegister.General destinationRegister = (AMD64EirRegister.General) destinationLocation;
                switch (sourceLocation.category()) {
                    case INTEGER_REGISTER: {
                        final AMD64EirRegister.General sourceRegister = (AMD64EirRegister.General) sourceLocation;
                        emitter.assembler().movsxd(destinationRegister.as64(), sourceRegister.as32());
                        break;
                    }
                    case IMMEDIATE_32: {
                        final Value value = sourceLocation.asImmediate().value();
                        if (value.isZero()) {
                            emitter.assembler().xor(destinationRegister.as64(), destinationRegister.as64());
                        } else {
                            emitter.assembler().movq(destinationRegister.as64(), value.toInt());
                        }
                        break;
                    }
                    case LITERAL: {
                        emitter.assembler().rip_movsxd(destinationRegister.as64(), sourceLocation.asLiteral().asLabel());
                        break;
                    }
                    case STACK_SLOT: {
                        final StackAddress source = emitter.stackAddress(sourceLocation.asStackSlot());
                        if (source.isOffset8Bit()) {
                            emitter.assembler().movsxd(destinationRegister.as64(), source.offset8(), source.base());
                        } else {
                            emitter.assembler().movsxd(destinationRegister.as64(), source.offset32(), source.base());
                        }
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case STACK_SLOT: {
                final StackAddress destination = emitter.stackAddress(destinationLocation.asStackSlot());
                if (destination.isOffset8Bit()) {
                    switch (sourceLocation.category()) {
                        case INTEGER_REGISTER: {
                            final AMD64EirRegister.General sourceRegister = (AMD64EirRegister.General) sourceLocation;
                            final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            emitter.assembler().movsxd(scratchRegister.as64(), sourceRegister.as32());
                            emitter.assembler().mov(destination.offset8(), destination.base(), scratchRegister.as64());
                            break;
                        }
                        case IMMEDIATE_32: {
                            final Value value = sourceLocation.asImmediate().value();
                            final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            emitter.assembler().movq(scratchRegister.as64(), value.toInt());
                            emitter.assembler().mov(destination.offset8(), destination.base(), scratchRegister.as64());
                            break;
                        }
                        case LITERAL: {
                            final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            emitter.assembler().rip_movsxd(scratchRegister.as64(), sourceLocation.asLiteral().asLabel());
                            emitter.assembler().mov(destination.offset8(), destination.base(), scratchRegister.as64());
                            break;
                        }
                        case STACK_SLOT: {
                            final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            final StackAddress source = emitter.stackAddress(sourceLocation.asStackSlot());
                            if (source.isOffset8Bit()) {
                                emitter.assembler().movsxd(scratchRegister.as64(), source.offset8(), source.base());
                                emitter.assembler().mov(destination.offset8(), destination.base(), scratchRegister.as64());
                            } else {
                                emitter.assembler().movsxd(scratchRegister.as64(), source.offset32(), source.base());
                                emitter.assembler().mov(destination.offset8(), destination.base(), scratchRegister.as64());
                            }
                            break;
                        }
                        default: {
                            impossibleLocationCategory();
                            break;
                        }
                    }
                } else {
                    switch (sourceLocation.category()) {
                        case INTEGER_REGISTER: {
                            final AMD64EirRegister.General sourceRegister = (AMD64EirRegister.General) sourceLocation;
                            final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            emitter.assembler().movsxd(scratchRegister.as64(), sourceRegister.as32());
                            emitter.assembler().mov(destination.offset32(), destination.base(), scratchRegister.as64());
                            break;
                        }
                        case IMMEDIATE_32: {
                            final Value value = sourceLocation.asImmediate().value();
                            final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            emitter.assembler().mov(scratchRegister.as64(), value.toInt());
                            emitter.assembler().mov(destination.offset32(), destination.base(), scratchRegister.as64());
                            break;
                        }
                        case LITERAL: {
                            final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            emitter.assembler().rip_movsxd(scratchRegister.as64(), sourceLocation.asLiteral().asLabel());
                            emitter.assembler().mov(destination.offset32(), destination.base(), scratchRegister.as64());
                            break;
                        }
                        case STACK_SLOT: {
                            final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            final StackAddress source = emitter.stackAddress(sourceLocation.asStackSlot());
                            if (source.isOffset8Bit()) {
                                emitter.assembler().movsxd(scratchRegister.as64(), source.offset8(), source.base());
                                emitter.assembler().mov(destination.offset32(), destination.base(), scratchRegister.as64());
                            } else {
                                emitter.assembler().movsxd(scratchRegister.as64(), source.offset32(), source.base());
                                emitter.assembler().mov(destination.offset32(), destination.base(), scratchRegister.as64());
                            }
                            break;
                        }
                        default: {
                            impossibleLocationCategory();
                            break;
                        }
                    }
                }
                break;
            }
            default: {
                impossibleLocationCategory();
                break;
            }
        }
    }

    private static void emit_I64(AMD64EirTargetEmitter emitter, Kind kind, EirLocation destinationLocation, EirLocation sourceLocation) {
        switch (destinationLocation.category()) {
            case INTEGER_REGISTER: {
                final AMD64EirRegister.General destinationRegister = (AMD64EirRegister.General) destinationLocation;
                final AMD64GeneralRegister64 destinationRegister64 = destinationRegister.as64();
                switch (sourceLocation.category()) {
                    case INTEGER_REGISTER: {
                        final AMD64EirRegister.General sourceRegister = (AMD64EirRegister.General) sourceLocation;
                        emitter.assembler().mov(destinationRegister64, sourceRegister.as64());
                        break;
                    }
                    case IMMEDIATE_32: {
                        final Value value = sourceLocation.asImmediate().value();
                        if (value.isZero()) {
                            emitter.assembler().xor(destinationRegister64, destinationRegister64);
                        } else {
                            emitter.assembler().movq(destinationRegister64, value.toInt());
                        }
                        break;
                    }
                    case IMMEDIATE_64: {
                        final Value value = sourceLocation.asImmediate().value();
                        if (value.isZero()) {
                            emitter.assembler().xor(destinationRegister64, destinationRegister64);
                        } else {
                            emitter.assembler().mov(destinationRegister64, sourceLocation.asImmediate().value().toLong());
                        }
                        break;
                    }
                    case LITERAL: {
                        emitter.assembler().rip_mov(destinationRegister64, sourceLocation.asLiteral().asLabel());
                        break;
                    }
                    case STACK_SLOT: {
                        final StackAddress source = emitter.stackAddress(sourceLocation.asStackSlot());
                        if (source.isOffset8Bit()) {
                            emitter.assembler().mov(destinationRegister64, source.offset8(), source.base());
                        } else {
                            emitter.assembler().mov(destinationRegister64, source.offset32(), source.base());
                        }
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case STACK_SLOT: {
                final StackAddress destination = emitter.stackAddress(destinationLocation.asStackSlot());
                if (destination.isOffset8Bit()) {
                    switch (sourceLocation.category()) {
                        case INTEGER_REGISTER: {
                            final AMD64EirRegister.General sourceRegister = (AMD64EirRegister.General) sourceLocation;
                            emitter.assembler().mov(destination.offset8(), destination.base(), sourceRegister.as64());
                            break;
                        }
                        case IMMEDIATE_32: {
                            final Value value = sourceLocation.asImmediate().value();
                            final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.abi().getScratchRegister(kind);
                            emitter.assembler().movq(scratchRegister.as64(), value.toInt());
                            emitter.assembler().mov(destination.offset8(), destination.base(), scratchRegister.as64());
                            break;
                        }
                        case IMMEDIATE_64:
                        case LITERAL:
                        case STACK_SLOT: {
                            final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.loadIntoScratchRegister(kind, sourceLocation);
                            emitter.assembler().mov(destination.offset8(), destination.base(), scratchRegister.as64());
                            break;
                        }
                        default: {
                            impossibleLocationCategory();
                            break;
                        }
                    }
                } else {
                    switch (sourceLocation.category()) {
                        case INTEGER_REGISTER: {
                            final AMD64EirRegister.General sourceRegister = (AMD64EirRegister.General) sourceLocation;
                            emitter.assembler().mov(destination.offset32(), destination.base(), sourceRegister.as64());
                            break;
                        }
                        case IMMEDIATE_32: {
                            final Value value = sourceLocation.asImmediate().value();
                            final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.abi().getScratchRegister(kind);
                            emitter.assembler().movq(scratchRegister.as64(), value.toInt());
                            emitter.assembler().mov(destination.offset32(), destination.base(), scratchRegister.as64());
                            break;
                        }
                        case IMMEDIATE_64:
                        case LITERAL:
                        case STACK_SLOT: {
                            final AMD64EirRegister.General scratchRegister = (AMD64EirRegister.General) emitter.loadIntoScratchRegister(kind, sourceLocation);
                            emitter.assembler().mov(destination.offset32(), destination.base(), scratchRegister.as64());
                            break;
                        }
                        default: {
                            impossibleLocationCategory();
                            break;
                        }
                    }
                }
                break;
            }
            default: {
                impossibleLocationCategory();
                break;
            }
        }
    }

    private static void emit_F32(AMD64EirTargetEmitter emitter, EirLocation destinationLocation, EirLocation sourceLocation) {
        switch (destinationLocation.category()) {
            case FLOATING_POINT_REGISTER: {
                final AMD64EirRegister.XMM destinationRegister = (AMD64EirRegister.XMM) destinationLocation;
                final AMD64XMMRegister destinationRegisterXMM = destinationRegister.as();
                switch (sourceLocation.category()) {
                    case FLOATING_POINT_REGISTER: {
                        final AMD64EirRegister.XMM sourceRegister = (AMD64EirRegister.XMM) sourceLocation;
                        emitter.assembler().movss(destinationRegisterXMM, sourceRegister.as());
                        break;
                    }
                    case LITERAL: {
                        emitter.assembler().rip_movss(destinationRegisterXMM, sourceLocation.asLiteral().asLabel());
                        break;
                    }
                    case STACK_SLOT: {
                        final StackAddress source = emitter.stackAddress(sourceLocation.asStackSlot());
                        if (source.isOffset8Bit()) {
                            emitter.assembler().movss(destinationRegisterXMM, source.offset8(), source.base());
                        } else {
                            emitter.assembler().movss(destinationRegisterXMM, source.offset32(), source.base());
                        }
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case STACK_SLOT: {
                final StackAddress destination = emitter.stackAddress(destinationLocation.asStackSlot());
                if (destination.isOffset8Bit()) {
                    switch (sourceLocation.category()) {
                        case FLOATING_POINT_REGISTER: {
                            final AMD64EirRegister.XMM sourceRegister = (AMD64EirRegister.XMM) sourceLocation;
                            emitter.assembler().movss(destination.offset8(), destination.base(), sourceRegister.as());
                            break;
                        }
                        case LITERAL:
                        case STACK_SLOT: {
                            final AMD64EirRegister.XMM scratchRegister = (AMD64EirRegister.XMM) emitter.loadIntoScratchRegister(Kind.FLOAT, sourceLocation);
                            emitter.assembler().movss(destination.offset8(), destination.base(), scratchRegister.as());
                            break;
                        }
                        default: {
                            impossibleLocationCategory();
                            break;
                        }
                    }
                } else {
                    switch (sourceLocation.category()) {
                        case FLOATING_POINT_REGISTER: {
                            final AMD64EirRegister.XMM sourceRegister = (AMD64EirRegister.XMM) sourceLocation;
                            emitter.assembler().movss(destination.offset32(), destination.base(), sourceRegister.as());
                            break;
                        }
                        case LITERAL:
                        case STACK_SLOT: {
                            final AMD64EirRegister.XMM scratchRegister = (AMD64EirRegister.XMM) emitter.loadIntoScratchRegister(Kind.FLOAT, sourceLocation);
                            emitter.assembler().movss(destination.offset32(), destination.base(), scratchRegister.as());
                            break;
                        }
                        default: {
                            impossibleLocationCategory();
                            break;
                        }
                    }
                }
                break;
            }
            default: {
                impossibleLocationCategory();
                break;
            }
        }
    }

    private static void emit_F64(AMD64EirTargetEmitter emitter, EirLocation destinationLocation, EirLocation sourceLocation) {
        switch (destinationLocation.category()) {
            case FLOATING_POINT_REGISTER: {
                final AMD64EirRegister.XMM destinationRegister = (AMD64EirRegister.XMM) destinationLocation;
                final AMD64XMMRegister destinationRegisterXMM = destinationRegister.as();
                switch (sourceLocation.category()) {
                    case FLOATING_POINT_REGISTER: {
                        final AMD64EirRegister.XMM sourceRegister = (AMD64EirRegister.XMM) sourceLocation;
                        emitter.assembler().movsd(destinationRegisterXMM, sourceRegister.as());
                        break;
                    }
                    case LITERAL: {
                        emitter.assembler().rip_movsd(destinationRegisterXMM, sourceLocation.asLiteral().asLabel());
                        break;
                    }
                    case STACK_SLOT: {
                        final StackAddress source = emitter.stackAddress(sourceLocation.asStackSlot());
                        if (source.isOffset8Bit()) {
                            emitter.assembler().movsd(destinationRegisterXMM, source.offset8(), source.base());
                        } else {
                            emitter.assembler().movsd(destinationRegisterXMM, source.offset32(), source.base());
                        }
                        break;
                    }
                    default: {
                        impossibleLocationCategory();
                        break;
                    }
                }
                break;
            }
            case STACK_SLOT: {
                final StackAddress destination = emitter.stackAddress(destinationLocation.asStackSlot());
                if (destination.isOffset8Bit()) {
                    switch (sourceLocation.category()) {
                        case FLOATING_POINT_REGISTER: {
                            final AMD64EirRegister.XMM sourceRegister = (AMD64EirRegister.XMM) sourceLocation;
                            emitter.assembler().movsd(destination.offset8(), destination.base(), sourceRegister.as());
                            break;
                        }
                        case LITERAL:
                        case STACK_SLOT: {
                            final AMD64EirRegister.XMM scratchRegister = (AMD64EirRegister.XMM) emitter.loadIntoScratchRegister(Kind.DOUBLE, sourceLocation);
                            emitter.assembler().movsd(destination.offset8(), destination.base(), scratchRegister.as());
                            break;
                        }
                        default: {
                            impossibleLocationCategory();
                            break;
                        }
                    }
                } else {
                    switch (sourceLocation.category()) {
                        case FLOATING_POINT_REGISTER: {
                            final AMD64EirRegister.XMM sourceRegister = (AMD64EirRegister.XMM) sourceLocation;
                            emitter.assembler().movsd(destination.offset32(), destination.base(), sourceRegister.as());
                            break;
                        }
                        case LITERAL:
                        case STACK_SLOT: {
                            final AMD64EirRegister.XMM scratchRegister = (AMD64EirRegister.XMM) emitter.loadIntoScratchRegister(Kind.DOUBLE, sourceLocation);
                            emitter.assembler().movsd(destination.offset32(), destination.base(), scratchRegister.as());
                            break;
                        }
                        default: {
                            impossibleLocationCategory();
                            break;
                        }
                    }
                }
                break;
            }
            default: {
                impossibleLocationCategory();
                break;
            }
        }
    }

    public static void emit(AMD64EirTargetEmitter emitter, Kind kind, EirLocation destination, EirLocation source) {
        switch (kind.asEnum) {
            case BYTE:
            case BOOLEAN:
            case SHORT:
            case CHAR:
            case INT:
                emit_I32(emitter, destination, source);
                break;
            case LONG:
            case WORD:
            case REFERENCE:
                emit_I64(emitter, kind, destination, source);
                break;
            case FLOAT:
                emit_F32(emitter, destination, source);
                break;
            case DOUBLE:
                emit_F64(emitter, destination, source);
                break;
            default:
                ProgramError.unknownCase();
        }
    }

    @Override
    public void emit(AMD64EirTargetEmitter emitter) {
        emit(emitter, kind(), destinationOperand().location(), sourceOperand().location());
    }

    @Override
    public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        visitor.run(destinationOperand());
        visitor.run(sourceOperand());
    }

    @Override
    public String toString() {
        String result = "assign-" + kind + " " + destinationOperand() + " := " + sourceOperand();

        if (type() != Type.NORMAL) {
            result += " (" + type().toString() + ")";
        }

        return result;
    }

}
