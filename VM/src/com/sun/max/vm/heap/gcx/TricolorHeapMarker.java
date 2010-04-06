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

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * A marking algorithm that uses a three-color mark-bitmap with a fixed-size tiny marking stack, a rescan map, and a
 * finger.The three-color mark-bitmap consumes as much space overhead as a two-color mark bitmap, thanks to padding tiny
 * objects to guarantee two color bits for every objects.
 *
 * The three colors mark bitmap three colors (white, grey, black) using two consecutive bits. Every bit maps to a fixed
 * chunk of heap such that every object's first words coincide with one fixed chunk. Almost all objects have a size
 * larger than the size of a single chunk covered by one bit. Those that don't (called tiny objects) are segregated or
 * padded (i.e., the heap allocate them the required space cover 2 bits of the mark bitmap). Maxine currently aligns
 * objects on a 8-byte word boundary, uses 8-bytes words, and uses a two-words header.
 *
 * The following choices are considered:
 * - each bit corresponds to a single word of the heap; Every object is thus guaranteed two-bit;
 * - each bit corresponds to two Words of the heap; Every object larger that 3 words occupies 2 chunks. With this design,
 * the smallest objects can only be allocated 1 bit. Since such objects are generally rare they can be treated
 * specially: e.g., padded to be associated with two bits, or segregated to be allocated in an area covered by a one bit
 * bitmap. Padding is simpler as it allows a unique bitmaps. Maxine's current 8-byte alignment raises another problem
 * with this approach: a chunk can be shared by two objects. This complicates finding the head of an object. The solution
 * is to require objects to be chunk-aligned (i.e., 16-byte aligned) potentially wasting heap space.
 *
 * Which solution is best depends on the amount of space wasted by the 2-words alignment requirement, compared to bitmap
 * space saved. A larger grain also means less time to scan the bitmap. We leave this choice to the heap implementation
 * / collector, which is responsible for aligning object and dealing with small object. For simplicity, we begin here
 * with the first alternative (1 bit per word).
 *
 * Finally, note that in both cases, the first bit of a color may be either an odd or an even bit, and a color may span
 * two bitmaps words. This complicates color search/update operation. A heap allocator may arrange for guaranteeing that
 * an object marks never span a bitmap word by padding a dead object before (Dead objects are special instance of object
 * whose size is strictly 2 words, regardless of other rules for dealing with tiny objects).
 *
 * An other alternative is to exploit location of an object with respect to the current cursor on the mark bitmap:
 * since object located after the cursor aren't visited yet, we can use the black mark for marking these grey
 * (i.e., an object with the black mark set is black only located before the finger). In this case, the grey mark is really only used
 * on overflow of the mark stack.
 *
 * This class enables both designs, and provides generic bitmap manipulation that understands color coding and
 * color-oriented operations (i.e., searching grey or black mark, etc.). It provides fast and slow variant of
 * operations, wherein the fast variant assumes that a color never span a bitmap word. The GC is responsible for
 * guaranteeing this property when it uses the fast variant.
 *
 * @author Laurent Daynes
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

    static final int LAST_BIT_INDEX_IN_WORD = Word.width() - 1;

    static final int bitIndexInWordMask = ~LAST_BIT_INDEX_IN_WORD;

    /**
     * Return the index within a word of the bit index.
     *
     * @param bitIndex
     * @return
     */
    static final int bitIndexInWord(int bitIndex) {
        return bitIndex & bitIndexInWordMask;
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
     * Log 2 to get bitmap word index from an address.
     */
    final int log2BitmapWord;

    /**
     * Start of the heap area covered by the mark bitmap.
     */
    Address coveredAreaStart;

    /**
     * End of the heap area covered by the mark bitmap.
     */
    Address coveredAreaEnd;

    /**
     * Finger that points to the rightmost visited (black) object.
     */
    private Pointer finger;

    /**
     * Leftmost marked position.
     */
    private Pointer leftmost;

    /**
     * Rightmost marked position.
     */
    private Pointer rightmost;

    /**
     * Indicates whether we're recovering from a marking stack overflow
     * (i.e., a scan of the marking stack in recovery mode is initiated).
     */
    boolean recovering = false;
    /**
     * Start of the current scan of the color map to recover from a marking stack overflow.
     */
    Pointer startOfCurrentOverflowScan;

    /**
     * Start of the next scan of the color map to recover from secondary marking stack overflow.
     */
    Pointer startOfNextOverflowScan;


    @INLINE
    public final boolean isCovered(Address address) {
        return address.greaterEqual(coveredAreaStart) && address.lessThan(coveredAreaEnd);
    }

    /**
     * Base of the bitmap biased with offset to the first word's bit. For fast-computing of heap word's color index in
     * the color map.
     */
    Address biasedBitmapBase;

    /**
     * Bias to the first word of the heap.
     */
    int baseBias;
    /**
     * Memory where the color map is stored.
     */
    final RuntimeMemoryRegion colorMap;

    /**
     * Shortcut to colorMap.start() for fast bitmap operation.
     */
    private Address base;

    /**
     * The marking stack.
     */
    private final MarkingStack markingStack;

    /**
     * Return the size in bytes required for a tricolor mark bitmap to cover a contiguous
     * heap area of the specified size.
     *
     * @param coveredAreaSize
     * @return the size a three color mark bitmaps should have to cover the specified area size.
     */
    Size bitmapSize(Size coveredAreaSize) {
        return coveredAreaSize.dividedBy(wordsCoveredPerBit * Word.widthValue().numberOfBits);
    }


    /**
     * Returns max amount of memory needed for a max heap size.
     * Let the HeapScheme decide where to allocate.
     *
     * @param maxHeapSize
     * @return
     */
    public Size memoryRequirement(Size maxHeapSize) {
        return bitmapSize(maxHeapSize);
    }

    /**
     *
     */
    public TricolorHeapMarker(int wordsCoveredPerBit) {
        this.wordsCoveredPerBit = wordsCoveredPerBit;
        log2BytesCoveredPerBit = Word.widthValue().log2numberOfBytes + Integer.numberOfTrailingZeros(wordsCoveredPerBit);
        assert wordsCoveredPerBit * Word.widthValue().numberOfBytes == 1 << log2BytesCoveredPerBit;
        log2BitmapWord = log2BytesCoveredPerBit + Word.widthValue().log2numberOfBits;
        colorMap = new RuntimeMemoryRegion("Mark Bitmap");
        markingStack = new MarkingStack();
        markingStack.setOverflowHandler(this);
    }

    /**
     * Initialize a three color mark bitmap for the covered area. The mark-bitmap is generated at VM startup.
     *
     * @param bitmapStorage
     * @param coveredArea
     */
    public void initialize(RuntimeMemoryRegion coveredArea, Address bitmapStorage, Size bitmapSize) {
        if (MaxineVM.isDebug()) {
            FatalError.check(bitmapSize.toLong() >= bitmapSize(coveredArea.size()).toLong(), "Mark bitmap too small to cover heap");
        }
        coveredAreaStart = coveredArea.start();
        coveredAreaEnd = coveredArea.end();

        colorMap.setStart(bitmapStorage);
        colorMap.setSize(bitmapSize);
        base = bitmapStorage;
        baseBias = coveredArea.start().unsignedShiftedRight(log2BitmapWord).toInt();
        biasedBitmapBase = colorMap.start().minus(baseBias);
        markingStack.initialize();

        clear();
    }

    // Address to bitmap word / bit index operations.

    @INLINE
    final boolean colorSpanWords(int bitIndex) {
        return bitIndexInWord(bitIndex) == (Word.width() - 1);
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

    @INLINE
    final Word bitmapWordAt(int bitIndex) {
        return base.asPointer().getWord(bitmapWordIndex(bitIndex));
    }
    /**
     *  Pointer to the bitmap word in the color map containing the bit at specified bit index.
     * @param bitIndex a bit index
     * @return a pointer to a word of the color map
  */
    @INLINE
    final Pointer bitmapWordPointerAt(int bitIndex) {
        return base.asPointer().plus(bitmapWordIndex(bitIndex));
    }
    // Bit index in the bitmap for the address.
    @INLINE
    final int bitIndexOf(Address heapAddress) {
        return heapAddress.unsignedShiftedRight(log2BytesCoveredPerBit).minus(baseBias).toInt();
    }

    @INLINE
    final Address addressOf(int bitIndex) {
        return coveredAreaStart.plus(bitIndex << log2BytesCoveredPerBit);
    }

    @INLINE
    final void markGrey_(int bitIndex) {
        FatalError.check(colorSpanWords(bitIndex), "Color must not cross word boundary.");
        final Pointer pointer = bitmapWordPointerAt(bitIndex);
        pointer.setLong(pointer.getLong() | (GREY << bitIndexInWord(bitIndex)));
    }

    @INLINE
    final void markGrey_(Address cell) {
        markGrey_(bitIndexOf(cell));
    }


    @INLINE
    final void markGrey(int bitIndex) {
        if (colorSpanWords(bitIndex)) {
            markGrey_(bitIndex);
        } else {
            final Pointer pointer = bitmapWordPointerAt(bitIndex);
            pointer.setLong(pointer.getLong() | (1 << bitIndexInWord(bitIndex)));
            pointer.setLong(1, pointer.getLong(1) | 1);
        }
    }

    @INLINE
    final void markGrey(Address cell) {
        markGrey(bitIndexOf(cell));
    }


    @INLINE
    final void markBlackFromGrey_(Address cell) {
        markBlackFromGrey_(bitIndexOf(cell));
    }

    @INLINE
    final void markBlackFromWhite(int bitIndex) {
        final Pointer pointer = bitmapWordPointerAt(bitIndex);
        pointer.setLong(pointer.getLong() | (BLACK << bitIndexInWord(bitIndex)));
    }

    @INLINE
    final void markBlackFromWhite(Address cell) {
        markBlackFromWhite(bitIndexOf(cell));
    }

    @INLINE
    final boolean markGreyIfWhite_(Pointer cell) {
        final int bitIndex = bitIndexOf(cell);
        if (isWhite(bitIndex)) {
            markGrey_(bitIndex);
            return true;
        }
        return false;
    }

    final boolean markGreyIfWhite(Pointer cell) {
        final int bitIndex = bitIndexOf(cell);
        if (isWhite(bitIndex)) {
            markGrey(bitIndex);
            return true;
        }
        return false;
    }

    final boolean markBlackIfWhite(Pointer cell) {
        final int bitIndex = bitIndexOf(cell);
        if (isWhite(bitIndex)) {
            markBlackFromWhite(bitIndex);
        }
        return false;
    }

    @INLINE
    final void markBlackFromGrey_(int bitIndex) {
        FatalError.check(colorSpanWords(bitIndex), "Color must not cross word boundary.");
        final Pointer pointer = bitmapWordPointerAt(bitIndex);
        // Clear the second bit of the color.
        pointer.setLong(pointer.getLong() & (1 << bitIndexInWord(bitIndex + 1)));
    }

    /**
     * Set a black mark not assuming previous mark in place.
     *
     * @param bitIndex an index in the color map corresponding to the first word of an object in the heap.
     */
    @INLINE
    final void markBlack_(int bitIndex) {
        FatalError.check(colorSpanWords(bitIndex), "Color must not cross word boundary.");
        final Pointer pointer = bitmapWordPointerAt(bitIndex);
        long bitmapWord = pointer.getLong();
        final int bitIndexInWord = bitIndexInWord(bitIndex);
        // Clear grey bit and set black one.
        final long blackBit = 1 << bitIndexInWord;
        bitmapWord |= blackBit;
        bitmapWord &= ~(blackBit << 1);
        pointer.setLong(bitmapWord);
    }

    @INLINE
    final boolean isColor_(int bitIndex, long color) {
        FatalError.check(colorSpanWords(bitIndex), "Color must not cross word boundary.");
        final Word bitmapWord = bitmapWordAt(bitIndex);
        return ((bitmapWord.asPointer().toLong() >> bitIndexInWord(bitIndex)) & COLOR_MASK) == color;
    }

    /**
     * Check the color map for a white object. Thanks to the color encoding, it only need to
     * check the leading bit of the two-bit color. As a result, no special care is needed
     * if the object's color span two words of the color map.
     *
     * @param bitIndex an index in the color map corresponding to the first word of an object in the heap.
     * @return true if the object is white.
     */
    @INLINE
    final boolean isWhite(int bitIndex) {
        return (bitmapWordAt(bitIndex).asPointer().toLong() & (1L << bitIndexInWord(bitIndex))) == 0;
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

    @INLINE
    final boolean isBlack_(Object o) {
        return isBlack_(Reference.fromJava(o));
    }

    @INLINE
    final boolean isGrey_(Object o) {
        return isGrey_(Reference.fromJava(o));
    }

    /**
     * Clear the color map, i.e., turn all bits to white.
     */
    void clear() {
        Memory.clearWords(colorMap.start().asPointer(), colorMap.size().toInt() >> Word.widthValue().log2numberOfBytes);
    }


    /**
     * Marking of strong roots outside of the covered area.
     *
     * Implements cell visitor and pointer index visitor.
     * Cell visitor must be used only for region outside of the heap areas covered by the mark
     * bitmap. This currently includes the boot region, the code region and the immortal heap.
     * Pointer visitor should be used only for references from outside of the covered area,
     * i.e., reference from the boot region and external roots (thread stack, live monitors).
     *
     * We haven't started visiting the mark bitmap, so we don't have any black marks.
     * Thus, we don't bother with first testing if a reference is white to mark it grey (it cannot be),
     * or to test it against the finger to decide whether to mark it grey or push it on the marking stack.
     * We can just blindingly mark grey any references to the covered area,
     * and update the leftmost and rightmost marked positions.
     */
    class RootCellVisitor extends PointerIndexVisitor implements CellVisitor {
        @INLINE
        protected final void visit(Pointer cell) {
            if (cell.greaterEqual(coveredAreaStart)) {
                markGrey_(cell);
                if (cell.lessThan(leftmost)) {
                    leftmost = cell;
                } else if (cell.greaterThan(rightmost)) {
                    rightmost = cell;
                }
            }
        }

        @Override
        public void visit(Pointer pointer, int wordIndex) {
            visit(Layout.originToCell(pointer.getGrip(wordIndex).toOrigin()));
        }

        /**
         * Visits a cell in the boot region. No need to mark the cell, it is outside of the covered area.
         *
         * @param cell a cell in 'boot region'
         */
        @Override
        public Pointer visitCell(Pointer cell) {
            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Visiting cell ");
                Log.println(cell);
                Log.unlock(lockDisabledSafepoints);
            }
            final Pointer origin = Layout.cellToOrigin(cell);
            final Grip hubGrip = Layout.readHubGrip(origin);
            visit(Layout.originToCell(hubGrip.toOrigin()));
            final Hub hub = UnsafeCast.asHub(hubGrip.toJava());

            // Update the other references in the object
            final SpecificLayout specificLayout = hub.specificLayout;
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, this);
                if (hub.isSpecialReference) {
                    SpecialReferenceManager.discoverSpecialReference(Grip.fromOrigin(origin));
                }
                return cell.plus(hub.tupleSize);
            }
            if (specificLayout.isHybridLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, this);
            } else if (specificLayout.isReferenceArrayLayout()) {
                final int length = Layout.readArrayLength(origin);
                for (int index = 0; index < length; index++) {
                    visit(Layout.originToCell(Layout.getGrip(origin, index).toOrigin()));
                }
            }
            return cell.plus(Layout.size(origin));
        }

    }
    private final RootCellVisitor rootCellVisitor = new RootCellVisitor();

    /**
     * Visiting grey cells at the finger.
     * White cells after the finger are marked grey. Those before the finger are
     * pushed on the marking stack, but aren't marked. They will be popped after
     * the marking stack reaches a drain threshold.
     * A cell pointed by the finger must have a grey mark.
     *
     * Reference outside of the covered area are ignored (they've been treated
     * as root already). By arranging for all "root" heap areas to be located
     * before the covered area in the virtual address space, we can avoid a
     * boundary check: cell after the finger are guaranteed to be in the covered area;
     * Cells before the finger must be checked against the leftmost position.
     */
    class GreyCellVisitor extends PointerIndexVisitor implements CellVisitor {
        @INLINE
        protected final void visit(Pointer cell) {
            if (cell.greaterThan(finger)) {
                // Object is after the finger. Mark grey and update rightmost if white.
                if (markGreyIfWhite(cell) && cell.greaterThan(rightmost)) {
                    rightmost = cell;
                }
            } else if (cell.greaterEqual(coveredAreaStart) && isWhite(cell)) {
                markingStack.push(cell);
            }
        }
        @INLINE
        private void visit(Grip grip) {
            visit(Layout.originToCell(grip.toOrigin()));
        }

        @Override
        public void visit(Pointer pointer, int wordIndex) {
            visit(pointer.getGrip(wordIndex).toOrigin());
        }

        @Override
        public final Pointer visitCell(Pointer cell) {
            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Visiting cell ");
                Log.println(cell);
                Log.unlock(lockDisabledSafepoints);
            }
            final Pointer origin = Layout.cellToOrigin(cell);
            final Grip hubGrip = Layout.readHubGrip(origin);
            finger = cell;
            visit(hubGrip);
            final Hub hub = UnsafeCast.asHub(hubGrip.toJava());

            // Update the other references in the object
            final SpecificLayout specificLayout = hub.specificLayout;
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, this);
                if (hub.isSpecialReference) {
                    SpecialReferenceManager.discoverSpecialReference(Grip.fromOrigin(origin));
                }
                return cell.plus(hub.tupleSize);
            }
            if (specificLayout.isHybridLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, this);
            } else if (specificLayout.isReferenceArrayLayout()) {
                final int length = Layout.readArrayLength(origin);
                for (int index = 0; index < length; index++) {
                    visit(Layout.getGrip(origin, index));
                }
            }
            markBlackFromGrey_(cell);
            return cell.plus(Layout.size(origin));
        }
    }

    /**
     * Visiting a cell popped from the marking stack.
     * The only difference is that the visited cells aren't marked.
     * Further, the caller doesn't use the return value of the visit method, so we can simply return the zero value.
     */
    class PoppedCellVisitor extends PointerIndexVisitor implements CellVisitor {
        @INLINE
        protected final void visit(Pointer cell) {
            if (cell.greaterEqual(finger)) {
                // Object is after the finger. Mark grey and update rightmost if white.
                if (markGreyIfWhite(cell) && cell.greaterThan(rightmost)) {
                    rightmost = cell;
                }
            } else if (cell.greaterEqual(coveredAreaStart) && isWhite(cell)) {
                markingStack.push(cell);
            }
        }

        @INLINE
        private void visit(Grip grip) {
            visit(Layout.originToCell(grip.toOrigin()));
        }

        @Override
        public void visit(Pointer pointer, int wordIndex) {
            visit(pointer.getGrip(wordIndex));
        }

        @Override
        public final Pointer visitCell(Pointer cell) {
            FatalError.check(cell.lessThan(finger), "Cell must be before the finger");
            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Visiting cell ");
                Log.println(cell);
                Log.unlock(lockDisabledSafepoints);
            }
            final Pointer origin = Layout.cellToOrigin(cell);
            final Grip hubGrip = Layout.readHubGrip(origin);
            visit(hubGrip);
            final Hub hub = UnsafeCast.asHub(hubGrip.toJava());

            // Update the other references in the object
            final SpecificLayout specificLayout = hub.specificLayout;
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, this);
                if (hub.isSpecialReference) {
                    SpecialReferenceManager.discoverSpecialReference(Grip.fromOrigin(origin));
                }
                return Pointer.zero(); // caller doesn't care, so return zero.
            }
            if (specificLayout.isHybridLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, this);
            } else if (specificLayout.isReferenceArrayLayout()) {
                final int length = Layout.readArrayLength(origin);
                for (int index = 0; index < length; index++) {
                    visit(Layout.getGrip(origin, index));
                }
            }
            markBlackFromWhite(cell);
            return Pointer.zero(); // caller doesn't care, so return zero.
        }
    }
    /**
     * Visitor for grey cell at finger position.
     */
    private final CellVisitor greyCellVisitor = new GreyCellVisitor();
    private final CellVisitor poppedCellVisitor = new PoppedCellVisitor();

    /**
     * Scanning of strong roots external to the heap and boot region (namely, thread stacks and live monitors).
     */
    private final SequentialHeapRootsScanner heapRootsScanner = new SequentialHeapRootsScanner(rootCellVisitor);

    void markBootHeap() {
        Heap.bootHeapRegion.visitReferences(rootCellVisitor);
    }


    void markCode() {
        // References in the boot code region are immutable and only ever refer
        // to objects in the boot heap region.
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
        leftmost = coveredAreaEnd.asPointer();
        rightmost = coveredAreaStart.asPointer();

        // Mark all out of heap roots first (i.e., thread).
        // This only needs setting grey marks blindly (there are no black mark at this stage).
        heapRootsScanner.run();
        // Next, mark all reachable from the boot area.
        markBootHeap();
        markImmortalHeap();
    }

    // FIXME: Maybe this should be turn into a more general "ScanState" with two
    // implementation: one for normal scan, one for overflow scan.
    // Further, the scan state may also implement the CellVisitor and Pointer Index visitor?
    interface RightmostMark {
        int rightmostBitmapWordIndex();
    }

    final class NormalScanRightmostMark implements RightmostMark {
        @INLINE
        public int rightmostBitmapWordIndex() {
            return bitmapWordIndex(bitIndexOf(rightmost));
        }
    }

    final class OverflowScanRightmostMark implements RightmostMark {
        private int fingerBitmapWordIndex;

        void updateFinger() {
            fingerBitmapWordIndex = bitmapWordIndex(bitIndexOf(finger));
        }
        @INLINE
        public int rightmostBitmapWordIndex() {
            return fingerBitmapWordIndex;
        }
    }

    private final RightmostMark normalRightmostMark = new NormalScanRightmostMark();
    private final OverflowScanRightmostMark overflowRightmostMark = new OverflowScanRightmostMark();

    /**
     * Search the tricolor mark bitmap for a grey object in a specific region of the heap.
     * @param start start of the heap region.
     * @param end end of the heap region.
     * @return Return the bit index to the first grey mark found if any, -1 otherwise.
     */
    public int firstGreyMarks(Pointer start, Pointer end) {
        final Pointer colorMapBase = bitmapWordPointerAt(0);
        final int lastBitIndex = bitIndexOf(end);
        final int lastBitmapWordIndex = bitmapWordIndex(lastBitIndex);
        int bitmapWordIndex = bitmapWordIndex(bitIndexOf(start));
        while (bitmapWordIndex <= lastBitmapWordIndex) {
            long bitmapWord = colorMapBase.getLong(bitmapWordIndex);
            if (bitmapWord != 0) {
                final long greyMarksInWord = bitmapWord & (bitmapWord >> 1);
                if (greyMarksInWord != 0) {
                    // First grey mark is the least set bit.
                    final int bitIndexInWord = Pointer.fromLong(greyMarksInWord).leastSignificantBitSet();
                    final int bitIndexOfGreyMark = (bitmapWordIndex << Word.width()) + bitIndexInWord;
                    return bitIndexOfGreyMark < lastBitIndex ? bitIndexOfGreyMark : -1;
                } else if ((bitmapWord >> LAST_BIT_INDEX_IN_WORD) == 1) {
                    // Mark span two words. Check first bit of next word to decide if mark is grey.
                    bitmapWord = colorMapBase.getLong(bitmapWordIndex + 1);
                    if ((bitmapWord & 1) != 0) {
                        // it is a grey object.
                        final int bitIndexOfGreyMark = (bitmapWordIndex << Word.width()) + LAST_BIT_INDEX_IN_WORD;
                        return bitIndexOfGreyMark < lastBitIndex ? bitIndexOfGreyMark : -1;
                    }
                }
            }
            bitmapWordIndex++;
        }
        return -1;
    }

    /**
     * Verifies that a heap region has no grey objects.
     * @param start start of the region
     * @param end end of the region
     * @return true if the region has no grey objects, false otherwise.
     */
    public boolean verifyHasNoGreyMarks(Pointer start, Pointer end) {
        return firstGreyMarks(start, end) < 0;
    }

    public void visitGreyObjects(Pointer start, RightmostMark rightmostMark) {
        final Pointer colorMapBase = bitmapWordPointerAt(0);
        int rightmostBitmapWordIndex = rightmostMark.rightmostBitmapWordIndex();
        Pointer p = greyCellVisitor.visitCell(start);

        int bitmapWordIndex = bitmapWordIndex(bitIndexOf(p));
        do {
            while (bitmapWordIndex <= rightmostBitmapWordIndex) {
                long bitmapWord = colorMapBase.getLong(bitmapWordIndex);
                if (bitmapWord != 0) {
                    final long greyMarksInWord = bitmapWord & (bitmapWord >> 1);
                    if (greyMarksInWord != 0) {
                        // First grey mark is the least set bit.
                        final int bitIndexInWord = Pointer.fromLong(greyMarksInWord).leastSignificantBitSet();
                        final int bitIndexOfGreyMark = (bitmapWordIndex << Word.width()) + bitIndexInWord;
                        p = addressOf(bitIndexOfGreyMark).asPointer();
                        p = greyCellVisitor.visitCell(p);
                        bitmapWordIndex = bitmapWordIndex(bitIndexOf(p));
                        continue;
                    } else if ((bitmapWord >> LAST_BIT_INDEX_IN_WORD) == 1) {
                        // Mark span two words. Check first bit of next word to decide if mark is grey.
                        bitmapWord = colorMapBase.getLong(bitmapWordIndex + 1);
                        if ((bitmapWord & 1) != 0) {
                            // it is a grey object.
                            final int bitIndexOfGreyMark = (bitmapWordIndex << Word.width()) + LAST_BIT_INDEX_IN_WORD;
                            p = addressOf(bitIndexOfGreyMark).asPointer();
                            p = greyCellVisitor.visitCell(p);
                            bitmapWordIndex = bitmapWordIndex(bitIndexOf(p));
                            continue;
                        }
                    }
                }
                bitmapWordIndex++;
            }
            // There might be some objects left in the marking stack. Drain it.
            markingStack.flush();
            // Rightmost may have been updated. Check for this, and loop back if it has.
            final int b = rightmostMark.rightmostBitmapWordIndex();
            if (b <= rightmostBitmapWordIndex) {
                // We're done.
                break;
            }
            rightmostBitmapWordIndex = b;
        } while(true);
    }

    /**
     * Alternative to visit grey object. Here, we assume that object after the finger are marked black directly.
     *
     * @param start
     * @param rightmostMark
     */
    public void scanForward(Pointer start, RightmostMark rightmostMark) {
        final Pointer colorMapBase = bitmapWordPointerAt(0);
        int rightmostBitmapWordIndex = rightmostMark.rightmostBitmapWordIndex();
        Pointer p = greyCellVisitor.visitCell(start);

        int bitmapWordIndex = bitmapWordIndex(bitIndexOf(p));
        do {
            while (bitmapWordIndex <= rightmostBitmapWordIndex) {
                long bitmapWord = colorMapBase.getLong(bitmapWordIndex);
                if (bitmapWord != 0) {
                    // At least one mark is set.
                    int bitIndexInWord = 0;
                    final int bitmapWordStart = bitmapWordIndex << Word.width();
                    do {
                        // First mark is the least set bit.
                        bitIndexInWord += Pointer.fromLong(bitmapWord).leastSignificantBitSet();
                        final int bitIndexOfGreyMark = bitmapWordStart + bitIndexInWord;
                        p = addressOf(bitIndexOfGreyMark).asPointer();
                        greyCellVisitor.visitCell(p);
                        // Need to read the mark bitmap word again in case a bit was set in the bitmap word by the visitor.
                        // We right shift it to flush the mark bit already processed.
                        // First, we increase the cursor in the current word by two, to skip the current color mark (2-bit wide).
                        bitIndexInWord += 2;
                        bitmapWord = colorMapBase.getLong(bitmapWordIndex) >> bitIndexInWord;
                    } while (bitmapWord != 0);
                } else {
                    bitmapWordIndex++;
                }
            }
            // There might be some objects left in the marking stack. Drain it.
            markingStack.flush();
            // Rightmost may have been updated. Check for this, and loop back if it has.
            final int b = rightmostMark.rightmostBitmapWordIndex();
            if (b <= rightmostBitmapWordIndex) {
                // We're done.
                break;
            }
            rightmostBitmapWordIndex = b;
        } while(true);
    }
    /**
     * A convenience class to flush the marking stack.
     */
    final class FlushMarkingStackCellVisitor implements CellVisitor {
        Pointer leftmostFlushed;
        void reset() {
            leftmostFlushed = finger;
        }

        @Override
        public Pointer visitCell(Pointer cell) {
            // Mark grey and record leftmost mark.
            markGrey(cell);
            if (cell.lessThan(leftmostFlushed)) {
                leftmostFlushed = cell;
            }
            return null; // doesn't matter what we return here.
        }
    }

    final FlushMarkingStackCellVisitor flushMarkingStackCellVisitor = new FlushMarkingStackCellVisitor();

    public void recoverFromOverflow() {
        // First, flush the marking stack, greying all objects in there,
        // and tracking the left most grey.
        // TODO: rescan map.
        flushMarkingStackCellVisitor.reset();
        markingStack.setCellVisitor(flushMarkingStackCellVisitor);
        markingStack.flush();
        Pointer leftmostFlushed = flushMarkingStackCellVisitor.leftmostFlushed;
        // Next, initiate scanning to recover from overflow. This consists of
        // marking grey objects between the leftmost flushed mark and the finger.
        // As for a normal scan, any reference pointing after the finger are marked grey and
        // the rightmost mark of the normal scan is updated.
        // Any reference before the finger are pushed on the marking stack.
        // The scan stops when reaching the finger (which act
        // as the rightmost bound for this scan).
        // If the marking stack overflow again, we flush the stack again, write down the leftmost mark
        // for the next scan.

        markingStack.setCellVisitor(poppedCellVisitor);
        if (!recovering) {
            recovering = true;
            startOfNextOverflowScan = leftmostFlushed;
            overflowRightmostMark.updateFinger();
            do {
                startOfCurrentOverflowScan = startOfNextOverflowScan;
                startOfNextOverflowScan = finger;
                visitGreyObjects(startOfCurrentOverflowScan, overflowRightmostMark);
            } while (startOfNextOverflowScan.lessThan(finger));
            recovering = false;
        } else if (leftmostFlushed.lessThan(startOfNextOverflowScan)) {
            startOfNextOverflowScan = leftmostFlushed;
        }
    }

    /**
     * Visit all objects marked grey during root marking.
     */
    void visitAllGreyObjects() {
        markingStack.setCellVisitor(poppedCellVisitor);
        visitGreyObjects(leftmost, normalRightmostMark);
    }

    public void markAll() {
        markRoots();
        visitAllGreyObjects();
    }
}
