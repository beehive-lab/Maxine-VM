/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
public abstract class Offset extends Word {

    @HOSTED_ONLY
    protected Offset() {
    }

    @INLINE
    public static Offset zero() {
        return isHosted() ? BoxedOffset.ZERO : fromInt(0);
    }

    @INLINE
    public static Offset fromUnsignedInt(int value) {
        return Address.fromUnsignedInt(value).asOffset();
    }

    @INLINE
    public static Offset fromInt(int value) {
        if (isHosted()) {
            return BoxedOffset.from(value);
        }
        if (Word.width() == 64) {
            final long n = value;
            return UnsafeCast.asOffset(n);
        }
        return UnsafeCast.asOffset(value);
    }

    @INLINE
    public static Offset fromLong(long value) {
        if (isHosted()) {
            return BoxedOffset.from(value);
        }
        if (Word.width() == 64) {
            return UnsafeCast.asOffset(value);
        }
        final int n = (int) value;
        return UnsafeCast.asOffset(n);
    }

    @Override
    @HOSTED_ONLY
    public String toString() {
        return "&" + toHexString();
    }

    @INLINE
    public final int toInt() {
        if (isHosted()) {
            final BoxedOffset box = (BoxedOffset) this;
            return (int) box.value();
        }
        if (Word.width() == 64) {
            final long n = UnsafeCast.asLong(this);
            return (int) n;
        }
        return UnsafeCast.asInt(this);
    }

    @INLINE
    public final long toLong() {
        if (isHosted()) {
            final BoxedOffset box = (BoxedOffset) this;
            return box.value();
        }
        if (Word.width() == 64) {
            return UnsafeCast.asLong(this);
        }
        return UnsafeCast.asInt(this);
    }

    @INLINE
    public final int compareTo(Offset other) {
        if (greaterThan(other)) {
            return 1;
        }
        if (lessThan(other)) {
            return -1;
        }
        return 0;
    }

    @INLINE
    public final boolean equals(int other) {
        if (isHosted()) {
            return toLong() == other;
        }
        return fromInt(other) == this;
    }

    @INLINE
    public final boolean lessEqual(Offset other) {
        if (Word.width() == 64) {
            return toLong() <= other.toLong();
        }
        return toInt() <= other.toInt();
    }

    @INLINE
    public final boolean lessEqual(int other) {
        return lessEqual(fromInt(other));
    }

    @INLINE
    public final boolean lessThan(Offset other) {
        if (Word.width() == 64) {
            return toLong() < other.toLong();
        }
        return toInt() < other.toInt();
    }

    @INLINE
    public final boolean lessThan(int other) {
        return lessThan(fromInt(other));
    }

    @INLINE
    public final boolean greaterEqual(Offset other) {
        if (Word.width() == 64) {
            return toLong() >= other.toLong();
        }
        return toInt() >= other.toInt();
    }

    @INLINE
    public final boolean greaterEqual(int other) {
        return greaterEqual(fromInt(other));
    }

    @INLINE
    public final boolean greaterThan(Offset other) {
        if (Word.width() == 64) {
            return toLong() > other.toLong();
        }
        return toInt() > other.toInt();
    }

    @INLINE
    public final boolean greaterThan(int other) {
        return greaterThan(fromInt(other));
    }

    @INLINE
    public final Offset negate() {
        if (Word.width() == 64) {
            return fromLong(-toLong());
        }
        return fromInt(-toInt());
    }

    @INLINE
    public final boolean isNegative() {
        if (Word.width() == 64) {
            return toLong() < 0L;
        }
        return toInt() < 0;
    }

    @INLINE
    public final Offset plus(Offset addend) {
        if (Word.width() == 64) {
            return fromLong(toLong() + addend.toLong());
        }
        return fromInt(toInt() + addend.toInt());
    }

    @INLINE
    public final Offset plus(Size addend) {
        return plus(addend.asOffset());
    }

    @INLINE
    public final Offset plus(int addend) {
        return plus(fromInt(addend));
    }

    @INLINE
    public final Offset plus(long addend) {
        return plus(fromLong(addend));
    }

    @INLINE
    public final Offset minus(Offset subtrahend) {
        if (Word.width() == 64) {
            return fromLong(toLong() - subtrahend.toLong());
        }
        return fromInt(toInt() - subtrahend.toInt());
    }

