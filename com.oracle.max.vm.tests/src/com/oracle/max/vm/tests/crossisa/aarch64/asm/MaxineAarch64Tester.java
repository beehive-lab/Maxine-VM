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
package com.oracle.max.vm.tests.crossisa.aarch64.asm;

import com.oracle.max.vm.tests.crossisa.CrossISATester;

public class MaxineAarch64Tester extends CrossISATester {

    public static final int NUM_REGS = 32;

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

    public MaxineAarch64Tester(long[] expected, boolean[] test, BitsFlag[] range,
                               float[] expectedFloats, boolean[] testFloats) {
        super();
        initializeQemu();
        bitMasks = range;
        expectedLongRegisters = expected;
        expectedFloatRegisters = expectedFloats;
        testLongRegisters = test;
        testFloatRegisters = testFloats;
    }

    public MaxineAarch64Tester() {
        super();
        initializeQemu();
    }

    /**
     * Assembles the instructions in contained in the assembly file into the object file.
     * @param assembly
     * @param object
     */
    public void assemble(String assembly, String object) {
        final ProcessBuilder assemble = new ProcessBuilder("aarch64-linux-gnu-as", "-march=armv8-a", "-g",
                        assembly, "-o", object);
        assemble.redirectOutput(asOutput);
        assemble.redirectError(asErrors);
        assembler = runBlocking(assemble);
    }

    /**
     * Links object files using the specified linker script.
     * @param linkScript -- name of the file
     * @param objects -- names of the object files
     */
    public void link(String linkScript, String... objects) {
        String [] args = {"aarch64-linux-gnu-ld", "-T", linkScript, "-o", "test.elf"};
        String [] fullargs = new String[args.length + objects.length];
        System.arraycopy(args, 0, fullargs, 0, args.length);
        System.arraycopy(objects, 0, fullargs, args.length, objects.length);
        final ProcessBuilder link = new ProcessBuilder(fullargs);
        link.redirectOutput(linkOutput);
        link.redirectError(linkErrors);
        linker = runBlocking(link);
    }

    @Override
    protected ProcessBuilder getCompilerProcessBuilder() {
        // aarch64-linux-gnu-gcc -march=armv8-a+simd -nostdlib -nostartfiles -g -Ttest_aarch64.ld startup_aarch64.s test_aarch64.c -o test.elf
        if (gccProcessBuilder != null) {
            return gccProcessBuilder;
        }
        return new ProcessBuilder("aarch64-linux-gnu-gcc", "-march=armv8-a+simd", "-nostdlib", "-nostartfiles", "-g",
                                  "-Ttest_aarch64.ld", "startup_aarch64.s", "test_aarch64.c", "-o", "test.elf");
    }

    @Override
    protected ProcessBuilder getLinkerProcessBuilder() {
        if (linkerProcessBuilder != null) {
            return linkerProcessBuilder;
        }
        return new ProcessBuilder("true");
    }

    protected ProcessBuilder getQEMUProcessBuilder() {
        if (qemuProcessBuilder != null) {
            return qemuProcessBuilder;
        }
        return new ProcessBuilder("qemu-system-aarch64", "-cpu", "cortex-a57", "-M", "virt", "-m", "128M", "-nographic",
                "-s", "-S", "-kernel", "test.elf");
    }

    public long[] runRegisteredSimulation() throws Exception {
        runSimulation();
        return simulatedLongRegisters;
    }

    /**
     * Parses a float register (32-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *     s0  1.55405596e-31  (raw 0x0c49ba5e)
     * </pre>
     *
     * @param line The line from the gdb output to be parsed
     * @return The parsed float value of the register
     */
    @Override
    protected float parseFloatRegister(String line) {
        String value = line.split("\\s+")[9];
        value = value.substring(2, value.length() - 1);
        return Float.intBitsToFloat(hexToIntBits(value));
    }

    /**
     * Parses a double register (64-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *     d0  {u8 = {0x5e, 0xba, 0x49, 0xc, 0x2, 0x40, 0x8f, 0x40}, u16 = {0xba5e, 0xc49, 0x4002, 0x408f}, u32 = {0xc49ba5e, 0x408f4002}, u64 = 0x408f40020c49ba5e, f32 = {0x0, 0x4}, f64 = 0x3e8}
     * </pre>
     *
     * @param line The line from the gdb output to be parsed
     * @return The parsed double value of the register
     */
    @Override
    protected double parseDoubleRegister(String line) {
        String value = line.split("\\s+")[9];
        value = value.substring(2, value.length() - 1);
        return Double.longBitsToDouble(hexToLongBits(value));
    }

    @Override
    public void runSimulation() throws Exception {
        super.runSimulation();
        parseLongRegisters("x0 ", "sp");
        parseFloatRegisters("s0 ", "s31");
        parseDoubleRegisters("d0 ", "d31");
    }

    public static void main(String[] args) throws Exception {
        MaxineAarch64Tester tester = new MaxineAarch64Tester(args);
        tester.run();
    }

}
