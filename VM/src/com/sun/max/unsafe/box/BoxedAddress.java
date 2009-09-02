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
package com.sun.max.unsafe.box;

import com.sun.max.unsafe.*;

/**
 * Boxed version of Address.
 *
 * @see Address
 *
 * @author Bernd Mathiske
 */
public final class BoxedAddress extends Address implements UnsafeBox {

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

    public long nativeWord() {
        return nativeWord;
    }

    private static native long nativeDivide(long dividend, long divisor);

    @Override
    public Address dividedByAddress(Address divisor) {
        final BoxedAddress box = (BoxedAddress) divisor.asAddress();
        if (box.nativeWord == 0L) {
            throw new ArithmeticException();
        }
        return new BoxedAddress(nativeDivide(nativeWord, box.nativeWord));
    }

    @Override
    public Address dividedByInt(int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException();
        }
        return new BoxedAddress(nativeDivide(nativeWord, divisor & BoxedWord.INT_MASK));
    }

    private static native long nativeRemainder(long dividend, long divisor);

    @Override
    public Address remainderByAddress(Address divisor) {
        final BoxedAddress box = (BoxedAddress) divisor.asAddress();
        if (box.nativeWord == 0L) {
            throw new ArithmeticException();
        }
        return new BoxedAddress(nativeRemainder(nativeWord, box.nativeWord));
    }

    @Override
    public int remainderByInt(int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException();
        }
        return (int) nativeRemainder(nativeWord, divisor & BoxedWord.INT_MASK);
    }
}
