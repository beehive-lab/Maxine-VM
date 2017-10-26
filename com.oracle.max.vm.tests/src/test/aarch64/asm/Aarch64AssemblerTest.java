/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
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
package test.aarch64.asm;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.aarch64.*;
import com.sun.cri.ci.*;
import com.sun.max.ide.*;

import test.aarch64.asm.MaxineAarch64Tester.BitsFlag;

public class Aarch64AssemblerTest extends MaxTestCase {

    private static final int VARIANT_32 = 32;
    private static final int VARIANT_64 = 64;

    private Aarch64Assembler asm;
    private Aarch64MacroAssembler masm;

    private CiTarget      aarch64;
    private ARMCodeWriter code;

    static final class Pair {

        public final int first;
        public final int second;

        Pair(int first, int second) {
            this.first = first;
            this.second = second;
        }
    }

    public Aarch64AssemblerTest() {
        aarch64 = new CiTarget(new Aarch64(), true, 8, 0, 4096, 0, false, false, false, true);
        asm = new Aarch64Assembler(aarch64, null);
        masm = new Aarch64MacroAssembler(aarch64, null);
        code = null;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Aarch64AssemblerTest.class);
    }

    private static int[] valueTestSet = {0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65535};
    private static int[] scratchTestSet = {0, 1, 0xff, 0xffff, 0xffffff, 0xfffffff, 0x7fffffff};
    private static boolean[] testValues = new boolean[MaxineAarch64Tester.NUM_REGS];

    // Each test should set the contents of this array appropriately,
    // it enables the instruction under test to select the specific bit values for
    // comparison i.e. for example ignoring upper or lower 16bits for movt, movw
    // and for ignoring specific bits in the status register etc
    // concerning whether a carry has been set
    private static MaxineAarch64Tester.BitsFlag[] bitmasks = new MaxineAarch64Tester.BitsFlag[MaxineAarch64Tester.NUM_REGS];
    static {
        for (int i = 0; i < MaxineAarch64Tester.NUM_REGS; i++) {
            bitmasks[i] = MaxineAarch64Tester.BitsFlag.All32Bits;
        }
    }

    private static void setBitMask(int i, MaxineAarch64Tester.BitsFlag mask) {
        bitmasks[i] = mask;
    }

    private static void setAllBitMasks(MaxineAarch64Tester.BitsFlag mask) {
        for (int i = 0; i < bitmasks.length; i++) {
            bitmasks[i] = mask;
        }
    }

    private static void setIgnoreValue(int i, boolean value, boolean all) {
        testValues[i] = value;
    }

    private static void resetIgnoreValues() {
        for (int i = 0; i < testValues.length; i++) {
            testValues[i] = false;
        }
    }

    // The following values will be updated
    // to those expected to be found in a register after simulated execution of code.
    private static long[] expectedValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

    private static long[] expectedLongValues = {Long.MAX_VALUE - 100, Long.MAX_VALUE};

    private static void initialiseExpectedValues() {
        for (int i = 0; i < MaxineAarch64Tester.NUM_REGS; i++) {
            expectedValues[i] = i;
        }
    }

    private static void initialiseTestValues() {
        for (int i = 0; i < MaxineAarch64Tester.NUM_REGS; i++) {
            testValues[i] = false;
        }
    }

    private void generateAndTest(long[] expected, boolean[] tests, MaxineAarch64Tester.BitsFlag[] masks, Buffer codeBuffer) throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(codeBuffer);
        code.createCodeFile();
        MaxineAarch64Tester r = new MaxineAarch64Tester(expected, tests, masks);
        if (!MaxineAarch64Tester.ENABLE_SIMULATOR) {
            System.out.println("Code Generation is disabled!");
            return;
        }
        r.assembleStartup();
        r.compile();
        r.link();
        r.runSimulation();
        r.reset();
    }

    public void work_b() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov64BitConstant(Aarch64.r0, -1);
        asm.b(24);
        asm.nop(4);                                                          // +16 bytes
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x123, 0);    // +20 bytes to here (skipped)
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 0x123, 0);    // +24 bytes to here (landing)
        expectedValues[0] = -1;
        testValues[0] = true;
        expectedValues[1] = 0x123;
        testValues[1] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_zero() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov(Aarch64.r0, 0xFF);
        masm.mov(64, Aarch64.r0, Aarch64.zr);
        expectedValues[0] = 0;
        testValues[0] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }


