/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.memory;

import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * Sorted list of non-intersecting memory regions.
 *
 * @author Bernd Mathiske
 */
public final class SortedMemoryRegionList<MemoryRegion_Type extends MemoryRegion> implements Iterable<MemoryRegion_Type> {

    public static final Comparator<MemoryRegion> COMPARATOR = new Comparator<MemoryRegion>() {
        @Override
        public int compare(MemoryRegion o1, MemoryRegion o2) {
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

    public SortedMemoryRegionList() {
        this(10);
    }

    public SortedMemoryRegionList(int initialCapacity) {
        Class<MemoryRegion_Type[]> type = null;
        memoryRegions = Utils.cast(type, new MemoryRegion[initialCapacity]);
    }

    @INSPECTED
    private MemoryRegion_Type[] memoryRegions;

    @INSPECTED
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
