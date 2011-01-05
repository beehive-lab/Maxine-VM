/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class DoubleValue extends PrimitiveValue<DoubleValue> {

    final double value;

    public static DoubleValue from(double value) {
        return new DoubleValue(value);
    }

    public static DoubleValue[] arrayFrom(double... values) {
        final DoubleValue[] result = new DoubleValue[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = from(values[i]);
        }
        return result;
    }

    private DoubleValue(double value) {
        this.value = value;
    }

    public static final DoubleValue ZERO = DoubleValue.from(0.0);
    public static final DoubleValue ONE = DoubleValue.from(1.0);

    @Override
    public Kind<DoubleValue> kind() {
        return Kind.DOUBLE;
    }

    @Override
    public boolean isZero() {
        return value == 0.0;
    }

    @Override
    public boolean isAllOnes() {
        return SpecialBuiltin.doubleToLong(value) == -1L;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || ((other instanceof DoubleValue) && (Double.doubleToLongBits(((DoubleValue) other).value) == Double.doubleToLongBits(value)));
    }

    @Override
    protected int compareSameKind(DoubleValue other) {
        return value < other.value ? -1 : (value == other.value ? 0 : 1);
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    @Override
    public Double asBoxedJavaValue() {
        return Double.valueOf(value);
    }

    @Override
    public byte toByte() {
        return (byte) value;
    }

    @Override
    public boolean toBoolean() {
        return (value != 0.0) ? true : false;
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
    public float toFloat() {
        return (float) value;
    }

    @Override
    public long toLong() {
        return (long) value;
    }

    @Override
    public double asDouble() {
        return value;
    }

    @Override
    public double unboxDouble() {
        return value;
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
        return WordWidth.BITS_64;
    }

    @Override
    public WordWidth unsignedEffectiveWidth() {
        return WordWidth.BITS_64;
    }

    @Override
    public byte[] toBytes(DataModel dataModel) {
        return dataModel.toBytes(value);
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeDouble(value);
    }

    @Override
    public CiConstant asCiConstant() {
        return CiConstant.forDouble(value);
    }

}
