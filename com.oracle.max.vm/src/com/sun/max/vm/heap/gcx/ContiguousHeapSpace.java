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

import static com.sun.max.platform.Platform.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;

/**
 * Contiguous heap storage backed up by reserved contiguous
 * virtual memory.
 */
public class ContiguousHeapSpace extends MemoryRegion implements EvacuatingSpace {
    @INSPECTED
    private Address committedEnd;

    private final SpaceBounds bounds;

    private SpaceBounds createSpaceBounds() {
        return new SpaceBounds() {
            @Override
            final Address lowestAddress() {
                return start;
            }

            @Override
            final boolean isIn(Address address) {
                return address.greaterEqual(start) && address.lessThan(committedEnd);
            }

            @Override
            final boolean isContiguous() {
                return true;
            }

            @Override
            final Address highestAddress() {
                return committedEnd;
            }
        };
    }

    public ContiguousHeapSpace() {
        super();
        committedEnd = start;
        bounds = createSpaceBounds();
    }

    public ContiguousHeapSpace(String regionName) {
        super(regionName);
        committedEnd = start;
        bounds = createSpaceBounds();
    }

    public void setReserved(Address reservedStart, Size reservedSize) {
        start = reservedStart;
        committedEnd = start;
        size = reservedSize;
    }

    public boolean reserve(Address atAddress, Size maxSize) {
        Pointer reservedHeapSpace = VirtualMemory.reserveMemory(atAddress, maxSize, VirtualMemory.Type.HEAP);
        if (reservedHeapSpace.isZero()) {
            return false;
        }
        setReserved(reservedHeapSpace, maxSize);
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
        int pageSize = platform().pageSize;
        Size pageAlignedGrowth = delta.alignUp(pageSize).asSize();
        Address end = end();
        if (committedEnd.plus(pageAlignedGrowth).greaterThan(end)) {
            return end.minus(committedEnd).asSize();
        }
        return pageAlignedGrowth;
    }

    public boolean growCommittedSpace(Size delta) {
        Address newCommittedEnd = committedEnd.plus(delta);
        if (MaxineVM.isDebug()) {
            FatalError.check(newCommittedEnd.lessEqual(end()), "Cannot grow beyond reserved space");
            FatalError.check(delta.isAligned(platform().pageSize), "Heap Growth must be page-aligned");
        }
        final boolean committed = Heap.AvoidsAnonOperations || VirtualMemory.commitMemory(committedEnd, delta, VirtualMemory.Type.HEAP);
        if (committed) {
            committedEnd = newCommittedEnd;
            return true;
        }
        return false;
    }

    public boolean shrinkCommittedSpace(Size delta) {
        Address newCommittedEnd = committedEnd.minus(delta);
        if (MaxineVM.isDebug()) {
            FatalError.check(newCommittedEnd.greaterEqual(start()), "Cannot shrink more than committed space");
            FatalError.check(delta.isAligned(platform().pageSize), "Heap Growth must be page-aligned");
        }
        final boolean committed = Heap.AvoidsAnonOperations || VirtualMemory.uncommitMemory(newCommittedEnd, delta, VirtualMemory.Type.HEAP);
        if (committed) {
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

    public void doBeforeGC() {
    }

    public void doAfterGC() {
    }

    public SpaceBounds bounds() {
        return bounds;
    }
}
