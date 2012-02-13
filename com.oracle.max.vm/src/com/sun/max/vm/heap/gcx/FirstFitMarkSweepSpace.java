/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
import static com.sun.max.vm.heap.gcx.HeapRegionInfo.*;
import static com.sun.max.vm.heap.gcx.HeapRegionState.*;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.rset.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;
/**
 * A region-based, mark-sweep heap space, with bump pointer allocation only.
 * Each partially occupied region has a list of addressed ordered free chunks, used to allocate TLAB refills.
 * An overflow allocator avoids refilling too frequently.
 */
public final class FirstFitMarkSweepSpace<T extends HeapAccountOwner> extends HeapRegionSweeper implements HeapSpace, RegionProvider {
    /* For simplicity at the moment. Should be able to allocate this in GC's own heap (i.e., the HeapRegionManager's allocator).
     */
    private static final OutOfMemoryError outOfMemoryError = new OutOfMemoryError();

    public static boolean TraceLargeObjectAllocations = false;
    static {
        VMOptions.addFieldOption("-XX:", "TraceLargeObjectAllocations", FirstFitMarkSweepSpace.class, "Trace allocation of large multi-regions objects", Phase.PRISTINE);
    }

    /**
     * Heap account regions from this space are allocated from.
     */
    final HeapAccount<T> heapAccount;

    /**
     * Tag for region of this space. By default, 0 (i.e., tag-less).
     */
    final int regionTag;

    /**
     * List of regions with space available for allocation.
     * Initialized with all regions. Then reset by the sweeper at every collection.
     * Used to refill the TLAB allocator and the overflow allocator.
     */
    private HeapRegionList allocationRegions;

    /**
     * List of regions with space available for TLAB allocation only.
     */
    private HeapRegionList tlabAllocationRegions;

    /**
     * List used to keep track of regions with live objects that are unavailable for allocation.
     */
    HeapRegionList unavailableRegions;

    /**
     * Temporary list used during GC-ing of this space. Before GC, all regions of the space are moved to this list, which then hold all the regions
     * allocated to this space. During sweeping, the GC redistribute the regions from this to the above three lists depending on their available free space.
     */
    private HeapRegionList sweepList;

    /**
     * Total number of regions currently allocated to this heap space.
     */
    private int numRegionsInSpace;

    /**
     * Maximum number of regions that this space can allocate from the heap account.
     */
    private int maxRegionsInSpace;

    /**
     * Total free space in allocation regions (i.e., regions in both {@link #allocationRegions} and {@link #tlabAllocationRegions} lists).
     * This doesn't count space in regions assigned to allocators (i.e., {@link #tlabAllocator} and {@link #overflowAllocator}).
     * Reset after each GC. Then decremented when allocators refill.
     */
    private Size allocationRegionsFreeSpace;

    /**
     * TLAB refill allocator. Can supplies TLAB refill either as a single contiguous chunk,
     * or as an address-ordered list of chunks.
     */
    final ChunkListAllocator<RegionChunkListRefillManager> tlabAllocator;

    /**
     * Overflow allocator. Handles direct allocation request and all small overflow of TLABs.
     */
    final BaseAtomicBumpPointerAllocator<RegionOverflowAllocatorRefiller> overflowAllocator;

    /**
     * Pre-allocated region range iterator. Provides GC operations with allocation free  iteration over contiguous region ranges from a list.
     */
    final HeapRegionRangeIterable regionsRangeIterable;

    /**
     * Support for iterating over region info of regions from a list.
     */
    final HeapRegionInfoIterable regionInfoIterable;

    /**
     * Minimum size to be treated as a large object.
     */
    private Size minLargeObjectSize;
    /**
     * Minimum free space to refill the overflow allocator.
     */
    private Size minOverflowRefillSize;

    /**
     * Indicate whether a size is categorized as large. Request for large size must go to the large object allocator.
     * @param size size in words
     * @return true if the size is considered large.
     */
    protected boolean isLarge(Size size) {
        return size.greaterEqual(minLargeObjectSize);
    }

