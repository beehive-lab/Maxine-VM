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

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.gcx.EvacuatingSpace.SpaceBounds;
import com.sun.max.vm.heap.gcx.rset.ctbl.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.runtime.*;
/**
 * A heap space evacuator that evacuates objects from one space to a card-table covered space.
 * Locations of references to evacuatees from other heap spaces are provided by a card table.
 *
 * TODO: replace direct cfotable updates with proper use of the DeadSpaceListener interface implemented by the card table.
 * (see all fixme comments below). This would make allocation in survivor space independent of details of the card table RSet.
 */
public class EvacuatorToCardSpace extends Evacuator {
    @FOLD
    private static Size evacuationBufferHeadroom() {
        return minObjectSize();
    }

    /**
     * Heap Space that is being evacuated.
     */
    EvacuatingSpace fromSpace;

    /**
     * Heap Space where the evacuated cells will relocate.
     */
    protected HeapSpace toSpace;

    /**
     * Threshold below which refills of the thread local promotion space is automatic.
     */
    private Size minRefillThreshold;

    /**
     * Set to true if always refill.
     */
    private boolean alwaysRefill;

    /**
     * Flags indicating that Evacuation buffers must be retired after evacuation.
     */
    private boolean retireAfterEvacuation;

    /**
     * The provider of the evacuation buffer for this evacuator.
     */
    private final EvacuationBufferProvider evacuationBufferProvider;

    /**
     * Remembered set of the from space.
     */
    protected final CardTableRSet rset;

    /**
     * Cached card first object table for fast access.
     */
    protected final CardFirstObjectTable cfoTable;

    /**
     * Amount of evacuated bytes.
     */
    private Size evacuatedBytes;

   /**
     * Allocation hand to the evacuator's private promotion space.
     */
    protected Pointer ptop;

    /***
     * End of the evacuator's private promotion space.
     */
    protected Pointer pend;

    /**
     * Next free space chunk in the evacuator's private promotion space.
     */
    private Address pnextChunk;

    /**
     * Mark to keep track of survivor ranges.
     */
    @INSPECTED
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
     * Bounds of the evacuated space. For fast in-bound testing.
     */
    private EvacuatingSpace.SpaceBounds evacuatedAreaBounds;

    private final EvacuationLogger logger;

    public EvacuatorToCardSpace(EvacuatingSpace fromSpace, HeapSpace toSpace, EvacuationBufferProvider evacuationBufferProvider, CardTableRSet rset, String name) {
        this.fromSpace = fromSpace;
        this.toSpace = toSpace;
        this.rset = rset;
        this.cfoTable = rset.cfoTable;
        this.evacuationBufferProvider = evacuationBufferProvider;
        this.evacuatedAreaBounds = fromSpace.bounds();
        this.logger = new EvacuationLogger(name);
    }

    public void setEvacuationSpace(EvacuatingSpace fromSpace,  HeapSpace toSpace) {
        this.fromSpace = fromSpace;
        this.toSpace = toSpace;
        evacuatedAreaBounds = fromSpace.bounds();
    }

    /**
     * Initialize the evacuator.
     *
     * @param maxSurvivorRanges maximum number of discontinuous range of survivors the evacuator may have to keep track of during evacuation
     * @param alwaysRefill if true, always refill on allocation failure, no matter what's left over in the current evacuation buffer. Otherwise, use minRefillThreshold to trigger refill
     * @param minRefillThreshold refill on allocation failure if the amount of space left in evacuation buffer is less than this threshold and alwaysRefill is false
     * @param retireAfterEvacuation indicate whether evacuation buffer are kept across evacuation. If set to false, the evacuation buffer is retire to its provider after evacuation.
     */
    public void initialize(int maxSurvivorRanges, boolean alwaysRefill, Size minRefillThreshold, boolean retireAfterEvacuation) {
        this.survivorRanges = new SurvivorRangesQueue(maxSurvivorRanges);
        this.alwaysRefill = alwaysRefill;
        this.minRefillThreshold =  alwaysRefill ? Size.fromLong(Long.MAX_VALUE) : minRefillThreshold;
        this.retireAfterEvacuation = retireAfterEvacuation;
    }

    /**
     * Number of bytes evacuated in the last evacuation.
     * @return a number of bytes
     */
    public Size evacuatedBytes() {
        return evacuatedBytes;
    }

