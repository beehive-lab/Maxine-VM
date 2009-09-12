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
        return UnsafeLoophole.asLong(value) == -1L;
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
        return new Double(value);
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
}