    private Pointer allocateSingleRegionLargeObject(HeapRegionInfo rinfo, Pointer allocated, Size requestedSize, Size totalChunkSize) {
        final int regionID = rinfo.toRegionID();
        allocationRegions.remove(regionID);
        Pointer leftover = allocated.plus(requestedSize);
        Size spaceLeft = totalChunkSize.minus(requestedSize);
        if (TraceLargeObjectAllocations) {
            Log.print("allocateLarge region #");
            Log.println(regionID);
        }
        deadSpaceListener.notifyCoaslescing(allocated, requestedSize);
        if (spaceLeft.lessThan(minReclaimableSpace)) {
            if (!spaceLeft.isZero()) {
                HeapSchemeAdaptor.fillWithDeadObject(leftover, leftover.plus(spaceLeft));
                deadSpaceListener.notifyCoaslescing(leftover, spaceLeft);
            }
            FULL_REGION.setState(rinfo);
            unavailableRegions.append(regionID);
        } else {
            if (TraceLargeObjectAllocations) {
                Log.print("allocateLarge putback region #");
                Log.print(regionID);
                Log.print(" in TLAB allocation list with ");
                Log.print(spaceLeft.toInt());
                Log.println(" bytes");
            }
            HeapFreeChunk.format(leftover, spaceLeft);
            deadSpaceListener.notifyCoaslescing(leftover, spaceLeft);
            rinfo.setFreeChunks(leftover,  spaceLeft, 1);
            FREE_CHUNKS_REGION.setState(rinfo);
            tlabAllocationRegions.append(regionID);
            allocationRegionsFreeSpace = allocationRegionsFreeSpace.plus(spaceLeft);
        }
        return allocated;
    }

    private int debug_numContiguousRegionNeeded;
    private int debug_firstRegion;
    private int debug_lastRegion;

