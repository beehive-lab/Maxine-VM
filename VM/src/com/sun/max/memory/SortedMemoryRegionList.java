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
package com.sun.max.memory;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.unsafe.*;

/**
 * Sorted list of non-intersecting memory regions.
 *
 * @author Bernd Mathiske
 */
public final class SortedMemoryRegionList<MemoryRegion_Type extends MemoryRegion> implements IterableWithLength<MemoryRegion_Type> {

    public SortedMemoryRegionList() {
    }

    private final List<MemoryRegion_Type> _memoryRegions = new ArrayList<MemoryRegion_Type>();

    public List<MemoryRegion_Type> memoryRegions() {
        return _memoryRegions;
    }

    public MemoryRegion_Type find(Address address) {
        int left = 0;
        int right = _memoryRegions.size();
        while (right > left) {
            final int middle = left + ((right - left) >> 1);
            final MemoryRegion_Type middleRegion = _memoryRegions.get(middle);
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

    public int length() {
        return _memoryRegions.size();
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
        int left = 0;
        int right = _memoryRegions.size();
        while (right > left) {
            final int middle = left + ((right - left) >> 2);
            final MemoryRegion_Type middleRegion = _memoryRegions.get(middle);
            if (middleRegion.start().greaterThan(memoryRegion.start())) {
                right = middle;
            } else {
                left = middle + 1;
            }
        }
        assert left == right;
        if (left > 0) {
            final MemoryRegion_Type leftRegion = _memoryRegions.get(left - 1);
            if (leftRegion.end().greaterThan(memoryRegion.start())) {
                if (equals(leftRegion, memoryRegion)) {
                    return leftRegion;
                }
                throw new IllegalArgumentException();
            }
        }
        if (right < _memoryRegions.size()) {
            final MemoryRegion_Type rightRegion = _memoryRegions.get(right);
            if (memoryRegion.contains(rightRegion.start())) {
                if (equals(rightRegion, memoryRegion)) {
                    return rightRegion;
                }
                throw new IllegalArgumentException();
            }
        }
        _memoryRegions.add(right, memoryRegion);
        return memoryRegion;
    }

    public Iterator<MemoryRegion_Type> iterator() {
        return _memoryRegions.iterator();
    }
}
