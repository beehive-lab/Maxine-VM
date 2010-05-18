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

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;

/**
 * Free heap space management.
 * Nothing ambitious, just to get going and test the tracing algorithm of the future hybrid mark-sweep-evacuate.
 * Implement the HeapSweeper abstract class which defines method called by a HeapMarker to notify free space.
 * The FreeHeapSpace manager records these into an vector of list of free space based on size of the freed space.
 *
 * Space allocation is primarily handled via TLABs, which are made of one or more heap chunks.
 * Request too large to be handled by TLABs are handled by the free space manager directly.
 * This one keeps a simple table of list of chunks, indexed by a power of 2 of the size requested, such that
 * Size >> log2FirstBin is an index to that table. The first bin in the table contains a linked list of chunk of any size
 * between log2FirstBin and minReclaimableSpace and is used primarily for TLAB and small object allocation.
 * The other bins are used for large object space allocation. "Bin" allocation are synchronized.
 *
 * @author Laurent Daynes.
 */
public class FreeHeapSpaceManager extends HeapSweeper implements ResizableSpace {
    private static final VMIntOption largeObjectsMinSizeOption =
        register(new VMIntOption("-XX:LargeObjectsMinSize=", Size.K.times(64).toInt(),
                        "Minimum size to be treated as a large object"), MaxineVM.Phase.PRISTINE);

    private static final VMIntOption freeChunkMinSizeOption =
        register(new VMIntOption("-XX:FreeChunkMinSize=", 256,
                        "Minimum size of contiguous space considered for space reclamation." +
                        "Below this size, the space is ignored (dark matter)"),
                        MaxineVM.Phase.PRISTINE);

    private static final VMBooleanXXOption doImpreciseSweepOption = register(new VMBooleanXXOption("-XX:+",
                    "ImpreciseSweep",
                    "Perform imprecise sweeping phase"),
                    MaxineVM.Phase.PRISTINE);

     //  Debug tracing
    private static final VMBooleanXXOption traceSweepingOption =  register(new VMBooleanXXOption("-XX:+",
                    "TraceSweep",
                    "Trace heap sweep operations. Do nothing for PRODUCT images"),
                    MaxineVM.Phase.PRISTINE);

    public static final VMBooleanXXOption traceTLABOption =  register(new VMBooleanXXOption("-XX:+",
                    "TraceTLAB",
                    "Trace TLAB allocation Do nothing for PRODUCT images"),
                    MaxineVM.Phase.PRISTINE);

    private static boolean TraceSweep = false;
    private static boolean TraceTLAB = false;

    private static final int REFILL_RATIO =  6;

    /**
     * Minimum size to be treated as a large object.
     */
    @CONSTANT_WHEN_NOT_ZERO
    static Size minLargeObjectSize;

    /**
     * Log 2 of the maximum size to enter the first bin of free space.
     */
    int log2FirstBinSize;

    /**
     * A linear space allocator.
     * Allocate space linearly from a region of the heap.
     *
     * FIXME: needs HEADROOM like semi-space to make sure we're never left with not enough space
     * at the end of a chunk to plant a dead object (for heap parsability).
     */
    class HeapSpaceAllocator extends LinearAllocationMemoryRegion {
        /**
         * End of space allocator.
         */
        private Address end;

        /**
         * Maximum size one can allocate with this allocator. Request for size larger than this
         * gets delegated to the allocation failure handler.
         */
        @CONSTANT_WHEN_NOT_ZERO
        private Size sizeLimit;

        /**
         * Size to reserve at the end of the allocator to guarantee that a dead object can always be
         * appended to a TLAB to fill unused space before a TLAB refill.
         * The headroom is used to compute a soft limit that'll be used as the tlab's top.
         */
        @CONSTANT_WHEN_NOT_ZERO
        private Size headroom = HeapSchemeAdaptor.MIN_OBJECT_SIZE;

        private Size refillSize;

        HeapSpaceAllocator(String description) {
            super(description);
        }

        void initialize(Address initialChunk, Size initialChunkSize, Size sizeLimit, Size refillSize) {
            this.sizeLimit = sizeLimit;
            this.refillSize = refillSize;
            this.headroom = HeapSchemeAdaptor.MIN_OBJECT_SIZE;
            if (initialChunk.isZero()) {
                clear();
            } else {
                refill(initialChunk, initialChunkSize);
            }
        }

        // FIXME: concurrency
        void clear() {
            start = Address.zero();
            end = Address.zero();
            mark.set(Address.zero());
        }

        // FIXME: concurrency
        void refill(Address chunk, Size chunkSize) {
            // Make sure we can cause any attempt to allocate to fail, regardless of the
            // value of the mark.
            end = Address.zero();
            // Now refill.
            start = chunk;
            size = chunkSize;
            mark.set(start);
            end = chunk.plus(chunkSize).minus(headroom);
        }

