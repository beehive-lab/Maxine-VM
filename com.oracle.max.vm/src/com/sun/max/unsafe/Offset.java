/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.MaxineVM.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * Offsets from addresses or pointers. Unlike an 'Address', an 'Offset' can be negative. Both types have the identical
 * number of bits used for representation. However, 'Offset' uses twos complement, whereas 'Address' is simply unsigned.
 */
public class Offset extends Word {

    @HOSTED_ONLY
    public Offset(long value) {
        super(value);
    }

    @INLINE
    public static Offset zero() {
        return isHosted() ? ZERO : fromInt(0);
    }

    @INLINE
    public static Offset fromUnsignedInt(int value) {
        return Address.fromUnsignedInt(value).asOffset();
    }

    @INLINE
    public static Offset fromInt(int value) {
        if (isHosted()) {
            return fromLong(value);
        }
        if (Word.width() == 64) {
            long n = value;
            return UnsafeCast.asOffset(n);
        }
        return UnsafeCast.asOffset(value);
    }

    @INLINE
    public static Offset fromLong(long value) {
        if (isHosted()) {
            if (value == 0) {
                return ZERO;
            }
            if (value >= Cache.LOWEST_VALUE && value <= Cache.HIGHEST_VALUE) {
                return Cache.cache[(int) value - Cache.LOWEST_VALUE];
            }
            return new Offset(value);
        }
        if (Word.width() == 64) {
            return UnsafeCast.asOffset(value);
        }
        int n = (int) value;
        return UnsafeCast.asOffset(n);
    }

    @Override
    @HOSTED_ONLY
    public String toString() {
        return "&" + toHexString();
    }

    @INLINE
    public int toInt() {
        if (isHosted()) {
            return (int) value;
        }
        if (Word.width() == 64) {
            long n = UnsafeCast.asLong(this);
            return (int) n;
        }
        return UnsafeCast.asInt(this);
    }

    @INLINE
    public long toLong() {
        if (isHosted()) {
            return value;
        }
        if (Word.width() == 64) {
            return UnsafeCast.asLong(this);
        }
        return UnsafeCast.asInt(this);
    }

    @INLINE
    public int compareTo(Offset other) {
        if (greaterThan(other)) {
            return 1;
        }
        if (lessThan(other)) {
            return -1;
        }
        return 0;
    }

    @INLINE
    public boolean equals(int other) {
        if (isHosted()) {
            return toLong() == other;
        }
        return fromInt(other) == this;
    }

    @INLINE
    public boolean lessEqual(Offset other) {
        if (Word.width() == 64) {
            return toLong() <= other.toLong();
        }
        return toInt() <= other.toInt();
    }

    @INLINE
    public boolean lessEqual(int other) {
        return lessEqual(fromInt(other));
    }

    @INLINE
    public boolean lessThan(Offset other) {
        if (Word.width() == 64) {
            return toLong() < other.toLong();
        }
        return toInt() < other.toInt();
    }

    @INLINE
    public boolean lessThan(int other) {
        return lessThan(fromInt(other));
    }

    @INLINE
    public boolean greaterEqual(Offset other) {
        if (Word.width() == 64) {
            return toLong() >= other.toLong();
        }
        return toInt() >= other.toInt();
    }

    @INLINE
    public boolean greaterEqual(int other) {
        return greaterEqual(fromInt(other));
    }

    @INLINE
    public boolean greaterThan(Offset other) {
        if (Word.width() == 64) {
            return toLong() > other.toLong();
        }
        return toInt() > other.toInt();
    }

    @INLINE
    public boolean greaterThan(int other) {
        return greaterThan(fromInt(other));
    }

    @INLINE
    public Offset negate() {
        if (Word.width() == 64) {
            return fromLong(-toLong());
        }
        return fromInt(-toInt());
    }

    @INLINE
    public boolean isNegative() {
        if (Word.width() == 64) {
            return toLong() < 0L;
        }
        return toInt() < 0;
    }

    @INLINE
    public Offset plus(Offset addend) {
        if (Word.width() == 64) {
            return fromLong(toLong() + addend.toLong());
        }
        return fromInt(toInt() + addend.toInt());
    }

    @INLINE
    public Offset plus(Size addend) {
        return plus(addend.asOffset());
    }

    @INLINE
    public Offset plus(int addend) {
        return plus(fromInt(addend));
    }

    @INLINE
    public Offset plus(long addend) {
        return plus(fromLong(addend));
    }

