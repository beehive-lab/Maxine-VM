/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.heap.gcx;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * A space allocator that can allocate TLAB formatted as a linked list of discontinuous chunks.
 * TLAB do not have to be allocated as a single contiguous chunk, and the space
 * requested should be treated as a hint, i.e., the returned TLAB may be slightly bigger
 * or smaller if it opportunistically reduce fragmentation of the available space.
 *
 * @author Laurent Daynes
 */
public class MultiChunkTLABAllocator extends LinearSpaceAllocator {
    abstract static class RefillManager extends LinearSpaceAllocator.RefillManager {
        /**
         * Minimum amount of space a TLAB chunk should be allocated.
         */        private Size tlabMinChunkSize;
        abstract Address allocateTLAB(Size tlabSize);
    }

    /**
     * Minimum amount of space a TLAB chunk should be allocated.
     */
    private Size tlabMinChunkSize;

    MultiChunkTLABAllocator(RefillManager refillManager) {
        super(refillManager);
    }

    void initialize(Address initialChunk, Size initialChunkSize, Size sizeLimit, Size headroom, Size tlabMinChunkSize) {
        super.initialize(initialChunk, initialChunkSize, sizeLimit, headroom);
        this.tlabMinChunkSize = tlabMinChunkSize;
    }

    synchronized Pointer allocateTLABAndRefill(Size tlabSize) {
        // We may have race with other threads to get here.
        // First, check that the condition that led us here still holds.
        Size spaceNeeded = tlabSize;
        Pointer cell = top.asPointer();
        if (cell.plus(tlabSize).greaterThan(end)) {
            Pointer hardLimit = hardLimit().asPointer();
            Pointer firstChunk = setTopToLimit();
            Size size = hardLimit.minus(firstChunk).asSize();
            if (size.lessThan(tlabMinChunkSize)) {
                // Don't bother with this chunk.
                if (size.greaterThan(0)) {
                    HeapSchemeAdaptor.fillWithDeadObject(firstChunk, hardLimit);
                }
                // Fall off to allocation by the refill manager.
            } else {
                spaceNeeded = tlabSize.minus(size);
                HeapFreeChunk.setFreeChunkSize(firstChunk, size);
                HeapFreeChunk.setFreeChunkNext(firstChunk, Address.zero());
                // Don't try allocate an additional chunk to fill up to the requested size.
                // This might raises a GC that would invalidate the first chunk just constructed.
                return firstChunk;
            }
            return ((RefillManager) refillManager).allocateTLAB(spaceNeeded).asPointer();
        }
        // We've lost the race to refill the allocator. Return zero to indicate
        // the allocator to retry.
        return Pointer.zero();
    }

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
        Address firstChunk = Address.zero();
        do {
            chunkSize = tlabSize;
            cell = top.asPointer();
            newTop = cell.plus(chunkSize);
            if (newTop.greaterThan(end)) {
                return allocateTLABAndRefill(tlabSize);
            }
        } while (thisAddress.compareAndSwapWord(TOP_OFFSET, cell, newTop) != cell);

        // Format as a chunk.
        HeapFreeChunk.setFreeChunkSize(cell, chunkSize);
        HeapFreeChunk.setFreeChunkNext(cell, firstChunk);
        return cell;
    }
}
