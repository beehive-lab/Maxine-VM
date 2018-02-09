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

import test.crossisa.*;

public class MaxineRISCV64Tester extends CrossISATester {
    public static final int NUM_REGS = 32;

    /*
     * riscv64-unknown-elf-gcc -g -march=rv64g -mabi=lp64d -static -mcmodel=medany -fvisibility=hidden -nostdlib -nostartfiles -Ttest_riscv64.ld startup_riscv64.s test_riscv64.c -o test.elf
     * qemu-system-riscv64 -nographic -S -s -kernel test.elf
     */
    public MaxineRISCV64Tester(String[] args) {
        super(NUM_REGS, args);
    }

    public MaxineRISCV64Tester(long[] expected, boolean[] test, BitsFlag[] range) {
        super(NUM_REGS);
        initializeQemu();
        bitMasks = range;
        for (int i = 0; i < NUM_REGS; i++) {
            expectRegs[i] = expected[i];
            testRegs[i] = test[i];
        }
    }

    @Override
    protected ProcessBuilder getCompilerProcessBuilder() {
        return new ProcessBuilder("riscv64-unknown-elf-gcc", "-g", "-march=rv64g", "-mabi=lp64d", "-static",
                                  "-mcmodel=medany", "-fvisibility=hidden", "-nostdlib", "-nostartfiles",
                                  "-Ttest_riscv64.ld", "startup_riscv64.s", "test_riscv64.c", "-o", "test.elf");
    }

    @Override
    protected ProcessBuilder getAssemblerProcessBuilder() {
        return new ProcessBuilder("true");
    }

    @Override
    protected ProcessBuilder getLinkerProcessBuilder() {
        return new ProcessBuilder("true");
    }

    public long[] runRegisteredSimulation() throws Exception {
        ProcessBuilder gdbProcess = new ProcessBuilder("riscv64-unknown-elf-gdb", "-q", "-x", "gdb_input_riscv");
        ProcessBuilder qemuProcess = new ProcessBuilder("qemu-system-riscv64", "-M", "virt", "-m", "128M", "-nographic", "-s", "-S", "-kernel", "test.elf");
        runSimulation(gdbProcess, qemuProcess);
        return parseRegistersToFile(gdbOutput.getName(), "ra ", "pc", NUM_REGS);
    }

    public static void main(String[] args) throws Exception {
        MaxineRISCV64Tester tester = new MaxineRISCV64Tester(args);
        tester.run();
    }

}
