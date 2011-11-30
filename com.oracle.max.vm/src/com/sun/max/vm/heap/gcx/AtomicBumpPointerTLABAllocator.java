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

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;


public abstract class AtomicBumpPointerTLABAllocator<T extends Refiller> extends BaseAtomicBumpPointerAllocator<T> {
    private Size minTLABSize;
    public AtomicBumpPointerTLABAllocator(T refiller, Size minTLSize) {
        super(refiller);
    }

    protected Pointer refillOrAllocateTLAB(Size size) {
        synchronized (refillLock()) {
            // We're the only thread that can refill the allocator now.
            // We're still racing with other threads that might try to allocate
            // what's left in the allocator (and succeed!).
            // We may also have raced with another concurrent thread which may have
            // refilled the allocator.
            // The following take care of both.

            Pointer cell = top.asPointer();
            if (cell.plus(size).greaterThan(end)) {
                // Atomically grab what's left in the allocator.
                // If there enough space for the TLAB, we'll return it as a HeapFreeChunk, otherwise, will fill it as a dead object
                // and try to refill the allocator.
                Pointer startOfSpaceLeft = atomicSetTopToLimit();
                Size numBytesLeft =  hardLimit().minus(startOfSpaceLeft).asSize();
                if (numBytesLeft.greaterThan(minTLABSize)) {
                    HeapFreeChunk.format(startOfSpaceLeft, numBytesLeft);
                    return startOfSpaceLeft;
                }
                Address chunk = refillManager.allocateRefill(startOfSpaceLeft, numBytesLeft);
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
     * Allocate a TLAB of the specified size. The size may be more or less the requested size, to avoid fragmenting the allocator.
     *
     * @param size size requested
     * @return a pointer to a {@link HeapFreeChunk}
     */
    @NO_SAFEPOINT_POLLS("allocation must be atomic")
    public Pointer allocateTLAB(Size size) {
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
                cell = refillOrAllocateTLAB(size);
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
}
