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

/**
 * An allocator that allocates space linearly by atomically increasing a pointer to a contiguous chunk of memory.
 * The allocator is associated with a refill manager that implement a policy for deciding whether a refill is warranted and
 * provide method for allocating large size request, overflow allocation (when refill is denied) and refill allocation.
 */
public class AtomicBumpPointerAllocator<T extends RefillManager> extends BaseAtomicBumpPointerAllocator<T> {

    /**
     * Maximum size one can allocate with this allocator. Request for size larger than this
     * are delegated to @link {@link RefillManager#allocateLarge(Size)}.
     */
    protected Size sizeLimit;


    AtomicBumpPointerAllocator(T refillManager) {
        super(refillManager);
    }

    void initialize(Address initialChunk, Size initialChunkSize, Size sizeLimit, Size headroom) {
        this.sizeLimit = sizeLimit;
        this.headroom = headroom;
        if (initialChunk.isZero()) {
            clear();
        } else {
            refill(initialChunk, initialChunkSize);
        }
    }


    @INLINE
    final boolean isLarge(Size size) {
        return size.greaterThan(sizeLimit);
    }

    @Override
    Pointer refillOrAllocate(Size size) {
        synchronized (refillLock()) {
            // We're the only thread that can refill the allocator now.
            // We're still racing with other threads that might try to allocate
            // what's left in the allocator (and succeed!).
            if (isLarge(size)) {
                // FIXME(ld) does this really need to be done under the refillLock() ?
                return refillManager.allocateLarge(size).asPointer();
            }
            // We may have raced with another concurrent thread which may have
            // refilled the allocator.
            Pointer cell = top.asPointer();

            if (cell.plus(size).greaterThan(end)) {
                // end isn't the hard limit of the space.
                // Check if allocation request can fit up to the limit.
                Address hardLimit = hardLimit();
                if (cell.plus(size).equals(hardLimit)) {
                    // We need to atomically change top as we may be racing with
                    // concurrent allocator for the left over. The refillLock above
                    // only protect against concurrent refiller.
                    Pointer start = setTopToLimit();
                    if (cell.equals(start)) {
                        return cell;
                    }
                    // Lost the race. Now we definitively have no space left.
                    // Fall off to refill or allocate.
                    cell = start;
                }
                if (!refillManager.shouldRefill(size, hardLimit.minus(cell).asSize())) {
                    // Don't refill, waste would be too high. Allocate from the bin table.
                    return refillManager.allocateOverflow(size).asPointer();
                }
                // Refill. First, fill up the allocator to bring everyone to refill synchronization.
                Pointer startOfSpaceLeft = setTopToLimit();

                Address chunk = refillManager.allocateRefill(startOfSpaceLeft, hardLimit.minus(startOfSpaceLeft).asSize());
                if (!chunk.isZero()) {
                    // Won race to get a next chunk to refill the allocator.
                    refill(chunk, HeapFreeChunk.getFreechunkSize(chunk));
                }
                // Fall-off to return to the non-blocking allocation loop.
            }
            // There was a race for refilling the allocator. Just return to
            // the non-blocking allocation loop.
            return Pointer.zero();
        }
    }
}
