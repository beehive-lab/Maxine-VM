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

import static com.sun.max.vm.heap.gcx.EvacuationTimers.TIMED_OPERATION.*;
import static com.sun.max.vm.heap.gcx.HeapFreeChunk.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.debug.DebugHeap.DetailLogger;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.VMLogger.Interval;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * Base class for evacuating objects of an evacuated area made of possibly discontinuous range of addresses.
 * The class provides common services. Sub-classes define what set of contiguous ranges delimit the evacuated area,
 *  what set of contiguous ranges hold the evacuated objects whose references haven't been processed yet, and
 * the remembered set holdings references to the evacuated area.
 *
 */
public abstract class Evacuator extends PointerIndexVisitor implements CellVisitor, OverlappingCellVisitor, SpecialReferenceManager.GC  {
    /**
     * Tells from what GC invocation should tracing of dirty card starts.
     */
    public static int TraceFromGCInvocation = 0;
    private static boolean TraceEvacVisitedCell = false;
    private static boolean traceEvacVisitedCellEnabled = false;

    static {
        VMOptions.addFieldOption("-XX:", "TraceFromGCInvocation", Evacuator.class, "Tells from which GC invocation tracing starts from", Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "TraceEvacVisitedCell", Evacuator.class, "Trace cells visited by the evacuator (Debug mode only)", Phase.PRISTINE);
    }

    @INLINE
    protected final boolean traceEvacVisitedCell() {
        return MaxineVM.isDebug() && traceEvacVisitedCellEnabled && detailLogger.enabled();
    }

    private final SequentialHeapRootsScanner heapRootsScanner = new SequentialHeapRootsScanner(this);

    private boolean refDiscoveryEnabled = true;

    private GCOperation currentGCOperation;

    private EvacuationTimers timers;

    protected PhaseLogger phaseLogger;

    protected DetailLogger detailLogger;

    public void setGCOperation(GCOperation gcOperation) {
        currentGCOperation = gcOperation;
        if (MaxineVM.isDebug() && gcOperation != null) {
            traceEvacVisitedCellEnabled = TraceEvacVisitedCell && TraceFromGCInvocation <= gcOperation.invocationCount();
        }
    }

    final public GCOperation getGCOperation() {
        return currentGCOperation;
    }

    private void updateSpecialReference(Pointer origin) {
        if (refDiscoveryEnabled) {
            SpecialReferenceManager.discoverSpecialReference(origin);
        } else {
            // Treat referent as strong reference.
            if (traceEvacVisitedCell()) {
                Log.print("Resurecting referent");
                Log.print(origin.getReference(SpecialReferenceManager.referentIndex()).toOrigin());
                Log.print(" from special ref ");
                Log.print(origin); Log.print(" + ");
                Log.println(SpecialReferenceManager.referentIndex());
            }
            updateEvacuatedRef(origin, SpecialReferenceManager.referentIndex());
        }
    }

    final void enableSpecialRefDiscovery() {
        refDiscoveryEnabled = true;
    }

    final void disableSpecialRefDiscovery() {
        refDiscoveryEnabled = false;
    }

    private void updateReferenceArray(Pointer refArrayOrigin, final int firstIndex, final int length) {
        for (int index = firstIndex; index < length; index++) {
            updateEvacuatedRef(refArrayOrigin, index);
        }
    }

    private void updateReferenceArray(Pointer refArrayOrigin) {
        final int length = Layout.readArrayLength(refArrayOrigin) + Layout.firstElementIndex();
        updateReferenceArray(refArrayOrigin, Layout.firstElementIndex(), length);
    }

