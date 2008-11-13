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

/**
 * @author Bernd Mathiske
 */
public class LinearAllocatorHeapRegion extends RuntimeMemoryRegion implements HeapRegion {

    public void setMark(Address mark) {
        _mark = mark.aligned();
    }

    public LinearAllocatorHeapRegion(String description) {
        super();
        _mark = Address.zero();
        setDescription(description);
    }

    public LinearAllocatorHeapRegion(Address start, Size size, String description) {
        super(start, size);
        _mark = start().aligned();
        setDescription(description);
    }

    public Size allocationSize(Size cellSize) {
        return VMConfiguration.target().debugging() ? cellSize.plus(VMConfiguration.target().wordWidth().numberOfBytes()) : cellSize;
    }

    public Pointer allocateCell(Size cellSize) {
        assert _mark.isAligned();
        final Pointer cellStart = VMConfiguration.target().debugging() ? _mark.plus(Word.size()).asPointer() : _mark.asPointer();
        final Address cellEnd = cellStart.plus(cellSize);
        if (cellEnd.greaterThan(end())) {
            if (MaxineVM.isPrototyping()) {
                ProgramError.unexpected("out of space in linear allocator region");
            }
            return Pointer.zero();
        }
        _mark = cellEnd.aligned();
        return cellStart;
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
        assert _mark.isAligned();
        final Pointer spaceStart = _mark.asPointer();
        final Address spaceEnd = spaceStart.plus(spaceSize);
        if (spaceEnd.greaterThan(end())) {
            return Pointer.zero();
        }
        _mark = spaceEnd.aligned();
        return spaceStart;
    }

    /**
     * Set size according to the current allocations. This also effectively disables further allocations.
     */
    public void trim() {
        setSize(getAllocationMark().minus(start()).asSize());
    }

    public void visitCells(CellVisitor cellVisitor) {
        Pointer cell = start().asPointer();
        while (cell.lessThan(_mark)) {
            if (VMConfiguration.hostOrTarget().debugging()) {
                cell = cell.plusWords(1);
                if (!DebugHeap.isValidCellTag(cell.getWord(-1))) {
                    Log.print("CELL VISITOR ERROR: missing object tag @ ");
                    Log.print(cell);
                    Log.print("(start + ");
                    Log.print(cell.minus(start()).asOffset().toInt());
                    Log.println(")");
                    System.exit(1);
                }
            }
            cell = cellVisitor.visitCell(cell);
        }
    }
}
