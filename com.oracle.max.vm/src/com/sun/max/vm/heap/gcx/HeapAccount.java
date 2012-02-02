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
import com.sun.max.vm.heap.gcx.HeapRegionList.RegionListUse;
import com.sun.max.vm.runtime.*;

/**
 * Backing storage for a heap is managed via a heap account created on demand by the {@link HeapRegionManager}.
 * A heap account provides a guaranteed reserve of space, corresponding to the maximum space required
 * by the account owner. Space is expressed in terms of number of heap regions, whose size is defined
 * in {@link HeapRegionConstants}.
 *
 * The account owner can allocate regions on demand up to the account's reserve.
 * Allocating regions actually allocates ranges of virtual memory to backup the desired number of regions.
 * The regions may be allocated from discontinuous range of virtual memory.
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
     * List of committed regions allocated to the account owner.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private HeapRegionList committed;

    /**
     * List of uncommitted regions allocated to the account owner.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private HeapRegionList uncommitted;

    private UnmodifiableHeapRegionList roCommitted;

    private UnmodifiableHeapRegionList roUncommitted;

    public HeapAccount(T owner) {
        this.owner = owner;
        this.regionsRangeIterable = new HeapRegionRangeIterable();
    }

    public HeapRegionList committedRegions() {
        return roCommitted;
    }

    public HeapRegionList uncommittedRegions() {
        return roUncommitted;
    }

    /**
     * Open the account with the specified amount of space.
     * @param numRegions number of regions to reserve.
     * @return true if the account is opened with the guaranteed reserve of space rounded up to an integral number of regions.
     */
    public boolean open(int numRegions) {
        if (reserve > 0) {
            // Can't open an account twice.
            return false;
        }
        if (theHeapRegionManager.reserve(numRegions)) {
            reserve = numRegions;
            committed = HeapRegionList.RegionListUse.ACCOUNTING.createList();
            uncommitted = HeapRegionList.RegionListUse.ACCOUNTING.createList();
            roCommitted = new UnmodifiableHeapRegionList(committed);
            roUncommitted = new UnmodifiableHeapRegionList(uncommitted);
            regionsRangeIterable.initialize(roCommitted);
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
        return committed.size();
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
        if (committed.size() < reserve) {
            int regionID = theHeapRegionManager.regionAllocator().allocate();
            if (regionID != INVALID_REGION_ID) {
                RegionTable.theRegionTable().regionInfo(regionID).setOwner(owner);
                committed.prepend(regionID);
            }
        }
        return INVALID_REGION_ID;
    }


    /**
     * Record a new contiguous range of regions into an accounting list. Region in the ranges may not be linked in the accounting list's shared storage.
     * @param regionID
     * @param numRegions
     * @param accountingList
     */
    private void recordRange(int regionID, int numRegions, HeapRegionList accountingList) {
        if (numRegions > 1) {
            final int lastRegionID = regionID + numRegions - 1;
            accountingList.linkRange(regionID, lastRegionID);
            addRange(regionID, lastRegionID, accountingList);
        } else {
            add(regionID, accountingList);
        }
    }

    /**
     * Add a contiguous range of regions into an accounting list. Region in the ranges must be linked in the accounting list's shared storage.
     * @param regionID
     * @param numRegions
     * @param accountingList
     */
    private void addRange(int regionID, int lastRegionID, HeapRegionList accountingList) {
        int tail = accountingList.tail();
        if (accountingList.isEmpty() || tail < regionID) {
            accountingList.appendRange(regionID, lastRegionID);
        } else if (regionID < accountingList.head()) {
            accountingList.prependRange(regionID, lastRegionID);
        } else {
            // Search the ordered accounting list for the first element below the range.
            int r = regionID;
            do {
                int n = accountingList.next(r);
                if (regionID > n) {
                    accountingList.insertRangeAfter(r, regionID, lastRegionID);
                    break;
                }
                r = n;
            } while (r != accountingList.tail());
        }
    }

    private void add(int regionID, HeapRegionList accountingList) {
        // Search the ordered accounting list for the first element below the range.
        if (regionID > accountingList.tail()) {
            accountingList.append(regionID);
        } else if (regionID < accountingList.head()) {
            accountingList.prepend(regionID);
        } else {
            int r = regionID;
            do {
                int n = accountingList.next(r);
                if (regionID > n) {
                    accountingList.insertAfter(r, regionID);
                    break;
                }
                r = n;
            } while (r != accountingList.tail());
        }
    }

    /**
     * Recording a newly allocated contiguous range regions. This set the ownership in all the regions in the range, and create the linkage between
     * regions in the accounting list's shared storage.
     *
     * @param regionID identifier of the first region of the allocated contiguous range
     * @param numRegions number of regions  of the allocated contiguous range
     * @param accountingList accounting list where the range should be recorded ({@link #committed} or {@link #uncommitted})
     * @param recipient a {@link RegionListUse#OWNERSHIP} region list where to record the allocated region if non-null
     * @param prepend prepend the range to the recipient list if true, append otherwise
     */
    private void recordAllocated(int regionID, int numRegions, HeapRegionList accountingList, HeapRegionList recipient, boolean prepend) {
        final int lastRegionID = regionID + numRegions - 1;
        int r = regionID;
        RegionTable regionTable = RegionTable.theRegionTable();
        // Record the allocated regions for accounting, initialize their region information,
        while (r <= lastRegionID) {
            regionTable.regionInfo(r++).setOwner(owner);
        }
        recordRange(regionID, numRegions, accountingList);
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
     * Allocate regions in a minimum number of discontinuous ranges and add them in the specified region list.
     *
     * @param numRegions number of contiguous regions requested
     * @param recipient the heap region list where the regions will be added if the request succeeds
     * @param prepend if true, insert the allocated regions at the head of the list, otherwise at the tail.
     * @param orLess  if true, allocate the remainder of what is left, otherwise, don't allocate any region.
     * @return The number of regions allocated, or, if orLess is false, the number of regions left if the
     * account doesn't have enough regions to satisfy the request.
     */
    public synchronized int allocate(int numRegions, HeapRegionList recipient, boolean prepend, boolean orLess, boolean commit) {
        int numRegionsLeft = reserve - (committed.size() + uncommitted.size());
        if (numRegionsLeft < numRegions) {
            if (!orLess) {
                return numRegionsLeft;
            }
            numRegions = numRegionsLeft;
        }

        final HeapRegionList accountingList = commit ? committed : uncommitted;
        final FixedSizeRegionAllocator regionAllocator = theHeapRegionManager.regionAllocator();
        int numRegionsNeeded = numRegions;
        while (numRegionsNeeded > 0) {
            final RegionRange range = regionAllocator.allocateLessOrEqual(numRegions);
            final int firstAllocatedRegion = range.firstRegion();
            final int numAllocatedRegions = range.numRegions();
            if (commit) {
                regionAllocator.commit(firstAllocatedRegion, numAllocatedRegions);
            }
            recordAllocated(firstAllocatedRegion, numAllocatedRegions, accountingList, recipient, prepend);
            numRegionsNeeded -= numAllocatedRegions;
        }
        return numRegions;
    }

    /**
     * Commit the memory of the specified region in virtual space.
     * This throws a FatalError if the region isn't allocated to this account.
     *
     * @param regionID the region allocated to this account whose virtual memory pages will be committed
     */
    public synchronized void commit(int regionID) {
        FatalError.check(uncommitted.contains(regionID), "The region must be allocated to this account");
        theHeapRegionManager.regionAllocator().commit(regionID, 1);
        uncommitted.remove(regionID);
        add(regionID, committed);
    }

    /**
     * Uncommit the memory of the specified region in virtual space.
     * This throws a FatalError if the region isn't allocated to this account.
     *
     * @param regionID the region from this account whose virtual memory pages will be uncommitted
     */
    public synchronized void uncommit(int regionID) {
        FatalError.check(committed.contains(regionID), "The region must be allocated and committed to this account");
        theHeapRegionManager.regionAllocator().uncommit(regionID, 1);
        committed.remove(regionID);
        add(regionID, uncommitted);
    }

    /**
     * Commit the memory of the specified contiguous range of regions in virtual space.
     *
     * @param regionsRange the contiguous range of regions allocated to this account whose virtual memory pages will be committed
     */
    public synchronized void commit(RegionRange regionsRange) {
        int rangeHead = regionsRange.firstRegion();
        int numRegions = regionsRange.numRegions();
        if (numRegions == 1) {
            commit(rangeHead);
            return;
        }
        int rangeTail = rangeHead +  numRegions - 1;
        FatalError.check(uncommitted.containsRange(rangeHead, rangeTail), "The regions range must be allocated to this account");
        theHeapRegionManager.regionAllocator().commit(rangeHead, numRegions);
        uncommitted.removeRange(rangeHead, rangeTail);
        addRange(rangeHead, rangeTail, committed);
    }

    /**
     * Uncommit the memory of the specified contiguous range of regions in virtual space.
     *
     * @param regionsRange the contiguous range of regions allocated to this account whose virtual memory pages will be uncommitted
     */
    public synchronized void uncommit(RegionRange regionsRange) {
        int rangeHead = regionsRange.firstRegion();
        int numRegions = regionsRange.numRegions();
        if (numRegions == 1) {
            commit(rangeHead);
            return;
        }
        int rangeTail = rangeHead +  numRegions - 1;
        FatalError.check(uncommitted.containsRange(rangeHead, rangeTail), "The regions range must be allocated to this account");
        theHeapRegionManager.regionAllocator().uncommit(regionsRange.firstRegion(), numRegions);
        committed.removeRange(rangeHead, rangeTail);
        addRange(rangeHead, rangeTail, uncommitted);
    }

    /**
     * Allocate contiguous regions and add them in the specified region list.
     *
     * @param numRegions number of contiguous regions requested
     * @param recipient the heap region list where the regions will be added if the request succeeds
     * @param prepend if true, insert the regions at the head of the list, otherwise at the tail.
     * @return true if the requested number of contiguous regions is allocated, false otherwise.
     */
    public synchronized boolean allocateContiguous(int numRegions, HeapRegionList recipient, boolean prepend, boolean commit) {
        if (committed.size() + uncommitted.size() + numRegions > reserve) {
            return false;
        }
        int regionID = theHeapRegionManager.regionAllocator().allocate(numRegions);
        if (regionID == INVALID_REGION_ID) {
            return false;
        }
        HeapRegionList accountingList = uncommitted;
        if (commit) {
            accountingList = committed;
            theHeapRegionManager.regionAllocator().commit(regionID, numRegions);
        }
        recordAllocated(regionID, numRegions, accountingList, recipient, prepend);
        return true;
    }

    static private boolean bootstrapCompleted = false;

    static void completeBootHeapAccountBootstrap(int numRegions) {
        FatalError.check(!bootstrapCompleted, "Bootstrap already completed!");
        HeapAccount<HeapRegionManager> bootAccount = HeapRegionManager.theHeapRegionManager().heapAccount();
        bootAccount.recordAllocated(0, numRegions, bootAccount.committed, null, false);
        bootstrapCompleted = true;
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
