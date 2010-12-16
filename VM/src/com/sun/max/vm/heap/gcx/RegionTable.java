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
package com.sun.max.vm.heap.gcx;
import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;

import java.util.*;

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

    @INLINE
    static RegionTable theRegionTable() {
        return theRegionTable;
    }

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

    int regionID(HeapRegionInfo regionInfo) {
        final int regionID = Reference.fromJava(regionInfo).toOrigin().minus(table()).dividedBy(regionInfoSize).toInt();
        return regionID;
    }

    int regionID(Address addr) {
        if (!isInHeapRegion(addr)) {
            return INVALID_REGION_ID;
        }
        return addr.minus(regionBaseAddress).unsignedShiftedRight(log2RegionSizeInBytes).toInt();
    }

    HeapRegionInfo regionInfo(int regionID) {
        return HeapRegionInfo.toHeapRegionInfo(table().plus(regionID * regionInfoSize));
    }

    HeapRegionInfo regionInfo(Address addr) {
        if (!isInHeapRegion(addr)) {
            return null;
        }
        return regionInfo(regionID(addr));
    }

    Address regionAddress(int regionID) {
        return regionBaseAddress.plus(regionID << log2RegionSizeInBytes);
    }

    Address regionAddress(HeapRegionInfo regionInfo) {
        return regionAddress(regionID(regionInfo));
    }

    HeapRegionInfo next(HeapRegionInfo regionInfo) {
        return HeapRegionInfo.toHeapRegionInfo(Reference.fromJava(regionInfo).toOrigin().plus(regionInfoSize));
    }

    HeapRegionInfo prev(HeapRegionInfo regionInfo) {
        return HeapRegionInfo.toHeapRegionInfo(Reference.fromJava(regionInfo).toOrigin().minus(regionInfoSize));
    }

    final class IterableRegionTableRange implements Iterable<HeapRegionInfo>, Iterator<HeapRegionInfo> {
        private HeapRegionInfo last;
        private HeapRegionInfo current;

        IterableRegionTableRange() {
            current = null;
            last = null;
        }

        public IterableRegionTableRange reset(int firstRegionID, int numRegions) {
            current = regionInfo(firstRegionID);
            last = regionInfo(firstRegionID + numRegions - 1);
            return this;
        }
        public Iterator<HeapRegionInfo> iterator() { return this; }
        public boolean hasNext() {
            return current != last;
        }

        public HeapRegionInfo next() {
            current = theRegionTable.next(current);
            return current;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
