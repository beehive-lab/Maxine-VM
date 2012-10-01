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

import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.*;

/**
 * A tagged large object space.
 * This extends {@link LargeObjectSpace} with support for multiple generations,
 * in-place promotion and immediate coalescing of free space.
 * All of the above is accomplished by tagging the end of allocated chunks with
 * objects that are nodes of a doubly linked list, and tagging both the start and end
 * of free chunks.
 * A tagged large object space is useful both in copying and generational collectors
 * to support non-moving collection of large objects.
 * All allocated objects are recorded in a doubly linked list using a
 * suffix object allocated at the end of the last block of the chunk hosting the
 * object. The suffix is used at the end of free chunks to quickly identify the end
 * of free chunk and enable simple coalescing of free chunks.
 *
 * In place collection is achieved by finding and marking the tag during tracing, and
 * sweeping the linked list to identify unmarked chunks, free them and attempt to coalesce
 * them with other free chunks.
 * Multiple generation are supported by multiple list (one per generation).
 * Promoting an object simply consists of moving it to another list.
 */
public class TaggedLargeObjectSpace extends LargeObjectSpace {

    /**
     * List of allocated large objects maintained by the space.
     */
    final Pointer [] largeObjectLists;

    /**
     * List where allocated object land by default.
     */
    final int defaultAllocatingObjectList;

    /**
     * Construct a large object space with the specified number of object lists. Use list 0
     * as the list allocated object get assigned to by default.
     *
     * @param numLargeObjectLists number of list to use.
     */
    public TaggedLargeObjectSpace(int numLargeObjectLists) {
        this(numLargeObjectLists, 0);
    }

    /**
     * Construct a tagged large object space with the specified number of object lists and using the
     * specified list as the list where allocated large object.
     * Generational heap would typically specified one list per-generation and set the list
     * of the youngest generation to be the default allocating list.
     * @param numLargeObjectLists
     * @param defaultAllocatingList
     */
    public TaggedLargeObjectSpace(int numLargeObjectLists, int defaultAllocatingList) {
        super();
        largeObjectLists = new Pointer[numLargeObjectLists];
        defaultAllocatingObjectList = defaultAllocatingList;
    }

    /**
     * Removed the chunk from its free list.
     * @param blockStart start of the chunk
     * @param numBlocks size of the chunk in number of BLOCK_SIZE blocks
     */
    protected void removeBlock(Pointer blockStart, int numBlocks) {
        Pointer listHead = getHead(listIndex(numBlocks));
        DLinkedHeapFreeChunk.unlink(blockStart, listHead);
    }

    /**
     * Try coalescing the specified dead large objects with its immediate surrounding.
     *
     * @param deadLargeObjectAddress the address of the dead large object
     * @return true if the object was coalesced.
     */
    private boolean coalesce(Pointer deadLargeObjectAddress) {
        final Pointer origin = Layout.cellToOrigin(deadLargeObjectAddress);
        Size size = Layout.size(origin);
        int totalContigousFreeBlocks = size.toInt() >>> LOG2_BLOCK_SIZE;
        Pointer blockStart = deadLargeObjectAddress;
        boolean coalesced = false;
        Pointer freeChunkBefore = LargeObjectTag.freeChunkBefore(deadLargeObjectAddress);
        Pointer freeChunkAfter = LargeObjectTag.freeChunkAfter(deadLargeObjectAddress);

        if (!freeChunkAfter.isZero()) {
            // Remove the chunk from it's list. It's going to be coalesced with the freed chunk.
            int numBlocks = DLinkedHeapFreeChunk.getFreechunkSize(freeChunkBefore).toInt();
            removeBlock(freeChunkAfter, numBlocks);
            totalContigousFreeBlocks += numBlocks;
            coalesced = true;
        }

        if (!freeChunkBefore.isZero()) {
            int numBlocks = DLinkedHeapFreeChunk.getFreechunkSize(freeChunkBefore).toInt();
            blockStart = freeChunkBefore;
            removeBlock(freeChunkBefore, numBlocks);
            totalContigousFreeBlocks += numBlocks;
            // Unlink the block from its list.
            coalesced = true;
        }
        // Free the block
        Size totalSize = numBlocksToBytes(totalContigousFreeBlocks);
        addChunk(getHeadAddress(listIndex(totalSize)), blockStart, totalSize);
        return coalesced;
    }

    /**
     * Return pointer to allocated cell of the specified cell, or the zero pointer
     * if running short of memory.
     */
    @Override
    public Pointer allocate(Size size) {
        return allocate(size, defaultAllocatingObjectList);
    }

    private Pointer allocate(Size size, int list) {
        Pointer allocated = super.allocate(size.plus(LargeObjectTag.tagSize()));
        if (!allocated.isZero()) {
            // Tag the end of its last block to ease future coalescing.
            LargeObjectTag.setTag(allocated, size, list);
        }
        return allocated;
    }
}

