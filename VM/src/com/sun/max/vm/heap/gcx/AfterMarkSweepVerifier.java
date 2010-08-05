/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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
 * FIXME: want to make this independent from FreeHeapSpaceManager.
 * @author Laurent Daynes
 */
public class AfterMarkSweepVerifier extends PointerIndexVisitor implements CellVisitor {
    final TricolorHeapMarker heapMarker;
    final FreeHeapSpaceManager freeHeapSpaceManager;
    long darkMatterByteCount;
    long freeChunksByteCount;
    long liveDataByteCount;
    Pointer visitedCellOrigin;

    public AfterMarkSweepVerifier(TricolorHeapMarker heapMarker, FreeHeapSpaceManager freeHeapSpaceManager) {
        this.heapMarker = heapMarker;
        this.freeHeapSpaceManager = freeHeapSpaceManager;
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
        visit(pointer.getGrip(wordIndex).toOrigin());
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
                    visit(Layout.getGrip(origin, index).toOrigin());
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
        freeHeapSpaceManager.committedHeapSpace().walkCommittedSpace(this);
        freeHeapSpaceManager.verifyUsage(freeChunksByteCount, darkMatterByteCount, liveDataByteCount);
    }
}
