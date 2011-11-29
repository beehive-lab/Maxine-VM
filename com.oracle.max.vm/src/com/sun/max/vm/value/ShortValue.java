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
public final class ShortValue extends PrimitiveValue<ShortValue> {

    private final short value;

    private static final class Cache {
        private Cache() {
        }

        static final ShortValue[] cache = new ShortValue[-(-128) + 127 + 1];

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new ShortValue((short) (i - 128));
            }
        }
    }

    public static ShortValue from(short value) {
        final int offset = 128;
        final int valueAsInt = value;
        if (valueAsInt >= -128 && valueAsInt <= 127) { // must cache
            return Cache.cache[valueAsInt + offset];
        }
        return new ShortValue(value);
    }

    public static ShortValue[] arrayFrom(short... values) {
        final ShortValue[] result = new ShortValue[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = from(values[i]);
        }
        return result;
    }

    private ShortValue(short value) {
        this.value = value;
    }

    @Override
    public Kind<ShortValue> kind() {
        return Kind.SHORT;
    }

    @Override
    public boolean isZero() {
        return value == (short) 0;
    }

    @Override
    public boolean isAllOnes() {
        return value == (short) -1;
    }

    public static final ShortValue ZERO = ShortValue.from((short) 0);

    @Override
    protected int unsignedCompareSameKind(ShortValue other) {
        return unsignedToInt() - other.unsignedToInt();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            // common case of reference equality
            return true;
        }
        if (!(other instanceof ShortValue)) {
            return false;
        }
        final ShortValue shortValue = (ShortValue) other;
        return value == shortValue.asShort();
    }

    @Override
    public String toString() {
        return Short.toString(value);
    }

    @Override
    public Short asBoxedJavaValue() {
        return Short.valueOf(value);
    }

    @Override
    public byte toByte() {
        return (byte) value;
    }

    @Override
    public byte unsignedToByte() {
        return (byte) (value & 0xff);
    }

    @Override
    public boolean toBoolean() {
        return (value != (short) 0) ? true : false;
    }

    @Override
    public short asShort() {
        return value;
    }

    @Override
    public short unboxShort() {
        return value;
    }

    @Override
    public int unboxInt() {
        return toInt();
    }

    @Override
    public short toShort() {
        return value;
    }

    @Override
    public short unsignedToShort() {
        return value;
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
        return value & 0xffff;
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
        return WordWidth.signedEffective(value);
    }

    @Override
    public WordWidth unsignedEffectiveWidth() {
        return WordWidth.unsignedEffective(value & 0xffff);
    }

    @Override
    public byte[] toBytes(DataModel dataModel) {
        return dataModel.toBytes(value);
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeShort(value);
    }

    @Override
    public CiConstant asCiConstant() {
        return CiConstant.forShort(value);
    }
}
