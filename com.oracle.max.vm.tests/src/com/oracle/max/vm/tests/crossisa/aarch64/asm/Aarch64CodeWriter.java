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
package com.oracle.max.vm.tests.crossisa.aarch64.asm;

import java.io.*;

import com.oracle.max.asm.*;

import static com.sun.max.vm.stack.JVMSFrameLayout.JVMS_SLOT_SIZE;

public class Aarch64CodeWriter {

    public static boolean debug = false;
    private int[] instructions;
    private byte[] byteVersion;
    private int totalInstructions;

    public Aarch64CodeWriter(Buffer codeBuffer) {
        totalInstructions = codeBuffer.position() / 4;
        byteVersion = null;
        instructions = new int[totalInstructions];
        for (int j = 0; j < totalInstructions; j++) {
            instructions[j] = codeBuffer.getInt(j * 4);
        }
    }

    public Aarch64CodeWriter(byte[] codeBuffer) {
        totalInstructions = codeBuffer.length / 4;
        instructions = null;
        byteVersion = codeBuffer;

    }

    public static void enableDebug() {
        debug = true;
    }

    public static void disableDebug() {
        debug = false;
    }

    private void log(String msg) {
        if (debug) {
            System.out.println(msg);
        }
    }

    public static String preAmble(String returnType, String listOfTypes, String listOfValues) {
        String val = new String(returnType + " (*pf)(");
        val += listOfTypes + ") = (" + returnType + "(*)(" + listOfTypes + "))(code);\n";
        val += "print_uart0(\"Changed!\");\n";
        val += "(*pf)(" + listOfValues + ");\n";
        return val;
    }

    public void createStaticCodeStubsFile(String functionPrototype, byte[] stubs, int entryPoint) {
        assert entryPoint > 0 : "Entry point cannot be negative : " + entryPoint
                + " (0x" + Integer.toHexString(entryPoint) + ")";
        assert entryPoint < (stubs.length + 1) * 4 : "Entry point must be within range of codeArray : " + entryPoint
                + " (0x" + Integer.toHexString(entryPoint) + ")";
        try {
            PrintWriter writer = new PrintWriter("codebuffer.c", "UTF-8");
            writer.println("unsigned char codeArray[" + (stubs.length + 1) * 4 + "] __attribute__((aligned(0x1000))) = {");
            for (int i = 0; i < stubs.length; i += 4) {
                writer.println("0x" + Integer.toHexString(stubs[i] & 0xFF) + ", " +
                               "0x" + Integer.toHexString(stubs[i + 1] & 0xFF) + ", " +
                               "0x" + Integer.toHexString(stubs[i + 2] & 0xFF) + ", " +
                               "0x" + Integer.toHexString(stubs[i + 3] & 0xFF) + ",");
            }
            // ret
            writer.println("0xd6, 0x5f, 0x03, 0xc0 };\n");
            writer.println("unsigned char *code = codeArray + " + entryPoint + ";");
            writer.println("void c_entry() {");
            writer.print(functionPrototype);
            writer.println("}");
            writer.close();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    public void createCodeFile() {
        createCodeFile(0);
    }

    public void createCodeFile(int numberOfArguments) {
        assert numberOfArguments >= 0;
        try {
            PrintWriter writer = new PrintWriter("codebuffer.c", "UTF-8");
            writer.println("unsigned char code[" + ((totalInstructions + 1) * 4) + "] __attribute__((aligned(0x1000))) = {");
            long xxx;
            for (int i = 0; i < totalInstructions; i++) {
                xxx = instructions[i];
                writer.println("0x" + Long.toHexString(xxx & 0xFF) + ", " +
                        "0x" + Long.toHexString(xxx >> 8 & 0xFF) + ", " +
                        "0x" + Long.toHexString(xxx >> 16 & 0xFF) + ", " +
                        "0x" + Long.toHexString(xxx >> 24 & 0xFF) + ",");
            }
            // ret
            writer.println("0xd6, 0x5f, 0x03, 0xc0 };\n");
            writer.println("void c_entry() {");
            String preAmble = preAmble("void", "", "");
            writer.print(preAmble);
            if (numberOfArguments != 0) {
                final int size = numberOfArguments * JVMS_SLOT_SIZE;
                writer.println("asm (\"add sp, sp, #" + size + "\");");
            }
            writer.println("}");
            writer.close();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }
}
