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
 * Allocation form the very large object list is performed in best-fit, address ordered fashion.
 * Given that no chunks can enter the list between two GC which re-organize the list in address order anyway,
 * this simply means that allocation follows a first fit search.
 *
 * The configuration of the large object space is currently hard-wired with block size being 2K, minimum object
 * size of 4 KB, and very large object size of 64 KB. At worse, internal fragmentation is 2KB.
 *
 * TODO: investigate interface and mechanism to enable temporary lease of dark matter
 * to other garbage collected space with evacuation support
 *
 * @author Laurent Daynes
 */
public class LargeObjectSpace extends HeapSweeper {

    protected static final int MIN_LARGE_OBJECT_SIZE = Size.K.times(4).toInt();
    protected static final int BLOCK_SIZE = Size.K.times(2).toInt();
    protected static final int VERY_LARGE_CHUNK_SIZE = Size.K.times(64).toInt();

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
    private Pointer[] chunkSizeTable;

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


    private long bitmaskForList(int listIndex) {
        return 1L << listIndex;
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
        // The chunkSizeTableStart pointer is set so that chunkSizeTableStart.getWord(size >> LOG2_BLOCK_SIZE)
        // points to the appropriate list.
        // Given that BLOCK_SIZE >> LOG2_BLOCK_SIZE == 1, we just need to set the start at the address
        // of the first element (index 0) minus 1.
        ReferenceArrayLayout layout = VMConfiguration.target().layoutScheme().referenceArrayLayout;
        Pointer tableOrigin = Reference.fromJava(chunkSizeTable).toOrigin();
        Pointer tableFirstElementPointer = tableOrigin.plus(layout.getElementOffsetInCell(0));
        chunkSizeTableStart = tableFirstElementPointer.minus(layout.getElementOffsetFromOrigin(1));
        sizeTableSummary = 0L;
    }


