/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.lang;

import java.io.*;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
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
        final int intValue = Float.floatToRawIntBits(value);
        return endianness.toBytes(intValue);
    }

    public byte[] toBytes(long value) {
        return endianness.toBytes(value);
    }

    public byte[] toBytes(double value) {
        final long longValue = Double.doubleToRawLongBits(value);
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
        throw ProgramError.unknownCase();
    }

    public void write(OutputStream stream, Value value) throws IOException {
        stream.write(value.toBytes(this));
    }

    @Override
    public String toString() {
        return wordWidth + "-bit, " + endianness + " endian, " + cacheAlignment + "-byte aligned cache";
    }
}
