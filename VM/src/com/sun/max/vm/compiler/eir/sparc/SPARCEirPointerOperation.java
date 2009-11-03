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
package com.sun.max.vm.compiler.eir.sparc;

import static com.sun.max.vm.compiler.eir.EirLocationCategory.*;

import com.sun.max.asm.sparc.complete.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.EirOperand.*;
import com.sun.max.vm.type.*;


public abstract class SPARCEirPointerOperation extends SPARCEirBinaryOperation {
    public final Kind kind;

    private final Kind offsetKind;

    public EirOperand offsetOperand() {
        return rightOperand();
    }

    private final EirOperand indexOperand;

    public EirOperand indexOperand() {
        return indexOperand;
    }

    public EirOperand pointerOperand() {
        return leftOperand();
    }

    private static PoolSet<EirLocationCategory> offsetLocationCategories(Kind kind) {
        switch (kind.asEnum) {
            case INT:
                return G_I8_I32;
            case LONG:
            case WORD:
                return G;
            default:
                throw ProgramError.unexpected("pointer offset not integer");
        }
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        super.visitOperands(visitor);
        if (indexOperand != null) {
            visitor.run(indexOperand);
        }
    }

    public SPARCEirRegister.GeneralPurpose pointerGeneralRegister() {
        return (SPARCEirRegister.GeneralPurpose) pointerOperand().location();
    }

    public SPARCEirRegister.GeneralPurpose indexGeneralRegister() {
        return (SPARCEirRegister.GeneralPurpose) indexOperand().location();
    }

    public SPARCEirRegister.GeneralPurpose offsetGeneralRegister() {
        return (SPARCEirRegister.GeneralPurpose) offsetOperand().location();
    }

    protected SPARCEirPointerOperation(EirBlock block, Kind kind, EirValue operand, Effect event,
                    PoolSet<EirLocationCategory> locationCategories, EirValue pointer) {
        super(block, operand, event, locationCategories, pointer,  EirOperand.Effect.USE, G, nullOperand);
        this.kind = kind;
        this.offsetKind = null;
        this.indexOperand = nullOperand;
    }

    private static final EirOperand nullOperand = null;

    protected SPARCEirPointerOperation(EirBlock block, Kind kind, EirValue operand, Effect event,
                    PoolSet<EirLocationCategory> locationCategories, EirValue pointer, Kind offsetKind, EirValue offset) {
        super(block, operand, event, locationCategories, pointer,  EirOperand.Effect.USE, G, offset, EirOperand.Effect.USE, G_I8_I32);
        this.kind = kind;
        this.offsetKind = offsetKind;
        this.indexOperand = nullOperand;
    }

    protected SPARCEirPointerOperation(EirBlock block, Kind kind, EirValue operand, Effect event,
                    PoolSet<EirLocationCategory> locationCategories, EirValue pointer,  EirValue index) {
        super(block, operand, event, locationCategories, pointer,  EirOperand.Effect.USE, G, nullOperand);
        this.kind = kind;
        this.offsetKind = null;
        this.indexOperand = new EirOperand(this, EirOperand.Effect.USE, index.locationCategories());
        this.indexOperand.setEirValue(index);
    }

    protected SPARCEirPointerOperation(EirBlock block, Kind kind, EirValue operand, Effect event,
                    PoolSet<EirLocationCategory> locationCategories, EirValue pointer, Kind offsetKind, EirValue offset,  EirValue index) {
        super(block, operand, event, locationCategories, pointer,  EirOperand.Effect.USE, G, offset, EirOperand.Effect.USE, G_I8_I32);
        this.kind = kind;
        this.offsetKind = offsetKind;
        this.indexOperand = new EirOperand(this, EirOperand.Effect.USE, index.locationCategories());
        this.indexOperand.setEirValue(index);
    }


    /**
     * Emit memory operation of the form {@code   ld [Rp + simm13], R}, {@code  st R, [Rp + + simm13]} .
     */
    protected abstract void emit(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose pointerRegister, int simm13);


    /**
     * Emit memory operation of the form {@code  ld [Rp + Ro], R}, {@code  st R, [Rp + Ro]}.
     */
    protected abstract void emit(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose pointerRegister, SPARCEirRegister.GeneralPurpose offsetRegister);

    /**
     * Emit memory operation of the form  {@code ld [Rp], R}, {@code  st R, [Rp]}.
     *
     * @param emitter
     * @param pointerRegister
     */
    private void emit(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose pointerRegister) {
        emit(emitter, pointerRegister, SPARCEirRegister.GeneralPurpose.G0);
    }

