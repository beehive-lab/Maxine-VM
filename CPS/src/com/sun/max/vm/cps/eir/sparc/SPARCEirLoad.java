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
package com.sun.max.vm.cps.eir.sparc;

import java.lang.reflect.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.type.*;

/**
 * Load from memory location.
 * The memory location is specified with a single pointer, a pointer and an offset, a pointer and an index, or a pointer, an offset, and an index.
 * The only forms supported directly by SPARC are the first two.
 * If supply with either of the last two, the scratch register is used to build an offset. If an index is specified, a scaling instruction is performed
 * beforehand (typically a right shift).
 *
 * @author Laurent Daynes
 */
public final class SPARCEirLoad extends SPARCEirPointerOperation {

    private static PoolSet<EirLocationCategory> destinationLocationCategories(Kind kind) {
        switch (kind.asEnum) {
            case FLOAT:
            case DOUBLE:
                return EirLocationCategory.F;
            default:
                return EirLocationCategory.G;
        }
    }

    public SPARCEirLoad(EirBlock block, Kind kind, EirValue destination, EirValue pointer) {
        super(block, kind, destination, EirOperand.Effect.DEFINITION, destinationLocationCategories(kind), pointer);
    }

    public SPARCEirLoad(EirBlock block, Kind kind, EirValue destination, EirValue pointer, Kind offsetKind, EirValue offset) {
        super(block, kind, destination, EirOperand.Effect.DEFINITION, destinationLocationCategories(kind), pointer, offsetKind, offset);
    }

    public SPARCEirLoad(EirBlock block, Kind kind, EirValue destination, EirValue pointer, EirValue index) {
        super(block, kind, destination, EirOperand.Effect.DEFINITION, destinationLocationCategories(kind), pointer, index);
    }

    public SPARCEirLoad(EirBlock block, Kind kind, EirValue destination, EirValue pointer, Kind offsetKind, EirValue offset, EirValue index) {
        super(block, kind, destination, EirOperand.Effect.DEFINITION, destinationLocationCategories(kind), pointer, offsetKind, offset, index);
    }

    @Override
    public void acceptVisitor(SPARCEirInstructionVisitor visitor) throws InvocationTargetException {
        visitor.visit(this);
    }

    static void emit(SPARCEirTargetEmitter emitter, Kind kind, SPARCEirRegisters.GeneralPurpose pointerRegister, SPARCEirRegisters.GeneralPurpose offsetRegister, EirLocation destinationLocation) {
        switch (kind.asEnum) {
            case BYTE:
                emitter.assembler().ldsb(pointerRegister.as(), offsetRegister.as(), toGeneralRegister(destinationLocation).as());
                break;
            case BOOLEAN:
                emitter.assembler().ldub(pointerRegister.as(), offsetRegister.as(), toGeneralRegister(destinationLocation).as());
                break;
            case SHORT:
                emitter.assembler().ldsh(pointerRegister.as(), offsetRegister.as(), toGeneralRegister(destinationLocation).as());
                break;
            case CHAR:
                emitter.assembler().lduh(pointerRegister.as(), offsetRegister.as(), toGeneralRegister(destinationLocation).as());
                break;
            case INT:
                emitter.assembler().ldsw(pointerRegister.as(), offsetRegister.as(), toGeneralRegister(destinationLocation).as());
                break;
            case LONG:
            case WORD:
            case REFERENCE:
                emitter.assembler().ldx(pointerRegister.as(), offsetRegister.as(), toGeneralRegister(destinationLocation).as());
                break;
            case FLOAT:
                emitter.assembler().ld(pointerRegister.as(), offsetRegister.as(), toFloatingPointRegister(destinationLocation).asSinglePrecision());
                break;
            case DOUBLE:
                emitter.assembler().ldd(pointerRegister.as(), offsetRegister.as(), toFloatingPointRegister(destinationLocation).asDoublePrecision());
                break;
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    static void emit(SPARCEirTargetEmitter emitter, Kind kind, SPARCEirRegisters.GeneralPurpose pointerRegister, int simm13, EirLocation destinationLocation) {
        assert isSimm13(simm13);
        switch (kind.asEnum) {
            case BYTE:
                emitter.assembler().ldsb(pointerRegister.as(), simm13, toGeneralRegister(destinationLocation).as());
                break;
            case BOOLEAN:
                emitter.assembler().ldub(pointerRegister.as(), simm13, toGeneralRegister(destinationLocation).as());
                break;
            case SHORT:
                emitter.assembler().ldsh(pointerRegister.as(), simm13, toGeneralRegister(destinationLocation).as());
                break;
            case CHAR:
                emitter.assembler().lduh(pointerRegister.as(), simm13, toGeneralRegister(destinationLocation).as());
                break;
            case INT:
                emitter.assembler().ldsw(pointerRegister.as(), simm13, toGeneralRegister(destinationLocation).as());
                break;
            case LONG:
            case WORD:
            case REFERENCE:
                emitter.assembler().ldx(pointerRegister.as(), simm13, toGeneralRegister(destinationLocation).as());
                break;
            case FLOAT:
                emitter.assembler().ld(pointerRegister.as(), simm13, toFloatingPointRegister(destinationLocation).asSinglePrecision());
                break;
            case DOUBLE:
                emitter.assembler().ldd(pointerRegister.as(), simm13, toFloatingPointRegister(destinationLocation).asDoublePrecision());
                break;
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    protected void emit(SPARCEirTargetEmitter emitter,  SPARCEirRegisters.GeneralPurpose pointerRegister,  SPARCEirRegisters.GeneralPurpose offsetRegister) {
        emit(emitter, kind, pointerRegister, offsetRegister, destinationLocation());
    }

    @Override
    protected void emit(SPARCEirTargetEmitter emitter, SPARCEirRegisters.GeneralPurpose pointerRegister, int simm13) {
        emit(emitter, kind, pointerRegister, simm13, destinationLocation());
    }

    @Override
    public String toString() {
        return "load-" + kind.character + " " + destinationOperand() + " := " + addressString();
    }
}
