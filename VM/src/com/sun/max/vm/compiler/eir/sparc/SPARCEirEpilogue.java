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

import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.runtime.sparc.*;
import com.sun.max.vm.stack.sparc.*;

/**
 * @author Bernd Mathiske
 */
public final class SPARCEirEpilogue extends EirEpilogue<SPARCEirInstructionVisitor, SPARCEirTargetEmitter> implements SPARCEirInstruction {

    public SPARCEirEpilogue(EirBlock block, EirMethod eirMethod,
                            EirValue[] calleeSavedValues, EirLocation[] calleeSavedRegisters,
                            EirLocation resultLocation) {
        super(block, eirMethod, calleeSavedValues, calleeSavedRegisters, resultLocation);
    }

    @Override
    public void emit(SPARCEirTargetEmitter emitter) {
        if (eirMethod().classMethodActor().isTrapStub()) {
            emitTrapStubEpilogue(emitter);
        }
    }

    @Override
    public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    private void emitTrapStubEpilogue(SPARCEirTargetEmitter emitter) {
        final SPARCAssembler asm = emitter.assembler();
        final GPR stackPointer = ((SPARCEirRegister.GeneralPurpose) emitter.abi().stackPointer()).as();
        final int wordSize = Word.size();
        final int trapStateOffset =  SPARCStackFrameLayout.offsetToFirstFreeSlotFromStackPointer();
        int offset = trapStateOffset;
        // restore all the general purpose registers no in the register windows
        // restore all the floating point registers
        for (GPR register : SPARCSafepoint.TRAP_SAVED_GLOBAL_SYMBOLIZER) {
            asm.ldx(stackPointer, offset, register);
            offset += wordSize;
        }
        for (GPR register : GPR.IN_SYMBOLIZER) {
            asm.ldx(stackPointer, offset, register);
            offset += wordSize;
        }
        for (int i = 0; i < 32; i++) {
            final FPR fpr = FPR.fromValue(i);
            if (fpr instanceof DFPR) {
                asm.ldd(stackPointer, offset, (DFPR) fpr);
            } else {
                asm.ld(stackPointer, offset, (SFPR) fpr);
            }
            offset += wordSize;
        }
    }
}
