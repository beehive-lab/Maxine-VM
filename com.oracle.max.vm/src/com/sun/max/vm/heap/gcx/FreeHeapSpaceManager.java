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

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Free heap space management.
 *
 * Implement the HeapSweeper abstract class which defines method called by a HeapMarker to notify free space.
 * The manager records free space into a vector of list of free space based on size of the freed space.
 *
 * Space allocation is primarily handled via TLABs, which are made of one or more heap chunks.
 * Requests too large to be handled by TLABs are handled by the free space manager directly.
 * This one keeps a simple table of list of chunks of same size range.
 * The first bin in the table contains a linked list of chunk of any size
 * between minReclaimableSpace and 1<< log2FirstBin and is used primarily for TLAB and small object allocation.
 * The other bins are used for large object space allocation.
 * "Bin" allocation are synchronized.
 */
public final class FreeHeapSpaceManager extends Sweeper implements HeapSpace {
    private static final VMIntOption largeObjectsMinSizeOption =
        register(new VMIntOption("-XX:LargeObjectsMinSize=", Size.K.times(64).toInt(),
                        "Minimum size to be treated as a large object"), MaxineVM.Phase.PRISTINE);

    /**
     * This controls how free chunks are distributed into bins. We are experimenting with two methods:
     * Put in the the same bin size with the same most significant bit (i.e., all size comprises between 2^i and (2^i+1) -1 ends up in the same bin.
     * Indexing requires computing the msb.
     * Put in the same bin size that occupies the same number of 2^k block, when k is log2 of the first bin. Indexing is a simple shift in this case.
     * The former seems more efficient for now.
     */
    private static boolean UseLog2BinIndexing = true;

    static {
        VMOptions.addFieldOption("-XX:", "UseLog2BinIndexing", FreeHeapSpaceManager.class, "Use log2(msb(Size)) - log2FirstBin for bin index instead of Size >> log2FirstBin)", Phase.PRISTINE);
    }

    private static boolean TraceTLABChunk = false;

    /**
     * Minimum size to be treated as a large object.
     */
    @CONSTANT_WHEN_NOT_ZERO
    static Size minLargeObjectSize;

    /**
     * Index to the last bin of free chunk size. The vector of free space list has LastBin + 1 entries.
     * Chunks of free space recorded in the list at entry i of the vector have a size  between 2^i and 2^(i+1) -1.
     * Any chunks of size greater than 2^LastBin are recorded in the last bin.
     */
    static final int LastBin = 10;

    final class LinearSpaceRefillManager extends ChunkListRefillManager {
        /**
         * Size linear space allocator managed by this refill manager are refilled with.
         */
        Size refillSize;
        /**
         * Amount of space below which a refill is warranted. Also used as the minimum acceptable size
         * for a TLAB chunk, when allocation discontinuous TLABs.
         */
        Size refillThreshold;


        void setPolicy(Size refillSize, Size refillThreshold, Size tlabMinChunkSize) {
            this.refillSize = refillSize;
            this.refillThreshold =  refillThreshold;
            this.minChunkSize = tlabMinChunkSize;
        }

        LinearSpaceRefillManager() {
        }

        @Override
        public Address allocateLarge(Size size) {
            return binAllocate(size);
        }

        @Override
        @INLINE
        public Address allocateOverflow(Size size) {
            return binAllocate(size);
        }

        @Override
        @INLINE
        public boolean shouldRefill(Size requestedSpace, Size spaceLeft) {
            return spaceLeft.lessThan(refillThreshold);
        }

        @Override
        @INLINE
        public Address allocateRefill(Pointer startOfSpaceLeft, Size spaceLeft) {
            return binRefill(refillSize, startOfSpaceLeft, spaceLeft);
        }

