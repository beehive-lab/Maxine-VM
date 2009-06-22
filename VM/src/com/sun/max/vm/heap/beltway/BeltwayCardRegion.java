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

import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.thread.*;

/**
 * @author Karthikeyan M.
 * @author Christos Kotselidis
 */
public class BeltwayCardRegion {

    public static final byte DIRTY = 0;
    public static final byte CLEAN = (byte) 0xff;

    public static final int CARD_SHIFT = 9;
    public static final int CARD_SLOT_LENGTH = 1;
    private static final boolean debug = false;
    private static Pointer adjustedCardTable = Pointer.zero();
    private static Size cardSize = Size.fromInt(1 << CARD_SHIFT);

    // Memory region occupied by the CardTable
    private RuntimeMemoryRegion region = new RuntimeMemoryRegion(Size.zero(), Size.zero());
    public Address cardTableStart; // Card Table start
    public Address coveredRegionStart; // Card Region(s) start
    public long numberOfCards;
    public Size cardTableSize;

    public final int getCardIndexFromHeapAddress(Address address) {
        return address.minus(coveredRegionStart).unsignedShiftedRight(CARD_SHIFT).toInt();
    }

    public static final int getAddressShiftLength() {
        return CARD_SHIFT;
    }

    public static Pointer getAdjustedCardTable() {
        return adjustedCardTable;
    }

    public final Address getCardFromHeapAddress(Address address) {
        final int offset = getCardIndexFromHeapAddress(address);
        return cardTableStart.plus(offset * CARD_SLOT_LENGTH);
    }

    public final Address getHeapAddressFromCardIndex(int cardIndex) {
        return coveredRegionStart.plus(cardIndex * cardSize().toInt());
    }

    private byte cardValue(Address address) {
        return cardTableStart.plus(getCardIndexFromHeapAddress(address) * CARD_SLOT_LENGTH).asPointer().getByte();
    }

    private byte cardValue(int index) {
        return cardTableStart.plus(index * CARD_SLOT_LENGTH).asPointer().getByte();
    }

    private void cleanCard(int index) {
        clearCard(cardTableStart.plus(index * CARD_SLOT_LENGTH));
    }

    private void clearCard(Address addr) {
        int count = 0;
        while (count++ < CARD_SLOT_LENGTH) {
            addr.asPointer().setByte(CLEAN);
        }
    }

    public static Size cardSize() {
        return cardSize;
    }

    public final Address cardTableBase() {
        return cardTableStart;
    }

    public final Address heapStart() {
        return coveredRegionStart;
    }

    public void initialize(Address start, Size size, Address cardTableStart) {
        coveredRegionStart = start;
        region.setStart(start);
        region.setSize(size);

        // calculate the integral number of cards required
        numberOfCards = (size.toLong() % cardSize().toLong() == 0) ? size.toLong() / cardSize().toLong() : size.toLong() / cardSize().toLong() + 1;
        cardTableSize = Size.fromLong(numberOfCards * CARD_SLOT_LENGTH);

        if (Heap.verbose()) {
            Log.print("\nCardRegion.initialize: covered region start ");
            Log.println(region.start());
            Log.print("CardRegion.initialize: covered region size ");
            Log.println(region.size());
            Log.print("CardRegion.initialize: cardTable start ");
            Log.println(cardTableStart);
            Log.print("CardRegion.initialize: cardTable card size ");
            Log.println(cardSize());
            Log.print("CardRegion.initialize: card table size ");
            Log.print(numberOfCards * CARD_SLOT_LENGTH);
            Log.println();
            Log.print("CardRegion.initialize: cardTable end ");
            Log.println(cardTableStart.plus(Size.fromLong(numberOfCards * CARD_SLOT_LENGTH)));
            Log.print("CardRegion.initialize: number of cards ");
            Log.println(numberOfCards);
        }

        if (VirtualMemory.allocatePageAlignedAtFixedAddress(cardTableStart, Size.fromLong(numberOfCards * CARD_SLOT_LENGTH), VirtualMemory.Type.HEAP) == false) {
            Log.print("MaxineVM: Could not allocate memory starting @address: ");
            Log.println(cardTableStart);
            MaxineVM.native_exit(MaxineVM.HARD_EXIT_CODE);
        }
        this.cardTableStart = cardTableStart;
        clearAllCards();
    }

