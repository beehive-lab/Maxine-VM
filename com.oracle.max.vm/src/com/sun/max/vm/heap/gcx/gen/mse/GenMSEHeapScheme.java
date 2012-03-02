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
package com.sun.max.vm.heap.gcx.gen.mse;
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;
import static com.sun.max.vm.heap.gcx.HeapRegionManager.*;
import static com.sun.max.vm.heap.gcx.gen.mse.GenMSEHeapScheme.GenMSEHeapRegionTag.*;

import com.sun.cri.xir.*;
import com.sun.cri.xir.CiXirAssembler.XirOperand;
import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.heap.gcx.rset.ctbl.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;


/**
 * Generational Heap Scheme with a mark-sweep old generation and a simple copying collector nursery.
 */
final public class GenMSEHeapScheme extends HeapSchemeWithTLABAdaptor  implements HeapAccountOwner, XirWriteBarrierSpecification, RSetCoverage {
    /**
     * Number of heap words covered by a single mark.
     */
    private static final int WORDS_COVERED_PER_BIT = 1;

    /**
     * Knob for the fixed ratio resizing policy.
     */
    static int YoungGenHeapPercent = 30;
    static Size ELABSize = Size.K.times(64);
    static {
        VMOptions.addFieldOption("-XX:", "YoungGenHeapPercent", GenMSEHeapScheme.class, "Fixed percentage of heap size that must be used by young gen", Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "ELABSize", GenMSEHeapScheme.class, "Size of local allocation buffers for evacuation to old gen", Phase.PRISTINE);
    }

    public enum GenMSEHeapRegionTag {
        UNTAGGED,
        YOUNG,
        OLD,
        BOOT;
        public int tag() {
            return ordinal();
        }
    }

    /**
     * Account for the application's generational heap. Both old and young generations tap in this account for their storage.
     */
    private final HeapAccount<GenMSEHeapScheme> heapAccount;

    // FIXME: the interface for a generations abstraction not fully designed yet, so for now we use spaces directly here.
    /**
     * Young generation.
     */
    @INSPECTED
    private final NoAgingNursery youngSpace;
    /**
     * Tenured generation.
     */
    @INSPECTED
    private final FirstFitMarkSweepSpace<GenMSEHeapScheme> oldSpace;

    /**
     * Policy for resizing the heap after each GC.
     */
    private GenHeapSizingPolicy heapResizingPolicy;

    /**
     * Card-table based remembered set for the nursery.
     */
    @INSPECTED
    private final CardTableRSet cardTableRSet;
    /**
     * Implementation of young space evacuation. Used by minor collection operations.
     */
    private NoAgingEvacuator youngSpaceEvacuator;

    /**
     * Operation to submit to the {@link VmOperationThread} to perform a generational collection.
     */
    private final GenCollection genCollection;

    /**
     * Marking algorithm used to trace the heap.
     */
    private final TricolorHeapMarker heapMarker;

    /**
     * Support for heap verification.
     */
    private final NoYoungReferenceVerifier noYoungReferencesVerifier;
    private final FOTVerifier fotVerifier;


    @HOSTED_ONLY
    public GenMSEHeapScheme() {
        heapAccount = new HeapAccount<GenMSEHeapScheme>(this);
        heapMarker = new TricolorHeapMarker(WORDS_COVERED_PER_BIT, new HeapAccounRootCellVisitor(this));
        cardTableRSet = new CardTableRSet();
        youngSpace = new NoAgingNursery(heapAccount, YOUNG.tag());

        final ChunkListAllocator<RegionChunkListRefillManager> tlabAllocator =
            new ChunkListAllocator<RegionChunkListRefillManager>(new RegionChunkListRefillManager(cardTableRSet));
        final CardSpaceAllocator<RegionOverflowAllocatorRefiller> overflowAllocator =
            new CardSpaceAllocator<RegionOverflowAllocatorRefiller>(new RegionOverflowAllocatorRefiller(cardTableRSet), cardTableRSet);

        oldSpace = new FirstFitMarkSweepSpace<GenMSEHeapScheme>(heapAccount, tlabAllocator, overflowAllocator, true, cardTableRSet, OLD.tag());
        noYoungReferencesVerifier = new NoYoungReferenceVerifier(cardTableRSet, youngSpace);
        fotVerifier = new FOTVerifier(cardTableRSet);
        genCollection = new GenCollection();
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        cardTableRSet.initialize(phase);
    }

