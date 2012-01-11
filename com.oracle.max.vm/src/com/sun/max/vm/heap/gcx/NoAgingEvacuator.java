/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.heap.HeapSchemeAdaptor.*;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
/**
 * A simple evacuator that evacuates only from one space to another, without aging.
 * The evacuator is parameterized with two heap space.
 *
 * TODO: move allocation and cfotable update code into a wrapper of the FirsFitMarkSweepSpace.
 * The wrapper keeps track of survivor ranges and update the remembered set (mostly the cfo table).
 * This makes the evacuator independent of the detail of survivor ranges tracking and imprecise rset subtleties.
 */
public final class NoAgingEvacuator extends Evacuator {
    /**
     * Heap Space that is being evacuated.
     */
    final HeapSpace fromSpace;

    /**
     * Heap Space where the evacuated cells will relocate.
     */
    final HeapSpace toSpace;

    /**
     * Threshold below which refills of the thread local promotion space is automatic.
     */
    private final Size minRefillThreshold;

    /**
     * Hint of size of local allocation buffer when refilling.
     */
    private final Size labSize;

    private final Size LAB_HEADROOM;

    /**
     * Remembered set of the from space.
     */
    private final CardTableRSet rset;

    /**
     * Cached card first object table for fast access.
     */
    private final CardFirstObjectTable cfoTable;

    /**
     * Amount of promoted bytes.
     */
    private Size promotedBytes;

   /**
     * Allocation hand in private promotion space.
     */
    private Pointer top;
    /***
     * End of the private promotion space.
     */
    private Pointer end;

    private Pointer nextCardBoundary;

    private Address nextLABChunk;

    /**
     * Mark to keep track of survivor ranges.
     */
    private Address allocatedRangeStart;

    /**
     * Start of the last unrecorded survivor ranges resulting from overflow allocation.
     */
    private Address lastOverflowAllocatedRangeStart;
    /**
     * End of the last unrecorded survivor ranges resulting from overflow allocation. This is kept to try
     * to coalesce the range with the overflow allocation request. If the ranges cannot be coalesced,
     * the last range is recorded and a new range begin with the result of new allocation request.
     */
    private Address lastOverflowAllocatedRangeEnd;

    /**
     * Queue of survivor ranges remaining to process for evacuation.
     */
    private final SurvivorRangesQueue survivorRanges;

    private final HeapSpaceRangeVisitor heapSpaceDirtyCardClosure = new HeapSpaceRangeVisitor() {
        @Override
        public void visitCells(Address start, Address end) {
            rset.cleanAndVisitCards(start, end, NoAgingEvacuator.this);
        }
    };

    public NoAgingEvacuator(HeapSpace fromSpace, HeapSpace toSpace, CardTableRSet rset, Size minRefillThreshold, SurvivorRangesQueue queue, Size labSize) {
        this.fromSpace = fromSpace;
        this.toSpace = toSpace;
        this.rset = rset;
        this.cfoTable = rset.cfoTable;
        this.minRefillThreshold = minRefillThreshold;
        this.survivorRanges = queue;
        this.labSize = labSize;
        this.LAB_HEADROOM =  MIN_OBJECT_SIZE;
    }

    @Override
    protected void doBeforeEvacuation() {
        promotedBytes = Size.zero();
        lastOverflowAllocatedRangeStart = Pointer.zero();
        lastOverflowAllocatedRangeEnd = Pointer.zero();
        fromSpace.doBeforeGC();
        if (top.isZero()) {
            Address chunk = toSpace.allocateTLAB(labSize);
            nextLABChunk = HeapFreeChunk.getFreeChunkNext(chunk);
            top = chunk.asPointer();
            end = chunk.plus(HeapFreeChunk.getFreechunkSize(chunk)).minus(LAB_HEADROOM).asPointer();
        }
        allocatedRangeStart = top;
    }

    @Override
    protected void doAfterEvacuation() {
        survivorRanges.clear();
        fromSpace.doAfterGC();
        Pointer limit = end.plus(LAB_HEADROOM);
        Size spaceLeft = limit.minus(top).asSize();
        if (spaceLeft.lessThan(minRefillThreshold)) {
            // Will trigger refill in doBeforeEvacution on next GC
            top = Pointer.zero();
            end = Pointer.zero();
            if (spaceLeft.isZero()) {
                fillWithDeadObject(top, limit);
            }
        } else {
            // Leave remaining space in an iterable format.
            // Next evacuation will start from top again.
            HeapFreeChunk.format(top, spaceLeft);
        }
    }

