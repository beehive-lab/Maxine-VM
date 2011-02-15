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
import static com.sun.max.vm.heap.gcx.RegionTable.*;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;

/**
 * A region-based, non-generational, mark-sweep heap.
 * Currently, for testing region-based GC mechanism (tracing, sweeping, region allocation, large object handling).
 *
 * @author Laurent Daynes
 */
public class FirstFitMarkSweepHeap extends Sweepable implements HeapAccountOwner, ApplicationHeap {
    /* For simplicity at the moment. Should be able to allocated this in GC's own heap (i.e., the bootstrap allocator).
     */
    private static final OutOfMemoryError outOfMemoryError = new OutOfMemoryError();

    final HeapAccount<FirstFitMarkSweepHeap> heapAccount;
    final MultiChunkTLABAllocator tlabAllocator;
    final LargeObjectSpace largeObjectAllocator;

    /**
     * Support for iterating over contiguous region ranges from a list.
     */
    final HeapRegionRangeIterable regionsRangeIterable;

    /**
     * Support for iterating over region info of regions from a list.
     */
    final HeapRegionInfoIterable regionInfoIterable;

    private Size minReclaimableSpace;

    /**
     * List of region with space available for allocation.
     * Used to refill the small object allocator and the overflow allocator.
     */
    private HeapRegionList allocatingRegions;

    /**
     * List of region with no space available for allocation.
     */
    private HeapRegionList fullRegions;

    /*
     * The lock on which refill and region allocation to object spaces synchronize on.
     */
    private Object heapLock() {
        return this;
    }

    private void outOfMemory() {
        throw outOfMemoryError;
    }

    /**
     * Simple allocator used to handle request to mid-size objects that are larger than the
     * refill threshold of the tlab allocator.
     * Calls to the allocator are always made under protection of the main allocator's refill lock.
     * So we don't bother with any clever non-blocking synchronization mechanisms here.
     */
    class OverflowAllocator {
        private Address top;
        private Address start;
        private Address end;
        private int overflowRegionID;
        private int overflowRefillCount;
        /**
         * Min space to request if triggering GC.
         */
        private Size minSpaceAfterGC;

        OverflowAllocator(Size minSpaceAfterGC) {
            top = Address.zero();
            start = Address.zero();
            end = Address.zero();
            overflowRegionID = INVALID_REGION_ID;
            overflowRefillCount = 0;
            this.minSpaceAfterGC = minSpaceAfterGC;
        }

        private void overflowAllocatorRefill(int regionId, Address chunkStart, Size chunkSize) {
            // Change the overflow allocator's region.
            if (overflowRegionID != INVALID_REGION_ID) {
                // Don't bother with re-using the space.
                HeapSchemeAdaptor.fillWithDeadObject(top.asPointer(), end.asPointer());
                fullRegions.append(overflowRegionID);
            }
            allocatingRegions.remove(regionId);
            overflowRegionID = regionId;
            start = chunkStart;
            top = chunkStart;
            end = chunkStart.plus(chunkSize);
            HeapRegionInfo.fromRegionID(regionId).setAllocating();
            overflowRefillCount++;
        }

        Pointer allocateCleared(Size size) {
            Pointer cell = top.asPointer();
            if (cell.plus(size).greaterThan(end)) {
                overflowRefill(this, size);
                cell = top.asPointer();
            }
            top = top.plus(size);
            Memory.clearWords(cell, size.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt());
            return cell;
        }
    }

    /**
     * Refill Manager for the small object space.
     */
    class RefillManager extends MultiChunkTLABAllocator.RefillManager {
        /**
         * Threshold below which the allocator should be refilled.
         */
        private Size refillThreshold;

        private Address nextFreeChunkInRegion;
        private OverflowAllocator overflowAllocator;

        /**
         * Space wasted on refill. For statistics only.
         */
        private Size wastedSpace;

        RefillManager(OverflowAllocator overflowAllocator) {
            nextFreeChunkInRegion = Address.zero();
            this.overflowAllocator = overflowAllocator;
        }

        void setRefillPolicy(Size refillThreshold) {
            this.refillThreshold = refillThreshold;
        }

        /**
         * Request cannot be satisfied with allocator and refill manager doesn't want to refill.
         * Allocate to large or overflow allocator.
         */
        @Override
        Address allocate(Size size) {
            return overflowAllocator.allocateCleared(size);
        }

        /**
         *
         */
        @Override
        Address allocateTLAB(Size tlabSize, Pointer leftover, Size leftoverSize) {
            Address firstChunk = tlabChunkOrZero(leftover, leftoverSize);
            if (!firstChunk.isZero()) {
                tlabSize = tlabSize.minus(leftoverSize);
                if (tlabSize.lessThan(tlabMinChunkSize)) {
                    // don't bother with it. Just return.
                    return firstChunk;
                }
            }

            return firstChunk;
        }


