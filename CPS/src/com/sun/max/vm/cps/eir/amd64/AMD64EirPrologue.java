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
package com.sun.max.vm.cps.eir.amd64;

import java.util.*;

import com.sun.max.asm.amd64.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;
import com.sun.max.vm.thread.*;

/**
 * Emit the prologue for a method.
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 */
public final class AMD64EirPrologue extends EirPrologue<AMD64EirInstructionVisitor, AMD64EirTargetEmitter> implements AMD64EirInstruction {

    public AMD64EirPrologue(EirBlock block, EirMethod eirMethod,
                            EirValue[] calleeSavedValues, EirLocation[] calleeSavedRegisters,
                            BitSet isCalleeSavedParameter,
                            EirValue[] parameters, EirLocation[] parameterLocations) {
        super(block, eirMethod, calleeSavedValues, calleeSavedRegisters, isCalleeSavedParameter, parameters, parameterLocations);
    }

    @Override
    public void emit(AMD64EirTargetEmitter emitter) {
        if (!eirMethod().isTemplate()) {
            final AMD64Assembler asm = emitter.assembler();
            final AMD64GeneralRegister64 framePointer = emitter.framePointer();
            if (eirMethod().classMethodActor().isTrapStub()) {
                // emit a special prologue that saves all the registers
                emitTrapStubPrologue(eirMethod(), asm, framePointer, eirMethod().frameSize());
            } else {
                // emit a regular prologue
                final int frameSize = eirMethod().frameSize();
                if (frameSize != 0) {
                    asm.subq(framePointer, frameSize);
                }
                if (Trap.STACK_BANGING && !eirMethod().classMethodActor().isVmEntryPoint()) {
                    // emit a read of the stack stackGuardSize bytes down to trigger a stack overflow earlier than would otherwise occur.
                    asm.mov(emitter.scratchRegister(), -Trap.stackGuardSize, emitter.stackPointer().indirect());
                }
            }
        }
    }

    private static void emitTrapStubPrologue(EirMethod eirMethod, final AMD64Assembler asm, final AMD64GeneralRegister64 framePointer, final int originalFrameSize) {
        final AMD64GeneralRegister64 latchRegister = AMD64Safepoint.LATCH_REGISTER;
        final AMD64GeneralRegister64 scratchRegister = AMD64GeneralRegister64.R11;
        // expand the frame size for this method to allow for the saved register state
        final int frameSize = originalFrameSize + AMD64TrapStateAccess.TRAP_STATE_SIZE_WITHOUT_RIP;
        final int endOfFrame = originalFrameSize + AMD64TrapStateAccess.TRAP_STATE_SIZE_WITH_RIP;
        eirMethod.setFrameSize(frameSize);

        // the very first instruction must save the flags.
        // we save them twice and overwrite one copy with the trap instruction/return address.
        asm.pushfq();

        asm.pushfq();

        // now allocate the frame for this method
        asm.subq(framePointer, endOfFrame - (2 * Word.size()));

        // save all the general purpose registers
        int offset = originalFrameSize;
        int latchOffset = 0;
        for (AMD64GeneralRegister64 register : AMD64GeneralRegister64.ENUMERATOR) {
            // all registers are the same as when the trap occurred (except the frame pointer and the latch register)
            if (register == latchRegister) {
                latchOffset = offset; // remember the latch offset
            }
            asm.mov(offset, framePointer.indirect(), register);
            offset += Word.size();
        }

        // save all the floating point registers
        for (AMD64XMMRegister register : AMD64XMMRegister.ENUMERATOR) {
            asm.movdq(offset, framePointer.indirect(), register);
            offset += 2 * Word.size();
        }

        // Now that we have saved all general purpose registers (including the scratch register),
        // store the value of the latch register from the thread locals into the trap state
        asm.mov(scratchRegister, VmThreadLocal.TRAP_LATCH_REGISTER.offset, latchRegister.indirect());
        asm.mov(latchOffset, framePointer.indirect(), scratchRegister);

        // write the return address pointer to the end of the frame
        asm.mov(scratchRegister, VmThreadLocal.TRAP_INSTRUCTION_POINTER.offset, latchRegister.indirect());
        asm.mov(frameSize, framePointer.indirect(), scratchRegister);

        // save the trap number
        asm.mov(scratchRegister, VmThreadLocal.TRAP_NUMBER.offset, latchRegister.indirect());
        asm.mov(originalFrameSize + AMD64TrapStateAccess.TRAP_NUMBER_OFFSET, framePointer.indirect(), scratchRegister);

        // now load the trap parameter information into registers from the VM thread locals
        final TargetABI targetABI = VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI;
        final List parameterRegisters = targetABI.integerIncomingParameterRegisters;
        // load the trap number into the first parameter register
        asm.mov((AMD64GeneralRegister64) parameterRegisters.get(0), VmThreadLocal.TRAP_NUMBER.offset, latchRegister.indirect());
        // load the trap state pointer into the second parameter register
        asm.lea((AMD64GeneralRegister64) parameterRegisters.get(1), originalFrameSize, framePointer.indirect());
        // load the fault address into the third parameter register
        asm.mov((AMD64GeneralRegister64) parameterRegisters.get(2), VmThreadLocal.TRAP_FAULT_ADDRESS.offset, latchRegister.indirect());
    }

    @Override
    public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

}
