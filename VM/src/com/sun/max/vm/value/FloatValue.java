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
/*VCSID=48cce481-4824-4b12-9bc2-65ab9ea8feb4*/
package com.sun.max.vm.value;

import java.io.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class FloatValue extends PrimitiveValue<FloatValue> {

    private final float _value;

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
        _value = value;
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
        return _value == (float) 0.0;
    }

    @Override
    public boolean isAllOnes() {
        return UnsafeLoophole.floatToInt(_value) == -1;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            // common case of reference equality
            return true;
        }
        if (!(other instanceof FloatValue)) {
            return false;
        }
        final FloatValue floatValue = (FloatValue) other;
        return _value == floatValue.asFloat();
    }

    @Override
    protected int compareSameKind(FloatValue other) {
        return _value < other._value ? -1 : (_value == other._value ? 0 : 1);
    }

    @Override
    public String toString() {
        return Float.toString(_value);
    }

    @Override
    public Float asBoxedJavaValue() {
        return new Float(_value);
    }

    @Override
    public byte toByte() {
        return (byte) _value;
    }

    @Override
    public boolean toBoolean() {
        return (_value != (float) 0.0) ? true : false;
    }

    @Override
    public short toShort() {
        return (short) _value;
    }

    @Override
    public char toChar() {
        return (char) _value;
    }

    @Override
    public int toInt() {
        return (int) _value;
    }

    @Override
    public float asFloat() {
        return _value;
    }

    @Override
    public float unboxFloat() {
        return _value;
    }

    @Override
    public float toFloat() {
        return _value;
    }

    @Override
    public long toLong() {
        return (long) _value;
    }

    @Override
    public double toDouble() {
        return _value;
    }

    @Override
    public Word toWord() {
        return Offset.fromLong((long) _value);
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
        return dataModel.toBytes(_value);
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeFloat(_value);
    }
}
