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

    private final byte _value;

    private static final class Cache {
        private Cache() {
        }

        static final ByteValue[] _cache = new ByteValue[-(-128) + 127 + 1];

        static {
            for (int i = 0; i < _cache.length; i++) {
                _cache[i] = new ByteValue((byte) (i - 128));
            }
        }
    }

    public static ByteValue from(byte value) {
        final int offset = 128;
        return Cache._cache[value + offset];
    }

    public static ByteValue[] arrayFrom(byte... values) {
        final ByteValue[] result = new ByteValue[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = from(values[i]);
        }
        return result;
    }

    private ByteValue(byte value) {
        _value = value;
    }

    @Override
    public Kind<ByteValue> kind() {
        return Kind.BYTE;
    }

    @Override
    public boolean isZero() {
        return _value == (byte) 0;
    }

    @Override
    public boolean isAllOnes() {
        return _value == (byte) -1;
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
        return _value == byteValue.asByte();
    }

    @Override
    protected int unsignedCompareSameKind(ByteValue other) {
        return unsignedToInt() - other.unsignedToInt();
    }

    @Override
    public String toString() {
        return Byte.toString(_value);
    }

    @Override
    public Byte asBoxedJavaValue() {
        return new Byte(_value);
    }

    @Override
    public byte asByte() {
        return _value;
    }

    @Override
    public byte unboxByte() {
        return _value;
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
        return _value;
    }

    @Override
    public byte unsignedToByte() {
        return _value;
    }

    @Override
    public boolean toBoolean() {
        return (_value != (byte) 0) ? true : false;
    }

    @Override
    public short toShort() {
        return _value;
    }

    @Override
    public short unsignedToShort() {
        return (short) (_value & 0xff);
    }

    @Override
    public char toChar() {
        return (char) _value;
    }

    @Override
    public int toInt() {
        return _value;
    }

    @Override
    public int unsignedToInt() {
        return _value & 0xff;
    }

    @Override
    public float toFloat() {
        return _value;
    }

    @Override
    public long toLong() {
        return _value;
    }

    @Override
    public double toDouble() {
        return _value;
    }

    @Override
    public Word toWord() {
        return Offset.fromInt(_value);
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
        return dataModel.toBytes(_value);
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeByte(_value);
    }
}
