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
package jtt.optimize;

/*
 * Tests constant folding of integer comparisons.
 * @Harness: java
 * @Runs: 0=true; 1=true; 2=true; 3=false; 4=false; 5=false
 */
public class Fold_Int02 {
    public static boolean test(int arg) {
        if (arg == 0) {
            return equ();
        }
        if (arg == 1) {
            return neq();
        }
        if (arg == 2) {
            return geq();
        }
        if (arg == 3) {
            return ge();
        }
        if (arg == 4) {
            return ltq();
        }
        if (arg == 5) {
            return lt();
        }
        return false;
    }
    static boolean equ() {
        int x = 34;
        return x == 34;
    }
    static boolean neq() {
        int x = 34;
        return x != 33;
    }
    static boolean geq() {
        int x = 34;
        return x >= 33;
    }
    static boolean ge() {
        int x = 34;
        return x > 35;
    }
    static boolean ltq() {
        int x = 34;
        return x <= 32;
    }
    static boolean lt() {
        int x = 34;
        return x < 31;
    }
}
