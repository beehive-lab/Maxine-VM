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

import static com.sun.max.vm.heap.HeapSchemeAdaptor.*;

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
import com.sun.max.vm.heap.HeapScheme.BootRegionMappingConstraint;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.heap.gcx.rset.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
/**
 * Card-table based remembered set.
 */
public final class CardTableRSet extends DeadSpaceListener implements HeapManagementMemoryRequirement {

    /**
     * Log2 of a card size in bytes.
     */
    static public final int LOG2_CARD_SIZE = 9;

    /**
     * Number of bytes per card.
     */
    static final int CARD_SIZE = 1 << LOG2_CARD_SIZE;

    static final int LOG2_NUM_WORDS_PER_CARD = LOG2_CARD_SIZE - Word.widthValue().log2numberOfBytes;

    static final int NUM_WORDS_PER_CARD = 1 << LOG2_NUM_WORDS_PER_CARD;

    static public final  int NO_CARD_INDEX = -1;

    static final Address CARD_ADDRESS_MASK = Address.fromInt(CARD_SIZE - 1).not();

    private static boolean TraceCardTableRSet = false;

    static {
        if (MaxineVM.isDebug()) {
            VMOptions.addFieldOption("-XX:", "TraceCardTableRSet", CardTableRSet.class, "Enables CardTableRSet Debugging Traces", Phase.PRISTINE);
        }
    }

    @INLINE
    public static boolean traceCardTableRSet() {
        return MaxineVM.isDebug() && TraceCardTableRSet;
    }

    public static void setTraceCardTableRSet(boolean flag) {
        TraceCardTableRSet = flag;
    }

    /**
     * Contiguous regions of virtual memory holding the remembered set data (both the card table and the FOT).
     * Mostly used to feed the inspector.
     */
    @INSPECTED
    final MemoryRegion tablesMemory;

    /**
     * The table recording card state. The table is updated by compiler-generated write-barrier execution and explicitly by the GC.
     */
    @INSPECTED
    public final CardTable cardTable;

    /**
     * The table recording the first object overlapping with every card.
     * The table is updated by allocators and free space reclamation.
     */
    @INSPECTED
    public final CardFirstObjectTable cfoTable;

    /**
     * CiConstant holding the card table's biased address in boot code region and used for all XirSnippets implementing the write barrier.
     * Biased card table address XirConstant are initialized with this CiConstant which holds a  WrappedWord object with a dummy address.
     * The WrappedWord object can be used during the serializing phase of boot image generation  to identify easily the literal locations
     * in the boot code region that hold the biased card table address. A table of these location is added in the boot image and used at
     * VM startup to patch them with the actual biased card table address once this one is known.
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
        tablesMemory = new MemoryRegion("Card and FOT tables");
    }

    /**
     * Initialization of the card-table remembered set according to VM initialization phase.
     *
     * @param phase the initializing phase the VM is initializing for
     */
    public void initialize(MaxineVM.Phase phase) {
        if (MaxineVM.isHosted()) {
            if (phase == Phase.SERIALIZING_IMAGE) {
                // Build a table of indexes to reference literals that point to the card table.
                assert biasedCardTableAddressCiConstant != null;
                literalRecorder = new ReferenceLiteralLocationRecorder(Code.bootCodeRegion(), biasedCardTableAddressCiConstant.asObject());
                bootCardTableLiterals = literalRecorder.getLiteralLocations();
                biasedCardTableAddressXirConstants = xirConstants.toArray(biasedCardTableAddressXirConstants);
            } else if (phase == MaxineVM.Phase.WRITING_IMAGE) {
                literalRecorder.fillLiteralLocations();
            }
        } else if (phase == MaxineVM.Phase.PRIMORDIAL) {
            // We need to initialize the card table to cover the boot region before hitting any write barriers.
            // If heap size is specified by the end user, it can only be known at PRISTINE time. We need to initialize the card table to a valid memory address
            // before that as some write barrier may be exercised before the heap is even allocated (e.g., to initialize a  VMOption in the boot region for instance).
            // To this end, we initialize at PRIMORDIAL time the card table with enough memory to cover the boot region.
            // This card table is temporary and will be replaced with the table covering the heap once this one is allocated (typically in PRISTINE initialization).

            // FIXME: this for now assume that the heap scheme using the card table has (i) reserved enough virtual space to hold the boot region and the
            // temporary card table, and (ii), has mapped the boot region at the start of this reserved space.
            // The following relies on these assumption to use the end of the reserved virtual space for the temporary card table.
            final Size reservedSpace = Size.K.times(VMConfiguration.vmConfig().heapScheme().reservedVirtualSpaceKB());
            final Size bootCardTableSize = memoryRequirement(Heap.bootHeapRegion.size()).roundedUpBy(Platform.platform().pageSize);
            final Address bootCardTableStart = Heap.bootHeapRegion.start().plus(reservedSpace).minus(bootCardTableSize);
            FatalError.check(reservedSpace.greaterThan(bootCardTableSize.plus(Heap.bootHeapRegion.size())) &&
                            VMConfiguration.vmConfig().heapScheme().bootRegionMappingConstraint().equals(BootRegionMappingConstraint.AT_START),
                "card table remembered set initialization invariant violated");
            initialize(Heap.bootHeapRegion.start(), Heap.bootHeapRegion.size(), bootCardTableStart, bootCardTableSize);
        }
    }

