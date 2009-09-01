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

import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.sparc.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.sparc.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 * @author Paul Caprioli
 */
public final class SPARCEirPrologue extends EirPrologue<SPARCEirInstructionVisitor, SPARCEirTargetEmitter> implements SPARCEirInstruction {

    public SPARCEirPrologue(EirBlock block, EirMethod eirMethod,
                            EirValue[] calleeSavedValues, EirLocation[] calleeSavedRegisters,
                            BitSet isCalleeSavedParameter,
                            EirValue[] parameters, EirLocation[] parameterLocations) {
        super(block, eirMethod, calleeSavedValues, calleeSavedRegisters, isCalleeSavedParameter, parameters, parameterLocations);
    }

    private static final SPARCAssembler ASM = SPARCAssembler.createAssembler(WordWidth.BITS_64);

    private static int trapStateOffsetFromFramePointer;

    /** Calculates the difference between the (biased) stack pointer in the trapped frame and the trapState.
     *  Equivalently, this is the difference between the (biased) frame pointer in the trap stub and the trapState.
     * @see SPARCTrapStateAccess
     * @return The offset of the trapState from the stack pointer register of the trapped frame.
     */
    public static int trapStateOffsetFromTrappedSP() {
        return trapStateOffsetFromFramePointer;
    }

    @Override
    public void emit(SPARCEirTargetEmitter emitter) {
        if (!eirMethod().isTemplate()) {
            final SPARCAssembler asm = emitter.assembler();
            final SPARCEirRegister.GeneralPurpose stackPointer = (SPARCEirRegister.GeneralPurpose) emitter.abi().stackPointer();
            if (eirMethod().classMethodActor().isTrapStub()) {
               // emit a special prologue that saves all the registers
                trapStateOffsetFromFramePointer = emitTrapStubPrologue(asm, stackPointer.as());
            } else {
                final GPR scratchRegister = ((SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT)).as();
                emitFrameBuilder(asm, eirMethod().frameSize(), stackPointer.as(), scratchRegister);
            }
            if (eirMethod().literalPool().hasLiterals()) {
                asm.bindLabel(emitter.literalBaseLabel());
                asm.rd(StateRegister.PC, ((SPARCEirABI) emitter.abi()).literalBaseRegister().as());
            }
        }
    }

    /**
     * Returns the size (in bytes) of the instructions emitted for the frame builder for a given frame size.
     * The save instruction in the last instruction of the frame builder.
     * This information is useful for stack walkers to determine whether a method activation is in its caller
     * register window.
     * @param frameSize
     * @return the number of bytes of code
     */
    public static int sizeOfFrameBuilderInstructions(int frameSize) {
        final int stackBangOffset = -Trap.stackGuardSize + StackBias.SPARC_V9.stackBias() - frameSize;
        int count;
        if (Trap.STACK_BANGING) {
            count = 2;  // The stack banging load instruction and the save instruction
            if (!SPARCAssembler.isSimm13(stackBangOffset)) {
                count += SPARCAssembler.setswNumberOfInstructions(stackBangOffset & ~0x3FF);
            }
        } else {
            count = 1;  // The save instruction
        }
        if (!SPARCAssembler.isSimm13(-frameSize)) {
            count += SPARCAssembler.setswNumberOfInstructions(-frameSize);
        }
        return count * InstructionSet.SPARC.instructionWidth;
    }

    /**
     * Emit the sequence of instructions that build an optimized code frame.
     * The sequences varies for different frame size due to SPARC limitation on using immediate operand in instructions.
     *
     * @param asm the assembler that'll be used to emit the frame builder
     * @param frameSize size of the frame
     * @param stackPointer stack pointer
     * @param scratchRegister a scratch register (may not necessarily be used)
     */
    public static void emitFrameBuilder(SPARCAssembler asm, int frameSize, GPR stackPointer, GPR scratchRegister) {
        try {
            if (Trap.STACK_BANGING) {
                // We must make sure we will not be in a situation where we will not be able to flush the register window for the
                // frame we're creating should an stack overflow occur (especially if a save instruction subsequent to the one that
                // create this frame traps). To avoid this, we bang on the top of the frame we're creating. If this one cause a SIGSEGV,
                // we know the current register window can take the trap.
                final int stackBangOffset = -Trap.stackGuardSize + StackBias.SPARC_V9.stackBias() - frameSize;
                if (SPARCAssembler.isSimm13(stackBangOffset)) {
                    asm.ldub(stackPointer, stackBangOffset, GPR.G0);
                } else {
                    asm.setsw(stackBangOffset & ~0x3FF, scratchRegister);   // Note: stackBangOffset is rounded off
                    asm.ldub(stackPointer, scratchRegister, GPR.G0);
                }
            }
            if (SPARCAssembler.isSimm13(-frameSize)) {
                asm.save(stackPointer, -frameSize, stackPointer);
            } else {
                asm.setsw(-frameSize, scratchRegister);
                asm.save(stackPointer, scratchRegister, stackPointer);
            }
        } catch (AssemblyException e) {
            FatalError.unexpected(null, e);
        }
    }

