/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.asm;

import java.util.*;

import com.sun.c1x.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.CiArchitecture.ByteOrder;

/**
 *
 * @author Thomas Wuerthinger
 */
public final class Buffer {

    private byte[] data;
    private int position;
    private int mark = -1;

    private final ByteOrder byteOrder;

    public Buffer(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
        this.data = new byte[C1XOptions.InitialCodeBufferSize];
    }

    /**
     * Closes this buffer. No extra data can be written to this buffer after this call.
     *
     * @param trimmedCopy if {@code true}, then a copy of the underlying byte array up to (but not including)
     *            {@code position()} is returned
     * @return the data in this buffer or a trimmed copy if {@code trimmedCopy} is {@code true}
     */
    public byte[] close(boolean trimmedCopy) {
        byte[] result = trimmedCopy ? Arrays.copyOf(data, position()) : data;
        data = null;
        return result;
    }

    public int emitByte(int b) {
        int oldPos = position;
        position = emitByte(b, oldPos);
        return oldPos;
    }

    public int emitShort(int b) {
        int oldPos = position;
        position = emitShort(b, oldPos);
        return oldPos;
    }

    public int emitInt(int b) {
        int oldPos = position;
        position = emitInt(b, oldPos);
        return oldPos;
    }

    public int emitLong(long b) {
        int oldPos = position;
        position = emitLong(b, oldPos);
        return oldPos;
    }

    private boolean isByte(int b) {
        return b == (b & 0xFF);
    }

    private boolean isShort(int s) {
        return s == (s & 0xFFFF);
    }

    /**
     * Places a bookmark at the {@linkplain #position() current position}.
     *
     * @return the previously placed bookmark or {@code -1} if there was no bookmark
     */
    public int mark() {
        int mark = this.mark;
        this.mark = position;
        return mark;
    }

    private void ensureSize(int length) {
        if (length >= data.length) {
            data = Arrays.copyOf(data, data.length * 4);
            C1XMetrics.CodeBufferCopies++;
        }
    }

    public int emitByte(int b, int pos) {
        assert data != null : "must not use buffer after calling finished!";
        assert isByte(b);
        ensureSize(pos + 1);
        data[pos++] = (byte) b;
        return pos;
    }

    public int emitShort(int b, int pos) {
        assert data != null : "must not use buffer after calling finished!";
        assert isShort(b);
        ensureSize(pos + 2);
        if (byteOrder == ByteOrder.BigEndian) {
            data[pos++] = (byte) ((b >> 8) & 0xFF);
            data[pos++] = (byte) (b & 0xFF);

        } else {
            assert byteOrder == ByteOrder.LittleEndian;
            data[pos++] = (byte) (b & 0xFF);
            data[pos++] = (byte) ((b >> 8) & 0xFF);
        }
        return pos;
    }

    public int emitInt(int b, int pos) {
        assert data != null : "must not use buffer after calling finished!";
        ensureSize(pos + 4);
        if (byteOrder == ByteOrder.BigEndian) {
            data[pos++] = (byte) ((b >> 24) & 0xFF);
            data[pos++] = (byte) ((b >> 16) & 0xFF);
            data[pos++] = (byte) ((b >> 8) & 0xFF);
            data[pos++] = (byte) (b & 0xFF);
        } else {
            assert byteOrder == ByteOrder.LittleEndian;
            data[pos++] = (byte) (b & 0xFF);
            data[pos++] = (byte) ((b >> 8) & 0xFF);
            data[pos++] = (byte) ((b >> 16) & 0xFF);
            data[pos++] = (byte) ((b >> 24) & 0xFF);
        }
        return pos;
    }

    public int emitLong(long b, int pos) {
        assert data != null : "must not use buffer after calling finished!";
        ensureSize(pos + 8);

        if (byteOrder == ByteOrder.BigEndian) {
            data[pos++] = (byte) ((b >> 56) & 0xFF);
            data[pos++] = (byte) ((b >> 48) & 0xFF);
            data[pos++] = (byte) ((b >> 40) & 0xFF);
            data[pos++] = (byte) ((b >> 32) & 0xFF);
            data[pos++] = (byte) ((b >> 24) & 0xFF);
            data[pos++] = (byte) ((b >> 16) & 0xFF);
            data[pos++] = (byte) ((b >> 8) & 0xFF);
            data[pos++] = (byte) (b & 0xFF);
        } else {
            assert byteOrder == ByteOrder.LittleEndian;
            data[pos++] = (byte) (b & 0xFF);
            data[pos++] = (byte) ((b >> 8) & 0xFF);
            data[pos++] = (byte) ((b >> 16) & 0xFF);
            data[pos++] = (byte) ((b >> 24) & 0xFF);
            data[pos++] = (byte) ((b >> 32) & 0xFF);
            data[pos++] = (byte) ((b >> 40) & 0xFF);
            data[pos++] = (byte) ((b >> 48) & 0xFF);
            data[pos++] = (byte) ((b >> 56) & 0xFF);
        }
        return pos;
    }

    public int position() {
        return position;
    }

    public int getByte(int i) {
        return Bytes.beU1(data, i);
    }

    public byte[] getData(int start, int end) {
        return Arrays.copyOfRange(data, start, end);
    }
}
