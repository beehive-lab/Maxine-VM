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

import com.sun.max.annotate.*;

/**
 * Boxed version of Size.
 *
 * @see Size
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
