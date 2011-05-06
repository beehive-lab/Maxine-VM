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

import static com.sun.max.vm.VMConfiguration.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * A simple large object allocator with space reclaimable via sweeping.
 * Backing storage is provided by a contiguous range of virtual space.
 * The allocator provides reasonably fast allocation using a best fit strategy with
 * splitting. Space is allocated in fixed-size increments, called blocks, that
 * are a power of two (currently 2 KB block).
 * Free lists are maintained for chunks of memory made of contiguous integral number of blocks
 * between a minimum large object size and a very large object size. Chunks smaller than the minimum
 * size are entered in a dark matter list, whereas chunks larger than the very large object size are maintained
 * in a very large chunks list. These are the only two lists of the large object space with chunks of
 * heterogeneous size.
 *
 * Coalescing of free space is intrinsically performed by a sweeping phase of the large object space.
 * Contiguous range of free blocks (a.k.a, chunks) enter their appropriate free list in FIFO order, resulting in
 * free list ordered in increasing or decreasing address order (depending on what direction the sweeper uses to
 * sweep the large object space).
 * Splitting will mess this order when entering a split-off chunk to another free list. Split-off chunks
 * are entered at the head of the list and re-used first.
 *
 * Allocation from the very large object list is performed in best-fit, address ordered fashion.
 * Given that no chunks can enter the list between two GCs and that a GC re-organizes the list in address order anyway,
 * this simply means that allocation follows a first fit search.
 *
 * The configuration of the large object space is currently hard-wired with block size being 2K, minimum object
 * size of 4 KB, and very large object size of 64 KB. At worse, internal fragmentation is 2KB.
 *
 * TODO: investigate interface and mechanism to enable temporary lease of dark matter
 * to other garbage collected space with evacuation support
 * TODO: enable downward growth and allocation (i.e., from high to low addresses).
 *
 * @author Laurent Daynes
 */
public class LargeObjectSpace extends Sweepable {

    protected static final int MIN_LARGE_OBJECT_SIZE = Size.K.times(4).toInt();
    protected static final int BLOCK_SIZE = Size.K.times(2).toInt();
    protected static final int VERY_LARGE_CHUNK_SIZE = Size.K.times(64).toInt();
    protected static final int ALIGNMENT_REQUIREMENT = Size.K.times(64).toInt();
    protected static final int LOG2_BLOCK_SIZE = Integer.highestOneBit(BLOCK_SIZE);
    protected static final int MIN_NUM_BLOCKS = MIN_LARGE_OBJECT_SIZE >>> LOG2_BLOCK_SIZE;
    protected static final int VERY_LARGE_CHUNK_LIST = (VERY_LARGE_CHUNK_SIZE >>> LOG2_BLOCK_SIZE) + 1;

    private final ContiguousHeapSpace committedHeapSpace;

    /**
     * Table recording free list of same-size chunks. Chunks are made of integral numbers of block
     * of {@link LargeObjectSpace#BLOCK_SIZE} bytes. Table entry <em>i</em> heads a linked list of chunks that all
     * have a size of <em>i</em> blocks. Only the last entry at index {@link LargeObjectSpace#VERY_LARGE_CHUNK_LIST}
     * has chunks of heterogeneous size.
     * For simplicity, the table also includes entries for dark matter chunks, chunks of a size smaller
     * than {@link LargeObjectSpace#MIN_LARGE_OBJECT_SIZE} to be re-used for allocation.
     * This allows to use a simple shift to find what free list a chunks should be entered too, for chunks
     * smaller or equals to {@link LargeObjectSpace#VERY_LARGE_CHUNK_SIZE}.
     */
    private final Pointer[] chunkSizeTable;

    /**
     * Virtual start of the chunk size table to enable fast access based on simple shift of the
     * requested size, and to avoid array boundary check. Chunks are allocated in integral number
     * of blocks of BLOCK_SIZE, a power of two. To find the appropriate free list where to allocate
     * an object of size <code>s</code>, one simply do <code>chunkSizeTableStart.getWord(s >> LOG2_BLOCK_SIZE)</code>.
     */
    private Pointer chunkSizeTableStart;

