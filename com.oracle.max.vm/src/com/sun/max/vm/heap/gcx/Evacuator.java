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
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.reference.*;

/**
 * Base class for evacuating objects of an evacuated area made of possibly discontinuous range of addresses.
 * The class provides common services. Sub-classes define what set of contiguous ranges delimit the evacuated area,
 *  what set of contiguous ranges hold the evacuated objects whose references haven't been processed yet, and
 * the remembered set holdings references to the evacuated area.
 *
 */
public abstract class Evacuator extends PointerIndexVisitor implements CellVisitor {
    abstract boolean inEvacuatedArea(Pointer origin);
    abstract Pointer evacuate(Pointer origin);

    abstract void writeBarrier(Pointer pointer, int wordIndex, Reference ref);

    final void scavengeReference(Pointer pointer, int wordIndex) {
        final Reference ref = pointer.getReference(wordIndex);
        Pointer origin = ref.toOrigin();
        if (inEvacuatedArea(origin)) {
            final Reference forwardRef = Layout.readForwardRef(origin);
            if (!forwardRef.isZero()) {
                pointer.setReference(wordIndex, forwardRef);
                writeBarrier(pointer, wordIndex, forwardRef);
                return;
            }
            final Pointer toOrigin = evacuate(origin);
            final Reference toRef = Reference.fromOrigin(toOrigin);
            Layout.writeForwardRef(origin, toRef);
            pointer.setReference(wordIndex, toRef);
        }
    }

    @Override
    public void visit(Pointer pointer, int wordIndex) {
        scavengeReference(pointer, wordIndex);
    }

    private final SequentialHeapRootsScanner heapRootsScanner = new SequentialHeapRootsScanner(this);
    private final int HUB_WORD_INDEX = Layout.generalLayout().getOffsetFromOrigin(HeaderField.HUB).toInt() >> Word.widthValue().log2numberOfBytes;

    private void scanReferenceArray(Pointer origin) {
        final int length = Layout.readArrayLength(origin);
        for (int index = 0; index < length; index++) {
            scavengeReference(origin, index);
        }
    }

    /**
     * Evacuate all objects of the evacuated area directly reachable from roots (thread stacks, monitors, etc.).
     */
    void evacuateFromRoots() {

    }
    /**
     * Evacuate all objects of the evacuated area directly reachable from the remembered sets of the evacuated area. By default, this does nothing
     * (i.e., there are no remembered sets). For instance, a pure semi-space flat heap doesn't have any remembered sets to evacuate from.
     */
    void evacuateFromRSets() {
    }

    abstract class EvacuationRangeIterable {
        abstract void doNextRange(Evacuator evacuator);
    }

    /**
     * Evacuate all objects of the evacuated area reachable from already evacuated objects.
     */
    void evacuateReachable() {

    }

    /**
     * Evacuate all objects of the evacuated area directly reachable from the boot heap.
     */
    void evacuateFromBootHeap() {
        Heap.bootHeapRegion.visitReferences(this);
    }

    /**
     * Visit an evacuated object.
     *
     * @param cell
     * @return
     */
    final public Pointer visitCell(Pointer cell) {
        final Pointer origin = Layout.cellToOrigin(cell);
        // Update the hub first so that is can be dereferenced to obtain
        // the reference map needed to find the other references in the object
        scavengeReference(origin,  HUB_WORD_INDEX);
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
            scanReferenceArray(origin);
        }
        return cell.plus(Layout.size(origin));
    }

    /**
     * Evacuate all objects of the evacuated area directly reachable from a range of heap addresses that contains previously evacuated objects.
     *
     * @param start first address of the range
     * @param end last address of the range
     */
    final void evacuateRange(Pointer start, Pointer end) {
        Pointer cell = start;
        while (cell.lessThan(end)) {
            cell = visitCell(cell);
        }
    }

    void evacuate() {
        evacuateFromRoots();
        evacuateFromBootHeap();
        evacuateFromRSets();
        evacuateReachable();
    }

}
