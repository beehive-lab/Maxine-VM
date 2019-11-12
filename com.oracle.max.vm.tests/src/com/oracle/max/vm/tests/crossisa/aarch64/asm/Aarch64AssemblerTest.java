/*
 * Copyright (c) 2017-2018, APT Group, School of Computer Science,
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
package com.oracle.max.vm.tests.crossisa.aarch64.asm;

import com.oracle.max.asm.target.aarch64.*;
import com.oracle.max.vm.tests.crossisa.*;
import com.sun.cri.ci.*;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.oracle.max.asm.target.aarch64.Aarch64.*;

public class Aarch64AssemblerTest {

    private static final int VARIANT_32 = 32;
    private static final int VARIANT_64 = 64;

    private Aarch64MacroAssembler masm;
    private MaxineAarch64Tester tester = new MaxineAarch64Tester();

    public Aarch64AssemblerTest() {
        CiTarget aarch64 = new CiTarget(new Aarch64(), true, 8, 16, 4096, 0, false, false, false, true);
        masm = new Aarch64MacroAssembler(aarch64, null);
    }

    private static final long[] scratchTestSet = {0, 1, 0xff, 0xffff, 0xffffff, 0xfffffff, 0x7fffffff, 0xffffffffffffL, 0x7fffffffffffffffL};

    // The following values will be updated
    // to those expected to be found in a register after simulated execution of code.
    private static final long[] expectedValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

    @Before
    public void initialiseTest() {
        for (int i = 0; i < MaxineAarch64Tester.NUM_REGS; i++) {
            expectedValues[i] = i;
        }
        tester.resetTestValues();
        masm.codeBuffer.reset();
    }

    @After
    public void generateAndTest() throws Exception {
        Aarch64CodeWriter code = new Aarch64CodeWriter(masm.codeBuffer);
        code.createCodeFile();
        if (!CrossISATester.ENABLE_SIMULATOR) {
            System.out.println("Code Generation is disabled!");
            System.exit(1);
        }
        tester.compile();
        tester.runSimulation();
        tester.cleanProcesses();
        tester.cleanFiles();
        Assert.assertTrue(tester.validateLongRegisters());
    }

    @Test
    public void b() throws Exception {
        masm.mov64BitConstant(r0, -1);
        masm.b(24);
        masm.nop(4);                                                          // +16 bytes
        masm.movz(VARIANT_64, r0, 0x123, 0);    // +20 bytes to here (skipped)
        masm.movz(VARIANT_64, r1, 0x123, 0);    // +24 bytes to here (landing)
        tester.setExpectedValue(r0, -1);
        tester.setExpectedValue(r1, 0x123);
    }

    @Test
    public void zero() throws Exception {
        masm.mov(r0, 0xFF);
        masm.mov(64, r0, zr);
        tester.setExpectedValue(r0, 0);
    }

    @Test
    public void adr_adrp() throws Exception {
        masm.adr(r0, 0); // r0 holds the PC
        masm.adrp(r1, 0);
        masm.adrp(r2, 4096);
        masm.adrp(r3, 8192);

        masm.sub(64, r1, r1, r0);
        masm.sub(64, r2, r2, r0);
        masm.sub(64, r3, r3, r0);

        tester.setExpectedValue(r1, 0);
        tester.setExpectedValue(r2, 4096 * 4096);
        tester.setExpectedValue(r3, 8192 * 4096);
    }

    @Test
    public void mov64() throws Exception {
        masm.mov64BitConstant(r0, 10);
        masm.mov64BitConstant(r1, -10);
        masm.mov64BitConstant(r2, -12345678987654321L);
        masm.mov64BitConstant(r3, Long.MAX_VALUE);
        masm.mov64BitConstant(r4, Long.MIN_VALUE);

        tester.setExpectedValue(r0, 10);
        tester.setExpectedValue(r1, -10);
        tester.setExpectedValue(r2, -12345678987654321L);
        tester.setExpectedValue(r3, Long.MAX_VALUE);
        tester.setExpectedValue(r4, Long.MIN_VALUE);
    }

    @Test
    public void movtest() throws Exception {
        masm.mov(r0, 10);
        masm.mov(r1, -10);

        tester.setExpectedValue(r0, 10);
        tester.setExpectedValue(r1, -10);
    }

    @Test
    public void mov2() throws Exception {
        masm.mov(r0, Integer.MAX_VALUE);
        masm.mov(r1, Integer.MIN_VALUE);
        masm.mov(r2, Long.MAX_VALUE);
        masm.mov(r3, Long.MIN_VALUE);
        masm.mov(r4, 0x1234);
        masm.mov(r5, 0xFFFF_FFFF_FFFF_FFFFL); // Both writes on r5 is intentional
        masm.mov(r5, 0xFFFF_0000_1234_0000L); // Test forceMov optimization
        masm.mov(r6, 0xFFFF_FFFF_FFFF_FFFFL); // Test 32bit sign-extend optimization
        masm.mov(r7, 0); // Test zero optimization
        masm.mov(r8, 0xcafebabe);

        tester.setExpectedValue(r0, Integer.MAX_VALUE);
        tester.setExpectedValue(r1, Integer.MIN_VALUE);
        tester.setExpectedValue(r2, Long.MAX_VALUE);
        tester.setExpectedValue(r3, Long.MIN_VALUE);
        tester.setExpectedValue(r4, 0x1234);
        tester.setExpectedValue(r5, 0xFFFF_0000_1234_0000L);
        tester.setExpectedValue(r6, 0xFFFF_FFFF_FFFF_FFFFL);
        tester.setExpectedValue(r7, 0);
        tester.setExpectedValue(r8, 0xcafebabe);
    }

    @Test
    public void forceMov() throws Exception {
        masm.mov(r0, 0xFFFFFFFF_FFFFFFFFL);
        masm.forceMov(r0, 0xFFFF_0000_FFFF_0000L, true);
        masm.mov(r1, 0xFFFFFFFF_FFFFFFFFL);
        masm.forceMov(r1, 0xFFFF_0000_0000_0000L, true);
        masm.mov(r2, 0xFFFFFFFF_FFFFFFFFL);
        masm.forceMov(r2, 0x0000_1010_0000_0000L, true);
        masm.mov(r3, 0xFFFFFFFF_FFFFFFFFL);
        masm.forceMov(r3, 0xFFFF_0000_FFFF_0000L, false);
        masm.mov(r4, 0xFFFFFFFF_FFFFFFFFL);
        masm.forceMov(r4, 0xFFFF_0000_0000_0000L, false);
        masm.mov(r5, 0xFFFFFFFF_FFFFFFFFL);
        masm.forceMov(r5, 0x0000_1010_0000_0000L, false);

        tester.setExpectedValue(r0, 0xFFFF_0000_FFFF_0000L);
        tester.setExpectedValue(r1, 0xFFFF_0000_0000_0000L);
        tester.setExpectedValue(r2, 0x0000_1010_0000_0000L);
        tester.setExpectedValue(r3, 0xFFFF_0000_FFFF_0000L);
        tester.setExpectedValue(r4, 0xFFFF_0000_0000_0000L);
        tester.setExpectedValue(r5, 0x0000_1010_0000_0000L);
    }

    @Test
    public void add_register() throws Exception {
        masm.mov64BitConstant(r0, 10);
        masm.mov64BitConstant(r1, -1);
        masm.mov64BitConstant(r2, 1);
        masm.mov64BitConstant(r3, Long.MAX_VALUE);
        masm.mov64BitConstant(r4, Long.MIN_VALUE);

        masm.add(64, r0, r0, r1);
        masm.add(64, r3, r3, r2);
        masm.add(64, r4, r4, r1);
        masm.add(64, r5, r4, r2);
        masm.add(64, r6, r3, r1);

        tester.setExpectedValue(r0, 9);
        tester.setExpectedValue(r3, Long.MIN_VALUE);
        tester.setExpectedValue(r4, Long.MAX_VALUE);
        tester.setExpectedValue(r5, Long.MIN_VALUE);
        tester.setExpectedValue(r6, Long.MAX_VALUE);
    }

    /**
     * branch.
     */
    @Test
    public void bcond() throws Exception {
        masm.mov32BitConstant(r0, 1);
        masm.mov32BitConstant(r1, 1);

        masm.cmp(32, r0, r1);
        masm.b(Aarch64Assembler.ConditionFlag.EQ, 4);
        masm.mov32BitConstant(Aarch64.r3, 66);
        masm.mov32BitConstant(Aarch64.r3, 77);

        tester.setExpectedValue(r3, 77);
    }

    /**
     * load and store instructions.
     */
    @Test
    public void ldr_str() throws Exception {
        masm.movz(VARIANT_64, r0, 0x123, 0);        // value to be stored to stack
        Aarch64Address address = Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, -8); // stack address
        masm.str(VARIANT_64, r0, address);         // store value to stack
        masm.ldr(VARIANT_64, r1, address);          // load value from stack
        tester.setExpectedValue(r0, 0x123);
        tester.setExpectedValue(r1, 0x123);
    }

    @Test
    public void add_imm() throws Exception {
        // ADD Xi, Xi, valueof(Xi) ; Xi's value should double
        for (int i = 0; i < 10; i++) {
            // expected values must not be larger than an integer in order to be converted to an int correctly
            assert expectedValues[i] < Integer.MAX_VALUE;
            masm.movz(VARIANT_64, Aarch64.cpuRegisters[i], (int) expectedValues[i], 0);
            masm.add(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], 2L * expectedValues[i]);
        }
    }

    @Test
    public void sub_imm() throws Exception {
        // SUB Xi, Xi, valueof(Xi) ; Xi should then be 0
        for (int i = 0; i < 10; i++) {
            // expected values must not be larger than an integer in order to be converted to an int correctly
            assert expectedValues[i] < Integer.MAX_VALUE;
            masm.movz(VARIANT_64, Aarch64.cpuRegisters[i], (int) expectedValues[i], 0);
            masm.sub(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], 0);
        }
    }

    @Test
    public void and_imm() throws Exception {
        // AND Xi, Xi, 0x1
        for (int i = 0; i < 10; i++) {
            masm.movz(VARIANT_64, Aarch64.cpuRegisters[i], (int) expectedValues[i], 0);
            masm.and(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 0x1);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], expectedValues[i] & 0x1);
        }
    }

    @Test
    public void eor_imm() throws Exception {
        // EOR Xi, Xi, 0x1
        for (int i = 0; i < 10; i++) {
            masm.movz(VARIANT_64, Aarch64.cpuRegisters[i], (int) expectedValues[i], 0);
            masm.eor(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 0x1);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], expectedValues[i] ^ 0x1);
        }
    }

    @Test
    public void orr_imm() throws Exception {
        // ORR Xi, Xi, 0x1
        for (int i = 0; i < 10; i++) {
            masm.movz(VARIANT_64, Aarch64.cpuRegisters[i], (int) expectedValues[i], 0);
            masm.orr(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 0x1);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], expectedValues[i] | 0x1);
        }
    }

    @Test
    public void movz_imm() throws Exception {
        // MOVZ Xi, 0x1, LSL #0x10
        for (int i = 0; i < 1; i++) {
            masm.movz(VARIANT_64, Aarch64.cpuRegisters[i], 0x1, 32);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], (long) 1 << 32);
        }
    }

    @Test
    public void movn_imm() throws Exception {
        // MOVZ Xi, 0x1, LSL #0x10
        for (int i = 0; i < 1; i++) {
            masm.movn(VARIANT_64, Aarch64.cpuRegisters[i], 0x1, 32);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], ~((long) 1 << 32));
        }
    }

    @Test
    public void bfm() throws Exception {
        masm.movn(VARIANT_64, r10, 0x0, 0);
        masm.movz(VARIANT_64, r0, 0x0, 0);

        masm.bfm(VARIANT_64, r0, r10, 3, 5);
        tester.setExpectedValue(r0, 0b111111L >>> 3);
    }

    @Test
    public void ubfm() throws Exception {
        masm.movn(VARIANT_64, r10, 0x0, 0);
        masm.movz(VARIANT_64, r0, 0x0, 0);

        masm.ubfm(VARIANT_64, r0, r10, 3, 5);
        tester.setExpectedValue(r0, 0b111111L >>> 3);
    }

    @Test
    public void sbfm() throws Exception {
        int mask;
        masm.movz(VARIANT_64, r10, 0b11110000, 0);
        masm.movz(VARIANT_64, r0, 0, 0);
        masm.movz(VARIANT_64, r1, 0, 0);

        // Signed
        masm.sbfm(VARIANT_64, r0, r10, 3, 5);
        mask = 1 << (5 - 1);
        tester.setExpectedValue(r0, ((0b11110000 >> 3) ^ mask) - mask);
        // Unsigned
        masm.sbfm(VARIANT_64, r1, r10, 4, 8);
        mask = 1 << (8 - 1);
        tester.setExpectedValue(r1, ((0b11110000 >> 4) ^ mask) - mask);
    }

    /******* Arithmetic (shifted register) (5.5.1). *******/

    @Test
    public void add_shift_reg() throws Exception {
        masm.movz(VARIANT_64, r10, 1, 0);
        for (int i = 0; i < 1; i++) {
            masm.add(VARIANT_64, Aarch64.cpuRegisters[i], r10, r10, Aarch64Assembler.ShiftType.LSL, 2);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], 5);
        }
    }

    @Test
    public void sub_shift_reg() throws Exception {
        masm.movz(VARIANT_64, r10, 0b1000, 0);
        for (int i = 0; i < 1; i++) {
            masm.sub(VARIANT_64, Aarch64.cpuRegisters[i], r10, r10, Aarch64Assembler.ShiftType.LSR, 3);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], 0b1000 - (0b1000 >>> 3));
        }
    }

    /******* Arithmetic (extended register) (5.5.2). *******/

    @Test
    public void add_ext_reg() throws Exception {
        masm.movz(VARIANT_64, r0, 1, 0);
        masm.movn(VARIANT_64, r10, 0x0, 0);

        for (int i = 0; i < 1; i++) {
            masm.add(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], r10, Aarch64Assembler.ExtendType.UXTB, 3);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], 1 + 0b11111111000L);
        }
    }

    @Test
    public void sub_ext_reg() throws Exception {
        masm.movz(VARIANT_64, r0, 0b11111111000, 0);
        masm.movn(VARIANT_64, r10, 0x0, 0);

        for (int i = 0; i < 1; i++) {
            masm.sub(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], r10, Aarch64Assembler.ExtendType.UXTB, 3);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], 0);
        }
    }

    /******* Logical (shifted register) (5.5.3). *******/

    @Test
    public void and_shift_reg() throws Exception {
        masm.movn(VARIANT_64, r0, 0x0, 0);
        masm.movz(VARIANT_64, r10, 0xf, 0);

        for (int i = 0; i < 1; i++) {
            masm.and(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], r10, Aarch64Assembler.ShiftType.LSL, 4);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], 0b1111L << 4);
        }
    }

    /* Variable Shift (5.5.4) */
    @Test
    public void asr() throws Exception {
        for (int i = 0; i < 10; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], 0b1010101111L);
            masm.mov32BitConstant(r10, i);
            masm.asr(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], r10);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], 0b1010101111L >> i);
        }
    }

    /* Bit Operations (5.5.5) */
    @Test
    public void cls() throws Exception {
        masm.movn(VARIANT_64, r30, 0x0, 0);
        masm.movz(VARIANT_64, r10, 0x0, 0);

        masm.cls(VARIANT_64, r30, r10);
        tester.setExpectedValue(r30, 63);
        tester.setExpectedValue(r10, 0);
    }

    /* Integer Multiply/Divide (5.6). */

    /* Floating-point Move (register) (5.7.2) */

    @Test
    public void float0() throws Exception {
        masm.movz(VARIANT_64, r0, 10, 0);
        masm.movz(VARIANT_64, r1, 11, 0);

        masm.fmovCpu2Fpu(VARIANT_64, d0, r0);
        masm.fmovCpu2Fpu(VARIANT_64, d1, r1);
        masm.fmovFpu2Cpu(VARIANT_64, r2, d0);
        masm.fmovFpu2Cpu(VARIANT_64, r3, d1);

        tester.setExpectedValue(d0, 10D);
        tester.setExpectedValue(d1, 11D);
        tester.setExpectedValue(r2, 10);
        tester.setExpectedValue(r3, 11);
    }

    /**
     * test: fadd, fsub, fmul, fdiv, scvtf, fcvtzs.
     */
    @Test
    public void float1() throws Exception {
        masm.movz(VARIANT_64, r0, 10, 0);
        masm.movz(VARIANT_64, r1, 110, 0);

        masm.scvtf(VARIANT_64, VARIANT_64, d0, r0);
        masm.scvtf(VARIANT_64, VARIANT_64, d1, r1);

        masm.fadd(VARIANT_64, d2, d0, d1);
        masm.fsub(VARIANT_64, d3, d1, d0);
        masm.fmul(VARIANT_64, d4, d1, d0);
        masm.fdiv(VARIANT_64, d5, d1, d0);

        masm.fcvtzs(VARIANT_64, VARIANT_64, r2, d2);
        masm.fcvtzs(VARIANT_64, VARIANT_64, r3, d3);
        masm.fcvtzs(VARIANT_64, VARIANT_64, r4, d4);
        masm.fcvtzs(VARIANT_64, VARIANT_64, r5, d5);

        tester.setExpectedValue(r0, 10);
        tester.setExpectedValue(r1, 110);
        tester.setExpectedValue(r2, 120);
        tester.setExpectedValue(r3, 100);
        tester.setExpectedValue(r4, 10 * 110);
        tester.setExpectedValue(r5, 110 / 10);

        tester.setExpectedValue(d0, 10D);
        tester.setExpectedValue(d1, 110D);
        tester.setExpectedValue(d2, 120D);
        tester.setExpectedValue(d3, 100D);
        tester.setExpectedValue(d4, 10D * 110D);
        tester.setExpectedValue(d5, 110D / 10D);
    }

    /**
     * frintz, fabs, fneg, fsqrt.
     */
    @Test
    public void float2() throws Exception {
        masm.movz(VARIANT_64, r0, 100, 0);
        masm.movz(VARIANT_64, r1, 110, 0);

        masm.scvtf(VARIANT_64, VARIANT_64, d0, r0);
        masm.scvtf(VARIANT_64, VARIANT_64, d1, r1);

        masm.fdiv(VARIANT_64, d2, d1, d0);
        masm.frintz(VARIANT_64, d2, d2);
        masm.fcvtzs(VARIANT_64, VARIANT_64, r2, d2);

        masm.fsub(VARIANT_64, d3, d0, d1);
        masm.fabs(VARIANT_64, d3, d3);
        masm.fcvtzs(VARIANT_64, VARIANT_64, r3, d3);

        masm.fsub(VARIANT_64, d4, d0, d1);
        masm.fneg(VARIANT_64, d4, d4);
        masm.fcvtzs(VARIANT_64, VARIANT_64, r4, d4);

        masm.fsqrt(VARIANT_64, d5, d0);
        masm.fcvtzs(VARIANT_64, VARIANT_64, r5, d5);

        tester.setExpectedValue(r0, 100);
        tester.setExpectedValue(r1, 110);
        tester.setExpectedValue(r2, 1);
        tester.setExpectedValue(r3, 10);
        tester.setExpectedValue(r4, 10);
        tester.setExpectedValue(r5, 10);

        tester.setExpectedValue(d0, 100D);
        tester.setExpectedValue(d1, 110D);
        tester.setExpectedValue(d2, 1D);
        tester.setExpectedValue(d3, 10D);
        tester.setExpectedValue(d4, 10D);
        tester.setExpectedValue(d5, 10D);
    }

    /**
     * fmadd, fmsub.
     */
    @Test
    public void float3() throws Exception {
        masm.movz(VARIANT_64, r0, 100, 0);
        masm.movz(VARIANT_64, r1, 110, 0);

        masm.scvtf(VARIANT_64, VARIANT_64, d0, r0);
        masm.scvtf(VARIANT_64, VARIANT_64, d1, r1);

        masm.fmadd(VARIANT_64, d3, d0, d1, d1);
        masm.fmsub(VARIANT_64, d4, d0, d1, d1);

        masm.fcvtzs(VARIANT_64, VARIANT_64, r3, d3);
        masm.fcvtzs(VARIANT_64, VARIANT_64, r4, d4);

        tester.setExpectedValue(r0, 100);
        tester.setExpectedValue(r1, 110);

        tester.setExpectedValue(r3, 110 + 100 * 110);
        tester.setExpectedValue(r4, 110 - 100 * 110);
    }

    /**
     * fstr, fldr.
     */
    @Test
    public void float4() throws Exception {
        masm.movz(VARIANT_64, r0, 0x123, 0);        // value to be stored to stack
        masm.scvtf(VARIANT_64, VARIANT_64, d0, r0);

        Aarch64Address address = Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, -8); // stack address

        masm.fstr(VARIANT_64, d0, address);         // store value to stack
        masm.fldr(VARIANT_64, d1, address);          // load value from stack

        masm.fcvtzs(VARIANT_64, VARIANT_64, r1, d1);

        tester.setExpectedValue(r0, 0x123);
        tester.setExpectedValue(r1, 0x123);
    }

    /**
     * mrs.
     */
    @Test
    public void mrs() throws Exception {
        masm.movz(VARIANT_64, r10, 1, 0);
        masm.movz(VARIANT_64, r11, 1, 0);
        masm.movz(VARIANT_64, r12, 2, 0);
        masm.movz(VARIANT_64, r13, 3, 0);

        masm.movz(VARIANT_64, r0, 0, 0);
        masm.movz(VARIANT_64, r1, 0, 0);
        masm.movz(VARIANT_64, r2, 0, 0);

        masm.mrs(r0, Aarch64Assembler.SystemRegister.NZCV);

        masm.subs(VARIANT_64, r15, r10, r11, Aarch64Assembler.ShiftType.LSL, 0);
        masm.mrs(r1, Aarch64Assembler.SystemRegister.NZCV);

        masm.subs(VARIANT_64, r15, r13, r12, Aarch64Assembler.ShiftType.LSL, 0);
        masm.mrs(r2, Aarch64Assembler.SystemRegister.NZCV);

        masm.subs(VARIANT_64, r15, r12, r13, Aarch64Assembler.ShiftType.LSL, 0);
        masm.mrs(r3, Aarch64Assembler.SystemRegister.NZCV);

        tester.setExpectedValue(r0, 0b0110L << 28);
        tester.setExpectedValue(r1, 0b0110L << 28);
        tester.setExpectedValue(r2, 0b0010L << 28);
        tester.setExpectedValue(r3, 0b1000L << 28);
    }

    /**
     * mrs_reg.
     */
    @Test
    public void mrs_reg() throws Exception {
        masm.movz(VARIANT_64, r1, 0b1111, 0);
        masm.msr(Aarch64Assembler.SystemRegister.SPSR_EL1, r1);
        masm.mrs(r0, Aarch64Assembler.SystemRegister.SPSR_EL1);

        tester.setExpectedValue(r0, 0b1111);
    }

    /**
     * mrs_imm.
     */
    @Test
    public void mrs_imm() throws Exception {
        masm.msr(Aarch64Assembler.PStateField.PSTATEField_DAIFClr, 0b1111);
        masm.msr(Aarch64Assembler.PStateField.PSTATEField_DAIFSet, 0b1001);
        masm.mrs(r0, Aarch64Assembler.SystemRegister.DAIF);

        // if dst == PSTATEField_SP, then the first 3 digits of the operand is ignored, which means only the last bit is used to set SPSel.
        masm.msr(Aarch64Assembler.PStateField.PSTATEField_SP, 0b0001);
        masm.mrs(r1, Aarch64Assembler.SystemRegister.SPSel);
        masm.msr(Aarch64Assembler.PStateField.PSTATEField_SP, 0b0000);
        masm.mrs(r2, Aarch64Assembler.SystemRegister.SPSel);

        tester.setExpectedValue(r0, 0b1001 << 6);
        tester.setExpectedValue(r1, 0b1);
        tester.setExpectedValue(r2, 0);
    }

