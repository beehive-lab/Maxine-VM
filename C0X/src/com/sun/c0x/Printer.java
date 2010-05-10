/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.c0x;

import java.io.PrintStream;

import com.sun.cri.bytecode.BytecodeSwitch;
import com.sun.cri.bytecode.Bytecodes;
import com.sun.cri.ri.RiMethod;

/**
 * The {@code Printer} class definition.
 *
 * @author Ben L. Titzer
 */
public class Printer {
    static final PrintStream out = System.out;
    static final String CTRL_RED = "\u001b[0;31m";
    static final String CTRL_GREEN = "\u001b[0;32m";
    static final String CTRL_DEFAULT = "\u001b[1;00m";
    static final String CTRL_YELLOW = "\u001b[1;33m";
    static final String CTRL_LIGHTGRAY = "\u001b[0;37m";

    static void printString(String str) {
        out.print(CTRL_GREEN);
        out.print("    " + str);
        out.print(CTRL_DEFAULT);
        out.println("");
    }

    static void printOp(C0XCompilation.Location r, String op, C0XCompilation.Location... locs) {
        StringBuilder b = new StringBuilder();
        if (r != null) {
            b.append(r);
            while (b.length() < 10) {
                b.append(" ");
            }
            b.append(" = ");
        }
        b.append(op).append("(");
        for (int i = 0; i < locs.length; i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(locs[i]);
        }
        b.append(")");
        // printString(b.toString());
    }

    static void printSwitch(String op, C0XCompilation.Location key, BytecodeSwitch sw) {
        StringBuilder b = new StringBuilder(op);
        b.append(" ").append(key).append(" ");
        int max = sw.numberOfCases();
        for (int i = 0; i < max; i++) {
            b.append(sw.keyAt(i)).append(" -> ").append(sw.targetAt(i));
        }
        b.append("default -> ").append(sw.defaultTarget());
        // printString(b.toString());
    }

    static void printBytecodeStart(int bci, int opcode, int depth) {
        out.print(CTRL_LIGHTGRAY);
        out.print("    " + Bytecodes.nameOf(opcode) + " @ " + bci + " depth = " + depth);
        out.println("");
        out.print(CTRL_DEFAULT);
    }

    static void printPrologue(RiMethod method) {
        out.print(CTRL_RED);
        String s = "====== " + method + " ";
        out.print(s);
        out.print(" locals: " + method.maxLocals() + " stack: " + method.maxStackSize() + " ");
        for (int i = s.length(); i < 100; i++) {
            out.print('=');
        }
        out.print(CTRL_DEFAULT);
        out.println("");
    }

    static void printBlockPrologue(C0XCompilation compilation, int bci) {
        out.print(CTRL_YELLOW);
        String s = "  === " + bci + " ";
        out.print(s);
        if (BlockMarker.isBackwardBranchTarget(bci, compilation.blockMap)) {
            out.print("[bw] ");
        }
        if (BlockMarker.isExceptionEntry(bci, compilation.blockMap)) {
            out.print("[ex] ");
        }
        for (int i = s.length(); i < 80; i++) {
            out.print('=');
        }
        out.print(CTRL_DEFAULT);
        out.println("");
    }
}
