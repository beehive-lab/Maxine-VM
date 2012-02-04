/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx.rset.ctbl;

import static com.sun.max.vm.heap.HeapSchemeAdaptor.*;
import static com.sun.max.vm.heap.gcx.rset.ctbl.CardTableRSet.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.heap.gcx.rset.*;

public class DeadSpaceCardTableUpdater extends DeadSpaceRSetUpdater  {
    final CardTableRSet rset;

    public DeadSpaceCardTableUpdater(CardTableRSet rset) {
        this.rset = rset;
    }

    @FOLD
    private Size heapFreeChunkHeaderSize() {
        return HeapFreeChunk.HEAP_FREE_CHUNK_HUB.tupleSize;
    }

    @Override
    public void updateRSet(Address deadSpace, Size numDeadBytes) {
        // Allocation may occur while iterating over dirty cards to evacuate young objects. Such allocations may temporarily invalidate
        // the FOT for cards overlapping with the allocator, and may break the ability to walk over these cards.
        // One solution is keep the FOT up to date at every allocation, which may be expensive.
        // Another solution is to make the last card of free chunk used by allocator independent of most allocation activity.
        // The last card is the only one that may be dirtied, and therefore the only one that can be walked over during evacuation.
        //
        // Let s and e be the start and end of a free chunk.
        // Let c be the top-most card address such that c >= e, and C be the card starting at c, with FOT(C) denoting the entry
        // of the FOT for card C.
        // If e == c,  whatever objects are allocated between s and e, FOT(C) == 0, i.e., allocations never invalidate FOT(C).
        // Thus C is iterable at all time by a dirty card walker.
        //
        // If e > c, then FOT(C) might be invalidated by allocation.
        // We may avoid this by formatting the space delimited by [c,e] as a dead object embedded in the heap free chunk.
        // This will allow FOT(C) to be zero, and not be invalidated by any allocation of space before C.
        // Formatting [c,e] has however several corner cases:
        // 1. [c,e] may be smaller than the minimum object size, so we can't format it.
        // Instead, we can format [x,e], such x < e and [x,e] is the min object size. In this case, FOT(C) = x - e.
        // [x,e] can only be overwritten by the last allocation from the buffer,
        // so FOT(C) doesn't need to be updated until that last allocation,
        // which is always special cased (see why below).
        // 2. s + sizeof(free chunk header) > c, i.e., the head of the free space chunk overlap C
        // Don't reformat the heap chunk. FOT(C) will be updated correctly since the allocator
        // always set the FOT table for the newly allocated cells. Since this one always
        if (numDeadBytes.greaterEqual(CARD_SIZE)) {
            final Pointer end = deadSpace.plus(numDeadBytes).asPointer();
            final Address lastCardStart = alignDownToCard(end);
            if (lastCardStart.minus(deadSpace).greaterThan(heapFreeChunkHeaderSize())) {
                // Format the end of the heap free chunk as a dead object.
                Pointer deadObjectAddress = lastCardStart.asPointer();
                Size deadObjectSize = end.minus(deadObjectAddress).asSize();

                if (deadObjectSize.lessThan(MIN_OBJECT_SIZE)) {
                    deadObjectSize = MIN_OBJECT_SIZE;
                    deadObjectAddress = end.minus(deadObjectSize);
                }
                if (MaxineVM.isDebug() && true) {
                    Log.print("Update Card Table for reclaimed range [");
                    Log.print(deadSpace); Log.print(", ");
                    Log.print(end);
                    Log.print("] / tail dead object at ");
                    Log.print(deadObjectAddress);
                    Log.print(" card # ");
                    Log.println(rset.cardTable.tableEntryIndex(deadObjectAddress));
                }
                HeapSchemeAdaptor.fillWithDeadObject(deadObjectAddress, end);
                rset.updateForFreeSpace(deadSpace, deadObjectAddress.minus(deadSpace).asSize());
                rset.updateForFreeSpace(deadObjectAddress, deadObjectSize);
                return;
            }
        }
        // Otherwise, the free chunk is either smaller than a card, or it is smaller than two cards and its header spawn the two cards.
        rset.updateForFreeSpace(deadSpace, numDeadBytes);
    }
}
