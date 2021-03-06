/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @Harness: java
 * @Runs: 0=8; 1=10; 2=12; 3=8; 4=10; 6=14
 */
public class Phi02 {
    int f;

    Phi02(int f) {
        this.f = f;
    }

    public static int test(int arg) {
        return test2(new Phi02(arg), arg);
    }

    private static int test2(Phi02 p, int arg) {
        if (arg > 2) {
            inc(p, 1);
            arg += 1;
        } else {
            inc(p, 2);
            arg += 2;
            if (arg > 3) {
                inc(p, 1);
                arg += 1;
                if (arg > 4) {
                    inc(p, 1);
                    arg += 1;
                } else {
                    inc(p, 2);
                    arg += 2;
                }
            } else {
                inc(p, 2);
                arg += 2;
            }
        }
        return arg + p.f;
    }

    private static void inc(Phi02 p, int inc) {
        p.f += inc;
    }
}
