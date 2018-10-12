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
package com.sun.max.vm.value;

import java.io.*;

import com.sun.cri.ci.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.type.*;

/**
 */
public final class CharValue extends PrimitiveValue<CharValue> {

    final char value;

    private static final class Cache {
        private Cache() {
        }

        static final CharValue[] cache = new CharValue[127 + 1];

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new CharValue((char) i);
            }
        }
    }

    public static CharValue from(char value) {
        if (value <= 127) { // must cache
            return Cache.cache[value];
        }
        return new CharValue(value);
    }

    public static CharValue[] arrayFrom(char... values) {
        final CharValue[] result = new CharValue[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = from(values[i]);
        }
        return result;
    }

    private CharValue(char value) {
        this.value = value;
    }

    @Override
    public Kind<CharValue> kind() {
        return Kind.CHAR;
    }

    @Override
    public boolean isZero() {
        return value == (char) 0;
    }

    @Override
    public boolean isAllOnes() {
        return value == (char) -1;
    }

    public static final CharValue ZERO = CharValue.from('\0');

    @Override
    protected int unsignedCompareSameKind(CharValue other) {
        // Char value comparisons are always unsigned
        return compareSameKind(other);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            // common case of reference equality
            return true;
        }
        if (!(other instanceof CharValue)) {
            return false;
        }
        final CharValue charValue = (CharValue) other;
        return value == charValue.asChar();
    }

    @Override
    public String toString() {
        return Character.toString(value);
    }

    @Override
    public Character asBoxedJavaValue() {
        return Character.valueOf(value);
    }

    @Override
    public byte toByte() {
        return (byte) value;
    }

    @Override
    public byte unsignedToByte() {
        return toByte();
    }

    @Override
    public boolean toBoolean() {
        return (value != '\0') ? true : false;
    }

    @Override
    public short toShort() {
        return (short) value;
    }

    @Override
    public short unsignedToShort() {
        return toShort();
    }

    @Override
    public char asChar() {
        return value;
    }

    @Override
    public char unboxChar() {
        return value;
    }

    @Override
    public int unboxInt() {
        return toInt();
    }

    @Override
    public char toChar() {
        return value;
    }

    @Override
    public int toInt() {
        return value;
    }

    @Override
    public int unsignedToInt() {
        return value;
    }

    @Override
    public float toFloat() {
        return value;
    }

    @Override
    public long toLong() {
        return value;
    }

    @Override
    public double toDouble() {
        return value;
    }

    @Override
    public Word toWord() {
        return Address.fromInt(value);
    }

    @Override
    public WordWidth signedEffectiveWidth() {
        return WordWidth.signedEffective((short) value);
    }

    @Override
    public WordWidth unsignedEffectiveWidth() {
        return WordWidth.unsignedEffective(value);
    }

    @Override
    public byte[] toBytes(DataModel dataModel) {
        return dataModel.toBytes(value);
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeChar(value);
    }

    @Override
    public CiConstant asCiConstant() {
        return CiConstant.forChar(value);
    }

}
