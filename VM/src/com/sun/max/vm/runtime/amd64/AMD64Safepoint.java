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
import com.sun.max.vm.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.runtime.*;

/**
 * The safepoint implementation for AMD64 defines the safepoint code that is injected at safepoint sites,
 * as well as the {@linkplain #latchRegister() latch register}, and the layout and size of a trap state
 * area. A trap state area contains the {@linkplain Trap.Number trap number} and the values of the
 * processor's registers when a trap occurs. A trap state area is embedded in each trap stub's frame as follows:
 *
 * <pre>
 *   <-- stack grows downward                       higher addresses -->
 * |---- normal trap stub frame ---- | ---- trap state area --- | RIP |==== stack as it was when trap occurred ===>
 *                                   |<---  TRAP_STATE_SIZE --->|<-8->|
 *
 *                                   ^ trapState
 * </pre>
 * The layout of the trap state area is described by the following C-like struct declaration:
 * <pre>
 * trap_state {
 *     Word generalPurposeRegisters[16];
 *     DoubleWord xmmRegisters[16];
 *     Word trapNumber;
 *     Word flagsRegister;
 * }
 *
 * trap_state_with_rip {
 *     trap_state ts;
 *     Word trapInstructionPointer;
 * }
 * </pre>
 *
 * The fault address is stored in the RIP slot, making this frame appear as if the trap location
 * called the trap stub directly.
 *
 * @author Ben L. Titzer
 * @author Bernd Mathiske
 */
public final class AMD64Safepoint extends Safepoint {

    /**
     * ATTENTION: must be callee-saved by all C ABIs in use.
     */
    public static final AMD64GeneralRegister64 LATCH_REGISTER = R14;

    @PROTOTYPE_ONLY
    public AMD64Safepoint(VMConfiguration vmConfiguration) {
    }

    @Override
    public AMD64GeneralRegister64 latchRegister() {
        return LATCH_REGISTER;
    }

    @PROTOTYPE_ONLY
    @Override
    protected byte[] createCode() {
        final AMD64Assembler asm = new AMD64Assembler(0L);
        try {
            asm.mov(LATCH_REGISTER, LATCH_REGISTER.indirect());
            return asm.toByteArray();
        } catch (AssemblyException assemblyException) {
            throw ProgramError.unexpected("could not assemble safepoint code");
        }
    }
}
