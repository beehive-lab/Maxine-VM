/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

public abstract class BaseAtomicBumpPointerAllocator<T extends Refiller> {
    /**
     * The allocation hand of the allocator.
     */
    protected volatile Address top;
    /**
     * Soft-end of the contiguous region of memory the allocator allocate from.
     * The {@link #headroom} controls how far from the actual end of the region
     */
    private Address end;
    /**
     * Start of the contiguous region of memory the allocator allocate from.
     */
    private Address start; // keep it private, so the only way to update it is via refill / reset / clear methods.

    protected final T refillManager;

    @FOLD
    public static int topOffset() {
        return ClassActor.fromJava(BaseAtomicBumpPointerAllocator.class).findLocalInstanceFieldActor("top").offset();
    }

    @FOLD
    public static Size headroom() {
        return ClassActor.fromJava(Object.class).dynamicHub().tupleSize;
    }

    @INLINE
    protected final Object refillLock() {
        return this;
    }

    @INLINE
    final T refillManager() {
        return refillManager;
    }

    @INLINE
    public final Address start() {
        return start;
    }

    @INLINE
    public final Address end() {
        return end;
    }

    @INLINE
    public boolean inCurrentContiguousChunk(Address address) {
        return address.greaterEqual(start) && address.lessThan(end);
    }

    protected final void clear() {
        start = Address.zero();
        end = Address.zero();
        top = Address.zero();
    }

    /**
     * Zap an area in the allocator from its start up to a specified limit.
     * For debugging purpose.
     * @param limit an address in the allocator, greater or equal to the start
     */
    private void zap(Address limit) {
        Word deadMark = HeapFreeChunk.deadSpaceMark();
        Pointer p = start.asPointer();
        while (p.lessThan(limit)) {
            p.setWord(deadMark);
            p = p.plusWords(1);
        }
    }

    final public void zap() {
        zap(hardLimit());
    }

    final public void zapToTop() {
        zap(top);
    }

    void initialize(Address initialChunk, Size initialChunkSize) {
        if (initialChunk.isZero()) {
            clear();
        } else {
            refill(initialChunk, initialChunkSize);
        }
    }

    protected final void reset() {
        top = start;
    }

    public BaseAtomicBumpPointerAllocator(T refillManager) {
        this.refillManager = refillManager;
    }

    /**
     * Size of the contiguous region of memory the allocator allocates from.
     * @return size in bytes
     */
    final Size size() {
        return hardLimit().minus(start).asSize();
    }

    final Size usedSpace() {
        return top.minus(start).asSize();
    }

    protected final Size freeSpace() {
        return hardLimit().minus(top).asSize();
    }

    @INLINE
    public final Address hardLimit() {
        return end.plus(headroom());
    }

    protected final void refill(Address chunk, Size chunkSize) {
        // Make sure we can cause any attempt to allocate to fail, regardless of the
        // value of top
        end = Address.zero();
        // Now refill.
        start = chunk;
        top = start;
        end = chunk.plus(chunkSize).minus(headroom());
    }

    /**
     * Bring the allocation hand to the top of the space allocator to force failure of any subsequent allocation.
     * Should only be used while holding the allocator's lock to prevent concurrent threads from refilling the
     * allocator and changing the hard limit.
     * @return previous value of top.
     */
    @NO_SAFEPOINT_POLLS("filling linear space allocator must not be subjected to safepoints")
    protected final Pointer atomicSetTopToLimit() {
        Pointer thisAddress = Reference.fromJava(this).toOrigin();
        Address cell;
        Address hardLimit = hardLimit();
        do {
            cell = top;
            if (cell.equals(hardLimit)) {
                // Already at end
                return cell.asPointer();
            }
        } while(thisAddress.compareAndSwapWord(topOffset(), cell, hardLimit) != cell);
        return cell.asPointer();
    }

