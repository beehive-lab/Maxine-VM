/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.vm.compiler.builtin.*;

/**
 * @author Aziz Ghuloum
 */
public class FloatNanTest {

    public static int run13() {
        final int j = 0x7fffffff;
        final float jf = j;
        if ((int) jf != j) {
            System.out.println(j);
            System.out.println((float) j);
            System.out.println((int) jf);
            return 2; // STATUS_FAILED
        }
        final int i = 0x80000000;
        final int k = -33;
        final int l = 777;
        final int j1 = 0x1000000;
        final float fi = i;
        if ((int) fi != i) {
            return 1; // STATUS_FAILED
        }
        final float fk = k;
        if ((int) fk != k) {
            return 3; // STATUS_FAILED
        }
        final float fl = l;
        if ((int) fl != l) {
            return 4; // STATUS_FAILED
        }
        final float fi1 = fi;
        if ((int) fi1 != 0) {
            return 5; // STATUS_FAILED
        }
        final float fj1 = j1;
        return (int) fj1 == 0x1000000
               ? 0  // STATUS_PASSED
               : 6; // STATUS_FAILED
    }

    static long cvtdouble2long(double x) {
        final double maxdouble = ((long) -1) >>> 1;
        if (x < maxdouble) {
            return (long) x;
        }
        return ((long) -1) >>> 1;
    }

    public static int run14() {
        final long j = ((long) -1) >>> 1;
        final double dj = j;
        if ((long) dj != j) {
            System.out.println(j);
            System.out.println(dj);
            System.out.println(cvtdouble2long(dj));
            System.out.println((long) dj);
            return 2; // STATUS_FAILED
        }
        return 0;
    }

    private static final double MAX_DOUBLE_VALUE = -1 >>> 1;

    public static int convertDoubleToInt(double value) {
        if (MAX_DOUBLE_VALUE >= value) {
            return IEEE754Builtin.ConvertDoubleToInt.convertDoubleToInt(value);
        }
        if (value >= MAX_DOUBLE_VALUE) {
            return -1 >>> 1;
        }
        return 0;
    }

    public static boolean ournan1(float x) {
        return x != x;
    }

    public static boolean ournan2(float x) {
        return !(x == x);
    }

    public static void isnantest(float x) {
        System.out.println("n=" + x + ", nan=" + (Float.isNaN(x) ? 1 : 0) + ", ournan1=" + (ournan1(x) ? 1 : 0) + " ournan2=" + (ournan2(x) ? 1 : 0));
    }

    public static void foo(double x) {
        System.out.println("x = " + x);
        System.out.println("(x / x) <= 0.0 ? " + ((x / x) <= 0.0));
        System.out.println("(x / x) >= 0.0 ? = " + ((x / x) >= 0.0));
        System.out.println("(x / x) == 0.0 ? = " + ((x / x) == 0.0));
        System.out.println("(x / x) < 0.0 ? = " + ((x / x) < 0.0));
        System.out.println("(x / x) > 0.0) ? = " + ((x / x) > 0.0));
        System.out.println("(x / x) != 0.0 ? = " + ((x / x) != 0.0));
        System.out.println("(x / x) == Double.NaN ? = " + ((x / x) == Double.NaN));
        System.out.println("x / x = " + x / x);
        System.out.println(convertDoubleToInt(x / x));
    }

    public static int run(double x) {
        return (int) (x / x);
    }
    public static int run1(double x) {
        return (int) x;
    }

    public static void main(String[] args) {
        System.out.println(run13());
        System.out.println(run14());
        isnantest(Float.NaN);
        isnantest(0.0f);
        isnantest(0.75f);
        foo(0.0);
        System.out.println("Double.NaN > 0 ? = " + (Double.NaN > 0));
        System.out.println("Double.NaN < 0 ? = " + (Double.NaN < 0));
        System.out.println("Double.NaN >= 0 ? = " + (Double.NaN >= 0));
        System.out.println("Double.NaN <= 0 ? = " + (Double.NaN <= 0));
        System.out.println(run(0.0));
        System.out.println(run(1.0));
        System.out.println(run1(0.0));
        System.out.println(run1(1.0));
    }
}
