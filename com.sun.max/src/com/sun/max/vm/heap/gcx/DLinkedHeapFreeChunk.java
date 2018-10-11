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

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * A Circular doubly linked list of free heap chunk.
 * Ease fast insertion and removal.
 */
public class DLinkedHeapFreeChunk extends HeapFreeChunk {

    public static final DynamicHub DLINKED_HEAP_FREE_CHUNK_HUB = ClassActor.fromJava(DLinkedHeapFreeChunk.class).dynamicHub();

    /**
     * Index of the word storing "prev" field of the heap free chunk.
     */
    private static final int PREV_INDEX;

    static {
        PREV_INDEX = ClassRegistry.findField(DLinkedHeapFreeChunk.class, "prev").offset() >> Word.widthValue().log2numberOfBytes;
    }

    @INLINE
    public static Address getFreeChunkPrev(Address chunkAddress) {
        return chunkAddress.asPointer().getWord(PREV_INDEX).asAddress();
    }

    @INLINE
    public static void setFreeChunkPrev(Address chunkAddress, Address prevChunkAddress) {
        chunkAddress.asPointer().setWord(PREV_INDEX, prevChunkAddress);
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native DLinkedHeapFreeChunk asDLinkedHeapFreeChunk(Object freeChunk);

    @INLINE
    private DLinkedHeapFreeChunk next() {
        return asDLinkedHeapFreeChunk(next);
    }

    @INLINE
    static Address from(DLinkedHeapFreeChunk chunk) {
        return Layout.originToCell(Reference.fromJava(chunk).toOrigin());
    }

    @INLINE
    public static DLinkedHeapFreeChunk to(Address address) {
        return asDLinkedHeapFreeChunk(Reference.fromOrigin(Layout.cellToOrigin(address.asPointer())).toJava());
    }

    static DLinkedHeapFreeChunk format(DynamicHub hub, Address deadSpace, Size numBytes, Address nextChunk, Address prevChunk) {
        if (MaxineVM.isDebug()) {
            FatalError.check(hub.isSubClassHub(heapFreeChunkHub().classActor),
                            "Should format with a sub-class of HeapFreeChunk");
        }
        HeapFreeChunk.format(deadSpace, numBytes, nextChunk, hub);
        setFreeChunkPrev(deadSpace, prevChunk);
        return to(deadSpace);
    }

    /**
     * Format dead space into a free chunk.
     * @param deadSpace pointer to  the first word of the dead space
     * @param numBytes size of the dead space in bytes
     * @param nextChunk address to the next {@link DLinkedHeapFreeChunk} in the free list
     * @param prevChunk address to the previous {@link DLinkedHeapFreeChunk} in the free list
     * @return a reference to HeapFreeChunk object just planted at the beginning of the free chunk.
     */
    static DLinkedHeapFreeChunk format(Address deadSpace, Size numBytes, Address nextChunk, Address prevChunk) {
        return format(DLINKED_HEAP_FREE_CHUNK_HUB, deadSpace, numBytes, nextChunk, prevChunk);
    }

    public static DLinkedHeapFreeChunk format(Address deadSpace, Size numBytes) {
        return format(deadSpace, numBytes, Address.zero(), Address.zero());
    }

    /**
     * Unlink the chunk off it's doubly linked list and return the next block.
     * @param dlinkedChunkAddress
     */
    public static Address unlink(Pointer dlinkedChunkAddress) {
        DLinkedHeapFreeChunk chunk = asDLinkedHeapFreeChunk(Reference.fromOrigin(dlinkedChunkAddress));
        DLinkedHeapFreeChunk nextChunk = asDLinkedHeapFreeChunk(chunk.next);
        chunk.prev.next = nextChunk;
        nextChunk.prev = chunk.prev;
        return fromHeapFreeChunk(nextChunk);
    }

    public static Pointer removeHead(Pointer listHead) {
        Pointer head = listHead.getWord().asPointer();
        if (!head.isZero()) {
            DLinkedHeapFreeChunk newHead = asDLinkedHeapFreeChunk(Reference.fromOrigin(head)).next();
            newHead.prev = null;
            listHead.setWord(from(newHead));
        }
        return head;
    }
    /**
     * Remove chunk from linked list headed by cell specified in argument.
     * @param listHead pointer to the location holding the head of the linked list.
     * @param dlinkedChunkAddress
     */
    public static Address unlink(Pointer listHead, Pointer dlinkedChunkAddress) {
        DLinkedHeapFreeChunk chunk = asDLinkedHeapFreeChunk(Reference.fromOrigin(dlinkedChunkAddress));
        if (chunk.next == null) {
            listHead.setWord(Address.zero());
            return Address.zero();
        }
        Address nextChunk = unlink(dlinkedChunkAddress);
        if (listHead.getWord().equals(dlinkedChunkAddress)) {
            listHead.setWord(nextChunk);
        }
        return nextChunk;
    }

    public static boolean isDLinkedHeapFreeChunk(Pointer cell) {
        return Layout.readHubReference(cell).equals(DLINKED_HEAP_FREE_CHUNK_HUB);
    }

    /**
     * Truncate the specified chunk to the left, and return the resulting chunk.
     *
     * @param chunk pointer to the chunk to truncate
     * @param size size the chunk is shortened by on its left
     * @return chunk.plus(size)
     */
    public static Pointer truncateLeft(Pointer chunk, Size size) {
        DLinkedHeapFreeChunk before = asDLinkedHeapFreeChunk(Reference.fromOrigin(chunk));
        DLinkedHeapFreeChunk after = format(chunk.plus(size),
                        before.size.minus(size));
        DLinkedHeapFreeChunk next = before.next();
        if (next != null) {
            after.next = next;
            next.prev = after;
        }
        if (before.prev != null) {
            after.prev = before.prev;
            before.prev.next = after;
        }
        return from(after).asPointer();
    }


    void insertBefore(DLinkedHeapFreeChunk chunk) {
        chunk.next = this;
        chunk.prev = prev;
        prev.next = chunk;
        prev = chunk;
    }


    public static void insertUnformattedBefore(Pointer listHead, Address address, Size size) {
        Pointer first = listHead.getWord().asPointer();
        if (first.isZero()) {
            format(address, size, address, address);
            listHead.setWord(address);
        } else {
            DLinkedHeapFreeChunk headOfList = asDLinkedHeapFreeChunk(Reference.fromOrigin(first));
            headOfList.insertBefore(format(address, size));
        }
    }

    DLinkedHeapFreeChunk prev;
}
