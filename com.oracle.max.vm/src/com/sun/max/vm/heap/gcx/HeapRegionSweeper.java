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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;

/**
 * Sweeping interface for heap made of multiple, possibly discontinuous regions.
 * A heap marker interacts with the sweeper via a hasNextSweepingRegion / beginSweep / endSweep methods that bracket the sweeping of each region.
 * Dead space within the region is notified to the sweeper via three interface: processLargeGap, processDeadSpace, and processFreeRegion.
 */
public abstract class HeapRegionSweeper extends Sweeper {
    static int SweepBreakAtRegion = -1;
    static {
        VMOptions.addFieldOption("-XX:", "SweepBreakAtRegion", HeapRegionSweeper.class, "Break before sweeping region", Phase.PRISTINE);
    }

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
     * Indicate that the region is the head of a multi-regions object.
     */
    boolean csrIsMultiRegionObjectHead;
    /**
     * Indicate that the sweeping region is the tail of a live multi-regions object.
     */
    boolean csrIsLiveMultiRegionObjectTail;

    /**
     * Cursor on the last live address seen during sweeping of the csr.
     */
    Address csrLastLiveAddress;

    /**
     * True if all dead spaces require their references to be erased. This is required if an imprecise remembered set (e.g., card table) is used for
     * root tracing. Erasing dead references is equivalent to turning dead space into reference-less heap cell.
     */
    final boolean zapDeadReferences;

    /**
     * Action to performed on a remembered set when dead space is identified.
     */
    final DeadSpaceRSetUpdater deadSpaceRSetUpdater;

    protected HeapRegionSweeper(boolean zapDeadReferences, DeadSpaceRSetUpdater deadSpaceRSetUpdater) {
        this.zapDeadReferences = zapDeadReferences;
        this.deadSpaceRSetUpdater = deadSpaceRSetUpdater == null ? DeadSpaceRSetUpdater.nullDeadSpaceRSetUpdater() : deadSpaceRSetUpdater;
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

        if (gapSize.greaterEqual(minReclaimableSpace())) {
            Log.println(" => reclaimable");
        } else {
            Log.println(" => dark matter");
        }
        Log.unlock(lockDisabledSafepoints);
    }

