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

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;

/**
 * Contiguous heap storage backed up by reserved contiguous
 * virtual memory.
 *
 * @author Laurent Daynes
 */
public class ContiguousHeapSpace extends RuntimeMemoryRegion {
    private Address committedEnd;
    public ContiguousHeapSpace() {
        super();
        committedEnd = start;
    }
    public ContiguousHeapSpace(String regionName) {
        super(regionName);
        committedEnd = start;
    }

    public boolean reserve(Address atAddress, Size maxSize) {
        Pointer reservedHeapSpace = VirtualMemory.reserveMemory(atAddress, maxSize, VirtualMemory.Type.HEAP);
        if (reservedHeapSpace.isZero()) {
            return false;
        }
        start = reservedHeapSpace;
        committedEnd = start;
        size = maxSize;
        return true;
    }

    public Address committedEnd() {
        return committedEnd;
    }

    public Size committedSize() {
        return committedEnd.minus(start).asSize();
    }

    public boolean inCommittedSpace(Address address) {
        return address.greaterEqual(start) && address.lessThan(committedEnd);
    }

    public Size adjustGrowth(Size delta) {
        int pageSize = VMConfiguration.target().platform.pageSize;
        Size pageAlignedGrowth = delta.roundedUpBy(pageSize).asSize();
        Address end = end();
        if (committedEnd.plus(pageAlignedGrowth).greaterThan(end)) {
            return end.minus(committedEnd).asSize();
        }
        return pageAlignedGrowth;
    }

    public boolean growCommittedSpace(Size growth) {
        Address newCommittedEnd = committedEnd.plus(growth);
        if (MaxineVM.isDebug()) {
            FatalError.check(newCommittedEnd.lessEqual(end()), "Cannot grow beyond reserved space");
            FatalError.check(growth.isAligned(VMConfiguration.target().platform.pageSize), "Heap Growth must be page-aligned");
        }
        if (VirtualMemory.commitMemory(committedEnd, growth, VirtualMemory.Type.HEAP)) {
            committedEnd = newCommittedEnd;
            return true;
        }
        return false;
    }

    public void walkCommittedSpace(CellVisitor cellVisitor) {
        Pointer p = start.asPointer();
        while (p.lessThan(committedEnd)) {
            p = cellVisitor.visitCell(p);
        }
    }
    public boolean canGrow() {
        return committedEnd.lessThan(end());
    }
}
