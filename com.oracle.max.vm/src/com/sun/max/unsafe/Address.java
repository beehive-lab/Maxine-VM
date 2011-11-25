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

import java.math.*;

import com.oracle.max.cri.intrinsics.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * A machine word interpreted as a linear address.
 * An Address is unsigned and arithmetic is supported.
 */
public abstract class Address extends Word {

    @HOSTED_ONLY
    protected Address() {
    }

    @INLINE
    public static Address zero() {
        return isHosted() ? BoxedAddress.ZERO : fromInt(0);
    }

    @INLINE
    public static Address max() {
        return isHosted() ? BoxedAddress.MAX : fromLong(-1L);
    }

    /**
     * Creates an Address value from a given int value. Note that unlike {@link #fromInt(int)},
     * the given int value is not sign extended. Also note that on 32-bit platforms, this operation
     * is effectively a no-op.
     *
     * @param value the value to be converted to an Address
     */
    @INLINE
    public static Address fromUnsignedInt(int value) {
        if (isHosted()) {
            final long longValue = value;
            final long n = longValue & 0xffffffffL;
            return BoxedAddress.from(n);
        }
        if (Word.width() == 64) {
            final long longValue = value;
            final long n = longValue & 0xffffffffL;
            return UnsafeCast.asAddress(n);
        }
        return UnsafeCast.asAddress(value);
    }

    /**
     * Creates an Address value from a given int value. Note that unlike {@link #fromUnsignedInt(int)},
     * the given int value is sign extended first. Also note that on 32-bit platforms, this operation
     * is effectively a no-op.
     *
     * @param value the value to be converted to an Address
     */
    @INLINE
    public static Address fromInt(int value) {
        if (isHosted()) {
            final long n = value;
            return BoxedAddress.from(n);
        }
        if (Word.width() == 64) {
            final long n = value;
            return UnsafeCast.asAddress(n);
        }
        return UnsafeCast.asAddress(value);
    }

    @INLINE
    public static Address fromLong(long value) {
        if (isHosted()) {
            return BoxedAddress.from(value);
        }
        if (Word.width() == 64) {
            return UnsafeCast.asAddress(value);
        }
        final int n = (int) value;
        return UnsafeCast.asAddress(n);
    }

    @Override
    @HOSTED_ONLY
    public String toString() {
        return "@" + toHexString();
    }

    @HOSTED_ONLY
    public final String toUnsignedString(int radix) {
        if (radix == 16) {
            if (Word.width() == 64) {
                return Long.toHexString(toLong());
            }
            assert Word.width() == 32;
            return Integer.toHexString(toInt());
        }
        if (radix == 8) {
            if (Word.width() == 64) {
                return Long.toOctalString(toLong());
            }
            assert Word.width() == 32;
            return Integer.toOctalString(toInt());
        }
        if (radix == 2) {
            if (Word.width() == 64) {
                return Long.toBinaryString(toLong());
            }
            assert Word.width() == 32;
            return Integer.toBinaryString(toInt());
        }
        assert radix == 10;

        final long n = toLong();
        if (Word.width() == 32) {
            if (n <= Integer.MAX_VALUE && n >= 0) {
                return Integer.toString(toInt());
            }
            return Long.toString(n & 0xffffffffL);
        }

        final long low = n & 0xffffffffL;
        final long high = n >>> 32;
        return BigInteger.valueOf(high).shiftLeft(32).or(BigInteger.valueOf(low)).toString();
    }

    @HOSTED_ONLY
    public static Address parse(String s, int radix) {
        Address result = Address.zero();
        for (int i = 0; i < s.length(); i++) {
            result = result.times(radix);
            result = result.plus(Integer.parseInt(String.valueOf(s.charAt(i)), radix));
        }
        return result;
    }

    @INLINE
    public final int toInt() {
        if (isHosted()) {
            final Boxed box = (Boxed) this;
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
            final Boxed box = (Boxed) this;
            return box.value();
        }
        if (Word.width() == 64) {
            return UnsafeCast.asLong(this);
        }
        return 0xffffffffL & UnsafeCast.asInt(this);
    }

