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

import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.runtime.sparc.*;
import com.sun.max.vm.stack.sparc.*;
import com.sun.max.vm.thread.*;

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
        final GPR latchRegister = SPARCSafepoint.LATCH_REGISTER;
        final GPR stackPointer = ((SPARCEirRegister.GeneralPurpose) emitter.abi().stackPointer()).as();
        final GPR returnAddressRegister = GPR.L0;
        final GPR scratchRegister1 = GPR.L1;
        final GPR scratchRegister2 = GPR.L2;
        final int wordSize = Word.size();
        final int trapStateOffset =  SPARCStackFrameLayout.OFFSET_FROM_SP_TO_FIRST_SLOT;
        int offset = trapStateOffset;
        // Setup return address -- to enable stack walker
        asm.ldx(latchRegister, VmThreadLocal.TRAP_INSTRUCTION_POINTER.offset, returnAddressRegister);

        // restore all the general purpose registers not in the register windows
        for (SPARCEirRegister.GeneralPurpose eirRegister : SPARCEirABI.integerNonSystemReservedGlobalRegisters) {
            final GPR gpr = eirRegister.as();
            asm.ldx(stackPointer, offset, gpr);
            offset += wordSize;
        }
        for (GPR register : GPR.IN_SYMBOLIZER) {
            asm.ldx(stackPointer, offset, register);
            offset += wordSize;
        }
        // restore all the floating point registers
        for (int i = 0; i < 64; i += 2) {
            final FPR fpr = FPR.fromValue(i);
            asm.ldd(stackPointer, offset, (DFPR) fpr);
            offset += wordSize;
        }
        asm.ldx(stackPointer, offset, scratchRegister1);
        offset += wordSize;
        asm.ldx(stackPointer, offset, scratchRegister2);
        asm.wr(scratchRegister1, GPR.G0, StateRegister.CCR);
        asm.wr(scratchRegister2, GPR.G0, StateRegister.FPRS);
    }
}
