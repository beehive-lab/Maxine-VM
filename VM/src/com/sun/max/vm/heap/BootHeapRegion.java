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
package com.sun.max.vm.heap;

import java.lang.ref.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;

/**
 * The special region describing the heap in the boot image.
 *
 * @author Doug Simon
 */
public class BootHeapRegion extends LinearAllocatorHeapRegion implements PointerOffsetVisitor {

    private byte[] referenceMapBytes;

    private Pointer referenceMap;

    private Reference[] specialReferences;

    public BootHeapRegion(Address start, Size size, String description) {
        super(start, size, description);
    }

    @PROTOTYPE_ONLY
    public void init(byte[] refMap, Reference[] specialRefs) {
        ProgramError.check(Address.fromInt(refMap.length).isWordAligned(), "Boot heap reference map must have word-aligned size");
        this.referenceMapBytes = refMap;
        this.specialReferences = specialRefs;
    }

    private void verifyCells() {
        Pointer cell = start().asPointer();
        while (cell.lessThan(mark)) {
            if (VMConfiguration.hostOrTarget().debugging()) {
                cell = cell.plusWords(1);
                if (!DebugHeap.isValidCellTag(cell.getWord(-1))) {
                    Log.print("CELL VISITOR ERROR: missing object tag @ ");
                    Log.print(cell);
                    Log.print("(start + ");
                    Log.print(cell.minus(start()).asOffset().toInt());
                    Log.println(")");
                    FatalError.unexpected("CELL VISITOR ERROR: missing object tag");
                }
            }
            cell = verifyCell(cell);
        }
    }

    public void visitPointerOffset(Pointer pointer, int offset) {
        final Pointer address = pointer.plus(offset);
        final int heapWordIndex = address.minus(start()).dividedBy(Word.size()).toInt();
        final Address value = address.readWord(0).asAddress();
        final int bitsPerMapWord = Word.width().numberOfBits;

        final boolean isWordTagged;
        final int referenceMapWordIndex = Unsigned.idiv(heapWordIndex, bitsPerMapWord);
        final Pointer referenceMapEnd = referenceMap.plus(referenceMapBytes.length);
        if (referenceMap.plus(referenceMapWordIndex * Word.size()).lessEqual(referenceMapEnd)) {
            final Address referenceMapWord = referenceMap.getWord(referenceMapWordIndex).asAddress();
            final Address mask = Address.fromLong(1).shiftedLeft(heapWordIndex % bitsPerMapWord);
            isWordTagged = !referenceMapWord.and(mask).isZero();
        } else {
            isWordTagged = false;
        }

        if (!isWordTagged) {
            if (!value.isZero() && !contains(value) && !Code.bootCodeRegion.contains(value)) {
                Log.println("Non-tagged reference in boot heap refers to address neither in boot heap nor boot code region");
                Log.print("Slot: ");
                printSlot(heapWordIndex, address);
                FatalError.unexpected("Non-tagged reference in boot heap refers to address neither in boot heap nor boot code region");
            }
        }
    }

    private Pointer verifyCell(Pointer cell) {
        final Pointer origin = Layout.cellToOrigin(cell);
        final Grip newHubGrip = Layout.readHubGrip(origin);
        final Hub hub = UnsafeLoophole.cast(newHubGrip.toJava());
        final SpecificLayout specificLayout = hub.specificLayout();
        if (specificLayout.isTupleLayout()) {
            TupleReferenceMap.visitOriginOffsets(hub, origin, this);
            return cell.plus(hub.tupleSize());
        }
        if (specificLayout.isHybridLayout()) {
            TupleReferenceMap.visitOriginOffsets(hub, origin, this);
        } else if (specificLayout.isReferenceArrayLayout()) {
            final int length = Layout.readArrayLength(origin);
            for (int index = 0; index < length; index++) {
                final int offset = Layout.referenceArrayLayout().getElementOffsetFromOrigin(index).toInt();
                visitPointerOffset(origin, offset);
            }
        }
        return cell.plus(Layout.size(origin));
    }

    public void visitPointers(PointerIndexVisitor pointerIndexVisitor) {
        if (referenceMap.isZero()) {
            referenceMap = Grip.fromJava(referenceMapBytes).toOrigin().plus(Layout.byteArrayLayout().getElementOffsetFromOrigin(0));
        }

        final Pointer refMap = referenceMap;
        final int referenceMapWords = Unsigned.idiv(referenceMapBytes.length, Word.size());
        if (Heap.traceGCRootScanning()) {
            Log.print("Scanning boot heap: start=");
            Log.print(start());
            Log.print(", end=");
            Log.print(end());
            Log.print(", mutable references end=");
            Log.println(start().plus(referenceMapWords * Word.size()));
        }
        if (VMConfiguration.hostOrTarget().debugging()) {
            verifyCells();
        }

        if (Heap.traceGCRootScanning()) {
            scanReferenceMap(pointerIndexVisitor, refMap, referenceMapWords, true);
        } else {
            scanReferenceMap(pointerIndexVisitor, refMap, referenceMapWords, false);
        }

        for (Reference specialReference : specialReferences) {
            SpecialReferenceManager.discoverSpecialReference(Grip.fromJava(specialReference));
        }
    }

    @INLINE
    private void scanReferenceMap(PointerIndexVisitor pointerIndexVisitor, Pointer refMap, int refMapWords, boolean tracing) {
        for (int i = 0; i < refMapWords; ++i) {
            Address refmapWord = refMap.getWord(i).asAddress();
            if (!refmapWord.isZero()) {
                int bitIndex = 0;
                while (!refmapWord.isZero()) {
                    if (!refmapWord.and(1).isZero()) {
                        final int heapWordIndex = (i * Word.width().numberOfBits) + bitIndex;
                        if (tracing) {
                            final Pointer address = start().asPointer().plus(heapWordIndex * Word.size());
                            final Address value = address.readWord(0).asAddress();
                            if (!value.isZero() && !contains(value) && !Code.bootCodeRegion.contains(value)) {
                                Log.print("    Slot: ");
                                printSlot(heapWordIndex, address);
                            }
                        }
                        pointerIndexVisitor.visitPointerIndex(start().asPointer(), heapWordIndex);
                    }
                    refmapWord = refmapWord.dividedBy(2);
                    bitIndex++;
                }
            }
        }
    }

    private void printSlot(final int heapWordIndex, final Pointer address) {
        Log.print("index=");
        Log.print(heapWordIndex);
        Log.print(", address=");
        Log.print(address);
        Log.print(", value=");
        Log.println(address.readWord(0));
    }
}

