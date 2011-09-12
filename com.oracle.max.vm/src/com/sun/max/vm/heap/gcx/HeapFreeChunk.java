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

import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * A chunk of free space in the heap.
 * Sweepers format re-usable free space as HeapFreeChunk and Long arrays.
 * This ease manipulation by space allocator, inspection of the heap, debugging and
 * heap walking.
 * Reference to HeapFreeChunk must never be stored in object and must never be used
 * when safepoint is enabled, otherwise they become visible to GC and will be considered live.
 * Similarly, direct updates to HeapFreeChunk.next may cause unwanted write-barrier executions.
 *
 * TODO (ld) need to revisit the visibility of this class.
 */
public class HeapFreeChunk {

    public static final DynamicHub HEAP_FREE_CHUNK_HUB = ClassActor.fromJava(HeapFreeChunk.class).dynamicHub();

    /**
     * Index of the word storing "next" field of the heap free chunk.
     */
    protected static final int NEXT_INDEX;
    /**
     * Index of the word storing "size" field of the heap free chunk.
     */
    protected static final int SIZE_INDEX;

    static {
        NEXT_INDEX = ClassRegistry.findField(HeapFreeChunk.class, "next").offset() >> Word.widthValue().log2numberOfBytes;
        SIZE_INDEX = ClassRegistry.findField(HeapFreeChunk.class, "size").offset() >> Word.widthValue().log2numberOfBytes;
    }

    private static final long HEAP_FREE_CHUNK_MARKER = 0xdadadadadadadadaL;

    @INLINE
    public static boolean isInDeadSpace(Address chunkAddress) {
        return chunkAddress.wordAligned().asPointer().getLong() == HEAP_FREE_CHUNK_MARKER;
    }

    @INLINE
    public static boolean isDeadSpaceMark(Address a) {
        return a.equals(deadSpaceMark());
    }

    @INLINE
    public static Address deadSpaceMark() {
        return Address.fromLong(HEAP_FREE_CHUNK_MARKER);
    }

    @INLINE
    public static Address getFreeChunkNext(Address chunkAddress) {
        return chunkAddress.asPointer().getWord(NEXT_INDEX).asAddress();

    }
    @INLINE
    public static Size getFreechunkSize(Address chunkAddress) {
        return chunkAddress.asPointer().getWord(SIZE_INDEX).asSize();
    }

    @INLINE
    public static void setFreeChunkNext(Address chunkAddress, Address nextChunkAddress) {
        chunkAddress.asPointer().setWord(NEXT_INDEX, nextChunkAddress);
    }

    @INLINE
    public static void setFreeChunkSize(Address chunkAddress, Size size) {
        chunkAddress.asPointer().setWord(SIZE_INDEX, size);
    }

    public static boolean isValidChunk(Pointer cell, MemoryRegion chunkRegion) {
        final Address hub = Layout.originToCell(Reference.fromJava(HEAP_FREE_CHUNK_HUB).toOrigin());
        if (cell.readWord(0).asAddress().equals(hub)) {
            Pointer nextChunk = getFreeChunkNext(cell).asPointer();
            if (nextChunk.isZero()) {
                return true;
            }
            if (chunkRegion.contains(nextChunk)) {
                return nextChunk.readWord(0).asAddress().equals(hub);
            }
        }
        return false;
    }

    void dump() {
        Log.print(fromHeapFreeChunk(this));
        Log.print(", ");
        Log.print(size.toInt());
    }

    static void dumpList(HeapFreeChunk head) {
        HeapFreeChunk c = head;
        while (c != null) {
            c.dump();
            Log.print(" -> ");
            c = c.next;
        }
    }

