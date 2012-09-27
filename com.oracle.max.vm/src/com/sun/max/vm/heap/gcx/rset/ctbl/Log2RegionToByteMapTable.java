/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx.rset.ctbl;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 *
 * Class that implements a mapping of fixed size region in a contiguous range of virtual memory to entry of a byte  table.
 * Regions have a size that is a power of two and are aligned on that power of 2.
 */
public class Log2RegionToByteMapTable {
    /**
     * Offset to the {@link #table} variable.
     * This is primarily used to bypass write-barrier when setting the variable.
     */
    @FOLD
    public static int tableOffset() {
        return ClassActor.fromJava(Log2RegionToByteMapTable.class).findLocalInstanceFieldActor("table").offset();
    }

    final int log2RangeSize;
    /**
     * Start of the contiguous range of virtual memory covered by this byte map table.
     * This address must be aligned to size of the region covered by a single byte.
     */
    @INSPECTED
    private Address coveredAreaStart = Pointer.zero();
    /**
     * End of the contiguous range of virtual memory covered by this byte map table.
     * This address must be aligned to size of the region covered by a single byte.
    */
    @INSPECTED
    private Address coveredAreaEnd = Pointer.zero();

    /**
     * Table containing a single byte of information per region.
     */
    private byte [] table;

    /**
     * Address of the first element of the table.
     */
    @INSPECTED
    Pointer tableAddress = Pointer.zero();
    /**
     * Address of the first element of the table biased by the covered area start.
     */
    Pointer biasedTableAddress = Pointer.zero();

    Log2RegionToByteMapTable(int log2RangeSize) {
        assert log2RangeSize < 32 : "size of contiguous range too large";
        this.log2RangeSize = log2RangeSize;
    }

    private Size tableSize(int tableLength) {
        return Layout.byteArrayLayout().getArraySize(Kind.BYTE, tableLength);
    }

    final protected Size tableHeaderSize() {
        return  tableSize(0);
    }

    /**
     * The length of the underlying byte for covering a contiguous range of virtual memory of the specified size.
     * @param coveredAreaSize the size of the contiguous range of  virtual memory to cover
     * @return the length of the byte table
     */
    final int tableLength(Size coveredAreaSize) {
        return coveredAreaSize.unsignedShiftedRight(log2RangeSize).toInt();
    }

    final Size tableSize(Size coveredAreaSize) {
        return tableSize(tableLength(coveredAreaSize));
    }


    void initialize(Address coveredAreaStart, Size coveredAreaSize, boolean noWriteBarrier) {
        initialize(coveredAreaStart, coveredAreaSize, new byte[tableLength(coveredAreaSize)], noWriteBarrier);
    }

    public Address coveredAreaStart() {
        return coveredAreaStart;
    }

    public Address coveredAreaEnd() {
        return coveredAreaEnd;
    }

    /**
     * Inspector support.
     * @return address of the card table address
     */
    @HOSTED_ONLY
    public Address tableAddress() {
        return tableAddress;
    }
    /**
     * Inspector support.
     * @return total size of the backing storage for the byte map.
     */
    @HOSTED_ONLY
    public long size() {
        return tableSize(coveredAreaEnd.minus(coveredAreaStart).asSize()).toLong();
    }

    void initialize(Address coveredAreaStart, Size coveredAreaSize, Address storageArea) {
        final byte [] table =  (byte[]) Cell.plantArray(storageArea.asPointer(), ClassRegistry.BYTE_ARRAY.dynamicHub(), tableLength(coveredAreaSize));
        initialize(coveredAreaStart, coveredAreaSize, table, true);
    }

    void initialize(Address coveredAreaStart, Size coveredAreaSize, byte [] table, boolean noWriteBarrier) {
        this.coveredAreaStart = coveredAreaStart;
        this.coveredAreaEnd = coveredAreaStart.plus(coveredAreaSize);
        assert coveredAreaStart.isAligned(1 << log2RangeSize) : "start of covered area must be aligned to specified power of 2";
        assert coveredAreaEnd.isAligned(1 << log2RangeSize) : "end of covered area must be aligned to specified power of 2";
        final Pointer tableOrigin = Reference.fromJava(table).toOrigin();
        if (noWriteBarrier) {
            Reference.fromJava(this).toOrigin().writeWord(tableOffset(), tableOrigin);
        } else {
            this.table = table;
        }
        tableAddress = tableOrigin.plus(tableHeaderSize());
        biasedTableAddress = tableAddress.minus(coveredAreaStart.unsignedShiftedRight(log2RangeSize));
        FatalError.check(this.table == table && byteAddressFor(coveredAreaStart).equals(tableAddress), "incorrect initialization of region table");
    }

