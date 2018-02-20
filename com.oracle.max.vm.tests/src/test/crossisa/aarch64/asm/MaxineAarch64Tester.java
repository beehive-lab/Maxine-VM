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
package test.crossisa.aarch64.asm;

import test.crossisa.*;

public class MaxineAarch64Tester extends CrossISATester {

    public static final int NUM_REGS = 31;

    /*
     * arm-unknown-eabi-gcc -c -march=armv8-a -g test_aarch64.c -o test_aarch64.o
     * aarch64-linux-gnu-as -march=armv8-a -g startup_aarch64.s -o startup_aarch64.o
     * aarch64-linux-gnu-ld -T test_aarch64.ld test_aarch64.o startup_aarch64.o -o test.elf
     * qemu-system-aarch64 -cpu cortex-a57 -M versatilepb -m 128M -nographic -s -S -kernel test.elf
     */
    private MaxineAarch64Tester(String[] args) {
        super();
        initializeQemu();
        expectedLongRegisters = new long[Integer.parseInt(args[0])];
        testIntRegisters = new boolean[Integer.parseInt(args[0])];

        for (int i = 1; i < args.length; i += 2) {
            expectedLongRegisters[Integer.parseInt(args[i])] = Long.parseLong(args[i + 1]);
            testIntRegisters[Integer.parseInt(args[i])] = true;
        }
    }

    public MaxineAarch64Tester(long[] expected, boolean[] test, BitsFlag[] range) {
        super();
        initializeQemu();
        bitMasks = range;
        expectedLongRegisters = expected;
        testLongRegisters = test;
    }

    @Override
    protected ProcessBuilder getCompilerProcessBuilder() {
        // aarch64-linux-gnu-gcc -c -march=armv8-a+simd -mgeneral-regs-only -g test_aarch64.c -o test_aarch64.o
        return new ProcessBuilder("aarch64-linux-gnu-gcc", "-c", "-march=armv8-a+simd", "-mgeneral-regs-only", "-g",
                                  "test_aarch64.c", "-o", "test_aarch64.o");
    }

    @Override
    protected ProcessBuilder getAssemblerProcessBuilder() {
        // aarch64-linux-gnu-as -march=armv8-a -g startup_aarch64.s -o startup_aarch64.o
        return new ProcessBuilder("aarch64-linux-gnu-as", "-march=armv8-a", "-g", "startup_aarch64.s",
                                  "-o", "startup_aarch64.o");
    }

    @Override
    protected ProcessBuilder getLinkerProcessBuilder() {
        // aarch64-linux-gnu-ld -T test_aarch64.ld test_aarch64.o startup_aarch64.o -o test.elf
        return new ProcessBuilder("aarch64-linux-gnu-ld", "-T", "test_aarch64.ld", "test_aarch64.o",
                "startup_aarch64.o", "-o", "test.elf");
    }

    protected ProcessBuilder getGDBProcessBuilder() {
        return new ProcessBuilder("aarch64-linux-gnu-gdb", "-q", "-x", gdbInput);
    }

    protected ProcessBuilder getQEMUProcessBuilder() {
        return new ProcessBuilder("qemu-system-aarch64", "-cpu", "cortex-a57", "-M", "virt", "-m", "128M", "-nographic",
                                  "-s", "-S", "-kernel", "test.elf");
    }

    @Override
    public void runSimulation() throws Exception {
        ProcessBuilder gdbProcess = getGDBProcessBuilder();
        ProcessBuilder qemuProcess = getQEMUProcessBuilder();
        runSimulation(gdbProcess, qemuProcess);
        parseLongRegisters("x0 ", "sp");
    }

    public static void main(String[] args) throws Exception {
        MaxineAarch64Tester tester = new MaxineAarch64Tester(args);
        tester.run();
    }

}