    /**
     * Interface to the heap region manager to request coverage of all heap spaces by remembered set.
     * This must be called before the first assignment to a reference location so that code
     * generated with write barrier doesn't fail.
     */
    @Override
    public void initializeCoverage(Address coveredAreaStart, Size coveredAreaSize) {
        final int pageSize = Platform.platform().pageSize;
        final Address endOfCoveredArea = coveredAreaStart.plus(coveredAreaSize);
        final Size cardTableCoveredAreaSize = endOfCoveredArea.minus(Heap.bootHeapRegion.start()).asSize();

        // Allocate Card Table Data at the end of the covered area (i.e., space reserved to the heap regions).
        final Address cardTableDataStart =  endOfCoveredArea.roundedUpBy(pageSize);

        // We want the card table to cover not just the dynamic heap, but also the boot image and code cache to avoid testing
        // for boundaries in the write barrier. Note that covering these with the card table doesn't mean we will iterate over these
        // cards to find references to young objects (i.e., it may be cheaper to use the reference maps for the boot image).
        final Size cardTableDataSize = cardTableRSet.memoryRequirement(cardTableCoveredAreaSize);
        if (!Heap.AvoidsAnonOperations) {
            if (!VirtualMemory.commitMemory(cardTableDataStart, cardTableDataSize,  VirtualMemory.Type.DATA)) {
                MaxineVM.reportPristineMemoryFailure("card table space", "commit", cardTableDataSize);
            }
        }
        cardTableRSet.initialize(Heap.bootHeapRegion.start(), cardTableCoveredAreaSize, cardTableDataStart, cardTableDataSize);
    }

