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
 * Boxed version of Address.
 *
 * @see Address
 */
@HOSTED_ONLY
public final class BoxedAddress extends Address implements Boxed {

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

    public long value() {
        return nativeWord;
    }
}
