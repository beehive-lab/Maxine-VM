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
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.TricolorHeapMarker.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
/**
 * State of the forward scan of the tricolor mark-bitmap.
 * The forward scan linearly iterates over the mark bitmap, starting from the leftmost object marked grey by the root scan, up to the rightmost
 * object. The forward scan advances a cursor, called the finger, which is set on the currently visited grey object.
 * References to the left of the cursor (i.e., backward references) are pushed on a marking stack while reference forward are marked grey.
 * To avoid duplicates on the marking stack, the backward references are marked grey as well.
 */
final class ForwardScanState extends ColorMapScanState implements SpecialReferenceManager.GC {

    ForwardScanState(TricolorHeapMarker heapMarker) {
        super(heapMarker);
    }

    Address endOfRightmostVisitedObject() {
        return rightmost.plus(Layout.size(Layout.cellToOrigin(rightmost.asPointer())));
    }

    @Override
    public int rightmostBitmapWordIndex() {
        return heapMarker.bitmapWordIndex(endOfRightmostVisitedObject());
    }

    /*
     * Helper instance variables for debugging purposes only.
     * Easier to track than local variables when under the inspector.
     */
    private int debugRightmostBitmapWordIndex;
    private int debugBitmapWordIndex;
    private long debugBitmapWord;

    /**
     * Visit grey objects located between the finger and a rightmost position identified by a word index in the color map.
     * Iteration stops when the index of the word holding the mark of the finger goes beyond the rightmost position specified.
     * This code is shared between marking for region-based heaps and marking for single-contiguous space heaps.
     *
     * @param rightmostBitmapWordIndex
     */
    void visitGreyObjects(int rightmostBitmapWordIndex) {
        final Pointer colorMapBase = heapMarker.base.asPointer();
        int bitmapWordIndex = heapMarker.bitmapWordIndex(finger);
        if (MaxineVM.isDebug()) {
            debugRightmostBitmapWordIndex = rightmostBitmapWordIndex;
            debugBitmapWordIndex = bitmapWordIndex;
        }
        visitGreyObjects(bitmapWordIndex, rightmostBitmapWordIndex);
         // There might be some objects left in the marking stack. Drain it.
        // Before draining, advance the finger to the next mark bitmap word boundary to force all white references from drained cells that point to objects
        // with mark in the current mark word to be pushed up on the marking stack and processed during the drainage.
        Address fingerBeforeDraining = finger;
        if (MaxineVM.isDebug() && TricolorHeapMarker.TraceMarking) {
            heapMarker.traceMark(fingerBeforeDraining, " => finger before draining marking stack\n");
        }
        finger = heapMarker.nextMarkWordBoundary(finger);
        heapMarker.markingStack.drain();

        // Reset the finger to the rightmost black object in the finger's mark word.
        int fingerBitmapWordIndex = heapMarker.bitmapWordIndex(fingerBeforeDraining);
        int fingerBitIndex = fingerBitmapWordIndex << Word.widthValue().log2numberOfBits;
        fingerBitIndex += Address.fromLong(colorMapBase.getLong(fingerBitmapWordIndex)).mostSignificantBitSet();
        finger = heapMarker.addressOf(fingerBitIndex);
        // Adjust the rightmost pointer too. This one may have been before the next mark word boundary, in which case it wouldn't be updated by
        // the draining of the marking stack if the rightmost visited object remain before that boundary.
        if (rightmost.lessThan(finger)) {
            rightmost = finger;
        }
        if (MaxineVM.isDebug() && TricolorHeapMarker.TraceMarking) {
            heapMarker.traceMark(rightmost, " => rightmost after draining marking stack\n");
        }
    }

