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

public class HeapRegionSweeper {
    Size minReclaimableSpace;

    int numFreeBytes;
    Size darkMatter = Size.zero();
    HeapFreeChunk head;
    HeapFreeChunk tail;
    HeapRegionInfo rinfo;
    Address regionEnd;
    short numChunks;

    Size minReclaimableSpace() {
        return minReclaimableSpace;
    }

    Address start() {
        return rinfo.regionStart();
    }

    Address end() {
        return regionEnd;
    }

    void beginSweep(HeapRegionInfo rinfo) {
        this.rinfo = rinfo;
        rinfo.resetOccupancy();
        numFreeBytes = 0;
        darkMatter = Size.zero();
        head = null;
        tail = null;
        numChunks = 0;
        regionEnd = rinfo.regionStart().plus(regionSizeInBytes);
    }

    void endSweep() {
        if (numFreeBytes == 0) {
            rinfo.setFull();
        } else if (numFreeBytes == regionSizeInBytes) {
            rinfo.setEmpty();
        } else {
            rinfo.setAllocating();
            rinfo.setFreeChunks(HeapFreeChunk.fromHeapFreeChunk(head), (short) numFreeBytes, numChunks);
        }
    }

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

        if (gapSize.greaterEqual(minReclaimableSpace)) {
            Log.println("");
        } else {
            Log.println(" => dark matter");
        }
        Log.unlock(lockDisabledSafepoints);
    }
    void recordFreeSpace(Address chunk, Size chunkSize) {
        HeapFreeChunk c = HeapFreeChunk.format(chunk, chunkSize);
        if (tail == null) {
            head = c;
        } else {
            tail.next = c;
        }
        tail = c;
        numChunks++;
        numFreeBytes += chunkSize.toInt();
    }

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
        assert rightLiveObject.lessEqual(regionEnd);
        Pointer endOfLeftObject = leftLiveObject.plus(Layout.size(Layout.cellToOrigin(leftLiveObject)));
        Size numDeadBytes = rightLiveObject.minus(endOfLeftObject).asSize();
        if (numDeadBytes.greaterEqual(minReclaimableSpace)) {
            recordFreeSpace(endOfLeftObject, numDeadBytes);
        }
        return rightLiveObject.plus(Layout.size(Layout.cellToOrigin(rightLiveObject)));
    }

    /**
     * Invoked to record a known chunk of free space.
     * Used both by precise and imprecise sweeper, typically to record the unmarked space
     * at both end of the traced space.
     * @param freeChunk
     * @param size
     */
    public void processDeadSpace(Address freeChunk, Size size) {
        assert freeChunk.plus(size).lessEqual(regionEnd);
        recordFreeSpace(freeChunk, size);
    }

    public void processFreeRegion() {
        numFreeBytes = regionSizeInBytes;
    }

}
