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

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * A space allocator that allocates both small objects and TLAB.
 * The allocator is backed with a single contiguous space and perform atomic bump pointer allocation.
 * TLAB may be allocated as a linked list of non-contiguous chunk of memory if the space left in
 * the current chunk backing the allocator is too small. In this case, the allocator delegate allocation
 * to its refill manager. The refill manager will takes the left over of the space as the first .
 *
 * The allocator may be refilled with chunks of memory of various size.
 * TLAB do not have to be allocated as a single contiguous chunk, and the space
 * requested should be treated as a hint, i.e., the returned TLAB may be slightly bigger
 * or smaller if it opportunistically reduces fragmentation of the available space.
 *
 * @author Laurent Daynes
 */
public class MultiChunkTLABAllocator extends LinearSpaceAllocator {
    abstract static class RefillManager extends LinearSpaceAllocator.RefillManager {
        abstract Address allocateTLAB(Size tlabSize, Pointer leftover, Size leftoverSize);
    }

    MultiChunkTLABAllocator(RefillManager refillManager) {
        super(refillManager);
    }

    void initialize(Address initialChunk, Size initialChunkSize, Size sizeLimit, Size headroom, Size tlabMinChunkSize) {
        super.initialize(initialChunk, initialChunkSize, sizeLimit, headroom);
    }

    /**
     * Allocate TLAB.
     * The allocator try to allocate the requested TLAB from its current
     * continuous chunk of memory, and delegate to its refill manager if it can't.
     * The refill manager is free to either refill the allocator, or allocated a TLAB
     * formatted as a linked list of chunk.
     *
     * @param tlabSize
     * @return
     */
    final Pointer allocateTLAB(Size tlabSize) {
        if (MaxineVM.isDebug()) {
            FatalError.check(tlabSize.isWordAligned(), "Size must be word aligned");
        }
        // Try first a non-blocking allocation out of the current chunk.
        // This may fail for a variety of reasons, all captured by the test
        // against the current chunk limit.
        Pointer thisAddress = Reference.fromJava(this).toOrigin();
        Pointer cell;
        Pointer newTop;
        Size chunkSize;
        do {
            chunkSize = tlabSize;
            cell = top.asPointer();
            newTop = cell.plus(chunkSize);
            if (newTop.greaterThan(end)) {
                synchronized (refillLock()) {
                    cell = top.asPointer();
                    if (cell.plus(tlabSize).greaterThan(end)) {
                        // Bring allocation hand to the limit of the chunk of memory backing the allocator.
                        // We hold the refill lock so we're guaranteed that chunk will not be replaced while we're doing this.
                        Pointer startOfLeftover = setTopToLimit();
                        Size sizeOfLeftover = hardLimit().minus(startOfLeftover).asSize();
                        cell = ((RefillManager) refillManager).allocateTLAB(tlabSize, startOfLeftover, sizeOfLeftover).asPointer();
                        FatalError.check(!cell.isZero(), "Refill manager must not return a null TLAB");
                        return cell;
                    }
                    // Otherwise, we lost the race to refill the TLAB. loop back to try again.
                    newTop = cell.plus(chunkSize);
                }
            }
        } while (thisAddress.compareAndSwapWord(TOP_OFFSET, cell, newTop) != cell);
        // Format as a chunk.
        HeapFreeChunk.format(cell, tlabSize);
        return cell;
    }
}
