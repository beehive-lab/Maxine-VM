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
package test.vm.output;

import static com.sun.max.vm.intrinsics.Infopoints.*;

import com.sun.max.annotate.*;

public class UncommonTrap_01 {

    private static boolean Z = true;
    private static byte B = 3;
    private static char C = '5';
    private static short S = 7;
    private static int I = 9;
    private static float F = 500f;
    private static long L = 13L;
    private static double D = Double.MIN_NORMAL;

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 10000000; i++) {
            me(F, D, L, Z, B, C, S, I, true);
        }
        // By now 'me()' should have been recompiled.
        me(F, D, L, Z, B, C, S, I, false);
    }

    @NEVER_INLINE
    private static void me(float f, double d, long l, boolean z, byte b, char c, short s, int i, boolean warmup) {
        if (warmup) {
            return;
        }

        printParams(l, d, z, b, c, s, i, f);

        // An optimizing compiler should allocate the variables used in the
        // following loop in registers. The uncommon trap is therefore an
        // attempt to test correct deoptimization of register located values.
        for (int x = 0; x < 100001; x++) {
            d = -d;
            f = -f;
            i = -i;
            s = (short) -s;
            if (x > 50000) {
                uncommonTrap();
            }
        }

        printParams(l, d, z, b, c, s, i, f);
    }

    private static void printParams(long l, double d, boolean z, byte b, char c, short s, int i, float f) {
        System.out.println("z=" + z + ", b=" + b + ",c=" + c + ", s=" + s + ", i=" + i + ", f=" + f + ", l=" + l + ", d=" + d);
    }
}
