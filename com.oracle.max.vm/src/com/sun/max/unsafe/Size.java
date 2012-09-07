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

/**
 * Use this type rather than Address to remark that you are referring to the size of something. A "size" is always
 * referring to a number of bytes. (In contrast, a "length" is bound to a given snippet unit that may differ from
 * bytes.)
 *
 * We delegate much of the implementation to Address.
 * We only need this for running on the host VM,
 * because there the actual types are BoxedSize and BoxedAddress.
 */
public final class Size extends Address {

    @HOSTED_ONLY
    public Size(long value) {
        super(value);
    }

    @HOSTED_ONLY
    public static final Size ZERO = new Size(0);

    @INLINE
    public static Size zero() {
        return isHosted() ? ZERO : fromInt(0);
    }

    public static final Size K = Size.fromInt(Ints.K);
    public static final Size M = Size.fromInt(Ints.M);
    public static final Size G = Size.fromLong(Longs.G);

    @INLINE
    public static Size fromUnsignedInt(int value) {
        return Address.fromUnsignedInt(value).asSize();
    }

    @INLINE
    public static Size fromInt(int value) {
        if (isHosted()) {
            return fromLong(value & INT_MASK);
        }
        return Address.fromInt(value).asSize();
    }

    @INLINE
    public static Size fromLong(long value) {
        if (isHosted()) {
            if (value == 0) {
                return ZERO;
            }
            return new Size(value);
        }
        return Address.fromLong(value).asSize();
    }

    @Override
    @HOSTED_ONLY
    public String toString() {
        return "#" + toUnsignedString(10);
    }

    @INLINE
    @Override
    public Size plus(int addend) {
        return super.plus(addend).asSize();
    }

    @INLINE
    @Override
    public Size plus(long addend) {
        return super.plus(addend).asSize();
    }

    @INLINE
    @Override
    public Size plus(Address addend) {
        return super.plus(addend).asSize();
    }

    @INLINE
    @Override
    public Size plus(Offset addend) {
        return super.plus(addend).asSize();
    }

    @INLINE
    @Override
    public Size minus(Address subtrahend) {
        return super.minus(subtrahend).asSize();
    }

    @INLINE
    @Override
    public Size minus(int subtrahend) {
        return super.minus(subtrahend).asSize();
    }

    @INLINE
    @Override
    public Size minus(long subtrahend) {
        return super.minus(subtrahend).asSize();
    }

    @INLINE
    @Override
    public Size minus(Offset subtrahend) {
        return super.minus(subtrahend).asSize();
    }

    @INLINE
    @Override
    public Size times(Address factor) {
        return super.times(factor).asSize();
    }

    @INLINE
    @Override
    public Size times(int factor) {
        return super.times(factor).asSize();
    }

    @INLINE
    @Override
    public Size dividedBy(Address divisor) {
        return super.dividedBy(divisor).asSize();
    }

    @INLINE
    @Override
    public Size dividedBy(int divisor) {
        return super.dividedBy(divisor).asSize();
    }

    @INLINE
    @Override
    public Size remainder(Address divisor) {
        return super.remainder(divisor).asSize();
    }

    @INLINE
    @Override
    public Size roundedUpBy(int nBytes) {
        return super.roundedUpBy(nBytes).asSize();
    }

    @INLINE
    @Override
    public Size roundedDownBy(int nBytes) {
        return super.roundedDownBy(nBytes).asSize();
    }

    @INLINE
    @Override
    public Size wordAligned() {
        return super.wordAligned().asSize();
    }

    @INLINE
    @Override
    public Size alignUp(int alignment) {
        return super.alignUp(alignment).asSize();
    }

    @INLINE
    @Override
    public Size alignDown(int alignment) {
        return super.alignDown(alignment).asSize();
    }

    @INLINE
    @Override
    public boolean isWordAligned() {
        return super.isWordAligned();
    }

    @INLINE
    @Override
    public Size bitSet(int index) {
        return super.bitSet(index).asSize();
    }

    @INLINE
    @Override
    public Size bitClear(int index) {
        return super.bitClear(index).asSize();
    }

    @INLINE
    @Override
    public Size and(Address operand) {
        return super.and(operand).asSize();
    }

    @INLINE
    @Override
    public Size and(int operand) {
        return super.and(operand).asSize();
    }

    @INLINE
    @Override
    public Size and(long operand) {
        return super.and(operand).asSize();
    }

    @INLINE
    @Override
    public Size or(Address operand) {
        return super.or(operand).asSize();
    }

    @INLINE
    @Override
    public Size or(int operand) {
        return super.or(operand).asSize();
    }

    @INLINE
    @Override
    public Size or(long operand) {
        return super.or(operand).asSize();
    }

    @INLINE
    @Override
    public Size not() {
        return super.not().asSize();
    }

    @INLINE
    @Override
    public Size shiftedLeft(int nBits) {
        return super.shiftedLeft(nBits).asSize();
    }

    @INLINE
    @Override
    public Size unsignedShiftedRight(int nBits) {
        return super.unsignedShiftedRight(nBits).asSize();
    }

    @INLINE
    public static Size min(Size a, Size b) {
        if (a.lessThan(b)) {
            return a;
        }
        return b;
    }
}
