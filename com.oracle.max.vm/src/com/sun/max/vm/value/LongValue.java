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
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 */
public final class LongValue extends PrimitiveValue<LongValue> {

    public final long value;

    private static final class Cache {
        private Cache() {
        }

        static final LongValue[] cache = new LongValue[-(-128) + 127 + 1];

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new LongValue(i - 128);
            }
        }
    }

    public static LongValue from(long value) {
        final int offset = 128;
        if (value >= -128 && value <= 127) { // will cache
            return Cache.cache[(int) value + offset];
        }
        return new LongValue(value);
    }

    public static LongValue[] arrayFrom(long... values) {
        final LongValue[] result = new LongValue[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = from(values[i]);
        }
        return result;
    }

    private LongValue(long value) {
        this.value = value;
    }

    public static final LongValue MINUS_ONE = LongValue.from(-1L);
    public static final LongValue ZERO = LongValue.from(0L);
    public static final LongValue ONE = LongValue.from(1L);

    @Override
    public Kind<LongValue> kind() {
        return Kind.LONG;
    }

    @Override
    public boolean isZero() {
        return value == 0L;
    }

    @Override
    public boolean isAllOnes() {
        return value == -1L;
    }

    @Override
    protected int unsignedCompareSameKind(LongValue other) {
        if (Word.width() == 64) {
            return Address.fromLong(value).compareTo(Address.fromLong(other.value));
        }
        throw FatalError.unimplemented();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            // common case of reference equality
            return true;
        }
        if (!(other instanceof LongValue)) {
            return false;
        }
        final LongValue longValue = (LongValue) other;
        return value == longValue.asLong();
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public Long asBoxedJavaValue() {
        return Long.valueOf(value);
    }

    @Override
    public byte toByte() {
        return (byte) value;
    }

    @Override
    public byte unsignedToByte() {
        return (byte) (value & 0xffL);
    }

    @Override
    public boolean toBoolean() {
        return (value != 0L) ? true : false;
    }

    @Override
    public short toShort() {
        return (short) value;
    }

    @Override
    public short unsignedToShort() {
        return (short) (value & 0xffffL);
    }

    @Override
    public char toChar() {
        return (char) value;
    }

    @Override
    public int toInt() {
        return (int) value;
    }

    @Override
    public int unsignedToInt() {
        return (int) (value & 0xffffffffL);
    }

    @Override
    public float toFloat() {
        return value;
    }

    @Override
    public long asLong() {
        return value;
    }

    @Override
    public long unboxLong() {
        return value;
    }

    @Override
    public Word unboxWord() {
        if (64 == Word.width()) {
            return toWord();
        }
        return super.unboxWord();
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
        return Offset.fromLong(value);
    }

    @Override
    public WordWidth signedEffectiveWidth() {
        return WordWidth.signedEffective(value);
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
        stream.writeLong(value);
    }

    @Override
    public CiConstant asCiConstant() {
        return CiConstant.forLong(value);
    }

}
