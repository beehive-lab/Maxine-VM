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
package com.sun.max.tele.data;

import java.io.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;

/**
 * A pipe supported by a ring buffer in memory.
 *
 * @author Bernd Mathiske
 */
public class RingBufferPipe {

    private final DataAccess dataAccess;
    private final Address data;
    private final Address buffer;
    private final Address end;

    public RingBufferPipe(DataAccess dataAccess, Address data, int size) {
        this.dataAccess = dataAccess;
        this.data = data;
        this.buffer = data.plus(2 * Word.size());
        this.end = data.plus(size);
    }

    private Address getReadAddress() {
        return dataAccess.getWord(data, 0, 0).asAddress();
    }

    private void setReadAddress(Address position) {
        dataAccess.setWord(data, 0, 0, position);
    }

    private Address getWriteAddress() {
        return dataAccess.getWord(data, 0, 1).asAddress();
    }

    private void setWriteAddress(Address position) {
        dataAccess.setWord(data, 0, 1, position);
    }

    public void reset() {
        setReadAddress(buffer);
        setWriteAddress(buffer);
    }

    private int read() throws IOException {
        Address r = getReadAddress();
        if (r.equals(getWriteAddress())) {
            return -1;
        }
        final int result = dataAccess.readByte(r) & Bytes.MASK;
        r = r.plus(1);
        if (r.greaterEqual(end)) {
            setReadAddress(buffer);
        } else {
            setReadAddress(r);
        }
        return result;
    }

    private void write(int value) throws IOException {
        final Address current = getWriteAddress();
        Address next = current.plus(1);
        if (next.greaterEqual(end)) {
            next = buffer;
        }
        if (next.equals(getReadAddress())) {
            throw new IOException();
        }
        dataAccess.writeByte(current, (byte) (value & 0xff));
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
                return end.minus(r).plus(w.minus(buffer)).toInt();
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
