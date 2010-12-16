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
import com.sun.max.vm.type.*;


public class FirstFitApplicationHeap extends Sweepable implements HeapAccountOwner, ApplicationHeap {
    /* For simplicity at the moment. Should be able to allocated this in GC's own heap (i.e., the bootstrap allocator).
     */
    private static final OutOfMemoryError outOfMemoryError = new OutOfMemoryError();

    @CONSTANT_WHEN_NOT_ZERO
    private static int NEXT_FREE_CHUNK_OFFSET;

    final HeapAccount<FirstFitApplicationHeap> heapAccount;
    final LinearSpaceAllocator smallObjectSpace;
    final LargeObjectSpace largeObjectSpace;
    final HeapRegionListIterable regionsIterable;

    private Size minReclaimableSpace;
    private HeapRegionList allocatingRegions;
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
     * Refill Manager for the small object space.
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
        smallObjectSpace = new LinearSpaceAllocator(new RefillManager());
        largeObjectSpace = new LargeObjectSpace();
        regionsIterable = new HeapRegionListIterable();
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
    }

    @HOSTED_ONLY
    public void hostInitialize() {
        NEXT_FREE_CHUNK_OFFSET = ClassRegistry.findField(RefillManager.class, "nextFreeChunkInRegion").offset();
        smallObjectSpace.hostInitialize();
    }

    @Override
    public Pointer allocate(Size size) {
        return smallObjectSpace.allocateCleared(size);
    }

    @Override
    public Pointer allocateTLAB(Size size) {
        return smallObjectSpace.allocateTLAB(size);
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
        return null;
    }

    @Override
    public Size freeSpace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Size usedSpace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void makeParsable() {
        smallObjectSpace.makeParsable();
    }

    public void mark(TricolorHeapMarker heapMarker) {
        heapMarker.markAll(regionsIterable, HeapRegionConstants.log2RegionSizeInBytes);
    }

    @Override
    public Pointer processLiveObject(Pointer liveObject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject) {
        // TODO Auto-generated method stub
        return null;
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
        return null;
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