    private void updateReferenceArray(Pointer refArrayOrigin, Address start, Address end) {
        final int endOfArrayIndex = Layout.readArrayLength(refArrayOrigin) + Layout.firstElementIndex();
        final Address firstElementAddr = refArrayOrigin.plusWords(Layout.firstElementIndex());
        final Address endOfArrayAddr = refArrayOrigin.plusWords(endOfArrayIndex);
        final int firstIndex = start.greaterThan(firstElementAddr) ? start.minus(refArrayOrigin).unsignedShiftedRight(Kind.REFERENCE.width.log2numberOfBytes).toInt() : Layout.firstElementIndex();
        final int endIndex = endOfArrayAddr.greaterThan(end) ? end.minus(refArrayOrigin).unsignedShiftedRight(Kind.REFERENCE.width.log2numberOfBytes).toInt() : endOfArrayIndex;
        updateReferenceArray(refArrayOrigin, firstIndex, endIndex);
    }

    protected HeapRangeDumper dumper;

    protected Evacuator() {
    }

    public void setDumper(HeapRangeDumper dumper) {
        this.dumper = dumper;
    }

    public void setTimers(EvacuationTimers timers) {
        this.timers = timers;
    }

    /**
     * Set the phase logger for this evacuator.
     * HeapScheme using multiple evacuator instances might have to share a single phase logger
     * as currently the Heap class assume there's a single such phase logger per-heap scheme.
     * @param phaseLogger a phase logger
     */
    public void setPhaseLogger(PhaseLogger phaseLogger) {
        this.phaseLogger = phaseLogger;
    }

    /**
     * Set the detail logger for this evacuator.
      * @param detailLogger a detail logger
    */
    public void setDetailLogger(DetailLogger detailLogger) {
        this.detailLogger = detailLogger;
    }

  /**
     * Indicate whether the cell at the specified origin is in an area under evacuation.
     * @param origin origin of a cell
     * @return true if the cell is in an evacuation area
     */
    abstract boolean inEvacuatedArea(Pointer origin);

    /**
     * Evacuate the cell at the specified origin. The destination of the cell is
     * @param origin origin of the cell to evacuate
     * @return origin of the cell after evacuation
     */
    @NEVER_INLINE
    abstract Pointer evacuate(Pointer origin);

    /**
     * Remembered set updates to apply to a reference to an evacuated cell.
     * Default is to do nothing.
     *
     * @param refHolderOrigin origin of the reference holder
     * @param wordIndex word index relative to the reference holder's origin where the reference to the evacuated cell is stored.
     * @param ref reference to an evacuated cell
     */
    void updateRSet(Pointer refHolderOrigin, int wordIndex, Reference ref) {
        // default is doing nothing.
    }


    /**
     * Evacuate a cell of the evacuated area if not already done, and return the reference to the evacuated cell new location.
     *
     * @param origin origin of the cell in the evacuated area
     * @return a reference to the evacuated cell's new location
     */
    protected final Reference getForwardRef(Pointer origin) {
        Reference forwardRef = Layout.readForwardRef(origin);
        if (forwardRef.isZero()) {
            final Pointer toOrigin = evacuate(origin);
            forwardRef = Reference.fromOrigin(toOrigin);
            Layout.writeForwardRef(origin, forwardRef);
            if (MaxineVM.isDebug() && detailLogger.enabled()) {
                final Hub hub = UnsafeCast.asHub(Layout.readHubReference(forwardRef).toJava());
                detailLogger.logForward(hub.classActor, origin, toOrigin, Layout.size(toOrigin).toInt());
            }
        }
        return forwardRef;
    }

    /**
     * Test if a reference in an cell points to the evacuated area. If it does, the referenced cell is
     * first evacuated if it is still in the evacuated area.
     * The reference is updated to the evacuated cell's new location.
     * @param refHolderOrigin origin of the holder of the reference
     * @param wordIndex index to a reference of the evacuated cell.
     */
    final void updateEvacuatedRef(Pointer refHolderOrigin, int wordIndex) {
        final Reference ref = refHolderOrigin.getReference(wordIndex);
        final Pointer origin = ref.toOrigin();
        if (inEvacuatedArea(origin)) {
            final Reference forwardRef = getForwardRef(origin);
            refHolderOrigin.setReference(wordIndex, forwardRef);
            updateRSet(refHolderOrigin, wordIndex, forwardRef);
        }
    }

