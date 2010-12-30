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
public class Fold_Int01 {
    public static int test(int arg) {
        if (arg == 0) {
            return add();
        }
        if (arg == 1) {
            return sub();
        }
        if (arg == 2) {
            return mul();
        }
        if (arg == 3) {
            return div();
        }
        if (arg == 4) {
            return mod();
        }
        if (arg == 5) {
            return and();
        }
        if (arg == 6) {
            return or();
        }
        if (arg == 7) {
            return xor();
        }
        return 0;
    }
    public static int add() {
        int x = 3;
        return x + 7;
    }
    public static int sub() {
        int x = 15;
        return x - 4;
    }
    public static int mul() {
        int x = 6;
        return x * 2;
    }
    public static int div() {
        int x = 26;
        return x / 2;
    }
    public static int mod() {
        int x = 29;
        return x % 15;
    }
    public static int and() {
        int x = 31;
        return x & 15;
    }
    public static int or() {
        int x = 16;
        return x | 16;
    }
    public static int xor() {
        int x = 0;
        return x ^ 17;
    }

}