    @INLINE
    public final Offset minus(Size subtrahend) {
        return minus(subtrahend.asOffset());
    }

    @INLINE
    public final Offset minus(int subtrahend) {
        return minus(fromInt(subtrahend));
    }

    @INLINE
    public final Offset minus(long subtrahend) {
        return minus(fromLong(subtrahend));
    }

    @INLINE
    public final Offset times(Offset factor) {
        if (Word.width() == 64) {
            return fromLong(toLong() * factor.toLong());
        }
        return fromInt(toInt() * factor.toInt());
    }

    @INLINE
    public final Offset times(Address factor) {
        return times(factor.asOffset());
    }

    @INLINE
    public final Offset times(int factor) {
        return times(fromInt(factor));
    }

    @INLINE
    public final Offset dividedBy(Offset divisor) throws ArithmeticException {
        if (Word.width() == 64) {
            return fromLong(toLong() / divisor.toLong());
        }
        return fromInt(toInt() / divisor.toInt());
    }

    @INLINE
    public final Offset dividedBy(int divisor) throws ArithmeticException {
        return dividedBy(fromInt(divisor));
    }

    @INLINE
    public final Offset remainder(Offset divisor) throws ArithmeticException {
        if (Word.width() == 64) {
            return fromLong(toLong() % divisor.toLong());
        }
        return fromInt(toInt() % divisor.toInt());
    }

    @INLINE
    public final int remainder(int divisor) throws ArithmeticException {
        return remainder(fromInt(divisor)).toInt();
    }

    @INLINE
    public final boolean isRoundedBy(int numberOfBytes) {
        return remainder(numberOfBytes) == 0;
    }

    @INLINE
    public final Offset roundedUpBy(int numberOfBytes) {
        if (isRoundedBy(numberOfBytes)) {
            return this;
        }
        return plus(numberOfBytes - remainder(numberOfBytes));
    }

    @INLINE(override = true)
    public Offset and(Offset operand) {
        if (Word.width() == 64) {
            return fromLong(toLong() & operand.toLong());
        }
        return fromInt(toInt() & operand.toInt());
    }

    @INLINE(override = true)
    public Offset and(int operand) {
        return and(fromInt(operand));
    }

    @INLINE(override = true)
    public Offset and(long operand) {
        return and(fromLong(operand));
    }

    @INLINE(override = true)
    public Offset or(Offset operand) {
        if (Word.width() == 64) {
            return fromLong(toLong() | operand.toLong());
        }
        return fromInt(toInt() | operand.toInt());
    }

    @INLINE(override = true)
    public Offset or(int operand) {
        return or(fromInt(operand));
    }

    @INLINE(override = true)
    public Offset or(long operand) {
        return or(fromLong(operand));
    }

    @INLINE(override = true)
    public Offset not() {
        if (Word.width() == 64) {
            return fromLong(~toLong());
        }
        return fromInt(~toInt());
    }

    @INLINE
    public final Offset roundedDownBy(int numberOfBytes) {
        return minus(remainder(numberOfBytes));
    }

    @INLINE(override = true)
    public final Offset aligned() {
        final int n = Word.size();
        return plus(n - 1).and(Offset.fromInt(n - 1).not());
    }

    @INLINE(override = true)
    public final boolean isAligned() {
        final int n = Word.size();
        return and(n - 1).equals(Offset.zero());
    }

    @HOSTED_ONLY
    public final int numberOfEffectiveBits() {
        if (Word.width() == 64) {
            final long n = toLong();
            if (n >= 0) {
                return 64 - Long.numberOfLeadingZeros(n);
            }
            return 65 - Long.numberOfLeadingZeros(~n);
        }
        final int n = toInt();
        if (n >= 0) {
            return 32 - Integer.numberOfLeadingZeros(n);
        }
        return 33 - Integer.numberOfLeadingZeros(~n);
    }

    @HOSTED_ONLY
    public final WordWidth effectiveWidth() {
        final int bit = numberOfEffectiveBits();
        for (WordWidth width : WordWidth.values()) {
            if (bit < width.numberOfBits) {
                return width;
            }
        }
        throw ProgramError.unexpected();
    }
}
