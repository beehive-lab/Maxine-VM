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
import static com.sun.max.vm.heap.gcx.RegionTable.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.rset.*;
import com.sun.max.vm.runtime.*;

/**
 * An extension of the ChunkListRefillManager where chunks are allocated out of a heap region reserved for the refill manager.
 * Region reservation is made via a region provider interface. The provider guarantees exclusive usage of the free space of the region to the
 * refill manager. The reserved region may either be empty, or with a non-empty list of free chunks.
 * When the region's free space is exhausted, the refill manager retires the region (i.e., gives it back to the provider) and request a new one.
 */
public final class RegionChunkListRefillManager extends ChunkListRefillManager {
    /**
     * Region providing space for refilling allocator.
     */
    private int allocatingRegion;

    /**
     * Provider of regions.
     */
    private RegionProvider regionProvider;
    /**
     * Dead space listener where to report dead space events.
     */
    protected final DeadSpaceListener deadSpaceListener;

   /**
     * Threshold below which the allocator should be refilled.
     */
    private Size refillThreshold;

    /**
     * Head of list of free chunks in the current allocating region.
     */
    private Address nextFreeChunkInRegion;

    /**
     * Free space left in the region. This doesn't count the free space in the allocator this refill manager serves.
     */
    private Size freeSpace;

    /**
     * Space wasted on refill. For statistics only.
     */
    private Size wastedSpace;

    private static final OutOfMemoryError outOfMemoryError = new OutOfMemoryError();

    public void setRegionProvider(RegionProvider regionProvider) {
        this.regionProvider = regionProvider;
    }

    public Object refillLock() {
        return regionProvider;
    }

    public int allocatingRegion() {
        return allocatingRegion;
    }

    public RegionChunkListRefillManager() {
        this(NullDeadSpaceListener.nullDeadSpaceListener());
    }

    public RegionChunkListRefillManager(DeadSpaceListener deadSpaceListener) {
        this.deadSpaceListener = deadSpaceListener;
        nextFreeChunkInRegion = Address.zero();
        allocatingRegion = INVALID_REGION_ID;
    }

    void setRefillPolicy(Size refillThreshold) {
        this.refillThreshold = refillThreshold;
    }

    static private void checkForSuspisciousGC(int gcCount) {
        if (gcCount > 1) {
            FatalError.breakpoint();
        }
        if (gcCount > 5) {
            FatalError.unexpected("Suspiscious repeating GC calls detected");
        }
    }

    private void  retireCurrentAllocatingRegion() {
        final int regionID = allocatingRegion;
        if (regionID != INVALID_REGION_ID) {
            allocatingRegion = INVALID_REGION_ID;
            toFullState(fromRegionID(regionID));
            regionProvider.retireAllocatingRegion(regionID);
            if (MaxineVM.isDebug() && regionID == DebuggedRegion) {
                // Not very precise: even if we retire the log, there might still be some Thread using a TLAB allocated from that region.
                TLABLog.TraceTLABAllocation = false;
            }
        }
    }

    private HeapRegionInfo changeAllocatingRegion() {
        synchronized (refillLock()) {
            int gcCount = 0;
            retireCurrentAllocatingRegion();
            do {
                allocatingRegion = regionProvider.getAllocatingRegion();
                if (allocatingRegion != INVALID_REGION_ID) {
                    if (allocatingRegion == DebuggedRegion) {
                        TLABLog.TraceTLABAllocation = true;
                    }
                    return  fromRegionID(allocatingRegion);
                }
                if (MaxineVM.isDebug()) {
                    checkForSuspisciousGC(gcCount++);
                }
            } while(Heap.collectGarbage(Size.fromInt(regionSizeInBytes))); // Always collect for at least one region.
            // Not enough freed memory.
            throw outOfMemoryError;
        }
    }

