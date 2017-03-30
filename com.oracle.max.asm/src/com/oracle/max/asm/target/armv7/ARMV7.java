/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.oracle.max.asm.target.armv7;

import static com.oracle.max.cri.intrinsics.MemoryBarriers.*;
import static com.sun.cri.ci.CiRegister.RegisterFlag.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiRegister.*;

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

    public static final CiRegister s0 = new CiRegister(16, 0, 8, "s0", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s1 = new CiRegister(17, 1, 8, "s1", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s2 = new CiRegister(18, 2, 8, "s2", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s3 = new CiRegister(19, 3, 8, "s3", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s4 = new CiRegister(20, 4, 8, "s4", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s5 = new CiRegister(21, 5, 8, "s5", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s6 = new CiRegister(22, 6, 8, "s6", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s7 = new CiRegister(23, 7, 8, "s7", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s8 = new CiRegister(24, 8, 8, "s8", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s9 = new CiRegister(25, 9, 8, "s9", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s10 = new CiRegister(26, 10, 8, "s10", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s11 = new CiRegister(27, 11, 8, "s11", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s12 = new CiRegister(28, 12, 8, "s12", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s13 = new CiRegister(29, 13, 8, "s13", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s14 = new CiRegister(30, 14, 8, "s14", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s15 = new CiRegister(31, 15, 8, "s15", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s16 = new CiRegister(32, 16, 8, "s16", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s17 = new CiRegister(33, 17, 8, "s17", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s18 = new CiRegister(34, 18, 8, "s18", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s19 = new CiRegister(35, 19, 8, "s19", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s20 = new CiRegister(36, 20, 8, "s20", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s21 = new CiRegister(37, 21, 8, "s21", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s22 = new CiRegister(38, 22, 8, "s22", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s23 = new CiRegister(39, 23, 8, "s23", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s24 = new CiRegister(40, 24, 8, "s24", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s25 = new CiRegister(41, 25, 8, "s25", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s26 = new CiRegister(42, 26, 8, "s26", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s27 = new CiRegister(43, 27, 8, "s27", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s28 = new CiRegister(44, 28, 8, "s28", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s29 = new CiRegister(45, 29, 8, "s29", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s30 = new CiRegister(46, 30, 8, "s30", RegisterFlag.FPU, RegisterFlag.FPU);
    public static final CiRegister s31 = new CiRegister(47, 31, 8, "s31", RegisterFlag.FPU, RegisterFlag.FPU);

    public static final CiRegister[] cpuRegisters = {
        r0, r1, r2, r3, r4, r5, r6, r7,
        r8, r9, r10, r11, r12, r13, r14,
        r15
    };

    public static final CiRegister[] floatRegisters = {
        s0, s1, s2, s3, s4, s5, s6, s7,
        s8, s9, s10, s11, s12, s13, s14,
        s15, s16, s17, s18, s19, s20, s21,
        s22, s23, s24, s25, s26, s27, s28,
        s29, s30, s31
    };

    public static final CiRegister[] allRegisters = {
        r0, r1, r2, r3, r4, r5, r6, r7,
        r8, r9, r10, r11, r12, r13, r14,
        r15, s0, s1, s2, s3, s4, s5, s6,
        s7, s8, s9, s10, s11, s12, s13,
        s14, s15, s16, s17, s18, s19, s20,
        s21, s22, s23, s24, s25, s26, s27,
        s28, s29, s30, s31
    };

    public static final CiRegister[] cpuxmmRegisters = {
        r0, r1, r2, r3, r4, r5, r6, r7,
        r8, r9, r10, r11, r12, r13, r14,
        s0, s1, s2, s3, s4, s5, s6, s7,
        s8, s9, s10, s11, s12, s13, s14,
        s15, s16, s17, s18, s19, s20, s21,
        s22, s23, s24, s25, s26, s27, s28,
        s29, s30, s31
    };

    public ARMV7() {
        super("ARMV7", 4, ByteOrder.LittleEndian, allRegisters, LOAD_STORE | STORE_STORE | LOAD_LOAD | STORE_LOAD, 1, s31.number + 1, 4);
    }

    public static final CiRegisterValue RSP = r13.asValue(CiKind.Int);
    public static final CiRegister LR = r14;
    public static final CiRegister PC = r15;
    public static final CiRegister FP = r11;
    public static final CiRegister rsp = r13;
    public static final CiRegister LATCH_REGISTER = r10;
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