    /**
     * A bitmap summarizing what entry of the chunk size table are free.
     * The nth entry of the chunkSizeTable is associated with the nth bit. A bit set to 1 means
     * the corresponding entry of {@link LargeObjectSpace#chunkSizeTable} has at least one chunk.
     * This allows finding quickly a free list that can provide a fit for the requested size.
     * Neither the very large chunk list nor the dark matter lists have their state recorded in the list.
     */
    private long sizeTableSummary;

    /**
     * Allocation hand of the tail of the large object space.
     * @see {@linkplain LargeObjectSpace#tailAllocate(int)}.
     */
    private Pointer top;

    /**
     * Soft end of the tail of the large object space.
     * @see {@linkplain LargeObjectSpace#tailAllocate(int)}.
     */
    private Pointer end;

    /**
     * Real end of the tail.
     */
    private Pointer realEnd;

    /**
     * Total number of free blocks (i.e., blocks of chunk greater or equal than {@link LargeObjectSpace#MIN_LARGE_OBJECT_SIZE}.
     * This is what can be allocated.
     */
    long totalFreeBlocks;

    /**
     * Total number of blocks of chunks that cannot be allocated because too small
     * (i.e., smaller than {@link LargeObjectSpace#MIN_LARGE_OBJECT_SIZE}).
     */
    long totalUnusableBlocks;

    /**
     * Large object space sweeping support for tracking the last processed address.
     */
    private Address endOfLastProcessedChunk;

    private long bitmaskForList(int listIndex) {
        return 1L << listIndex;
    }

    private boolean isListEmpty(int listIndex) {
        return (sizeTableSummary & bitmaskForList(listIndex)) != 0L;
    }

    private void addToSummary(int listIndex) {
        sizeTableSummary |= bitmaskForList(listIndex);
    }

    private void removeFromSummary(int listIndex) {
        sizeTableSummary &= ~bitmaskForList(listIndex);
    }

    public LargeObjectSpace() {
        committedHeapSpace = new ContiguousHeapSpace("LOS");
        int numChunkSizeLists = 1 + VERY_LARGE_CHUNK_SIZE;
        chunkSizeTable = new Pointer[numChunkSizeLists];

        sizeTableSummary = 0L;
    }

    public Size alignment() {
        return Size.fromInt(ALIGNMENT_REQUIREMENT);
    }

    public void initialize(Address start, Size initSize, Size maxSize) {
        FatalError.check(initSize.lessEqual(maxSize) &&
                        maxSize.greaterEqual(MIN_LARGE_OBJECT_SIZE) &&
                        start.isAligned(ALIGNMENT_REQUIREMENT),
                        "Incorrect initial setup for large object space");

        Size adjustedMaxSize = maxSize.roundedUpBy(MIN_LARGE_OBJECT_SIZE);
        Size adjustedInitSize = maxSize.roundedUpBy(MIN_LARGE_OBJECT_SIZE);

        // The chunkSizeTableStart pointer is set so that chunkSizeTableStart.getWord(size >> LOG2_BLOCK_SIZE)
        // points to the appropriate list.
        // Given that BLOCK_SIZE >> LOG2_BLOCK_SIZE == 1, we just need to set the start at the address
        // of the first element (index 0) minus 1.
        ArrayLayout layout = vmConfig().layoutScheme().referenceArrayLayout;
        Pointer tableOrigin = Reference.fromJava(chunkSizeTable).toOrigin();
        Pointer tableFirstElementPointer = tableOrigin.plus(layout.getElementOffsetInCell(0));
        chunkSizeTableStart = tableFirstElementPointer.minus(layout.getElementOffsetFromOrigin(1));

        if (!committedHeapSpace.reserve(start, adjustedMaxSize)) {
            MaxineVM.reportPristineMemoryFailure("large object space", "reserve", adjustedMaxSize);
        }
        if (!committedHeapSpace.growCommittedSpace(adjustedInitSize)) {
            MaxineVM.reportPristineMemoryFailure("large object space", "commit", adjustedInitSize);
        }

        // Initial set up.

        int index = listIndex(adjustedInitSize);
        addChunk(getHeadAddress(index), committedHeapSpace.start().asPointer(), adjustedInitSize);

        if (index < VERY_LARGE_CHUNK_LIST) {
            addToSummary(index);
            totalFreeBlocks = index;
            // No space for tail allocation.
            top = Pointer.zero();
            end = Pointer.zero();
            realEnd = Pointer.zero();
        } else {
            totalFreeBlocks = adjustedInitSize.unsignedShiftedRight(LOG2_BLOCK_SIZE).toLong();
            refillTail();
        }

    }