    /**
     * Apply the evacuation logic to the reference at the specified index from the origin of a cell.
     * @param origin origin of the cell holding the visited reference
     * @param wordIndex  index of the visited reference from the origin
     */
    @Override
    public void visit(Pointer origin, int wordIndex) {
        updateEvacuatedRef(origin, wordIndex);
    }

    @Override
    public boolean isReachable(Reference ref) {
        final Pointer origin = ref.toOrigin();
        if (inEvacuatedArea(origin)) {
            return !Layout.readForwardRef(origin).isZero();
        }
        return true;
    }

    @Override
    public Reference preserve(Reference ref) {
        final Pointer origin = ref.toOrigin();
        if (inEvacuatedArea(origin)) {
            return getForwardRef(origin);
        }
        return ref;
    }

    @Override
    public boolean mayRelocateLiveObjects() {
        return true;
    }

    /**
     * Evacuate all objects of the evacuated area directly reachable from roots (thread stacks, monitors, etc.).
     */
    void evacuateFromRoots() {
        heapRootsScanner.run();
    }
    /**
     * Evacuate all objects of the evacuated area directly reachable from the remembered sets of the evacuated area. By default, this does nothing
     * (i.e., there are no remembered sets). For instance, a pure semi-space flat heap doesn't have any remembered sets to evacuate from, neither does
     * a old semi-space old generation.
     */
    protected void evacuateFromRSets() {
    }

    /**
     * Evacuate all objects of the evacuated area reachable from already evacuated cells.
     */
    abstract protected void evacuateReachables();

    /**
     * Action to performed before evacuation begins, e.g., making evacuated space iterable, etc, initializing allocation buffers, etc.
     */
    abstract protected void doBeforeEvacuation();
    /**
     * Action to performed once evacuation is complete, e.g., releasing or making allocation buffers iterable, zero-ing out evacuated regions, etc.
     */
    abstract protected void doAfterEvacuation();

    /**
     * Action to performed before a GC on the heap space where objects are evacuated begins.
     */
    public void doBeforeGC() {
    }

    /**
     * Action to performed after a GC on the heap space where objects are evacuated begins.
     */
    public void doAfterGC() {

    }
    /**
     * Evacuate all objects of the evacuated area directly reachable from the boot heap.
     */
    protected void evacuateFromBootHeap() {
        Heap.bootHeapRegion.visitReferences(this);
    }

    void evacuateFromCode() {
        // References in the boot code region are immutable and only ever refer
        // to objects in the boot heap region.
        boolean includeBootCode = false;
        Code.visitCells(this, includeBootCode);
    }

    /**
     * Scan a cell to evacuate the cells in the evacuation area it refers to and update its references to already evacuated cells.
     * @param cell
     */
    final protected Pointer scanCellForEvacuatees(Pointer cell) {
        if (traceEvacVisitedCell()) {
            detailLogger.logVisitCell(cell);
        }
        final Pointer origin = Layout.cellToOrigin(cell);
        // Update the hub first so that is can be dereferenced to obtain
        // the reference map needed to find the other references in the object
        updateEvacuatedRef(origin,  Layout.hubIndex());
        final Hub hub =  Layout.getHub(origin);
        if (hub == heapFreeChunkHub()) {
            return cell.plus(toHeapFreeChunk(origin).size);
        }
        // Update the other references in the object
        final SpecificLayout specificLayout = hub.specificLayout;
        if (specificLayout == Layout.tupleLayout()) {
            //TupleReferenceMap.visitReferences(hub, origin, this);
            hub.visitMappedReferences(origin, this);
            if (hub.isJLRReference) {
                updateSpecialReference(origin);
            }
            return cell.plus(hub.tupleSize);
        }
        final int length = Layout.readArrayLength(origin);
        if (specificLayout == Layout.hybridLayout()) {
            hub.visitMappedReferences(origin, this);
            //TupleReferenceMap.visitReferences(hub, origin, this);
            return cell.plus(Layout.hybridLayout().getArraySize(length));
        } else if (specificLayout == Layout.referenceArrayLayout()) {
            updateReferenceArray(origin);
            return cell.plus(Layout.referenceArrayLayout().getArraySize(Kind.REFERENCE, length));
        }
        return cell.plus(Layout.size(origin)); // cell.plus(Layout.arrayLayout().getArraySize(length));
    }

