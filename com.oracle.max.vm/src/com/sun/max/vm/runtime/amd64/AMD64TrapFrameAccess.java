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
 * The trap frame on AMD64 contains the {@linkplain Trap.Number trap number} and the values of the
 * processor's registers when a trap occurs. The trap frame is as follows:
 *
 * <pre>
 *   Base       Contents
 *
 *          :                                :
 *          |                                | Trapped frame
 *   -------+--------------------------------+----------
 *          | trapped PC                     | Trap frame
 *          +--------------------------------+     ---
 *          | flags register                 |      ^
 *          +--------------------------------+      |
 *          | trap number                    |      |
 *          +--------------------------------+      |
 *          |                                |    frame
 *          : XMM0 - XMM15  save area        :    size
 *          |                                |      |
 *          +--------------------------------+      |
 *          |                                |      |
 *          : GPR (rax - r15)  save area     :      |
 *    %sp   |                                |      v
 *   -------+--------------------------------+----------
 * </pre>
 *
 * Or, alternatively, the trap frame is described by the following C-like struct declaration:
 *
 * <pre>
 * trap_state {
 *     Word generalPurposeRegisters[16];
 *     Word xmmRegisters[16];
 *     Word trapNumber;
 *     Word flagsRegister;
 * }
 * </pre>
 *
 * The fault address (i.e. trapped PC) is stored in the return address slot, making the
 * trap frame appear as if the trapped method called the trap stub directly.
 */
public final class AMD64TrapFrameAccess extends TrapFrameAccess {

    public static final int TRAP_NUMBER_OFFSET;
    public static final int FLAGS_OFFSET;

    public static final CiCalleeSaveLayout CSL;
    static {
        CiRegister[] csaRegs = {
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

        CSL = new CiCalleeSaveLayout(0, size, 8, csaRegs);
    }

    @Override
    public Pointer getPCPointer(Pointer trapFrame) {
        return trapFrame.plus(vm().stubs.trapStub().frameSize());
    }

    @Override
    public Pointer getSP(Pointer trapFrame) {
        return trapFrame.plus(vm().stubs.trapStub().frameSize() + 8);
    }

    @Override
    public Pointer getFP(Pointer trapFrame) {
        return trapFrame.readWord(CSL.offsetOf(rbp)).asPointer();
    }

    @Override
    public Pointer getSafepointLatch(Pointer trapFrame) {
        Pointer csa = getCalleeSaveArea(trapFrame);
        int offset = CSL.offsetOf(LATCH_REGISTER);
        return csa.readWord(offset).asPointer();
    }

    @Override
    public void setSafepointLatch(Pointer trapFrame, Pointer value) {
        Pointer csa = getCalleeSaveArea(trapFrame);
        int offset = CSL.offsetOf(LATCH_REGISTER);
        csa.writeWord(offset, value);
    }

    @Override
    public Pointer getCalleeSaveArea(Pointer trapFrame) {
        return trapFrame.plus(CSL.frameOffsetToCSA);
    }

    @Override
    public int getTrapNumber(Pointer trapFrame) {
        return trapFrame.readWord(TRAP_NUMBER_OFFSET).asAddress().toInt();
    }

    @Override
    public void setTrapNumber(Pointer trapFrame, int trapNumber) {
        trapFrame.writeWord(TRAP_NUMBER_OFFSET, Address.fromInt(trapNumber));
    }

    @Override
    public void logTrapFrame(Pointer trapFrame) {
        final Pointer csa = getCalleeSaveArea(trapFrame);
        Log.println("Non-zero registers:");

        for (CiRegister reg : CSL.registers) {
            if (reg.isCpu()) {
                int offset = CSL.offsetOf(reg);
                final Word value = csa.readWord(offset);
                if (!value.isZero()) {
                    Log.print("  ");
                    Log.print(reg.name);
                    Log.print("=");
                    Log.println(value);
                }
            }
        }
        Log.print("  rip=");
        Log.println(getPC(trapFrame));
        Log.print("  rflags=");
        final Word flags = csa.readWord(FLAGS_OFFSET);
        Log.print(flags);
        Log.print(' ');
        logFlags(flags.asAddress().toInt());
        Log.println();
        if (false) {
            boolean seenNonZeroXMM = false;
            for (CiRegister reg : CSL.registers) {
                if (reg.isFpu()) {
                    int offset = CSL.offsetOf(reg);
                    final double value = csa.readDouble(offset);
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
        final int trapNumber = getTrapNumber(trapFrame);
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
