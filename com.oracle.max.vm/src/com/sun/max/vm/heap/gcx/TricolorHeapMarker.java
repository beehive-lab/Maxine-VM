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
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * A marking algorithm that uses a tricolor mark-bitmap with a fixed-size marking stack, an (optional) rescan map, and a
 * finger. The tricolor mark-bitmap encodes three colors using two consecutive bits but consumes as much space overhead as
 * a single-bit mark bitmap, thanks to padding rare tiny objects to guarantee two color bits for every objects.
 * Tracing algorithm uses a single-bit mark bitmap and a fairly large marking stack (from several thousands of references, up
 * hundreds of thousands of references). The reason for the large marking stack is that if this one overflows, tracing must
 * rescan the heap starting from leftmost reference that was on the stack, and must visit every marked object from that point,
 * thus revisiting many objects. The cost of rescan is so overwhelming that a very large marking stack is used to avoid
 * this possibility. The reason for the blind rescan is that with a single bit, one cannot distinguish visited (black) objects from
 * unvisited but live (grey) objects.
 *
 * Every bit maps to a fixed chunk of heap such that every object's first words coincide with one fixed chunk.
 *  Almost all objects have a size larger than the size of a single chunk covered by one bit. Those that don't
 *  (called tiny objects) are segregated or padded (i.e., the heap allocate them the required space to cover 2 bits of the mark bitmap).
 *  Maxine currently aligns objects on a 8-byte word boundary, uses 8-bytes words, and uses a two-words header.
 *
 * The following choices are considered:
 * - each bit corresponds to a single word of the heap; Every object is thus guaranteed two-bit; the mark bitmap consumes 16 Kb
 * per Mb of heap.
 * - each bit corresponds to two words of the heap; Every object larger that 3 words occupies 2 chunks. With this design,
 * the smallest objects can only be allocated 1 bit. Since such objects are generally rare they can be treated
 * specially: e.g., padded to be associated with two bits, or segregated to be allocated in an area covered by a one bit
 * bitmap. Padding is simpler as it allows a unique bitmaps. Maxine's current 8-byte alignment raises another problem
 * with this approach: a chunk can be shared by two objects. This complicates finding the "origin" of an object. The solution
 * is to require objects to be chunk-aligned (i.e., 16-byte aligned) potentially wasting heap space. This would make the
 * mark bitmap consumes 8 Kb / Mb of heap.
 *
 * Which solution is best depends on the amount of space wasted by the 2-words alignment requirement, compared to bitmap
 * space saved. A larger grain also means less time to scan the bitmap. We leave this choice to the heap implementation
 * / collector, which is responsible for aligning object and dealing with small object. For simplicity, we begin here
 * with the first alternative (1 bit per word).
 *
 * Finally, note that when a bit of the color map covers X bytes and objects are X byte-aligned (for X = 8 or 16),
 * the first bit of a color may be either an odd or an even bit, and a color may span
 * two bitmap words. This complicates color search/update operation. A heap allocator may arrange for guaranteeing that
 * an object marks never span a bitmap word by padding a dead object before (Dead objects are special instance of Object
 * whose size is strictly 2 words, regardless of other rules for dealing with tiny objects).
 *
 * An other alternative is to exploit location of an object with respect to the current cursor on the mark bitmap:
 * since object located after the cursor aren't visited yet, we can use the black mark for marking these grey
 * (i.e., an object with the black mark set is black only if located before the finger). In this case, the grey mark is really only used
 * on overflow of the mark stack.
 *
 * This class enables both designs, and provides generic bitmap manipulation that understands color coding and
 * color-oriented operations (i.e., searching grey or black mark, etc.). It provides fast and slow variant of
 * operations, wherein the fast variant assumes that a color never span a bitmap word. The GC is responsible for
 * guaranteeing this property when it uses the fast variant.
 *
 */
public class TricolorHeapMarker implements MarkingStack.OverflowHandler {

    // The color encoding is chosen to optimize the mark bitmaps.
    // The tracing algorithm primarily search for grey objects, 64-bits at a time.
    // By encoding black as 10 and grey as 11, a fast and common way to
    // determine if a 64-bit words holds any grey objects is to compare w & (w <<1).
    // A bit pattern with only black and white objects will result in a 0, whereas a
    // bit pattern with at least one grey object will yield a non zero result, with the
    // leading bit indicating the position of a first grey object.

    // The encoding below follows the following convention:
    // [Heap position] [bitmap word] [bit index in bitmap word].
    // Word 0  0 0
    // Word 1  0 1
    // Word 63 0 63
    // Word 64 1 0
    // Word 65 1 1
    // ...
    // This means that the leading bit of a color is always at a lower position in a bitmap word.
    // Iterating over a word of the bitmap goes from low order bit to high order bit.
    // A mark in the color map is always identified by the bit index of the leading bit.

    /**
     * 2-bit mark for white object (00).
     */
    static final long WHITE = 0L;
    /**
     * 2-bit mark for black object (01).
     */
    static final long BLACK = 1L;
    /**
     * 2-bit mark for grey objects (11).
     */
    static final long GREY = 3L;
    /**
     * Invalid 2-bit mark pattern (10).
     */
    static final long INVALID = 2L;

    static final long COLOR_MASK = 3L;

    static final String [] COLOR_NAMES = new String[] {"WHITE", "BLACK", "INVALID", "GREY"};
    static final char [] COLOR_CHARS = new char[] {'W', 'B', 'I', 'G' };

    static final int LAST_BIT_INDEX_IN_WORD = Word.width() - 1;

    static final int bitIndexInWordMask = LAST_BIT_INDEX_IN_WORD;

    static boolean TraceMarking = false;
    static boolean UseRescanMap;
    static boolean UseDeepMarkStackFlush = true;
    static boolean VerifyAfterMarking = false;
    static boolean VerifyGreyLessAreas = false;
    static {
        VMOptions.addFieldOption("-XX:", "TraceMarking", TricolorHeapMarker.class, "Trace each mark update (Debug mode only)", Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "UseRescanMap", TricolorHeapMarker.class, "Use a rescan map when recovering from mark stack overflow", Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "UseDeepMarkStackFlush", TricolorHeapMarker.class, "Visit flushed cells and mark their reference grey when flushing the mark stack", Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "VerifyAfterMarking", TricolorHeapMarker.class, "Verify absence of grey bits after marking is completed", Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "VerifyGreyLessAreas", TricolorHeapMarker.class, "Verify absence of grey bits in areas that shouldn't have any grey objects", Phase.PRISTINE);
    }

    private static enum MARK_PHASE {
        SCAN_THREADS("T"),
        SCAN_BOOT_HEAP("B"),
        SCAN_CODE("C"),
        SCAN_IMMORTAL("I"),
        VISIT_GREY_FORWARD("V"),
        SPECIAL_REF("W"),
        DONE("D");

        String tag;
        private MARK_PHASE(String tag) {
            this.tag = tag;
        }

        @Override
        final public String toString() {
            return tag;
        }
    }

    /**
     * Return the index within a word of the bit index.
     *
     * @param bitIndex
     * @return
     */
    @INLINE
    static final int bitIndexInWord(int bitIndex) {
        return bitIndex & bitIndexInWordMask;
    }

    @INLINE
    static final long bitmaskFor(int bitIndex) {
        return 1L << bitIndex;
    }

