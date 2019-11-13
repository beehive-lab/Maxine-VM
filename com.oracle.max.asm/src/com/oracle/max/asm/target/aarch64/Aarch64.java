/*
 * Copyright (c) 2017-2019, APT Group, School of Computer Science,
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
package com.oracle.max.asm.target.aarch64;

import static com.oracle.max.cri.intrinsics.MemoryBarriers.*;
import static com.sun.cri.ci.CiRegister.RegisterFlag.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiArchitecture.*;
import com.sun.cri.ci.CiRegister.*;


public class Aarch64 extends CiArchitecture {

    // General purpose CPU registers
    // r0 - r7 -> arg0 - arg7
    public static final CiRegister r0 = gpCiRegister(0);
    public static final CiRegister r1 = gpCiRegister(1);
    public static final CiRegister r2 = gpCiRegister(2);
    public static final CiRegister r3 = gpCiRegister(3);
    public static final CiRegister r4 = gpCiRegister(4);
    public static final CiRegister r5 = gpCiRegister(5);
    public static final CiRegister r6 = gpCiRegister(6);
    public static final CiRegister r7 = gpCiRegister(7);
    public static final CiRegister r8 = gpCiRegister(8);
    public static final CiRegister r9 = gpCiRegister(9);
    public static final CiRegister r10 = gpCiRegister(10);
    public static final CiRegister r11 = gpCiRegister(11);
    public static final CiRegister r12 = gpCiRegister(12);
    public static final CiRegister r13 = gpCiRegister(13);
    public static final CiRegister r14 = gpCiRegister(14);
    public static final CiRegister r15 = gpCiRegister(15);
    public static final CiRegister r16 = gpCiRegister(16);
    public static final CiRegister r17 = gpCiRegister(17);
    public static final CiRegister r18 = gpCiRegister(18);
    public static final CiRegister r19 = gpCiRegister(19);
    public static final CiRegister r20 = gpCiRegister(20);
    public static final CiRegister r21 = gpCiRegister(21);
    public static final CiRegister r22 = gpCiRegister(22);
    public static final CiRegister r23 = gpCiRegister(23);
    public static final CiRegister r24 = gpCiRegister(24);
    public static final CiRegister r25 = gpCiRegister(25);
    public static final CiRegister r26 = gpCiRegister(26);
    public static final CiRegister r27 = gpCiRegister(27);
    public static final CiRegister r28 = gpCiRegister(28);
    public static final CiRegister r29 = gpCiRegister(29);
    public static final CiRegister r30 = gpCiRegister(30);

/********************************************************************************************************/
    // r31 is not a general purpose register, but represents either the stackpointer
    // or the zero/discard register depending on the instruction. So we represent
    // those two uses as two different registers.
    // The register numbers are kept in sync with register_aarch64.hpp and have to
    // be sequential, hence we also need a general r31 register here, which is never used.
    public static final CiRegister r31 = gpCiRegister(31);
    public static final CiRegister sp = new CiRegister(32, 31, 8, "SP", CPU);
    public static final CiRegisterValue rsp = sp.asValue(CiKind.Int);
    public static final CiRegister zr = new CiRegister(33, 31, 8, "ZR", CPU);

    // Names for special registers.
    public static final CiRegister linkRegister = r30;
    public static final CiRegister fp = r29;
    public static final CiRegister threadRegister = r28;
    public static final CiRegister heapBaseRegister = r27;
    // Register used for inline cache class.
    // see definition of IC_Klass in c1_LIRAssembler_aarch64.cpp
    public static final CiRegister inlineCacheRegister = r9;
    // Register used to store metaspace method.
    // see definition in sharedRuntime_aarch64.cpp:gen_c2i_adapter
    public static final CiRegister metaspaceMethodRegister = r12;
    // ATTENTION: must be callee-saved by all C ABIs in use.
    public static final CiRegister LATCH_REGISTER = r26;
