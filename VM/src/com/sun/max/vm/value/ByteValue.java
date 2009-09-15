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
        return new Byte(value);
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
}
