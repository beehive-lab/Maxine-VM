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
package com.sun.max.util;

/**
 * A specialized bit set implementation to use with enum types.
 * The ordinal of the enumerated values are used as bit position in a bit set of width no larger than the width of an int.
 * Each enum value is treated as a bit in the bit set.
 * This allows implementing set of flags over a single int and perform flag setting / clearing / checking.
 *
 * @param <T> an enum type
 */
public class IntBitSet<T extends Enum> {
    /**
     * Storage for the bit set.
     */
    private int bits;
    private static <T extends Enum>int bitmask(T bit) {
        return 1 << bit.ordinal();
    }

    /**
     * Test if the bit corresponding to an enum value is set.
     * @param bit an enum value
     * @return true if the bit is set, false otherwise.
     */
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
