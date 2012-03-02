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
package com.sun.max.vm.classfile;

import static com.sun.max.vm.classfile.ErrorContext.*;

import java.io.*;

import com.sun.max.util.*;

/**
 * Operations for sequentially scanning data items in a class file. Any IO exceptions that occur during scanning
 * are converted to {@link ClassFormatError}s.
 */
public class ClassfileStream {

    private final int length;
    private final DataInputStream stream;
    private int pos;

    public ClassfileStream(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public ClassfileStream(byte[] bytes, int offset, int length) {
        this.length = length;
        this.stream = new DataInputStream(new ByteArrayInputStream(bytes, offset, length));
    }

    public byte readByte() {
        try {
            final byte value = stream.readByte();
            pos = pos++;
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public short readShort() {
        try {
            final short value = stream.readShort();
            pos += 2;
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public char readChar() {
        try {
            final char value = stream.readChar();
            pos += 2;
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public int readInt() {
        try {
            final int value = stream.readInt();
            pos += 4;
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public float readFloat() {
        try {
            final float value = stream.readFloat();
            pos += 4;
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public long readLong() {
        try {
            final long value = stream.readLong();
            pos += 8;
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public double readDouble() {
        try {
            final double value = stream.readDouble();
            pos += 8;
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public int readUnsigned1() {
        try {
            final int value = stream.readUnsignedByte();
            pos++;
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public int readUnsigned2() {
        try {
            final int value = stream.readUnsignedShort();
            pos += 2;
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public int readSize4() {
        try {
            final int value = stream.readInt();
            pos += 4;
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public int readSigned1() {
        try {
            final byte value = stream.readByte();
            pos++;
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public int readSigned2() {
        try {
            final short value = stream.readShort();
            pos += 2;
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public int readSigned4() {
        try {
            final int value = stream.readInt();
            pos += 4;
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public byte[] readByteArray(int len) {
        try {
            final byte[] bytes = new byte[len];
            stream.readFully(bytes);
            pos += len;
            return bytes;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public String readUtf8String() {
        try {
            final int utflen = stream.readUnsignedShort();
            final String value = Utf8.readUtf8(stream, true, utflen);
            pos += 2 + utflen;
            return value;
        } catch (Utf8Exception e) {
            throw classFormatError("Invalid UTF-8 encoded string", e);
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public void skip(int nBytes) {
        try {
            pos += nBytes;
            stream.skipBytes(nBytes);
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public boolean isAtEndOfFile() {
        return pos == length;
    }

    public void checkEndOfFile() {
        if (!isAtEndOfFile()) {
            throw classFormatError("Extra bytes in class file");
        }
    }

    public int getPosition() {
        return pos;
    }

    public void close() {
        try {
            stream.close();
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public ClassFormatError ioError(IOException ioException) {
        throw classFormatError("IO error while reading class file (" + ioException + ")");
    }

    public ClassFormatError eofError() {
        throw classFormatError("Truncated class file");
    }
}
