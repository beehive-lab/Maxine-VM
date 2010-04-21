/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.value;

import java.io.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class IntValue extends PrimitiveValue<IntValue> {

    private final int value;

    private static final class Cache {
        private Cache() {
        }

        static final IntValue[] cache = new IntValue[-(-128) + 127 + 1];

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new IntValue(i - 128);
            }
        }
    }

    public static IntValue from(int value) {
        final int offset = 128;
        if (value >= -128 && value <= 127) { // must cache
            return Cache.cache[value + offset];
        }
        return new IntValue(value);
    }

    public static IntValue[] arrayFrom(int... values) {
        final IntValue[] result = new IntValue[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = from(values[i]);
        }
        return result;
    }

    private IntValue(int value) {
        this.value = value;
    }

    public static final IntValue MINUS_ONE = from(-1);
    public static final IntValue ZERO = from(0);
    public static final IntValue ONE = from(1);
    public static final IntValue TWO = from(2);
    public static final IntValue THREE = from(3);
    public static final IntValue FOUR = from(4);
    public static final IntValue FIVE = from(5);

    @Override
    public Kind<IntValue> kind() {
        return Kind.INT;
    }

    @Override
    public boolean isZero() {
        return value == 0;
    }

    @Override
    public boolean isAllOnes() {
        return value == -1;
    }

    @Override
    protected int unsignedCompareSameKind(IntValue other) {
        if (value == other.value) {
            return 0;
        }
        final long unsignedThis = value & 0xFFFFFFFFL;
        final long unsignedOther = other.value & 0xFFFFFFFFL;
        if (unsignedThis < unsignedOther) {
            return -1;
        }
        return 1;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            // common case of reference equality
            return true;
        }
        if (!(other instanceof IntValue)) {
            return false;
        }
        final IntValue intValue = (IntValue) other;
        return value == intValue.asInt();
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public Object asBoxedJavaValue() {
        return Integer.valueOf(value);
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
        return (value != 0) ? true : false;
    }

    @Override
    public short toShort() {
        return (short) value;
    }

    @Override
    public short unsignedToShort() {
        return (short) (value & 0xffff);
    }

    @Override
    public char toChar() {
        return (char) value;
    }

    @Override
    public int asInt() {
        return value;
    }

    @Override
    public int unboxInt() {
        return toInt();
    }

    @Override
    public boolean unboxBoolean() {
        return toBoolean();
    }

    @Override
    public byte unboxByte() {
        assert toByte() == value;
        return toByte();
    }

    @Override
    public char unboxChar() {
        assert toChar() == value;
        return toChar();
    }

    @Override
    public short unboxShort() {
        assert toShort() == value;
        return toShort();
    }

    @Override
    public Word unboxWord() {
        if (32 == Word.width()) {
            return toWord();
        }
        return super.unboxWord();
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
        return Offset.fromInt(value);
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
        stream.writeInt(value);
    }
}
