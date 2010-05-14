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

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;

/**
 * After mark-sweep verifier for a free space manager with tracing based on TricolorHeapMarker.
 * FIXME: want to make this independent from FreeHeapSpaceManager.
 * @author Laurent Daynes
 */
public class AfterMarkSweepVerifier implements CellVisitor {
    final TricolorHeapMarker heapMarker;
    final FreeHeapSpaceManager freeHeapSpaceManager;
    long darkMatterByteCount;
    long freeChunksByteCount;
    long liveDataByteCount;

    public AfterMarkSweepVerifier(TricolorHeapMarker heapMarker, FreeHeapSpaceManager freeHeapSpaceManager) {
        this.heapMarker = heapMarker;
        this.freeHeapSpaceManager = freeHeapSpaceManager;
    }

    public Pointer visitCell(Pointer cell) {
        final Pointer origin = Layout.cellToOrigin(cell);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(origin).toJava());
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