        /**
         * Allocate a TLAB and refill the allocator along the way.
         * If possible, the remaining space in the allocator is used for the TLAB.
         * The allocator is refilled only if there is enough space, otherwise it is left full
         * which will trigger GC on next request.
         */
        @Override
        public Address allocateChunkListOrRefill(ChunkListAllocator<? extends ChunkListRefillManager> allocator, Size tlabSize, Pointer leftover, Size leftoverSize) {
            // FIXME (ld) this never refill the allocator!
            Address firstChunk = retireChunk(leftover, leftoverSize);
            if (!firstChunk.isZero()) {
                tlabSize = tlabSize.minus(leftoverSize);
                if (tlabSize.lessThan(minChunkSize)) {
                    return firstChunk;
                }
            }
            return binAllocateTLAB(tlabSize, firstChunk);
        }

        @Override
        protected void doBeforeGC() {
        }

        @INLINE
        @Override
        protected void retireDeadSpace(Pointer deadSpace, Size size) {
            HeapSchemeAdaptor.fillWithDeadObject(deadSpace, deadSpace.plus(size));
        }

        @INLINE
        @Override
        protected void retireFreeSpace(Pointer freeSpace, Size size) {
            HeapFreeChunk.format(freeSpace, size);
        }
    }

    /**
     * The currently committed heap space.
     */
    @INSPECTED
    public final ContiguousHeapSpace committedHeapSpace;

    private boolean useTLABBin;

    private final ChunkListAllocator<LinearSpaceRefillManager> smallObjectAllocator;

    /**
     * Log 2 of the maximum size to enter the first bin of free space.
     */
    private int log2FirstBinSize;

    private Size lastBinThreshold;
    private Size lastBinThreshold3;

    /**
     * Head of a linked list of free space recovered by the Sweeper.
     * Chunks are appended in the list only during sweeping.
     * The entries are therefore ordered from low to high addresses
     * (they are entered as the sweeper discover them).
     */
    final class FreeSpaceList {
        Address head;
        Address last;
        long totalSize;
        long totalChunks;
        final int binIndex;
        FreeSpaceList(int binIndex) {
            this.binIndex = binIndex;
            reset();
        }

        void reset() {
            head = Address.zero();
            last = Address.zero();
            totalSize = 0L;
            totalChunks = 0L;
        }

        @INLINE
        void makeParsable() {
            if (!head.isZero()) {
                HeapFreeChunk.makeParsable(head);
                reset();
            }
        }

        @INLINE
        private void appendChunk(Address chunk, Size size) {
            if (last.isZero()) {
                head = chunk;
            } else {
                HeapFreeChunk.setFreeChunkNext(last, chunk);
            }
            last = chunk;
            totalSize += size.toLong();
            totalChunks++;
        }

        void append(Address chunk, Size size) {
            HeapFreeChunk.format(chunk, size);
            appendChunk(chunk, size);
        }

        void append(HeapFreeChunk chunk) {
            appendChunk(HeapFreeChunk.fromHeapFreeChunk(chunk), chunk.size);
            useTLABBin = tlabFreeSpaceList.totalSize > 0;
        }

        @INLINE
        private void remove(HeapFreeChunk prev, HeapFreeChunk chunk) {
            totalChunks--;
            totalSize -= chunk.size.toLong();
            if (prev == null) {
                head =  HeapFreeChunk.fromHeapFreeChunk(chunk.next);
            } else {
                prev.next = chunk.next;
            }
            chunk.next = null;
            if (last ==  HeapFreeChunk.fromHeapFreeChunk(chunk)) {
                last = HeapFreeChunk.fromHeapFreeChunk(prev);
            }
            if (MaxineVM.isDebug()) {
                FatalError.check(totalChunks != 0 || (totalSize == 0 && head == Address.zero() && last == Address.zero()), "Inconsistent free list state");
            }
        }

