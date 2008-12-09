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
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

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
import com.sun.max.vm.compiler.eir.sparc.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.sparc.*;

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
 *     Word globalRegisters[15];  %g1 to %g5
 *     Word outputRegisters[16]   %o0 to %o7
 *     Word floatingPointRegisters[32]; %f0 to %f31
 *     Word trapNumber;
 *     Word flagsRegister;
 * }
 *
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
     * ATTENTION: must be callee-saved by all C ABIs in use.
     */
    public static final GPR LATCH_REGISTER = G2;

    public static final Symbolizer<GPR> TRAP_SAVED_GLOBAL_SYMBOLIZER = Symbolizer.Static.fromSymbolizer(GLOBAL_SYMBOLIZER, new com.sun.max.util.Predicate<GPR>() {
        private final Collection<SPARCEirRegister> _reservedGlobalRegisters = SPARCEirABI.integerSystemReservedGlobalRegisters().toCollection();
        public boolean evaluate(GPR register) {
            return !_reservedGlobalRegisters.contains(SPARCEirRegister.GeneralPurpose.from(register));
        }
    });

    public static final int TRAP_STATE_SIZE;
    public static final int TRAP_NUMBER_OFFSET;
    public static final int TRAP_SP_OFFSET;
    public static final int TRAP_CALL_ADDRESS_OFFSET;

    public static final int TRAP_LATCH_OFFSET;

    static {
        final int globalRegisterWords = TRAP_SAVED_GLOBAL_SYMBOLIZER.numberOfValues(); // %g1 to %g5
        final int outRegisterWords = IN_SYMBOLIZER.numberOfValues();    // %o0 to %o7
        final int floatingPointRegisterWords = 32; // %f0-%f32
        final int stateRegisters = 2;
        TRAP_LATCH_OFFSET = Word.size();
        // Offset to %o6 in trap state.
        TRAP_SP_OFFSET = Word.size() * (globalRegisterWords + (O6.value() - O0.value()));
        // Offset to %o7 in trap state
        TRAP_CALL_ADDRESS_OFFSET = TRAP_SP_OFFSET + Word.size();
        TRAP_NUMBER_OFFSET = Word.size() * (globalRegisterWords + outRegisterWords + floatingPointRegisterWords + stateRegisters);
        TRAP_STATE_SIZE = TRAP_NUMBER_OFFSET + Word.size();
    }

    @INLINE(override = true)
    @Override
    public GPR latchRegister() {
        return LATCH_REGISTER;
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
                asm.lduw(LATCH_REGISTER, G0, LATCH_REGISTER);
            } else {
                asm.ldx(LATCH_REGISTER, G0, LATCH_REGISTER);
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

    @Override
    public Pointer getInstructionPointer(Pointer trapState) {
        // We're in the trap stub. The latch register is set to the disabled vm thread locals.
        return TRAP_INSTRUCTION_POINTER.pointer(Safepoint.getLatchRegister()).readWord(0).asPointer();
    }

    @Override
    public Pointer getStackPointer(Pointer trapState, TargetMethod targetMethod) {
        return trapState.readWord(SPARCSafepoint.TRAP_SP_OFFSET).asPointer();
    }

    @Override
    public Pointer getFramePointer(Pointer trapState, TargetMethod targetMethod) {
        final Pointer registerWindow = getStackPointer(trapState, targetMethod);
        final GPR framePointerRegister = (GPR) targetMethod.abi().framePointer();
        return SPARCStackFrameLayout.getRegisterInSavedWindow(registerWindow, framePointerRegister).asPointer();
    }

    @Override
    public Pointer getSafepointLatch(Pointer trapState) {
        return trapState.readWord(TRAP_LATCH_OFFSET).asPointer();
    }

    @Override
    public void setSafepointLatch(Pointer trapState, Pointer value) {
        trapState.writeWord(TRAP_LATCH_OFFSET, value);
    }

    @Override
    public Pointer getRegisterState(Pointer trapState) {
        return trapState;
    }

    @Override
    public int getTrapNumber(Pointer trapState) {
        return trapState.readWord(TRAP_NUMBER_OFFSET).asAddress().toInt();
    }

    public Pointer getCallAddressRegister(Pointer trapState) {
        return trapState.readWord(TRAP_CALL_ADDRESS_OFFSET).asPointer();
    }
}
