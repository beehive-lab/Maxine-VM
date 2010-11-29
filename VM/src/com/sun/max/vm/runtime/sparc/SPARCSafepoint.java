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
import static com.sun.max.platform.Platform.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.runtime.*;

/**
 * The safepoint implementation for SPARC defines the safepoint code that is injected at safepoint sites,
 * as well as the {@linkplain #latchRegister() latch register}, and the layout and size of a trap state
 * area. A trap state area contains the {@linkplain Trap.Number trap number} and the values of the
 * processor's registers when a trap occurs -- except for those registers captured in the trapped frame's register window.
 * The trap state is stored just above the register window (spill slots are stored immediately after the frame pointer).
 * A trap state area is embedded in each trap stub's frame as follows:
 * <pre>
 * RW: register window
 *  <-- stack grows downward                                                         higher addresses -->
 *  trap                                                            trapped
 *  stub's                                                          frame's
 * <--RW--> <--  trap state area -->                                 <--RW->
 * | %i %l |%g | %o | %f | trap num |--- normal trap stub frame --- | %i %l |==== stack as it was when trap occurred ===>
 *         |<--  TRAP_STATE_SIZE -->|
 *         ^ trapState
 * </pre>
 *
 * The layout of the trap state area is described by the following C-like struct declaration:
* trap_state {
 *     Word globalRegisters[5];         // %g1 to %g5
 *     Word outputRegisters[8];         // %o0 to %o7
 *     Word floatingPointRegisters[32]; // %f0 to %f31
 *     Word trapNumber;
 *     Word flagsRegister;
 * }
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 * @author Paul Caprioli
 */
public final class SPARCSafepoint extends Safepoint {

    private final boolean is32Bit;

    @HOSTED_ONLY
    public SPARCSafepoint() {
        is32Bit = platform().wordWidth() == WordWidth.BITS_32;
    }

    /**
     * ATTENTION: must be callee-saved by all C ABIs in use.
     */
    public static final GPR LATCH_REGISTER = G2;

    @HOSTED_ONLY
    private SPARCAssembler createAssembler() {
        if (is32Bit) {
            return new SPARC32Assembler(0);
        }
        return new SPARC64Assembler(0);
    }

    @HOSTED_ONLY
    @Override
    protected byte[] createCode() {
        final SPARCAssembler asm = createAssembler();
        try {
            if (is32Bit) {
                asm.lduw(LATCH_REGISTER, G0, LATCH_REGISTER);
            } else {
                asm.ldx(LATCH_REGISTER, G0, LATCH_REGISTER);
            }
            return asm.toByteArray();
        } catch (AssemblyException assemblyException) {
            throw ProgramError.unexpected("could not assemble safepoint code");
        }
    }
}