        /**
         * Allocate first chunk of the free list fitting the size.
         * Space left-over is re-entered in the appropriate bin, or dismissed as dark matter.
         * @param size
         */
        Address allocateFirstFit(Size size, boolean exactFit) {
            Size spaceWithHeadRoom = size.plus(HeapSchemeAdaptor.minObjectSize());
            HeapFreeChunk prevChunk = null;
            HeapFreeChunk chunk = HeapFreeChunk.toHeapFreeChunk(head);
            do {
                if (chunk.size.greaterEqual(spaceWithHeadRoom)) {
                    Address result = HeapFreeChunk.fromHeapFreeChunk(chunk);
                    if (!exactFit) {
                        totalFreeChunkSpace -= chunk.size.toLong();
                        remove(prevChunk, chunk);
                        return result;
                    }
                    Size spaceLeft = chunk.size.minus(size);
                    if (spaceLeft.greaterEqual(minReclaimableSpace)) {
                        // Space is allocated at the end of the chunk to avoid reformatting the leftover
                        // if it doesn't change bins.
                        // TODO (ld) need to revisit the API to clearly distinguish the cases when what's needed is formatted chunks
                        // (e.g., when allocating for allocators, like TLABs), or when all that is needed is bytes (i.e., for direct object allocation)
                        result = result.plus(spaceLeft);
                        HeapFreeChunk.format(result, size);
                        FreeSpaceList newFreeList =  freeChunkBins[binIndex(spaceLeft)];
                        if (newFreeList == this) {
                            // Chunk remains in its free list.
                            chunk.size = spaceLeft;
                            totalSize -= size.toLong();
                            totalFreeChunkSpace -= size.toLong();
                            return result;
                        }
                        // Chunk changes of free list.
                        remove(prevChunk, chunk);
                        chunk.size = spaceLeft;
                        newFreeList.append(chunk);
                        totalFreeChunkSpace -= size.toLong();
                        useTLABBin = tlabFreeSpaceList.totalSize > 0;
                    } else {
                        // Chunk is removed.
                        totalFreeChunkSpace -=  chunk.size.toLong();
                        remove(prevChunk, chunk);
                        Pointer start = result.asPointer().plus(size);
                        HeapSchemeAdaptor.fillWithDeadObject(start, start.plus(spaceLeft));
                    }
                    return result;
                } else if (chunk.size.equals(size)) {
                    // Exact fit.
                    Address result = HeapFreeChunk.fromHeapFreeChunk(chunk);
                    totalFreeChunkSpace -= size.toLong();
                    remove(prevChunk, chunk);
                    return result;
                }
                prevChunk = chunk;
                chunk = chunk.next;
            } while(chunk != null);
            return Address.zero();
        }

        boolean canFit(Size size) {
            HeapFreeChunk chunk = HeapFreeChunk.toHeapFreeChunk(head);
            Size spaceWithHeadRoom = size.plus(HeapSchemeAdaptor.minObjectSize());

            while (chunk != null) {
                if (spaceWithHeadRoom.lessThan(chunk.size) || size.equals(chunk.size)) {
                    return true;
                }
                chunk = chunk.next;
            }
            return false;
        }

        private void printChunk(Address chunk) {
            int size = HeapFreeChunk.getFreechunkSize(chunk).toInt();
            Log.print('[');
            Log.print(chunk);
            Log.print(',');
            Log.print(chunk.plus(size));
            Log.print("] (");
            Log.print(size);
            Log.print(')');
        }

        private void printAllocatedChunk(Address first, Address last, int numAllocated) {
            final boolean lockDisabledSafepoints = Log.lock();
            if (numAllocated == 1) {
                Log.print("Allocate 1 chunk from bin #0:   ");
                printChunk(first);
            } else {
                Log.print("Allocate ");
                Log.print(numAllocated);
                Log.print(" chunks from bin #0: first = ");
                printChunk(first);
                Log.print(" last = ");
                printChunk(last);
            }

            Log.print("\n chunk list: h = ");
            Log.print(head);
            Log.print("l = ");
            Log.print(last);
            Log.print(", totalSize = ");
            Log.print(tlabFreeSpaceList.totalSize);
            Log.print(", totalChunks = ");
            Log.println(tlabFreeSpaceList.totalChunks);
            Log.unlock(lockDisabledSafepoints);
        }

