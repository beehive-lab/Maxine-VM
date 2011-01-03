/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.page;

import java.nio.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.hosted.*;

/**
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public class PageDataAccess extends DataAccessAdapter {

    private static final int TRACE_VALUE = 1;

    protected String  tracePrefix() {
        return "[PageDataAccess] ";
    }

    private final TeleIO teleIO;
    private final int indexShift;
    private final int offsetMask;
    private final ByteBuffer writeBuffer;

    public PageDataAccess(TeleIO teleProcess, DataModel dataModel) {
        super(dataModel.wordWidth, dataModel.endianness.asByteOrder());
        teleIO = teleProcess;
        TeleError.check(Ints.isPowerOfTwoOrZero(teleIO.pageSize()), "Page size is not a power of 2: " + teleIO.pageSize());
        indexShift = Integer.numberOfTrailingZeros(teleProcess.pageSize());
        offsetMask = teleProcess.pageSize() - 1;
        writeBuffer = ByteBuffer.wrap(new byte[Longs.SIZE]).order(byteOrder);
    }

    public int pageSize() {
        return teleIO.pageSize();
    }

    private long getIndex(Address address) {
        return address.toLong() >>> indexShift;
    }

    private int getOffset(Address address) {
        return address.toInt() & offsetMask;
    }

    private final HashMap<Long, Page> indexToPage = new HashMap<Long, Page>();

    private static void checkNullPointer(Address address) {
        if (address.isZero()) {
            throw new DataIOError(address, "Cannot access address ZERO");
        }
    }

    private void invalidatePage(long index) {
        final Page page = indexToPage.get(index);
        if (page != null) {
            page.invalidate();
        }
    }

    public synchronized void invalidate(Address address, Size size) {
        long numberOfPages = getIndex(size);
        if (getOffset(address) + getOffset(size) > pageSize()) {
            numberOfPages++;
        }
        final long startIndex = getIndex(address);
        for (long index = startIndex; index <= startIndex + numberOfPages; index++) {
            invalidatePage(index);
        }
    }

    public void invalidateForWrite(Address address, int size) {
        checkNullPointer(address);
        invalidate(address, Size.fromInt(size));
    }

    private Page getPage(long index) {
        Page page = indexToPage.get(index);
        if (page == null) {
            page = new Page(teleIO, index, byteOrder);
            indexToPage.put(index, page);
            if ((indexToPage.size() % 1000) == 0) {
                Trace.line(TRACE_VALUE, tracePrefix() + "Memory cache: " + indexToPage.size() + " pages");
            }
        }
        return page;
    }

    private Page getPage(Address address) {
        return getPage(getIndex(address));
    }

    public synchronized int read(Address address, ByteBuffer buffer, int offset, int length) {
        final int toRead = Math.min(length, buffer.limit() - offset);
        long pageIndex = getIndex(address);
        int pageOffset = getOffset(address);
        int i = 0;
        while (i < toRead) {
            i += getPage(pageIndex).readBytes(pageOffset, buffer, i + offset);
            pageIndex++;
            pageOffset = 0;
        }
        return toRead;
    }

    public synchronized byte readByte(Address address) {
        checkNullPointer(address);
        return getPage(address).readByte(getOffset(address));
    }

    public synchronized short readShort(Address address) {
        checkNullPointer(address);
        return getPage(address).readShort(getOffset(address));
    }

    public synchronized int readInt(Address address) {
        checkNullPointer(address);
        return getPage(address).readInt(getOffset(address));
    }

    public long readLong(Address address) {
        checkNullPointer(address);
        return getPage(address).readLong(getOffset(address));
    }

    public synchronized int write(ByteBuffer buffer, int offset, int length, Address address) {
        invalidateForWrite(address, buffer.limit());
        DataIO.Static.checkRead(buffer, offset, length);
        return teleIO.write(buffer, offset, buffer.limit(), address);
    }

    public synchronized void writeByte(Address address, byte value) {
        invalidateForWrite(address, Bytes.SIZE);
        writeBuffer.put(0, value);
        teleIO.write(writeBuffer, 0, Bytes.SIZE, address);
    }

    public synchronized void writeShort(Address address, short value) {
        invalidateForWrite(address, Shorts.SIZE);
        writeBuffer.putShort(0, value);
        teleIO.write(writeBuffer, 0, Shorts.SIZE, address);
    }

    public synchronized void writeInt(Address address, int value) {
        invalidateForWrite(address, Ints.SIZE);
        writeBuffer.putInt(0, value);
        teleIO.write(writeBuffer, 0, Ints.SIZE, address);
    }

    public synchronized void writeLong(Address address, long value) {
        invalidateForWrite(address, Longs.SIZE);
        writeBuffer.putLong(0, value);
        teleIO.write(writeBuffer, 0, Longs.SIZE, address);
    }
}