//    public void test_adr_adrp() throws Exception {
//        initialiseExpectedValues();
//        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//        resetIgnoreValues();
//        masm.codeBuffer.reset();
//
//        masm.adrp(Aarch64.r0, 0);
//        masm.adrp(Aarch64.r1, 4096);
//        masm.adrp(Aarch64.r2, 8192);
//        masm.adr(Aarch64.r3, 0);
//        masm.mov(Aarch64.r4, 0xfffL);
//
//        long[] reg = generate(expectedValues, testValues, bitmasks,
//                masm.codeBuffer);
//
//        for (int i = 0; i < 5; i++) {
//            System.out.println("REG-" + i + ": " + reg[i]);
//        }
//        assert (reg[3] & ~reg[4]) == reg[0];
//    }

    public void work_mov64() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();

        masm.mov64BitConstant(Aarch64.r0, 10);
        masm.mov64BitConstant(Aarch64.r1, -10);
        masm.mov64BitConstant(Aarch64.r2, -12345678987654321L);
        masm.mov64BitConstant(Aarch64.r3, Long.MAX_VALUE);
        masm.mov64BitConstant(Aarch64.r4, Long.MIN_VALUE);

        expectedValues[0] = 10;
        expectedValues[1] = -10;
        expectedValues[2] = -12345678987654321L;
        expectedValues[3] = Long.MAX_VALUE;
        expectedValues[4] = Long.MIN_VALUE;


        testValues[0] = true;
        testValues[1] = true;
        testValues[2] = true;
        testValues[3] = true;
        testValues[4] = true;

        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void work_mov() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();

        masm.mov(Aarch64.r0, 10);
        masm.mov(Aarch64.r1, -10);

        expectedValues[0] = 10;
        expectedValues[1] = -10;

        testValues[0] = true;
        testValues[1] = true;

        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void work_mov2() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();

        masm.mov(Aarch64.r0, (int) Integer.MAX_VALUE);
        masm.mov(Aarch64.r1, (int) Integer.MIN_VALUE);
        masm.mov(Aarch64.r2, (long) Long.MAX_VALUE);
        masm.mov(Aarch64.r3, (long) Long.MIN_VALUE);

        expectedValues[0] = Integer.MAX_VALUE;
        expectedValues[1] = Integer.MIN_VALUE;
        expectedValues[2] = Long.MAX_VALUE;
        expectedValues[3] = Long.MIN_VALUE;

        testValues[0] = true;
        testValues[1] = true;
        testValues[2] = true;
        testValues[3] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void work_add_register() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov64BitConstant(Aarch64.r0, 10);
        masm.mov64BitConstant(Aarch64.r1, -1);
        masm.mov64BitConstant(Aarch64.r2, 1);
        masm.mov64BitConstant(Aarch64.r3, Long.MAX_VALUE);
        masm.mov64BitConstant(Aarch64.r4, Long.MIN_VALUE);

        masm.add(64, Aarch64.r0, Aarch64.r0, Aarch64.r1);
        masm.add(64, Aarch64.r3, Aarch64.r3, Aarch64.r2);
        masm.add(64, Aarch64.r4, Aarch64.r4, Aarch64.r1);
        masm.add(64, Aarch64.r5, Aarch64.r4, Aarch64.r2);
        masm.add(64, Aarch64.r6, Aarch64.r3, Aarch64.r1);

        expectedValues[0] = 9;
        expectedValues[3] = Long.MIN_VALUE;
        expectedValues[4] = Long.MAX_VALUE;
        expectedValues[5] = Long.MIN_VALUE;
        expectedValues[6] = Long.MAX_VALUE;
        testValues[0] = true;
        testValues[3] = true;
        testValues[4] = true;
        testValues[5] = true;
        testValues[6] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    //    TODO: Look again at this.
    public void todo_movImmediate() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movImmediate(Aarch64.r0, 10);
        asm.movImmediate(Aarch64.r1, 1);
        asm.movImmediate(Aarch64.r2, -10);

        expectedValues[0] = 10;
        expectedValues[1] = 1;
        expectedValues[2] = -10;

        testValues[0] = true;
        testValues[1] = true;
        testValues[2] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * branch.
     */
    public void work_bcond() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();

        masm.mov32BitConstant(Aarch64.r0, 1);
        masm.mov32BitConstant(Aarch64.r1, 1);

        masm.cmp(32, Aarch64.r0, Aarch64.r1);
        masm.b(Aarch64Assembler.ConditionFlag.EQ, 4);
        masm.mov32BitConstant(Aarch64.r3, 66);
        masm.mov32BitConstant(Aarch64.r3, 77);

        expectedValues[3] = 77;
        testValues[3] = true;

        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    /**
     * load and store instructions.
     */
    public void todo_ldr_str() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x123, 0);        // value to be stored to stack
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 8, 0);           // offset
        Aarch64Address address = Aarch64Address.createRegisterOffsetAddress(Aarch64.sp,  Aarch64.cpuRegisters[10], false); // stack address
        asm.str(VARIANT_64,  Aarch64.cpuRegisters[0], address);         // store value to stack
        asm.ldr(VARIANT_64, Aarch64.cpuRegisters[1], address);          // load value from stack
        expectedValues[0] = 0x123;
        testValues[0] = true;
        expectedValues[1] = 0x123;
        testValues[1] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void todo_add_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // ADD Xi, Xi, valueof(Xi) ; Xi's value should double
        for (int i = 0; i < 10; i++) {
            // expected values must not be larger than an integer in order to be converted to an int correctly
            assert (expectedValues[i] < Integer.MAX_VALUE);
            asm.movImmediate(Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            asm.add(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            expectedValues[i] += expectedValues[i];
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void todo_sub_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // SUB Xi, Xi, valueof(Xi) ; Xi should then be 0
        for (int i = 0; i < 10; i++) {
            // expected values must not be larger than an integer in order to be converted to an int correctly
            assert (expectedValues[i] < Integer.MAX_VALUE);
            asm.movImmediate(Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            asm.sub(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            expectedValues[i] = 0; //expectedValues[i] -= expectedValues[i];
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void todo_and_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // AND Xi, Xi, 0x1
        for (int i = 0; i < 10; i++) {
            asm.movImmediate(Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            asm.and(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 0x1);
            expectedValues[i] &= 0x1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void todo_eor_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // EOR Xi, Xi, 0x1
        for (int i = 0; i < 10; i++) {
            asm.movImmediate(Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            asm.eor(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 0x1);
            expectedValues[i] ^= 0x1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void todo_orr_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // ORR Xi, Xi, 0x1
        for (int i = 0; i < 10; i++) {
            asm.movImmediate(Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            asm.orr(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 0x1);
            expectedValues[i] |= 0x1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void todo_movz_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // MOVZ Xi, 0x1, LSL #0x10
        for (int i = 0; i < 1; i++) {
            asm.movz(VARIANT_64, Aarch64.cpuRegisters[i], 0x1, 32);
            expectedValues[i] = (long) 1 << 32;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void todo_movn_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // MOVZ Xi, 0x1, LSL #0x10
        for (int i = 0; i < 1; i++) {
            asm.movn(VARIANT_64, Aarch64.cpuRegisters[i], 0x1, 32);
            expectedValues[i] = ~((long) 1 << 32);
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_bfm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movn(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x0, 0);

        asm.bfm(VARIANT_64, Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[10], 3, 5);
        expectedValues[0] = 0b111111L >>> 3;
        testValues[0] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_ubfm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movn(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x0, 0);

        asm.ubfm(VARIANT_64, Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[10], 3, 5);
        expectedValues[0] = 0b111111L >>> 3;
        testValues[0] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_sbfm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movn(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x0, 0);

        asm.ubfm(VARIANT_64, Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[10], 3, 5);
        expectedValues[0] = 0b111111L >>> 3;
        testValues[0] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /******* Arithmetic (shifted register) (5.5.1). *******/

    public void work_add_shift_reg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImmediate(Aarch64.cpuRegisters[10], 1);
        for (int i = 0; i < 1; i++) {
            asm.add(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], Aarch64.cpuRegisters[10], Aarch64Assembler.ShiftType.LSL, 2);
            expectedValues[i] = 5;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_sub_shift_reg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImmediate(Aarch64.cpuRegisters[10], 0b1000);
        for (int i = 0; i < 1; i++) {
            asm.sub(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], Aarch64.cpuRegisters[10], Aarch64Assembler.ShiftType.LSR, 3);
            expectedValues[i] = 0b1000 - (0b1000 >>> 3);
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /******* Arithmetic (extended register) (5.5.2). *******/

    public void work_add_ext_reg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImmediate(Aarch64.cpuRegisters[0], 1);
        asm.movn(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);

        for (int i = 0; i < 1; i++) {
            asm.add(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], Aarch64Assembler.ExtendType.UXTB, 3);
            expectedValues[i] = 1 + 0b11111111000L;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_sub_ext_reg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImmediate(Aarch64.cpuRegisters[0], 0b11111111000);
        asm.movn(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);

        for (int i = 0; i < 1; i++) {
            asm.sub(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], Aarch64Assembler.ExtendType.UXTB, 3);
            expectedValues[i] = 0;
            testValues[i] = true;
        }

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /******* Logical (shifted register) (5.5.3). *******/

    public void work_and_shift_reg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movn(VARIANT_64, Aarch64.cpuRegisters[0], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 0xf, 0);

        for (int i = 0; i < 1; i++) {
            asm.and(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], Aarch64Assembler.ShiftType.LSL, 4);
            expectedValues[i] = 0xffffffffffffffffL & (0b1111L << 4);
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);

    }

    /* Variable Shift (5.5.4) */
    //Duplicate of work_and_shift_reg()
    public void work_asr() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movn(VARIANT_64, Aarch64.cpuRegisters[0], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 0xf, 0);

        for (int i = 0; i < 1; i++) {
            asm.and(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], Aarch64Assembler.ShiftType.LSL, 4);
            expectedValues[i] = 0xffffffffffffffffL & (0b1111L << 4);
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /* Bit Operations (5.5.5). */
    //not working... check cls
    public void todo_cls() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movn(VARIANT_64, Aarch64.cpuRegisters[30], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);

        asm.cls(VARIANT_64, Aarch64.cpuRegisters[30], Aarch64.cpuRegisters[10]);
        expectedValues[30] = 63;
        testValues[30] = true;
        expectedValues[10] = 0;
        testValues[10] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /* Integer Multiply/Divide (5.6). */

   /* Floating-point Move (register) (5.7.2) */

    public void work_float0() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 10, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 11, 0);

        asm.fmovCpu2Fpu(VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);
        asm.fmovCpu2Fpu(VARIANT_64, Aarch64.fpuRegisters[1], Aarch64.cpuRegisters[1]);
        asm.fmovFpu2Cpu(VARIANT_64, Aarch64.cpuRegisters[2], Aarch64.fpuRegisters[0]);
        asm.fmovFpu2Cpu(VARIANT_64, Aarch64.cpuRegisters[3], Aarch64.fpuRegisters[1]);

        expectedValues[2] = 10;
        testValues[2] = true;
        expectedValues[3] = 11;
        testValues[3] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);

    }

    /**
     * test: fadd, fsub, fmul, fdiv, scvtf, fcvtzs.
     */
    public void work_float1() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 10, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 110, 0);

        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);
        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[1], Aarch64.cpuRegisters[1]);

        asm.fadd(VARIANT_64, Aarch64.fpuRegisters[2], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1]);
        asm.fsub(VARIANT_64, Aarch64.fpuRegisters[3], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[0]);
        asm.fmul(VARIANT_64, Aarch64.fpuRegisters[4], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[0]);
        asm.fdiv(VARIANT_64, Aarch64.fpuRegisters[5], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[0]);

        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[2], Aarch64.fpuRegisters[2]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[3], Aarch64.fpuRegisters[3]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[4], Aarch64.fpuRegisters[4]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[5], Aarch64.fpuRegisters[5]);

        expectedValues[0] = 10;
        testValues[0] = true;
        expectedValues[1] = 110;
        testValues[1] = true;
        expectedValues[2] = 120;         // fadd
        testValues[2] = true;
        expectedValues[3] = 100;         // fsub
        testValues[3] = true;
        expectedValues[4] = 10 * 110;    // fmul
        testValues[4] = true;
        expectedValues[5] = 110 / 10;    // fdiv
        testValues[5] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * frintz, fabs, fneg, fsqrt.
     */
    public void work_float2() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 100, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 110, 0);

        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);
        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[1], Aarch64.cpuRegisters[1]);

        asm.fdiv(VARIANT_64, Aarch64.fpuRegisters[2], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[0]);
        asm.frintz(VARIANT_64, Aarch64.fpuRegisters[2], Aarch64.fpuRegisters[2]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[2], Aarch64.fpuRegisters[2]);

        asm.fsub(VARIANT_64, Aarch64.fpuRegisters[3], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1]);
        asm.fabs(VARIANT_64, Aarch64.fpuRegisters[3], Aarch64.fpuRegisters[3]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[3], Aarch64.fpuRegisters[3]);

        asm.fsub(VARIANT_64, Aarch64.fpuRegisters[4], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1]);
        asm.fneg(VARIANT_64, Aarch64.fpuRegisters[4], Aarch64.fpuRegisters[4]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[4], Aarch64.fpuRegisters[4]);

        asm.fsqrt(VARIANT_64, Aarch64.fpuRegisters[5], Aarch64.fpuRegisters[0]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[5], Aarch64.fpuRegisters[5]);

        expectedValues[0] = 100;
        testValues[0] = true;
        expectedValues[1] = 110;
        testValues[1] = true;
        expectedValues[2] = 1;         // frintz
        testValues[2] = true;
        expectedValues[3] = 10;        // fabs
        testValues[3] = true;
        expectedValues[4] = 10;        // fneg
        testValues[4] = true;
        expectedValues[5] = 10;        // fsqrt
        testValues[5] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * fmadd, fmsub.
     */
    public void work_float3() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 100, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 110, 0);

        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);
        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[1], Aarch64.cpuRegisters[1]);

        asm.fmadd(VARIANT_64, Aarch64.fpuRegisters[3], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[1]);
        asm.fmsub(VARIANT_64, Aarch64.fpuRegisters[4], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[1]);

        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[3], Aarch64.fpuRegisters[3]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[4], Aarch64.fpuRegisters[4]);

        expectedValues[0] = 100;
        testValues[0] = true;
        expectedValues[1] = 110;
        testValues[1] = true;

        expectedValues[3] = 110 + 100 * 110;        // fmadd
        testValues[3] = true;
        expectedValues[4] = 110 - 100 * 110;        // fmsub
        testValues[4] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * fstr, fldr.
     */
    public void work_float4() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x123, 0);        // value to be stored to stack
        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 8, 0);           // offset
        Aarch64Address address = Aarch64Address.createRegisterOffsetAddress(Aarch64.sp, Aarch64.cpuRegisters[10], false); // stack address

        asm.fstr(VARIANT_64, Aarch64.fpuRegisters[0], address);         // store value to stack
        asm.fldr(VARIANT_64, Aarch64.fpuRegisters[1], address);          // load value from stack

        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[1], Aarch64.fpuRegisters[1]);

        expectedValues[0] = 0x123;
        testValues[0] = true;
        expectedValues[1] = 0x123;
        testValues[1] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * mrs.
     */
    public void work_mrs() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 1, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[11], 1, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[12], 2, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[13], 3, 0);

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[2], 0, 0);

        asm.mrs(Aarch64.cpuRegisters[0], Aarch64Assembler.SystemRegister.NZCV);

        asm.subs(VARIANT_64, Aarch64.cpuRegisters[15], Aarch64.cpuRegisters[10], Aarch64.cpuRegisters[11], Aarch64Assembler.ShiftType.LSL, 0);
        asm.mrs(Aarch64.cpuRegisters[1], Aarch64Assembler.SystemRegister.NZCV);

        asm.subs(VARIANT_64, Aarch64.cpuRegisters[15], Aarch64.cpuRegisters[13], Aarch64.cpuRegisters[12], Aarch64Assembler.ShiftType.LSL, 0);
        asm.mrs(Aarch64.cpuRegisters[2], Aarch64Assembler.SystemRegister.NZCV);

        asm.subs(VARIANT_64, Aarch64.cpuRegisters[15], Aarch64.cpuRegisters[12], Aarch64.cpuRegisters[13], Aarch64Assembler.ShiftType.LSL, 0);
        asm.mrs(Aarch64.cpuRegisters[3], Aarch64Assembler.SystemRegister.NZCV);

        expectedValues[0] = 0b0110L << 28;
        testValues[0] = true;
        expectedValues[1] = 0b0110L << 28;
        testValues[1] = true;
        expectedValues[2] = 0b0010L << 28;
        testValues[2] = true;
        expectedValues[3] = 0b1000L << 28;
        testValues[3] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * mrs_reg.
     */
    public void work_mrs_reg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 0b1111, 0);
        asm.msr(Aarch64Assembler.SystemRegister.SPSR_EL1, Aarch64.cpuRegisters[1]);
        asm.mrs(Aarch64.cpuRegisters[0], Aarch64Assembler.SystemRegister.SPSR_EL1);

        expectedValues[0] = 0b1111;
        testValues[0] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * mrs_imm.
     */
    public void work_mrs_imm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.msr(Aarch64Assembler.PStateField.PSTATEField_DAIFClr, 0b1111);
        asm.msr(Aarch64Assembler.PStateField.PSTATEField_DAIFSet, 0b1001);
        asm.mrs(Aarch64.cpuRegisters[0], Aarch64Assembler.SystemRegister.DAIF);

        // if dst == PSTATEField_SP, then the first 3 digits of the operand is ignored, which means only the last bit is used to set SPSel.
        asm.msr(Aarch64Assembler.PStateField.PSTATEField_SP, 0b0001);
        asm.mrs(Aarch64.cpuRegisters[1], Aarch64Assembler.SystemRegister.SPSel);
        asm.msr(Aarch64Assembler.PStateField.PSTATEField_SP, 0b0000);
        asm.mrs(Aarch64.cpuRegisters[2], Aarch64Assembler.SystemRegister.SPSel);

        expectedValues[0] = 0b1001 << 6;
        testValues[0] = true;
        expectedValues[1] = 0b1 << 0;
        testValues[1] = true;
        expectedValues[2] = 0b0 << 0;
        testValues[2] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

