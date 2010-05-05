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

import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;

/**
 * A chunk of free space in the heap.
 * Sweepers format re-usable free space as HeapFreeChunk and Long arrays.
 * This ease manipulation by space allocator, inspection of the heap, debugging and
 * heap walking.
 * Reference to HeapFreeChunk must never be stored in object and must never be used
 * when safepoint is enabled, otherwise they become visible to GC and will be considered live.
 * Similarly, direct updates to HeapFreeChunk.next may cause unwanted write-barrier executions.
 *
 * FIXME: need to revisit the visibility of this class.
 * @author Laurent Daynes
 */
public final class HeapFreeChunk {

    public static final DynamicHub HEAP_FREE_CHUNK_HUB = ClassActor.fromJava(HeapFreeChunk.class).dynamicHub();

    /**
     * Index of the word storing "next" field of the heap free chunk.
     */
    private static final int NEXT_INDEX;
    /**
     * Index of the word storing "size" field of the heap free chunk.
     */
    private static final int SIZE_INDEX;

    static {
        NEXT_INDEX = HEAP_FREE_CHUNK_HUB.classActor.findFieldActor(SymbolTable.makeSymbol("next")).offset() >> Word.widthValue().log2numberOfBytes;
        SIZE_INDEX = HEAP_FREE_CHUNK_HUB.classActor.findFieldActor(SymbolTable.makeSymbol("size")).offset() >> Word.widthValue().log2numberOfBytes;
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

    public static boolean isValidChunk(Pointer cell, LinearAllocationMemoryRegion chunkRegion) {
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

    /**
     * Format dead space into a free chunk.
     * @param deadSpace pointer to  the first word of the dead space
     * @param numBytes size of the dead space in bytes
     * @return a reference to HeapFreeChunk object just planted at the beginning of the free chunk.
     */
    static HeapFreeChunk format(Address deadSpace, Size numBytes, Address nextChunk) {
        final Pointer cell = deadSpace.asPointer();
        if (MaxineVM.isDebug()) {
            DebugHeap.writeCellPadding(cell, numBytes.toInt() >> Word.widthValue().log2numberOfBytes);
        }
        Cell.plantTuple(cell, HEAP_FREE_CHUNK_HUB);
        Layout.writeMisc(Layout.cellToOrigin(cell), Word.zero());
        setFreeChunkSize(cell, numBytes);
        setFreeChunkNext(cell, nextChunk);
        return toHeapFreeChunk(cell);
    }

    @INLINE
    static HeapFreeChunk format(Address deadSpace, Size numBytes) {
        return format(deadSpace, numBytes, Address.zero());
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native HeapFreeChunk asHeapFreeChunk(Object freeChunk);

    @INLINE
    public static HeapFreeChunk toHeapFreeChunk(Address address) {
        return asHeapFreeChunk(Reference.fromOrigin(Layout.cellToOrigin(address.asPointer())).toJava());
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
     * Heap Free Chunk are never allocated.
     */
    private HeapFreeChunk() {
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
