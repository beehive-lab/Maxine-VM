/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.sequential.gen.semiSpace;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.heap.HeapSchemeAdaptor.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.runtime.*;


public class ContiguousAllocatingSpace<T extends BaseAtomicBumpPointerAllocator<? extends Refiller>> implements HeapSpace {
    /**
     * Contiguous space used for allocation.
     */
    @INSPECTED
    protected ContiguousHeapSpace space;
    /**
     * Allocator that allocates cells from the toSpace.
     */
    @INSPECTED
    protected T allocator;

    ContiguousAllocatingSpace(T allocator, String name) {
        this.allocator = allocator;
        this.space = new ContiguousHeapSpace(name);
    }

    public T allocator() {
        return allocator;
    }

    public void initialize(Address start, Size maxSize, Size initialSize) {
        space.setReserved(start, maxSize);
        space.growCommittedSpace(initialSize);
        allocator.refill(start, initialSize);
        // Inspector support:
        // Zero-fill  the first word of  the still virgin backing storage of the space to force the OS to map the first page in virtual memory.
        // This avoids the inspector to get DataIO Error on trying to read the bytes from the first page.
        space.start().asPointer().setWord(Word.zero());
    }

    public Size increaseSize(Size delta) {
        final Size size = space.adjustGrowth(delta);
        boolean hasGrown = space.growCommittedSpace(size);
        FatalError.check(hasGrown, "request for growing space after GC must always succeed");
        allocator.grow(size);
        return size;
    }

    public Size decreaseSize(Size delta) {
        int pageSize = platform().pageSize;
        Size size = delta.alignUp(pageSize);
        boolean hasShrunk = space.shrinkCommittedSpace(size);
        FatalError.check(hasShrunk, "request for shrinking space after GC must always succeed");
        hasShrunk = allocator.shrink(size);
        FatalError.check(hasShrunk, "request for shrinking allocator's space after GC must always succeed");
        return size;
    }

    public Size totalSpace() {
        return space.committedSize();
    }

    public Size capacity() {
        return space.size();
    }

    public Pointer allocate(Size size) {
        return allocator.allocateCleared(size);
    }

    @Override
    public Pointer allocateTLAB(Size size) {
        final Pointer tlab = allocator.allocateRaw(size);
        HeapFreeChunk.format(tlab, size);
        return tlab;
    }

    public void retireTLAB(Pointer start, Size size) {
        if (!allocator.retireTop(start, size)) {
            fillWithDeadObject(start, start.plus(size));
        }
    }

    public boolean contains(Address address) {
        return space.contains(address);
    }

    public void doBeforeGC() {
        allocator.doBeforeGC();
    }

    public void doAfterGC() {
        if (MaxineVM.isDebug()) {
            allocator.zap();
        }
        allocator.reset();
    }

    public Size freeSpace() {
        return allocator.freeSpace();
    }

    public Size usedSpace() {
        // Allocator may be refilled with the top of the space, so we need to count what's before as well.
        return allocator.usedSpace();
    }

    public void visit(CellRangeVisitor visitor) {
        visitor.visitCells(space.start(), allocator.unsafeTop());
    }

    public void visitAllocatedCells(CellVisitor visitor) {
        final Address top = allocator.unsafeTop();
        Pointer cell = space.start().asPointer();
        do {
            cell = visitor.visitCell(cell);
        } while (cell.lessThan(top));
    }

    @Override
    public SpaceBounds bounds() {
        return space.bounds();
    }

}
