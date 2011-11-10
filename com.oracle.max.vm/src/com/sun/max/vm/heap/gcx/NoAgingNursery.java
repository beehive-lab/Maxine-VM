/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;

/**
 * A simple nursery implementation that allocates objects in a single contiguous space and evacuate all survivors to the next generation on minor collections.
 * The next generation is responsible for keeping a reserve large enough to accommodate the worst-case evacuation.
 *
 *
 * with a fixed ratio between young and old generation size.
 */
public class NoAgingNursery implements HeapSpace {


    class NurseryRefiller extends Refiller {
        @Override
        public Address allocateRefill(Pointer startOfSpaceLeft, Size spaceLeft) {
            HeapSchemeAdaptor.fillWithDeadObject(startOfSpaceLeft, startOfSpaceLeft.plus(spaceLeft));
            Size size = totalSpace();
            while (!Heap.collectGarbage(size)) {
                size = totalSpace();
                // TODO: condition for OOM
            }
            HeapFreeChunk.format(allocator.start, size);
            return allocator.start;
        }

        @Override
        protected void doBeforeGC() {
            // TODO Auto-generated method stub
        }

    }

    /**
     * The heap account space for this nursery is allocated from.
     */
    private final HeapAccount<? extends HeapAccountOwner> heapAccount;

    /**
     * List of region allocated to the nursery.
     */
    private HeapRegionList nurseryRegionsList;
    /**
     * List of regions allocated to the nursery but uncommitted.
     */
    private HeapRegionList uncommitedNurseryRegionsList;

    /**
     * Atomic bump pointer allocator over the nursery space. The current bounds and size of the nursery are obtained from the allocator's start and end addresses.
     */
    private final BaseAtomicBumpPointerAllocator<NurseryRefiller> allocator = new BaseAtomicBumpPointerAllocator<NurseryRefiller>(new NurseryRefiller());

    public NoAgingNursery(HeapAccount<? extends HeapAccountOwner> heapAccount) {
        this.heapAccount = heapAccount;
    }

    public void initialize(GenHeapSizingPolicy genSizingPolicy) {
        nurseryRegionsList = HeapRegionList.RegionListUse.OWNERSHIP.createList();
        uncommitedNurseryRegionsList = HeapRegionList.RegionListUse.OWNERSHIP.createList();
        if (!heapAccount.allocateContiguous(HeapRegionConstants.numberOfRegions(genSizingPolicy.maxYoungGenSize()), nurseryRegionsList, false, false)) {
            FatalError.unexpected("Couldn't allocate contiguous range to the nursery");
        }
        int regionID = nurseryRegionsList.head();
        int numCommittedRegions = HeapRegionConstants.numberOfRegions(genSizingPolicy.initialYoungGenSize());
        heapAccount.commit(RegionRange.from(regionID, numCommittedRegions));

        int lastCommittedRegion = regionID + numCommittedRegions - 1;
        while (nurseryRegionsList.tail() != lastCommittedRegion) {
            uncommitedNurseryRegionsList.prepend(nurseryRegionsList.removeTail());
        }
        allocator.initialize(RegionTable.theRegionTable().regionAddress(nurseryRegionsList.head()), genSizingPolicy.initialYoungGenSize());
    }

    public Pointer allocate(Size size) {
        return allocator.allocate(size);
    }

    @Override
    public Size growAfterGC(Size delta) {
        // TODO
        FatalError.unimplemented();
        return null;
    }

    @Override
    public Size shrinkAfterGC(Size delta) {
        // TODO
        FatalError.unimplemented();
        return Size.zero();
    }

    @Override
    public Size totalSpace() {
        return allocator.size();
    }

    @Override
    public Size capacity() {
        return Size.fromInt(HeapRegionConstants.regionSizeInBytes).times(uncommitedNurseryRegionsList.size()).plus(allocator.size());
    }

    @Override
    public Pointer allocateTLAB(Size size) {
        // TODO
        FatalError.unimplemented();
        return Pointer.zero();
    }

    @Override
    public boolean contains(Address address) {
        return address.greaterEqual(allocator.start) && address.equals(allocator.end);
    }

    @Override
    public void doBeforeGC() {
        // TODO Auto-generated method stub

    }

    @Override
    public void doAfterGC() {
        // TODO Auto-generated method stub

    }

    @Override
    public Size freeSpace() {
        return allocator.freeSpace();
    }

    @Override
    public Size usedSpace() {
        return allocator.usedSpace();
    }
}
