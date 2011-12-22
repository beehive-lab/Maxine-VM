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

import com.sun.cri.ci.CiAddress.Scale;
import com.sun.cri.ci.*;
import com.sun.cri.xir.*;
import com.sun.cri.xir.CiXirAssembler.XirConstant;
import com.sun.cri.xir.CiXirAssembler.XirOperand;
import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.CardTable.CardState;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
/**
 * A pure card-table based remembered set.
 */
public class CardTableRSet implements HeapManagementMemoryRequirement {
    /**
     * Contiguous regions of virtual memory holding the card table data.
     * Mostly used to feed the inspector.
     */
    final MemoryRegion cardTableMemory;

    final CardTable cardTable;
    final CardFirstObjectTable cfoTable;

    /**
     * Dummy object used to find card table literal constants in boot code region.
     * The biased card table address is initialize with the origin of this object, so that
     * during the serializing phase of boot image generation we can identify easily the location
     * in the boot code region holding it.
     * See {@link #recordLiteralsLocations()}
     * See {@link #patchBootCodeLiterals()}
     */
    @HOSTED_ONLY
    byte [] dummyCardTable = new byte[0];

    @HOSTED_ONLY
    ReferenceLiteralLocationRecorder literalRecorder;

    /**
     * Table holding all the locations in the boot code region of reference literals to the biased card table.
     * This allows to patch these literals with the actual correct value of the biased card table, known only at
     * heap scheme pristine initialization.
     * The index are word indexes relative to the start of the boot code region.
     */
    private int [] bootCardTableLiterals;

    private void patchBootCodeLiterals() {
        final Pointer base = Code.bootCodeRegion().start().asPointer();
        final Address biasedTableAddress = cardTable.biasedTableAddress;
        for (int literalPos : bootCardTableLiterals) {
            base.setWord(literalPos, biasedTableAddress);
        }
    }

    public CardTableRSet() {
        cardTable = new CardTable();
        cfoTable = new CardFirstObjectTable();
        cardTableMemory = new MemoryRegion("Card and FOT tables");
    }

    public void initialize(MaxineVM.Phase phase) {
        if (MaxineVM.isHosted()) {
            if (phase == Phase.BOOTSTRAPPING) {
                Log2RegionToByteMapTable.hostInitialize();
            } else if (phase == Phase.SERIALIZING_IMAGE) {
                // Build a table of indexes to reference literals that point to the card table.
                literalRecorder = new ReferenceLiteralLocationRecorder(Code.bootCodeRegion(), dummyCardTable);
                bootCardTableLiterals = literalRecorder.getLiteralLocations();
            } else if (phase == MaxineVM.Phase.WRITING_IMAGE) {
                literalRecorder.fillLiteralLocations();
            }
        } else if (phase == MaxineVM.Phase.PRIMORDIAL) {
            final Size reservedSpace = Size.K.times(VMConfiguration.vmConfig().heapScheme().reservedVirtualSpaceKB());
            final Size bootCardTableSize = memoryRequirement(Heap.bootHeapRegion.size()).roundedUpBy(Platform.platform().pageSize);
            final Address bootCardTableStart = Heap.bootHeapRegion.start().plus(reservedSpace).minus(bootCardTableSize);
            initialize(Heap.bootHeapRegion.start(), Heap.bootHeapRegion.size(), bootCardTableStart, bootCardTableSize);
        }
    }

    public void initialize(Address coveredAreaStart, Size coveredAreaSize, Address cardTableDataStart, Size cardTableDataSize) {
        cardTableMemory.setStart(cardTableDataStart);
        cardTableMemory.setSize(cardTableDataSize);
        cardTable.initialize(coveredAreaStart, coveredAreaSize, cardTableDataStart);
        final Address cfoTableStart = cardTableDataStart.plus(cardTable.tableSize(coveredAreaSize).wordAligned());
        cfoTable.initialize(coveredAreaStart, coveredAreaSize, cfoTableStart);
        if (bootCardTableLiterals != null) {
            patchBootCodeLiterals();
        }
    }

    @HOSTED_ONLY
    public void genTuplePostWriteBarrier(CiXirAssembler asm, XirOperand tupleCell) {
        final XirOperand temp = asm.createTemp("temp", WordUtil.archKind());
        asm.shr(temp, tupleCell, asm.i(CardTable.LOG2_CARD_SIZE));
        // Watch out: this create a reference literal that will not point to an object!
        // The GC will need to carefully skip reference table entries holding the biased base of the card table.
        final XirConstant biasedCardTableAddress = asm.createConstant(CiConstant.forObject(dummyCardTable));
        asm.pstore(CiKind.Byte, biasedCardTableAddress, temp, asm.i(CardState.DIRTY_CARD.value()), false);
        if (MaxineVM.isDebug()) {
            // Just so that we get the address of the card entry in a register when inspecting...
            asm.lea(temp, biasedCardTableAddress, temp, 0, Scale.Times1);
        }
    }

    @HOSTED_ONLY
    public void genArrayPostWriteBarrier(CiXirAssembler asm, XirOperand arrayCell, XirOperand elemIndex) {
        final XirOperand temp = asm.createTemp("temp", WordUtil.archKind());
        final Scale scale = Scale.fromInt(Word.size());
        final int disp = Layout.referenceArrayLayout().getElementOffsetInCell(0).toInt();
        asm.lea(temp, arrayCell, elemIndex, disp, scale);
        asm.shr(temp, temp, asm.i(CardTable.LOG2_CARD_SIZE));
        final XirConstant biasedCardTableAddress = asm.createConstant(CiConstant.forObject(dummyCardTable));
        asm.pstore(CiKind.Byte, biasedCardTableAddress, temp, asm.i(CardState.DIRTY_CARD.value()), false);
    }

    public void record(Reference ref, Offset offset) {
        cardTable.dirtyCovered(ref.toOrigin().plus(offset));
    }

    public void record(Reference ref,  int displacement, int index) {
        cardTable.dirtyCovered(ref.toOrigin().plus(Address.fromInt(index).shiftedLeft(Word.widthValue().log2numberOfBytes).plus(displacement)));
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
            int endCardIndex = cardTable.firstNot(++startCardIndex, endOfRange, CardState.DIRTY_CARD);
            if (endCardIndex == CardTable.NO_CARD_INDEX) {
                // All cards in the range are dirty.
                endCardIndex = endOfRange;
            }
            cardTable.clean(startCardIndex, endCardIndex);
            visitCards(startCardIndex, endCardIndex, cellVisitor);
            if (++endCardIndex >= endOfRange) {
                break;
            }
            startCardIndex = cardTable.first(endCardIndex, endOfRange, CardState.DIRTY_CARD);
        }
    }

    public void visitCards(Address start, Address end, CardState cardState, OverlappingCellVisitor cellVisitor) {
        final int endOfRange = cardTable.tableEntryIndex(end);
        int startCardIndex = cardTable.first(cardTable.tableEntryIndex(start), endOfRange, cardState);
        while (startCardIndex < endOfRange) {
            int endCardIndex = cardTable.firstNot(++startCardIndex, endOfRange, cardState);
            if (endCardIndex == CardTable.NO_CARD_INDEX) {
                endCardIndex = endOfRange;
            }
            visitCards(startCardIndex, endCardIndex, cellVisitor);
            if (++endCardIndex >= endOfRange) {
                break;
            }
            startCardIndex = cardTable.first(endCardIndex, endOfRange, cardState);
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
