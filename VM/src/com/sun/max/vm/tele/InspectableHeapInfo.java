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
import com.sun.max.vm.runtime.*;

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
    public static RuntimeMemoryRegion rootsRegion;

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
     * How many words of the heap refer to one Word in the card table.
     */
    @INSPECTED
    private static int cardTableRatio = 100;

    /**
     * Number of entries in the card table.
     */
    @INSPECTED
    private static int totalCardTableEntries = 0;

    /**
     * Card table memory.
     */
    @INSPECTED
    private static Pointer cardTablePointer = Pointer.zero();

    /**
     * Address of object before compaction.
     */
    @INSPECTED
    public static Address oldAddress;

    /**
     * Address of object after compaction.
     * TODO: remove, not used
     */
    @INSPECTED
    public static Address newAddress;

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
        if (MaxineMessenger.isVmInspected()) {
            InspectableHeapInfo.memoryRegions = memoryRegions;

            try {
                Heap.enableImmortalMemoryAllocation();
                rootsRegion = new RuntimeMemoryRegion("TeleRoots");
            } finally {
                Heap.disableImmortalMemoryAllocation();
            }

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

    /**
     * Initialize the card table.
     * @param memoryRegions
     */
    private static void initCardTable(MemoryRegion[] memoryRegions) {
        if (cardTablePointer.equals(Pointer.zero())) {
            for (MemoryRegion memoryRegion : memoryRegions) {
                totalCardTableEntries += calculateNumberOfCardTableEntries(memoryRegion);
            }
            cardTablePointer = Memory.allocate(Size.fromInt(totalCardTableEntries * Word.size()));
        }
    }

    /**
     * Calculates the number of slots in the card table for a given memory region.
     * @param memoryRegion the memory region
     * @return number of card table slots
     */
    private static int calculateNumberOfCardTableEntries(MemoryRegion memoryRegion) {
        int nrOfWords = memoryRegion.size().dividedBy(Word.size()).toInt();
        int cardTableEntries = nrOfWords / cardTableRatio;
        if (nrOfWords % cardTableRatio != 0) {
            cardTableEntries++;
        }
        return cardTableEntries;
    }

    /**
     * Touches as field in the card table at the corresponding address.
     * @param address
     * @return field
     */
    public static Word touchCardTableField(Address address) {
        return touchCardTableField(address, InspectableHeapInfo.memoryRegions);
    }

    /**
     * Touches as field in the card table at the corresponding address.
     * @param address
     * @param memoryRegions
     * @return field
     */
    public static Word touchCardTableField(Address address, MemoryRegion... memoryRegions) {
        int index;
        index = getCardTableIndex(address, memoryRegions);
        if (index == -1) {
            FatalError.unexpected("Card table index invalid");
        }

        return cardTablePointer.getWord(index);
    }

    /**
     * Returns the card table slot number (index) to a corresponding address.
     * @param address
     * @param memoryRegions
     * @return card table index
     */
    public static int getCardTableIndex(Address address, MemoryRegion... memoryRegions) {
        int cardTableEntry = 0;

        // used for sorting algorithm
        Address last = Address.zero();
        Address current = Address.zero();
        int i = 0;
        int pos = 0;

        //sort memory regions without allocation
        for (int j = 0; j < memoryRegions.length; j++) {
            for (MemoryRegion memoryRegion : memoryRegions) {
                if (memoryRegion.start().greaterThan(last) && (current.greaterThan(memoryRegion.start()) || current.equals(Address.zero()))) {
                    current = memoryRegion.start();
                    pos = i;
                }
                i++;
            }

            // do work
            if (memoryRegions[pos].contains(address)) {
                final int offset = address.minus(memoryRegions[pos].start()).toInt();
                return cardTableEntry + calculateCardTableIndexInMemoryRegion(offset);
            }
            cardTableEntry += calculateNumberOfCardTableEntries(memoryRegions[pos]);

            // reset sorting algorithm
            i = 0;
            last = current;
            current = Address.zero();
        }
        return -1;
    }

    /**
     * Get start of memory region covered by index.
     * @param index
     * @param memoryRegions
     * @return Memory region start address
     */
    public static Address getStartAddressOfMemoryRange(int index, MemoryRegion... memoryRegions) {
        int cardTableEntries = 0;
        int tmpCardTableEntries = 0;

        // used for sorting algorithm
        Address last = Address.zero();
        Address current = Address.zero();
        int i = 0;
        int pos = 0;

        //sort memory regions without allocation
        for (int j = 0; j < memoryRegions.length; j++) {
            for (MemoryRegion memoryRegion : memoryRegions) {
                if (memoryRegion.start().greaterThan(last) && (current.greaterThan(memoryRegion.start()) || current.equals(Address.zero()))) {
                    current = memoryRegion.start();
                    pos = i;
                }
                i++;
            }

            // do work
            cardTableEntries += calculateNumberOfCardTableEntries(memoryRegions[pos]);
            if (cardTableEntries >= index + 1) {
                int offset;
                if (tmpCardTableEntries == 0) {
                    offset = index;
                } else {
                    offset = index - tmpCardTableEntries;
                }
                return memoryRegions[pos].start().plus(offset * cardTableRatio * Word.size());
            }
            tmpCardTableEntries = cardTableEntries;

            // reset sorting algorithm
            i = 0;
            last = current;
            current = Address.zero();
        }
        return Address.zero();
    }

    /**
     * Get end of memory region covered by index.
     * @param index
     * @param memoryRegions
     * @return Memory region start address
     */
    public static Address getEndAddressOfMemoryRange(int index, MemoryRegion... memoryRegions) {
        Address address = getStartAddressOfMemoryRange(index, memoryRegions);
        return address.plus(cardTableRatio * Word.size() - 1);
    }

    /**
     * Calculates the card table index to an offset.
     * @param offset
     * @return index
     */
    private static int calculateCardTableIndexInMemoryRegion(int offset) {
        return Unsigned.idiv(offset / cardTableRatio, Word.size());
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