        Address allocateChunks(Size size) {
            if (MaxineVM.isDebug()) {
                FatalError.check(!head.isZero(), "Head of free list must not be null");
            }
            // Allocate enough chunks to meet requested Size.
            // This is very imprecise and we may end up with much more than the
            // size initially requested.
            Size allocated = Size.zero();
            HeapFreeChunk lastChunk = HeapFreeChunk.toHeapFreeChunk(head);
            int numAllocatedChunks = 1;
            allocated = allocated.plus(lastChunk.size);
            while (allocated.lessThan(size) && lastChunk.next != null) {
                lastChunk = lastChunk.next;
                numAllocatedChunks++;
                allocated = allocated.plus(lastChunk.size);
            }
            Address result = head;
            head =  HeapFreeChunk.fromHeapFreeChunk(lastChunk.next);
            Address lastChunkAddress = HeapFreeChunk.fromHeapFreeChunk(lastChunk);
            // To escape any write-barrier when zeroing out lastChunk.next
            HeapFreeChunk.setFreeChunkNext(lastChunkAddress, Address.zero());
            totalChunks -= numAllocatedChunks;
            totalSize -= allocated.toLong();
            totalFreeChunkSpace -= allocated.toLong();
            if (last == lastChunkAddress) {
                if (MaxineVM.isDebug()) {
                    FatalError.check(totalChunks == 0, "Invariant violation");
                }
                last = Address.zero();
            }

            if (MaxineVM.isDebug() && TraceTLABChunk) {
                printAllocatedChunk(result, lastChunkAddress, numAllocatedChunks);
            }

            return result;
        }
    }

    /**
     * Free space is managed via segregated list. The minimum chunk size managed is minFreeChunkSize.
     */
    final FreeSpaceList [] freeChunkBins = new FreeSpaceList[LastBin + 1];

    /**
     * Short cut to first bin dedicated to TLAB refills.
     */
    private final FreeSpaceList  tlabFreeSpaceList;

    /**
     * Total space in free chunks. This doesn't include space of chunks allocated to heap space allocator.
     */
    long totalFreeChunkSpace;

    @INLINE
    private int binIndex(Size size) {
        return UseLog2BinIndexing ? binIndex2(size) : binIndex1(size);
    }

    @INLINE
    private int binIndex1(Size size) {
        if (size.greaterEqual(lastBinThreshold)) {
            return LastBin;
        }
        return size.unsignedShiftedRight(log2FirstBinSize).toInt();
    }

    @INLINE
    private int binIndex2(Size size) {
        if (size.greaterEqual(lastBinThreshold)) {
            return LastBin;
        }
        return size.unsignedShiftedRight(log2FirstBinSize).mostSignificantBitSet() + 1;
    }

    /**
     * Allocate a TLAB from the segregated list of free chunks.
     * @param size
     * @return  the address of the first chunks allocated to the TLAB
     */
    private synchronized Address binAllocateTLAB(Size size, Address firstChunk) {
        long requiredSpace = size.toLong();
        // First, try to allocate from the TLAB bin.
        if (tlabFreeSpaceList.totalSize > requiredSpace) {
            Address result = tlabFreeSpaceList.allocateChunks(size);
            checkBinFreeSpace();
            if (firstChunk.isZero()) {
                return result;
            }
            HeapFreeChunk.setFreeChunkNext(firstChunk, result);
            return firstChunk;
        }
        // Here, we can't use firstChunk after any call to allocate as a GC might occur
        // and invalidate it. Simplest is to put it back in the pool of free space.
        if (!firstChunk.isZero()) {
            recordFreeSpace(firstChunk, HeapFreeChunk.getFreechunkSize(firstChunk));
        }
        // In any case, after this call, there will be no more TLAB chunks left.
        // Let future TLAB allocation not use this until filled again by GC.
        Address initialChunks = tlabFreeSpaceList.head;
        if (initialChunks.isZero()) {
            // No chunk left in bin #0.
            Address result = binAllocate(1, size, true);
            // Chunk may have been appended to bin #0
            useTLABBin = tlabFreeSpaceList.totalSize > 0;
            return result;
        }
        size = size.minus(tlabFreeSpaceList.totalSize);
        totalFreeChunkSpace -= tlabFreeSpaceList.totalSize;
        tlabFreeSpaceList.head = Address.zero();
        tlabFreeSpaceList.last = Address.zero();
        tlabFreeSpaceList.totalSize = 0;
        tlabFreeSpaceList.totalChunks = 0;

        if (size.greaterThan(minReclaimableSpace)) {
            // Try allocate additional space off higher free space bins.
            Address additionalChunks = binTryAllocate(1, size, true);
            if (!additionalChunks.isZero()) {
                HeapFreeChunk.format(additionalChunks, size, initialChunks);
                if (MaxineVM.isDebug() && TraceTLABChunk) {
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.print("binAllocateTLAB from TLAB bin #1: additional chunk = ");
                    Log.print(additionalChunks);
                    Log.print("(");
                    Log.print(size.toLong());
                    Log.print("), initial chunk ");
                    Log.println(initialChunks);
                    Log.unlock(lockDisabledSafepoints);
                }
                useTLABBin = tlabFreeSpaceList.totalSize > 0;
                return additionalChunks;
            }
        }
        checkBinFreeSpace();
        useTLABBin = false;
        return initialChunks;
    }


