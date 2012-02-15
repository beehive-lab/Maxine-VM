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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.*;

/**
 * A listener for events related to dead heap space.
 * Two events are reported: coalescing, and splitting, both of which produces free chunk of memory formatted as parsable heap cell (i.e., cells that a heap space scanner can
 * identify and walk over). Example of format of free chunk of memory include byte arrays, instances of {@link Object}, or {@link HeapFreeChunk}.
 * Dead space coalescing events are notified during space reclamation, when garbage has been identified and is coalesced into a single chunk of memory that can be walked over
 * by heap space scanner. They are also notified when coalescing together some large unit of dead space into a single unit to either uncommit heap memory or to serve a large
 * object allocation.
 * Dead space splitting are notified during allocation of a prefix of a free chunks, splitting the chunks into two parsable heap cells.
 *
 * Dead space listener may be use, for example, to update the FOT of a card table to maintain the ability to walk over a card at any time.
 *
 */
public abstract class DeadSpaceListener {
    public static DeadSpaceListener nullDeadSpaceRSetUpdater() {
        return new DeadSpaceListener() {
            @INLINE
            @Override
            public final void notifyCoaslescing(Address deadSpace, Size numDeadBytes) {
            }

            @INLINE
            @Override
            public final void notifySplit(Address leftDeadSpace, Address rightDeadSpace, Size leftSize) {
            }
        };
    }

    protected DeadSpaceListener() {
    }

    /**
     * Notify the creation or coalescing of dead space into a single parsable chunk of memory.
     * @param deadSpace start of the dead area
     * @param numDeadBytes size of the dead area
     */
    public abstract void notifyCoaslescing(Address deadSpace, Size numDeadBytes);

    /**
     * Notify the split of a single parsable chunk of memory in two parsable chunks of memory.
     * @param start start of the chunk of memory being split.
     * @param end end of the chunk of memory being split
     * @param leftSize size of the left cell being split off the chunk of memory.
     */
    public abstract void notifySplit(Address start, Address end, Size leftSize);
}
