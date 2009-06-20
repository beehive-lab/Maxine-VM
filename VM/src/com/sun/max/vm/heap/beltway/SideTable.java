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
package com.sun.max.vm.heap.beltway;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;

/**
 * @author Christos Kotselidis
 */
public class SideTable {

    public static final int MIDDLE = 0;
    public static final int START = 1;
    public static final int SCAVENGED = 2;
    public static final int CREATING = 3;

    public static final int CHUNK_SHIFT = 9;
    public static final int CHUNK_SLOT_LENGTH = VMConfiguration.hostOrTarget().wordWidth().numberOfBytes();

    private static final boolean debug = false;

    private static Size chunkSize = Size.fromInt(1 << CHUNK_SHIFT);

    // Memory region occupied by the SideTable
    private RuntimeMemoryRegion region = new RuntimeMemoryRegion(Size.zero(), Size.zero());
    public static Address sideTableStart; // SideTable Table start
    public static Address sideTableCoveredRegionStart; // SideTable Region(s) start
    public long numberOfChunks;
    public Size sideTableSize;
    public Address biasedSideTableBase;

    @INLINE
    public static final int getChunkIndexFromHeapAddress(Address address) {
        return address.minus(sideTableCoveredRegionStart).unsignedShiftedRight(CHUNK_SHIFT).toInt();
    }

    @INLINE
    public static final int getAddressShiftLength() {
        return CHUNK_SHIFT;
    }

    @INLINE
    public static final Address getChunkFromHeapAddress(Address address) {
        final int offset = getChunkIndexFromHeapAddress(address);
        return sideTableStart.plus(offset * CHUNK_SLOT_LENGTH);
    }

    @INLINE
    public final Address getHeapAddressFromChunkIndex(int cardIndex) {
        return sideTableCoveredRegionStart.plus(cardIndex * chunkSize().toInt());
    }

    @INLINE
    private long chunkValue(Address address) {
        return sideTableStart.plus(getChunkIndexFromHeapAddress(address) * CHUNK_SLOT_LENGTH).asPointer().getLong();
    }

    @INLINE
    private long chunkValue(int index) {
        return sideTableStart.plus(index * CHUNK_SLOT_LENGTH).asPointer().getLong();
    }

    private void setStart(int index) {
        setStart(sideTableStart.plus(index * CHUNK_SLOT_LENGTH));
    }

    private void setCreating(int index) {
        setCreating(sideTableStart.plus(index * CHUNK_SLOT_LENGTH));
    }

    public int compareAndSwapStart(int index) {
        return compareAndSwapStart(sideTableStart.plus(index * CHUNK_SLOT_LENGTH));
    }

    public void setStart(Address addr) {
        addr.asPointer().setInt(START);
    }

    public void setCreating(Address addr) {
        addr.asPointer().setInt(CREATING);
    }

    public int compareAndSwapStart(Address addr) {
        return addr.asPointer().compareAndSwapInt(START, SCAVENGED);
    }

    private void setMiddle(int index) {
        setStart(sideTableStart.plus(index * CHUNK_SLOT_LENGTH));
    }

    public void setMiddle(Address addr) {
        addr.asPointer().setInt(MIDDLE);
    }

    private void setScavenged(int index) {
        setScavenged(sideTableStart.plus(index * CHUNK_SLOT_LENGTH));
    }

    private void setScavenged(Address addr) {
        addr.asPointer().compareAndSwapInt(START, SCAVENGED);
    }

    public static int chunkShift() {
        return CHUNK_SHIFT;
    }

    public static Size chunkSize() {
        return chunkSize;
    }

    @INLINE
    public static final Address sideTableBase() {
        return sideTableStart;
    }

    @INLINE
    public final Size sideTableSize() {
        return sideTableSize;
    }

    @INLINE
    public final Address heapStart() {
        return sideTableCoveredRegionStart;
    }

    // Initialisation of card Regions. Explanation
    public void initialize(Address start, Size size, Address sideTableStart) {
        sideTableCoveredRegionStart = start;
        region.setStart(start);
        region.setSize(size);
        SideTable.sideTableStart = sideTableStart;
        // calculate the integral number of chunks required
        numberOfChunks = (size.toLong() % chunkSize().toLong() == 0) ? size.toLong() / chunkSize().toLong() : size.toLong() / chunkSize().toLong() + 1;
        sideTableSize = Size.fromLong(numberOfChunks * CHUNK_SLOT_LENGTH);

        if (Heap.verbose()) {
            Log.print("\nSidetable.initialize: covered region start ");
            Log.println(region.start());
            Log.print("Sidetable.initialize: covered region size ");
            Log.println(region.size());
            Log.print("Sidetable.initialize: Sidetable start ");
            Log.println(sideTableStart);
            Log.print("Sidetable.initialize: Sidetable chunk size ");
            Log.println(chunkSize());
            Log.print("Sidetable.initialize: Sidetable size ");
            Log.println(sideTableSize.toLong());
            Log.print("Sidetable.initialize: sideTable end ");
            Log.println(sideTableStart.plus(sideTableSize));
            Log.print("Sidetable.initialize: number of cards ");
            Log.println(numberOfChunks);
        }

        if (VirtualMemory.allocatePageAlignedAtFixedAddress(sideTableStart, sideTableSize, VirtualMemory.Type.HEAP) == false) {
            Log.print("MaxineVM: Could not allocate memory starting @address ");
            Log.print(sideTableStart);
            MaxineVM.native_exit(MaxineVM.HARD_EXIT_CODE);
        }


        if (Heap.verbose()) {
            Log.print("--boot address shift");
            Log.println(Heap.bootHeapRegion().start().unsignedShiftedRight(chunkShift()));
            Log.print("--sidetable start");
            Log.println(sideTableStart);
        }

        biasedSideTableBase = sideTableStart.minus(Heap.bootHeapRegion().start().unsignedShiftedRight(chunkShift()));
        clearAllChunkSlots();
    }

