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
package com.oracle.max.vm.tests.crossisa.armv7.asm;

import static com.oracle.max.asm.target.armv7.ARMV7.*;

import com.oracle.max.vm.tests.crossisa.*;
import org.junit.*;

import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.asm.target.armv7.ARMV7Assembler.*;
import com.sun.cri.ci.*;
import com.sun.max.ide.*;

public class ARMV7AssemblerTest extends MaxTestCase {

    private ARMV7Assembler asm;
    private ARMV7MacroAssembler masm;
    private CiTarget armv7;

    public ARMV7AssemblerTest() {
        armv7 = new CiTarget(new ARMV7(), true, 8, 0, 4096, 0, false, false, false, true);
        asm = new ARMV7Assembler(armv7, null);
        masm = new ARMV7MacroAssembler(armv7, null);
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

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param cpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    private static void setExpectedValue(int cpuRegister, int expectedValue) {
        expectedIntValues[cpuRegister] = expectedValue;
    }

    private static void initialiseTestValues() {
        for (int i = 0; i < testValues.length; i++) {
            testValues[i] = false;
        }
    }

    // The following values will be updated
    // to those expected to be found in a register after simulated execution of code.
    private static int[] expectedIntValues = new int[MaxineARMv7Tester.NUM_REGS];

    private static long[] expectedLongValues = {Long.MAX_VALUE - 100, Long.MAX_VALUE};

    private static void initialiseExpectedValues() {
        for (int i = 0; i < MaxineARMv7Tester.NUM_REGS; i++) {
            expectedIntValues[i] = i;
        }
    }

    private void generateAndTest() throws Exception {
        ARMV7CodeWriter code = new ARMV7CodeWriter(asm.codeBuffer);
        code.createCodeFile();
        MaxineARMv7Tester r = new MaxineARMv7Tester(expectedIntValues, testValues, bitmasks);
        if (!CrossISATester.ENABLE_SIMULATOR) {
            System.out.println("Code Generation is disabled!");
            System.exit(1);
        }
        r.compile();
        r.runSimulation();
        if (!r.validateIntRegisters()) {
            r.reset();
            assert false : "Error while validating int registers";
        }
        r.reset();
    }

    private int[] generate() throws Exception {
        ARMV7CodeWriter code = new ARMV7CodeWriter(asm.codeBuffer);
        code.createCodeFile();
        MaxineARMv7Tester r = new MaxineARMv7Tester();
        if (!CrossISATester.ENABLE_SIMULATOR) {
            System.out.println("Code Generation is disabled!");
            System.exit(1);
        }
        r.compile();
        r.runSimulation();
        r.reset();
        return r.getSimulatedIntRegisters();
    }

    public void test_Ldrb() throws Exception {
        int[] testval = {0x03020100, 0xffedcba9};
        int mask = 0xff;
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        // load r0 and r1 with sensible values for ignoring the loading of bytes.
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], testval[0]);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], testval[1]);
        asm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2); // values now lie on the stack
        for (int i = 0; i < 8; i++) {
            // stackpointer advanced by 8
            asm.ldrb(ARMV7Assembler.ConditionFlag.Always, 1, 1, 0, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[13], i);
            if (i < 4) {
                setExpectedValue(i, testval[0] & (mask << (8 * (i % 4))));
            } else {
                setExpectedValue(i, testval[1] & (mask << (8 * (i % 4))));
            }
            setExpectedValue(i, expectedIntValues[i] >> 8 * (i % 4));
            if (expectedIntValues[i] < 0) {
                setExpectedValue(i, 0x100 + expectedIntValues[i]);
            }
            // Bytes do not have a sign! So we need to make sure the expectedValues are
            // not affected by sign extension side effects when we take the MSByte of
            // an integer.
        }
        generateAndTest();

    }

    private long connectRegs(long reg0, long reg1) {
        long returnVal;
        if (reg1 < 0) {
            returnVal = reg1 << 32;
            if (reg0 < 0) {
                returnVal += Long.parseLong(String.format("%32s", Long.toBinaryString(reg0)).replace(' ', '0').substring(32), 2);
            } else {
                returnVal += reg0;
            }
        } else {
            returnVal = reg1 << 32;
            if (reg1 == 0) {
                returnVal += reg0;
            } else if (reg0 < 0) {
                returnVal += Long.parseLong(String.format("%32s", Long.toBinaryString(reg0)).replace(' ', '0').substring(32), 2);
            } else {
                returnVal += reg0;
            }
        }
        return returnVal;
    }

    public void test_mov64BitConstant() throws Exception {
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
        for (long value : values) {
            asm.codeBuffer.reset();
            asm.movImm64(ConditionFlag.Always, r0, r1, value);
            final int[] registers = generate();
            assert value == connectRegs(registers[0], registers[1]);
        }
    }

    public void test_AddConstant() throws Exception {
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        for (int value : scratchTestSet) {
            initialiseExpectedValues();
            initialiseTestValues();
            asm.codeBuffer.reset();
            for (int srcReg = 0; srcReg < 3; srcReg++) {
                for (int destReg = 0; destReg < 3; destReg++) {
                    asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[srcReg], value);
                    asm.add12BitImmediate(ConditionFlag.Always, false, ARMV7.cpuRegisters[destReg], ARMV7.cpuRegisters[srcReg], 5);
                    setExpectedValue(destReg, value + 5);
                }
            }
            generateAndTest();
        }
    }

    public void test_VPushPop() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
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
        setExpectedValue(0, 0);
        setExpectedValue(1, 1);
        setExpectedValue(2, 2);
        setExpectedValue(3, 3);
        setExpectedValue(4, 4);
        generateAndTest();
    }

    public void test_Vdiv() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
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
        setExpectedValue(0, 10);
        setExpectedValue(1, 24);
        setExpectedValue(2, 2);
        generateAndTest();
    }

    public void test_Vcvt_int2float() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
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
        setExpectedValue(0, 10);
        setExpectedValue(1, 24);
        generateAndTest();
    }

    public void test_Vcvt_int2double() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 10);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], 24);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s1, ARMV7.r1, null, CiKind.Float, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s2, false, true, ARMV7.s0, CiKind.Double, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s4, false, true, ARMV7.s1, CiKind.Double, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, true, true, ARMV7.s2, CiKind.Float, CiKind.Double);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s1, true, true, ARMV7.s4, CiKind.Float, CiKind.Double);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r0, ARMV7.s0, null, CiKind.Int, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s1, null, CiKind.Int, CiKind.Float);
        setExpectedValue(0, 10);
        setExpectedValue(1, 24);
        generateAndTest();
    }

    public void test_Vcvt_double2float() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        asm.movImm64(ConditionFlag.Always, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[1], Double.doubleToRawLongBits(-10));
        asm.movImm64(ConditionFlag.Always, ARMV7.cpuRegisters[2], ARMV7.cpuRegisters[3], Double.doubleToRawLongBits(-24));
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r0, ARMV7.r1, CiKind.Double, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s2, ARMV7.r2, ARMV7.r3, CiKind.Double, CiKind.Int);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s4, false, true, ARMV7.s0, CiKind.Float, CiKind.Double);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s5, false, true, ARMV7.s2, CiKind.Float, CiKind.Double);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s4, true, true, ARMV7.s4, CiKind.Int, CiKind.Float);
        asm.vcvt(ARMV7Assembler.ConditionFlag.Always, ARMV7.s5, true, true, ARMV7.s5, CiKind.Int, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r0, ARMV7.s4, null, CiKind.Int, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r1, ARMV7.s5, null, CiKind.Int, CiKind.Float);
        setExpectedValue(0, -10);
        setExpectedValue(1, -24);
        generateAndTest();
    }

    public void test_VAdd() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
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
        setExpectedValue(0, 12);
        setExpectedValue(1, 10);
        setExpectedValue(2, 22);
        generateAndTest();
    }

    public void test_VSub() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
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
        setExpectedValue(0, 12);
        setExpectedValue(1, 10);
        setExpectedValue(2, -2);
        generateAndTest();
    }

    public void test_VcvtvMul() throws Exception {
        initialiseExpectedValues();
        initialiseTestValues();
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
        setExpectedValue(0, 12);
        setExpectedValue(1, 10);
        setExpectedValue(2, 120);
        generateAndTest();
    }

    public void test_VldrStr() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
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
        setExpectedValue(0, 12);
        setExpectedValue(1, 10);
        setExpectedValue(2, 12);
        setExpectedValue(4, 12);
        setExpectedValue(6, 12);
        setExpectedValue(8, 12);
        generateAndTest();
    }

    public void test_Vldr() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 12);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], 10);
        asm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512); // 1 instruction
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.s31, ARMV7.r13, 0, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s31, null, CiKind.Int, CiKind.Float);
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.s4, ARMV7.r13, 0, CiKind.Int, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r4, ARMV7.s4, null, CiKind.Int, CiKind.Float);
        setExpectedValue(0, 12);
        setExpectedValue(1, 10);
        setExpectedValue(2, 12);
        setExpectedValue(4, 12);
        generateAndTest();
    }

    public void test_MVov() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 12);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], 10);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, ARMV7.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s0, null, CiKind.Int, CiKind.Float);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.s5, ARMV7.r0, null, CiKind.Float, CiKind.Int);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r4, ARMV7.s5, null, CiKind.Int, CiKind.Float);
        setExpectedValue(0, 12);
        setExpectedValue(1, 10);
        setExpectedValue(2, 12);
        setExpectedValue(4, 12);
        generateAndTest();
    }

    // TODO: Fix vmovimm
    public void broken_vmovimm() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        asm.vmovImm(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, 1, CiKind.Double);
        asm.vmovImm(ARMV7Assembler.ConditionFlag.Always, ARMV7.s0, 0, CiKind.Double);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r0, ARMV7.s0, null, CiKind.Int, CiKind.Double);
        asm.vmovImm(ARMV7Assembler.ConditionFlag.Always, ARMV7.s1, -100, CiKind.Double);
        asm.vmov(ARMV7Assembler.ConditionFlag.Always, ARMV7.r2, ARMV7.s1, null, CiKind.Int, CiKind.Double);
        setExpectedValue(0, 0);
        setExpectedValue(2, -100);
        generateAndTest();
    }

    public void test_FloatIngPointExperiments() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
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
        setExpectedValue(0, 120);
        generateAndTest();
    }

    public void test_SubReg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 5; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedIntValues[i]);
        }
        for (int i = 0; i < 5; i++) {
            asm.sub(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.cpuRegisters[i + 5], ARMV7.cpuRegisters[5 - (i + 1)], ARMV7.cpuRegisters[i], 0, 0);
            setExpectedValue(i + 5, expectedIntValues[5 - (i + 1)] - expectedIntValues[i]);
        }
        generateAndTest();
    }

    public void test_Mov() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 5; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedIntValues[i]);
            asm.mov(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[i + 5], ARMV7.cpuRegisters[i]);
            setExpectedValue(i + 5, expectedIntValues[i]);
            testValues[i] = true;
        }
        generateAndTest();
    }

    public void test_Sub() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedIntValues[i]);
            asm.sub(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[i], i * 2, 0);
            setExpectedValue(i, expectedIntValues[i] - i * 2);
        }
        generateAndTest();
    }

    public void test_Str() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[12], 0);
        for (int i = 0; i < 10; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedIntValues[i]);
            testValues[i] = true;
            asm.str(ARMV7Assembler.ConditionFlag.Always, 1, 0, 0, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[13], ARMV7.cpuRegisters[12], i * 4, 0);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], -2 * (expectedIntValues[i]));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, 1, 0, 0, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[13], ARMV7.cpuRegisters[12], i * 4, 0);
        }
        generateAndTest();
    }

    public void test_neg() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        setExpectedValue(1, -1);
        initialiseTestValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 32);
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], -1);
        asm.xorq(ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0]);
        asm.mvn(ConditionFlag.Always, false, ARMV7.cpuRegisters[1], ARMV7.cpuRegisters[0], 0);
        generateAndTest();
    }

    public void test_msb_int() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        int[] input = new int[] {0, 1, 2, -1, 61440};
        int[] output = new int[] {-1, 0, 1, 31, 15};
        for (int i = 0; i < input.length; i++) {
            initialiseTestValues();
            setExpectedValue(0, output[i]);
            asm.codeBuffer.reset();
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], input[i]);
            asm.cmpImmediate(ConditionFlag.Always, ARMV7.cpuRegisters[0], 0);
            asm.movImm32(ConditionFlag.Equal, ARMV7.cpuRegisters[0], -1);
            asm.jcc(ConditionFlag.Equal, 40, false);
            asm.clz(ConditionFlag.Always, ARMV7.cpuRegisters[1], ARMV7.cpuRegisters[0]);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 31);
            asm.sub(ConditionFlag.Always, false, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[1], 0, 0);
            generateAndTest();
        }
    }

    public void test_lsb_int() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        int[] input = new int[] {1, 2, 0, -1, 61440};
        int[] output = new int[] {0, 1, -1, 0, 12};
        for (int i = 0; i < input.length; i++) {
            initialiseTestValues();
            setExpectedValue(0, output[i]);
            asm.codeBuffer.reset();
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], input[i]);
            asm.cmpImmediate(ConditionFlag.Always, ARMV7.cpuRegisters[0], 0);
            asm.movImm32(ConditionFlag.Equal, ARMV7.cpuRegisters[0], -1);
            asm.jcc(ConditionFlag.Equal, 32, false);
            asm.rbit(ConditionFlag.Always, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0]);
            asm.clz(ConditionFlag.Always, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0]);
            generateAndTest();
        }
    }

    public void test_msb_long() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        long[] input = new long[] {0L, -1L, 2147483648L, 4294967296L};
        int[] output = new int[] {-1, 63, 31, 32};
        for (int i = 0; i < input.length; i++) {
            initialiseTestValues();
            setExpectedValue(0, output[i]);
            asm.codeBuffer.reset();
            int low32 = (int) (input[i] & 0xffffffffL);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], low32); // 4
            int high32 = (int) ((input[i] >> 32) & 0xffffffffL);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], high32); // 12
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
            generateAndTest();
        }
    }

    public void test_lsb_long() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        long[] input = new long[] {0L, 1L, 2L, -1L, 61440L, 2147483648L, 4294967296L};
        int[] output = new int[] {-1, 0, 1, 0, 12, 31, 32};
        for (int i = 0; i < input.length; i++) {
            initialiseTestValues();
            setExpectedValue(0, output[i]);
            asm.codeBuffer.reset();
            int low32 = (int) (input[i] & 0xffffffffL);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], low32); // 4
            int high32 = (int) ((input[i] >> 32) & 0xffffffffL);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[1], high32); // 12
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
            generateAndTest();
        }
    }

    public void test_Ldr() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedIntValues[i]);
            testValues[i] = true;
        }
        asm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        for (int i = 0; i < 10; i++) {
            asm.add12BitImmediate(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[i], i * 2);
            asm.movw(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, i * 4);
            asm.movt(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, 0);
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, 1, 1, 0, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[13], ARMV7.cpuRegisters[12], 0, 0);
        }
        generateAndTest();
    }

    public void test_Decq() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedIntValues[i]);
            asm.decq(ARMV7.cpuRegisters[i]);
            setExpectedValue(i, expectedIntValues[i] - 1);
        }
        generateAndTest();
    }

    public void test_Incq() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedIntValues[i]);
            asm.incq(ARMV7.cpuRegisters[i]);
            setExpectedValue(i, expectedIntValues[i] + 1);
        }
        generateAndTest();
    }

    public void test_Subq() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedIntValues[i]);
            if (i % 2 == 1) {
                asm.subq(ARMV7.cpuRegisters[i], 2 * expectedIntValues[i]);
                setExpectedValue(i, -expectedIntValues[i]);
            } else {
                asm.subq(ARMV7.cpuRegisters[i], expectedIntValues[i]);
                setExpectedValue(i, 0);
            }
        }
        generateAndTest();
    }

    public void test_addq() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedIntValues[i]);
            asm.addq(ARMV7.cpuRegisters[i], expectedIntValues[i]);
            setExpectedValue(i, 2 * expectedIntValues[i]);
        }
        generateAndTest();
    }

    public void test_Ldrsh() throws Exception {
        int[] testval = {0x03020100, 0x8fed9ba9};
        int mask = 0xffff;
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
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
            if (i < 2) {
                setExpectedValue(i, testval[0] & (mask << (16 * (i % 2))));
            } else {
                setExpectedValue(i, testval[1] & (mask << (16 * (i % 2))));
            }
            if ((expectedIntValues[i] & 0x8000) != 0) {
                setExpectedValue(i, expectedIntValues[i] - 0x10000); // sign extension workaround.
            }
            setExpectedValue(i, expectedIntValues[i] >> 16 * (i % 2));
        }

        generateAndTest();
    }

    public void test_StrdAndLdrd() throws Exception {
        MaxineARMv7Tester.disableDebug();
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        for (int i = 0; i < 10; i += 2) {
            asm.codeBuffer.reset();
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedIntValues[i]);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i + 1], expectedIntValues[i + 1]);
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
            generateAndTest();
        }
    }

    public void test_StrexdAndLdrexd() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        // Perform an ldrexd to get exclusive access
        asm.ldrexd(ConditionFlag.Always, ARMV7.cpuRegisters[0], ARMV7.rsp);
        for (int i = 0; i < 10; i += 2) {
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedIntValues[i]);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i + 1], expectedIntValues[i + 1]);
            asm.strexd(ConditionFlag.Always, ARMV7.cpuRegisters[10], ARMV7.cpuRegisters[i], ARMV7.rsp);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], 0);
            asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i + 1], 0);
            asm.ldrexd(ConditionFlag.Always, ARMV7.cpuRegisters[i], ARMV7.rsp);
            testValues[i] = true;
            testValues[i + 1] = true;
        }
        generateAndTest();
    }

    public void test_StrexdWithoutExclusiveAccess() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[2], 0);
        asm.strexd(ConditionFlag.Always, ARMV7.cpuRegisters[2], ARMV7.cpuRegisters[0], ARMV7.rsp);
        // The exclusive  store should fail since we don't have exclusive access
        setExpectedValue(2, 1);
        generateAndTest();
    }

    public void test_PushEvenNumberOfRegisters() throws Exception {
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseExpectedValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.push(ConditionFlag.Always, (1 << i) | (1 << 10));
            asm.mov(ConditionFlag.Always, false, ARMV7.cpuRegisters[i], ARMV7.rsp);
        }
        int[] resultValues = generate();
        Assert.assertNotNull(resultValues);
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(resultValues[i] % 8, 0);
        }
    }

    public void test_PushAndPop() throws Exception {
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseExpectedValues();
        for (int i = 0; i < 11; i++) {
            if (i % 2 == 0) {
                setExpectedValue(i, i);
            } else {
                setExpectedValue(i, -i);
            }
        }

        int registers = 2;
        for (int bitmask = 0b11; bitmask <= 0xfff; bitmask = (bitmask + 1) * 4 - 1, registers += 2) {
            asm.codeBuffer.reset();
            for (int i = 0; i < 11; i++) { // we are not breaking the stack (r13)
                asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], expectedIntValues[i]); // 2 instructions
            }
            asm.push(ARMV7Assembler.ConditionFlag.Always, bitmask); // store all registers referred to
            // by bitmask on the stack
            for (int i = 0; i < 11; i++) {
                asm.add12BitImmediate(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.cpuRegisters[i], ARMV7.cpuRegisters[i], 1);
            }
            // r0..r12 should now all have +1 more than their previous values stored on the stack
            // restore the same registers that were placed on the stack
            asm.pop(ARMV7Assembler.ConditionFlag.Always, bitmask);
            for (int i = 0; i < 11; i++) {
                if (i >= registers) {
                    expectedIntValues[i] = expectedIntValues[i] + 1;
                }
            }
            generateAndTest();
        }
    }

    public void test_MovRor() throws Exception {
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
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
        final int value = 0x0fab0dad;
        for (int shift = 1; shift < 31;) {
            asm.codeBuffer.reset();
            for (int i = 0; i < 3 && shift < 32; i++, shift++) {
                final int index = i * 3;
                asm.movImm32(ARMV7Assembler.ConditionFlag.Always, ARMV7.cpuRegisters[index], value);
                setExpectedValue(index, value);
                asm.movror(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[index + 1], ARMV7.cpuRegisters[index], shift);
                setExpectedValue(index + 1, Integer.rotateRight(value, shift));
                asm.movror(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[index + 2], ARMV7.cpuRegisters[index + 1], 32 - shift);
                setExpectedValue(index + 2, value);
            }
            generateAndTest();
        }
    }

    public void test_Movw() throws Exception {
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.Lower16Bits);
        for (int value : valueTestSet) {
            initialiseTestValues();
            asm.codeBuffer.reset();
            for (int destReg = 0; destReg < 11; destReg++) {
                setExpectedValue(destReg, value);
                asm.movw(ConditionFlag.Always, ARMV7.cpuRegisters[destReg], value);
            }
            generateAndTest();
        }
    }

    public void test_Movt() throws Exception {
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.Upper16Bits);
        initialiseTestValues();
        for (int value : valueTestSet) {
            asm.codeBuffer.reset();
            for (int destReg = 0; destReg < 11; destReg++) {
                setExpectedValue(destReg, (value & 0xffff) << 16);
                asm.movt(ConditionFlag.Always, ARMV7.cpuRegisters[destReg], value & 0xffff);
            }
            generateAndTest();
        }
    }

    public void test_Flags() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        asm.codeBuffer.reset();
        asm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 30);
        asm.sub(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0], 10, 0);
        asm.sub(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0], 10, 0);
        asm.sub(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0], 10, 0);
        asm.sub(ARMV7Assembler.ConditionFlag.Always, true, ARMV7.cpuRegisters[0], ARMV7.cpuRegisters[0], 10, 0);
        setExpectedValue(0, -10);
        generateAndTest();
    }

    public void test_Ldrd() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
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

    public void test_casInt() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        masm.codeBuffer.reset();
        CiRegister cmpReg = ARMV7.r0;
        CiRegister newReg = ARMV7.r1;
        for (int i = 1; i < 6; i++) {
            masm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], (i + 1) * 10);
        }
        masm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[0], 50);
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32);
        CiAddress addr = new CiAddress(CiKind.Int, ARMV7.rsp.asValue(), 24);
        masm.casIntAsmTest(newReg, cmpReg, addr);
        setExpectedValue(1, 20);
        generateAndTest();
    }

    public void test_casLong() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
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
        setExpectedValue(0, 30);
        setExpectedValue(1, 0);
        generateAndTest();
    }

    public void test_decrementl() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        masm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            masm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], (i + 1) * 10);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        CiAddress addr = new CiAddress(CiKind.Int, ARMV7.r13.asValue(), 16);
        masm.decrementl(addr, 10);
        masm.pop(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        setExpectedValue(0, 10);
        setExpectedValue(1, 20);
        setExpectedValue(2, 30);
        setExpectedValue(3, 40);
        setExpectedValue(4, 40);
        setExpectedValue(5, 60);
        setExpectedValue(6, 70);
        setExpectedValue(7, 80);
        setExpectedValue(8, 90);
        setExpectedValue(9, 100);
        generateAndTest();
    }

    public void test_incrementl() throws Exception {
        initialiseExpectedValues();
        setAllBitMasks(MaxineARMv7Tester.BitsFlag.All32Bits);
        initialiseTestValues();
        masm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            masm.movImm32(ConditionFlag.Always, ARMV7.cpuRegisters[i], (i + 1) * 10);
        }
        masm.push(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        CiAddress addr = new CiAddress(CiKind.Int, ARMV7.r13.asValue(), 16);
        masm.incrementl(addr, 10);
        masm.pop(ARMV7Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        setExpectedValue(0, 10);
        setExpectedValue(1, 20);
        setExpectedValue(2, 30);
        setExpectedValue(3, 40);
        setExpectedValue(4, 60);
        setExpectedValue(5, 60);
        setExpectedValue(6, 70);
        setExpectedValue(7, 80);
        setExpectedValue(8, 90);
        setExpectedValue(9, 100);
        generateAndTest();
    }
}
