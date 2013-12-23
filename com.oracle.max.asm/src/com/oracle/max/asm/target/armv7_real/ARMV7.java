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
package com.oracle.max.asm.target.armv7_real;

import static com.oracle.max.cri.intrinsics.MemoryBarriers.*;
import static com.sun.cri.ci.CiRegister.RegisterFlag.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiRegister.RegisterFlag;

/**
 * Represents the ARMv7 architecture.
 */
public class ARMV7 extends CiArchitecture {

    // General purpose CPU registers
    public static final CiRegister r0 = new CiRegister(0, 0, 4, "r0", CPU, RegisterFlag.Byte);
    public static final CiRegister r1 = new CiRegister(1, 1, 4, "r1", CPU, RegisterFlag.Byte);
    public static final CiRegister r2 = new CiRegister(2, 2, 4, "r2", CPU, RegisterFlag.Byte);
    public static final CiRegister r3 = new CiRegister(3, 3, 4, "r3", CPU, RegisterFlag.Byte);
    public static final CiRegister r4 = new CiRegister(4, 4, 4, "r4", CPU, RegisterFlag.Byte);
    public static final CiRegister r5 = new CiRegister(5, 5, 4, "r5", CPU, RegisterFlag.Byte);
    public static final CiRegister r6 = new CiRegister(6, 6, 4, "r6", CPU, RegisterFlag.Byte);
    public static final CiRegister r7 = new CiRegister(7, 7, 4, "r7", CPU, RegisterFlag.Byte);

    public static final CiRegister r8  = new CiRegister(8,  8,  4, "r8", CPU, RegisterFlag.Byte);
    public static final CiRegister r9  = new CiRegister(9,  9,  4, "r9", CPU, RegisterFlag.Byte);
    public static final CiRegister r10 = new CiRegister(10, 10, 4, "r10", CPU, RegisterFlag.Byte);
    public static final CiRegister r11 = new CiRegister(11, 11, 4, "r11", CPU, RegisterFlag.Byte);
    public static final CiRegister r12 = new CiRegister(12, 12, 4, "r12", CPU, RegisterFlag.Byte);
    // R13 -> SP
    public static final CiRegister r13 = new CiRegister(13, 13, 4, "r13", CPU, RegisterFlag.Byte);
    // R14 -> LR
    public static final CiRegister r14 = new CiRegister(14, 14, 4, "r14", CPU, RegisterFlag.Byte);
    // R15 -> PC
    public static final CiRegister r15 = new CiRegister(15, 15, 4, "r15", CPU, RegisterFlag.Byte);

    //TODO: CiArchitecture doesn't allow mixed-size registers (pretending we have 4 byte FP regs)
    //TODO: Might be something to do with the spillSlotSize the size of the stack slot used to spill the value of the register
    public static final CiRegister vfp0 = new CiRegister(16, 0, 4, "vfp0", FPU);
    public static final CiRegister vfp1 = new CiRegister(17, 1, 4, "vfp1", FPU);
    public static final CiRegister vfp2 = new CiRegister(18, 2, 4, "vfp2", FPU);
    public static final CiRegister vfp3 = new CiRegister(19, 3, 4, "vfp3", FPU);
    public static final CiRegister vfp4 = new CiRegister(20, 4, 4, "vfp4", FPU);
    public static final CiRegister vfp5 = new CiRegister(21, 5, 4, "vfp5", FPU);
    public static final CiRegister vfp6 = new CiRegister(22, 6, 4, "vfp6", FPU);
    public static final CiRegister vfp7 = new CiRegister(23, 7, 4, "vfp7", FPU);
    public static final CiRegister vfp8 = new CiRegister(24, 8, 4, "vfp8", FPU);
    public static final CiRegister vfp9 = new CiRegister(25, 9, 4, "vfp9", FPU);
    public static final CiRegister vfp10 = new CiRegister(26, 10, 4, "vfp10", FPU);
    public static final CiRegister vfp11 = new CiRegister(27, 11, 4, "vfp11", FPU);
    public static final CiRegister vfp12 = new CiRegister(28, 12, 4, "vfp12", FPU);
    public static final CiRegister vfp13 = new CiRegister(29, 13, 4, "vfp13", FPU);
    public static final CiRegister vfp14 = new CiRegister(30, 14, 4, "vfp14", FPU);
    public static final CiRegister vfp15 = new CiRegister(31, 15, 4, "vfp15", FPU);

    public static final CiRegister[] cpuRegisters = {
            r0, r1, r2, r3, r4, r5, r6, r7,
            r8, r9, r10, r11, r12, r13, r14, r15
    };

    // TODO: not including the other 16 floating point registers out of laziness for now
    public static final CiRegister[] floatRegisters = {
            vfp0, vfp1, vfp2, vfp3, vfp4, vfp5,
            vfp6, vfp7, vfp8, vfp9, vfp10, vfp11,
            vfp12, vfp13, vfp14, vfp15
    };


    /**
     * Register used to construct an instruction-relative address.
     */


    public static final CiRegister[] allRegisters = {
            r0,  r1,  r2,   r3,   r4,   r5,   r6,   r7,
            r8,   r9,   r10,   r11,   r12,   r13,   r14,   r15,
            vfp0, vfp1, vfp2, vfp3, vfp4, vfp5,
            vfp6, vfp7, vfp8, vfp9, vfp10, vfp11,
            vfp12, vfp13, vfp14, vfp15
    };


    // TODO: checkout load/store ordering on A15
    // TODO: checkout nativeDisplacementOffset on A15
    public ARMV7() {
        super("ARMV7",
                4,
                ByteOrder.BigEndian,
                allRegisters,
                LOAD_STORE | STORE_STORE,
                1,
                r15.encoding + 1,
                4);
    }

    @Override
    public boolean isARMV7() {
        return true;
    }

}
