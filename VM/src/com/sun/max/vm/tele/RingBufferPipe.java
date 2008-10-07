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
package com.sun.max.vm.tele;

import java.io.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;

/**
 * A pipe supported by a ring buffer in memory.
 * 
 * @author Bernd Mathiske
 */
public class RingBufferPipe {

    private final DataAccess _dataAccess;
    private final Address _data;
    private final Address _buffer;
    private final Address _end;

    public RingBufferPipe(DataAccess dataAccess, Address data, int size) {
        _dataAccess = dataAccess;
        _data = data;
        _buffer = _data.plus(2 * Word.size());
        _end = _data.plus(size);
    }

    private Address getReadAddress() {
        return _dataAccess.getWord(_data, 0, 0).asAddress();
    }

    private void setReadAddress(Address position) {
        _dataAccess.setWord(_data, 0, 0, position);
    }

    private Address getWriteAddress() {
        return _dataAccess.getWord(_data, 0, 1).asAddress();
    }

    private void setWriteAddress(Address position) {
        _dataAccess.setWord(_data, 0, 1, position);
    }

    public void reset() {
        setReadAddress(_buffer);
        setWriteAddress(_buffer);
    }

    private int read() throws IOException {
        Address r = getReadAddress();
        if (r.equals(getWriteAddress())) {
            return -1;
        }
        final int result = _dataAccess.readByte(r) & Bytes.MASK;
        r = r.plus(1);
        if (r.greaterEqual(_end)) {
            setReadAddress(_buffer);
        } else {
            setReadAddress(r);
        }
        return result;
    }

    private void write(int value) throws IOException {
        final Address current = getWriteAddress();
        Address next = current.plus(1);
        if (next.greaterEqual(_end)) {
            next = _buffer;
        }
        if (next.equals(getReadAddress())) {
            throw new IOException();
        }
        _dataAccess.writeByte(current, (byte) (value & 0xff));
        setWriteAddress(next);
    }

    public InputStream createInputStream() {
        return new InputStream() {
            @Override
            public int available() throws IOException {
                final Address r = getReadAddress();
                final Address w = getWriteAddress();
                if (w.greaterEqual(r)) {
                    return w.minus(r).toInt();
                }
                return _end.minus(r).plus(w.minus(_buffer)).toInt();
            }

            @Override
            public int read() throws IOException {
                return RingBufferPipe.this.read();
            }
        };
    }

    public OutputStream createOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int ch) throws IOException {
                RingBufferPipe.this.write(ch);
            }
        };
    }
}
