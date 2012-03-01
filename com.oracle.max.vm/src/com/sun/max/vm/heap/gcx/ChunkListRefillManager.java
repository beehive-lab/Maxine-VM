/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A refill manager that can also hands out list of free chunks to an allocator.
 */
public abstract class ChunkListRefillManager extends RefillManager {
    /**
     * Minimum size for a chunk. The refill manager will not hand out chunks with a size smaller than this.
     */
    protected Size minChunkSize;

    ChunkListRefillManager() {
    }

    void setMinChunkSize(Size size) {
        minChunkSize = size;
    }

    abstract protected void  retireDeadSpace(Pointer deadSpace, Size size);

    abstract protected void  retireFreeSpace(Pointer freeSpace, Size size);

    /**
     * Retire space from allocator. If the space is larger than the minimum chunk size, format it as a heap
     * free chunk and return its address.
     * Otherwise, plant a dead object and return zero.
     *
     * @param address address to the first word of the region
     * @param size size of the region
     * @return a non-null address if the specified region can be used as a free chunk.
     */
    protected Address retireChunk(Pointer address, Size size) {
        if (size.greaterThan(minChunkSize)) {
            retireFreeSpace(address, size);
            return address;
        }
        if (!size.isZero()) {
            retireDeadSpace(address, size);
        }
        return Address.zero();
    }

    /**
     * Try to refill the allocator with a single contiguous range of free space large enough to accommodate the allocator, or return a list of chunk
     * large enough to satisfy the requested size. The allocator's has been filled up (with dead space for the left-over) and its refill lock is being held.
     *
     * @param allocator the allocator issuing the request
     * @param listSize
     * @param leftover pointer to space that was left in the bump pointer allocator
     * @param leftoverSize size of the space that was left in the  bump pointer allocator
     * @return a zero address if the allocator was refilled, the head of a list of free chunk otherwise
     */
    public abstract Address allocateChunkListOrRefill(ChunkListAllocator<? extends ChunkListRefillManager> allocator, Size listSize, Pointer leftover, Size leftoverSize);
}
