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
package com.sun.max.vm.prototype;

import java.nio.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;

/**
 *
 * @author Doug Simon
 */
public final class MappedByteBufferDataAccess extends DataAccessAdapter {

    private final MappedByteBuffer buffer;
    private final Address base;

    public MappedByteBufferDataAccess(MappedByteBuffer buffer, Address base, WordWidth wordWidth) {
        super(wordWidth, buffer.order());
        this.buffer = buffer;
        this.base = base;
    }

    public int read(Address src, ByteBuffer dst, int dstOffset, int length) throws DataIOError {
        final int toRead = Math.min(length, dst.limit() - dstOffset);
        final ByteBuffer srcView = (ByteBuffer) buffer.duplicate().position(src.toInt()).limit(toRead);
        dst.put(srcView);
        return toRead;
    }

    public int write(ByteBuffer src, int srcOffset, int length, Address dst) throws DataIOError {
        buffer.position(dst.toInt());
        final ByteBuffer srcView = (ByteBuffer) src.duplicate().position(srcOffset).limit(length);
        buffer.put(srcView);
        return length;
    }

    private int asOffset(Address address) {
        if (base.isZero()) {
            return address.toInt();
        }
        if (address.lessThan(base)) {
            throw new DataIOError(address);
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
