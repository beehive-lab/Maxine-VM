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
package com.oracle.max.vm.tests.crossisa.riscv64.asm;

import static com.oracle.max.asm.target.riscv64.RISCV64.*;

import com.oracle.max.vm.tests.crossisa.CrossISATester;
import com.oracle.max.asm.Label;
import org.junit.*;

import com.oracle.max.asm.target.riscv64.*;
import com.sun.cri.ci.*;

import com.oracle.max.asm.target.riscv64.RISCV64MacroAssembler.ConditionFlag;

public class RISCV64AssemblerTest {

    private RISCV64MacroAssembler asm;
    private MaxineRISCV64Tester tester = new MaxineRISCV64Tester();

    public RISCV64AssemblerTest() {
        CiTarget risc64 = new CiTarget(new RISCV64(), true, 8, 0, 4096, 0, false, false, false, true);
        asm = new RISCV64MacroAssembler(risc64);
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
    // RV32I Base instruction set /////////////////////////////////////////////

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

    @Test
    public void slli() throws Exception {
        //store values
        asm.lui(s2, 0xFFFFF000);
        asm.addi(s4, zero, 0x11111BBB);
        asm.lui(s5, 0x11111000);
        asm.addi(s6, s5, 0x00000222);

        asm.slli(s2, s2, 3);
        asm.slli(s4, s4, 2);
        asm.slli(s6, s6, 0);
        tester.setExpectedValue(s2, 0xFFFF8000);
        tester.setExpectedValue(s4, 0xFFFFeeec);
        tester.setExpectedValue(s6, 0x11111222);
    }

    @Test
    public void srli() throws Exception {
        //store values
        asm.lui(s2, 0x11111000);
        asm.lui(s3, 0x11111000);
        asm.addi(s4, s3, 0x00000222);
        asm.lui(s5, 0x33333000);
        asm.addi(s6, s5, 0x00000B3A); // s2 = 0x33332B3A

        asm.srli(s2, s2, 3);
        asm.srli(s4, s4, 2);
        asm.srli(s6, s6, 4);
        tester.setExpectedValue(s2, 0x2222200);
        tester.setExpectedValue(s4, 0x4444488);
        tester.setExpectedValue(s6, 0x33332b3);
    }

    @Test
    public void srl() throws Exception {
        //store values
        asm.lui(s1, 0);
        asm.addi(s1, s1, 30);
        asm.lui(s3, 0);
        asm.addi(s3, s3, 0);
        asm.lui(s5, 0);
        asm.addi(s5, s5, 16);

        asm.lui(s2, 0x40000000);

        asm.lui(s4, 0);
        asm.addi(s4, s4, 0x2BC);

        asm.lui(s6, 0x10000);

        asm.srl(t0, s2, s1);
        asm.srl(t1, s4, s3);
        asm.srl(t2, s6, s5);
        tester.setExpectedValue(t0, 0b1);
        tester.setExpectedValue(t1, 0x2BC);
        tester.setExpectedValue(t2, 0b1);
    }

    @Test
    public void sll() throws Exception {
        asm.lui(s1, 0);
        asm.addi(s1, s1, 31);
        asm.lui(s3, 0);
        asm.addi(s3, s3, 0);
        asm.lui(s5, 0);
        asm.addi(s5, s5, 16);

        asm.lui(s2, 0);
        asm.addi(s2, s2, 0b1);

        asm.lui(s4, 0);
        asm.addi(s4, s4, 0x2BC);

        asm.lui(s6, 0);
        asm.addi(s6, s6, 0b1);

        asm.sll(t0, s2, s1);
        asm.sll(t1, s4, s3);
        asm.sll(t2, s6, s5);
        tester.setExpectedValue(t0, 0x80000000);
        tester.setExpectedValue(t1, 0x2BC);
        tester.setExpectedValue(t2, 0x10000);
    }

    @Test
    public void sltiu() throws Exception {
        //store values
        asm.lui(s1, 0x33333000);
        asm.addi(s2, s1, 0x00000B3A);
        asm.lui(s3, 0x33333000);
        asm.addi(s4, s3, 0x00000444);
        asm.lui(s5, 0xFFFFF000);
        asm.addi(s6, s5, 0x00000111);

        asm.sltiu(t0, s2, 0x00220444);
        asm.sltiu(t1, s4, 0xFFFFFFFF);
        asm.sltiu(t2, s6, 0xAA22344A);
        tester.setExpectedValue(t0, 0);
        tester.setExpectedValue(t1, 1);
        tester.setExpectedValue(t2, 0);
    }

    @Test
    public void sra() throws Exception {
        //store values
        asm.lui(s1, 0xABAB1);
        asm.addi(s2, s1, 0x0000011A);
        asm.lui(s3, 0x11111000);
        asm.addi(s4, s3, 0x00000222);
        asm.lui(s5, 0x33333000);
        asm.addi(s6, s5, 0x00000B3A);

        asm.addi(s7, s7, 0x00000001);
        asm.addi(s8, s8, 0x00000003);
        asm.addi(s9, s9, 0x0000001F);

        asm.sra(t0, s2, s7);
        asm.sra(t1, s4, s8);
        asm.sra(t2, s6, s9);
        tester.setExpectedValue(t0, 0x0005588d);
        tester.setExpectedValue(t1, 0x02222244);
        tester.setExpectedValue(t2, 0x0);
    }

    @Test
    public void beq() throws Exception {
        //store values
        asm.lui(s1, 0x33333000);
        asm.lui(s2, 0x44444000);
        asm.lui(s3, 0x33333000);

        asm.addi(t1, zero, 0x0);
        asm.beq(s1, s2, 0x8); // not equal
        asm.addi(t1, t1, 0x1);
        asm.addi(t2, zero, 0x2);

        asm.addi(t3, zero, 0x0);
        asm.beq(s1, s3, 0x8); // equal
        asm.addi(t3, t3, 0x1);
        asm.addi(t4, zero, 0x2);

        tester.setExpectedValue(t1, 0x1);
        tester.setExpectedValue(t3, 0x0);
    }

    @Test
    public void bne() throws Exception {
        //store values
        asm.lui(s1, 0x33333000);
        asm.lui(s2, 0x44444000);
        asm.lui(s3, 0x33333000);

        asm.addi(t1, zero, 0x0);
        asm.bne(s1, s2, 0x8);
        asm.addi(t1, t1, 0x1);
        asm.addi(t2, zero, 0x2);

        asm.addi(t3, zero, 0x0);
        asm.bne(s1, s3, 0x8);
        asm.addi(t3, t3, 0x1);
        asm.addi(t4, zero, 0x2);

        tester.setExpectedValue(t1, 0x0);
        tester.setExpectedValue(t3, 0x1);
    }

    @Test
    public void blt() throws Exception {
        //store values
        asm.lui(s1, 0x33333000);
        asm.lui(s2, 0x44444000);
        asm.lui(s3, 0x33333000);

        asm.addi(t1, zero, 0x0);
        asm.blt(s1, s2, 0x8);
        asm.addi(t1, t1, 0x1);
        asm.addi(t2, zero, 0x2);

        asm.addi(t3, zero, 0x0);
        asm.blt(s2, s1, 0x8);
        asm.addi(t3, t3, 0x1);
        asm.addi(t4, zero, 0x2);

        tester.setExpectedValue(t1, 0x0);
        tester.setExpectedValue(t3, 0x1);
    }

    @Test
    public void bltu() throws Exception {
        //store values
        asm.lui(s1, 0x33333000);
        asm.lui(s2, 0x44444000);

        asm.addi(t1, zero, 0x0);
        asm.bltu(s1, s2, 0x8);
        asm.addi(t1, t1, 0x1);
        asm.addi(t2, zero, 0x2);

        asm.addi(t3, zero, 0x0);
        asm.bltu(s2, s1, 0x8);
        asm.addi(t3, t3, 0x1);
        asm.addi(t4, zero, 0x2);

        tester.setExpectedValue(t1, 0x0);
        tester.setExpectedValue(t3, 0x1);
    }

    @Test
    public void bge() throws Exception {
        //store values
        asm.lui(s1, 0x33333000);
        asm.lui(s2, 0x44444000);
        asm.lui(s3, 0x33333000);

        asm.addi(t1, zero, 0x0);
        asm.bge(s2, s1, 0x8);
        asm.addi(t1, t1, 0x1);
        asm.addi(t2, zero, 0x2);

        asm.addi(t3, zero, 0x0);
        asm.bge(s1, s2, 0x8);
        asm.addi(t3, t3, 0x1);
        asm.addi(t4, zero, 0x2);

        tester.setExpectedValue(t1, 0x0);
        tester.setExpectedValue(t3, 0x1);
    }

    @Test
    public void bgeu() throws Exception {
        //store values
        asm.lui(s1, 0x33333000);
        asm.lui(s2, 0x44444000);
        asm.lui(s3, 0x33333000);

        asm.addi(t1, zero, 0x0);
        asm.bgeu(s2, s1, 0x8);
        asm.addi(t1, zero, 0x1);
        asm.addi(t2, zero, 0x2);

        asm.addi(t3, zero, 0x0);
        asm.bgeu(s1, s2, 0x8);
        asm.addi(t3, t3, 0x1);
        asm.addi(t4, zero, 0x2);

        tester.setExpectedValue(t1, 0x0);
        tester.setExpectedValue(t3, 0x1);
    }

    @Test
    public void jal() throws Exception {
        asm.lui(s1, 0x33333000); // s1 before jal
        asm.add(t1, ra, zero);   // store ra
        asm.jal(x1, 8);
        asm.lui(s1, 0x44444000); // ignored from jar
        asm.lui(s4, 0x55555000); // pc+8 - jal link
        asm.add(ra, t1, zero);


        asm.lui(s2, 0x11111000); // s2 before jal
        asm.lui(s3, 0x22222000); // s3 before jal
        asm.add(t2, ra, zero);   // store ra
        asm.jal(x1, 12);
        asm.lui(s2, 0x44444000); // ignored from jar
        asm.lui(s3, 0x55555000); // ignored from jar
        asm.lui(s5, 0x44444000); // pc+12 - jal link
        asm.add(ra, t2, zero);

        tester.setExpectedValue(s1, 0x33333000);
        tester.setExpectedValue(s4, 0x55555000);
        tester.setExpectedValue(s2, 0x11111000);
        tester.setExpectedValue(s3, 0x22222000);
    }

    @Test
    public void jalr() throws Exception {
        asm.lui(s1, 0x33333000); // s1 before jal
        asm.auipc(t0, 0);
        asm.add(t1, ra, zero);   // store ra
        asm.jalr(x1, t0, 16);
        asm.lui(s1, 0x44444000); // ignored from jar
        asm.lui(s4, 0x55555000); // pc+8 - jal link
        asm.add(ra, t1, zero);

        asm.lui(s2, 0x11111000); // s2 before jal
        asm.lui(s3, 0x22222000); // s3 before jal
        asm.add(t2, ra, zero);   // store ra
        asm.auipc(t0, 0);
        asm.jalr(x1, t0, 12);
        asm.lui(s2, 0x44444000); // ignored from jar
        asm.lui(s3, 0x55555000); // ignored from jar
        asm.lui(s5, 0x44444000); // pc+12 - jal link
        asm.add(ra, t2, zero);

        tester.setExpectedValue(s1, 0x33333000);
        tester.setExpectedValue(s4, 0x55555000);
        tester.setExpectedValue(s2, 0x11111000);
        tester.setExpectedValue(s5, 0x44444000);
    }

    @Test
    public void srai() throws Exception {
        //store values
        asm.lui(s1, 0xABAB1);
        asm.addi(s2, s1, 0x0000011A);
        asm.lui(s3, 0x11111000);
        asm.addi(s4, s3, 0x00000222);
        asm.lui(s5, 0x33333000);
        asm.addi(s6, s5, 0x00000B3A);

        asm.srai(t0, s2, 0x00000001);
        asm.srai(t1, s4, 0x00000003);
        asm.srai(t2, s6, 0x0000001F);
        tester.setExpectedValue(t0, 0x0005588d);
        tester.setExpectedValue(t1, 0x02222244);
        tester.setExpectedValue(t2, 0x0);
    }

    // RV64I Base instruction set /////////////////////////////////////////////

    @Test
    public void addiw() {
        //store values
        asm.lui(s1, 0x33333000);
        asm.lui(s2, 0xFABF1000);
        asm.lui(s3, 0x00);
        asm.lui(s4, 0xFABF1000);
        asm.lui(s5, 0x99993000);

        asm.addiw(t0, s1, 0x00000222);
        asm.addiw(t1, s2, 0x00000333);
        asm.addiw(t2, s3, 0x00000111);
        asm.addiw(t3, s4, 0x0000022A);
        asm.addiw(t4, s5, 0x00000AB3);
        asm.addiw(s6, zero, 2);
        asm.addiw(t5, s6, 1);

        tester.setExpectedValue(t0, 0x33333222);
        tester.setExpectedValue(t1, 0xFABF1333);
        tester.setExpectedValue(t2, 0x00000111);
        tester.setExpectedValue(t3, 0xFABF122A);
        tester.setExpectedValue(t4, 0x99992AB3);
        tester.setExpectedValue(t5, 3);
    }

    @Test
    public void slliw() throws Exception {
        //store values
        asm.lui(s2, 0xFFFFF000);
        asm.addi(s4, zero, 0x11111BBB);
        asm.lui(s5, 0x11111000);
        asm.addi(s6, s5, 0x00000222);

        asm.slliw(s2, s2, 3);
        asm.slliw(s4, s4, 2);
        asm.slliw(s6, s6, 0);
        tester.setExpectedValue(s2, 0xFFFF8000);
        tester.setExpectedValue(s4, 0xFFFFeeec);
        tester.setExpectedValue(s6, 0x11111222);
    }

    @Test
    public void srliw() throws Exception {
        //store values
        asm.lui(s2, 0x11111000);
        asm.lui(s3, 0x11111000);
        asm.addi(s4, s3, 0x00000222);
        asm.lui(s5, 0x33333000);
        asm.addi(s6, s5, 0x00000B3A); // s2 = 0x33332B3A

        asm.srliw(s2, s2, 3);
        asm.srliw(s4, s4, 2);
        asm.srliw(s6, s6, 4);
        tester.setExpectedValue(s2, 0x2222200);
        tester.setExpectedValue(s4, 0x4444488);
        tester.setExpectedValue(s6, 0x33332b3);
    }

    @Test
    public void sllw() throws Exception {
        //store values
        asm.lui(s1, 0xABAB1);
        asm.addi(s2, s1, 0x0000011A);
        asm.lui(s3, 0x00001000);
        asm.addi(s4, s3, 0x00000223);
        asm.lui(s5, 0x00000003);

        asm.sllw(t0, s2, s1);
        asm.sllw(t1, s4, s3);
        asm.sllw(t2, s5, s5);
        tester.setExpectedValue(t0, 0x000ab11a);
        tester.setExpectedValue(t1, 0x00001223);
        tester.setExpectedValue(t2, 0x0);
    }

    @Test
    public void srlw() throws Exception {
        //store values
        asm.lui(s1, 0xABAB1);
        asm.addi(s2, s1, 0x0000011A);
        asm.lui(s3, 0x11111000);
        asm.addi(s4, s3, 0x00000222);
        asm.lui(s5, 0x33333000);
        asm.addi(s6, s5, 0x00000B3A);

        asm.srlw(t0, s2, s1);
        asm.srlw(t1, s4, s3);
        asm.srlw(t2, s6, s5);
        tester.setExpectedValue(t0, 0x000ab11a);
        tester.setExpectedValue(t1, 0x11111222);
        tester.setExpectedValue(t2, 0x33332b3a);
    }

    @Test
    public void addw() {
        //store values test case-1
        asm.lui(s1, 0x00011000);
        asm.lui(s2, 0x00022000);

        //store values test case-2
        asm.lui(s3, 0x10020000);
        asm.lui(s4, 0x30022000);

        //store values test case-3
        asm.lui(s5, 0x00000000);
        asm.lui(s6, 0xFF000000);

        asm.addw(t0, s1, s2);
        asm.addw(t1, s3, s4);
        asm.addw(t2, s5, s6);
        tester.setExpectedValue(t0, 0x00033000);
        tester.setExpectedValue(t1, 0x40042000);
        tester.setExpectedValue(t2, 0XFF000000);
    }

    @Test
    public void subw() {
        //store values
        asm.lui(s1, 0x00022000);
        asm.lui(s2, 0x00089000);
        asm.lui(s3, 0xFFFFF000);

        asm.subw(t0, s2, s1);
        asm.subw(t1, s3, zero);
        asm.subw(t2, zero, zero);
        tester.setExpectedValue(t0, 0x00067000);
        tester.setExpectedValue(t1, 0xFFFFF000);
        tester.setExpectedValue(t2, 0x0);
    }

    @Test
    public void sraw() throws Exception {
        //store values
        asm.lui(s1, 0xABAB1);
        asm.addi(s2, s1, 0x0000011A);
        asm.lui(s3, 0x11111000);
        asm.addi(s4, s3, 0x00000222);
        asm.lui(s5, 0x33333000);
        asm.addi(s6, s5, 0x00000B3A);

        asm.addi(s7, s7, 0x00000001);
        asm.addi(s8, s8, 0x00000003);
        asm.addi(s9, s9, 0x0000001F);

        asm.sraw(t0, s2, s7);
        asm.sraw(t1, s4, s8);
        asm.sraw(t2, s6, s9);
        tester.setExpectedValue(t0, 0x0005588d);
        tester.setExpectedValue(t1, 0x02222244);
        tester.setExpectedValue(t2, 0x0);
    }

    @Test
    public void sd() {
        //store values
        asm.lui(s1, 0x11111000);
        asm.lui(s2, 0xFFFFF000);
        asm.addi(s3, s2, 0x00000111);
        asm.lui(s4, 0x33333000);
        asm.addi(s5, s4, 0x00000B3A);

        //test case 1
        asm.sd(sp, s1, 64);
        asm.ld(t1, sp, 64);

        //test case 2
        asm.sd(sp, s3, 128);
        asm.ld(t2, sp, 128);

        //test case 3
        asm.sd(sp, s5, 0);
        asm.ld(t3, sp, 0);

        tester.setExpectedValue(t1, 0x11111000);
        tester.setExpectedValue(t2, 0xFFFFF111);
        tester.setExpectedValue(t3, 0x33332B3A);
    }

    @Test
    public void lwu() {
        //store values
        asm.lui(s1, 0x11111000);
        asm.lui(s2, 0xFFFFF000);
        asm.addi(s3, s2, 0x00000111);
        asm.lui(s4, 0x33333000);
        asm.addi(s5, s4, 0x00000B3A);

        //test case 1
        asm.sd(sp, s1, 64);
        asm.lwu(t1, sp, 64);

        //test case 2
        asm.sd(sp, s3, 128);
        asm.lwu(t2, sp, 128);

        //test case 3
        asm.sd(sp, s5, 0);
        asm.lwu(t3, sp, 0);

        tester.setExpectedValue(t1, 0x11111000);
        tester.setExpectedValue(t2, 0xFFFFF111);
        tester.setExpectedValue(t3, 0x33332B3A);
    }

    @Test
    public void sraiw() throws Exception {
        //store values
        asm.lui(s1, 0xABAB1);
        asm.addi(s2, s1, 0x0000011A);
        asm.lui(s3, 0x11111000);
        asm.addi(s4, s3, 0x00000222);
        asm.lui(s5, 0x33333000);
        asm.addi(s6, s5, 0x00000B3A);

        asm.sraiw(t0, s2, 0x00000001);
        asm.sraiw(t1, s4, 0x00000003);
        asm.sraiw(t2, s6, 0x0000001F);
        tester.setExpectedValue(t0, 0x0005588d);
        tester.setExpectedValue(t1, 0x02222244);
        tester.setExpectedValue(t2, 0x0);
    }

    @Test
    public void bLabel() {
        Label loop = new Label();

        asm.add(64, t1, zero, 0);
        asm.add(64, t2, zero, 4);

        asm.bind(loop);

        asm.add(64, t1, t1, 1);
        asm.branchConditionally(ConditionFlag.LT, t1, t2, loop);

        tester.setExpectedValue(t1, 0x4);
    }
}
