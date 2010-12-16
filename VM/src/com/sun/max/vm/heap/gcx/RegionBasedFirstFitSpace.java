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
 * A region-based space supporting the sweeping interface.
 * The space is made of an number of possibly non-contiguous
 * fixed-size regions. Each region maintains a list of free chunks
 * of minimum size, and occupancy statistics.
 * Both the list and the occupancy statistics are filled during sweeping.
 * Free chunks are organized in address order; regions with free space are organized in
 * address order as well.
 *
 * Allocation is performed on a first-fit basis.
 *
 * @author Laurent Daynes
 */
public class RegionBasedFirstFitSpace extends Sweepable implements ResizableSpace {
    /**
     * Regions available for allocation.
     * Ordered from low to high addresses.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private HeapRegionList allocatingRegions;
    /**
     * Small object allocator.
     */
    private final LinearSpaceAllocator smallObjectAllocator;

    /**
     * Minimum size to be considered reclaimable.
     */
    private Size minReclaimableSpace;

    /* For simplicity at the moment. Should be able to allocated this in GC's own heap (i.e., the bootstrap allocator).
     */
    private static final OutOfMemoryError outOfMemoryError = new OutOfMemoryError();

    RegionBasedFirstFitSpace regionProvider() {
        return this;
    }

    public Pointer allocate(Size size) {
        return smallObjectAllocator.allocateCleared(size);
    }

    public Pointer allocateTLAB(Size size) {
        return smallObjectAllocator.allocateTLAB(size);
    }

    public boolean contains(Address address) {
        return false;
    }

    public boolean canSatisfyAllocation(Size size) {
        // TODO Auto-generated method stub
        return false;
    }

    public void makeParsable() {
        smallObjectAllocator.makeParsable();
    }

    /**
     * Refill Manager for the small object allocator.
     */
    class RefillManager extends LinearSpaceAllocator.RefillManager {
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
        /**
         * Request cannot be satisfied with allocator and refill manager doesn't want to refill.
         * Allocate to large to overflow allocator.
         */
        @Override
        Address allocate(Size size) {
            // TODO Auto-generated method stub
            return Address.zero();
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

        @Override
        Address refill(LinearSpaceAllocator allocator, Pointer startOfSpaceLeft, Size spaceLeft) {
            FatalError.check(spaceLeft.lessThan(refillThreshold), "Should not refill before threshold is reached");
            // First, make the space left parsable, then change of allocating regions.
            if (spaceLeft.greaterThan(0)) {
                wastedSpace = wastedSpace.plus(spaceLeft);
                HeapSchemeAdaptor.fillWithDeadObject(startOfSpaceLeft, startOfSpaceLeft.plus(spaceLeft));
            }
            synchronized (regionProvider()) {
                Address result = nextFreeChunkInRegion;
                if (result.isZero()) {
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
                    throw RegionBasedFirstFitSpace.outOfMemoryError;
                }
                nextFreeChunkInRegion = HeapFreeChunk.getFreeChunkNext(result);
                FatalError.check(HeapFreeChunk.getFreechunkSize(result).greaterThan(spaceLeft), "Should not refill with chunk no larger than wastage");
                return result;
            }
        }
    }

    public RegionBasedFirstFitSpace() {
        smallObjectAllocator = new LinearSpaceAllocator(null);
    }

    @HOSTED_ONLY
    public void hostInitialize() {
        //smallObjectAllocator.hostInitialize();
    }

    @Override
    public Size beginSweep(boolean precise) {
        return minReclaimableSpace;
    }

    @Override
    public Size endSweep() {
        return Size.zero();
    }

    @Override
    public void processDeadSpace(Address freeChunk, Size size) {
    }

    @Override
    public Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject) {
        // TODO
        return Pointer.zero();
    }

    @Override
    public Pointer processLiveObject(Pointer liveObject) {
        // TODO
        return Pointer.zero();
    }

    @Override
    public Size growAfterGC(Size delta) {
        // TODO
        return Size.zero();
    }

    @Override
    public Size shrinkAfterGC(Size delta) {
        // TODO
        return Size.zero();
    }

    @Override
    public void verify(AfterMarkSweepVerifier verifier) {
        // TODO Auto-generated method stub

    }

}
