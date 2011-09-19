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
public final class FloatValue extends PrimitiveValue<FloatValue> {

    private final float value;

    public static FloatValue from(float value) {
        return new FloatValue(value);
    }

    public static FloatValue[] arrayFrom(float... values) {
        final FloatValue[] result = new FloatValue[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = from(values[i]);
        }
        return result;
    }

    private FloatValue(float value) {
        this.value = value;
    }

    public static final FloatValue ZERO = FloatValue.from((float) 0.0);
    public static final FloatValue ONE = FloatValue.from((float) 1.0);
    public static final FloatValue TWO = FloatValue.from((float) 2.0);

    @Override
    public Kind<FloatValue> kind() {
        return Kind.FLOAT;
    }

    @Override
    public boolean isZero() {
        return value == (float) 0.0;
    }

    @Override
    public boolean isAllOnes() {
        return Float.floatToRawIntBits(value) == -1;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || ((other instanceof FloatValue) && (Float.floatToIntBits(((FloatValue) other).value) == Float.floatToIntBits(value)));
    }

    @Override
    protected int compareSameKind(FloatValue other) {
        return value < other.value ? -1 : (value == other.value ? 0 : 1);
    }

    @Override
    public String toString() {
        return Float.toString(value);
    }

    @Override
    public Float asBoxedJavaValue() {
        return Float.valueOf(value);
    }

    @Override
    public byte toByte() {
        return (byte) value;
    }

    @Override
    public boolean toBoolean() {
        return (value != (float) 0.0) ? true : false;
    }

    @Override
    public short toShort() {
        return (short) value;
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
    public float asFloat() {
        return value;
    }

    @Override
    public float unboxFloat() {
        return value;
    }

    @Override
    public float toFloat() {
        return value;
    }

    @Override
    public long toLong() {
        return (long) value;
    }

    @Override
    public double toDouble() {
        return value;
    }

    @Override
    public Word toWord() {
        return Offset.fromLong((long) value);
    }

    @Override
    public WordWidth signedEffectiveWidth() {
        return WordWidth.BITS_32;
    }

    @Override
    public WordWidth unsignedEffectiveWidth() {
        return WordWidth.BITS_32;
    }

    @Override
    public byte[] toBytes(DataModel dataModel) {
        return dataModel.toBytes(value);
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeFloat(value);
    }

    @Override
    public CiConstant asCiConstant() {
        return CiConstant.forFloat(value);
    }

}
