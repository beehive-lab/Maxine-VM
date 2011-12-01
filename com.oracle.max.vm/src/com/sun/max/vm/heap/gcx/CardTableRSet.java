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

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiAddress.*;
import com.sun.cri.xir.*;
import com.sun.cri.xir.CiXirAssembler.XirConstant;
import com.sun.cri.xir.CiXirAssembler.XirOperand;
import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.CardTable.CardState;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.amd64.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.thread.VmThreadLocal.Nature;
/**
 * A pure card-table based remembered set.
 */
public class CardTableRSet implements HeapManagementMemoryRequirement {
    public static final String CACHED_BIASED_CARD_TABLE_THREAD_LOCAL_NAME = "CACHED_BIASED_CARD_TABLE";
    public static final VmThreadLocal CACHED_BIASED_CARD_TABLE = new VmThreadLocal(CACHED_BIASED_CARD_TABLE_THREAD_LOCAL_NAME, false, "CardTableRSet: biased based of card table", Nature.Triple) {
        @Override
        public void initialize() {
            HeapScheme heapScheme = VMConfiguration.vmConfig().heapScheme();
            if (heapScheme instanceof ThreadLocalRSetInitializer) {
                ((ThreadLocalRSetInitializer) heapScheme).initializeRSetThreadLocals(this);
            }
        }
    };

    /**
     * Contiguous regions of virtual memory holding the card table data.
     * Mostly used to feed the inspector.
     */
    final MemoryRegion cardTableMemory;

    final CardTable cardTable;
    final CardFirstObjectTable cfoTable;

    @HOSTED_ONLY
    public void genTuplePostWriteBarrier(CiXirAssembler asm, XirOperand tupleCell) {
        final XirOperand temp = asm.createTemp("temp", WordUtil.archKind());
        asm.shr(temp, tupleCell, asm.i(CardTable.LOG2_CARD_SIZE));
        if (MaxineVM.isHosted()) {
            final XirConstant offsetToCardTableCache = asm.i(CACHED_BIASED_CARD_TABLE.offset);
            final XirOperand tla = asm.createRegisterTemp("TLA", WordUtil.archKind(), AMD64SafepointPoll.LATCH_REGISTER);
            final XirOperand etla = asm.createTemp("ETLA", WordUtil.archKind());
            final XirOperand cachedCardTable = asm.createTemp("ETLA", WordUtil.archKind());
            asm.pload(WordUtil.archKind(), etla, tla, asm.i(VmThreadLocal.ETLA.offset), false);
            asm.pload(WordUtil.archKind(), cachedCardTable, etla, offsetToCardTableCache, false);
            asm.pstore(CiKind.Byte, cachedCardTable, temp, asm.i(CardState.DIRTY_CARD.value()), false);
        } else {
            final XirConstant biasedCardTableAddress = asm.createConstant(CiConstant.forObject(cardTable.biasedTableAddress));
            asm.pstore(CiKind.Byte, biasedCardTableAddress, temp, asm.i(CardState.DIRTY_CARD.value()), false);
        }
    }

    @HOSTED_ONLY
    public void genArrayPostWriteBarrier(CiXirAssembler asm, XirOperand arrayCell, XirOperand elemIndex) {
        final XirOperand temp = asm.createTemp("temp", WordUtil.archKind());
        final Scale scale = Scale.fromInt(Word.size());
        final int disp = Layout.referenceArrayLayout().getElementOffsetInCell(0).toInt();
        asm.lea(temp, arrayCell, elemIndex, disp, scale);
        asm.shr(temp, temp, asm.i(CardTable.LOG2_CARD_SIZE));
        if (MaxineVM.isHosted()) {
            final XirConstant offsetToCardTableCache = asm.i(CACHED_BIASED_CARD_TABLE.offset);
            final XirOperand tla = asm.createRegisterTemp("TLA", WordUtil.archKind(), AMD64SafepointPoll.LATCH_REGISTER);
            final XirOperand etla = asm.createTemp("ETLA", WordUtil.archKind());
            final XirOperand cachedCardTable = asm.createTemp("ETLA", WordUtil.archKind());
            asm.pload(WordUtil.archKind(), etla, tla, asm.i(VmThreadLocal.ETLA.offset), false);
            asm.pload(WordUtil.archKind(), cachedCardTable, etla, offsetToCardTableCache, false);
            asm.pstore(CiKind.Byte, cachedCardTable, temp, asm.i(CardState.DIRTY_CARD.value()), false);
        } else {
            final XirConstant biasedCardTableAddress = asm.createConstant(CiConstant.forObject(cardTable.biasedTableAddress));
            asm.pstore(CiKind.Byte, biasedCardTableAddress, temp, asm.i(CardState.DIRTY_CARD.value()), false);
        }
    }



