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
package jtt.except;

/*
 * @Harness: java
 * @Runs: 4 = -170; 5 = -150; 6 = -140; 7 = -240; 30 = -700
 */
public class Catch_Loop02 {
    public static int test(int arg) {
        int accum = 0;
        for (int i = 0; i < arg; i++) {
            try {
                accum += div(20, i);
            } catch (IllegalArgumentException e) {
                accum -= 100;
            }
        }
        return accum;
    }

    static int div(int a, int b) {
        if (b % 3 == 0) {
            throw new IllegalArgumentException();
        }
        return a / (b % 3);
    }
}
