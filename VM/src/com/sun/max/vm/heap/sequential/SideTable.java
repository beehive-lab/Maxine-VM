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
package com.sun.max.vm.heap.sequential;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.debug.*;
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

    private static final boolean _debug = false;

    private static Size _chunkSize = Size.fromInt(1 << CHUNK_SHIFT);

    // Memory region occupied by the SideTable
    private RuntimeMemoryRegion _region = new RuntimeMemoryRegion(Size.zero(), Size.zero());
    public static Address _sideTableStart; // SideTable Table start
    public static Address _sideTableCoveredRegionStart; // SideTable Region(s) start
    public long _numberOfChunks;
    public Size _sideTableSize;
    public Address _biasedSideTableBase;

    @INLINE
    public static final int getChunkIndexFromHeapAddress(Address address) {
        return address.minus(_sideTableCoveredRegionStart).unsignedShiftedRight(CHUNK_SHIFT).toInt();
    }

    @INLINE
    public static final int getAddressShiftLength() {
        return CHUNK_SHIFT;
    }

    @INLINE
    public static final Address getChunkFromHeapAddress(Address address) {
        final int offset = getChunkIndexFromHeapAddress(address);
        return _sideTableStart.plus(offset * CHUNK_SLOT_LENGTH);
    }

    @INLINE
    public final Address getHeapAddressFromChunkIndex(int cardIndex) {
        return _sideTableCoveredRegionStart.plus(cardIndex * chunkSize().toInt());
    }

    @INLINE
    private long chunkValue(Address address) {
        return _sideTableStart.plus(getChunkIndexFromHeapAddress(address) * CHUNK_SLOT_LENGTH).asPointer().getLong();
    }

    @INLINE
    private long chunkValue(int index) {
        return _sideTableStart.plus(index * CHUNK_SLOT_LENGTH).asPointer().getLong();
    }

    private void setStart(int index) {
        setStart(_sideTableStart.plus(index * CHUNK_SLOT_LENGTH));
    }

    private void setCreating(int index) {
        setCreating(_sideTableStart.plus(index * CHUNK_SLOT_LENGTH));
    }

    public int compareAndSwapStart(int index) {
        return compareAndSwapStart(_sideTableStart.plus(index * CHUNK_SLOT_LENGTH));
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
        setStart(_sideTableStart.plus(index * CHUNK_SLOT_LENGTH));
    }

    public void setMiddle(Address addr) {
        addr.asPointer().setInt(MIDDLE);
    }

    private void setScavenged(int index) {
        setScavenged(_sideTableStart.plus(index * CHUNK_SLOT_LENGTH));
    }

    private void setScavenged(Address addr) {
        addr.asPointer().compareAndSwapInt(START, SCAVENGED);
    }

    public static int chunkShift() {
        return CHUNK_SHIFT;
    }

    public static Size chunkSize() {
        return _chunkSize;
    }

    @INLINE
    public static final Address sideTableBase() {
        return _sideTableStart;
    }

    @INLINE
    public final Size sideTableSize() {
        return _sideTableSize;
    }

    @INLINE
    public final Address heapStart() {
        return _sideTableCoveredRegionStart;
    }

    // Initialisation of card Regions. Explanation
    public void initialize(Address start, Size size, Address sideTableStart) {
        _sideTableCoveredRegionStart = start;
        _region.setStart(start);
        _region.setSize(size);
        _sideTableStart = sideTableStart;
        // calculate the integral number of chunks required
        _numberOfChunks = (size.toLong() % chunkSize().toLong() == 0) ? size.toLong() / chunkSize().toLong() : size.toLong() / chunkSize().toLong() + 1;
        _sideTableSize = Size.fromLong(_numberOfChunks * CHUNK_SLOT_LENGTH);

        if (Heap.verbose()) {
            Debug.print("\nSidetable.initialize: covered region start ");
            Debug.println(_region.start());
            Debug.print("Sidetable.initialize: covered region size ");
            Debug.println(_region.size());
            Debug.print("Sidetable.initialize: Sidetable start ");
            Debug.println(_sideTableStart);
            Debug.print("Sidetable.initialize: Sidetable chunk size ");
            Debug.println(chunkSize());
            Debug.print("Sidetable.initialize: Sidetable size ");
            Debug.println(_sideTableSize.toLong());
            Debug.print("Sidetable.initialize: sideTable end ");
            Debug.println(_sideTableStart.plus(_sideTableSize));
            Debug.print("Sidetable.initialize: number of cards ");
            Debug.println(_numberOfChunks);
        }

        if (VirtualMemory.allocateMemoryAtFixedAddress(_sideTableStart, _sideTableSize) == false) {
            Debug.print("MaxineVM: Could not allocate memory starting @address ");
            Debug.print(_sideTableStart);
            MaxineVM.native_exit(MaxineVM.HARD_EXIT_CODE);
        }


        if (Heap.verbose()) {
            Debug.print("--boot address shift");
            Debug.println(Heap.bootHeapRegion().start().unsignedShiftedRight(chunkShift()));
            Debug.print("--sidetable start");
            Debug.println(_sideTableStart);
        }

        _biasedSideTableBase = _sideTableStart.minus(Heap.bootHeapRegion().start().unsignedShiftedRight(chunkShift()));
        clearAllChunkSlots();
    }

    public void clearAllChunkSlots() {
        if (Heap.verbose()) {
            Debug.println("Sidetable.clearAllChunkSlots: clearing the card table before use ");
        }
        clearSideTableRegion(_sideTableStart, _sideTableStart.plus(_sideTableSize));

    }

    public void restoreAllChunkSlots() {
        if (Heap.verbose()) {
            Debug.println("Sidetable.clearAllChunkSlots: clearing the card table before use ");
        }
        restoreSideTableRegion(_sideTableStart, _sideTableStart.plus(_sideTableSize));

    }

    // clears all cards from start to end , including end.
    void clearSideTableRegion(Address start, Address end) {
        if (Heap.verbose()) {
            Debug.print("Sidetable.clearAllChunkSlots: begin ");
            Debug.println(start);
            Debug.print("Sidetable.clearAllChunkSlots: end ");
            Debug.println(end);
        }

        Address addr = start;
        while (addr.lessEqual(end)) {
            setMiddle(addr);
            addr = addr.plus(CHUNK_SLOT_LENGTH);
        }

        if (Heap.verbose()) {
            Debug.println("Sidetable.clearAllChunkSlots   Region: done");

        }
    }

    // clears all cards from start to end , including end.
    void restoreSideTableRegion(Address start, Address end) {
        if (Heap.verbose()) {
            Debug.print("Sidetable.restoreAllChunkSlots: begin ");
            Debug.println(start);
            Debug.print("Sidetable.restoreAllChunkSlots: end ");
            Debug.println(end);
        }

        Address addr = start;
        while (addr.lessEqual(end)) {
            if (isScavenged(addr)) {
                setStart(addr);
            }
            addr = addr.plus(CHUNK_SLOT_LENGTH);
        }

        if (Heap.verbose()) {
            Debug.println("Sidetable:restoreAllChunkSlots   Region: done");

        }
    }

    public void dumpSideTable() {
        final Address start = _sideTableStart;
        final Address end = _sideTableStart.plus(_sideTableSize.minus(CHUNK_SLOT_LENGTH));

        Debug.print("SideRegion.dumpSideTable: begin");
        Debug.print(start);
        Debug.print("SideRegion.dumpSideTable: end ");
        Debug.print(end);

        Address addr = start;
        while (addr.lessEqual(end)) {
            if (isStart(addr)) {
                Debug.print("---- 1 ");
                Debug.print(addr);
            } else if (isScavenged(addr)) {
                Debug.print("++ 2 ");
                Debug.print(addr);

            }
            addr = addr.plus(CHUNK_SLOT_LENGTH);
        }

        Debug.println("SideRegion.dumpSideTable: done");
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
            Debug.println("CardRegion.clearAllCards: clearing the card table before use ");
        }
        restoreCardRegion(_sideTableStart, _sideTableStart.plus(_sideTableSize));

    }

    // clears all cards from start to end , including end.
    void restoreCardRegion(Address start, Address end) {
        if (Heap.verbose()) {
            Debug.print("SideTableRegion.clearCardRegion: begin ");
            Debug.println(start);
            Debug.print("SideTableRegion.clearCardRegion: end ");
            Debug.println(end);
        }

        Address addr = start;
        while (addr.lessEqual(end)) {
            if (isScavenged(addr)) {
                setStart(addr);
            }
            addr = addr.plus(CHUNK_SLOT_LENGTH);
        }

        if (Heap.verbose()) {
            Debug.println("CardRegion.clearCard   Region: done");

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
