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

    public static boolean DEBUGOBJECTS = false;
    public static final int NUM_REGS = 17;

    /*
     * arm-unknown-eabi-gcc -c -march=armv7-a -g test_armv7.c -o test_armv7.o
     * arm-unknown-eabi-as -mcpu=cortex-a9 -g startup_armv7.s -o startup_armv7.o
     * arm-unknown-eabi-ld -T test_armv7.ld test_armv7.o startup_armv7.o -o test.elf
     * qemu-system-arm -cpu cortex-a9 -M versatilepb -m 128M -nographic -s -S -kernel test.elf
     */

    public void newCompile() {
        final ProcessBuilder removeFiles = new ProcessBuilder("/bin/rm", "-rR", "test.elf");
        final ProcessBuilder compile = new ProcessBuilder("arm-none-eabi-gcc", "-c", "-march=armv7-a", "-mfloat-abi=hard", "-mfpu=vfpv3-d16", "-g", "test_armv7.c", "-o", "test_armv7.o");
        compile.redirectOutput(gccOutput);
        compile.redirectError(gccErrors);
        try {
            removeFiles.start().waitFor();
            gcc = compile.start();
            gcc.waitFor();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

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

    private void runSimulation(boolean captureFPREGs) throws Exception {
        ProcessBuilder gdbProcess = new ProcessBuilder("arm-none-eabi-gdb", "-q", "-x", captureFPREGs ? gdbInputFPREGS : gdbInput);
        ProcessBuilder qemuProcess = new ProcessBuilder("qemu-system-arm", "-cpu", "cortex-a15", "-M", "versatilepb", "-m", "128M", "-nographic", "-s", "-S", "-kernel", "test.elf");
        runSimulation(gdbProcess, qemuProcess);
    }

    public Object[] runObjectRegisteredSimulation() throws Exception {
        runSimulation(true);
        return parseObjectRegistersToFile(gdbOutput.getName());
    }

    @Override
    public long[] runRegisteredSimulation() throws Exception {
        runSimulation(false);
        long[] simulatedValues = parseRegistersToFile(gdbOutput.getName(), "r0  ", "cpsr");
        // Treat values as ints not longs
        for (int i = 0; i < simulatedValues.length; i++) {
            if (simulatedValues[i] > Integer.MAX_VALUE) {
                simulatedValues[i] = (int) (2L * Integer.MIN_VALUE + simulatedValues[i]);
            }
        }
        return simulatedValues;
    }

    public MaxineARMv7Tester(int[] expected, boolean[] test, BitsFlag[] range) {
        super(NUM_REGS);
        initializeQemu();
        bitMasks = range;
        for (int i = 0; i < NUM_REGS; i++) {
            expectRegs[i] = expected[i];
            testRegs[i] = test[i];
        }
    }

    public MaxineARMv7Tester(long[] expected, boolean[] test, BitsFlag[] range) {
        super(NUM_REGS);
        initializeQemu();
        bitMasks = range;
        int j = 0;
        for (int i = 0; i < NUM_REGS; i++) {
            if (test[i]) {
                expectRegs[j] = (int) ((expected[i] >> 32));
                expectRegs[j + 1] = (int) (expected[i]);
                testRegs[j] = testRegs[j + 1] = test[i];
                j = +2;
            }
        }
    }
    public MaxineARMv7Tester() {
        super(NUM_REGS);
        initializeQemu();
        for (int i = 0; i < NUM_REGS; i++) {
            testRegs[i] = false;
        }
    }

    public MaxineARMv7Tester(String[] args) {
        super(NUM_REGS, args);
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
            if (DEBUGOBJECTS) {
                System.out.println(" CORE " + i + " " + ((Integer) expectedValues[i]).intValue());
            }
            i++;
            if (line.contains("cpsr")) {
                enabled = false;
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
                            expectedValues[i++] = new Double(Double.POSITIVE_INFINITY);
                        } else if (str.equals("-inf")) {
                            expectedValues[i++] = new Double(Double.NEGATIVE_INFINITY);
                        } else {
                            expectedValues[i++] = new Double(Double.NaN);
                        }
                    }
                    break;
                }
            }
            if (DEBUGOBJECTS) {
                System.out.println(" DOUBLE " + (i - 1) + " " + ((Double) expectedValues[i - 1]).doubleValue());
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
            if (DEBUGOBJECTS) {
                System.out.println(" FLOAT " + (i - 1) + " " + expectedValues[i - 1]);
            }
        }
        return expectedValues;
    }

    public static void main(String[] args) throws Exception {
        MaxineARMv7Tester tester = new MaxineARMv7Tester(args);
        tester.run();
    }
}
