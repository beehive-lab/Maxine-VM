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

import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.layout.*;

/**
 * Sweeping interface for heap made of multiple, possibly discontinuous regions.
 * A heap marker interacts with the sweeper via a hasNextSweepingRegion / beginSweep / endSweep methods that bracket the sweeping of each region.
 * Dead space within the region is notified to the sweeper via three interface: processLargeGap, processDeadSpace, and processFreeRegion.
 */
public abstract class HeapRegionSweeper implements MarkSweepVerification {

    protected Size minReclaimableSpace;

    /**
     * Region information for the current sweeping region (csr).
     */
    HeapRegionInfo csrInfo;
    /**
     * Number of free bytes recorded for the csr.
     */
    int csrFreeBytes;

    /**
     * Number of live bytes recorded for the csr.
     */
    int csrLiveBytes;

    /**
     * Number of free chunks in the csr.
     */
    int csrFreeChunks;
    /**
     * Head of the list of free chunks recorded for the csr.
     */
    HeapFreeChunk csrHead;
    /**
     * Tail of the list of free chunks recorded for the csr.
     */
    HeapFreeChunk csrTail;
    /**
     * End of the current sweeping region.
     */
    Address csrEnd;

    /**
     * Cursor on the last live address seen during sweeping of the csr.
     */
    Address csrLastLiveAddress;

    private void printNotifiedGap(Pointer leftLiveObject, Pointer rightLiveObject, Pointer gapAddress, Size gapSize) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("Gap between [");
        Log.print(leftLiveObject);
        Log.print(", ");
        Log.print(rightLiveObject);
        Log.print("] = @");
        Log.print(gapAddress);
        Log.print("(");
        Log.print(gapSize.toLong());
        Log.print(")");

        if (gapSize.greaterEqual(minReclaimableSpace())) {
            Log.println(" => reclaimable");
        } else {
            Log.println(" => dark matter");
        }
        Log.unlock(lockDisabledSafepoints);
    }

    final public int liveBytes() {
        return csrLiveBytes;
    }

    public final Size minReclaimableSpace() {
        return minReclaimableSpace;
    }

    final Address startOfSweepingRegion() {
        return csrInfo.regionStart();
    }

    final Address endOfSweepingRegion() {
        return csrEnd;
    }


    final void resetSweepingRegion(HeapRegionInfo rinfo) {
        csrInfo = rinfo;
        csrFreeBytes = 0;
        csrHead = null;
        csrTail = null;
        csrFreeChunks = 0;

        csrLiveBytes = 0;
        csrLastLiveAddress = csrInfo.regionStart();

        csrInfo.resetOccupancy();
        csrEnd = csrLastLiveAddress.plus(regionSizeInBytes);

        Log.print("Sweeping region #");
        Log.println(csrInfo.toRegionID());
    }

    void recordFreeSpace(Address chunk, Size chunkSize) {
        HeapFreeChunk c = HeapFreeChunk.format(chunk, chunkSize);
        if (csrTail == null) {
            csrHead = c;
        } else {
            csrTail.next = c;
        }
        csrTail = c;
        csrFreeChunks++;
        csrFreeBytes += chunkSize.toInt();
    }

    public void processFreeRegion() {
        csrFreeBytes = regionSizeInBytes;
    }

    public abstract boolean hasNextSweepingRegion();
    public abstract void beginSweep();
    public abstract void endSweep();
    public abstract void verify(AfterMarkSweepVerifier verifier);

    /**
     * Invoked when doing imprecise sweeping to process an large interval between to marked locations.
     * Imprecise heap sweeping ignores any space before two live objects smaller than a specified amount of space.
     * When the distance between two live marks is large enough to indicate a potentially large chunk of free space,
     * the sweeper invoke this method.
     *
     * @param leftLiveObject
     * @param rightLiveObject
     * @return
     */
    public Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject) {
        assert rightLiveObject.lessEqual(endOfSweepingRegion());
        Pointer endOfLeftObject = leftLiveObject.plus(Layout.size(Layout.cellToOrigin(leftLiveObject)));
        csrLiveBytes += endOfLeftObject.minus(csrLastLiveAddress).asSize().toInt();
        Size numDeadBytes = rightLiveObject.minus(endOfLeftObject).asSize();
        if (MaxineVM.isDebug()) {
            printNotifiedGap(leftLiveObject, rightLiveObject, endOfLeftObject, numDeadBytes);
        }
        if (numDeadBytes.greaterEqual(minReclaimableSpace)) {
            recordFreeSpace(endOfLeftObject, numDeadBytes);
        }
        csrLastLiveAddress = rightLiveObject.plus(Layout.size(Layout.cellToOrigin(rightLiveObject)));
        return csrLastLiveAddress.asPointer();
    }

    /**
     * Invoked to record a known chunk of free space.
     * Used both by precise and imprecise sweeper, typically to record the unmarked space
     * at both end of the traced space.
     * @param freeChunk
     * @param size
     */
    public void processDeadSpace(Address freeChunk, Size size) {
        assert freeChunk.plus(size).lessEqual(endOfSweepingRegion());
        csrLastLiveAddress = freeChunk.plus(size);
        recordFreeSpace(freeChunk, size);
    }
}