    /**
     * Emit synthetic memory operation of the form  {@code  ld [ Rp + Ri * scale], R}.
     * This translates into a pair of real instructions  {@code srl Ri, scale, Ro; ld [Rp + Ro], R}..
     */
    private void emitWithIndex(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose pointerRegister, SPARCEirRegister.GeneralPurpose indexRegister) {
        final int scale = indexShiftScale(kind);
        SPARCEirRegister.GeneralPurpose offsetRegister = indexRegister;
        if (scale > 0) {
            offsetRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT);
            emitter.assembler().srl(indexRegister.as(), scale, offsetRegister.as());
        }
        emit(emitter, pointerRegister, offsetRegister);
    }

    /**
     * Emit synthetic memory operation of the form  {@code  ld [ Rp + offset32], R}.
     * This translates into a pair of real instructions  {@code sethi, Ro; ld [Rp + Ro], R}..
     */
    private void emitWithOffset(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose pointerRegister, int offset32) {
        final SPARCEirRegister.GeneralPurpose offsetRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT);
        emitter.assembler().setsw(offset32, offsetRegister.as());
        emit(emitter, pointerRegister, offsetRegister);
    }

    private void emitWithIndex(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose pointerRegister, int offset13, SPARCEirRegister.GeneralPurpose indexRegister) {
        final int scale = indexShiftScale(kind);
        final SPARCEirRegister.GeneralPurpose scratchRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT);
        SPARCEirRegister.GeneralPurpose scaledIndexRegister =  indexRegister;
        assert !scratchRegister.equals(indexRegister);
        if (scale > 0) {
            scaledIndexRegister = scratchRegister;
            emitter.assembler().srl(indexRegister.as(), scale, scaledIndexRegister.as());
        }
        emitter.assembler().add(scaledIndexRegister.as(), offset13, scratchRegister.as());
        emit(emitter, pointerRegister, scratchRegister);
    }

    private void emitWithOffsetWithIndex(SPARCEirTargetEmitter emitter, SPARCEirRegister.GeneralPurpose pointerRegister, SPARCEirRegister.GeneralPurpose offsetRegister, SPARCEirRegister.GeneralPurpose indexRegister) {
        final int scale = indexShiftScale(kind);
        final SPARCEirRegister.GeneralPurpose scratchRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT);
        SPARCEirRegister.GeneralPurpose scaledIndexRegister =  indexRegister;
        assert !(scratchRegister.equals(indexRegister) || scratchRegister.equals(offsetRegister));
        if (scale > 0) {
            scaledIndexRegister = scratchRegister;
            emitter.assembler().srl(indexRegister.as(), scale, scaledIndexRegister.as());
        }
        emitter.assembler().add(scaledIndexRegister.as(), offsetRegister.as(), scratchRegister.as());
        emit(emitter, pointerRegister, scratchRegister);
    }

    @Override
    public void emit(SPARCEirTargetEmitter emitter) {
        final SPARCEirRegister.GeneralPurpose pointerRegister = pointerGeneralRegister();
        if (offsetOperand() == null) {
            if (indexOperand() == null) {
                emit(emitter, pointerRegister);
            } else if (indexOperand().location().category().equals(INTEGER_REGISTER)) {
                emitWithIndex(emitter, pointerRegister, indexGeneralRegister());
            } else {
                impossibleLocationCategory();
            }
            return;
        }
        switch (offsetOperand().location().category()) {
            case INTEGER_REGISTER:
                if (indexOperand() == null) {
                    emit(emitter, pointerRegister, offsetGeneralRegister());
                } else {
                    emitWithOffsetWithIndex(emitter, pointerRegister, offsetGeneralRegister(), indexGeneralRegister());
                }
                break;
            case IMMEDIATE_32:
                final int offset32 = offsetOperand().location().asImmediate().value().toInt();
                if (!SPARCAssembler.isSimm13(offset32)) {
                    if (indexOperand() == null) {
                        emitWithOffset(emitter, pointerRegister, offset32);
                    } else {
                        impossibleLocationCategory();
                    }
                    break;
                }
            case IMMEDIATE_8:
                final int simm13 = offsetOperand().location().asImmediate().value().toInt();
                if (indexOperand() == null) {
                    emit(emitter, pointerRegister, simm13);
                } else {
                    emitWithIndex(emitter, pointerRegister, simm13, indexGeneralRegister());
                }
                break;
            default: {
                impossibleLocationCategory();
                break;
            }
        }
    }

    /**
     * Return shift operand for scaling an index to an element of kind k.
     * @param k
     * @return
     */
    public static int indexShiftScale(Kind k) {
        switch (k.asEnum) {
            case BYTE:
            case BOOLEAN:
                return 0;
            case SHORT:
            case CHAR:
                return 1;
            case INT:
            case FLOAT:
                return 2;
            case LONG:
            case WORD:
            case REFERENCE:
            case DOUBLE:
                return 3;
            default: {
                throw ProgramError.unexpected("kind has no scale " + k);
            }
        }
    }

    public String addressString() {
        if (indexOperand() == null) {
            if (offsetOperand() == null) {
                return "[" + pointerOperand() + "]";
            }
            return "[" + pointerOperand() + " + " + offsetOperand() + "]";
        }
        if (offsetOperand() == null) {
            return pointerOperand() + "[" + indexOperand() + " * " + kind.width.numberOfBytes + "]";
        }
        return pointerOperand() + "[" + indexOperand() + " * " + kind.width.numberOfBytes + " + " + offsetOperand() + "]";
    }
}
