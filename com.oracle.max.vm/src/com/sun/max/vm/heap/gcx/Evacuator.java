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
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Base class for evacuating objects of an evacuated area made of possibly discontinuous range of addresses.
 * The class provides common services. Sub-classes define what set of contiguous ranges delimit the evacuated area,
 *  what set of contiguous ranges hold the evacuated objects whose references haven't been processed yet, and
 * the remembered set holdings references to the evacuated area.
 *
 */
public abstract class Evacuator extends PointerIndexVisitor implements CellVisitor, OverlappingCellVisitor {
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
    abstract Pointer evacuate(Pointer origin);

    /**
     * Remembered set updates to apply to reference a to an evacuated cells.
     * Default is to do nothing.
     *
     * @param refHolderOrigin origin of the reference holder
     * @param wordIndex
     * @param ref reference to an evacuated cell
     */
    void updateRSet(Pointer refHolderOrigin, int wordIndex, Reference ref) {
        // default is doing nothing.
    }

    /**
     * Update a potential reference to an evacuated cell.
     * If the reference points to the evacuation area, the cell is evacuated and the reference is updated to the evacuated cell's new location.
     * @param refHolderOrigin origin of the holder of the referencel
     * @param wordIndex index to a reference of the evacuated cell.
     */
    final void updateEvacuatedRef(Pointer refHolderOrigin, int wordIndex) {
        final Reference ref = refHolderOrigin.getReference(wordIndex);
        Pointer origin = ref.toOrigin();
        if (inEvacuatedArea(origin)) {
            final Reference forwardRef = Layout.readForwardRef(origin);
            if (!forwardRef.isZero()) {
                refHolderOrigin.setReference(wordIndex, forwardRef);
                updateRSet(refHolderOrigin, wordIndex, forwardRef);
                return;
            }
            final Pointer toOrigin = evacuate(origin);
            final Reference toRef = Reference.fromOrigin(toOrigin);
            HeapScheme.Inspect.notifyObjectRelocated(Layout.originToCell(origin), Layout.originToCell(toOrigin));
            Layout.writeForwardRef(origin, toRef);
            refHolderOrigin.setReference(wordIndex, toRef);
        }
    }

    @Override
    public void visit(Pointer pointer, int wordIndex) {
        updateEvacuatedRef(pointer, wordIndex);
    }

    private final SequentialHeapRootsScanner heapRootsScanner = new SequentialHeapRootsScanner(this);
    private final int HUB_WORD_INDEX = Layout.generalLayout().getOffsetFromOrigin(HeaderField.HUB).toInt() >> Word.widthValue().log2numberOfBytes;
    private final int FIRST_ELEMENT_OFFSET = Layout.referenceArrayLayout().getElementOffsetInCell(0).toInt();

    protected Hub getHub(Pointer origin) {
        return UnsafeCast.asHub(origin.getReference(HUB_WORD_INDEX));
    }


    private void updateReferenceArray(Pointer refArrayOrigin, final int firstIndex, final int length) {
        for (int index = firstIndex; index < length; index++) {
            updateEvacuatedRef(refArrayOrigin, index);
        }
    }

    private void updateReferenceArray(Pointer refArrayOrigin) {
        final int length = Layout.readArrayLength(refArrayOrigin);
        updateReferenceArray(refArrayOrigin, 0, length);
    }


    private void updateReferenceArray(Pointer refArrayOrigin, Address start, Address end) {
        final int length = Layout.readArrayLength(refArrayOrigin);
        final Address firstElementAddr = refArrayOrigin.plus(FIRST_ELEMENT_OFFSET);
        final Address endOfArrayAddr = firstElementAddr.plusWords(length);
        final int firstIndex = firstElementAddr.lessEqual(start) ? start.minus(firstElementAddr).unsignedShiftedRight(Kind.REFERENCE.width.log2numberOfBytes).toInt() : 0;
        final int endIndex = endOfArrayAddr.greaterThan(end) ? end.minus(firstElementAddr).unsignedShiftedRight(Kind.REFERENCE.width.log2numberOfBytes).toInt() : length;
        updateReferenceArray(refArrayOrigin, firstIndex, endIndex);
    }

