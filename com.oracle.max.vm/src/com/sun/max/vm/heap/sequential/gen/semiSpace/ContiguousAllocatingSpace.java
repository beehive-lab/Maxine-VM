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

import static com.sun.max.vm.heap.HeapSchemeAdaptor.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
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

    @Override
    public Size growAfterGC(Size delta) {
        FatalError.unimplemented();
        return Size.zero();
    }
    @Override
    public Size shrinkAfterGC(Size delta) {
        // TODO Auto-generated method stub
        FatalError.unimplemented();
        return Size.zero();
    }

    @Override
    public Size totalSpace() {
        return space.committedSize();
    }

    @Override
    public Size capacity() {
        return space.size();
    }

    @Override
    public Pointer allocate(Size size) {
        return allocator.allocateCleared(size);
    }

    @Override
    public Pointer allocateTLAB(Size size) {
        // FIXME: the interface wants a HeapFreeChunk, but that shouldn't be necessary here. See how this can be changed
        // based on the code for TLAB overflow handling.
        final Pointer tlab = allocator.allocateCleared(size);
        HeapFreeChunk.format(tlab, size);
        return tlab;
    }

    public void retireTLAB(Pointer start, Size size) {
        FatalError.check(space.contains(start) && space.contains(start.plus(size.minusWords(1))), "Retired TLAB Space must be in allocating space");
        if (!allocator.retireTop(start, size)) {
            fillWithDeadObject(start, start.plus(size));
        }
    }

    @Override
    public boolean contains(Address address) {
        return space.contains(address);
    }

    @Override
    public void doBeforeGC() {
        allocator.doBeforeGC();
    }

    @Override
    public void doAfterGC() {
        if (MaxineVM.isDebug()) {
            allocator.zap();
        }
        allocator.reset();
    }

    @Override
    public Size freeSpace() {
        return allocator.freeSpace();
    }

    @Override
    public Size usedSpace() {
        // Allocator may be refilled with the top of the space, so we need to count what's before as well.
        return allocator.usedSpace();
    }

    @Override
    public void visit(HeapSpaceRangeVisitor visitor) {
        visitor.visitCells(space.start(), allocator.unsafeTop());
    }
}