    @INLINE
    protected static Size numBlocksToBytes(int numBlocks) {
        return Size.fromInt(numBlocks).shiftedLeft(LOG2_BLOCK_SIZE);
    }

    @INLINE
    protected static int smallSizeToNumBlocks(Size size) {
        return size.toInt() >>> LOG2_BLOCK_SIZE;
    }

    /**
     * Return the index to the block table where the list tracking chunks of size equals to the
     * specified number of block is stored.
     * @param numBlocks
     * @return index from the block table start to a list of block
     */
    protected static int listIndex(int numBlocks) {
        return (numBlocks >= VERY_LARGE_CHUNK_LIST)  ? VERY_LARGE_CHUNK_LIST : numBlocks;
    }

    /**
     * Return what free chunk list to allocate from for the specified size.
     * @param size
     * @return an index to the chunk size table.
     */
    protected static int listIndex(Size size) {
        if (size.greaterThan(VERY_LARGE_CHUNK_SIZE)) {
            return VERY_LARGE_CHUNK_LIST;
        }
        // size is guaranteed to fit in an int.
        return smallSizeToNumBlocks(size);
    }


    /**
     * Set the head of the free list at the specified index in the chunk size table.
     * @param chunk
     * @param listIndex
     */
    protected void setHead(Pointer chunk, int listIndex) {
        chunkSizeTableStart.setWord(listIndex, chunk);
    }

    /**
     * Get the head of the free list at the specified index in the chunk size table.
     * @param listIndex
     * @return
     */
    protected Pointer getHead(int listIndex) {
        return chunkSizeTableStart.getWord(listIndex).asPointer();
    }

    protected Pointer getHeadAddress(int listIndex) {
        return chunkSizeTableStart.plusWords(listIndex);
    }


    protected void addChunk(Pointer headPointer, Address chunk, Size sizeInBytes) {
        DLinkedHeapFreeChunk.insertUnformattedBefore(headPointer, chunk, sizeInBytes);
    }

    protected void addSmallChunk(Address chunk, Size sizeInBytes) {
        // Number of blocks is also the list index.
        int numBlocks = smallSizeToNumBlocks(sizeInBytes);
        addChunk(getHeadAddress(numBlocks), chunk, sizeInBytes);
        if (numBlocks < MIN_NUM_BLOCKS) {
            totalUnusableBlocks += numBlocks;
        } else {
            addToSummary(numBlocks);
        }
    }

    /**
     * Helper for sweeping. Record a chunk notified by the sweeper in the appropriate
     * list of free chunks. Chunks are notified in address order (according to the
     * direction of the sweep), so a simple FIFO insert guarantees the order.
     * @param chunk
     * @param sizeInBytes
     */
    protected void recordFreeChunk(Address chunk, Size sizeInBytes) {
        if (MaxineVM.isDebug()) {
            FatalError.check(committedHeapSpace.contains(chunk) && sizeInBytes.remainder(BLOCK_SIZE) == 0,
                "Large object space dead space should always be an integral number of blocks");
            FatalError.check(chunk.greaterEqual(endOfLastProcessedChunk), "dead space alread swept");
        }
        if (sizeInBytes.greaterThan(VERY_LARGE_CHUNK_SIZE)) {
            addChunk(getHeadAddress(VERY_LARGE_CHUNK_LIST), chunk, sizeInBytes);
        } else {
            addSmallChunk(chunk, sizeInBytes);
        }
        totalFreeBlocks += sizeInBytes.unsignedShiftedRight(LOG2_BLOCK_SIZE).toLong();
    }

