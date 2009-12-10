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

import com.sun.max.annotate.*;

/**
 * Boxed version of Size.
 *
 * @see Size
 *
 * @author Bernd Mathiske
 */
@HOSTED_ONLY
public final class BoxedSize extends Size implements Boxed {

    private long nativeWord;

    public static final BoxedSize ZERO = new BoxedSize(0);
    public static final BoxedSize MAX = new BoxedSize(-1L);

    private static final class Cache {
        private Cache() {
        }

        static final int HIGHEST_VALUE = 1000;

        static final BoxedSize[] cache = new BoxedSize[HIGHEST_VALUE + 1];

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new BoxedSize(i);
            }
        }
    }

    public static BoxedSize from(long value) {
        if (value == 0) {
            return ZERO;
        }
        if (value >= 0 && value <= Cache.HIGHEST_VALUE) {
            return Cache.cache[(int) value];
        }
        if (value == -1L) {
            return MAX;
        }

        return new BoxedSize(value);
    }

    private BoxedSize(long value) {
        nativeWord = value;
    }

    public static BoxedSize from(int value) {
        return from(value & BoxedWord.INT_MASK);
    }

    public long value() {
        return nativeWord;
    }

    @Override
    protected Size dividedByAddress(Address divisor) {
        return BoxedAddress.from(nativeWord).dividedByAddress(divisor).asSize();
    }

    @Override
    protected Size dividedByInt(int divisor) {
        return BoxedAddress.from(nativeWord).dividedByInt(divisor).asSize();
    }

    @Override
    protected Size remainderByAddress(Address divisor) {
        return BoxedAddress.from(nativeWord).remainderByAddress(divisor).asSize();
    }

    @Override
    protected int remainderByInt(int divisor) {
        return BoxedAddress.from(nativeWord).remainderByInt(divisor);
    }

}
