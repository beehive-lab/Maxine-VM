/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import java.io.*;

/**
 * Variation on {@link ByteArrayInputStream} and {@link DataInputStream} that supports
 * direct access to the underlying buffer and read position.
 */
final class DecodingStream {

    byte[] buf;
    int pos;

    public DecodingStream(byte[] buf) {
        this.buf = buf;
    }

    public int read() {
        if (pos < buf.length) {
            return buf[pos++] & 0xff;
        }
        throw new InternalError("ran off end of decoding stream");
    }

    /**
     * Decodes an unsigned integer from a given stream.
     *
     * @return the decoded value
     * @see    EncodingStream#encodeUInt(int)
     */
    int decodeUInt() {
        int lo = read() & 0xFF;
        if (lo < 128) {
            /* 0xxxxxxx */
            return lo;
        }
        lo &= 0x7f;
        int mid = read() & 0xFF;
        if (mid < 128) {
            /* 1xxxxxxx 0xxxxxxx */
            return (mid << 7) + lo;
        }
        mid &= 0x7f;
        int hi = read() & 0xFF;
        if (hi < 128) {
            /* 1xxxxxxx 1xxxxxxx 0xxxxxxx */
            return (hi << 14) + (mid << 7) + lo;
        }
        hi &= 0x7f;
        int last = read() & 0xFF;
        if (last < 128) {
            /* 1xxxxxxx 1xxxxxxx 1xxxxxxx 0xxxxxxx */
            return (last << 21) + (hi << 14) + (mid << 7) + lo;
        }
        throw new InternalError();
    }

    public byte readByte() {
        return (byte) read();
    }

    public boolean readBoolean() {
        return read() != 0;
    }

    public char readChar() {
        int ch1 = read();
        int ch2 = read();
        return (char) ((ch1 << 8) + (ch2 << 0));
    }

    public short readShort() {
        int ch1 = read();
        int ch2 = read();
        return (short) ((ch1 << 8) + (ch2 << 0));
    }

    public int readInt() {
        int ch1 = read();
        int ch2 = read();
        int ch3 = read();
        int ch4 = read();
        return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0);
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public long readLong() {
        return ((long) read() << 56) +
                ((long) (read() & 255) << 48) +
                ((long) (read() & 255) << 40) +
                ((long) (read() & 255) << 32) +
                ((long) (read() & 255) << 24) +
                ((read() & 255) << 16) +
                ((read() & 255) <<  8) +
                ((read() & 255) <<  0);
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }
}