    private Pointer allocateLarge(Size size) {
        final Size roundedUpSize = size.alignUp(regionSizeInBytes);
        final Size tailSize = roundedUpSize.minus(size);
        final int extraRegion = tailSize.greaterThan(0) && tailSize.lessThan(HeapSchemeAdaptor.MIN_OBJECT_SIZE)  ? 1 : 0;
        int numContiguousRegionNeeded = roundedUpSize.unsignedShiftedRight(log2RegionSizeInBytes).toInt() + extraRegion;

        if (TraceLargeObjectAllocations) {
            Log.print("requesting #");
            Log.print(numContiguousRegionNeeded);
            Log.println(" contiguous regions");
        }
        synchronized (refillLock()) {
            int gcCount = 0;
            do {
                regionInfoIterable.initialize(allocationRegions);
                regionInfoIterable.reset();
                if (numContiguousRegionNeeded == 1) {
                    final int numBytesNeeded = size.toInt();
                   // Actually, any region with a chunk large enough can do in that case.
                    while (regionInfoIterable.hasNext()) {
                        final HeapRegionInfo rinfo = regionInfoIterable.next();
                        if (rinfo.isEmpty()) {
                            allocationRegionsFreeSpace = allocationRegionsFreeSpace.minus(regionSizeInBytes);
                            return allocateSingleRegionLargeObject(rinfo, rinfo.regionStart().asPointer(), size, Size.fromInt(regionSizeInBytes));
                        } else if (!rinfo.isAllocating() && rinfo.numFreeChunks() == 1 && rinfo.freeBytesInChunks() >= numBytesNeeded) {
                            allocationRegionsFreeSpace = allocationRegionsFreeSpace.minus(rinfo.freeBytesInChunks());
                            return allocateSingleRegionLargeObject(rinfo,  rinfo.firstFreeBytes().asPointer(), size, Size.fromInt(rinfo.freeBytesInChunks()));
                        }
                    }
                } else {
                    int n = 0;
                    int firstRegion = INVALID_REGION_ID;
                    int lastRegion = INVALID_REGION_ID;

                    debug_numContiguousRegionNeeded = numContiguousRegionNeeded;
                    debug_firstRegion = firstRegion;
                    debug_lastRegion = lastRegion;

                    while (regionInfoIterable.hasNext()) {
                        final HeapRegionInfo rinfo = regionInfoIterable.next();
                        if (rinfo.isEmpty()) {
                            int rid = rinfo.toRegionID();
                            if (n == 0) {
                                firstRegion  = rid;
                                lastRegion  = rid;
                                debug_firstRegion = firstRegion;
                                n = 1;
                            } else if (rid == lastRegion + 1) {
                                lastRegion = rid;
                                debug_lastRegion = lastRegion;
                                if (++n >= numContiguousRegionNeeded) {
                                    // Got the number of requested contiguous regions.
                                    // Remove them all from the list (except the tail if it leaves enough space for overflow allocation)
                                    // and turn them into large object regions.
                                    if (TraceLargeObjectAllocations) {
                                        Log.print("allocate contiguous regions [");
                                        Log.print(firstRegion);
                                        Log.print(", ");
                                        Log.print(lastRegion);
                                        Log.println("]");
                                    }
                                    allocationRegions.remove(firstRegion);
                                    HeapRegionInfo firstRegionInfo = HeapRegionInfo.fromRegionID(firstRegion);
                                    LARGE_HEAD.setState(firstRegionInfo);
                                    unavailableRegions.append(firstRegion);
                                    if (n > 2) {
                                        for (int i = firstRegion + 1; i < lastRegion; i++) {
                                            allocationRegions.remove(i);
                                            LARGE_BODY.setState(HeapRegionInfo.fromRegionID(i));
                                            unavailableRegions.append(i);
                                        }
                                    }
                                    HeapRegionInfo lastRegionInfo =  HeapRegionInfo.fromRegionID(lastRegion);
                                    Pointer tailEnd = lastRegionInfo.regionStart().plus(regionSizeInBytes).asPointer();
                                    Pointer tail = tailEnd.minus(tailSize);
                                    // Another ugly trick to share this code between generational and flat heap code. We use the deadSpaceUpdater to set up
                                    // the FOT if the space is paired with a card table.
                                    Address largeObjectCell = firstRegionInfo.regionStart();
                                    deadSpaceListener.notifyCoaslescing(largeObjectCell, size);
                                    if (tailSize.lessThan(minReclaimableSpace)) {
                                        if (!tailSize.isZero()) {
                                            HeapSchemeAdaptor.fillWithDeadObject(tail, tailEnd);
                                            deadSpaceListener.notifyCoaslescing(tail, tailSize);
                                        }
                                        allocationRegions.remove(lastRegion);
                                        LARGE_FULL_TAIL.setState(lastRegionInfo);
                                        unavailableRegions.append(lastRegion);
                                        allocationRegionsFreeSpace = allocationRegionsFreeSpace.minus(Size.fromInt(numContiguousRegionNeeded).shiftedLeft(log2RegionSizeInBytes));
                                    } else {
                                        // Format the tail as a free chunk.
                                        HeapFreeChunk.format(tail, tailSize);
                                        deadSpaceListener.notifyCoaslescing(tail, tailSize);
                                        LARGE_TAIL.setState(lastRegionInfo);
                                        lastRegionInfo.setFreeChunks(tail, tailSize, 1);
                                        if (tailSize.lessThan(minOverflowRefillSize)) {
                                            allocationRegions.remove(lastRegion);
                                            tlabAllocationRegions.append(lastRegion);
                                        }
                                        allocationRegionsFreeSpace = allocationRegionsFreeSpace.minus(size);
                                    }
                                    return largeObjectCell.asPointer();
                                }
                            } else {
                                n = 0;
                            }
                        }
                    }
                }
                if (MaxineVM.isDebug()) {
                    checkForSuspisciousGC(gcCount++);
                }
            } while(Heap.collectGarbage(roundedUpSize)); // Always collect for at least one region.
            // Not enough freed memory.
            throw outOfMemoryError;
        }
    }

    Pointer allocateLargeCleared(Size size) {
        Pointer cell = allocateLarge(size);
        Memory.clearWords(cell, size.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt());
        return cell;
    }

