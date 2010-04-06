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
import com.sun.max.vm.runtime.*;

/**
 * Marking algorithm with single-bit external mark-bitmap, a marking stack and a finger.
 *
 *
 * @author Laurent Daynes
 */
public class SingleBitHeapMarker implements MarkingStack.OverflowHandler {
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
     *  Log 2 to get bitmap word index from an address.
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
    protected Pointer finger;

    /**
     * Leftmost marked position.
     */
    protected Pointer leftmost;

    /**
     * Rightmost marked position.
     */
    protected Pointer rightmost;

    /**
     * Memory where the color map is stored.
     */
    final RuntimeMemoryRegion markBitmap;


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
     * Shortcut to colorMap.start() for fast bitmap operation.
     */
    private Address base;

    /**
     * The marking stack.
     */
    private final MarkingStack markingStack;


    @INLINE
    public final boolean isCovered(Address address) {
        return address.greaterEqual(coveredAreaStart) && address.lessThan(coveredAreaEnd);
    }

    void clear() {
        Memory.clearWords(markBitmap.start().asPointer(), markBitmap.size().toInt() >> Word.widthValue().log2numberOfBytes);
    }

    public  SingleBitHeapMarker(int log2WordsCoveredPerBit) {
        wordsCoveredPerBit = 1 << log2WordsCoveredPerBit;
        log2BytesCoveredPerBit = Word.widthValue().log2numberOfBytes + log2WordsCoveredPerBit;
        log2BitmapWord = log2BytesCoveredPerBit + Word.widthValue().log2numberOfBits;
        markingStack = new MarkingStack();
        markingStack.setOverflowHandler(this);
        markBitmap = new RuntimeMemoryRegion("Mark Bitmap");
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
            FatalError.check(bitmapSize.toLong() >= coveredArea.size().dividedBy(wordsCoveredPerBit * Word.widthValue().numberOfBits).toLong(),
                            "Mark bitmap too small to cover heap");
        }
        coveredAreaStart = coveredArea.start();
        coveredAreaEnd = coveredArea.end();

        markBitmap.setStart(bitmapStorage);
        markBitmap.setSize(bitmapSize);
        baseBias = coveredArea.start().unsignedShiftedRight(log2BitmapWord).toInt();
        biasedBitmapBase = markBitmap.start().minus(baseBias);
        markingStack.initialize();

