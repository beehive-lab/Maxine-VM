/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.debug.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;

/**
 */
public class LinearAllocatorRegion extends LinearAllocationMemoryRegion {

    public void setMark(Address mark) {
        this.mark.set(mark.wordAligned());
    }

    public LinearAllocatorRegion(String description) {
        setRegionName(description);
    }

    public LinearAllocatorRegion(Address start, Size size, String description) {
        super(start, size);
        mark.set(start.wordAligned());
        setRegionName(description);
    }

    public Size allocationSize(Size cellSize) {
        return DebugHeap.adjustForDebugTag(cellSize.asPointer()).asSize();
    }

    /**
     * Allocates some memory from this region.
     *
     * Garbage Collector considerations:
     *
     * If the garbage collector expects to be able to scan this memory region as a sequence of
     * well-formed, contiguous objects between {@link #start()} and {@link #mark()}, then
     * the caller(s) of this method must ensure that safepoints are {@linkplain SafepointPoll#disable() disabled}
     * until the space allocated by this call has been initialized with the appropriate object header(s).
     *
     * Thread safety considerations: The caller(s) are responsible for ensuring that calls to this
     * method are synchronized. Failure to do so will leave the {@link #mark()} in an inconsistent state.
     *
     * @param size the requested cell size to be allocated. This value must be {@linkplain Address#isWordAligned() word aligned}.
     * @param adjustForDebugTag specifies if an extra word is to be reserved before the cell for the debug tag word
     */
    public Pointer allocate(Size size, boolean adjustForDebugTag) {
        if (!size.isWordAligned()) {
            FatalError.unexpected("Allocation size must be word aligned");
        }

        Pointer oldAllocationMark = mark();
        Pointer cell = adjustForDebugTag ? DebugHeap.adjustForDebugTag(oldAllocationMark) : oldAllocationMark;
        Address end = cell.plus(size);
        if (end.greaterThan(end())) {
            if (MaxineVM.isHosted()) {
                throw ProgramError.unexpected("out of space in linear allocator region");
            }
            return Pointer.zero();
        }
        setMark(end);
        return cell;
    }

    /**
     * Set size according to the current allocations. This also effectively disables further allocations.
     */
    @HOSTED_ONLY
    public void trim() {
        setSize(getAllocationMark().minus(start()).asSize());
    }

    @INLINE
    private void scanReferences(PointerIndexVisitor pointerIndexVisitor,  Pointer refMap, int refmapWordIndex, boolean logging) {
        final int firstWordIndex = refmapWordIndex << Word.widthValue().log2numberOfBits;
        final Pointer base = start.plusWords(firstWordIndex).asPointer();
        long refmapWord = refMap.getLong(refmapWordIndex);
        long w = refmapWord;
        int bitIndexInWord = 0;
        while (w != 0L) {
            bitIndexInWord += Address.fromLong(w).leastSignificantBitSet();
            if (logging) {
                final Address value = base.getWord(bitIndexInWord).asAddress();
                if (!value.isZero() && !contains(value) && !Code.bootCodeRegion().contains(value)) {
                    final Pointer address = base.plusWords(bitIndexInWord);
                    Heap.rootScanLogger.logVisitReferenceMapSlot(firstWordIndex + bitIndexInWord, address, value);
                }
            }
            pointerIndexVisitor.visit(base, bitIndexInWord);
            if (++bitIndexInWord == 64) {
                return;
            }
            w = refmapWord >>> bitIndexInWord;
        }
    }

    @INLINE
    private void scanReferences(PointerIndexVisitor pointerIndexVisitor,  Pointer refMap, int refmapWordIndex, int firstBit, int lastBit, boolean logging) {
        final int firstWordIndex = refmapWordIndex << Word.widthValue().log2numberOfBits;
        final Pointer base = start.plusWords(firstWordIndex).asPointer();
        Address refmapWord = refMap.getWord(refmapWordIndex).asAddress();
        Address w = refmapWord.unsignedShiftedRight(firstBit);
        int bitIndex = firstBit;
        while (!w.isZero()) {
            bitIndex += w.leastSignificantBitSet();
            if (bitIndex >= lastBit) {
                return;
            }
            if (logging) {
                final Pointer address = base.plusWords(bitIndex);
                final Address value = address.getWord().asAddress();
                if (!value.isZero() && !contains(value) && !Code.bootCodeRegion().contains(value)) {
                    Heap.rootScanLogger.logVisitReferenceMapSlot(firstWordIndex + bitIndex, address, value);
                }
            }
            pointerIndexVisitor.visit(base, bitIndex);
            w = refmapWord.unsignedShiftedRight(++bitIndex);
        }
    }

    @INLINE
    private void scanReferences(PointerIndexVisitor pointerIndexVisitor, Pointer refMap, int firstBitIndex, int lastBitIndex, boolean logging) {
        final int mask = Word.widthValue().log2numberOfBits - 1;
        final int firstRefMapWordIndex = firstBitIndex >> Word.widthValue().log2numberOfBits;
        final int lastRefMapWordIndex = lastBitIndex >> Word.widthValue().log2numberOfBits;
        int refMapWordIndex = firstRefMapWordIndex;
        int nextBitIndex = firstBitIndex;
        if (refMapWordIndex < lastRefMapWordIndex) {
            if ((firstBitIndex  & mask) != 0) {
                // first bit is not at a word boundary. Scan reference from the first bit index to the last bit of the current refmap word
                final int lastBit = (refMapWordIndex + 1) << Word.widthValue().log2numberOfBits;
                scanReferences(pointerIndexVisitor, refMap, refMapWordIndex++, firstBitIndex, lastBit, logging);
            }
            while (refMapWordIndex < lastRefMapWordIndex) {
                scanReferences(pointerIndexVisitor, refMap, refMapWordIndex++, logging);
            }
            nextBitIndex = lastBitIndex  & ~mask;
        }
        if ((lastBitIndex  & mask) != 0) {
            scanReferences(pointerIndexVisitor, refMap, refMapWordIndex, nextBitIndex, lastBitIndex, logging);
        } else {
            scanReferences(pointerIndexVisitor, refMap, refMapWordIndex, logging);
        }
    }

    @INLINE
    protected final void scanReferences(PointerIndexVisitor pointerIndexVisitor, byte [] referenceMapBytes, Address rangeStart, Address rangeEnd, boolean logging) {
        final int firstBitIndex = rangeStart.minus(start).unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
        final int lastBitIndex = rangeEnd.minus(start).unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
        final Pointer refMap =  ArrayAccess.elementPointer(referenceMapBytes, 0);
        scanReferences(pointerIndexVisitor, refMap, firstBitIndex, lastBitIndex, logging);
    }

    @INLINE
    protected final void scanReferenceMap(PointerIndexVisitor pointerIndexVisitor, byte [] referenceMapBytes, int refMapWords, boolean logging) {
        final Pointer refMap =  ArrayAccess.elementPointer(referenceMapBytes, 0);
        int refMapWordIndex = 0;
        while (refMapWordIndex < refMapWords) {
            scanReferences(pointerIndexVisitor, refMap, refMapWordIndex++, logging);
        }
    }
}
