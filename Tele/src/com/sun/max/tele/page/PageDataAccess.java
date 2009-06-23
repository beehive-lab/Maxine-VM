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

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;

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
        super(dataModel.wordWidth());
        teleIO = teleProcess;
        ProgramError.check(Ints.isPowerOfTwoOrZero(teleIO.pageSize()), "Page size is not a power of 2: " + teleIO.pageSize());
        indexShift = Integer.numberOfTrailingZeros(teleProcess.pageSize());
        offsetMask = teleProcess.pageSize() - 1;
        writeBuffer = ByteBuffer.wrap(new byte[Longs.SIZE]).order(dataModel.endianness().asByteOrder());
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

    private final VariableMapping<Long, Page> indexToPage = HashMapping.createVariableEqualityMapping();

    private static void checkNullPointer(Address address) {
        if (address.isZero()) {
            throw new NullPointerException("Cannot access address ZERO");
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
            page = new Page(teleIO, index, writeBuffer.order());
            indexToPage.put(index, page);
            if ((indexToPage.length() % 1000) == 0) {
                Trace.line(TRACE_VALUE, tracePrefix() + "Memory cache: " + indexToPage.length() + " pages");
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
