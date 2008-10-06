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
/*VCSID=4e0187e1-fb3d-4fcb-8611-6d3ec62905ac*/
package com.sun.max.vm.runtime.amd64;

import static com.sun.max.asm.amd64.AMD64GeneralRegister64.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64Safepoint extends Safepoint {

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

    /**
     * ATTENTION: must be callee-saved by all C ABIs in use.
     */
    private static final AMD64GeneralRegister64 _LATCH_REGISTER = R14;

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

}
