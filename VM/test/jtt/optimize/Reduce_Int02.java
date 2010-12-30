/*
 * Copyright (c) 2009, 2009, Oracle and/or its affiliates. All rights reserved.
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
package jtt.optimize;

/*
 * Tests constant folding of integer operations.
 * @Harness: java
 * @Runs: 0=10; 1=11; 2=12; 3=13; 4=14; 5=15; 6=16; 7=17
 */
public class Reduce_Int02 {
    public static int test(int arg) {
        if (arg == 0) {
            return add(10);
        }
        if (arg == 1) {
            return sub();
        }
        if (arg == 2) {
            return mul(12);
        }
        if (arg == 3) {
            return div();
        }
        if (arg == 4) {
            return mod();
        }
        if (arg == 5) {
            return and(15);
        }
        if (arg == 6) {
            return or(16);
        }
        if (arg == 7) {
            return xor(17);
        }
        return 0;
    }
    public static int add(int x) {
        return 0 + x;
    }
    public static int sub() {
        return 11;
    }
    public static int mul(int x) {
        return 1 * x;
    }
    public static int div() {
        return 13;
    }
    public static int mod() {
        return 14;
    }
    public static int and(int x) {
        return -1 & x;
    }
    public static int or(int x) {
        return 0 | x;
    }
    public static int xor(int x) {
        return 0 ^ x;
    }
}
