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
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * An allocator that allocates space linearly by atomically increasing a pointer to a contiguous chunks of memory.
 * The allocator is associated with a refill manager that takes care of refilling the allocator when this one
 * runs out of space, and servicing requires for objects larger than what the allocator can handle.
 *
 *
 * @author Laurent Daynes
 */
public class LinearSpaceAllocator {

    @CONSTANT_WHEN_NOT_ZERO
    protected static int TOP_OFFSET;

    /**
     * Space allocator capable of refilling the linear space allocator with contiguous regions, or
     * to handle allocation requests that the linear space allocator cannot handle.
     */
    abstract static class RefillManager {
        /**
         * Called directly by the allocator if it cannot handle the specified size,
         * either because the object is deemed too large for this allocator, or because there isn't enough
         * space left to satisfy request and the refill manager rejected the refill.
         *
         * @param size number of bytes requested
         * @return the address to a contiguous region of the requested size
         */
        abstract Address allocate(Size size);

        /**
         * Tell whether the amount of space left warrants a refill.
         * @param requestedSpace initial space requested
         * @param spaceLeft space left in the allocator requesting refill
         *
         * @return
         */
        abstract boolean shouldRefill(Size requestedSpace, Size spaceLeft);

        /**
         * Dispose of the contiguous space left in the allocator and return a new chunk of memory to refill it.
         *
         * @param startOfSpaceLeft address of the first byte of the space left at the end of the linear space allocator being asking for refill.
         * @param spaceLeft size, in bytes, of the space left
         * @return
         */
        abstract Address refill(Pointer startOfSpaceLeft, Size spaceLeft);
    }

    /**
     * The allocation hand of the allocator.
     */
    protected volatile Address top;

    /**
     * Soft-end of the contiguous region of memory the allocator allocate from.
     * The {@link #headroom} controls how far from the actual end of the region
     */
    protected Address end;

    /**
     * Start of the contiguous region of memory the allocator allocate from.
     */
    protected Address start;

    /**
     * Maximum size one can allocate with this allocator. Request for size larger than this
     * gets delegated to the allocation failure handler.
     */
    protected Size sizeLimit;

    /**
     * Size to reserve at the end of the allocator to guarantee that a dead object can always be
     * appended to a TLAB to fill unused space before a TLAB refill.
     * The headroom is used to compute a soft limit that'll be used as the tlab's top.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected Size headroom = HeapSchemeAdaptor.MIN_OBJECT_SIZE;

    protected final RefillManager refillManager;

    LinearSpaceAllocator(RefillManager refillManager) {
        this.refillManager = refillManager;
    }

    @INLINE
    final Object refillLock() {
        return this;
    }

    void clear() {
        start = Address.zero();
        end = Address.zero();
        top = Address.zero();
    }


    void refill(Address chunk, Size chunkSize) {
        // Make sure we can cause any attempt to allocate to fail, regardless of the
        // value of top
        end = Address.zero();
        // Now refill.
        start = chunk;
        top = start;
        end = chunk.plus(chunkSize).minus(headroom);
    }

    @HOSTED_ONLY
    public static void hostInitialize() {
        TOP_OFFSET = ClassRegistry.findField(LinearSpaceAllocator.class, "top").offset();
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

    /**
     * Size of the contiguous region of memory the allocator allocate from.
     * @return size in bytes
     */
    Size size() {
        return hardLimit().minus(start).asSize();
    }

    Size usedSpace() {
        return top.minus(start).asSize();
    }

    Size freeSpace() {
        return hardLimit().minus(top).asSize();
    }

    final RefillManager refillManager() {
        return refillManager;
    }

    @INLINE
    final Address hardLimit() {
        return end.plus(headroom);
    }

    /**
     * Bring the allocation hand to the top of the space allocator to force failure of any subsequent
     * allocation.
     * Should only be used while holding the allocator's lock to prevent
     * concurrent threads from refilling the allocator and changing the hard limit.
     * @return
     */
    @INLINE
    final Pointer setTopToLimit() {
        Pointer thisAddress = Reference.fromJava(this).toOrigin();
        Address cell;
        Address hardLimit = hardLimit();
        do {
            cell = top;
            if (cell.equals(hardLimit)) {
                // Already at end
                return cell.asPointer();
            }
        } while(thisAddress.compareAndSwapWord(TOP_OFFSET, cell, hardLimit) != cell);
        return cell.asPointer();
    }

    @INLINE
    final boolean isLarge(Size size) {
        return size.greaterThan(sizeLimit);
    }

    Pointer refillOrAllocate(Size size) {
        synchronized (refillLock()) {
            // We're the only thread that can refill the allocator now.
            // We're still racing with other threads that might try to allocate
            // what's left in the allocator (and succeed!).
            if (isLarge(size)) {
                return refillManager.allocate(size).asPointer();
            }
            // We may have raced with another concurrent thread which may have
            // refilled the allocator.
            Pointer cell = top.asPointer();

            if (cell.plus(size).greaterThan(end)) {
                Address hardLimit = hardLimit();
                if (cell.plus(size).equals(hardLimit)) {
                    // We need to atomically change top
                    Pointer start = setTopToLimit();
                    if (cell.equals(start)) {
                        return cell;
                    }
                    // Lost the race
                    cell = start;
                }
                if (!refillManager.shouldRefill(size, hardLimit.minus(cell).asSize())) {
                    // Don't refill, waste would be too high. Allocate from the bin table.
                    return refillManager.allocate(size).asPointer();
                }
                // Refill. First, fill up the allocator to bring everyone to refill synchronization.
                Pointer startOfSpaceLeft = setTopToLimit();

                Address chunk = refillManager.refill(startOfSpaceLeft, hardLimit.minus(startOfSpaceLeft).asSize());
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
    /**
     * Allocate a zeroed-out space of the specified size.
     *
     * @param size size requested in bytes.
     * @return
     */
    @UNINTERRUPTIBLE("object allocation and initialization must be atomic")
    public final Pointer allocateCleared(Size size) {
        if (MaxineVM.isDebug()) {
            FatalError.check(size.isWordAligned(), "Size must be word aligned");
        }
        // Try first a non-blocking allocation out of the current chunk.
        // This may fail for a variety of reasons, all captured by the test
        // against the current chunk limit.
        Pointer thisAddress = Reference.fromJava(this).toOrigin();
        Pointer cell;
        Pointer newTop;
        do {
            cell = top.asPointer();
            newTop = cell.plus(size);
            while (newTop.greaterThan(end)) {
                cell = refillOrAllocate(size);
                if (!cell.isZero()) {
                    Memory.clearWords(cell, size.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt());
                    return cell;
                }
                // loop back to retry.
                cell = top.asPointer();
                newTop = cell.plus(size);
            }
        } while (thisAddress.compareAndSwapWord(TOP_OFFSET, cell, newTop) != cell);
        Memory.clearWords(cell, size.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt());
        return cell;
    }

    void makeParsable() {
        Pointer cell = setTopToLimit();
        Pointer hardLimit = hardLimit().asPointer();
        if (cell.lessThan(hardLimit)) {
            HeapSchemeAdaptor.fillWithDeadObject(cell.asPointer(), hardLimit);
        }
    }
}
