/*
 * Copyright (c) 2018-2019, APT Group, School of Computer Science,
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
package com.oracle.max.asm.target.riscv64;

import static com.sun.cri.ci.CiRegister.RegisterFlag.*;

import com.sun.cri.ci.*;

/**
 * Defines the registers of the RISC-V 64 architecture along with their ABI names.
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
public class RISCV64 extends CiArchitecture {

    public static final CiRegister x0 = new CiRegister(0, 0, 8, "x0", CPU, Byte);
    public static final CiRegister x1 = new CiRegister(1, 1, 8, "x1", CPU, Byte);
    public static final CiRegister x2 = new CiRegister(2, 2, 8, "x2", CPU, Byte);
    public static final CiRegister x3 = new CiRegister(3, 3, 8, "x3", CPU, Byte);
    public static final CiRegister x4 = new CiRegister(4, 4, 8, "x4", CPU, Byte);
    public static final CiRegister x5 = new CiRegister(5, 5, 8, "x5", CPU, Byte);
    public static final CiRegister x6 = new CiRegister(6, 6, 8, "x6", CPU, Byte);
    public static final CiRegister x7 = new CiRegister(7, 7, 8, "x7", CPU, Byte);
    public static final CiRegister x8 = new CiRegister(8, 8, 8, "x8", CPU, Byte);
    public static final CiRegister x9 = new CiRegister(9, 9, 8, "x9", CPU, Byte);
    public static final CiRegister x10 = new CiRegister(10, 10, 8, "x10", CPU, Byte);
    public static final CiRegister x11 = new CiRegister(11, 11, 8, "x11", CPU, Byte);
    public static final CiRegister x12 = new CiRegister(12, 12, 8, "x12", CPU, Byte);
    public static final CiRegister x13 = new CiRegister(13, 13, 8, "x13", CPU, Byte);
    public static final CiRegister x14 = new CiRegister(14, 14, 8, "x14", CPU, Byte);
    public static final CiRegister x15 = new CiRegister(15, 15, 8, "x15", CPU, Byte);
    public static final CiRegister x16 = new CiRegister(16, 16, 8, "x16", CPU, Byte);
    public static final CiRegister x17 = new CiRegister(17, 17, 8, "x17", CPU, Byte);
    public static final CiRegister x18 = new CiRegister(18, 18, 8, "x18", CPU, Byte);
    public static final CiRegister x19 = new CiRegister(19, 19, 8, "x19", CPU, Byte);
    public static final CiRegister x20 = new CiRegister(20, 20, 8, "x20", CPU, Byte);
    public static final CiRegister x21 = new CiRegister(21, 21, 8, "x21", CPU, Byte);
    public static final CiRegister x22 = new CiRegister(22, 22, 8, "x22", CPU, Byte);
    public static final CiRegister x23 = new CiRegister(23, 23, 8, "x23", CPU, Byte);
    public static final CiRegister x24 = new CiRegister(24, 24, 8, "x24", CPU, Byte);
    public static final CiRegister x25 = new CiRegister(25, 25, 8, "x25", CPU, Byte);
    public static final CiRegister x26 = new CiRegister(26, 26, 8, "x26", CPU, Byte);
    public static final CiRegister x27 = new CiRegister(27, 27, 8, "x27", CPU, Byte);
    public static final CiRegister x28 = new CiRegister(28, 28, 8, "x28", CPU, Byte);
    public static final CiRegister x29 = new CiRegister(29, 29, 8, "x29", CPU, Byte);
    public static final CiRegister x30 = new CiRegister(30, 30, 8, "x30", CPU, Byte);
    public static final CiRegister x31 = new CiRegister(31, 31, 8, "x31", CPU, Byte);

    public static final CiRegister f0 = new CiRegister(32, 0, 8, "f0", FPU, FPU);
    public static final CiRegister f1 = new CiRegister(33, 1, 8, "f1", FPU, FPU);
    public static final CiRegister f2 = new CiRegister(34, 2, 8, "f2", FPU, FPU);
    public static final CiRegister f3 = new CiRegister(35, 3, 8, "f3", FPU, FPU);
    public static final CiRegister f4 = new CiRegister(36, 4, 8, "f4", FPU, FPU);
    public static final CiRegister f5 = new CiRegister(37, 5, 8, "f5", FPU, FPU);
    public static final CiRegister f6 = new CiRegister(38, 6, 8, "f6", FPU, FPU);
    public static final CiRegister f7 = new CiRegister(39, 7, 8, "f7", FPU, FPU);
    public static final CiRegister f8 = new CiRegister(40, 8, 8, "f8", FPU, FPU);
    public static final CiRegister f9 = new CiRegister(41, 9, 8, "f9", FPU, FPU);
    public static final CiRegister f10 = new CiRegister(42, 10, 8, "f10", FPU, FPU);
    public static final CiRegister f11 = new CiRegister(43, 11, 8, "f11", FPU, FPU);
    public static final CiRegister f12 = new CiRegister(44, 12, 8, "f12", FPU, FPU);
    public static final CiRegister f13 = new CiRegister(45, 13, 8, "f13", FPU, FPU);
    public static final CiRegister f14 = new CiRegister(46, 14, 8, "f14", FPU, FPU);
    public static final CiRegister f15 = new CiRegister(47, 15, 8, "f15", FPU, FPU);
    public static final CiRegister f16 = new CiRegister(48, 16, 8, "f16", FPU, FPU);
    public static final CiRegister f17 = new CiRegister(49, 17, 8, "f17", FPU, FPU);
    public static final CiRegister f18 = new CiRegister(50, 18, 8, "f18", FPU, FPU);
    public static final CiRegister f19 = new CiRegister(51, 19, 8, "f19", FPU, FPU);
    public static final CiRegister f20 = new CiRegister(52, 20, 8, "f20", FPU, FPU);
    public static final CiRegister f21 = new CiRegister(53, 21, 8, "f21", FPU, FPU);
    public static final CiRegister f22 = new CiRegister(54, 22, 8, "f22", FPU, FPU);
    public static final CiRegister f23 = new CiRegister(55, 23, 8, "f23", FPU, FPU);
    public static final CiRegister f24 = new CiRegister(56, 24, 8, "f24", FPU, FPU);
    public static final CiRegister f25 = new CiRegister(57, 25, 8, "f25", FPU, FPU);
    public static final CiRegister f26 = new CiRegister(58, 26, 8, "f26", FPU, FPU);
    public static final CiRegister f27 = new CiRegister(59, 27, 8, "f27", FPU, FPU);
    public static final CiRegister f28 = new CiRegister(60, 28, 8, "f28", FPU, FPU);
    public static final CiRegister f29 = new CiRegister(61, 29, 8, "f29", FPU, FPU);
    public static final CiRegister f30 = new CiRegister(62, 30, 8, "f30", FPU, FPU);
    public static final CiRegister f31 = new CiRegister(63, 31, 8, "f31", FPU, FPU);

    public static final CiRegister zero = x0;
    public static final CiRegister zr = x0;
    public static final CiRegister ra   = x1;
    public static final CiRegister sp   = x2;
    public static final CiRegister gp   = x3;
    public static final CiRegister tp   = x4;

    public static final CiRegisterValue rsp = sp.asValue(CiKind.Int);

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
    public static final CiRegister s2  = x18;
    public static final CiRegister s3  = x19;
    public static final CiRegister s4  = x20;
    public static final CiRegister s5  = x21;
    public static final CiRegister s6  = x22;
    public static final CiRegister s7  = x23;
    public static final CiRegister s8  = x24;
    public static final CiRegister s9  = x25;
    public static final CiRegister s10 = x26;
    public static final CiRegister s11 = x27;

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

    public static final CiRegister LATCH_REGISTER = x26;

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

    public static final CiRegister[] csaRegisters = {
        x0, x1, x2, x3, x4, x5, x6, x7, x8, x9, x10, x11, x12, x13,
        x14, x15, x16, x17, x18, x19, x20, x21, x22, x23, x24,
        x25, x26, x27, x28, x29, x30, x31,
        f0, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13,
        f14, f15, f16, f17, f18, f19, f20, f21, f22, f23, f24,
        f25, f26, f27, f28, f29, f30, f31
    };

    @Override
    public boolean isRISCV64() {
        return true;
    }

    @Override
    public boolean twoOperandMode() {
        return true;
    }

    public RISCV64() {
        super("RISCV64",                        //architecture name
              8,                                //word size (8 bytes)
              ByteOrder.LittleEndian,           //endianness
              allRegisters,                     //available registers
              0, /*LOAD_STORE | STORE_STORE*/   //implicitMemoryBarriers (no implicit barriers)
              -1,                               //nativeCallDisplacementOffset (ignore)
              32,                               //registerReferenceMapBitCount
              16);                               //returnAddressSize (16 bytes for alignment)
    }

    /**
     * @param reg If null this method returns false.
     * @return true if register is a general purpose register, including the stack pointer and zero register.
     */
    public static boolean isIntReg(CiRegister reg) {
        return reg != null && reg.isCpu();
    }

    /**
     * @param reg If null this method returns false..
     * @return true if register is a floating-point register, false otherwise.
     */
    public static boolean isFpuReg(CiRegister reg) {
        return reg != null && reg.isFpu();
    }

    /**
     * @param reg the register that is checked. If null this method returns false.
     * @return true if register can be used as a general purpose register.
     * This means the register is neither null nor the zero/discard/stack pointer register.
     */
    public static boolean isGeneralPurposeReg(CiRegister reg) {
        return isIntReg(reg) && !reg.equals(zr) && !reg.equals(sp);
    }

}
