/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.cri.xir;


public class IntBitSet<T extends Enum> {
    private int bits;
    private static <T extends Enum>int bitmask(T bit) {
        return 1 << bit.ordinal();
    }

    public boolean isSet(T bit) {
        return (bits & bitmask(bit)) != 0L;
    }
    public boolean isClear(T bit) {
        return (bits & bitmask(bit)) == 0L;
    }

    public IntBitSet<T>  set(T bit) {
        bits |= bitmask(bit);
        return this;
    }
    public IntBitSet<T >  clear(T bit) {
        bits &= ~bitmask(bit);
        return this;
    }

    public static <T extends Enum> int set(T bit, int bits) {
        return bits | bitmask(bit);
    }

    public static <T extends Enum> int clear(T bit, int bits) {
        return bits & ~bitmask(bit);
    }

    public int value() {
        return bits;
    }

    public boolean equals(IntBitSet<T> set) {
        return bits == set.bits;
    }
}
