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
package com.sun.max.vm.compiler.cps.eir.sparc;

import static com.sun.max.vm.compiler.cps.eir.EirLocationCategory.*;

import java.lang.reflect.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.cps.eir.*;
import com.sun.max.vm.type.*;

public final class SPARCEirStore extends SPARCEirPointerOperation {

    private static PoolSet<EirLocationCategory> valueLocationCategories(Kind kind) {
        switch (kind.asEnum) {
            case FLOAT:
            case DOUBLE:
                return F;
            default:
                return G;
        }
    }

    public SPARCEirStore(EirBlock block, Kind kind, EirValue value, EirValue pointer) {
        super(block, kind, value, EirOperand.Effect.USE, valueLocationCategories(kind), pointer);
    }

    public SPARCEirStore(EirBlock block, Kind kind, EirValue destination, EirValue pointer, Kind offsetKind, EirValue offset) {
        super(block, kind, destination, EirOperand.Effect.USE, valueLocationCategories(kind), pointer, offsetKind, offset);
    }

    public SPARCEirStore(EirBlock block, Kind kind, EirValue destination, EirValue pointer, EirValue index) {
        super(block, kind, destination, EirOperand.Effect.USE, valueLocationCategories(kind), pointer, index);
    }

    public SPARCEirStore(EirBlock block, Kind kind, EirValue destination, EirValue pointer, Kind offsetKind, EirValue offset, EirValue index) {
        super(block, kind, destination, EirOperand.Effect.USE, valueLocationCategories(kind), pointer, offsetKind, offset, index);
    }

    @Override
    public void acceptVisitor(SPARCEirInstructionVisitor visitor) throws InvocationTargetException {
        visitor.visit(this);
    }

    public EirOperand valueOperand() {
        return destinationOperand();
    }

    public SPARCEirRegister.GeneralPurpose valueGeneralRegister() {
        return destinationGeneralRegister();
    }

    public SPARCEirRegister.FloatingPoint valueFloatingPointRegister() {
        return destinationFloatingPointRegister();
    }

    static void emit(SPARCEirTargetEmitter emitter, Kind kind, EirRegister value,  SPARCEirRegister.GeneralPurpose pointerRegister,  SPARCEirRegister.GeneralPurpose offsetRegister) {
        switch (kind.asEnum) {
            case BYTE:
            case BOOLEAN:
                emitter.assembler().stb(toGeneralRegister(value).as(), pointerRegister.as(), offsetRegister.as());
                break;
            case SHORT:
            case CHAR:
                emitter.assembler().sth(toGeneralRegister(value).as(), pointerRegister.as(), offsetRegister.as());
                break;
            case INT:
                emitter.assembler().stw(toGeneralRegister(value).as(), pointerRegister.as(), offsetRegister.as());
                break;
            case LONG:
            case WORD:
            case REFERENCE:
                emitter.assembler().stx(toGeneralRegister(value).as(), pointerRegister.as(), offsetRegister.as());
                break;
            case FLOAT:
                emitter.assembler().st(toFloatingPointRegister(value).asSinglePrecision(), pointerRegister.as(), offsetRegister.as());
                break;
            case DOUBLE:
                emitter.assembler().std(toFloatingPointRegister(value).asDoublePrecision(), pointerRegister.as(), offsetRegister.as());
                break;
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    static void emit(SPARCEirTargetEmitter emitter, Kind kind, EirRegister value,  SPARCEirRegister.GeneralPurpose pointerRegister, int simm13) {
        assert isSimm13(simm13);
        switch (kind.asEnum) {
            case BYTE:
            case BOOLEAN:
                emitter.assembler().stb(toGeneralRegister(value).as(), pointerRegister.as(), simm13);
                break;
            case SHORT:
            case CHAR:
                emitter.assembler().sth(toGeneralRegister(value).as(), pointerRegister.as(), simm13);
                break;
            case INT:
                emitter.assembler().stw(toGeneralRegister(value).as(), pointerRegister.as(), simm13);
                break;
            case LONG:
            case WORD:
            case REFERENCE:
                emitter.assembler().stx(toGeneralRegister(value).as(), pointerRegister.as(), simm13);
                break;
            case FLOAT:
                emitter.assembler().st(toFloatingPointRegister(value).asSinglePrecision(), pointerRegister.as(), simm13);
                break;
            case DOUBLE:
                emitter.assembler().std(toFloatingPointRegister(value).asDoublePrecision(), pointerRegister.as(), simm13);
                break;
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    protected void emit(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose pointerRegister,  SPARCEirRegister.GeneralPurpose offsetRegister) {
        emit(emitter, kind, (EirRegister) destinationLocation(), pointerRegister, offsetRegister);
    }

    @Override
    protected void emit(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose pointerRegister, int simm13) {
        emit(emitter, kind, (EirRegister) destinationLocation(), pointerRegister, simm13);
    }

    @Override
    public String toString() {
        return "store-" + kind.character + " " + addressString() + " := " + valueOperand();
    }

}
