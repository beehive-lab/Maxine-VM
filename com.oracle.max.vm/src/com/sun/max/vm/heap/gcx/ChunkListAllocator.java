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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * Space allocator that allocates discontinuous chunks
 * of memory. Used for allocating TLABs made of possibly discontinuous area. TLABs allocation differs in two ways from object allocations.
 * First, the space returned may not be of the requested size: it may be slightly smaller or larger if it helps
 * avoiding fragmentation. Second, the space returned may not be a single contiguous chunks, but a linked list of
 * of chunks no smaller than a minimum size.
 */
public class ChunkListAllocator<T extends ChunkListRefillManager> extends AtomicBumpPointerAllocator<T> {
    ChunkListAllocator(T refillManager) {
        super(refillManager);
    }

    void initialize(Address initialChunk, Size initialChunkSize, Size sizeLimit, Size headroom, Size tlabMinChunkSize) {
        super.initialize(initialChunk, initialChunkSize, sizeLimit, headroom);
        refillManager.setMinChunkSize(tlabMinChunkSize);
    }

    /**
     * Allocate TLAB.
     * The allocator tries to allocate the requested TLAB from its current
     * continuous chunk of memory, and delegate to its refill manager if it can't.
     * The refill manager is free to either refill the allocator, or to allocate a TLAB
     * formatted as a linked list of chunk.
     * @see HeapFreeChunk
     *
     * @param tlabSize
     * @return a pointer to a heap free chunk.
     */
    @NO_SAFEPOINTS("non-blocking tlab allocation loop must not be subjected to safepoints")
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
        do {
            cell = top.asPointer();
            newTop = cell.plus(tlabSize);
            if (newTop.greaterThan(end)) {
                synchronized (refillLock()) {
                    cell = top.asPointer();
                    if (cell.plus(tlabSize).greaterThan(end)) {
                        // Bring allocation hand to the limit of the chunk of memory backing the allocator.
                        // We hold the refill lock so we're guaranteed that the chunk will not be replaced while we're doing this.
                        Pointer startOfLeftover = setTopToLimit();
                        Size sizeOfLeftover = hardLimit().minus(startOfLeftover).asSize();
                        cell = refillManager.allocateChunkList(tlabSize, startOfLeftover, sizeOfLeftover).asPointer();
                        FatalError.check(!cell.isZero(), "Refill manager must not return a null TLAB");
                        return cell;
                    }
                    // Otherwise, we lost the race to refill the TLAB. loop back to try again.
                    newTop = cell.plus(tlabSize);
                }
            }
        } while (thisAddress.compareAndSwapWord(TOP_OFFSET, cell, newTop) != cell);
        // Format as a chunk.
        HeapFreeChunk.format(cell, tlabSize);
        return cell;
    }
}