        @Override
        boolean shouldRefill(Size requestedSpace, Size spaceLeft) {
            // Should refill only if we're not going to waste too much space and
            // the refill will succeed (we assume it will if switching regions).
            return spaceLeft.lessThan(refillThreshold) && (nextFreeChunkInRegion.isZero() ||
                            requestedSpace.lessThan(HeapFreeChunk.getFreechunkSize(nextFreeChunkInRegion)));
        }

        /**
         * Refill the linear space allocator. Note that this one already guarantees that only one
         * thread can enter this method.
         */
        @Override
        Address refill(Pointer startOfSpaceLeft, Size spaceLeft) {
            FatalError.check(spaceLeft.lessThan(refillThreshold), "Should not refill before threshold is reached");
            // First, make the space left parsable, then change of allocating regions.
            if (spaceLeft.greaterThan(0)) {
                wastedSpace = wastedSpace.plus(spaceLeft);
                final Pointer endOfSpaceLeft = startOfSpaceLeft.plus(spaceLeft);
                if (MaxineVM.isDebug()) {
                    FatalError.check(regionStart(endOfSpaceLeft).lessEqual(startOfSpaceLeft),
                                    "space left must be in the same regions");
                }
                HeapSchemeAdaptor.fillWithDeadObject(startOfSpaceLeft, endOfSpaceLeft);
            }
            Address result = nextFreeChunkInRegion;
            if (result.isZero()) {
                HeapRegionInfo regionInfo = changeAllocatingRegion();
                FatalError.check(regionInfo != null, "must never be null");
                result = regionInfo.firstFreeBytes();
            }
            nextFreeChunkInRegion = HeapFreeChunk.getFreeChunkNext(result);
            FatalError.check(HeapFreeChunk.getFreechunkSize(result).greaterThan(spaceLeft),
                            "Should not refill with chunk no larger than wastage");
            return result;
        }


        @Override
        void makeParsable(Pointer start, Pointer end) {
            if (MaxineVM.isDebug()) {
                FatalError.check(regionStart(end).lessEqual(start), "space left must be in the same regions");
            }
            HeapSchemeAdaptor.fillWithDeadObject(start, end);
            theRegionTable().regionInfo(start).setIterable();
        }
    }

    /**
     * Region currently used for allocation.
     */
    private int currentAllocatingRegion;

    HeapRegionInfo changeAllocatingRegion() {
        synchronized (heapLock()) {
            int gcCount = 0;
            // No more free chunk in this region.
            theRegionTable().regionInfo(currentAllocatingRegion).setFull();
            do {
                currentAllocatingRegion = allocatingRegions.next(currentAllocatingRegion);
                if (currentAllocatingRegion != INVALID_REGION_ID) {
                    final HeapRegionInfo regionInfo = theRegionTable().regionInfo(currentAllocatingRegion);
                    regionInfo.setAllocating();
                    return regionInfo;
                }

                if (MaxineVM.isDebug() && Heap.traceGC()) {
                    gcCount++;
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.unlock(lockDisabledSafepoints);
                    if (gcCount > 5) {
                        FatalError.unexpected("Suspiscious repeating GC calls detected");
                    }
                }
            } while(Heap.collectGarbage(Size.fromInt(regionSizeInBytes))); // Always collect for at least one region.
            // Not enough freed memory.
            throw outOfMemoryError;
        }
    }

    /**
     * Try to refill the overflow allocator with a single continuous chunk. Runs GC if can't.
     * @param minRefillSize minimum amount of space to refill the allocator with
     * @return address to a chunk of the requested size, or zero if none requested.
     */
    void overflowRefill(OverflowAllocator overflowAllocator, Size minRefillSize) {
        final int minFreeWords = minRefillSize.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
        int gcCount = 0;
        synchronized (heapLock()) {
            do {
                int regionWithLargestSingleChunk = INVALID_REGION_ID;
                int numFreeWords = minFreeWords - 1;
                regionInfoIterable.initialize(allocatingRegions);
                regionInfoIterable.reset();
                for (HeapRegionInfo regionInfo : regionInfoIterable) {
                    if (regionInfo.isEmpty()) {
                        int regionId = regionInfo.toRegionID();
                        // Refill allocator with the rest.
                        overflowAllocator.overflowAllocatorRefill(regionId, regionInfo.regionStart(), Size.fromInt(HeapRegionConstants.regionSizeInBytes));
                        return;
                    }
                    if (regionInfo.freeWords() > numFreeWords && regionInfo.numFreeChunks() == 1) {
                        regionWithLargestSingleChunk = regionInfo.toRegionID();
                        numFreeWords = regionInfo.freeWords();
                    }
                }
                if (regionWithLargestSingleChunk != INVALID_REGION_ID) {
                    final HeapRegionInfo regionInfo = HeapRegionInfo.fromRegionID(regionWithLargestSingleChunk);
                    Address firstChunk = regionInfo.firstFreeBytes();
                    Size chunkSize = HeapFreeChunk.getFreechunkSize(firstChunk);
                    overflowAllocator.overflowAllocatorRefill(regionWithLargestSingleChunk, firstChunk, chunkSize);
                    return;
                }

                if (MaxineVM.isDebug() && Heap.traceGC()) {
                    gcCount++;
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.unlock(lockDisabledSafepoints);
                    if (gcCount > 5) {
                        FatalError.unexpected("Suspiscious repeating GC calls detected");
                    }
                }
            } while(Heap.collectGarbage(minRefillSize));
        }
    }

