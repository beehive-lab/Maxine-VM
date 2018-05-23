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
package test.crossisa.riscv64.asm;

import static com.oracle.max.asm.target.riscv.RISCV64.*;

import org.junit.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.riscv.*;
import com.sun.cri.ci.*;

import test.crossisa.*;

public class RISCV64AssemblerTest {

    private RISCVAssembler asm;

    public RISCV64AssemblerTest() {
        CiTarget risc64 = new CiTarget(new RISCV64(), true, 8, 0, 4096, 0, false, false, false, true);
        asm = new RISCVAssembler(risc64);
    }

    private static final boolean[] testValues = new boolean[MaxineRISCV64Tester.NUM_REGS];

    // Each test should set the contents of this array appropriately,
    // it enables the instruction under test to select the specific bit values for
    // comparison i.e. for example ignoring upper or lower 16bits for movt, movw
    // and for ignoring specific bits in the status register etc
    // concerning whether a carry has been set
    private static final MaxineRISCV64Tester.BitsFlag[] bitmasks =
            new MaxineRISCV64Tester.BitsFlag[MaxineRISCV64Tester.NUM_REGS];
    static {
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
    }

    private static void initializeTestValues() {
        for (int i = 0; i < testValues.length; i++) {
            testValues[i] = false;
        }
    }

    // The following values will be updated
    // to those expected to be found in a register after simulated execution of code.
    private static final long[] expectedValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

