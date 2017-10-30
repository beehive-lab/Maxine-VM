/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
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
package test.aarch64.asm;

import java.io.*;
import java.math.*;

public class MaxineAarch64Tester {

    public static boolean DEBUG = true;
    public static boolean RESET = false;
    public static boolean DEBUGOBJECTS = false;
    public static final String ENABLE_QEMU = "max.arm.qemu";
    public static boolean ENABLE_SIMULATOR = true;
    public static final int NUM_REGS = 31;
    public static final int NUM_SIMD_REGS = 32;
    public static final int NUM_DP_REGS = 16;

    private static String[] regs = new String[]{"X00", "X01", "X02", "X03", "X04", "X05", "X06", "X07", "X08", "X09",
        "X10", "X11", "X12", "X13", "X14", "X15", "X16", "X17", "X18", "X19", "X20", "X21", "X22", "X23", "X24",
        "X25", "X26", "X27", "X28", "X29", "X30"};

    public enum BitsFlag {
        Bit0(0x1), Bit1(0x2), Bit2(0x4), Bit3(0x8), Bit4(0x10), Bit5(0x20), Bit6(0x40), Bit7(0x80), Bit8(0x100),
        Bit9(0x200), Bit10(0x400), Bit11(0x800), Bit12(0x1000), Bit13(0x2000), Bit14(0x4000), Bit15(0x8000),
        Bit16(0x10000), Bit17(0x20000), Bit18(0x40000), Bit19(0x80000), Bit20(0x100000), Bit21(0x200000),
        Bit22(0x400000), Bit23(0x800000), Bit24(0x1000000), Bit25(0x2000000), Bit26(0x4000000), Bit27(0x8000000),
        Bit28(0x10000000), Bit29(0x20000000), Bit30(0x40000000), Bit31(0x80000000), NZCBits(0xe0000000),
        NZCVBits(0xf0000000), Lower16Bits(0x0000ffff), Upper16Bits(0xffff0000), All32Bits(0xffffffff);

        public static final BitsFlag[] values = values();
        private final long value;

        BitsFlag(long value) {
            this.value = value;
        }

        public long value() {
            return value;
        }
    }

    private static final File qemuOutput    = new File("qemu_output");
    private static final File qemuErrors    = new File("qemu_errors");
    private static final File bindOutput    = new File("bind_output");
    private static final File gdbOutput     = new File("gdb_output");
    private static final String gdbInput    = "gdb_input";
    private static final File gdbErrors     = new File("gdb_errors");
    private static final File gccOutput     = new File("gcc_output");
    private static final File gccErrors     = new File("gcc_errors");
    private static final File asOutput      = new File("as_output");
    private static final File asErrors      = new File("as_errors");
    private static final File linkOutput    = new File("link_output");
    private static final File linkErrors    = new File("link_errors");

    private Process gcc;
    private Process assembler;
    private Process linker;
    private Process qemu;
    private Process gdb;
    private BitsFlag[] bitMasks;
    private long[]    expectRegs = new long[NUM_REGS];
    private boolean[] testRegs   = new boolean[NUM_REGS];
    public static int oldpos = 0;

    /*
     * arm-unknown-eabi-gcc -c -march=armv8-a -g test_aarch64.c -o test_aarch64.o
     * aarch64-none-elf-as -march=armv8-a -g startup_aarch64.s -o startup_aarch64.o
     * aarch64-none-elf-ld -T test_aarch64.ld test_aarch64.o startup_aarch64.o -o test.elf
     * qemu-system-aarch64 -cpu cortex-a57 -M versatilepb -m 128M -nographic -s -S -kernel test.elf
     */
    public MaxineAarch64Tester(String[] args) {
        initializeQemu();
        for (int i = 0; i < NUM_REGS; i++) {
            testRegs[i] = false;
        }
        for (int i = 0; i < args.length; i += 2) {
            expectRegs[Integer.parseInt(args[i])] = Integer.parseInt(args[i + 1]);
            testRegs[Integer.parseInt(args[i])] = true;
        }
    }

    public MaxineAarch64Tester(long[] expected, boolean[] test, BitsFlag[] range) {
        initializeQemu();
        bitMasks = range;
        for (int i = 0; i < NUM_REGS; i++) {
            expectRegs[i] = expected[i];
            testRegs[i] = test[i];
        }
    }

