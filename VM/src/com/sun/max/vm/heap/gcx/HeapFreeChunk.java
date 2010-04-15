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
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.*;

/**
 * A chunk of free space in the heap.
 * Sweepers format re-usable free space as HeapFreeChunk and Long arrays.
 * This ease manipulation by space allocator, inspection of the heap, debugging and
 * heap walking.
 * Reference to HeapFreeChunk must never be stored in object and must never be used
 * when safepoint is enabled, otherwise they become visible to GC and will be considered live.
 *
 * @author Laurent Daynes
 */
final class HeapFreeChunk {

    private static final  Hub heapFreeChunkHub = ClassActor.fromJava(HeapFreeChunk.class).dynamicHub();

    /**
     * Index of the word storing the address to the next free space within the current free heap space.
     */
    private static final int NEXT_INDEX = 3; // FIXME: should be obtained via the field actor for the corresponding field
    private static final int SIZE_INDEX = 4; // FIXME: same as above

    static Address getFreeChunkNext(Address chunkAddress) {
        return chunkAddress.asPointer().getWord(NEXT_INDEX).asAddress();

    }
    static Size getFreechunkSize(Address chunkAddress) {
        return chunkAddress.asPointer().getWord(SIZE_INDEX).asSize();
    }

    static void setFreeChunkNext(Address chunkAddress, Address nextChunkAddress) {
        chunkAddress.asPointer().setWord(NEXT_INDEX, nextChunkAddress);
    }

    static void setFreechunkSize(Address chunkAddress, Size size) {
        chunkAddress.asPointer().setWord(SIZE_INDEX, size);
    }

    /**
     * Format dead space into a free chunk.
     * @param deadSpace
     * @param size
     * @return
     */
    static HeapFreeChunk format(Address deadSpace, Size size) {
        Cell.plantTuple(deadSpace.asPointer(), heapFreeChunkHub);
        HeapFreeChunk freeChunk = toHeapFreeChunk(deadSpace);
        freeChunk.size = size;
        freeChunk.next = null;
        return freeChunk;
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native HeapFreeChunk asHeapFreeChunk(Object freeChunk);

    static HeapFreeChunk toHeapFreeChunk(Address address) {
        return asHeapFreeChunk(Reference.fromOrigin(address.asPointer()).toJava());
    }

    /**
     * Heap Free Chunk are never allocated.
     */
    private HeapFreeChunk() {
    }

    Size size;
    HeapFreeChunk next;

}