        clear();
    }

    // Pointer to the word in the bitmap containing the bit for the specified address.
    @INLINE
    final Pointer bitmapWordPointerOf(Address addr) {
        return biasedBitmapBase.asPointer().plus(addr.unsignedShiftedRight(log2BitmapWord));
    }

    @INLINE
    final Word bitmapWordOf(Address address) {
        return bitmapWordPointerOf(address).getWord(0);
    }

    /**
     * Index in the mark bitmap to the word containing the bit at the specified index.
     * @param bitIndex a bit index.
     * @return an index to a word of the mark bitmap.
     */
    @INLINE
    final int bitmapWordIndex(int bitIndex) {
        return bitIndex >> Word.widthValue().log2numberOfBits;
    }

    @INLINE
    final int bitIndexOf(Address address) {
        return address.unsignedShiftedRight(log2BytesCoveredPerBit).minus(baseBias).toInt();
    }

    @INLINE
    final Word bitmapWordAt(int bitIndex) {
        return base.asPointer().getWord(bitmapWordIndex(bitIndex));
    }

    /**
     *  Pointer to the bitmap word in the mark bitmap containing the bit at specified bit index.
     * @param bitIndex a bit index
     * @return a pointer to a word of the mark bitmap
     */
    @INLINE
    final Pointer bitmapWordPointerAt(int bitIndex) {
        return base.asPointer().plus(bitmapWordIndex(bitIndex));
    }

    @INLINE
    final Address addressOf(int bitIndex) {
        return coveredAreaStart.plus(bitIndex << log2BytesCoveredPerBit);
    }

    @INLINE
    final boolean isMarked(int bitIndex) {
        return (bitmapWordAt(bitIndex).asPointer().toLong() & (1L << bitIndexInWord(bitIndex))) != 0L;
    }

    @INLINE
    final void setMark(int bitIndex) {
        final Pointer pointer = bitmapWordPointerAt(bitIndex);
        pointer.setLong(pointer.getLong() | (1L << bitIndexInWord(bitIndex)));
    }

    /**
     * Set the mark at the specified bitIndex if not already set and return a boolean value indicating if the mark wasn't already set.
     * @return true if the mark wasn't set.
     */
    @INLINE
    final boolean markIfNotMarked(int bitIndex) {
        final long markInWord = 1L << bitIndexInWord(bitIndex);
        final Pointer pointer = bitmapWordPointerAt(bitIndex);
        final Long bitmapWord = pointer.getLong();
        if ((bitmapWord & markInWord) != 0) {
            return false;
        }
        pointer.setLong(bitmapWord | markInWord);
        return true;
    }

    @INLINE
    final void setMark(Address cell) {
        setMark(bitIndexOf(cell));
    }

    @INLINE
    final boolean isMarked(Address cell) {
        return isMarked(bitIndexOf(cell));
    }

    @INLINE
    final boolean markIfNotMarked(Address cell) {
        return markIfNotMarked(bitIndexOf(cell));
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
     * We haven't started visiting the mark bitmap.
     * We can just blindingly mark grey any references to the covered area,
     * and update the leftmost and rightmost marked positions.
     */
    class RootCellVisitor extends PointerIndexVisitor implements CellVisitor {
        @INLINE
        protected final void visit(Pointer cell) {
            if (cell.greaterEqual(coveredAreaStart)) {
                setMark(cell);
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
         * Visits a cell in the boot region. No need to mark the cell black.
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
    final RootCellVisitor rootCellVisitor = new RootCellVisitor();

    /**
     * Visiting cell at the finger during forward scan of the mark bitmap
     * There are no black cell after the finger. Marked cells are grey and must be visited.
     *
     * Colors of cell before the finger can be inferred from their mark and the mark stack.
     * If no mark is set, the cell is white and must be pushed to the mark stack.
     * In order to avoid pushing multiple times the same cell on the  mark stack, it's mark is set.
     * Thus marked cells located before the finger are black if they aren't on the mark stack,
     * grey otherwise.
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
                // Object is after the finger. Mark and update rightmost if wasn't already marked (i.e., it was a white cell).
                if (markIfNotMarked(cell) && cell.greaterThan(rightmost)) {
                    rightmost = cell;
                }
            } else if (cell.greaterEqual(coveredAreaStart) && markIfNotMarked(cell)) {
                // Cell is before the finger and wasn't marked. Push on the mark stack.
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
            FatalError.check(isMarked(cell), "cell must be marked");
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
            return cell.plus(Layout.size(origin));
        }
    }

    /**
     * Visiting a cell popped from the marking stack.
     * The only difference is that the finger isn't updated when visiting
     *  a reference popped from the marking stack. The rightmost pointer
     *  must be updated.
     *
     */
    class PoppedCellVisitor extends PointerIndexVisitor implements CellVisitor {
        @INLINE
        protected final void visit(Pointer cell) {
            if (cell.greaterThan(finger)) {
                // Object is after the finger. Mark and update rightmost if wasn't already marked (i.e., it was a white cell).
                if (markIfNotMarked(cell) && cell.greaterThan(rightmost)) {
                    rightmost = cell;
                }
            } else if (cell.greaterEqual(coveredAreaStart) && markIfNotMarked(cell)) {
                // Cell is before the finger and wasn't marked. Push on the mark stack.
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
                return Pointer.zero(); // Not used by caller, so return zero.
            }
            if (specificLayout.isHybridLayout()) {
                TupleReferenceMap.visitReferences(hub, origin, this);
            } else if (specificLayout.isReferenceArrayLayout()) {
                final int length = Layout.readArrayLength(origin);
                for (int index = 0; index < length; index++) {
                    visit(Layout.getGrip(origin, index));
                }
            }
            return Pointer.zero(); // Not used by caller, so return zero.
        }
    }

    private final CellVisitor greyCellVisitor = new GreyCellVisitor();
    private final  PoppedCellVisitor poppedCellVisitor = new PoppedCellVisitor();

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

    public void visitGreyObjects(Pointer start, RightmostMark rightmostMark) {
        final Pointer markBitmapBase = bitmapWordPointerAt(0);
        int rightmostBitmapWordIndex = rightmostMark.rightmostBitmapWordIndex();
        Pointer p = greyCellVisitor.visitCell(start);

        int bitmapWordIndex = bitmapWordIndex(bitIndexOf(p));
        do {
            while (bitmapWordIndex <= rightmostBitmapWordIndex) {
                long bitmapWord = markBitmapBase.getLong(bitmapWordIndex);
                if (bitmapWord != 0) {
                    // At least one mark is set.
                    int bitIndexInWord = 0;
                    do {
                        // First mark is the least set bit.
                        bitIndexInWord += Pointer.fromLong(bitmapWord).leastSignificantBitSet();
                        final int bitIndexOfGreyMark = (bitmapWordIndex << Word.width()) + bitIndexInWord;
                        p = addressOf(bitIndexOfGreyMark).asPointer();
                        p = greyCellVisitor.visitCell(p);
                        final  int bitmapWordIndex2 = bitmapWordIndex(bitIndexOf(p));
                        if (bitmapWordIndex2 >  bitmapWordIndex) {
                            // We change of bitmap word.
                            bitmapWordIndex = bitmapWordIndex2;
                            break;
                        }
                        // Need to read the mark bitmap word again in case a bit was set in the bitmap word by the visitor.
                        // We right shift it to flush the mark bit already processed.
                        bitmapWord = markBitmapBase.getLong(bitmapWordIndex) >> ++bitIndexInWord;
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
            if (markIfNotMarked(cell) && cell.lessThan(leftmostFlushed)) {
                leftmostFlushed = cell;
            }
            return null; // doesn't matter what we return here.
        }
    }
    final FlushMarkingStackCellVisitor flushMarkingStackCellVisitor = new FlushMarkingStackCellVisitor();

    public void recoverFromOverflow() {
        // First, flush the mark stack, i.e., mark all cells in the stack remembering the leftmost pointer.
        flushMarkingStackCellVisitor.reset();
        markingStack.setCellVisitor(flushMarkingStackCellVisitor);
        markingStack.flush();
        Pointer leftmostFlushed = flushMarkingStackCellVisitor.leftmostFlushed;
        // Next, initiate scanning to recover from overflow. This consists of
        // visiting all marked objects between the leftmost flushed mark and the finger.
        // Doing so, we're likely going to revisit black already marked object.
        // As for a normal scan, any reference pointing after the finger are marked grey and
        // the rightmost mark of the normal scan is updated.
        // Any reference before the finger are pushed on the marking stack.
        // The scan stops when reaching the finger (which act
        // as the rightmost bound for this scan).
        // If the marking stack overflow again, we flush the stack again, write down the leftmost mark
        // for the next scan.
   }

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
