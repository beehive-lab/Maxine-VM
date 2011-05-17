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
import static com.sun.max.vm.heap.gcx.RegionRange.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

/**
 * Region allocator initialized. The allocator offers method to allocate / free / commit and uncommit
 * fixed size regions from a memory pool backed up by a contiguous range of virtual addresses.
 * The region size must be a power of 2 integral number of virtual memory pages.
 * Methods for allocating individual unit or contiguous ranges are provided.
 * Allocating a region means the range of virtual memory addresses corresponding to the region are reserved.
 * In order to use them, the underlying virtual memory needs to be committed.
 *
 * @author Laurent Daynes
 */
public class FixedSizeRegionAllocator {

    /**
     * A bit set class tailored to the needs of the region allocator.
     */
    static final class RegionBitSet {
        static final int LOG2_BITS_PER_WORD =  WordWidth.BITS_64.log2numberOfBits;
        static final int BIT_INDEX_MASK = WordWidth.BITS_64.numberOfBits - 1;
        static final long ALL_ONES = ~0L;


        @CONSTANT_WHEN_NOT_ZERO
        private long [] bits;

        RegionBitSet() {
        }

        int numBits() {
            return bits.length << LOG2_BITS_PER_WORD;
        }
        void initialize(long [] bits) {
            this.bits = bits;
        }

        private static int wordIndex(int i) {
            return i >> LOG2_BITS_PER_WORD;
        }

        private static long bitmask(int bitIndex) {
            return 1L << bitIndex;
        }

        /**
         * Bitmask with all the bits to the left of the specified index (including it) set to 1.
         * @param bitIndex
         * @return
         */
        private static long bitsLeftOf(int bitIndex) {
            return ALL_ONES << bitIndex;
        }
        /**
         * Bitmask with all the bits to the right of the specified index (excluding it) set to 1.
         * @param bitIndex
         * @return
         */
        private static long bitsRightOf(int bitIndex) {
            // ALL_ONES >>> -X  == ALL_ONES >>> (ALL_ONES - X + 1), i.e.,
            // all the bits to the left of X (including X) are set to 0, the other to 1.
            // e.g.,  ALL_ONES >>> -2  == 0x3 => bit 0 and 1 set, bit 2 and higher clear.
            return ALL_ONES >>> -bitIndex;
        }

        /**
         * Return bitmask corresponding to the interval between startIndex (inclusive) and endIndex (exclusive), wherein
         * both index to the same word.
         * @param startIndex
         * @param endIndex
         * @return
         */
        private static long bitmask(int startIndex, int endIndex) {
            return bitsLeftOf(startIndex) & bitsRightOf(endIndex);
        }

        boolean isSet(int bitIndex) {
            return (bits[wordIndex(bitIndex)] & bitmask(bitIndex)) != 0L;
        }

        boolean isClear(int bitIndex) {
            return (bits[wordIndex(bitIndex)] & bitmask(bitIndex)) == 0L;
        }

        void set(int bitIndex) {
            bits[wordIndex(bitIndex)] |= bitmask(bitIndex);
        }

        void clear(int bitIndex) {
            bits[wordIndex(bitIndex)] &= ~bitmask(bitIndex);
        }

        void set(int startIndex, int endIndex) {
            int startWordIndex = wordIndex(startIndex);
            int endWordIndex   = wordIndex(endIndex - 1);
            if (startWordIndex == endWordIndex) {
                bits[startWordIndex] |= bitmask(startIndex, endIndex);
            } else {
                int i = startWordIndex;
                bits[i++] |= bitsLeftOf(startWordIndex);
                while (i < endWordIndex) {
                    bits[i++] = ALL_ONES;
                }
                bits[i] |= bitsRightOf(endIndex);
            }
        }

        void clear(int startIndex, int endIndex) {
            int startWordIndex = wordIndex(startIndex);
            int endWordIndex   = wordIndex(endIndex - 1);
            if (startWordIndex == endWordIndex) {
                bits[startWordIndex] &= ~bitmask(startIndex, endIndex);
            } else {

                int i = startWordIndex;
                bits[i++] &= ~bitsLeftOf(startWordIndex);
                while (i < endWordIndex) {
                    bits[i++] = 0L;
                }
                bits[i] &= ~bitsRightOf(endIndex);
            }
        }