    /**
     * The lock on which refill and region allocation to object spaces synchronize on.
     */
    private Object refillLock() {
        return this;
    }

    private HeapRegionList tlabAllocationRegionList() {
        return tlabAllocationRegions.isEmpty() ? allocationRegions : tlabAllocationRegions;
    }

    private void checkForSuspisciousGC(int gcCount) {
        if (gcCount > 1) {
            FatalError.breakpoint();
        }
        if (gcCount > 5) {
            FatalError.unexpected("Suspiscious repeating GC calls detected");
        }
    }

    public FirstFitMarkSweepSpace(HeapAccount<T> heapAccount) {
        this(heapAccount, new BaseAtomicBumpPointerAllocator<RegionOverflowAllocatorRefiller>(new RegionOverflowAllocatorRefiller()) {
            @Override
            protected void postAllocationDo(Pointer cell, Size size) {
            }
        }, false, null, 0);
    }

    public FirstFitMarkSweepSpace(HeapAccount<T> heapAccount, BaseAtomicBumpPointerAllocator<RegionOverflowAllocatorRefiller> overflowAllocator, boolean zapDeadReferences, DeadSpaceListener deadSpaceRSetUpdater, int regionTag) {
        super(zapDeadReferences, deadSpaceRSetUpdater);
        this.heapAccount = heapAccount;
        this.regionTag = regionTag;
        this.overflowAllocator = overflowAllocator;
        tlabAllocator = new ChunkListAllocator<RegionChunkListRefillManager>(new RegionChunkListRefillManager(this));
        regionsRangeIterable = new HeapRegionRangeIterable();
        regionInfoIterable = new HeapRegionInfoIterable();
        overflowAllocator.refillManager.setRegionProvider(this);
    }

    public HeapAccount<T> heapAccount() {
        return heapAccount;
    }

    /**
     * Initialization of those elements that relies on parameters available at VM start only.
     * @param minSize
     * @param maxSize
     */
    public void initialize(Size minSize, Size maxSize) {
        Size regionSize = Size.fromInt(regionSizeInBytes);
        tlabAllocationRegions = HeapRegionList.RegionListUse.OWNERSHIP.createList();
        allocationRegions = HeapRegionList.RegionListUse.OWNERSHIP.createList();
        unavailableRegions = HeapRegionList.RegionListUse.OWNERSHIP.createList();
        sweepList = HeapRegionList.RegionListUse.OWNERSHIP.createList();

        maxRegionsInSpace = numberOfRegions(maxSize);
        FatalError.check(maxRegionsInSpace <= heapAccount.reserve(), "under provisioned heap account");

        int initialNumberOfRegions = numberOfRegions(minSize);
        int result = heapAccount.allocate(initialNumberOfRegions, allocationRegions, true, false, true, regionTag);
        if (result != initialNumberOfRegions) {
            FatalError.unexpected("Failed to create application heap");
        }

        numRegionsInSpace = initialNumberOfRegions;
        minReclaimableSpace = Size.fromInt(freeChunkMinSizeOption.getValue());
        overflowAllocator.refillManager().setMinRefillSize(minOverflowRefillSize);
        // Set the iterable to the list of committed regions. This is the default. Any exception to this should
        // reset to the committed region list when done.
        // WARNING: if the account is shared between multiple heap space, this may be problematic as regions not used by
        // this heap space will be seen during iterations.
        regionsRangeIterable.initialize(heapAccount.committedRegions());
        regionsRangeIterable.reset();
        allocationRegionsFreeSpace = regionSize.times(allocationRegions.size());

        // The following two are connected: if you deny refill after overflow, the only solution left is allocating large.
        minLargeObjectSize = regionSize;
        minOverflowRefillSize = regionSize.dividedBy(4);
        RegionChunkListRefillManager refillManager = tlabAllocator.refillManager();
        refillManager.setRefillPolicy(minReclaimableSpace);
        refillManager.setMinChunkSize(minReclaimableSpace);
        // Initialize the tlab allocator with a first region.
        tlabAllocator.initialize(regionSize, regionSize);
        overflowAllocator.initialize(Address.zero(), Size.zero());
    }

