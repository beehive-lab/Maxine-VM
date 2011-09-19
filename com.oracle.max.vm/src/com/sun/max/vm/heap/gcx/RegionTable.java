/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * The heap region table centralizes all the descriptors for all regions in the heap space.
 * Region descriptors of all the regions are allocated in a single contiguous space in order
 * to enable a simple mapping from region address to region descriptor, which in turn, enables
 * direct retrieval of a region descriptor from an arbitrary pointer to the heap space.
 * The index in the region table forms a unique region identifier that can be simply maps to a region
 * descriptor and the region address in the heap space. The region table occupies the first bytes of
 * the heap space.
 */
public final class RegionTable {
    public static final HeapRegionInfo nullHeapRegionInfo = new HeapRegionInfo();

    @INSPECTED
    static final int TableOffset = ClassActor.fromJava(RegionTable.class).dynamicTupleSize().toInt();

    @INSPECTED
    @CONSTANT_WHEN_NOT_ZERO
    private static RegionTable theRegionTable;

    @INLINE
    static RegionTable theRegionTable() {
        return theRegionTable;
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native HeapRegionInfo asHeapRegionInfo(Object regionInfo);

    @INLINE
    private static HeapRegionInfo toHeapRegionInfo(Pointer regionInfoPointer) {
        return asHeapRegionInfo(Reference.fromOrigin(Layout.cellToOrigin(regionInfoPointer)).toJava());
    }

    private Pointer table() {
        return Reference.fromJava(this).toOrigin().plus(TableOffset);
    }

    @INSPECTED
    final private Address regionPoolStart;

    @INSPECTED
    final private Address regionPoolEnd;

    @INSPECTED
    final private int regionInfoSize;

    @INSPECTED
    final private int length;

    private boolean isInHeapRegion(Address address) {
        return address.greaterEqual(regionPoolStart) && address.lessThan(regionPoolEnd);
    }

    @HOSTED_ONLY
    public RegionTable(Address start, Address end, int numRegions, int infoSize) {
        regionPoolStart = start;
        regionPoolEnd = end;
        length = numRegions;
        regionInfoSize = infoSize;
    }

    private RegionTable(Class<HeapRegionInfo> regionInfoClass, MemoryRegion regionPool, int numRegions) {
        final HeapScheme heapScheme = VMConfiguration.vmConfig().heapScheme();
        final Hub regionInfoHub = ClassActor.fromJava(regionInfoClass).dynamicHub();
        regionPoolStart = regionPool.start();
        regionPoolEnd = regionPool.end();
        regionInfoSize = regionInfoHub.tupleSize.toInt();

        for (int i = 0; i < numRegions; i++) {
            Object regionInfo = heapScheme.createTuple(regionInfoHub);
            if (MaxineVM.isDebug()) {
                FatalError.check(regionInfo == regionInfo(i), "Failed to create valid region table");
            }
        }
        // This makes the region table initialized with respect to the inspector.
        length = numRegions;
    }

    static void initialize(Class<HeapRegionInfo> regionInfoClass, MemoryRegion regionPool, int numRegions) {
        theRegionTable = new RegionTable(regionInfoClass, regionPool, numRegions);
    }

    int regionID(HeapRegionInfo regionInfo) {
        final int regionID = Reference.fromJava(regionInfo).toOrigin().minus(table()).dividedBy(regionInfoSize).toInt();
        return regionID;
    }

    private int inHeapAddressRegionID(Address addr) {
        return addr.minus(regionPoolStart).unsignedShiftedRight(log2RegionSizeInBytes).toInt();
    }

    /**
     * Returns the region ID of the heap region an address refers to.
     * @param address
     * @return an integer identifying a heap region, or {@link HeapRegionConstants#INVALID_REGION_ID} if the address doesn't refer to a location in a heap region.
     */
    public int regionID(Address address) {
        if (!isInHeapRegion(address)) {
            return INVALID_REGION_ID;
        }
        return inHeapAddressRegionID(address);
    }

    /**
     * Inspector support.
     * @param regionID
     * @return
     */
    @HOSTED_ONLY
    public int regionInfoOffset(int regionID) {
        return TableOffset +  regionID * regionInfoSize;
    }
    @HOSTED_ONLY
    public int regionIDFromRegionInfoOffset(int offsetFromTableAddress) {
        int offsetFromTableBase = offsetFromTableAddress - TableOffset;
        assert offsetFromTableBase % regionInfoSize == 0;
        int index = (offsetFromTableAddress - TableOffset) / regionInfoSize;
        assert index < length;
        return index;
    }

    public boolean isValidRegionID(int regionID) {
        return regionID >= 0 && regionID < length;
    }

    HeapRegionInfo regionInfo(int regionID) {
        return toHeapRegionInfo(table().plus(regionID * regionInfoSize));
    }

    HeapRegionInfo inHeapAddressRegionInfo(Address addr) {
        return regionInfo(inHeapAddressRegionID(addr));
    }

    HeapRegionInfo regionInfo(Address addr) {
        if (!isInHeapRegion(addr)) {
            return nullHeapRegionInfo;
        }
        return regionInfo(inHeapAddressRegionID(addr));
    }

    public Address regionAddress(int regionID) {
        return regionPoolStart.plus(regionID << log2RegionSizeInBytes);
    }

    Address regionAddress(HeapRegionInfo regionInfo) {
        return regionAddress(regionID(regionInfo));
    }


    /**
     * Apply CellVisitor over all references within a region range.
     * @param regionRange
     * @param cellVisitor
     */
    void walk(RegionRange regionRange, CellVisitor cellVisitor) {
        Pointer p = regionAddress(regionRange.firstRegion()).asPointer();
        final Pointer end = p.plus(regionRange.numRegions()  << log2RegionSizeInBytes);
        while (p.lessThan(end)) {
            p = cellVisitor.visitCell(p);
        }
    }

    HeapRegionInfo next(HeapRegionInfo regionInfo) {
        return toHeapRegionInfo(Reference.fromJava(regionInfo).toOrigin().plus(regionInfoSize));
    }

    HeapRegionInfo prev(HeapRegionInfo regionInfo) {
        return toHeapRegionInfo(Reference.fromJava(regionInfo).toOrigin().minus(regionInfoSize));
    }
}
