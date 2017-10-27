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

import com.oracle.max.asm.*;

public class ARMCodeWriter {

    public static boolean debug = false;
    private int[] instructions;
    private byte[] byteVersion;
    private int totalInstructions;

    public ARMCodeWriter(Buffer codeBuffer) {
        totalInstructions = codeBuffer.position() / 4;
        byteVersion = null;
        instructions = new int[totalInstructions];
        for (int j = 0; j < totalInstructions; j++) {
            instructions[j] = codeBuffer.getInt(j * 4);
        }
    }

    public ARMCodeWriter(byte[] codeBuffer) {
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
        val += listOfTypes + ") = (" + returnType + "(*))(code);\n";
        val += "print_uart0(\"Changed!\");\n";
        val += "(*pf)(" + listOfValues + ");\n";
        val += "asm volatile(\"forever: b forever\");\n";
        return val;
    }

    public void createStaticCodeStubsFile(String functionPrototype, byte[] stubs, int entryPoint) {
        try {
            PrintWriter writer = new PrintWriter("codebuffer.c", "UTF-8");
            writer.println("unsigned char codeArray[" + ((totalInstructions + 1) * 4 + stubs.length) + "] __attribute__((aligned(0x1000))) = { \n");
            log("unsigned char code[" + ((totalInstructions + 1) * 4 + stubs.length) + "] __attribute__((aligned(0x1000))) ;\n");
            log("void c_entry() {");
            for (int i = 0; i < stubs.length; i += 4) {
                writer.println("0x" + Integer.toHexString(stubs[i]) + ", " + "0x" + Integer.toHexString(stubs[i + 1]) + ", " + "0x" + Integer.toHexString(stubs[i + 2]) + ", " + "0x" +
                                Integer.toHexString(stubs[i + 3]) + ",\n");
            }
            // ret
            writer.println("0xd6, 0x5f, 0x03, 0xc0 };\n");
            writer.println("unsigned char *code = codeArray + " + entryPoint + ";");
            writer.println("void c_entry() {");
            writer.print(functionPrototype);
            writer.close();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    public void createCodeStubsFile(byte[] stubs, int entryPoint) {
        try {
            PrintWriter writer = new PrintWriter("codebuffer.c", "UTF-8");
            writer.println("unsigned char codeArray[" + ((totalInstructions + 1) * 4 + stubs.length) + "] __attribute__((aligned(0x1000))) ;\n");
            writer.println("void c_entry() {");
            log("unsigned char code[" + ((totalInstructions + 1) * 4 + stubs.length) + "] __attribute__((aligned(0x1000))) ;\n");
            log("void c_entry() {");
            for (int i = 0; i < stubs.length; i += 4) {
                writer.println("codeArray[" + i + " ] = " + stubs[i] + ";");
                writer.println("codeArray[" + i + " + 1] = " + stubs[i + 1] + ";");
                writer.println("codeArray[" + i + " + 2] = " + stubs[i + 2] + ";");
                writer.println("codeArray[" + i + " + 3] = " + stubs[i + 3] + ";");
            }
            // ret
            writer.println("codeArray[" + (stubs.length + 3) + "] = " + 0xc0 + ";");
            writer.println("codeArray[" + (stubs.length + 2) + "] = " + 0x03 + ";");
            writer.println("codeArray[" + (stubs.length + 1) + "] = " + 0x5f + ";");
            writer.println("codeArray[" + (stubs.length) + "] = " + 0xd6 + ";");
            writer.println("unsigned char *code = codeArray + " + entryPoint + ";");
            writer.close();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    public void createCodeFile() {
        try {
            PrintWriter writer = new PrintWriter("codebuffer.c", "UTF-8");
            writer.println("unsigned char code[" + ((totalInstructions + 1) * 4) + "] __attribute__((aligned(0x1000))) ;\n");
            writer.println("void c_entry() {");
            log("unsigned char code[" + ((totalInstructions + 1) * 4) + "] __attribute__((aligned(0x1000))) ;\n");
            log("void c_entry() {");
            long xxx = 0xe30090f0; // r9 240
            int val;
            for (int i = 0; i < totalInstructions; i++) {
                xxx = instructions[i];
                val = i * 4;
                writer.println("code[" + val + "] = " + (xxx & 0xff) + ";");
                log("code[" + val + "] = 0x" + Long.toString(xxx & 0xff, 16) + ";");
                val = val + 1;
                writer.println("code[" + val + "] = " + ((xxx >> 8) & 0xff) + ";");
                log("code[" + val + "] = 0x" + Long.toString((xxx >> 8) & 0xff, 16) + ";");
                val = val + 1;
                writer.println("code[" + val + "] = " + ((xxx >> 16) & 0xff) + ";");
                log("code[" + val + "] = 0x" + Long.toString((xxx >> 16) & 0xff, 16) + ";");
                val = val + 1;
                writer.println("code[" + val + "] = " + (xxx >> 24 & 0xff) + ";");
                log("code[" + val + "] = 0x" + Long.toString(xxx >> 24 & 0xff, 16) + ";");
            }
            // ret
            writer.println("code[" + totalInstructions * 4 + "] = " + 0xc0 + ";");
            log("code[" + totalInstructions * 4 + "] = " + 0xc0 + ";");
            writer.println("code[" + totalInstructions * 4 + "+1] = " + 0x03 + ";");
            log("code[" + totalInstructions * 4 + "+1] = " + 0x03 + ";");
            writer.println("code[" + totalInstructions * 4 + "+2] = " + 0x5f + ";");
            log("code[" + totalInstructions * 4 + "+2] = " + 0x5f + ";");
            writer.println("code[" + totalInstructions * 4 + "+3] = " + 0xd6 + ";");
            log("code[" + totalInstructions * 4 + "+3] = " + 0xd6 + ";");
            writer.close();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }
}
