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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * Space allocator that allocates discontinuous chunks of memory.
 * Used for allocating TLABs made of possibly discontinuous area. TLABs allocation differs in two ways from object allocations.
 * First, the space returned may not be of the requested size: it may be slightly smaller or larger if it helps
 * avoiding fragmentation. Second, the space returned may not be a single contiguous chunks, but a linked list of
 * of chunks no smaller than a minimum size.
 */
public class ChunkListAllocator<T extends ChunkListRefillManager> extends BaseAtomicBumpPointerAllocator<T> {
    /**
     * Maximum size one can allocate with this allocator. Request for size larger than this
     * are delegated to @link {@link RefillManager#allocateLarge(Size)}.
     */
    protected Size sizeLimit;
    /**
     * Flags for enabling/disabling traces for this allocator.
     */
    boolean debugTrace = false;

    public ChunkListAllocator(T refillManager) {
        super(refillManager);
    }

    /**
     * Initialize the allocator with an initial refill.
     * @param initialRefillSize
     * @param sizeLimit
     */
    void initialize(Size initialRefillSize, Size sizeLimit) {
        this.sizeLimit = sizeLimit;
        super.initialize(Address.zero(), Size.zero());
        Address chunk = refillManager.allocateChunkListOrRefill(this, initialRefillSize, Pointer.zero(), Size.zero());
        // A zero chunk means the allocator is refilled -- see allocateChunkListOrRefill for details.
        FatalError.check(chunk.isZero() && start().isNotZero() && top.equals(start()) && end().greaterThan(top), "allocator must be refilled");
    }

    @INLINE
    @Override
    public Pointer allocateCleared(Size size) {
        return clearAllocatedCell(allocate(size), size);
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
    @NO_SAFEPOINT_POLLS("non-blocking tlab allocation loop must not be subjected to safepoints")
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
            if (newTop.greaterThan(end())) {
                synchronized (refillLock()) {
                    cell = top.asPointer();
                    if (cell.plus(tlabSize).greaterThan(end())) {
                        // Bring allocation hand to the limit of the chunk of memory backing the allocator.
                        // We hold the refill lock so we're guaranteed that the chunk will not be replaced while we're doing this.
                        Pointer startOfLeftover = atomicSetTopToLimit();
                        Pointer h = hardLimit().asPointer();
                        Size sizeOfLeftover = h.minus(startOfLeftover).asSize();
                        if (sizeOfLeftover.equals(tlabSize)) {
                            cell = startOfLeftover;
                            break;
                        }
                        cell = refillManager.allocateChunkListOrRefill(this, tlabSize, startOfLeftover, sizeOfLeftover).asPointer();
                        if (!cell.isZero()) {
                            return cell;
                        }
                        cell = top.asPointer();
                    }
                    // Otherwise, we lost the race to refill the TLAB. loop back to try again.
                    newTop = cell.plus(tlabSize);
                }
            }
        } while (thisAddress.compareAndSwapWord(topOffset(), cell, newTop) != cell);
        if (MaxineVM.isDebug() && debugTrace) {
            Log.print("allocateTLAB() = "); Log.print(cell); Log.println(tlabSize);
        }
        // Format as a chunk.
        HeapFreeChunk.format(cell, tlabSize);
        return cell;
    }

    void initialize(Address initialChunk, Size initialChunkSize, Size sizeLimit) {
        this.sizeLimit = sizeLimit;
        super.initialize(initialChunk, initialChunkSize);
    }

    @INLINE
    final boolean isLarge(Size size) {
        return size.greaterThan(sizeLimit);
    }

    @Override
    protected Pointer refillOrAllocate(Size size) {
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

            if (cell.plus(size).greaterThan(end())) {
                // end isn't the hard limit of the space.
                // Check if allocation request can fit up to the limit.
                Address hardLimit = hardLimit();
                if (cell.plus(size).equals(hardLimit)) {
                    // We need to atomically change top as we may be racing with
                    // concurrent allocator for the left over. The refillLock above
                    // only protect against concurrent refiller.
                    Pointer start = atomicSetTopToLimit();
                    if (cell.equals(start)) {
                        return cell;
                    }
                    // Lost the race. Now we definitively have no space left.
                    // Fall off to refill or allocate.
                    cell = start;
                }
                if (!refillManager.shouldRefill(size, hardLimit.minus(cell).asSize())) {
                    // Don't refill, waste would be too high. Allocate from the overflow allocator.
                    return refillManager.allocateOverflow(size).asPointer();
                }
                // Refill. First, fill up the allocator to bring everyone to refill synchronization.
                Pointer startOfSpaceLeft = atomicSetTopToLimit();

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
