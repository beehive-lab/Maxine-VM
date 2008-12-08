/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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
