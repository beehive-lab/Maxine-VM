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
package com.oracle.max.vm.tests.crossisa.armv7.asm;

import com.oracle.max.vm.tests.crossisa.*;

public class MaxineARMv7Tester extends CrossISATester {

    public static final int NUM_REGS = 52;

    /*
     * arm-unknown-eabi-gcc -DSTATIC -mfloat-abi=hard -mfpu=vfpv3-d16 -march=armv7-a -nostdlib -nostartfiles -g -Ttest_armv7.ld startup_armv7.s test_armv7.c -o test.elf
     */

    @Override
    protected ProcessBuilder getCompilerProcessBuilder() {
        if (gccProcessBuilder != null) {
            return gccProcessBuilder;
        }
        return new ProcessBuilder("arm-none-eabi-gcc", "-DSTATIC", "-mfloat-abi=hard", "-mfpu=vfpv3-d16",
                                  "-march=armv7-a", "-nostdlib", "-nostartfiles", "-g", "-Ttest_armv7.ld",
                                  "startup_armv7.s", "test_armv7.c", "-o", "test.elf");
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
        return new ProcessBuilder("qemu-system-arm", "-cpu", "cortex-a15", "-M", "versatilepb", "-m", "128M",
                                  "-nographic", "-s", "-S", "-kernel", "test.elf");
    }

    @Override
    public void runSimulation() throws Exception {
        super.runSimulation();
        parseIntRegisters("r0  ", "cpsr");
        parseFloatRegisters("s0  ", "s31");
        parseDoubleRegisters("d0  ", "d31");
    }

    public MaxineARMv7Tester(int[] expected, boolean[] test, BitsFlag[] range) {
        super();
        initializeQemu();
        bitMasks = range;
        expectedIntRegisters = expected;
        testIntRegisters = test;
    }

    public MaxineARMv7Tester() {
        super();
        initializeQemu();
    }

    private MaxineARMv7Tester(String[] args) {
        super();
        initializeQemu();
        expectedIntRegisters = new int[Integer.parseInt(args[0])];
        testIntRegisters = new boolean[Integer.parseInt(args[0])];

        for (int i = 1; i < args.length; i += 2) {
            expectedIntRegisters[Integer.parseInt(args[i])] = Integer.parseInt(args[i + 1]);
            testIntRegisters[Integer.parseInt(args[i])] = true;
        }
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
        String value = line.split("\\s+")[1];

        if (value.contains("nan")) {
            return Float.NaN;
        } else if (value.equals("inf")) {
            return Float.POSITIVE_INFINITY;
        } else if (value.equals("-inf")) {
            return Float.NEGATIVE_INFINITY;
        }

        return Float.parseFloat(value);
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
    protected double parseDoubleRegister(String line) {
        String value = line.split("\\s+")[23];
        value = value.substring(2, value.length() - 1);
        return Double.longBitsToDouble(hexToLongBits(value));
    }

    public static void main(String[] args) throws Exception {
        MaxineARMv7Tester tester = new MaxineARMv7Tester(args);
        tester.run();
    }
}
