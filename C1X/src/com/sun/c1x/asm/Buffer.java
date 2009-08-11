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

import com.sun.c1x.target.Architecture.*;
import com.sun.c1x.util.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public final class Buffer {

    private static final int InitialSize = 64;

    private byte[] data;
    private int position;

    private final BitOrdering bitOrdering;

    public Buffer(BitOrdering bitOrdering) {
        this.bitOrdering = bitOrdering;
        this.data = new byte[InitialSize];
    }

    /**
     * After calling this method, the buffer must no longer be used!
     * @return
     */
    public byte[] finished() {
        byte[] result = data;
        data = null;
        return result;
    }

    public int emitByte(int b) {
        int oldPos = position;
        position = emitByte(b, position);
        return oldPos;
    }

    public int emitShort(int b) {
        int oldPos = position;
        position = emitShort(b, position);
        return oldPos;
    }

    public int emitInt(int b) {
        int oldPos = position;
        position = emitInt(b, position);
        return oldPos;
    }

    public int emitLong(long b) {
        int oldPos = position;
        position = emitLong(b, position);
        return oldPos;
    }

    public int emitLongLong(long high, long low) {
        int oldPos = position;
        position = emitLongLong(high, low, position);
        return oldPos;
    }

    private boolean isByte(int b) {
        return b == (b & 0xFF);
    }

    private boolean isShort(int s) {
        return s == (s & 0xFFFF);
    }

    private void ensureSize(int length) {
        if (length >= data.length) {
            data = Arrays.copyOf(data, data.length * 2);
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
        if (bitOrdering == BitOrdering.BigEndian) {
            data[pos++] = (byte) ((b >> 8) & 0xFF);
            data[pos++] = (byte) (b & 0xFF);

        } else {
            assert bitOrdering == BitOrdering.LittleEndian;
            data[pos++] = (byte) (b & 0xFF);
            data[pos++] = (byte) ((b >> 8) & 0xFF);
        }
        return pos;
    }

    public int emitInt(int b, int pos) {
        assert data != null : "must not use buffer after calling finished!";
        ensureSize(pos + 4);
        if (bitOrdering == BitOrdering.BigEndian) {
            data[pos++] = (byte) ((b >> 24) & 0xFF);
            data[pos++] = (byte) ((b >> 16) & 0xFF);
            data[pos++] = (byte) ((b >> 8) & 0xFF);
            data[pos++] = (byte) (b & 0xFF);
        } else {
            assert bitOrdering == BitOrdering.LittleEndian;
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

        if (bitOrdering == BitOrdering.BigEndian) {
            data[pos++] = (byte) ((b >> 56) & 0xFF);
            data[pos++] = (byte) ((b >> 48) & 0xFF);
            data[pos++] = (byte) ((b >> 40) & 0xFF);
            data[pos++] = (byte) ((b >> 32) & 0xFF);
            data[pos++] = (byte) ((b >> 24) & 0xFF);
            data[pos++] = (byte) ((b >> 16) & 0xFF);
            data[pos++] = (byte) ((b >> 8) & 0xFF);
            data[pos++] = (byte) (b & 0xFF);
        } else {
            assert bitOrdering == BitOrdering.LittleEndian;
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

    public int emitLongLong(long high, long low, int pos) {
        assert data != null : "must not use buffer after calling finished!";
        if (bitOrdering == BitOrdering.BigEndian) {
            emitLong(high);
            emitLong(low);
        } else {
            emitLong(low);
            emitLong(high);
        }
        return pos;
    }

    public int position() {
        return position;
    }

    public int emitFloat(float f) {
        return emitInt(Float.floatToIntBits(f));
    }

    public int emitDouble(double d) {
        return emitLong(Double.doubleToLongBits(d));
    }

    public int getByte(int i) {
        return Bytes.beU1(data, i);
    }

    public byte[] getData(int start, int end) {
        return Arrays.copyOfRange(data, start, end);
    }

    public void align(int align) {
        position = Util.roundTo(position + 1, align) - 1;
    }
}
