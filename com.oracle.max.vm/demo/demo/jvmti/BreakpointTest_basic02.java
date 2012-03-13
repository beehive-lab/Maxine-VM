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
package demo.jvmti;

/**
 * Program used to debug Maxine's breakpoint implementation. Basic test 02.
 * Usage:
 * <ol>
 * <li>Set a breakpoint at {@link #foo}.</li>
 * <li>Run the program, should hit breakpoint.</li>
 * <li>Now set a breakpoint at {@link #bar} and continue.
 * </ol>
 * N.B. In the above the breakpoint at {@code bar} set after {@code bar} is compiled,
 * so it is recompiled to instrument for the breakpoint.
 */


public class BreakpointTest_basic02 {
    public static void main(String[] args) {
        int arg = args.length == 0 ? 0 : Integer.parseInt(args[0]);
        // this gets bar compiled
        bar(arg);
        int r = foo(arg);
        System.out.printf("foo returned %d%n", r);
    }

    private static int foo(int arg) {
        return bar(arg);
    }

    private static int bar(int a) {
        System.out.printf("a=%d%n", a);
        return a + 1;
    }

}
