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

import com.sun.max.annotate.NEVER_INLINE;

/*
 * @Harness: java
 * @Runs: 0=4; 1=5; 2=6; 3=4; 4=5; 6=7
 */
public class Phi03 {
    int f;

    Phi03(int f) {
        this.f = f;
    }

    public static int test(int arg) {
        return test2(new Phi03(arg), arg);
    }

    @NEVER_INLINE
    private static int test2(Phi03 p, int arg) {
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
        return p.f;
    }

    @NEVER_INLINE
    private static void inc(Phi03 p, int inc) {
        p.f += inc;
    }
}