    private boolean cellInRegion(Pointer cell, Pointer endOfCell, Address start, Address end) {
        return cell.greaterEqual(start) && endOfCell.lessEqual(end);
    }

    private void checkCellOverlap(Pointer cell, Address start, Address end) {
        if (MaxineVM.isDebug()) {
            final Pointer origin = Layout.cellToOrigin(cell);
            final boolean isHeapFreeChunk = HeapFreeChunk.isHeapFreeChunkOrigin(origin);
            final Pointer endOfCell = isHeapFreeChunk ? cell.plus(HeapFreeChunk.getFreechunkSize(cell)) : cell.plus(Layout.size(origin));
            if ((cell.lessThan(end) && endOfCell.greaterThan(start)) || cellInRegion(cell, endOfCell, start, end)) {
                return;
            }

            Log.print(isHeapFreeChunk ? "Free Chunk [" : "Cell [");
            Log.print(cell);
            Log.print(',');
            Log.print(endOfCell);
            Log.print("]  don't overlap range [");
            Log.print(start);
            Log.print(", ");
            Log.print(end);
            Log.println("]");
            FatalError.check(false, "Cell doesn't overlap range");
        }
    }

    /**
     * Scan the part of cell that overlap with a region of memory to evacuate the cells in the evacuation area it refers to and update its references
     * to already evacuated cells.
     *
     * @param cell Pointer to the first word of the cell to be visited
     * @param start start of the region overlapping with the cell
     * @param end end of the region overlapping with the cell
     * @return pointer to the end of the cell
     */
    final protected Pointer scanCellForEvacuatees(Pointer cell, Address start, Address end) {
        checkCellOverlap(cell, start, end);
        final Pointer origin = Layout.cellToOrigin(cell);
        Pointer hubReferencePtr = origin.plus(Layout.hubIndex());
        if (hubReferencePtr.greaterEqual(start)) {
            updateEvacuatedRef(origin,  Layout.hubIndex());
        }
        final Hub hub = UnsafeCast.asHub(origin.getReference(Layout.hubIndex()));
        if (hub == heapFreeChunkHub()) {
            return cell.plus(toHeapFreeChunk(origin).size);
        }
        // Update the other references in the object
        final SpecificLayout specificLayout = hub.specificLayout;
        if (specificLayout == Layout.tupleLayout()) {
            // Visit all the references of the object and not just those over the range.
            // This is because the write barrier dirty the card holding the tuple header, not
            // the actually modified reference. Thus bounding the iteration to the dirty card might
            // miss references on the next card.
            //TupleReferenceMap.visitReferences(hub, origin, this);
            hub.visitMappedReferences(origin, this);
            if (hub.isJLRReference) {
                updateSpecialReference(origin);
            }
            return cell.plus(hub.tupleSize);
        }
        if (specificLayout == Layout.referenceArrayLayout()) {
            updateReferenceArray(origin, start, end);
        } else if (specificLayout == Layout.hybridLayout()) {
            hub.visitMappedReferences(origin, this);
            // See comment above
            // TupleReferenceMap.visitReferences(hub, origin, this);
        }
        return cell.plus(Layout.size(origin));
    }

