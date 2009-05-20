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
package com.sun.max.lang;


/**
 * Additional methods that one might want in java.lang.Long.
 *
 * @author Bernd Mathiske
 */
public final class Longs {

    private Longs() {
    }

    public static final int SIZE = 8;
    public static final int WIDTH = 64;

    public static final long INT_MASK = 0xffffffffL;

    public static int compare(long greater, long lesser) {
        if (greater > lesser) {
            return 1;
        }
        if (greater == lesser) {
            return 0;
        }
        return -1;
    }

    public static int numberOfEffectiveSignedBits(long signed) {
        if (signed >= 0) {
            return 65 - Long.numberOfLeadingZeros(signed);
        }
        return 65 - Long.numberOfLeadingZeros(~signed);
    }

    public static int numberOfEffectiveUnsignedBits(long unsigned) {
        return 64 - Long.numberOfLeadingZeros(unsigned);
    }

    public static byte getByte(long value, int index) {
        return (byte) ((value >> (index * 8)) & 0xffL);
    }

    public static String toPaddedHexString(long n, char pad) {
        final String s = Long.toHexString(n);
        return Strings.times(pad, 16 - s.length()) + s;
    }

    /**
     * Determines if a given number is zero or a power of two.
     */
    public static boolean isPowerOfTwoOrZero(long n) {
        return Long.lowestOneBit(n) == n;
    }

    public static final long K = 1024;
    public static final long M = K * K;
    public static final long G = M * K;
    public static final long T = G * K;
    public static final long P = T * K;

    /**
     * Converts a positive number to a string using unit suffixes to reduce the
     * number of digits to three or less using base 2 for sizes.
     */
    public static String toUnitsString(long number) {
        if (number < 0) {
            throw new IllegalArgumentException();
        }
        if (number >= P) {
            return number / P + "P";
        }
        if (number >= T) {
            return number / T + "T";
        }
        if (number >= G) {
            return number / G + "G";
        }
        if (number >= M) {
            return number / M + "M";
        }
        if (number >= K) {
            return number / K + "K";
        }
        return Long.toString(number);
    }

    public static long[] insert(long[] array, int index, long element) {
        if (array == null) {
            final long[] result = new long[1];
            result[index] = element;
            return result;
        }
        final long[] result = new long[array.length + 1];
        if (index > 0) {
            System.arraycopy(array, 0, result, 0, index);
        }
        result[index] = element;
        if (index < array.length) {
            System.arraycopy(array, index, result, index + 1, array.length - index);
        }
        return result;
    }

    public static long[] remove(long[] array, int index) {
        final int newLength = array.length - 1;
        final long[] result = new long[newLength];
        System.arraycopy(array, 0, result, 0, index);
        if (index < newLength) {
            System.arraycopy(array, index + 1, result, index, newLength - index);
        }
        return result;
    }
}