    /**
     * Retire promotion buffer before a GC on the promotion space is performed.
     */
    @Override
    public void doBeforeGC() {
        if (MaxineVM.isDebug() && !ptop.isZero()) {
            FatalError.check(HeapFreeChunk.isTailFreeChunk(ptop, pend.plus(evacuationBufferHeadroom())), "Evacuator's allocation buffer must be parseable");
        }
        ptop = Pointer.zero();
        pend = Pointer.zero();
    }

    @Override
    protected void doBeforeEvacuation() {
        fromSpace.doBeforeGC();
        evacuatedBytes = Size.zero();
        lastOverflowAllocatedRangeStart = Pointer.zero();
        lastOverflowAllocatedRangeEnd = Pointer.zero();

        if (ptop.isZero()) {
            Address chunk = evacuationBufferProvider.refillEvacuationBuffer();
            Size chunkSize = HeapFreeChunk.getFreechunkSize(chunk);
            pnextChunk = HeapFreeChunk.getFreeChunkNext(chunk);
            rset.notifyRefill(chunk, chunkSize);
            ptop = chunk.asPointer();
            pend = chunk.plus(chunkSize.minus(evacuationBufferHeadroom())).asPointer();
        }
        allocatedRangeStart = ptop;
        if (logger.enabled()) {
            SpaceBounds toSpaceBounds = toSpace.bounds();
            logger.logBeginEvacuation(evacuatedAreaBounds.lowestAddress(), evacuatedAreaBounds.highestAddress(), toSpaceBounds.lowestAddress(), toSpaceBounds.highestAddress());
        }
    }

    @Override
    protected void doAfterEvacuation() {
        survivorRanges.clear();
        fromSpace.doAfterGC();
        Pointer limit = pend.plus(evacuationBufferHeadroom());
        if (logger.enabled()) {
            logger.logEndEvacuation(limit);
        }
        Size spaceLeft = limit.minus(ptop).asSize();
        if ((alwaysRefill && spaceLeft.greaterThan(minObjectSize())) || spaceLeft.greaterEqual(minRefillThreshold)) {
            // Leave remaining space in an iterable format.
            // Next evacuation will start from top again.
            HeapFreeChunk.format(ptop, spaceLeft);
            rset.notifyRetireFreeSpace(ptop, spaceLeft);
            if (retireAfterEvacuation) {
                // Note: if an overflow occurred and the TLAB isn't in the toSpace but in some other space, the leftover will not be retired but simply formatted as dead object.
                evacuationBufferProvider.retireEvacuationBuffer(ptop, limit);
                // Will trigger refill in doBeforeEvacution on next GC
                ptop = Pointer.zero();
                pend = Pointer.zero();
            }
        } else {
            if (!spaceLeft.isZero()) {
                DarkMatter.format(ptop, spaceLeft);
                rset.notifyRetireDeadSpace(ptop, spaceLeft);
            }
            // Will trigger refill in doBeforeEvacution on next GC
            ptop = Pointer.zero();
            pend = Pointer.zero();
        }
    }

