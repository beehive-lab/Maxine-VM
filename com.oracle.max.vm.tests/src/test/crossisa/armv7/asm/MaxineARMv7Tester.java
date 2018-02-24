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
package test.crossisa.armv7.asm;

import java.io.*;

import test.crossisa.*;

public class MaxineARMv7Tester extends CrossISATester {

    public static final int NUM_REGS = 52;

    /*
     * arm-unknown-eabi-gcc -c -march=armv7-a -g test_armv7.c -o test_armv7.o
     * arm-unknown-eabi-as -mcpu=cortex-a9 -g startup_armv7.s -o startup_armv7.o
     * arm-unknown-eabi-ld -T test_armv7.ld test_armv7.o startup_armv7.o -o test.elf
     * qemu-system-arm -cpu cortex-a9 -M versatilepb -m 128M -nographic -s -S -kernel test.elf
     */

    @Override
    protected ProcessBuilder getCompilerProcessBuilder() {
        return new ProcessBuilder("arm-none-eabi-gcc", "-c", "-DSTATIC", "-mfloat-abi=hard", "-mfpu=vfpv3-d16",
                                  "-march=armv7-a", "-g", "test_armv7.c", "-o", "test_armv7.o");
    }

    @Override
    protected ProcessBuilder getAssemblerProcessBuilder() {
        return new ProcessBuilder("arm-none-eabi-as", "-mcpu=cortex-a15", "-mfloat-abi=hard", "-mfpu=vfpv3-d16", "-g",
                                  "startup_armv7.s", "-o", "startup_armv7.o");
    }

    @Override
    protected ProcessBuilder getLinkerProcessBuilder() {
        return new ProcessBuilder("arm-none-eabi-ld", "-T", "test_armv7.ld", "test_armv7.o", "startup_armv7.o", "-o",
                                  "test.elf");
    }

    public void runSimulation(boolean captureFPREGs) throws Exception {
        ProcessBuilder gdbProcess = new ProcessBuilder("arm-none-eabi-gdb", "-q", "-x", captureFPREGs ? gdbInputFPREGS : gdbInput);
        ProcessBuilder qemuProcess = new ProcessBuilder("qemu-system-arm", "-cpu", "cortex-a15", "-M", "versatilepb", "-m", "128M", "-nographic", "-s", "-S", "-kernel", "test.elf");
        runSimulation(gdbProcess, qemuProcess);
        parseIntRegisters("r0  ", "cpsr");
        parseFloatRegisters("s0  ", "s31");
        parseDoubleRegisters("d0  ", "d31");
    }

    public Object[] runObjectRegisteredSimulation() throws Exception {
        runSimulation(true);
        return parseObjectRegistersToFile(gdbOutput.getName());
    }

    @Override
    public void runSimulation() throws Exception {
        runSimulation(false);
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

    private Object[] parseObjectRegistersToFile(String file) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        boolean enabled = false;
        int i = 0;
        Object[] expectedValues = new Object[16 + 1 + 16 + 32];
        while ((line = reader.readLine()) != null) {
            if (line.contains("r0  ")) {
                enabled = true;
                line = line.substring(12, line.length());
            }
            if (!enabled) {
                continue;
            }

            String value = line.split("\\s+")[1];
            long tmp = Long.parseLong(value.substring(2, value.length()).toString(), 16);

            if (tmp > Integer.MAX_VALUE) {
                tmp = (int) (2L * Integer.MIN_VALUE + tmp);
            } else {
                expectedValues[i] = (int) tmp;
            }
            expectedValues[i] = new Integer((int) tmp);
            i++;
            if (line.contains("cpsr")) {
                // might want to get cpsr but we dont need it right now
                expectedValues[i] = null;
                break;
            }
        }
        while ((line = reader.readLine()) != null) {
            enabled = line.contains("f64");
            if (i >= (16 + 16 + 1)) {
                break;
            }
            if (!enabled) {
                continue;
            }
            String[] values = line.split("\\s+");
            for (int j = 0; j < values.length; j++) {
                if (values[j].equals("f64")) {
                    String doubleVal = values[j + 2];
                    String str = doubleVal.substring(0, doubleVal.length() - 1);
                    try {
                        Double tmp = new Double(str);
                        expectedValues[i++] = tmp;
                    } catch (Exception e) {
                        // we get exceptions when there is a NaN
                        // currently we just set them to null
                        if (str.equals("inf")) {
                            expectedValues[i++] = Double.POSITIVE_INFINITY;
                        } else if (str.equals("-inf")) {
                            expectedValues[i++] = Double.NEGATIVE_INFINITY;
                        } else {
                            expectedValues[i++] = Double.NaN;
                        }
                    }
                    break;
                }
            }
            if (i >= (16 + 16 + 1)) {
                break;
            }
        }
        while ((line = reader.readLine()) != null) {
            enabled = line.contains("=");
            if (!enabled) {
                continue;
            }
            String[] values = line.split("\\s+");
            for (int j = 0; j < values.length; j++) {

                if (values[j].equals("=")) {
                    String doubleVal = values[j + 1];
                    try {
                        Float tmp = new Float(doubleVal);
                        expectedValues[i++] = tmp;
                    } catch (Exception e) {
                        switch (doubleVal) {
                            case "inf":
                                expectedValues[i++] = Float.POSITIVE_INFINITY;
                                break;
                            case "-inf":
                                expectedValues[i++] = Float.NEGATIVE_INFINITY;
                                break;
                            default:
                                expectedValues[i++] = Float.NaN;
                                break;
                        }
                    }
                    break;
                }
            }
            if (i == expectedValues.length) {
                break;
            }
        }
        return expectedValues;
    }

    public static void main(String[] args) throws Exception {
        MaxineARMv7Tester tester = new MaxineARMv7Tester(args);
        tester.run();
    }
}
