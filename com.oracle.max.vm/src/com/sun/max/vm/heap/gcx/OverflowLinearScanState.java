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

import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.TricolorHeapMarker.*;

/**
 * Recovery from marking stack overflow using a linear scan of the mark bitmap.
 * Flush the marking stack, marking all objects referenced from it grey, and iterate over
 * the mark-bitmap from the mark of the leftmost flushed reference up to the forward scan finger.
 * As with a forward scan, a rescan finger is maintained: references before the rescan finger are pushed
 * on the marking stack, those after aren't since they'll be iterated over and found.
 */
final class OverflowLinearScanState extends OverflowScanState {
    /**
     * Start of the next scan of the color map to recover from secondary marking stack overflow.
     */
    Address startOfNextOverflowScan;

    private final DeepMarkingStackFlusher deepMarkStackFlusher;
    private final BaseMarkingStackFlusher shallowMarkStackFlusher;

    OverflowLinearScanState(TricolorHeapMarker heapMarker) {
        super(heapMarker);
        deepMarkStackFlusher = new DeepMarkingStackFlusher(heapMarker);
        shallowMarkStackFlusher = new BaseMarkingStackFlusher();
    }

    private void visitGreyObjects() {
        int rightmostBitmapWordIndex = rightmostBitmapWordIndex();
        int fingerBitmapWordIndex = heapMarker.bitmapWordIndex(finger);

        if (regionsRanges != null) {
            visitGreyObjects(regionsRanges);
        } else {
            visitGreyObjectsToEndOfScan(fingerBitmapWordIndex, rightmostBitmapWordIndex);
        }
        // The overflow rescan may have ended with a finger before the end of scan position as the finger is updated only when
        // visiting grey cell. Before draining the marking stack, we set it to the end of scan position so draining
        // operates with the marking stack only.
        finger = endOfScan;
        // There might be some objects left in the marking stack. Drain it.
        heapMarker.markingStack.drain();
    }

    @Override
    void initialize() {
        setMarkingStackFlusher(TricolorHeapMarker.UseDeepMarkStackFlush ? deepMarkStackFlusher : shallowMarkStackFlusher);
        super.initialize();
    }

    @Override
    void recoverFromOverflow() {
        // First, flush the marking stack, greying all objects referenced from it,
        // and tracking the left most grey from which the rescan will start.
        Address leftmostFlushed = markingStackFlusher.flushMarkingStack();
        // Next, initiate the scan to recover from overflow. This consists of
        // visiting all grey objects between the leftmost flushed mark and the forward scan finger.
        // The rescan iterate over the mark bitmap, as the forward scan, except that it uses its own finger.
        //
        // As for a normal scan, any reference pointing after the "rescan" finger are marked grey and not visited, they
        // will be visited as the rescan finger pass over them.
        // Any reference before the finger are marked grey and  pushed on the marking stack.
        // The scan stops when reaching the forward scan finger (which act
        // as the rightmost bound for this scan).
        // If the marking stack overflow again, we flush the stack again and write down the leftmost mark
        // for the next scan.

        if (!isRecovering()) {
            beginRecovery();
            startOfNextOverflowScan = leftmostFlushed;
            final Address forwardScanFinger = endOfScan;

            do {
                verifyHasNoGreyMarks(startOfNextOverflowScan);
                finger = startOfNextOverflowScan;
                startOfNextOverflowScan = forwardScanFinger;
                visitGreyObjects();
            } while (startOfNextOverflowScan.lessThan(forwardScanFinger));
            endRecovery();
            numMarkinkgStackOverflow = 0;
        } else if (leftmostFlushed.lessThan(startOfNextOverflowScan)) {
            numMarkinkgStackOverflow++;
            // Schedule another rescan if the leftmost flushed cell is before the
            // currently visited cell.
            startOfNextOverflowScan = leftmostFlushed;
        }
    }
}
