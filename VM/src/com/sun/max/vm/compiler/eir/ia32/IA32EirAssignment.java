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
/*VCSID=2cec78ce-747f-442c-8633-26fcdce90b1f*/
package com.sun.max.vm.compiler.eir.ia32;

import static com.sun.max.vm.compiler.eir.EirLocationCategory.*;

import com.sun.max.asm.ia32.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.ia32.IA32EirTargetEmitter.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

//NOTE on recording of target code annotation for assignment of constant & literal:
//Currently, we track only assignment of constant & literal values to register. We do not
//keep track of assignment to stack slot.
//TODO: The current system of target code annotation is too dependent of a particular implementation of annotations,
//i.e., the Recorder are explicitly allocated. Ideally, we would like to obtain a recorder factory from some "compilation-specific" context,
//such that the factory provides method for all type of recorder (e.g., recorder for immediate value, for literal and so on).
//The factory would be for each compilation (in particular, different compilation may be set up differently).

//Laurent.

/**
 * Assignments from one IA32 EIR location to another.
 * Typically these assemble to "mov" instructions in some form.
 * 
 * @author Bernd Mathiske
 */
public class IA32EirAssignment extends IA32EirBinaryOperation.Move implements EirAssignment {

    private final Kind _kind;

    public Kind kind() {
        return _kind;
    }