    /**
     * Allocate memory for both the heap and the GC's data structures (mark bitmaps, marking stacks, card & offset tables, etc.).
     */
    @Override
    protected void allocateHeapAndGCStorage() {
        final Size reservedSpace = Size.K.times(reservedVirtualSpaceKB());
        final Size initSize = Heap.initialSize();
        final Size maxSize = Heap.maxSize();
        final int pageSize = Platform.platform().pageSize;

        // Verify that the constraint of the heap scheme are met:
        FatalError.check(Heap.bootHeapRegion.start() == Heap.startOfReservedVirtualSpace(),
            "Boot heap region must be mapped at start of reserved virtual space");

        final Address endOfCodeRegion = Code.getCodeManager().getRuntimeOptCodeRegion().end();
        final Address endOfReservedSpace = Heap.bootHeapRegion.start().plus(reservedSpace);

        // Initialize the heap region manager.
        final Address  firstUnusedByteAddress = endOfCodeRegion;
        theHeapRegionManager().initialize(firstUnusedByteAddress, endOfReservedSpace, maxSize, HeapRegionInfo.class, BOOT.tag());

        try {
            enableCustomAllocation(theHeapRegionManager().allocator());
            final MemoryRegion heapBounds = theHeapRegionManager().bounds();
            final Size applicationHeapMaxSize = heapBounds.size().minus(theHeapRegionManager().size());

            // Compute space needed by the heap marker. This is proportional to the size of the space traced by the heap marker.
            // The boot image isn't traced (it is assumed a permanent root of collection).
            final Size heapMarkerDatasize = heapMarker.memoryRequirement(heapBounds.size());

            // Heap Marker Data are allocated after the remembered set's.
            final Address heapMarkerDataStart = cardTableRSet.memory().end().roundedUpBy(pageSize);

            // Address to the first reserved byte unused by the heap scheme.
            Address unusedReservedSpaceStart = heapMarkerDataStart.plus(heapMarkerDatasize).roundedUpBy(pageSize);

            if (unusedReservedSpaceStart.greaterThan(endOfReservedSpace)) {
                MaxineVM.reportPristineMemoryFailure("Can't allocate heap marker", "reserve", heapMarkerDatasize);
            }
            if (!Heap.AvoidsAnonOperations) {
                if (!VirtualMemory.commitMemory(heapMarkerDataStart, heapMarkerDatasize,  VirtualMemory.Type.DATA)) {
                    MaxineVM.reportPristineMemoryFailure("heap marker space", "commit", heapMarkerDatasize);
                }
            }
            heapMarker.initialize(heapBounds.start(), heapBounds.end(), heapMarkerDataStart, heapMarkerDatasize);

            // Free reserved space we will not be using.
            Size leftoverSize = endOfReservedSpace.minus(unusedReservedSpaceStart).asSize();

            // First, uncommit range we want to free (this will create a new mapping that can then be deallocated)
            if (!Heap.AvoidsAnonOperations) {
                if (!VirtualMemory.uncommitMemory(unusedReservedSpaceStart, leftoverSize,  VirtualMemory.Type.DATA)) {
                    MaxineVM.reportPristineMemoryFailure("reserved space leftover", "uncommit", leftoverSize);
                }
            }
            if (VirtualMemory.deallocate(unusedReservedSpaceStart, leftoverSize, VirtualMemory.Type.DATA).isZero()) {
                MaxineVM.reportPristineMemoryFailure("reserved space leftover", "deallocate", leftoverSize);
            }

            heapResizingPolicy = new FixedRatioGenHeapSizingPolicy(initSize, maxSize, YoungGenHeapPercent, log2RegionSizeInBytes);
            if (!heapAccount().open(numberOfRegions(applicationHeapMaxSize))) {
                FatalError.unexpected("Failed to create application heap");
            }
            youngSpace.initialize(heapResizingPolicy);
            oldSpace.initialize(heapResizingPolicy.initialOldGenSize(), heapResizingPolicy.maxOldGenSize());

            // FIXME: the capacity of the survivor range queues should be dynamic. Its upper bound could be computed based on the
            // worst case evacuation and the number of fragments of old space available for allocation.
            // Same with the lab size. In non parallel evacuators, this should be all the space available for allocation in a region.
            youngSpaceEvacuator = new NoAgingEvacuator(youngSpace, oldSpace, cardTableRSet, oldSpace.minReclaimableSpace());
            youngSpaceEvacuator.initialize(1000, ELABSize);

            if (HeapRangeDumper.DumpOnError) {
                MemoryRegion dumpingCoverage = new MemoryRegion();
                dumpingCoverage.setStart(Heap.bootHeapRegion.start());
                dumpingCoverage.setEnd(heapBounds.end());
                HeapRangeDumper dumper = new HeapRangeDumper(heapBounds);
                dumper.refineOnFirstUnparsableWith(new RefineDumpRangeToCard(cardTableRSet));
                youngSpaceEvacuator.setDumper(dumper);
            }

            cardTableRSet.initializeXirStartupConstants();
             // Make the heap inspectable
            InspectableHeapInfo.init(false, heapBounds, heapMarker.memory(), cardTableRSet.memory());
        } finally {
            disableCustomAllocation();
        }
        theHeapRegionManager().checkOutgoingReferences();
    }

    @Override
    public int reservedVirtualSpaceKB() {
        // 2^30 Kb = 1 TB of reserved virtual space.
        // This will be truncated as soon as we taxed what we need at initialization time.
        return Size.G.toInt();
    }

    @Override
    protected void releaseUnusedReservedVirtualSpace() {
        // Do nothing. This heap scheme has its own way of doing this.
        // See allocateHeapAndGCStorage
    }

    final class GenCollection extends GCOperation {
        HeapRegionRangeIterable regionsRangeIterable;
        int fullCollectionCount = 0;
        GenCollection() {
            super("GenCollection");
            regionsRangeIterable = new HeapRegionRangeIterable();
        }
        private void verifyAfterEvacuation() {
            // Verify that:
            // 1. offset table is correctly setup
            // 2. there are no pointer from old to young.
            // 3. cards are all cleaned (except for those holding special references, which may have been dirtied during reference discovery)
            oldSpace.visit(fotVerifier);
            oldSpace.visit(noYoungReferencesVerifier);
        }