    public CardTableRSet() {
        cardTable = new CardTable();
        cfoTable = new CardFirstObjectTable();
        cardTableMemory = new MemoryRegion("Card and FOT tables");
    }

    public void initializeThreadLocals(VmThreadLocal threadLocal) {
        assert threadLocal == CACHED_BIASED_CARD_TABLE;
        threadLocal.store3(cardTable.biasedTableAddress);
    }

    public void initialize(Address coveredAreaStart, Size coveredAreaSize, Address cardTableDataStart, Size cardTableDataSize) {
        cardTableMemory.setStart(cardTableDataStart);
        cardTableMemory.setSize(cardTableDataSize);
        cardTable.initialize(coveredAreaStart, coveredAreaSize, cardTableDataStart);
        final Address cfoTableStart = cardTableDataStart.plus(cardTable.tableSize(coveredAreaSize).wordAligned());
        cfoTable.initialize(coveredAreaStart, coveredAreaSize, cfoTableStart);
    }

    /**
     * Visit the cells that overlap a card.
     *
     * @param cardIndex index of the card
     * @param cellVisitor the logic to apply to the visited cell
     */
    private void visitCard(int cardIndex, OverlappingCellVisitor cellVisitor) {
        visitCards(cardIndex, cardIndex + 1, cellVisitor);
    }

    /**
     * Visit the cells that overlap a contiguous range of cards.
     *
     * @param cardIndex index of the card
     * @param cellVisitor the logic to apply to the visited cell
     */
    private void visitCards(int startCardIndex, int endCardIndex, OverlappingCellVisitor cellVisitor) {
        final Address start = cardTable.rangeStart(startCardIndex);
        final Address end = cardTable.rangeStart(endCardIndex);
        Pointer cell = cfoTable.cellStart(startCardIndex).asPointer();
        do {
            cell = cellVisitor.visitCell(cell, start, end);
        } while (cell.lessThan(end));
    }

    /**
     * Iterate over cells that overlap the specified region and comprises recorded reference locations.
     * @param start
     * @param end
     * @param cellVisitor
     */
    void cleanAndVisitCards(Address start, Address end, OverlappingCellVisitor cellVisitor) {
        final int endOfRange = cardTable.tableEntryIndex(end);
        int startCardIndex = cardTable.first(cardTable.tableEntryIndex(start), endOfRange, CardState.DIRTY_CARD);
        while (startCardIndex < endOfRange) {
            int endCardIndex = cardTable.first(++startCardIndex, endOfRange, CardState.CLEAN_CARD);
            cardTable.clean(startCardIndex, endCardIndex);
            visitCards(startCardIndex, endCardIndex, cellVisitor);
            if (++endCardIndex >= endOfRange) {
                break;
            }
            startCardIndex = cardTable.first(endCardIndex, endOfRange, CardState.DIRTY_CARD);
        }
    }


    @Override
    public Size memoryRequirement(Size maxCoveredAreaSize) {
        return cardTable.tableSize(maxCoveredAreaSize).plus(cfoTable.tableSize(maxCoveredAreaSize));
    }

    /**
     * Contiguous region of memory used by the remembered set.
     * @return a non-null {@link MemoryRegion}
     */
    public MemoryRegion memory() {
        return cardTableMemory;
    }

}