    synchronized Address binAllocate(Size size) {
        return  binAllocate(binIndex(size), size, true);
    }

    /* For simplicity at the moment.
     */
    private static final OutOfMemoryError outOfMemoryError = new OutOfMemoryError();

    private Address binTryAllocate(int index, Size size, boolean exactFit) {
        // Any chunks in bin larger or equal to index is large enough to contain the requested size.
        // We may have to re-enter the leftover into another bin.
        while (index < freeChunkBins.length) {
            FreeSpaceList freelist = freeChunkBins[index];
            if (!freelist.head.isZero()) {
                Address result = freelist.allocateFirstFit(size, exactFit);
                if (!result.isZero()) {
                    checkBinFreeSpace();
                    return result;
                }
            }
            index++;
        }
        return Address.zero();
    }

    public boolean canSatisfyAllocation(Size size) {
        // assert: must hold this class lock and must only be called from GC
        int index = binIndex(size);
        // Any chunks in bin larger or equal to index is large enough to contain the requested size.
        // We may have to re-enter the leftover into another bin.
        while (index < freeChunkBins.length) {
            FreeSpaceList freelist = freeChunkBins[index];
            if (!freelist.head.isZero() && freelist.canFit(size)) {
                return true;
            }
            index++;
        }
        return false;
    }

    private void printTlabFreeSpace() {
        Log.print("TLAB freelist: totalChunks = ");
        Log.print(tlabFreeSpaceList.totalChunks);
        Log.print(", totalSize = ");
        Log.print(tlabFreeSpaceList.totalSize);
        Log.print(" useTLABBin = ");
        Log.println(useTLABBin);
    }

    private Address binAllocate(int firstBinIndex, Size size, boolean exactFit) {
        int gcCount = 0;
        // Search for a bin with a chunk large enough to satisfy this allocation.
        // Bin #0 contains chunks of any size between minReclaimableSpace and 1 << log2FirstBinSize,
        // so it needs to be scanned for a chunk big enough to hold the requested size.
        do {
            Address result = binTryAllocate(firstBinIndex, size, exactFit);
            if (!result.isZero()) {
                return result;
            }
            if (MaxineVM.isDebug() && Heap.logAllGC()) {
                gcCount++;
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Allocation failure: ");
                Log.print("firstBinIndex ");
                Log.print(firstBinIndex);
                Log.print(", size: ");
                Log.print(size.toLong());
                Log.print(",  fit: ");
                Log.println(exactFit ? "exact" : "not exact");
                printTlabFreeSpace();
                Log.print("Small object allocator: space left = ");
                Log.println(smallObjectAllocator.freeSpace());
                Log.unlock(lockDisabledSafepoints);
                if (gcCount > 5) {
                    FatalError.unexpected("Suspiscious repeating GC calls detected");
                }
            }
        } while (Heap.collectGarbage(size));
        // Not enough freed memory.
        throw outOfMemoryError;
    }

