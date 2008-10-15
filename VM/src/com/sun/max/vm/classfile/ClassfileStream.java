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
import com.sun.max.vm.*;

/**
 * Operations for sequentially scanning data items in a class file. Any IO exceptions that occur during scanning
 * are converted to {@link ClassFormatError}s. 
 * 
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class ClassfileStream {

    private final int _length;
    private final DataInputStream _stream;
    private Address _position = Address.zero();

    public ClassfileStream(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public ClassfileStream(byte[] bytes, int offset, int length) {
        _length = length;
        _stream = new DataInputStream(new ByteArrayInputStream(bytes, offset, length));
    }

    public byte readByte() {
        try {
            final byte value = _stream.readByte();
            _position = _position.plus(1);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public short readShort() {
        try {
            final short value = _stream.readShort();
            _position = _position.plus(2);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public char readChar() {
        try {
            final char value = _stream.readChar();
            _position = _position.plus(2);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public int readInt() {
        try {
            final int value = _stream.readInt();
            _position = _position.plus(4);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public float readFloat() {
        try {
            final float value = _stream.readFloat();
            _position = _position.plus(4);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public long readLong() {
        try {
            final long value = _stream.readLong();
            _position = _position.plus(8);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public double readDouble() {
        try {
            final double value = _stream.readDouble();
            _position = _position.plus(8);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public int readUnsigned1() {
        try {
            final int value = _stream.readUnsignedByte();
            _position = _position.plus(1);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public int readUnsigned2() {
        try {
            final int value = _stream.readUnsignedShort();
            _position = _position.plus(2);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public Size readSize4() {
        try {
            final Size value = Size.fromUnsignedInt(_stream.readInt());
            _position = _position.plus(4);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public int readSigned1() {
        try {
            final byte value = _stream.readByte();
            _position = _position.plus(1);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public int readSigned2() {
        try {
            final short value = _stream.readShort();
            _position = _position.plus(2);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public int readSigned4() {
        try {
            final int value = _stream.readInt();
            _position = _position.plus(4);
            return value;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public byte[] readByteArray(Size length) {
        try {
            final byte[] bytes = new byte[length.toInt()];
            _stream.readFully(bytes);
            _position = _position.plus(length);
            return bytes;
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public String readUtf8String() {
        try {
            final int utflen = _stream.readUnsignedShort();
            final String value = Utf8.readUtf8(_stream, true, utflen);
            _position = _position.plus(2 + utflen);
            return value;
        } catch (Utf8Exception e) {
            throw classFormatError("Invalid UTF-8 encoded string");
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public void skip(Size nBytes) {
        try {
            _position = _position.plus(nBytes);
            _stream.skipBytes(nBytes.toInt());
        } catch (EOFException eofException) {
            throw eofError();
        } catch (IOException ioException) {
            throw ioError(ioException);
        }
    }

    public boolean isAtEndOfFile() {
        return _position.toLong() == _length;
    }

    public void checkEndOfFile() {
        if (!isAtEndOfFile()) {
            throw classFormatError("Extra bytes in class file");
        }
    }

    public Address getPosition() {
        // Prevent sharing by reference of _position when not bootstrapped:
        assert !MaxineVM.isPrototyping() || _position.asAddress() != _position;
        return _position.asAddress();
    }

    public void close() {
        try {
            _stream.close();
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