    /**
     * Entry point for direct allocation when TLAB cannot be refilled.
     */
    @Override
    public Pointer allocate(Size size) {
        if (isLarge(size)) {
            return allocateLargeCleared(size);
        }
        return overflowAllocator.allocateCleared(size);
    }

    /**
     * Entry point to allocate storage for TLAB refill.
     */
    @Override
    public Pointer allocateTLAB(Size size) {
        return tlabAllocator.allocateTLAB(size);
    }

    @Override
    public boolean contains(Address address) {
        final HeapRegionInfo regionInfo = fromAddress(address);
        return regionInfo.owner() == heapAccount.owner;
    }

    public boolean canSatisfyAllocation(Size size) {
        // FIXME: this is used to avoid redundant GC operation when a race occurred
        // to trigger a GC. This is called with heap lock protection.
        // Figuring this out is almost the same as running the allocation code.
        // There might be better way to figure out if a race occurred.
        return false;
    }

    public Size totalSpace() {
        return Size.fromInt(numRegionsInSpace).shiftedLeft(log2RegionSizeInBytes);
    }

    public Size capacity() {
        return Size.fromInt(maxRegionsInSpace).shiftedLeft(log2RegionSizeInBytes);
    }

    public Size freeSpace() {
        return allocationRegionsFreeSpace.plus(tlabAllocator.refillManager.freeSpace().plus(tlabAllocator.freeSpace().plus(overflowAllocator.freeSpace())));
    }

    public Size usedSpace() {
        return totalSpace().minus(freeSpace());
    }

    public void doBeforeGC() {
        overflowAllocator.doBeforeGC();
        tlabAllocator.doBeforeGC();
        FatalError.check(tlabAllocator.refillManager.allocatingRegion() == INVALID_REGION_ID, "TLAB allocating region must have been retired");
        // Move all regions to the sweep list. This tracks all the regions used by the space.
        sweepList.appendAndClear(unavailableRegions);
        sweepList.appendAndClear(allocationRegions);
        sweepList.appendAndClear(tlabAllocationRegions);
        FatalError.check(numRegionsInSpace == sweepList.size(), "incorrect account of regions in space");
        sweepList.sort();
    }

    public void doAfterGC() {
    }

    public void mark(TricolorHeapMarker heapMarker) {
        regionsRangeIterable.reset();
        heapMarker.markAll(regionsRangeIterable);
    }


    public void sweep(TricolorHeapMarker heapMarker, boolean doImprecise) {
        if (MaxineVM.isDebug()) {
            sweepList.checkIsAddressOrdered();
        }
        allocationRegionsFreeSpace = Size.zero();
        csrIsLiveMultiRegionObjectTail = false;
        heapMarker.sweep(this, doImprecise);
        FatalError.check(sweepList.isEmpty(), "Sweeping list must be empty");
    }

    private HeapRegionInfo nextRegionToSweep() {
        return RegionTable.theRegionTable().regionInfo(sweepList.removeHead());
    }

    @Override
    public boolean hasNextSweepingRegion() {
        return !sweepList.isEmpty();
    }

    @Override
    public void beginSweep() {
        resetSweepingRegion(nextRegionToSweep());
    }

    private void traceSweptRegion() {
        Log.print("#");
        Log.print(csrInfo.toRegionID());
        if (csrInfo.hasFreeChunks()) {
            Log.print(csrInfo.isTailOfLargeObject() ? " T" : " ");
            if (csrFreeChunks > 1 || minOverflowRefillSize.greaterThan(csrFreeBytes)) {
                Log.print("A,  nc: ");
                Log.print(csrFreeChunks);
                Log.print(", nb: ");
            } else {
                Log.print("A,  nc: 1, nb: ");
            }
            Log.println(csrFreeBytes);
        } else if (csrInfo.isEmpty()) {
            Log.println("  E");
        } else if (csrInfo.isLarge()) {
            if (LARGE_HEAD.isInState(csrInfo)) {
                Log.println(" H");
            } else if (LARGE_BODY.isInState(csrInfo)) {
                Log.println(" B");
            } else if (LARGE_FULL_TAIL.isInState(csrInfo)) {
                Log.println(" T");
            } else {
                FatalError.unexpected("Unexpected large region state after sweep");
            }
        } else if (csrInfo.isFull()) {
            Log.println("  F");
        } else {
            FatalError.unexpected("Unexpected region state after sweep");
        }
    }

