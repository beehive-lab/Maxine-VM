/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.sequential.semiSpace;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.debug.*;
import com.sun.max.vm.heap.SpecialReferenceManager.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * Extension of {@link DebugHeap} that is specific to {@link SemiSpaceHeapScheme}.
 */
public final class SemiSpaceDebugHeap extends DebugHeap {

    private SemiSpaceDebugHeap() {
    }

    /**
     * Verifies that a given memory region consists of a contiguous objects is well formed.
     * The memory region is well formed if:
     *
     * a. It starts with an object preceded by a debug tag word.
     * b. Each object in the region is immediately succeeded by another object with the preceding debug tag.
     * c. Each reference embedded in an object points to an address in the given memory region, the boot
     *    {@linkplain Heap#bootHeapRegion heap} or {@linkplain Code#bootCodeRegion code} region.
     *
     * This method can only be called in a {@linkplain MaxineVM#isDebug() debug} VM.
     *
     * @param region the region being verified
     * @param start the start of the memory region to verify
     * @param end the end of memory region
     * @param space the address space in which valid objects can be found apart from the boot
     *            {@linkplain Heap#bootHeapRegion heap} and {@linkplain Code#bootCodeRegion code} regions.
     * @param verifier a {@link PointerIndexVisitor} instance that will call
     *            {@link #verifyRefAtIndex(Address, int, Reference, MemoryRegion, MemoryRegion)} for a reference value denoted by a base
     *            pointer and offset
     */
    static void verifyRegion(MemoryRegion region, Address start, final Address end, final MemoryRegion space, PointerIndexVisitor verifier) {

        if (Heap.logGCPhases()) {
            SemiSpaceHeapScheme.phaseLogger.logVerifyingRegion(region, start, end);
        }

        Pointer cell = start.asPointer();
        while (cell.lessThan(end)) {
            cell = skipCellPadding(cell);
            if (cell.greaterEqual(end)) {
                break;
            }
            cell = checkDebugCellTag(start, cell);

            final Pointer origin = Layout.cellToOrigin(cell);
            final Hub hub = checkHub(origin, space);

            if (hub.isJLRReference) {
                JLRRAlias refAlias = SpecialReferenceManager.asJLRRAlias(Reference.fromOrigin(origin).toJava());
                if (refAlias.discovered != null) {
                    Log.print("Special reference of type ");
                    Log.print(hub.classActor.name.string);
                    Log.print(" at ");
                    Log.print(cell);
                    Log.print(" has non-null value for 'discovered' field: ");
                    Log.println(Reference.fromJava(refAlias.discovered).toOrigin());
                    FatalError.unexpected("invalid special ref");
                }
            }

            if (Heap.logAllGC()) {
                SemiSpaceHeapScheme.detailLogger.logVerifyObject(hub.classActor, cell, Layout.size(origin).toInt());
            }

            final SpecificLayout specificLayout = hub.specificLayout;
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, verifier);
                cell = cell.plus(hub.tupleSize);
            } else {
                if (specificLayout.isHybridLayout()) {
                    TupleReferenceMap.visitReferences(hub, origin, verifier);
                } else if (specificLayout.isReferenceArrayLayout()) {
                    final int length = Layout.readArrayLength(origin);
                    for (int index = 0; index < length; index++) {
                        verifyRefAtIndex(origin, index, Layout.getReference(origin, index), space, null);
                    }
                }
                cell = cell.plus(Layout.size(origin));
            }
        }
    }

    public static Pointer skipCellPadding(Pointer cell) {
        if (MaxineVM.isDebug()) {
            Pointer cellStart = cell;
            while (cell.getWord().equals(DebugHeap.padWord())) {
                cell = cell.plusWords(1);
            }
            if (!cell.equals(cellStart)) {
                if (SemiSpaceHeapScheme.detailLogger.enabled()) {
                    SemiSpaceHeapScheme.detailLogger.logSkipped(cell.minus(cellStart).toInt());
                }
            }
        }
        return cell;
    }



}
