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
package test.armv7.asm;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.asm.target.armv7.ARMV7Assembler.*;
import com.sun.cri.ci.*;
import com.sun.max.ide.*;

public class ARMV7AssemblerTest extends MaxTestCase {

    private ARMV7Assembler asm;
    private ARMV7MacroAssembler masm;
    private CiTarget armv7;
    private ARMCodeWriter code;

    static final class Pair {

        public final int first;
        public final int second;

        Pair(int first, int second) {
            this.first = first;
            this.second = second;
        }
    }

    public ARMV7AssemblerTest() {
        armv7 = new CiTarget(new ARMV7(), true, 8, 0, 4096, 0, false, false, false, true);
        asm = new ARMV7Assembler(armv7, null);
        masm = new ARMV7MacroAssembler(armv7, null);
        code = null;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ARMV7AssemblerTest.class);
    }

    private static int[] valueTestSet = {0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65535};
    private static int[] scratchTestSet = {0, 1, 0xff, 0xffff, 0xffffff, 0xfffffff, 0x7fffffff};
    private static boolean[] testValues = new boolean[MaxineARMv7Tester.NUM_REGS];

    // Each test should set the contents of this array appropriately,
    // it enables the instruction under test to select the specific bit values for
    // comparison i.e. for example ignoring upper or lower 16bits for movt, movw
    // and for ignoring specific bits in the status register etc
    // concerning whether a carry has been set
    private static MaxineARMv7Tester.BitsFlag[] bitmasks = {MaxineARMv7Tester.BitsFlag.All32Bits, MaxineARMv7Tester.BitsFlag.All32Bits, MaxineARMv7Tester.BitsFlag.All32Bits,
        MaxineARMv7Tester.BitsFlag.All32Bits, MaxineARMv7Tester.BitsFlag.All32Bits, MaxineARMv7Tester.BitsFlag.All32Bits, MaxineARMv7Tester.BitsFlag.All32Bits, MaxineARMv7Tester.BitsFlag.All32Bits,
        MaxineARMv7Tester.BitsFlag.All32Bits, MaxineARMv7Tester.BitsFlag.All32Bits, MaxineARMv7Tester.BitsFlag.All32Bits, MaxineARMv7Tester.BitsFlag.All32Bits, MaxineARMv7Tester.BitsFlag.All32Bits,
        MaxineARMv7Tester.BitsFlag.All32Bits, MaxineARMv7Tester.BitsFlag.All32Bits, MaxineARMv7Tester.BitsFlag.All32Bits, MaxineARMv7Tester.BitsFlag.All32Bits};

    private static void setBitMask(int i, MaxineARMv7Tester.BitsFlag mask) {
        bitmasks[i] = mask;
    }

    private static void setAllBitMasks(MaxineARMv7Tester.BitsFlag mask) {
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
    private static int[] expectedValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    private static long[] expectedLongValues = {Long.MAX_VALUE - 100, Long.MAX_VALUE};

    private static void initialiseExpectedValues() {
        for (int i = 0; i < MaxineARMv7Tester.NUM_REGS; i++) {
            expectedValues[i] = i;
        }
    }

    private static void initialiseTestValues() {
        for (int i = 0; i < MaxineARMv7Tester.NUM_REGS; i++) {
            testValues[i] = false;
        }
    }

    private void generateAndTest(int[] expected, boolean[] tests, MaxineARMv7Tester.BitsFlag[] masks, Buffer codeBuffer) throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(codeBuffer);
        code.createCodeFile();
        MaxineARMv7Tester r = new MaxineARMv7Tester(expected, tests, masks);
        if (!MaxineARMv7Tester.ENABLE_SIMULATOR) {
            System.out.println("Code Generation is disabled!");
            return;
        }
        r.assembleStartup();
        r.compile();
        r.link();
        r.runSimulation();
        r.reset();
    }

    private int[] generate() throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(asm.codeBuffer);
        code.createCodeFile();
        int[] retArr;
        MaxineARMv7Tester r = new MaxineARMv7Tester();
        if (!MaxineARMv7Tester.ENABLE_SIMULATOR) {
            System.out.println("Code Generation is disabled!");
            return null;
        }
        r.assembleStartup();
        r.compile();
        r.link();
        retArr = r.runSimulationRegisters();
        r.reset();
        return retArr;
    }

    private void generateAndTest(long[] expected, boolean[] tests, MaxineARMv7Tester.BitsFlag[] masks) throws Exception {
        ARMCodeWriter code = new ARMCodeWriter(asm.codeBuffer);
        code.createCodeFile();
        MaxineARMv7Tester r = new MaxineARMv7Tester(expected, tests, masks);
        if (!MaxineARMv7Tester.ENABLE_SIMULATOR) {
            System.out.println("Code Generation is disabled!");
            return;
        }
        r.assembleStartup();
        r.compile();
        r.link();
        r.runSimulation();
        r.reset();
    }

    public void work_Ldrb() throws Exception {
        int[] testval = {0x03020100, 0xffedcba9};
        int mask = 0xff;
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // load r0 and r1 with sensible values for ignoring the loading of bytes.
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], testval[0]);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], testval[1]);
        asm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2); // values now lie on the stack
        for (int i = 0; i < 8; i++) {
            // stackpointer advanced by 8
            asm.ldrb(ARMV7Assembler.ConditionFlag.Always, 1, 1, 0, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[13], i);
            testValues[i] = true;
            if (i < 4) {
                expectedValues[i] = testval[0] & (mask << (8 * (i % 4)));
            } else {
                expectedValues[i] = testval[1] & (mask << (8 * (i % 4)));
            }
            expectedValues[i] = expectedValues[i] >> 8 * (i % 4);
            if (expectedValues[i] < 0) {
                expectedValues[i] = 0x100 + expectedValues[i];
            }
            // Bytes do not have a sign! So we need to make sure the expectedValues are
            // not affected by sign extension side effects when we take the MSByte of
            // an integer.
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);

    }

    public long connectRegs(int reg0, int reg1) {
        long returnVal = 0;
        if (reg1 < 0) {
            returnVal = ((long) reg1) << 32;
            if (reg0 < 0) {
                returnVal += Long.parseLong(String.format("%32s", Integer.toBinaryString(reg0)).replace(' ', '0'), 2);
            } else {
                returnVal += reg0;
            }
        } else {
            returnVal = ((long) reg1) << 32;
            if (reg1 == 0) {
                returnVal += reg0;
            } else if (reg0 < 0) {
                returnVal += Long.parseLong(String.format("%32s", Integer.toBinaryString(reg0)).replace(' ', '0'), 2);
            } else {
                returnVal += reg0;
            }
        }
        return returnVal;
    }

    public void work_mov64BitConstant() throws Exception {
        int[] instructions = new int[6];
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        long[] values = new long[10];
        values[0] = 0L;
        values[1] = -1L;
        values[2] = Integer.MIN_VALUE;
        values[3] = Integer.MAX_VALUE;
        values[4] = Long.MAX_VALUE;
        values[5] = Long.MIN_VALUE;
        values[6] = Long.MIN_VALUE + 5;
        values[7] = Long.MAX_VALUE - 5;
        values[8] = (Integer.MIN_VALUE) + 5L;
        values[9] = (Integer.MAX_VALUE) - 5L;
        int[] registers = null;
        for (int i = 0; i < values.length; i++) {
            asm.codeBuffer.reset();
            asm.movImm64(ConditionFlag.Always, ARMV7.r0, ARMV7.r1, values[i]);
            instructions[0] = asm.codeBuffer.getInt(0);
            instructions[1] = asm.codeBuffer.getInt(4);
            instructions[2] = asm.codeBuffer.getInt(8);
            instructions[3] = asm.codeBuffer.getInt(12);
            registers = generate();
            assert values[i] == connectRegs(registers[0], registers[1]);
        }
    }

    public void work_AddConstant() throws Exception {
        int[] instructions = new int[3];
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseExpectedValues();
        resetIgnoreValues();
        for (int srcReg = 0; srcReg < 3; srcReg++) {
            for (int destReg = 0; destReg < 3; destReg++) {
                initialiseTestValues();
                testValues[destReg] = true;
                for (int i = 0; i < scratchTestSet.length; i++) {
                    asm.codeBuffer.reset();
                    int value = scratchTestSet[i];
                    asm.movw(ARMV7Assembler.ConditionFlag.Always, ARMV7.cpuRegisters[srcReg], value & 0xffff);
                    asm.movt(ARMV7Assembler.ConditionFlag.Always, ARMV7.cpuRegisters[srcReg], (value >> 16) & 0xffff);
                    asm.add12BitImmediate(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.cpuRegisters[destReg], ARMV7.cpuRegisters[srcReg], 0);
                    instructions[0] = asm.codeBuffer.getInt(0);
                    instructions[1] = asm.codeBuffer.getInt(4);
                    instructions[2] = asm.codeBuffer.getInt(8);
                    expectedValues[destReg] = value;
                    generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
                }
            }
        }
    }

    public void work_VPushPop() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 5; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], i);
            asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.allRegisters[i + 16], ARMV7.cpuRegisters[i], null, CiKind.Float, CiKind.Int);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], -i);
        }
        asm.vpush(ARMV7Assembler.ConditionFlag.Always, ARMV7.allRegisters[16], ARMV7.allRegisters[16 + 4], CiKind.Float, CiKind.Float);
        for (int i = 0; i < 5; i++) {
            asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.allRegisters[i + 16], ARMV7.cpuRegisters[i], null, CiKind.Float, CiKind.Int);
        }
        asm.vpop(ARMV7Assembler.ConditionFlag.Always, ARMV7.allRegisters[16], ARMV7.allRegisters[16 + 4], CiKind.Float, CiKind.Float);
        for (int i = 0; i < 5; i++) {
            asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.cpuRegisters[i], ARMV7.allRegisters[i + 16], null, CiKind.Int, CiKind.Float);
        }
        expectedValues[0] = 0;
        testValues[0] = true;
        expectedValues[1] = 1;
        testValues[1] = true;
        expectedValues[2] = 2;
        testValues[2] = true;
        expectedValues[3] = 3;
        testValues[3] = true;
        expectedValues[4] = 4;
        testValues[4] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_Vdiv() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 10);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], 24);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s1, ARMV7.r1, null, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s2, false, true, ARMV7.s0, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s3, false, true, ARMV7.s1, CiKind.Float, CiKind.Int);
        asm.vdiv(ARMV7Assembler.ConditionFlag.Always, ARMV7.s2, ARMV7.s3, ARMV7.s2, CiKind.Float);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, true, true, ARMV7.s2, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s0, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 10;
        testValues[0] = true;
        expectedValues[1] = 24;
        testValues[1] = true;
        expectedValues[2] = 2;
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_Vcvt_int2float() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 10);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], 24);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s1, ARMV7.r1, null, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s2, false, true, ARMV7.s0, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s3, false, true, ARMV7.s1, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s2, true, true, ARMV7.s2, CiKind.Float, CiKind.Float);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s3, true, true, ARMV7.s3, CiKind.Float, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r0, ARMV7.s2, null, CiKind.Int, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r1, ARMV7.s3, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 10;
        testValues[0] = true;
        expectedValues[1] = 24;
        testValues[1] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_Vcvt_int2double() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 10);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], 24);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s1, ARMV7.r1, null, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s2, false, true, ARMV7.s0, CiKind.Double, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s3, false, true, ARMV7.s1, CiKind.Double, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, true, true, ARMV7.s2, CiKind.Float, CiKind.Double);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s1, true, true, ARMV7.s3, CiKind.Float, CiKind.Double);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r0, ARMV7.s0, null, CiKind.Int, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s1, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 10;
        testValues[0] = true;
        expectedValues[1] = 24;
        testValues[1] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_Vcvt_double2float() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImm64(ConditionFlag.Always, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[1], Double.doubleToRawLongBits(-10));
        asm.movImm64(ConditionFlag.Always, ARMV7.cpuRegisters[2], ARMV7.cpuRegisters[3], Double.doubleToRawLongBits(-24));
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r0, ARMV7.r1, CiKind.Double, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s1, ARMV7.r2, ARMV7.r3, CiKind.Double, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s4, false, true, ARMV7.s0, CiKind.Float, CiKind.Double);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s5, false, true, ARMV7.s1, CiKind.Float, CiKind.Double);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r0, ARMV7.s4, null, CiKind.Int, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s5, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = Float.floatToRawIntBits(-10);
        testValues[0] = true;
        expectedValues[2] = Float.floatToRawIntBits(-24);
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_VAdd() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 12);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], 10);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s1, ARMV7.r1, null, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s2, false, false, ARMV7.s0, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s4, false, false, ARMV7.s1, CiKind.Float, CiKind.Int);
        asm.vadd(ARMV7Assembler.ConditionFlag.Always, ARMV7.s2, ARMV7.s4, ARMV7.s2, CiKind.Float);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, true, true, ARMV7.s2, CiKind.Float, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s0, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = 22;
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_VSub() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 12);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], 10);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s1, ARMV7.r1, null, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s2, false, false, ARMV7.s0, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s4, false, false, ARMV7.s1, CiKind.Float, CiKind.Int);
        asm.vsub(ARMV7Assembler.ConditionFlag.Always, ARMV7.s2, ARMV7.s4, ARMV7.s2, CiKind.Float);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, true, true, ARMV7.s2, CiKind.Float, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s0, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = -2;
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_VcvtvMul() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 12);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], 10);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s1, ARMV7.r1, null, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s2, false, false, ARMV7.s0, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s4, false, false, ARMV7.s1, CiKind.Float, CiKind.Int);
        asm.vmul(ARMV7Assembler.ConditionFlag.Always, ARMV7.s2, ARMV7.s4, ARMV7.s2, CiKind.Float);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, true, false, ARMV7.s2, CiKind.Float, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s0, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = 120;
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_VldrStr() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 12);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], 10);
        asm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512 | 1024 | 2048); // instruction
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r13, 0, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s0, null, CiKind.Int, CiKind.Float);
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.s4, ARMV7.r13, 0, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r4, ARMV7.s4, null, CiKind.Int, CiKind.Float);
        asm.vstr(ARMV7Assembler.ConditionFlag.Always, ARMV7.s4, ARMV7.r13, -8, CiKind.Float, CiKind.Int);
        asm.vstr(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r13, -16, CiKind.Float, CiKind.Int);
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.s10, ARMV7.r13, -8, CiKind.Float, CiKind.Int);
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.s31, ARMV7.r13, -16, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r6, ARMV7.s10, null, CiKind.Int, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, ARMV7.s31, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = 12;
        testValues[2] = true;
        expectedValues[4] = 12;
        testValues[4] = true;
        expectedValues[6] = 12;
        testValues[6] = true;
        expectedValues[8] = 12;
        testValues[8] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_Vldr() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 12);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], 10);
        asm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512); // 1 instruction
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.s31, ARMV7.r13, 0, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s31, null, CiKind.Int, CiKind.Float);
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.s4, ARMV7.r13, 0, CiKind.Int, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r4, ARMV7.s4, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = 12;
        testValues[2] = true;
        expectedValues[4] = 12;
        testValues[4] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_MVov() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 12);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], 10);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s0, null, CiKind.Int, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s5, ARMV7.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r4, ARMV7.s5, null, CiKind.Int, CiKind.Float);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = 12;
        testValues[2] = true;
        expectedValues[4] = 12;
        testValues[4] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    // TODO: Fix vmovimm
    public void broken_vmovimm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.vmovImm(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, 1, CiKind.Double);
        asm.vmovImm(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, 0, CiKind.Double);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r0, ARMV7.s0, null, CiKind.Int, CiKind.Double);
        asm.vmovImm(ARMV7Assembler.ConditionFlag.Always, ARMV7.s1, -100, CiKind.Double);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s1, null, CiKind.Int, CiKind.Double);
        expectedValues[0] = 0;
        testValues[0] = true;
        expectedValues[2] = -100;
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_FloatIngPointExperiments() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 12);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], 10);
        asm.codeBuffer.emitInt(0xee000a10);
        asm.codeBuffer.emitInt(0xee001a90);
        asm.codeBuffer.emitInt(0xeeb81ac0);
        asm.codeBuffer.emitInt(0xeef81ae0);
        asm.codeBuffer.emitInt(0xee210a21);
        asm.codeBuffer.emitInt(0xeebd0a40);
        asm.codeBuffer.emitInt(0xee100a10);
        expectedValues[0] = 120;
        testValues[0] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_SubReg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 5; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedValues[i]);
        }
        for (int i = 0; i < 5; i++) {
            asm.sub(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.cpuRegisters[i + 5], ARMV7.cpuRegisters[5 - (i + 1)], ARMV7.cpuRegisters[i], 0, 0);
            expectedValues[i + 5] = expectedValues[5 - (i + 1)] - expectedValues[i];
            testValues[i + 5] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_Mov() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 5; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedValues[i]);
            asm.mov(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[i + 5], ARMV7.cpuRegisters[i]);
            expectedValues[i + 5] = expectedValues[i];
            testValues[i] = true;
            testValues[i + 5] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_Sub() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedValues[i]);
            asm.sub(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[i], i * 2, 0);
            expectedValues[i] = expectedValues[i] - i * 2;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_Str() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[12], 0);
        for (int i = 0; i < 10; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedValues[i]);
            testValues[i] = true;
            asm.str(ARMV7Assembler.ConditionFlag.Always, 1, 0, 0, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[13], ARMV7.cpuRegisters[12], i * 4, 0);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], -2 * (expectedValues[i]));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, 1, 0, 0, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[13], ARMV7.cpuRegisters[12], i * 4, 0);
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_neg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        expectedValues[1] = -1;
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 32);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], -1);
        testValues[1] = true;
        asm.xorq(ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0]);
        asm.mvn(ConditionFlag.Always, false, ARMV7.cpuRegisters[1], ARMV7.cpuRegisters[0], 0);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_msb_int() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        int[] input = new int[] {0, 1, 2, -1, 61440};
        int[] output = new int[] {-1, 0, 1, 31, 15};
        for (int i = 0; i < input.length; i++) {
            resetIgnoreValues();
            expectedValues[0] = output[i];
            testValues[0] = true;
            asm.codeBuffer.reset();
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], input[i]);
            asm.cmpImmediate(ConditionFlag.Always, ARMV7.cpuRegisters[0], 0);
            asm.movImm32(ConditionFlag.Equal, ARMV7.cpuRegisters[0], -1);
            asm.jcc(ConditionFlag.Equal, 40, false);
            asm.clz(ConditionFlag.Always, ARMV7.cpuRegisters[1], ARMV7.cpuRegisters[0]);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 31);
            asm.sub(ConditionFlag.Always, false, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[1], 0, 0);
            generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
        }
    }

    public void work_lsb_int() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        int[] input = new int[] {1, 2, 0, -1, 61440};
        int[] output = new int[] {0, 1, -1, 0, 12};
        for (int i = 0; i < input.length; i++) {
            resetIgnoreValues();
            expectedValues[0] = output[i];
            testValues[0] = true;
            asm.codeBuffer.reset();
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], input[i]);
            asm.cmpImmediate(ConditionFlag.Always, ARMV7.cpuRegisters[0], 0);
            asm.movImm32(ConditionFlag.Equal, ARMV7.cpuRegisters[0], -1);
            asm.jcc(ConditionFlag.Equal, 32, false);
            asm.rbit(ConditionFlag.Always, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0]);
            asm.clz(ConditionFlag.Always, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0]);
            generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
        }
    }

    public void work_msb_long() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        long[] input = new long[] {0L, -1L, 2147483648L, 4294967296L};
        int[] output = new int[] {-1, 63, 31, 32};
        for (int i = 0; i < input.length; i++) {
            resetIgnoreValues();
            expectedValues[0] = output[i];
            testValues[0] = true;
            asm.codeBuffer.reset();
            asm.movImm64(ConditionFlag.Always, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[1], input[i]); // 12
            asm.cmpImmediate(ConditionFlag.Always, ARMV7.cpuRegisters[1], 0); // 16
            asm.jcc(ConditionFlag.NotEqual, 40, false); // 20
            asm.cmpImmediate(ConditionFlag.Equal, ARMV7.cpuRegisters[0], 0); // 24
            asm.movImm32(ConditionFlag.Equal, ARMV7.cpuRegisters[0], -1); // 32
            asm.jcc(ConditionFlag.Equal, 84, false); // 36
            asm.clz(ConditionFlag.Always, ARMV7.cpuRegisters[2], ARMV7.cpuRegisters[1]); // 40
            asm.cmpImmediate(ConditionFlag.Always, ARMV7.cpuRegisters[2], 32); // 44
            asm.jcc(ConditionFlag.Equal, 72, false); // 48
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 63); // 56
            asm.sub(ConditionFlag.Always, false, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[2], 0, 0); // 60
            asm.nop();
            asm.jcc(ConditionFlag.Always, 84, false); // 68
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 31); // 72
            asm.sub(ConditionFlag.Always, false, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[1], 0, 0); // 80
            generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
        }
    }

    public void work_lsb_long() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        long[] input = new long[] {0L, 1L, 2L, -1L, 61440L, 2147483648L, 4294967296L};
        int[] output = new int[] {-1, 0, 1, 0, 12, 31, 32};
        for (int i = 0; i < input.length; i++) {
            resetIgnoreValues();
            expectedValues[0] = output[i];
            testValues[0] = true;
            asm.codeBuffer.reset();
            asm.movImm64(ConditionFlag.Always, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[1], input[i]); // 12
            asm.cmpImmediate(ConditionFlag.Always, ARMV7.cpuRegisters[1], 0); // 16
            asm.jcc(ConditionFlag.NotEqual, 40, false); // 20
            asm.cmpImmediate(ConditionFlag.Always, ARMV7.cpuRegisters[0], 0); // 24
            asm.movImm32(ConditionFlag.Equal, ARMV7.cpuRegisters[0], -1); // 32
            asm.jcc(ConditionFlag.Equal, 84, false); // 36
            asm.rbit(ConditionFlag.Always, ARMV7.cpuRegisters[2], ARMV7.cpuRegisters[0]); // 40
            asm.rbit(ConditionFlag.Always, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[1]); // 44
            asm.mov(ConditionFlag.Always, false, ARMV7.cpuRegisters[1], ARMV7.cpuRegisters[2]); // 48
            asm.clz(ConditionFlag.Always, ARMV7.cpuRegisters[2], ARMV7.cpuRegisters[1]); // 52
            asm.cmpImmediate(ConditionFlag.Always, ARMV7.cpuRegisters[2], 32); // 56
            asm.mov(ConditionFlag.NotEqual, false, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[2]); // 60
            asm.jcc(ConditionFlag.NotEqual, 84, false); // 64
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[2], 32); // 72
            asm.clz(ConditionFlag.Always, ARMV7.cpuRegisters[1], ARMV7.cpuRegisters[0]); // 76
            asm.addRegisters(ConditionFlag.Always, false, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[2], ARMV7.cpuRegisters[1], 0, 0); // 80
            generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
        }
    }

    public void work_Ldr() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedValues[i]);
            testValues[i] = true;
        }
        asm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        for (int i = 0; i < 10; i++) {
            asm.add12BitImmediate(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[i], i * 2);
            asm.movw(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, i * 4);
            asm.movt(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, 0);
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, 1, 1, 0, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[13], ARMV7.cpuRegisters[12], 0, 0);
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_Decq() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedValues[i]);
            asm.decq(ARMV7.cpuRegisters[i]);
            expectedValues[i] -= 1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_Incq() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedValues[i]);
            asm.incq(ARMV7.cpuRegisters[i]);
            expectedValues[i] += 1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_Subq() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedValues[i]);
            if (i % 2 == 1) {
                asm.subq(ARMV7.cpuRegisters[i], 2 * expectedValues[i]);
                expectedValues[i] -= 2 * expectedValues[i];
            } else {
                asm.subq(ARMV7.cpuRegisters[i], expectedValues[i]);
                expectedValues[i] -= expectedValues[i];
            }
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_addq() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedValues[i]);
            asm.addq(ARMV7.cpuRegisters[i], expectedValues[i]);
            expectedValues[i] += expectedValues[i];
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_Ldrsh() throws Exception {
        int[] testval = {0x03020100, 0x8fed9ba9};
        int mask = 0xffff;
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // load r0 and r1 with sensible values for ignoring the loading of bytes.
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], testval[0]);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], testval[1]);
        asm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2); // values now lie on the stack
        // we now try to extract the "signed halfwords"
        // from the stack and place them into r0..r3
        for (int i = 0; i < 4; i++) {
            // in this test we are using the stack register as the base value!
            asm.ldrshw(ARMV7Assembler.ConditionFlag.Always, 1, 1, 0, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[13], i * 2);
            testValues[i] = true;
            if (i < 2) {
                expectedValues[i] = testval[0] & (mask << (16 * (i % 2)));
            } else {
                expectedValues[i] = testval[1] & (mask << (16 * (i % 2)));
            }
            if ((expectedValues[i] & 0x8000) != 0) {
                expectedValues[i] = expectedValues[i] - 0x10000; // sign extension workaround.
            }
            expectedValues[i] = expectedValues[i] >> 16 * (i % 2);
        }

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_StrdAndLdrd() throws Exception {
        MaxineARMv7Tester.disableDebug();
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        for (int i = 0; i < 10; i += 2) {
            asm.codeBuffer.reset();
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedValues[i]);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i + 1], expectedValues[i + 1]);
            asm.strd(ARMV7Assembler.ConditionFlag.Always, ARMV7.cpuRegisters[i], ARMV7.r13, 0);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], 0);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i + 1], 0);
            asm.ldrd(ARMV7Assembler.ConditionFlag.Always, ARMV7.cpuRegisters[i], ARMV7.r13, 0);
            testValues[i] = true;
            testValues[i + 1] = true;
            if (i != 0) {
                testValues[i - 1] = false;
                testValues[i - 2] = false;
            }
            generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
        }
    }

    public void work_PushAndPop() throws Exception {
        int registers = 1;
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseExpectedValues();
        for (int i = 0; i < 16; i++) {
            if (i % 2 == 0) {
                expectedValues[i] = i;
            } else {
                expectedValues[i] = -i;
            }
            if (i < 13) {
                testValues[i] = true;
            }
        }

        for (int bitmask = 1; bitmask <= 0xfff; bitmask = bitmask | (bitmask + 1), registers++) {
            asm.codeBuffer.reset();
            for (int i = 0; i < 13; i++) { // we are not breaking the stack (r13)
                asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedValues[i]); // 2 instructions
            }
            asm.push(ARMV7Assembler.ConditionFlag.Always, bitmask); // store all registers referred to
            // by bitmask on the stack
            for (int i = 0; i < 13; i++) {
                asm.add12BitImmediate(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[i], 1);
            }
            // r0..r12 should now all have +1 more than their previous values stored on the stack
            // restore the same registers that were placed on the stack
            asm.pop(ARMV7Assembler.ConditionFlag.Always, bitmask);
            for (int i = 0; i < 13; i++) {
                if (i < registers) {
                    expectedValues[i] = expectedValues[i];
                } else {
                    expectedValues[i] = expectedValues[i] + 1;
                }
            }
            generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
        }
    }

    public void work_MovRor() throws Exception {
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        for (int srcReg = 0; srcReg < 16; srcReg++) {
            for (int destReg = 0; destReg < 16; destReg++) {
                for (int shift = 0; shift <= 31; shift++) {
                    for (int i = 0; i < ARMV7Assembler.ConditionFlag.values().length; i++) { // test encodings
                        asm.movror(ARMV7Assembler.ConditionFlag.values()[i], false, ARMV7.cpuRegisters[destReg], ARMV7.cpuRegisters[srcReg], shift);
                        assertTrue(asm.codeBuffer.getInt(0) == (0x01A00060 | (shift << 7) | (destReg << 12) | srcReg | ARMV7Assembler.ConditionFlag.values()[i].value() << 28));
                        asm.codeBuffer.reset();
                        asm.movror(ARMV7Assembler.ConditionFlag.values()[i], true, ARMV7.cpuRegisters[destReg], ARMV7.cpuRegisters[srcReg], shift);
                        assertTrue(asm.codeBuffer.getInt(0) == (0x01B00060 | (shift << 7) | srcReg | (destReg << 12) | ARMV7Assembler.ConditionFlag.values()[i].value() << 28));
                        asm.codeBuffer.reset();
                    }
                }
            }
        }
        int mask = 1;
        for (int shift = 1; shift <= 31; shift++) {
            asm.codeBuffer.reset();
            asm.movw(ARMV7Assembler.ConditionFlag.Always, ARMV7.cpuRegisters[0], 0xffff);
            asm.movt(ARMV7Assembler.ConditionFlag.Always, ARMV7.cpuRegisters[0], 0x0);
            asm.movror(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[1], ARMV7.cpuRegisters[0], shift);
            asm.movror(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[2], ARMV7.cpuRegisters[1], 32 - shift);
            expectedValues[0] = 0x0000ffff;
            testValues[0] = true;
            expectedValues[1] = (0x0000ffff >> shift) | (((expectedValues[0] & mask) << (32 - shift)));
            testValues[1] = true;
            expectedValues[2] = 0x0000ffff;
            testValues[2] = true;
            expectedValues[16] = 0x0;
            testValues[16] = false;
            setBitMask(16, MaxineARMv7Tester.BitsFlag.NZCBits);
            generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
            mask = mask | (mask + 1);
        }
    }

    public void work_Movw() throws Exception {
        int value;
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.Lower16Bits);
        for (int destReg = 0; destReg < 13; destReg++) {
            resetIgnoreValues();
            testValues[destReg] = true;
            for (int j = 0; j < valueTestSet.length; j++) {
                value = valueTestSet[j];
                expectedValues[destReg] = value;
                asm.movw(ARMV7Assembler.ConditionFlag.Always, ARMV7.cpuRegisters[destReg], value);
                generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
                assert asm.codeBuffer.getInt(0) == (0x03000000 | (ARMV7Assembler.ConditionFlag.Always.value() << 28) | (destReg << 12) | (value & 0xfff) | ((value & 0xf000) << 4));
                asm.codeBuffer.reset();
            }
        }
    }

    public void work_Movt() throws Exception {
        int value;
        int j;
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.Upper16Bits);
        for (int destReg = 0; destReg < 13; destReg++) {
            resetIgnoreValues();
            testValues[destReg] = true;
            for (j = 0; j < valueTestSet.length; j++) {
                value = valueTestSet[j];
                expectedValues[destReg] = (value & 0xffff) << 16;
                asm.movt(ARMV7Assembler.ConditionFlag.Always, ARMV7.cpuRegisters[destReg], value & 0xffff);
                generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
                assert asm.codeBuffer.getInt(0) == (0x03400000 | (ARMV7Assembler.ConditionFlag.Always.value() << 28) | (destReg << 12) | (value & 0xfff) | (value & 0xf000) << 4);
                asm.codeBuffer.reset();
            }
        }
    }

    public void work_Flags() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 30);
        asm.sub(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0], 10, 0);
        asm.sub(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0], 10, 0);
        asm.sub(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0], 10, 0);
        asm.sub(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0], 10, 0);
        expectedValues[0] = -10;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void work_Ldrd() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < expectedLongValues.length; i++) {
            asm.movImm64(ConditionFlag.Always, ARMV7.cpuRegisters[i * 2], ARMV7.cpuRegisters[(i * 2) + 1], expectedLongValues[i]);
            testValues[i] = true;
        }
        asm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8);
        for (int i = 0; i < expectedLongValues.length * 2; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], 0);
        }
        for (int i = 0; i < expectedLongValues.length; i++) {
            asm.movw(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, i * 8);
            asm.movt(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, 0);
            asm.ldrd(ConditionFlag.Always, 1, 1, 0, ARMV7.RSP.asRegister(), ARMV7.cpuRegisters[i * 2], ARMV7.r12);
        }
        int[] simRegs = generate();
        for (int i = 0; i < testValues.length; i++) {
            if (testValues[i]) {
                assert expectedLongValues[i] == connectRegs(simRegs[2 * i], simRegs[(2 * i) + 1]) : "Expected " + expectedLongValues[i] + " Connected " +
                                connectRegs(simRegs[2 * i], simRegs[(2 * i) + 1]);
            }
        }
    }

    public void work_casInt() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        CiRegister cmpReg = ARMV7.r0;
        CiRegister newReg = ARMV7.r1;
        for (int i = 1; i < 5; i++) {
            masm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], (i + 1) * 10);
        }
        masm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 50);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16);
        CiAddress addr = new CiAddress(CiKind.Int, ARMV7.r13.asValue(), 20);
        masm.casIntAsmTest(newReg, cmpReg, addr);
        expectedValues[1] = 20;
        testValues[1] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void work_casLong() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        CiRegister cmpReg = ARMV7.r0;
        CiRegister newReg = ARMV7.r2;
        for (int i = 2; i < 10; i += 2) {
            masm.movImm64(ConditionFlag.Always, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[i + 1], (i + 1) * 10);
        }
        masm.movImm64(ConditionFlag.Always, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[1], 90);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        CiAddress addr = new CiAddress(CiKind.Int, ARMV7.r13.asValue(), 32);
        masm.casLongAsmTest(newReg, cmpReg, addr);
        expectedValues[0] = 30;
        testValues[0] = true;
        expectedValues[1] = 0;
        testValues[1] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void work_decrementl() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            masm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], (i + 1) * 10);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        CiAddress addr = new CiAddress(CiKind.Int, ARMV7.r13.asValue(), 16);
        masm.decrementl(addr, 10);
        masm.pop(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        expectedValues[0] = 10;
        testValues[0] = true;
        expectedValues[1] = 20;
        testValues[1] = true;
        expectedValues[2] = 30;
        testValues[2] = true;
        expectedValues[3] = 40;
        testValues[3] = true;
        expectedValues[4] = 40;
        testValues[4] = true;
        expectedValues[5] = 60;
        testValues[5] = true;
        expectedValues[6] = 70;
        testValues[6] = true;
        expectedValues[7] = 80;
        testValues[7] = true;
        expectedValues[8] = 90;
        testValues[8] = true;
        expectedValues[9] = 100;
        testValues[9] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void work_incrementl() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            masm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], (i + 1) * 10);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        CiAddress addr = new CiAddress(CiKind.Int, ARMV7.r13.asValue(), 16);
        masm.incrementl(addr, 10);
        masm.pop(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        expectedValues[0] = 10;
        testValues[0] = true;
        expectedValues[1] = 20;
        testValues[1] = true;
        expectedValues[2] = 30;
        testValues[2] = true;
        expectedValues[3] = 40;
        testValues[3] = true;
        expectedValues[4] = 60;
        testValues[4] = true;
        expectedValues[5] = 60;
        testValues[5] = true;
        expectedValues[6] = 70;
        testValues[6] = true;
        expectedValues[7] = 80;
        testValues[7] = true;
        expectedValues[8] = 90;
        testValues[8] = true;
        expectedValues[9] = 100;
        testValues[9] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }
}
