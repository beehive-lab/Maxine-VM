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
    private MaxineRISCV64Tester(String[] args) {
        super();
        initializeQemu();
        expectedLongRegisters = new long[Integer.parseInt(args[0])];
        testIntRegisters = new boolean[Integer.parseInt(args[0])];

        for (int i = 1; i < args.length; i += 2) {
            expectedLongRegisters[Integer.parseInt(args[i])] = Long.parseLong(args[i + 1]);
            testIntRegisters[Integer.parseInt(args[i])] = true;
        }
    }

    public MaxineRISCV64Tester(long[] expected, boolean[] test, BitsFlag[] range) {
        super();
        initializeQemu();
        bitMasks = range;
        expectedLongRegisters = expected;
        testLongRegisters = test;
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

    public void runSimulation() throws Exception {
        ProcessBuilder gdbProcess = new ProcessBuilder("riscv64-unknown-elf-gdb", "-q", "-x", "gdb_input_riscv");
        ProcessBuilder qemuProcess = new ProcessBuilder("qemu-system-riscv64", "-M", "virt", "-m", "128M", "-nographic", "-s", "-S", "-kernel", "test.elf");
        runSimulation(gdbProcess, qemuProcess);
        parseLongRegisters("ra ", "pc");
    }

    public static void main(String[] args) throws Exception {
        MaxineRISCV64Tester tester = new MaxineRISCV64Tester(args);
        tester.run();
    }

}