    private static PoolSet<EirLocationCategory> destinationLocationCategories(Kind kind) {
        switch (kind.asEnum()) {
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
        switch (kind.asEnum()) {
            case INT:
                return G_I32_L_S;
            case LONG:
            case WORD:
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

    public IA32EirAssignment(EirBlock block, Kind kind, EirValue destination, EirValue source) {
        super(block, destination, destinationLocationCategories(kind), source, sourceLocationCategories(kind));
        _kind = kind;
        switch (kind.asEnum()) {
            case INT:
            case LONG:
            case WORD:
            case REFERENCE:
                destinationOperand().setPreferredRegister(IA32EirRegister.General.EAX);
                break;
            default:
                break;
        }
    }

    private static void emit_I32(IA32EirTargetEmitter emitter, EirLocation destinationLocation, EirLocation sourceLocation) {
        switch (destinationLocation.category()) {
            case INTEGER_REGISTER: {
                final IA32EirRegister.General destinationRegister = (IA32EirRegister.General) destinationLocation;
                switch (sourceLocation.category()) {
                    case INTEGER_REGISTER: {
                        final IA32EirRegister.General sourceRegister = (IA32EirRegister.General) sourceLocation;
                        emitter.assembler().mov(destinationRegister.as32(), sourceRegister.as32());
                        break;
                    }
                    case IMMEDIATE_32: {
                        final Value value = sourceLocation.asImmediate().value();
                        if (value.isZero()) {
                            emitter.assembler().xor(destinationRegister.as32(), destinationRegister.as32());
                        } else {
                            emitter.assembler().movl(destinationRegister.as32(), value.toInt());
                        }
                        break;
                    }
                    case LITERAL: {
                        emitter.assembler().m_mov(destinationRegister.as32(), sourceLocation.asLiteral().asLabel());
                        break;
                    }
                    case STACK_SLOT: {
                        final StackAddress source = emitter.stackAddress(sourceLocation.asStackSlot());
                        if (source.isOffset8Bit()) {
                            emitter.assembler().mov(destinationRegister.as32(), source.offset8(), source.base());
                        } else {
                            emitter.assembler().mov(destinationRegister.as32(), source.offset32(), source.base());
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
                            final IA32EirRegister.General sourceRegister = (IA32EirRegister.General) sourceLocation;
                            final IA32EirRegister.General scratchRegister = (IA32EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            emitter.assembler().mov(scratchRegister.as32(), sourceRegister.as32());
                            emitter.assembler().mov(destination.offset8(), destination.base(), scratchRegister.as32());
                            break;
                        }
                        case IMMEDIATE_32: {
                            final Value value = sourceLocation.asImmediate().value();
                            final IA32EirRegister.General scratchRegister = (IA32EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            emitter.assembler().movl(scratchRegister.as32(), value.toInt());
                            emitter.assembler().mov(destination.offset8(), destination.base(), scratchRegister.as32());
                            break;
                        }
                        case LITERAL: {
                            final IA32EirRegister.General scratchRegister = (IA32EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            emitter.assembler().m_mov(scratchRegister.as32(), sourceLocation.asLiteral().asLabel());
                            emitter.assembler().mov(destination.offset8(), destination.base(), scratchRegister.as32());
                            break;
                        }
                        case STACK_SLOT: {
                            final IA32EirRegister.General scratchRegister = (IA32EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            final StackAddress source = emitter.stackAddress(sourceLocation.asStackSlot());
                            if (source.isOffset8Bit()) {
                                emitter.assembler().mov(scratchRegister.as32(), source.offset8(), source.base());
                                emitter.assembler().mov(destination.offset8(), destination.base(), scratchRegister.as32());
                            } else {
                                emitter.assembler().mov(scratchRegister.as32(), source.offset32(), source.base());
                                emitter.assembler().mov(destination.offset8(), destination.base(), scratchRegister.as32());
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
                            final IA32EirRegister.General sourceRegister = (IA32EirRegister.General) sourceLocation;
                            final IA32EirRegister.General scratchRegister = (IA32EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            emitter.assembler().mov(scratchRegister.as32(), sourceRegister.as32());
                            emitter.assembler().mov(destination.offset32(), destination.base(), scratchRegister.as32());
                            break;
                        }
                        case IMMEDIATE_32: {
                            final Value value = sourceLocation.asImmediate().value();
                            final IA32EirRegister.General scratchRegister = (IA32EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            emitter.assembler().mov(scratchRegister.as32(), value.toInt());
                            emitter.assembler().mov(destination.offset32(), destination.base(), scratchRegister.as32());
                            break;
                        }
                        case LITERAL: {
                            final IA32EirRegister.General scratchRegister = (IA32EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            emitter.assembler().m_mov(scratchRegister.as32(), sourceLocation.asLiteral().asLabel());
                            emitter.assembler().mov(destination.offset32(), destination.base(), scratchRegister.as32());
                            break;
                        }
                        case STACK_SLOT: {
                            final IA32EirRegister.General scratchRegister = (IA32EirRegister.General) emitter.abi().getScratchRegister(Kind.INT);
                            final StackAddress source = emitter.stackAddress(sourceLocation.asStackSlot());
                            if (source.isOffset8Bit()) {
                                emitter.assembler().mov(scratchRegister.as32(), source.offset8(), source.base());
                                emitter.assembler().mov(destination.offset32(), destination.base(), scratchRegister.as32());
                            } else {
                                emitter.assembler().mov(scratchRegister.as32(), source.offset32(), source.base());
                                emitter.assembler().mov(destination.offset32(), destination.base(), scratchRegister.as32());
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

    private static void emit_I64(IA32EirTargetEmitter emitter, Kind kind, EirLocation destinationLocation, EirLocation sourceLocation) {
        switch (destinationLocation.category()) {
            case INTEGER_REGISTER: {
                final IA32EirRegister.General destinationRegister = (IA32EirRegister.General) destinationLocation;
                final IA32GeneralRegister32 destinationRegister64 = destinationRegister.as32();
                switch (sourceLocation.category()) {
                    case INTEGER_REGISTER: {
                        final IA32EirRegister.General sourceRegister = (IA32EirRegister.General) sourceLocation;
                        emitter.assembler().mov(destinationRegister64, sourceRegister.as32());
                        break;
                    }
                    case IMMEDIATE_32: {
                        final Value value = sourceLocation.asImmediate().value();
                        if (value.isZero()) {
                            emitter.assembler().xor(destinationRegister64, destinationRegister64);
                        } else {
                            emitter.assembler().movl(destinationRegister64, value.toInt());
                        }
                        break;
                    }
                    case IMMEDIATE_64: {
                        final Value value = sourceLocation.asImmediate().value();
                        if (value.isZero()) {
                            emitter.assembler().xor(destinationRegister64, destinationRegister64);
                        } else {
                            Problem.unimplemented();
                        }
                        break;
                    }
                    case LITERAL: {
                        emitter.assembler().m_mov(destinationRegister64, sourceLocation.asLiteral().asLabel());
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
                            final IA32EirRegister.General sourceRegister = (IA32EirRegister.General) sourceLocation;
                            emitter.assembler().mov(destination.offset8(), destination.base(), sourceRegister.as32());
                            break;
                        }
                        case IMMEDIATE_32: {
                            final Value value = sourceLocation.asImmediate().value();
                            final IA32EirRegister.General scratchRegister = (IA32EirRegister.General) emitter.abi().getScratchRegister(kind);
                            emitter.assembler().movl(scratchRegister.as32(), value.toInt());
                            emitter.assembler().mov(destination.offset8(), destination.base(), scratchRegister.as32());
                            break;
                        }
                        case IMMEDIATE_64:
                        case LITERAL:
                        case STACK_SLOT: {
                            final IA32EirRegister.General scratchRegister = (IA32EirRegister.General) emitter.loadIntoScratchRegister(kind, sourceLocation);
                            emitter.assembler().mov(destination.offset8(), destination.base(), scratchRegister.as32());
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
                            final IA32EirRegister.General sourceRegister = (IA32EirRegister.General) sourceLocation;
                            emitter.assembler().mov(destination.offset32(), destination.base(), sourceRegister.as32());
                            break;
                        }
                        case IMMEDIATE_32: {
                            final Value value = sourceLocation.asImmediate().value();
                            final IA32EirRegister.General scratchRegister = (IA32EirRegister.General) emitter.abi().getScratchRegister(kind);
                            emitter.assembler().movl(scratchRegister.as32(), value.toInt());
                            emitter.assembler().mov(destination.offset32(), destination.base(), scratchRegister.as32());
                            break;
                        }
                        case IMMEDIATE_64:
                        case LITERAL:
                        case STACK_SLOT: {
                            final IA32EirRegister.General scratchRegister = (IA32EirRegister.General) emitter.loadIntoScratchRegister(kind, sourceLocation);
                            emitter.assembler().mov(destination.offset32(), destination.base(), scratchRegister.as32());
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

    private static void emit_F32(IA32EirTargetEmitter emitter, EirLocation destinationLocation, EirLocation sourceLocation) {
        switch (destinationLocation.category()) {
            case FLOATING_POINT_REGISTER: {
                final IA32EirRegister.XMM destinationRegister = (IA32EirRegister.XMM) destinationLocation;
                final IA32XMMRegister destinationRegisterXMM = destinationRegister.as();
                switch (sourceLocation.category()) {
                    case FLOATING_POINT_REGISTER: {
                        final IA32EirRegister.XMM sourceRegister = (IA32EirRegister.XMM) sourceLocation;
                        emitter.assembler().movss(destinationRegisterXMM, sourceRegister.as());
                        break;
                    }
                    case LITERAL: {
                        emitter.assembler().m_movss(destinationRegisterXMM, sourceLocation.asLiteral().asLabel());
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
                            final IA32EirRegister.XMM sourceRegister = (IA32EirRegister.XMM) sourceLocation;
                            emitter.assembler().movss(destination.offset8(), destination.base(), sourceRegister.as());
                            break;
                        }
                        case LITERAL:
                        case STACK_SLOT: {
                            final IA32EirRegister.XMM scratchRegister = (IA32EirRegister.XMM) emitter.loadIntoScratchRegister(Kind.FLOAT, sourceLocation);
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
                            final IA32EirRegister.XMM sourceRegister = (IA32EirRegister.XMM) sourceLocation;
                            emitter.assembler().movss(destination.offset32(), destination.base(), sourceRegister.as());
                            break;
                        }
                        case LITERAL:
                        case STACK_SLOT: {
                            final IA32EirRegister.XMM scratchRegister = (IA32EirRegister.XMM) emitter.loadIntoScratchRegister(Kind.FLOAT, sourceLocation);
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

    private static void emit_F64(IA32EirTargetEmitter emitter, EirLocation destinationLocation, EirLocation sourceLocation) {
        switch (destinationLocation.category()) {
            case FLOATING_POINT_REGISTER: {
                final IA32EirRegister.XMM destinationRegister = (IA32EirRegister.XMM) destinationLocation;
                final IA32XMMRegister destinationRegisterXMM = destinationRegister.as();
                switch (sourceLocation.category()) {
                    case FLOATING_POINT_REGISTER: {
                        final IA32EirRegister.XMM sourceRegister = (IA32EirRegister.XMM) sourceLocation;
                        emitter.assembler().movsd(destinationRegisterXMM, sourceRegister.as());
                        break;
                    }
                    case LITERAL: {
                        emitter.assembler().m_movsd(destinationRegisterXMM, sourceLocation.asLiteral().asLabel());
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
                            final IA32EirRegister.XMM sourceRegister = (IA32EirRegister.XMM) sourceLocation;
                            emitter.assembler().movsd(destination.offset8(), destination.base(), sourceRegister.as());
                            break;
                        }
                        case LITERAL:
                        case STACK_SLOT: {
                            final IA32EirRegister.XMM scratchRegister = (IA32EirRegister.XMM) emitter.loadIntoScratchRegister(Kind.DOUBLE, sourceLocation);
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
                            final IA32EirRegister.XMM sourceRegister = (IA32EirRegister.XMM) sourceLocation;
                            emitter.assembler().movsd(destination.offset32(), destination.base(), sourceRegister.as());
                            break;
                        }
                        case LITERAL:
                        case STACK_SLOT: {
                            final IA32EirRegister.XMM scratchRegister = (IA32EirRegister.XMM) emitter.loadIntoScratchRegister(Kind.DOUBLE, sourceLocation);
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

    public static void emit(IA32EirTargetEmitter emitter, Kind kind, EirLocation destination, EirLocation source) {
        switch (kind.asEnum()) {
            case BYTE:
            case BOOLEAN:
            case SHORT:
            case CHAR:
            case INT:
            case WORD:
            case REFERENCE:
                emit_I32(emitter, destination, source);
                break;
            case LONG:
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
    public void emit(IA32EirTargetEmitter emitter) {
        emit(emitter, kind(), destinationOperand().location(), sourceOperand().location());
    }

    @Override
    public void acceptVisitor(IA32EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        visitor.run(destinationOperand());
        visitor.run(sourceOperand());
    }

    @Override
    public String toString() {
        return "assign-" + _kind + " " + destinationOperand() + " := " + sourceOperand();
    }

}
