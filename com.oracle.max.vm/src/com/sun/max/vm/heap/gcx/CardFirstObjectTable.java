/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx;

import com.sun.max.unsafe.*;
import static com.sun.max.vm.heap.gcx.CardTable.*;

/**
 * Table providing support for  linear scanning of arbitrary cards of a contiguous range of virtual addresses associated with a card table.
 *
 * Walking arbitrary cards requires that the contiguous range of virtual address is formatted to allow walking it linearly.
 * It also requires that the header of any object overlapping with the first word of any card
 * can be quickly retrieved (in order to figure out the layout of the object, its size, and its references locations).
 * The CardFirstObjectTable encodes per-card information allowing to find the
 * address of the object overlapping with the first word of a card.
 * Because the table only has 1 byte per entry, objects that overlaps many card will have load multiple entries to find the amount of space
 * to remove to the start of a card to find the start of the object that overlaps with the first word of the card.
 * The value stored in an the entry E of {@link CardFirstObjectTable} is either a negative number of words to add to the address of the first word of card E,
 * or a power of 2 of a number of card to subtract to the index of card E.
 *
 * Let C be a card whose first word overlaps with an object that starts at address s, such that s is in card c0.
 * Let d be the distance in number of cards between c0 and C.
 * The value stored in the entry of {@link CardFirstObjectTable}
 * is the negative number of words from the end of card C to the start of the object s if d = 1, otherwise, it is a positive k, such that
 * 2^k < d < 2^(k+1).
 *
 *  For example, let assume that s points to a 270 Kb object, and O[d] is the entry of {@link CardFirstObjectTable} for the card C
 * at distance d from c0.
 * If d = 1, O[d] = number of words from the end of c0 to s, and is a value between 0 and -127
 * if d > 1,  O[d] = k, the power of 2 of the number of cards to subtract to C's index to retrieve further information related to where s is.
 *
 *
 * A heap implementation using a card table is responsible for updating the information tracked by the table.
 * Update of the information simply requires calling {@link CardFirstObjectTable#set(Address, Size)} with the
 * address and size of an object crossing a card boundary.
 *
 * A heap component is responsible for updating the table with information.
 *
 *
 *
 */
public class CardFirstObjectTable extends Power2RegionToByteMapTable {
    static final int LOG2_NUM_WORD_PER_CARD = LOG2_CARD_SIZE - Word.widthValue().log2numberOfBytes;
    static final int NUM_WORD_PER_CARD = 1 << LOG2_NUM_WORD_PER_CARD;
    static final byte ZERO = 0;

    CardFirstObjectTable() {
        super(LOG2_CARD_SIZE);
    }

    /**
     * Update the table information to record the position of the start of the specified objects for the cards that this objects overlap.
     * If the object fit within a single card, no update take place.
     *
     * @param cell address of an object
     * @param size size of the object
     */
    void set(Address cell, Size size) {
        // ADD CHECK TO VERIFY LIMIT ON SIZE.
        int firstCard = tableEntryIndex(cell);
        int lastCard = tableEntryIndex(cell.plus(size));
        int numCards = lastCard  - firstCard;
        if (numCards == 0) {
            if (atBoundary(cell)) {
                set(firstCard, (byte) 0);
                return;
            }
            // Otherwise, the cell doesn't cross a card boundary, so nothing to update.
        }
        int nextCard = firstCard + 1;
        final byte numWords = (byte) rangeStart(nextCard).minus(cell).unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
        set(nextCard, numWords);

        int k = 1;
        int distance = 1 << k;
        while (distance <= numCards) {

        }
        while (numCards > 1) {
            byte log2 =  (byte) Address.fromInt(numCards).mostSignificantBitSet();
            int prevCard = lastCard - (1 << (log2 -1));
            while (lastCard > prevCard) {
                set(lastCard--, log2);
            }
            numCards = lastCard - firstCard;
        }
    }

    Address cellStart(int cardIndex) {
        int nextCardIndex = cardIndex;
        byte startInfo = get(nextCardIndex);
        while (startInfo > ZERO) {
            int distanceToNextCard = 1 << startInfo;
            nextCardIndex -= distanceToNextCard;
            startInfo = get(nextCardIndex);
        }
        int offset = startInfo << Word.widthValue().log2numberOfBytes;
        return rangeStart(cardIndex).minus(offset);
    }
}