    /**
     * Evacuate all objects of the evacuated area directly reachable from roots (thread stacks, monitors, etc.).
     */
    void evacuateFromRoots() {
        heapRootsScanner.run();
    }
    /**
     * Evacuate all objects of the evacuated area directly reachable from the remembered sets of the evacuated area. By default, this does nothing
     * (i.e., there are no remembered sets). For instance, a pure semi-space flat heap doesn't have any remembered sets to evacuate from.
     */
    protected void evacuateFromRSets() {
    }

    /**
     * Evacuate all objects of the evacuated area reachable from already evacuated cells.
     */
    abstract protected void evacuateReachables();

    abstract protected void doBeforeEvacuation();
    abstract protected void doAfterEvacuation();

    /**
     * Evacuate all objects of the evacuated area directly reachable from the boot heap.
     */
    void evacuateFromBootHeap() {
        Heap.bootHeapRegion.visitReferences(this);
    }

    /**
     * Scan a cell to evacuate the cells in the evacuation area it refers to and update its references to already evacuated cells.
     *
     * @param cell a pointer to a cell
     * @return pointer to the end of the cell
     */
    final public Pointer visitCell(Pointer cell) {
        final Pointer origin = Layout.cellToOrigin(cell);
        // Update the hub first so that is can be dereferenced to obtain
        // the reference map needed to find the other references in the object
        updateEvacuatedRef(origin,  HUB_WORD_INDEX);
        final Hub hub = UnsafeCast.asHub(origin.getReference(HUB_WORD_INDEX));
        // Update the other references in the object
        final SpecificLayout specificLayout = hub.specificLayout;
        if (specificLayout.isTupleLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, this);
            if (hub.isJLRReference) {
                SpecialReferenceManager.discoverSpecialReference(origin);
            }
            return cell.plus(hub.tupleSize);
        }
        if (specificLayout.isHybridLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, this);
        } else if (specificLayout.isReferenceArrayLayout()) {
            updateReferenceArray(origin);
        }
        return cell.plus(Layout.size(origin));
    }

    private boolean cellInRegion(Pointer cell, Pointer endOfCell, Address start, Address end) {
        return cell.greaterEqual(start) && endOfCell.lessEqual(end);
    }

    private void checkCellOverlap(Pointer cell, Address start, Address end) {
        if (MaxineVM.isDebug()) {
            final Pointer endOfCell = cell.plus(Layout.size(Layout.cellToOrigin(cell)));
            if ((cell.lessThan(end) && endOfCell.greaterThan(start)) || cellInRegion(cell, endOfCell, start, end)) {
                return;
            }
            FatalError.check(false, "Cell doesn't overlap range");
        }
    }

    /**
     * Scan the part of cell that overlap with a region of memory to evacuate the cells in the evacuation area it refers to and update its references to already evacuated cells.
     *
     * @param cell Pointer to the first word of the cell to be visited
     * @param start start of the region overlapping with the cell
     * @param end end of the region overlapping with the cell
     * @return pointer to the end of the cell
     */
    final public Pointer visitCell(Pointer cell, Address start, Address end) {
        checkCellOverlap(cell, start, end);
        final Pointer origin = Layout.cellToOrigin(cell);
        Pointer hubReferencePtr = origin.plus(HUB_WORD_INDEX);
        if (hubReferencePtr.greaterEqual(start)) {
            updateEvacuatedRef(origin,  HUB_WORD_INDEX);
        }
        final Hub hub = UnsafeCast.asHub(origin.getReference(HUB_WORD_INDEX));
        // Update the other references in the object
        final SpecificLayout specificLayout = hub.specificLayout;
        if (specificLayout.isTupleLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, this, start, end);
            if (hub.isJLRReference) {
                SpecialReferenceManager.discoverSpecialReference(origin);
            }
            return cell.plus(hub.tupleSize);
        }
        if (specificLayout.isHybridLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, this, start, end);
        } else if (specificLayout.isReferenceArrayLayout()) {
            updateReferenceArray(origin, start, end);
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

    public void evacuate() {
        doBeforeEvacuation();
        evacuateFromRoots();
        evacuateFromBootHeap();
        evacuateFromRSets();
        evacuateReachables();
        doAfterEvacuation();
    }

}