    synchronized Address binRefill(Size refillSize, Pointer topAtRefill, Size spaceLeft) {
        // First, deal with the left-over.
        if  (spaceLeft.greaterEqual(minReclaimableSpace)) {
            recordFreeSpace(topAtRefill, spaceLeft);
            useTLABBin = tlabFreeSpaceList.totalSize > 0;
        } else if (spaceLeft.greaterThan(0)) {
            HeapSchemeAdaptor.fillWithDeadObject(topAtRefill, topAtRefill.plus(spaceLeft));
        }
        return binAllocate(1, refillSize, false);
    }

    @INLINE
    private void recordFreeSpace(Address chunk, Size numBytes) {
        freeChunkBins[binIndex(numBytes)].append(chunk, numBytes);
        totalFreeChunkSpace += numBytes.toLong();
    }

    /**
     * Recording of free chunk of space.
     * Chunks are recording in different list depending on their size.
     * @param freeChunk
     * @param size
     */
    @Override
    public void processDeadSpace(Address freeChunk, Size size) {
        recordFreeSpace(freeChunk, size);
        endOfLastVisitedObject = freeChunk.plus(size).asPointer();
    }

    /**
     * Minimum size to be considered reclaimable.
     */
    private Size minReclaimableSpace;

    @Override
    public Size minReclaimableSpace() {
        return minReclaimableSpace;
    }

    /**
     * Pointer to the end of the last dead object notified by the sweeper. Used  for precise sweeping.
     */
    private Pointer endOfLastVisitedObject;

    private void printDeadSpace(Size deadSpace) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("Dead space (");
        Log.print(deadSpace.toLong());
        Log.print(" bytes) @");
        Log.print(endOfLastVisitedObject);
        Log.print(" - ");
        Log.print(endOfLastVisitedObject.plus(deadSpace));

