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

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.value.*;

public class DataModel {

    private final WordWidth _wordWidth;
    private final Endianness _endianness;
    private final Alignment _alignment;

    public DataModel(WordWidth wordWidth, Endianness endianness, Alignment alignment) {
        _wordWidth = wordWidth;
        _endianness = endianness;
        _alignment = alignment;
    }

    @INLINE
    public final WordWidth wordWidth() {
        return _wordWidth;
    }

    @INLINE
    public final Endianness endianness() {
        return _endianness;
    }

    @INLINE
    public final Alignment alignment() {
        return _alignment;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DataModel)) {
            return false;
        }
        final DataModel dataModel = (DataModel) other;
        return _wordWidth.equals(dataModel._wordWidth) && _endianness.equals(dataModel._endianness) && _alignment.equals(dataModel._alignment);
    }

    public byte[] toBytes(byte value) {
        return endianness().toBytes(value);
    }

    public byte[] toBytes(boolean value) {
        final byte[] result = new byte[1];
        result[0] = value ? (byte) 1 : (byte) 0;
        return result;
    }

    public byte[] toBytes(short value) {
        return endianness().toBytes(value);
    }

    public byte[] toBytes(char value) {
        final short shortValue = UnsafeLoophole.charToShort(value);
        return endianness().toBytes(shortValue);
    }

    public byte[] toBytes(int value) {
        return endianness().toBytes(value);
    }

    public byte[] toBytes(float value) {
        final int intValue = UnsafeLoophole.floatToInt(value);
        return endianness().toBytes(intValue);
    }

    public byte[] toBytes(long value) {
        return endianness().toBytes(value);
    }

    public byte[] toBytes(double value) {
        final long longValue = UnsafeLoophole.doubleToLong(value);
        return endianness().toBytes(longValue);
    }

    public byte[] toBytes(Word value) {
        switch (wordWidth()) {
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
        return _wordWidth + "-bit, " + _endianness + " endian, " + _alignment + "-byte aligned";
    }
}
