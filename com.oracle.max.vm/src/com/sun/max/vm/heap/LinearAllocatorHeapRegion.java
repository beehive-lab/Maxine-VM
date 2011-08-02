/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.debug.*;
import com.sun.max.vm.runtime.*;

/**
 */
public class LinearAllocatorHeapRegion extends LinearAllocationMemoryRegion {

    public void setMark(Address mark) {
        this.mark.set(mark.wordAligned());
    }

    public LinearAllocatorHeapRegion(String description) {
        setRegionName(description);
    }

    public LinearAllocatorHeapRegion(Address start, Size size, String description) {
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
     * this caller(s) of this method must ensure that safepoints are {@linkplain Safepoint#disable() disabled}
     * until the space allocated by this call has been initialized with the appropriate object header(s).
     *
     * Thread safety considerations: The caller(s) are responsible for ensuring that calls to this
     * method are synchronized. Failure to do so will leave the {@link #mark()} in an inconsistent state.
     *
     * @param size the requested cell size to be allocated. This value must be {@linkplain Address#isWordAligned() word aligned}.
     * @param adjustForDebugTag specifies if an extra word is to be reserved before the cell for the debug tag word
     * @return
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
                ProgramError.unexpected("out of space in linear allocator region");
            }
            return Pointer.zero();
        }
        setMark(end);
        return cell;
    }

    /**
     * Set size according to the current allocations. This also effectively disables further allocations.
     */
    public void trim() {
        setSize(getAllocationMark().minus(start()).asSize());
    }

    @INLINE
    protected final void scanReferenceMap(PointerIndexVisitor pointerIndexVisitor, Pointer refMap, int refMapWords, boolean tracing) {
        for (int i = 0; i < refMapWords; ++i) {
            Address refmapWord = refMap.getWord(i).asAddress();
            if (!refmapWord.isZero()) {
                int bitIndex = 0;
                while (!refmapWord.isZero()) {
                    if (!refmapWord.and(1).isZero()) {
                        final int regionWordIndex = (i * Word.width()) + bitIndex;
                        if (tracing) {
                            final Pointer address = start().asPointer().plus(regionWordIndex * Word.size());
                            final Address value = address.readWord(0).asAddress();
                            if (!value.isZero() && !contains(value) && !Code.bootCodeRegion().contains(value)) {
                                Log.print("    Slot: ");
                                logSlot(regionWordIndex, address);
                            }
                        }
                        pointerIndexVisitor.visit(start().asPointer(), regionWordIndex);
                    }
                    refmapWord = refmapWord.dividedBy(2);
                    bitIndex++;
                }
            }
        }
    }

    /**
     * Prints the region-start relative index, absolute address and value of a word within a memory region.
     *
     * @param regionWordIndex the index of the word in a region relative to the start of the region
     * @param address the absolute address of a word within a region
     */
    protected static void logSlot(final int regionWordIndex, final Pointer address) {
        Log.print("index=");
        Log.print(regionWordIndex);
        Log.print(", address=");
        Log.print(address);
        Log.print(", value=");
        Log.println(address.readWord(0));
    }
}