    static HeapFreeChunk format(Address deadSpace, Size numBytes, Address nextChunk, DynamicHub hub) {
        final Pointer cell = deadSpace.asPointer();
        if (MaxineVM.isDebug()) {
            FatalError.check(hub.isSubClassHub(HEAP_FREE_CHUNK_HUB.classActor),
                            "Should format with a sub-class of HeapFreeChunk");
            FatalError.check(numBytes.greaterEqual(HEAP_FREE_CHUNK_HUB.tupleSize), "Size must be at least a heap free chunk size");
            Memory.setWords(cell, numBytes.toInt() >> Word.widthValue().log2numberOfBytes, deadSpaceMark());
        }
        Cell.plantTuple(cell, hub);
        Layout.writeMisc(Layout.cellToOrigin(cell), Word.zero());
        setFreeChunkSize(cell, numBytes);
        setFreeChunkNext(cell, nextChunk);
        return toHeapFreeChunk(cell);
    }
    /**
     * Format dead space into a free chunk.
     * @param deadSpace pointer to  the first word of the dead space
     * @param numBytes size of the dead space in bytes
     * @return a reference to HeapFreeChunk object just planted at the beginning of the free chunk.
     */
    static HeapFreeChunk format(Address deadSpace, Size numBytes, Address nextChunk) {
        return format(deadSpace, numBytes, nextChunk, HEAP_FREE_CHUNK_HUB);
    }

    @INLINE
    static HeapFreeChunk format(Address deadSpace, Size numBytes) {
        return format(deadSpace, numBytes, Address.zero());
    }

    /**
     * Split a chunk. Format the right side of the split as a free chunk, and return
     * its address.
     * @param chunk the original chunk
     * @param size size of the left chunk.
     * @return
     */
    static Pointer splitRight(Address chunk, Size leftChunkSize, Address rightNextFreeChunk) {
        HeapFreeChunk originalChunk = toHeapFreeChunk(chunk);
        Size rightSize = originalChunk.size.minus(leftChunkSize);
        HeapFreeChunk rightChunk = format(chunk.plus(leftChunkSize), rightSize, rightNextFreeChunk);
        originalChunk.size = leftChunkSize;
        return fromHeapFreeChunk(rightChunk).asPointer();
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native HeapFreeChunk asHeapFreeChunk(Object freeChunk);

    @INLINE
    public static HeapFreeChunk toHeapFreeChunk(Address cell) {
        return asHeapFreeChunk(Reference.fromOrigin(Layout.cellToOrigin(cell.asPointer())).toJava());
    }

    @INLINE
    static Address fromHeapFreeChunk(HeapFreeChunk chunk) {
        return Layout.originToCell(Reference.fromJava(chunk).toOrigin());
    }

    public static void makeParsable(Address headOfFreeChunkListAddress) {
        Address chunkAddress = headOfFreeChunkListAddress;
        while (!chunkAddress.isZero()) {
            Pointer start = chunkAddress.asPointer();
            Pointer end = start.plus(HeapFreeChunk.getFreechunkSize(chunkAddress));
            chunkAddress =  HeapFreeChunk.getFreeChunkNext(chunkAddress);
            HeapSchemeAdaptor.fillWithDeadObject(start, end);
        }
    }

    /**
     * Find the first chunk in a list of chunks that can accommodate the requested number of bytes.
     * @param head a pointer to a HeapFreeChunk
     * @param size size in bytes
     * @return a chunk of size greater or equal to {code size}, null otherwise.
     */
    public static Pointer firstFit(Pointer head, Size size) {
        return fromHeapFreeChunk(toHeapFreeChunk(head.getWord().asAddress()).firstFit(size)).asPointer();
    }

    public static Pointer removeFirst(Pointer head) {
        Pointer first = head.getWord().asPointer();
        if (!first.isZero()) {
            toHeapFreeChunk(first).removeFirstFromList(head);
        }
        return first;
    }

    protected void removeFirstFromList(Pointer head) {
        head.setWord(fromHeapFreeChunk(next));
    }

    final HeapFreeChunk firstFit(Size size) {
        HeapFreeChunk chunk = this;
        while (chunk != null) {
            if (chunk.size.greaterEqual(size)) {
                return chunk;
            }
            chunk = chunk.next;
        }
        return null;
    }

    /**
     * Heap Free Chunk are never allocated.
     */
    protected HeapFreeChunk() {
    }

    /**
     * Size of the chunk in bytes (including the size of the instance of HeapFreeChunk prefixing the chunk).
     */
    Size size;
    /**
     * A link to a next free chunk in a linked list.
     */
    HeapFreeChunk next;
}
