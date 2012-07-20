/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap;

/**
 * The object management phases of a {@link HeapScheme} implementation.
 * The heap cycles in order through he following three phases:
 * <ol>
 * <li> {@link #MUTATING}: normal operation, heap mutation & new allocation.</li>
 * <li> {@link #ANALYZING}: first ("liveness analysis") phase of GC.</li>
 * <li> {@link #RECLAIMING}: final ("clean up") phase of GC.</li>
 * </ol>
 */
public enum HeapPhase {

    /**
     * This is the non-GC phase, during which the only activity
     * is to allocate object memory from its free
     * list, which is initially live.
     * During this phase any live
     * object in one of the heap's regions remains live,
     * unmoved, and of constant type.
     */
    MUTATING("Mutating", "Mutating & new allocation, no GC"),

    /**
     * The first phase of a GC, during which the heap investigates
     * the liveness of every object in its region without loss of historical
     * information. During this phase no new objects are allocated.  The status
     * of all objects becomes unknown at the beginning
     * of this phase and they may or may not become once again live
     * during the phase. Objects whose status remains unknown
     * at the end of the phase are deemed unreachable and become permanently
     * dead.
     */
    ANALYZING("Analyzing", "First (liveness analysis) phase of GC"),

    /**
     * The second phase of a GC, during which the heap finalizes the status of
     * objects and reclaims unreachable memory that had been allocated to objects
     * now determined to be dead, as well as any other
     * information produced during GC that is no longer needed.
     */
    RECLAIMING("Reclaiming", "Second (clean up, reclaim memory) phase of GC");

    private final String label;
    private final String description;

    private HeapPhase(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    /**
     * Returns {@code true} if the heap is in one of the GC phases.
     * {@code false} if mutating, new object allocation
     */
    public boolean isCollecting() {
        return this != MUTATING;
    }

    /**
     * @return whether the heap is in the normal, non-GC phase
     * during which the heap is mutating and new objects are being allocated.
     * @see #MUTATING
     */
    public boolean isMutating() {
        return this == MUTATING;
    }

    /**
     * @return whether the heap is in the first phase of GC,
     * during which the reachability of objects is being determined.
     * @see #ANALYZING
     */
    public boolean isAnalyzing() {
        return this == ANALYZING;
    }

    /**
     * @return whether the heap is in the second phase of GC,
     * during which information no longer necessary is being cleaned up.
     * @see #RECLAIMING
     */
    public boolean isReclaiming() {
        return this == RECLAIMING;
    }
}