        private Size refillLimit() {
            return size().dividedBy(REFILL_RATIO);
        }

        @INLINE
        private Address hardLimit() {
            return end.plus(headroom);
        }

        @INLINE
        private Pointer setTopToLimit() {
            Pointer cell;
            Address hardLimit = hardLimit();
            do {
                cell = top();
                if (cell.equals(hardLimit)) {
                    // Already at end
                    return cell;
                }
            } while(mark.compareAndSwap(cell, hardLimit) != cell);
            return cell;
        }

        Size freeSpaceLeft() {
            return size.minus(used());
        }

        @INLINE
        private Pointer top() {
            return mark.get().asPointer();
        }

        @INLINE
        private boolean isLarge(Size size) {
            return size.greaterThan(sizeLimit);
        }

        synchronized Pointer refillOrAllocate(Size size, boolean forTLAB) {
            if (isLarge(size)) {
                if (MaxineVM.isDebug()) {
                    FatalError.check(!forTLAB, "must not be for TLAB");
                }
                return binAllocate(size).asPointer();
            }
            // We may have raced with another concurrent thread which may have
            // refilled the allocator.
            Pointer cell = top();

            if (cell.plus(size).greaterThan(end)) {
                Address hardLimit = hardLimit();
                if (cell.plus(size).equals(hardLimit)) {
                    // We need to atomically change top
                    Pointer start = setTopToLimit();
                    if (cell.equals(start)) {
                        return cell;
                    }
                    // Lost the race
                    cell = start;
                }
                // Fill up the allocator to bring all mutators to the refill point.
                Size spaceLeft = hardLimit.minus(cell).asSize();
                if (spaceLeft.greaterThan(refillLimit())) {
                      // Don't refill, waste would be too high. Allocate from the bin table.
                    Address result = forTLAB ? binAllocateTLAB(size) : binAllocate(size);
                    return result.asPointer();
                }
                // Refill. First, fill up the allocator to bring everyone to refill synchronization.
                Pointer start = setTopToLimit();

                Address chunk = binRefill(refillSize, start, hardLimit.minus(start).asSize());
                refill(chunk, HeapFreeChunk.getFreechunkSize(chunk));
                // Fall-off to return zero.
            }
            // There was a race for refilling the allocator. Just return to
            // the non-blocking allocation loop.
            return Pointer.zero();
        }

       /**
         * Allocate space of the specified size.
         *
         * @param size size requested in bytes.
         * @return
         */
        final Pointer allocateCleared(Size size) {
            if (MaxineVM.isDebug()) {
                FatalError.check(size.isWordAligned(), "Size must be word aligned");
            }
            // Try first a non-blocking allocation out of the current chunk.
            // This may fail for a variety of reasons, all captured by the test
            // against the current chunk limit.
            Pointer cell;
            Pointer nextMark;
            do {
                cell = top();
                nextMark = cell.plus(size);
                while (nextMark.greaterThan(end)) {
                    cell = refillOrAllocate(size, false);
                    if (!cell.isZero()) {
                        Memory.clearWords(cell, size.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt());
                        return cell;
                    }
                    // loop back to retry.
                    cell = top();
                    nextMark = cell.plus(size);
                }
            } while (mark.compareAndSwap(cell, nextMark) != cell);
            Memory.clearWords(cell, size.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt());
            return cell;
        }

        final Pointer allocateTLAB(Size tlabSize) {
            if (MaxineVM.isDebug()) {
                FatalError.check(tlabSize.isWordAligned(), "Size must be word aligned");
            }
            // Try first a non-blocking allocation out of the current chunk.
            // This may fail for a variety of reasons, all captured by the test
            // against the current chunk limit.
            Pointer cell;
            Pointer nextMark;
            Size chunkSize;
            do {
                chunkSize = tlabSize;
                cell = top();
                nextMark = cell.plus(chunkSize);
                while (nextMark.greaterThan(end)) {
                    // FIXME: should use some ratio of TLAB size instead here.
                    if (nextMark.minus(end).lessThan(minReclaimableSpace)) {
                        // Can use what's left in the allocator for the TLAB.
                        nextMark = hardLimit().asPointer();
                        chunkSize = nextMark.minus(cell).asSize();
                        break;
                    }
                    cell = refillOrAllocate(chunkSize, true);
                    if (!cell.isZero()) {
                        if (MaxineVM.isDebug()) {
                            // Check cell is formated as chunk
                            FatalError.check(HeapFreeChunk.isValidChunk(cell, committedHeapSpace), "must be a valid heap chunk format");
                        }
                        return cell;
                    }
                    // loop back to retry.
                    cell = top();
                    nextMark = cell.plus(chunkSize);
                }
            } while (mark.compareAndSwap(cell, nextMark) != cell);

            // Format as a chunk.
            HeapFreeChunk.setFreeChunkSize(cell, chunkSize);
            HeapFreeChunk.setFreeChunkNext(cell, null);
            return cell;
        }

