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

import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;
import static com.sun.max.vm.heap.gcx.HeapRegionInfo.Flag.*;
import static com.sun.max.vm.heap.gcx.HeapRegionState.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.*;
/**
 * Descriptor of a heap region.
 * The information recorded is carefully crafted so that a zero-filled HeapRegionInfo
 * instance represents the state of a free region, not allocated to any heap account.
 * This avoids initialization of the {@link RegionTable} entries at startup.
 */
public class HeapRegionInfo {

    static enum Flag {
        /**
         * Indicates that the region is part of an iterable range.
         * GCs and other operations walking over the heap must never come
         * across a non-iterable region. Empty regions are not iterable (they could be made iterable but
         * it is pointless to iterate over them). Allocating regions are considered non-iterable.
         */
        IS_ITERABLE,
        /**
         * Indicates that the region is used by an allocator and may not be iterable. An allocation region must be made iterable before iterating over it.
         */
        IS_ALLOCATING,
        /**
         * Region has a list of {@link HeapFreeChunk} tracking space available for allocation within the region.
         * The head of the list is located at the {@link HeapRegionInfo#firstFreeChunkOffset} word in the region.
         * Note that empty regions doesn't have free chunks according to this definition.
         */
        HAS_FREE_CHUNK,
        /**
         * Region is part of  a large multi-regions, object.
         */
        IS_LARGE,
        /**
         * Region is the head of a large multi-regions object.
         */
        IS_HEAD,
        /**
         * Region is the last region of a multi-regions object. Space after the end of the large object may be used for allocation.
         */
        IS_TAIL;

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

        public final boolean only(int flags) {
            return flags == mask;
        }

        static private Flag [] allFlags = values();

        static void log(int flags) {
            String sep = "";
            for (Flag f : allFlags) {
                if (f.isSet(flags)) {
                    Log.print(sep);
                    Log.print(f.toString());
                    sep = " | ";
                }
            }
        }
    }

    static final int LARGE_REGION_FLAGS = IS_LARGE.or(IS_TAIL.or(IS_HEAD.or(0)));

    public static String flagsToString(int flags) {
        if (flags == 0) {
            return "IS_EMPTY";
        }
        StringBuffer b = new StringBuffer("");
        String sep = "";
        for (Flag f : Flag.values()) {
            if (f.isSet(flags)) {
                b.append(sep); b.append(f);
                sep = " | ";
            }
        }
        return b.toString();
    }

    /**
     * A 32-bit vector compounding several flags information. See {@link Flag} for usage of each of the bits.
     */
    @INSPECTED
    int flags; // NOTE: don't want to use an EnumSet here. Don't want a long for storing flags; and want the flags embedded in the heap region info.

    /**
     * A customizable tag used to discriminate regions.
     */
    @INSPECTED
    int tag;

    /**
     * Offset (in bytes) relative to the beginning of a region to the first free chunk of the region.
     * Zero if the region is empty.
     */
    @INSPECTED
    private int firstFreeChunkOffset;
    /**
     * Number of free chunks. Zero if the region is empty.
     */
    @INSPECTED
    private int numFreeChunks;
    /**
     * Space available for allocation, in bytes. This excludes dark matter than cannot be used
     * for allocation. Zero if the region is empty.
     */
    private int freeSpace;
    /**
     * Amount of live data, in bytes. Zero if the region is empty.
     * Can be used  with {@link #freeSpace} to determine dark matter.
     */
    private int liveData;

    /**
     * Owner of the region described by {@link HeapRegionInfo} instance.
     */
    HeapAccountOwner owner;

    public final boolean isEmpty() {
        return flags == EMPTY_REGION.flags;
    }

    public final boolean isFull() {
        return IS_ITERABLE.only(flags & ~LARGE_REGION_FLAGS);
    }

    public final boolean isAllocating() {
        return IS_ALLOCATING.isSet(flags);
    }

    public final boolean isIterable() {
        return IS_ITERABLE.isSet(flags);
    }

    public final boolean hasFreeChunks() {
        return HAS_FREE_CHUNK.isSet(flags);
    }