    public void clearAllChunkSlots() {
        if (Heap.verbose()) {
            Log.println("Sidetable.clearAllChunkSlots: clearing the card table before use ");
        }
        clearSideTableRegion(sideTableStart, sideTableStart.plus(sideTableSize));

    }

    public void restoreAllChunkSlots() {
        if (Heap.verbose()) {
            Log.println("Sidetable.clearAllChunkSlots: clearing the card table before use ");
        }
        restoreSideTableRegion(sideTableStart, sideTableStart.plus(sideTableSize));

    }

    // clears all cards from start to end , including end.
    void clearSideTableRegion(Address start, Address end) {
        if (Heap.verbose()) {
            Log.print("Sidetable.clearAllChunkSlots: begin ");
            Log.println(start);
            Log.print("Sidetable.clearAllChunkSlots: end ");
            Log.println(end);
        }

        Address addr = start;
        while (addr.lessEqual(end)) {
            setMiddle(addr);
            addr = addr.plus(CHUNK_SLOT_LENGTH);
        }

        if (Heap.verbose()) {
            Log.println("Sidetable.clearAllChunkSlots   Region: done");

        }
    }

    // clears all cards from start to end , including end.
    void restoreSideTableRegion(Address start, Address end) {
        if (Heap.verbose()) {
            Log.print("Sidetable.restoreAllChunkSlots: begin ");
            Log.println(start);
            Log.print("Sidetable.restoreAllChunkSlots: end ");
            Log.println(end);
        }

        Address addr = start;
        while (addr.lessEqual(end)) {
            if (isScavenged(addr)) {
                setStart(addr);
            }
            addr = addr.plus(CHUNK_SLOT_LENGTH);
        }

        if (Heap.verbose()) {
            Log.println("Sidetable:restoreAllChunkSlots   Region: done");

        }
    }

    public void dumpSideTable() {
        final Address start = sideTableStart;
        final Address end = sideTableStart.plus(sideTableSize.minus(CHUNK_SLOT_LENGTH));

        Log.print("SideRegion.dumpSideTable: begin");
        Log.print(start);
        Log.print("SideRegion.dumpSideTable: end ");
        Log.print(end);

        Address addr = start;
        while (addr.lessEqual(end)) {
            if (isStart(addr)) {
                Log.print("---- 1 ");
                Log.print(addr);
            } else if (isScavenged(addr)) {
                Log.print("++ 2 ");
                Log.print(addr);

            }
            addr = addr.plus(CHUNK_SLOT_LENGTH);
        }

        Log.println("SideRegion.dumpSideTable: done");
    }

    private static  boolean isStart(Address addr) {
        return addr.asPointer().readInt(0) == START;
    }

    public static boolean isCreating(Address addr) {
        return addr.asPointer().readInt(0) == CREATING;
    }

    private boolean isMiddle(Address addr) {
        return addr.asPointer().readInt(0) == MIDDLE;
    }

    public static boolean isScavenged(Address addr) {
        return addr.asPointer().readInt(0) == SCAVENGED;
    }

    @INLINE
    public static final boolean isStart(int index) {
        return isStart(sideTableBase().plus(index * CHUNK_SLOT_LENGTH));
    }

    @INLINE
    public static final boolean isCreating(int index) {
        return isCreating(sideTableBase().plus(index * CHUNK_SLOT_LENGTH));
    }

    @INLINE
    public  final boolean isMiddle(int index) {
        return isMiddle(sideTableBase().plus(index * CHUNK_SLOT_LENGTH));
    }

    @INLINE
    public  static final boolean isScavenged(int index) {
        return isScavenged(sideTableBase().plus(index * CHUNK_SLOT_LENGTH));
    }

    public void restoreAllChunks() {
        if (Heap.verbose()) {
            Log.println("CardRegion.clearAllCards: clearing the card table before use ");
        }
        restoreCardRegion(sideTableStart, sideTableStart.plus(sideTableSize));

    }

    // clears all cards from start to end , including end.
    void restoreCardRegion(Address start, Address end) {
        if (Heap.verbose()) {
            Log.print("SideTableRegion.clearCardRegion: begin ");
            Log.println(start);
            Log.print("SideTableRegion.clearCardRegion: end ");
            Log.println(end);
        }

        Address addr = start;
        while (addr.lessEqual(end)) {
            if (isScavenged(addr)) {
                setStart(addr);
            }
            addr = addr.plus(CHUNK_SLOT_LENGTH);
        }

        if (Heap.verbose()) {
            Log.println("CardRegion.clearCard   Region: done");

        }
    }

    private static void setSideTableSlot(Address heapAddress, int value) {
        getChunkFromHeapAddress(heapAddress).asPointer().writeInt(0, value);
    }

    @INLINE
    public static final void markStartSideTable(Address heapAddress) {
        setSideTableSlot(heapAddress, START);
    }

    @INLINE
    public final void markCreatingSideTable(Address heapAddress) {
        setSideTableSlot(heapAddress, CREATING);
    }

    @INLINE
    public  final void markMiddleSideTable(Address heapAddress) {
        setSideTableSlot(heapAddress, MIDDLE);
    }

    @INLINE
    public  static final void markScavengeSideTable(Address heapAddress) {
        setSideTableSlot(heapAddress, SCAVENGED);
    }

}
