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

/**
 * @author Bernd Mathiske
 */
public final class AMD64EirLoad extends AMD64EirPointerOperation {

    private static PoolSet<EirLocationCategory> destinationLocationCategories(Kind kind) {
        switch (kind.asEnum) {
            case FLOAT:
            case DOUBLE:
                return F;
            default:
                return G;
        }
    }

    public AMD64EirLoad(EirBlock block, Kind kind, EirValue destination, EirValue pointer) {
        super(block, kind, destination, EirOperand.Effect.DEFINITION, destinationLocationCategories(kind), pointer);
    }

    public AMD64EirLoad(EirBlock block, Kind kind, EirValue destination, EirValue pointer, Kind offsetKind, EirValue offset) {
        super(block, kind, destination, EirOperand.Effect.DEFINITION, destinationLocationCategories(kind), pointer, offsetKind, offset);
    }

    public AMD64EirLoad(EirBlock block, Kind kind, EirValue destination, EirValue pointer, EirValue index) {
        super(block, kind, destination, EirOperand.Effect.DEFINITION, destinationLocationCategories(kind), pointer, index);
    }

    public AMD64EirLoad(EirBlock block, Kind kind, EirValue destination, EirValue pointer, EirValue displacement, EirValue index) {
        super(block, kind, destination, EirOperand.Effect.DEFINITION, destinationLocationCategories(kind), pointer, displacement, index);
    }

    @Override
    public String toString() {
        return "load-" + kind().character + " " + destinationOperand() + " := " + addressString();
    }

