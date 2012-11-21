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

import com.sun.max.annotate.*;

/**
 * An iterator over a {@link HeapRegionList}.
 * The iterator return ranges of contiguous regions in the order of the list.
 * By default, ranges only comprise {@link HeapRegionInfo.Flag#IS_ITERABLE} regions.
 * The default can be changed to return ranges of regions whose state matches any {@link HeapRegionInfo.Flag} from a set
 * specified using the {@link #clearMatchingFlags()} and {@link #addMatchingFlags(com.sun.max.vm.heap.gcx.HeapRegionInfo.Flag)}.
 */
public final class HeapRegionRangeIterable extends HeapRegionListIterable {
    int matchingFlags;

    public HeapRegionRangeIterable() {
        resetMatchingFlags();
    }

    public void resetMatchingFlags() {
        matchingFlags = HeapRegionInfo.Flag.IS_ITERABLE.or(0);
    }

    public void clearMatchingFlags() {
        matchingFlags = 0;
    }

    public void addMatchingFlags(HeapRegionInfo.Flag flag) {
        this.matchingFlags = flag.or(matchingFlags);
    }

    public RegionRange next() {
        final int firstRegion = cursor;
        int endRange = cursor + 1;
        int next = regionList.next(cursor);
        while (next == endRange) {
            endRange++;
            next = regionList.next(next);
        }
        final int numRegions = endRange - firstRegion;
        cursor = next;
        return RegionRange.from(firstRegion, numRegions);
    }

    /**
     * Reset the iterator to the first iterable region if any.
     * @see HeapRegionInfo#isIterable()
     */
    public void resetToFirstIterable() {
        reset();
        nextIterable();
    }

    @INLINE
    private boolean isIterable(HeapRegionInfo rinfo) {
        return (rinfo.flags & matchingFlags) != 0;
    }

    @INLINE
    private boolean isIterable(HeapRegionInfo rinfo, int tag) {
        return rinfo.tag == tag && (rinfo.flags & matchingFlags) != 0;
    }

    private void nextIterable() {
        final RegionTable theRegionTable = RegionTable.theRegionTable();
        while (cursor != INVALID_REGION_ID) {
            if (isIterable(theRegionTable.regionInfo(cursor))) {
                return;
            }
            cursor = regionList.next(cursor);
        }
    }

    public RegionRange nextIterableRange() {
        final int firstRegion = cursor;
        int endRange = cursor + 1;
        int next = regionList.next(cursor);
        final RegionTable theRegionTable = RegionTable.theRegionTable();
        while (next == endRange) {
            if (!isIterable(theRegionTable.regionInfo(next))) {
                break;
            }
            endRange++;
            next = regionList.next(next);
        }
        final int numRegions = endRange - firstRegion;
        cursor = next;
        nextIterable();
        return RegionRange.from(firstRegion, numRegions);
    }


    /**
     * Reset the iterator to the first iterable region if any.
     * @see HeapRegionInfo#isIterable()
     */
    public void resetToFirstIterable(int tag) {
        reset();
        nextIterable(tag);
    }

    private void nextIterable(int tag) {
        final RegionTable theRegionTable = RegionTable.theRegionTable();
        while (cursor != INVALID_REGION_ID) {
            if (isIterable(theRegionTable.regionInfo(cursor), tag)) {
                return;
            }
            cursor = regionList.next(cursor);
        }
    }

    public RegionRange nextIterableRange(int tag) {
        final int firstRegion = cursor;
        int endRange = cursor + 1;
        int next = regionList.next(cursor);
        final RegionTable theRegionTable = RegionTable.theRegionTable();
        while (next == endRange) {
            if (!isIterable(theRegionTable.regionInfo(next), tag)) {
                break;
            }
            endRange++;
            next = regionList.next(next);
        }
        final int numRegions = endRange - firstRegion;
        cursor = next;
        nextIterable(tag);
        return RegionRange.from(firstRegion, numRegions);
    }
}
