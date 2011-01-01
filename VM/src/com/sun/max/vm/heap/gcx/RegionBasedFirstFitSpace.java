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

import static com.sun.max.vm.heap.gcx.HeapRegionList.RegionListUse.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * A region-based space supporting the sweeping interface.
 * The space is made of an number of possibly non-contiguous
 * fixed-size regions. Each region maintains a list of free chunks
 * of minimum size, and occupancy statistics.
 * Both the list and the occupancy statistics are filled during sweeping.
 * Free chunks are organized in address order; regions with free space are organized in
 * address order as well.
 * Allocation is performed on a first-fit basis.
 *
 * @author Laurent Daynes
 */
public class RegionBasedFirstFitSpace extends Sweepable implements ResizableSpace {
    /**
     * Region available for allocation.
     */
    private final HeapRegionList allocatingRegions;


    /**
     * Minimum size to be considered reclaimable.
     */
    private Size minReclaimableSpace;

    public RegionBasedFirstFitSpace() {
        allocatingRegions = OWNERSHIP.createList();
    }

    @HOSTED_ONLY
    public void hostInitialize() {
        //smallObjectAllocator.hostInitialize();
    }

    @Override
    public Size beginSweep(boolean precise) {
        return minReclaimableSpace;
    }

    @Override
    public Size endSweep() {
        return Size.zero();
    }

    @Override
    public void processDeadSpace(Address freeChunk, Size size) {
    }

    @Override
    public Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject) {
        // TODO
        return Pointer.zero();
    }

    @Override
    public Pointer processLiveObject(Pointer liveObject) {
        // TODO
        return Pointer.zero();
    }

    @Override
    public Size growAfterGC(Size delta) {
        // TODO
        return Size.zero();
    }

    @Override
    public Size shrinkAfterGC(Size delta) {
        // TODO
        return Size.zero();
    }

    @Override
    public void verify(AfterMarkSweepVerifier verifier) {
        // TODO Auto-generated method stub

    }

}
