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
    @INSPECTED
    protected volatile Address top;
    /**
     * Soft-end of the contiguous region of memory the allocator allocate from.
     * The {@link #headroom} controls how far from the actual end of the region
     */
    @INSPECTED
    private Address end;
    /**
     * Start of the contiguous region of memory the allocator allocate from.
     */
    @INSPECTED
    private Address start; // keep it private, so the only way to update it is via refill / reset / clear methods.

    protected final T refillManager;

    /**
     * Maximum size one can allocate with this allocator. Request for size larger than this
     * are delegated to @link {@link RefillManager#allocateLargeRaw(Size)}.
     */
    protected Size sizeLimit;

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

    public final boolean holdsRefillLock() {
        return Thread.holdsLock(refillLock());
    }

    @INLINE
    public final T refillManager() {
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
    public final Address unsafeTop() {
        return top;
    }

    public final void unsafeSetTop(Address newTop) {
        FatalError.check(inCurrentContiguousChunk(newTop), "top must be within allocating chunk");
        top = newTop;
    }

    public final Address unsafeSetTopToLimit()  {
        final Address oldTop = top;
        top = hardLimit();
        return oldTop;
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

    public final void zap() {
        zap(hardLimit());
    }

    public final  void zapToTop() {
        zap(top);
    }

    public void initialize(Address initialChunk, Size initialChunkSize, Size sizeLimit) {
        this.sizeLimit = sizeLimit;
        if (initialChunk.isZero()) {
            clear();
        } else {
            refill(initialChunk, initialChunkSize);
        }
    }

    public final void setSizeLimit(Size sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    public final void reset() {
        top = start;
    }

    /**
     * Grow the allocator's contiguous chunk of memory.
     * Not multi-thread safe.
     * @param delta number of bytes to grow the allocator's backing storage with
     */
    public final void grow(Size delta) {
        end = end.plus(delta);
    }

    /**
     * Shrink the allocator's contiguous chunk of memory.
     * Failed if trying to shrink below the already allocated area.
     * Not multi-thread safe.
     * @param delta number of bytes to shrink the allocator's backing storage with
     */
    public final boolean shrink(Size delta) {
        final Address newEnd = end.minus(delta);
        if (newEnd.lessThan(top.plus(headroom()))) {
            return false;
        }
        end = newEnd;
        return true;
    }

    public BaseAtomicBumpPointerAllocator(T refillManager) {
        this.refillManager = refillManager;
    }

    /**
     * Size of the contiguous region of memory the allocator allocates from.
     * @return size in bytes
     */
    public final Size size() {
        return hardLimit().minus(start).asSize();
    }

    public final Size usedSpace() {
        return top.minus(start).asSize();
    }

    public final Size freeSpace() {
        return hardLimit().minus(top).asSize();
    }

    @INLINE
    public final Address hardLimit() {
        return end.plus(headroom());
    }

    public final void refill(Address chunk, Size chunkSize) {
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
        final Pointer thisAddress = Reference.fromJava(this).toOrigin();
        final Address hardLimit = hardLimit();
        Address cell;
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
     * Retire top of allocated space.
     * Cannot succeed if the retired space is not the top of the allocator (i.e., if <code> !top.equals(retiredTop.plus(retiredSize)</code>).
     *
     * @param retiredTop
     * @param retiredSize
     * @return true if succeed, false otherwise.
     */
    @NO_SAFEPOINT_POLLS("retired space may not be formatted")
    public final boolean retireTop(Address retiredTop, Size retiredSize) {
        final Pointer thisAddress = Reference.fromJava(this).toOrigin();
        final Address oldTop = retiredTop.plus(retiredSize);
        Address cell;
        do {
            cell = top;
            if (!cell.equals(oldTop)) {
                return false;
            }
        } while(thisAddress.compareAndSwapWord(topOffset(), oldTop, retiredTop) != oldTop);
        return true;
    }

    /**
     * Atomically fill up the allocator with a dead object  to make it parsable.
     */
    public final void doBeforeGC() {
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
     * Make the allocator parsable without filling up the allocator.
     * This is unsafe and should only be used when non concurrent allocation can take place.
     */
    final void unsafeMakeParsable() {
        final Address cell = top;
        if (cell.isNotZero()) {
            Address hardLimit = hardLimit();
            if (cell.lessThan(hardLimit)) {
                DarkMatter.format(cell, hardLimit);
            }
        }
    }

    protected Pointer allocateLarge(Size size) {
        FatalError.check(isLarge(size), "Size must be large");
        synchronized (refillLock()) {
            return refillManager.allocateLargeRaw(size).asPointer();
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
                if (isLarge(size)) {
                    return refillManager.allocateLargeRaw(size).asPointer();
                }
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
     * Allocate a zero-filled contiguous space of the specified size.
     *
     * @param size size requested in bytes.
     * @return pointer to zero-filled pointer to contiguous space
     */
    public abstract Pointer allocateCleared(Size size);

    /**
     * Allocate a contiguous space of the specified size.
     * The space may not be zero-filled.
     *
     * @param size size requested in bytes.
     * @return pointer to contiguous space
     */
    public abstract Pointer allocateRaw(Size size);

    /**
     * Allocate a space of the specified size.
     *
     * @param size size requested in bytes.
     * @return pointer to uncleared allocated cell
     */
    @INLINE
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    protected final Pointer bumpAllocate(Size size) {
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

    @INLINE
    protected final boolean isLarge(Size size) {
        return size.greaterThan(sizeLimit);
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
