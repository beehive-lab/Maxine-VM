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

import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 */
public class LinearAllocatorHeapRegion extends RuntimeMemoryRegion implements HeapRegion {

    public void setMark(Address mark) {
        this.mark.set(mark.wordAligned());
    }

    public LinearAllocatorHeapRegion(String description) {
        setDescription(description);
    }

    public LinearAllocatorHeapRegion(Address start, Size size, String description) {
        super(start, size);
        mark.set(start.wordAligned());
        setDescription(description);
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
     * @param size the requested cell size to be allocated
     * @param adjustForDebugTag specifies if an extra word is to be reserved before the cell for the debug tag word
     * @return
     */
    private Pointer allocate(Size size, boolean adjustForDebugTag) {
        if (!size.isWordAligned()) {
            FatalError.unexpected("Allocation size must be word aligned");
        }

        Pointer oldAllocationMark = mark();
        Pointer cell = adjustForDebugTag ? DebugHeap.adjustForDebugTag(oldAllocationMark) : oldAllocationMark;
        Address end = cell.plus(size);
        if (end.greaterThan(end())) {
            if (MaxineVM.isPrototyping()) {
                ProgramError.unexpected("out of space in linear allocator region");
            }
            return Pointer.zero();
        }
        setMark(end);
        return cell;
    }

    public Pointer allocateCell(Size cellSize) {
        return allocate(cellSize, true);
    }

    /**
     * Allocation of bare space.
     *
     * Unlike 'allocateCell()' it is not assumed that the contents of the allocated space is an object and therefore no
     * memory initialization of any kind is performed and the object's cell space is not extended by any extra
     * administrative data such as debug tags.
     *
     * @return start address of allocated space
     */
    public Pointer allocateSpace(Size spaceSize) {
        return allocate(spaceSize, false);
    }

    /**
     * Set size according to the current allocations. This also effectively disables further allocations.
     */
    public void trim() {
        setSize(getAllocationMark().minus(start()).asSize());
    }

    public void visitCells(CellVisitor cellVisitor) {
        Pointer cell = start().asPointer();
        while (cell.lessThan(mark())) {
            cell = DebugHeap.skipCellPadding(cell);
            if (cell.greaterEqual(mark())) {
                break;
            }
            cell = DebugHeap.checkDebugCellTag(start(), cell);
            cell = cellVisitor.visitCell(cell);
        }
    }
}