//    public void todo_Ldrb() throws Exception {
//        int[] testval = { 0x03020100, 0xffedcba9};
//        int mask = 0xff;
////        setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
//        // load r0 and r1 with sensible values for ignoring the loading of bytes.
//        masm.mov32BitConstant(r0], testval[0);
//        masm.mov32BitConstant(r1], testval[1);
//        masm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2); // values now lie on the stack
//        for (int i = 0; i < 8; i++) {
//            // stackpointer advanced by 8
//            masm.ldrb(Aarch64Assembler.ConditionFlag.Always, 1, 1, 0, Aarch64.cpuRegisters[i], r13, i);
//            testValues[i] = true;
//            if (i < 4) {
//                expectedValues[i] = testval[0] & (mask << (8 * (i % 4)));
//            } else {
//                expectedValues[i] = testval[1] & (mask << (8 * (i % 4)));
//            }
//
//            expectedValues[i] = expectedValues[i] >> 8 * (i % 4);
//            if (expectedValues[i] < 0) {
//                expectedValues[i] = 0x100 + expectedValues[i];
//            }
//            // Bytes do not have a sign! So we need to make sure the expectedValues are
//            // not affected by sign extension side effects when we take the MSByte of
//            // an integer.
//       }
//
//       generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
//      }
//

    private static long getUnsignedInt(int x) {
        return x & 0x00000000ffffffffL;
    }

    @Test
    public void mov32BitConstant() throws Exception {
        int[] values = new int[10];
        values[0] = 0;
        values[1] = -1;
        values[2] = Integer.MIN_VALUE;
        values[3] = Integer.MAX_VALUE;
        values[4] = Integer.MIN_VALUE + 5;
        values[5] = Integer.MAX_VALUE - 5;

        for (int i = 0; i < values.length; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], values[i]);

            tester.setExpectedValue(Aarch64.cpuRegisters[i], getUnsignedInt(values[i]));
        }
    }

    @Test
    public void mov64BitConstant() throws Exception {
        long[] values = new long[10];
        values[0] = 0L;
        values[1] = -1L;
        values[2] = (long) Integer.MIN_VALUE;
        values[3] = (long) Integer.MAX_VALUE;
        values[4] = Long.MAX_VALUE;
        values[5] = Long.MIN_VALUE;
        values[6] = Long.MIN_VALUE + 5;
        values[7] = Long.MAX_VALUE - 5;
        values[8] = ((long) Integer.MIN_VALUE) + 5L;
        values[9] = ((long) Integer.MAX_VALUE) - 5L;

        for (long value : values) {
            masm.mov64BitConstant(r0, value);

            tester.setExpectedValue(r0, value);

        }
    }

    @Test
    public void addConstant() throws Exception {
        for (long aScratchTestSet : scratchTestSet) {
            for (int srcReg = 0; srcReg < 3; srcReg++) {
                for (int destReg = 0; destReg < 3; destReg++) {
                    masm.mov64BitConstant(Aarch64.cpuRegisters[srcReg], aScratchTestSet);
                    masm.add(64, Aarch64.cpuRegisters[destReg], Aarch64.cpuRegisters[srcReg], 5);
                    tester.setExpectedValue(Aarch64.cpuRegisters[destReg], aScratchTestSet + 5);
                }
            }
        }
    }

    @Test
    public void vPushPop() throws Exception {
        for (int i = 0; i < 5; i++) {
            masm.mov64BitConstant(Aarch64.cpuRegisters[i], i); // r1 = 1
            masm.fmov(64, Aarch64.fpuRegisters[i], Aarch64.cpuRegisters[i]); // d1 = r1 = 1
            masm.mov64BitConstant(Aarch64.cpuRegisters[i], -i); // r1 = -1
        }
        masm.fpush(2 | 8); // save d1
        for (int i = 0; i < 5; i++) {
            masm.fmov(64, Aarch64.fpuRegisters[i], Aarch64.cpuRegisters[i]); // d1 = r1 = -1
        }
        masm.fpop(2 | 8); // pop d1 as 1
        for (int i = 0; i < 5; i++) {
            masm.fmov(64, Aarch64.cpuRegisters[i], Aarch64.fpuRegisters[i]); // see if d1 is 1 or -1
        }
        tester.setExpectedValue(r0, 0);
        tester.setExpectedValue(r1, 1);
        tester.setExpectedValue(r2, -2);
        tester.setExpectedValue(r3, 3);
        tester.setExpectedValue(r4, -4);
    }

    @Test
    public void pushPop() throws Exception {
        for (int i = 0; i < 5; i++) {
            masm.mov64BitConstant(Aarch64.cpuRegisters[i], i);
        }
        masm.push(1 | 2 | 4 | 8 | 16);

        for (int i = 0; i < 5; i++) {
            masm.mov64BitConstant(Aarch64.cpuRegisters[i], -i);
        }

        masm.pop(1 | 2 | 4 | 8 | 16);

        tester.setExpectedValue(r0, 0);
        tester.setExpectedValue(r1, 1);
        tester.setExpectedValue(r2, 2);
        tester.setExpectedValue(r3, 3);
        tester.setExpectedValue(r4, 4);
    }

    @Test
    public void vdiv() throws Exception {
        masm.mov64BitConstant(r0, Double.doubleToRawLongBits(10));
        masm.mov64BitConstant(r1, Double.doubleToRawLongBits(24));
        masm.fmov(64, Aarch64.d0, r0);
        masm.fmov(64, Aarch64.d1, r1);
        masm.fdiv(64, Aarch64.d2, Aarch64.d1, Aarch64.d0);
        masm.fcvtzs(64, 64, r2, Aarch64.d2);
        tester.setExpectedValue(r2, 2);
    }

    @Test
    public void vcvt_int2float() throws Exception {
        masm.mov32BitConstant(r0, 10);
        masm.mov32BitConstant(r1, 24);
        masm.scvtf(32, 32, Aarch64.d0, r0);
        masm.scvtf(32, 32, Aarch64.d1, r1);
        masm.fmov(32, r0, Aarch64.d0);
        masm.fmov(32, r1, Aarch64.d1);
        tester.setExpectedValue(r0, Float.floatToRawIntBits(10));
        tester.setExpectedValue(r1, Float.floatToRawIntBits(24));
    }

    @Test
    public void vcvt_int2double() throws Exception {
        masm.mov64BitConstant(r0, 10);
        masm.mov64BitConstant(r1, 24);
        masm.scvtf(64, 64, Aarch64.d0, r0);
        masm.scvtf(64, 64, Aarch64.d1, r1);
        masm.fmov(64, r0, Aarch64.d0);
        masm.fmov(64, r1, Aarch64.d1);
        tester.setExpectedValue(r0, Double.doubleToRawLongBits(10));
        tester.setExpectedValue(r1, Double.doubleToRawLongBits(24));
    }

    @Test
    public void vcvt_double2float() throws Exception {
        masm.mov64BitConstant(r0, Double.doubleToRawLongBits(-10));
        masm.mov64BitConstant(r2, Double.doubleToRawLongBits(-24));
        masm.fmov(64, Aarch64.d0, r0);
        masm.fmov(64, Aarch64.d1, r2);
        masm.fcvt(64, Aarch64.d4, Aarch64.d0);
        masm.fcvt(64, Aarch64.d5, Aarch64.d1);
        masm.fmov(32, r0, Aarch64.d4);
        masm.fmov(32, r2, Aarch64.d5);
        tester.setExpectedValue(r0, Float.floatToRawIntBits(-10) & 0xffffffffL);
        tester.setExpectedValue(r2, Float.floatToRawIntBits(-24) & 0xffffffffL);
    }

    @Test
    public void vAdd() throws Exception {
        masm.mov64BitConstant(r0, 12);
        masm.mov64BitConstant(r1, 10);
        masm.scvtf(64, 64, Aarch64.d0, r0);
        masm.scvtf(64, 64, Aarch64.d1, r1);
        masm.fadd(64, Aarch64.d2, Aarch64.d0, Aarch64.d1);
        masm.fmov(64, r2, Aarch64.d2);
        tester.setExpectedValue(r0, 12);
        tester.setExpectedValue(r1, 10);
        tester.setExpectedValue(r2, Double.doubleToRawLongBits(22));
    }

    @Test
    public void vSub() throws Exception {
        masm.mov64BitConstant(r0, 12);
        masm.mov64BitConstant(r1, 10);
        masm.scvtf(64, 64, Aarch64.d0, r0);
        masm.scvtf(64, 64, Aarch64.d1, r1);
        masm.fsub(64, Aarch64.d2, Aarch64.d1, Aarch64.d0);
        masm.fmov(64, r2, Aarch64.d2);
        tester.setExpectedValue(r0, 12);
        tester.setExpectedValue(r1, 10);
        tester.setExpectedValue(r2, Double.doubleToRawLongBits(-2));
    }

    @Test
    public void fcvtMul() throws Exception {
        masm.mov64BitConstant(r0, 12);
        masm.mov64BitConstant(r1, 10);
        masm.scvtf(64, 64, Aarch64.d0, r0);
        masm.scvtf(64, 64, Aarch64.d1, r1);
        masm.fmul(64, Aarch64.d2, Aarch64.d1, Aarch64.d0);
        masm.fcvtzs(64, 64, r2, Aarch64.d2);
        tester.setExpectedValue(r0, 12);
        tester.setExpectedValue(r1, 10);
        tester.setExpectedValue(r2, 120);
    }

    @Test
    public void fldrStr() throws Exception {
        masm.mov64BitConstant(r0, 12);
        masm.mov64BitConstant(r1, 10);
        masm.push(1 | 2);
        masm.fldr(64, Aarch64.d0, Aarch64Address.createBaseRegisterOnlyAddress(Aarch64.sp));
        masm.fmov(64, r2, Aarch64.d0);
        masm.fldr(64, Aarch64.d4, Aarch64Address.createBaseRegisterOnlyAddress(Aarch64.sp));
        masm.fmov(64, Aarch64.r4, Aarch64.d4);
        masm.fstr(64, Aarch64.d4, Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, -16));
        masm.fstr(64, Aarch64.d0, Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, -32));
        masm.fldr(64, Aarch64.d10, Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, -16));
        masm.fldr(64, Aarch64.d31, Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, -32));
        masm.fmov(64, Aarch64.r6, Aarch64.d10);
        masm.fmov(64, Aarch64.r8, Aarch64.d31);
        tester.setExpectedValue(r0, 12);
        tester.setExpectedValue(r1, 10);
        tester.setExpectedValue(r2, 10);
        tester.setExpectedValue(r4, 10);
        tester.setExpectedValue(r6, 10);
        tester.setExpectedValue(r8, 10);
    }

    @Test
    public void fldr() throws Exception {
        masm.mov64BitConstant(r0, 12);
        masm.mov64BitConstant(r1, 10);
        masm.push(1 | 2);
        Aarch64Address address = Aarch64Address.createBaseRegisterOnlyAddress(Aarch64.sp);
        masm.fldr(64, Aarch64.d31, address);
        masm.fmov(64, r2, Aarch64.d31);
        masm.fldr(64, Aarch64.d4, address);
        masm.fmov(64, Aarch64.r4, Aarch64.d4);
        tester.setExpectedValue(r0, 12);
        tester.setExpectedValue(r1, 10);
        tester.setExpectedValue(r2, 10);
        tester.setExpectedValue(r4, 10);
    }

    @Test
    public void mVov() throws Exception {
        masm.mov64BitConstant(r0, 12);
        masm.mov64BitConstant(r1, 10);
        masm.fmov(64, Aarch64.d0, r0);
        masm.fmov(64, r2, Aarch64.d0);
        masm.fmov(64, Aarch64.d5, r0);
        masm.fmov(64, Aarch64.r4, Aarch64.d5);

        tester.setExpectedValue(r0, 12);
        tester.setExpectedValue(r1, 10);
        tester.setExpectedValue(r2, 12);
        tester.setExpectedValue(r4, 12);
    }