        /**
         * Perform old generation collection. This is done after the young generation has been fully evacuated.
         */
        private void doOldGenCollection() {
            youngSpaceEvacuator.doBeforeGC();
            youngSpace.doBeforeGC();
            oldSpace.doBeforeGC();
            regionsRangeIterable.initialize(heapAccount.committedRegions());
            HeapScheme.Inspect.notifyHeapPhaseChange(HeapPhase.ANALYZING);
            heapMarker.markAll(regionsRangeIterable);
            HeapScheme.Inspect.notifyHeapPhaseChange(HeapPhase.RECLAIMING);
            oldSpace.sweep(heapMarker, false);
            oldSpace.doAfterGC();
            youngSpaceEvacuator.doAfterGC();
            fullCollectionCount++;
            HeapScheme.Inspect.notifyHeapPhaseChange(HeapPhase.ALLOCATING);
        }

        @Override
        protected void collect(int invocationCount) {
            // Collector proceeds as follows:
            // 1. evacuate nursery
            // 2. if old gen free space smaller than worst case evacuation (WCE) , do a full collection
            // 3. if old gen free space still smaller than WCE, resize the heap (either grow it, or shrink the young gen
            // 4. if heap resizing fail, GC failed, we're out of memory.
            //
            // The rationale for this is that in order for mutator to proceeds, the nursery must be empty again.
            // This requires evacuating all of its objects somehow. Rather that doing a full GC covering both
            // the old and young gen and somehow reclaim enough regions for a fresh nursery, we just perform a nursery evacuation.
            // The full GC is thereafter just a old gen GC with an empty young gen.
            VmThreadMap.ACTIVE.forAllThreadLocals(null, tlabFiller);
            vmConfig().monitorScheme().beforeGarbageCollection();
            if (Heap.verbose()) {
                Log.println("--Begin nursery evacuation");
            }
            youngSpaceEvacuator.evacuate();
            if (Heap.verbose()) {
                Log.println("--End nursery evacuation");
            }
            if (VerifyAfterGC) {
                verifyAfterEvacuation();
            }
            Size worstCaseEvac = youngSpace.totalSpace();
            Size freeSpace = oldSpace.freeSpace();
            if (worstCaseEvac.greaterThan(freeSpace)) {
                if (Heap.verbose()) {
                    Log.println("--Begin old geneneration collection");
                }
                doOldGenCollection();
                if (Heap.verbose()) {
                    Log.println("--End   old geneneration collection");
                }

                if (VerifyAfterGC) {
                    verifyAfterEvacuation();
                }
                freeSpace = oldSpace.freeSpace();
                if (worstCaseEvac.greaterThan(freeSpace)) {
                    // TODO: 3 and 4.
                    FatalError.unimplemented();
                }
            }
        }
    }

    @Override
    public boolean contains(Address address) {
        return theHeapRegionManager().contains(address);
    }

    @Override
    public boolean collectGarbage(Size requestedFreeSpace) {
        genCollection.submit();
        return true;
    }

    @Override
    public Size reportFreeSpace() {
        return oldSpace.freeSpace().plus(youngSpace.freeSpace());
    }

    @Override
    public Size reportUsedSpace() {
        return oldSpace.usedSpace().plus(youngSpace.usedSpace());
    }

    @INLINE
    @FOLD
    @Override
    public boolean needsBarrier(IntBitSet<WriteBarrierSpecification.WriteBarrierSpec> writeBarrierSpec) {
        return writeBarrierSpec.isSet(WriteBarrierSpec.POST_WRITE);
    }

    @INLINE
    @Override
    public void postWriteBarrier(Reference ref, Offset offset, Reference value) {
        cardTableRSet.record(ref, offset);
    }

    @INLINE
    @Override
    public void postWriteBarrier(Reference ref,  int displacement, int index, Reference value) {
        cardTableRSet.record(ref, displacement, index);
    }

