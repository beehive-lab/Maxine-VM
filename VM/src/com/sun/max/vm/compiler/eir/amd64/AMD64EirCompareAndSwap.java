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
package com.sun.max.vm.compiler.eir.amd64;

import static com.sun.max.asm.x86.Scale.*;
import static com.sun.max.vm.compiler.eir.EirLocationCategory.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64EirCompareAndSwap extends AMD64EirPointerOperation {

    @CONSTANT
    private EirOperand _comparableAndResultOperand;

    private void initialize(EirValue comparableAndResult) {
        _comparableAndResultOperand = new EirOperand(this, EirOperand.Effect.UPDATE, G);
        _comparableAndResultOperand.setRequiredRegister(AMD64EirRegister.General.RAX);
        _comparableAndResultOperand.setEirValue(comparableAndResult);
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
        visitor.run(_comparableAndResultOperand);
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
        switch (kind().asEnum()) {
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
                Problem.unimplemented();
            }
        }
    }

    @Override
    protected void translateWithoutOffsetWithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, AMD64GeneralRegister64 indexRegister) {
        Problem.unimplemented();
    }

    @Override
    protected void translateWithRegisterOffsetWithoutIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, AMD64GeneralRegister64 offsetRegister) {
        emitter.assembler().lock();
        switch (kind().asEnum()) {
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
                Problem.unimplemented();
            }
        }
    }

    @Override
    protected void translateWithRegisterOffsetWithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, AMD64GeneralRegister64 offsetRegister, AMD64GeneralRegister64 indexRegister) {
        Problem.unimplemented();
    }

    @Override
    protected void translateWithImmediateOffset8WithoutIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, byte offset8) {
        emitter.assembler().lock();
        switch (kind().asEnum()) {
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
                Problem.unimplemented();
            }
        }
    }

    @Override
    protected void translateWithImmediateOffset8WithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, byte offset8, AMD64GeneralRegister64 indexRegister) {
        Problem.unimplemented();
    }

    @Override
    protected void translateWithImmediateOffset32WithoutIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, int offset32) {
        emitter.assembler().lock();
        switch (kind().asEnum()) {
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
                Problem.unimplemented();
            }
        }
    }

    @Override
    protected void translateWithImmediateOffset32WithIndex(AMD64EirTargetEmitter emitter, AMD64GeneralRegister64 pointerRegister, int offset32, AMD64GeneralRegister64 indexRegister) {
        Problem.unimplemented();
    }


    @Override
    public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

}
