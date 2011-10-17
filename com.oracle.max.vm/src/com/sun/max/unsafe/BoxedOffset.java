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
 * Boxed version of Offset.
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