        /**
         * Returns the number of clear bits starting at the specified bit index, bounded by the second parameter if not 0.
         * specified
         * @param bitIndex
         * @param maxBits
         * @return number of clear bits starting at {code bitIndex}, or {code maxBits} if {code maxBits} is greater than zero and the number
         * is greater or equals to {code maxBits}.
         */
        int numClearBitsAt(int bitIndex, int maxBits) {
            int i = wordIndex(bitIndex);
            long w = bits[i] >> bitIndex;
            if (w != 0L) {
                int numZeros =  Long.numberOfTrailingZeros(w);
                return numZeros > maxBits ? maxBits : numZeros;
            }
            int numZeros = WordWidth.BITS_64.numberOfBits - (bitIndex & BIT_INDEX_MASK);
            if (numZeros >= maxBits) {
                return maxBits;
            }
            i++;
            while (i < bits.length) {
                w = bits[i];
                if (w != 0L) {
                    numZeros += Long.numberOfTrailingZeros(w);
                    return numZeros > maxBits ? maxBits : numZeros;
                }
                numZeros += WordWidth.BITS_64.numberOfBits;
                if (numZeros >= maxBits) {
                    return maxBits;
                }
                i++;
            }
            assert numZeros < maxBits;
            return numZeros;
        }

        int nextClearBit(int startIndex) {
            int i = wordIndex(startIndex);
            // First, search a clear bit in the same word.
            long w = ~bits[i] & bitsLeftOf(startIndex);
            while (w == 0L) {
                if (++i > bits.length) {
                    return INVALID_REGION_ID;
                }
                w = ~bits[i];
            }
            return (i << LOG2_BITS_PER_WORD) + Long.numberOfTrailingZeros(w);
        }

        int previousSetBit(int startIndex) {
            int i = wordIndex(startIndex);
            // Clear bits higher (i.e., left of) start index.
            long w = bits[i] & (bitsRightOf(startIndex + 1));
            while (w == 0L) {
                w = bits[--i];
            }
            return (i << LOG2_BITS_PER_WORD) + (BIT_INDEX_MASK - Long.numberOfLeadingZeros(w));
        }
    }

    /**
     * Backing storage for the allocator.
     */
    private final MemoryRegion backingStorage;

    /**
     * Set of allocated regions.
     */
    private final RegionBitSet allocated;

    /**
     * Set of committed regions.
     */
    private final RegionBitSet committed;

    /**
     * Number of regions that are committed.
     */
    private int committedSize;
    /**
     * Number of regions that aren't allocated.
     */
    private int numFreeRegions;

    /**
     * Number of regions reserved at the beginning of the backing storage that cannot be allocated.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private int residentRegions;

    /**
     * Highest region allocated.
     */
    private int highestAllocated;

    public FixedSizeRegionAllocator(String name) {
        backingStorage = new MemoryRegion(name);
        allocated = new RegionBitSet();
        committed = new RegionBitSet();
        committedSize = 0;
        numFreeRegions = 0;
    }
    /**
     * Set the bounds of backing store and the size of the regions.
     *
     * In addition to bound and size, amount of pre-committed and pre-allocated space can be specified
     * (this allow to get around bootstrapping issues, e.g., when memory accounted by the backing store
     * is used to allocate the part or all of the objects implementing it).
     *
     * @param start address to the first byte of backing storage for the allocator
     * @param size size of the memory region backing the region allocator
     * @param the size of the regions. Must be a multiple of the platform's page size.
     * @param amount of space already allocated from the start of the backing storage space.
     */
    public void initialize(Address start, int numRegions, int numPreCommitted) {
        FatalError.check(backingStorage.start().isZero(), "Can only be initialized once");
        final int numWordsPerBitSet = 1 + (numRegions >> RegionBitSet.LOG2_BITS_PER_WORD);
        numFreeRegions = numRegions;

        backingStorage.setStart(start);
        backingStorage.setSize(Size.fromInt(numRegions << log2RegionSizeInBytes));

        allocated.initialize(new long[numWordsPerBitSet]);
        committed.initialize(new long[numWordsPerBitSet]);

        highestAllocated = INVALID_REGION_ID;
        residentRegions = numPreCommitted;
        if (residentRegions > 0) {
            allocated.set(0, residentRegions);
            highestAllocated = residentRegions - 1;
            committed.set(0, residentRegions);
            committedSize = residentRegions;
            numFreeRegions -= residentRegions;
        }
    }

    public boolean contains(Address address) {
        return backingStorage.contains(address);
    }

    /**
     * The total number of regions managed by this allocator.
     * @return a number of regions
     */
    int capacity() {
        return backingStorage.size().unsignedShiftedRight(log2RegionSizeInBytes).toInt();
    }

    boolean isValidRegionId(int regionId) {
        return regionId >= 0 && regionId < capacity();
    }

    private Address validRegionStart(int regionId) {
        assert isValidRegionId(regionId);
        return backingStorage.start().plus(regionId << log2RegionSizeInBytes);
    }

    Address regionStart(int regionId) {
        return isValidRegionId(regionId) ? validRegionStart(regionId) : Address.zero();
    }

