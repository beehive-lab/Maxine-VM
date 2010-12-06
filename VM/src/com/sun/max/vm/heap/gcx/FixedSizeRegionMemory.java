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

import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

/**
 * Memory virtually sliced in fixed size regions that are the unit of reservation / allocations.
 * The region size must be a power of 2 integral number of virtual memory pages.
 * Methods for allocating individual unit or contiguous ranges are provided.
 * Allocating a region means the range of virtual memory addresses corresponding to the region are reserved.
 * In order to use them, backing storage needs to be committed.
 *
 * @author Laurent Daynes
 */
public class FixedSizeRegionMemory  extends MemoryRegion {
    // This is a simple implementation of a low-level memory manager.
    // Allocated and committed space is kept track in two bit sets.
    // BitSet-based allocation has the advantage to be simple and to have a relatively small and fixed footprint,
    // which is perfect for the heap region manager.

    static class RegionBitSet {
        static final int LOG2_BITS_PER_WORD =  WordWidth.BITS_64.log2numberOfBits;
        static final int BITMASK = WordWidth.BITS_64.numberOfBits - 1;
        private long [] bits;

        private static int wordIndex(int i) {
            return i >> LOG2_BITS_PER_WORD;
        }

        private static long bitmask(int bitIndex) {
            return 1L << bitIndex;
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
        }
    }

    /**
     * Size of the regions.
     */
    private Size regionSize;

    private int log2RegionSize;

    /**
     * Number of regions that are committed.
     */
    private Size committedSize;
    /**
     * Number of regions that aren't allocated.
     */
    private Size freeSpaceSize;

    /**
     * Set of Allocated regions.
     */
    private BitSet allocated;

    /**
     * Set of committed regions.
     */
    private BitSet committed;

    /**
     * Lowest region allocated.
     */
    private int lowestAllocated;
    /**
     * Highest region allocated.
     */
    private int highestAllocated;

    public FixedSizeRegionMemory(String name) {
        super(name);
        regionSize = Size.zero();
        committedSize = Size.zero();
    }
    /**
     * Set the bounds of backing store and the size of the regions.
     * Initialization of a FixedSizeRegionMemory object is allowed only if it isn't already initialized, or if
     * it doesn't have any allocated nor committed memory left.
     * In addition to bound and size, amount of pre-committed and pre-allocated space can be specified
     * (this allow to get around bootstrapping issues, e.g., when memory accounted by the backing store
     * is used to allocate the part or all of the objects implementating it).
     *
     * @param start
     * @param size
     * @param the size of the regions. Must be a multiple of the platform's page size.
     * @param amount of space already allocated from the start of the backing storage space.
     */
    public void initialize(Address start, Size size, Size regionSize, Size preAllocated, Size preCommitted) {
        this.start = start;
        this.size = size;
        this.regionSize = regionSize;
        final int rsize = regionSize.toInt();
        log2RegionSize = Integer.numberOfTrailingZeros(rsize);
        FatalError.check((rsize == 1 << log2RegionSize) && (rsize >= Platform.getPageSize()),
                        "Region size must be a power of 2 and an integral number of platform pages");
        freeSpaceSize = size.unsignedShiftedRight(log2RegionSize);
        final int numRegions = freeSpaceSize.toInt();

        allocated = new BitSet(numRegions);
        committed = new BitSet(numRegions);

        lowestAllocated = -1;
        highestAllocated = -1;

        if (!preAllocated.isZero()) {
            allocated.set(0, preAllocated.toInt());
            lowestAllocated = 0;
            highestAllocated = preAllocated.toInt() - 1;
        }
        if (!preCommitted.isZero()) {
            committed.set(0, preCommitted.toInt());
        }
    }