    public FirstFitMarkSweepHeap() {
        heapAccount = new HeapAccount<FirstFitMarkSweepHeap>(this);
        Size overflowAllocatorMinSpaceAfterGC = Size.fromInt(regionSizeInBytes).dividedBy(2);
        final RefillManager refillManager = new RefillManager(new OverflowAllocator(overflowAllocatorMinSpaceAfterGC));
        tlabAllocator = new MultiChunkTLABAllocator(refillManager);
        largeObjectAllocator = new LargeObjectSpace();
        regionsRangeIterable = new HeapRegionRangeIterable();
        regionInfoIterable = new HeapRegionInfoIterable();
    }

    public HeapAccount<FirstFitMarkSweepHeap> heapAccount() {
        return heapAccount;
    }

    public void initialize(Size minSize, Size maxSize) {
        if (!heapAccount.open(numberOfRegions(maxSize))) {
            FatalError.unexpected("Failed to create application heap");
        }
        allocatingRegions = HeapRegionList.RegionListUse.OWNERSHIP.createList();
        fullRegions = HeapRegionList.RegionListUse.OWNERSHIP.createList();
        heapAccount.allocate(numberOfRegions(minSize), allocatingRegions, true);
        minReclaimableSpace = Size.fromInt(Sweepable.freeChunkMinSizeOption.getValue());
        // Set the iterable to the allocating regions. This is the default. Any exception to this should
        // reset to the allocating region list when done.
        regionsRangeIterable.initialize(allocatingRegions);

        // Initialize the tlab allocator with the first contiguous range.
        regionsRangeIterable.reset();
        Size regionSize = Size.fromInt(regionSizeInBytes);
        ((MultiChunkTLABAllocator.RefillManager) tlabAllocator.refillManager).setMinTLABChunkSize(minReclaimableSpace);

        // Initialize TLAB allocator with first region.
        final HeapRegionInfo regionInfo = HeapRegionInfo.fromRegionID(allocatingRegions.head());
        regionInfo.setAllocating();
        tlabAllocator.initialize(regionInfo.firstFreeBytes(), regionSize, regionSize, HeapSchemeAdaptor.MIN_OBJECT_SIZE);
    }

    /**
     * Entry point for direct allocation by HeapScheme using this heap.
     */
    @Override
    public Pointer allocate(Size size) {
        return tlabAllocator.allocateCleared(size);
    }

    /**
     * Entry point for direct TLAB allocation by HeapScheme using this heap.
     */
    @Override
    public Pointer allocateTLAB(Size size) {
        return tlabAllocator.allocateTLAB(size);
    }

    @Override
    public boolean contains(Address address) {
        return RegionTable.theRegionTable().regionInfo(address).owner() == this;
    }

    @Override
    public boolean canSatisfyAllocation(Size size) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Size totalSpace() {
        // TODO Auto-generated method stub
        return Size.zero();
    }

    @Override
    public Size freeSpace() {
        // TODO Auto-generated method stub
        return Size.zero();
    }

    @Override
    public Size usedSpace() {
        // TODO Auto-generated method stub
        return Size.zero();
    }

    @Override
    public void makeParsable() {
        tlabAllocator.makeParsable();
    }

    public void mark(TricolorHeapMarker heapMarker) {
        // All regions must be in the full list, sorted
        regionsRangeIterable.initialize(fullRegions);
        regionsRangeIterable.reset();
        heapMarker.markAll(regionsRangeIterable, HeapRegionConstants.log2RegionSizeInBytes);
    }

    @Override
    public Pointer processLiveObject(Pointer liveObject) {
        // TODO Auto-generated method stub
        return Pointer.zero();
    }

    @Override
    public Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject) {
        // TODO Auto-generated method stub
        return Pointer.zero();
    }

    @Override
    public void processDeadSpace(Address freeChunk, Size size) {
        // TODO Auto-generated method stub

    }

    @Override
    public Size beginSweep(boolean precise) {
        // make all region empty again.

        return minReclaimableSpace;
    }

    @Override
    public Size endSweep() {
        // TODO Auto-generated method stub
        return Size.zero();
    }

    @Override
    public void verify(AfterMarkSweepVerifier verifier) {
        // TODO Auto-generated method stub

    }

    public Size sweep(TricolorHeapMarker heapMarker, boolean doImpreciseSweep) {
        // TODO: what about large object space ?
        Size minReclaimableSpace = beginSweep(doImpreciseSweep);

        if (doImpreciseSweep) {
            heapMarker.impreciseSweep(this, minReclaimableSpace);
        } else {
            heapMarker.sweep(this);
        }

        return endSweep();
    }
}

