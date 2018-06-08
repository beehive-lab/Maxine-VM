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

import com.oracle.max.asm.target.riscv.*;
import com.sun.cri.ci.*;

import test.crossisa.*;

public class RISCV64AssemblerTest {

    private RISCVAssembler asm;
    private MaxineRISCV64Tester tester = new MaxineRISCV64Tester();

    public RISCV64AssemblerTest() {
        CiTarget risc64 = new CiTarget(new RISCV64(), true, 8, 0, 4096, 0, false, false, false, true);
        asm = new RISCVAssembler(risc64);
    }

    @Before
    public void initialiseTest() {
        tester.resetTestValues();
    }

    @After
    public void generateAndTest() throws Exception {
        RISCV64CodeWriter code = new RISCV64CodeWriter(asm.codeBuffer);
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
    public void lui() {
        asm.lui(t0, 0xFF);
        asm.lui(t1, 0xFF << 12);
        asm.lui(t2, 0xFFFFFFFF << 12);
        tester.setExpectedValue(t0, 0);
        tester.setExpectedValue(t1, 0xFF000);
        tester.setExpectedValue(t2, 0xFFFFF000);
    }

    @Test
    public void add() {
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
        tester.setExpectedValue(t0, 0x00033000);
        tester.setExpectedValue(t1, 0x40042000);
        tester.setExpectedValue(t2, 0XFF000000);
    }

    @Test
    public void sub() {
        //store values
        asm.lui(s1, 0x00022000);
        asm.lui(s2, 0x00089000);
        asm.lui(s3, 0xFFFFF000);

        asm.sub(t0, s2, s1);
        asm.sub(t1, s3, zero);
        asm.sub(t2, zero, zero);
        tester.setExpectedValue(t0, 0x00067000);
        tester.setExpectedValue(t1, 0xFFFFF000);
        tester.setExpectedValue(t2, 0x0);
    }

    @Test
    public void slt() {
        //store values
        asm.lui(s1, 0XFFFFF000);
        asm.lui(s2, 0X11111000);

        asm.slt(t0, s1, s2);
        asm.slt(t1, s2, s1);
        tester.setExpectedValue(t0, 1);
        tester.setExpectedValue(t1, 0);
    }

    @Test
    public void sltu() {
        //store values
        asm.lui(s1, 0XFFFFF000);

        asm.sltu(t0, s1, zero);
        asm.sltu(t1, zero, s1);
        tester.setExpectedValue(t0, 0);
        tester.setExpectedValue(t1, 1);
    }

    @Test
    public void xor() {
        //store values
        asm.lui(s1, 0XFF0FF000);
        asm.lui(s2, 0X0FF0F000);
        asm.lui(s4, 0xFFFFF000);

        asm.xor(t0, s1, s2);
        asm.xor(t1, s4, zero);
        tester.setExpectedValue(t0, 0xF0FF0000);
        tester.setExpectedValue(t1, 0xFFFFF000);
    }

    @Test
    public void or() {
        //store values
        asm.lui(s1, 0xFF0FF000);
        asm.lui(s2, 0x0FF0F000);

        asm.or(t0, s1, s2);
        asm.or(t1, s1, zero);
        tester.setExpectedValue(t0, 0xFFFFF000);
        tester.setExpectedValue(t1, 0xFF0FF000);
    }

    @Test
    public void and() {
        //store values
        asm.lui(s1, 0xAF1F1000);
        asm.lui(s2, 0xF0FF5000);
        asm.lui(s3, 0x00000000);
        asm.lui(s4, 0xFFFFF000);

        asm.and(t0, s1, s2);
        asm.and(t1, s1, s3);
        asm.and(t2, s4, s4);
        tester.setExpectedValue(t0, 0xA01F1000);
        tester.setExpectedValue(t1, 0x00000000);
        tester.setExpectedValue(t2, 0xFFFFF000);
    }

    @Test
    public void addi() {
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
        asm.addi(s6, zero, 2);
        asm.addi(t5, s6, 1);

        tester.setExpectedValue(t0, 0x33333222);
        tester.setExpectedValue(t1, 0xFABF1333);
        tester.setExpectedValue(t2, 0x00000111);
        tester.setExpectedValue(t3, 0xFABF122A);
        tester.setExpectedValue(t4, 0x99992AB3);
        tester.setExpectedValue(t5, 3);
    }

    @Test
    public void andi() {
        //store values
        asm.lui(s1, 0x33333000);
        asm.addi(s2, s1, 0x00000B3A); // s2 = 0x33332B3A
        asm.lui(s3, 0x11ABC000);
        asm.addi(s4, s3, 0x000001B2); // s4 = 0x11ABC1B2
        asm.lui(s5, 0x11111000);
        asm.addi(s6, s5, 0x00000111); // s6 = 0x11111111

        asm.andi(t0, s2, 0x00000C22);
        asm.andi(t1, s4, 0x00000BBC);
        asm.andi(t2, s6, 0x0);
        asm.andi(t3, zero, 0x22222222);

        tester.setExpectedValue(t0, 0x33332822);
        tester.setExpectedValue(t1, 0x11ABC1B0);
        tester.setExpectedValue(t2, 0x0);
        tester.setExpectedValue(t3, 0x0);
    }

    @Test
    public void ori() {
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
        tester.setExpectedValue(t0, 0xfffffF3A);
        tester.setExpectedValue(t1, 0x33333564);
        tester.setExpectedValue(t2, 0xFFFFFBBB);
    }

    @Test
    public void xori() {
        //store values

        asm.lui(s1, 0x33333000);
        asm.addi(s2, s1, 0x00000B3A);

        asm.lui(s3, 0x22222000);
        asm.addi(s4, s3, 0x00000BBA);

        asm.xori(t0, s2, 0x00000111);
        asm.xori(t1, s4, 0x00000F1F);
        tester.setExpectedValue(t0, 0x33332a2b);
        tester.setExpectedValue(t1, 0xdddde4a5);
    }

    @Test
    public void slti() {
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
        tester.setExpectedValue(t0, 0);
        tester.setExpectedValue(t1, 1);
        tester.setExpectedValue(t2, 0);
    }

    @Test
    public void sw() {
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

        tester.setExpectedValue(t1, 0x11111000);
        tester.setExpectedValue(t2, 0xFFFFF111);
        tester.setExpectedValue(t3, 0x33332B3A);
    }

    @Test
    public void sh() {
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

        tester.setExpectedValue(t1, 0x00001000);
        tester.setExpectedValue(t2, 0x00002131);
        tester.setExpectedValue(t3, 0x00002B3A);
    }

    @Test
    public void sb() {
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

        tester.setExpectedValue(t1, 0x00);
        tester.setExpectedValue(t2, 0x31);
        tester.setExpectedValue(t3, 0x3A);
    }

    @Test
    public void lhu() {
        //store values
        asm.lui(s1, 0xFFFFF000);
        asm.addi(s2, s1, 0x00000FFF);
        asm.lui(s3, 0xAB122000);
        asm.addi(s4, s3, 0x00000131);
        asm.lui(s5, 0x33333000);
        asm.addi(s6, s5, 0x00000B3A);

        //test case 1
        asm.sh(sp, s2, 128);
        asm.lhu(t1, sp, 128);

        //test case 2
        asm.sh(sp, s4, 4);
        asm.lhu(t2, sp, 4);

        //test case 3
        asm.sh(sp, s6, 0);
        asm.lhu(t3, sp, 0);

        tester.setExpectedValue(t1, 0xEFFF);
        tester.setExpectedValue(t2, 0x2131);
        tester.setExpectedValue(t3, 0x2B3A);
    }

    @Test
    public void lbu() {
        //store values
        asm.lui(s1, 0xFFFFF000);
        asm.addi(s2, s1, 0x00000FFF);
        asm.lui(s3, 0xAB122000);
        asm.addi(s4, s3, 0x00000131);
        asm.lui(s5, 0x33333000);
        asm.addi(s6, s5, 0x00000B3A);

        //test case 1
        asm.sb(sp, s2, 128);
        asm.lbu(t1, sp, 128);

        //test case 2
        asm.sb(sp, s4, 4);
        asm.lbu(t2, sp, 4);

        //test case 3
        asm.sb(sp, s6, 0);
        asm.lbu(t3, sp, 0);

        tester.setExpectedValue(t1, 0xFF);
        tester.setExpectedValue(t2, 0x31);
        tester.setExpectedValue(t3, 0x3A);
    }
}