    static final void printVisitedCell(Address cell, String message) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print(message);
        Log.println(cell);
        Log.unlock(lockDisabledSafepoints);
    }

    /**
     * Number of bytes covered by each bit of the bitmaps. Must be a power of 2 of a number of words.
     */
    final int wordsCoveredPerBit;

    /**
     * Log 2 of the number of bytes covered by a bit. Used to compute bit index in the color map from address.
     */
    final int log2BytesCoveredPerBit;
    /**
     * Log 2 to compute the bitmap word index from an offset from the beginning of the covered area.
     */
    final int log2BitmapWord;

    /**
     * Start of the contiguous range of addresses covered by the mark bitmap.
     */
    Address coveredAreaStart;

    /**
     * End of the contiguous range of addresses  covered by the mark bitmap.
     */
    Address coveredAreaEnd;

    /**
     * Finger that points to the rightmost visited (black) object.
     */
    private Pointer blackFinger;

    /**
     * Rightmost marked position.
     */
    private Pointer blackRightmost;

    @INLINE
    public final boolean isCovered(Address address) {
        return address.greaterEqual(coveredAreaStart) && address.lessThan(coveredAreaEnd);
    }

    /**
     * Base of the bitmap biased with offset to the first word's bit. For fast-computing of heap word's color index in
     * the color map.
     */
    private Address biasedBitmapBase;

    /**
     * Memory where the color map is stored.
     */
    final MemoryRegion colorMap;

    /**
     * Shortcut to colorMap.start() for fast bitmap operation.
     */
    private Address base;

    /**
     * The marking stack.
     */
    final MarkingStack markingStack;

    private final TimerMetric rootScanTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric bootHeapScanTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric codeScanTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric immortalSpaceScanTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric heapMarkingTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    final TimerMetric recoveryScanTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric weakRefTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));

    /**
     * Counter of the number of recovery overflow scheduled during the current mark. For statistics purposes.
     */
    private int totalRecoveryScanCount = 0;
    private long totalRecoveryElapsedTime = 0L;

    private boolean traceGCTimes = false;

    private MARK_PHASE markPhase = MARK_PHASE.DONE;


    private static String colorName(long color) {
        return COLOR_NAMES[(int) color & 0x3];
    }
    private static char colorChar(long color) {
        return COLOR_CHARS[(int) color & 0x3];
    }

    private void traceMark(Address cell,  long color, int bitIndex) {
        final int bwi = bitmapWordIndex(bitIndex);
        Log.print("#mark ");
        Log.print(markPhase);
        Log.print(" ");
        Log.print(colorChar(color));
        Log.print(" ");
        Log.print(cell);
        Log.print(" bi: ");
        Log.print(bitIndex);
        Log.print(" [ ");
        Log.print(bwi);
        Log.print(" ] @ ");
        Log.println(bitmapWordPointerAt(bitIndex));
    }

    private void traceMark(Address cell,  long color) {
        traceMark(cell, color, bitIndexOf(cell));
    }

    @INLINE
    Pointer colorMapBase() {
        return base.asPointer();
    }

    @INLINE
    final public void traceGreyMark(Address cell, int bitIndex) {
        if (MaxineVM.isDebug() && TraceMarking) {
            traceMark(cell, GREY, bitIndex);
        }
    }

    @INLINE
    final public void traceBlackMark(Address cell, int bitIndex) {
        if (MaxineVM.isDebug() && TraceMarking) {
            traceMark(cell, BLACK, bitIndex);
        }
    }

    void startTimer(Timer timer) {
        if (traceGCTimes) {
            timer.start();
        }
    }
    void stopTimer(Timer timer) {
        if (traceGCTimes) {
            timer.stop();
        }
    }

    public void reportLastElapsedTimes() {
        Log.print("root scan=");
        Log.print(rootScanTimer.getLastElapsedTime());
        Log.print(", boot heap scan=");
        Log.print(bootHeapScanTimer.getLastElapsedTime());
        Log.print(", code scan=");
        Log.print(codeScanTimer.getLastElapsedTime());
        Log.print(", marking=");
        Log.print(heapMarkingTimer.getLastElapsedTime());
        Log.print(", marking stack overflow (");
        Log.print(recoveryScanTimer.getCount());
        Log.print(") =");
        Log.print(recoveryScanTimer.getElapsedTime());
        Log.print(", weak refs=");
        Log.print(weakRefTimer.getLastElapsedTime());
    }

    public void reportTotalElapsedTimes() {
        Log.print("root scan=");
        Log.print(rootScanTimer.getElapsedTime());
        Log.print(", boot heap scan=");
        Log.print(bootHeapScanTimer.getElapsedTime());
        Log.print(", code scan=");
        Log.print(codeScanTimer.getElapsedTime());
        Log.print(", marking=");
        Log.print(heapMarkingTimer.getElapsedTime());
        Log.print(", marking stack overflow (");
        Log.print(totalRecoveryScanCount);
        Log.print(") =");
        Log.print(totalRecoveryElapsedTime);
        Log.print(", weak refs=");
        Log.print(weakRefTimer.getElapsedTime());
    }

    /**
     * Return the size in bytes required for a tricolor mark bitmap to cover a contiguous
     * heap area of the specified size.
     *
     * @param coveredAreaSize the size in bytes of the heap area covered by the tricolor mark bitmap
     * @return the size a tricolor mark bitmap should have to cover the specified area size.
     */
    Size bitmapSize(Size coveredAreaSize) {
        // The mark bitmap uses a single bit to cover wordsCoveredPerBit words.
        // num bits in bitmaps = coveredAreaSize >> log2BytesCoveredPerBit
        // bitmap size in bytes = num bits >> log2numberOfBits per byte
        FatalError.check(coveredAreaSize.isWordAligned(), "Area covered by a mark bitmap must be word aligned");
        Size numberOfBytesNeeded = coveredAreaSize.unsignedShiftedRight(log2BytesCoveredPerBit + WordWidth.BITS_8.log2numberOfBits);

        // Mark bitmap must be word-aligned (the marking algorithm operates on mark bitmap words)
        // We also add an extra-word at the end to allow termination of the marking algorithm:
        return numberOfBytesNeeded.alignUp(Word.size()).plus(Word.size());
    }


    /**
     * Returns max amount of memory needed for a max heap size.
     * Let the HeapScheme decide where to allocate.
     *
     * @param maxHeapSize the maximum size, in bytes, of the heap
     * @return
     */
    public Size memoryRequirement(Size maxHeapSize) {
        return bitmapSize(maxHeapSize);
    }

    public void setCoveredArea(Address start, Address end) {
        coveredAreaStart = start;
        coveredAreaEnd = end;
    }

    /**
     *
     */
    public TricolorHeapMarker(int wordsCoveredPerBit, RootCellVisitor rootCellVisitor)  {
        this.wordsCoveredPerBit = wordsCoveredPerBit;
        log2BytesCoveredPerBit = Word.widthValue().log2numberOfBytes + Integer.numberOfTrailingZeros(wordsCoveredPerBit);
        assert wordsCoveredPerBit * Word.widthValue().numberOfBytes == 1 << log2BytesCoveredPerBit;
        log2BitmapWord = log2BytesCoveredPerBit + Word.widthValue().log2numberOfBits;
        colorMap = new MemoryRegion("Mark Bitmap");
        markingStack = new MarkingStack();
        markingStack.setOverflowHandler(this);
        this.rootCellVisitor = rootCellVisitor;
        rootCellVisitor.initialize(this);
        heapRootsScanner = new SequentialHeapRootsScanner(rootCellVisitor);
    }

    /**
     * Initialize a tricolor mark bitmap for the covered area. The mark-bitmap is generated at VM startup.
     *
     * @param bitmapStorage
     * @param coveredArea
     */
    public void initialize(Address start, Address end, Address bitmapStorage, Size bitmapSize) {
        if (MaxineVM.isDebug()) {
            Size coveredAreaSize = end.minus(start).asSize();
            FatalError.check(bitmapSize.toLong() >= bitmapSize(coveredAreaSize).toLong(), "Mark bitmap too small to cover heap");
        }
        setCoveredArea(start, end);
        colorMap.setStart(bitmapStorage);
        colorMap.setSize(bitmapSize);
        base = bitmapStorage;
        final int baseBias = start.unsignedShiftedRight(log2BitmapWord).toInt();
        biasedBitmapBase = colorMap.start().minus(baseBias);
        if (UseRescanMap) {
            overflowScanState = overflowScanWithRescanMapState;
        } else {
            overflowScanState = overflowLinearScanState;
        }
        overflowScanState.initialize();
    }

    // Address to bitmap word / bit index operations.

    @INLINE
    final boolean colorSpanWords(int bitIndex) {
        return bitIndexInWord(bitIndex) == LAST_BIT_INDEX_IN_WORD;
    }

    // Pointer to the word in the bitmap containing the bit for the specified address.
    @INLINE
    final Pointer bitmapWordPointerOf(Address addr) {
        return biasedBitmapBase.asPointer().plus(addr.unsignedShiftedRight(log2BitmapWord));
    }

    /**
     * Index in the color map to the word containing the bit at the specified index.
     * @param bitIndex a bit index.
     * @return an index to a word of the color map.
     */
    @INLINE
    final int bitmapWordIndex(int bitIndex) {
        return bitIndex >> Word.widthValue().log2numberOfBits;
    }

    /**
     * Index in the color map to the word containing the first mark bit corresponding to the specified address.
     * @param address An address in the covered area.
     * @return an index to a word of the color map.
     */
    @INLINE
    final int bitmapWordIndex(Address address) {
        return address.minus(coveredAreaStart).unsignedShiftedRight(log2BitmapWord).toInt();
    }

    @INLINE
    final long bitmapWordAt(int bitIndex) {
        return base.asPointer().getLong(bitmapWordIndex(bitIndex));
    }

    /**
     *  Pointer to the bitmap word in the color map containing the bit at specified bit index.
     * @param bitIndex a bit index
     * @return a pointer to a word of the color map
  */
    @INLINE
    final Pointer bitmapWordPointerAt(int bitIndex) {
        return base.asPointer().plus(bitmapWordIndex(bitIndex) << Word.widthValue().log2numberOfBytes);
    }

    /**
     *  Bit index in the bitmap for the address into the covered area.
     */
    @INLINE
    final int bitIndexOf(Address address) {
        return address.minus(coveredAreaStart).unsignedShiftedRight(log2BytesCoveredPerBit).toInt();
    }

    @INLINE
    final Address addressOf(int bitIndex) {
        return coveredAreaStart.plus(bitIndex << log2BytesCoveredPerBit);
    }

    @INLINE
    final void markGrey_(int bitIndex) {
        FatalError.check(!colorSpanWords(bitIndex), "Color must not cross word boundary.");
        final int wordIndex = bitmapWordIndex(bitIndex);
        final Pointer basePointer = base.asPointer();
        basePointer.setLong(wordIndex, basePointer.getLong(wordIndex) | (GREY << bitIndexInWord(bitIndex)));
    }

    @INLINE
    final void markGrey_(Address cell) {
        markGrey_(bitIndexOf(cell));
    }


    @INLINE
    final void markGrey(int bitIndex) {
        if (!colorSpanWords(bitIndex)) {
            markGrey_(bitIndex);
        } else {
            // Color span words.
            final Pointer basePointer = base.asPointer();
            int wordIndex = bitmapWordIndex(bitIndex);
            basePointer.setLong(wordIndex, basePointer.getLong(wordIndex) | bitmaskFor(LAST_BIT_INDEX_IN_WORD));
            wordIndex++;
            basePointer.setLong(wordIndex, basePointer.getLong(wordIndex) | 1L);
        }
    }

    @INLINE
    final void markGrey(Address cell) {
        final int bitIndex = bitIndexOf(cell);
        traceGreyMark(cell, bitIndex);
        markGrey(bitIndex);
    }

    @INLINE
    final void markBlackFromWhite(int bitIndex) {
        final int wordIndex = bitmapWordIndex(bitIndex);
        final Pointer basePointer = base.asPointer();
        basePointer.setLong(wordIndex, basePointer.getLong(wordIndex) | (BLACK << bitIndexInWord(bitIndex)));
    }

    @INLINE
    final void markBlackFromWhite(Address cell) {
        markBlackFromWhite(bitIndexOf(cell));
    }

    final boolean markGreyIfWhite(Pointer cell) {
        final int bitIndex = bitIndexOf(cell);
        if (isWhite(bitIndex)) {
            traceGreyMark(cell, bitIndex);
            markGrey(bitIndex);
            return true;
        }
        return false;
    }

    @INLINE
    final boolean markBlackIfWhite(Pointer cell) {
        return markBlackIfWhite(bitIndexOf(cell));
    }

    /**
     * Mark cell corresponding to this bit index black if white.
     * @param bitIndex bit index corresponding to the address of a cell in the heap covered area.
     * @return true if the mark was white.
     */
    @INLINE
    final boolean markBlackIfWhite(int bitIndex) {
        final Pointer basePointer = base.asPointer();
        final int wordIndex = bitmapWordIndex(bitIndex);
        final long bitmask = bitmaskFor(bitIndex);
        final long bitmapWord = basePointer.getLong(wordIndex);
        if ((bitmapWord & bitmask) == 0) {
            // Is white. Mark black.
            basePointer.setLong(wordIndex, bitmapWord | bitmask);
            return true;
        }
        return false;
    }

    @INLINE
    final void markBlackFromGrey(int bitIndex) {
        final Pointer basePointer = base.asPointer();
        // Only need to clear the second bit. No need to worry about the color crossing a word boundary here.
        final int greyBitIndex = bitIndex + 1;
        final int wordIndex = bitmapWordIndex(greyBitIndex);
        basePointer.setLong(wordIndex, basePointer.getLong(wordIndex) & ~bitmaskFor(greyBitIndex));
    }

    @INLINE
    final void markBlackFromGrey(Address cell) {
        final int bitIndex = bitIndexOf(cell);
        traceBlackMark(cell, bitIndex);
        markBlackFromGrey(bitIndex);
    }

    @INLINE
    final boolean isColor_(int bitIndex, long color) {
        FatalError.check(!colorSpanWords(bitIndex), "Color must not cross word boundary.");
        final long bitmapWord = bitmapWordAt(bitIndex);
        return ((bitmapWord >>> bitIndexInWord(bitIndex)) & COLOR_MASK) == color;
    }

    final boolean isGrey(int bitIndex) {
        int bitIndexInWord = bitIndexInWord(bitIndex);
        if (bitIndexInWord == LAST_BIT_INDEX_IN_WORD) {
            return (bitmapWordAt(bitIndex + 1) & 1L) != 0;
        }
        return (bitmapWordAt(bitIndex) & bitmaskFor(bitIndexInWord + 1)) != 0;
    }

    /**
     * Check the color map for a white object. Thanks to the color encoding, it only needs to
     * check the lowest bit of the two-bit color, i.e., the bit corresponding to the cell address. As a result, no special care is needed
     * if the object's color spans two words of the color map.
     *
     * @param bitIndex an index in the color map corresponding to the first word of an object in the heap.
     * @return true if the object is white.
     */
    @INLINE
    final boolean isWhite(int bitIndex) {
        return (bitmapWordAt(bitIndex) & bitmaskFor(bitIndex)) == 0;
    }

    @INLINE
    final boolean isWhite(Pointer cell) {
        return isWhite(bitIndexOf(cell));
    }

    @INLINE
    final boolean isBlack_(int bitIndex) {
        return isColor_(bitIndex, BLACK);
    }

    @INLINE
    final boolean isGrey_(int bitIndex) {
        return isColor_(bitIndex, GREY);
    }

    @INLINE
    final boolean isBlack_(Pointer cell) {
        return isBlack_(bitIndexOf(cell));
    }

    @INLINE
    final boolean isGrey_(Pointer cell) {
        return isGrey_(bitIndexOf(cell));
    }

    /**
     * Only used when tracing is completed. There should be no grey objects left.
     * @param bitIndex
     * @return true if the mark at the specified bit index is black
     */
    @INLINE
    final boolean isBlackWhenNoGreys(int bitIndex) {
        final Pointer basePointer = base.asPointer();
        final int wordIndex = bitmapWordIndex(bitIndex);
        final long bitmask = bitmaskFor(bitIndex);
        final long bitmapWord = basePointer.getLong(wordIndex);
        if ((bitmapWord & bitmask) == 0L) {
            return false;
        }
        if (MaxineVM.isDebug()) {
            // Mustn't be grey
            final int greyBitIndex = bitIndex + 1;
            final long greymask =  bitmaskFor(greyBitIndex);
            if ((bitmapWordAt(greyBitIndex) & greymask) != 0L) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("grey bit ");
                Log.print(greyBitIndex);
                Log.print(" in bitmap word ");
                Log.print(bitmapWordIndex(bitIndex));
                Log.print(" cells @ ");
                Log.println(addressOf(bitIndex));
                Log.unlock(lockDisabledSafepoints);
                FatalError.unexpected("Must have no grey marks");
            }
        }
        return true;
    }

    final boolean isBlackWhenNotWhite(int blackBitIndex) {
        // Only need to check the grey bit
        final int greyBitIndex = blackBitIndex + 1;
        final long bitmask = bitmaskFor(greyBitIndex);
        final long bitmapWord = bitmapWordAt(greyBitIndex);
        if ((bitmapWord & bitmask) == 0L) {
            if (MaxineVM.isDebug()) {
                // Mustn't be white
                FatalError.check((bitmapWordAt(blackBitIndex) & bitmaskFor(blackBitIndex)) != 0L, "Must have a black mark");
            }
            // Grey bit not set.
            return true;
        }
        return false;
    }

    @INLINE
    final boolean isBlackWhenNotWhite(Pointer cell) {
        return isBlackWhenNotWhite(bitIndexOf(cell));
    }

    /**
     * Only used when tracing is completed. There should be no grey objects left.
     * @param cell
     * @return true if the object at the specified address is black
     */
    @INLINE
    final boolean isBlackWhenNoGreys(Pointer cell) {
        return isBlackWhenNoGreys(bitIndexOf(cell));
    }

    /**
     * Clear the color map, i.e., turn all bits to white.
     */
    void clearColorMap() {
        Memory.clearWords(colorMap.start().asPointer(), colorMap.size().toInt() >> Word.widthValue().log2numberOfBytes);
    }


    private final RootCellVisitor rootCellVisitor;

    public RootCellVisitor rootCellVisitor() {
        return rootCellVisitor;
    }

    abstract static class MarkingStackFlusher extends MarkingStack.MarkingStackCellVisitor {
        ColorMapScanState scanState;

        final void setScanState(ColorMapScanState scanState) {
            this.scanState = scanState;
        }

        abstract Address flushMarkingStack();

        @Override
        void visitPoppedCell(Pointer cell) {
            scanState.markAndVisitPoppedCell(cell);
        }
    }

    static final class BaseMarkingStackFlusher extends MarkingStackFlusher {
        Address leftmostFlushed;

        @Override
        Address flushMarkingStack() {
            leftmostFlushed = scanState.rightmost;
            scanState.heapMarker.markingStack.flush();
            return leftmostFlushed;
        }

        @Override
        void visitFlushedCell(Pointer cell) {
            // Record leftmost mark.
            // Note: references on the marking stack were already
            // marked grey to avoid storing multiple times the same reference.
            if (cell.lessThan(leftmostFlushed)) {
                leftmostFlushed = cell;
            }
        }

    }

    static class FlushingIndexVisitor extends PointerIndexVisitor {
        final TricolorHeapMarker heapMarker;
        /**
         * Rightmost cell marked in the covered area.
         */
        Address rightmost;

        Address finger;

        Address leftmostFlushed;

        FlushingIndexVisitor(TricolorHeapMarker heapMarker) {
            this.heapMarker = heapMarker;
        }

        /**
         * Mark the object at the specified address grey.
         *
         * @param cell
         */
        @INLINE
        private void markObjectGrey(Pointer cell) {
            if (cell.greaterThan(finger)) {
                // Object is after the finger. Mark grey and update rightmost if white.
                if (heapMarker.markGreyIfWhite(cell) && cell.greaterThan(rightmost)) {
                    rightmost = cell;
                }
            } else if (cell.greaterEqual(heapMarker.coveredAreaStart) && heapMarker.markGreyIfWhite(cell) && cell.lessThan(leftmostFlushed)) {
                leftmostFlushed = cell;
            }
        }

        @INLINE
        private void markRefGrey(Reference ref) {
            markObjectGrey(Layout.originToCell(ref.toOrigin()));
        }

        @Override
        public void visit(Pointer pointer, int wordIndex) {
            markRefGrey(pointer.getReference(wordIndex));
        }

        @INLINE
        final void visitFlushedCell(Pointer cell) {
            final int bitIndex = heapMarker.bitIndexOf(cell);
            // Due to how grey mark are being scanned, we may end up with black objects on the marking stack.
            // We filter them out here. See comments in ForwardScan.visitGreyObjects
            if (heapMarker.isBlackWhenNotWhite(bitIndex)) {
                if (Heap.traceGC()) {
                    printVisitedCell(cell, "Skip black flushed cell ");
                }
                return;
            }

            // Visit the flushed cell and mark all it's white references grey, then mark the object black.
            // This helps avoiding going in tight loop of flushing/recovering when
            // a large object of backward references is visited.
            // Note: references on the marking stack were already
            // marked grey to avoid storing multiple times the same reference.
            if (Heap.traceGC()) {
                printVisitedCell(cell, "Visiting flushed cell ");
            }

            final Pointer origin = Layout.cellToOrigin(cell);
            final Reference hubRef = Layout.readHubReference(origin);
            markRefGrey(hubRef);
            final Hub hub = UnsafeCast.asHub(hubRef.toJava());
            if (MaxineVM.isDebug()) {
                FatalError.check(hub != HeapFreeChunk.HEAP_FREE_CHUNK_HUB, "Must never mark a HeapFreeChunk");
            }
            // Update the other references in the object
            final SpecificLayout specificLayout = hub.specificLayout;
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, this);
                if (hub.isJLRReference) {
                    SpecialReferenceManager.discoverSpecialReference(cell);
                }
            } else if (specificLayout.isHybridLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, this);
            } else if (specificLayout.isReferenceArrayLayout()) {
                final int length = Layout.readArrayLength(origin);
                for (int index = 0; index < length; index++) {
                    markRefGrey(Layout.getReference(origin, index));
                }
            }
            heapMarker.traceBlackMark(cell, bitIndex);
            heapMarker.markBlackFromGrey(bitIndex);
        }

        Address flushMarkingStack(ColorMapScanState scanState) {
            rightmost = scanState.rightmost;
            finger = scanState.finger;
            leftmostFlushed = finger;
            heapMarker.markingStack.flush();
            scanState.rightmost = rightmost;
            if (MaxineVM.isDebug() && Heap.traceGC()) {
                Log.print(" leftmost flushed: ");
                Log.println(leftmostFlushed);
                Log.print(" rightmost after flush: ");
                Log.println(rightmost);
                Log.print(" finger after flush: ");
                Log.println(finger);
            }
            return leftmostFlushed;
        }

    }

    static final class DeepMarkingStackFlusher extends MarkingStackFlusher {
        final FlushingIndexVisitor flushingCellVisitor;
        DeepMarkingStackFlusher(TricolorHeapMarker heapMarker) {
            flushingCellVisitor = new FlushingIndexVisitor(heapMarker);
        }

        @Override
        void visitFlushedCell(Pointer cell) {
            flushingCellVisitor.visitFlushedCell(cell);
        }

        @Override
        Address flushMarkingStack() {
            return flushingCellVisitor.flushMarkingStack(scanState);
        }
    }

    static final class MarkingStackWithRescanMapCellVisitor extends MarkingStackFlusher {
        final RescanMap rescanMap;

        MarkingStackWithRescanMapCellVisitor(RescanMap rescanMap) {
            this.rescanMap = rescanMap;
        }

        @Override
        Address flushMarkingStack() {
            scanState.heapMarker.markingStack.flush();
            return Address.zero();
        }

        @Override
        public void visitFlushedCell(Pointer cell) {
            rescanMap.recordCellForRescan(cell);
        }
    }


    abstract static class ColorMapScanState extends PointerIndexVisitor {
        final TricolorHeapMarker heapMarker;

        ColorMapScanState(TricolorHeapMarker heapMarker) {
            this.heapMarker = heapMarker;
        }
        /**
         * Current position of this scan in the covered area.
         */
        Address finger;
        /**
         * Rightmost cell marked in the covered area.
         */
        Address rightmost;

        /**
         * Counter of the number of marking stack overflow this scan went through.
         * Serves only debugging and statistics purposes.
         */
        int numMarkinkgStackOverflow;

        /**
         * Mark the object at the specified address grey.
         *
         * @param cell
         */
        @INLINE
        private void markObjectGrey(Pointer cell) {
            if (cell.greaterThan(finger)) {
                // Object is after the finger. Mark grey and update rightmost if white.
                if (heapMarker.markGreyIfWhite(cell) && cell.greaterThan(rightmost)) {
                    rightmost = cell;
                }
            } else if (cell.greaterEqual(heapMarker.coveredAreaStart) && heapMarker.markGreyIfWhite(cell)) {
                heapMarker.markingStack.push(cell);
            }
        }

        @INLINE
        private void markRefGrey(Reference ref) {
            markObjectGrey(Layout.originToCell(ref.toOrigin()));
        }

        @Override
        public void visit(Pointer pointer, int wordIndex) {
            markRefGrey(pointer.getReference(wordIndex));
        }

        public void visitArrayReferences(Pointer origin) {
            final int length = Layout.readArrayLength(origin);
            for (int index = 0; index < length; index++) {
                markRefGrey(Layout.getReference(origin, index));
            }
        }

        public void visit(Reference ref) {
            markRefGrey(ref);
        }

        @INLINE
        private Pointer visitGreyCell(Pointer cell) {
            if (Heap.traceGC()) {
                printVisitedCell(cell, "Visiting grey cell ");
            }
            final Pointer origin = Layout.cellToOrigin(cell);
            final Reference hubRef = Layout.readHubReference(origin);
            markRefGrey(hubRef);
            final Hub hub = UnsafeCast.asHub(hubRef.toJava());
            if (MaxineVM.isDebug()) {
                FatalError.check(hub != HeapFreeChunk.HEAP_FREE_CHUNK_HUB, "Must never mark a HeapFreeChunk");
            }
            final SpecificLayout specificLayout = hub.specificLayout;
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, this);
                if (hub.isJLRReference) {
                    // The marking stack might have overflow before reaching this point, and doing so, it
                    // might have already register this reference to the SpecialReferenceManager
                    // (e.g., if using deep mark stack flush).
                    // Hence, we may end up calling discoverSpecialReference twice which would cause an error (SpecialReferenceManager
                    // allows for a single call only). We need to protect against this, so we test here if
                    // the object wasn't set black already.
                    if (!heapMarker.isBlackWhenNotWhite(origin)) {
                        SpecialReferenceManager.discoverSpecialReference(cell);
                    }
                }
                return cell.plus(hub.tupleSize);
            }
            if (specificLayout.isHybridLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, this);
            } else if (specificLayout.isReferenceArrayLayout()) {
                visitArrayReferences(origin);
            }
            return cell.plus(Layout.size(origin));
        }

        abstract  int rightmostBitmapWordIndex();

        Pointer markAndVisitCell(Pointer cell) {
            finger = cell;
            final Pointer endOfCell = visitGreyCell(cell);
            heapMarker.markBlackFromGrey(cell);
            return endOfCell;
        }

        void markAndVisitPoppedCell(Pointer cell) {
            int bitIndex = heapMarker.bitIndexOf(cell);
            // Due to how grey mark are being scanned, we may end up with black objects on the marking stack.
            // We filter them out here. See comments in visitGreyObjects
            if (heapMarker.isBlackWhenNotWhite(bitIndex)) {
                return;
            }
            if (MaxineVM.isDebug() && Heap.traceGC()) {
                printVisitedCell(cell, "Visiting popped cell ");
            }
            visitGreyCell(cell);
            heapMarker.traceBlackMark(cell, bitIndex);
            heapMarker.markBlackFromGrey(bitIndex);
        }

        public abstract void visitGreyObjects();

        void printState() {
            Log.print("finger:");
            Log.println(finger);
            Log.print("rightmost:");
            Log.println(rightmost);
            Log.print("#mark stack overflows:");
            Log.println(numMarkinkgStackOverflow);
        }
    }

    static final class ForwardScanState extends ColorMapScanState implements SpecialReferenceManager.GC {

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

        void visitGreyObjects(int rightmostBitmapWordIndex) {
            final Pointer colorMapBase = heapMarker.base.asPointer();

            int bitmapWordIndex = heapMarker.bitmapWordIndex(finger);
            while (bitmapWordIndex <= rightmostBitmapWordIndex) {
                long bitmapWord = colorMapBase.getLong(bitmapWordIndex);
                if (bitmapWord != 0) {
                    // FIXME (ld) this way of scanning the mark bitmap may cause black objects to end up on the marking stack.
                    // Here's how.
                    // If the object pointed by the finger contains backward references to objects covered by the same word
                    // of the mark bitmap, and its end is covered by the same word, we will end up visiting these objects although
                    // there were pushed on the marking stack.
                    // One way to avoid that is to leave the finger set to the beginning of the word and iterate over all grey marks
                    // of the word until reaching a fix point where all mark are white or black on the mark bitmap word.
                    final long greyMarksInWord = bitmapWord & (bitmapWord >>> 1);
                    if (greyMarksInWord != 0) {
                        // First grey mark is the least set bit.
                        final int bitIndexInWord = Pointer.fromLong(greyMarksInWord).leastSignificantBitSet();
                        final int bitIndexOfGreyCell = (bitmapWordIndex << Word.widthValue().log2numberOfBits) + bitIndexInWord;
                        final Pointer p = markAndVisitCell(heapMarker.addressOf(bitIndexOfGreyCell).asPointer());
                        // Get bitmap word index at the end of the object. This may avoid reading multiple mark bitmap words
                        // when marking objects crossing multiple mark bitmap words.
                        bitmapWordIndex = heapMarker.bitmapWordIndex(p);
                        continue;
                    } else if ((bitmapWord >>> LAST_BIT_INDEX_IN_WORD) == 1L) {
                        // Mark span two words. Check first bit of next word to decide if mark is grey.
                        bitmapWord = colorMapBase.getLong(bitmapWordIndex + 1);
                        if ((bitmapWord & 1) != 0) {
                            // it is a grey object.
                            final int bitIndexOfGreyCell = (bitmapWordIndex << Word.widthValue().log2numberOfBits) + LAST_BIT_INDEX_IN_WORD;
                            final Pointer p = markAndVisitCell(heapMarker.addressOf(bitIndexOfGreyCell).asPointer());
                            bitmapWordIndex = heapMarker.bitmapWordIndex(p);
                            continue;
                        }
                    }
                }
                bitmapWordIndex++;
            }
            // There might be some objects left in the marking stack. Drain it.
            heapMarker.markingStack.drain();
        }

        public void visitGreyObjects(HeapRegionRangeIterable regionsRanges) {
            final int log2RegionToBitmapWord = HeapRegionConstants.log2RegionSizeInBytes - heapMarker.log2BitmapWord;

            while (regionsRanges.hasNext()) {
                final int fingerBitmapWordIndex = heapMarker.bitmapWordIndex(finger);
                final RegionRange regionRange = regionsRanges.next();
                final int firstRegion = regionRange.firstRegion();
                final int rangeRightmostBitmapWordIndex = (firstRegion + regionRange.numRegions()) << log2RegionToBitmapWord;
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
                    final int b = rightmostBitmapWordIndex();
                    if (b <= rightmostBitmapWordIndex) {
                        // We reached the right most mark. No need to continue iterating over regions.
                        return;
                    }
                    if (rightmostBitmapWordIndex ==  rangeRightmostBitmapWordIndex) {
                        // We're done with the current regions range. Break
                        // to the outer loop to iterate over subsequent region ranges.
                        break;
                    }
                    // Update rightmost for the next iterate over the current range.
                    rightmostBitmapWordIndex = b > rangeRightmostBitmapWordIndex ? rangeRightmostBitmapWordIndex : b;
                } while(true);
            }
            // There might be some objects left in the marking stack. Drain it.
            heapMarker.markingStack.drain();
        }

        @Override
        public void visitGreyObjects() {
            int rightmostBitmapWordIndex = rightmostBitmapWordIndex();
            do {
                visitGreyObjects(rightmostBitmapWordIndex);
                // Rightmost may have been updated. Check for this, and loop back if it has.
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
            // Tmp traces. REMOVE / add as debug code
            if (!heapMarker.isWhite(ref.toOrigin())) {
                Log.print("preserving a non-white reference : ");
                Log.println(ref.toOrigin());
            }
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

    final ForwardScanState forwardScanState = new ForwardScanState(this);
    /**
     * Scanning state when recovering from a marking state overflow.
     * Set to either {@link #overflowLinearScanState} or {@link #overflowScanWithRescanMapState}
     * depending on whether a rescan map is used.
     */
    private OverflowScanState overflowScanState;
    private final OverflowLinearScanState overflowLinearScanState = new OverflowLinearScanState(this);
    private final OverflowScanWithRescanMapState overflowScanWithRescanMapState = new OverflowScanWithRescanMapState(this);

    /**
     * Indicates whether we're recovering from a marking stack overflow
     * (i.e., a scan of the marking stack in recovery mode is initiated).
     */
    ColorMapScanState currentScanState = forwardScanState;

    /**
     * Scanning of strong roots external to the heap and boot region (namely, thread stacks and live monitors).
     */
    private final SequentialHeapRootsScanner heapRootsScanner;

    void markBootHeap() {
        Heap.bootHeapRegion.visitReferences(rootCellVisitor);
    }


    void markCode() {
        // References in the boot code region are immutable and only ever refer
        // to objects in the boot heap region.
        // Setting includeBootCode to false tells to scan non-boot code regions only.
        boolean includeBootCode = false;
        Code.visitCells(rootCellVisitor, includeBootCode);
    }

    void markImmortalHeap() {
        ImmortalHeap.visitCells(rootCellVisitor);
    }

    /**
     * Marking roots.
     * The boot region needs special treatment. Currently, the image generator generates a map of references in the boot
     * image, and all of its object are considered as root. This leaves the following options: 1. Walk once over the
     * boot image on first GC to mark permanently all boot objects black. However, the boot image generator needs to
     * follow the padding rules of the GC (which may be constraining if we want to be able to select another GC with
     * different padding rules at startup time. (Due to the encoding of colors, we cannot just mark bit to 1). 2.
     * Perform boundary check to ignore references to the boot region. In this case, we don't need to cover the boot
     * image with the mark bitmap. 3. Same as 2, but we cover the boot region with the mark bitmap, and we mark any boot
     * object we come across as black, and leave the mark permanently (i.e., only the heap space get's cleared at GC
     * start).
     *
     * We implement option 2 for now.
     *
     * The algorithm mark roots grey they linearly scan the mark bitmap looking for grey mark. A finger indicates the
     * current position on the heap: the goal is to limit random access when following lives objects. To this end,
     * references to white objects from the "fingered" object after the finger are marked grey and will be inspected
     * when the finger comes across them. White objects before the finger are entered into a tiny marking stack, which
     * is drained whenever reaching a drain threshold.
     */
    public void markRoots() {
        rootCellVisitor.reset();
        if (Heap.traceGCPhases()) {
            Log.println("Scanning external roots...");
        }
        // Mark all out of heap roots first (i.e., thread).
        // This only needs setting grey marks blindly (there are no black mark at this stage).
        startTimer(rootScanTimer);
        markPhase = MARK_PHASE.SCAN_THREADS;
        heapRootsScanner.run();
        stopTimer(rootScanTimer);

        // Next, mark all reachable from the boot area.
        if (Heap.traceGCPhases()) {
            Log.println("Marking roots from boot heap...");
        }
        startTimer(bootHeapScanTimer);
        markPhase = MARK_PHASE.SCAN_BOOT_HEAP;
        markBootHeap();
        stopTimer(bootHeapScanTimer);

        if (Heap.traceGCPhases()) {
            Log.println("Marking roots from code...");
        }
        startTimer(codeScanTimer);
        markPhase = MARK_PHASE.SCAN_CODE;
        markCode();
        stopTimer(codeScanTimer);

        if (Heap.traceGCPhases()) {
            Log.println("Marking roots from immortal heap...");
        }
        startTimer(immortalSpaceScanTimer);
        markPhase = MARK_PHASE.SCAN_IMMORTAL;
        markImmortalHeap();
        stopTimer(immortalSpaceScanTimer);
    }

    /*
     * Helper instance variables for debugging purposes only.
     * Easier to track than local variables when under the inspector.
     */
    private Pointer debugCursor;
    private Pointer debugGapLeftObject;
    private Pointer debugGapRightObject;
    private int debugBitmapWordIndex;
    private int debugBitIndex;
    private int debugRightmostBitmapWordIndex;

    /**
     * Return bit index in the color map of the first grey object in the specified area or -1 if none
     * is found.
     * @param start start of the scanned area
     * @param end end of the scanned area
     * @return a bit index in the color map, or -1 if no grey object was met during the scan.
     */
    public int scanForGreyMark(Address start, Address end) {
        Pointer p = start.asPointer();
        while (p.lessThan(end)) {
            if (MaxineVM.isDebug()) {
                debugCursor = p;
                final Pointer origin = Layout.cellToOrigin(p);
                final Hub hub = UnsafeCast.asHub(Layout.readHubReference(origin).toJava());
                if (hub == HeapFreeChunk.HEAP_FREE_CHUNK_HUB) {
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.print("Found chunk at ");
                    Log.println(p);
                    Log.unlock(lockDisabledSafepoints);
                    FatalError.unexpected("Must not have FreeHeapChunk when tracing");
                }
            }
            final int bitIndex = bitIndexOf(p);
            if (isGrey(bitIndex)) {
                return bitIndex;
            }
            p = p.plus(Layout.size(Layout.cellToOrigin(p)));
            if (MaxineVM.isDebug() && p.readWord(0).isZero()) {
                Log.print(" suspiscious obj"); Log.print(debugCursor); Log.print(" size = ");
                Log.println(Layout.size(Layout.cellToOrigin(debugCursor)).toLong());
            }
        }
        return -1;
    }

    /**
     * Search the tricolor mark bitmap for a grey object in a specific region of the heap.
     * @param start start of the heap region.
     * @param end end of the heap region.
     * @return Return the bit index to the first grey mark found if any, -1 otherwise.
     */
    public int firstGreyMark(Pointer start, Pointer end) {
        final Pointer colorMapBase = base.asPointer();
        final int lastBitIndex = bitIndexOf(end);
        final int lastBitmapWordIndex = bitmapWordIndex(lastBitIndex);
        int bitmapWordIndex = bitmapWordIndex(bitIndexOf(start));
        while (bitmapWordIndex <= lastBitmapWordIndex) {
            long bitmapWord = colorMapBase.getLong(bitmapWordIndex);
            if (bitmapWord != 0) {
                final long greyMarksInWord = bitmapWord & (bitmapWord >>> 1);
                if (greyMarksInWord != 0) {
                    // First grey mark is the least set bit.
                    final int bitIndexInWord = Pointer.fromLong(greyMarksInWord).leastSignificantBitSet();
                    final int bitIndexOfGreyCell = (bitmapWordIndex << Word.widthValue().log2numberOfBits) + bitIndexInWord;
                    return bitIndexOfGreyCell < lastBitIndex ? bitIndexOfGreyCell : -1;
                } else if ((bitmapWord >>> LAST_BIT_INDEX_IN_WORD) == 1L) {
                    // Mark span two words. Check first bit of next word to decide if mark is grey.
                    bitmapWord = colorMapBase.getLong(bitmapWordIndex + 1);
                    if ((bitmapWord & 1) != 0) {
                        // it is a grey object.
                        final int bitIndexOfGreyMark = (bitmapWordIndex << Word.widthValue().log2numberOfBits) + LAST_BIT_INDEX_IN_WORD;
                        return bitIndexOfGreyMark < lastBitIndex ? bitIndexOfGreyMark : -1;
                    }
                }
            }
            bitmapWordIndex++;
        }
        return -1;
    }

    public void verifyHasNoGreyMarks(HeapRegionRangeIterable regionsRanges, Address end)  {
        final int log2RegionSize = HeapRegionConstants.log2RegionSizeInBytes;
        while (regionsRanges.hasNext()) {
            final RegionRange regionsRange = regionsRanges.next();
            Address startOfRange = coveredAreaStart.plus(regionsRange.firstRegion() << log2RegionSize);
            if (startOfRange.greaterThan(end)) {
                return;
            }
            Address endOfRange = startOfRange.plus(regionsRange.numRegions() << log2RegionSize);
            verifyHasNoGreyMarks(startOfRange, end.lessThan(endOfRange) ? end : endOfRange);
        }
    }

    /**
     * Verifies that a heap region has no grey objects.
     * @param start start of the region
     * @param end end of the region
     * @return true if the region has no grey objects, false otherwise.
     */
    public void verifyHasNoGreyMarks(Address start, Address end) {
        final int bitIndex = scanForGreyMark(start, end);
        if (bitIndex >= 0) {
            final boolean lockDisabledSafepoints = Log.lock();
            final Pointer greyCell = coveredAreaStart.plus(bitIndex << log2BytesCoveredPerBit).asPointer();
            assert isGrey(bitIndex);
            Log.print("grey mark found for cell: ");
            Log.print(greyCell);
            Log.print(", size: ");
            Log.print(Layout.size(Layout.cellToOrigin(greyCell)));
            Log.print(" bit index: ");
            Log.print(bitIndex);
            Log.print(" in grey-free area [");
            Log.print(start);
            Log.print(", ");
            Log.print(end);
            Log.println(" ]");

            // More GC state printing:
            Log.print("   current scanning state = ");
            Log.println(currentScanState.toString());
            forwardScanState.printState();
            overflowScanState.printState();

            Log.unlock(lockDisabledSafepoints);
            FatalError.unexpected("Must not have any grey marks");
        }
    }

    /**
     * Set the black bit of a live but not yet visited object.
     * This is used during normal forward scan of the mark bitmap.
     * Object after the finger are marked black directly, although in term of the
     * tricolor algorithm, they are grey. This simplifies forward scanning and avoid
     * updating the mark bitmap from grey to black when visiting a cell.
     * Object before the marking stack are marked black as well and pushed on the marking stack.
     * If this one overflows, the mark are set to grey so they can be distinguished from real black ones
     * during rescan of the mark bitmap.
     *
     * @param cell the address of the object to mark.
     */
    @INLINE
    private void markObjectBlack(Pointer cell) {
        if (cell.greaterThan(blackFinger)) {
            // Object is after the finger. Mark grey and update rightmost if white.
            if (markBlackIfWhite(cell) && cell.greaterThan(blackRightmost)) {
                blackRightmost = cell;
            }
        } else if (cell.greaterEqual(coveredAreaStart) && markBlackIfWhite(cell)) {
            markingStack.push(cell);
        }
    }

    @INLINE
    final void markRefBlack(Reference ref) {
        markObjectBlack(Layout.originToCell(ref.toOrigin()));
    }

    final PointerIndexVisitor markBlackPointerIndexVisitor = new PointerIndexVisitor() {
        @Override
        public  final void visit(Pointer pointer, int wordIndex) {
            markRefBlack(pointer.getReference(wordIndex));
        }
    };

    Pointer visitBlackCell(Pointer cell) {
        if (Heap.traceGC()) {
            printVisitedCell(cell, "Visiting black cell ");
        }
        final Pointer origin = Layout.cellToOrigin(cell);
        final Reference hubRef = Layout.readHubReference(origin);
        markRefBlack(hubRef);
        final Hub hub = UnsafeCast.asHub(hubRef.toJava());

        // Update the other references in the object
        final SpecificLayout specificLayout = hub.specificLayout;
        if (specificLayout.isTupleLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, markBlackPointerIndexVisitor);
            if (hub.isJLRReference) {
                SpecialReferenceManager.discoverSpecialReference(cell);
            }
            return cell.plus(hub.tupleSize);
        }
        if (specificLayout.isHybridLayout()) {
            TupleReferenceMap.visitReferences(hub, origin, markBlackPointerIndexVisitor);
        } else if (specificLayout.isReferenceArrayLayout()) {
            final int length = Layout.readArrayLength(origin);
            for (int index = 0; index < length; index++) {
                markRefBlack(Layout.getReference(origin, index));
            }
        }
        return cell.plus(Layout.size(origin));
    }

    final Pointer setAndVisitBlackFinger(Pointer cell) {
        blackFinger = cell;
        return visitBlackCell(cell);
    }

    /**
     * Alternative to visit grey object. Here, we assume that object after the finger are marked black directly.
     *
     * @param start
     * @param scanState
     */
    public void scanForward(Pointer start, ColorMapScanState scanState) {
        final Pointer colorMapBase = base.asPointer();
        int rightmostBitmapWordIndex = scanState.rightmostBitmapWordIndex();
        int bitmapWordIndex = bitmapWordIndex(bitIndexOf(start));
        do {
            while (bitmapWordIndex <= rightmostBitmapWordIndex) {
                long bitmapWord = colorMapBase.getLong(bitmapWordIndex);
                if (bitmapWord != 0) {
                    // At least one mark is set.
                    int bitIndexInWord = 0;
                    final int bitmapWordFirstBitIndex = bitmapWordIndex << Word.widthValue().log2numberOfBits;
                    Pointer endOfLastVisitedCell;
                    do {
                        // First mark is the least set bit.
                        bitIndexInWord += Pointer.fromLong(bitmapWord).leastSignificantBitSet();
                        final int bitIndexOfGreyMark = bitmapWordFirstBitIndex + bitIndexInWord;
                        endOfLastVisitedCell = setAndVisitBlackFinger(addressOf(bitIndexOfGreyMark).asPointer());
                        // Need to read the mark bitmap word again in case a mark was set in the bitmap word by the visitor.
                        // We also right-shift the bitmap word shift it to flush the mark bits already processed.
                        // This is necessary because live objects after the finger are marked black only, so we cannot
                        // distinguish black from grey.
                        bitIndexInWord += 2;
                        bitmapWord = colorMapBase.getLong(bitmapWordIndex) >>> bitIndexInWord;
                    } while(bitmapWord != 0L);
                    bitmapWordIndex++;
                    final int nextCellBitmapWordIndex =  bitmapWordIndex(endOfLastVisitedCell);
                    if (nextCellBitmapWordIndex > bitmapWordIndex) {
                        bitmapWordIndex = nextCellBitmapWordIndex;
                    }
                } else {
                    bitmapWordIndex++;
                }
            }
            // There might be some objects left in the marking stack. Drain it.
            markingStack.drain();
            // Rightmost may have been updated. Check for this, and loop back if it has.
            final int b = scanState.rightmostBitmapWordIndex();
            if (b <= rightmostBitmapWordIndex) {
                // We're done.
                break;
            }
            rightmostBitmapWordIndex = b;
        } while(true);
    }

    public void recoverFromOverflow() {
        currentScanState.numMarkinkgStackOverflow++;
        overflowScanState.recoverFromOverflow();
    }

    private void initAfterRootMarking() {
        forwardScanState.rightmost = rootCellVisitor.rightmost;
        forwardScanState.finger = rootCellVisitor.leftmost;
        forwardScanState.numMarkinkgStackOverflow = 0;
        overflowScanState.numMarkinkgStackOverflow = 0;
    }

    private void visitGreyObjects() {
        currentScanState = forwardScanState;
        overflowScanState.markingStackFlusher().setScanState(currentScanState);
        forwardScanState.visitGreyObjects();
    }

    private void visitGreyObjects(HeapRegionRangeIterable regionRanges) {
        currentScanState = forwardScanState;
        overflowScanState.markingStackFlusher().setScanState(currentScanState);
        forwardScanState.visitGreyObjects(regionRanges);
    }

    /**
     * Visit all objects marked grey during root marking that resides in list of memory region ranges.
     * Regions are numbered from 0, where in the address to the first bytes of region 0 coincide with
     * the covered area's start address.
     *
     * @param regionsRanges an enumeration of the heap region ranges holding objects to trace.
     */
    void visitGreyObjectsAfterRootMarking(HeapRegionRangeIterable regionsRanges) {
        initAfterRootMarking();
        visitGreyObjects(regionsRanges);
    }

    /**
     * Visit all objects marked grey during root marking.
     */
    void visitGreyObjectsAfterRootMarking() {
        initAfterRootMarking();
        visitGreyObjects();
    }


    int firstBlackMark(int firstBitIndex, int lastBitIndex) {
        final Pointer colorMapBase = base.asPointer();
        final int lastBitmapWordIndex = bitmapWordIndex(lastBitIndex);
        int bitmapWordIndex = bitmapWordIndex(firstBitIndex);
        while (bitmapWordIndex <= lastBitmapWordIndex) {
            long bitmapWord = colorMapBase.getLong(bitmapWordIndex);
            if (bitmapWord != 0) {
                // First mark is the least set bit.
                final int bitIndexInWord = Pointer.fromLong(bitmapWord).leastSignificantBitSet();
                final int bitIndexOfCell = (bitmapWordIndex << Word.widthValue().log2numberOfBits) + bitIndexInWord;
                if (bitIndexOfCell >= lastBitIndex) {
                    return -1;
                }
                return bitIndexOfCell;
            }
            bitmapWordIndex++;
        }
        return -1;
    }

    public void sweep(Sweeper sweeper) {
        if (Sweeper.DoImpreciseSweep) {
            impreciseSweep(sweeper, sweeper.minReclaimableSpace());
        } else {
            preciseSweep(sweeper);
        }
    }

    private void preciseSweep(Sweeper sweeper) {
        final Pointer colorMapBase = base.asPointer();
        final int rightmostBitmapWordIndex =  bitmapWordIndex(bitIndexOf(forwardScanState.rightmost));
        int bitmapWordIndex = bitmapWordIndex(bitIndexOf(coveredAreaStart));

        while (bitmapWordIndex <= rightmostBitmapWordIndex) {
            long bitmapWord = colorMapBase.getLong(bitmapWordIndex);
            if (bitmapWord != 0) {
                // At least one mark is set.
                int bitIndexInWord = 0;
                final int bitmapWordFirstBitIndex = bitmapWordIndex << Word.widthValue().log2numberOfBits;
                Pointer nextBitmapWordLimit = addressOf(bitmapWordFirstBitIndex + Word.widthValue().numberOfBits).asPointer();
                long w = bitmapWord;
                do {
                    // First mark is the least set bit.
                    bitIndexInWord += Pointer.fromLong(w).leastSignificantBitSet();
                    final int bitIndexOfBlackMark = bitmapWordFirstBitIndex + bitIndexInWord;
                    if (MaxineVM.isDebug()) {
                        debugBitmapWordIndex = bitmapWordIndex;
                        debugBitIndex = bitIndexInWord;
                        debugCursor = addressOf(bitIndexOfBlackMark).asPointer();
                    }
                    final Pointer endOfLastVisitedCell = sweeper.processLiveObject(addressOf(bitIndexOfBlackMark).asPointer());
                    if (endOfLastVisitedCell.greaterEqual(nextBitmapWordLimit)) {
                        nextBitmapWordLimit = endOfLastVisitedCell;
                        break;
                    }
                    // End of visited cell is within the same mark word. Just
                    // right-shift the bitmap word to skip the mark bits already processed and loop back to
                    // find the next black object with this word.
                    bitIndexInWord += 2;
                    w = bitmapWord >>> bitIndexInWord;
                } while(w != 0L);
                bitmapWordIndex = bitmapWordIndex(nextBitmapWordLimit);
            } else {
                bitmapWordIndex++;
            }
        }
    }


    /**
     * Imprecise sweeping of the heap.
     * The sweeper is notified only when the distance between two live marks is larger than a specified minimum amount of
     * space. In this case, the sweeper is passed on the address of the two live objects delimiting the space.
     * This avoids touching the heap for adjacent small objects, and paying reclamation cost for unusable dead spaces.
     *
     * @param sweeper
     * @param minReclaimableSpace
     */
    private void impreciseSweep(Sweeper sweeper, Size minReclaimableSpace) {
        final Pointer colorMapBase = base.asPointer();
        final int minBitsBetweenMark = minReclaimableSpace.toInt() >> log2BytesCoveredPerBit;
        int bitmapWordIndex = 0;
        final Address rightmost = forwardScanState.rightmost;
         // Indicate the closest position the next live mark should be at to make the space reclaimable.
        int nextReclaimableMark = minBitsBetweenMark;
        int lastLiveMark = firstBlackMark(0, bitIndexOf(rightmost));
        if (lastLiveMark > 0) {
            if (lastLiveMark >=  nextReclaimableMark) {
                sweeper.processDeadSpace(coveredAreaStart, Size.fromInt(lastLiveMark << log2BytesCoveredPerBit));
            }
            bitmapWordIndex =  bitmapWordIndex(lastLiveMark + 2);
            nextReclaimableMark = lastLiveMark + 2 + minBitsBetweenMark;
        } else if (lastLiveMark < 0) {
            // The whole heap is free. (is that ever possible ?)
            sweeper.processDeadSpace(coveredAreaStart, rightmost.minus(coveredAreaStart).asSize());
            return;
        }

        // Loop over the color map and call the sweeper only when the distance between two live mark is larger than
        // the minimum reclaimable space specified.
        int rightmostBitmapWordIndex =  bitmapWordIndex(bitIndexOf(rightmost));
        if (MaxineVM.isDebug()) {
            debugGapLeftObject = Pointer.zero();
            debugGapRightObject = Pointer.zero();
            debugBitIndex = 0;
            debugBitmapWordIndex = 0;
        }

        while (bitmapWordIndex <= rightmostBitmapWordIndex) {
            final long bitmapWord = colorMapBase.getLong(bitmapWordIndex);
            if (bitmapWord != 0) {
                // At least one mark is set.
                int bitIndexInWord = 0;
                final int bitmapWordFirstBitIndex = bitmapWordIndex << Word.widthValue().log2numberOfBits;
                int nextCellBitmapWordIndex = bitmapWordIndex + 1;
                long w = bitmapWord;
                do {
                    // First mark is the least set bit.
                    bitIndexInWord += Pointer.fromLong(w).leastSignificantBitSet();
                    final int bitIndexOfBlackMark = bitmapWordFirstBitIndex + bitIndexInWord;
                    if (bitIndexOfBlackMark < nextReclaimableMark) {
                        // Too small a gap between two live marks to be worth reporting to the sweeper.
                        // Reset the next mark.
                        lastLiveMark = bitIndexOfBlackMark;
                        nextReclaimableMark = bitIndexOfBlackMark + minBitsBetweenMark;
                        if (bitIndexInWord >= 62) {
                            // next object begins in next word.
                            break;
                        }
                    } else {
                        if (MaxineVM.isDebug()) {
                            debugBitIndex = bitIndexOfBlackMark;
                            debugBitmapWordIndex = bitmapWordIndex;
                            debugGapLeftObject = addressOf(lastLiveMark).asPointer();
                            debugGapRightObject = addressOf(bitIndexOfBlackMark).asPointer();
                        }
                        final Pointer endOfLastVisitedCell = sweeper.processLargeGap(addressOf(lastLiveMark).asPointer(), addressOf(bitIndexOfBlackMark).asPointer());
                        lastLiveMark  = bitIndexOfBlackMark;
                        nextReclaimableMark = bitIndexOf(endOfLastVisitedCell) + minBitsBetweenMark;
                        final int index =  bitmapWordIndex(endOfLastVisitedCell);
                        if (index > bitmapWordIndex) {
                            nextCellBitmapWordIndex = index;
                            break;
                        }
                        // End of visited cell is within the same mark word. Just
                        // right-shift the bitmap word to skip the mark bits already processed and loop back to
                        // find the next black object with this word.
                    }
                    bitIndexInWord += 2;
                    w = bitmapWord >>> bitIndexInWord;
                } while(w != 0L);
                bitmapWordIndex = nextCellBitmapWordIndex;
            } else {
                bitmapWordIndex++;
            }
        }
        Pointer tail = rightmost.asPointer().plus(Layout.size(Layout.cellToOrigin(rightmost.asPointer())));
        Size tailSpace = coveredAreaEnd.minus(tail).asSize();
        if (tailSpace.greaterEqual(minReclaimableSpace)) {
            sweeper.processDeadSpace(tail, tailSpace);
        }
    }

    /**
     * Sweep a region of memory described by the heap region sweeper.
     * The sweeper implements an API for recording range of dead space, and to specify the bounds of the region.
     * This can server region-based heap as well as contiguous heap, wherein a single region is passed in this case.
     * @param sweeper
     * @return
     */
    public void sweep(HeapRegionSweeper regionsSweeper) {
        final Address rightmostLiveObject = forwardScanState.rightmost;
        do {
            assert regionsSweeper.hasNextSweepingRegion();
            regionsSweeper.beginSweep();
            impreciseSweep(regionsSweeper);
            regionsSweeper.endSweep();
        } while(regionsSweeper.endOfSweepingRegion().lessEqual(rightmostLiveObject));
        regionsSweeper.reachedRightmostLiveRegion();
    }

    @INLINE
    private Address endOfCellAtBitIndex(int blackBitIndex) {
        Pointer cell = addressOf(blackBitIndex).asPointer();
        return cell.plus(Layout.size(Layout.cellToOrigin(cell)));
    }

    /**
     * Perform imprecise sweeping of a heap region described by a {@link HeapRegionSweeper} and notifies the {@link HeapRegionSweeper}
     * of every encountered dead space larger or equal to {@link HeapRegionSweeper#minReclaimableSpace()}.
     * @param sweeper
     */
    private void impreciseSweep(HeapRegionSweeper sweeper) {
        final Pointer colorMapBase = base.asPointer();
        final Address regionLeftmost = sweeper.startOfSweepingRegion();
        final Address regionRightmost = sweeper.endOfSweepingRegion();
        final Address rightmost =  regionRightmost.greaterThan(forwardScanState.rightmost) ? forwardScanState.rightmost : regionRightmost.minus(Word.size());
        final int rightmostBitIndex = bitIndexOf(rightmost);
        final int leftmostBitIndex = bitIndexOf(regionLeftmost);
        int lastLiveMark = firstBlackMark(leftmostBitIndex, rightmostBitIndex);
        if (lastLiveMark < 0) {
            // No live mark found on this region.
            sweeper.processDeadRegion();
            return;
        }
        if (lastLiveMark == leftmostBitIndex && sweeper.sweepingRegionIsLargeHead()) {
            return;
        }
        final int minBitsBetweenMark = sweeper.minReclaimableSpace().toInt() >> log2BytesCoveredPerBit;
        // Indicate the closest position the next live mark should be at to make the space reclaimable.
        int nextReclaimableMark = leftmostBitIndex + minBitsBetweenMark;
        if (lastLiveMark > leftmostBitIndex) {
            if (lastLiveMark >=  nextReclaimableMark) {
                sweeper.processDeadSpace(regionLeftmost, Size.fromInt((lastLiveMark - leftmostBitIndex) << log2BytesCoveredPerBit));
            }
            nextReclaimableMark = lastLiveMark + 2 + minBitsBetweenMark;
        }

        int bitmapWordIndex =  bitmapWordIndex(lastLiveMark + 2); // FIXME: 2 here is to skip the object header. Need to derive that from some named constants.

        // Loop over the color map and call the sweeper only when the distance between two live mark is larger than
        // the minimum reclaimable space specified.
        final int rightmostBitmapWordIndex =  bitmapWordIndex(rightmostBitIndex);
        if (MaxineVM.isDebug()) {
            debugBitIndex = 0;
            debugBitmapWordIndex = 0;
            debugGapLeftObject = Pointer.zero();
            debugGapRightObject =  Pointer.zero();
            debugRightmostBitmapWordIndex = rightmostBitmapWordIndex;
        }
        while (bitmapWordIndex <= rightmostBitmapWordIndex) {
            final long bitmapWord = colorMapBase.getLong(bitmapWordIndex);
            if (bitmapWord != 0) {
                // At least one mark is set.
                int bitIndexInWord = 0;
                final int bitmapWordFirstBitIndex = bitmapWordIndex << Word.widthValue().log2numberOfBits;
                int nextCellBitmapWordIndex = bitmapWordIndex + 1;
                long w = bitmapWord;
                do {
                    // First mark is the least set bit.
                    bitIndexInWord += Pointer.fromLong(w).leastSignificantBitSet();
                    final int bitIndexOfBlackMark = bitmapWordFirstBitIndex + bitIndexInWord;
                    if (bitIndexOfBlackMark < nextReclaimableMark) {
                        // Too small a gap between two live marks to be worth reporting to the sweeper.
                        // NOTE: it may be live data only, or it may comprise dead data. If the latter, we have some unaccounted dark matter.
                        // Reset the next mark.
                        lastLiveMark = bitIndexOfBlackMark;
                        nextReclaimableMark = bitIndexOfBlackMark + minBitsBetweenMark;
                        if (bitIndexInWord >= 62) {
                            // next object begins in next word.
                            break;
                        }
                    } else {
                        if (MaxineVM.isDebug()) {
                            debugBitIndex = bitIndexOfBlackMark;
                            debugBitmapWordIndex = bitmapWordIndex;
                            debugGapLeftObject = addressOf(lastLiveMark).asPointer();
                            debugGapRightObject = addressOf(bitIndexOfBlackMark).asPointer();
                        }
                        final Pointer endOfLastVisitedCell = sweeper.processLargeGap(addressOf(lastLiveMark).asPointer(), addressOf(bitIndexOfBlackMark).asPointer());
                        lastLiveMark  = bitIndexOfBlackMark;
                        nextReclaimableMark = bitIndexOf(endOfLastVisitedCell) + minBitsBetweenMark;
                        final int index =  bitmapWordIndex(endOfLastVisitedCell);
                        if (index > bitmapWordIndex) {
                            nextCellBitmapWordIndex = index;
                            break;
                        }
                        // End of visited cell is within the same mark word. Just
                        // right-shift the bitmap word to skip the mark bits already processed and loop back to
                        // find the next black object with this word.
                    }
                    bitIndexInWord += 2;
                    w = bitmapWord >>> bitIndexInWord;
                } while(w != 0L);
                bitmapWordIndex = nextCellBitmapWordIndex;
            } else {
                bitmapWordIndex++;
            }
        }
        Address tail = endOfCellAtBitIndex(lastLiveMark);
        Size tailSpace = regionRightmost.minus(tail).asSize();
        if (tailSpace.greaterEqual(sweeper.minReclaimableSpace())) {
            sweeper.processDeadSpace(tail, tailSpace);
        }
    }

    public void markAll() {
        traceGCTimes = Heap.traceGCTime();
        if (traceGCTimes) {
            recoveryScanTimer.reset();
        }
        FatalError.check(markingStack.isEmpty(), "Marking stack must be empty");

        clearColorMap();
        markRoots();
        if (Heap.traceGCPhases()) {
            Log.println("Tracing grey objects...");
        }
        startTimer(heapMarkingTimer);
        markPhase = MARK_PHASE.VISIT_GREY_FORWARD;
        visitGreyObjectsAfterRootMarking();
        stopTimer(heapMarkingTimer);
        if (traceGCTimes) {
            totalRecoveryScanCount += recoveryScanTimer.getCount();
            totalRecoveryElapsedTime += recoveryScanTimer.getElapsedTime();
        }

        if (VerifyAfterMarking) {
            verifyHasNoGreyMarks(coveredAreaStart, forwardScanState.endOfRightmostVisitedObject());
        }

        startTimer(weakRefTimer);
        markPhase = MARK_PHASE.SPECIAL_REF;
        SpecialReferenceManager.processDiscoveredSpecialReferences(forwardScanState);
        visitGreyObjects();
        stopTimer(weakRefTimer);

        if (VerifyAfterMarking) {
            verifyHasNoGreyMarks(coveredAreaStart, forwardScanState.endOfRightmostVisitedObject());
        }
    }

    /**
     * Mark all live objects that resides in the heap regions enumerated by the iterable region range.
     * @param regionsRanges enumerate ranges of heap regions holding objects to trace
     */
    public void markAll(HeapRegionRangeIterable regionsRanges) {
        traceGCTimes = Heap.traceGCTime();
        if (traceGCTimes) {
            recoveryScanTimer.reset();
        }
        FatalError.check(markingStack.isEmpty(), "Marking stack must be empty");
        clearColorMap();
        overflowScanState.setHeapRegionsRanges(regionsRanges);
        markRoots();
        if (Heap.traceGCPhases()) {
            Log.println("Tracing grey objects...");
        }
        startTimer(heapMarkingTimer);
        markPhase = MARK_PHASE.VISIT_GREY_FORWARD;
        visitGreyObjectsAfterRootMarking(regionsRanges);
        stopTimer(heapMarkingTimer);

        if (traceGCTimes) {
            totalRecoveryScanCount += recoveryScanTimer.getCount();
            totalRecoveryElapsedTime += recoveryScanTimer.getElapsedTime();
        }
        FatalError.check(markingStack.isEmpty(), "Marking Stack must be empty after visiting grey objects.");
        if (VerifyAfterMarking || VerifyGreyLessAreas) {
            regionsRanges.reset();
            verifyHasNoGreyMarks(regionsRanges, forwardScanState.endOfRightmostVisitedObject());
        }
        startTimer(weakRefTimer);
        markPhase = MARK_PHASE.SPECIAL_REF;
        SpecialReferenceManager.processDiscoveredSpecialReferences(forwardScanState);
        // Note: the VISIT_GREY_FORWARD has already visited the whole heap, so any additional grey reference added by the special reference
        // manager are on the marking stack. So the following reduces to flushing the marking stack.
        visitGreyObjects(regionsRanges);
        stopTimer(weakRefTimer);
        FatalError.check(markingStack.isEmpty(), "Marking Stack must be empty after special references are processed.");
        markPhase = MARK_PHASE.DONE;
    }

}
