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
public final class ShortValue extends PrimitiveValue<ShortValue> {

    private final short _value;

    private static final class Cache {
        private Cache() {
        }

        static final ShortValue[] _cache = new ShortValue[-(-128) + 127 + 1];

        static {
            for (int i = 0; i < _cache.length; i++) {
                _cache[i] = new ShortValue((short) (i - 128));
            }
        }
    }

    public static ShortValue from(short value) {
        final int offset = 128;
        final int valueAsInt = value;
        if (valueAsInt >= -128 && valueAsInt <= 127) { // must cache
            return Cache._cache[valueAsInt + offset];
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
        _value = value;
    }

    @Override
    public Kind<ShortValue> kind() {
        return Kind.SHORT;
    }

    @Override
    public boolean isZero() {
        return _value == (short) 0;
    }

    @Override
    public boolean isAllOnes() {
        return _value == (short) -1;
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
        return _value == shortValue.asShort();
    }

    @Override
    public String toString() {
        return Short.toString(_value);
    }

    @Override
    public Short asBoxedJavaValue() {
        return new Short(_value);
    }

    @Override
    public byte toByte() {
        return (byte) _value;
    }

    @Override
    public byte unsignedToByte() {
        return (byte) (_value & 0xff);
    }

    @Override
    public boolean toBoolean() {
        return (_value != (short) 0) ? true : false;
    }

    @Override
    public short asShort() {
        return _value;
    }

    @Override
    public short unboxShort() {
        return _value;
    }

    @Override
    public int unboxInt() {
        return toInt();
    }

    @Override
    public short toShort() {
        return _value;
    }

    @Override
    public short unsignedToShort() {
        return _value;
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
        return _value & 0xffff;
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
        return WordWidth.signedEffective(_value);
    }

    @Override
    public WordWidth unsignedEffectiveWidth() {
        return WordWidth.unsignedEffective(_value & 0xffff);
    }

    @Override
    public byte[] toBytes(DataModel dataModel) {
        return dataModel.toBytes(_value);
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeShort(_value);
    }
}
