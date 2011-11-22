/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

public class BaseAtomicBumpPointerAllocator<T extends Refiller> {

    @CONSTANT_WHEN_NOT_ZERO
    protected static int TOP_OFFSET;
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
     * Size to reserve at the end of the allocator to guarantee that a dead object can always be
     * appended to a TLAB to fill unused space before a TLAB refill.
     * The headroom is used to compute a soft limit that'll be used as the tlab's top.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected Size headroom;

    protected final T refillManager;

    @HOSTED_ONLY
    public static void hostInitialize() {
        TOP_OFFSET = ClassRegistry.findField(BaseAtomicBumpPointerAllocator.class, "top").offset();
    }

    @INLINE
    protected final Object refillLock() {
        return this;
    }

    final T refillManager() {
        return refillManager;
    }

    protected final void clear() {
        start = Address.zero();
        end = Address.zero();
        top = Address.zero();
    }

    void initialize(Address initialChunk, Size initialChunkSize) {
        headroom = HeapSchemeAdaptor.MIN_OBJECT_SIZE;
        if (initialChunk.isZero()) {
            clear();
        } else {
            refill(initialChunk, initialChunkSize);
        }
    }

    protected final void refill(Address chunk, Size chunkSize) {
        // Make sure we can cause any attempt to allocate to fail, regardless of the
        // value of top
        end = Address.zero();
        // Now refill.
        start = chunk;
        top = start;
        end = chunk.plus(chunkSize).minus(headroom);
    }

    public BaseAtomicBumpPointerAllocator(T refiller) {
        refillManager = refiller;
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
    protected final Address hardLimit() {
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
        } while(thisAddress.compareAndSwapWord(TOP_OFFSET, cell, hardLimit) != cell);
        return cell.asPointer();
    }

    protected final Pointer setTopToLimit() {
        Pointer cell = top.asPointer();
        top =  hardLimit().asPointer();
        return cell;
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
    protected final void unsafeMakeParsable() {
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
                // Refill. First, fill up the allocator to bring everyone to refill synchronization.
                Pointer startOfSpaceLeft = atomicSetTopToLimit();

                Address chunk = refillManager.allocateRefill(startOfSpaceLeft, hardLimit.minus(startOfSpaceLeft).asSize());
                if (MaxineVM.isDebug()) {
                    FatalError.check(!chunk.isZero(), "refill must not be null");
                }
                refill(chunk, HeapFreeChunk.getFreechunkSize(chunk));
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
     * @return pointer to zero-filled  allocated cell
     */
    public final Pointer allocateCleared(Size size) {
        Pointer cell = allocate(size);
        Memory.clearWords(cell, size.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt());
        return cell;
    }

    /**
     * Allocate a space of the specified size.
     *
     * @param size size requested in bytes.
     * @return pointer to uncleared allocated cell
     */
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    public final Pointer allocate(Size size) {
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
