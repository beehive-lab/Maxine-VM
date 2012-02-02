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
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;
/**
 * A heap space evacuator that evacuate objects from one space to another, without aging.
 * Locations of references to evacuatees from other heap spaces are provided by a card table.
 *
 * TODO: move allocation and cfotable update code into a wrapper of the FirsFitMarkSweepSpace.
 * The wrapper keeps track of survivor ranges and update the remembered set (mostly the cfo table).
 * This makes the evacuator independent of the detail of survivor ranges tracking and imprecise rset subtleties.
 */
public final class NoAgingEvacuator extends Evacuator {
    public static boolean TraceDirtyCardWalk = false;
    static {
        VMOptions.addFieldOption("-XX:", "TraceDirtyCardWalk", NoAgingEvacuator.class, "Trace Dirty Card Walk", Phase.PRISTINE);
    }
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
     * Hint of amount of space to use to refill the promotion allocation buffer.
     */
    private Size pSize;

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
     * Allocation hand to the evacuator's private promotion space.
     */
    private Pointer ptop;

    /***
     * End of the evacuator's private promotion space.
     */
    private Pointer pend;

    /**
     * Next free space chunk in the evacuator's private promotion space.
     */
    private Address pnextChunk;

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
    private SurvivorRangesQueue survivorRanges;

    /**
     * Closure for evacuating cells in dirty card. A dirty card may overlap with an area currently used for allocation.
     * Allocation is made via the evacuator's promotion lab, which may during iteration over dirty cards.
     * To avoid maintaining parsability of the promotion lab at every allocation,
     * we check if the visited cell boundary coincide with the first free bytes of the allocator, and skip it if it does.
     *
     * Note that the allocator that feed the promotion lab is kept in an iterable state.
     */
    final class DirtyCardEvacuationClosure implements CellVisitor, OverlappingCellVisitor,  HeapSpaceRangeVisitor {
        private final CardTableRSet cachedRSet;

        DirtyCardEvacuationClosure() {
            cachedRSet = rset;
        }
        @Override
        public Pointer visitCell(Pointer cell, Address start, Address end) {
            if (cell.equals(ptop)) {
                // Skip allocating area.
                return pend;
            }
            return scanCellForEvacuatees(cell, start, end);
        }

        @Override
        public Pointer visitCell(Pointer cell) {
            if (cell.equals(ptop)) {
                // Skip allocating area
                return pend;
            }
            return scanCellForEvacuatees(cell);
        }

        public void visitCells(Address start, Address end) {
            if (MaxineVM.isDebug() && dumper != null && HeapRangeDumper.DumpOnError) {
                dumper.setRange(start, end);
                FatalError.setOnVMOpError(dumper);
            }
            cachedRSet.cleanAndVisitCards(start, end, this);
            if (MaxineVM.isDebug()) {
                FatalError.setOnVMOpError(null);
            }
        }
    }

    private final DirtyCardEvacuationClosure heapSpaceDirtyCardClosure;

    public NoAgingEvacuator(HeapSpace fromSpace, HeapSpace toSpace, CardTableRSet rset, Size minRefillThreshold) {
        this.fromSpace = fromSpace;
        this.toSpace = toSpace;
        this.rset = rset;
        this.cfoTable = rset.cfoTable;
        this.minRefillThreshold = minRefillThreshold;
        this.LAB_HEADROOM =  MIN_OBJECT_SIZE;
        this.heapSpaceDirtyCardClosure = new DirtyCardEvacuationClosure();
    }

    public void initialize(int maxSurvivorRanges, Size labSize) {
        this.survivorRanges = new SurvivorRangesQueue(maxSurvivorRanges);
        this.pSize = labSize;
    }

    /**
     * Retire promotion buffer before a GC on the promotion space is performed.
     */
    @Override
    public void doBeforeGC() {
        if (MaxineVM.isDebug() && !ptop.isZero()) {
            FatalError.check(HeapFreeChunk.isTailFreeChunk(ptop, pend.plus(LAB_HEADROOM)), "Evacuator's allocation buffer must be parseable");
        }
        ptop = Pointer.zero();
        pend = Pointer.zero();
    }

    @Override
    protected void doBeforeEvacuation() {
        fromSpace.doBeforeGC();
        promotedBytes = Size.zero();
        lastOverflowAllocatedRangeStart = Pointer.zero();
        lastOverflowAllocatedRangeEnd = Pointer.zero();

        if (ptop.isZero()) {
            Address chunk = toSpace.allocateTLAB(pSize);
            pnextChunk = HeapFreeChunk.getFreeChunkNext(chunk);
            ptop = chunk.asPointer();
            pend = chunk.plus(HeapFreeChunk.getFreechunkSize(chunk)).minus(LAB_HEADROOM).asPointer();
        }
        allocatedRangeStart = ptop;
    }

