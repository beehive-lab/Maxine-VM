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
