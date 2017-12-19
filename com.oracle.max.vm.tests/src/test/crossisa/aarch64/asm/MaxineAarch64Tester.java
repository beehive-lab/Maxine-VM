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
    public MaxineAarch64Tester(String[] args) {
        super(NUM_REGS);
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
        super(NUM_REGS);
        initializeQemu();
        bitMasks = range;
        for (int i = 0; i < NUM_REGS; i++) {
            expectRegs[i] = expected[i];
            testRegs[i] = test[i];
        }
    }

    public void compile() {
        // aarch64-linux-gnu-gcc -c -march=armv8-a+simd -mgeneral-regs-only -g test_aarch64.c -o test_aarch64.o
        final ProcessBuilder removeFiles = new ProcessBuilder("/bin/rm", "-rR", "test.elf");
        final ProcessBuilder compile = new ProcessBuilder("aarch64-linux-gnu-gcc", "-c", "-march=armv8-a+simd",
                "-mgeneral-regs-only", "-g", "test_aarch64.c", "-o", "test_aarch64.o");
        compile.redirectOutput(gccOutput);
        compile.redirectError(gccErrors);
        runBlocking(removeFiles);
        gcc = runBlocking(compile);
    }

    public void assembleStartup() {
        // aarch64-linux-gnu-as -march=armv8-a -g startup_aarch64.s -o
        // startup_aarch64.o
        final ProcessBuilder assemble = new ProcessBuilder("aarch64-linux-gnu-as", "-march=armv8-a", "-g",
                "startup_aarch64.s", "-o", "startup_aarch64.o");
        assemble.redirectOutput(asOutput);
        assemble.redirectError(asErrors);
        assembler = runBlocking(assemble);
    }

    public void link() {
        // aarch64-linux-gnu-ld -T test_aarch64.ld test_aarch64.o startup_aarch64.o -o test.elf
        final ProcessBuilder link = new ProcessBuilder("aarch64-linux-gnu-ld", "-T", "test_aarch64.ld", "test_aarch64.o",
                "startup_aarch64.o", "-o", "test.elf");
        link.redirectOutput(linkOutput);
        link.redirectError(linkErrors);
        linker = runBlocking(link);
    }

    public long[] runRegisteredSimulation() throws Exception {
        ProcessBuilder gdbProcess = new ProcessBuilder("aarch64-linux-gnu-gdb", "-q", "-x", gdbInput);
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
            bindToQemu();
            gdbProcess.start().waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            cleanProcesses();
            System.exit(-1);
        }
        return parseRegistersToFile(gdbOutput.getName(), "x0 ", "sp");
    }

    public void runSimulation() throws Exception {
        long[] simulatedRegisters = runRegisteredSimulation();
        if (!validateRegisters(simulatedRegisters, expectRegs, testRegs)) {
            cleanProcesses();
            assert false : "Error while validating registers";
        }
    }

    public void run() throws Exception {
        assembleStartup();
        compile();
        link();
        runSimulation();
    }

    public static void main(String[] args) throws Exception {
        MaxineAarch64Tester tester = new MaxineAarch64Tester(args);
        tester.run();
    }

}
