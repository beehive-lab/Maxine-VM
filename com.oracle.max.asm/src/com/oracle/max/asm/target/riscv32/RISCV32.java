/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
package com.oracle.max.asm.target.riscv32;

import static com.sun.cri.ci.CiRegister.RegisterFlag.Byte;
import static com.sun.cri.ci.CiRegister.RegisterFlag.*;

import com.sun.cri.ci.*;

/**
 * Defines the registers of the RISC-V 32 architecture along with their ABI names.
 *
 * <pre>
 *     |----------+----------+-----------------------------------+--------|
 *     | Register | ABI Name | Description                       | Saver  |
 *     |----------+----------+-----------------------------------+--------|
 *     | x0       | zero     | Hard-wired zero                   | -      |
 *     | x1       | ra       | Return address                    | Caller |
 *     | x2       | sp       | Stack pointer                     | Callee |
 *     | x3       | gp       | Global pointer                    | -      |
 *     | x4       | tp       | Thread pointer                    | -      |
 *     | x5       | t0       | Temporary/alternate link register | Caller |
 *     | x6-7     | t1-2     | Temporaries                       | Caller |
 *     | x8       | s0/fp    | Saved register/frame pointer      | Callee |
 *     | x9       | s1       | Saved register                    | Callee |
 *     | x10-11   | a0-1     | Function arguments/return values  | Caller |
 *     | x12-17   | a2-7     | Function arguments                | Caller |
 *     | x18-27   | s2-11    | Saved registers                   | Callee |
 *     | x28-31   | t3-6     | Temporaries                       | Caller |
 *     | f0-7     | ft0-7    | FP temporaries                    | Caller |
 *     | f8-9     | fs0-1    | FP saved registers                | Callee |
 *     | f10-11   | fa0-1    | FP arguments/return values        | Caller |
 *     | f12-17   | fa2-7    | FP arguments                      | Caller |
 *     | f18-27   | fs2-11   | FP saved registers                | Callee |
 *     | f28-31   | ft8-11   | FP temporaries                    | Caller |
 *     |----------+----------+-----------------------------------+--------|
 * </pre>
 */
public class RISCV32 extends CiArchitecture {

    public static final CiRegister x0 = new CiRegister(0, 0, 4, "x0", CPU, Byte);
    public static final CiRegister x1 = new CiRegister(1, 1, 4, "x1", CPU, Byte);
    public static final CiRegister x2 = new CiRegister(2, 2, 4, "x2", CPU, Byte);
    public static final CiRegister x3 = new CiRegister(3, 3, 4, "x3", CPU, Byte);
    public static final CiRegister x4 = new CiRegister(4, 4, 4, "x4", CPU, Byte);
    public static final CiRegister x5 = new CiRegister(5, 5, 4, "x5", CPU, Byte);
    public static final CiRegister x6 = new CiRegister(6, 6, 4, "x6", CPU, Byte);
    public static final CiRegister x7 = new CiRegister(7, 7, 4, "x7", CPU, Byte);
    public static final CiRegister x8 = new CiRegister(8, 8, 4, "x8", CPU, Byte);
    public static final CiRegister x9 = new CiRegister(9, 9, 4, "x9", CPU, Byte);
    public static final CiRegister x10 = new CiRegister(10, 10, 4, "x10", CPU, Byte);
    public static final CiRegister x11 = new CiRegister(11, 11, 4, "x11", CPU, Byte);
    public static final CiRegister x12 = new CiRegister(12, 12, 4, "x12", CPU, Byte);
    public static final CiRegister x13 = new CiRegister(13, 13, 4, "x13", CPU, Byte);
    public static final CiRegister x14 = new CiRegister(14, 14, 4, "x14", CPU, Byte);
    public static final CiRegister x15 = new CiRegister(15, 15, 4, "x15", CPU, Byte);
    public static final CiRegister x16 = new CiRegister(16, 16, 4, "x16", CPU, Byte);
    public static final CiRegister x17 = new CiRegister(17, 17, 4, "x17", CPU, Byte);
    public static final CiRegister x18 = new CiRegister(18, 18, 4, "x18", CPU, Byte);
    public static final CiRegister x19 = new CiRegister(19, 19, 4, "x19", CPU, Byte);
    public static final CiRegister x20 = new CiRegister(20, 20, 4, "x20", CPU, Byte);
    public static final CiRegister x21 = new CiRegister(21, 21, 4, "x21", CPU, Byte);
    public static final CiRegister x22 = new CiRegister(22, 22, 4, "x22", CPU, Byte);
    public static final CiRegister x23 = new CiRegister(23, 23, 4, "x23", CPU, Byte);
    public static final CiRegister x24 = new CiRegister(24, 24, 4, "x24", CPU, Byte);
    public static final CiRegister x25 = new CiRegister(25, 25, 4, "x25", CPU, Byte);
    public static final CiRegister x26 = new CiRegister(26, 26, 4, "x26", CPU, Byte);
    public static final CiRegister x27 = new CiRegister(27, 27, 4, "x27", CPU, Byte);
    public static final CiRegister x28 = new CiRegister(28, 28, 4, "x28", CPU, Byte);
    public static final CiRegister x29 = new CiRegister(29, 29, 4, "x29", CPU, Byte);
    public static final CiRegister x30 = new CiRegister(30, 30, 4, "x30", CPU, Byte);
    public static final CiRegister x31 = new CiRegister(31, 31, 4, "x31", CPU, Byte);

