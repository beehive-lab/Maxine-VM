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

import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.thread.*;

/**
 * @author Karthikeyan M.
 * @author Christos Kotselidis
 */

public class CardRegion {

    public static final byte DIRTY = 0;
    public static final byte CLEAN = (byte) 0xff;

    public static final int CARD_SHIFT = 9;
    public static final int CARD_SLOT_LENGTH = 1;
    private static final boolean _debug = false;
    private static Pointer _adjustedCardTable = Pointer.zero();
    private static Size _cardSize = Size.fromInt(1 << CARD_SHIFT);
    // Memory region occupied by the CardTable
    private RuntimeMemoryRegion _region = new RuntimeMemoryRegion(Size.zero(), Size.zero());
    public Address _cardTableStart; // Card Table start
    public Address _coveredRegionStart; // Card Region(s) start
    public long _numberOfCards;
    public Size _cardTableSize;

    public final int getCardIndexFromHeapAddress(Address address) {
        return address.minus(_coveredRegionStart).unsignedShiftedRight(CARD_SHIFT).toInt();
    }

    public static final int getAddressShiftLength() {
        return CARD_SHIFT;
    }

    public static Pointer getAdjustedCardTable() {
        return _adjustedCardTable;
    }

    public final Address getCardFromHeapAddress(Address address) {
        final int offset = getCardIndexFromHeapAddress(address);
        return _cardTableStart.plus(offset * CARD_SLOT_LENGTH);
    }

    public final Address getHeapAddressFromCardIndex(int cardIndex) {
        return _coveredRegionStart.plus(cardIndex * cardSize().toInt());
    }

    private byte cardValue(Address address) {
        return _cardTableStart.plus(getCardIndexFromHeapAddress(address) * CARD_SLOT_LENGTH).asPointer().getByte();
    }

    private byte cardValue(int index) {
        return _cardTableStart.plus(index * CARD_SLOT_LENGTH).asPointer().getByte();
    }

    private void cleanCard(int index) {
        clearCard(_cardTableStart.plus(index * CARD_SLOT_LENGTH));
    }

    private void clearCard(Address addr) {
        int count = 0;
        while (count++ < CARD_SLOT_LENGTH) {
            addr.asPointer().setByte(CLEAN);
        }
    }

    public static int cardShift() {
        return CARD_SHIFT;
    }

    public static Size cardSize() {
        return _cardSize;
    }

    public final Address cardTableBase() {
        return _cardTableStart;
    }

    public final Address heapStart() {
        return _coveredRegionStart;
    }

    // Initialisation of card Regions. Explanation
    public void initialize(Address start, Size size, Address cardTableStart) {
        _coveredRegionStart = start;
        _region.setStart(start);
        _region.setSize(size);

        // calculate the integral number of cards required
        _numberOfCards = (size.toLong() % cardSize().toLong() == 0) ? size.toLong() / cardSize().toLong() : size.toLong() / cardSize().toLong() + 1;
        _cardTableSize = Size.fromLong(_numberOfCards * CARD_SLOT_LENGTH);

        if (Heap.verbose()) {
            Debug.print("\nCardRegion.initialize: covered region start ");
            Debug.println(_region.start());
            Debug.print("CardRegion.initialize: covered region size ");
            Debug.println(_region.size());
            Debug.print("CardRegion.initialize: cardTable start ");
            Debug.println(cardTableStart);
            Debug.print("CardRegion.initialize: cardTable card size ");
            Debug.println(cardSize());
            Debug.print("CardRegion.initialize: card table size ");
            Debug.print(_numberOfCards * CARD_SLOT_LENGTH);
            Debug.println();
            Debug.print("CardRegion.initialize: cardTable end ");
            Debug.println(cardTableStart.plus(Size.fromLong(_numberOfCards * CARD_SLOT_LENGTH)));
            Debug.print("CardRegion.initialize: number of cards ");
            Debug.println(_numberOfCards);
        }

        if (VirtualMemory.allocateMemoryAtFixedAddress(cardTableStart, Size.fromLong(_numberOfCards * CARD_SLOT_LENGTH)) == false) {
            Debug.print("MaxineVM: Could not allocate memory starting @address: ");
            Debug.println(cardTableStart);
            MaxineVM.native_exit(MaxineVM.HARD_EXIT_CODE);
        }
        _cardTableStart = cardTableStart;
        clearAllCards();
    }

