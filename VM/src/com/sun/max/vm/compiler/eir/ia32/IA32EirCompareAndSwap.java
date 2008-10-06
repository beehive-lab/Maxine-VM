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
/*VCSID=b427cc3d-5e9f-4e07-aeec-7f4b291e4941*/
package com.sun.max.vm.compiler.eir.ia32;

import static com.sun.max.asm.x86.Scale.*;
import static com.sun.max.vm.compiler.eir.EirLocationCategory.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.ia32.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class IA32EirCompareAndSwap extends IA32EirPointerOperation {

    @CONSTANT
    private EirOperand _comparableAndResultOperand;

    private void initialize(EirValue comparableAndResult) {
        _comparableAndResultOperand = new EirOperand(this, EirOperand.Effect.UPDATE, G);
        _comparableAndResultOperand.setRequiredRegister(IA32EirRegister.General.EAX);
        _comparableAndResultOperand.setEirValue(comparableAndResult);
    }

    public IA32EirCompareAndSwap(EirBlock block, Kind kind, EirValue newValue, EirValue pointer, EirValue comparableAndResult) {
        super(block, kind, newValue, EirOperand.Effect.USE, G, pointer);
        initialize(comparableAndResult);
    }

    public IA32EirCompareAndSwap(EirBlock block, Kind kind, EirValue newValue, EirValue pointer, Kind offsetKind, EirValue offset, EirValue comparableAndResult) {
        super(block, kind, newValue, EirOperand.Effect.USE, G, pointer, offsetKind, offset);
        initialize(comparableAndResult);
    }

    public IA32EirCompareAndSwap(EirBlock block, Kind kind, EirValue newValue, EirValue pointer, EirValue index, EirValue comparableAndResult) {
        super(block, kind, newValue, EirOperand.Effect.USE, G, pointer, index);
        initialize(comparableAndResult);
    }

    public IA32EirCompareAndSwap(EirBlock block, Kind kind, EirValue newValue, EirValue pointer, EirValue displacement, EirValue index, EirValue comparableAndResult) {
        super(block, kind, newValue, EirOperand.Effect.USE, G, pointer, displacement, index);
        initialize(comparableAndResult);
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        super.visitOperands(visitor);
        visitor.run(_comparableAndResultOperand);
    }

    public IA32GeneralRegister32 newValueRegister() {
        return destinationGeneralRegister().as32();
    }

    @Override
    protected void translateWithoutOffsetWithoutIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister) {
        emitter.assembler().lock();
        switch (kind().asEnum()) {
            case WORD:
            case REFERENCE: {
                emitter.assembler().cmpxchg(pointerRegister.indirect(), newValueRegister());
                break;
            }
            default: {
                Problem.unimplemented();
            }
        }
    }

    @Override
    protected void translateWithoutOffsetWithIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, IA32GeneralRegister32 indexRegister) {
        Problem.unimplemented();
    }

    @Override
    protected void translateWithRegisterOffsetWithoutIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, IA32GeneralRegister32 offsetRegister) {
        emitter.assembler().lock();
        switch (kind().asEnum()) {
            case WORD:
            case REFERENCE: {
                emitter.assembler().cmpxchg(pointerRegister.base(), offsetRegister.index(), SCALE_1, newValueRegister());
                break;
            }
            default: {
                Problem.unimplemented();
            }
        }
    }

    @Override
    protected void translateWithRegisterOffsetWithIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, IA32GeneralRegister32 offsetRegister, IA32GeneralRegister32 indexRegister) {
        Problem.unimplemented();
    }

    @Override
    protected void translateWithImmediateOffset8WithoutIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, byte offset8) {
        emitter.assembler().lock();
        switch (kind().asEnum()) {
            case WORD:
            case REFERENCE: {
                emitter.assembler().cmpxchg(offset8, pointerRegister.indirect(), newValueRegister());
                break;
            }
            default: {
                Problem.unimplemented();
            }
        }
    }

    @Override
    protected void translateWithImmediateOffset8WithIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, byte offset8, IA32GeneralRegister32 indexRegister) {
        Problem.unimplemented();
    }

    @Override
    protected void translateWithImmediateOffset32WithoutIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, int offset32) {
        emitter.assembler().lock();
        switch (kind().asEnum()) {
            case WORD:
            case REFERENCE: {
                emitter.assembler().cmpxchg(offset32, pointerRegister.indirect(), newValueRegister());
                break;
            }
            default: {
                Problem.unimplemented();
            }
        }
    }

    @Override
    protected void translateWithImmediateOffset32WithIndex(IA32EirTargetEmitter emitter, IA32GeneralRegister32 pointerRegister, int offset32, IA32GeneralRegister32 indexRegister) {
        Problem.unimplemented();
    }


    @Override
    public void acceptVisitor(IA32EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

}
