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
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class LongValue extends PrimitiveValue<LongValue> {

    public final long value;

    private static final class Cache {
        private Cache() {
        }

        static final LongValue[] cache = new LongValue[-(-128) + 127 + 1];

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new LongValue(i - 128);
            }
        }
    }

    public static LongValue from(long value) {
        final int offset = 128;
        if (value >= -128 && value <= 127) { // will cache
            return Cache.cache[(int) value + offset];
        }
        return new LongValue(value);
    }

    public static LongValue[] arrayFrom(long... values) {
        final LongValue[] result = new LongValue[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = from(values[i]);
        }
        return result;
    }

    private LongValue(long value) {
        this.value = value;
    }

    public static final LongValue MINUS_ONE = LongValue.from(-1L);
    public static final LongValue ZERO = LongValue.from(0L);
    public static final LongValue ONE = LongValue.from(1L);

    @Override
    public Kind<LongValue> kind() {
        return Kind.LONG;
    }

    @Override
    public boolean isZero() {
        return value == 0L;
    }

    @Override
    public boolean isAllOnes() {
        return value == -1L;
    }

    @Override
    protected int unsignedCompareSameKind(LongValue other) {
        if (Word.width() == 64) {
            return Address.fromLong(value).compareTo(Address.fromLong(other.value));
        }
        throw FatalError.unimplemented();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            // common case of reference equality
            return true;
        }
        if (!(other instanceof LongValue)) {
            return false;
        }
        final LongValue longValue = (LongValue) other;
        return value == longValue.asLong();
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public Long asBoxedJavaValue() {
        return Long.valueOf(value);
    }

    @Override
    public byte toByte() {
        return (byte) value;
    }

    @Override
    public byte unsignedToByte() {
        return (byte) (value & 0xffL);
    }

    @Override
    public boolean toBoolean() {
        return (value != 0L) ? true : false;
    }

    @Override
    public short toShort() {
        return (short) value;
    }

    @Override
    public short unsignedToShort() {
        return (short) (value & 0xffffL);
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
    public int unsignedToInt() {
        return (int) (value & 0xffffffffL);
    }

    @Override
    public float toFloat() {
        return value;
    }

    @Override
    public long asLong() {
        return value;
    }

    @Override
    public long unboxLong() {
        return value;
    }

    @Override
    public Word unboxWord() {
        if (64 == Word.width()) {
            return toWord();
        }
        return super.unboxWord();
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
        return Offset.fromLong(value);
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
        stream.writeLong(value);
    }
}
