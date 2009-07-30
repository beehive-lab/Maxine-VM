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
    public static RuntimeMemoryRegion rootsRegion = new RuntimeMemoryRegion("TeleRoots");

    /**
     * Inspectable location of the memory allocated for the Inspector's root table.
     * Equivalent to {@link RuntimeMemoryRegion#start()}, but it must be
     * readable by the Inspector using only low level operations during startup.
     */
    @INSPECTED
    public static Pointer rootsPointer = Pointer.zero();

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
     * How many words of the Heap refer to one Word in the card table.
     */
    private static int cardTableRatio = 100;

    private static long cardTableSize = 0;

    private static Pointer cardTablePointer = Pointer.zero();

    private static int[] cardTableRegions;

    /**
     * Stores memory allocated by the heap in an location that can
     * be inspected easily.
     * <br>
     * No-op when VM is not being inspected.
     *
     * @param memoryRegions regions allocated by the heap implementation
     */
    public static void init(MemoryRegion... memoryRegions) {
        if (MaxineMessenger.isVmInspected()) {
            InspectableHeapInfo.memoryRegions = memoryRegions;
            initRootsRegion();
            initCardTable(memoryRegions);
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

    private static void initCardTable(MemoryRegion[] memoryRegions) {
        if (cardTablePointer.equals(Pointer.zero())) {
            for (MemoryRegion memoryRegion : memoryRegions) {
                cardTableSize += calculateNumberOfCardTableEntries(memoryRegion);
            }
            cardTablePointer = Memory.allocate(Size.fromLong(cardTableSize));
        }
    }

    private static long calculateNumberOfCardTableEntries(MemoryRegion memoryRegion) {
        long cardTableEntries = 0;
        cardTableEntries += memoryRegion.size().toLong() / cardTableRatio;
        if (memoryRegion.size().toLong() % cardTableRatio != 0) {
            cardTableEntries += Word.size();
        }
        return cardTableEntries;
    }

    public void touchCardTableField(Address address) {
        long cardTableEntry = 0;

        for (MemoryRegion memoryRegion : memoryRegions) {
            if (memoryRegion.contains(address)) {
                //calculate offset;
            } else {
                cardTableEntry += calculateNumberOfCardTableEntries(memoryRegion);
            }
        }
    }

    /**
     * @return base of the specially allocated memory region containing inspectable root pointers
     */
    public static Pointer rootsPointer() {
        return rootsPointer;
    }

    /**
     * Records that a GC has begun, using an inspectable counter.
     */
    public static void beforeGarbageCollection() {
        collectionEpoch++;
    }

    /**
     * Records that a GC has concluded, using an inspectable counter.
     */
    public static void afterGarbageCollection() {
        rootEpoch = collectionEpoch;
    }
}
