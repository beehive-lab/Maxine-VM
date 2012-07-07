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

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;

/**
 * Marking of strong roots outside of the area covered by a heap marker.
 *
 * Implements cell visitor and pointer index visitor.
 * Cell visitor must be used only for region outside of the heap areas covered by the heap marker.
 * This currently includes the boot region, the code region and the immortal heap.
 * Pointer visitor should be used only for references from outside of the covered area,
 * i.e., reference from the boot region and external roots (thread stack, live monitors).
 *
 * We haven't started visiting the mark bitmap, so we don't have any black marks.
 * Thus, we don't bother with first testing if a reference is white to mark it grey (it cannot be),
 * or to test it against the finger to decide whether to mark it grey or push it on the marking stack.
 * We can just blindingly mark grey any references to the covered area,
 * and update the leftmost and rightmost marked positions.
 */
abstract class RootCellVisitor extends PointerIndexVisitor implements CellVisitor {

    protected TricolorHeapMarker heapMarker;
    /**
     * Leftmost marked position in the area covered by the heap marker.
     */
    protected Address leftmost;
    /**
     * Rightmost marked position in the area covered by the heap marker.
     */
    protected Address rightmost;

    protected Address bottom;

    RootCellVisitor() {
    }

    void initialize(TricolorHeapMarker heapMarker) {
        this.heapMarker = heapMarker;
    }

    abstract boolean isNonNullCovered(Pointer cell);

    void reset() {
        leftmost = heapMarker.coveredAreaEnd;
        rightmost = heapMarker.coveredAreaStart;
    }

    final void markExternalRoot(Pointer cell) {
        // Note: the first test also acts as a null pointer filter.
        if (cell.greaterEqual(bottom) && isNonNullCovered(cell)) {
            heapMarker.markGrey(cell);
            if (cell.lessThan(leftmost)) {
                leftmost = cell;
            } else if (cell.greaterThan(rightmost)) {
                rightmost = cell;
            }
        }
    }

    @Override
    final public void visit(Pointer pointer, int wordIndex) {
        markExternalRoot(Layout.originToCell(pointer.getReference(wordIndex).toOrigin()));
    }

    /**
     * Visits a cell in the boot region. No need to mark the cell, it is outside of the covered area.
     *
     * @param cell a cell in 'boot region'
     */
    @Override
    public Pointer visitCell(Pointer cell) {
        if (MaxineVM.isDebug() && Heap.logRootScanning()) {
            TricolorHeapMarker.printVisitedCell(cell, "Visiting root cell ");
        }
        // FIXME: can we have a hub for a cell in the boot heap that is not itself in the boot heap !!!
        // I don't think we can, so it should be safe to remove the marking of the hub.
        final Pointer origin = Layout.cellToOrigin(cell);
        final Reference hubRef = Layout.readHubReference(origin);
        markExternalRoot(Layout.originToCell(hubRef.toOrigin()));
        final Hub hub = UnsafeCast.asHub(hubRef.toJava());

        // Update the other references in the object
        final SpecificLayout specificLayout = hub.specificLayout;
        if (specificLayout == Layout.tupleLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, this);
            if (hub.isJLRReference) {
                SpecialReferenceManager.discoverSpecialReference(cell);
            }
            return cell.plus(hub.tupleSize);
        }
        if (specificLayout == Layout.hybridLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, this);
        } else if (specificLayout == Layout.referenceArrayLayout()) {
            final int length = Layout.readArrayLength(origin);
            for (int index = 0; index < length; index++) {
                markExternalRoot(Layout.originToCell(Layout.getReference(origin, index).toOrigin()));
            }
        }
        return cell.plus(Layout.size(origin));
    }
}
