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

import static com.sun.max.vm.MaxineVM.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * Offsets from addresses or pointers. Unlike an 'Address', an 'Offset' can be negative. Both types have the identical
 * number of bits used for representation. However, 'Offset' uses twos complement, whereas 'Address' is simply unsigned.
 *
 * @author Bernd Mathiske
 */
public abstract class Offset extends Word {

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