    // FIXME: The following two are inherited from the ChunkListRefillManager used for the ChunkListAllocator.
    // These aren't needed for the logic here where allocation is always dispatched to three independent allocators:
    // large, overflow, and tlab. The latter never comes across a large or overflow situation,  and therefore never call these.
    // What should be done is a variant of the ChunkListAllocator that extend the BaseBumpPointer allocator and override
    // the refill allocate. We currently leave this as is because doing this change also requires changing the FreeHeapSpaceManager of the MSHeapScheme

    @Override
    public Address allocateLargeRaw(Size size) {
        FatalError.unexpected("Should not reach here");
        return Address.zero();
    }

    /**
     * Request cannot be satisfied with allocator and refill manager doesn't want to refill.
     * Allocate to large or overflow allocator.
     */
    @Override
    public Address allocateOverflow(Size size) {
        FatalError.unexpected("Should not reach here");
        return Address.zero();
    }

    /**
     * Allocate a chunk list for a TLAB. This is an overflow situation of the current chunk of the tlab allocator.
     * We must be holding the refillLock of that allocator.
     */
    @Override
    @NO_SAFEPOINT_POLLS("tlab allocation loop must not be subjected to safepoints")
    public Address allocateChunkListOrRefill(ChunkListAllocator<? extends ChunkListRefillManager> allocator, Size tlabSize, Pointer leftover, Size leftoverSize) {
        Address firstChunk = retireChunk(leftover, leftoverSize);
        if (!firstChunk.isZero()) {
            tlabSize = tlabSize.minus(leftoverSize);
            if (tlabSize.lessThan(minChunkSize)) {
                // don't bother with it. Just return.
                return firstChunk;
            }
        }
        if (nextFreeChunkInRegion.isZero()) {
            if (!freeSpace.isZero()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Region #");
                Log.print(allocatingRegion);
                Log.print(" has ");
                Log.print(freeSpace.toInt());
                Log.println(" free space but not free chunk!");
                Log.unlock(lockDisabledSafepoints);
                FatalError.unexpected("must not have any free space");
            }
            if (!firstChunk.isZero()) {
                // Return what we have for now as changeAllocatingRegion can cause a GC.
                return firstChunk;
            }
            if (MaxineVM.isDebug() && allocatingRegion == DebuggedRegion) {
                allocator.debugTrace = false;
                HeapSchemeWithTLAB.setTraceTLAB(false);
            }
            // We're not under the heap lock anymore here, although we're protected with the refill lock.
            // Is it ok to change the heap region info in these conditions ?
            HeapRegionInfo regionInfo = changeAllocatingRegion();
            if (MaxineVM.isDebug() && allocatingRegion == DebuggedRegion) {
                allocator.debugTrace = true;
                HeapSchemeWithTLAB.setTraceTLAB(true);
            }
            // Still protected by the refill lock of the alllocator.
            FatalError.check(regionInfo != null, "must never be null");
            Address firstFreeBytes = regionInfo.firstFreeBytes();
            if (regionInfo.hasFreeChunks()) {
                freeSpace = Size.fromInt(regionInfo.freeBytesInChunks());
                nextFreeChunkInRegion = firstFreeBytes;
                regionInfo.clearFreeChunks();
                toAllocatingState(regionInfo);
            } else {
                FatalError.check(!firstFreeBytes.isZero() && regionInfo.isEmpty(), "must never be null");
                // It's an empty region.
                // Refill the allocator with the whole region.
                freeSpace = Size.zero();
                nextFreeChunkInRegion = Address.zero();
                Size refillSize = Size.fromInt(regionSizeInBytes);
                toAllocatingState(regionInfo);
                allocator.refill(firstFreeBytes, refillSize);
                return Address.zero(); // indicates that allocator was refilled.
            }
        }
        // FIXME: revisit this. We want to refill the allocator if the next chunk is much larger than the requested space.
        Address result = Address.zero();
        if (freeSpace.lessEqual(tlabSize)) {
            result = nextFreeChunkInRegion;
            nextFreeChunkInRegion = Address.zero();
            freeSpace = Size.zero();
        } else {
            // Grab enough chunks to satisfy TLAB refill
            Size allocatedSize = tlabSize;   // remember how much space was initially requested.
            Size spaceNeeded = tlabSize;  // space left to allocate
            Address lastChunk = Address.zero();
            Address chunk = nextFreeChunkInRegion.asPointer();
            do {
                Size chunkSize = HeapFreeChunk.getFreechunkSize(chunk);
                if (chunkSize.greaterEqual(spaceNeeded)) {
                    if (spaceNeeded.lessThan(minChunkSize)) {
                        // Adjust last chunk size. Can't be smaller than min chunk size (TLAB invariant)
                        allocatedSize = allocatedSize.plus(minChunkSize.minus(spaceNeeded));
                        spaceNeeded = minChunkSize;
                    }
                    Address next = HeapFreeChunk.getFreeChunkNext(chunk);
                    // Split if leftover larger that min tlab size.
                    Size chunkLeftover = chunkSize.minus(spaceNeeded);
                    if (chunkLeftover.greaterEqual(minChunkSize)) {
                        lastChunk = HeapFreeChunk.splitRight(chunk, spaceNeeded, next);
                        deadSpaceListener.notifySplitDead(chunk, spaceNeeded, chunk.plus(chunkSize));
                    } else {
                        lastChunk = next;
                        // Adjust allocated size, to keep accounting correct.
                        allocatedSize = allocatedSize.plus(chunkLeftover);
                    }
                    HeapFreeChunk.setFreeChunkNext(chunk, Address.zero());
                    break;
                }
                spaceNeeded = spaceNeeded.minus(chunkSize);
                chunk = HeapFreeChunk.getFreeChunkNext(chunk);
            } while(!chunk.isZero());
            result = nextFreeChunkInRegion;
            if (!(!lastChunk.isZero() || freeSpace.equals(allocatedSize))) {
                Log.print("lastChunk =");
                Log.print(lastChunk);
                Log.print(", freeSpace = ");
                Log.print(freeSpace.toLong());
                Log.print(", allocatedSize = ");
                Log.println(allocatedSize.toLong());
                FatalError.check(!lastChunk.isZero() || freeSpace.equals(allocatedSize), "must not have free space if no chunk left");
            }
            nextFreeChunkInRegion = lastChunk;
            freeSpace = freeSpace.minus(allocatedSize);
        }
        if (!firstChunk.isZero()) {
            HeapFreeChunk.setFreeChunkNext(firstChunk, result);
            result = firstChunk;
        }
        return result;
    }

