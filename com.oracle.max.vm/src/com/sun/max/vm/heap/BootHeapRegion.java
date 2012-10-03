/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap;

import com.oracle.max.cri.intrinsics.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.debug.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * The special region describing the heap in the boot image.
 */
public final class BootHeapRegion extends LinearAllocatorRegion {

    private byte[] referenceMapBytes;

    private Pointer referenceMap;

    private java.lang.ref.Reference[] specialReferences = {};

    public BootHeapRegion(Address start, Size size, String description) {
        super(start, size, description);
    }

    @HOSTED_ONLY
    public void init(byte[] refMap, java.lang.ref.Reference[] specialRefs) {
        ProgramError.check(Address.fromInt(refMap.length).isWordAligned(), "Boot heap reference map must have word-aligned size");
        this.referenceMapBytes = refMap;
        this.specialReferences = specialRefs;
    }

    @INLINE
    public void discoverSpecialReference() {
        for (java.lang.ref.Reference specialReference : specialReferences) {
            SpecialReferenceManager.discoverSpecialReference(Reference.fromJava(specialReference).toOrigin());
        }
    }

    public void visitReferences(PointerIndexVisitor pointerIndexVisitor) {
        if (referenceMap.isZero()) {
            referenceMap = ArrayAccess.elementPointer(referenceMapBytes, 0);
        }
        final int referenceMapWords = UnsignedMath.divide(referenceMapBytes.length, Word.size());
        if (Heap.logRootScanning()) {
            Heap.rootScanLogger.logScanningBootHeap(this, start().plus(referenceMapWords * Word.size()));
            scanReferenceMap(pointerIndexVisitor, referenceMap, referenceMapWords, true);
        } else {
            scanReferenceMap(pointerIndexVisitor, referenceMap, referenceMapWords, false);
        }
        discoverSpecialReference();
    }

    /**
     * Visit references comprised in the specified range within the boot heap region.
     * @param start first address (inclusive) of the range
     * @param end last address (exclusive) of the range
     * @param pointerIndexVisitor
     */
    public void visitReferences(Address start, Address end, PointerIndexVisitor pointerIndexVisitor) {
        if (referenceMap.isZero()) {
            referenceMap = ArrayAccess.elementPointer(referenceMapBytes, 0);
        }
        FatalError.check(contains(start) && contains(end), "range not in boot heap region");
        if (Heap.logRootScanning()) {
            // Heap.rootScanLogger.logScanningBootHeap(this, start().plus(referenceMapWords * Word.size()));
            scanReferences(pointerIndexVisitor, referenceMap, start, end, true);
        } else {
            scanReferences(pointerIndexVisitor, referenceMap, start, end, false);
        }
    }

    public int referenceMapIndex(Address referenceLocation) {
        if (contains(referenceLocation)) {
            return referenceLocation.minus(start).unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
        }
        return -1;
    }

    public boolean isMutableReference(Address referenceLocation) {
        if (contains(referenceLocation)) {
            final int bitIndex = referenceLocation.minus(start).unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
            final int wordIndex = bitIndex >> Word.widthValue().log2numberOfBits;
            final   Address refmapWord = referenceMap.getWord(wordIndex).asAddress();
            final Address bitmask = Address.fromLong(1L).shiftedLeft(bitIndex);
            return refmapWord.and(bitmask).isNotZero();
        }
        return false;
    }

    public void visitCells(CellVisitor cellVisitor) {
        final Pointer firstCell = start().asPointer();
        final Pointer lastCell = mark();
        Pointer cell = firstCell;
        while (cell.isNotZero() && cell.lessThan(lastCell)) {
            cell = DebugHeap.checkDebugCellTag(firstCell, cell);
            cell = cellVisitor.visitCell(cell);
        }
    }

}

