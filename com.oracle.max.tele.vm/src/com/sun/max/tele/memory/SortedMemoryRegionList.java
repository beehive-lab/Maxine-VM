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
package com.sun.max.tele.memory;

import java.util.*;

import com.sun.max.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;

/**
 * List of sorted non-intersecting memory regions.
 * This list sorts itself (if necessary) when {@linkplain #find(Address) searched} or
 * asked for an {@linkplain #iterator() iterator}. It can also handle the encapsulated
 * memory regions changing their address ranges.
 */
public final class SortedMemoryRegionList<T extends MaxMemoryRegion> implements Iterable<T> {

    private T[] list;
    private int size;

    /**
     * Sorting is done lazily.
     */
    private boolean sorted;

    public SortedMemoryRegionList() {
        Class<T[]> type = null;
        list = Utils.cast(type, new MaxMemoryRegion[10]);
    }

    public int size() {
        return size;
    }

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

    /**
     * Searches for an element in this list based on an address.
     */
    public synchronized T find(Address address) {
        ensureSorted();
        T t = binarySearch(address);
        if (t == null) {
            t = linearSearch(address);
            if (t != null) {
                // The elements must have been relocated
                Arrays.sort(list, 0, size, COMPARATOR);
            }
        }
        return t;
    }

    private void ensureSorted() {
        if (!sorted) {
            Arrays.sort(list, 0, size, COMPARATOR);
            sorted = true;
        }
    }

    private T linearSearch(Address address) {
        for (int i = 0; i < size; i++) {
            T t = list[i];
            if (t.contains(address)) {
                return t;
            }
        }
        return null;
    }

    private T binarySearch(Address address) {
        int left = 0;
        int right = size;
        while (right > left) {
            final int middle = left + ((right - left) >> 1);
            final T t = list[middle];
            if (t.start().greaterThan(address)) {
                right = middle;
            } else if (t.start().plus(t.nBytes()).greaterThan(address)) {
                return t;
            } else {
                left = middle + 1;
            }
        }
        return null;
    }

    /**
     * Adds an element to this list.
     */
    public synchronized void add(T e) {
        if (list.length == size) {
            int newCapacity = (list.length * 3) / 2 + 1;
            list = Arrays.copyOf(list, newCapacity);
        }
        list[size] = e;
        if (size > 0) {
            T last = list[size - 1];
            if (COMPARATOR.compare(last, e) > 0) {
                sorted = false;
            }
        }
        size++;
    }

    @Override
    public synchronized Iterator<T> iterator() {
        // Make sure the return list is sorted
        T[] list = Arrays.copyOf(this.list, size);
        Arrays.sort(list, 0, size, COMPARATOR);
        return Arrays.asList(list).iterator();
    }
}
