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
 * The rescan map logically decomposes the heap in fixed sized region and for each region, tracks
 * the interval comprising grey marks on the mark bitmaps.
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
     * Rescan region size, in number of mark bitmap words. Must be a power of 2.
     */
    private int rescanRegionSize;
    /**
     * The log2 to shift an heap address into a rescan map index.
     */
    private int log2RescanMapIndex;

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
    private int rescanMapTable [];
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

    private static final int shortMask = (1 << WordWidth.BITS_16.log2numberOfBits) -1;

    private int rightBound(int rescanMapEntry) {
        return rescanMapEntry >> WordWidth.BITS_16.log2numberOfBits;
    }

    private int leftBound(int rescanMapEntry) {
        return rescanMapEntry & shortMask;
    }

    private int rescanMapEntry(int leftBound, int rightBound) {
        return leftBound | (rightBound << WordWidth.BITS_16.log2numberOfBits);
    }

    /**
     * Cache the bounds of the leftmost entry in the rescan map.
     */
    void cacheLeftmostEntryBound() {
        final int baseOfLeftmostEntry = leftmostEntry << log2RescanMapIndex;
        final int rescanMapEntry = ArrayAccess.getInt(rescanMapTable, leftmostEntry);
        leftmostLeftBound = baseOfLeftmostEntry +  leftBound(rescanMapEntry);
        leftmostRightBound = baseOfLeftmostEntry + rightBound(rescanMapEntry);
    }

    int leftmostLeftBound() {
        return leftmostLeftBound;
    }

    int leftmostRightBound() {
        return leftmostRightBound;
    }

    /**
     * Clear the leftmost entry in the rescan map and search the rescan map for next entry holding grey marks,
     * and set cursor to the next leftmost entry in the rescan map.
     */
    void fetchNextEntry() {
        // Clear the leftmost entry:
        ArrayAccess.setInt(rescanMapTable, leftmostEntry++, 0);
        while (leftmostEntry  <= rightmostEntry) {
            if (ArrayAccess.getInt(rescanMapTable, leftmostEntry) != 0) {
                return;
            }
            leftmostEntry++;
        }
        leftmostEntry = rescanMapTable.length;
    }

    boolean isEmpty() {
        return leftmostEntry == rescanMapTable.length;
    }

    RescanMap() {
    }

    void resetRescanBound() {
        leftmostEntry = rescanMapTable.length;
        rightmostEntry = 0;
    }

    void recordCellForRescan(Pointer cell) {
        Pointer offsetInHeap = cell.minus(coveredAreaStart);
        final int rescanMapIndex = offsetInHeap.unsignedShiftedRight(log2RescanMapIndex).toInt();
        final int markWordIndex = offsetInHeap.asOffset().and(rescanRegionOffsetMask).toInt();

        if (rightmostEntry < rescanMapIndex) {
            rightmostEntry = rescanMapIndex;
        }

        if (leftmostEntry > rescanMapIndex) {
            leftmostEntry = rescanMapIndex;
            ArrayAccess.setInt(rescanMapTable, rescanMapIndex, rescanMapEntry(markWordIndex, markWordIndex + 1));
            return;
        }
        final int currentEntry = ArrayAccess.getInt(rescanMapTable, rescanMapIndex);
        final int leftBound = leftBound(currentEntry);
        if (leftBound > markWordIndex) {
            ArrayAccess.setInt(rescanMapTable, rescanMapIndex, rescanMapEntry(markWordIndex, rightBound(currentEntry)));
        } else if (markWordIndex >= rightBound(currentEntry)) {
            ArrayAccess.setInt(rescanMapTable, rescanMapIndex, rescanMapEntry(leftBound, markWordIndex + 1));
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
        log2RescanMapIndex = log2RescanRegionSize + tricolorHeapMarker.log2BytesCoveredPerBit;
        Size rescanMapSize = tricolorHeapMarker.colorMap.size().unsignedShiftedRight(log2RescanRegionSize);
        rescanRegionOffsetMask = rescanRegionSize - 1;
        Heap.enableImmortalMemoryAllocation();
        rescanMapTable = new int[rescanMapSize.toInt()];
        Heap.disableImmortalMemoryAllocation();
        leftmostEntry = rescanMapTable.length;
        FatalError.check(rescanRegionSize == 1 << log2RescanRegionSize, "RescanMap region size must be a power of 2");
    }
}