        if (deadSpace.greaterEqual(minReclaimableSpace)) {
            Log.print(" => bin #");
            Log.println(binIndex(deadSpace));
        } else {
            Log.println(" => dark matter");
        }
        Log.unlock(lockDisabledSafepoints);

    }

    private void printNotifiedGap(Pointer leftLiveObject, Pointer rightLiveObject, Pointer gapAddress, Size gapSize) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("Gap between [");
        Log.print(leftLiveObject);
        Log.print(", ");
        Log.print(rightLiveObject);
        Log.print("] = @");
        Log.print(gapAddress);
        Log.print("(");
        Log.print(gapSize.toLong());
        Log.print(")");

        if (gapSize.greaterEqual(minReclaimableSpace)) {
            Log.print(" => bin #");
            Log.println(binIndex(gapSize));
        } else {
            Log.println(" => dark matter");
        }
        Log.unlock(lockDisabledSafepoints);
    }

    @Override
    public Pointer processLiveObject(Pointer liveObject) {
        final Size deadSpace = liveObject.minus(endOfLastVisitedObject).asSize();
        if (MaxineVM.isDebug() && !deadSpace.isZero()) {
            printDeadSpace(deadSpace);
        }

        if (deadSpace.greaterThan(minReclaimableSpace)) {
            recordFreeSpace(endOfLastVisitedObject, deadSpace);
        } else if (MaxineVM.isDebug()) {
            // Helping the inspector.
            DarkMatterDebugHelper.setDarkMatter(endOfLastVisitedObject, deadSpace);
        }
        endOfLastVisitedObject = liveObject.plus(Layout.size(Layout.cellToOrigin(liveObject)));
        return endOfLastVisitedObject;
    }

    @Override
    public Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject) {
        Pointer endOfLeftObject = leftLiveObject.plus(Layout.size(Layout.cellToOrigin(leftLiveObject)));
        Size numDeadBytes = rightLiveObject.minus(endOfLeftObject).asSize();
        if (MaxineVM.isDebug()) {
            logger.logGap(leftLiveObject, rightLiveObject);
        }
        if (numDeadBytes.greaterEqual(minReclaimableSpace)) {
            recordFreeSpace(endOfLeftObject, numDeadBytes);
        } else if (MaxineVM.isDebug()) {
            // Helping the inspector.
            DarkMatterDebugHelper.setDarkMatter(endOfLeftObject, numDeadBytes);
        }
        return rightLiveObject.plus(Layout.size(Layout.cellToOrigin(rightLiveObject)));
    }

    void print() {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("Min reclaimable space: "); Log.println(minReclaimableSpace);
        for (int i = 0; i < freeChunkBins.length; i++) {
            Log.print("Bin ["); Log.print(i); Log.print("] (");
            Log.print(i << log2FirstBinSize); Log.print(" <= chunk size < "); Log.print((i + 1) << log2FirstBinSize);
            Log.print(") total chunks: "); Log.print(freeChunkBins[i].totalChunks);
            Log.print("   total space : "); Log.println(freeChunkBins[i].totalSize);
        }
        Log.unlock(lockDisabledSafepoints);
    }

    public boolean contains(Address address) {
        return committedHeapSpace.inCommittedSpace(address);
    }

    public FreeHeapSpaceManager() {
        committedHeapSpace = new ContiguousHeapSpace("Heap");
        totalFreeChunkSpace = 0;
        for (int i = 0; i < freeChunkBins.length; i++) {
            freeChunkBins[i] = new FreeSpaceList(i);
        }
        tlabFreeSpaceList = freeChunkBins[0];
        smallObjectAllocator = new ChunkListAllocator<LinearSpaceRefillManager>(new LinearSpaceRefillManager());
    }

    public void initialize(HeapScheme heapScheme, Address start, Size initSize, Size maxSize) {
        if (!committedHeapSpace.reserve(start, maxSize)) {
            MaxineVM.reportPristineMemoryFailure("object heap", "reserve", maxSize);
        }
        if (!committedHeapSpace.growCommittedSpace(initSize)) {
            MaxineVM.reportPristineMemoryFailure("object heap", "commit", initSize);
        }
        // Round down to power of two.
        minLargeObjectSize = Size.fromInt(Integer.highestOneBit(largeObjectsMinSizeOption.getValue()));
        log2FirstBinSize = Integer.numberOfTrailingZeros(minLargeObjectSize.toInt());
        if (UseLog2BinIndexing) {
            lastBinThreshold = Size.fromInt(1).shiftedLeft(LastBin + log2FirstBinSize);
        } else {
            lastBinThreshold = Size.fromInt(LastBin).shiftedLeft(log2FirstBinSize);
        }
        minReclaimableSpace = Size.fromInt(freeChunkMinSizeOption.getValue());

        TraceTLABChunk = heapScheme instanceof HeapSchemeWithTLAB && HeapSchemeWithTLAB.traceTLAB();

        // Refill allocator if space left below this:
        Size allocatorRefillThreshold = Size.fromInt(Word.widthValue().numberOfBytes * 64);

        // Dumb refill policy. Doesn't matter in the long term as we'll switch to a first fit linear allocator
        // with overflow allocator on the side.
        smallObjectAllocator.refillManager.setPolicy(minLargeObjectSize, allocatorRefillThreshold, minReclaimableSpace);
        smallObjectAllocator.initialize(start, initSize, minLargeObjectSize);
        useTLABBin = false;
    }

    private Size lockedFreeSpaceLeft() {
        return Size.fromLong(totalFreeChunkSpace).plus(smallObjectAllocator.freeSpace());
    }

    /**
     * Estimated free space left.
     * @return an estimation of the space available for allocation (in bytes).
     */
    public Size freeSpace() {
        if (VmThread.current().isVmOperationThread()) {
            return lockedFreeSpaceLeft();
        }
        synchronized (this) {
            return lockedFreeSpaceLeft();
        }
    }

    public Size usedSpace() {
        return totalSpace().minus(freeSpace());
    }

    @Override
    public void beginSweep() {
        for (int i = 0; i < freeChunkBins.length; i++) {
            freeChunkBins[i].reset();
        }
        totalFreeChunkSpace = 0;
        endOfLastVisitedObject = committedHeapSpace.start().asPointer();
    }

    @Override
    public void endSweep() {
        useTLABBin = tlabFreeSpaceList.totalSize > 0;
        if (MaxineVM.isDebug()) {
            checkBinFreeSpace();
            if (TraceSweep) {
                print();
            }
        }
    }

    @Override
    public Size freeSpaceAfterSweep() {
        return lockedFreeSpaceLeft();
    }

    public void doBeforeGC() {
        smallObjectAllocator.doBeforeGC();
        for (FreeSpaceList fsp : freeChunkBins) {
            fsp.makeParsable();
        }
    }

    public void doAfterGC() {
    }

    @INLINE
    private void checkBinFreeSpace() {
        if (MaxineVM.isDebug()) {
            long totalSpaceInFreelists = 0L;
            for (FreeSpaceList fsp : freeChunkBins) {
                totalSpaceInFreelists += fsp.totalSize;
            }
            FatalError.check(totalSpaceInFreelists == totalFreeChunkSpace, "Inconsistent free space counts");
        }
    }

    void verifyUsage(long freeChunksByteCount, long darkMatterByteCount, long liveDataByteCount) {
        FatalError.check(freeChunksByteCount == totalFreeChunkSpace, "Inconsistent free chunk space");
        final long total = darkMatterByteCount + freeChunksByteCount + liveDataByteCount;
        FatalError.check(total == committedHeapSpace.committedSize().toLong(), "Inconsistent committed space size");
    }

    @INLINE
    public Pointer allocate(Size size) {
        return smallObjectAllocator.allocateCleared(size);
    }

    @INLINE
    public Pointer allocateTLAB(Size size) {
        return useTLABBin ? binAllocateTLAB(size, Address.zero()).asPointer() : smallObjectAllocator.allocateTLAB(size);
    }

    public void retireTLAB(Pointer start, Size size) {
        // Ignore.
        HeapSchemeAdaptor.fillWithDeadObject(start, start.plus(size));
    }

    /**
     * Try to grow free space backing storage by delta bytes.
     * The method rounds the delta up to the alignment constraint of the free space backing
     * storage. If delta is larger than space left, the heap is grown to its capacity.
     * @param delta the number of bytes to grow the heap with
     * @return the effective growth
     */
    public Size increaseSize(Size delta) {
        Size adjustedGrowth = committedHeapSpace.adjustGrowth(delta);
        if (adjustedGrowth.isZero()) {
            return Size.zero();
        }
        Address chunkStart = committedHeapSpace.committedEnd();
        boolean res = committedHeapSpace.growCommittedSpace(adjustedGrowth);
        FatalError.check(res, "Committing over reserved space should always succeed");
        freeChunkBins[binIndex(adjustedGrowth)].append(HeapFreeChunk.format(chunkStart, adjustedGrowth));
        totalFreeChunkSpace += adjustedGrowth.toLong();
        return adjustedGrowth;
    }

    public Size decreaseSize(Size delta) {
        // TODO (ld) Can't do much without evacuation or regions apart from freeing the chunk that is at the end of
        // committed heap space. Don't bother with this for now.
        return Size.zero();
    }

    public Size totalSpace() {
        return committedHeapSpace.committedSize();
    }

    public Size capacity() {
        return committedHeapSpace.size();
    }

    @Override
    public void verify(AfterMarkSweepVerifier verifier) {
        committedHeapSpace.walkCommittedSpace(verifier);
        verifyUsage(verifier.freeChunksByteCount, verifier.darkMatterByteCount, verifier.liveDataByteCount);
    }

    @Override
    public Address startOfSweepingRegion() {
        return committedHeapSpace.start();
    }

    @Override
    public Address endOfSweepingRegion() {
        return committedHeapSpace.committedEnd();
    }

    @Override
    public void visit(CellRangeVisitor visitor) {
        visitor.visitCells(committedHeapSpace.start(), committedHeapSpace.committedEnd());
    }

    @Override
    public SpaceBounds bounds() {
        return committedHeapSpace.bounds();
    }
}
