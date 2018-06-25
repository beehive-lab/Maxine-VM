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
package com.oracle.max.asm.target.riscv;

import com.sun.cri.ci.CiRegister;
import com.sun.cri.ci.CiTarget;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

public class RISCVAssemblerTest {

    private RISCVAssembler asm;

    public RISCVAssemblerTest() {
        CiTarget risc64 = new CiTarget(new RISCV64(), true, 8, 0, 4096, 0, false, false, false, true);
        asm = new RISCVAssembler(risc64);
    }

    private int assemble(String instruction) {
        try {
            String[] cmd = { "/bin/sh", "-c", "echo \"" + instruction + "\" | riscv64-unknown-elf-as -- && riscv64-unknown-elf-objdump -D a.out | tail -n 1 | awk '{print $2}'" };
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
        final int immediates[] = {0, 0xff, 0x0fff, 0xf000, 0xff_f000, 0xffff_ffff};
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
    public void auipc() {
        final int immediates[] = {0, 0xff, 0x0fff, 0xf000, 0xff_f000, 0xffff_ffff};
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
        final int immediates[] = {0, 0xff, 0x07ff};
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


}