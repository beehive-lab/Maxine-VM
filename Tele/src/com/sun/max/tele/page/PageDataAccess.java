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

    private final TeleIO _teleIO;
    private final int _indexShift;
    private final int _offsetMask;
    private Endianness _endianness;

    public PageDataAccess(DataModel dataModel, TeleIO teleIO) {
        super(dataModel);
        _teleIO = teleIO;
        ProgramError.check(Integer.bitCount(_teleIO.pageSize()) == 1, "Page size is not a power of 2: " + _teleIO.pageSize());
        _indexShift = Integer.numberOfTrailingZeros(teleIO.pageSize());
        _offsetMask = teleIO.pageSize() - 1;
        _endianness = dataModel.endianness();
    }

    public int pageSize() {
        return _teleIO.pageSize();
    }

    private long getIndex(Address address) {
        return address.toLong() >>> _indexShift;
    }

    private int getOffset(Address address) {
        return address.toInt() & _offsetMask;
    }

    private final VariableMapping<Long, Page> _indexToPage = HashMapping.createVariableEqualityMapping();

    public synchronized void invalidateCache() {
        _indexToPage.clear();
    }

    private void invalidatePage(long index) {
        _indexToPage.remove(index);
    }

    private static final int _MAX_PAGE_INVALIDATIONS = 1024;

    public synchronized void invalidate(Address address, Size size) {
        long numberOfPages = getIndex(size);
        if (numberOfPages > _MAX_PAGE_INVALIDATIONS) {
            invalidateCache();
        }
        if (getOffset(address) + getOffset(size) > pageSize()) {
            numberOfPages++;
        }
        final long startIndex = getIndex(address);
        for (long index = startIndex; index <= startIndex + numberOfPages; index++) {
            invalidatePage(index);
        }
    }

    public void invalidate(Address address, int size) {
        invalidate(address, Size.fromInt(size));
    }

    private Page getPage(long index) {
        Page page = _indexToPage.get(index);
        if (page == null) {
            page = new Page(_teleIO, index);
            _indexToPage.put(index, page);
            if ((_indexToPage.length() % 1000) == 0) {
                Trace.line(TRACE_VALUE, tracePrefix() + "Memory cache: " + _indexToPage.length() + " pages");
            }
        }
        return page;
    }

    private Page getPage(Address address) {
        return getPage(getIndex(address));
    }

    public synchronized int read(Address address, byte[] buffer, int offset, int length) {
        final int toRead = Math.min(length, buffer.length - offset);
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
        return getPage(address).readByte(getOffset(address));
    }

    public synchronized short readShort(Address address) {
        final int a = readByte(address);
        final int b = readByte(address.plus(Bytes.SIZE));
        switch (_endianness) {
            case LITTLE:
                return (short) ((a & Bytes.MASK) | (b << Bytes.WIDTH));
            case BIG:
                return (short) ((a << Bytes.WIDTH) | (b & Bytes.MASK));
            default:
                ProgramError.unknownCase();
                return -1;
        }
    }

    public synchronized int readInt(Address address) {
        final int a = readShort(address);
        final int b = readShort(address.plus(Shorts.SIZE));
        switch (_endianness) {
            case LITTLE:
                return (a & Shorts.MASK) | (b << Shorts.WIDTH);
            case BIG:
                return (a << Shorts.WIDTH) | (b & Shorts.MASK);
            default:
                ProgramError.unknownCase();
                return -1;
        }
    }

    public synchronized int write(byte[] buffer, int offset, int length, Address address) {
        invalidate(address, buffer.length);
        DataIO.Static.checkRead(buffer, offset, length);
        return  _teleIO.write(buffer, offset, buffer.length, address);
    }

    public synchronized void writeByte(Address address, byte value) {
        invalidate(address, Bytes.SIZE);
        final byte[] buffer = _endianness.toBytes(value);
        _teleIO.write(buffer, 0, buffer.length, address);
    }

    public synchronized void writeShort(Address address, short value) {
        invalidate(address, Shorts.SIZE);
        final byte[] buffer = _endianness.toBytes(value);
        _teleIO.write(buffer, 0, buffer.length, address);
    }

    public synchronized void writeInt(Address address, int value) {
        invalidate(address, Ints.SIZE);
        final byte[] buffer = _endianness.toBytes(value);
        _teleIO.write(buffer, 0, buffer.length, address);
    }

    public synchronized void writeLong(Address address, long value) {
        invalidate(address, Longs.SIZE);
        final byte[] buffer = _endianness.toBytes(value);
        _teleIO.write(buffer, 0, buffer.length, address);
    }
}