    @Override
    public void endSweep() {
        if (csrIsMultiRegionObjectHead) {
            // Large object regions are at least 2 regions long.
            if (csrFreeBytes == 0) {
                // Large object is live.
                Size largeObjectSize = Layout.size(Layout.cellToOrigin(csrLastLiveAddress.asPointer()));
                csrLastLiveAddress =  csrLastLiveAddress.plus(largeObjectSize);
                csrIsLiveMultiRegionObjectTail = true;
                // Reset the flag
                LARGE_HEAD.setState(csrInfo);
                unavailableRegions.append(csrInfo.toRegionID());
               // Skip all intermediate regions. They are full.
                if (TraceSweep) {
                    traceSweptRegion();
                }
                while (!csrInfo.next().isTailOfLargeObject()) {
                    csrInfo =  nextRegionToSweep();
                    unavailableRegions.append(csrInfo.toRegionID());
                    if (TraceSweep) {
                        traceSweptRegion();
                    }
                }
            } else {
                Size largeObjectSize = Layout.size(Layout.cellToOrigin(csrInfo.regionStart().asPointer()));
                 // Free all intermediate regions. The tail needs to be swept
                // in case it was used for allocating small objects, so we
                // don't free it. It'll be set as the next sweeping region by the next call to beginSweep, so
                // be careful not to consume it from the iterable.
                do {
                    EMPTY_REGION.setState(csrInfo);
                    HeapFreeChunk.format(csrInfo.regionStart(), regionSizeInBytes);
                    allocationRegions.append(csrInfo.toRegionID());
                    allocationRegionsFreeSpace =  allocationRegionsFreeSpace.plus(regionSizeInBytes);
                    if (TraceSweep) {
                        traceSweptRegion();
                    }
                    if (csrInfo.next().isTailOfLargeObject()) {
                        break;
                    }
                    csrInfo = nextRegionToSweep();
                } while (true);
                csrLastLiveAddress = csrInfo.regionStart().plus(regionSizeInBytes);
                // If the large object is dead and its tail isn't large enough to be reclaimable, we must fill it with a dead object to maintain heap parsability.
                Size tailSize = largeObjectSize.and(regionAlignmentMask);
                if (tailSize.lessThan(minReclaimableSpace)) {
                    if (!tailSize.isZero()) {
                        final Pointer tailStart = csrLastLiveAddress.asPointer();
                        HeapSchemeAdaptor.fillWithDeadObject(tailStart, tailStart.plus(tailSize));
                    }
                }
            }
            csrIsMultiRegionObjectHead = false;
        } else {
            if (csrFreeBytes == 0) {
                if (csrIsLiveMultiRegionObjectTail) {
                    // FIXME: is this true if the large object was already dead ?
                    LARGE_FULL_TAIL.setState(csrInfo);
                    csrIsLiveMultiRegionObjectTail = false;
                }  else {
                    FULL_REGION.setState(csrInfo);
                }
                unavailableRegions.append(csrInfo.toRegionID());
            } else {
                if (csrFreeBytes == regionSizeInBytes) {
                    EMPTY_REGION.setState(csrInfo);
                    HeapFreeChunk.format(csrInfo.regionStart(), regionSizeInBytes);
                    allocationRegions.append(csrInfo.toRegionID());
                    allocationRegionsFreeSpace =  allocationRegionsFreeSpace.plus(regionSizeInBytes);
                } else {
                    if (csrIsLiveMultiRegionObjectTail) {
                        LARGE_TAIL.setState(csrInfo);
                        csrIsLiveMultiRegionObjectTail = false;
                    } else {
                        FREE_CHUNKS_REGION.setState(csrInfo);
                    }
                    allocationRegionsFreeSpace =  allocationRegionsFreeSpace.plus(csrFreeBytes);
                    if (csrFreeChunks == 1 && minOverflowRefillSize.lessEqual(csrFreeBytes)) {
                        csrInfo.setFreeChunks(HeapFreeChunk.fromHeapFreeChunk(csrHead), csrFreeBytes,  csrFreeChunks);
                        allocationRegions.append(csrInfo.toRegionID());
                    } else {
                        FatalError.check(csrFreeBytes > 0 && (csrFreeChunks > 1 || minOverflowRefillSize.greaterThan(csrFreeBytes)) && csrHead != null, "unknown state for a swept region");
                        csrInfo.setFreeChunks(HeapFreeChunk.fromHeapFreeChunk(csrHead),  csrFreeBytes, csrFreeChunks);
                        tlabAllocationRegions.append(csrInfo.toRegionID());
                    }
                }
            }
            if (TraceSweep) {
                traceSweptRegion();
            }
        }
    }

