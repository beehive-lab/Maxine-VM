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
/*VCSID=8ef1462b-2a0e-4a7e-a9bc-c42be24dc3be*/
package com.sun.max.lang;

import com.sun.max.util.*;

/**
 * Additional methods that one might want in java.lang.Integer
 * and int array stuff.
 *
 * @author Bernd Mathiske
 */
public final class Ints {

    // Utility classes should not be instantiated.
    private Ints() {
    }

    public static final int SIZE = 4;
    public static final int WIDTH = 32;

    public static final Range VALUE_RANGE = new Range(Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final int K = 1024;
    public static final int M = K * K;

    public static int compare(int greater, int lesser) {
        if (greater > lesser) {
            return 1;
        }
        if (greater == lesser) {
            return 0;
        }
        return -1;
    }

    public static int numberOfEffectiveSignedBits(int signed) {
        if (signed >= 0) {
            return 33 - Integer.numberOfLeadingZeros(signed);
        }
        return 33 - Integer.numberOfLeadingZeros(~signed);
    }

    public static int numberOfEffectiveUnsignedBits(int unsigned) {
        return 32 - Integer.numberOfLeadingZeros(unsigned);
    }

    /**
     * Determines if a given number is zero or a power of two.
     */
    public static boolean isPowerOfTwo(int n) {
        return Integer.lowestOneBit(n) == n;
    }

    public static int log2(int n) {
        if (n <= 0) {
            throw new ArithmeticException();
        }
        return 31 - Integer.numberOfLeadingZeros(n);
    }

    public static int roundUp(int value, int by) {
        final int rest = value % by;
        if (rest == 0) {
            return value;
        }
        if (value < 0) {
            return value - rest;
        }
        return value + (by - rest);
    }

    /**
     * Returns the hexadecimal string representation of the given value with at least 8 digits, e.g. 0x0000CAFE.
     */
    public static String toHexLiteral(int value) {
        return "0x" + toPaddedHexString(value, '0');
    }

    /**
     * Returns the given value as a hexadecimal number with at least 8 digits, e.g. 0000CAFE.
     */
    public static String toPaddedHexString(int n, char pad) {
        final String s = Integer.toHexString(n).toUpperCase();
        return Strings.times(pad, 8 - s.length()) + s;
    }

    public static boolean contains(int[] array, int value) {
        for (int element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }

    public static int[] append(int[] array, int element) {
        final int resultLength = array.length + 1;
        final int[] result = new int[resultLength];
        System.arraycopy(array, 0, result, 0, array.length);
        result[array.length] = element;
        return result;
    }

    public static int[] append(int[] head, int[] tail) {
        final int[] result = new int[head.length + tail.length];
        System.arraycopy(head, 0, result, 0, head.length);
        System.arraycopy(tail, 0, result, head.length, tail.length);
        return result;
    }

    public static int[] createRange(int first, int last) {
        if (first > last) {
            throw new IllegalArgumentException();
        }
        final int n = last + 1 - first;
        final int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            result[i] = first + i;
        }
        return result;
    }

    public static void copyAll(int[] fromArray, int[] toArray) {
        for (int i = 0; i < fromArray.length; i++) {
            toArray[i] = fromArray[i];
        }
    }

    /**
     * Returns a string representation of the contents of the specified array.
     * Adjacent elements are separated by the specified separator. Elements are
     * converted to strings as by <tt>String.valueOf(int)</tt>.
     *
     * @param array     the array whose string representation to return
     * @param separator the separator to use
     * @return a string representation of <tt>array</tt>
     * @throws NullPointerException if {@code array} or {@code separator} is null
     */
    public static String toString(int[] array, String separator) {
        if (array == null || separator == null) {
            throw new NullPointerException();
        }
        if (array.length == 0) {
            return "";
        }

        final StringBuilder buf = new StringBuilder();
        buf.append(array[0]);

        for (int i = 1; i < array.length; i++) {
            buf.append(separator);
            buf.append(array[i]);
        }

        return buf.toString();
    }

    /**
     * @see Longs#toUnitsString(long)
     */
    public static String toUnitsString(long number) {
        return Longs.toUnitsString(number);
    }
}
