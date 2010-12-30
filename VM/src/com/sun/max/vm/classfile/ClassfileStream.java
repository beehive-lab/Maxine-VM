/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.unsafe.*;
import com.sun.max.util.*;

/**
 * Operations for sequentially scanning data items in a class file. Any IO exceptions that occur during scanning
 * are converted to {@link ClassFormatError}s.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class ClassfileStream {

    private final int length;
    private final DataInputStream stream;
    private Address position = Address.zero();

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
            position = position.plus(1);
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
            position = position.plus(2);
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
            position = position.plus(2);
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
            position = position.plus(4);
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
            position = position.plus(4);
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
            position = position.plus(8);
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
            position = position.plus(8);
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
            position = position.plus(1);
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
            position = position.plus(2);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public Size readSize4() {
        try {
            final Size value = Size.fromUnsignedInt(stream.readInt());
            position = position.plus(4);
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
            position = position.plus(1);
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
            position = position.plus(2);
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
            position = position.plus(4);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public byte[] readByteArray(Size len) {
        try {
            final byte[] bytes = new byte[len.toInt()];
            stream.readFully(bytes);
            position = position.plus(len);
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
            position = position.plus(2 + utflen);
            return value;
        } catch (Utf8Exception e) {
            throw classFormatError("Invalid UTF-8 encoded string", e);
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public void skip(Size nBytes) {
        try {
            position = position.plus(nBytes);
            stream.skipBytes(nBytes.toInt());
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public boolean isAtEndOfFile() {
        return position.toLong() == length;
    }

    public void checkEndOfFile() {
        if (!isAtEndOfFile()) {
            throw classFormatError("Extra bytes in class file");
        }
    }

    public Address getPosition() {
        // Prevent sharing by reference of _position when not bootstrapped:
        return position.asAddress();
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