    @Override
    public void reachedRightmostLiveRegion() {
        while (hasNextSweepingRegion()) {
            final HeapRegionInfo rinfo = nextRegionToSweep();
            EMPTY_REGION.setState(rinfo);
            HeapFreeChunk.format(rinfo.regionStart(), regionSizeInBytes);
            rinfo.resetOccupancy();
            allocationRegionsFreeSpace =  allocationRegionsFreeSpace.plus(regionSizeInBytes);
            allocationRegions.append(rinfo.toRegionID());
        }
        // Done with sweeping now. Clean state of the sweeper, especially those holding address of free
        // heap chunks (as they may be taken for valid live objects by the next GC!
        // FIXME(ld) should we have some system wide GC epilogue for these type of cleanup ?
        csrHead = null;
        csrTail = null;
    }

    @Override
    public Size freeSpaceAfterSweep() {
        return freeSpace();
    }


    @Override
    public Size growAfterGC(Size delta) {
        int numRegions = delta.roundedUpBy(regionSizeInBytes).unsignedShiftedRight(log2RegionSizeInBytes).toInt();
        if (numRegions == 0) {
            numRegions = 1;
        }
        int allocated = heapAccount.allocate(numRegions, allocationRegions, false, true, true, regionTag);
        return Size.fromInt(allocated).shiftedLeft(log2RegionSizeInBytes);
    }

    @Override
    public Size shrinkAfterGC(Size delta) {
        // TODO
        return Size.zero();
    }

    /**
     * Change state of an allocating region to iterable allocating region.
     *
     * @param allocatingRegion
     */
    private void toIterableAllocatingRegion(BaseAtomicBumpPointerAllocator<?> allocator, int allocatingRegion) {
        if (allocatingRegion == INVALID_REGION_ID) {
            return;
        }
        final HeapRegionInfo rinfo = HeapRegionInfo.fromRegionID(allocatingRegion);
        // Makes the region iterable first.
        allocator.unsafeMakeParsable();
        // Change its state, so that the regionsRangeIterable will include this region in the iterable set.
        HeapRegionState.toIterableAllocatingState(rinfo);
    }

    /**
     * Change state of an allocating region from iterable to non-iterable allocating region.
     * @param allocatingRegion
     */
    private void toAllocatingRegion(int allocatingRegion) {
        if (allocatingRegion != INVALID_REGION_ID) {
            final HeapRegionInfo rinfo = HeapRegionInfo.fromRegionID(allocatingRegion);
            HeapRegionState.toAllocatingState(rinfo);
        }
    }

