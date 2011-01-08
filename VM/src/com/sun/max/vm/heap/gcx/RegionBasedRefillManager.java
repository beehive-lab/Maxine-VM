/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;

/**
 * Region-based refill manager for linear space allocator.
 * The manager refills the allocator using regions that may be partially
 * free. Free space in a region is organized as a list of {@link HeapFreeChunk},
 * the head of which is stored in the {@link HeapRegionInfo#firstFreeChunkIndex}
 * obtained from the {@link RegionTable}.
 *
 * @author Laurent Daynes
 */
public abstract class RegionBasedRefillManager extends LinearSpaceAllocator.RefillManager {

    /**
     * Region currently used by the small object allocator.
     */
    private int currentRegion;
    private Address nextFreeChunkInRegion;
    private Size refillThreshold;

    protected abstract HeapRegionInfo getNextAllocatingRegion();

    RegionBasedRefillManager() {
        currentRegion = INVALID_REGION_ID;
        nextFreeChunkInRegion = Address.zero();
    }

    /**
     * Request cannot be satisfied with allocator and refill manager doesn't want to refill.
     * Allocate to large object space or to overflow allocator.
     */
    @Override
    Address allocate(Size size) {
        // TODO Auto-generated method stub
        return Address.zero();
    }

    @Override
    Address allocateTLAB(Size size) {
        // TODO Auto-generated method stub
        return Address.zero();
    }

    @Override
    boolean shouldRefill(Size spaceLeft) {
        return spaceLeft.lessThan(refillThreshold);
    }

    @Override
    Address refill(Pointer startOfSpaceLeft, Size spaceLeft) {
        FatalError.check(spaceLeft.lessThan(refillThreshold), "Should not refill before threshold is reached");
        // First, make the space left parsable, then change of allocating regions.
        if (spaceLeft.greaterThan(0)) {
            HeapSchemeAdaptor.fillWithDeadObject(startOfSpaceLeft, startOfSpaceLeft.plus(spaceLeft));
        }
        Address result = nextFreeChunkInRegion;
        if (!result.isZero()) {
            // Get the next free chunk within the region.
            nextFreeChunkInRegion = HeapFreeChunk.getFreeChunkNext(nextFreeChunkInRegion);
            return result;
        }
        // Needs another region.
        HeapRegionInfo regionInfo = getNextAllocatingRegion();
        // todo

        return Address.zero();
    }
}
