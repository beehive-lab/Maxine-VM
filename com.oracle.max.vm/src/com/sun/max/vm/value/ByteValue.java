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
public final class ByteValue extends PrimitiveValue<ByteValue> {

    private final byte value;

    private static final class Cache {
        private Cache() {
        }

        static final ByteValue[] cache = new ByteValue[-(-128) + 127 + 1];

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new ByteValue((byte) (i - 128));
            }
        }
    }

    public static ByteValue from(byte value) {
        final int offset = 128;
        return Cache.cache[value + offset];
    }

    public static ByteValue[] arrayFrom(byte... values) {
        final ByteValue[] result = new ByteValue[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = from(values[i]);
        }
        return result;
    }

    private ByteValue(byte value) {
        this.value = value;
    }

    @Override
    public Kind<ByteValue> kind() {
        return Kind.BYTE;
    }

    @Override
    public boolean isZero() {
        return value == (byte) 0;
    }

    @Override
    public boolean isAllOnes() {
        return value == (byte) -1;
    }

    public static final ByteValue ZERO = ByteValue.from((byte) 0);

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            // common case of reference equality
            return true;
        }
        if (!(other instanceof ByteValue)) {
            return false;
        }
        final ByteValue byteValue = (ByteValue) other;
        return value == byteValue.asByte();
    }

    @Override
    protected int unsignedCompareSameKind(ByteValue other) {
        return unsignedToInt() - other.unsignedToInt();
    }

    @Override
    public String toString() {
        return Byte.toString(value);
    }

    @Override
    public Byte asBoxedJavaValue() {
        return Byte.valueOf(value);
    }

    @Override
    public byte asByte() {
        return value;
    }

    @Override
    public byte unboxByte() {
        return value;
    }

    @Override
    public char unboxChar() {
        return toChar();
    }

    @Override
    public int unboxInt() {
        return toInt();
    }

    @Override
    public short unboxShort() {
        return toShort();
    }

    @Override
    public byte toByte() {
        return value;
    }

    @Override
    public byte unsignedToByte() {
        return value;
    }

    @Override
    public boolean toBoolean() {
        return (value != (byte) 0) ? true : false;
    }

    @Override
    public short toShort() {
        return value;
    }

    @Override
    public short unsignedToShort() {
        return (short) (value & 0xff);
    }

    @Override
    public char toChar() {
        return (char) value;
    }

    @Override
    public int toInt() {
        return value;
    }

    @Override
    public int unsignedToInt() {
        return value & 0xff;
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
        return Offset.fromInt(value);
    }

    @Override
    public WordWidth signedEffectiveWidth() {
        return WordWidth.BITS_8;
    }

    @Override
    public WordWidth unsignedEffectiveWidth() {
        return WordWidth.BITS_8;
    }

    @Override
    public byte[] toBytes(DataModel dataModel) {
        return dataModel.toBytes(value);
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeByte(value);
    }

    @Override
    public CiConstant asCiConstant() {
        return CiConstant.forByte(value);
    }
}
