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
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * Verify that boot heap references into a heap are considered root (i.e., that the heap object their refer too are marked black).
 */
public class AfterMarkSweepBootHeapVerifier<T extends AfterMarkSweepBootHeapVerifier.InHeapChecker> extends PointerIndexVisitor implements CellVisitor {
    public static class InHeapChecker {
        boolean isNonNullCovered(Pointer cell) {
            return true;
        }
    }
    private static final InHeapChecker alwaysTrueInHeapCheck = new InHeapChecker();

    final TricolorHeapMarker heapMarker;

    final T inHeapChecker;
    /**
     * Bottom-most address in the covered area for the heap account being verified.
     * Act as a fast first level filter for null-pointer and irrelevant references (e.g., intra-boot heap region references).
     */
    private Address bottom;
    /**
     * Count of the number of locations in the boot heap region that hold a reference to the verified heap account.
     */
    private int rootCount;

    private Pointer pointerUnderCheck;

    public AfterMarkSweepBootHeapVerifier(TricolorHeapMarker heapMarker, T inHeapChecker) {
        this.heapMarker = heapMarker;
        this.inHeapChecker = inHeapChecker;
    }


    final void checkExternalRoot(Pointer cell) {
        // Note: the first test also acts as a null pointer filter.
        pointerUnderCheck = cell;
        if (cell.greaterEqual(bottom) && inHeapChecker.isNonNullCovered(cell)) {
            FatalError.check(heapMarker.isBlackWhenNoGreys(cell), "References from boot heap must point to black cell");
            final Pointer origin = Layout.cellToOrigin(cell);
            final Pointer hub = Layout.readHubReference(origin).toOrigin();
            FatalError.check(!hub.isZero() && !hub.equals(HeapFreeChunk.deadSpaceMark()),  "Invalid address value for a hub");
            rootCount++;
        }
    }

    private Pointer visitedRootLocation;

    @Override
    final public void visit(Pointer pointer, int wordIndex) {
        if (MaxineVM.isDebug())  {
            visitedRootLocation = pointer.plusWords(wordIndex);
        }
        checkExternalRoot(Layout.originToCell(pointer.getReference(wordIndex).toOrigin()));
    }

    /**
     * Visits a cell in the boot region. No need to mark the cell, it is outside of the covered area.
     *
     * @param cell a cell in 'boot region'
     */
    @Override
    public Pointer visitCell(Pointer cell) {
        // FIXME: can we have a hub for a cell in the boot heap that is not itself in the boot heap !!!
        // I don't think we can, so it should be safe to remove the marking of the hub.
        final Pointer origin = Layout.cellToOrigin(cell);
        final Reference hubRef = Layout.readHubReference(origin);
        checkExternalRoot(Layout.originToCell(hubRef.toOrigin()));
        final Hub hub = UnsafeCast.asHub(hubRef.toJava());

        // Update the other references in the object
        final SpecificLayout specificLayout = hub.specificLayout;
        if (specificLayout.isTupleLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, this);
            return cell.plus(hub.tupleSize);
        }
        if (specificLayout.isHybridLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, this);
        } else if (specificLayout.isReferenceArrayLayout()) {
            final int length = Layout.readArrayLength(origin);
            for (int index = 0; index < length; index++) {
                checkExternalRoot(Layout.originToCell(Layout.getReference(origin, index).toOrigin()));
            }
        }
        return cell.plus(Layout.size(origin));
    }

    public void run() {
        bottom = heapMarker.coveredAreaStart;
        rootCount = 0;
        Heap.bootHeapRegion.visitReferences(this);
    }

    static class HeapAccountChecker extends InHeapChecker {
        final HeapAccountOwner owner;
        HeapAccountChecker(HeapAccountOwner owner) {
            this.owner = owner;
        }
        @INLINE
        @Override
        public final boolean isNonNullCovered(Pointer cell) {
            return HeapRegionInfo.fromInRegionAddress(cell).owner == owner;
        }
    }


    public static AfterMarkSweepBootHeapVerifier makeVerifier(TricolorHeapMarker heapMarker) {
        return new AfterMarkSweepBootHeapVerifier<InHeapChecker>(heapMarker, alwaysTrueInHeapCheck);
    }

    public static AfterMarkSweepBootHeapVerifier makeVerifier(TricolorHeapMarker heapMarker, final HeapAccountOwner heapAccountOwner) {
        return new AfterMarkSweepBootHeapVerifier<HeapAccountChecker>(heapMarker, new HeapAccountChecker(heapAccountOwner));
    }
}
