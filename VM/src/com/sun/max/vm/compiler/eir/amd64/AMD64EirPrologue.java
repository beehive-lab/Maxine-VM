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

import java.util.*;

import com.sun.max.asm.amd64.*;
import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.amd64.*;
import com.sun.max.vm.thread.*;

/**
 * Emit the prologue for a method.
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 */
public final class AMD64EirPrologue extends EirPrologue<AMD64EirInstructionVisitor, AMD64EirTargetEmitter> implements AMD64EirInstruction {

    private static final boolean STACK_BANGING = true;

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
                if (STACK_BANGING) {
                    // emit a read of the stack 2 pages down to trigger a stack overflow earlier
                    // TODO (tw): Check why the LSRA needs the value 3 here. Can probably be removed after implementing better stack slot sharing.
                    asm.mov(emitter.scratchRegister(), -3 * emitter.abi().vmConfiguration().platform().pageSize, emitter.stackPointer().indirect());
                    //asm.mov(emitter.scratchRegister(), -2 * emitter.abi().vmConfiguration().platform().pageSize(), emitter.stackPointer().indirect());

                }
            }
        }
    }

    private static void emitTrapStubPrologue(EirMethod eirMethod, final AMD64Assembler asm, final AMD64GeneralRegister64 framePointer, final int originalFrameSize) {
        final AMD64GeneralRegister64 latchRegister = AMD64Safepoint.LATCH_REGISTER;
        final AMD64GeneralRegister64 scratchRegister = AMD64GeneralRegister64.R11;
        // expand the frame size for this method to allow for the saved register state
        final int frameSize = originalFrameSize + AMD64Safepoint.TRAP_STATE_SIZE_WITHOUT_RIP;
        final int endOfFrame = originalFrameSize + AMD64Safepoint.TRAP_STATE_SIZE_WITH_RIP;
        eirMethod.setFrameSize(frameSize);

        // the very first instruction must save the flags.
        // we save them twice and overwrite one copy with the trap instruction address.
        asm.pushfq();
        asm.pushfq();

        // now allocate the frame for this method
        asm.subq(framePointer, endOfFrame - (2 * Word.size()));

        // We want to copy into the trap state the value of the latch register at the instruction that causes the trap.
        asm.mov(scratchRegister, VmThreadLocal.TRAP_LATCH_REGISTER.offset(), latchRegister.indirect());

        // save all the general purpose registers
        int offset = originalFrameSize;
        for (AMD64GeneralRegister64 register : AMD64GeneralRegister64.ENUMERATOR) {
            // all registers are the same as when the trap occurred (except the frame pointer and the latch register)
            if (register == latchRegister) {
                asm.mov(offset, framePointer.indirect(), scratchRegister);
            } else {
                asm.mov(offset, framePointer.indirect(), register);
            }
            offset += Word.size();
        }
        // save all the floating point registers
        for (AMD64XMMRegister register : AMD64XMMRegister.ENUMERATOR) {
            asm.movdq(offset, framePointer.indirect(), register);
            offset += 2 * Word.size();
        }

        // write the return address pointer at the end of the frame
        asm.mov(scratchRegister, VmThreadLocal.TRAP_INSTRUCTION_POINTER.offset(), latchRegister.indirect());
        asm.mov(frameSize, framePointer.indirect(), scratchRegister);

        // save the trap number
        asm.mov(scratchRegister, VmThreadLocal.TRAP_NUMBER.offset(), latchRegister.indirect());
        asm.mov(originalFrameSize + AMD64Safepoint.TRAP_NUMBER_OFFSET, framePointer.indirect(), scratchRegister);

        // now load the trap parameter information into registers from the vm thread locals
        final TargetABI targetABI = VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI();
        final IndexedSequence parameterRegisters = targetABI.integerIncomingParameterRegisters();
        // load the trap number into the first parameter register
        asm.mov((AMD64GeneralRegister64) parameterRegisters.get(0), VmThreadLocal.TRAP_NUMBER.offset(), latchRegister.indirect());
        // load the register state pointer into the second parameter register
        asm.lea((AMD64GeneralRegister64) parameterRegisters.get(1), originalFrameSize, framePointer.indirect());
        // load the fault address into the third parameter register
        asm.mov((AMD64GeneralRegister64) parameterRegisters.get(2), VmThreadLocal.TRAP_FAULT_ADDRESS.offset(), latchRegister.indirect());
    }

    @Override
    public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

}
