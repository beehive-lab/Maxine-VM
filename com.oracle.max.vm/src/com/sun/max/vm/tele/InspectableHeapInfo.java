/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.tele;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

/**
 * Makes critical state information about the object heap
 * remotely inspectable.
 * <p>
 * Active only when VM is being inspected.
 * <p>
 * Dynamic object allocation is to be avoided.
 * <p>
 * The methods in this with names inspectable* are intended to act as a kind of hook for the Inspector, so that it
 * can interrupt the VM at certain interesting moments.  This could also be used as a kind of low-wattage event
 * mechanism.
 * <p>
 * The inspectable* methods here are distinct from those with similar or identical names in {@link HeapScheme.Inspect},
 * which are intended to act as convenient places for a user to set a breakpoint, perhaps from a menu of standard
 * locations.  The intention is that those locations would not land the user in this class.
 *
 * @see HeapScheme.Inspect
 */
public final class InspectableHeapInfo {

    private InspectableHeapInfo() {
    }

    /**
     * Should inspectable information about heap regions be allocated in immortal memory.
     */
    private static boolean useImmortalMemory = false;

    /**
     * Inspectable array of memory regions allocated dynamically for heap memory management.
     * @see com.sun.max.vm.heap.HeapScheme
     */
    @INSPECTED
    private static MemoryRegion[] dynamicHeapMemoryRegions;

    /**
     * The ordinal value of the enum describing the current heap phase.
     * This permits inspection of the phase at any time and, if needed,
     * detection of phase change by watchpoint.
     */
    @INSPECTED
    private static int heapPhaseOrdinal = HeapPhase.MUTATING.ordinal();

    /**
     * Inspectable counter of the number of Garbage Collections that have <strong>begun</strong>.
     * <p>
     * Used by the Inspector to determine if the VM is currently collecting.
     */
    @INSPECTED
    private static long gcStartedCounter;

    /**
     * Inspectable counter of the number of Garbage Collections that have <strong>completed</strong>.
     * <p>
     * Used by the Inspector to determine if the VM is currently collecting, and to
     * determine if the Inspector's cache of the root locations is current.
     */
    @INSPECTED
    private static long gcCompletedCounter;

    /**
     * Heap size most recently requested.
     */
    @INSPECTED
    private static long recentHeapSizeRequest;

    /**
     * Sets up root table and other information needed for heap inspection.
     * <p>
     * No-op when VM is not being inspected.
     * @param useImmortalMemory should allocations should be made in immortal memory.
     */
    public static void init(boolean useImmortalMemory) {
        if (Inspectable.isVmInspected()) {
            InspectableHeapInfo.useImmortalMemory = useImmortalMemory;
        }
    }

    /**
     * <strong>This is scheduled for retirement.</strong>
     * <p> The new heap scheme support
     * classes in the Inspector locate the memory regions for each heap scheme
     * implementation directly from the implementation class.  This notification
     * call is only in place to support legacy support for "attach" mode in the
     * SemiSpace support, which will also cease to depend on this notification.
     * (mlvdv 4/22/12).
     * <p>
     * Stores descriptions of memory allocated by the heap in a location that can
     * be inspected easily.
     * <p>
     * It is a good idea to use instances of {@link MemoryRegion} that have
     * been allocated in the boot heap if at all possible, thus avoiding having
     * meta information about the dynamic heap being described by objects
     * in the dynamic heap.
     * <p>
     * No-op when VM is not being inspected.
     * @param useImmortalMemory should allocations should be made in immortal memory.
     * @param memoryRegions regions allocated by the heap implementation
     */
    public static void setMemoryRegions(MemoryRegion[] memoryRegions) {
        if (Inspectable.isVmInspected()) {
            if (useImmortalMemory) {
                try {
                    Heap.enableImmortalMemoryAllocation();
                    dynamicHeapMemoryRegions = Arrays.copyOf(memoryRegions, memoryRegions.length);
                } finally {
                    Heap.disableImmortalMemoryAllocation();
                }
            } else {
                dynamicHeapMemoryRegions = memoryRegions;
            }
        }
    }

    @INSPECTED
    @NEVER_INLINE
    private static void inspectableObjectRelocated(Address oldCellLocation,  Address newCellLocation) {
    }

    public static void notifyPhaseChange(HeapPhase phase) {
        heapPhaseOrdinal = phase.ordinal();
        switch (phase) {
            case ANALYZING:
                gcStartedCounter++;
                // From the Inspector's perspective, a GC begins when
                // the epoch counter gets incremented.
                inspectableGCAnalyzing(gcStartedCounter);
                break;
            case RECLAIMING:
                inspectableGCReclaiming(gcStartedCounter);
                break;
            case MUTATING:
                gcCompletedCounter++;
                // From the Inspector's perspective, a GC is complete when
                // the two epoch counters become equal.
                inspectableGCMutating(gcCompletedCounter);
                break;
        }
    }