    @Override
    public boolean shouldRefill(Size requestedSpace, Size spaceLeft) {
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
    public Address allocateRefill(Pointer startOfSpaceLeft, Size spaceLeft) {
        // FIXME: see comment above. We should never reach here as request for refilling the allocator can only happen via the allocateCleared call, which
        // should never be called on the tlab allocator since these are routed early on to the overflow allocator.
        FatalError.unexpected("Should not reach here");
        FatalError.check(spaceLeft.lessThan(refillThreshold), "Should not refill before threshold is reached");
        return Address.zero();
    }

    @Override
    protected void doBeforeGC() {
        retireCurrentAllocatingRegion();
        nextFreeChunkInRegion = Address.zero();
        freeSpace = Size.zero();
    }

    Size freeSpace() {
        return freeSpace;
    }

    @Override
    protected void retireDeadSpace(Pointer deadSpace, Size size) {
        HeapSchemeAdaptor.fillWithDeadObject(deadSpace, deadSpace.plus(size));
        deadSpaceListener.notifyRetireDeadSpace(deadSpace, size);
    }

    @Override
    protected void retireFreeSpace(Pointer freeSpace, Size size) {
        HeapFreeChunk.format(freeSpace, size);
        deadSpaceListener.notifyRetireFreeSpace(freeSpace, size);
    }
}