    @INLINE
    public final int compareTo(Address other) {
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
    public final boolean greaterThan(Address other) {
        if (Word.width() == 64) {
            return UnsignedMath.aboveThan(toLong(), other.toLong());
        }
        return UnsignedMath.aboveThan(toInt(), other.toInt());
    }

    @INLINE
    public final boolean greaterThan(int other) {
        return greaterThan(fromInt(other));
    }

    @INLINE
    public final boolean greaterEqual(Address other) {
        if (Word.width() == 64) {
            return UnsignedMath.aboveOrEqual(toLong(), other.toLong());
        }
        return UnsignedMath.aboveOrEqual(toInt(), other.toInt());
    }

    @INLINE
    public final boolean greaterEqual(int other) {
        return greaterEqual(fromInt(other));
    }

    @INLINE
    public final boolean lessThan(Address other) {
        if (Word.width() == 64) {
            return UnsignedMath.belowThan(toLong(), other.toLong());
        }
        return UnsignedMath.belowThan(toInt(), other.toInt());
    }

    @INLINE
    public final boolean lessThan(int other) {
        return lessThan(fromInt(other));
    }

    @INLINE
    public final boolean lessEqual(Address other) {
        if (Word.width() == 64) {
            return UnsignedMath.belowOrEqual(toLong(), other.toLong());
        }
        return UnsignedMath.belowOrEqual(toInt(), other.toInt());
    }

    @INLINE
    public final boolean lessEqual(int other) {
        return lessEqual(fromInt(other));
    }

    @INLINE(override = true)
    public Address plus(Address addend) {
        return asOffset().plus(addend.asOffset()).asAddress();
    }

    @INLINE(override = true)
    public Address plus(Offset offset) {
        return asOffset().plus(offset).asAddress();
    }

    @INLINE(override = true)
    public Address plus(int addend) {
        return asOffset().plus(addend).asAddress();
    }

    @INLINE(override = true)
    public Address plus(long addend) {
        return asOffset().plus(addend).asAddress();
    }

    @INLINE(override = true)
    public Address minus(Address subtrahend) {
        return asOffset().minus(subtrahend.asOffset()).asAddress();
    }

    @INLINE(override = true)
    public Address minus(Offset offset) {
        return asOffset().minus(offset).asAddress();
    }

    @INLINE(override = true)
    public Address  plusWords(int nWords) {
        return plus(nWords * Word.size());
    }

    @INLINE(override = true)
    public Address minus(int subtrahend) {
        return asOffset().minus(subtrahend).asAddress();
    }

    @INLINE(override = true)
    public Address minusWords(int nWords) {
        return minus(nWords * Word.size());
    }

    @INLINE(override = true)
    public Address minus(long subtrahend) {
        return asOffset().minus(subtrahend).asAddress();
    }

    @INLINE(override = true)
    public Address times(Address factor) {
        return asOffset().times(factor.asOffset()).asAddress();
    }

    @INLINE(override = true)
    public Address times(int factor) {
        return asOffset().times(factor).asAddress();
    }

    @INLINE(override = true)
    public Address dividedBy(Address divisor) {
        if (Word.width() == 64) {
            return fromLong(UnsignedMath.divide(toLong(), divisor.toLong()));
        }
        return fromInt(UnsignedMath.divide(toInt(), divisor.toInt()));
    }

    @INLINE(override = true)
    public Address dividedBy(int divisor) {
        return dividedBy(fromInt(divisor));
    }

    @INLINE(override = true)
    public Address remainder(Address divisor) {
        if (Word.width() == 64) {
            return fromLong(UnsignedMath.remainder(toLong(), divisor.toLong()));
        }
        return fromInt(UnsignedMath.remainder(toInt(), divisor.toInt()));
    }

    @INLINE(override = true)
    public final int remainder(int divisor) {
        return remainder(fromInt(divisor)).toInt();
    }

    @INLINE(override = true)
    public final boolean isRoundedBy(Address nBytes) {
        return remainder(nBytes).isZero();
    }

    @INLINE(override = true)
    public final boolean isRoundedBy(int nBytes) {
        return remainder(nBytes) == 0;
    }

    @INLINE(override = true)
    public Address roundedUpBy(Address nBytes) {
        if (isRoundedBy(nBytes)) {
            return this;
        }
        return plus(nBytes.minus(remainder(nBytes)));
    }

    @INLINE(override = true)
    public Address roundedUpBy(int nBytes) {
        if (isRoundedBy(nBytes)) {
            return this;
        }
        return plus(nBytes - remainder(nBytes));
    }

    @INLINE(override = true)
    public Address roundedDownBy(int nBytes) {
        return minus(remainder(nBytes));
    }

    @INLINE(override = true)
    public Address wordAligned() {
        return alignUp(Word.size());
    }

    @INLINE(override = true)
    public Address alignUp(int alignment) {
        return plus(alignment - 1).alignDown(alignment);
    }

    @INLINE(override = true)
    public Address alignDown(int alignment) {
        return and(Address.fromInt(alignment - 1).not());
    }

    @INLINE(override = true)
    public boolean isWordAligned() {
        final int n = Word.size();
        return and(n - 1).equals(Address.zero());
    }

    @INLINE(override = true)
    public boolean isAligned(int alignment) {
        return and(alignment - 1).equals(Address.zero());
    }

    @INLINE(override = true)
    public final boolean isBitSet(int index) {
        return (toLong() & (1L << index)) != 0;
    }

    @INLINE(override = true)
    public Address bitSet(int index) {
        return fromLong(toLong() | (1L << index));
    }

    @INLINE(override = true)
    public Address bitClear(int index) {
        return fromLong(toLong() & ~(1L << index));
    }

    @INLINE(override = true)
    public Address and(Address operand) {
        if (Word.width() == 64) {
            return fromLong(toLong() & operand.toLong());
        }
        return fromInt(toInt() & operand.toInt());
    }

    @INLINE(override = true)
    public Address and(int operand) {
        return and(fromInt(operand));
    }

    @INLINE(override = true)
    public Address and(long operand) {
        return and(fromLong(operand));
    }

    @INLINE(override = true)
    public Address or(Address operand) {
        if (Word.width() == 64) {
            return fromLong(toLong() | operand.toLong());
        }
        return fromInt(toInt() | operand.toInt());
    }

    @INLINE(override = true)
    public Address or(int operand) {
        return or(fromInt(operand));
    }

    @INLINE(override = true)
    public Address or(long operand) {
        return or(fromLong(operand));
    }

    @INLINE(override = true)
    public Address not() {
        if (Word.width() == 64) {
            return fromLong(~toLong());
        }
        return fromInt(~toInt());
    }

    @INLINE(override = true)
    public Address shiftedLeft(int nBits) {
        if (Word.width() == 64) {
            return fromLong(toLong() << nBits);
        }
        return fromInt(toInt() << nBits);
    }

    @INLINE(override = true)
    public Address unsignedShiftedRight(int nBits) {
        if (Word.width() == 64) {
            return fromLong(toLong() >>> nBits);
        }
        return fromInt(toInt() >>> nBits);
    }

    @INLINE(override = true)
    public final int numberOfEffectiveBits() {
        if (Word.width() == 64) {
            return 64 - Long.numberOfLeadingZeros(toLong());
        }
        return 32 - Integer.numberOfLeadingZeros(toInt());
    }

    @HOSTED_ONLY
    public final WordWidth effectiveWidth() {
        final int bit = numberOfEffectiveBits();
        for (WordWidth width : WordWidth.VALUES) {
            if (bit < width.numberOfBits) {
                return width;
            }
        }
        throw ProgramError.unexpected();
    }
}
