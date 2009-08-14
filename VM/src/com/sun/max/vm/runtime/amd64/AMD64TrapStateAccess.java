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
import static com.sun.max.vm.runtime.amd64.AMD64Safepoint.*;

import com.sun.max.asm.amd64.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * The trap state area on AMD64 contains the {@linkplain Trap.Number trap number} and the values of the
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
public final class AMD64TrapStateAccess extends TrapStateAccess {

    private static final AMD64GeneralRegister64 RETURN_VALUE_REGISTER = RAX;
    private static final AMD64GeneralRegister64 STACK_POINTER_REGISTER = RSP;

    public static final int TRAP_STATE_SIZE_WITH_RIP;
    public static final int TRAP_STATE_SIZE_WITHOUT_RIP;
    public static final int TRAP_NUMBER_OFFSET;
    public static final int FLAGS_OFFSET;

    private static final String[] gprNames;
    private static final String[] xmmNames;

    static {
        final int gprCount = AMD64GeneralRegister64.ENUMERATOR.length();
        final int xmmCount = AMD64XMMRegister.ENUMERATOR.length();
        gprNames = new String[gprCount];
        xmmNames = new String[xmmCount];
        for (AMD64GeneralRegister64 register : AMD64GeneralRegister64.ENUMERATOR) {
            gprNames[register.ordinal()] = register.name();
        }
        for (AMD64XMMRegister register : AMD64XMMRegister.ENUMERATOR) {
            xmmNames[register.ordinal()] = register.name();
        }
        final int generalPurposeRegisterWords = gprCount;
        final int xmmRegisterWords = 2 * xmmCount;
        final int flagRegisterWords = 1;
        final int trapNumberWords = 1;
        TRAP_NUMBER_OFFSET = Word.size() * (generalPurposeRegisterWords + xmmRegisterWords);
        FLAGS_OFFSET = TRAP_NUMBER_OFFSET + Word.size();
        TRAP_STATE_SIZE_WITHOUT_RIP = TRAP_NUMBER_OFFSET + (Word.size() * (flagRegisterWords + trapNumberWords));
        TRAP_STATE_SIZE_WITH_RIP = TRAP_STATE_SIZE_WITHOUT_RIP + Word.size();
    }

    public AMD64TrapStateAccess(VMConfiguration vmConfiguration) {
        super();
    }

    public static Pointer getTrapStateFromRipPointer(Pointer ripPointer) {
        return ripPointer.minus(TRAP_STATE_SIZE_WITHOUT_RIP);
    }

    @Override
    public Pointer getInstructionPointer(Pointer trapState) {
        // the instruction pointer is the last word in the register state
        return trapState.readWord(TRAP_STATE_SIZE_WITHOUT_RIP).asPointer();
    }

    @Override
    public void setInstructionPointer(Pointer trapState, Pointer value) {
        trapState.writeWord(TRAP_STATE_SIZE_WITHOUT_RIP, value);
    }

    @Override
    public Pointer getStackPointer(Pointer trapState, TargetMethod targetMethod) {
        // TODO: get the frame pointer register from the ABI
        return trapState.plus(TRAP_STATE_SIZE_WITH_RIP);
    }

    @Override
    public Pointer getFramePointer(Pointer trapState, TargetMethod targetMethod) {
        // TODO: get the frame pointer register from the ABI
        return trapState.readWord(AMD64GeneralRegister64.RBP.value() * Word.size()).asPointer();
    }

    @Override
    public Pointer getSafepointLatch(Pointer trapState) {
        return trapState.readWord(LATCH_REGISTER.value() * Word.size()).asPointer();
    }

    @Override
    public void setSafepointLatch(Pointer trapState, Pointer value) {
        trapState.writeWord(LATCH_REGISTER.value() * Word.size(), value);
    }

    @Override
    public void setExceptionObject(Pointer trapState, Throwable throwable) {
        trapState.writeWord(RETURN_VALUE_REGISTER.value() * Word.size(), Reference.fromJava(throwable).toOrigin());
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
        Pointer register = getRegisterState(trapState);
        Log.println("Non-zero registers:");
        for (String gpr : gprNames) {
            final Word value = register.readWord(0);
            if (!value.isZero()) {
                Log.print("  ");
                Log.print(gpr);
                Log.print("=");
                Log.println(value);
            }
            register = register.plus(Word.size());
        }
        Log.print("  RIP=");
        Log.println(getInstructionPointer(trapState));
        Log.print("  RFLAGS=");
        final Word flags = trapState.readWord(FLAGS_OFFSET);
        Log.print(flags);
        Log.print(' ');
        logFlags(flags.asAddress().toInt());
        Log.println();
        boolean seenNonZeroXMM = false;
        for (String xmm : xmmNames) {
            final double value = register.readDouble(0);
            if (value != 0) {
                if (!seenNonZeroXMM) {
                    Log.println("Non-zero XMM registers:");
                    seenNonZeroXMM = true;
                }
                Log.print("  ");
                Log.print(xmm);
                Log.print("=");
                Log.print(value);
                Log.print("  {bits: ");
                Log.print(Address.fromLong(Double.doubleToRawLongBits(value)));
                Log.println("}");
            }
            register = register.plus(Word.size() * 2);
        }
        Log.print("Trap number: ");
        Log.println(getTrapNumber(trapState));
    }

    private static final String[] rflags = {
        "CF", // 0
        null, // 1
        "PF", // 2
        null, // 3
        "AF", // 4
        null, // 5
        "ZF", // 6
        "SF", // 7
        "TF", // 8
        "IF", // 9
        "DF", // 10
        "OF", // 11
        "IO", // 12
        "PL", // 13
        "NT", // 14
        null, // 15
        "RF", // 16
        "VM", // 17
        "AC", // 18
        "VIF", // 19
        "VIP", // 20
        "ID" // 21
    };

    private static void logFlags(int flags) {
        Log.print('{');
        boolean first = true;
        for (int i = rflags.length - 1; i >= 0; i--) {
            int mask = 1 << i;
            if ((flags & mask) != 0) {
                final String flag = rflags[i];
                if (flag != null) {
                    if (!first) {
                        Log.print(", ");
                    } else {
                        first = false;
                    }
                    Log.print(flag);
                }
            }
        }
        Log.print('}');
    }

    @Override
    public void setStackPointer(Pointer trapState, Pointer value) {
        trapState.writeWord(STACK_POINTER_REGISTER.value() * Word.size(), value);
    }
}
