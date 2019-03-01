/*
 * Copyright (c) 2018-2019, APT Group, School of Computer Science,
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

import com.oracle.max.vm.tests.crossisa.CrossISATester;
import com.sun.cri.ci.CiRegister;
import com.sun.max.vm.runtime.FatalError;

import static com.oracle.max.vm.tests.crossisa.CrossISATester.BitsFlag.All64Bits;
import static com.oracle.max.vm.tests.crossisa.CrossISATester.BitsFlag.Lower32Bits;
import static java.lang.Enum.valueOf;

public class MaxineRISCV64Tester extends CrossISATester {
    public static final int NUM_REGS = 32;

    /*
     * riscv64-linux-gnu-gcc -g -march=rv64g -mabi=lp64d -static -mcmodel=medany -fvisibility=hidden -nostdlib -nostartfiles -Ttest_riscv64.ld startup_riscv64.s test_riscv64.c -o test.elf
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

    public MaxineRISCV64Tester() {
        super();
        initializeQemu();
    }

    @Override
    protected ProcessBuilder getCompilerProcessBuilder() {
        if (gccProcessBuilder != null) {
            return gccProcessBuilder;
        }
        return new ProcessBuilder("riscv64-linux-gnu-gcc", "-g", "-march=rv64g", "-mabi=lp64d", "-static",
                                  "-mcmodel=medany", "-fvisibility=hidden", "-nostdlib", "-nostartfiles",
                                  "-Ttest_riscv64.ld", "startup_riscv64.s", "test_riscv64.c", "-o", "test.elf");
    }

    @Override
    protected ProcessBuilder getLinkerProcessBuilder() {
        if (linkerProcessBuilder != null) {
            return linkerProcessBuilder;
        }
        return new ProcessBuilder("true");
    }

    @Override
    protected ProcessBuilder getGDBProcessBuilder() {
        if (gdbProcessBuilder != null) {
            return gdbProcessBuilder;
        }
        return new ProcessBuilder("riscv64-elf-gdb", "-q", "-x", gdbInput);
    }

    @Override
    protected ProcessBuilder getQEMUProcessBuilder() {
        if (qemuProcessBuilder != null) {
            return qemuProcessBuilder;
        }
        return new ProcessBuilder("qemu-system-riscv64", "-M", "virt", "-m", "128M", "-nographic", "-s", "-S", "-kernel", "test.elf");
    }

    public long[] runRegisteredSimulation() throws Exception {
        runSimulation();
        return simulatedLongRegisters;
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param cpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    @Override
    public void setExpectedValue(CiRegister cpuRegister, long expectedValue) {
        final int index = cpuRegister.getEncoding() - 1; // -1 to compensate for the zero register
        expectedLongRegisters[index] = expectedValue;
        testLongRegisters[index] = true;
        bitMasks[index] = All64Bits;
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param cpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    @Override
    public void setExpectedValue(CiRegister cpuRegister, int expectedValue) {
        final int index = cpuRegister.getEncoding() - 1; // -1 to compensate for the zero register
        expectedLongRegisters[index] = expectedValue;
        testLongRegisters[index] = true;
        bitMasks[index] = Lower32Bits;
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param fpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    @Override
    public void setExpectedValue(CiRegister fpuRegister, float expectedValue) {
        final int index = fpuRegister.getEncoding();
        expectedFloatRegisters[index] = expectedValue;
        testFloatRegisters[index] = true;
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param fpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    @Override
    public void setExpectedValue(CiRegister fpuRegister, double expectedValue) {
        final int index = fpuRegister.getEncoding();
        expectedDoubleRegisters[index] = expectedValue;
        testDoubleRegisters[index] = true;
    }

    /**
     * Parses a float register (32-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *     TODO
     * </pre>
     *
     * @param line The line from the gdb output to be parsed
     * @return The parsed float value of the register
     */
    @Override
    protected float parseFloatRegister(String line) {
        try {
            if (line.contains("nan")) {
                String number = line.split("\\s+-nan\\(0xfffff")[1];
                number = number.split("\\)")[0];
                return Float.intBitsToFloat(Integer.parseUnsignedInt(number, 16));
            } else {
                double number = Double.parseDouble(line.split("\\s+")[1]);
                return Float.intBitsToFloat((int) Double.doubleToRawLongBits(number));
            }
        } catch (Exception e) {
            System.out.println("Float: GDB output line could not be parsed: " + line);
        }

        return 0;
    }

    /**
     * Parses a double register (64-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *     TODO
     * </pre>
     *
     * @param line The line from the gdb output to be parsed
     * @return The parsed double value of the register
     */
    protected double parseDoubleRegister(String line) {
        try {
            String value = line.split("\\s+")[1];
            if ("inf".equals(value)) {
                return Double.POSITIVE_INFINITY;
            } else if ("-inf".equals(value)) {
                return Double.NEGATIVE_INFINITY;
            } else {
                return Double.parseDouble(value);
            }
        } catch (Exception e) {
            System.out.println("Double: GDB output line could not be parsed: " + line);
        }

        return 0.0;
    }

    public void runSimulation() throws Exception {
        super.runSimulation();
        parseLongRegisters("ra ", "pc");
        parseFloatRegisters("ft0 ", "ustatus");
        parseDoubleRegisters("ft0 ", "ustatus");
    }

    public static void main(String[] args) throws Exception {
        MaxineRISCV64Tester tester = new MaxineRISCV64Tester(args);
        tester.run();
    }
}
