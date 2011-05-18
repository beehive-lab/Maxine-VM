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

import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * Large object tags are objects stored at the end of large chunk of heap to ease operations
 * on large object spaces, such as in-place promotion and coalescing of free space.
 *
 * Tags are doubly linked list elements specifying the size of the chunk they tag, the list
 * they belong to, plus a space for custom flags.
 * Tags are "ghost" objects only visible to the GC.
 *
 * Given the address of a large object, one can obtain simply the address of its preceding
 * chunk by reading the tags stored immediately before. Similarly, the next chunk can be found
 * by rounding the size of the large object (plus the size of the mandatory tag) to the block size
 * used by the large object space.
 *
 * @see TaggedLargeObjectSpace
 */
class LargeObjectTag extends DLinkedHeapFreeChunk {
    private static final DynamicHub HUB = ClassActor.fromJava(LargeObjectTag.class).dynamicHub();

    @INTRINSIC(UNSAFE_CAST)
    private static native LargeObjectTag asLargeObjectTag(Object object);

    @INLINE
    public static LargeObjectTag toLargeObjectTag(Address address) {
        return asLargeObjectTag(Reference.fromOrigin(Layout.cellToOrigin(address.asPointer())).toJava());
    }

    /**
     * A live mark that may be used by garbage collector.
     */
    private boolean mark;
    /**
     * Identifier of a live large object list this large object tags belong to.
     * Negative value if the tagged chunk is free
     */
    private int largeObjectListID;

    /**
     * Return a boolean value indicating whether the tag suffixes a free chunk.
     * @return true if tags a free chunk, false otherwise.
     */
    boolean tagsFreeChunk() {
        return largeObjectListID < 0;
    }

    /**
     * Returns the identifier of the list the tagged chunk is assigned to, or -1 if it isn't
     * assigned (i.e., it is a free chunk).
     *
     * @return a large object list identifier, or -1.
     */
    int getLargeObjectListID() {
        return largeObjectListID;
    }

    public boolean setMark() {
        boolean oldMark = mark;
        mark = true;
        return oldMark;
    }

    public boolean isMarked() {
        return mark;
    }

    /**
     * Size in bytes of instances of {@link LargeObjectTag}.
     *
     * @return size in bytes
     */
    static Size tagSize() {
        return HUB.tupleSize;
    }


    /**
     * Compute the size a chunks must have to store a large object of the specified size.
     * The chunks must host both the large object and its tag, and must be aligned to the
     * block size of the large object space.
     *
     * @param largeObjectSize the size of the large object.
     * @return the size a chunk storing the specified large object must have
     */
    private static Size getLargeObjectChunkSize(Size largeObjectSize) {
        return largeObjectSize.plus(tagSize()).roundedUpBy(TaggedLargeObjectSpace.BLOCK_SIZE);
    }

    /**
     * Return the address of the large object's tag.
     * @param largeObjectPointer pointer to the large object.
     * @param largeObjectSize size of the large object
     * @return
     */
    private static Address tagAddress(Address largeObjectPointer, Size largeObjectSize) {
        Address tag = largeObjectPointer.plus(getLargeObjectChunkSize(largeObjectSize)).minus(tagSize());
        if (MaxineVM.isDebug()) {
            FatalError.check(tag.greaterEqual(largeObjectPointer.plus(largeObjectSize)), "not enough space for large object tag");
        }
        return tag;
    }

    /**
     * Format the end of an allocated chunk into a LargeObjectTag.
     *
     * @param allocated pointer to the allocated space in a tagged large object space
     * @param largeObjectSize the size of the allocated object
     * @param listID the large object list the object is assigned to
     */
    static void setTag(Pointer largeObjectPointer, Size largeObjectSize, int listID) {
        Size chunkSize = getLargeObjectChunkSize(largeObjectSize);
        Address cell = largeObjectPointer.plus(getLargeObjectChunkSize(largeObjectSize)).minus(tagSize());
        LargeObjectTag tag = asLargeObjectTag(DLinkedHeapFreeChunk.format(HUB, cell, chunkSize, Address.zero(), Address.zero()));
        tag.mark = false;
        tag.largeObjectListID = listID;
    }


    static void setFreeChunkTag(Pointer freedChunk, Size chunkSize) {
    }

    /**
     * Return a pointer to the chunk just before the dead large object if it is free.
     * This assumes the presence of a LargeObjectTag at the end of every chunk of memory,
     * whether allocated or free.
     *
     * @param deadLargeObjectAddress  a pointer to a dead large object
     * @return a pointer to the chunk just before the large object if it is free, zero otherwise
     */
    static Pointer freeChunkBefore(Pointer deadLargeObjectAddress) {
        // Every object in a tagged large object space is immediately
        // preceded by a LargeObjectTag.
        Address tagAddress = deadLargeObjectAddress.minus(tagSize());
        LargeObjectTag tag = toLargeObjectTag(tagAddress);
        if (tag.tagsFreeChunk()) {
            return deadLargeObjectAddress.minus(tag.size);
        }
        return Pointer.zero();
    }


    /**
     * Returns a pointer to the chunk just after the dead large object if it is free.
     * This assumes that every free chunk have their first bytes formatted as a DLinkedHeapFreeChunk.
     * @param deadLargeObjectAddress a pointer to a dead large object
     * @return a pointer to the chunk just before the large object if it is free, zero otherwise
     */
    static Pointer freeChunkAfter(Pointer deadLargeObjectAddress) {
        Size size = Layout.size(Layout.cellToOrigin(deadLargeObjectAddress));
        // Compute address of the following check and check if its first object is a
        // DLinkedHeapFreeChunk. If it is, then we have a free object here.
        Pointer nextchunk = deadLargeObjectAddress.plus(getLargeObjectChunkSize(size));
        return DLinkedHeapFreeChunk.isDLinkedHeapFreeChunk(nextchunk) ? nextchunk : Pointer.zero();
    }
}
