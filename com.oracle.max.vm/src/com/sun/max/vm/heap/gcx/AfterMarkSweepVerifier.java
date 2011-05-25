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
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * After mark-sweep verifier for a free space manager with tracing based on TricolorHeapMarker.
 */
public class AfterMarkSweepVerifier extends PointerIndexVisitor implements CellVisitor {
    final TricolorHeapMarker heapMarker;
    final MarkSweepVerification msVerification;
    long darkMatterByteCount;
    long freeChunksByteCount;
    long liveDataByteCount;
    Pointer visitedCellOrigin;

    public AfterMarkSweepVerifier(TricolorHeapMarker heapMarker, MarkSweepVerification msVerification) {
        this.heapMarker = heapMarker;
        this.msVerification = msVerification;
    }

    @INLINE
    private void visit(Pointer origin) {
        if (!origin.isZero()) {
            final boolean inDeadSpace = HeapFreeChunk.isInDeadSpace(origin);
            if (inDeadSpace) {
                Log.print("\n\nCell @");
                Log.print(visitedCellOrigin);
                Log.print("[ bit index = ");
                Log.print(heapMarker.bitIndexOf(visitedCellOrigin));
                Log.print(" at bitmap word # ");
                Log.print(heapMarker.bitmapWordIndex(visitedCellOrigin));
                Log.print("] pointing to dead space @");
                Log.println(origin);

                FatalError.check(!HeapFreeChunk.isInDeadSpace(origin), "must not points to dead space");
            }
            // Check that the reference points to a valid object, and that if it is in the covered area, it is marked black.
            Reference hubRef = Layout.readHubReference(origin);
            FatalError.check(!hubRef.isZero() && hubRef.toJava() instanceof Hub, "Invalid reference detected");
            if (heapMarker.isCovered(origin)) {
                FatalError.check(heapMarker.isBlackWhenNoGreys(origin), "pointer to live object in covered area must be black after GC");
            }
        }
    }

    @Override
    public void visit(Pointer pointer, int wordIndex) {
        visit(pointer.getReference(wordIndex).toOrigin());
    }

    public Pointer visitCell(Pointer cell) {
        final Pointer origin = Layout.cellToOrigin(cell);
        visitedCellOrigin = origin;
        final Reference hubRef = Layout.readHubReference(origin);
        final Hub hub = UnsafeCast.asHub(hubRef.toJava());
        if (hub == HeapFreeChunk.HEAP_FREE_CHUNK_HUB) {
            Size chunkSize = HeapFreeChunk.getFreechunkSize(cell);
            freeChunksByteCount += chunkSize.toLong();
            return cell.plus(chunkSize);
        }
        Size size = Layout.size(origin);
        if (heapMarker.isWhite(cell)) {
            darkMatterByteCount += size.toLong();
        } else {
            FatalError.check(heapMarker.isBlackWhenNoGreys(cell), "cell must be marked live");
            final Pointer hubOrigin = hubRef.toOrigin();
            if (heapMarker.isCovered(hubOrigin)) {
                FatalError.check(heapMarker.isBlackWhenNoGreys(hubOrigin), "hub must be marked live");
            }
            final SpecificLayout specificLayout = hub.specificLayout;
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, this);
            } else if (specificLayout.isHybridLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, this);
            } else if (specificLayout.isReferenceArrayLayout()) {
                final int length = Layout.readArrayLength(origin);
                for (int index = 0; index < length; index++) {
                    visit(Layout.getReference(origin, index).toOrigin());
                }
            }
            liveDataByteCount += size.toLong();
        }
        return cell.plus(size);
    }

    public void run() {
        darkMatterByteCount = 0L;
        freeChunksByteCount = 0L;
        liveDataByteCount = 0L;
        msVerification.verify(this);
    }
}