    /**
     * Forward scan over the mark bitmap, from the finger to the rightmost marked position.
     * @param regionsRanges
     */
    public void visitGreyObjects(HeapRegionRangeIterable regionsRanges) {
        final int log2RegionToBitmapWord = HeapRegionConstants.log2RegionSizeInBytes - heapMarker.log2BitmapWord;
        if (MaxineVM.isDebug() && TricolorHeapMarker.TraceMarking) {
            Log.println("Begin Forward Scan");
        }
        while (regionsRanges.hasNext()) {
            final int fingerBitmapWordIndex = heapMarker.bitmapWordIndex(finger);
            final RegionRange regionRange = regionsRanges.next();
            final int firstRegion = regionRange.firstRegion();
            if (MaxineVM.isDebug() && TricolorHeapMarker.TraceMarking) {
                Log.print("Begin scan of regions range ["); Log.print(firstRegion); Log.print(", "); Log.print(firstRegion + regionRange.numRegions()); Log.println("[");
            }

            final int rangeRightmostBitmapWordIndex = ((firstRegion + regionRange.numRegions()) << log2RegionToBitmapWord) - 1;
            if (fingerBitmapWordIndex > rangeRightmostBitmapWordIndex) {
                // skip this range, finger is past it already. This may happen after initial root marking. when the leftmost marked
                // position is beyond the first ranges, or when starting a new pass on the mark bitmap, e.g., to trace live objects from untraced special references.
                continue;
            }
            FatalError.check((firstRegion << log2RegionToBitmapWord) <= fingerBitmapWordIndex, "finger must be within the region range");

            int rightmostBitmapWordIndex = rightmostBitmapWordIndex();
            if (rightmostBitmapWordIndex > rangeRightmostBitmapWordIndex) {
                rightmostBitmapWordIndex = rangeRightmostBitmapWordIndex;
            }
            do {
                visitGreyObjects(rightmostBitmapWordIndex);
                if (finger == rightmost) {
                    // We reached the right most mark. No need to continue iterating over regions.
                    FatalError.check(heapMarker.markingStack.isEmpty(), "marking stack must be empty");
                    if (!heapMarker.isBlackWhenNotWhite(rightmost)) {
                        int rbi = heapMarker.bitIndexOf(rightmost);
                        heapMarker.traceMark(rightmost, heapMarker.color(rbi), rbi, " *** rightmost object must be marked black\n");
                        printState();
                        heapMarker.overflowScanState.printState();
                        MarkingError.rightmostNotBlackError.report(heapMarker.markPhase);
                    }
                    if (MaxineVM.isDebug() && TricolorHeapMarker.TraceMarking) {
                        Log.println("End Forward Scan");
                    }
                    return;
                }
                if (MaxineVM.isDebug() && finger.greaterThan(rightmost)) {
                    MarkingError.fingerGreaterThanRightmostError.report(heapMarker.markPhase);
                }
                // finger is less than rightmost. This may be because:
                // - the rightmost was not in this region range but in one further up the address space
                // - we may have drained an object that contained a reference past the finger's mark word boundary.

                final int b = rightmostBitmapWordIndex();
                if (rightmostBitmapWordIndex ==  rangeRightmostBitmapWordIndex) {
                    if (MaxineVM.isDebug() && b > rangeRightmostBitmapWordIndex) {
                        MarkingError.rightmostNotAboveCurrentRegionRangeError.report(heapMarker.markPhase);
                    }
                    if (MaxineVM.isDebug() && TricolorHeapMarker.TraceMarking) {
                        Log.print("End scan of regions range ["); Log.print(firstRegion); Log.print(", "); Log.print(firstRegion + regionRange.numRegions()); Log.println("[");
                    }
                    // We're done with the current regions range. Break
                    // to the outer loop to iterate over subsequent region ranges.
                    break;
                }
                // Update rightmost for the next iterate over the current range.
                rightmostBitmapWordIndex = b > rangeRightmostBitmapWordIndex ? rangeRightmostBitmapWordIndex : b;
            } while(true);
        }
    }

    public void visitGreyObjects() {
        int rightmostBitmapWordIndex = rightmostBitmapWordIndex();
        do {
            visitGreyObjects(rightmostBitmapWordIndex);
            // Rightmost may have been updated (e.g., when the marking stack was drained). Check for this, and loop back if it has.
            final int b = rightmostBitmapWordIndex();
            if (b <= rightmostBitmapWordIndex) {
                // We're done.
                return;
            }
            rightmostBitmapWordIndex = b;
        } while(true);
    }

    public boolean isReachable(Reference ref) {
        Pointer origin = ref.toOrigin();
        if (heapMarker.isCovered(origin)) {
            // If either back or grey, the object is reachable.
            return !heapMarker.isWhite(origin);
        }
        // If not in the covered area, it must be in one of the regions treated as permanent roots.
        // We cannot easily check that here because of NativeMutex which store the address of a NativeMutex
        // in there reference field (nasty piece of work...).
        return true;
    }

    public Reference preserve(Reference ref) {
        visit(ref);
        return ref;
    }

    public boolean mayRelocateLiveObjects() {
        return false;
    }

    @Override
    public String toString() {
        return "forward scan";
    }
}