    /**
     * Initialize a card table based remembered set to cover a contiguous range of virtual addresses.
     * The memory for the remembered set tables (card and FOT tables) must be provided by the caller as the
     * caller may want to enforce some constraints on the location of these tables in memory with respect to the covered heap addresses.
     * The amount of memory needed by remembered set's tables for a given area to cover is computed using  {@link #memoryRequirement(Size)}.
     *
     * @param coveredAreaStart start of the contiguous range of heap covered by the remembered set
     * @param coveredAreaSize end of the contiguous range of heap covered by the remembered set
     * @param tablesDataStart start of the memory regions reserved for the remembered set's tables
     * @param tablesDataSize end of the memory regions reserved for the remembered set's tables
     */
    public void initialize(Address coveredAreaStart, Size coveredAreaSize, Address tablesDataStart, Size tablesDataSize) {
        tablesMemory.setStart(tablesDataStart);
        tablesMemory.setSize(tablesDataSize);
        cardTable.initialize(coveredAreaStart, coveredAreaSize, tablesDataStart);
        final Address cfoTableStart = tablesDataStart.plus(cardTable.tableSize(coveredAreaSize).wordAligned());
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

    /**
     * Set the constant values representing the biased address of the card table in XIR snippets.
     * Must be done once, after the final card table is initialized and before the compiler generates code using these XIR snippets.
     * Typically, this is called at the end of the PRISTINE initialization of the heap scheme.
     */
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
        asm.shr(temp, tupleCell, asm.i(CardTableRSet.LOG2_CARD_SIZE));
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
        asm.shr(temp, temp, asm.i(CardTableRSet.LOG2_CARD_SIZE));
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
     * Visit all the cells that overlap the specified range of cards.
     * The range of visited cards extends from the card startCardIndex, inclusive, to the
     * card endCardIndex, exclusive.
     *
     * @param cellVisitor the logic to apply to the visited cell
     */
    private void visitCards(int startCardIndex, int endCardIndex, OverlappingCellVisitor cellVisitor) {
        final Address start = cardTable.rangeStart(startCardIndex);
        final Address end = cardTable.rangeStart(endCardIndex);
        Pointer cell = cfoTable.cellStart(startCardIndex).asPointer();
        do {
            cell = cellVisitor.visitCell(cell, start, end);
            if (MaxineVM.isDebug()) {
                // FIXME: this is too strong, because DeadSpaceCardTableUpdater may leave a FOT entry temporarily out-dated by up
                // to minObjectSize() words when the HeapFreeChunkHeader of the space the allocator was refill with is immediately
                // before a card boundary.
                // I 'm leaving this as is now to see whether we ever run into this case, but this
                // should really be cell.plus(minObjectSize()).greaterThan(start);
                FatalError.check(cell.plus(HeapSchemeAdaptor.minObjectSize()).greaterThan(start), "visited cell must overlap visited card.");
            }
        } while (cell.lessThan(end));
    }

    private void traceVisitedCard(int startCardIndex, int endCardIndex, CardState cardState) {
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
        Log.println(")");
    }


    public void checkNoCardInState(Address start, Address end, CardState cardState) {
        final int endOfRange = cardTable.tableEntryIndex(end);
        int cardIndex = cardTable.first(cardTable.tableEntryIndex(start), endOfRange, cardState);
        if (cardIndex < endOfRange) {
            Log.print("Unexpected state for card #");
            Log.print(cardIndex);
            Log.print(" [");
            Log.print(cardTable.rangeStart(cardIndex));
            Log.print(", ");
            Log.print(cardTable.rangeStart(cardIndex + 1));
            Log.println(" ]");
            FatalError.breakpoint();
            FatalError.crash("invariant violation");
        }
    }

    public int countCardInState(Address start, Address end, CardState cardState) {
        final int endOfRange = cardTable.tableEntryIndex(end);
        int count = 0;
        int cardIndex = cardTable.first(cardTable.tableEntryIndex(start), endOfRange, cardState);
        while (cardIndex < endOfRange) {
            count++;
            cardIndex = cardTable.first(++cardIndex, endOfRange, cardState);
        }
        return count;
    }

    public void setCards(Address start, Address end, CardState cardState) {
        cardTable.fill(cardTable.tableEntryIndex(start), cardTable.tableEntryIndex(end), cardState.value);
    }

    public static abstract class CardRangeVisitor {
        abstract public void visitCards(Address start, Address end);
    }

    public void cleanAndVisitCards(Address start, Address end, CardRangeVisitor cardRangeVisitor) {
        final int endOfRange = cardTable.tableEntryIndex(end);
        int startCardIndex = cardTable.first(cardTable.tableEntryIndex(start), endOfRange, CardState.DIRTY_CARD);
        while (startCardIndex < endOfRange) {
            int endCardIndex = cardTable.firstNot(startCardIndex + 1, endOfRange, CardState.DIRTY_CARD);
            if (traceCardTableRSet()) {
                traceVisitedCard(startCardIndex, endCardIndex, CardState.DIRTY_CARD);
            }
            cardTable.clean(startCardIndex, endCardIndex);
            cardRangeVisitor.visitCards(cardTable.rangeStart(startCardIndex), cardTable.rangeStart(endCardIndex));
            if (++endCardIndex >= endOfRange) {
                return;
            }
            startCardIndex = cardTable.first(endCardIndex, endOfRange, CardState.DIRTY_CARD);
        }
    }

    /**
     * Iterate over cells that overlap the specified region and comprises recorded reference locations.
     * @param start
     * @param end
     * @param cellVisitor
     */
    public void cleanAndVisitCards(Address start, Address end, OverlappingCellVisitor cellVisitor) {
        final int endOfRange = cardTable.tableEntryIndex(end);
        int startCardIndex = cardTable.first(cardTable.tableEntryIndex(start), endOfRange, CardState.DIRTY_CARD);
        while (startCardIndex < endOfRange) {
            int endCardIndex = cardTable.firstNot(startCardIndex + 1, endOfRange, CardState.DIRTY_CARD);
            if (traceCardTableRSet()) {
                traceVisitedCard(startCardIndex, endCardIndex, CardState.DIRTY_CARD);
            }
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
            if (traceCardTableRSet()) {
                traceVisitedCard(startCardIndex, endCardIndex, cardState);
            }
            visitCards(startCardIndex, endCardIndex, cellVisitor);
            if (++endCardIndex >= endOfRange) {
                return;
            }
            startCardIndex = cardTable.first(endCardIndex, endOfRange, cardState);
        }
    }

    /**
     * Returns the amount of memory needed by the card table to cover a contiguous range of memory of the specified size.
     * @param maxCoveredAreaSize the size of the contiguous range of memory that the card table should cover
     * @return the number of bytes needed by the card table remembered set
     */
    @Override
    public Size memoryRequirement(Size maxCoveredAreaSize) {
        return cardTable.tableSize(maxCoveredAreaSize).plus(cfoTable.tableSize(maxCoveredAreaSize));
    }

    /**
     * Contiguous region of memory used by the remembered set.
     * @return a non-null {@link MemoryRegion}
     */
    public MemoryRegion memory() {
        return tablesMemory;
    }

    private void updateForFreeSpace(Address start, Address end) {
        // Note: this doesn't invalid subsequent entries of the FOT table.
        cfoTable.set(start, end);
        // Clean cards that are completely overlapped by the free space only.
        cardTable.setCardsInRange(start, end, CardState.CLEAN_CARD);
    }

    /**
     * Update the remembered set to take into account the newly freed space.
     * @param start address to the first word of the freed chunk
     * @param size size of the freed chunk
     */
    public void updateForFreeSpace(Address start, Size size) {
        updateForFreeSpace(start,  start.plus(size));
    }

    public static Address alignUpToCard(Address coveredAddress) {
        return coveredAddress.plus(CARD_SIZE).and(CARD_ADDRESS_MASK);
    }

    public static Address alignDownToCard(Address coveredAddress) {
        return coveredAddress.and(CARD_ADDRESS_MASK);
    }

    // Allocation may occur while iterating over dirty cards to evacuate young objects. Such allocations may temporarily invalidate
    // the FOT for cards overlapping with the allocator, and may break the ability to walk over these cards.
    // One solution is keep all the FOT entries covering the allocation space up to date at every allocation.
    // Every time an allocation occurs it splits the allocation space in two. Each side forms a new cell for which FOT entries need to be
    // updated.
    //
    // Another solution is to make the last card of the free chunk used by the allocator independent of FOT updates required by the split.
    // The last card is the only one that may be dirtied, and therefore the only one that can be walked over during evacuation.
    //
    // Let s and e be the start and end of a free chunk.
    // Let c be the top-most card address such that c >= e, and C be the card starting at c, with FOT(C) denoting the entry
    // of the FOT for card C.
    // If e == c,  whatever objects are allocated between s and e, FOT(C) == 0, i.e., allocations never invalidate FOT(C).
    // Thus C is iterable at all time by a dirty card walker.
    //
    // If e > c, then FOT(C) might be invalidated by allocation.
    // We may avoid this by formatting the space delimited by [c,e] as a dead object embedded in the heap free chunk.
    // This will allow FOT(C) to be zero, and not be invalidated by any allocation of space before C.
    // Formatting [c,e] has however several corner cases:
    // 1. [c,e] may be smaller than the minimum object size, so we can't format it.
    // Instead, we can format [x,e], such x < e and [x,e] is the min object size. In this case, FOT(C) = x - e.
    // [x,e] can only be overwritten by the last allocation from the buffer,
    // so FOT(C) doesn't need to be updated until that last allocation,
    // which is always special cased (see why below).
    // 2. s + sizeof(free chunk header) > c, i.e., the head of the free space chunk overlap C.
    // Don't reformat the heap chunk. FOT(C) will be updated correctly since the allocator
    // always set the FOT table for the newly allocated cells.
    //
    // This function decide whether the last card of the dead space need to be split and formatted as a
    // dead objects at card boundary to avoid updating the FOT entries covering the right-hand side of an allocation split.
    // If it return Address.zero() then the chunks remains as a single object, otherwise, it's tail was reformated as a dead object
    // and the FOT must be updated accordingly.
    // In both case, subsequent split event on this dead space only need to update the left-hand side of the allocation split (using the set() method).
    private Address splitLastCard(Address deadSpace, Size numDeadBytes) {
        if (numDeadBytes.greaterEqual(CARD_SIZE)) {
            final Address end = deadSpace.plus(numDeadBytes);
            final Address lastCardStart = alignDownToCard(end);
            if (lastCardStart.minus(deadSpace).greaterThan(HeapFreeChunk.heapFreeChunkHeaderSize())) {
                // Format the end of the heap free chunk as a dead object.
                Address deadObjectAddress = lastCardStart;
                Size deadObjectSize = end.minus(deadObjectAddress).asSize();

                if (deadObjectSize.lessThan(minObjectSize())) {
                    deadObjectAddress = end.minus(minObjectSize());
                }
                if (MaxineVM.isDebug() && CardFirstObjectTable.TraceFOT) {
                    Log.print("Split last card of Dead Space [");
                    Log.print(deadSpace); Log.print(", ");
                    Log.print(end);
                    Log.print("] @ ");
                    Log.print(deadObjectAddress);
                    Log.print(" card # ");
                    Log.println(cardTable.tableEntryIndex(deadObjectAddress));
                }
                // Here we don't use dark matter as we're formatting allocatable space.
                HeapSchemeAdaptor.fillWithDeadObject(deadObjectAddress, end);
                return deadObjectAddress;
            }
        }
        return Address.zero();
    }

    /**
     * Prepare an contiguous range of heap space to be used for allocation (i.e., split live operations).
     * See comments for {@link #splitLastCard(Address, Size)}.
     *
     * @param deadSpace address to the first byte of the dead space
     * @param numDeadBytes number of bytes
     */
    private void prepareForSplitLive(Address deadSpace, Size numDeadBytes) {
        // Same as above, except that the dead space may need reformatted similar to coalescing.
        final Address deadObjectAddress = splitLastCard(deadSpace, numDeadBytes);
        if (deadObjectAddress.isNotZero()) {
            cfoTable.set(deadSpace, deadObjectAddress);
            cfoTable.set(deadObjectAddress, deadSpace.plus(numDeadBytes));
        } else {
            // Otherwise, the free chunk is either smaller than a card, or it is smaller than two cards and its header spawn the two cards.
            cfoTable.set(deadSpace, numDeadBytes);
        }
    }

    @INLINE
    @Override
    public void notifyCoalescing(Address deadSpace, Size numDeadBytes) {
        final Address deadObjectAddress = splitLastCard(deadSpace, numDeadBytes);
        if (deadObjectAddress.isNotZero()) {
            updateForFreeSpace(deadSpace, deadObjectAddress);
            updateForFreeSpace(deadObjectAddress, deadSpace.plus(numDeadBytes));
        } else {
            // Otherwise, the free chunk is either smaller than a card, or it is smaller than two cards and its header spawn the two cards.
            updateForFreeSpace(deadSpace, numDeadBytes);
        }
    }

    @INLINE
    @Override
    public void notifySplitLive(Address start, Size leftSize, Address end) {
        // We only have to set the FOT for the left-hand side of the split. The right-hand side is formatted to avoid update to its FOT.
        // See notifyCoalescing and notifyRetireFreeSpace.
        cfoTable.set(start, leftSize);
    }

    @INLINE
    @Override
    public void notifySplitDead(Address start, Size leftSize, Address end) {
        Address splitBound = start.plus(leftSize);
        // We need each of the split free space to be formatted as if each were just coalesced
        prepareForSplitLive(start, leftSize);
        prepareForSplitLive(splitBound, end.minus(splitBound).asSize());
    }

    @INLINE
    @Override
    public void notifyRefill(Address deadSpace, Size numDeadBytes) {
        // We don't need to do anything as dead space are already formatted correctly during coalescing and retire events.
    }

    @INLINE
    @Override
    public void notifyRetireDeadSpace(Address deadSpace, Size numDeadBytes) {
        // Retired space is unoccupied space left-over by an allocator.
        // We only need to update the FOT table to form a single contiguous dead space.
        // Don't reset the card table overlapping it (as is done in notifyCoalescing): cards that completely overlap with
        // the dead space were already cleaned in the coalescing event that formed the dead space this retired dead
        // space was originally a part of.
        // Cards that partially overlap with live objects (i.e., at the bounds of the dead space) must be left untouched.
        cfoTable.set(deadSpace, numDeadBytes);
    }

    @INLINE
    @Override
    public void notifyRetireFreeSpace(Address deadSpace, Size numDeadBytes) {
        prepareForSplitLive(deadSpace, numDeadBytes);
    }
}
