/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.heap.gcx.TricolorHeapMarker.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * Recovery using a rescan map.
 * On marking stack overflow, flush the marking stack, updating the rescan map doing so.
 * Then iterate over the rescan map to find area of the mark bitmap to iterate over.
 * An overflow rescan finger is maintained. References before the rescan finger
 * are pushed on them marking stack.
 * References beyond the rescan finger but before the forward finger requires updating the rescan map,
 * otherwise grey objects might be missed.
 */
final class OverflowScanWithRescanMapState extends OverflowScanState {
    final RescanMap rescanMap;
    OverflowScanWithRescanMapState(TricolorHeapMarker heapMarker) {
        super(heapMarker);
        rescanMap = new RescanMap();
        setMarkingStackFlusher(new MarkingStackWithRescanMapCellVisitor(rescanMap));
    }

    @Override
    void initialize() {
        rescanMap.initialize(heapMarker);
        super.initialize();
    }

    @Override
    void recoverFromOverflow() {
        markingStackFlusher.flushMarkingStack();
        if (!isRecovering()) {
            if (MaxineVM.isDebug()) {
                verifyHasNoGreyMarks(rescanMap.beginOfGreyArea());
                FatalError.check(!rescanMap.isEmpty(), "rescan map must not be empty after a mark stack overflow");
            }
            beginRecovery();
            visitGreyObjects();
            endRecovery();
        } else {
            // rescan map already updated.
            verifyHasNoGreyMarks(rescanMap.beginOfGreyArea());
        }
    }

    /**
     * Mark the object at the specified address grey.
     *
     * @param cell
     */
    @INLINE
    private void markObjectGrey(Pointer cell) {
        if (cell.greaterThan(finger)) {
            // Object is after the finger. Mark grey and update rightmost if white.
            if (heapMarker.markGreyIfWhite(cell)) {
                if (cell.greaterThan(rightmost)) {
                    rightmost = cell;
                } else if (cell.lessThan(endOfScan)) {
                    rescanMap.recordCellForRescan(cell);
                }
            }
        } else if (cell.greaterEqual(heapMarker.coveredAreaStart) && heapMarker.markGreyIfWhite(cell)) {
            heapMarker.markingStack.push(cell);
        }
    }

    @INLINE
    private void markRefGrey(Reference ref) {
        markObjectGrey(Layout.originToCell(ref.toOrigin()));
    }

    @Override
    public void visit(Pointer pointer, int wordIndex) {
        markRefGrey(pointer.getReference(wordIndex));
    }

    @Override
    public void visitArrayReferences(Pointer origin) {
        final int length = Layout.readArrayLength(origin);
        for (int index = 0; index < length; index++) {
            markRefGrey(Layout.getReference(origin, index));
        }
    }

    @Override
    public void visit(Reference ref) {
        markRefGrey(ref);
    }


    /**
     * Visit grey cells using the specified rescan map.
     */
    @Override
    public void visitGreyObjects() {
        // Iterate over the rescan map and iterate over the corresponding bounds in the mark bitmap for each
        // entry recording the presence of grey objects.
        while (!rescanMap.isEmpty()) {
            rescanMap.cacheLeftmostEntryBound();
            int bitmapWordIndex = rescanMap.leftmostLeftBound();
            int rightmostBitmapWordIndex = rescanMap.leftmostRightBound();
            visitGreyObjectsTillEndOfScan(bitmapWordIndex, rightmostBitmapWordIndex);
            rescanMap.fetchNextEntry();
        }
        // The loop may have ended with a finger before the end of scan position as the finger is updated only when
        // visiting grey cell. Before draining the marking stack, we set it to the end of scan position so draining
        // operates with the marking stack only.
        finger = endOfScan;
        // There might be some objects left in the marking stack. Drain it.
        heapMarker.markingStack.drain();
    }
}
