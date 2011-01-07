/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
