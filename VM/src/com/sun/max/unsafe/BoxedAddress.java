/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