    /**
     * Allocate a single region.
     * @return
     */
    synchronized Address allocate() {
        int i = -1;
        if (lowestAllocated > 0) {
            i = --lowestAllocated;
        } else {
            // Might want to do better than that to limit fragmentation.
            i = allocated.nextClearBit(lowestAllocated + 1);
            if (i > highestAllocated) {
                highestAllocated = i;
            }
        }
        allocated.set(i);
        return start.plus(i << log2RegionSize);
    }

    /**
     * Request a number of contiguous regions of space.
     * @param numRegions the number of region requested
     * @return
     */
    synchronized Address allocate(Size numRegions) {
        if (freeSpaceSize.lessThan(numRegions)) {
            return Address.zero();
        }
        int rsize = numRegions.toInt();
        int begin = -1;
        int end = -1;

        if (lowestAllocated >= rsize) {
            begin = lowestAllocated - rsize;
            end = lowestAllocated;
            lowestAllocated = begin;
        } else if (lowestAllocated < 0) {
            begin = 0;
            end = rsize;
            lowestAllocated = begin;
            highestAllocated = end - 1;
        } else {
            // Not enough space before the first allocated.
            // Search for the first contiguous range of numRegions free regions.
            int i = allocated.nextClearBit(lowestAllocated + 1);
            while (i < highestAllocated) {
                int last = i + rsize - 1;
                if (!allocated.get(last)) {
                    allocated.set(last);
                    int nextSet = allocated.nextSetBit(i + 1);
                    if (nextSet == last) {
                        // Found the contiguous range
                        begin = i;
                        end = last + 1;
                        break;
                    }
                    allocated.clear(last);
                    i = allocated.nextClearBit(nextSet + 1);
                } else {
                    i = allocated.nextClearBit(i + rsize);
                }
            }
            if (begin < 0) {
                // None was found.
                assert i >= highestAllocated;
                begin = highestAllocated + 1;
                end = begin + rsize;
                highestAllocated += rsize;
            } else if (end > highestAllocated) {
                highestAllocated = end;
            }
        }
        // Set bits in allocated set.
        allocated.set(begin, end);
        freeSpaceSize = freeSpaceSize.minus(numRegions);
        return start.plus(begin << log2RegionSize);
    }

    private static boolean isSet(BitSet bs, int firstRegionId,  int numRegions) {
        final int end = firstRegionId + numRegions;
        int r = firstRegionId;
        while (r < end) {
            if (!bs.get(r++)) {
                return false;
            }
        }
        return true;
    }

    synchronized boolean free(Address firstRegion, Size numRegions) {
        assert contains(firstRegion) && contains(firstRegion.plus(numRegions.shiftedLeft(log2RegionSize)).minus(1));
        final int firstRegionId = firstRegion.unsignedShiftedRight(log2RegionSize).toInt();
        assert isSet(allocated, firstRegionId, numRegions.toInt());
        allocated.clear(firstRegionId, firstRegionId + numRegions.toInt());
        // FIXME: should we uncommit them too ?
        return true;
    }

    synchronized boolean commit(Address firstRegion, Size numRegions) {
        assert contains(firstRegion) && contains(firstRegion.plus(numRegions.shiftedLeft(log2RegionSize)).minus(1));
        if (VirtualMemory.commitMemory(firstRegion, numRegions, VirtualMemory.Type.HEAP)) {
            final int firstRegionId = firstRegion.unsignedShiftedRight(log2RegionSize).toInt();
            committed.set(firstRegionId, firstRegionId + numRegions.toInt());
            return true;
        }
        return false;
    }

    synchronized boolean uncommit(Address firstRegion, Size numRegions) {
        assert contains(firstRegion) && contains(firstRegion.plus(numRegions.shiftedLeft(log2RegionSize)).minus(1));
        if (VirtualMemory.uncommitMemory(firstRegion, numRegions, VirtualMemory.Type.HEAP)) {
            final int firstRegionId = firstRegion.unsignedShiftedRight(log2RegionSize).toInt();
            committed.clear(firstRegionId, firstRegionId + numRegions.toInt());
            return true;
        }
        return false;
    }
}
