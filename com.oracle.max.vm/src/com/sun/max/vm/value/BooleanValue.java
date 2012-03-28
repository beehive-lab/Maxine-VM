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
public final class BooleanValue extends PrimitiveValue<BooleanValue> {

    private final boolean value;

    public static BooleanValue from(boolean value) {
        return value ? TRUE : FALSE;
    }

    public static BooleanValue[] arrayFrom(boolean... values) {
        final BooleanValue[] result = new BooleanValue[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = from(values[i]);
        }
        return result;
    }

    private BooleanValue(boolean value) {
        this.value = value;
    }

    @Override
    public Kind<BooleanValue> kind() {
        return Kind.BOOLEAN;
    }

    @Override
    public boolean isZero() {
        return value == false;
    }

    @Override
    public boolean isAllOnes() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            // common case of reference equality
            return true;
        }
        if (!(other instanceof BooleanValue)) {
            return false;
        }
        final BooleanValue booleanValue = (BooleanValue) other;
        return value == booleanValue.asBoolean();
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }

    @Override
    public Boolean asBoxedJavaValue() {
        return Boolean.valueOf(value);
    }

    @Override
    public byte toByte() {
        return value ? (byte) 1 : (byte) 0;
    }

    @Override
    public byte unsignedToByte() {
        return toByte();
    }

    @Override
    public boolean asBoolean() {
        return value;
    }

    @Override
    public boolean unboxBoolean() {
        return value;
    }

    @Override
    public int unboxInt() {
        return toInt();
    }

    @Override
    public boolean toBoolean() {
        return value;
    }

    @Override
    public short toShort() {
        return value ? (short) 1 : (short) 0;
    }

    @Override
    public short unsignedToShort() {
        return toShort();
    }

    @Override
    public char toChar() {
        return value ? '\1' : '\0';
    }

    @Override
    public int toInt() {
        return value ? 1 : 0;
    }

    @Override
    public int unsignedToInt() {
        return toInt();
    }

    @Override
    public float toFloat() {
        return value ? (float) 1.0 : (float) 0.0;
    }

    @Override
    public long toLong() {
        return value ? 1L : 0L;
    }

    @Override
    public double toDouble() {
        return value ? 1.0 : 0.0;
    }

    @Override
    public Word toWord() {
        return value ? Address.fromInt(1) : Word.zero();
    }

    public static final BooleanValue FALSE = new BooleanValue(false);
    public static final BooleanValue TRUE = new BooleanValue(true);

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
        stream.writeBoolean(value);
    }

    @Override
    public CiConstant asCiConstant() {
        return CiConstant.forBoolean(value);
    }
}
