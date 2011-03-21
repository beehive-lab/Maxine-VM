/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;

/**
 * A rescan map keeps track of the areas that hold grey objects after a marking stack overflow.
 * It helps limiting rescan of the mark bitmaps.
 * The rescan map logically decomposes the mark bitmap associated to the heap in fixed sized region and for each region, tracks
 * the interval comprising grey marks.
 * The rescan map is updated when the marking stack overflow, and used during overflow rescan.
 *
 * @author Laurent Daynes
 */
public class RescanMap {
    private static short shortZero = (short) 0;
    /**
     * VM option to set the size of a rescan map entry. It is expressed in terms
     * of mark-bitmap storage. The VM rounds this up to the nearest power of 2 size corresponding
     * to a integral number of mark bitmap words.
     *
     * Assuming each mark bit covers 8-byte heap words, a region size of 1Kb covers 64 Kb of heap.
     *
     */
    static VMSizeOption rescanRegionSizeOption =
        register(new VMSizeOption("-XX:RescanRegionSize=", Size.K.times(1), "Unit of rescan after marking stack overflow"), MaxineVM.Phase.PRISTINE);

    /**
     * Address to the first byte of the heap covered by the mark bitmap.
     */
    private Address coveredAreaStart;

    /**
     * Rescan region size, in number of mark bitmap bytes. Must be a power of 2.
     */
    private int rescanRegionSize;
    /**
     * The log2 of the number of words per rescan map regions.
     */
    private int log2NumWordsPerRescanRegion;

    private int log2BitmapWord;

    /**
     * The log2 to shift a heap offset to obtain an index to the entry of the rescan map.
     */
    private int log2AddressToRescanMapIndex;

    /**
     * Mask to obtain the offset in the rescan region from a heap address.
     */
    private int rescanRegionOffsetMask;
    /**
     * The table tracking intervals of mark bitmap holdings grey marks.
     * There are two entries per logical region of the mark bitmap, for
     * respectively the first and last grey marks within that region.
     * A short index allows to identify 2^16 mark word, each holding 2^3 mark bits. Assuming a coverage of 8 bytes per
     * mark, this allows for logical regions of up to 2^25 = 32 Mb of heap.
     * A rescan region is typically much smaller than that (in the order of a few dozen of Kb).
     *
     * An entry records a range of mark bitmap words to scan. Because we want grey-free areas to be recorded with
     * 0, we use the convention of recording an interval [a,b] as <a, b+1>, i.e., the range [0,0[ indicates
     * that the corresponding mark bitmap regions doesn't have any grey objects.
     */
    private int[] rescanMapTable;
    /**
     * Index to the leftmost entry of the rescan map that contains grey objects.
     */
    private int leftmostEntry;
    /**
     * Index to the rightmost entry of the rescan map that contains grey objects.
     */
    private int rightmostEntry;

    /**
     * Index to the leftmost mark word with a grey bit in the leftmost rescan map entry.
     */
    private int leftmostLeftBound;
    /**
     * Index to the rightmost mark word with a grey bit in the leftmost rescan map entry.
     */
    private int leftmostRightBound;

    private int cachedRescanMapEntry;

    private static final int shortMask = (1 << WordWidth.BITS_16.numberOfBits) - 1;

    private int rightBound(int rescanMapEntry) {
        return rescanMapEntry >> WordWidth.BITS_16.numberOfBits;
    }

    private int leftBound(int rescanMapEntry) {
        return rescanMapEntry & shortMask;
    }

    private int rescanMapEntry(int leftBound, int rightBound) {
        return leftBound | (rightBound << WordWidth.BITS_16.numberOfBits);
    }

    /**
     * Cache the bounds of the leftmost entry in the rescan map.
     * Used in with {@link #fetchNextEntry()} to iterate over a mark-bitmap covered by the rescan map.
     * The bounds are indexes (in number of words) to the mark bitmap.
     */
    void cacheLeftmostEntryBound() {
        final int baseOfLeftmostEntry = leftmostEntry << log2NumWordsPerRescanRegion;
        cachedRescanMapEntry = ArrayAccess.getInt(rescanMapTable, leftmostEntry);
        leftmostLeftBound = baseOfLeftmostEntry +  leftBound(cachedRescanMapEntry);
        leftmostRightBound = baseOfLeftmostEntry + rightBound(cachedRescanMapEntry) - 1;
        if (MaxineVM.isDebug()) {
            FatalError.check(leftmostLeftBound <= leftmostRightBound, "Invalid rescan map entry");
        }
    }

    // DEBUG ONLY -- REMOVE
    Address beginOfGreyArea() {
        final int baseOfLeftmostEntry = leftmostEntry << log2NumWordsPerRescanRegion;
        final int rescanMapEntry = ArrayAccess.getInt(rescanMapTable, leftmostEntry);
        final int bitmapWordIndex = baseOfLeftmostEntry +  leftBound(rescanMapEntry);
        return coveredAreaStart.plus(bitmapWordIndex << log2BitmapWord);
    }

    /**
     * Returns the index to the word of the mark-bitmap corresponding to
     * the leftmost mark-bitmap word of the leftmost rescan region recorded
     * in the rescan map associated to the mark bitmap.
     * @see #cacheLeftmostEntryBound()
     *
     * @return index to a word of the mark bitmap covered by the rescan map
     *
     */
    int leftmostLeftBound() {
        return leftmostLeftBound;
    }

    /**
     * Returns the index to the word of the mark-bitmap corresponding to
     * the rightmost mark-bitmap word of the leftmost rescan region recorded in
     * the rescan map associated to the mark bitmap.
     * @see #cacheLeftmostEntryBound()
     *
     * @return index to a word of the mark bitmap covered by the rescan map
     *
     */
    int leftmostRightBound() {
        return leftmostRightBound;
    }

