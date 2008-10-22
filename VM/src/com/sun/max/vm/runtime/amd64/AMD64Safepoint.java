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
package com.sun.max.vm.runtime.amd64;

import static com.sun.max.asm.amd64.AMD64GeneralRegister64.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64Safepoint extends Safepoint {

    /**
     * ATTENTION: must be callee-saved by all C ABIs in use.
     */
    public static final AMD64GeneralRegister64 _LATCH_REGISTER = R14;
    public static final int REGISTER_SAVE_SIZE = Word.size() * (AMD64GeneralRegister64.values().length + (2 * AMD64XMMRegister.values().length));

    public AMD64Safepoint(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    public int numberOfIntegerRegisters() {
        return 16;
    }

    @Override
    public int numberOfFloatingPointRegisters() {
        return 16;
    }


    @INLINE(override = true)
    @Override
    public AMD64GeneralRegister64 latchRegister() {
        return _LATCH_REGISTER;
    }

    @Override
    public int latchRegisterIndex() {
        return _LATCH_REGISTER.value();
    }

    @Override
    protected byte[] createCode() {
        final AMD64Assembler asm = new AMD64Assembler(0L);
        try {
            asm.mov(_LATCH_REGISTER, _LATCH_REGISTER.indirect());
            return asm.toByteArray();
        } catch (AssemblyException assemblyException) {
            throw ProgramError.unexpected("could not assemble safepoint code");
        }
    }

    @Override
    public SafepointStub createSafepointStub(CriticalMethod entryPoint, Venue venue) {
        final AMD64Assembler asm = new AMD64Assembler(0L);

        try {
            asm.mov(RAX, entryPoint.address().toLong());
            asm.call(RAX);

            if (venue == Venue.JAVA) {
                for (AMD64GeneralRegister64 register : AMD64GeneralRegister64.ENUMERATOR) {
                    asm.mov(register, (VmThreadLocal.REGISTERS.index() + register.value()) * Word.size(), latchRegister().indirect());
                }
            }
            asm.jmp(VmThreadLocal.TRAP_INSTRUCTION_POINTER.index() * Word.size(), latchRegister().indirect());

            return new SafepointStub(asm.toByteArray(), entryPoint.classMethodActor());
        } catch (AssemblyException assemblyException) {
            throw ProgramError.unexpected("could not assemble safepoint stub");
        }
    }

    @Override
    public Pointer getInstructionPointer(Pointer registerState) {
        return registerState.readWord(AMD64Safepoint.REGISTER_SAVE_SIZE).asPointer();
    }
    @Override
    public Pointer getStackPointer(Pointer registerState, TargetMethod targetMethod) {
        // TODO: get the frame pointer register from the ABI
        return registerState.plus(REGISTER_SAVE_SIZE + Word.size());
    }
    @Override
    public Pointer getFramePointer(Pointer registerState, TargetMethod targetMethod) {
        // TODO: get the frame pointer register from the ABI
        return registerState.readWord(AMD64GeneralRegister64.RBP.value() * Word.size()).asPointer();
    }
    @Override
    public Pointer getSafepointLatch(Pointer registerState) {
        return registerState.readWord(_LATCH_REGISTER.value() * Word.size()).asPointer();
    }
    @Override
    public void setSafepointLatch(Pointer registerState, Pointer value) {
        registerState.writeWord(_LATCH_REGISTER.value() * Word.size(), value);
    }
}