    @Override
    protected void doAfterEvacuation() {
        survivorRanges.clear();
        fromSpace.doAfterGC();
        Pointer limit = pend.plus(LAB_HEADROOM);
        Size spaceLeft = limit.minus(ptop).asSize();
        if (spaceLeft.lessThan(minRefillThreshold)) {
            // Will trigger refill in doBeforeEvacution on next GC
            if (!spaceLeft.isZero()) {
                fillWithDeadObject(ptop, limit);
            }
            ptop = Pointer.zero();
            pend = Pointer.zero();
        } else {
            // Leave remaining space in an iterable format.
            // Next evacuation will start from top again.
            HeapFreeChunk.format(ptop, spaceLeft);
        }
    }

    private void recordRange(Address start, Address end) {
        promotedBytes = promotedBytes.plus(end.minus(start));
        survivorRanges.add(start, end);
    }

    private void updateSurvivorRanges() {
        if (ptop.greaterThan(allocatedRangeStart)) {
            // Something was allocated in the current evacuation allocation buffer.
            recordRange(allocatedRangeStart, ptop);
            allocatedRangeStart = ptop;
        }
        if (lastOverflowAllocatedRangeEnd.greaterThan(lastOverflowAllocatedRangeStart)) {
            recordRange(lastOverflowAllocatedRangeStart, lastOverflowAllocatedRangeEnd);
            lastOverflowAllocatedRangeStart = lastOverflowAllocatedRangeEnd;
        }
    }

    private Pointer refillOrAllocate(Size size) {
        if (size.lessThan(minRefillThreshold)) {
            // check if request can fit in the remaining space when taking the headroom into account.
            Pointer limit = pend.plus(LAB_HEADROOM);
            if (ptop.plus(size).equals(limit)) {
                // Does fit.
                return ptop;
            }
            if (ptop.lessThan(limit)) {
                // format remaining storage into dead space for parsability
                fillWithDeadObject(ptop, limit);
            }
            // Check if there is another chunk in the lab.
            Address chunk = pnextChunk;
            if (chunk.isZero()) {
                chunk = toSpace.allocateTLAB(pSize);
                // FIXME: we should have exception path to handle out of memory here -- rollback or stop evacuation to initiate full GC or throw OOM
                assert !chunk.isZero() && HeapFreeChunk.getFreechunkSize(chunk).greaterEqual(minRefillThreshold);
            }
            pnextChunk = HeapFreeChunk.getFreeChunkNext(chunk);
            if (!chunk.equals(limit)) {
                recordRange(allocatedRangeStart, ptop);
                allocatedRangeStart = chunk;
            }
            ptop = chunk.asPointer();
            pend = chunk.plus(HeapFreeChunk.getFreechunkSize(chunk)).minus(LAB_HEADROOM).asPointer();
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

    /**
     * Allocate space in evacuator's promotion allocation buffer.
     *
     * @param size
     * @return
     */
    private Pointer allocate(Size size) {
        Pointer cell = ptop;
        Pointer newTop = ptop.plus(size);
        while (newTop.greaterThan(pend)) {
            cell = refillOrAllocate(size);
            if (!cell.isZero()) {
                return cell;
            }
            // We refilled. Retry allocating from local allocation buffer.
            cell = ptop;
            newTop = ptop.plus(size);
        }
        ptop = newTop;
        cfoTable.set(cell, ptop);
        return cell;
    }

    /**
     * Scan a cell to evacuate the cells in the evacuation area it refers to and update its references to already evacuated cells.
     *
     * @param cell a pointer to a cell
     * @return pointer to the end of the cell
     */
    public Pointer visitCell(Pointer cell) {
        return scanCellForEvacuatees(cell);
    }

    /**
     * Scan a cell to evacuate the cells in the evacuation area it refers to and update its references to already evacuated cells.
     *
     * @param cell a pointer to a cell
     * @return pointer to the end of the cell
     */
    public Pointer visitCell(Pointer cell, Address start, Address end) {
        return scanCellForEvacuatees(cell, start, end);
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
        final boolean traceRSet = CardTableRSet.TraceCardTableRSet;
        if (MaxineVM.isDebug() && TraceDirtyCardWalk) {
            CardTableRSet.TraceCardTableRSet = true;
        }
        toSpace.visit(heapSpaceDirtyCardClosure);
        if (MaxineVM.isDebug() && TraceDirtyCardWalk) {
            CardTableRSet.TraceCardTableRSet = traceRSet;
        }
    }

    @Override
    protected void evacuateReachables() {
        updateSurvivorRanges();
        while (!survivorRanges.isEmpty()) {
            final Pointer start = survivorRanges.start();
            final Pointer end = survivorRanges.end();
            survivorRanges.remove();
            evacuateRange(start, end);
            updateSurvivorRanges();
        }
    }
}