        /**
         * Fill up the allocator and return address of its allocation mark
         * before filling.
         * This is used to ease concurrent refill: a thread requesting a refill first
         * grab a refill monitor, then fill up the allocator to force every racer to
         * to grab the refill monitor.
         * Refill can then be performed by changing first the bounds of the allocator, then
         * the allocation mark.
         *
         * @return
         */
        Pointer fillUp() {
            Pointer cell = setTopToLimit();
            Pointer hardLimit = hardLimit().asPointer();
            if (cell.lessThan(hardLimit)) {
                HeapSchemeAdaptor.fillWithDeadObject(cell.asPointer(), hardLimit);
            }
            return cell;
        }

        void makeParsable() {
            fillUp();
        }

        void printSpaceLeft() {
            Log.print("Small object allocator: space left = ");
            Log.println(hardLimit().minus(top()).toLong());

        }
    }

    /**
     * The currently committed heap space.
     * As a temporary hack to please the inspector, we use a LinearAllocationMemoryRegion to
     * record the committed heap space. We only grow and shrink the heap space linearly.
     * So we can use the LinearallocationMemoryRegion.mark to represent the top of the
     * committed space, whereas the end is the end of the reserved memory.
     */
    private final ContiguousHeapSpace committedHeapSpace;
    private boolean doImpreciseSweep;
    private final HeapSpaceAllocator smallObjectAllocator;
    private boolean useTLABBin;

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
         * @return
         */
        Address allocateFirstFit(Size size, boolean exactFit) {
            Size spaceWithHeadRoom = size.plus(HeapSchemeAdaptor.MIN_OBJECT_SIZE);
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
                        // FIXME: need to revisit the API to clear distinguish the cases when what's needed is formatted chunks
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
            while (chunk != null) {
                if (size.lessThan(chunk.size)) {
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

            if (MaxineVM.isDebug() && TraceTLAB) {
                printAllocatedChunk(result, lastChunkAddress, numAllocatedChunks);
            }

            return result;
        }
    }

    /**
     * Free space is managed via segregated list. The minimum chunk size managed is minFreeChunkSize.
     */
    final FreeSpaceList [] freeChunkBins = new FreeSpaceList[10];

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
        final long l = size.unsignedShiftedRight(log2FirstBinSize).toLong();
        return  (l < freeChunkBins.length) ?  (int) l : (freeChunkBins.length - 1);
    }

