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
     * Number of free chunks.
     */
    short numFreeChunks;
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

    public final int freeWords() {
        return freeSpace;
    }
    public final int numFreeChunks() {
        return numFreeChunks;
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

    int toRegionID() {
        return RegionTable.theRegionTable().regionID(this);
    }

    static HeapRegionInfo fromRegionID(int regionID) {
        return RegionTable.theRegionTable().regionInfo(regionID);
    }
}