    private int emitTrapStubPrologue(SPARCAssembler asm, GPR stackPointer) {
        // Note: the safepoint latch register is already set to the disabled state (the C code in trap.c took care of that).
        // The value of the latch register at the trap instruction is stored in the trap state.
        final GPR latchRegister = SPARCSafepoint.LATCH_REGISTER;
        final int frameSize = eirMethod().frameSize() + SPARCTrapStateAccess.TRAP_STATE_SIZE;
        final GPR scratchRegister0 = GPR.L0;
        final GPR scratchRegister1 = GPR.L1;
        assert SPARCAssembler.isSimm13(frameSize);
        eirMethod().setFrameSize(frameSize);

        emitFrameBuilder(asm, frameSize, stackPointer, scratchRegister0 /* will not be used */);
        // Only need to save the %i and %g of the trap stub frame, plus the %f.
        // Can use all %l and %o of the trap stub frame as temporary registers, since these don't contain any state of the
        // trapped frame.

        // flush register window as the trap stub will access the register window of the trapped frame.
        // this is likely a nop as we're entering here from returning from a signal handler.
        asm.flushw();
        final int wordSize = Word.size();
        final int trapStateOffset =  SPARCStackFrameLayout.OFFSET_FROM_SP_TO_FIRST_SLOT;
        int offset = trapStateOffset;

        // We want to copy into the trap state the value of the latch register at the instruction that causes the trap.
        asm.ldx(latchRegister, VmThreadLocal.TRAP_LATCH_REGISTER.offset, scratchRegister0);

        for (SPARCEirRegister.GeneralPurpose eirRegister : SPARCEirABI.integerNonSystemReservedGlobalRegisters) {
            final GPR gpr = eirRegister.as();
            if (gpr == latchRegister) {
                asm.stx(scratchRegister0, stackPointer, offset);
            } else {
                asm.stx(gpr, stackPointer, offset);
            }
            offset += wordSize;
        }
        for (GPR register : GPR.IN_SYMBOLIZER) {
            asm.stx(register, stackPointer, offset);
            offset += wordSize;
        }
        for (int i = 0; i < 64; i += 2) {
            final FPR fpr = FPR.fromValue(i);
            asm.std((DFPR) fpr, stackPointer, offset);
            offset += wordSize;
        }
        asm.rd(StateRegister.CCR, scratchRegister0);
        asm.rd(StateRegister.FPRS, scratchRegister1);
        asm.stx(scratchRegister0, stackPointer, offset);
        offset += wordSize;
        asm.stx(scratchRegister1, stackPointer, offset);
        offset += wordSize;
        // offset now points to the location where the trap number will be stored in the trap state.

        final TargetABI targetABI = VMConfiguration.hostOrTarget().targetABIsScheme().optimizedJavaABI();

        // Setup return address -- to enable stack walker
        asm.ldx(latchRegister, VmThreadLocal.TRAP_INSTRUCTION_POINTER.offset, GPR.I7);
        // Setup arguments for the trapStub
        final IndexedSequence parameterRegisters = targetABI.integerIncomingParameterRegisters();
        asm.ldx(latchRegister, VmThreadLocal.TRAP_NUMBER.offset, (GPR) parameterRegisters.get(0));
        asm.add(stackPointer, trapStateOffset, (GPR) parameterRegisters.get(1));
        asm.ldx(latchRegister, VmThreadLocal.TRAP_FAULT_ADDRESS.offset, (GPR) parameterRegisters.get(2));

        // Write trap number in corresponding trap state location
        asm.stx((GPR) parameterRegisters.get(0), stackPointer, offset);
        return -frameSize + trapStateOffset;
    }

    @Override
    public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
        visitor.visit(this);
    }

}