    /**
     * Evacuate all cells from the evacuated area reachable from the specified range of heap addresses.
     * The range comprise an integral number of cells.
     *
     * @param start first address of the range, must coincide with the start of a cell
     * @param end last address of the range, must coincide with the end of a cell
     */
    final void evacuateRange(Pointer start, Pointer end) {
        Pointer cell = start;
        while (cell.lessThan(end)) {
            cell = visitCell(cell);
        }
    }

    public final void evacuate(boolean logPhases) {
        timers.start(PROLOGUE);
        doBeforeEvacuation();
        timers.stop(PROLOGUE);
        HeapScheme.Inspect.notifyHeapPhaseChange(HeapPhase.ANALYZING);

        if (logPhases) {
            phaseLogger.logScanningRoots(VMLogger.Interval.BEGIN);
        }
        timers.start(ROOT_SCAN);
        evacuateFromRoots();
        timers.stop(ROOT_SCAN);
        if (logPhases) {
            phaseLogger.logScanningRoots(VMLogger.Interval.BEGIN);
        }

        if (logPhases) {
            phaseLogger.logScanningBootHeap(VMLogger.Interval.BEGIN);
        }
        timers.start(BOOT_HEAP_SCAN);
        evacuateFromBootHeap();
        timers.stop(BOOT_HEAP_SCAN);
        if (logPhases) {
            phaseLogger.logScanningBootHeap(VMLogger.Interval.END);
        }

        if (logPhases) {
            phaseLogger.logScanningCode(VMLogger.Interval.BEGIN);
        }
        timers.start(CODE_SCAN);
        evacuateFromCode();
        timers.stop(CODE_SCAN);
        if (logPhases) {
            phaseLogger.logScanningCode(VMLogger.Interval.END);
        }

        if (logPhases) {
            phaseLogger.logScanningRSet(VMLogger.Interval.BEGIN);
        }
        timers.start(RSET_SCAN);
        evacuateFromRSets();
        timers.stop(RSET_SCAN);
        if (logPhases) {
            phaseLogger.logScanningRSet(VMLogger.Interval.END);
        }

        if (logPhases) {
            phaseLogger.logEvacuating(VMLogger.Interval.BEGIN);
        }
        timers.start(COPY);
        evacuateReachables();
        timers.stop(COPY);
        if (logPhases) {
            phaseLogger.logEvacuating(VMLogger.Interval.END);
        }

        if (logPhases) {
            phaseLogger.logProcessingSpecialReferences(VMLogger.Interval.BEGIN);
        }
        timers.start(WEAK_REF);
        disableSpecialRefDiscovery();
        SpecialReferenceManager.processDiscoveredSpecialReferences(this);
        evacuateReachables();
        enableSpecialRefDiscovery();
        timers.stop(WEAK_REF);
        if (logPhases) {
            phaseLogger.logProcessingSpecialReferences(VMLogger.Interval.END);
        }

        HeapScheme.Inspect.notifyHeapPhaseChange(HeapPhase.RECLAIMING);
        timers.start(EPILOGUE);
        doAfterEvacuation();
        timers.stop(EPILOGUE);
    }


    @HOSTED_ONLY
    @VMLoggerInterface(parent = HeapScheme.PhaseLogger.class)
    private interface PhaseLoggerInterface {
        void scanningThreadRoots(@VMLogParam(name = "vmThread") VmThread vmThread);
        void scanningRoots(@VMLogParam(name = "interval") Interval interval);
        void scanningBootHeap(@VMLogParam(name = "interval") Interval interval);
        void scanningCode(@VMLogParam(name = "interval") Interval interval);
        void scanningRSet(@VMLogParam(name = "interval") Interval interval);
        void evacuating(@VMLogParam(name = "interval") Interval interval);
        void processingSpecialReferences(@VMLogParam(name = "interval") Interval interval);
    }

    public static final class PhaseLogger extends PhaseLoggerAuto {

