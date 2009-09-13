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
package com.sun.max.lang;

import java.io.*;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.value.*;

public class DataModel {

    public final WordWidth wordWidth;
    public final Endianness endianness;
    public final int cacheAlignment;

    public DataModel(WordWidth wordWidth, Endianness endianness, int cacheAlignment) {
        this.wordWidth = wordWidth;
        this.endianness = endianness;
        this.cacheAlignment = cacheAlignment;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DataModel)) {
            return false;
        }
        final DataModel dataModel = (DataModel) other;
        return wordWidth.equals(dataModel.wordWidth) && endianness.equals(dataModel.endianness) && cacheAlignment == dataModel.cacheAlignment;
    }

    public byte[] toBytes(byte value) {
        return endianness.toBytes(value);
    }

    public byte[] toBytes(boolean value) {
        final byte[] result = new byte[1];
        result[0] = value ? (byte) 1 : (byte) 0;
        return result;
    }

    public byte[] toBytes(short value) {
        return endianness.toBytes(value);
    }

    public byte[] toBytes(char value) {
        final short shortValue = UnsafeCast.asShort(value);
        return endianness.toBytes(shortValue);
    }

    public byte[] toBytes(int value) {
        return endianness.toBytes(value);
    }

    public byte[] toBytes(float value) {
        final int intValue = SpecialBuiltin.floatToInt(value);
        return endianness.toBytes(intValue);
    }

    public byte[] toBytes(long value) {
        return endianness.toBytes(value);
    }

    public byte[] toBytes(double value) {
        final long longValue = SpecialBuiltin.doubleToLong(value);
        return endianness.toBytes(longValue);
    }

    public byte[] toBytes(Word value) {
        switch (wordWidth) {
            case BITS_64:
                return toBytes(value.asOffset().toLong());
            case BITS_32:
                return toBytes((int) value.asOffset().toLong());
            case BITS_16:
                return toBytes((short) value.asOffset().toLong());
            case BITS_8:
                return toBytes((byte) value.asOffset().toLong());
        }
        ProgramError.unknownCase();
        return null;
    }

    public void write(OutputStream stream, Value value) throws IOException {
        stream.write(value.toBytes(this));
    }

    @Override
    public String toString() {
        return wordWidth + "-bit, " + endianness + " endian, " + cacheAlignment + "-byte aligned cache";
    }
}