/********************************************************************************************************/

    // Floating point and SIMD registers
    public static final CiRegister d0 = fpCiRegister(0);
    public static final CiRegister d1 = fpCiRegister(1);
    public static final CiRegister d2 = fpCiRegister(2);
    public static final CiRegister d3 = fpCiRegister(3);
    public static final CiRegister d4 = fpCiRegister(4);
    public static final CiRegister d5 = fpCiRegister(5);
    public static final CiRegister d6 = fpCiRegister(6);
    public static final CiRegister d7 = fpCiRegister(7);
    public static final CiRegister d8 = fpCiRegister(8);
    public static final CiRegister d9 = fpCiRegister(9);
    public static final CiRegister d10 = fpCiRegister(10);
    public static final CiRegister d11 = fpCiRegister(11);
    public static final CiRegister d12 = fpCiRegister(12);
    public static final CiRegister d13 = fpCiRegister(13);
    public static final CiRegister d14 = fpCiRegister(14);
    public static final CiRegister d15 = fpCiRegister(15);
    public static final CiRegister d16 = fpCiRegister(16);
    public static final CiRegister d17 = fpCiRegister(17);
    public static final CiRegister d18 = fpCiRegister(18);
    public static final CiRegister d19 = fpCiRegister(19);
    public static final CiRegister d20 = fpCiRegister(20);
    public static final CiRegister d21 = fpCiRegister(21);
    public static final CiRegister d22 = fpCiRegister(22);
    public static final CiRegister d23 = fpCiRegister(23);
    public static final CiRegister d24 = fpCiRegister(24);
    public static final CiRegister d25 = fpCiRegister(25);
    public static final CiRegister d26 = fpCiRegister(26);
    public static final CiRegister d27 = fpCiRegister(27);
    public static final CiRegister d28 = fpCiRegister(28);
    public static final CiRegister d29 = fpCiRegister(29);
    public static final CiRegister d30 = fpCiRegister(30);
    public static final CiRegister d31 = fpCiRegister(31);

    public static final CiRegister[] cpuRegisters = {
        r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13,
        r14, r15, r16, r17, r18, r19, r20, r21, r22, r23, r24,
        r25, r26, r27, r28, r29, r30, // Exclude special register 31 /* r31, sp, zr */
    };

    public static final CiRegister[] fpuRegisters = {
        d0, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13,
        d14, d15, d16, d17, d18, d19, d20, d21, d22, d23, d24,
        d25, d26, d27, d28, d29, d30, d31
    };

    public static final CiRegister[] csaRegisters = {
        r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13,
        r14, r15, r16, r17, r18, r19, r20, r21, r22, r23, r24,
        r25, r26, r27, r28, r29, r30, // Exclude special register 31 /* r31, sp, zr */
        d0, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13,
        d14, d15, d16, d17, d18, d19, d20, d21, d22, d23, d24,
        d25, d26, d27, d28, d29, d30, d31
    };

    public static final CiRegister[] allRegisters = {
        r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13,
        r14, r15, r16, r17, r18, r19, r20, r21, r22, r23, r24,
        r25, r26, r27, r28, r29, r30, r31, sp, zr,
        d0, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13,
        d14, d15, d16, d17, d18, d19, d20, d21, d22, d23, d24,
        d25, d26, d27, d28, d29, d30, d31
    };

    public static final CiRegister[] calleeSavedRegisters = {
        r19, r20, r21, r22, r23, r24, r25, r26, r27, r28
    };

    private static CiRegister gpCiRegister(int nr) {
        return new CiRegister(nr, nr, 8, "r" + nr, CPU, RegisterFlag.Byte);
    }

    private static CiRegister fpCiRegister(int nr) {
        // zr is last integer register.
        int firstFpRegNumber = zr.number + 1;
        return new CiRegister(firstFpRegNumber + nr, nr, 16, "v" + nr, FPU);
    }

    public Aarch64() {
        super("Aarch64",                        //architecture name
              8,                                //word size (8 bytes)
              ByteOrder.LittleEndian,           //endianness
              allRegisters,                     //available registers
              0, /*LOAD_STORE | STORE_STORE*/   //implicitMemoryBarriers (no implicit barriers)
              -1,                               //nativeCallDisplacementOffset (ingore)
              32,                               //registerReferenceMapBitCount
              16);                              //returnAddressSize (16 bytes)
        /* Although aarch64 addresses have a 64 bit precision, a callers return address effectively occupies a 16 byte
         * stack slot in an activation frame according to the AAPCS sp alignment. The returnAddressSize field is used
         * for stack navigation/alignment purposes. */
    }

    /**
     * @param reg If null this method return false.
     * @return true if register is the stackpointer, false otherwise.
     */
    public static boolean isSp(CiRegister reg) {
        assert !r31.equals(reg) : "r31 should not be used.";
        // Cannot use reg == sp since registers are not singletons for some strange reason.
        return reg != null && reg.name.equals("SP");
    }

    /**
     * @param reg If null this method returns false.
     * @return true if register is a general purpose register, including the stack pointer and zero register.
     */
    public static boolean isIntReg(CiRegister reg) {
        assert !r31.equals(reg) : "r31 should not be used.";
        //return reg != null && reg.getRegisterCategory().equals(CPU);
        return reg != null && reg.isCpu();

    }

    /**
     * @param reg If null this method returns false..
     * @return true if register is a floating-point register, false otherwise.
     */
    public static boolean isFpuReg(CiRegister reg) {
        assert !r31.equals(reg) : "r31 should not be used.";
        //return reg != null && reg.getRegisterCategory().equals(FPU);
        return reg != null && reg.isFpu();
    }

    /**
     * @param reg the register that is checked. If null this method returns false.
     * @return true if register can be used as a general purpose register.
     * This means the register is neither null nor the zero/discard/stack pointer register.
     */
    public static boolean isGeneralPurposeReg(CiRegister reg) {
        assert !r31.equals(reg) : "r31 should not be used.";
        return isIntReg(reg) && !reg.equals(zr) && !reg.equals(sp);
    }

    /**
     * @param reg the register that is checked. If null this method returns false.
     * @return true if register is a general purpose register or the zero register.
     */
    public static boolean isGeneralPurposeOrZeroReg(CiRegister reg) {
        assert !r31.equals(reg) : "r31 should not be used.";
        return isIntReg(reg) && !reg.equals(sp);
    }

    /**
     * @param reg the register that is checked. If null this method returns false.
     * @return true if register is a general purpose register or the stack pointer.
     */
    public static boolean isGeneralPurposeOrSpReg(CiRegister reg) {
        assert !r31.equals(reg) : "r31 should not be used.";
        return isIntReg(reg) && !reg.equals(zr);
    }

    @Override
    public boolean isAarch64() {
        return true;
    }

    @Override
    public boolean twoOperandMode() {
        return true;
    }

    @Override
    public boolean usesTrampolines() {
        return true;
    }
}
