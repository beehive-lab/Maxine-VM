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
package com.sun.max.unsafe;

import java.math.*;

import com.sun.max.annotate.*;

/**
 * Boxed version of Address.
 *
 * @see Address
 *
 * @author Bernd Mathiske
 */
@HOSTED_ONLY
public final class BoxedAddress extends Address implements Boxed {

    private long nativeWord;

    public static final BoxedAddress ZERO = new BoxedAddress(0);
    public static final BoxedAddress MAX = new BoxedAddress(-1L);

    private static final class Cache {
        private Cache() {
        }

        static final int HIGHEST_VALUE = 1000;

        static final BoxedAddress[] cache = new BoxedAddress[HIGHEST_VALUE + 1];

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new BoxedAddress(i);
            }
        }
    }

    public static BoxedAddress from(long value) {
        if (value == 0) {
            return ZERO;
        }
        if (value >= 0 && value <= Cache.HIGHEST_VALUE) {
            return Cache.cache[(int) value];
        }
        if (value == -1L) {
            return MAX;
        }
        return new BoxedAddress(value);
    }

    public static BoxedAddress from(int value) {
        return from(value & BoxedWord.INT_MASK);
    }

    private BoxedAddress(long value) {
        if (Word.width() == 64) {
            nativeWord = value;
        } else {
            nativeWord = value & BoxedWord.INT_MASK;
        }
    }

    public long value() {
        return nativeWord;
    }

    private static BigInteger bi(long unsigned) {
        if (unsigned < 0) {
            long signBit = 0x8000000000000000L;
            long low63Bits = unsigned & ~signBit;
            return BigInteger.valueOf(low63Bits).setBit(63);
        }
        return BigInteger.valueOf(unsigned);
    }


    private static long unsignedDivide(long dividend, long divisor) {
        if (dividend >= 0 && divisor >= 0) {
            return dividend / divisor;
        }
        return bi(dividend).divide(bi(divisor)).longValue();
    }

    @Override
    public Address dividedByAddress(Address divisor) {
        final BoxedAddress box = (BoxedAddress) divisor.asAddress();
        if (box.nativeWord == 0L) {
            throw new ArithmeticException();
        }
        return new BoxedAddress(unsignedDivide(nativeWord, box.nativeWord));
    }

    @Override
    public Address dividedByInt(int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException();
        }
        return new BoxedAddress(unsignedDivide(nativeWord, divisor & BoxedWord.INT_MASK));
    }

    private static long unsignedRemainder(long dividend, long divisor) {
        if (dividend >= 0 && divisor >= 0) {
            return dividend % divisor;
        }
        return bi(dividend).remainder(bi(divisor)).longValue();
    }

    @Override
    public Address remainderByAddress(Address divisor) {
        final BoxedAddress box = (BoxedAddress) divisor.asAddress();
        if (box.nativeWord == 0L) {
            throw new ArithmeticException();
        }
        return new BoxedAddress(unsignedRemainder(nativeWord, box.nativeWord));
    }

    @Override
    public int remainderByInt(int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException();
        }
        return (int) unsignedRemainder(nativeWord, divisor & BoxedWord.INT_MASK);
    }
}
