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
package com.sun.max.tele.memory;

import java.util.*;

import com.sun.max.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;

/**
 * Sorted list of non-intersecting memory regions.
 * <br>
 * Cloned from {@link com.sun.max.memory.SortedMemoryRegionList} to extract it from the VM's type space for memory.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class OrderedMemoryRegionList<MemoryRegion_Type extends MaxMemoryRegion> implements Iterable<MemoryRegion_Type> {

    public static final Comparator<MaxMemoryRegion> COMPARATOR = new Comparator<MaxMemoryRegion>() {
        @Override
        public int compare(MaxMemoryRegion o1, MaxMemoryRegion o2) {
            Address o1Start = o1.start();
            Address o2Start = o2.start();
            if (o1Start.lessThan(o2Start)) {
                assert o1.end().lessEqual(o2Start) : "intersecting regions";
                return -1;
            }
            if (o1Start.equals(o2Start)) {
                assert o1.end().equals(o2.end()) : "intersecting regions";
                return 0;
            }
            assert o2.end().lessEqual(o1Start) : "intersecting regions";
            return 1;
        }
    };

    public OrderedMemoryRegionList() {
        this(10);
    }

    public OrderedMemoryRegionList(int initialCapacity) {
        Class<MemoryRegion_Type[]> type = null;
        memoryRegions = Utils.cast(type, new MaxMemoryRegion[initialCapacity]);
    }

    private MemoryRegion_Type[] memoryRegions;

    private int size;

    public MemoryRegion_Type get(int index) {
        if (index >= memoryRegions.length) {
            return null;
        }
        return memoryRegions[index];
    }

    public MemoryRegion_Type find(Address address) {
        int left = 0;
        int right = size;
        while (right > left) {
            final int middle = left + ((right - left) >> 1);
            final MemoryRegion_Type middleRegion = memoryRegions[middle];
            if (middleRegion.start().greaterThan(address)) {
                right = middle;
            } else if (middleRegion.start().plus(middleRegion.size()).greaterThan(address)) {
                return middleRegion;
            } else {
                left = middle + 1;
            }
        }
        return null;
    }

    public int size() {
        return size;
    }

    /**
     * Since we cannot have the implementation of 'equals()' declared in the *interface* 'MemoryRegion', we nail it down here.
     */
    private boolean equals(MemoryRegion_Type m1, MemoryRegion_Type m2) {
        return m1.start().equals(m2.start()) && m1.size().equals(m2.size());
    }

    /**
     * Adds a memory region to this sorted list of region.
     *
     * @param memoryRegion a memory region that is disjoint from all other memory regions currently in the list or the same as one of them
     * @return either the new memory region if added or otherwise the pre-existing same one
     * @throws IllegalArgumentException if {@code memoryRegion} overlaps another memory region currently in this list
     */
    public MemoryRegion_Type add(MemoryRegion_Type memoryRegion) {
        int index = Arrays.binarySearch(memoryRegions, 0, size, memoryRegion, COMPARATOR);
        if (index < 0) {
            int insertionPoint = -(index + 1);
            if (size == memoryRegions.length) {
                int newCapacity = (memoryRegions.length * 3) / 2 + 1;
                memoryRegions = Arrays.copyOf(memoryRegions, newCapacity);
            }
            System.arraycopy(memoryRegions, insertionPoint, memoryRegions, insertionPoint + 1, size - insertionPoint);
            memoryRegions[insertionPoint] = memoryRegion;
            size++;
        }
        return memoryRegion;
    }

    public Iterator<MemoryRegion_Type> iterator() {
        return new Iterator<MemoryRegion_Type>() {
            int index;
            @Override
            public boolean hasNext() {
                return index < size;
            }
            @Override
            public MemoryRegion_Type next() {
                try {
                    return memoryRegions[index++];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new NoSuchElementException();
                }
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