    protected Pointer nextChunk(Pointer chunk) {
        return HeapFreeChunk.getFreechunkSize(chunk).asPointer();
    }

    private void resetTail() {
        top = Pointer.zero();
        realEnd = Pointer.zero();
        end = Pointer.zero();
    }

    private boolean refillTail() {
        top = DLinkedHeapFreeChunk.removeHead(getHeadAddress(VERY_LARGE_CHUNK_LIST));
        if (top.isZero()) {
            realEnd = Pointer.zero();
            end = Pointer.zero();
            return false;
        }
        realEnd = top.plus(DLinkedHeapFreeChunk.getFreechunkSize(top));
        end = realEnd.roundedDownBy(VERY_LARGE_CHUNK_SIZE);
        return true;
    }

    /**
     * Allocate at the tail of the large object space.
     * The tail is the very large block with the lowest address.
     * Allocation hits there first, then if failed, search the very large object list
     * to satisfy allocation. If that failed too, a zero pointer is returned.
     * @param size size in bytes
     * @return
     */
    private Pointer tailAllocate(Size size) {
        while (true) {
            Pointer allocated = top;
            Pointer newTop  = top.plus(size);
            long numAllocatedBlocks = size.unsignedShiftedRight(LOG2_BLOCK_SIZE).toLong();
            if (newTop.greaterThan(end)) {
                if (newTop.lessEqual(realEnd)) {
                    // Can fit the requested size. Dispatch the left over to the appropriate list.
                    Size spaceLeft = realEnd.minus(newTop).asSize();
                    addSmallChunk(newTop, spaceLeft);
                    // Reset tail. Will be refilled on next request.
                    resetTail();
                    totalFreeBlocks -= numAllocatedBlocks;
                    return allocated;
                }
                // Requested size larger than space available at the tail.
                // First, refill if the tail is empty.
                if (top.isZero()) {
                    // Try refill and allocate.
                    if (!refillTail()) {
                        return Pointer.zero();
                    }
                    // retry
                    continue;
                }
                // Can't fit request in the tail. Check the very large chunk list.
                Pointer firstFit = HeapFreeChunk.firstFit(getHeadAddress(VERY_LARGE_CHUNK_LIST), size);
                if (firstFit.isZero()) {
                    return Pointer.zero();
                }

                Size chunkSize = HeapFreeChunk.getFreechunkSize(firstFit);
                Size sizeLeft = chunkSize.minus(size);
                if (!sizeLeft.isZero()) {
                    if (sizeLeft.toLong() <= VERY_LARGE_CHUNK_SIZE) {
                        addSmallChunk(firstFit.plus(size), sizeLeft);
                    } else {
                        // Still a very large chunk. Just need to truncate what's left, leaving it in its place
                        // in the address ordered list.
                        Pointer truncated = DLinkedHeapFreeChunk.truncateLeft(firstFit, size);
                        if (firstFit.equals(getHead(VERY_LARGE_CHUNK_LIST))) {
                            setHead(truncated, VERY_LARGE_CHUNK_LIST);
                        }
                    }
                }
                totalFreeBlocks -= numAllocatedBlocks;
                return firstFit;
            }
            top = newTop;
            totalFreeBlocks -= numAllocatedBlocks;
            return allocated;
        }
    }

    /**
     * Split specified block to allocate numBlocks and update chunkSizeTable accordingly.
     * @param listIndex index of the list whose first block is to be split
     * @param splitListIndex
     * @param numBlocks
     * @return
     */
    private Pointer splitAllocate(int listIndex, int splitListIndex, int numBlocks) {
        // Remove the chunk from its list.
        Pointer allocated = HeapFreeChunk.removeFirst(getHead(listIndex));
        if (getHead(listIndex).isZero()) {
            removeFromSummary(listIndex);
        }
        // Split the chunk and add the remainder to the appropriate list.
        // The allocated space is the left part of the split, the remainder is the right side.
        Pointer head = getHead(splitListIndex);
        Pointer splitOff = HeapFreeChunk.splitRight(allocated, numBlocksToBytes(numBlocks), head);
        setHead(splitOff, splitListIndex);
        return allocated;
    }

