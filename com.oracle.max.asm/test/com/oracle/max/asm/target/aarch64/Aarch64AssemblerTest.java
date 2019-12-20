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
package com.oracle.max.asm.target.aarch64;

import com.sun.cri.ci.CiRegister;
import com.sun.cri.ci.CiTarget;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

/**
 * Tests the assembler against the GNU assembler (or any other).
 *
 * Expected values are generated using:
 * {@code echo "nop" | aarch64-linux-gnu-as -- && objdump -D a.out | tail -n 1 | awk '{print $2}'}
 */
public class Aarch64AssemblerTest {
    private Aarch64Assembler asm;

    public Aarch64AssemblerTest() {
        CiTarget aarch64 = new CiTarget(new Aarch64(), true, 8, 16, 4096, 0, false, false, false, true);
        asm = new Aarch64Assembler(aarch64, null);
    }

    private int assemble(String instruction) {
        try {
            String[] cmd = {"/bin/sh", "-c", "echo \"" + instruction + "\" | aarch64-linux-gnu-as -- && aarch64-linux-gnu-objdump -D a.out | tail -n 1 | awk '{print $2}'"};
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            p.waitFor();
            if (p.exitValue() != 0) {
                System.out.println("Command: " + cmd[2] + " returned " + p.exitValue());
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                }
                assert false;
            }
            final String line = in.readLine();
            assert line != null;
            in.close();
            return (int) Long.parseLong(line, 16);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        assert false; // Should not reach here
        return 0;
    }

    @Before
    public void initialization() {
        asm.codeBuffer.reset();
    }

    @Test
    public void nop() {
        asm.nop();
        assertEquals(assemble("nop"), asm.codeBuffer.getInt(0));
    }

    @Test
    public void add() {
        final int[] immediates = {0, 0xff, 0x0fff, 0xf000, 0xff_f000};
        for (CiRegister dest: Aarch64.cpuRegisters) {
            for (CiRegister src: Aarch64.cpuRegisters) {
                for (int imm: immediates) {
                    asm.codeBuffer.reset();
                    asm.add(64, dest, src, imm);
                    assertEquals(assemble("add x" + dest.getEncoding() + ", x" + src.getEncoding() + ", " + imm), asm.codeBuffer.getInt(0));
                }

            }
        }
    }

    @Test
    public void dsb() {
        for (Aarch64Assembler.BarrierKind bkind : Aarch64Assembler.BarrierKind.values()) {
            asm.codeBuffer.reset();
            asm.dsb(bkind);
            assertEquals(assemble("dsb " + bkind.optionName), asm.codeBuffer.getInt(0));
        }
    }

    @Test
    public void isb() {
        asm.isb();
        assertEquals(assemble("isb"), asm.codeBuffer.getInt(0));
    }

    @Test
    public void dmb() {
        for (Aarch64Assembler.BarrierKind bkind : Aarch64Assembler.BarrierKind.values()) {
            asm.codeBuffer.reset();
            asm.dmb(bkind);
            assertEquals(assemble("dmb " + bkind.optionName), asm.codeBuffer.getInt(0));
        }
    }

    @Test
    public void adr() {
        final int[] immediates = {0, 0xff, 0x0fff, 0xf000, 0xf_f000, 0xf_ffff};
        for (CiRegister dest : Aarch64.cpuRegisters) {
            for (int imm : immediates) {
                asm.codeBuffer.reset();
                asm.adr(dest, imm);
                assertEquals(assemble("adr x" + dest.getEncoding() + ", " + imm), asm.codeBuffer.getInt(0));
            }
        }
    }
}
