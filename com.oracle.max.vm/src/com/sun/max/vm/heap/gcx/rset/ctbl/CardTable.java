/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx.rset.ctbl;

import static com.sun.max.vm.heap.gcx.rset.ctbl.CardState.*;

import com.sun.max.unsafe.*;
/**
 * A simple, two-valued card table.
 *
 */
class CardTable extends  Log2RegionToByteMapTable {
    CardTable() {
        super(CardTableRSet.LOG2_CARD_SIZE);
    }

    @Override
    void initialize(Address coveredAreaStart, Size coveredAreaSize, Address storageArea) {
        super.initialize(coveredAreaStart, coveredAreaSize, storageArea);
        cleanAll();
    }

    void initialize(Address coveredAreaStart, Size coveredAreaSize) {
        super.initialize(coveredAreaStart, coveredAreaSize, true);
        cleanAll();
    }

    /**
     * Set all cards of the table to the {@link CardState#CLEAN_CARD} value.
     */
    void cleanAll() {
        fill(CLEAN_CARD.value());
    }

    /**
     * Set all cards in the specified index range to the {@link CardState#CLEAN_CARD} value.
     * The range extends from index fromIndex, inclusive, to index toIndex, exclusive.
     * @param fromIndex index to the first card of the range (inclusive)
     * @param toIndex index to the last card of the range (exclusive)
     */
    void clean(int fromIndex, int toIndex) {
        fill(fromIndex, toIndex, CLEAN_CARD.value());
    }

    /**
     * Set all cards in the specified index range to the {@link CardState#DIRTY_CARD} value.
      * The range extends from index fromIndex, inclusive, to index toIndex, exclusive.
    * @param fromIndex index to the first card of the range (inclusive)
     * @param toIndex index to the last card of the range (exclusive)
     */
    void dirty(int fromIndex, int toIndex) {
        fill(fromIndex, toIndex, DIRTY_CARD.value());
    }

    /**
     * Set card at specified index to the {@link CardState#CLEAN_CARD} value.
     * @param index a card index
     */
    void clean(int index) {
        set(index, CLEAN_CARD.value());
    }

   /**
    * Set card at specified index to the {@link CardState#DIRTY_CARD} value.
    * @param index a card index
    */
    void dirty(int index) {
        set(index, DIRTY_CARD.value());
    }

    /**
     * Dirty the entry in the card table corresponding to the card of the covered heap address.
     * @param coveredAddress an address in heap covered by the card table
     */
    void dirtyCovered(Address coveredAddress) {
        unsafeSet(coveredAddress, DIRTY_CARD.value());
    }

    /**
     * Find the first card set to the specified card state in the specified range of entries in the table .
     * @param start index of the first card in the range (inclusive)
     * @param end index of the last card of the range (exclusive)
     * @param cardState a card state
    * @return the index to the first card in the specified state, or the end index if none of the cards in the range are set to that state.
    */
    final int first(int start, int end, CardState cardState) {
        // This may be optimized with special support from the compiler to exploit cpu-specific instruction for string ops (e.g.).
        // We may also get rid of the limit test by making the end of the range looking like a marked card.
        // e.g.:   tmp = limit.getByte(); limit.setByte(1);  loop; limit.setByte(tmp); This could be factor over multiple call of firstNonZero...
        final Pointer first = tableAddress.plus(start);
        final Pointer limit = tableAddress.plus(end);
        final byte cardValue = cardState.value;
        Pointer cursor = first;
        while (cursor.getByte() != cardValue) {
            cursor = cursor.plus(1);
            if (cursor.greaterEqual(limit)) {
                return end;
            }
        }
        return cursor.minus(tableAddress).toInt();
    }


    /**
     * Find the first card not set to the specified card state in the specified range of entries in the table .
     * @param start index of the first card in the range (inclusive)
     * @param end index of the last card of the range (exclusive)
     * @param cardState a card state
    * @return the index to the first card in a state different than the specified state, or the end index if  all the cards in the range have that state.
    */
    final int firstNot(int start, int end, CardState cardState) {
        // This may be optimized with special support from the compiler to exploit cpu-specific instruction for string ops (e.g.).
        // We may also get rid of the limit test by making the end of the range looking like a marked card.
        // e.g.:   tmp = limit.getByte(); limit.setByte(1);  loop; limit.setByte(tmp); This could be factor over multiple call of firstNonZero...
        final Pointer first = tableAddress.plus(start);
        final Pointer limit = tableAddress.plus(end);
        final byte cardValue = cardState.value;
        Pointer cursor = first;
        while (cursor.getByte() == cardValue) {
            cursor = cursor.plus(1);
            if (cursor.greaterEqual(limit)) {
                return end;
            }
        }
        return cursor.minus(tableAddress).toInt();
    }


 /**
     * Set all cards completely covered by the specified range to the specified card state.
     * @param start
     * @param end
     * @param cardState
     */
    void setCardsInRange(Address start, Address end, CardState cardState) {
        Address firstCard = CardTableRSet.alignUpToCard(start.minus(Word.size()));
        Address lastCard = CardTableRSet.alignDownToCard(end);
        if (lastCard.greaterThan(firstCard)) {
            final byte cardValue = cardState.value;
            do {
                unsafeSet(firstCard, cardValue);
                firstCard = firstCard.plus(CardTableRSet.CARD_SIZE);
            } while(lastCard.greaterThan(firstCard));
        }
    }
}
