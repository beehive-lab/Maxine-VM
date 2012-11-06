/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;

/**
 * Data access to a mapped byte buffer, e.g., the boot image code and data.
 */
public final class MappedByteBufferDataAccess extends DataAccessAdapter {

    private final MappedByteBuffer buffer;
    /**
     * The virtual address that the start of this buffer corresponds to.
     */
    private final Address base;
    /**
     * The virtual address that the end of the buffer corresponds to.
     */
    private final Address limit;

    public MappedByteBufferDataAccess(MappedByteBuffer buffer, Address base, WordWidth wordWidth) {
        super(wordWidth, buffer.order());
        this.buffer = buffer;
        this.base = base;
        this.limit = base.plus(buffer.limit() - 1); // assumes pos() == 0;
    }

    public int read(Address src, ByteBuffer dst, int dstOffset, int length) throws DataIOError {
        final int toRead = Math.min(length, dst.limit() - dstOffset);
        int srcViewPos = asOffset(src);
        int srcViewLimit = srcViewPos + toRead;
        final ByteBuffer srcView = (ByteBuffer) buffer.duplicate().position(srcViewPos).limit(srcViewLimit);
        dst.put(srcView);
        return toRead;
    }

    public int write(ByteBuffer src, int srcOffset, int length, Address dst) throws DataIOError {
        buffer.position(dst.toInt());
        final ByteBuffer srcView = (ByteBuffer) src.duplicate().position(srcOffset).limit(length);
        buffer.put(srcView);
        return length;
    }

    /**
     * Returns the offset into the buffer that corresponds to {@code address}, checking validity.
     * @param address
     */
    private int asOffset(Address address) {
        if (address.lessThan(base) || address.greaterThan(limit)) {
            throw new DataIOError(address);
        }
        if (base.isZero()) {
            return address.toInt();
        }
        return address.minus(base).toInt();
    }

    public byte readByte(Address address) {
        return buffer.get(asOffset(address));
    }

    public int readInt(Address address) {
        return buffer.getInt(asOffset(address));
    }

    public long readLong(Address address) {
        return buffer.getLong(asOffset(address));
    }

    public short readShort(Address address) {
        return buffer.getShort(asOffset(address));
    }

    public void writeByte(Address address, byte value) {
        buffer.put(asOffset(address), value);
    }

    public void writeInt(Address address, int value) {
        buffer.putInt(asOffset(address), value);
    }

    public void writeLong(Address address, long value) {
        buffer.putLong(asOffset(address), value);
    }

    public void writeShort(Address address, short value) {
        buffer.putShort(asOffset(address), value);
    }
}
