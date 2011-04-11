/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.output;


/**
 * GC Test for large number of object parameters.
 *
 * @author Michael Bebenita
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public class GCTest7 {
    public static void main(String[] args) {
        String a0 = "a" + 0;
        String a1 = "a" + 1;
        String a2 = "a" + 2;
        String a3 = "a" + 3;
        String a4 = "a" + 4;
        String a5 = "a" + 5;
        String a6 = "a" + 6;
        String a7 = "a" + 7;
        String a8 = "a" + 8;
        String a9 = "a" + 9;
        String b0 = "b" + 0;
        String b1 = "b" + 1;
        String b2 = "b" + 2;
        String b3 = "b" + 3;
        String b4 = "b" + 4;
        String b5 = "b" + 5;
        String b6 = "b" + 6;
        String b7 = "b" + 7;
        String b8 = "b" + 8;
        String b9 = "b" + 9;

        Object result = objStackParams(16, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9,
                                           b0, b1, b2, b3, b4, b5, b6, b7, b8, b9);
        if (result == b9) {
            System.out.println("OK!");
        }
    }

    private static Object objStackParams(int depth,
                                         String a0, String a1, String a2, String a3, String a4,
                                         String a5, String a6, String a7, String a8, String a9,
                                         String b0, String b1, String b2, String b3, String b4,
                                         String b5, String b6, String b7, String b8, String b9) {
        if (depth > 0) {
            System.gc();
            System.out.println(a0 + a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8 + a9);
            System.out.println(b0 + b1 + b2 + b3 + b4 + b5 + b6 + b7 + b8 + b9);
            return objStackParams(depth - 1, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9,
                                           b0, b1, b2, b3, b4, b5, b6, b7, b8, b9);
        }
        return b9;
    }
}
