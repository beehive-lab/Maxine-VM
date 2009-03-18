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
public final class CharValue extends PrimitiveValue<CharValue> {

    final char _value;

    private static final class Cache {
        private Cache() {
        }

        static final CharValue[] _cache = new CharValue[127 + 1];

        static {
            for (int i = 0; i < _cache.length; i++) {
                _cache[i] = new CharValue((char) i);
            }
        }
    }

    public static CharValue from(char value) {
        if (value <= 127) { // must cache
            return Cache._cache[value];
        }
        return new CharValue(value);
    }

    public static CharValue[] arrayFrom(char... values) {
        final CharValue[] result = new CharValue[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = from(values[i]);
        }
        return result;
    }

    private CharValue(char value) {
        super();
        _value = value;
    }

    @Override
    public Kind<CharValue> kind() {
        return Kind.CHAR;
    }

    @Override
    public boolean isZero() {
        return _value == (char) 0;
    }

    @Override
    public boolean isAllOnes() {
        return _value == (char) -1;
    }

    public static final CharValue ZERO = CharValue.from('\0');

    @Override
    protected int unsignedCompareSameKind(CharValue other) {
        // Char value comparisons are always unsigned
        return compareSameKind(other);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            // common case of reference equality
            return true;
        }
        if (!(other instanceof CharValue)) {
            return false;
        }
        final CharValue charValue = (CharValue) other;
        return _value == charValue.asChar();
    }

    @Override
    public String toString() {
        return Character.toString(_value);
    }

    @Override
    public Character asBoxedJavaValue() {
        return new Character(_value);
    }

    @Override
    public byte toByte() {
        return (byte) _value;
    }

    @Override
    public byte unsignedToByte() {
        return toByte();
    }

    @Override
    public boolean toBoolean() {
        return (_value != '\0') ? true : false;
    }

    @Override
    public short toShort() {
        return (short) _value;
    }

    @Override
    public short unsignedToShort() {
        return toShort();
    }

    @Override
    public char asChar() {
        return _value;
    }

    @Override
    public char unboxChar() {
        return _value;
    }

    @Override
    public int unboxInt() {
        return toInt();
    }

    @Override
    public char toChar() {
        return _value;
    }

    @Override
    public int toInt() {
        return _value;
    }

    @Override
    public int unsignedToInt() {
        return _value;
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
        return Address.fromInt(_value);
    }

    @Override
    public WordWidth signedEffectiveWidth() {
        return WordWidth.signedEffective((short) _value);
    }

    @Override
    public WordWidth unsignedEffectiveWidth() {
        return WordWidth.unsignedEffective(_value);
    }

    @Override
    public byte[] toBytes(DataModel dataModel) {
        return dataModel.toBytes(_value);
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeChar(_value);
    }
}