    public void reset() {
        if (RESET) {
            cleanFiles();
        }
        cleanProcesses();
    }

    public void cleanFiles() {
        deleteFile(qemuOutput);
        deleteFile(qemuErrors);
        deleteFile(bindOutput);
        deleteFile(gdbOutput);
        deleteFile(gdbErrors);
        deleteFile(gccOutput);
        deleteFile(gccErrors);
        deleteFile(asOutput);
        deleteFile(asErrors);
        deleteFile(linkOutput);
        deleteFile(linkErrors);
    }

    private void deleteFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    public void compile() {
        // aarch64-none-elf-gcc -c -march=armv8-a+simd -mgeneral-regs-only -g test_aarch64.c -o test_aarch64.o
        final ProcessBuilder removeFiles = new ProcessBuilder("/bin/rm", "-rR", "test.elf");
        final ProcessBuilder compile = new ProcessBuilder("aarch64-none-elf-gcc", "-c", "-march=armv8-a+simd",
                "-mgeneral-regs-only", "-g", "test_aarch64.c", "-o", "test_aarch64.o");
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

    public void assembleStartup() {
        // aarch64-none-elf-as -march=armv8-a -g startup_aarch64.s -o
        // startup_aarch64.o
        final ProcessBuilder assemble = new ProcessBuilder("aarch64-none-elf-as", "-march=armv8-a", "-g",
                "startup_aarch64.s", "-o", "startup_aarch64.o");
        assemble.redirectOutput(new File("as_output"));
        assemble.redirectError(new File("as_errors"));
        try {
            assembler = assemble.start();
            assembler.waitFor();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    public void link() {
        // aarch64-none-elf-ld -T test_aarch64.ld test_aarch64.o startup_aarch64.o -o test.elf
        final ProcessBuilder link = new ProcessBuilder("aarch64-none-elf-ld", "-T", "test_aarch64.ld", "test_aarch64.o",
                "startup_aarch64.o", "-o", "test.elf");
        link.redirectOutput(linkOutput);
        link.redirectError(linkErrors);
        try {
            linker = link.start();
            linker.waitFor();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    public void cleanProcesses() {
        terminateProcess(gcc);
        terminateProcess(assembler);
        terminateProcess(linker);
        terminateProcess(gdb);
        terminateProcess(qemu);
    }

    private void terminateProcess(Process process) {
        if (process != null) {
            process.destroy();
        }
    }

    public long[] runRegisteredSimulation() throws Exception {
        ProcessBuilder gdbProcess = new ProcessBuilder("aarch64-none-elf-gdb", "-q", "-x", gdbInput);
        gdbProcess.redirectOutput(gdbOutput);
        gdbProcess.redirectError(gdbErrors);
        ProcessBuilder qemuProcess = new ProcessBuilder("qemu-system-aarch64", "-cpu", "cortex-a57", "-M", "virt", "-m",
                "128M", "-nographic", "-s", "-S", "-kernel", "test.elf");
        qemuProcess.redirectOutput(qemuOutput);
        qemuProcess.redirectError(qemuErrors);
        try {
            qemu = qemuProcess.start();
            while (!qemuOutput.exists()) {
                Thread.sleep(500);
            }
            do {
                ProcessBuilder bindTest = new ProcessBuilder("lsof", "-i", "TCP:1234");
                bindTest.redirectOutput(bindOutput);
                bindTest.start().waitFor();
                FileInputStream inputStream = new FileInputStream(bindOutput);
                if (inputStream.available() != 0) {
                    log("MaxineAarch64Tester:: qemu ready");
                    inputStream.close();
                    break;
                } else {
                    log("MaxineAarch64Tester: gemu not ready");
                    Thread.sleep(500);
                }
            } while (true);
            gdbProcess.start().waitFor();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            cleanProcesses();
            System.exit(-1);
        }
        long[] simulatedRegisters = parseRegistersToFile(gdbOutput.getName());
        return simulatedRegisters;
    }

    public void runSimulation() throws Exception {
        // qemu-system-aarch64 -cpu cortex-a57 -M virt -m 128M -nographic -s -S -kernel test.elf
        cleanFiles();
        ProcessBuilder gdbProcess = new ProcessBuilder("aarch64-none-elf-gdb", "-q", "-x", gdbInput);
        gdbProcess.redirectOutput(gdbOutput);
        gdbProcess.redirectError(gdbErrors);
        System.out.println(gdbProcess.command().toString());
        ProcessBuilder qemuProcess = new ProcessBuilder("qemu-system-aarch64", "-cpu", "cortex-a57", "-M", "virt", "-m",
                "128M", "-nographic", "-s", "-S", "-kernel", "test.elf");
        qemuProcess.redirectOutput(qemuOutput);
        qemuProcess.redirectError(qemuErrors);
        try {
            qemu = qemuProcess.start();
            while (!qemuOutput.exists()) {
                Thread.sleep(500);
            }
            do {
                ProcessBuilder binder = new ProcessBuilder("lsof", "-i", "TCP:1234");
                binder.redirectOutput(bindOutput);
                binder.start().waitFor();
                FileInputStream inputStream = new FileInputStream(bindOutput);
                if (inputStream.available() != 0) {
                    log("MaxineAarch64Tester:: qemu ready");
                    inputStream.close();
                    break;
                } else {
                    log("MaxineAarch64Tester: gemu not ready");
                    Thread.sleep(500);
                }
            } while (true);
            gdb = gdbProcess.start();
            gdb.waitFor();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            cleanProcesses();
            System.exit(-1);
        }
        long[] simulatedRegisters = parseRegistersToFile(gdbOutput.getName());
        if (!validateRegisters(simulatedRegisters, expectRegs, testRegs)) {
            cleanProcesses();
            assert false : "Error while validating registers";
        }
    }

    private boolean validateRegisters(long[] simRegisters, long[] expectedRegisters, boolean[] testRegisters) {
        for (int i = 0; i < NUM_REGS; i++) {
            log(i + " sim: " + simRegisters[i] + " exp: " + expectedRegisters[i] + " test: " + testRegisters[i]);
        }
        boolean result  = true;
        long    bitmask = 0;
        for (int i = 0; i < NUM_REGS; i++) {
            long simulatedRegister = simRegisters[i] & bitMasks[i].value();
            long expectedRegister  = expectedRegisters[i];
            if (testRegisters[i]) {
                if (simulatedRegister != expectedRegister) {
                    bitmask = bitmask | (1 << i);
                    result = false;
                }
            }
        }
        if (!result) {
            for (int i = 0; i < NUM_REGS; i++) {
                System.out.println(i + " sim: " + simRegisters[i] + " exp: " + expectedRegisters[i] + " test: "
                        + testRegisters[i]);
            }
        }
        return result;
    }

    private void initializeQemu() {
        ENABLE_SIMULATOR = Integer.getInteger(ENABLE_QEMU) != null && Integer.getInteger(ENABLE_QEMU) > 0 ? true
                : false;
    }

    private long[] parseRegistersToFile(String file) throws IOException {
        BufferedReader reader         = new BufferedReader(new FileReader(file));
        String         line           = null;
        boolean        enabled        = false;
        int            i              = 0;
        long[]         expectedValues = new long[NUM_REGS];
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("x0 ")) {
                enabled = true;
                line = line.substring(6, line.length());
            }
            if (!enabled) {
                continue;
            }
            String value = line.split("\\s+")[1];

            long tmp = new BigInteger(value.substring(2, value.length()).toString(), 16).longValue();
            if (tmp > Long.MAX_VALUE) {
                expectedValues[i] = (long) (2L * Long.MIN_VALUE + tmp);
            } else {
                expectedValues[i] = (long) tmp;
            }
            if (++i >= NUM_REGS) {
                break;
            }
            if (line.contains("sp")) {
                enabled = false;
            }
        }
        reader.close();
        return expectedValues;
    }

    public static void enableDebug() {
        DEBUG = true;
    }

    public static void disableDebug() {
        DEBUG = false;
    }

    public static void main(String[] args) throws Exception {
        MaxineAarch64Tester tester = new MaxineAarch64Tester(args);
        tester.run();
    }

    public void run() throws Exception {
        assembleStartup();
        compile();
        link();
        runSimulation();
    }

    private void log(String msg) {
        if (DEBUG) {
            System.out.println(msg);
        }
    }
}