    public final boolean isLarge() {
        return IS_LARGE.isSet(flags);
    }

    public final boolean isHeadOfLargeObject() {
        return IS_HEAD.isSet(flags);
    }

    public final boolean isTailOfLargeObject() {
        return IS_TAIL.isSet(flags);
    }

    HeapRegionInfo() {
        // Not a class one can allocate. Allocation is the responsibility of the region table.
    }

    /**
     * Total number of free bytes in free chunks. This is only relevant for region with at least one free chunk.
     * Empty regions have a free bytes count of zero.
     */
    public final int freeBytesInChunks() {
        return freeSpace;
    }

    public final int freeBytes() {
        return isEmpty() ?  regionSizeInBytes : freeBytesInChunks();
    }

    public final int liveBytes() {
        return liveData << Word.widthValue().log2numberOfBytes;
    }

    public final int numFreeChunks() {
        return numFreeChunks;
    }

    public void dump(boolean enumerateFreeChunks) {
        Log.print("region #");
        Log.print(toRegionID());
        Log.print(" [");
        Log.print(regionStart());
        Log.print(",");
        Log.print(regionStart().plus(regionSizeInBytes));
        Log.print(" [ ");
        Flag.log(flags);
        Log.print(", free: ");
        Log.print(freeBytes());
        Log.print(" live: ");
        Log.print(liveBytes());
        Log.print(" owner: ");
        Log.print(Reference.fromJava(owner).toOrigin());
        Log.print(" #free chunks: ");
        Log.print(numFreeChunks);
        if (numFreeChunks > 0) {
            if (enumerateFreeChunks) {
                Log.print("free chunks: ");
                HeapFreeChunk.dumpList(HeapFreeChunk.toHeapFreeChunk(firstFreeBytes()));
            } else {
                Log.print("first free chunk");
                Log.print(firstFreeBytes());
            }
        }
        Log.println();
    }

    /**
     * Return the address to the first chunk of space available for allocation.
     * The chunk is formatted as a {@link HeapFreeChunk} only if the region has its {@link Flag#HAS_FREE_CHUNK} set.
     * @return Address to the first chunk of space available for allocation, or zero if the region is full.
     */
    final Address firstFreeBytes() {
        return isFull() ? Address.zero() : regionStart().plus(firstFreeChunkOffset);
    }

    private int offsetInRegion(Address address) {
        return address.and(regionAlignmentMask).toInt();
    }

    /**
     * Start of the region described by this {@link HeapRegionInfo} instance.
     * @return address to the start of a region
     */
    final Address regionStart() {
        return RegionTable.theRegionTable().regionAddress(this);
    }

    private void clear() {
        liveData = 0;
        numFreeChunks  = 0;
        firstFreeChunkOffset = 0;
        freeSpace = 0;
    }

    final void setFreeChunks(Address firstChunkAddress, int numBytes, int numChunks) {
        firstFreeChunkOffset = offsetInRegion(firstChunkAddress);
        numFreeChunks = numChunks;
        freeSpace = numBytes;
    }

    final void setFreeChunks(Address firstChunkAddress, Size numBytes, int numChunks) {
        setFreeChunks(firstChunkAddress, numBytes.toInt(),  numChunks);
    }

    final void clearFreeChunks() {
        firstFreeChunkOffset = 0;
        numFreeChunks = 0;
        freeSpace = 0;
    }

    final void resetOccupancy() {
        clear();
    }

    public final HeapAccountOwner owner() {
        return owner;
    }

    final void setOwner(HeapAccountOwner owner) {
        this.owner = owner;
    }

    final void setTag(int tag) {
        this.tag = tag;
    }

    final int getTag() {
        return tag;
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
     */
    @INLINE
    static HeapRegionInfo fromInRegionAddress(Address address) {
        return RegionTable.theRegionTable().inHeapAddressRegionInfo(address);
    }

    /**
     * Return heap region associated information from an address not guaranteed to point in a heap region.
     * Return {@linkplain RegionTable#nullHeapRegionInfo} if not.
     * @param address
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
