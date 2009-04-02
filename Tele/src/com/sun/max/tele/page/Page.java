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
package com.sun.max.tele.page;

import java.nio.*;

import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public class Page {

    private TeleIO _teleIO;
    private long _epoch = -1;
    private final long _index;

    private final ByteBuffer _buffer;

    private static final boolean _useDirectBuffers = true; //System.getProperty("max.tele.page.useDirectBuffers") != null;

    private static ByteBuffer _globalBuffer;;

    private static synchronized ByteBuffer allocate(TeleIO teleIO, ByteOrder byteOrder, long index) {
        final int pageSize = teleIO.pageSize();
        if (_globalBuffer == null) {
            _globalBuffer = ByteBuffer.allocate(1024 * 1024 * 100).order(byteOrder);
        }
        if (_useDirectBuffers) {
            if (_globalBuffer.remaining() >= pageSize) {
                final ByteBuffer buffer = _globalBuffer.slice().order(byteOrder);
                _globalBuffer.position(_globalBuffer.position() + pageSize);
                buffer.limit(pageSize);
                return buffer;
            }
        }
        return ByteBuffer.allocate(pageSize).order(byteOrder);
    }

    public Page(TeleIO teleIO, long index, ByteOrder byteOrder) {
        _teleIO = teleIO;
        _index = index;
        _buffer = allocate(teleIO, byteOrder, index);
    }

    public int size() {
        return _teleIO.pageSize();
    }

    public Address address() {
        return Address.fromLong(_index * size());
    }

    public void invalidate() {
        _epoch = -1;
    }

    private void refreshRead() throws DataIOError {
        if (_epoch < _teleIO.epoch()) {
            DataIO.Static.readFully(_teleIO, address(), _buffer);
            _epoch = _teleIO.epoch();
        }
    }

    public byte readByte(int offset) throws DataIOError {
        refreshRead();
        return _buffer.get(offset);
    }

    public short readShort(int offset) {
        refreshRead();
        return _buffer.getShort(offset);
    }

    public int readInt(int offset) {
        refreshRead();
        final int result = _buffer.getInt(offset);
        return result;
    }

    public long readLong(int offset) {
        refreshRead();
        final long result = _buffer.getLong(offset);
        return result;
    }

    /**
     * Transfers {@code n} bytes from this page to a given buffer where
     * {@code n == min(dst.limit() - dstOffset, size() - offset)}.
     *
     * @param offset the offset in this page from which to start reading
     * @param dst the buffer into which the bytes will be copied
     * @param dstOffset the offset in {@code dst} at which to start writing
     * @return the number of bytes copied
     */
    public int readBytes(int offset, ByteBuffer dst, int dstOffset) throws DataIOError {
        refreshRead();

        final int n = Math.min(dst.limit() - dstOffset, size() - offset);

        final ByteBuffer srcSlice = _buffer.duplicate();
        final ByteBuffer dstSlice = dst.duplicate();

        srcSlice.position(offset).limit(offset + n);
        dstSlice.position(dstOffset).limit(dstOffset + n);

        dstSlice.put(srcSlice);
        return n;
    }
}
