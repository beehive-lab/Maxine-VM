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
import com.sun.max.vm.heap.gcx.TricolorHeapMarker.ColorMapScanState;
import com.sun.max.vm.heap.gcx.TricolorHeapMarker.ForwardScanState;
import com.sun.max.vm.heap.gcx.TricolorHeapMarker.MarkingStackFlusher;
import static com.sun.max.vm.heap.gcx.TricolorHeapMarker.*;

/**
 * State of an overflow scan of the tricolor map. A scan of the tricolor map enter this state when the marking stack overflow.
 * The overflow state remembers the bound of the forward scan to resume it once the overflow is dealt with, in addition to state necessary to
 * deal with the overflow of the marking stack. This consists in rescanning a part of the tricolor map already scanned to find grey references
 * dropped from the marking stack.
 * Sub-class of this class implements various rescan strategies depending on information recorded during forward scan  to increase the precision of
 * overflow scan.
 */
abstract class OverflowScanState extends ColorMapScanState {
    /**
     * Specifies where the end of the overflow scan should end. Typically, this is saving the finger of the forward scan.
     */
    protected Address endOfScan;
    /**
     * Specifies how the marking stack is flushed.
     */
    protected MarkingStackFlusher markingStackFlusher;
    /**
     * Specifies the discontinuous regions to iterate over.
     */
    protected HeapRegionRangeIterable regionsRanges;

    OverflowScanState(TricolorHeapMarker heapMarker) {
        super(heapMarker);
    }

    protected final void setMarkingStackFlusher(MarkingStackFlusher flusher) {
        markingStackFlusher = flusher;
    }

    /**
     * VM startup initialization.
     */
    void initialize() {
        heapMarker.markingStack.initialize(markingStackFlusher);
    }

    void setHeapRegionsRanges(HeapRegionRangeIterable regionsRanges) {
        this.regionsRanges = regionsRanges;
    }

    @Override
    public int rightmostBitmapWordIndex() {
        return heapMarker.bitmapWordIndex(endOfScan);
    }

    @INLINE
    final boolean isRecovering() {
        return heapMarker.currentScanState == this;
    }

    final MarkingStackFlusher markingStackFlusher() {
        return markingStackFlusher;
    }

    abstract void recoverFromOverflow();

    /**
     * Visit all grey objects whose mark is within the specified range of words of the color map.
     * The forward scan finger may be anywhere within the rightmost bitmap word of the range, so
     * we must test every grey cell against it to stop correctly the recovery scan.
     *
     * @param bitmapWordIndex
     * @param rightmostBitmapWordIndex
     */
    void visitGreyObjectsTillEndOfScan(int bitmapWordIndex, int rightmostBitmapWordIndex) {
        // This is slightly different from the forward scan:
        // The scan ends when reaching the forward scan finger. This one may be located in word shared with some
        // object visited during overflow scan. So we must check that precise condition to avoid visiting
        // objects already visited by the forward scan.

        final Pointer colorMapBase = heapMarker.colorMapBase();
        while (bitmapWordIndex <= rightmostBitmapWordIndex) {
            long bitmapWord = colorMapBase.getLong(bitmapWordIndex);
            if (bitmapWord != 0L) {
                final long greyMarksInWord = bitmapWord & (bitmapWord >>> 1);
                if (greyMarksInWord != 0L) {
                    // First grey mark is the least set bit.
                    final int bitIndexInWord = Pointer.fromLong(greyMarksInWord).leastSignificantBitSet();
                    final int bitIndexOfGreyCell = (bitmapWordIndex << Word.widthValue().log2numberOfBits) + bitIndexInWord;
                    Pointer p = heapMarker.addressOf(bitIndexOfGreyCell).asPointer();
                    if (p.greaterEqual(endOfScan)) {
                        return;
                    }
                    p = markAndVisitCell(p);
                    // Get bitmap word index at the end of the object. This may avoid reading multiple mark bitmap words
                    // when marking objects crossing multiple mark bitmap words.
                    bitmapWordIndex = heapMarker.bitmapWordIndex(p);
                    continue;
                } else if ((bitmapWord >>> TricolorHeapMarker.LAST_BIT_INDEX_IN_WORD) == 1L) {
                    // Mark span two words. Check first bit of next word to decide if mark is grey.
                    bitmapWord = colorMapBase.getLong(bitmapWordIndex + 1);
                    if ((bitmapWord & 1) != 0) {
                        // it is a grey object.
                        final int bitIndexOfGreyCell = (bitmapWordIndex << Word.widthValue().log2numberOfBits) + TricolorHeapMarker.LAST_BIT_INDEX_IN_WORD;
                        Pointer p = heapMarker.addressOf(bitIndexOfGreyCell).asPointer();
                        if (p.greaterEqual(endOfScan)) {
                            return;
                        }
                        p = markAndVisitCell(p);
                        bitmapWordIndex = heapMarker.bitmapWordIndex(p);
                        continue;
                    }
                }
            }
            bitmapWordIndex++;
        }
    }