    public void clearAllCards() {
        if (Heap.verbose()) {
            Debug.println("CardRegion.clearAllCards: clearing the card table before use ");
        }
        clearCardRegion(_cardTableStart, _cardTableStart.plus(_cardTableSize));
    }

    // clears all cards from start to end , including end.
    void clearCardRegion(Address start, Address end) {
        if (Heap.verbose()) {
            Debug.print("CardRegion.clearCardRegion: begin ");
            Debug.println(start);
            Debug.print("CardRegion.clearCardRegion: end ");
            Debug.println(end);
        }

        Address addr = start;
        while (addr.lessEqual(end)) {
            clearCard(addr);
            addr = addr.plus(CARD_SLOT_LENGTH);
        }

        if (Heap.verbose()) {
            Debug.println("CardRegion.clearCard   Region: done");

        }
    }

    public void dumpCardTable() {
        final Address start = _cardTableStart;
        final Address end = _cardTableStart.plus(_cardTableSize.minus(CARD_SLOT_LENGTH));

        Debug.print("CardRegion.dumpCardTable: begin");
        Debug.println(start);
        Debug.print("CardRegion.dumpCardTable: end ");
        Debug.println(end);

        Address addr = start;
        while (addr.lessEqual(end)) {
            if (isCardMarked(addr)) {
                Debug.print("---- 0 ");
                Debug.println(addr);
            } else {
                Debug.print("1 ");
                Debug.println(addr);
            }

            addr = addr.plus(CARD_SLOT_LENGTH);
        }

        Debug.println("CardRegion.dumpCardTable: done");
    }

    private boolean isCardMarked(Address addr) {
        return addr.asPointer().getByte() == DIRTY;
    }

    public final boolean isCardMarked(int index) {
        return isCardMarked(cardTableBase().plus(index * CARD_SLOT_LENGTH));
    }

    public static Pointer adjustedCardTableBase(Pointer auxiliarySpace) {
        return auxiliarySpace.minus(Heap.bootHeapRegion().start().unsignedShiftedRight(CardRegion.CARD_SHIFT));
    }

    private static class SetLocals implements Procedure<VmThread> {
        @Override
        public void run(VmThread thread) {
            ADJUSTED_CARDTABLE_BASE.setConstantWord(thread.vmThreadLocals(), _adjustedCardTable);
        }
    }

    private static SetLocals _setLocals = new SetLocals();
    /**
     * Copies cards from auxiliary card table ( used for marking updates before the regular card table is created) to
     * regular card table and sets regular card table's adjusted address in the vm thread locals.
     *
     * @param regularCardTable
     */
    public static void switchToRegularCardTable(Pointer regularCardTable) {
        // copy cards from primordial card table to the newly created cardtable
        final Pointer primordialCardTable = VmThreadLocal.ADJUSTED_CARDTABLE_BASE.getConstantWord(MaxineVM.primordialVmThreadLocals()).asPointer().plus(
                        Heap.bootHeapRegion().start().unsignedShiftedRight(CardRegion.CARD_SHIFT));
        final int primordialCardTableSize = Heap.bootHeapRegion().size().plus(Code.bootCodeRegion().size()).unsignedShiftedRight(CARD_SHIFT).toInt();


        _adjustedCardTable = CardRegion.adjustedCardTableBase(regularCardTable);

        if (Heap.verbose()) {
            Debug.print("switchToRegularCardTable: primordialCardTable address ");
            Debug.println(primordialCardTable);
            Debug.print("switchToRegularCardTable: primordialCardTable size ");
            Debug.println(primordialCardTableSize);
            Debug.print("switchToRegularCardTable: regular card table address ");
            Debug.println(regularCardTable);
            Debug.print("switchToRegularCardTable: regular adjusted card table address ");
            Debug.println(_adjustedCardTable);
        }

        for (int index = 0; index < primordialCardTableSize; index++) {
            // copy the primordial card table to the regular card table
            regularCardTable.writeByte(index, primordialCardTable.readByte(index));
        }
        VmThreadMap.ACTIVE.forAllVmThreads(null, _setLocals);
    }

    public final Size cardTableSize() {
        return _cardTableSize;
    }
}
