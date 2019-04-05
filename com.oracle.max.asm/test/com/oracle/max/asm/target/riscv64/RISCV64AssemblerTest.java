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
package com.oracle.max.asm.target.riscv64;

import com.sun.cri.ci.CiRegister;
import com.sun.cri.ci.CiTarget;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

public class RISCV64AssemblerTest {

    private RISCV64Assembler asm;

    public RISCV64AssemblerTest() {
        CiTarget risc64 = new CiTarget(new RISCV64(), true, 8, 0, 4096, 0, false, false, false, true);
        asm = new RISCV64Assembler(risc64);
    }

    private int assemble(String instruction) {
        try {
            String[] cmd = {"/bin/sh", "-c", "echo \"" + instruction + "\" | riscv64-linux-gnu-as -- && riscv64-linux-gnu-objdump -D a.out | tail -n 1 | awk '{print $2}'"};
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
    public void lui() {
        final int[] immediates = {0, 0xff, 0x0fff, 0xf000, 0xff_f000, 0xffff_ffff};
        for (CiRegister dest: RISCV64.cpuRegisters) {
            for (int imm32: immediates) {
                asm.codeBuffer.reset();
                asm.lui(dest, imm32);
                final String assemblyInstruction = "lui x" + dest.getEncoding() + ", " + (imm32 >>> 12);
                assertEquals(assemblyInstruction, assemble(assemblyInstruction), asm.codeBuffer.getInt(0));
            }
        }
    }

    @Test
    public void sd() {
        asm.sd(RISCV64.x5, RISCV64.x6, 32);
        final String assemblyInstruction = "sd x" + RISCV64.x6.getEncoding() + ", " + 32 + "(x" + RISCV64.x5.getEncoding() + ")";
        assertEquals(assemblyInstruction, assemble(assemblyInstruction), asm.codeBuffer.getInt(0));
    }

    @Test
    public void auipc() {
        final int[] immediates = {0, 0xff, 0x0fff, 0xf000, 0xff_f000, 0xffff_ffff};
        for (CiRegister dest: RISCV64.cpuRegisters) {
            for (int imm32: immediates) {
                asm.codeBuffer.reset();
                asm.auipc(dest, imm32);
                final String assemblyInstruction = "auipc x" + dest.getEncoding() + ", " + (imm32 >>> 12);
                assertEquals(assemblyInstruction, assemble(assemblyInstruction), asm.codeBuffer.getInt(0));
            }
        }
    }

    @Test
    public void lb() {
        final int[] immediates = {0, 0xff, 0x07ff};
        for (CiRegister dest: RISCV64.cpuRegisters) {
            for (CiRegister src : RISCV64.cpuRegisters) {
                for (int imm32 : immediates) {
                    asm.codeBuffer.reset();
                    asm.lb(dest, src, imm32);
                    final String assemblyInstruction = "lb x" + dest.getEncoding() + ", " + (imm32 & 0xfff) + "(x" + src.getEncoding() + ")";
                    assertEquals(assemblyInstruction, assemble(assemblyInstruction), asm.codeBuffer.getInt(0));

                }
            }
        }
    }

    @Test
    public void ecall() {
        asm.codeBuffer.reset();
        asm.ecall();
        final String assemblyInstruction = "ecall ";
        assertEquals(assemblyInstruction, assemble(assemblyInstruction), asm.codeBuffer.getInt(0));
    }

    @Test
    public void ebreak() {
        asm.codeBuffer.reset();
        asm.ebreak();
        final String assemblyInstruction = "ebreak ";
        assertEquals(assemblyInstruction, assemble(assemblyInstruction), asm.codeBuffer.getInt(0));
    }

    @Test
    public void csrrw() {
        for (CiRegister dest: RISCV64.cpuRegisters) {
            for (CiRegister src : RISCV64.cpuRegisters) {
                asm.codeBuffer.reset();
                asm.csrrw(dest, 5, src);
                final String assemblyInstruction = "csrrw x" + dest.getEncoding() + "," + 5 + "," + "x" + src.getEncoding();
                assertEquals(assemblyInstruction, assemble(assemblyInstruction), asm.codeBuffer.getInt(0));
            }
        }
    }

    @Test
    public void csrrs() {
        for (CiRegister dest: RISCV64.cpuRegisters) {
            for (CiRegister src : RISCV64.cpuRegisters) {
                asm.codeBuffer.reset();
                asm.csrrs(dest, 0, src);
                final String assemblyInstruction = "csrrs x" + dest.getEncoding() + "," + 0 + "," + "x" + src.getEncoding();
                assertEquals(assemblyInstruction, assemble(assemblyInstruction), asm.codeBuffer.getInt(0));
            }
        }
    }

    @Test
    public void csrrc() {
        for (CiRegister dest: RISCV64.cpuRegisters) {
            for (CiRegister src : RISCV64.cpuRegisters) {
                asm.codeBuffer.reset();
                asm.csrrc(dest, 0, src);
                final String assemblyInstruction = "csrrc x" + dest.getEncoding() + "," + 0 + "," + "x" + src.getEncoding();
                assertEquals(assemblyInstruction, assemble(assemblyInstruction), asm.codeBuffer.getInt(0));
            }
        }
    }

    @Test
    public void csrrwi() {
        for (CiRegister dest: RISCV64.cpuRegisters) {
            asm.codeBuffer.reset();
            asm.csrrwi(dest, 1, 0);
            final String assemblyInstruction = "csrrwi x" + dest.getEncoding() + "," + 1 + "," + 0;
            assertEquals(assemblyInstruction, assemble(assemblyInstruction), asm.codeBuffer.getInt(0));
        }
    }

    @Test
    public void csrrsi() {
        for (CiRegister dest: RISCV64.cpuRegisters) {
            asm.codeBuffer.reset();
            asm.csrrsi(dest, 1, 0);
            final String assemblyInstruction = "csrrsi x" + dest.getEncoding() + "," + 1 + "," + 0;
            assertEquals(assemblyInstruction, assemble(assemblyInstruction), asm.codeBuffer.getInt(0));
        }
    }

    @Test
    public void csrrci() {
        for (CiRegister dest: RISCV64.cpuRegisters) {
            asm.codeBuffer.reset();
            asm.csrrci(dest, 1, 0);
            final String assemblyInstruction = "csrrci x" + dest.getEncoding() + "," + 1 + "," + 0;
            assertEquals(assemblyInstruction, assemble(assemblyInstruction), asm.codeBuffer.getInt(0));
        }
    }


    @Test
    public void fence() {
        for (int predecessor = 1; predecessor < 0b1111; predecessor++) {
            for (int successor = 1; successor < 0b1111; successor++) {
                asm.codeBuffer.reset();
                asm.fence(predecessor, successor);
                final StringBuilder assemblyInstruction = new StringBuilder("fence ");
                appendFenceMask(predecessor, assemblyInstruction);
                assemblyInstruction.append(',');
                appendFenceMask(successor, assemblyInstruction);
                assertEquals(assemblyInstruction.toString(), assemble(assemblyInstruction.toString()), asm.codeBuffer.getInt(0));
            }
        }
    }

    private void appendFenceMask(int fenceMask, StringBuilder fenceInstruction) {
        if ((fenceMask & 0b1000) != 0) {
            fenceInstruction.append('i');
        }
        if ((fenceMask & 0b0100) != 0) {
            fenceInstruction.append('o');
        }
        if ((fenceMask & 0b0010) != 0) {
            fenceInstruction.append('r');
        }
        if ((fenceMask & 0b0001) != 0) {
            fenceInstruction.append('w');
        }
    }
}
