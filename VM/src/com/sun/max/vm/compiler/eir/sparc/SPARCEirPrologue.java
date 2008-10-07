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
/*VCSID=f5d276a4-a08e-4b48-8896-2dbce413f234*/
package com.sun.max.vm.compiler.eir.sparc;

import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class SPARCEirPrologue extends EirPrologue<SPARCEirInstructionVisitor, SPARCEirTargetEmitter> implements SPARCEirInstruction {

    public SPARCEirPrologue(EirBlock block, EirMethod eirMethod,
                            EirValue[] calleeSavedValues, EirLocation[] calleeSavedRegisters,
                            BitSet isCalleeSavedParameter,
                            EirValue[] parameters, EirLocation[] parameterLocations) {
        super(block, eirMethod, calleeSavedValues, calleeSavedRegisters, isCalleeSavedParameter, parameters, parameterLocations);
    }

    private static final SPARCAssembler _ASM = SPARCAssembler.createAssembler(WordWidth.BITS_64);

    /**
     * Returns the number of instructions of the frame builder for a given frame size.
     * The save instruction in the last instruction of the frame builder.
     * This information is useful for stack walkers to determine whether a method activation is in its caller
     * register window.
     * @param frameSize
     * @return
     */
    public static int numberOfFrameBuilderInstructions(int frameSize) {
        if (SPARCAssembler.isSimm13(frameSize)) {
            return 2;
        }
        final int stackBangOffset = STACK_BIAS.SPARC_V9.stackBias() - frameSize;
        if (SPARCAssembler.isSimm13(stackBangOffset)) {
            return 2 + _ASM.setswNumberOfInstructions(stackBangOffset);
        }
        return 3 + _ASM.setswNumberOfInstructions(stackBangOffset);
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
        // We must make sure we will not be in a situation where we will not be able to flush the register window for the
        // frame we're creating should an stack overflow occur (especially if a save instruction subsequent to the one that
        // create this frame traps). To avoid this, we bang on the top of the frame we're creating. If this one cause a SIGSEGV,
        // we know the current register window can take the trap.
        final int stackBangOffset = STACK_BIAS.SPARC_V9.stackBias() - frameSize;
        if (SPARCAssembler.isSimm13(frameSize)) {
            assert SPARCAssembler.isSimm13(stackBangOffset);
            asm.ldx(stackPointer, stackBangOffset, GPR.G0);
            asm.save(stackPointer, -frameSize, stackPointer);
        } else {
            final GPR frameSizeReg = scratchRegister;
            try {
                if (SPARCAssembler.isSimm13(stackBangOffset)) {
                    asm.ldx(stackPointer, stackBangOffset, GPR.G0);
                    asm.setsw(-frameSize, frameSizeReg);
                } else {
                    asm.setsw(stackBangOffset, frameSizeReg);
                    asm.ldx(stackPointer, frameSizeReg, GPR.G0);
                    asm.sub(frameSizeReg, STACK_BIAS.SPARC_V9.stackBias(), frameSizeReg);
                }
                asm.save(stackPointer, frameSizeReg, stackPointer);
            } catch (AssemblyException e) {
            }
        }
    }

    @Override
    public void emit(SPARCEirTargetEmitter emitter) {
        if (!eirMethod().isTemplate()) {
            final SPARCEirRegister.GeneralPurpose scratchRegister = (SPARCEirRegister.GeneralPurpose) emitter.abi().getScratchRegister(Kind.INT);
            final SPARCEirRegister.GeneralPurpose stackPointer = (SPARCEirRegister.GeneralPurpose) emitter.abi().stackPointer();
            emitFrameBuilder(emitter.assembler(),  eirMethod().frameSize(), stackPointer.as(), scratchRegister.as());
            if (eirMethod().literalPool().hasLiterals()) {
                emitter.assembler().bindLabel(emitter.literalBaseLabel());
                emitter.assembler().rd(StateRegister.PC, ((SPARCEirABI) emitter.abi()).literalBaseRegister().as());
            }
        }
    }

    @Override
    public void acceptVisitor(SPARCEirInstructionVisitor visitor) {
        visitor.visit(this);
    }

}
