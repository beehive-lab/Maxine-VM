/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;

/**
 * The heap region table centralizes all the descriptors for all regions in the heap space.
 * Region descriptors of all the regions are allocated in a single contiguous space in order
 * to enable a simple mapping from region address to region descriptor, which in turn, enables
 * direct retrieval of a region descriptor from an arbitrary pointer to the heap space.
 * The index in the region table forms a unique region identifier that can be simply maps to a region
 * descriptor and the region address in the heap space. The region table occupies the first bytes of
 * the heap space.
 *
 * @author Laurent Daynes
 */
public final class RegionTable {
    static final int TableOffset = ClassActor.fromJava(RegionTable.class).dynamicTupleSize().toInt();

    @CONSTANT_WHEN_NOT_ZERO
    private static RegionTable theRegionTable;

    private Pointer table() {
        return Reference.fromJava(this).toOrigin().plus(TableOffset);
    }

    /**
     * Base address of the contiguous space backing up the heap regions.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private  Address regionBaseAddress;

    @CONSTANT_WHEN_NOT_ZERO
    private int regionInfoSize;

    @CONSTANT_WHEN_NOT_ZERO
    private int length;

    private boolean isInHeapRegion(Address addr) {
        return HeapRegionManager.theHeapRegionManager.contains(addr);
    }

    private RegionTable() {
    }

    static void initialize(RegionTable regionTable, Class<HeapRegionInfo> regionInfoClass, Address firstRegion, int numRegions) {
        regionTable.regionBaseAddress = firstRegion;
        regionTable.length = numRegions;
        regionTable.regionInfoSize = ClassActor.fromJava(regionInfoClass).dynamicTupleSize().toInt();
        theRegionTable = regionTable;
    }

    public Pointer regionAddress(int regionID) {
        return table().plus(regionID * regionSizeInBytes);
    }
    public int addressToRegionID(Address addr) {
        if (!isInHeapRegion(addr)) {
            return INVALID_REGION_ID;
        }
        return addr.minus(regionBaseAddress).unsignedShiftedRight(log2RegionSizeInBytes).toInt();
    }

    public HeapRegionInfo addressToRegionInfo(Address addr) {
        if (!isInHeapRegion(addr)) {
            return null;
        }
        return HeapRegionInfo.toHeapRegionInfo(regionAddress(addressToRegionID(addr)));
    }

    public int toRegionID(HeapRegionInfo regionInfo) {
        final int regionID = Reference.fromJava(regionInfo).toOrigin().minus(table()).dividedBy(regionInfoSize).toInt();
        return regionID;
    }
}
