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

import com.sun.max.unsafe.*;

/**
 * Interface for getting / retiring allocating region from a region-based space.
 * Allow to compose heap space with allocators.
 */
public interface RegionProvider {
    /**
     * Retire a region previously obtained via getAllocatingRegion.
     *
     * @param regionID
     */
    void retireAllocatingRegion(int regionID);
    /**
     * Obtain a region with free space from the region provider.
     * TODO: may need to refine this with argument specify constraint on the requested region, e.g., empty, with minimum number of fragment or free space,
     * suitable for TLAB allocation, etc..
     * @return an region identifier, or {@link HeapRegionConstants#INVALID_REGION_ID} if free space is exhausted.
     */
    int getAllocatingRegion();

    /**
     * Obtain a region with at least the specified amount of free space, and at most the specified number of chunks.
     * @param minFreeBytes
     * @param maxFreeChunks
     * @return an region identifier, or {@link HeapRegionConstants#INVALID_REGION_ID} if the request cannot be satisfied.
     */
    int getAllocatingRegion(Size minFreeBytes, int maxFreeChunks);

    /**
     * Minimum size of free chunks in regions being retired. Free space smaller than dead must be turned into dead objects as they will not be reused for allocation by
     * the region provider. Free chunks available for allocation should be appended to the retiring region's list of free chunks.
     * @return a size in bytes no larger than a region size.
     */
    Size minRetiredFreeChunkSize();
}
