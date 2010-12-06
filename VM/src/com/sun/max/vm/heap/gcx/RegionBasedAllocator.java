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
import com.sun.max.vm.runtime.*;

/**
 * A region based allocator.
 * Use a linear space allocator for small objects, and a large object space.
 *
 *
 * @author Laurent Daynes
 */
public class RegionBasedAllocator {

    private final HeapRegionList fullRegions;
    private final HeapRegionList allocatingRegions;
    private final LinearSpaceAllocator smallObjectAllocator;

    /**
     * Refill manager for the small object space allocator.
     *
     */
    class RefillManager extends LinearSpaceAllocator.RefillManager {
        /**
         * Region used by the small object allocator.
         */
        private int currentRegion;
        private Size refillThreshold;

        RefillManager() {
            currentRegion = INVALID_REGION_ID;
        }

        @Override
        Address allocate(Size size) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        Address allocateTLAB(Size size) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        boolean shouldRefill(Size spaceLeft) {
            return spaceLeft.lessThan(refillThreshold);
        }

        @Override
        Address refill(Pointer startOfSpaceLeft, Size spaceLeft) {
            FatalError.check(spaceLeft.lessThan(refillThreshold), "Should not refill before threshold is reached");

            return null;
        }
    }

    RegionBasedAllocator(HeapRegionList allocatingRegion, HeapRegionList fullRegions) {
        this.allocatingRegions = allocatingRegion;
        this.fullRegions = fullRegions;
        smallObjectAllocator = new LinearSpaceAllocator(new RefillManager());
    }

    Pointer allocate(Size size) {
        return smallObjectAllocator.allocateCleared(size);
    }
    Pointer allocateTLAB(Size size) {
        return smallObjectAllocator.allocateTLAB(size);
    }
}