    public static final CiRegister f0 = new CiRegister(0, 0, 4, "f0", FPU, FPU);
    public static final CiRegister f1 = new CiRegister(1, 1, 4, "f1", FPU, FPU);
    public static final CiRegister f2 = new CiRegister(2, 2, 4, "f2", FPU, FPU);
    public static final CiRegister f3 = new CiRegister(3, 3, 4, "f3", FPU, FPU);
    public static final CiRegister f4 = new CiRegister(4, 4, 4, "f4", FPU, FPU);
    public static final CiRegister f5 = new CiRegister(5, 5, 4, "f5", FPU, FPU);
    public static final CiRegister f6 = new CiRegister(6, 6, 4, "f6", FPU, FPU);
    public static final CiRegister f7 = new CiRegister(7, 7, 4, "f7", FPU, FPU);
    public static final CiRegister f8 = new CiRegister(8, 8, 4, "f8", FPU, FPU);
    public static final CiRegister f9 = new CiRegister(9, 9, 4, "f9", FPU, FPU);
    public static final CiRegister f10 = new CiRegister(10, 10, 4, "f10", FPU, FPU);
    public static final CiRegister f11 = new CiRegister(11, 11, 4, "f11", FPU, FPU);
    public static final CiRegister f12 = new CiRegister(12, 12, 4, "f12", FPU, FPU);
    public static final CiRegister f13 = new CiRegister(13, 13, 4, "f13", FPU, FPU);
    public static final CiRegister f14 = new CiRegister(14, 14, 4, "f14", FPU, FPU);
    public static final CiRegister f15 = new CiRegister(15, 15, 4, "f15", FPU, FPU);
    public static final CiRegister f16 = new CiRegister(16, 16, 4, "f16", FPU, FPU);
    public static final CiRegister f17 = new CiRegister(17, 17, 4, "f17", FPU, FPU);
    public static final CiRegister f18 = new CiRegister(18, 18, 4, "f18", FPU, FPU);
    public static final CiRegister f19 = new CiRegister(19, 19, 4, "f19", FPU, FPU);
    public static final CiRegister f20 = new CiRegister(20, 20, 4, "f20", FPU, FPU);
    public static final CiRegister f21 = new CiRegister(21, 21, 4, "f21", FPU, FPU);
    public static final CiRegister f22 = new CiRegister(22, 22, 4, "f22", FPU, FPU);
    public static final CiRegister f23 = new CiRegister(23, 23, 4, "f23", FPU, FPU);
    public static final CiRegister f24 = new CiRegister(24, 24, 4, "f24", FPU, FPU);
    public static final CiRegister f25 = new CiRegister(25, 25, 4, "f25", FPU, FPU);
    public static final CiRegister f26 = new CiRegister(26, 26, 4, "f26", FPU, FPU);
    public static final CiRegister f27 = new CiRegister(27, 27, 4, "f27", FPU, FPU);
    public static final CiRegister f28 = new CiRegister(28, 28, 4, "f28", FPU, FPU);
    public static final CiRegister f29 = new CiRegister(29, 29, 4, "f29", FPU, FPU);
    public static final CiRegister f30 = new CiRegister(30, 30, 4, "f30", FPU, FPU);
    public static final CiRegister f31 = new CiRegister(31, 31, 4, "f31", FPU, FPU);