    public void clearAllCards() {
        if (Heap.verbose()) {
            Log.println("CardRegion.clearAllCards: clearing the card table before use ");
        }
        clearCardRegion(cardTableStart, cardTableStart.plus(cardTableSize));
    }

    // clears all cards from start to end , including end.
    void clearCardRegion(Address start, Address end) {
        if (Heap.verbose()) {
            Log.print("CardRegion.clearCardRegion: begin ");
            Log.println(start);
            Log.print("CardRegion.clearCardRegion: end ");
            Log.println(end);
        }

        Address addr = start;
        while (addr.lessEqual(end)) {
            clearCard(addr);
            addr = addr.plus(CARD_SLOT_LENGTH);
        }

        if (Heap.verbose()) {
            Log.println("CardRegion.clearCard   Region: done");

        }
    }

    public void dumpCardTable() {
        final Address start = cardTableStart;
        final Address end = cardTableStart.plus(cardTableSize.minus(CARD_SLOT_LENGTH));

        Log.print("CardRegion.dumpCardTable: begin");
        Log.println(start);
        Log.print("CardRegion.dumpCardTable: end ");
        Log.println(end);

        Address addr = start;
        while (addr.lessEqual(end)) {
            if (isCardMarked(addr)) {
                Log.print("---- 0 ");
                Log.println(addr);
            } else {
                Log.print("1 ");
                Log.println(addr);
            }

            addr = addr.plus(CARD_SLOT_LENGTH);
        }

        Log.println("CardRegion.dumpCardTable: done");
    }

    private boolean isCardMarked(Address addr) {
        return addr.asPointer().getByte() == DIRTY;
    }

    public final boolean isCardMarked(int index) {
        return isCardMarked(cardTableBase().plus(index * CARD_SLOT_LENGTH));
    }

    public static Pointer adjustedCardTableBase(Pointer auxiliarySpace) {
        return auxiliarySpace.minus(Heap.bootHeapRegion().start().unsignedShiftedRight(CARD_SHIFT));
    }

    private static class SetLocals implements Procedure<VmThread> {
        public void run(VmThread thread) {
            ADJUSTED_CARDTABLE_BASE.setConstantWord(thread.vmThreadLocals(), adjustedCardTable);
        }
    }

    private static SetLocals setLocals = new SetLocals();
    /**
     * Copies cards from auxiliary card table ( used for marking updates before the regular card table is created) to
     * regular card table and sets regular card table's adjusted address in the vm thread locals.
     *
     * @param regularCardTable
     */
    public static void switchToRegularCardTable(Pointer regularCardTable) {
        // copy cards from primordial card table to the newly created cardtable
        final Pointer primordialCardTable = VmThreadLocal.ADJUSTED_CARDTABLE_BASE.getConstantWord(MaxineVM.primordialVmThreadLocals()).asPointer().plus(
                        Heap.bootHeapRegion().start().unsignedShiftedRight(BeltwayCardRegion.CARD_SHIFT));
        final int primordialCardTableSize = Heap.bootHeapRegion().size().plus(Code.bootCodeRegion().size()).unsignedShiftedRight(CARD_SHIFT).toInt();


        adjustedCardTable = BeltwayCardRegion.adjustedCardTableBase(regularCardTable);

        if (Heap.verbose()) {
            Log.print("switchToRegularCardTable: primordialCardTable address ");
            Log.println(primordialCardTable);
            Log.print("switchToRegularCardTable: primordialCardTable size ");
            Log.println(primordialCardTableSize);
            Log.print("switchToRegularCardTable: regular card table address ");
            Log.println(regularCardTable);
            Log.print("switchToRegularCardTable: regular adjusted card table address ");
            Log.println(adjustedCardTable);
        }

        for (int index = 0; index < primordialCardTableSize; index++) {
            // copy the primordial card table to the regular card table
            regularCardTable.writeByte(index, primordialCardTable.readByte(index));
        }
        VmThreadMap.ACTIVE.forAllVmThreads(null, setLocals);
    }

    public final Size cardTableSize() {
        return cardTableSize;
    }
}