    /**
     * Clear the leftmost entry in the rescan map and search the rescan map for next entry holding grey marks,
     * and set cursor to the next leftmost entry in the rescan map.
     */
    void fetchNextEntry() {
        final int currentEntry = ArrayAccess.getInt(rescanMapTable, leftmostEntry);
        if (currentEntry != cachedRescanMapEntry) {
            if (MaxineVM.isDebug()) {
                FatalError.check(leftmostLeftBound == leftBound(currentEntry), "Left bound of updated rescan map entry must not have changed");
                FatalError.check(leftmostRightBound < rightBound(currentEntry), "Right bound of updated rescan map entry must be greater");
            }
            // Set left bound of the rescan map to the previous right bound to avoid rescanning whole entry. Just want to scan
            // the added part.
            ArrayAccess.setInt(rescanMapTable, leftmostEntry, rescanMapEntry(leftmostRightBound, rightBound(currentEntry)));
            return;
        }
        // Clear the leftmost entry:
        ArrayAccess.setInt(rescanMapTable, leftmostEntry++, 0);
        while (leftmostEntry  <= rightmostEntry) {
            if (ArrayAccess.getInt(rescanMapTable, leftmostEntry) != 0) {
                return;
            }
            leftmostEntry++;
        }
        // Rescan map empty, clear its bounds.
        resetRescanBound();
    }

    /**
     * Return a boolean indicating whether the rescan map is empty, i.e., has no recorded words of the mark bitmap containing grey objects.
     * @return true if the rescan map is empty, false otherwise.
     */
    boolean isEmpty() {
        return leftmostEntry == rescanMapTable.length;
    }

    RescanMap() {
    }

    void resetRescanBound() {
        leftmostEntry = rescanMapTable.length;
        rightmostEntry = 0;
    }

    // FOR DEBUGGING. REMOVE ME
    TricolorHeapMarker tricolorHeapMarker;

    void recordCellForRescan(Pointer cell) {
        final int bitmapWordIndex = cell.minus(coveredAreaStart).unsignedShiftedRight(log2BitmapWord).toInt();
        final int rescanMapIndex = bitmapWordIndex >> log2NumWordsPerRescanRegion;
        final int indexInRescanRegion = bitmapWordIndex & rescanRegionOffsetMask;
        if (MaxineVM.isDebug()) {
            final int index =  (rescanMapIndex << log2NumWordsPerRescanRegion) + indexInRescanRegion;
            FatalError.check(index == tricolorHeapMarker.bitmapWordIndex(cell), "Bitmap Word Index from rescan map is incorrect");
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Recording cell ");
            Log.print(cell);
            Log.print(" bit index: ");
            Log.print(cell.minus(coveredAreaStart).unsignedShiftedRight(tricolorHeapMarker.log2BytesCoveredPerBit).toInt());
            Log.print(" bitmap word:");
            Log.print(bitmapWordIndex);
            Log.print(" rescan map index: ");
            Log.print(rescanMapIndex);
            Log.print(" index in rescan region: ");
            Log.println(indexInRescanRegion);
            Log.unlock(lockDisabledSafepoints);
        }

        if (rightmostEntry < rescanMapIndex) {
            rightmostEntry = rescanMapIndex;
        }

        if (leftmostEntry > rescanMapIndex) {
            leftmostEntry = rescanMapIndex;
            ArrayAccess.setInt(rescanMapTable, rescanMapIndex, rescanMapEntry(indexInRescanRegion, indexInRescanRegion + 1));
            return;
        }
        final int currentEntry = ArrayAccess.getInt(rescanMapTable, rescanMapIndex);
        final int leftBound = leftBound(currentEntry);
        if (leftBound > indexInRescanRegion) {
            ArrayAccess.setInt(rescanMapTable, rescanMapIndex, rescanMapEntry(indexInRescanRegion, rightBound(currentEntry)));
        } else if (indexInRescanRegion >= rightBound(currentEntry)) {
            ArrayAccess.setInt(rescanMapTable, rescanMapIndex, rescanMapEntry(leftBound, indexInRescanRegion + 1));
        }
    }

    /**
     * Initialize the rescan map of a tricolor heap marker.
     * @param tricolorHeapMarker the tricolor heap marker associated with the rescan map
     */
    void initialize(TricolorHeapMarker tricolorHeapMarker) {
        coveredAreaStart = tricolorHeapMarker.coveredAreaStart;
        rescanRegionSize = rescanRegionSizeOption.getValue().roundedUpBy(1 << tricolorHeapMarker.log2BytesCoveredPerBit).toInt();
        final int log2RescanRegionSize = Integer.numberOfTrailingZeros(rescanRegionSize);
        log2NumWordsPerRescanRegion = log2RescanRegionSize - WordWidth.BITS_64.log2numberOfBytes;
        log2BitmapWord = tricolorHeapMarker.log2BitmapWord;
        Size rescanMapSize = tricolorHeapMarker.colorMap.size().unsignedShiftedRight(log2RescanRegionSize);
        rescanRegionOffsetMask = (1 << log2NumWordsPerRescanRegion) - 1;
        Heap.enableImmortalMemoryAllocation();
        rescanMapTable = new int[rescanMapSize.toInt()];
        Heap.disableImmortalMemoryAllocation();
        resetRescanBound();
        FatalError.check(rescanRegionSize == 1 << log2RescanRegionSize, "RescanMap region size must be a power of 2");
        if (MaxineVM.isDebug()) {
            this.tricolorHeapMarker = tricolorHeapMarker;
        }
    }
}
