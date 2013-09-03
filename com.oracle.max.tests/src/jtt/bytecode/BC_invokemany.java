/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package jtt.bytecode;

/*
 * @Harness: java
 * @Runs: 0 = 49; 1 = 57; 2 = 57
 */
public class BC_invokemany {
    public static long test(int x) {
        BC_invokemany obj = new BC_invokemany();
        String r;
        if (x == 0) {
            r = sevend(x, x + 1, "5", "6", "7", "8", "9", 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0,
                        14.0, 15.0, 16.0, 17.0, "18");
        } else if (x == 1) {
            r = ssix(x, x + 1, "5", "6", "7", "8", "9", 2, "10", 'A', (byte) -128, "11");
        } else if (x == 2) {
            r = obj.six(x, "5", "6", "7", "8", "9", 2, "10", 'A', (byte) -128, "11");
        } else {
            return -1;
        }
        return r.charAt(0);
    }

    String six(long a, Object b, Object c, Object d, Object e, String f, long g, String h, char i, byte j, String k) {
        return use6(f);
    }

    static String ssix(long a, long y, Object b, Object c, Object d, Object e, String f, long g, String h, char i, byte j, String k) {
        return f;
    }

    String use6(String obj) {
        return obj.substring(0);
    }

    static String sevend(long a, long y, Object b, Object c, Object d, Object e, String f,
                    double a1, double a2, double a3, double a4,
                    double a5, double a6, double a7, double a8,
                    double a9, double a10, double a11, double a12,
                    double a13, double a14, double a15, double a16,
                    double a17, String s) {
        return s;
    }

}
