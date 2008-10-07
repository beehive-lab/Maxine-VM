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
/*VCSID=8a5dfd1b-6a1e-43f0-8c17-748fcf9a39a1*/
package com.sun.max.vm.value;

import java.io.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class BooleanValue extends PrimitiveValue<BooleanValue> {

    private final boolean _value;

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
        _value = value;
    }

    @Override
    public Kind<BooleanValue> kind() {
        return Kind.BOOLEAN;
    }

    @Override
    public boolean isZero() {
        return _value == false;
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
        return _value == booleanValue.asBoolean();
    }

    @Override
    public String toString() {
        return Boolean.toString(_value);
    }

    @Override
    public Boolean asBoxedJavaValue() {
        return new Boolean(_value);
    }

    @Override
    public byte toByte() {
        return _value ? (byte) 1 : (byte) 0;
    }

    @Override
    public byte unsignedToByte() {
        return toByte();
    }

    @Override
    public boolean asBoolean() {
        return _value;
    }

    @Override
    public boolean unboxBoolean() {
        return _value;
    }

    @Override
    public int unboxInt() {
        return toInt();
    }

    @Override
    public boolean toBoolean() {
        return _value;
    }

    @Override
    public short toShort() {
        return _value ? (short) 1 : (short) 0;
    }

    @Override
    public short unsignedToShort() {
        return toShort();
    }

    @Override
    public char toChar() {
        return _value ? '\1' : '\0';
    }

    @Override
    public int toInt() {
        return _value ? 1 : 0;
    }

    @Override
    public int unsignedToInt() {
        return toInt();
    }

    @Override
    public float toFloat() {
        return _value ? (float) 1.0 : (float) 0.0;
    }

    @Override
    public long toLong() {
        return _value ? 1L : 0L;
    }

    @Override
    public double toDouble() {
        return _value ? 1.0 : 0.0;
    }

    @Override
    public Word toWord() {
        return _value ? Address.fromInt(1) : Word.zero();
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
        return dataModel.toBytes(_value);
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeBoolean(_value);
    }
}