    /**
     * Visit all grey objects whose mark is within the specified range of words of the color map.
     * The forward scan finger is guaranteed not to be within the rightmost bitmap word of the range.
     *
     * @param bitmapWordIndex
     * @param rightmostBitmapWordIndex
     */
    void visitGreyObjectsBeforeEndOfScan(int bitmapWordIndex, int rightmostBitmapWordIndex) {
        // Same as above, but we don't need to check against the forward scan finger since the scanned range is strictly before it.
        final Pointer colorMapBase = heapMarker.colorMapBase();
        while (bitmapWordIndex <= rightmostBitmapWordIndex) {
            long bitmapWord = colorMapBase.getLong(bitmapWordIndex);
            if (bitmapWord != 0L) {
                final long greyMarksInWord = bitmapWord & (bitmapWord >>> 1);
                if (greyMarksInWord != 0L) {
                    // First grey mark is the least set bit.
                    final int bitIndexInWord = Pointer.fromLong(greyMarksInWord).leastSignificantBitSet();
                    final int bitIndexOfGreyCell = (bitmapWordIndex << Word.widthValue().log2numberOfBits) + bitIndexInWord;
                    Pointer p = heapMarker.addressOf(bitIndexOfGreyCell).asPointer();
                    p = markAndVisitCell(p);
                    // Get bitmap word index at the end of the object. This may avoid reading multiple mark bitmap words
                    // when marking objects crossing multiple mark bitmap words.
                    bitmapWordIndex = heapMarker.bitmapWordIndex(p);
                    continue;
                } else if ((bitmapWord >>> TricolorHeapMarker.LAST_BIT_INDEX_IN_WORD) == 1L) {
                    // Mark span two words. Check first bit of next word to decide if mark is grey.
                    bitmapWord = colorMapBase.getLong(bitmapWordIndex + 1);
                    if ((bitmapWord & 1) != 0) {
                        // it is a grey object.
                        final int bitIndexOfGreyCell = (bitmapWordIndex << Word.widthValue().log2numberOfBits) + TricolorHeapMarker.LAST_BIT_INDEX_IN_WORD;
                        Pointer p = heapMarker.addressOf(bitIndexOfGreyCell).asPointer();
                        p = markAndVisitCell(p);
                        bitmapWordIndex = heapMarker.bitmapWordIndex(p);
                        continue;
                    }
                }
            }
            bitmapWordIndex++;
        }
    }


    private  void visitGreyObjects(HeapRegionRangeIterable regionsRanges, int fingerBitmapWordIndex, int rightmostBitmapWordIndex) {
        final int log2RegionToBitmapWord = HeapRegionConstants.log2RegionSizeInBytes - heapMarker.log2BitmapWord;
        int fingerRegion = HeapRegionConstants.regionStart(finger).minus(heapMarker.coveredAreaStart).unsignedShiftedRight(HeapRegionConstants.log2RegionSizeInBytes).toInt();
        regionsRanges.reset(fingerRegion);

        while (regionsRanges.hasNext()) {
            final RegionRange regionRange = regionsRanges.next();
            final int rangeLeftmostBitmapWordIndex = regionRange.firstRegion() << log2RegionToBitmapWord;
            final int rangeRightmostBitmapWordIndex = rangeLeftmostBitmapWordIndex + (regionRange.numRegions() << log2RegionToBitmapWord);
            if (rangeLeftmostBitmapWordIndex > fingerBitmapWordIndex) {
                fingerBitmapWordIndex = rangeLeftmostBitmapWordIndex;
            }
            if (rangeRightmostBitmapWordIndex > rightmostBitmapWordIndex) {
                visitGreyObjectsTillEndOfScan(fingerBitmapWordIndex, rightmostBitmapWordIndex);
                break;
            }
            visitGreyObjectsBeforeEndOfScan(fingerBitmapWordIndex, rangeRightmostBitmapWordIndex);
        }
    }

    @INLINE
    protected void visitGreyObjects(int fingerBitmapWordIndex, int rightmostBitmapWordIndex) {
        if (regionsRanges != null) {
            visitGreyObjects(regionsRanges, fingerBitmapWordIndex, rightmostBitmapWordIndex);
        } else {
            visitGreyObjectsTillEndOfScan(fingerBitmapWordIndex, rightmostBitmapWordIndex);
        }
    }

    @INLINE
    protected void verifyHasNoGreyMarks(Address end) {
        if (!TricolorHeapMarker.VerifyGreyLessAreas) {
            return;
        }
        if (regionsRanges != null) {
            regionsRanges.reset();
            heapMarker.verifyHasNoGreyMarks(regionsRanges, end);
        } else {
            heapMarker.verifyHasNoGreyMarks(heapMarker.coveredAreaStart, end);
        }
    }

    protected void beginRecovery() {
        final ForwardScanState forwardScanState = heapMarker.forwardScanState;
        heapMarker.startTimer(heapMarker.recoveryScanTimer);
        heapMarker.currentScanState = this;
        // set the upper bound for rescanning to the finger of the forward scan
        endOfScan = forwardScanState.finger;
        // cache rightmost locally.
        rightmost = forwardScanState.rightmost;
        markingStackFlusher.setScanState(this);
        if (MaxineVM.isDebug() && TraceMarking) {
            Log.println("Begin Overflow Scan");
            heapMarker.traceMark(endOfScan, " => endOfScan\n");
            heapMarker.traceMark(rightmost, " => rightmost\n");
        }
    }

    protected void endRecovery() {
        final ForwardScanState forwardScanState = heapMarker.forwardScanState;
        verifyHasNoGreyMarks(forwardScanState.finger);
        forwardScanState.rightmost = rightmost;
        markingStackFlusher.setScanState(forwardScanState);
        heapMarker.currentScanState = forwardScanState;
        heapMarker.stopTimer(heapMarker.recoveryScanTimer);
        if (MaxineVM.isDebug() && TraceMarking) {
            Log.print("End Overflow Scan (# mark stack overflow = ");
            Log.print(numMarkinkgStackOverflow); Log.println(")");
            heapMarker.traceMark(finger, " => overflow finger\n");
            heapMarker.traceMark(rightmost, " => rightmost\n");
        }
    }

    @Override
    public String toString() {
        return "overflow scan";
    }
}