    /**
     * Inspector support.
     *
     * @param coveredAreaStart
     * @param coveredAreaEnd
     * @param tableAddress
     */
    @HOSTED_ONLY
    public void initialize(Address coveredAreaStart, Address coveredAreaEnd, Address tableAddress) {
        this.coveredAreaStart = coveredAreaStart;
        this.coveredAreaEnd = coveredAreaEnd;
        assert coveredAreaStart.isAligned(1 << log2RangeSize) : "start of covered area must be aligned to specified power of 2";
        assert coveredAreaEnd.isAligned(1 << log2RangeSize) : "end of covered area must be aligned to specified power of 2";
        this.tableAddress = tableAddress.asPointer();
        biasedTableAddress = this.tableAddress.minus(coveredAreaStart.unsignedShiftedRight(log2RangeSize));
        FatalError.check(byteAddressFor(coveredAreaStart).equals(tableAddress), "incorrect initialization of region table");
    }

    /**
     * Test whether the address is covered by this table.
     * @param address an address in virtual memory
     * @return true if the address in within the contiguous range of virtual memory covered by this table.
     */
    final public boolean isCovered(Address address) {
        return address.greaterEqual(coveredAreaStart) && address.lessThan(coveredAreaEnd);
    }

    @INLINE
    private void checkCoverage(Address address) {
        if (MaxineVM.isDebug()) {
            FatalError.check(isCovered(address), "must not pass an uncovered address to an unsafe method");
        }
    }

    private void checkIndex(int index) {
        if (MaxineVM.isDebug()) {
            FatalError.check(index >= 0 && index < table.length, "must not pass an uncovered index to an unsafe method");
        }
    }

    public final int tableEntryIndex(Address coveredAddress) {
        return coveredAddress.minus(coveredAreaStart).unsignedShiftedRight(log2RangeSize).toInt();
    }

    final boolean atBoundary(Address address) {
        return address.isAligned(1 << log2RangeSize);
    }
    /**
     * Start of the region covered by the entry of the table at the specified index.
     * @param index an valid index to an element of the table
     * @return address to the first byte of the region
     */
    Address rangeStart(int index) {
        return coveredAreaStart.plus(Address.fromInt(index).shiftedLeft(log2RangeSize));
    }

    /**
     * Returns the address of the entry holding the byte corresponding to the region that contains the specified address.
     * @param coveredAddress an address in the contiguous range of virtual memory covered by the table.
     */
    @INLINE
    private Pointer byteAddressFor(Address coveredAddress) {
        checkCoverage(coveredAddress);
        return biasedTableAddress.plus(coveredAddress.unsignedShiftedRight(log2RangeSize));
    }

    /**
     * Set the byte in the table corresponding to an address that the caller guarantees is covered by the table.
     * Passing an uncovered address here may result in memory corruption.
     * @param coveredAddress an address guaranteed to be covered by the table
     * @param value a byte value
     */
    final void unsafeSet(Address coveredAddress, byte value) {
        byteAddressFor(coveredAddress).setByte(value);
    }

    /**
     * Get the byte in the table corresponding to an address that the caller guarantees is covered by the table.
     * Passing an uncovered address here result in returning an random value or a memory access violation.
     * @param address an address guaranteed to be covered by the table
     * @return a byte value
     */
    final byte unsafeGet(Address address) {
        return byteAddressFor(address).getByte();
    }

    final byte unsafeGet(int index) {
        return tableAddress.getByte(index);
    }

    final void unsafeSet(int index, byte value) {
        tableAddress.setByte(index, value);
    }

    final public byte get(int index) {
        return table[index];
    }

    final  public byte set(int index, byte value) {
        return table[index] = value;
    }

    /**
     * Set all the entries of the table to the specified value.
     * @param value byte value to set all entries of the table to.
     */
    void fill(byte value) {
        Arrays.fill(table, value);
    }

    /**
     * Set all the entries of the table in the specified range of indexes to the specified byte value.
     * The range extends from index fromIndex, inclusive, to index toIndex, exclusive.
     * @param fromIndex first entry of the range (inclusive)
     * @param toIndex index to the last entry of the range (exclusive)
     * @param value
     */
    void fill(int fromIndex, int toIndex, byte value) {
        Arrays.fill(table, fromIndex, toIndex, value);
    }

}
