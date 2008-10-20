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
package com.sun.max.vm.runtime.sparc;

import static com.sun.max.asm.sparc.GPR.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.util.Predicate;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class SPARCSafepoint extends Safepoint {

    private final boolean _is32Bit;

    public SPARCSafepoint(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        _is32Bit = vmConfiguration.platform().processorKind().dataModel().wordWidth() == WordWidth.BITS_32;
    }

    /**
     * Return the number of integer registers that need to be saved by safepoint traps.
     * On Solaris / SPARC, we only need to save the %o and %g registers (excluding %g0), i.e., 15 registers.
     * These are the only registers whose state is being stored in the ucontext data structure on Solaris / SPARC.
     * The other integer registers (%i and %l) are already saved in the register window's saving area on the stack of the trapped context,
     * and their location can be found using the stack pointer (%o6).
     * However, because of the way the trap code is being written, we need to count %g0 so that the values of the GPR instances coincide with indices
     * to the array of registers passed to the trap handlers.
     */
    @Override
    public int numberOfIntegerRegisters() {
        return 16;
    }

    @Override
    public int numberOfFloatingPointRegisters() {
        return 32;
    }

    /**
     * ATTENTION: must be callee-saved by all C ABIs in use.
     */
    private static final GPR _LATCH_REGISTER = G2;

    @INLINE(override = true)
    @Override
    public GPR latchRegister() {
        return _LATCH_REGISTER;
    }

    @Override
    public int latchRegisterIndex() {
        return _LATCH_REGISTER.value();
    }

    private SPARCAssembler createAssembler() {
        if (_is32Bit) {
            return new SPARC32Assembler(0);
        }
        return new SPARC64Assembler(0);
    }

    @Override
    protected byte[] createCode() {
        final SPARCAssembler asm = createAssembler();
        try {
            if (_is32Bit) {
                asm.lduw(_LATCH_REGISTER, G0, _LATCH_REGISTER);
            } else {
                asm.ldx(_LATCH_REGISTER, G0, _LATCH_REGISTER);
            }
            return asm.toByteArray();
        } catch (AssemblyException assemblyException) {
            throw ProgramError.unexpected("could not assemble safepoint code");
        }
    }

    private static final Symbolizer<GPR> SIGNAL_INTEGER_REGISTER = Symbolizer.Static.fromSymbolizer(GPR.SYMBOLIZER, new Predicate<GPR>() {
        public boolean evaluate(GPR register) {
            return register.isOut() || (register.isGlobal() && register != G0);
        }
    });

    private SafepointStub create64BitPostSignalStub(CriticalMethod entryPoint, Venue venue) throws AssemblyException {
        final SPARC64Assembler asm = new SPARC64Assembler(0);
        final long address = entryPoint.address().toLong();
        if (Longs.numberOfEffectiveSignedBits(address) > WordWidth.BITS_32.numberOfBits()) {
            asm.setx(address, L1, L0);
            asm.jmpl(L0, G0, O7);
        } else {
            asm.sethi(asm.hi(address), L0);
            asm.jmpl(L0, asm.lo(address), O7);
        }
        asm.nop();
        if (venue == Venue.JAVA) {
            // Skip G0.
            final int firstIndex = VmThreadLocal.REGISTERS.index() + 1;
            for (GPR register : SIGNAL_INTEGER_REGISTER) {
                final int offset = (firstIndex + register.value()) * Word.size();
                assert Ints.numberOfEffectiveSignedBits(offset) <= 13;
                asm.ldx(latchRegister(), offset, register);
            }
        }
        // return to trapped instruction
        asm.jmp(latchRegister(), VmThreadLocal.TRAP_INSTRUCTION_POINTER.index() * Word.size());
        asm.nop();
        return new SafepointStub(asm.toByteArray(), entryPoint.classMethodActor());
    }

    private SafepointStub create32BitPostSignalStub(CriticalMethod entryPoint, Venue venue) throws AssemblyException {
        final SPARC32Assembler asm = new SPARC32Assembler(0);
        final int address = entryPoint.address().toInt();
        asm.sethi(asm.hi(address), L0);
        asm.jmpl(L0, asm.lo(address), O7);
        asm.nop();
        if (venue == Venue.JAVA) {
            // Skip G0.
            final int firstIndex = VmThreadLocal.REGISTERS.index() + 1;
            for (GPR register : SIGNAL_INTEGER_REGISTER) {
                final int offset = (firstIndex + register.value()) * Word.size();
                assert Ints.numberOfEffectiveSignedBits(offset) <= 13;
                asm.ldsw(latchRegister(), offset, register);
            }
        }
        // return to trapped instruction
        asm.jmp(latchRegister(), VmThreadLocal.TRAP_INSTRUCTION_POINTER.index() * Word.size());
        asm.nop();
        return new SafepointStub(asm.toByteArray(), entryPoint.classMethodActor());
    }

    @Override
    public SafepointStub createSafepointStub(CriticalMethod entryPoint, Venue venue) {
        try {
            if (_is32Bit) {
                return create32BitPostSignalStub(entryPoint, venue);
            }
            return create64BitPostSignalStub(entryPoint, venue);
        } catch (AssemblyException assemblyException) {
            throw ProgramError.unexpected("could not assemble safepoint stub");
        }
    }

}