//    public void todo_Ldrb() throws Exception {
//        int[] testval = { 0x03020100, 0xffedcba9};
//        int mask = 0xff;
//        initialiseExpectedValues();
//        setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//        resetIgnoreValues();
//        asm.codeBuffer.reset();
//        // load r0 and r1 with sensible values for ignoring the loading of bytes.
//        asm.mov32BitConstant(Aarch64.cpuRegisters[0], testval[0]);
//        asm.mov32BitConstant(Aarch64.cpuRegisters[1], testval[1]);
//        asm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2); // values now lie on the stack
//        for (int i = 0; i < 8; i++) {
//            // stackpointer advanced by 8
//            asm.ldrb(Aarch64Assembler.ConditionFlag.Always, 1, 1, 0, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[13], i);
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
//       generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//      }
//
//    public long connectRegs(int reg0, int reg1) {
//         long returnVal = 0;
//         long tmp = 0;
//         //  r1 is MSW
//         // r2 is LSW
//         System.out.println(" REG0 " + reg0 + " REG1 " + reg1);
//         if(reg1 < 0) {
//             // -ve long number
//
//             returnVal = ((long) reg1) << 32;
//
//
//             if(reg0 < 0) {
//                 returnVal += Long.parseLong(String.format("%32s", Integer.toBinaryString(reg0)).replace(' ', '0'),2);
//             }else {
//                 returnVal += reg0;
//             }
//         } else {
//             // +ve long number
//             returnVal = ((long)reg1) << 32;
//             if(reg1 == 0) {
//                 returnVal += reg0;
//             } else
//             if(reg0 < 0) {
//                 //returnVal += 1L << 31;
//                 returnVal += Long.parseLong(String.format("%32s", Integer.toBinaryString(reg0)).replace(' ', '0'),2);
//             } else {
//                 returnVal += reg0;
//             }
//         }
//
//         return returnVal;
//     }
//
//     public void ignore_mov64BitConstant() throws Exception {
//         int[] instructions = new int[6];
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         long[] values = new long[10];
//         values[0] = 0L;
//         values[1] = -1L;
//         values[2] = (long) Integer.MIN_VALUE;
//         values[3] = (long) Integer.MAX_VALUE;
//         values[4] = Long.MAX_VALUE;
//         values[5] = Long.MIN_VALUE;
//         values[6] = Long.MIN_VALUE + 5;
//         values[7] = Long.MAX_VALUE - 5;
//         values[8] = ((long) Integer.MIN_VALUE) + 5L;
//         values[9] = ((long) Integer.MAX_VALUE) - 5L;
//         int registers[] = null;
//         for (int i = 0; i < values.length; i++) {
//             asm.codeBuffer.reset();
//             asm.mov64BitConstant(Aarch64.r0, Aarch64.r1, values[i]);
//             instructions[0] = asm.codeBuffer.getInt(0);
//             instructions[1] = asm.codeBuffer.getInt(4);
//             instructions[2] = asm.codeBuffer.getInt(8);
//             instructions[3] = asm.codeBuffer.getInt(12);
//             registers = generate();
//             assert values[i] == connectRegs(registers[0], registers[1]);
//         }
//     }

