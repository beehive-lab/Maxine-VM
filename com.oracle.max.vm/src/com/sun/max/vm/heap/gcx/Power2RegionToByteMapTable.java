/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx;

import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 *
 * Class that implements a mapping of fixed size region in a contiguous range of virtual memory to entry of a byte  table.
 * Regions have a size that is a power of two and are aligned on that power of 2.
 */
public class Power2RegionToByteMapTable {
    final int log2RangeSize;
    private Address coveredAreaStart;
    private Address coveredAreaEnd;

    /**
     * Table containing a single byte of information per region.
     */
    byte [] table;
    /**
     * Address of the first element of the table.
     */
    Pointer tableAddress;
    /**
     * Address of the first element of the table biased by the covered area start.
     */
    Pointer biasedTableAddress;

    Power2RegionToByteMapTable(int log2RangeSize) {
        assert log2RangeSize < 32 : "size of contiguous range too large";
        this.log2RangeSize = log2RangeSize;
    }

    /**
     * The length of the underlying byte for covering a contiguous range of virtual memory of the specified size.
     * @param coveredAreaSize the size of the contiguous range of  virtual memory to cover
     * @return the length of the byte table
     */
    final int tableLength(Size coveredAreaSize) {
        return coveredAreaSize.unsignedShiftedRight(log2RangeSize).toInt();
    }

    void initialize(Address coveredAreaStart, Size coveredAreaSize) {
        initialize(coveredAreaStart, coveredAreaSize, new byte[tableLength(coveredAreaSize)]);
    }

    void initialize(Address coveredAreaStart, Size coveredAreaSize, byte [] table) {
        this.coveredAreaStart = coveredAreaStart;
        this.coveredAreaEnd = coveredAreaStart.plus(coveredAreaSize);
        assert coveredAreaStart.isAligned(1 << log2RangeSize) : "start of covered area must be aligned to specified power of 2";
        assert coveredAreaEnd.isAligned(1 << log2RangeSize) : "end of covered area must be aligned to specified power of 2";
        this.table = table;
        tableAddress = Reference.fromJava(table).toOrigin().plus(Layout.byteArrayLayout().getArraySize(Kind.BYTE, 0));
        biasedTableAddress = tableAddress.minus(coveredAreaStart.unsignedShiftedRight(log2RangeSize));
        assert tableEntryAddress(coveredAreaStart).equals(tableAddress);
    }

    /**
     * Test whether the address is covered by this table.
     * @param address an address in virtual memory
     * @return true if the address in within the contiguous range of virtual memory covered by this table.
     */
    final public boolean isCovered(Address address) {
        return address.greaterEqual(coveredAreaStart) && address.lessThan(coveredAreaEnd);
    }

    final int tableEntryIndex(Address coveredAddress) {
        return coveredAddress.minus(coveredAreaStart).unsignedShiftedRight(log2RangeSize).toInt();
    }

    boolean atBoundary(Address address) {
        return address.isAligned(1 << log2RangeSize);
    }
    /**
     * Start of the region covered by the entry of the table at the specified index.
     * @param index an valid index to an element of the table
     * @return address to the first byte of the region
     */
    Address rangeStart(int index) {
        return coveredAreaStart.plus(Address.fromLong(1L).shiftedLeft(log2RangeSize));
    }

    private Pointer tableEntryAddress(Address coveredAddress) {
        return biasedTableAddress.plus(coveredAddress.unsignedShiftedRight(log2RangeSize));
    }

    final void set(Address address, byte value) {
        tableEntryAddress(address).setByte(value);
    }

    /**
     * Find the first entry set to non-zero in the specified range of entries in the table.
     * @param start
     * @param end
    * @return
     */
    final int firstNonZero(int start, int end) {
        // This may be optimized with special support from the compiler to exploit cpu-specific instruction for string ops (e.g.).
        // We may also get rid of the limit test by making the end of the range looking like a marked card.
        // e.g.:   tmp = limit.getByte(); limit.setByte(1);  loop; limit.setByte(tmp); This could be factor over multiple call of firstNonZero...
        final Pointer first = tableAddress.plus(start);
        final Pointer limit = tableAddress.plus(end);
        Pointer cursor = first;
        while (cursor.getByte() != 0) {
            cursor = cursor.plus(1);
            if (cursor.greaterEqual(limit)) {
                return -1;
            }
        }
        return cursor.minus(first).toInt();
    }

    final byte get(Address address) {
        return tableEntryAddress(address).getByte();
    }

    final byte unsafeGet(int regionNum) {
        return tableAddress.getByte(regionNum);
    }

    final void unsafeSet(int regionNum, byte value) {
        tableAddress.setByte(regionNum, value);
    }

    final public byte get(int regionNum) {
        return table[regionNum];
    }

    final  public byte set(int regionNum, byte value) {
        return table[regionNum] = value;
    }
}