    private void printNotifiedDeadSpace(Address deadSpace, Size size) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("Dead Space @");
        Log.print(deadSpace);
        Log.print("(");
        Log.print(size.toLong());
        Log.println(")");
        Log.unlock(lockDisabledSafepoints);
    }

    final public int liveBytes() {
        return csrLiveBytes;
    }

    @Override
    public final Size minReclaimableSpace() {
        return minReclaimableSpace;
    }

    @Override
    public final Address startOfSweepingRegion() {
        return csrLastLiveAddress;
    }

    @Override
    public final Address endOfSweepingRegion() {
        return csrEnd;
    }

    @NEVER_INLINE
    private void breakpoint() {
    }

    final void resetSweepingRegion(HeapRegionInfo rinfo) {
        if (MaxineVM.isDebug() && SweepBreakAtRegion >= 0 && SweepBreakAtRegion == rinfo.toRegionID()) {
            breakpoint();
        }
        final Address regionStart = rinfo.regionStart();

        csrInfo = rinfo;
        csrEnd = regionStart.plus(regionSizeInBytes);
        csrFreeBytes = 0;
        csrHead = null;
        csrTail = null;
        csrFreeChunks = 0;
        csrLiveBytes = 0;
        if (!csrIsLiveMultiRegionObjectTail) {
            csrIsMultiRegionObjectHead = csrInfo.isHeadOfLargeObject();
            csrLastLiveAddress = regionStart;
        } else if (MaxineVM.isDebug()) {
            // Otherwise, csrLastLiveAddress is the address of the last word of the live multi-region object.
            FatalError.check(csrLastLiveAddress.greaterEqual(regionStart) && csrLastLiveAddress.lessEqual(csrEnd), "csrLastLiveAddress must be within tail region");
        }
        HeapRegionState.EMPTY_REGION.setState(csrInfo);
        csrInfo.resetOccupancy();
    }

    final void recordFreeSpace(Address chunk, Size chunkSize) {
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

    /**
     * Process a region without any live mark. If the region isn't part of a multi-region object, then it is free.
     * Otherwise, if it is the tail of a multi-region object, we need to record a single chunk after its tail.
     */
    public void processDeadRegion() {
        if (csrIsLiveMultiRegionObjectTail) {
            if (MaxineVM.isDebug()) {
                FatalError.check(csrLastLiveAddress.greaterThan(csrInfo.regionStart()), "Last live address must be greater than start of a live multi-regions object's tail");
            }
            recordFreeSpace(csrLastLiveAddress, csrEnd.minus(csrLastLiveAddress).asSize());
        } else {
            csrFreeBytes = regionSizeInBytes;
        }
    }

    public abstract boolean hasNextSweepingRegion();
    public abstract void reachedRightmostLiveRegion();

    /**
     * Indicates whether the current sweeping region is the head of a multi-regions object.
     * @return
     */
    public final boolean sweepingRegionIsLargeHead() {
        return csrIsMultiRegionObjectHead;
    }

    @Override
    public abstract void beginSweep();
    @Override
    public abstract void endSweep();

    @Override
    public abstract void verify(AfterMarkSweepVerifier verifier);

    /**
     * Invoked when doing imprecise sweeping to process a large interval between two marked locations.
     * Imprecise heap sweeping ignores any space before two live objects smaller than a specified amount of space.
     * When the distance between two live marks is large enough to indicate a potentially large chunk of free space,
     * the sweeper invoke this method. Note however that the reported gap may not be entirely free
     * (e.g., if the first object of the gap is actually larger than the minimum gap size).
     * FIXME: probably better to just have a processDeadSpace interface and leave these details to the imprecise sweeper.
     *
     * @param leftLiveObject
     * @param rightLiveObject
     * @return
     */
    @Override
    public Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject) {
        FatalError.check(rightLiveObject.lessEqual(endOfSweepingRegion()), "dead space must not cross region boundary");
        Pointer endOfLeftObject = leftLiveObject.plus(Layout.size(Layout.cellToOrigin(leftLiveObject)));
        csrLiveBytes += endOfLeftObject.minus(csrLastLiveAddress).asSize().toInt();
        Size numDeadBytes = rightLiveObject.minus(endOfLeftObject).asSize();
        if (MaxineVM.isDebug() &&  ((TraceSweep && Heap.verbose()) || TraceGap)) {
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
    @Override
    public void processDeadSpace(Address freeChunk, Size size) {
        assert freeChunk.plus(size).lessEqual(endOfSweepingRegion());
        csrLastLiveAddress = freeChunk.plus(size);
        if (MaxineVM.isDebug() && ((TraceSweep && Heap.verbose()) || TraceGap)) {
            printNotifiedDeadSpace(freeChunk, size);
        }
        recordFreeSpace(freeChunk, size);
    }

    @Override
    public Pointer processLiveObject(Pointer liveObject) {
        final Size numDeadBytes = liveObject.minus(csrLastLiveAddress).asSize();
        if (!numDeadBytes.isZero()) {
            final Pointer deadSpace = csrLastLiveAddress.asPointer();
            if (numDeadBytes.greaterThan(minReclaimableSpace)) {
                recordFreeSpace(deadSpace, numDeadBytes);
            } else if (zapDeadReferences) {
                HeapSchemeAdaptor.fillWithDeadObject(deadSpace, liveObject);
            }
            deadSpaceRSetUpdater.updateRSet(deadSpace, numDeadBytes);
        }
        final Size numLiveBytes = Layout.size(Layout.cellToOrigin(liveObject));
        csrLastLiveAddress = liveObject.plus(numLiveBytes);
        csrLiveBytes += numLiveBytes.toInt();
        return csrLastLiveAddress.asPointer();
    }
}