//
//     //TODO: Fix vmovimm
//     public void broken_vmovimm() throws Exception {
////         setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
//         masm.vmovImm(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, 1, CiKind.Double);
//         masm.vmovImm(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, 0, CiKind.Double);
//         masm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r0, Aarch64.s0, null, CiKind.Int, CiKind.Double);
//         masm.vmovImm(Aarch64Assembler.ConditionFlag.Always, Aarch64.s1, -100, CiKind.Double);
//         masm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r2, Aarch64.s1,null,  CiKind.Int, CiKind.Double);
//         expectedValues[0] = 0;
//         testValues[0] = true;
//         expectedValues[2] = -100;
//         testValues[2] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
//     }
//
//     public void ignore_FloatIngPointExperiments() throws Exception {
////         setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
//         masm.mov32BitConstant(r0, 12);
//         masm.mov32BitConstant(r1, 10);
//         masm.codeBuffer.emitInt(0xee000a10);
//         masm.codeBuffer.emitInt(0xee001a90);
//         masm.codeBuffer.emitInt(0xeeb81ac0);
//         masm.codeBuffer.emitInt(0xeef81ae0);
//         masm.codeBuffer.emitInt(0xee210a21);
//         masm.codeBuffer.emitInt(0xeebd0a40);
//         masm.codeBuffer.emitInt(0xee100a10);
//         expectedValues[0] = 120;
//         testValues[0] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
//     }
//
    @Test
    public void subReg() throws Exception {
        for (int i = 0; i < 5; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
        }
        for (int i = 0; i < 5; i++) {
            masm.sub(64, Aarch64.cpuRegisters[i + 5], Aarch64.cpuRegisters[5 - (i + 1)], Aarch64.cpuRegisters[i]);
            tester.setExpectedValue(Aarch64.cpuRegisters[i + 5], expectedValues[5 - (i + 1)] - expectedValues[i]);
        }
    }

    @Test
    public void mov() throws Exception {
        for (int i = 0; i < 5; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            masm.mov(64, Aarch64.cpuRegisters[i + 5], Aarch64.cpuRegisters[i]);
            tester.setExpectedValue(Aarch64.cpuRegisters[i + 5], expectedValues[i]);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], expectedValues[i]);
        }
    }

    @Test
    public void sub() throws Exception {
        for (int i = 0; i < 10; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            masm.sub(64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], i * 2);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], expectedValues[i] - i * 2);
        }
    }

    @Test
    public void str() throws Exception {
        masm.mov32BitConstant(r12, 0);
        Aarch64Address address = Aarch64Address.createRegisterOffsetAddress(Aarch64.sp, r12, false);
        for (int i = 0; i < 10; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], expectedValues[i]);
            masm.str(VARIANT_64, Aarch64.cpuRegisters[i], address);
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], -2 * (expectedValues[i]));
            masm.ldr(VARIANT_64, Aarch64.cpuRegisters[i], address);
        }
    }

    @Test
    public void stp_ldp() throws Exception {
        expectedValues[0] = expectedValues[2] = 0x00001;
        expectedValues[1] = expectedValues[3] = 0xFFFFF;

        for (int i = 0; i < 4; i++) {
            tester.setExpectedValue(Aarch64.cpuRegisters[i], expectedValues[i]);
        }
        masm.mov64BitConstant(r0, expectedValues[0]);
        masm.mov64BitConstant(r1, expectedValues[1]);
        masm.stp(64, r0, r1, Aarch64Address.createPreIndexedImmediateAddress(Aarch64.sp, -2));
        masm.ldp(64, r2, Aarch64.r3, Aarch64Address.createPostIndexedImmediateAddress(Aarch64.sp, 2));
    }

    @Test
    public void ldr() throws Exception {
        for (int i = 0; i < 10; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], expectedValues[i]);
        }
        masm.push(1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        Aarch64Address address = Aarch64Address.createRegisterOffsetAddress(Aarch64.sp, r12, false);
        for (int i = 0; i < 10; i++) {
            masm.mov32BitConstant(r12, 16 * i);
            masm.ldr(VARIANT_64, Aarch64.cpuRegisters[9 - i], address);
        }
    }

    @Test
    public void decq() throws Exception {
        for (int i = 0; i < 10; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            masm.decq(Aarch64.cpuRegisters[i]);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], expectedValues[i] - 1);
        }
    }

    @Test
    public void incq() throws Exception {
        for (int i = 0; i < 10; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            masm.incq(Aarch64.cpuRegisters[i]);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], expectedValues[i] + 1);
        }
    }

    @Test
    public void addq() throws Exception {
        for (int i = 0; i < 10; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            masm.addq(Aarch64.cpuRegisters[i], expectedValues[i]);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], 2L * expectedValues[i]);
        }
    }