//     public void ignore_AddConstant() throws Exception {
//         int[] instructions = new int[3];
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         initialiseExpectedValues();
//         resetIgnoreValues();
//         for (int srcReg = 0; srcReg < 3; srcReg++) {
//             for (int destReg = 0; destReg < 3; destReg++) {
//                 initialiseTestValues();
//                 testValues[destReg] = true;
//                 for (int i = 0; i < scratchTestSet.length; i++) {
//                     asm.codeBuffer.reset();
//                     int value = scratchTestSet[i];
//                     asm.movw(Aarch64Assembler.ConditionFlag.Always, Aarch64.cpuRegisters[srcReg], value & 0xffff);
//                     asm.movt(Aarch64Assembler.ConditionFlag.Always, Aarch64.cpuRegisters[srcReg], (value >> 16) & 0xffff);
//                     asm.add(Aarch64Assembler.ConditionFlag.Always, false, Aarch64.cpuRegisters[destReg], Aarch64.cpuRegisters[srcReg], 0, 0);
//                     instructions[0] = asm.codeBuffer.getInt(0);
//                     instructions[1] = asm.codeBuffer.getInt(4);
//                     instructions[2] = asm.codeBuffer.getInt(8);
//                     expectedValues[destReg] = value;
//                     generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//                 }
//             }
//         }
//     }
//
//     public void test_VPushPop() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         for (int i = 0; i < 5; i++) {
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i], i);
//             asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.allRegisters[i + 16], Aarch64.cpuRegisters[i], null, CiKind.Float, CiKind.Int);
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i], -i);
//         }
//         asm.vpush(Aarch64Assembler.ConditionFlag.Always, Aarch64.allRegisters[16], Aarch64.allRegisters[16 + 4], CiKind.Float, CiKind.Float);
//         for (int i = 0; i < 5; i++) {
//             asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.allRegisters[i + 16], Aarch64.cpuRegisters[i], null, CiKind.Float, CiKind.Int);
//         }
//         asm.vpop(Aarch64Assembler.ConditionFlag.Always, Aarch64.allRegisters[16], Aarch64.allRegisters[16 + 4], CiKind.Float, CiKind.Float);
//         for (int i = 0; i < 5; i++) {
//             asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.cpuRegisters[i], Aarch64.allRegisters[i + 16], null, CiKind.Int, CiKind.Float);
//         }
//         expectedValues[0] = 0;
//         testValues[0] = true;
//         expectedValues[1] = 1;
//         testValues[1] = true;
//         expectedValues[2] = 2;
//         testValues[2] = true;
//         expectedValues[3] = 3;
//         testValues[3] = true;
//         expectedValues[4] = 4;
//         testValues[4] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void test_Vdiv() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.mov32BitConstant(Aarch64.cpuRegisters[0], 10);
//         asm.mov32BitConstant(Aarch64.cpuRegisters[1], 24);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, Aarch64.r0, null, CiKind.Float, CiKind.Int);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s1, Aarch64.r1, null, CiKind.Float, CiKind.Int);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s2, false, true, Aarch64.s0, CiKind.Float, CiKind.Int);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s3, false, true, Aarch64.s1, CiKind.Float, CiKind.Int);
//
//         asm.vdiv(Aarch64Assembler.ConditionFlag.Always, Aarch64.s2, Aarch64.s3, Aarch64.s2, CiKind.Float);
//
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, true, true, Aarch64.s2, CiKind.Float, CiKind.Int);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r2, Aarch64.s0, null, CiKind.Int, CiKind.Float);
//         expectedValues[0] = 10;
//         testValues[0] = true;
//         expectedValues[1] = 24;
//         testValues[1] = true;
//         expectedValues[2] = 2;
//         testValues[2] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void test_Vcvt_int2float() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.mov32BitConstant(Aarch64.cpuRegisters[0], 10);
//         asm.mov32BitConstant(Aarch64.cpuRegisters[1], 24);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, Aarch64.r0, null, CiKind.Float, CiKind.Int);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s1, Aarch64.r1, null, CiKind.Float, CiKind.Int);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s2, false, true, Aarch64.s0, CiKind.Float, CiKind.Int);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s3, false, true, Aarch64.s1, CiKind.Float, CiKind.Int);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s2, true, true, Aarch64.s2, CiKind.Float, CiKind.Float);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s3, true, true, Aarch64.s3, CiKind.Float, CiKind.Float);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r0, Aarch64.s2, null, CiKind.Int, CiKind.Float);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r1, Aarch64.s3, null, CiKind.Int, CiKind.Float);
//         expectedValues[0] = 10;
//         testValues[0] = true;
//         expectedValues[1] = 24;
//         testValues[1] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void test_Vcvt_int2double() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.mov32BitConstant(Aarch64.cpuRegisters[0], 10);
//         asm.mov32BitConstant(Aarch64.cpuRegisters[1], 24);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, Aarch64.r0, null, CiKind.Float, CiKind.Int);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s1, Aarch64.r1, null, CiKind.Float, CiKind.Int);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s2, false, true, Aarch64.s0, CiKind.Double, CiKind.Int);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s3, false, true, Aarch64.s1, CiKind.Double, CiKind.Int);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, true, true, Aarch64.s2, CiKind.Float, CiKind.Double);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s1, true, true, Aarch64.s3, CiKind.Float, CiKind.Double);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r0, Aarch64.s0, null, CiKind.Int, CiKind.Float);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r2, Aarch64.s1, null, CiKind.Int, CiKind.Float);
//         expectedValues[0] = 10;
//         testValues[0] = true;
//         expectedValues[1] = 24;
//         testValues[1] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//
//
//     public void test_Vcvt_double2float() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.mov64BitConstant(Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[1], Double.doubleToRawLongBits(-10));
//         asm.mov64BitConstant(Aarch64.cpuRegisters[2], Aarch64.cpuRegisters[3], Double.doubleToRawLongBits(-24));
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, Aarch64.r0, Aarch64.r1, CiKind.Double, CiKind.Int);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s1, Aarch64.r2, Aarch64.r3, CiKind.Double, CiKind.Int);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s4, false, true, Aarch64.s0, CiKind.Float, CiKind.Double);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s5, false, true, Aarch64.s1, CiKind.Float, CiKind.Double);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r0, Aarch64.s4, null, CiKind.Int, CiKind.Float);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r2, Aarch64.s5, null, CiKind.Int, CiKind.Float);
//         expectedValues[0] = Float.floatToRawIntBits(-10);
//         testValues[0] = true;
//         expectedValues[2] =Float.floatToRawIntBits(-24);
//         testValues[2] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//
//     public void test_VAdd() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.mov32BitConstant(Aarch64.cpuRegisters[0], 12);
//         asm.mov32BitConstant(Aarch64.cpuRegisters[1], 10);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, Aarch64.r0, null, CiKind.Float, CiKind.Int); // r2 has r0?
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s1, Aarch64.r1, null, CiKind.Float, CiKind.Int); // r4 and r5 contain r0 and r1
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s2, false, false, Aarch64.s0, CiKind.Float, CiKind.Int);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s4, false, false, Aarch64.s1, CiKind.Float, CiKind.Int);
//         asm.vadd(Aarch64Assembler.ConditionFlag.Always, Aarch64.s2, Aarch64.s4, Aarch64.s2, CiKind.Float);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, true, true, Aarch64.s2, CiKind.Float, CiKind.Float);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r2, Aarch64.s0, null, CiKind.Int, CiKind.Float);
//         expectedValues[0] = 12;
//         testValues[0] = true;
//         expectedValues[1] = 10;
//         testValues[1] = true;
//         expectedValues[2] = 22;
//         testValues[2] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void test_VSub() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.mov32BitConstant(Aarch64.cpuRegisters[0], 12);
//         asm.mov32BitConstant(Aarch64.cpuRegisters[1], 10);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, Aarch64.r0, null, CiKind.Float, CiKind.Int);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s1, Aarch64.r1, null, CiKind.Float, CiKind.Int);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s2, false, false, Aarch64.s0, CiKind.Float, CiKind.Int);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s4, false, false, Aarch64.s1, CiKind.Float, CiKind.Int);
//         asm.vsub(Aarch64Assembler.ConditionFlag.Always, Aarch64.s2, Aarch64.s4, Aarch64.s2, CiKind.Float);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, true, true, Aarch64.s2, CiKind.Float, CiKind.Float);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r2, Aarch64.s0, null, CiKind.Int, CiKind.Float);
//         expectedValues[0] = 12;
//         testValues[0] = true;
//         expectedValues[1] = 10;
//         testValues[1] = true;
//         expectedValues[2] = -2;
//         testValues[2] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void test_VcvtvMul() throws Exception {
//         initialiseExpectedValues();
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.mov32BitConstant(Aarch64.cpuRegisters[0], 12);
//         asm.mov32BitConstant(Aarch64.cpuRegisters[1], 10);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, Aarch64.r0, null, CiKind.Float, CiKind.Int);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s1, Aarch64.r1, null, CiKind.Float, CiKind.Int);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s2, false, false, Aarch64.s0, CiKind.Float, CiKind.Int);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s4, false, false, Aarch64.s1, CiKind.Float, CiKind.Int);
//         asm.vmul(Aarch64Assembler.ConditionFlag.Always, Aarch64.s2, Aarch64.s4, Aarch64.s2, CiKind.Float);
//         asm.vcvt(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, true, false, Aarch64.s2, CiKind.Float, CiKind.Float);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r2, Aarch64.s0, null, CiKind.Int, CiKind.Float);
//         expectedValues[0] = 12;
//         testValues[0] = true;
//         expectedValues[1] = 10;
//         testValues[1] = true;
//         expectedValues[2] = 120;
//         testValues[2] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void test_VldrStr() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.mov32BitConstant(Aarch64.cpuRegisters[0], 12);
//         asm.mov32BitConstant(Aarch64.cpuRegisters[1], 10);
//         asm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512 | 1024 | 2048); // instruction
//         asm.vldr(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, Aarch64.r13, 0, CiKind.Float, CiKind.Int);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r2, Aarch64.s0, null, CiKind.Int, CiKind.Float);
//         asm.vldr(Aarch64Assembler.ConditionFlag.Always, Aarch64.s4, Aarch64.r13, 0, CiKind.Float, CiKind.Int);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r4, Aarch64.s4, null, CiKind.Int, CiKind.Float);
//         asm.vstr(Aarch64Assembler.ConditionFlag.Always, Aarch64.s4, Aarch64.r13, -8, CiKind.Float, CiKind.Int);
//         asm.vstr(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, Aarch64.r13, -16, CiKind.Float, CiKind.Int);
//         asm.vldr(Aarch64Assembler.ConditionFlag.Always, Aarch64.s10, Aarch64.r13, -8, CiKind.Float, CiKind.Int);
//         asm.vldr(Aarch64Assembler.ConditionFlag.Always, Aarch64.s31, Aarch64.r13, -16, CiKind.Float, CiKind.Int);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r6, Aarch64.s10, null, CiKind.Int, CiKind.Float);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r8, Aarch64.s31, null, CiKind.Int, CiKind.Float);
//         expectedValues[0] = 12;
//         testValues[0] = true;
//         expectedValues[1] = 10;
//         testValues[1] = true;
//         expectedValues[2] = 12;
//         testValues[2] = true;
//         expectedValues[4] = 12;
//         testValues[4] = true;
//         expectedValues[6] = 12;
//         testValues[6] = true;
//         expectedValues[8] = 12;
//         testValues[8] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void test_Vldr() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.mov32BitConstant(Aarch64.cpuRegisters[0], 12);
//         asm.mov32BitConstant(Aarch64.cpuRegisters[1], 10);
//         asm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512); // 1 instruction
//         asm.vldr(Aarch64Assembler.ConditionFlag.Always, Aarch64.s31, Aarch64.r13, 0, CiKind.Float, CiKind.Int);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r2, Aarch64.s31, null, CiKind.Int, CiKind.Float);
//         asm.vldr(Aarch64Assembler.ConditionFlag.Always, Aarch64.s4, Aarch64.r13, 0, CiKind.Int, CiKind.Float);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r4, Aarch64.s4, null, CiKind.Int, CiKind.Float);
//         expectedValues[0] = 12;
//         testValues[0] = true;
//         expectedValues[1] = 10;
//         testValues[1] = true;
//         expectedValues[2] = 12;
//         testValues[2] = true;
//         expectedValues[4] = 12;
//         testValues[4] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void test_MVov() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.mov32BitConstant(Aarch64.cpuRegisters[0], 12);
//         asm.mov32BitConstant(Aarch64.cpuRegisters[1], 10);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, Aarch64.r0, null, CiKind.Float, CiKind.Int);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r2, Aarch64.s0, null, CiKind.Int, CiKind.Float);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.s5, Aarch64.r0, null, CiKind.Float, CiKind.Int);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r4, Aarch64.s5, null, CiKind.Int, CiKind.Float);
//         expectedValues[0] = 12;
//         testValues[0] = true;
//         expectedValues[1] = 10;
//         testValues[1] = true;
//         expectedValues[2] = 12;
//         testValues[2] = true;
//         expectedValues[4] = 12;
//         testValues[4] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     //TODO: Fix vmovimm
//     public void broken_vmovimm() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.vmovImm(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, 1, CiKind.Double);
//         asm.vmovImm(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, 0, CiKind.Double);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r0, Aarch64.s0, null, CiKind.Int, CiKind.Double);
//         asm.vmovImm(Aarch64Assembler.ConditionFlag.Always, Aarch64.s1, -100, CiKind.Double);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r2, Aarch64.s1,null,  CiKind.Int, CiKind.Double);
//         expectedValues[0] = 0;
//         testValues[0] = true;
//         expectedValues[2] = -100;
//         testValues[2] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void ignore_FloatIngPointExperiments() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.mov32BitConstant(Aarch64.cpuRegisters[0], 12);
//         asm.mov32BitConstant(Aarch64.cpuRegisters[1], 10);
//         asm.codeBuffer.emitInt(0xee000a10);
//         asm.codeBuffer.emitInt(0xee001a90);
//         asm.codeBuffer.emitInt(0xeeb81ac0);
//         asm.codeBuffer.emitInt(0xeef81ae0);
//         asm.codeBuffer.emitInt(0xee210a21);
//         asm.codeBuffer.emitInt(0xeebd0a40);
//         asm.codeBuffer.emitInt(0xee100a10);
//         expectedValues[0] = 120;
//         testValues[0] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void ignore_SubReg() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         for (int i = 0; i < 5; i++) {
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
//         }
//         for (int i = 0; i < 5; i++) {
//             asm.sub(Aarch64Assembler.ConditionFlag.Always, false, Aarch64.cpuRegisters[i + 5], Aarch64.cpuRegisters[5 - (i + 1)], Aarch64.cpuRegisters[i], 0, 0);
//             expectedValues[i + 5] = expectedValues[5 - (i + 1)] - expectedValues[i];
//             testValues[i + 5] = true;
//         }
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void ignore_Mov() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         for (int i = 0; i < 5; i++) {
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
//             asm.mov(Aarch64Assembler.ConditionFlag.Always, true, Aarch64.cpuRegisters[i + 5], Aarch64.cpuRegisters[i]);
//             expectedValues[i + 5] = expectedValues[i];
//             testValues[i] = true;
//             testValues[i + 5] = true;
//         }
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void ignore_Sub() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         for (int i = 0; i < 10; i++) {
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
//             asm.sub(Aarch64Assembler.ConditionFlag.Always, true, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], i * 2, 0);
//             expectedValues[i] = expectedValues[i] - i * 2;
//             testValues[i] = true;
//         }
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void ignore_Str() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.mov32BitConstant(Aarch64.cpuRegisters[12], 0);
//         for (int i = 0; i < 10; i++) {
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
//             testValues[i] = true;
//             asm.str(Aarch64Assembler.ConditionFlag.Always, 1, 0, 0, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[13], Aarch64.cpuRegisters[12], i * 4, 0);
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i], -2 * (expectedValues[i]));
//             asm.ldr(Aarch64Assembler.ConditionFlag.Always, 1, 0, 0, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[13], Aarch64.cpuRegisters[12], i * 4, 0);
//         }
//         generateAndTest( expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void ignore_Ldr() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         for (int i = 0; i < 10; i++) {
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
//             testValues[i] = true;
//         }
//         asm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
//         for (int i = 0; i < 10; i++) {
//             asm.add(Aarch64Assembler.ConditionFlag.Always, false, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], i * 2, 0);
//             asm.movw(Aarch64Assembler.ConditionFlag.Always, Aarch64.r12, i * 4);
//             asm.movt(Aarch64Assembler.ConditionFlag.Always, Aarch64.r12, 0);
//             asm.ldr(Aarch64Assembler.ConditionFlag.Always, 1, 1, 0, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[13], Aarch64.cpuRegisters[12], 0, 0);
//         }
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void ignore_Decq() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         for (int i = 0; i < 10; i++) {
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
//             asm.decq(Aarch64.cpuRegisters[i]);
//             expectedValues[i] -= 1;
//             testValues[i] = true;
//         }
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void ignore_Incq() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         for (int i = 0; i < 10; i++) {
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
//             asm.incq(Aarch64.cpuRegisters[i]);
//             expectedValues[i] += 1;
//             testValues[i] = true;
//         }
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void ignore_Subq() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         for (int i = 0; i < 10; i++) {
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
//             if (i % 2 == 1) {
//                 asm.subq(Aarch64.cpuRegisters[i], 2 * expectedValues[i]);
//                 expectedValues[i] -= 2 * expectedValues[i];
//             } else {
//                 asm.subq(Aarch64.cpuRegisters[i], expectedValues[i]);
//                 expectedValues[i] -= expectedValues[i];
//             }
//             testValues[i] = true;
//         }
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void ignore_addq() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         for (int i = 0; i < 10; i++) {
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
//             asm.addq(Aarch64.cpuRegisters[i], expectedValues[i]);
//             expectedValues[i] += expectedValues[i];
//             testValues[i] = true;
//         }
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void ignore_Ldrsh() throws Exception {
//         int[] testval = { 0x03020100, 0x8fed9ba9};
//         int mask = 0xffff;
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         // load r0 and r1 with sensible values for ignoring the loading of bytes.
//         asm.mov32BitConstant(Aarch64.cpuRegisters[0], testval[0]);
//         asm.mov32BitConstant(Aarch64.cpuRegisters[1], testval[1]);
//         asm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2); // values now lie on the stack
//         // we now try to extract the "signed halfwords"
//         // from the stack and place them into r0..r3
//         for (int i = 0; i < 4; i++) {
//             // in this test we are using the stack register as the base value!
//             asm.ldrshw(Aarch64Assembler.ConditionFlag.Always, 1, 1, 0, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[13], i * 2);
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
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void ignore_StrdAndLdrd() throws Exception {
//         MaxineAarch64Tester.disableDebug();
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         for (int i = 0; i < 10; i += 2) {
//             asm.codeBuffer.reset();
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i + 1], expectedValues[i + 1]);
//             asm.strd(Aarch64Assembler.ConditionFlag.Always, Aarch64.cpuRegisters[i], Aarch64.r13, 0);
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i], 0);
//             asm.mov32BitConstant(Aarch64.cpuRegisters[i + 1], 0);
//             asm.ldrd(Aarch64Assembler.ConditionFlag.Always, Aarch64.cpuRegisters[i], Aarch64.r13, 0);
//             testValues[i] = true;
//             testValues[i + 1] = true;
//             if (i != 0) {
//                 testValues[i - 1] = false;
//                 testValues[i - 2] = false;
//             }
//             generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//         }
//     }
//
//     public void ignore_PushAndPop() throws Exception {
//         int registers = 1;
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         initialiseExpectedValues();
//         for (int i = 0; i < 16; i++) {
//             if (i % 2 == 0) {
//                 expectedValues[i] = i;
//             } else {
//                 expectedValues[i] = -i;
//             }
//             if (i < 13) {
//                 testValues[i] = true;
//             }
//         }
//
//         for (int bitmask = 1; bitmask <= 0xfff; bitmask = bitmask | (bitmask + 1), registers++) {
//             asm.codeBuffer.reset();
//             for (int i = 0; i < 13; i++) { // we are not breaking the stack (r13)
//                 asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]); // 2 instructions movw, movt
//                 // all registers initialized.
//             }
//             asm.push(Aarch64Assembler.ConditionFlag.Always, bitmask); // store all registers referred to
//             // by bitmask on the stack
//             for (int i = 0; i < 13; i++) {
//                 asm.add(Aarch64Assembler.ConditionFlag.Always, false, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 1, 0);
//             }
//             // r0..r12 should now all have +1 more than their previous values stored on the stack
//             // restore the same registers that were placed on the stack
//             asm.pop(Aarch64Assembler.ConditionFlag.Always, bitmask);
//             for (int i = 0; i < 13; i++) {
//                 if (i < registers) {
//                     expectedValues[i] = expectedValues[i];
//                 } else {
//                     expectedValues[i] = expectedValues[i] + 1;
//                 }
//             }
//             generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//         }
//     }
//
//     public void ignore_MovRor() throws Exception {
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         for (int srcReg = 0; srcReg < 16; srcReg++) {
//             for (int destReg = 0; destReg < 16; destReg++) {
//                 for (int shift = 0; shift <= 31; shift++) {
//                     for (int i = 0; i < Aarch64Assembler.ConditionFlag.values().length; i++) { // test encodings
//                         asm.movror(Aarch64Assembler.ConditionFlag.values()[i], false, Aarch64.cpuRegisters[destReg], Aarch64.cpuRegisters[srcReg], shift);
//                         // rotate right two bits 0x30003fff?
//                         assertTrue(asm.codeBuffer.getInt(0) == (0x01A00060 | (shift << 7) | (destReg << 12) | srcReg | Aarch64Assembler.ConditionFlag.values()[i].value() << 28));
//                         asm.codeBuffer.reset();
//                         asm.movror(Aarch64Assembler.ConditionFlag.values()[i], true, Aarch64.cpuRegisters[destReg], Aarch64.cpuRegisters[srcReg], shift);
//                         // rotate right 30 bits? to get 0x0000ffff
//                         assertTrue(asm.codeBuffer.getInt(0) == (0x01B00060 | (shift << 7) | srcReg | (destReg << 12) | Aarch64Assembler.ConditionFlag.values()[i].value() << 28));
//                         asm.codeBuffer.reset();
//                     }
//                 }
//             }
//         }
//         int mask = 1;
//         for (int shift = 1; shift <= 31; shift++) {
//             asm.codeBuffer.reset();
//             asm.movw(Aarch64Assembler.ConditionFlag.Always, Aarch64.cpuRegisters[0], 0xffff); // load 0x0000ffff
//             asm.movt(Aarch64Assembler.ConditionFlag.Always, Aarch64.cpuRegisters[0], 0x0);
//             asm.movror(Aarch64Assembler.ConditionFlag.Always, true, Aarch64.cpuRegisters[1], Aarch64.cpuRegisters[0], shift);
//             // not ignoring ROR with ZEROshift as that needs to know the carry bit of the registerA RRX
//             asm.movror(Aarch64Assembler.ConditionFlag.Always, true, Aarch64.cpuRegisters[2], Aarch64.cpuRegisters[1], 32 - shift);
//             // rotate right 30 bits?
//             // implies ... APSR.N = , APSR.Z = , APSR.C =
//
//             expectedValues[0] = 0x0000ffff;
//             testValues[0] = true;
//             expectedValues[1] = (0x0000ffff >> shift) | (((expectedValues[0] & mask) << (32 - shift)));
//             testValues[1] = true;
//             expectedValues[2] = 0x0000ffff;
//             testValues[2] = true;
//             expectedValues[16] = 0x0;
//             testValues[16] = false;
//             setBitMask(16, MaxineAarch64Tester.BitsFlag.NZCBits);
//             generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//             mask = mask | (mask + 1);
//         }
//     }
//
//     public void ignore_Movw() throws Exception {
//         int value;
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.Lower16Bits);
//         for (int destReg = 0; destReg < 13; destReg++) {
//             resetIgnoreValues();
//             testValues[destReg] = true;
//             for (int j = 0; j < valueTestSet.length; j++) {
//                 value = valueTestSet[j];
//                 expectedValues[destReg] = value;
//                 asm.movw(Aarch64Assembler.ConditionFlag.Always, Aarch64.cpuRegisters[destReg], value);
//                 generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//                 assert asm.codeBuffer.getInt(0) == (0x03000000 | (Aarch64Assembler.ConditionFlag.Always.value() << 28) | (destReg << 12) | (value & 0xfff) | ((value & 0xf000) << 4));
//                 asm.codeBuffer.reset();
//             }
//         }
//     }
//
//     public void ignore_Movt() throws Exception {
//         int value;
//         int j;
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.Upper16Bits);
//         for (int destReg = 0; destReg < 13; destReg++) {
//             resetIgnoreValues();
//             testValues[destReg] = true;
//             for (j = 0; j < valueTestSet.length; j++) {
//                 value = valueTestSet[j];
//                 expectedValues[destReg] = (value & 0xffff) << 16;
//                 asm.movt(Aarch64Assembler.ConditionFlag.Always, Aarch64.cpuRegisters[destReg], value & 0xffff);
//                 generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//                 assert asm.codeBuffer.getInt(0) == (0x03400000 | (Aarch64Assembler.ConditionFlag.Always.value() << 28) | (destReg << 12) | (value & 0xfff) | (value & 0xf000) << 4);
//                 asm.codeBuffer.reset();
//             }
//         }
//     }
//
//     public void ignore_Flags() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.mov32BitConstant(Aarch64.cpuRegisters[0], 30);
//         asm.sub(Aarch64Assembler.ConditionFlag.Always, true, Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[0], 10, 0);
//         asm.sub(Aarch64Assembler.ConditionFlag.Always, true, Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[0], 10, 0);
//         asm.sub(Aarch64Assembler.ConditionFlag.Always, true, Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[0], 10, 0);
//         asm.sub(Aarch64Assembler.ConditionFlag.Always, true, Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[0], 10, 0);
//         expectedValues[0] = -10;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void ignore_Ldrd() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         for (int i = 0; i < expectedLongValues.length; i++) {
//             System.out.println(i + " " + expectedLongValues[i]);
//             asm.mov64BitConstant(Aarch64.cpuRegisters[i * 2], Aarch64.cpuRegisters[(i * 2) + 1], expectedLongValues[i]);
//             testValues[i] = true;
//         }
//         asm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8);
//         for (int i = 0; i < expectedLongValues.length * 2; i++) {
//              asm.mov32BitConstant(Aarch64.cpuRegisters[i],0);
//         }
//         for (int i = 0; i < expectedLongValues.length; i++) {
//             asm.movw(Aarch64Assembler.ConditionFlag.Always, Aarch64.r12, i * 8);
//             asm.movt(Aarch64Assembler.ConditionFlag.Always, Aarch64.r12, 0);
//             asm.ldrd(ConditionFlag.Always, 0, 0, 0, Aarch64.RSP.asRegister(), Aarch64.cpuRegisters[i * 2], Aarch64.r12);
//         }
//         generateAndTest(expectedLongValues, testValues, bitmasks);
//     }
//
//     public void test_casInt() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         masm.codeBuffer.reset();
//         CiRegister cmpReg = Aarch64.r0;
//         CiRegister newReg = Aarch64.r1;
//
//         // r0=10, r1=20, r2=30, r3=40, r4=50
//         for (int i = 1; i < 5; i++) {
//             masm.mov32BitConstant(Aarch64.cpuRegisters[i], (i + 1) * 10);
//         }
//         masm.mov32BitConstant(Aarch64.cpuRegisters[0], 50);
//         masm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16);
//         CiAddress addr = new CiAddress(CiKind.Int, Aarch64.r13.asValue(), 20);
//         masm.casIntAsmTest(newReg, cmpReg, addr);
//         expectedValues[1] = 20;
//         testValues[1] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
//     }
//
//     public void test_casLong() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         masm.codeBuffer.reset();
//         CiRegister cmpReg = Aarch64.r0;
//         CiRegister newReg = Aarch64.r2;
//
//         // r0=10, r1=0
//         // r2=30, r3=0
//         // r4=50, r5=0
//         // r6=70, r7=0
//         // r8=90, r9=0
//         for (int i = 2; i < 10; i += 2) {
//             masm.mov64BitConstant(Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i + 1], (i + 1) * 10);
//         }
//         masm.mov64BitConstant(Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[1], 90);
//         masm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
//         CiAddress addr = new CiAddress(CiKind.Int, Aarch64.r13.asValue(), 32);
//         masm.casLongAsmTest(newReg, cmpReg, addr);
//         expectedValues[0] = 30;
//         testValues[0] = true;
//         expectedValues[1] = 0;
//         testValues[1] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
//     }
//
//     public void test_decrementl() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         masm.codeBuffer.reset();
//         // r0=10, r1=20
//         // r2=30, r3=40
//         // r4=50, r5=60
//         // r6=70, r7=80
//         // r8=90, r9=100
//         for (int i = 0; i < 10; i++) {
//             masm.mov32BitConstant(Aarch64.cpuRegisters[i], (i + 1) * 10);
//         }
//         masm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
//         CiAddress addr = new CiAddress(CiKind.Int, Aarch64.r13.asValue(), 16);
//         masm.decrementl(addr, 10);
//         masm.pop(Aarch64Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
//         expectedValues[0] = 10;
//         testValues[0] = true;
//         expectedValues[1] = 20;
//         testValues[1] = true;
//         expectedValues[2] = 30;
//         testValues[2] = true;
//         expectedValues[3] = 40;
//         testValues[3] = true;
//         expectedValues[4] = 40;
//         testValues[4] = true;
//         expectedValues[5] = 60;
//         testValues[5] = true;
//         expectedValues[6] = 70;
//         testValues[6] = true;
//         expectedValues[7] = 80;
//         testValues[7] = true;
//         expectedValues[8] = 90;
//         testValues[8] = true;
//         expectedValues[9] = 100;
//         testValues[9] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
//     }
//
//     public void test_incrementl() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(MaxineAarch64Tester.BitsFlag.All32Bits);
//         resetIgnoreValues();
//         masm.codeBuffer.reset();
//         // r0=10, r1=20
//         // r2=30, r3=40
//         // r4=50, r5=60
//         // r6=70, r7=80
//         // r8=90, r9=100
//         for (int i = 0; i < 10; i++) {
//             masm.mov32BitConstant(Aarch64.cpuRegisters[i], (i + 1) * 10);
//         }
//         masm.push(Aarch64Assembler.ConditionFlag.AL, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
//         CiAddress addr = new CiAddress(CiKind.Int, Aarch64.r13.asValue(), 16);
//         masm.incrementl(addr, 10);
//         masm.pop(Aarch64Assembler.ConditionFlag.AL, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
//         expectedValues[0] = 10;
//         testValues[0] = true;
//         expectedValues[1] = 20;
//         testValues[1] = true;
//         expectedValues[2] = 30;
//         testValues[2] = true;
//         expectedValues[3] = 40;
//         testValues[3] = true;
//         expectedValues[4] = 60;
//         testValues[4] = true;
//         expectedValues[5] = 60;
//         testValues[5] = true;
//         expectedValues[6] = 70;
//         testValues[6] = true;
//         expectedValues[7] = 80;
//         testValues[7] = true;
//         expectedValues[8] = 90;
//         testValues[8] = true;
//         expectedValues[9] = 100;
//         testValues[9] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
//     }
}