    private synchronized Address binAllocateTLAB(Size size) {
        long requiredSpace = size.toLong();
        // First, try to allocate from the TLAB bin.
        if (tlabFreeSpaceList.totalSize > requiredSpace) {
            Address result = tlabFreeSpaceList.allocateChunks(size);
            checkBinFreeSpace();
            return result;
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
                if (MaxineVM.isDebug() && TraceTLAB) {
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
            if (MaxineVM.isDebug() && Heap.traceGC()) {
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
                smallObjectAllocator.printSpaceLeft();
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
    public final void processDeadSpace(Address freeChunk, Size size) {
        recordFreeSpace(freeChunk, size);
    }

    /**
     * Minimum size to be considered reclaimable.
     */
    private Size minReclaimableSpace;

    /**
     * Pointer to the end of the last dead object notified by the sweeper. Used  for precise sweeping.
     */
    private Pointer endOfLastVisitedObject;

    @INLINE
    private Pointer setEndOfLastVisitedObject(Pointer cell) {
        final Pointer origin = Layout.cellToOrigin(cell);
        endOfLastVisitedObject = cell.plus(Layout.size(origin));
        return endOfLastVisitedObject;
    }

    @Override
    public Pointer processLiveObject(Pointer liveObject) {
        final Size deadSpace = liveObject.minus(endOfLastVisitedObject).asSize();
        if (deadSpace.greaterThan(minReclaimableSpace)) {
            recordFreeSpace(endOfLastVisitedObject, deadSpace);
        }
        endOfLastVisitedObject = liveObject.plus(Layout.size(Layout.cellToOrigin(liveObject)));
        return endOfLastVisitedObject;
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
    public Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject) {
        Pointer endOfLeftObject = leftLiveObject.plus(Layout.size(Layout.cellToOrigin(leftLiveObject)));
        Size numDeadBytes = rightLiveObject.minus(endOfLeftObject).asSize();
        if (MaxineVM.isDebug() && TraceSweep) {
            printNotifiedGap(leftLiveObject, rightLiveObject, endOfLeftObject, numDeadBytes);
        }
        if (numDeadBytes.greaterEqual(minReclaimableSpace)) {
            recordFreeSpace(endOfLeftObject, numDeadBytes);
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

    public ContiguousHeapSpace committedHeapSpace() {
        return committedHeapSpace;
    }

    public FreeHeapSpaceManager() {
        committedHeapSpace = new ContiguousHeapSpace("Heap");
        totalFreeChunkSpace = 0;
        for (int i = 0; i < freeChunkBins.length; i++) {
            freeChunkBins[i] = new FreeSpaceList(i);
        }
        tlabFreeSpaceList = freeChunkBins[0];
        smallObjectAllocator = new HeapSpaceAllocator("Small Objects Allocator");
    }



    public void initialize(Address start, Size initSize, Size maxSize) {
        if (!committedHeapSpace.reserve(start, maxSize)) {
            MaxineVM.reportPristineMemoryFailure("object heap", "reserve", maxSize);
        }
        if (!committedHeapSpace.growCommittedSpace(initSize)) {
            MaxineVM.reportPristineMemoryFailure("object heap", "commit", maxSize);
        }
        // Round down to power of two.
        minLargeObjectSize = Size.fromInt(Integer.highestOneBit(largeObjectsMinSizeOption.getValue()));
        log2FirstBinSize = Integer.numberOfTrailingZeros(minLargeObjectSize.toInt());
        minReclaimableSpace = Size.fromInt(freeChunkMinSizeOption.getValue());
        doImpreciseSweep = doImpreciseSweepOption.getValue();
        TraceSweep = MaxineVM.isDebug() ? traceSweepingOption.getValue() : false;
        TraceTLAB = MaxineVM.isDebug() ? traceTLABOption.getValue() : false;
        smallObjectAllocator.initialize(start, initSize, minLargeObjectSize, minLargeObjectSize);
        useTLABBin = false;
        InspectableHeapInfo.init(committedHeapSpace);
    }

    private Size lockedFreeSpaceLeft() {
        return Size.fromLong(totalFreeChunkSpace).plus(smallObjectAllocator.freeSpaceLeft());
    }

    /**
     * Estimated free space left.
     * @return an estimation of the space available for allocation (in bytes).
     */
    public synchronized Size freeSpaceLeft() {
        return lockedFreeSpaceLeft();
    }

    public Size reclaim(TricolorHeapMarker heapMarker) {
        for (int i = 0; i < freeChunkBins.length; i++) {
            freeChunkBins[i].reset();
        }
        totalFreeChunkSpace = 0;

        if (doImpreciseSweep) {
            if (Heap.traceGCPhases()) {
                Log.println("Imprecise sweeping of the heap...");
            }
            heapMarker.impreciseSweep(this, minReclaimableSpace);
        } else {
            if (Heap.traceGCPhases()) {
                Log.println("Precise sweeping of the heap...");
            }
            endOfLastVisitedObject = committedHeapSpace.start().asPointer();
            heapMarker.sweep(this);
        }
        useTLABBin = tlabFreeSpaceList.totalSize > 0;
        if (MaxineVM.isDebug()) {
            checkBinFreeSpace();
            if (TraceSweep) {
                print();
            }
        }
        return lockedFreeSpaceLeft();
    }

    public void makeParsable() {
        smallObjectAllocator.makeParsable();
        for (FreeSpaceList fsp : freeChunkBins) {
            fsp.makeParsable();
        }
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

    /**
     * Allocation of zero-filled memory, ready to use for object allocation.
     * @param size
     * @return
     */
    @INLINE
    public final Pointer allocate(Size size) {
        return smallObjectAllocator.allocateCleared(size);
    }

    @INLINE
    public final Pointer allocateTLAB(Size size) {
        return useTLABBin ? binAllocateTLAB(size).asPointer() : smallObjectAllocator.allocateTLAB(size);
    }

    /**
     * Try to grow free space backing storage by delta bytes.
     * The method rounds the delta up to the alignment constraint of the free space backing
     * storage. If delta is larger than space left, the heap is grown to its capacity.
     * @param delta the number of bytes to grow the heap with
     * @return the effective growth
     */
    public Size growAfterGC(Size delta) {
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

    public Size shrinkAfterGC(Size delta) {
        // FIXME: Can't do much without evacuation or regions apart from freeing the chunk that is at the end of
        // committed heap space. Don't bother with this for now.
        return Size.zero();
    }

    public Size totalSpace() {
        return committedHeapSpace.committedSize();
    }

    public Size totalCapacity() {
        return committedHeapSpace.size();

    }
}
