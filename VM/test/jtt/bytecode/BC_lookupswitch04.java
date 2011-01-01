/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @Runs: 0 = 42; 1 = 42;
 * @Runs: 66 = 42; 67 = 0; 68 = 42;
 * @Runs: 96 = 42; 97 = 1; 98 = 42;
 * @Runs: 106 = 42; 107 = 2; 108 = 42;
 * @Runs: 132 = 42; 133 = 3; 134 = 42;
 * @Runs: 211 = 42; 212 = 4; 213 = 42;
 * @Runs: -121 = 42; -122 = 5; -123 = 42
 */
public class BC_lookupswitch04 {
    public static int test(int a) {
        final int b = a + 8;
        final int c = b + 2;
        switch (c) {
            case 77:
                return 0;
            case 107:
                return 1;
            case 117:
                return 2;
            case 143:
                return 3;
            case 222:
                return 4;
            case -112:
                return 5;
        }
        return 42;
    }
}
