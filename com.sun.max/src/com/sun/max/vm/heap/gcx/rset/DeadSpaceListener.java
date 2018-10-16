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
package com.sun.max.vm.heap.gcx.rset;

import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.*;

/**
 * A listener for events related to dead heap space, i.e., heap space not occupied by live objects.
 * Dead heap space may be coalesced to form free space that can be used to refill allocators.
 * Coalescing may happen at initialization of new heap space, or when reclaiming contiguous range of garbage.
 * Allocators retire the left over from their refill when they cannot use it anymore.
 * The retired space may be dead (i.e., cannot be re-used for further allocations), or free (may be re-usable for further allocation), in which case it may be refill again.
 *
 * A dead space listener takes care of any special formatting required for enabling heap space scanner to walk over dead heap space.
 * Examples of format of dead heap space include byte arrays, instances of {@link Object}, or {@link HeapFreeChunk}.
 * Reformatting is needed when an event changes the boundary of dead heap space: coalescing and splitting.
 *
 *  Dead space coalescing events are notified during space reclamation, when garbage has been identified and is coalesced into a single chunk of memory
 *  that can be walked over by heap space scanner. They are also notified when coalescing together some large unit of dead space into a single unit to
 *  either uncommit heap memory or to serve a large object allocation.
 *
 * Allocators refill and retire area also notified as they may be used to perform formatting that limit dead space listening overhead.
 * Main used of dead space listener at the moment is to update the FOT of a card table to maintain the ability to walk over a card at any time.
 */
public abstract class DeadSpaceListener {

    protected DeadSpaceListener() {
    }

    /**
     * Notify the creation or coalescing of dead space into a single parsable chunk of memory.
     * @param deadSpace start of the dead area
     * @param numDeadBytes size of the dead area
     */
    public abstract void notifyCoalescing(Address deadSpace, Size numDeadBytes);

    /**
     * Notify the split of a single parsable chunk of memory.
     * The left-hand side of the split is allocated to a live cell.
     *
     * @param start start of the chunk of memory being split.
     * @param leftSize size of the left cell being split off the chunk of memory.
     * @param end end of the chunk of memory being split
     */
    public abstract void notifySplitLive(Address start, Size leftSize, Address end);


    /**
     * Notify the split of a single parsable chunk of memory.
     * The left-hand side of the split is a new dead space.
     *
     * @param start start of the chunk of memory being split.
     * @param leftSize size of the left cell being split off the chunk of memory.
     * @param end end of the chunk of memory being split
     */
    public abstract void notifySplitDead(Address start, Size leftSize, Address end);

    /**
     * Notify the refill of an allocator with a dead space.
     *
     * @param deadSpace address of the first word of the dead space
     * @param numDeadBytes size of the dead space
     */
    public abstract void notifyRefill(Address deadSpace, Size numDeadBytes);

    /**
     * Notify the retiring of dead space unusable for further allocation by an allocator.
     * @param deadSpace address of the first word of the dead space
     * @param numDeadBytes size of the dead space
     */
    public abstract void notifyRetireDeadSpace(Address deadSpace, Size numDeadBytes);

    /**
     * Notify the retiring of dead space usable for further allocation by an allocator.
     * @param deadSpace address of the first word of the dead space
     * @param numDeadBytes size of the dead space
     */
    public abstract void notifyRetireFreeSpace(Address deadSpace, Size numDeadBytes);
}