//
//     public void ignore_Ldrsh() throws Exception {
//         int[] testval = { 0x03020100, 0x8fed9ba9};
//         int mask = 0xffff;
////         setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
//         // load r0 and r1 with sensible values for ignoring the loading of bytes.
//         masm.mov32BitConstant(r0], testval[0);
//         masm.mov32BitConstant(r1], testval[1);
//         masm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2); // values now lie on the stack
//         // we now try to extract the "signed halfwords"
//         // from the stack and place them into r0..r3
//         for (int i = 0; i < 4; i++) {
//             // in this test we are using the stack register as the base value!
//             masm.ldrshw(Aarch64Assembler.ConditionFlag.Always, 1, 1, 0, Aarch64.cpuRegisters[i], r13, i * 2);
//             testValues[i] = true;
//             if (i < 2) {
//                 expectedValues[i] = testval[0] & (mask << (16 * (i % 2)));
//             } else {
//                 expectedValues[i] = testval[1] & (mask << (16 * (i % 2)));
//             }
//             if ((expectedValues[i] & 0x8000) != 0) {
//                 expectedValues[i] = expectedValues[i] - 0x10000; // sign extension workaround.
//             }
//             expectedValues[i] = expectedValues[i] >> 16 * (i % 2);
//         }
//
//         generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
//     }
//
    @Test
    public void strdAndLdrd() throws Exception {
        Aarch64Address address = Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, -16); // stack address
        for (int i = 0; i < 10; i += 2) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            masm.mov32BitConstant(Aarch64.cpuRegisters[i + 1], expectedValues[i + 1]);
            masm.str(64, Aarch64.cpuRegisters[i], address);
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], 0);
            masm.mov32BitConstant(Aarch64.cpuRegisters[i + 1], 0);
            masm.ldr(64, Aarch64.cpuRegisters[i], address);
            tester.setExpectedValue(Aarch64.cpuRegisters[i], expectedValues[i]);
            tester.setExpectedValue(Aarch64.cpuRegisters[i + 1], 0);
        }
    }

    @Test
    public void pushAndPop() throws Exception {
        int registers = 1;
        for (int bitmask = 1; bitmask <= 0xfff; bitmask = bitmask | (bitmask + 1), registers++) {
            masm.codeBuffer.reset();
            for (int i = 0; i < 13; i++) { // we are not breaking the stack (r13)
                masm.mov64BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]); // 2 instructions movw, movt
                // all registers initialized.
            }
            masm.push(bitmask); // store all registers referred to
            // by bitmask on the stack
            for (int i = 0; i < 13; i++) {
                masm.add(64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 1);
            }
            // r0..r12 should now all have +1 more than their previous values stored on the stack
            // restore the same registers that were placed on the stack
            masm.pop(bitmask);
            for (int i = 0; i < 13; i++) {
                if (i < registers) {
                    tester.setExpectedValue(Aarch64.cpuRegisters[i], expectedValues[i]);
                } else {
                    expectedValues[i] = expectedValues[i] + 1;
                    tester.setExpectedValue(Aarch64.cpuRegisters[i], expectedValues[i]);
                }
            }
        }
    }

    @Test
    public void movRor() throws Exception {
        final long value = 0x0fab0dad0badcafeL;

        for (int shift = 1; shift < 64; shift += 4) {
            for (int i = 0; i < 4 && shift < 64; i++, shift++) {
                final int index = i * 3;
                masm.mov64BitConstant(Aarch64.cpuRegisters[index], value);
                tester.setExpectedValue(Aarch64.cpuRegisters[index], value);
                masm.movror(Aarch64.cpuRegisters[index + 1], Aarch64.cpuRegisters[index], shift);
                tester.setExpectedValue(Aarch64.cpuRegisters[index], Long.rotateRight(value, shift));
                masm.movror(Aarch64.cpuRegisters[index + 2], Aarch64.cpuRegisters[index + 1], 63 - shift);
                tester.setExpectedValue(Aarch64.cpuRegisters[index], value);
            }
        }
    }

