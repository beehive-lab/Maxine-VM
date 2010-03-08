package com.sun.max.vm.heap.gcx;

import static com.sun.c1x.bytecode.Bytecodes.*;

import com.sun.c1x.bytecode.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

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
class HeapFreeChunk {

    static final private Hub heapFreeChunkHub = ClassActor.fromJava(HeapFreeChunk.class).dynamicHub();

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
     * Format dead space into a free chunk
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
        FatalError.check(Safepoint.isDisabled(), "leaking a HeapFreeChunk");
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