    private void recordRange(Address start, Address end) {
        evacuatedBytes = evacuatedBytes.plus(end.minus(start));
        survivorRanges.add(start, end);
        if (logger.enabled()) {
            logger.logUpdateSurvivorRange(start, end);
        }
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

    /**
     * Prefill survivor ranges with a range of addresses in the to-space.
     * This must be done before evacuation start.
     * Currently used for situation when minor collection overflowed to the from-space.
     *
     * @param start start of the range (inclusive)
     * @param end end of the range (exclusive)
     */
    public void prefillSurvivorRanges(Address start, Address end) {
        FatalError.check(toSpace.contains(start) && toSpace.contains(end), "Range must be in to-space");
        survivorRanges.add(start, end);
        if (logger.enabled()) {
            logger.logPrefillSurvivorRanges(start, end);
        }
    }

    private Address debugRetired_ptop = Address.zero(); // FIXME: just for debugging for now

    protected Pointer refillOrAllocate(Size size) {
        if (size.lessThan(minRefillThreshold)) {
            // check if request can fit in the remaining space when taking the headroom into account.
            Pointer limit = pend.plus(evacuationBufferHeadroom());
            if (ptop.plus(size).equals(limit)) {
                // Does fit.
                return ptop;
            }
            if (ptop.lessThan(limit)) {
                debugRetired_ptop = ptop;
                // Retire and update FOT accordingly
                // FIXME: same as  rset.notifyRetireDeadSpace(ptop, limit.minus(ptop).asSize()) but faster. It'll be cleaner to use the rset interface though.
                cfoTable.set(ptop, limit);
                evacuationBufferProvider.retireEvacuationBuffer(ptop, limit);
                if (MaxineVM.isDebug()) {
                    final Address deadSpaceLastWordAddress = limit.minus(Word.size());
                    if (CardTableRSet.alignDownToCard(ptop).lessThan(CardTableRSet.alignDownToCard(deadSpaceLastWordAddress))) {
                        FatalError.check(ptop.equals(cfoTable.cellStart(rset.cardTable.tableEntryIndex(deadSpaceLastWordAddress))), "corrupted FOT");
                    }
                }
            }
            // Check if there is another chunk in the lab.
            Address chunk = pnextChunk;
            if (chunk.isZero()) {
                chunk = evacuationBufferProvider.refillEvacuationBuffer();
                FatalError.check(!chunk.isZero() && (alwaysRefill || HeapFreeChunk.getFreechunkSize(chunk).greaterEqual(minRefillThreshold)), "refill request should always succeed");
            }
            pnextChunk = HeapFreeChunk.getFreeChunkNext(chunk);
            if (!chunk.equals(limit)) {
                recordRange(allocatedRangeStart, ptop);
                allocatedRangeStart = chunk;
            }
            Size chunkSize = HeapFreeChunk.getFreechunkSize(chunk);
            rset.notifyRefill(chunk, chunkSize);
            ptop = chunk.asPointer();
            pend = chunk.plus(chunkSize.minus(evacuationBufferHeadroom())).asPointer();
            // Return zero to force loop back.
            return Pointer.zero();
        }
        // Overflow allocate
        final Pointer cell = toSpace.allocate(size);
        // Allocator must have already fire a notifySplitLive event to the space's DeadSpaceListener (i.e., the CardTableRSet in this case).
        if (!cell.equals(lastOverflowAllocatedRangeEnd)) {
            if (lastOverflowAllocatedRangeEnd.greaterThan(lastOverflowAllocatedRangeStart)) {
                recordRange(lastOverflowAllocatedRangeStart, lastOverflowAllocatedRangeEnd);
            }
            lastOverflowAllocatedRangeStart = cell;
        }
        lastOverflowAllocatedRangeEnd = cell.plus(size);
        return cell;
    }
    @INLINE
    @Override
    final boolean inEvacuatedArea(Pointer origin) {
        return evacuatedAreaBounds.isIn(origin);
    }

    /**
     * Allocate space in evacuator's promotion allocation buffer.
     *
     * @param size
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
        // FIXME ? it'll be cleaner to do a rset.notifySplitLive(cell, size, hardLimit) here.
        cfoTable.set(cell, ptop);
        return cell;
    }

    /**
     * Scan a cell to evacuate the cells in the evacuation area it refers to and update its references to already evacuated cells.
     *
     * @param cell a pointer to a cell
     * @return pointer to the end of the cell
     */
    public final Pointer visitCell(Pointer cell) {
        return scanCellForEvacuatees(cell);
    }

    /**
     * Scan a cell to evacuate the cells in the evacuation area it refers to and update its references to already evacuated cells.
     *
     * @param cell a pointer to a cell
     * @return pointer to the end of the cell
     */
    public final Pointer visitCell(Pointer cell, Address start, Address end) {
        return scanCellForEvacuatees(cell, start, end);
    }

    @Override
    final Pointer evacuate(Pointer fromOrigin) {
        final Pointer fromCell = Layout.originToCell(fromOrigin);
        final Size size = Layout.size(fromOrigin);
        final Pointer toCell = allocate(size);
        Memory.copyBytes(fromCell, toCell, size);
        return toCell;
    }

    @Override
    final protected void evacuateReachables() {
        updateSurvivorRanges();
        while (!survivorRanges.isEmpty()) {
            final Pointer start = survivorRanges.start();
            final Pointer end = survivorRanges.end();
            survivorRanges.remove();
            if (logger.enabled()) {
                logger.logEvacuateSurvivorRange(start, end);
            }
            evacuateRange(start, end);
            updateSurvivorRanges();
        }
    }

    /*
     * Interface for logging evacuation ranges.
     * The interface uses long instead of Size to improve human-readability from the inspector's log views.
     */
    @HOSTED_ONLY
    @VMLoggerInterface(defaultConstructor = true)
    private interface EvacuationLoggerInterface {
        void beginEvacuation(
                        @VMLogParam(name = "fromStart") Address fromStart,
                        @VMLogParam(name = "fromEnd")  Address fromEnd,
                        @VMLogParam(name = "toStart")Address toStart,
                        @VMLogParam(name = "toEnd") Address toEnd);

        void endEvacuation(
                        @VMLogParam(name = "evacuationLimit") Address evacuationLimit
        );

        void evacuateSurvivorRange(
                        @VMLogParam(name = "start") Address start,
                        @VMLogParam(name = "end") Address end
        );
        void updateSurvivorRange(
                        @VMLogParam(name = "start") Address start,
                        @VMLogParam(name = "end") Address end
        );
        void prefillSurvivorRanges(
                        @VMLogParam(name = "start") Address start,
                        @VMLogParam(name = "end") Address end
        );
    }

    static final class EvacuationLogger extends EvacuationLoggerAuto {
        final String instanceName;
        EvacuationLogger(String instanceName) {
            super(instanceName + "Evacuation", "Log evacuation scanning ranges in to-space");
            this.instanceName = instanceName;
        }

        private void traceRange(String title, Address start, Address end, boolean newLine) {
            Log.print(title);
            Log.printRange(start, end, newLine);
        }

        @Override
        protected void traceEvacuateSurvivorRange(Address start, Address end) {
            traceRange("Evacuated range ", start, end, true);
        }

        @Override
        protected void tracePrefillSurvivorRanges(Address start, Address end) {
            traceRange("Prefill with range ", start, end, true);
        }

        @Override
        protected void traceUpdateSurvivorRange(Address start, Address end) {
            traceRange("Add range", start, end, true);
        }

        @Override
        protected void traceBeginEvacuation(Address fromStart, Address fromEnd, Address toStart, Address toEnd) {
            Log.print("Begin "); Log.print(instanceName); Log.print(" evacuation: ");
            traceRange("from space ", fromStart, fromEnd, false);
            traceRange("to space,",  toStart, toEnd, true);
        }

        @Override
        protected void traceEndEvacuation(Address evacuationLimit) {
            Log.print("End "); Log.print(instanceName); Log.print(" evacuation : "); Log.println(evacuationLimit);
        }

    }
// START GENERATED CODE
    private static abstract class EvacuationLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            BeginEvacuation, EndEvacuation, EvacuateSurvivorRange,
            PrefillSurvivorRanges, UpdateSurvivorRange;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected EvacuationLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        protected EvacuationLoggerAuto() {
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logBeginEvacuation(Address fromStart, Address fromEnd, Address toStart, Address toEnd) {
            log(Operation.BeginEvacuation.ordinal(), fromStart, fromEnd, toStart, toEnd);
        }
        protected abstract void traceBeginEvacuation(Address fromStart, Address fromEnd, Address toStart, Address toEnd);

        @INLINE
        public final void logEndEvacuation(Address evacuationLimit) {
            log(Operation.EndEvacuation.ordinal(), evacuationLimit);
        }
        protected abstract void traceEndEvacuation(Address evacuationLimit);

        @INLINE
        public final void logEvacuateSurvivorRange(Address start, Address end) {
            log(Operation.EvacuateSurvivorRange.ordinal(), start, end);
        }
        protected abstract void traceEvacuateSurvivorRange(Address start, Address end);

        @INLINE
        public final void logPrefillSurvivorRanges(Address start, Address end) {
            log(Operation.PrefillSurvivorRanges.ordinal(), start, end);
        }
        protected abstract void tracePrefillSurvivorRanges(Address start, Address end);

        @INLINE
        public final void logUpdateSurvivorRange(Address start, Address end) {
            log(Operation.UpdateSurvivorRange.ordinal(), start, end);
        }
        protected abstract void traceUpdateSurvivorRange(Address start, Address end);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //BeginEvacuation
                    traceBeginEvacuation(toAddress(r, 1), toAddress(r, 2), toAddress(r, 3), toAddress(r, 4));
                    break;
                }
                case 1: { //EndEvacuation
                    traceEndEvacuation(toAddress(r, 1));
                    break;
                }
                case 2: { //EvacuateSurvivorRange
                    traceEvacuateSurvivorRange(toAddress(r, 1), toAddress(r, 2));
                    break;
                }
                case 3: { //PrefillSurvivorRanges
                    tracePrefillSurvivorRanges(toAddress(r, 1), toAddress(r, 2));
                    break;
                }
                case 4: { //UpdateSurvivorRange
                    traceUpdateSurvivorRange(toAddress(r, 1), toAddress(r, 2));
                    break;
                }
            }
        }
    }

// END GENERATED CODE
}