    @INLINE
    public Offset minus(Offset subtrahend) {
        if (Word.width() == 64) {
            return fromLong(toLong() - subtrahend.toLong());
        }
        return fromInt(toInt() - subtrahend.toInt());
    }

    @INLINE
    public Offset minus(Size subtrahend) {
        return minus(subtrahend.asOffset());
    }

    @INLINE
    public Offset minus(int subtrahend) {
        return minus(fromInt(subtrahend));
    }

    @INLINE
    public Offset minus(long subtrahend) {
        return minus(fromLong(subtrahend));
    }

    @INLINE
    public Offset times(Offset factor) {
        if (Word.width() == 64) {
            return fromLong(toLong() * factor.toLong());
        }
        return fromInt(toInt() * factor.toInt());
    }

    @INLINE
    public Offset times(Address factor) {
        return times(factor.asOffset());
    }

    @INLINE
    public Offset times(int factor) {
        return times(fromInt(factor));
    }

    @INLINE
    public Offset dividedBy(Offset divisor) throws ArithmeticException {
        if (Word.width() == 64) {
            return fromLong(toLong() / divisor.toLong());
        }
        return fromInt(toInt() / divisor.toInt());
    }

    @INLINE
    public Offset dividedBy(int divisor) throws ArithmeticException {
        return dividedBy(fromInt(divisor));
    }

    @INLINE
    public Offset remainder(Offset divisor) throws ArithmeticException {
        if (Word.width() == 64) {
            return fromLong(toLong() % divisor.toLong());
        }
        return fromInt(toInt() % divisor.toInt());
    }

    @INLINE
    public int remainder(int divisor) throws ArithmeticException {
        return remainder(fromInt(divisor)).toInt();
    }

    @INLINE
    public boolean isRoundedBy(int numberOfBytes) {
        return remainder(numberOfBytes) == 0;
    }

    @INLINE
    public Offset roundedUpBy(int numberOfBytes) {
        if (isRoundedBy(numberOfBytes)) {
            return this;
        }
        return plus(numberOfBytes - remainder(numberOfBytes));
    }

    @INLINE
    public Offset and(Offset operand) {
        if (Word.width() == 64) {
            return fromLong(toLong() & operand.toLong());
        }
        return fromInt(toInt() & operand.toInt());
    }

    @INLINE
    public Offset and(int operand) {
        return and(fromInt(operand));
    }

    @INLINE
    public Offset and(long operand) {
        return and(fromLong(operand));
    }

    @INLINE
    public Offset or(Offset operand) {
        if (Word.width() == 64) {
            return fromLong(toLong() | operand.toLong());
        }
        return fromInt(toInt() | operand.toInt());
    }

    @INLINE
    public Offset or(int operand) {
        return or(fromInt(operand));
    }

    @INLINE
    public Offset or(long operand) {
        return or(fromLong(operand));
    }

    @INLINE
    public Offset not() {
        if (Word.width() == 64) {
            return fromLong(~toLong());
        }
        return fromInt(~toInt());
    }

    @INLINE
    public Offset roundedDownBy(int numberOfBytes) {
        return minus(remainder(numberOfBytes));
    }

    @INLINE
    public Offset aligned() {
        int n = Word.size();
        return plus(n - 1).and(Offset.fromInt(n - 1).not());
    }

    @INLINE
    public boolean isAligned() {
        int n = Word.size();
        return and(n - 1).equals(Offset.zero());
    }

    @HOSTED_ONLY
    public int numberOfEffectiveBits() {
        if (Word.width() == 64) {
            long n = toLong();
            if (n >= 0) {
                return 64 - Long.numberOfLeadingZeros(n);
            }
            return 65 - Long.numberOfLeadingZeros(~n);
        }
        int n = toInt();
        if (n >= 0) {
            return 32 - Integer.numberOfLeadingZeros(n);
        }
        return 33 - Integer.numberOfLeadingZeros(~n);
    }

    @HOSTED_ONLY
    public WordWidth effectiveWidth() {
        int bit = numberOfEffectiveBits();
        for (WordWidth width : WordWidth.values()) {
            if (bit < width.numberOfBits) {
                return width;
            }
        }
        throw ProgramError.unexpected();
    }

    @HOSTED_ONLY
    public static Offset ZERO = new Offset(0);

    @HOSTED_ONLY
    private static class Cache {
        private Cache() {
        }

        static int LOWEST_VALUE = -100;
        static int HIGHEST_VALUE = 1000;

        static Offset[] cache = new Offset[(HIGHEST_VALUE - LOWEST_VALUE) + 1];

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new Offset(i + LOWEST_VALUE);
            }
        }
    }
}