    public static final CiRegister zero = x0;
    public static final CiRegister ra   = x1;
    public static final CiRegister sp   = x2;
    public static final CiRegister gp   = x3;
    public static final CiRegister tp   = x4;

    public static final CiRegister t0 = x5;
    public static final CiRegister t1 = x6;
    public static final CiRegister t2 = x7;
    public static final CiRegister t3 = x28;
    public static final CiRegister t4 = x29;
    public static final CiRegister t5 = x30;
    public static final CiRegister t6 = x31;

    public static final CiRegister fp = x8;

    public static final CiRegister a0 = x10;
    public static final CiRegister a1 = x11;
    public static final CiRegister a2 = x12;
    public static final CiRegister a3 = x13;
    public static final CiRegister a4 = x14;
    public static final CiRegister a5 = x15;
    public static final CiRegister a6 = x16;
    public static final CiRegister a7 = x17;

    public static final CiRegister s1  = x9;
    public static final CiRegister s2  = x17;
    public static final CiRegister s3  = x18;
    public static final CiRegister s4  = x19;
    public static final CiRegister s5  = x20;
    public static final CiRegister s6  = x21;
    public static final CiRegister s7  = x22;
    public static final CiRegister s8  = x23;
    public static final CiRegister s9  = x24;
    public static final CiRegister s10 = x25;
    public static final CiRegister s11 = x26;

    public static final CiRegister ft0  = f0;
    public static final CiRegister ft1  = f1;
    public static final CiRegister ft2  = f2;
    public static final CiRegister ft3  = f3;
    public static final CiRegister ft4  = f4;
    public static final CiRegister ft5  = f5;
    public static final CiRegister ft6  = f6;
    public static final CiRegister ft7  = f7;
    public static final CiRegister ft8  = f28;
    public static final CiRegister ft9  = f29;
    public static final CiRegister ft10 = f30;
    public static final CiRegister ft11 = f31;

    public static final CiRegister fs0  = f8;
    public static final CiRegister fs1  = f9;
    public static final CiRegister fs2  = f18;
    public static final CiRegister fs3  = f19;
    public static final CiRegister fs4  = f20;
    public static final CiRegister fs5  = f21;
    public static final CiRegister fs6  = f22;
    public static final CiRegister fs7  = f23;
    public static final CiRegister fs8  = f24;
    public static final CiRegister fs9  = f25;
    public static final CiRegister fs10 = f26;
    public static final CiRegister fs11 = f27;

    public static final CiRegister fa0 = f10;
    public static final CiRegister fa1 = f11;
    public static final CiRegister fa2 = f12;
    public static final CiRegister fa3 = f13;
    public static final CiRegister fa4 = f14;
    public static final CiRegister fa5 = f15;
    public static final CiRegister fa6 = f16;
    public static final CiRegister fa7 = f17;

    public static final CiRegister[] cpuRegisters = {
        x0, x1, x2, x3, x4, x5, x6, x7, x8, x9, x10, x11, x12, x13,
        x14, x15, x16, x17, x18, x19, x20, x21, x22, x23, x24,
        x25, x26, x27, x28, x29, x30, x31
    };

    public static final CiRegister[] fpuRegisters = {
        f0, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13,
        f14, f15, f16, f17, f18, f19, f20, f21, f22, f23, f24,
        f25, f26, f27, f28, f29, f30, f31
    };

    public static final CiRegister[] allRegisters = {
        x0, x1, x2, x3, x4, x5, x6, x7, x8, x9, x10, x11, x12, x13,
        x14, x15, x16, x17, x18, x19, x20, x21, x22, x23, x24,
        x25, x26, x27, x28, x29, x30, x31,
        f0, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13,
        f14, f15, f16, f17, f18, f19, f20, f21, f22, f23, f24,
        f25, f26, f27, f28, f29, f30, f31
    };

    public RISCV32() {
        super("RISCV32",                        //architecture name
              4,                                //word size (4 bytes)
              ByteOrder.LittleEndian,           //endianness
              allRegisters,                     //available registers
              0, /*LOAD_STORE | STORE_STORE*/   //implicitMemoryBarriers (no implicit barriers)
              -1,                               //nativeCallDisplacementOffset (ignore)
              32,                               //registerReferenceMapBitCount
              4);                               //returnAddressSize (4 bytes)
    }

}