    private void recordRange(Address start, Address end) {
        promotedBytes = promotedBytes.plus(end.minus(start));
        survivorRanges.add(start, end);
    }

    private void updateSurvivorRanges() {
        if (top.greaterThan(allocatedRangeStart)) {
            // Something was allocated in the current evacuation allocation buffer.
            recordRange(allocatedRangeStart, top);
            allocatedRangeStart = top;
        }
        if (lastOverflowAllocatedRangeEnd.greaterThan(lastOverflowAllocatedRangeStart)) {
            recordRange(lastOverflowAllocatedRangeStart, lastOverflowAllocatedRangeEnd);
            lastOverflowAllocatedRangeStart = lastOverflowAllocatedRangeEnd;
        }
    }

    Pointer refillOrAllocate(Size size) {
        if (size.lessThan(minRefillThreshold)) {
            // check if request can fit in the remaining space when taking the headroom into account.
            Pointer limit = end.plus(LAB_HEADROOM);
            if (top.plus(size).equals(limit)) {
                // Does fit.
                return top;
            }
            if (top.lessThan(limit)) {
                // format remaining storage into dead space for parsability
                fillWithDeadObject(top, limit);
            }
            // Check if there is another chunk in the lab.
            Address chunk = nextLABChunk;
            if (chunk.isZero()) {
                chunk = toSpace.allocateTLAB(labSize);
                // FIXME: we should have exception path to handle out of memory here -- rollback or stop evacuation to initiate full GC or throw OOM
                assert !chunk.isZero() && HeapFreeChunk.getFreechunkSize(chunk).greaterEqual(minRefillThreshold);
            }
            nextLABChunk = HeapFreeChunk.getFreeChunkNext(chunk);
            if (!chunk.equals(limit)) {
                recordRange(allocatedRangeStart, top);
                allocatedRangeStart = chunk;
            }
            top = chunk.asPointer();
            end = chunk.plus(HeapFreeChunk.getFreechunkSize(chunk)).minus(LAB_HEADROOM).asPointer();
            // Return zero to force loop back.
            return Pointer.zero();
        }
        // Overflow allocate
        final Pointer cell = toSpace.allocate(size);

        if (!cell.equals(lastOverflowAllocatedRangeEnd)) {
            if (lastOverflowAllocatedRangeEnd.greaterThan(lastOverflowAllocatedRangeStart)) {
                recordRange(lastOverflowAllocatedRangeStart, lastOverflowAllocatedRangeEnd);
            }
            lastOverflowAllocatedRangeStart = cell;
        }
        lastOverflowAllocatedRangeEnd = cell.plus(size);
        cfoTable.set(cell, lastOverflowAllocatedRangeEnd);
        return cell;
    }

    @Override
    boolean inEvacuatedArea(Pointer origin) {
        return fromSpace.contains(origin);
    }

    private Pointer allocate(Size size) {
        Pointer cell = top;
        Pointer newTop = top.plus(size);
        while (newTop.greaterThan(end)) {
            cell = refillOrAllocate(size);
            if (!cell.isZero()) {
                return cell;
            }
            // We refilled. Retry allocating from local allocation buffer.
            cell = top;
            newTop = top.plus(size);
        }
        top = newTop;
        cfoTable.set(cell, top);
        return cell;
    }

    @Override
    void updateRSet(Pointer refHolderOrigin, int wordIndex, Reference ref) {
    }

    @Override
    Pointer evacuate(Pointer fromOrigin) {
        final Pointer fromCell = Layout.originToCell(fromOrigin);
        final Size size = Layout.size(fromOrigin);
        final Pointer toCell = allocate(size);
        Memory.copyBytes(fromCell, toCell, size);
        return toCell;
    }

    @Override
    protected void evacuateFromRSets() {
        // Visit the dirty cards of the old gen (i.e., the toSpace).
        toSpace.visit(heapSpaceDirtyCardClosure);
    }

    @Override
    protected void evacuateReachables() {
        updateSurvivorRanges();
        while (!survivorRanges.isEmpty()) {
            evacuateRange(survivorRanges.start(), survivorRanges.end());
            survivorRanges.remove();
            updateSurvivorRanges();
        }
    }
}
