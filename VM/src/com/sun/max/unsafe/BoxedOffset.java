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
 * Boxed version of Offset.
 *
 * @author Bernd Mathiske
 */
@HOSTED_ONLY
public final class BoxedOffset extends Offset implements Boxed {

    private long nativeWord;

    public static final BoxedOffset ZERO = new BoxedOffset(0);

    private static final class Cache {
        private Cache() {
        }

        static final int LOWEST_VALUE = -100;
        static final int HIGHEST_VALUE = 1000;

        static final BoxedOffset[] cache = new BoxedOffset[(HIGHEST_VALUE - LOWEST_VALUE) + 1];

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new BoxedOffset(i + LOWEST_VALUE);
            }
        }
    }

    public static BoxedOffset from(long value) {
        if (value == 0) {
            return ZERO;
        }
        if (value >= Cache.LOWEST_VALUE && value <= Cache.HIGHEST_VALUE) {
            return Cache.cache[(int) value - Cache.LOWEST_VALUE];
        }
        return new BoxedOffset(value);
    }

    private BoxedOffset(long value) {
        nativeWord = value;
    }

    public long value() {
        return nativeWord;
    }
}