    public Pointer allocate(Size size) {
        if (sizeTableSummary != 0L && size.lessEqual(VERY_LARGE_CHUNK_SIZE)) {
            // At least one list is not empty.
            int numBlocks = smallSizeToNumBlocks(size);
            int listIndex = numBlocks;
            Pointer allocated = HeapFreeChunk.removeFirst(getHead(listIndex));
            if (!allocated.isZero()) {
                totalFreeBlocks -= numBlocks;
                if (getHead(listIndex).isZero()) {
                    removeFromSummary(listIndex);
                }
                return allocated;
            }

            // Search for a coarser chunk that we can split without leaving dark matter.
            listIndex += MIN_NUM_BLOCKS;

            int bitIndex = Pointer.fromLong(sizeTableSummary >>> listIndex).leastSignificantBitSet();
            if (bitIndex >= 0) {
                listIndex += bitIndex;
                int remainderListIndex = listIndex - numBlocks;
                addToSummary(remainderListIndex);
                totalFreeBlocks -= numBlocks;
                return splitAllocate(listIndex, remainderListIndex, numBlocks);
            }

            // Didn't find a list
            // Try lists that will leave dark matter behind.
            bitIndex = Pointer.fromLong(sizeTableSummary >>> numBlocks).leastSignificantBitSet();
            if (bitIndex >= 0) {
                listIndex = numBlocks + bitIndex;
                totalFreeBlocks -= listIndex;
                totalUnusableBlocks += bitIndex;
                return splitAllocate(listIndex, bitIndex, numBlocks);
            }
        }
        return tailAllocate(size);
    }

    protected Pointer getNextChunk(Pointer chunk) {
        return HeapFreeChunk.getFreeChunkNext(chunk).asPointer();
    }


    public Pointer allocateCleared(Size size) {
        Pointer cell = allocate(size);
        Memory.clearWords(cell, size.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt());
        return cell;
    }

    @Override
    public void beginSweep() {
        endOfLastProcessedChunk = committedHeapSpace.start();
        // Drop every list on the floor.
        WordArray.fill(chunkSizeTable,  Pointer.zero());
    }

    @Override
    public Size endSweep() {
        return Size.fromLong(totalFreeBlocks << LOG2_BLOCK_SIZE);
    }

    @Override
    public void processDeadSpace(Address freeChunk, Size size) {
        recordFreeChunk(freeChunk, size);
        endOfLastProcessedChunk = freeChunk.plus(size);
    }

    @Override
    public Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject) {
        Pointer endOfLeftObject = leftLiveObject.plus(Layout.size(Layout.cellToOrigin(leftLiveObject)));
        Size numDeadBytes = rightLiveObject.minus(endOfLeftObject).asSize();
        if  (!numDeadBytes.isZero()) {
            recordFreeChunk(endOfLeftObject, numDeadBytes);
        }
        return rightLiveObject.plus(Layout.size(Layout.cellToOrigin(rightLiveObject)));
    }

    @Override
    public Pointer processLiveObject(Pointer liveObject) {
        final Size numDeadBytes = liveObject.minus(endOfLastProcessedChunk).asSize();
        if (!numDeadBytes.isZero()) {
            recordFreeChunk(endOfLastProcessedChunk, numDeadBytes);
            endOfLastProcessedChunk = liveObject.plus(Layout.size(Layout.cellToOrigin(liveObject)));
        }
        return endOfLastProcessedChunk.asPointer();
    }

    @Override
    public void verify(AfterMarkSweepVerifier verifier) {
        FatalError.unimplemented();
    }

    @Override
    public Size minReclaimableSize() {
        return Size.fromInt(BLOCK_SIZE);
    }

}
