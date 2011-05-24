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

import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;
import static com.sun.max.vm.heap.gcx.HeapRegionInfo.Flag.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.type.*;
/**
 * Descriptor of heap region.
 * The information recorded is carefully crafted so that a zero-filled HeapRegionInfo
 * instance represents the state of a free region, not allocated to any heap account.
 * This avoids costly initialization of the {@link RegionTable} entries at startup.
 */
public class HeapRegionInfo {

    static enum Flag {
        /**
         * Indicates that the region can be iterated over.
         * GCs and other operations walking over the heap must never come
         * across a non-iterable region. Empty and allocating region may not be iterable.
         */
        IS_ITERABLE,
        /**
         * Indicates that the region is used by an allocator. An allocation region must be made iterable before iterating over it.
         */
        IS_ALLOCATING,
        /**
         * Region has a list of {@link HeapFreeChunk} tracking space available for allocation within the region.
         * The head of the list is located at the {@link HeapRegionInfo#firstFreeChunkIndex} word in the region.
         * Note that empty regions doesn't have free chunks according to this definition.
         */
        HAS_FREE_CHUNK;

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
        public final int or(Flag flag) {
            return flag.mask | mask;
        }

        public final int and(Flag flag) {
            return flag.mask & mask;
        }

        public static int allFlagsMask() {
            return IS_ITERABLE.or(IS_ALLOCATING.or(HAS_FREE_CHUNK));
        }
    }

    static final int EMPTY_REGION = 0;
    static final int ALLOCATING_REGION = IS_ALLOCATING.or(0);
    static final int FULL_REGION = IS_ITERABLE.or(0);
    static final int FREE_CHUNKS_REGION = IS_ITERABLE.or(HAS_FREE_CHUNK.or(0));

    /**
     * A 32-bit vector compounding several flags information. See {@link Flag} for usage of each of the bits.
     */
    int flags; // NOTE: don't want to use an EnumSet here. Don't want a long for storing flags; and want the flags embedded in the heap region info.

    public final boolean isEmpty() {
        return flags == EMPTY_REGION;
    }
    public final boolean isFull() {
        return flags == FULL_REGION;
    }

    public final boolean isAllocating() {
        return IS_ALLOCATING.isSet(ALLOCATING_REGION);
    }

    public final boolean isIterable() {
        return IS_ITERABLE.isSet(flags);
    }

    public final boolean hasFreeChunks() {
        return HAS_FREE_CHUNK.isSet(flags);
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
     * Zero if the region is empty.
     */
    short firstFreeChunkIndex;
    /**
     * Number of free chunks. Zero if the region is empty.
     */
    short numFreeChunks;
    /**
     * Space available for allocation, in words. This excludes dark matter than cannot be used
     * for allocation. Zero if the region is empty.
     */
    short freeSpace;
    /**
     * Amount of live data. Zero if the region is empty.
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

    public final int freeBytes() {
        return freeSpace << Word.widthValue().log2numberOfBytes;
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
    final Address firstFreeBytes() {
        return isFull() ? Address.zero() : regionStart().plus(firstFreeChunkOffset());
    }

    /**
     * Start of the region described by this {@link HeapRegionInfo} instance.
     * @return address to the start of a region
     */
    final Address regionStart() {
        return RegionTable.theRegionTable().regionAddress(this);
    }

    final void setFull() {
        flags = FULL_REGION;
    }

    final void setAllocating() {
        flags = IS_ALLOCATING.or(IS_ITERABLE.clear(flags));
    }

    final void setIterable() {
        flags = IS_ITERABLE.and(IS_ALLOCATING.clear(flags));
    }

    final void setEmpty() {
        flags = EMPTY_REGION;
    }

    final void setFreeChunks(Address firstChunkAddress, short numFreeWords, short numChunks) {
        flags = FREE_CHUNKS_REGION;
        firstFreeChunkIndex = (short) firstChunkAddress.minus(regionStart()).unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
        numFreeChunks = numChunks;
        freeSpace = numFreeWords;
    }

    final void setFreeChunks(Address firstChunkAddress, Size numBytes, int numChunks) {
        final short numFreeWords = (short) firstChunkAddress.unsignedShiftedRight(Kind.SHORT.width.log2numberOfBytes).toInt();
        setFreeChunks(firstChunkAddress, numFreeWords, (short) numChunks);
    }

    final void clearFreeChunks() {
        numFreeChunks = 0;
        freeSpace = 0;
    }

    final void resetOccupancy() {
        flags = EMPTY_REGION;
        liveData = 0;
        numFreeChunks  = 0;
        freeSpace = 0;
    }

    final HeapAccountOwner owner() {
        return owner;
    }

    final void setOwner(HeapAccountOwner owner) {
        this.owner = owner;
    }

    final HeapRegionInfo next() {
        return RegionTable.theRegionTable().next(this);
    }
    final HeapRegionInfo prev() {
        return RegionTable.theRegionTable().prev(this);
    }

    final int toRegionID() {
        return RegionTable.theRegionTable().regionID(this);
    }

    @INLINE
    static HeapRegionInfo fromRegionID(int regionID) {
        return RegionTable.theRegionTable().regionInfo(regionID);
    }

    /**
     * Return heap region associated information from an address guaranteed to point in a heap region.
     * @param address
     * @return
     */
    @INLINE
    static HeapRegionInfo fromInRegionAddress(Address address) {
        return RegionTable.theRegionTable().inHeapAddressRegionInfo(address);
    }

    /**
     * Return heap region associated information from an address not guaranteed to point in a heap region.
     * Return {@linkplain RegionTable#nullHeapRegionInfo} if not.
     * @param address
     * @return
     */
    @INLINE
    static HeapRegionInfo fromAddress(Address address) {
        return RegionTable.theRegionTable().regionInfo(address);
    }

    @INLINE
    static void walk(RegionRange regionRange, CellVisitor cellVisitor) {
        RegionTable.theRegionTable().walk(regionRange, cellVisitor);
    }
}
