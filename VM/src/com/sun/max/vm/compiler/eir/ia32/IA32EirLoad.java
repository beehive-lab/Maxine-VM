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
/*VCSID=8dbcf318-2046-4a58-a000-7562f4b1c826*/
package com.sun.max.vm.compiler.eir.ia32;

import static com.sun.max.vm.compiler.eir.EirLocationCategory.*;

import com.sun.max.asm.ia32.*;
import com.sun.max.asm.x86.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class IA32EirLoad extends IA32EirPointerOperation {

    private static PoolSet<EirLocationCategory> destinationLocationCategories(Kind kind) {
        switch (kind.asEnum()) {
            case FLOAT:
            case DOUBLE:
                return F;
            default:
                return G;
        }
    }

    public IA32EirLoad(EirBlock block, Kind kind, EirValue destination, EirValue pointer) {
        super(block, kind, destination, EirOperand.Effect.DEFINITION, destinationLocationCategories(kind), pointer);
    }

    public IA32EirLoad(EirBlock block, Kind kind, EirValue destination, EirValue pointer, Kind offsetKind, EirValue offset) {
        super(block, kind, destination, EirOperand.Effect.DEFINITION, destinationLocationCategories(kind), pointer, offsetKind, offset);
    }

    public IA32EirLoad(EirBlock block, Kind kind, EirValue destination, EirValue pointer, EirValue index) {
        super(block, kind, destination, EirOperand.Effect.DEFINITION, destinationLocationCategories(kind), pointer, index);
    }

    public IA32EirLoad(EirBlock block, Kind kind, EirValue destination, EirValue pointer, EirValue displacement, EirValue index) {
        super(block, kind, destination, EirOperand.Effect.DEFINITION, destinationLocationCategories(kind), pointer, displacement, index);
    }

    @Override
    public String toString() {
        return "load-" + kind().character() + " " + destinationOperand() + " := " + addressString();
    }

    @Override
    protected void translateWithoutOffsetWithoutIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister) {
        switch (kind().asEnum()) {
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as32(), pointerRegister.indirect());
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as32(), pointerRegister.indirect());
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as32(), pointerRegister.indirect());
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as32(), pointerRegister.indirect());
                break;
            }
            case INT: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), pointerRegister.indirect());
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), pointerRegister.indirect());
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
    protected void translateWithoutOffsetWithIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, IA32GeneralRegister32 indexRegister) {
        switch (kind().asEnum()) {
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as32(), pointerRegister.base(), indexRegister.index(), Scale.SCALE_1);
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as32(), pointerRegister.base(), indexRegister.index(), Scale.SCALE_1);
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as32(), pointerRegister.base(), indexRegister.index(), Scale.SCALE_2);
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as32(), pointerRegister.base(), indexRegister.index(), Scale.SCALE_2);
                break;
            }
            case INT: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), pointerRegister.base(), indexRegister.index(), Scale.SCALE_4);
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), pointerRegister.base(), indexRegister.index(), Scale.SCALE_8);
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(destinationXMMRegister().as(), pointerRegister.base(), indexRegister.index(), Scale.SCALE_4);
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(destinationXMMRegister().as(), pointerRegister.base(), indexRegister.index(), Scale.SCALE_8);
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
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as32(), pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1);
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as32(), pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1);
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as32(), pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1);
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as32(), pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1);
                break;
            }
            case INT: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1);
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1);
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(destinationXMMRegister().as(), pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1);
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(destinationXMMRegister().as(), pointerRegister.base(), offsetRegister.index(), Scale.SCALE_1);
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
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as32(), scratchRegister.base(), indexRegister.index(), Scale.SCALE_1);
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as32(), scratchRegister.base(), indexRegister.index(), Scale.SCALE_1);
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as32(), scratchRegister.base(), indexRegister.index(), Scale.SCALE_2);
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as32(), scratchRegister.base(), indexRegister.index(), Scale.SCALE_2);
                break;
            }
            case INT: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), scratchRegister.base(), indexRegister.index(), Scale.SCALE_4);
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), scratchRegister.base(), indexRegister.index(), Scale.SCALE_8);
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(destinationXMMRegister().as(), scratchRegister.base(), indexRegister.index(), Scale.SCALE_4);
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(destinationXMMRegister().as(), scratchRegister.base(), indexRegister.index(), Scale.SCALE_8);
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
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as32(), offset8, pointerRegister.indirect());
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as32(), offset8, pointerRegister.indirect());
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as32(), offset8, pointerRegister.indirect());
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as32(), offset8, pointerRegister.indirect());
                break;
            }
            case INT: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), offset8, pointerRegister.indirect());
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), offset8, pointerRegister.indirect());
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
    protected void translateWithImmediateOffset8WithIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, byte offset8, IA32GeneralRegister32 indexRegister) {
        switch (kind().asEnum()) {
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as32(), offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_1);
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as32(), offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_1);
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as32(), offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_2);
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as32(), offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_2);
                break;
            }
            case INT:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_4);
                break;
            }
            case LONG: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_8);
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(destinationXMMRegister().as(), offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_4);
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(destinationXMMRegister().as(), offset8, pointerRegister.base(), indexRegister.index(), Scale.SCALE_8);
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
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as32(), offset32, pointerRegister.indirect());
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as32(), offset32, pointerRegister.indirect());
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as32(), offset32, pointerRegister.indirect());
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as32(), offset32, pointerRegister.indirect());
                break;
            }
            case INT: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), offset32, pointerRegister.indirect());
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), offset32, pointerRegister.indirect());
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
    protected void translateWithImmediateOffset32WithIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, int offset32, IA32GeneralRegister32 indexRegister) {
        switch (kind().asEnum()) {
            case BYTE: {
                emitter.assembler().movsxb(destinationGeneralRegister().as32(), offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_1);
                break;
            }
            case BOOLEAN: {
                emitter.assembler().movzxb(destinationGeneralRegister().as32(), offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_1);
                break;
            }
            case SHORT: {
                emitter.assembler().movsxw(destinationGeneralRegister().as32(), offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_2);
                break;
            }
            case CHAR: {
                emitter.assembler().movzxw(destinationGeneralRegister().as32(), offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_2);
                break;
            }
            case INT: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_4);
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                emitter.assembler().mov(destinationGeneralRegister().as32(), offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_8);
                break;
            }
            case FLOAT: {
                emitter.assembler().movss(destinationXMMRegister().as(), offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_4);
                break;
            }
            case DOUBLE: {
                emitter.assembler().movsd(destinationXMMRegister().as(), offset32, pointerRegister.base(), indexRegister.index(), Scale.SCALE_8);
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
