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
import static com.sun.max.vm.heap.gcx.HeapRegionManager.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

/**
 * Backing storage for a heap is managed via a heap account created on demand by the {@link HeapRegionManager}.
 * A heap account provides a guaranteed reserve of space, corresponding to the maximum space required
 * by the account owner. Space is expressed in terms of number of heap regions, whose size is defined
 * in {@link HeapRegionConstants}.
 * The account owner can allocate regions on demand up to the account's reserve.
 */
public class HeapAccount<T extends HeapAccountOwner>{
    /**
     * Owner of the account.
     */
    final T owner;
    /**
     * Guaranteed reserve of regions for this account.
     */
    private int reserve;

    /**
     * A region range iterable private to the heap account.
     * Mostly for debugging / verification purposes.
     */
    private final HeapRegionRangeIterable regionsRangeIterable;

    /**
     * List of regions allocated to the account owner. All allocated regions are committed
     */
    @CONSTANT_WHEN_NOT_ZERO
    private HeapRegionList allocated;

    public HeapAccount(T owner) {
        this.owner = owner;
        this.regionsRangeIterable = new HeapRegionRangeIterable();
    }

    /**
     * Open the account with the specified amount of space.
     * @param spaceReserve
     * @return true if the account is opened with the guaranteed reserve of space rounded up to an integral number of regions.
     */
    public boolean open(int numRegions) {
        if (reserve > 0) {
            // Can't open an account twice.
            return false;
        }
        if (theHeapRegionManager.reserve(numRegions)) {
            reserve = numRegions;
            allocated = HeapRegionList.RegionListUse.ACCOUNTING.createList();
            regionsRangeIterable.initialize(allocated);
            return true;
        }
        return false;
    }

    public void close() {
        if (reserve > 0) {
            // TODO (ld) NEED SOME GUARANTEE THAT THE ACCOUNT HOLDS NO LIVE OBJECTS
            // Free all the regions. Should we uncommit them too ?
            theHeapRegionManager.release(reserve);
            reserve = 0;
        }
    }

    /**
     * Number of regions in the reserve.
     * @return a number of regions.
     */
    public int reserve() {
        return reserve;
    }

    public int used() {
        return allocated.size();
    }

    /**
     * The owner of the heap account.
     * @return an object
     */
    public T owner() { return owner; }

    /**
     * Allocate region, commit their backing storage.
     * @return
     */
    public synchronized int allocate() {
        if (allocated.size() < reserve) {
            int regionID = theHeapRegionManager.regionAllocator().allocate();
            if (regionID != INVALID_REGION_ID) {
                RegionTable.theRegionTable().regionInfo(regionID).setOwner(owner);
                allocated.prepend(regionID);
            }
        }
        return INVALID_REGION_ID;
    }

    void recordAllocated(int regionID, int numRegions, HeapRegionList recipient, boolean prepend) {
        final int lastRegionID = regionID + numRegions - 1;
        int r = regionID;
        // Record the allocated regions for accounting, initialize their region information,
        HeapRegionInfo regionInfo = RegionTable.theRegionTable().regionInfo(r);
        while (r <= lastRegionID) {
            regionInfo.setOwner(owner);
            allocated.append(r++);
            regionInfo = regionInfo.next();
        }
        if (recipient != null) {
            // add them to their recipient in the desired order.
            if (prepend) {
                r = lastRegionID;
                while (r >= regionID) {
                    recipient.prepend(r--);
                }
            } else {
                r = regionID;
                while (r <= lastRegionID) {
                    recipient.append(r++);
                }
            }
        }
    }

    /**
     * Allocate regions in a minimum number of discontinuous range and add them in the specified region list.
     *
     * @param numRegions number of contiguous regions requested
     * @param recipient the heap region list where the regions will be added if the request succeeds
     * @param prepend if true, insert the allocated regions at the head of the list, otherwise at the tail.
     * @return true if the requested number of regions is allocated, false otherwise.
     */
    public synchronized boolean allocate(int numRegions, HeapRegionList recipient, boolean prepend) {
        if (allocated.size() + numRegions > reserve) {
            return false;
        }
        final FixedSizeRegionAllocator regionAllocator = theHeapRegionManager.regionAllocator();
        int numRegionsNeeded = numRegions;
        while (numRegionsNeeded > 0) {
            final RegionRange range = regionAllocator.allocateLessOrEqual(numRegions);
            // For now, every allocated region is always committed
            // Probably only want to do that on not already committed regions. The
            // region allocator should be able to discriminate that.
            final int firstAllocatedRegion = range.firstRegion();
            final int numAllocatedRegions = range.numRegions();
            regionAllocator.commit(firstAllocatedRegion, numAllocatedRegions);
            recordAllocated(firstAllocatedRegion, numAllocatedRegions, recipient, prepend);
            numRegionsNeeded -= numAllocatedRegions;
        }
        return true;
    }

    /**
     * Allocate contiguous regions and add them in the specified region list.
     *
     * @param numRegions number of contiguous regions requested
     * @param recipient the heap region list where the regions will be added if the request succeeds
     * @param prepend if true, insert the regions at the head of the list, otherwise at the tail.
     * @return true if the requested number of contiguous regions is allocated, false otherwise.
     */
    public synchronized boolean allocateContiguous(int numRegions, HeapRegionList recipient, boolean prepend) {
        if (allocated.size() + numRegions > reserve) {
            return false;
        }
        int regionID = theHeapRegionManager.regionAllocator().allocate(numRegions);
        if (regionID == INVALID_REGION_ID) {
            return false;
        }
        theHeapRegionManager.regionAllocator().commit(regionID, numRegions);
        recordAllocated(regionID, numRegions, recipient, prepend);
        return true;
    }

    public boolean allocate(int numRegions) {
        return allocate(numRegions, null, false);
    }

    public boolean allocateContiguous(int numRegions) {
        return allocateContiguous(numRegions, null, false);
    }

    /**
     * Walk over the regions of the heap account an apply the closure to all objects.
     * This assume that all the regions of the account are parsable.
     */
    public void visitObjects(CellVisitor visitor) {
        final int log2RegionSizeInBytes = HeapRegionConstants.log2RegionSizeInBytes;
        regionsRangeIterable.reset();
        while (regionsRangeIterable.hasNext()) {
            final RegionRange regionRange = regionsRangeIterable.next();
            final int firstRegion = regionRange.firstRegion();
            Pointer cell = RegionTable.theRegionTable().regionAddress(firstRegion).asPointer();
            final Pointer endOfRange = cell.plus(regionRange.numRegions() << log2RegionSizeInBytes);
            do {
                cell = visitor.visitCell(cell);
            } while(cell.lessThan(endOfRange));
        }
    }
}
