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
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;

/**
 * A simple nursery implementation that allocates objects in a single contiguous space and evacuate all survivors to the next generation on minor collections.
 * The next generation is responsible for keeping a reserve large enough to accommodate the worst-case evacuation.
 */
public class NoAgingNursery implements HeapSpace {

    final class NurseryRefiller extends Refiller {
        @Override
        public Address allocateRefill(Pointer startOfSpaceLeft, Size spaceLeft) {
            Size size = allocator.size();
            while (!Heap.collectGarbage(size)) {
                size = allocator.size();
                // TODO: condition for OOM
            }
            // We're out of safepoint. The current thread hold the refill lock and will do the refill of the allocator.
            return allocator.start;
        }

        @Override
        protected void doBeforeGC() {
            // Nothing to do.
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
        return allocator.allocateCleared(size);
    }

    @Override
    public Size growAfterGC(Size delta) {
        // TODO
        FatalError.unimplemented();
        return Size.zero();
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
        final Pointer tlab = allocator.allocateCleared(size);
        HeapFreeChunk.format(tlab, size);
        return tlab;
    }

    @Override
    public boolean contains(Address address) {
        return address.greaterEqual(allocator.start) && address.lessThan(allocator.end);
    }

    @Override
    public void doBeforeGC() {
        allocator.doBeforeGC();
    }

    @Override
    public void doAfterGC() {
        if (MaxineVM.isDebug()) {
            allocator.zap();
        }
        // Format the allocator's space as a heap free chunk to comply with the allocateRefill interface.
        // This is pure overhead induced by the BaseAtomicBumpPointerAllocator interface.
        HeapFreeChunk.format(allocator.start, allocator.size());
        // We leave the allocator in the "full state" (i.e., top == hardLimit) to avoid the mutator doing the refill racing with
        // other mutators allocating as soon as we're out of the safepoint. That is, allocating requeste will be forced to
        // queue on the refill lock until the thread that caused the GC has perform the refill.
        // Doing otherwise requires changing the BaseAtomicBumpPointerAllocator.refillOrAllocate logic.
    }

    @Override
    public Size freeSpace() {
        return allocator.freeSpace();
    }

    @Override
    public Size usedSpace() {
        return allocator.usedSpace();
    }

    @Override
    public void visit(HeapSpaceRangeVisitor visitor) {
        visitor.visitCells(allocator.start, allocator.top);
    }

}
