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
 * Boxed version of Size.
 *
 * @see Size
 *
 * @author Bernd Mathiske
 */
public final class BoxedSize extends Size implements UnsafeBox {

    private long _nativeWord;

    public static final BoxedSize ZERO = new BoxedSize(0);
    public static final BoxedSize MAX = new BoxedSize(-1L);

    private static final class Cache {
        private Cache() {
        }

        static final int HIGHEST_VALUE = 1000;

        static final BoxedSize[] _cache = new BoxedSize[HIGHEST_VALUE + 1];

        static {
            for (int i = 0; i < _cache.length; i++) {
                _cache[i] = new BoxedSize(i);
            }
        }
    }

    public static BoxedSize from(long value) {
        if (value == 0) {
            return ZERO;
        }
        if (value >= 0 && value <= Cache.HIGHEST_VALUE) {
            return Cache._cache[(int) value];
        }
        if (value == -1L) {
            return MAX;
        }

        return new BoxedSize(value);
    }

    private BoxedSize(long value) {
        _nativeWord = value;
    }

    public static BoxedSize from(int value) {
        return from(value & BoxedWord.INT_MASK);
    }

    public long nativeWord() {
        return _nativeWord;
    }

    @Override
    protected Size dividedByAddress(Address divisor) {
        return BoxedAddress.from(_nativeWord).dividedByAddress(divisor).asSize();
    }

    @Override
    protected Size dividedByInt(int divisor) {
        return BoxedAddress.from(_nativeWord).dividedByInt(divisor).asSize();
    }

    @Override
    protected Size remainderByAddress(Address divisor) {
        return BoxedAddress.from(_nativeWord).remainderByAddress(divisor).asSize();
    }

    @Override
    protected int remainderByInt(int divisor) {
        return BoxedAddress.from(_nativeWord).remainderByInt(divisor);
    }

}
