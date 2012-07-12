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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.rset.ctbl.*;
import com.sun.max.vm.runtime.*;


public class NoAgingNurseryEvacuator extends EvacuatorToCardSpace {
    public static boolean TraceDirtyCardWalk = false;
    private static boolean traceDirtyCardWalk = false;
    static {
        VMOptions.addFieldOption("-XX:", "TraceDirtyCardWalk", NoAgingNurseryEvacuator.class, "Trace Dirty Card Walk", Phase.PRISTINE);
    }

    @INLINE
    private static boolean traceDirtyCardWalk() {
        return MaxineVM.isDebug() && traceDirtyCardWalk;
    }

    /**
     * Closure for evacuating cells in dirty card. A dirty card may overlap with an area currently used for allocation.
     * Allocation is made via the evacuator's promotion lab, which may during iteration over dirty cards.
     * To avoid maintaining parsability of the promotion lab at every allocation,
     * we check if the visited cell boundary coincide with the first free bytes of the allocator, and skip it if it does.
     *
     * Note that the allocator that feed the promotion lab is kept in an iterable state.
     */
    final class DirtyCardEvacuationClosure implements CellVisitor, OverlappingCellVisitor,  CellRangeVisitor {
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

    final class BootRegionDirtyCardEvacuationClosure extends CardTableRSet.CardRangeVisitor {
        BootRegionDirtyCardEvacuationClosure() {
        }

        @INLINE
        @Override
        public void visitCards(Address start, Address end) {
            Heap.bootHeapRegion.visitReferences(start, end, NoAgingNurseryEvacuator.this);
        }
    }

    private final DirtyCardEvacuationClosure heapSpaceDirtyCardClosure;
    private final BootRegionDirtyCardEvacuationClosure bootRegionDirtyCardClosure;

    public NoAgingNurseryEvacuator(EvacuatingSpace fromSpace, HeapSpace toSpace, EvacuationBufferProvider evacuationBufferProvider, CardTableRSet rset) {
        super(fromSpace, toSpace, evacuationBufferProvider, rset);
        this.heapSpaceDirtyCardClosure = new DirtyCardEvacuationClosure();
        this.bootRegionDirtyCardClosure = new BootRegionDirtyCardEvacuationClosure();
    }

    @Override
    public void setGCOperation(GCOperation gcOperation) {
        super.setGCOperation(gcOperation);
        if (MaxineVM.isDebug() && gcOperation != null) {
            traceDirtyCardWalk = TraceDirtyCardWalk && TraceFromGCInvocation <= gcOperation.invocationCount();
        }
    }
    @Override
    protected void evacuateFromBootHeap() {
        final BootHeapRegion bootHeapRegion = Heap.bootHeapRegion;
        rset.cleanAndVisitCards(bootHeapRegion.start(), bootHeapRegion.end(), bootRegionDirtyCardClosure);
        bootHeapRegion.discoverSpecialReference();
    }

    @Override
    protected void evacuateFromRSets() {
        // Visit the dirty cards of the old gen (i.e., the toSpace).
        final boolean traceRSet = CardTableRSet.traceCardTableRSet();
        if (traceDirtyCardWalk()) {
            CardTableRSet.setTraceCardTableRSet(true);
        }
        toSpace.visit(heapSpaceDirtyCardClosure);
        if (traceDirtyCardWalk()) {
            CardTableRSet.setTraceCardTableRSet(traceRSet);
        }
    }

}

