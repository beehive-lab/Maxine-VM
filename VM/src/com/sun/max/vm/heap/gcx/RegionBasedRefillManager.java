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