    /**
     * An empty method whose purpose is to be interrupted by the Inspector
     * when it needs to observe the VM at the beginning of a GC, i.e. when
     * it enters the {@link HeapPhase#ANALYZING}.
     * <p>
     * This particular method is intended for internal use by the inspector.
     * Should a user wish to break at the beginning of GC, another, more
     * convenient inspectable method is provided
     * <p>
     * <strong>Important:</strong> The Inspector assumes that this method is loaded
     * and compiled in the boot image and that it will never be dynamically recompiled.
     *
     * @param gcStartedCounter the GC epoch that is starting.
     * @see HeapScheme.Inspect#inspectableGCStarting()
     */
    @INSPECTED
    @NEVER_INLINE
    private static void inspectableGCAnalyzing(long gcStartedCounter) {
    }

    /**
     * An empty method whose purpose is to be interrupted by the Inspector
     * when it needs to observe the VM when GC is ready to start reclaiming,
     * i.e. when it enters the {@link HeapPhase#RECLAIMING}.
     * <p>
     * This particular method is intended for internal use by the inspector.
     * Should a user wish to break at this phase, another, more
     * convenient inspectable method is provided
     * <p>
     * <strong>Important:</strong> The Inspector assumes that this method is loaded
     * and compiled in the boot image and that it will never be dynamically recompiled.
     *
     * @param gcStartedCounter the GC epoch that is starting.
     * @see HeapScheme.Inspect#inspectableGCReclaiming()
     */
    @INSPECTED
    @NEVER_INLINE
    private static void inspectableGCReclaiming(long gcStartedCounter) {
    }

    /**
     * An empty method whose purpose is to be interrupted by the Inspector
     * when it needs to observe the VM at the conclusion of a GC, i.e. hwen
     * it enters the {@link HeapPhase#MUTATING}.
     * <p>
     * This particular method is intended for internal use by the inspector.
     * Should a user wish to break at the conclusion of GC, another, more
     * convenient inspectable method is provided
     * <p>
     * <strong>Important:</strong> The Inspector assumes that this method is loaded
     * and compiled in the boot image and that it will never be dynamically recompiled.
     *
     * @param gcStartedCounter the GC epoch that is ending.
     * @see HeapScheme.Inspect#inspectableGCComplete()
     */
    @INSPECTED
    @NEVER_INLINE
    private static void inspectableGCMutating(long gcCompletedCounter) {
    }

    /**
     * Records that an increase of heap size has been requested.
     *
     * @param size the desired new heap size.
     */
    public static void notifyIncreaseMemoryRequested(Size size) {
        recentHeapSizeRequest = size.toLong();
        inspectableIncreaseMemoryRequested(size);
    }

    /**
     * An empty method whose purpose is to be interrupted by the Inspector
     * when it needs to observe a request for heap memory size increase.
     * <p>
     * This particular method is intended for internal use by the inspector.
     * Should a user wish to break at the request,  another, more
     * convenient inspectable method is provided
     * <p>
     * <strong>Important:</strong> The Inspector assumes that this method is loaded
     * and compiled in the boot image and that it will never be dynamically recompiled.
     *
     * @param
     * @see HeapScheme.Inspect#inspectableGCComplete()
     */
    @INSPECTED
    @NEVER_INLINE
    private static void inspectableIncreaseMemoryRequested(Size size) {
    }

    /**
     * Records that an decrease of heap size has been requested.
     *
     * @param size the desired new heap size.
     */
    public static void notifyDecreaseMemoryRequested(Size size) {
        recentHeapSizeRequest = size.toLong();
        inspectableDecreaseMemoryRequested(size);
    }

    /**
     * An empty method whose purpose is to be interrupted by the Inspector
     * when it needs to observe a request for heap memory size decrease.
     * <p>
     * This particular method is intended for internal use by the inspector.
     * Should a user wish to break at the request,  another, more
     * convenient inspectable method is provided
     * <p>
     * <strong>Important:</strong> The Inspector assumes that this method is loaded
     * and compiled in the boot image and that it will never be dynamically recompiled.
     *
     * @param size the desired new heap size
     * @see HeapScheme.Inspect#inspectableGCComplete()
     */
    @INSPECTED
    @NEVER_INLINE
    private static void inspectableDecreaseMemoryRequested(Size size) {
    }


}
