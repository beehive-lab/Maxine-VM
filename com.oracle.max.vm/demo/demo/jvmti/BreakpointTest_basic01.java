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
 * Program used to debug Maxine's breakpoint implementation. Basic test 01.
 * Usage:
 * <ol>
 * <li>Set a breakpoint at {@link #foo}.</li>
 * <li>Run the program, should hit breakpoint.</li>
 * <li>Then either exit the test by continuing or step, which should step over the print.
 *     A further step should step into main, which tests that main is instrumented for
 *     single step.
 * </ol>
 * N.B. In the above the breakpoint is set before {@code foo} is compiled,
 * so it is instrumented for the breakpoint on the first compilation.
 */

public class BreakpointTest_basic01 {
    public static void main(String[] args) {
        int r = foo(args.length == 0 ? 0 : Integer.parseInt(args[0]));
        System.out.printf("foo returned %d%n", r);
    }

    private static int foo(int a) {
        System.out.printf("a=%d%n", a);
        return a + 1;
    }
}
