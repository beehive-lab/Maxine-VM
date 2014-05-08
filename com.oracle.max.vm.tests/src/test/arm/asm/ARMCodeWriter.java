package test.arm.asm;

import java.io.*;

import com.oracle.max.asm.*;

public class ARMCodeWriter {

    public static boolean debug = false;
    private int[] instructions;
    private int totalInstructions;

    public ARMCodeWriter(int assemblerStatements, Buffer codeBuffer) {
        totalInstructions = assemblerStatements;
        instructions = new int[assemblerStatements];
        for (int j = 0; j < assemblerStatements; j++) {
            instructions[j] = codeBuffer.getInt(j * 4);
        }
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
                writer.println("code[" + val + "] = " + ((xxx >> 24) & 0xff) + ";");
                log("code[" + val + "] = 0x" + Long.toString((xxx >> 24) & 0xff, 16) + ";");
            }
            writer.println("code[" + totalInstructions * 4 + "] = " + 0xfe + ";");
            log("code[" + totalInstructions * 4 + "] = " + 0xfe + ";");
            writer.println("code[" + totalInstructions * 4 + "+1] = " + 0xff + ";");
            log("code[" + totalInstructions * 4 + "+1] = " + 0xff + ";");
            writer.println("code[" + totalInstructions * 4 + "+2] = " + 0xff + ";");
            log("code[" + totalInstructions * 4 + "+2] = " + 0xff + ";");
            writer.println("code[" + totalInstructions * 4 + "+3] = " + 0xea + ";");
            log("code[" + totalInstructions * 4 + "+3] = " + 0xea + ";");
            writer.close();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }
}
/*
 * unsigned char code[12] __attribute__((aligned(0x1000))) ; void c_entry() { code[0] = 0xff; code[1] = 0x90; code[2] =
 * 0xa0; code[3] = 0xe3; code[4] = 0xff; code[5] = 0xaf;// r10? code[6] =0x4f; code[7] = 0xe3; // do load or r9 twice
 * code[8] = 0xfe; code[9] = 0xff; code[10] = 0xff; code[11] = 0xea;
 */