    @Override
    protected void translateWithoutOffsetWithoutIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister) {
        switch (kind().asEnum) {
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as64(), pointerRegister.indirect());
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as64(), pointerRegister.indirect());
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as64(), pointerRegister.indirect());
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as64(), pointerRegister.indirect());
                break;
            }
            case INT: {
                emitter.assembler().movsxd(destinationGeneralRegister().as64(), pointerRegister.indirect());
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as64(), pointerRegister.indirect());
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(destinationXMMRegister().as(), pointerRegister.indirect());
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(destinationXMMRegister().as(), pointerRegister.indirect());
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    protected void translateWithoutOffsetWithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, AMD64GeneralRegister64 indexRegister) {
        final Scale scale = getScale(kind());
        switch (kind().asEnum) {
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as64(), pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as64(), pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as64(), pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as64(), pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case INT: {
                emitter.assembler().movsxd(destinationGeneralRegister().as64(), pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as64(), pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(destinationXMMRegister().as(), pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(destinationXMMRegister().as(), pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    protected void translateWithRegisterOffsetWithoutIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, AMD64GeneralRegister64 offsetRegister) {
        final Scale scale = Scale.SCALE_1;
        switch (kind().asEnum) {
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as64(), pointerRegister.base(), offsetRegister.index(), scale);
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as64(), pointerRegister.base(), offsetRegister.index(), scale);
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as64(), pointerRegister.base(), offsetRegister.index(), scale);
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as64(), pointerRegister.base(), offsetRegister.index(), scale);
                break;
            }
            case INT: {
                emitter.assembler().movsxd(destinationGeneralRegister().as64(), pointerRegister.base(), offsetRegister.index(), scale);
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as64(), pointerRegister.base(), offsetRegister.index(), scale);
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(destinationXMMRegister().as(), pointerRegister.base(), offsetRegister.index(), scale);
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(destinationXMMRegister().as(), pointerRegister.base(), offsetRegister.index(), scale);
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    protected void translateWithRegisterOffsetWithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, AMD64GeneralRegister64 offsetRegister, AMD64GeneralRegister64 indexRegister) {
        final Scale scale = getScale(kind());
        final AMD64EirRegister.General eirScratchRegister = (AMD64EirRegister.General) emitter.abi().getScratchRegister(Kind.LONG);
        final AMD64GeneralRegister64 scratchRegister = eirScratchRegister.as64();
        emitter.assembler().mov(scratchRegister, pointerRegister);
        emitter.assembler().add(scratchRegister, offsetRegister);
        switch (kind().asEnum) {
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as64(), scratchRegister.base(), indexRegister.index(), scale);
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as64(), scratchRegister.base(), indexRegister.index(), scale);
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as64(), scratchRegister.base(), indexRegister.index(), scale);
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as64(), scratchRegister.base(), indexRegister.index(), scale);
                break;
            }
            case INT: {
                emitter.assembler().movsxd(destinationGeneralRegister().as64(), scratchRegister.base(), indexRegister.index(), scale);
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as64(), scratchRegister.base(), indexRegister.index(), scale);
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(destinationXMMRegister().as(), scratchRegister.base(), indexRegister.index(), scale);
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(destinationXMMRegister().as(), scratchRegister.base(), indexRegister.index(), scale);
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
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as64(), offset8, pointerRegister.indirect());
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as64(), offset8, pointerRegister.indirect());
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as64(), offset8, pointerRegister.indirect());
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as64(), offset8, pointerRegister.indirect());
                break;
            }
            case INT: {
                emitter.assembler().movsxd(destinationGeneralRegister().as64(), offset8, pointerRegister.indirect());
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as64(), offset8, pointerRegister.indirect());
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(destinationXMMRegister().as(), offset8, pointerRegister.indirect());
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(destinationXMMRegister().as(), offset8, pointerRegister.indirect());
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    protected void translateWithImmediateOffset8WithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, byte offset8, AMD64GeneralRegister64 indexRegister) {
        final Scale scale = getScale(kind());
        switch (kind().asEnum) {
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as64(), offset8, pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as64(), offset8, pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as64(), offset8, pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as64(), offset8, pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case INT: {
                emitter.assembler().movsxd(destinationGeneralRegister().as64(), offset8, pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as64(), offset8, pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(destinationXMMRegister().as(), offset8, pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(destinationXMMRegister().as(), offset8, pointerRegister.base(), indexRegister.index(), scale);
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
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as64(), offset32, pointerRegister.indirect());
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as64(), offset32, pointerRegister.indirect());
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as64(), offset32, pointerRegister.indirect());
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as64(), offset32, pointerRegister.indirect());
                break;
            }
            case INT: {
                emitter.assembler().movsxd(destinationGeneralRegister().as64(), offset32, pointerRegister.indirect());
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as64(), offset32, pointerRegister.indirect());
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(destinationXMMRegister().as(), offset32, pointerRegister.indirect());
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(destinationXMMRegister().as(), offset32, pointerRegister.indirect());
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    protected void translateWithImmediateOffset32WithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, int offset32, AMD64GeneralRegister64 indexRegister) {
        final Scale scale = getScale(kind());
        switch (kind().asEnum) {
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as64(), offset32, pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as64(), offset32, pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as64(), offset32, pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as64(), offset32, pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case INT: {
                emitter.assembler().movsxd(destinationGeneralRegister().as64(), offset32, pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as64(), offset32, pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(destinationXMMRegister().as(), offset32, pointerRegister.base(), indexRegister.index(), scale);
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(destinationXMMRegister().as(), offset32, pointerRegister.base(), indexRegister.index(), scale);
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

    public static Scale getScale(Kind k) {
        switch (k.asEnum) {
            case BYTE: {
                return Scale.SCALE_1;
            }
            case BOOLEAN: {
                return Scale.SCALE_1;
            }
            case SHORT: {
                return Scale.SCALE_2;
            }
            case CHAR: {
                return Scale.SCALE_2;
            }
            case INT: {
                return Scale.SCALE_4;
            }
            case LONG: {
                return Scale.SCALE_8;
            }
            case WORD: {
                return Scale.SCALE_8;
            }
            case REFERENCE: {
                return Scale.SCALE_8;
            }
            case FLOAT: {
                return Scale.SCALE_4;
            }
            case DOUBLE: {
                return Scale.SCALE_8;
            }
            default: {
                throw ProgramError.unexpected("kind has no scale " + k);
            }
        }
    }

}
