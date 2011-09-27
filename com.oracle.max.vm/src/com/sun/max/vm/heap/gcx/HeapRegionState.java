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

import static com.sun.max.vm.heap.gcx.HeapRegionInfo.Flag.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

public enum HeapRegionState {
    EMPTY_REGION(0),
    ALLOCATING_REGION(IS_ALLOCATING.or(0)),
    FULL_REGION(IS_ITERABLE.or(0)),
    FREE_CHUNKS_REGION(IS_ITERABLE.or(HAS_FREE_CHUNK.or(0))),
    LARGE_HEAD(IS_ITERABLE.or(IS_LARGE.or(IS_HEAD.or(0)))),
    LARGE_BODY(IS_ITERABLE.or(IS_LARGE.or(0))),
    LARGE_FULL_TAIL(IS_ITERABLE.or(IS_LARGE.or(IS_TAIL.or(0)))),
    LARGE_TAIL(IS_ITERABLE.or(IS_LARGE.or(IS_TAIL.or(HAS_FREE_CHUNK.or(0))))),
    LARGE_ALLOCATING_TAIL(IS_ALLOCATING.or(IS_LARGE.or(IS_TAIL.or(0))));

    private static final boolean [][] validStateTransitions = new boolean[HeapRegionState.values().length][HeapRegionState.values().length];
    private static final IntHashMap<HeapRegionState> allValidStates = new IntHashMap<HeapRegionState>(values().length);

    /**
     * Initialize valid heap region state transitions.
     * @param fromState the state the heap region transitions from.
     * @param toStates the valid states the heap region may transition to.
     */
    @HOSTED_ONLY
    static void initialize(HeapRegionState fromState, HeapRegionState [] toStates) {
        boolean [] stateTransitions = validStateTransitions[fromState.ordinal()];
        for (HeapRegionState toState : toStates) {
            stateTransitions[toState.ordinal()] = true;
        }
    }

    static {
        for (HeapRegionState state : values()) {
            allValidStates.put(state.flags, state);
        }
        // Initialize the valid state transitions.
        // FIXME: may want to introduce a special  "SWEPT" state  to distinguish between valid empty->state transitions that correspond to sweeping transitions from the empty -> allocating
        // transitions.
        initialize(EMPTY_REGION, new HeapRegionState [] {EMPTY_REGION, ALLOCATING_REGION, FULL_REGION, FREE_CHUNKS_REGION, LARGE_HEAD, LARGE_BODY, LARGE_FULL_TAIL, LARGE_TAIL});
        initialize(ALLOCATING_REGION, new HeapRegionState [] {FULL_REGION, FREE_CHUNKS_REGION});
        initialize(FULL_REGION, new HeapRegionState [] {EMPTY_REGION, FREE_CHUNKS_REGION});
        initialize(FREE_CHUNKS_REGION, new HeapRegionState [] {EMPTY_REGION, ALLOCATING_REGION, FREE_CHUNKS_REGION});
        initialize(LARGE_HEAD, new HeapRegionState [] {EMPTY_REGION, FREE_CHUNKS_REGION});
        initialize(LARGE_BODY, new HeapRegionState [] {EMPTY_REGION, FREE_CHUNKS_REGION});
        initialize(LARGE_FULL_TAIL, new HeapRegionState [] {EMPTY_REGION, FREE_CHUNKS_REGION, LARGE_TAIL});
        initialize(LARGE_TAIL, new HeapRegionState [] {EMPTY_REGION, LARGE_ALLOCATING_TAIL});
        initialize(LARGE_ALLOCATING_TAIL, new HeapRegionState [] {LARGE_FULL_TAIL, LARGE_TAIL});
    }

    static public boolean isValidTransition(HeapRegionState from, HeapRegionState to) {
        return validStateTransitions[from.ordinal()][to.ordinal()];
    }

    /**
     * Return the HeapRegionState for the specified set of HeapRegionInfo flags, or null if the specified flags don't represent a valid state.
     * @param flags
     * @return a HeapRegionState, or null
     */
    static HeapRegionState toHeapRegionState(int flags) {
        return allValidStates.get(flags);
    }

    final int flags;
    HeapRegionState(int flags) {
        this.flags = flags;
    }

    public final boolean isInState(HeapRegionInfo rinfo) {
        return rinfo.flags == flags;
    }

    private static void checkStateTransition(HeapRegionInfo rinfo, HeapRegionState to) {
        HeapRegionState from = toHeapRegionState(rinfo.flags);
        FatalError.check(from != null && to != null, "invalid heap state(s)");
        if (!validStateTransitions[from.ordinal()][to.ordinal()]) {
            Log.print("Region #");
            Log.print(RegionTable.theRegionTable().regionID(rinfo));
            Log.print(" ");
            Log.print(from.toString()); Log.print(" -> "); Log.print(to.toString());
            FatalError.unexpected(": not a valid HeapRegionInfo state transition");
        }
    }

    public final void setState(HeapRegionInfo rinfo) {
        if (MaxineVM.isDebug()) {
            checkStateTransition(rinfo, this);
        }
        rinfo.flags = flags;
    }

    public final static void toAllocatingState(HeapRegionInfo rinfo) {
        int flags = IS_ALLOCATING.or(HAS_FREE_CHUNK.clear(IS_ITERABLE.clear(rinfo.flags)));
        if (MaxineVM.isDebug()) {
            checkStateTransition(rinfo, toHeapRegionState(flags));
        }
        rinfo.flags = flags;
    }

    public final static void toFullState(HeapRegionInfo rinfo) {
        int flags = IS_ITERABLE.or(IS_ALLOCATING.clear(rinfo.flags));
        if (MaxineVM.isDebug()) {
            checkStateTransition(rinfo, toHeapRegionState(flags));
        }
        rinfo.flags = flags;
    }
}
