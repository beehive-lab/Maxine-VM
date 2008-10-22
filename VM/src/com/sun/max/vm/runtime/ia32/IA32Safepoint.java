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
package com.sun.max.vm.runtime.ia32;

import static com.sun.max.asm.ia32.IA32GeneralRegister32.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.ia32.*;
import com.sun.max.asm.ia32.complete.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * @author Bernd Mathiske
 */
public final class IA32Safepoint extends Safepoint {

    public IA32Safepoint(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    public int numberOfIntegerRegisters() {
        return 4;
    }

    @Override
    public int numberOfFloatingPointRegisters() {
        return 16;
    }

    @INLINE(override = true)
    @Override
    public IA32GeneralRegister32 latchRegister() {
        return null;
    }

    @Override
    public int latchRegisterIndex() {
        return latchRegister().value();
    }

    @Override
    protected byte[] createCode() {
        final IA32Assembler asm = new IA32Assembler(0);
        try {
            asm.nop();
            return asm.toByteArray();
        } catch (AssemblyException assemblyException) {
            throw ProgramError.unexpected("could not assemble safepoint code");
        }
    }

    @Override
    public SafepointStub createSafepointStub(CriticalMethod entryPoint, Venue venue) {
        final IA32Assembler asm = new IA32Assembler(0);
        try {
            asm.mov(EAX, entryPoint.address().toInt());
            asm.call(EAX.indirect());
            if (venue == Venue.JAVA) {
                for (IA32GeneralRegister32 register : IA32GeneralRegister32.ENUMERATOR) {
                    asm.mov(register, VmThreadLocal.REGISTERS.index() + register.value(), latchRegister().indirect());
                }
            }
            asm.jmp(VmThreadLocal.LAST_JAVA_CALLER_INSTRUCTION_POINTER.index() * Word.size(), latchRegister().indirect());
            return new SafepointStub(asm.toByteArray(), entryPoint.classMethodActor());
        } catch (AssemblyException assemblyException) {
            throw ProgramError.unexpected("could not assemble safepoint stub");
        }
    }

    @Override
    public Pointer getInstructionPointer(Pointer registerState) {
        throw Problem.unimplemented();
    }
    @Override
    public Pointer getStackPointer(Pointer registerState, TargetMethod targetMethod) {
        throw Problem.unimplemented();
    }
    @Override
    public Pointer getFramePointer(Pointer registerState, TargetMethod targetMethod) {
        throw Problem.unimplemented();
    }
    @Override
    public Pointer getSafepointLatch(Pointer registerState) {
        throw Problem.unimplemented();
    }
    @Override
    public void setSafepointLatch(Pointer registerState, Pointer value) {
        throw Problem.unimplemented();
    }

}
