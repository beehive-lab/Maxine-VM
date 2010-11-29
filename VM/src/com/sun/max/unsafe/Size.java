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

/**
 * Use this type rather than Address to remark that you are referring to the size of something. A "size" is always
 * referring to a number of bytes. (In contrast, a "length" is bound to a given snippet unit that may differ from
 * bytes.)
 *
 * We delegate much of the implementation to Address.
 * We only need this for running on the host VM,
 * because there the actual types are BoxedSize and BoxedAddress.
 *
 * @author Bernd Mathiske
 */
public abstract class Size extends Address {

    protected Size() {
    }

    @INLINE
    public static Size zero() {
        return isHosted() ? BoxedSize.ZERO : fromInt(0);
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
        return Address.fromInt(value).asSize();
    }

    @INLINE
    public static Size fromLong(long value) {
        return Address.fromLong(value).asSize();
    }

    @Override
    public String toString() {
        return "#" + toUnsignedString(10);
    }

    @INLINE
    @Override
    public final Size plus(int addend) {
        return asAddress().plus(addend).asSize();
    }

    @INLINE
    @Override
    public final Size plus(long addend) {
        return asAddress().plus(addend).asSize();
    }

    @INLINE
    @Override
    public final Size plus(Address addend) {
        return asAddress().plus(addend).asSize();
    }

    @INLINE
    @Override
    public final Size plus(Offset addend) {
        return asAddress().plus(addend).asSize();
    }

    @INLINE
    @Override
    public final Size minus(Address subtrahend) {
        return asAddress().minus(subtrahend).asSize();
    }

    @INLINE
    @Override
    public final Size minus(int subtrahend) {
        return asAddress().minus(subtrahend).asSize();
    }

    @INLINE
    @Override
    public final Size minus(long subtrahend) {
        return asAddress().minus(subtrahend).asSize();
    }

    @INLINE
    @Override
    public final Size minus(Offset subtrahend) {
        return asAddress().minus(subtrahend).asSize();
    }

    @INLINE
    @Override
    public final Size times(Address factor) {
        return asAddress().times(factor).asSize();
    }

    @INLINE
    @Override
    public final Size times(int factor) {
        return asAddress().times(factor).asSize();
    }

    @INLINE
    @Override
    public final Size dividedBy(Address divisor) {
        return asAddress().dividedBy(divisor).asSize();
    }

    @INLINE
    @Override
    public final Size dividedBy(int divisor) {
        return asAddress().dividedBy(divisor).asSize();
    }

    @INLINE
    @Override
    public final Size remainder(Address divisor) {
        return asAddress().remainder(divisor).asSize();
    }

    @INLINE
    @Override
    public final Size roundedUpBy(int nBytes) {
        return asAddress().roundedUpBy(nBytes).asSize();
    }

    @INLINE
    @Override
    public final Size roundedDownBy(int nBytes) {
        return asAddress().roundedDownBy(nBytes).asSize();
    }

    @INLINE
    @Override
    public final Size wordAligned() {
        return asAddress().wordAligned().asSize();
    }

    @INLINE(override = true)
    @Override
    public Size aligned(int alignment) {
        return asAddress().aligned(alignment).asSize();
    }

    @INLINE(override = true)
    @Override
    public final boolean isWordAligned() {
        return asAddress().isWordAligned();
    }

    @INLINE
    @Override
    public final Size bitSet(int index) {
        return asAddress().bitSet(index).asSize();
    }

    @INLINE
    @Override
    public final Size bitClear(int index) {
        return asAddress().bitClear(index).asSize();
    }

    @INLINE
    @Override
    public final Size and(Address operand) {
        return asAddress().and(operand).asSize();
    }

    @INLINE
    @Override
    public final Size and(int operand) {
        return asAddress().and(operand).asSize();
    }

    @INLINE
    @Override
    public final Size and(long operand) {
        return asAddress().and(operand).asSize();
    }

    @INLINE
    @Override
    public final Size or(Address operand) {
        return asAddress().or(operand).asSize();
    }

    @INLINE
    @Override
    public final Size or(int operand) {
        return asAddress().or(operand).asSize();
    }

    @INLINE
    @Override
    public final Size or(long operand) {
        return asAddress().or(operand).asSize();
    }

    @INLINE
    @Override
    public final Size not() {
        return asAddress().not().asSize();
    }

    @INLINE
    @Override
    public final Size shiftedLeft(int nBits) {
        return asAddress().shiftedLeft(nBits).asSize();
    }

    @INLINE
    @Override
    public final Size unsignedShiftedRight(int nBits) {
        return asAddress().unsignedShiftedRight(nBits).asSize();
    }

    public static Size min(Size a, Size b) {
        if (a.lessThan(b)) {
            return a;
        }
        return b;
    }

}