    private void iterateRegions(HeapSpaceRangeVisitor visitor) {
        final RegionTable regionTable = RegionTable.theRegionTable();
        regionsRangeIterable.initialize(heapAccount.committedRegions());
        if (regionTag == 0) {
            regionsRangeIterable.resetToFirstIterable();
            while (regionsRangeIterable.hasNext()) {
                regionTable.walk(regionsRangeIterable.nextIterableRange(), visitor);
            }
        } else {
            regionsRangeIterable.resetToFirstIterable(regionTag);
            while (regionsRangeIterable.hasNext()) {
                final RegionRange regionsRange = regionsRangeIterable.nextIterableRange(regionTag);
                regionTable.walk(regionsRange, visitor);
            }
        }
    }

    @Override
    public void visit(HeapSpaceRangeVisitor visitor) {
        // Make allocating regions iterable first.
        final int currentTLABAllocatingRegion = tlabAllocator.refillManager().allocatingRegion();
        final int currentOverflowAllocatingRegion = overflowAllocator.refillManager().allocatingRegion();
        toIterableAllocatingRegion(tlabAllocator, currentTLABAllocatingRegion);
        toIterableAllocatingRegion(overflowAllocator, currentOverflowAllocatingRegion);
        iterateRegions(visitor);
        // set allocating region back to allocating state.
        toAllocatingRegion(currentTLABAllocatingRegion);
        toAllocatingRegion(currentOverflowAllocatingRegion);
    }


    private void verifyHeapRegionsBalance() {
        int balance = 0;
        balance += tlabAllocator.refillManager().allocatingRegion() == INVALID_REGION_ID ? 0 : 1;
        // balance += currentOverflowAllocatingRegion == INVALID_REGION_ID ? 0 : 1;
        balance += overflowAllocator.refillManager().allocatingRegion() == INVALID_REGION_ID ? 0 : 1;

        balance += tlabAllocationRegions.size();
        balance += allocationRegions.size();
        balance += unavailableRegions.size();
        FatalError.check(balance == numRegionsInSpace, "incorrect balance of regions in space");
    }

    @Override
    public void verify(AfterMarkSweepVerifier verifier) {
        verifyHeapRegionsBalance();
        tlabAllocationRegions.checkIsAddressOrdered();
        allocationRegions.checkIsAddressOrdered();
        unavailableRegions.checkIsAddressOrdered();
        iterateRegions(verifier);
    }

    public void retireAllocatingRegion(int regionID) {
        // No more free chunks in this region.
        final HeapRegionInfo regionInfo = fromRegionID(regionID);
        if (regionInfo.hasFreeChunks()) {
            allocationRegionsFreeSpace = allocationRegionsFreeSpace.plus(regionInfo.freeBytes());
            tlabAllocationRegions.append(regionID);
        } else {
            unavailableRegions.append(regionID);
        }
    }

    public int getAllocatingRegion() {
        final int regionID = tlabAllocationRegionList().removeHead();
        if (regionID != INVALID_REGION_ID) {
            final HeapRegionInfo regionInfo = fromRegionID(regionID);
            final int numFreeBytes = regionInfo.isEmpty() ?  regionSizeInBytes : regionInfo.freeBytesInChunks();
            allocationRegionsFreeSpace = allocationRegionsFreeSpace.minus(numFreeBytes);
        }
        return regionID;
    }

    public int getAllocatingRegion(Size minFreeBytes, int maxFreeChunks) {
        final int minFreeSpace = minFreeBytes.toInt();
        regionInfoIterable.initialize(allocationRegions);
        regionInfoIterable.reset();
        for (HeapRegionInfo regionInfo : regionInfoIterable) {
            if (regionInfo.isEmpty()) {
                allocationRegionsFreeSpace = allocationRegionsFreeSpace.minus(regionSizeInBytes);
            } else if (regionInfo.freeBytesInChunks() >= minFreeSpace && regionInfo.numFreeChunks() == maxFreeChunks) {
                allocationRegionsFreeSpace = allocationRegionsFreeSpace.minus(regionInfo.freeBytesInChunks());
            } else {
                continue;
            }
            // Found a refill.
            regionInfoIterable.remove();
            return  regionInfo.toRegionID();
        }
        return INVALID_REGION_ID;
    }


    public Size minRetiredFreeChunkSize() {
        return minReclaimableSpace;
    }
}
