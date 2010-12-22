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
import static com.sun.max.vm.heap.gcx.RegionTable.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;

/**
 * A region-based, non-generational, mark-sweep heap.
 * Currently, for testing region-based GC mechanism (tracing, sweeping, region allocation, large object handling).
 *
 *
 * @author Laurent Daynes
 */
public class FirstFitMarkSweepHeap extends Sweepable implements HeapAccountOwner, ApplicationHeap {
    /* For simplicity at the moment. Should be able to allocated this in GC's own heap (i.e., the bootstrap allocator).
     */
    private static final OutOfMemoryError outOfMemoryError = new OutOfMemoryError();

    final HeapAccount<FirstFitMarkSweepHeap> heapAccount;
    final MultiChunkTLABAllocator smallObjectAllocator;
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

        Pointer allocate(Size size) {
            Pointer cell = top.asPointer();
            if (cell.plus(size).greaterThan(end)) {
                return overflowRefillOrAllocate(this, size).asPointer();
            }
            top = top.plus(size);
            return cell;
        }

        Pointer allocateCleared(Size size) {
            Pointer cell = allocate(size);
            Memory.clearWords(cell, size.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt());
            return cell;
        }

    }
    /**
     * Refill manager for the overflow allocator.
     * Only refill with a single chunk. Never used for TLAB allocation.
     *
     */
    class OverflowRefillManager extends LinearSpaceAllocator.RefillManager {

        @Override
        Address allocate(Size size) {
            FatalError.unexpected("Must never call this on the overflow refill manager");
            return Address.zero();
        }

        @Override
        boolean shouldRefill(Size requestedSpace, Size spaceLeft) {
            // Always refill overflow allocator
            return true;
        }

        @Override
        Address refill(LinearSpaceAllocator allocator, Pointer startOfSpaceLeft, Size spaceLeft) {
            // TODO Auto-generated method stub
            return Address.zero();
        }
    }

    /**
     * Refill Manager for the small object space.
     */
    class RefillManager extends MultiChunkTLABAllocator.RefillManager {
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

        @Override
        Address allocateTLAB(Size size, Pointer leftover, Size leftoverSize) {
            // TODO Auto-generated method stub
            return Address.zero();
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
        Address refill(LinearSpaceAllocator allocator, Pointer startOfSpaceLeft, Size spaceLeft) {
            FatalError.check(spaceLeft.lessThan(refillThreshold), "Should not refill before threshold is reached");
            // First, make the space left parsable, then change of allocating regions.
            if (spaceLeft.greaterThan(0)) {
                wastedSpace = wastedSpace.plus(spaceLeft);
                HeapSchemeAdaptor.fillWithDeadObject(startOfSpaceLeft, startOfSpaceLeft.plus(spaceLeft));
            }
            Address result = nextFreeChunkInRegion;
            if (result.isZero()) {
                // The space do we ask for a possible GC to free up if running out of space.
                // This is arbitrary !!!! We should really pass to this refill the size that caused the refill to make sure the refill will satisfy it.
                Size minSpace = spaceLeft.times(2);
                result = changeAllocatingRegion(minSpace);
                FatalError.check(!result.isZero(), "must never return 0");
            }
            nextFreeChunkInRegion = HeapFreeChunk.getFreeChunkNext(result);
            FatalError.check(HeapFreeChunk.getFreechunkSize(result).greaterThan(spaceLeft), "Should not refill with chunk no larger than wastage");
            return result;
        }
    }

    /**
     * Region currently used for allocation.
     */
    private int currentAllocatingRegion;

    Address changeAllocatingRegion(Size minSpace) {
        synchronized (heapLock()) {
            int gcCount = 0;
            // No more free chunk in this region.
            theRegionTable().regionInfo(currentAllocatingRegion).setFull();
            do {
                currentAllocatingRegion = allocatingRegions.next(currentAllocatingRegion);
                if (currentAllocatingRegion != INVALID_REGION_ID) {
                    final HeapRegionInfo regionInfo = theRegionTable().regionInfo(currentAllocatingRegion);
                    final Address result = regionInfo.firstFreeBytes();
                    regionInfo.setAllocating();
                    return result;
                }

                if (MaxineVM.isDebug() && Heap.traceGC()) {
                    gcCount++;
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.unlock(lockDisabledSafepoints);
                    if (gcCount > 5) {
                        FatalError.unexpected("Suspiscious repeating GC calls detected");
                    }
                }
            } while(Heap.collectGarbage(minSpace));
            // Not enough freed memory.
            throw outOfMemoryError;
        }
    }

    /**
     * Try to refill the overflow allocator with a single continuous chunk. Runs GC if can't.
     * @param size
     * @return address to a chunk of the requested size, or zero if none requested.
     */
    Address overflowRefillOrAllocate(OverflowAllocator overflowAllocator, Size size) {
        final int minFreeWords = size.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
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
                        final Address result = regionInfo.regionStart();
                        final Size spaceLeft = Size.fromUnsignedInt(HeapRegionConstants.regionSizeInBytes).minus(size);
                        // Refill allocator with the rest.
                        overflowAllocator.overflowAllocatorRefill(regionId, result.plus(size), spaceLeft);
                        return result;
                    }
                    if (regionInfo.freeWords() > numFreeWords && regionInfo.numFreeChunks() == 1) {
                        regionWithLargestSingleChunk = regionInfo.toRegionID();
                        numFreeWords = regionInfo.freeWords();
                    }
                }
                if (regionWithLargestSingleChunk != INVALID_REGION_ID) {
                    final HeapRegionInfo regionInfo = HeapRegionInfo.fromRegionID(regionWithLargestSingleChunk);
                    Address result = regionInfo.firstFreeBytes();
                    Size spaceLeft = HeapFreeChunk.getFreechunkSize(result).minus(size);
                    overflowAllocator.overflowAllocatorRefill(regionWithLargestSingleChunk, result.plus(size), spaceLeft);
                    return result;
                }

                if (MaxineVM.isDebug() && Heap.traceGC()) {
                    gcCount++;
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.unlock(lockDisabledSafepoints);
                    if (gcCount > 5) {
                        FatalError.unexpected("Suspiscious repeating GC calls detected");
                    }
                }
            } while(Heap.collectGarbage(size));
            return Address.zero();
        }
    }

    public FirstFitMarkSweepHeap() {
        heapAccount = new HeapAccount<FirstFitMarkSweepHeap>(this);
        Size overflowAllocatorMinSpaceAfterGC = Size.fromInt(regionSizeInBytes).dividedBy(2);
        final RefillManager refillManager = new RefillManager(new OverflowAllocator(overflowAllocatorMinSpaceAfterGC));
        smallObjectAllocator = new MultiChunkTLABAllocator(refillManager);
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

        //smallObjectSpace.initialize(start, initSize, minLargeObjectSize, HeapSchemeAdaptor.MIN_OBJECT_SIZE, minReclaimableSpace);
        // Set the iterable to the allocating regions. This is the default. Any exception to this should
        // reset to the allocating region list when done.
        regionsRangeIterable.initialize(allocatingRegions);
    }

    @HOSTED_ONLY
    public void hostInitialize() {
        smallObjectAllocator.hostInitialize();
    }

    /**
     * Entry point for direct allocation by HeapScheme using this heap.
     */
    @Override
    public Pointer allocate(Size size) {
        return smallObjectAllocator.allocateCleared(size);
    }

    /**
     * Entry point for direct TLAB allocation by HeapScheme using this heap.
     */
    @Override
    public Pointer allocateTLAB(Size size) {
        return smallObjectAllocator.allocateTLAB(size);
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
        smallObjectAllocator.makeParsable();
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