    /**
     * Atomically fill up the allocator with a dead object  to make it parsable.
     */
    protected final void doBeforeGC() {
        Pointer cell = top.asPointer();
        if (!cell.isZero()) {
            Pointer hardLimit = hardLimit().asPointer();
            top = hardLimit;
            if (cell.lessThan(hardLimit)) {
                refillManager.makeParsable(cell, hardLimit);
            }
        }
        refillManager.doBeforeGC();
    }

    /**
     * Make the allocator parseable without filling up the allocator.
     * This is unsafe and should only be used when non concurrent allocation can take place.
     */
    final void unsafeMakeParsable() {
        Pointer cell = top.asPointer();
        Pointer hardLimit = hardLimit().asPointer();
        if (cell.lessThan(hardLimit)) {
            HeapSchemeAdaptor.fillWithDeadObject(cell.asPointer(), hardLimit);
        }
    }

    protected Pointer refillOrAllocate(Size size) {
        synchronized (refillLock()) {
            // We're the only thread that can refill the allocator now.
            // We're still racing with other threads that might try to allocate
            // what's left in the allocator (and succeed!).
            // We may also have raced with another concurrent thread which may have
            // refilled the allocator.
            // The following take care of both.

            Pointer cell = top.asPointer();
            if (cell.plus(size).greaterThan(end)) {
                // end isn't the hard limit of the space.
                // Here we atomically fill up the linear space.
                // Atomically set top to the limit as we may be racing with
                // non-blocking concurrent allocator for the left over. The refillLock above
                // only protect against concurrent refiller.
               // This brings every one to the refill lock if needed. Further, if the
                // space that was left is enough to satisfy the allocation, we can just return.
                Address hardLimit = hardLimit();
                Pointer startOfSpaceLeft = atomicSetTopToLimit();

                // Check if we can use the space that was left over.
                if (cell.equals(startOfSpaceLeft) && cell.plus(size).equals(hardLimit)) {
                    return cell;
                }

                Address chunk = refillManager.allocateRefill(startOfSpaceLeft, hardLimit.minus(startOfSpaceLeft).asSize());
                if (chunk.isNotZero()) {
                    refill(chunk, HeapFreeChunk.getFreechunkSize(chunk));
                }
                // Fall-off to return to the non-blocking allocation loop.
            }
            // There was a race for refilling the allocator. Just return to the non-blocking allocation loop.
            return Pointer.zero();
        }
    }

    @INLINE
    final protected Pointer clearAllocatedCell(Pointer cell, Size size) {
        Memory.clearWords(cell, size.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt());
        return cell;
    }

    /**
     * Allocate a zeroed-out space of the specified size.
     *
     * @param size size requested in bytes.
     * @return pointer to zero-filled  allocated cell
     */
    public abstract Pointer allocateCleared(Size size);

    /**
     * Allocate a space of the specified size.
     *
     * @param size size requested in bytes.
     * @return pointer to uncleared allocated cell
     */
    @INLINE
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    protected final Pointer allocate(Size size) {
        if (MaxineVM.isDebug()) {
            FatalError.check(size.isWordAligned(), "Size must be word aligned");
        }
        // Try first a non-blocking allocation out of the current chunk.
        // This may fail for a variety of reasons, all captured by the test against the current chunk limit.
        Pointer thisAddress = Reference.fromJava(this).toOrigin();
        Pointer cell;
        Pointer newTop;
        do {
            cell = top.asPointer();
            newTop = cell.plus(size);
            while (newTop.greaterThan(end)) {
                cell = refillOrAllocate(size);
                if (!cell.isZero()) {
                    return cell;
                }
                // loop back to retry.
                cell = top.asPointer();
                newTop = cell.plus(size);
            }
        } while (thisAddress.compareAndSwapWord(topOffset(), cell, newTop) != cell);
        return cell;
    }

    /**
     * Custom allocation support.
     * @see HeapSchemeWithTLAB
     * @param object
     * @return an instance of BaseAtomicBumpPointerAllocator
     */
    @INTRINSIC(UNSAFE_CAST)
    static public native BaseAtomicBumpPointerAllocator asBumpPointerAllocator(Object object);

}
