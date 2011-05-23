/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.runtime.amd64;

import static com.oracle.max.asm.target.amd64.AMD64.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.runtime.amd64.AMD64Safepoint.*;

import com.sun.cri.ci.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

/**
 * The trap state area on AMD64 contains the {@linkplain Trap.Number trap number} and the values of the
 * processor's registers when a trap occurs. The trap state area is as follows:
 *
 * <pre>
 *   <-- stack grows downward                       higher addresses -->
 * | ---- trap state area --- | RIP |==== stack as it was when trap occurred ===>
 * |<---  TRAP_STATE_SIZE --->|<-8->|
 *
 * </pre>
 * The layout of the trap state area is described by the following C-like struct declaration:
 * <pre>
 * trap_state {
 *     Word generalPurposeRegisters[16];
 *     Word xmmRegisters[16];
 *     Word trapNumber;
 *     Word flagsRegister;
 * }
 * </pre>
 *
 * The fault address is stored in the RIP slot, making this frame appear as if the trap location
 * called the trap stub directly.
 */
public final class AMD64TrapStateAccess extends TrapStateAccess {

    public static final int TRAP_NUMBER_OFFSET;
    public static final int FLAGS_OFFSET;

    public static final CiCalleeSaveArea CSA;
    static {
        CiRegister[] rsaRegs = {
            rax,  rcx,  rdx,   rbx,   rsp,   rbp,   rsi,   rdi,
            r8,   r9,   r10,   r11,   r12,   r13,   r14,   r15,
            xmm0, xmm1, xmm2,  xmm3,  xmm4,  xmm5,  xmm6,  xmm7,
            xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15
        };

        int size = (16 * 8) + (16 * 16);
        TRAP_NUMBER_OFFSET = size;
        size += 8;
        FLAGS_OFFSET = size;
        size += 8;

        CSA = new CiCalleeSaveArea(size, 8, rsaRegs);
    }

    @Override
    public Pointer getPC(Pointer trapState) {
        return trapState.readWord(vm().stubs.trapStub().frameSize()).asPointer();
    }

    @Override
    public void setPC(Pointer trapState, Pointer value) {
        trapState.writeWord(vm().stubs.trapStub().frameSize(), value);
    }

    @Override
    public Pointer getSP(Pointer trapState) {
        return trapState.plus(vm().stubs.trapStub().frameSize() + 8);
    }

    @Override
    public Pointer getFP(Pointer trapState) {
        return trapState.readWord(CSA.offsetOf(rbp)).asPointer();
    }

    @Override
    public Pointer getSafepointLatch(Pointer trapState) {
        Pointer rsa = getRegisterState(trapState);
        int offset = CSA.offsetOf(LATCH_REGISTER);
        return rsa.readWord(offset).asPointer();
    }

    @Override
    public void setSafepointLatch(Pointer trapState, Pointer value) {
        Pointer rsa = getRegisterState(trapState);
        int offset = CSA.offsetOf(LATCH_REGISTER);
        rsa.writeWord(offset, value);
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
        final Pointer rsa = getRegisterState(trapState);
        Log.println("Non-zero registers:");

        for (CiRegister reg : CSA.registers) {
            if (reg.isCpu()) {
                int offset = CSA.offsetOf(reg);
                final Word value = rsa.readWord(offset);
                if (!value.isZero()) {
                    Log.print("  ");
                    Log.print(reg.name);
                    Log.print("=");
                    Log.println(value);
                }
            }
        }
        Log.print("  rip=");
        Log.println(getPC(trapState));
        Log.print("  rflags=");
        final Word flags = rsa.readWord(FLAGS_OFFSET);
        Log.print(flags);
        Log.print(' ');
        logFlags(flags.asAddress().toInt());
        Log.println();
        if (false) {
            boolean seenNonZeroXMM = false;
            for (CiRegister reg : CSA.registers) {
                if (reg.isFpu()) {
                    int offset = CSA.offsetOf(reg);
                    final double value = rsa.readDouble(offset);
                    if (value != 0) {
                        if (!seenNonZeroXMM) {
                            Log.println("Non-zero XMM registers:");
                            seenNonZeroXMM = true;
                        }
                        Log.print("  ");
                        Log.print(reg.name);
                        Log.print("=");
                        Log.print(value);
                        Log.print("  {bits: ");
                        Log.print(Address.fromLong(Double.doubleToRawLongBits(value)));
                        Log.println("}");
                    }
                }
            }
        }
        final int trapNumber = getTrapNumber(trapState);
        Log.print("Trap number: ");
        Log.print(trapNumber);
        Log.print(" == ");
        Log.println(Trap.Number.toExceptionName(trapNumber));
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
}