    /**
     * Allocate a chunk of memory of the specified size and refill a thread's TLAB with it.
     * @param etla the thread whose TLAB will be refilled
     * @param tlabSize the size of the chunk of memory used to refill the TLAB
     */
    private void allocateAndRefillTLAB(Pointer etla, Size tlabSize) {
        Pointer tlab = youngSpace.allocate(tlabSize);
        Size effectiveSize = tlabSize.minus(TLAB_HEADROOM);
        if (traceTLAB()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.printCurrentThread(false);
            Log.print(": Allocated TLAB at ");
            Log.print(tlab);
            Log.print(" [TOP=");
            Log.print(tlab.plus(tlabSize.minus(TLAB_HEADROOM)));
            Log.print(", end=");
            Log.print(tlab.plus(tlabSize));
            Log.print(", size=");
            Log.print(tlabSize);
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
        refillTLAB(etla, tlab, effectiveSize);
    }

    @Override
    @NEVER_INLINE
    protected Pointer handleTLABOverflow(Size size, Pointer etla, Pointer tlabMark, Pointer tlabEnd) {
        // Should we refill the TLAB ?
        final TLABRefillPolicy refillPolicy = TLABRefillPolicy.getForCurrentThread(etla);
        if (refillPolicy == null) {
            // No policy yet for the current thread. This must be the first time this thread uses a TLAB (it does not have one yet).
            FatalError.check(tlabMark.isZero(), "thread must not have a TLAB yet");
            if (!usesTLAB()) {
                // We're not using TLAB. So let's assign the never refill tlab policy.
                TLABRefillPolicy.setForCurrentThread(etla, NEVER_REFILL_TLAB);
                return youngSpace.allocate(size);
            }
            // Allocate an initial TLAB and a refill policy. For simplicity, this one is allocated from the TLAB (see comment below).
            final Size tlabSize = initialTlabSize();
            allocateAndRefillTLAB(etla, tlabSize);
            // Let's do a bit of meta-circularity. The TLAB is refilled, and no-one except the current thread can use it.
            // So the TLAB allocation is going to succeed here
            TLABRefillPolicy.setForCurrentThread(etla, new SimpleTLABRefillPolicy(tlabSize));
            // Now, address the initial request. Note that we may recurse down to handleTLABOverflow again here if the
            // request is larger than the TLAB size. However, this second call will succeed and allocate outside of the TLAB.
            return tlabAllocate(size);
        }
        final Size nextTLABSize = refillPolicy.nextTlabSize();
        if (size.greaterThan(nextTLABSize)) {
            // This couldn't be allocated in a TLAB, so go directly to direct allocation routine.
            // NOTE: this is where we always go if we don't use TLABs (the "never refill" TLAB policy
            // always return zero for the next TLAB size.
            return youngSpace.allocate(size);
        }
        if (!refillPolicy.shouldRefill(size, tlabMark)) {
            // Size would fit in a new tlab, but the policy says we shouldn't refill the TLAB yet, so allocate directly in the young generation.
            return youngSpace.allocate(size);
        }
        // Refill TLAB and allocate (we know the request can be satisfied with a fresh TLAB and will therefore succeed).
        allocateAndRefillTLAB(etla, nextTLABSize);
        return tlabAllocate(size);
    }

    @Override
    protected Pointer customAllocate(Pointer customAllocator, Size size) {
        return BaseAtomicBumpPointerAllocator.asBumpPointerAllocator(Reference.fromOrigin(Layout.cellToOrigin(customAllocator)).toJava()).allocateCleared(size);
    }

    @Override
    public HeapAccount<GenMSEHeapScheme> heapAccount() {
        return heapAccount;
    }

    @Override
    protected void reportTotalGCTimes() {
        // TODO
    }

    @HOSTED_ONLY
    public XirWriteBarrierGenerator barrierGenerator(IntBitSet<WriteBarrierSpecification.WriteBarrierSpec> writeBarrierSpec) {
        if (writeBarrierSpec.equals(TUPLE_POST_BARRIER)) {
            return new XirWriteBarrierGenerator() {
                @Override
                public void genWriteBarrier(CiXirAssembler asm, XirOperand ... operands) {
                    cardTableRSet.genTuplePostWriteBarrier(asm, operands[0]);
                }
            };
        } else if (writeBarrierSpec.equals(ARRAY_POST_BARRIER)) {
            return new XirWriteBarrierGenerator() {
                @Override
                public void genWriteBarrier(CiXirAssembler asm, XirOperand ... operands) {
                    cardTableRSet.genArrayPostWriteBarrier(asm, operands[0], operands[1]);
                }
            };
        }
        return XirWriteBarrierSpecification.NULL_WRITE_BARRIER_GEN;
    }

}
