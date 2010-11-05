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
import static com.sun.max.vm.thread.VmThread.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.sparc.*;

/**
 * The trap state on SPARC contains the {@linkplain Trap.Number trap number} and the values of the
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
public final class SPARCTrapStateAccess extends TrapStateAccess {

    public static final int TRAP_STATE_SIZE;
    public static final int TRAP_NUMBER_OFFSET;
    public static final int TRAP_RETURN_VALUE_OFFSET;
    public static final int TRAP_SP_OFFSET;
    public static final int TRAP_CALL_ADDRESS_OFFSET;
    public static final int TRAP_LATCH_OFFSET;

    public static final GPR[] OUT_REGISTERS = {O0, O1, O2, O3, O4, O5, O6, O7};
    public static final GPR[] INTEGER_NON_SYSTEM_RESERVED_GLOBAL_REGISTERS = {G1, G2, G3, G4, G5};

    static {
        final int globalRegisterWords = INTEGER_NON_SYSTEM_RESERVED_GLOBAL_REGISTERS.length; // %g1 to %g5
        final int outRegisterWords = IN_SYMBOLIZER.numberOfValues();    // %o0 to %o7
        final int floatingPointRegisterWords = 32; // %f0-%f32
        final int stateRegisters = 2;
        TRAP_LATCH_OFFSET = Word.size();
        // Offset to %o0 in trap state
        TRAP_RETURN_VALUE_OFFSET =  Word.size() * globalRegisterWords;
        // Offset to %o6 in trap state.
        TRAP_SP_OFFSET = Word.size() * (globalRegisterWords + (O6.value() - O0.value()));
        // Offset to %o7 in trap state
        TRAP_CALL_ADDRESS_OFFSET = TRAP_SP_OFFSET + Word.size();
        TRAP_NUMBER_OFFSET = Word.size() * (globalRegisterWords + outRegisterWords + floatingPointRegisterWords + stateRegisters);
        TRAP_STATE_SIZE = TRAP_NUMBER_OFFSET + Word.size();
    }

    public static Pointer getCallAddressRegister(Pointer trapState) {
        return trapState.readWord(TRAP_CALL_ADDRESS_OFFSET).asPointer();
    }

    /**
     * Gets the number of bytes needed for a bitmap covering the integer registers in a trap state.
     */
    public static int registerReferenceMapSize() {
        return ByteArrayBitMap.computeBitMapSize(INTEGER_NON_SYSTEM_RESERVED_GLOBAL_REGISTERS.length + OUT_REGISTERS.length);
    }

    @HOSTED_ONLY
    public SPARCTrapStateAccess(VMConfiguration vmConfiguration) {
    }

    @Override
    public Pointer getInstructionPointer(Pointer trapState) {
        // We're in the trap stub. The latch register is set to the disabled vm thread locals.
        return TRAP_INSTRUCTION_POINTER.loadPtr(currentVmThreadLocals());
    }

    /**
     * Set the instruction pointer to which the trap stub should return.
     */
    @Override
    public void setInstructionPointer(Pointer trapState, Pointer value) {
        TRAP_INSTRUCTION_POINTER.store3(value);
    }

    @Override
    public Pointer getStackPointer(Pointer trapState, TargetMethod targetMethod) {
        return trapState.readWord(TRAP_SP_OFFSET).asPointer();
    }

    /**
     * Get the frame pointer of the trapped frame.
     */
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

    @Override
    public void setTrapNumber(Pointer trapState, int trapNumber) {
        trapState.writeWord(TRAP_NUMBER_OFFSET, Address.fromInt(trapNumber));
    }

    @Override
    public void logTrapState(Pointer trapState) {
        int trapStateIndex = 0;
        for (GPR register : INTEGER_NON_SYSTEM_RESERVED_GLOBAL_REGISTERS) {
            Log.print(register.toString());
            Log.print(": ");
            Log.println(trapState.readWord(trapStateIndex * Word.size()));
            trapStateIndex++;
        }
        for (GPR register : OUT_REGISTERS) {
            Log.print(register.toString());
            Log.print(": ");
            Log.println(trapState.readWord(trapStateIndex * Word.size()));
            trapStateIndex++;
        }
        final int trapNumber = getTrapNumber(trapState);
        Log.print("Trap number: ");
        Log.print(trapNumber);
        Log.print(" == ");
        Log.println(Trap.Number.toExceptionName(trapNumber));
    }

}
