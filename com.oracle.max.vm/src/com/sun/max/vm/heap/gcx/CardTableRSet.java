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
package com.sun.max.vm.heap.gcx;

import java.util.*;

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
    static boolean TraceCardTableRSet = false;
    static {
        VMOptions.addFieldOption("-XX:", "TraceCardTableRSet", CardTableRSet.class, "Enables CardTableRSet Debugging Traces", Phase.PRISTINE);
    }
    /**
     * Contiguous regions of virtual memory holding the card table data.
     * Mostly used to feed the inspector.
     */
    final MemoryRegion cardTableMemory;

    final CardTable cardTable;
    final CardFirstObjectTable cfoTable;

    /**
     * CiConstant holding the card table's biased address in boot code region and used for all XirSnippets implementing the write barrier.
     * Biased card table address XirConstant are initialized with this CiConstant which holds a  WrappedWord object with a dummy address.
     * The WrappedWord object can be used during the serializing phase of boot image generation  to identify easily the literal locations
     * in the boot code region that hold the biased card table address. A table of these location is added in the boot image and used at
     * VM startup to patch them with the actual biased card table address once this one is known.
     * See {@link #recordLiteralsLocations()}
     * See {@link #patchBootCodeLiterals()}
     */
    @HOSTED_ONLY
    private CiConstant biasedCardTableAddressCiConstant;

    @HOSTED_ONLY
    private List<XirConstant> xirConstants = new ArrayList<XirConstant>(16);

    /**
     * List of XIR constants representing the biased card table address.
     * The list is used at startup to initialize the "startup-time" constant value.
     */
    private XirBiasedCardTableConstant [] biasedCardTableAddressXirConstants = new XirBiasedCardTableConstant[0];

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
                assert biasedCardTableAddressCiConstant != null;
                literalRecorder = new ReferenceLiteralLocationRecorder(Code.bootCodeRegion(), biasedCardTableAddressCiConstant.asObject());
                bootCardTableLiterals = literalRecorder.getLiteralLocations();
                biasedCardTableAddressXirConstants = xirConstants.toArray(biasedCardTableAddressXirConstants);
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

    static class XirBiasedCardTableConstant extends CiXirAssembler.XirConstant {
        XirBiasedCardTableConstant(CiXirAssembler asm, CiConstant value) {
            super(asm, "Card Table biased-address", value);
            asm.recordConstant(this);
        }

        void setStartupValue(CiConstant startupValue) {
            value = startupValue;
        }
    }

    public void initializeXirStartupConstants() {
        final CiConstant biasedCardTableCiConstant = CiConstant.forLong(cardTable.biasedTableAddress.toLong());
        for (XirBiasedCardTableConstant c : biasedCardTableAddressXirConstants) {
            c.setStartupValue(biasedCardTableCiConstant);
        }
    }

    @HOSTED_ONLY
    private XirConstant biasedCardTableAddressXirConstant(CiXirAssembler asm) {
        if (biasedCardTableAddressCiConstant == null) {
            biasedCardTableAddressCiConstant = WordUtil.wrappedConstant(Address.fromLong(123456789L));
        }
        XirConstant constant = new XirBiasedCardTableConstant(asm, biasedCardTableAddressCiConstant);
        xirConstants.add(constant);
        return constant;
    }

    @HOSTED_ONLY
    public void genTuplePostWriteBarrier(CiXirAssembler asm, XirOperand tupleCell) {
        final XirOperand temp = asm.createTemp("temp", WordUtil.archKind());
        asm.shr(temp, tupleCell, asm.i(CardTable.LOG2_CARD_SIZE));
        // Watch out: this create a reference literal that will not point to an object!
        // The GC will need to carefully skip reference table entries holding the biased base of the card table.
        // final XirConstant biasedCardTableAddress = asm.createConstant(CiConstant.forObject(dummyCardTable));
        final XirConstant biasedCardTableAddress = biasedCardTableAddressXirConstant(asm);
        asm.pstore(CiKind.Byte, biasedCardTableAddress, temp, asm.i(CardState.DIRTY_CARD.value()), false);

        // FIXME: remove this temp debug code
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
        // final XirConstant biasedCardTableAddress = asm.createConstant(CiConstant.forObject(dummyCardTable));
        final XirConstant biasedCardTableAddress = biasedCardTableAddressXirConstant(asm);
        asm.pstore(CiKind.Byte, biasedCardTableAddress, temp, asm.i(CardState.DIRTY_CARD.value()), false);
    }

    /**
     * Record update to a reference slot of a cell.
     * @param ref the cell whose reference is updated
     * @param offset the offset from the origin of the cell to the updated reference.
     */
    public void record(Reference ref, Offset offset) {
        cardTable.dirtyCovered(ref.toOrigin().plus(offset));
    }

    /**
     * Record update to a reference slot of a cell.
     * @param ref the cell whose reference is updated
     * @param displacement a displacement from the origin of the cell
     * @param index a word index to the updated reference
     */
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
            int endCardIndex = cardTable.firstNot(startCardIndex + 1, endOfRange, CardState.DIRTY_CARD);
            cardTable.clean(startCardIndex, endCardIndex);
            visitCards(startCardIndex, endCardIndex, cellVisitor);
            if (++endCardIndex >= endOfRange) {
                return;
            }
            startCardIndex = cardTable.first(endCardIndex, endOfRange, CardState.DIRTY_CARD);
        }
    }

    public void visitCards(Address start, Address end, CardState cardState, OverlappingCellVisitor cellVisitor) {
        final int endOfRange = cardTable.tableEntryIndex(end);
        int startCardIndex = cardTable.first(cardTable.tableEntryIndex(start), endOfRange, cardState);
        while (startCardIndex < endOfRange) {
            int endCardIndex = cardTable.firstNot(startCardIndex + 1, endOfRange, cardState);
            if (MaxineVM.isDebug() && TraceCardTableRSet) {
                Log.print("Visiting ");
                Log.print(cardState.name());
                Log.print(" cards [");
                Log.print(startCardIndex);
                Log.print(", ");
                Log.print(endCardIndex);
                Log.print("]  (");
                Log.print(cardTable.rangeStart(startCardIndex));
                Log.print(", ");
                Log.print(cardTable.rangeStart(endCardIndex));
                Log.print(")  R = ");
                Log.print(RegionTable.theRegionTable().regionID(cardTable.rangeStart(startCardIndex)));
                Log.println(")");
            }
            visitCards(startCardIndex, endCardIndex, cellVisitor);
            if (++endCardIndex >= endOfRange) {
                return;
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

    /**
     * Update the remembered set to take into account the newly freed space.
     * @param start address to the first word of the freed chunk
     * @param size size of the freed chunk
     */
    public void updateForFreeSpace(Address start, Size size) {
        final Address end = start.plus(size);
        cfoTable.set(start, end);
        cardTable.setCardsInRange(start, end, CardState.DIRTY_CARD);
    }
}
