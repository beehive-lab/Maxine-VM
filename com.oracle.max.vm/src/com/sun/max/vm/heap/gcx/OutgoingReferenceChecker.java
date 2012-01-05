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
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.reference.*;

/**
 * A simple tool that counts (and print to the log) references that escapes a particular heap account.
 * References to the boot image aren't counted as escaping.
 * The tools is currently used to verify that no references escape the {@link HeapRegionManager}'s heap account.
 */
class OutgoingReferenceChecker extends PointerIndexVisitor implements CellVisitor {
    private final Object accountOwner;
    private final RegionTable regionTable;
    private final int hubIndex;
    private final int refArrayFirstIndex;
    private long outgoingReferenceCount = 0L;

    /**
     * @param heapAccount
     */
    OutgoingReferenceChecker(HeapAccount<?> heapAccount) {
        accountOwner = heapAccount.owner();
        regionTable = RegionTable.theRegionTable();
        hubIndex = Layout.generalLayout().getOffsetFromOrigin(HeaderField.HUB).toInt() >> Word.widthValue().log2numberOfBytes;
        refArrayFirstIndex = Layout.referenceArrayLayout().getElementOffsetFromOrigin(0).toInt() >> Word.widthValue().log2numberOfBytes;
    }

    void reset() {
        outgoingReferenceCount = 0L;
    }

    private void checkReference(Pointer cell) {
        if (!cell.isZero()) {
            final HeapRegionInfo rinfo = regionTable.regionInfo(cell);
            if (rinfo.owner() != accountOwner && !Heap.bootHeapRegion.contains(cell)) {
                Log.print("outgoing ref: ");
                Log.println(cell);
                outgoingReferenceCount++;
            }
        }
    }
    private void checkReference(Pointer refHolder, int wordIndex) {
        Pointer cell = refHolder.getReference(wordIndex).toOrigin();
        if (!cell.isZero()) {
            final HeapRegionInfo rinfo = regionTable.regionInfo(cell);
            if (rinfo.owner() != accountOwner && !Heap.bootHeapRegion.contains(cell)) {
                Log.print("outgoing ref: ");
                Log.print(refHolder);
                Log.print(" [ ");
                Log.print(wordIndex);
                Log.print(" ] = ");
                Log.println(cell);
                outgoingReferenceCount++;
            }
        }
    }


    @Override
    public Pointer visitCell(Pointer cell) {
        final Pointer origin = Layout.cellToOrigin(cell);
        final Reference hubRef = Layout.readHubReference(origin);
        final Hub hub = UnsafeCast.asHub(hubRef.toJava());
        if (hub == HeapFreeChunk.HEAP_FREE_CHUNK_HUB) {
            Size chunkSize = HeapFreeChunk.getFreechunkSize(cell);
            return cell.plus(chunkSize);
        }
        checkReference(origin, hubIndex);
        final SpecificLayout specificLayout = hub.specificLayout;
        if (specificLayout.isTupleLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, this);
            return cell.plus(hub.tupleSize);
        } else if (specificLayout.isHybridLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, this);
        } else if (specificLayout.isReferenceArrayLayout()) {
            final int length = refArrayFirstIndex + Layout.readArrayLength(origin);
            for (int index = refArrayFirstIndex; index < length; index++) {
                checkReference(origin, index);
            }
        }
        Size size = Layout.size(origin);
        return cell.plus(size);
    }

    @Override
    public void visit(Pointer pointer, int wordIndex) {
        checkReference(pointer, wordIndex);
    }

    public long outgoingReferenceCount() {
        return outgoingReferenceCount;
    }
}
