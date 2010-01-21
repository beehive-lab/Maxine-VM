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
package com.sun.max.vm.tele;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

/**
 * Makes critical state information about the object heap
 * remotely inspectable.
 * <br>
 * Active only when VM is being inspected.
 * <br>
 * Dynamic object allocation is to be avoided.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 * @author Hannes Payer
 */
public final class InspectableHeapInfo {

    private InspectableHeapInfo() {
    }

    /**
     * Inspectable array of memory regions allocated for heap memory management.
     * @see com.sun.max.vm.heap.HeapScheme
     */
    @INSPECTED
    private static MemoryRegion[] memoryRegions;

    /**
     * Maximum number of roots that the Inspector can register for tracking relocations.
     */
    public static final int MAX_NUMBER_OF_ROOTS = Ints.M / 8;

    /**
     * Inspectable description the memory allocated for the Inspector's root table.
     */
    @INSPECTED
    private static RuntimeMemoryRegion rootsRegion;

    /**
     * Inspectable location of the memory allocated for the Inspector's root table.
     * Equivalent to {@link RuntimeMemoryRegion#start()}, but it must be
     * readable by the Inspector using only low level operations during startup.
     */
    @INSPECTED
    private static Pointer rootsPointer = Pointer.zero();

    /**
     * Inspectable counter of the number of Garbage Collections that have <strong>begun</strong>.
     * <br>
     * Used by the Inspector to determine if the VM is currently collecting.
     */
    @INSPECTED
    private static long collectionEpoch;

    /**
     * Inspectable counter of the number of Garbage Collections that have <strong>completed</strong>.
     * <br>
     * Used by the Inspector to determine if the VM is currently collecting, and to
     * determine if the Inspector's cache of the root locations is current.
     */
    @INSPECTED
    private static long rootEpoch;

    /**
     * Old memory cell location of the object most recently relocated.
     */
    @INSPECTED
    private static Address recentRelocationOldCell;

    /**
     * New memory cell location of the object most recently relocated.
     */
    @INSPECTED
    private static Address recentRelocationNewCell;

    /**
     * Stores descriptions of memory allocated by the heap in a location that can
     * be inspected easily.
     * <br>
     * It is a good idea to use instances of {@link MemoryRegion} that have
     * been allocated in the boot heap if at all possible, thus avoiding having
     * meta information about the dynamic heap being described by objects
     * in the dynamic heap.
     * <br>
     * No-op when VM is not being inspected.
     *
     * @param memoryRegions regions allocated by the heap implementation
     */
    public static void init(MemoryRegion... memoryRegions) {
        if (Inspectable.isVmInspected()) {
            InspectableHeapInfo.memoryRegions = memoryRegions;

            try {
                Heap.enableImmortalMemoryAllocation();
                rootsRegion = new RuntimeMemoryRegion("TeleRoots");
            } finally {
                Heap.disableImmortalMemoryAllocation();
            }

            initRootsRegion();
        }
    }

    /**
     * Allocates a special area of memory for references held by the Inspector.
     * The Inspector writes values into the array, and each GC implementation is obliged to
     * relocate them at the conclusion of each collection.
     */
    private static void initRootsRegion() {
        final Size size = Size.fromInt(Pointer.size() * MAX_NUMBER_OF_ROOTS);
        rootsPointer = Memory.allocate(size);
        rootsRegion.setStart(rootsPointer);
        rootsRegion.setSize(size);
    }

    /**
     * @return base of the specially allocated memory region containing inspectable root pointers
     */
    public static Pointer rootsPointer() {
        return rootsPointer;
    }

    /**
     * Records the old and new locations of a just-completed object relocation.
     *
     * @param oldCellLocation
     * @param newCellLocation
     */
    public static void notifyObjectRelocated(Address oldCellLocation,  Address newCellLocation) {
        recentRelocationOldCell = oldCellLocation;
        recentRelocationNewCell = newCellLocation;
    }

    /**
     * Records that a GC has begun, using an inspectable counter.
     */
    public static void notifyGCStarting() {
        collectionEpoch++;
        inspectableGCStarting(collectionEpoch);
    }

    /**
     * An empty method whose purpose is to be interrupted by the Inspector
     * when it needs to observe the VM at the beginning of a GC.
     * <br>
     * This particular method is intended for internal use by the inspector.
     * Should a user wish to break at the beginning of GC, another, more
     * convenient inspectable method is provided
     *
     * @param collectionEpoch the GC epoch that is starting.
     * @see HeapScheme.Static#inspectableGCStarting()
     */
    @INSPECTED
    @NEVER_INLINE
    private static void inspectableGCStarting(long collectionEpoch) {
    }

    /**
     * Records that a GC has concluded, using an inspectable counter.
     */
    public static void notifyGCComplete() {
        rootEpoch = collectionEpoch;
        inspectableGCComplete(collectionEpoch);
    }

    /**
     * An empty method whose purpose is to be interrupted by the Inspector
     * when it needs to observe the VM at the conclusion of a GC.
     * <br>
     * This particular method is intended for internal use by the inspector.
     * Should a user wish to break at the conclusion of GC, another, more
     * convenient inspectable method is provided
     *
     * @param collectionEpoch the GC epoch that is ending.
     * @see HeapScheme.Static#inspectableGCComplete()
     */
    @INSPECTED
    @NEVER_INLINE
    private static void inspectableGCComplete(long collectionEpoch) {
    }
}
