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
/*VCSID=0598dca6-4170-43fe-9210-cb4bb8cace6b*/
package com.sun.max.vm.compiler.eir.ia32;

import static com.sun.max.vm.compiler.eir.EirLocationCategory.*;

import com.sun.max.asm.ia32.*;
import com.sun.max.asm.x86.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.type.*;

public final class IA32EirStore extends IA32EirPointerOperation {

    private static PoolSet<EirLocationCategory> valueLocationCategories(Kind kind) {
        switch (kind.asEnum()) {
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

    public IA32EirStore(EirBlock block, Kind kind, EirValue value, EirValue pointer) {
        super(block, kind, value, EirOperand.Effect.USE, valueLocationCategories(kind), pointer);
    }

    public IA32EirStore(EirBlock block, Kind kind, EirValue value, EirValue pointer, Kind offsetKind, EirValue offset) {
        super(block, kind, value, EirOperand.Effect.USE, valueLocationCategories(kind), pointer, offsetKind, offset);
    }

    public IA32EirStore(EirBlock block, Kind kind, EirValue value, EirValue pointer, EirValue index) {
        super(block, kind, value, EirOperand.Effect.USE, valueLocationCategories(kind), pointer, index);
    }

    public IA32EirStore(EirBlock block, Kind kind, EirValue value, EirValue pointer, EirValue displacement, EirValue index) {
        super(block, kind, value, EirOperand.Effect.USE, valueLocationCategories(kind), pointer, displacement, index);
    }

    public EirOperand valueOperand() {
        return destinationOperand();
    }

    public IA32EirRegister.General valueGeneralRegister() {
        return destinationGeneralRegister();
    }

    public IA32EirRegister.XMM valueXMMRegister() {
        return destinationXMMRegister();
    }

    @Override
    public String toString() {
        return "store-" + kind().character() + " " + addressString() + " := " + valueOperand();
    }

    @Override
    protected void translateWithoutOffsetWithoutIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister) {
        switch (kind().asEnum()) {
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
                emitter.assembler().mov(pointerRegister.indirect(), valueGeneralRegister().as32());
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
    protected void translateWithoutOffsetWithIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, IA32GeneralRegister32 indexRegister) {
        switch (kind().asEnum()) {
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
                emitter.assembler().mov(pointerRegister.base(), indexRegister.index(), Scale.SCALE_8, valueGeneralRegister().as32());
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
    protected void translateWithRegisterOffsetWithoutIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, IA32GeneralRegister32 offsetRegister) {
        switch (kind().asEnum()) {
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
                emitter.assembler().mov(pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1, valueGeneralRegister().as32());
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
    protected void translateWithRegisterOffsetWithIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, IA32GeneralRegister32 offsetRegister, IA32GeneralRegister32 indexRegister) {
        final IA32EirRegister.General eirScratchRegister = (IA32EirRegister.General) emitter.abi().getScratchRegister(Kind.LONG);
        final IA32GeneralRegister32 scratchRegister = eirScratchRegister.as32();
        emitter.assembler().mov(scratchRegister, pointerRegister);
        emitter.assembler().add(scratchRegister, offsetRegister);
        switch (kind().asEnum()) {
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
                emitter.assembler().mov(scratchRegister.base(), indexRegister.index(), Scale.SCALE_8, valueGeneralRegister().as32());
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
    protected void translateWithImmediateOffset8WithoutIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, byte offset8) {
        switch (kind().asEnum()) {
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
                emitter.assembler().mov(offset8, pointerRegister.indirect(), valueGeneralRegister().as32());
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
    protected void translateWithImmediateOffset8WithIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, byte offset8, IA32GeneralRegister32 indexRegister) {
        switch (kind().asEnum()) {
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
                emitter.assembler().mov(offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_8, valueGeneralRegister().as32());
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
    protected void translateWithImmediateOffset32WithoutIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, int offset32) {
        switch (kind().asEnum()) {
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
                emitter.assembler().mov(offset32, pointerRegister.indirect(), valueGeneralRegister().as32());
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
    protected void translateWithImmediateOffset32WithIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, int offset32, IA32GeneralRegister32 indexRegister) {
        switch (kind().asEnum()) {
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
                emitter.assembler().mov(offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_8, valueGeneralRegister().as32());
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
    public void acceptVisitor(IA32EirInstructionVisitor visitor) {
        visitor.visit(this);
    }
}