    private static void initialiseExpectedValues() {
        for (int i = 0; i < MaxineRISCV64Tester.NUM_REGS; i++) {
            expectedValues[i] = i;
        }
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param cpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    private static void setExpectedValue(CiRegister cpuRegister, long expectedValue) {
        final int index = cpuRegister.number - 1; // -1 to compensate for the zero register
        expectedValues[index] = expectedValue;
        testValues[index] = true;
    }

    private void generateAndTest(long[] expected, boolean[] tests, MaxineRISCV64Tester.BitsFlag[] masks, Buffer codeBuffer) throws Exception {
        RISCV64CodeWriter code = new RISCV64CodeWriter(codeBuffer);
        code.createCodeFile();
        MaxineRISCV64Tester r = new MaxineRISCV64Tester(expected, tests, masks);
        if (!CrossISATester.ENABLE_SIMULATOR) {
            System.out.println("Code Generation is disabled!");
            return;
        }
        r.assembleStartup();
        r.compile();
        r.link();
        r.runSimulation();
        if (!r.validateLongRegisters()) {
            r.reset();
            assert false : "Error while validating long registers";
        }
        r.reset();
    }

    @Test
    public void lui() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        asm.lui(t0, 0xFF);
        asm.lui(t1, 0xFF << 12);
        asm.lui(t2, 0xFFFFFFFF << 12);
        setExpectedValue(t0, 0);
        setExpectedValue(t1, 0xFF000);
        setExpectedValue(t2, 0xFFFFF000);

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void add() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values test case-1
        asm.lui(s1, 0x00011000);
        asm.lui(s2, 0x00022000);

        //store values test case-2
        asm.lui(s3, 0x10020000);
        asm.lui(s4, 0x30022000);

        //store values test case-3
        asm.lui(s5, 0x00000000);
        asm.lui(s6, 0xFF000000);

        asm.add(t0, s1, s2);
        asm.add(t1, s3, s4);
        asm.add(t2, s5, s6);
        setExpectedValue(t0, 0x00033000);
        setExpectedValue(t1, 0x40042000);
        setExpectedValue(t2, 0XFF000000);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void sub() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values
        asm.lui(s1, 0x00022000);
        asm.lui(s2, 0x00089000);
        asm.lui(s3, 0xFFFFF000);

        asm.sub(t0, s2, s1);
        asm.sub(t1, s3, zero);
        asm.sub(t2, zero, zero);
        setExpectedValue(t0, 0x00067000);
        setExpectedValue(t1, 0xFFFFF000);
        setExpectedValue(t2, 0x0);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void slt() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values
        asm.lui(s1, 0XFFFFF000);
        asm.lui(s2, 0X11111000);

        asm.slt(t0, s1, s2);
        asm.slt(t1, s2, s1);
        setExpectedValue(t0, 1);
        setExpectedValue(t1, 0);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void sltu() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values
        asm.lui(s1, 0XFFFFF000);

        asm.sltu(t0, s1, zero);
        asm.sltu(t1, zero, s1);
        setExpectedValue(t0, 0);
        setExpectedValue(t1, 1);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void xor() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values
        asm.lui(s1, 0XFF0FF000);
        asm.lui(s2, 0X0FF0F000);
        asm.lui(s4, 0xFFFFF000);

        asm.xor(t0, s1, s2);
        asm.xor(t1, s4, zero);
        setExpectedValue(t0, 0xF0FF0000);
        setExpectedValue(t1, 0xFFFFF000);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void or() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values
        asm.lui(s1, 0xFF0FF000);
        asm.lui(s2, 0x0FF0F000);

        asm.or(t0, s1, s2);
        asm.or(t1, s1, zero);
        setExpectedValue(t0, 0xFFFFF000);
        setExpectedValue(t1, 0xFF0FF000);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void and() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values
        asm.lui(s1, 0xAF1F1000);
        asm.lui(s2, 0xF0FF5000);
        asm.lui(s3, 0x00000000);
        asm.lui(s4, 0xFFFFF000);

        asm.and(t0, s1, s2);
        asm.and(t1, s1, s3);
        asm.and(t2, s4, s4);
        setExpectedValue(t0, 0xA01F1000);
        setExpectedValue(t1, 0x00000000);
        setExpectedValue(t2, 0xFFFFF000);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void addi() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values
        asm.lui(s1, 0x33333000);
        asm.lui(s2, 0xFABF1000);
        asm.lui(s3, 0x00);
        asm.lui(s4, 0xFABF1000);
        asm.lui(s5, 0x99993000);

        asm.addi(t0, s1, 0x00000222);
        asm.addi(t1, s2, 0x00000333);
        asm.addi(t2, s3, 0x00000111);
        asm.addi(t3, s4, 0x0000022A);
        asm.addi(t4, s5, 0x00000AB3);

        setExpectedValue(t0, 0x33333222);
        setExpectedValue(t1, 0xFABF1333);
        setExpectedValue(t2, 0x00000111);
        setExpectedValue(t3, 0xFABF122A);
        setExpectedValue(t4, 0x99992AB3);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void andi() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values
        asm.lui(s1, 0x33333000);
        asm.addi(s2, s1, 0x00000B3A); // s2 = 0x33332B3A
        asm.lui(s3, 0x11ABC000);
        asm.addi(s4, s3, 0x000001B2); // s4 = 0x11ABC1B2
        setExpectedValue(s4, 0x11ABC1B2);

        asm.andi(t0, s2, 0x00000C22);
        asm.andi(t1, s4, 0x00000BBC);
        setExpectedValue(t0, 0x33332822);
        setExpectedValue(t1, 0x11ABC1B0);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void ori() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values
        asm.lui(s1, 0x33333000);
        asm.addi(s2, s1, 0x00000B3A); // s2 = 0x33332B3A
        asm.lui(s3, 0x33333000);
        asm.addi(s4, s3, 0x00000444); // s4 = 0x33333444
        asm.lui(s5, 0xFFFFF000);
        asm.addi(s6, s5, 0x00000111); // s6 = 0xFFFFF111

        asm.ori(t0, s2, 0x00000C22);
        asm.ori(t1, s4, 0x00000524);
        asm.ori(t2, s6, 0x00000AAA);
        setExpectedValue(t0, 0xfffffF3A);
        setExpectedValue(t1, 0x33333564);
        setExpectedValue(t2, 0xFFFFFBBB);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void xori() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values

        asm.lui(s1, 0x33333000);
        asm.addi(s2, s1, 0x00000B3A);

        asm.lui(s3, 0x22222000);
        asm.addi(s4, s3, 0x00000BBA);

        asm.xori(t0, s2, 0x00000111);
        asm.xori(t1, s4, 0x00000F1F);
        setExpectedValue(t0, 0x33332a2b);
        setExpectedValue(t1, 0xdddde4a5);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void slti() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values
        asm.lui(s1, 0x33333000);
        asm.addi(s2, s1, 0x00000B3A);
        asm.lui(s3, 0x33333000);
        asm.addi(s4, s3, 0x00000444);
        asm.lui(s5, 0xFFFFF000);
        asm.addi(s6, s5, 0x00000111);


        asm.slti(t0, s2, 0x00220444);
        asm.slti(t1, s4, 0xFFFFFFFF);
        asm.slti(t2, s6, 0xAA22344A);
        setExpectedValue(t0, 0);
        setExpectedValue(t1, 1);
        setExpectedValue(t2, 0);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void sw() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values
        asm.lui(s1, 0x11111000);
        asm.lui(s2, 0xFFFFF000);
        asm.addi(s3, s2, 0x00000111);
        asm.lui(s4, 0x33333000);
        asm.addi(s5, s4, 0x00000B3A);

        //test case 1
        asm.sw(sp, s1, 64);
        asm.lw(t1, sp, 64);

        //test case 2
        asm.sw(sp, s3, 128);
        asm.lw(t2, sp, 128);

        //test case 3
        asm.sw(sp, s5, 0);
        asm.lw(t3, sp, 0);

        setExpectedValue(t1, 0x11111000);
        setExpectedValue(t2, 0xFFFFF111);
        setExpectedValue(t3, 0x33332B3A);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void sh() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values
        asm.lui(s1, 0x11111000);
        asm.lui(s2, 0xAB122000);
        asm.addi(s3, s2, 0x00000131);
        asm.lui(s4, 0x33333000);
        asm.addi(s5, s4, 0x00000B3A);

        //test case 1
        asm.sh(sp, s1, 4);
        asm.lh(t1, sp, 4);

        //test case 2
        asm.sh(sp, s3, 4);
        asm.lh(t2, sp, 4);

        //test case 3
        asm.sh(sp, s5, 0);
        asm.lh(t3, sp, 0);

        setExpectedValue(t1, 0x00001000);
        setExpectedValue(t2, 0x00002131);
        setExpectedValue(t3, 0x00002B3A);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    @Test
    public void sb() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        //store values
        asm.lui(s1, 0x11111000);
        asm.lui(s2, 0xAB122000);
        asm.addi(s3, s2, 0x00000131);
        asm.lui(s4, 0x33333000);
        asm.addi(s5, s4, 0x00000B3A);

        //test case 1
        asm.sb(sp, s1, 4);
        asm.lb(t1, sp, 4);

        //test case 2
        asm.sb(sp, s3, 4);
        asm.lb(t2, sp, 4);

        //test case 3
        asm.sb(sp, s5, 0);
        asm.lb(t3, sp, 0);

        setExpectedValue(t1, 0x00);
        setExpectedValue(t2, 0x31);
        setExpectedValue(t3, 0x3A);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

}
