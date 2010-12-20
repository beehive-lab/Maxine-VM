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
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;

/**
 * A region-based, non-generational, mark-sweep heap.
 * Currently, for testing region-based GC mechanism (tracing, sweeping, region allocation, large object handling).
 *
 *
 *
 * TODO: rename FirstFitMarkSweepHeap.
 * @author Laurent Daynes
 */
public class FirstFitApplicationHeap extends Sweepable implements HeapAccountOwner, ApplicationHeap {
    /* For simplicity at the moment. Should be able to allocated this in GC's own heap (i.e., the bootstrap allocator).
     */
    private static final OutOfMemoryError outOfMemoryError = new OutOfMemoryError();

    final HeapAccount<FirstFitApplicationHeap> heapAccount;
    final MultiChunkTLABAllocator smallObjectAllocator;
    final LinearSpaceAllocator overflowAllocator;
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

    private int overflowRegionID;
    /*
     * The lock on which refill and region allocation to object spaces synchronize on.
     */
    private Object heapLock() {
        return this;
    }

    private void outOfMemory() {
        throw outOfMemoryError;
    }

    private void overflowAllocatorRefill(int regionId, Address chunkStart, Size chunkSize) {
        // Change the overflow allocator's region.
        if (overflowRegionID != INVALID_REGION_ID) {
            fullRegions.append(overflowRegionID);
        }
        allocatingRegions.remove(regionId);
        overflowRegionID = regionId;
        overflowAllocator.refill(chunkStart, chunkSize);
        HeapRegionInfo.fromRegionID(regionId).setAllocating();
    }

    /**
     * Refill the overflow allocator.
     * @param size
     * @return address to a chunk of the requested size, or zero if none requested.
     */
    Address overflowRefillOrAllocate(Size size) {
        final int minFreeWords = size.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
        int firstFitRegion = INVALID_REGION_ID;
        int regionWithLargestSingleChunk = INVALID_REGION_ID;
        int numFreeWords = 0;
        regionInfoIterable.initialize(allocatingRegions);
        regionInfoIterable.reset();
        for (HeapRegionInfo regionInfo : regionInfoIterable) {
            if (regionInfo.isEmpty()) {
                int regionId = regionInfo.toRegionID();
                final Address result = regionInfo.regionStart();
                final Size spaceLeft = Size.fromUnsignedInt(HeapRegionConstants.regionSizeInBytes).minus(size);
                // Refill allocator with the rest.
                overflowAllocatorRefill(regionId, result.plus(size), spaceLeft);
                return result;
            }
            if (regionInfo.freeWords() > numFreeWords && regionInfo.numFreeChunks() == 1) {
                regionWithLargestSingleChunk = regionInfo.toRegionID();
            } else if (firstFitRegion == INVALID_REGION_ID && regionInfo.freeWords() > minFreeWords) {
                firstFitRegion = regionInfo.toRegionID();
            }
        }
        if (regionWithLargestSingleChunk != INVALID_REGION_ID) {
            final HeapRegionInfo regionInfo = HeapRegionInfo.fromRegionID(regionWithLargestSingleChunk);
            Address result = regionInfo.firstFreeBytes();
            Size spaceLeft = HeapFreeChunk.getFreechunkSize(result).minus(size);
            overflowAllocatorRefill(regionWithLargestSingleChunk, result.plus(size), spaceLeft);
            return result;
        }

        // Allocate without refilling.
        return Address.zero();
    }

    class OverflowRefillManager extends LinearSpaceAllocator.RefillManager {

        @Override
        Address allocate(Size size) {
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
     * Handle allocation requests that cannot be addressed by the small object space.
     * Handle only object request, not TLABs.
     * @param size
     * @return
     */
    private Address overflowAllocation(Size size) {
        return overflowAllocator.allocateCleared(size);
    }
    /**
     * Refill Manager for the small object space.
     */
    class RefillManager extends MultiChunkTLABAllocator.RefillManager {
        private Size refillThreshold;
        private Address nextFreeChunkInRegion;
        private int currentRegion;

        /**
         * Space wasted on refill. For statistics only.
         */
        private Size wastedSpace;

        RefillManager() {
            nextFreeChunkInRegion = Address.zero();
        }

        void setRefillPolicy(Size refillThreshold) {
            this.refillThreshold = refillThreshold;
        }

        void resetAfterCollection() {
            currentRegion = allocatingRegions.head();
            final HeapRegionInfo regionInfo = theRegionTable().regionInfo(currentRegion);
            nextFreeChunkInRegion = regionInfo.firstFreeBytes();
            if (MaxineVM.isDebug()) {
                FatalError.check(!nextFreeChunkInRegion.isZero() && theRegionTable().regionID(nextFreeChunkInRegion) == currentRegion,
                                "invalid address of first free chunk");
            }
        }

        Address nextFreeChunkList(Size minSpace) {
            int gcCount = 0;
            // No more free chunk in this region.
            theRegionTable().regionInfo(currentRegion).setFull();
            do {
                currentRegion = allocatingRegions.next(currentRegion);
                if (currentRegion != INVALID_REGION_ID) {
                    final HeapRegionInfo regionInfo = theRegionTable().regionInfo(currentRegion);
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

        /**
         * Request cannot be satisfied with allocator and refill manager doesn't want to refill.
         * Allocate to large or overflow allocator.
         */
        @Override
        Address allocate(Size size) {
            return overflowAllocation(size);
        }

        @Override
        Address allocateTLAB(Size size) {
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
                synchronized (heapLock()) {
                    int gcCount = 0;
                    // No more free chunk in this region.
                    theRegionTable().regionInfo(currentRegion).setFull();
                    do {
                        currentRegion = allocatingRegions.next(currentRegion);
                        if (currentRegion != INVALID_REGION_ID) {
                            final HeapRegionInfo regionInfo = theRegionTable().regionInfo(currentRegion);
                            result = regionInfo.firstFreeBytes();
                            regionInfo.setAllocating();
                            break;
                        }

                        if (MaxineVM.isDebug() && Heap.traceGC()) {
                            gcCount++;
                            final boolean lockDisabledSafepoints = Log.lock();
                            Log.unlock(lockDisabledSafepoints);
                            if (gcCount > 5) {
                                FatalError.unexpected("Suspiscious repeating GC calls detected");
                            }
                        }
                    } while(Heap.collectGarbage(spaceLeft.plus(Word.size())));
                    // Not enough freed memory.
                    outOfMemory();
                }
            }
            nextFreeChunkInRegion = HeapFreeChunk.getFreeChunkNext(result);
            FatalError.check(HeapFreeChunk.getFreechunkSize(result).greaterThan(spaceLeft), "Should not refill with chunk no larger than wastage");
            return result;
        }
    }

    public FirstFitApplicationHeap() {
        heapAccount = new HeapAccount<FirstFitApplicationHeap>(this);
        final RefillManager refillManager = new RefillManager();
        smallObjectAllocator = new MultiChunkTLABAllocator(refillManager);
        overflowAllocator = new MultiChunkTLABAllocator(refillManager);

        largeObjectAllocator = new LargeObjectSpace();
        regionsRangeIterable = new HeapRegionRangeIterable();
        regionInfoIterable = new HeapRegionInfoIterable();
    }

    public HeapAccount<FirstFitApplicationHeap> heapAccount() {
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

