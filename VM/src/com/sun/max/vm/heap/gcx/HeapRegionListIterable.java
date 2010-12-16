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
import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;
import java.util.*;


public class HeapRegionListIterable implements Iterable<RegionRange>, Iterator<RegionRange> {
    HeapRegionList regionList;
    int cursor = INVALID_REGION_ID;

    HeapRegionListIterable() {
    }

    void initialize(HeapRegionList regionList) {
        this.regionList = regionList;
    }

    void reset(HeapRegionList regionList) {
        cursor = regionList.head();
    }

    @Override
    public Iterator<RegionRange> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return cursor != INVALID_REGION_ID;
    }

    @Override
    public RegionRange next() {
        final int firstRegion = cursor;

        while (regionList.next(cursor) == cursor + 1) {
            cursor++;
        }
        final int numRegions = cursor - firstRegion + 1;
        return RegionRange.from(firstRegion, numRegions);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
