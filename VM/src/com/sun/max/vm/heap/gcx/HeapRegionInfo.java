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
import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;
import static com.sun.max.vm.heap.gcx.HeapRegionInfo.Flag.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
/**
 * Descriptor of heap region.
 * The information recorded is carefully crafted so that a zero-filled HeapRegionInfo
 * instance represents the state of a free region, not allocated to any heap account.
 * This avoids costly initialization of the {@link RegionTable} entries at startup.
 *
 * @author Laurent Daynes
 */
public class HeapRegionInfo {

    // revisit this. What we need is bitfield, e.g.,
    // bits 0 to 3 holds enumerated values EMPTY, FULL, HAS_FREE_CHUNK, ALLOCATING.
    // bits 4 to 5 ... etc.
    // The following isn't expressive enough.
    static enum Flag {
        /**
         * Flags indicating that the region has no space left for allocation.
         * Note that it doesn't mean that it is 100% occupied.
         */
        FULL,
        /**
         * Region has a list of {@link HeapFreeChunk} tracking space available for allocation within the region.
         * The head of the list is located at the {@link HeapRegionInfo#firstFreeChunkIndex} word in the region.
         */
        HAS_FREE_CHUNK,
        /**
         * Region is being used by an allocator. It's free chunks information cannot be trusted.
         */
        ALLOCATING;

        private final int mask = 1 << ordinal();

        public final boolean isSet(int flags) {
            return (flags & mask) != 0;
        }
        public final boolean isClear(int flags) {
            return (flags & mask) == 0;
        }
        public final int or(int flags) {
            return flags | mask;
        }
        public final int and(int flags) {
            return flags & mask;
        }
        public final int xor(int flags) {
            return flags ^ mask;
        }

        public int clear(int flags) {
            return flags & ~mask;
        }

        /**
         * Check that the flag set is a legal combination.
         * @param flags the flags to be checked.
         * @return true if the flag set defines a legal combination, false otherwise
         */
        public static boolean isValidFlagSet(int flags) {
            return !(FULL.isSet(flags) && HAS_FREE_CHUNK.isSet(flags));
        }
    }

    /**
     * A 32-bit vector compounding several flags information. See {@link Flag} for usage of each of the bits.
     */
    int flags; // NOTE: don't want to use an EnumSet here. Don't want a long for storing flags; and want the flags embedded in the heap region info.

    public boolean isEmpty() {
        return (flags & (FULL.mask | HAS_FREE_CHUNK.mask)) == 0;
    }

    public boolean isFull() {
        return Flag.FULL.isClear(flags);
    }


    HeapRegionInfo() {
        // Not a class one can allocate. Allocation is the responsibility of the region table.
    }

    /**
     * Offset to the next HeapRegionInfo. This takes into account heap padding and alignment issue.
     * HeapRegionInfo objects are allocated in a single contiguous area so they can be accessed as a single
     */
    private static final Size OFFSET_TO_NEXT = ClassActor.fromJava(HeapFreeChunk.class).dynamicHub().tupleSize;

    /**
     * Index, in number of minimum object size relative to the beginning of a region to the first free chunk of the region.
     * Value is 0 if the region is empty.
     */
    short firstFreeChunkIndex;
    /**
     * Number of fragments not holding live data (i.e., dark-matter + free space).
     * TODO: better define this base on actual use of this field!
     */
    short numFragments;
    /**
     * Space available for allocation. This excludes dark matter than cannot be used
     * for allocation.
     */
    short freeSpace;
    /**
     * Amount of live data.
     */
    short liveData;

    HeapAccountOwner owner;

    private int firstFreeChunkOffset() {
        return firstFreeChunkIndex << Word.widthValue().log2numberOfBytes;
    }
    public final int liveInWords() {
        return liveData;
    }

    public final int darkMatterInWords() {
        return regionSizeInWords - (liveData + freeSpace);
    }

    public final int freeSpaceInWords() {
        return freeSpace;
    }

    /**
     * Return the address to the first chunk of space available for allocation.
     * The chunk is formatted as a {@link HeapFreeChunk} only if the region has its {@link Flag#HAS_FREE_CHUNK}
     * set.
     * @return Address to the first chunk of space available for allocation, or zero if the region is full.
     */
    Address firstFreeBytes() {
        return isFull() ? Address.zero() : regionStart().plus(firstFreeChunkOffset());
    }



    @INTRINSIC(UNSAFE_CAST)
    private static native HeapRegionInfo asHeapRegionInfo(Object regionInfo);

    @INLINE
    static HeapRegionInfo toHeapRegionInfo(Pointer regionInfoPointer) {
        return asHeapRegionInfo(Reference.fromOrigin(Layout.cellToOrigin(regionInfoPointer)).toJava());
    }

    /**
     * Start of the region described by this {@link HeapRegionInfo} instance.
     * @return address to the start of a region
     */
    Address regionStart() {
        return RegionTable.theRegionTable().regionAddress(this);
    }

    void setFull() {
        flags = FULL.mask;
    }
    void setAllocating() {
        flags = ALLOCATING.mask;
    }

    void setFirstFreeChunks(Address firstChunkAddress) {
        flags = HAS_FREE_CHUNK.mask;
        firstFreeChunkIndex = (short) firstChunkAddress.minus(regionStart()).unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
    }

    HeapAccountOwner owner() {
        return owner;
    }

    void setOwner(HeapAccountOwner owner) {
        this.owner = owner;
    }

    HeapRegionInfo next() {
        return RegionTable.theRegionTable().next(this);
    }
    HeapRegionInfo prev() {
        return RegionTable.theRegionTable().prev(this);
    }
}
