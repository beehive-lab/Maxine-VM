/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved. DO NOT ALTER OR REMOVE COPYRIGHT NOTICES
 * OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2 for
 * more details (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 2 along with this work; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA or visit www.oracle.com if you need
 * additional information or have any questions.
 */
package com.oracle.max.asm.target.armv7;

import com.sun.cri.ci.CiArchitecture;
import com.sun.cri.ci.CiKind;
import com.sun.cri.ci.CiRegister;
import com.sun.cri.ci.CiRegister.RegisterFlag;
import com.sun.cri.ci.CiRegisterValue;

import static com.oracle.max.cri.intrinsics.MemoryBarriers.LOAD_STORE;
import static com.oracle.max.cri.intrinsics.MemoryBarriers.STORE_STORE;
import static com.oracle.max.cri.intrinsics.MemoryBarriers.LOAD_LOAD;
import static com.oracle.max.cri.intrinsics.MemoryBarriers.STORE_LOAD;
import static com.sun.cri.ci.CiRegister.RegisterFlag.CPU;

/**
 * Represents the ARMv7 architecture.
 */
public class ARMV7 extends CiArchitecture {

    public static final CiRegister r0 = new CiRegister(0, 0, 4, "r0", CPU, RegisterFlag.Byte);
    public static final CiRegister r1 = new CiRegister(1, 1, 4, "r1", CPU, RegisterFlag.Byte);
    public static final CiRegister r2 = new CiRegister(2, 2, 4, "r2", CPU, RegisterFlag.Byte);
    public static final CiRegister r3 = new CiRegister(3, 3, 4, "r3", CPU, RegisterFlag.Byte);
    public static final CiRegister r4 = new CiRegister(4, 4, 4, "r4", CPU, RegisterFlag.Byte);
    public static final CiRegister r5 = new CiRegister(5, 5, 4, "r5", CPU, RegisterFlag.Byte);
    public static final CiRegister r6 = new CiRegister(6, 6, 4, "r6", CPU, RegisterFlag.Byte);
    public static final CiRegister r7 = new CiRegister(7, 7, 4, "r7", CPU, RegisterFlag.Byte);
    public static final CiRegister r8 = new CiRegister(8, 8, 4, "r8", CPU, RegisterFlag.Byte);
    public static final CiRegister r9 = new CiRegister(9, 9, 4, "r9", CPU, RegisterFlag.Byte);
    public static final CiRegister r10 = new CiRegister(10, 10, 4, "r10", CPU, RegisterFlag.Byte);
    public static final CiRegister r11 = new CiRegister(11, 11, 4, "r11", CPU, RegisterFlag.Byte);
    public static final CiRegister r12 = new CiRegister(12, 12, 4, "r12", CPU, RegisterFlag.Byte);
    public static final CiRegister r13 = new CiRegister(13, 13, 4, "r13", CPU, RegisterFlag.Byte);
    public static final CiRegister r14 = new CiRegister(14, 14, 4, "r14", CPU, RegisterFlag.Byte);
    public static final CiRegister r15 = new CiRegister(15, 15, 4, "r15", CPU, RegisterFlag.Byte);

    public static final CiRegister s0 = new CiRegister(16, 0, 4, "s0", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s1 = new CiRegister(17, 1, 4, "s1", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s2 = new CiRegister(18, 2, 4, "s2", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s3 = new CiRegister(19, 3, 4, "s3", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s4 = new CiRegister(20, 4, 4, "s4", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s5 = new CiRegister(21, 5, 4, "s5", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s6 = new CiRegister(22, 6, 4, "s6", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s7 = new CiRegister(23, 7, 4, "s7", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s8 = new CiRegister(24, 8, 4, "s8", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s9 = new CiRegister(25, 9, 4, "s9", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s10 = new CiRegister(26, 10, 4, "s10", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s11 = new CiRegister(27, 11, 4, "s11", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s12 = new CiRegister(28, 12, 4, "s12", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s13 = new CiRegister(29, 13, 4, "s13", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s14 = new CiRegister(30, 14, 4, "s14", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s15 = new CiRegister(31, 15, 4, "s15", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s16 = new CiRegister(32, 16, 4, "s16", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s17 = new CiRegister(33, 17, 4, "s17", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s18 = new CiRegister(34, 18, 4, "s18", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s19 = new CiRegister(35, 19, 4, "s19", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s20 = new CiRegister(36, 20, 4, "s20", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s21 = new CiRegister(37, 21, 4, "s21", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s22 = new CiRegister(38, 22, 4, "s22", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s23 = new CiRegister(39, 23, 4, "s23", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s24 = new CiRegister(40, 24, 4, "s24", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s25 = new CiRegister(41, 25, 4, "s25", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s26 = new CiRegister(42, 26, 4, "s26", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s27 = new CiRegister(43, 27, 4, "s27", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s28 = new CiRegister(44, 28, 4, "s28", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s29 = new CiRegister(45, 29, 4, "s29", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s30 = new CiRegister(46, 30, 4, "s30", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s31 = new CiRegister(47, 31, 4, "s31", RegisterFlag.FPU, RegisterFlag.FPU);


    public static final CiRegister[] cpuRegisters = { r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15};

    public static final CiRegister[] floatRegisters = { s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16,
        s17, s18, s19, s20, s21, s22, s23, s24, s25, s26, s27, s28, s29, s30, s31};

    public static final CiRegister[] allRegisters = { r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15, s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16,
        s17, s18, s19, s20, s21, s22, s23, s24, s25, s26, s27, s28, s29, s30, s31 };

    public static final CiRegister[] cpuxmmRegisters = { r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16,
        s17, s18, s19, s20, s21, s22, s23, s24, s25, s26, s27, s28, s29, s30, s31};

    // TODO: checkout load/store ordering on A15
    // TODO: checkout nativeDisplacementOffset on A15
    public ARMV7() {
        super("ARMV7", 4, ByteOrder.LittleEndian, allRegisters, LOAD_STORE | STORE_STORE | LOAD_LOAD | STORE_LOAD, 1, s31.number + 1, 4);
    }
    public static final CiRegisterValue RSP = r13.asValue(CiKind.Int);
    public static final CiRegister LR = r14;
    public static final CiRegister PC = r15;
    public static final CiRegister rsp = r13;
    public static final CiRegister rip = new CiRegister(48, -1, 0, "rip");

    @Override
    public boolean isARM() {
        return true;
    }

    @Override
    public boolean twoOperandMode() {
        return true;
    }
}