    public void initialize(Address start, Size initSize, Size maxSize) {
        FatalError.check(initSize.lessEqual(maxSize) &&
                        maxSize.greaterEqual(MIN_LARGE_OBJECT_SIZE),
                        "Incorrect initial setup for large object space");

        Size adjustedMaxSize = maxSize.roundedUpBy(MIN_LARGE_OBJECT_SIZE);
        Size adjustedInitSize = maxSize.roundedUpBy(MIN_LARGE_OBJECT_SIZE);

        if (!committedHeapSpace.reserve(start, adjustedMaxSize)) {
            MaxineVM.reportPristineMemoryFailure("large object space", "reserve", adjustedMaxSize);
        }
        if (!committedHeapSpace.growCommittedSpace(adjustedInitSize)) {
            MaxineVM.reportPristineMemoryFailure("large object space", "commit", adjustedInitSize);
        }
        top = committedHeapSpace.start().asPointer();

        int index = listIndex(adjustedInitSize);
        if (index < VERY_LARGE_CHUNK_LIST) {
            addChunk(index, top);
            top = top.plus(adjustedInitSize);
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
        return (numBlocks > VERY_LARGE_CHUNK_LIST)  ? VERY_LARGE_CHUNK_LIST : numBlocks;
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
        return listIndex(smallSizeToNumBlocks(size));
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


    protected void addChunk(Pointer headPointer, Pointer chunk, Size sizeInBytes) {
        HeapFreeChunk.format(chunk, sizeInBytes, headPointer.getWord().asPointer());
        headPointer.setWord(chunk);
    }
    /**
     * Add a chunk to the list of free chunk at the specified index of the chunkSizeTable.
     * The chunk is formatted as a free chunk list element before being inserted at the
     * head of the list.
     *
     * @param index index to the free list of chunk
     * @param chunk chunk to be added.
     */
    protected void addChunk(int index, Pointer chunk) {
        addChunk(getHeadAddress(index), chunk, numBlocksToBytes(index));
        addToSummary(index);
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
            if (newTop.greaterThan(end)) {
                if (newTop.lessEqual(realEnd)) {
                    // Can fit the requested size. Dispatch the left over to the appropriate list.
                    Size spaceLeft = realEnd.minus(newTop).asSize();
                    int numBlocksLeft = smallSizeToNumBlocks(spaceLeft);
                    addChunk(getHeadAddress(numBlocksLeft), newTop, spaceLeft);
                    // Reset tail. Will be refilled on next request.
                    resetTail();
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
                final int list = listIndex(sizeLeft);
                if (list < VERY_LARGE_CHUNK_LIST) {
                    Pointer chunkLeft = firstFit.plus(size);
                    addChunk(getHeadAddress(list), chunkLeft, sizeLeft);
                } else {
                    // Still a very large chunk. Just need to truncate what's left, leaving it at its place
                    // in the address ordered list.
                    Pointer truncated = DLinkedHeapFreeChunk.truncateLeft(firstFit, size);
                    if (firstFit.equals(getHead(list))) {
                        setHead(truncated, list);
                    }
                }
                return firstFit;
            }
            top = newTop;
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
        Pointer allocated = getHead(listIndex);
        Pointer newHead = nextChunk(allocated);
        setHead(newHead, listIndex);
        if (newHead.isZero()) {
            removeFromSummary(listIndex);
        }
        // Split the chunk and add the remainder to the appropriate list.
        // The allocated space is the left part of the split, the remainder is the right side.
        Pointer head = getHead(splitListIndex);
        Pointer splitOff = HeapFreeChunk.splitRight(allocated, numBlocksToBytes(numBlocks), head);
        setHead(splitOff, splitListIndex);
        return allocated;
    }

    private Pointer handleBlockAllocationFailure(int numBlocks) {
        if (sizeTableSummary != 0L) {
            // At least one list is not empty.
            // Search for a coarser chunk that we can split without leaving dark matter.
            int listIndex = numBlocks + MIN_NUM_BLOCKS;
            int bitIndex = Pointer.fromLong(sizeTableSummary >>> listIndex).leastSignificantBitSet();
            if (bitIndex >= 0) {
                listIndex += bitIndex;
                int remainderListIndex = listIndex - numBlocks;
                addToSummary(remainderListIndex);
                return splitAllocate(listIndex, remainderListIndex, numBlocks);
            }

            // Didn't find a list
            // Try lists that will leave dark matter behind.
            bitIndex = Pointer.fromLong(sizeTableSummary >>> numBlocks).leastSignificantBitSet();
            if (bitIndex >= 0) {
                listIndex = numBlocks + bitIndex;
                return splitAllocate(listIndex, bitIndex, numBlocks);
            }
        }
        // No space in any of the previous list.
        // Carve up the end of the large object space.
        return tailAllocate(numBlocksToBytes(numBlocks));
    }

    protected Pointer getNextChunk(Pointer chunk) {
        return HeapFreeChunk.getFreeChunkNext(chunk).asPointer();
    }

    /**
     * Return pointer to allocated cell of the specified cell, or the zero pointer
     * if running short of memory.
     */
    public Pointer allocate(Size size) {
        if (size.greaterThan(VERY_LARGE_CHUNK_SIZE)) {
            return tailAllocate(size);
        }
        int index = smallSizeToNumBlocks(size);
        Pointer chunk = getHead(index);
        if (chunk.isZero()) {
            chunk = handleBlockAllocationFailure(index);
        } else {
            // Otherwise, remove the first block off the list
            // FIXME: we want this to be customized by sub-classes which may not use a simple linked list,
            // and may further requires post-processing of the allocated chunk.
            Pointer nextBlock = getNextChunk(chunk);
            setHead(nextBlock, index);
        }
        return chunk;
    }

    public Pointer allocateCleared(Size size) {
        Pointer cell = allocate(size);
        Memory.clearWords(cell, size.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt());
        return cell;
    }

    @Override
    public void processDeadSpace(Address freeChunk, Size size) {
        // TODO Auto-generated method stub

    }

    @Override
    public Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pointer processLiveObject(Pointer liveObject) {
        // TODO Auto-generated method stub
        return null;
    }
}
