/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package demo;

/**
 * Program used to debug Maxine's breakpoint implementation for methods that may
 * have been inlined by the optimizing compiler at the time the breakpoint is set.
 * Usage:
 * <ol>
 * <li>Set a breakpoint at the call to {@link #spinUntilDone}.</li>
 * <li>Run the program, should hit breakpoint.</li>
 * <li>Set a breakpoint at {@link incTotal}, which should have been optimized.</li>
 * <li>Continue, should hit breakpoint at {@link incTotal}.</li>
 * <li>Change value of {@link #done} to true and continue; program should terminate.</li>
 * </ol>
 */
public class BreakpointTest_inline {

    public static void main(String[] args) {

        forceInline();
        spinUntilDone();

        System.out.println(total);
    }

    static boolean done;
    static int total;

    public static void spinUntilDone() {
        while (!done) {
            incTotal();
        }
    }

    private static void forceInline() {
        for (int i = 0; i < 10000; i++) {
            incTotal();
        }
    }

    public static void incTotal() {
        total++;
    }

}