//     public void ignore_Ldrd() throws Exception {
////         setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
//         for (int i = 0; i < expectedLongValues.length; i++) {
//             System.out.println(i + " " + expectedLongValues[i]);
//             masm.mov64BitConstant(Aarch64.cpuRegisters[i] * 2], Aarch64.cpuRegisters[(i * 2) + 1], expectedLongValues[i);
//             testValues[i] = true;
//         }
//         masm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8);
//         for (int i = 0; i < expectedLongValues.length * 2; i++) {
//              masm.mov32BitConstant(Aarch64.cpuRegisters[i],0);
//         }
//         for (int i = 0; i < expectedLongValues.length; i++) {
//             masm.movw(Aarch64Assembler.ConditionFlag.Always, Aarch64.r12, i * 8);
//             masm.movt(Aarch64Assembler.ConditionFlag.Always, Aarch64.r12, 0);
//             masm.ldrd(ConditionFlag.Always, 0, 0, 0, Aarch64.RSP.asRegister(), Aarch64.cpuRegisters[i] * 2, Aarch64.r12);
//         }
//         generateAndTest(expectedLongValues, testValues, bitmasks);
//     }

    @Test
    public void casInt() throws Exception {
        CiRegister cmpReg = r0;

        // r0=50, r1=10, r2=20, r3=30, r4=40, r5=50
        masm.mov(r0, 50);
        for (int i = 1; i < 6; i++) {
            masm.mov(Aarch64.cpuRegisters[i], i * 10);
        }
        masm.push(r0, r1, r2, r3, r4, r5);

        // Invoke a cas that fails
        masm.add(64, r6, sp, 4 * 16); // point to r1 value in the stack
        Aarch64Address addr = Aarch64Address.createBaseRegisterOnlyAddress(r6);
        masm.cas(32, r1, cmpReg, addr);
        masm.ldr(32, r1, addr);
        tester.setExpectedValue(r1, 10);

        // Invoke a cas that succeeds
        addr = Aarch64Address.createBaseRegisterOnlyAddress(sp); // point to r5 value in the stack
        masm.cas(32, Aarch64.r3, cmpReg, addr);
        masm.ldr(32, r2, addr);
        tester.setExpectedValue(r2, 30);
        tester.setExpectedValue(masm.scratchRegister, 50);
    }

    @Test
    public void casLong() throws Exception {
        CiRegister cmpReg = r0;

        // r0=50 << 32, r1=10 << 32, r2=20 << 32, r3=30 << 32, r4=40 << 32, r5=50 << 32
        masm.mov64BitConstant(r0, 50L << 32);
        for (int i = 1; i < 6; i++) {
            masm.mov64BitConstant(Aarch64.cpuRegisters[i], i * 10L << 32);
        }
        masm.push(r0, r1, r2, Aarch64.r3, Aarch64.r4, Aarch64.r5);

        // Invoke a cas that fails
        masm.add(64, Aarch64.r6, Aarch64.sp, 4 * 16); // point to r1 value in the stack
        Aarch64Address addr = Aarch64Address.createBaseRegisterOnlyAddress(Aarch64.r6);
        masm.cas(64, r1, cmpReg, addr);
        masm.ldr(64, r1, addr);
        tester.setExpectedValue(r1, 10L << 32);

        // Invoke a cas that succeeds
        addr = Aarch64Address.createBaseRegisterOnlyAddress(Aarch64.sp); // point to r5 value in the stack
        masm.cas(64, Aarch64.r3, cmpReg, addr);
        masm.ldr(64, r2, addr);
        tester.setExpectedValue(r2, 30L << 32);
        tester.setExpectedValue(r0, 0);
    }

}