        private static void tracePhase(String description, Interval interval) {
            Log.print(interval.name()); Log.print(": "); Log.println(description);
        }

        public PhaseLogger() {
            super(null, null);
        }

        @Override
        protected void traceEvacuating(Interval interval) {
            tracePhase("Evacuating reachables", interval);
        }

        @Override
        protected void traceProcessingSpecialReferences(Interval interval) {
            tracePhase("Processing special references", interval);
        }

        @Override
        protected void traceScanningBootHeap(Interval interval) {
            tracePhase("Scanning boot heap", interval);
        }

        @Override
        protected void traceScanningCode(Interval interval) {
            tracePhase("Scanning code", interval);
        }

        @Override
        protected void traceScanningRSet(Interval interval) {
            tracePhase("Scanning remembered sets", interval);
        }

        @Override
        protected void traceScanningRoots(Interval interval) {
            tracePhase("Scanning roots", interval);
        }

        @Override
        protected void traceScanningThreadRoots(VmThread vmThread) {
            Log.print("Scanning thread local and stack roots for thread ");
            Log.printThread(vmThread, true);
        }

    }

// START GENERATED CODE
    private static abstract class PhaseLoggerAuto extends com.sun.max.vm.heap.HeapScheme.PhaseLogger {
        public enum Operation {
            Evacuating, ProcessingSpecialReferences, ScanningBootHeap,
            ScanningCode, ScanningRSet, ScanningRoots, ScanningThreadRoots;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected PhaseLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logEvacuating(Interval interval) {
            log(Operation.Evacuating.ordinal(), intervalArg(interval));
        }
        protected abstract void traceEvacuating(Interval interval);

        @INLINE
        public final void logProcessingSpecialReferences(Interval interval) {
            log(Operation.ProcessingSpecialReferences.ordinal(), intervalArg(interval));
        }
        protected abstract void traceProcessingSpecialReferences(Interval interval);

        @INLINE
        public final void logScanningBootHeap(Interval interval) {
            log(Operation.ScanningBootHeap.ordinal(), intervalArg(interval));
        }
        protected abstract void traceScanningBootHeap(Interval interval);

        @INLINE
        public final void logScanningCode(Interval interval) {
            log(Operation.ScanningCode.ordinal(), intervalArg(interval));
        }
        protected abstract void traceScanningCode(Interval interval);

        @INLINE
        public final void logScanningRSet(Interval interval) {
            log(Operation.ScanningRSet.ordinal(), intervalArg(interval));
        }
        protected abstract void traceScanningRSet(Interval interval);

        @INLINE
        public final void logScanningRoots(Interval interval) {
            log(Operation.ScanningRoots.ordinal(), intervalArg(interval));
        }
        protected abstract void traceScanningRoots(Interval interval);

        @Override
        @INLINE
        public final void logScanningThreadRoots(VmThread vmThread) {
            log(Operation.ScanningThreadRoots.ordinal(), vmThreadArg(vmThread));
        }
        protected abstract void traceScanningThreadRoots(VmThread vmThread);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //Evacuating
                    traceEvacuating(toInterval(r, 1));
                    break;
                }
                case 1: { //ProcessingSpecialReferences
                    traceProcessingSpecialReferences(toInterval(r, 1));
                    break;
                }
                case 2: { //ScanningBootHeap
                    traceScanningBootHeap(toInterval(r, 1));
                    break;
                }
                case 3: { //ScanningCode
                    traceScanningCode(toInterval(r, 1));
                    break;
                }
                case 4: { //ScanningRSet
                    traceScanningRSet(toInterval(r, 1));
                    break;
                }
                case 5: { //ScanningRoots
                    traceScanningRoots(toInterval(r, 1));
                    break;
                }
                case 6: { //ScanningThreadRoots
                    traceScanningThreadRoots(toVmThread(r, 1));
                    break;
                }
            }
        }
    }

// END GENERATED CODE

}