    /**
     * Allocate a single region.
     * @return the region identifier or INVALID_REGION_ID if the request cannot be satisfied
     */
    synchronized int allocate() {
        if (numFreeRegions == 0) {
            return INVALID_REGION_ID;
        }
        int result = allocated.nextClearBit(residentRegions);
        if (result > highestAllocated) {
            highestAllocated = result;
        }
        numFreeRegions--;
        allocated.set(result);
        return result;
    }

    /**
     * Allocate the next available contiguous space of a size smaller or equals to the specified number of regions.
     * @param numRegions
     * @return the identifier of the first region of the allocated range, or INVALID_REGION_ID if the request cannot be satisfied
     */
    synchronized RegionRange allocateLessOrEqual(int numRegions) {
        if (numFreeRegions == 0) {
            return INVALID_RANGE;
        }
        final int begin = allocated.nextClearBit(residentRegions);
        final int numAllocated = allocated.numClearBitsAt(begin, numRegions);
        assert numAllocated != 0;
        final int end = begin + numAllocated;
        final int last = end - 1;
        if (last > highestAllocated) {
            highestAllocated = last;
        }
        allocated.set(begin, end);
        numFreeRegions -= numAllocated;
        return RegionRange.from(begin, numAllocated);
    }

    /**
     * Allocate number of contiguous regions.
     * @param numRegions the number of region requested
     * @return the first region of the allocated range, INVALID_REGION_ID if request cannot be satisfied.
     */
    synchronized int allocate(int numRegions) {
        if (numFreeRegions < numRegions) {
            return INVALID_REGION_ID;
        }
        int begin = -1;
        int end = -1;

        // Not enough space before the first allocated.
        // Search for the first contiguous range of numRegions free regions.
        int i = allocated.nextClearBit(residentRegions);
        while (i < highestAllocated) {
            int numClearBits = allocated.numClearBitsAt(i, numRegions);
            if (numClearBits == numRegions) {
                // Found the contiguous range
                begin = i;
                break;
            }
            i = allocated.nextClearBit(numClearBits + 1);
        }
        if (begin < 0) {
            // None was found.
            assert i >= highestAllocated;
            begin = highestAllocated + 1;
            end = begin + numRegions;
            highestAllocated += numRegions;
        } else if (end > highestAllocated) {
            highestAllocated = end;
        }

        // Set bits in allocated set.
        allocated.set(begin, end);
        numFreeRegions -= numRegions;
        return begin;
    }

    private boolean isValidAllocatedRange(int firstRegionId, int numRegions) {
        final int end = firstRegionId + numRegions;
        return firstRegionId >= residentRegions && (end - 1) <= highestAllocated &&
        allocated.numClearBitsAt(firstRegionId, end) == 0;
    }

    private boolean isValidCommittedRange(int firstRegionId, int numRegions) {
        final int end = firstRegionId + numRegions;
        return firstRegionId >= residentRegions && end <= committed.numBits() && committed.numClearBitsAt(firstRegionId, end) == numRegions;
    }

    synchronized boolean free(int firstRegionId, int numRegions) {
        if (!isValidAllocatedRange(firstRegionId, numRegions)) {
            return false;
        }
        final int end = firstRegionId + numRegions;
        allocated.clear(firstRegionId, end);
        if (highestAllocated == (end - 1)) {
            highestAllocated = allocated.previousSetBit(firstRegionId);
        }
        return true;
    }

    synchronized boolean commit(int firstRegionId, int numRegions) {
        if (!isValidAllocatedRange(firstRegionId, numRegions)) {
            // Cannot commit non-allocated regions.
            return false;
        }
        // TODO (ld) should we try to avoid calling commitMemmory if the range is already committed ?
        // Should we try to commit only uncommitted sub-range ?
        final Size size = Size.fromInt(numRegions).shiftedLeft(log2RegionSizeInBytes);
        if (VirtualMemory.commitMemory(regionStart(firstRegionId), size, VirtualMemory.Type.HEAP)) {
            committed.set(firstRegionId, firstRegionId + numRegions);
            committedSize += numRegions;
            return true;
        }
        return false;
    }

    synchronized boolean uncommit(int firstRegionId, int numRegions) {
        if (isValidCommittedRange(firstRegionId, numRegions)) {
            final Size size = Size.fromInt(numRegions).shiftedLeft(log2RegionSizeInBytes);
            if (VirtualMemory.uncommitMemory(regionStart(firstRegionId), size, VirtualMemory.Type.HEAP)) {
                committed.clear(firstRegionId, firstRegionId + numRegions);
                return true;
            }
        }
        return false;
    }

    MemoryRegion bounds() {
        return backingStorage;
    }
}
