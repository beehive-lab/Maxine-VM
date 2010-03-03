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
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;
import static com.sun.max.vm.VMOptions.*;

/**
 * Simple free heap space management.
 * Nothing ambitious, just to get going and test the tracing algorithm of the future hybrid mark-sweep-evacuate.
 *
 * Free space is tracked using linked list(s)  threaded over the heap build by the heap sweeper.
 * Two free lists are used: one for large objects, and one for common object allocations.
 * The sweeper ignore contiguous space of size smaller than
 * Each chunk of free space is at least 4-words large, and holds in its first word the
 * address to the next free space (or 0 if none) and in its second word its size.
 * Any free space smaller than
 * Tiny objects (i.e., objects of size equals to 2 words) are handled specially and allocated from
 * a special pool.
 *
 * @author Laurent Daynes.
 */
public class FreeHeapSpace extends LinearAllocationMemoryRegion {

    /**
     * Index of the word storing the address to the next free space within the current free heap space.
     */
    private static int NEXT_INDEX = 0;
    private static int SIZE_INDEX = 1;

    private static final VMIntOption largeObjectsMinSizeOption =
        register(new VMIntOption("-XX:LargeObjectsMinSize=", Size.K.times(4).toInt(),
                        "Minimum size to be treated as a large object"), MaxineVM.Phase.PRISTINE);

    private static final VMIntOption freeChunkMinSizeOption =
        register(new VMIntOption("-XX:FreeChunkMinSize=", 512,
        "Minimum size of contiguous space considered for free space management. Below this size, the space is ignored (dark matter)"),
        MaxineVM.Phase.PRISTINE);

    @CONSTANT_WHEN_NOT_ZERO
    static Size minLargeObjectSize;

    @CONSTANT_WHEN_NOT_ZERO
    static Size minFreeChunkSize;

    private static Address getFreeChunkNext(Address chunkAddress) {
        return chunkAddress.asPointer().getWord(NEXT_INDEX).asAddress();

    }
    private static Size getFreechunkSize(Address chunkAddress) {
        return chunkAddress.asPointer().getWord(SIZE_INDEX).asSize();
    }

    private static void setFreeChunkNext(Address chunkAddress, Address nextChunkAddress) {
        chunkAddress.asPointer().setWord(NEXT_INDEX, nextChunkAddress);

    }

    private static void setFreechunkSize(Address chunkAddress, Size size) {
        chunkAddress.asPointer().setWord(SIZE_INDEX, size);
    }

    /**
     * End of the current chunk being used for allocation.
     */
    private Address limit;

    /**
     * Start of the current pool of tiny object free space. Tiny objects (i.e., of size < 2 words) are
     * segregated from the rest of the heap in 1 K chunks, aligned on a 1 K boundary so that
     * all objects of the 1 K chunk fits in the same word of the mark bitmap.
     */
    private Address tinyObjectFreePoolStart;
    private Address tinyObjectFreePoolEnd;

    /**
     * Next free chunks of space.
     */
    private Address freeChunks;

    /**
     *
     */
    private Address largeFreeChunks;

    public FreeHeapSpace() {
        super("Free Heap Space");
    }

    public void initialize(RuntimeMemoryRegion committedSpace) {
        FatalError.check(committedSpace.start().isAligned(Size.K.toInt()), "committed heap space must be 1 K aligned");
        // First off, allocate space for the tiny object pool.
        tinyObjectFreePoolStart = committedSpace.start();
        tinyObjectFreePoolEnd = tinyObjectFreePoolStart.plus(Size.K);
        setStart(committedSpace.start());
        setSize(committedSpace.size());
        mark.set(start());
        limit = end();
        largeFreeChunks = freeChunks = Address.zero();
        minLargeObjectSize = Size.fromInt(largeObjectsMinSizeOption.getValue());
    }

    private boolean isLarge(Size size) {
        return size.greaterThan(minLargeObjectSize);
    }

    /**
     * Find first fit and move it at the head of the list.
     * @param chunkList
     * @param size
     * @return the first chunk in the list that can allocate the specified size, or zero if none.
     */
    private Address findFirstFit(Address chunkList, Size size) {
        FatalError.check(!chunkList.isZero(), "chunk list must not be null");
        Address prevChunk = Address.zero();
        Address chunk = chunkList;
        do {
           if (getFreechunkSize(chunk).greaterEqual(size)) {
               // Found one. Move it ahead of the list.
               Address nextChunk = getFreeChunkNext(chunk);
               setFreeChunkNext(prevChunk, nextChunk);
               setFreeChunkNext(chunk, chunkList);
               return chunk;
           }
           prevChunk = chunk;
           chunk = getFreeChunkNext(chunk);
        } while(!chunk.isZero());
        return Address.zero();
    }

    /**
     *
     * @param size
     */
    private void handleAllocationFailure(Size size) {
        if (isLarge(size)) {

        }
        if (!freeChunks.isZero()) {
            // We have more free space. Find the next one that can accommodate our needs.
            do {
                Size chunkSize = getFreechunkSize(freeChunks);
                if (chunkSize.greaterEqual(size)) {

                }
            } while(!freeChunks.isZero());

        }
        if (!Heap.collectGarbage(size)) {
        }
    }

    private void refillTinyObjectPool() {

    }
    Pointer allocateTinyObject() {
        return null;
    }

    Pointer allocate(Size size) {
        if (MaxineVM.isDebug()) {
            FatalError.check(size.isWordAligned(), "Size must be word aligned");
        }
        Pointer cell;
        Pointer nextMark;
        do {
            cell = mark();
            nextMark = cell.plus(size);
            if (nextMark.greaterThan(limit)) {
                handleAllocationFailure(size);
                // loop back to retry.
                continue;
            }
        } while(mark.compareAndSwap(cell, nextMark) != cell);
        return cell;
    }

}
