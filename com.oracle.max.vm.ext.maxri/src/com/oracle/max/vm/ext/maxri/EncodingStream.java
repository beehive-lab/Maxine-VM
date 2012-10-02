/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.maxri;

import java.io.*;
import java.util.*;

/**
 * Combination of {@link ByteArrayOutputStream} and {@link DataOutputStream} that supports
 * direct access to the underlying buffer and write position.
 */
final class EncodingStream {

    byte[] buf;
    int pos;

    public EncodingStream(int capacity) {
        buf = new byte[capacity];
    }

    private void ensureCapacity(int capacity) {
        if (capacity > buf.length) {
            buf = Arrays.copyOf(buf, Math.max(buf.length << 1, capacity));
        }
    }

    /**
     * Updates the write position of this stream. The stream can only be repositioned between {@code [0 .. this.buf.length]}.
     *
     * @param zeroAhead specifies if all the data at indexes greater or equal to {@code pos} should be zeroed
     */
    public void seek(int pos, boolean zeroAhead) {
        assert pos >= 0 && pos <= buf.length;
        this.pos = pos;
        if (zeroAhead) {
            Arrays.fill(buf, pos, buf.length, (byte) 0);
        }
    }

    /**
     * Skips the write position of this stream forward by {@code amount} bytes.
     */
    void skip(int amount) {
        ensureCapacity(pos + amount);
        pos += amount;
    }

    public void write(int b) {
        ensureCapacity(pos + 1);
        buf[pos++] = (byte) b;
    }

    public void writeByte(int v) {
        write(v);
    }

    public void writeBoolean(boolean v) {
        write(v ? 1 : 0);
    }

    public void writeShort(int v) {
        ensureCapacity(pos + 2);
        assert buf[pos] == 0;
        assert buf[pos + 1] == 0;
        buf[pos++] = (byte) (v >>> 8);
        buf[pos++] = (byte) (v >>> 0);
    }

    public void writeChar(int v) {
        writeShort(v);
    }

    public void writeInt(int v) {
        ensureCapacity(pos + 4);
        assert buf[pos] == 0;
        assert buf[pos + 1] == 0;
        assert buf[pos + 2] == 0;
        assert buf[pos + 3] == 0;
        buf[pos++] = (byte) (v >>> 24);
        buf[pos++] = (byte) (v >>> 16);
        buf[pos++] = (byte) (v >>>  8);
        buf[pos++] = (byte) (v >>>  0);
    }

    public void writeFloat(float v) {
        writeInt(Float.floatToRawIntBits(v));
    }

    public void writeLong(long v) {
        ensureCapacity(pos + 8);
        assert buf[pos] == 0;
        assert buf[pos + 1] == 0;
        assert buf[pos + 2] == 0;
        assert buf[pos + 3] == 0;
        assert buf[pos + 4] == 0;
        assert buf[pos + 5] == 0;
        assert buf[pos + 6] == 0;
        assert buf[pos + 7] == 0;
        buf[pos++] = (byte) (v >>> 56);
        buf[pos++] = (byte) (v >>> 48);
        buf[pos++] = (byte) (v >>> 40);
        buf[pos++] = (byte) (v >>> 32);
        buf[pos++] = (byte) (v >>> 24);
        buf[pos++] = (byte) (v >>> 16);
        buf[pos++] = (byte) (v >>>  8);
        buf[pos++] = (byte) (v >>>  0);
    }

    public void writeDouble(double v) {
        writeLong(Double.doubleToRawLongBits(v));
    }

    /**
     * Encodes an unsigned integer value to this stream.
     * The number of bytes written to the stream is given below:
     * <pre>
     *     Value range               Bytes used for encoding
     *     0       .. 127                 1
     *     128     .. 16383               2
     *     16384   .. 2097151             3
     *     2097152 .. 268435455           4
     * </pre>
     *
     * @param value  the value to encode (must be between 0 and 0x0FFFFFFF)
     */
    void encodeUInt(int value) {
        if (value < 128) {
            assert value >= 0;
            /* 0xxxxxxx */
            write(value);
        } else if (value < 16384) {
            /* 1xxxxxxx 0xxxxxxx */
            ensureCapacity(pos + 2);
            assert buf[pos] == 0;
            assert buf[pos + 1] == 0;
            buf[pos++] = (byte) (((value >> 0) & 0x7F) | 0x80);
            buf[pos++] = (byte) (value >> 7);
        } else if (value < 2097152) {
            /* 1xxxxxxx 1xxxxxxx 0xxxxxxx */
            ensureCapacity(pos + 3);
            assert buf[pos] == 0;
            assert buf[pos + 1] == 0;
            assert buf[pos + 2] == 0;
            buf[pos++] = (byte) (((value >> 0) & 0x7F) | 0x80);
            buf[pos++] = (byte) (((value >> 7) & 0x7F) | 0x80);
            buf[pos++] = (byte) (value >> 14);
        } else {
            assert value < 0x0FFFFFFF;
            /* 1xxxxxxx 1xxxxxxx 1xxxxxxx 0xxxxxxx */
            ensureCapacity(pos + 4);
            assert buf[pos] == 0;
            assert buf[pos + 1] == 0;
            assert buf[pos + 2] == 0;
            assert buf[pos + 3] == 0;
            buf[pos++] = (byte) (((value >> 0) & 0x7F) | 0x80);
            buf[pos++] = (byte) (((value >> 7) & 0x7F) | 0x80);
            buf[pos++] = (byte) (((value >> 14) & 0x7F) | 0x80);
            buf[pos++] = (byte) (value >> 21);
        }
    }

    byte toByteArray()[] {
        return Arrays.copyOf(buf, pos);
    }
}
