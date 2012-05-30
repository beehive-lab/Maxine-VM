/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.sequential.gen.semiSpace;

import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.heap.gcx.EvacuationTimers.TIMERS.*;

import com.sun.cri.xir.*;
import com.sun.cri.xir.CiXirAssembler.XirOperand;
import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.heap.gcx.rset.*;
import com.sun.max.vm.heap.gcx.rset.ctbl.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.VMLogger.Interval;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
/**
 * A heap scheme implementing a two-generations heap, where each generation implements a semi-space collector.
 *
 */
public final class GenSSHeapScheme extends HeapSchemeWithTLABAdaptor implements XirWriteBarrierSpecification, RSetCoverage {
    /**
     * Knob for the fixed ratio resizing policy.
     */
    static int YoungGenHeapPercent = 30;
    static {
        VMOptions.addFieldOption("-XX:", "YoungGenHeapPercent", GenSSHeapScheme.class, "Fixed percentage of heap size that must be used by young gen", Phase.PRISTINE);
    }

    /**
     * Refiller for the OldSpace allocator.
     */
    final class OldSpaceRefiller extends Refiller {
        @Override
        public Address allocateRefill(Pointer startOfSpaceLeft, Size spaceLeft) {
            if (Heap.holdsHeapLock() &&  VmThread.current().isVmOperationThread()) {
                // First, make sure we're doing minor collection here.
                if (youngSpaceEvacuator.getGCOperation() != null) {
                    final CardSpaceAllocator<OldSpaceRefiller> allocator = oldSpace.allocator();
                    final ContiguousHeapSpace fromSpace = oldSpace.fromSpace;
                    FatalError.check(!fromSpace.start().equals(allocator.start()), "Must not have recursive overflow of old space during minor collection");
                    HeapFreeChunk.format(startOfSpaceLeft, spaceLeft);
                    resizingPolicy.notifyMinorEvacuationOverflow();
                    // Refill the allocator with the old from space.
                    allocator.refill(oldSpace.fromSpace.start(), oldSpace.fromSpace.committedSize());
                } else {
                    // We may overflow during full GC because of a previous minor collection overflow.
                    FatalError.unimplemented();
                }
            } else {
                // Force full collection.
                Heap.collectGarbage(Size.ZERO);
            }
            return Address.zero();
        }

        @Override
        protected void doBeforeGC() {
        }

    }

    /**
     * Refiller for the young space allocator. Just trigger a garbage collect.
     */
    class YoungSpaceRefiller extends Refiller {
        @Override
        public Address allocateRefill(Pointer startOfSpaceLeft, Size spaceLeft) {
            AtomicBumpPointerAllocator<YoungSpaceRefiller> allocator = youngSpace.allocator();
            Size size = allocator.size();
            while (!Heap.collectGarbage(size)) {
                size = allocator.size();
                // TODO: condition for OOM
            }
            // We're out of safepoint. The current thread hold the refill lock and will do the refill of the allocator.
            return Address.zero();
        }

        @Override
        protected void doBeforeGC() {
            // Nothing to do.
        }
    }

    /**
     * Always start with a minor collection.
     * If space left after the minor collection in the old generation is less than estimated space for survivors of the next minor collection,
     * a full GC is performed immediately.
     * It is possible for the minor collection to overflow the old generation because of under-estimated survivor space at the last minor collection.
     * This is caught by the refiller of the old generation allocator, which in this case allocate space directly in the second semi-space.
     */
    final class GenCollection extends GCOperation {
        private int minSurvivingPercent = 15; // expected percentage of survivors. Arbitrary for now

        GenCollection() {
            super("GenCollection");
        }
        private void verifyAfterMinorCollection() {
            // Verify that:
            // 1. offset table is correctly setup
            // 2. there are no pointer from old to young.
            oldSpace.visit(fotVerifier);
            noFromSpaceReferencesVerifiers.setEvacuatedSpace(youngSpace);
            oldSpace.visit(noFromSpaceReferencesVerifiers);
        }

        private void verifyAfterFullCollection() {
            oldSpace.visit(fotVerifier);
            noFromSpaceReferencesVerifiers.setEvacuatedSpace(oldSpace.fromSpace);
            oldSpace.visit(noFromSpaceReferencesVerifiers);
        }

        private void doOldGenCollection() {
            youngSpaceEvacuator.doBeforeGC();
            // NOTE: counter must be incremented before a heap phase change  to ANALYZING.
            fullCollectionCount++;
            final boolean minorEvacuationOverflow = resizingPolicy.minorEvacuationOverflow();
            if (minorEvacuationOverflow) {
                Address startRange =  oldSpace.allocator.start();
                Address endRange = oldSpace.allocator.unsafeTop();
                oldSpaceEvacuator.prefillSurvivorRanges(startRange, endRange);
            }
            oldSpace.flipSpaces(!minorEvacuationOverflow);
            oldSpaceEvacuator.setGCOperation(this);
            oldSpaceEvacuator.setEvacuationSpace(oldSpace.fromSpace, oldSpace);
            oldSpaceEvacuator.evacuate();
            final CardFirstObjectTable fot = cardTableRSet.cfoTable;
            final int startIndex = fot.tableEntryIndex(oldSpace.fromSpace.start());
            final int endIndex = fot.tableEntryIndex(oldSpace.fromSpace.committedEnd());
            fot.clear(startIndex, endIndex);
            cardTableRSet.cardTable.clean(startIndex, endIndex);
            youngSpaceEvacuator.doAfterGC();
            oldSpaceEvacuator.setGCOperation(null);
        }

        private Size estimatedNextEvac() {
            final Size min = youngSpace.totalSpace().dividedBy(100).times(minSurvivingPercent);
            final Size lastSurvivorCount = youngSpaceEvacuator.evacuatedBytes();
            return lastSurvivorCount.greaterThan(min) ? lastSurvivorCount : min;
        }

        private void resize(HeapSpace space, Size newSize) {
            if (newSize.lessThan(space.totalSpace())) {
                Size delta = space.totalSpace().minus(newSize);
                space.shrinkAfterGC(delta);
            } else if (newSize.greaterThan(space.totalSpace())) {
                Size delta = newSize.minus(space.totalSpace());
                space.growAfterGC(delta);
            }
        }

        @Override
        protected void collect(int invocationCount) {
            evacTimers.resetTrackTime();

            VmThreadMap.ACTIVE.forAllThreadLocals(null, tlabFiller);
            vmConfig().monitorScheme().beforeGarbageCollection();
            if (MaxineVM.isDebug() && Heap.verbose()) {
                Log.println("--Begin nursery evacuation");
            }
            evacTimers.start(TOTAL);
            youngSpaceEvacuator.setGCOperation(this);
            youngSpaceEvacuator.setEvacuationBufferSize(oldSpace.freeSpace());
            youngSpaceEvacuator.evacuate();
            youngSpaceEvacuator.setGCOperation(null);
            if (MaxineVM.isDebug() && Heap.verbose()) {
                Log.println("--End nursery evacuation");
            }
            if (VerifyAfterGC) {
                verifyAfterMinorCollection();
            }
            final Size estimatedEvac = estimatedNextEvac();
            evacTimers.stop(TOTAL);
            if (Heap.logGCTime()) {
                timeLogger.logPhaseTimes(invocationCount,
                                evacTimers.get(ROOT_SCAN).getLastElapsedTime(),
                                evacTimers.get(BOOT_HEAP_SCAN).getLastElapsedTime(),
                                evacTimers.get(CODE_SCAN).getLastElapsedTime(),
                                evacTimers.get(RSET_SCAN).getLastElapsedTime(),
                                evacTimers.get(COPY).getLastElapsedTime(),
                                evacTimers.get(WEAK_REF).getLastElapsedTime());
                timeLogger.logGcTimes(invocationCount, true, evacTimers.get(TOTAL).getLastElapsedTime());
            }
            if (resizingPolicy.shouldPerformFullGC(estimatedEvac, oldSpace.freeSpace())) {
                // Force a temporary transition to MUTATING state.
                // This simplifies the inspector's maintenance of references state and GC counters.
                HeapScheme.Inspect.notifyHeapPhaseChange(HeapPhase.MUTATING);
                if (MaxineVM.isDebug() && Heap.verbose()) {
                    Log.println("--Begin old geneneration collection");
                }
                evacTimers.start(TOTAL);
                doOldGenCollection();
                if (MaxineVM.isDebug() && Heap.verbose()) {
                    Log.println("--End   old geneneration collection");
                }
                if (VerifyAfterGC) {
                    verifyAfterFullCollection();
                }
                if (resizingPolicy.resizeAfterFullGC(estimatedEvac, oldSpace.freeSpace())) {
                    resize(youngSpace, resizingPolicy.youngGenSize());
                    resize(oldSpace, resizingPolicy.oldGenSize());
                    oldSpaceEvacuator.setEvacuationBufferSize(oldSpace.fromSpace.committedSize());
                }
                evacTimers.stop(TOTAL);
                if (Heap.logGCTime()) {
                    timeLogger.logPhaseTimes(invocationCount,
                                    evacTimers.get(ROOT_SCAN).getLastElapsedTime(),
                                    evacTimers.get(BOOT_HEAP_SCAN).getLastElapsedTime(),
                                    evacTimers.get(CODE_SCAN).getLastElapsedTime(),
                                    evacTimers.get(RSET_SCAN).getLastElapsedTime(),
                                    evacTimers.get(COPY).getLastElapsedTime(),
                                    evacTimers.get(WEAK_REF).getLastElapsedTime());
                    timeLogger.logGcTimes(invocationCount, false, evacTimers.get(TOTAL).getLastElapsedTime());
                }

            }
            vmConfig().monitorScheme().afterGarbageCollection();
            HeapScheme.Inspect.notifyHeapPhaseChange(HeapPhase.MUTATING);
        }
    }

    /**
     * Old generation, organized as a semi-space.
     */
    @INSPECTED
    final ContiguousSemiSpace<CardSpaceAllocator<OldSpaceRefiller>> oldSpace;

    /**
     * Young generation, organized as a simple linear space. The collector doesn't do aging
     * in this first prototype. Later, this will be changed for a semi-space variant
     * (either with equal-size semi-space, or with a eden / survivor semi-space).
     */
    @INSPECTED
    final ContiguousAllocatingSpace<AtomicBumpPointerAllocator<YoungSpaceRefiller>> youngSpace;

    /**
     * Policy for resizing the heap after each GC.
     */
    private final GenSSHeapSizingPolicy resizingPolicy;

    /**
     * Operation to submit to the {@link VmOperationThread} to perform a generational collection.
     */
    private final GenCollection genCollection;

    /**
     * Card-table based remembered set for the nursery.
     */
    @INSPECTED
    private final CardTableRSet cardTableRSet;

    /**
     * Implementation of young space evacuation. Used by minor collection operations.
     */
    @INSPECTED
    private final NoAgingNurseryEvacuator youngSpaceEvacuator;

    @INSPECTED
    private final EvacuatorToCardSpace oldSpaceEvacuator;

    @INSPECTED
    private int fullCollectionCount;

    /**
     * Support for heap verification. All live objects are evacuated to the old to space on minor collection.
     * There should remain no references from the old space to the young space.
     * Similarly, all live objects are evacuated from the old "from" space on full collection.
     * There should remain no references from the old "to" space to the old "from" space.
     * This verifier can be used for both verification.
     */
    private final NoEvacuatedSpaceReferenceVerifier noFromSpaceReferencesVerifiers;

    /**
     * Verify that the FOT table is correctly setup.
     */
    private final FOTVerifier fotVerifier;

    private final EvacuationTimers evacTimers = new EvacuationTimers();

    private final TimeLogger timeLogger = new TimeLogger();

    private final PhaseLogger phaseLogger = new PhaseLogger();

    @HOSTED_ONLY
    public GenSSHeapScheme() {
        cardTableRSet = new CardTableRSet();
        AtomicBumpPointerAllocator<YoungSpaceRefiller> nurseryAllocator =
            new AtomicBumpPointerAllocator<YoungSpaceRefiller>(new YoungSpaceRefiller());
        CardSpaceAllocator<OldSpaceRefiller> tenuredAllocator =
            new CardSpaceAllocator<GenSSHeapScheme.OldSpaceRefiller>(new OldSpaceRefiller(), cardTableRSet);
        resizingPolicy = new GenSSHeapSizingPolicy();
        youngSpace = new ContiguousAllocatingSpace<AtomicBumpPointerAllocator<YoungSpaceRefiller>>(nurseryAllocator, "Young Generation");
        oldSpace = new ContiguousSemiSpace<CardSpaceAllocator<OldSpaceRefiller>>(tenuredAllocator, "Old Generation");
        youngSpaceEvacuator = new NoAgingNurseryEvacuator(youngSpace, oldSpace, cardTableRSet);
        oldSpaceEvacuator = new  EvacuatorToCardSpace(oldSpace.fromSpace, oldSpace, cardTableRSet);
        noFromSpaceReferencesVerifiers = new NoEvacuatedSpaceReferenceVerifier(cardTableRSet, youngSpace);
        fotVerifier = new FOTVerifier(cardTableRSet);
        genCollection = new GenCollection();
        youngSpaceEvacuator.setTimers(evacTimers);
        oldSpaceEvacuator.setTimers(evacTimers);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        cardTableRSet.initialize(phase);
        if (phase == MaxineVM.Phase.TERMINATING) {
            if (Heap.logGCTime()) {
                timeLogger.logPhaseTimes(-1,
                                evacTimers.get(ROOT_SCAN).getElapsedTime(),
                                evacTimers.get(BOOT_HEAP_SCAN).getElapsedTime(),
                                evacTimers.get(CODE_SCAN).getElapsedTime(),
                                evacTimers.get(RSET_SCAN).getElapsedTime(),
                                evacTimers.get(COPY).getElapsedTime(),
                                evacTimers.get(WEAK_REF).getElapsedTime());
                timeLogger.logGcTimes(-1, false,  evacTimers.get(TOTAL).getElapsedTime());
            }
        }
    }

    @Override
    public TimeLogger timeLogger() {
        return timeLogger;
    }

    @Override
    public PhaseLogger phaseLogger() {
        return phaseLogger;
    }

    @Override
    public boolean contains(Address address) {
        return oldSpace.contains(address) || youngSpace.contains(address);
    }

    @Override
    public boolean collectGarbage(Size requestedFreeSpace) {
        if ((requestedFreeSpace.isZero() && !DisableExplicitGC) || youngSpace.freeSpace().lessThan(requestedFreeSpace)) {
            if (!Heap.gcDisabled()) {
                genCollection.submit();
            }
        }
        return oldSpace.freeSpace().plus(youngSpace.freeSpace()).greaterThan(requestedFreeSpace);
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

    @Override
    protected void allocateHeapAndGCStorage() {
        final Size reservedSpace = Size.K.times(reservedVirtualSpaceKB());
        final Size initSize = Heap.initialSize();
        final Size maxSize = Heap.maxSize();
        final int pageSize = Platform.platform().pageSize;
        final int log2Alignment = Integer.numberOfTrailingZeros(pageSize);
        // Verify that the constraint of the heap scheme are met:
        FatalError.check(Heap.bootHeapRegion.start() ==
            Heap.startOfReservedVirtualSpace(),
            "Boot heap region must be mapped at start of reserved virtual space");

        final Address endOfCodeRegion = Code.getCodeManager().getRuntimeOptCodeRegion().end();
        final Address endOfReservedSpace = Heap.bootHeapRegion.start().plus(reservedSpace);
        final Address  firstUnusedByteAddress = endOfCodeRegion.alignUp(pageSize);
        try {
            // Use immortal memory for now.
            Heap.enableImmortalMemoryAllocation();
            resizingPolicy.initialize(initSize, maxSize, YoungGenHeapPercent, log2Alignment);
            youngSpace.initialize(firstUnusedByteAddress, resizingPolicy.maxYoungGenSize(), resizingPolicy.initialYoungGenSize());
            Address startOfOldSpace = youngSpace.space.end().alignUp(pageSize);
            oldSpace.initialize(startOfOldSpace, resizingPolicy.maxOldGenSize(), resizingPolicy.initialOldGenSize());
            /*
             * FIXME:
             * We set retireAfterEvacuation parameter to true. We allocate the entire old free space as evacuation LAB when doing a minor evacuation,
             * and retire the entire left over. This is necessary in order for the oldSpace.freeSpace to report the free space accurately independently
             * of the youngSpaceEvacuator (otherwise, we'd have to include the evacuator's ELAB in the calculation). It is also necessary to
             * retire the TLAB if we need mutators to allocate directly in the old gen.
             * This is rather complicated and we need to rethink the APIs here and how to share the evacuator.
              * An alternative would be to allocate an ELAB of size equal to the expected survivor space minus leftover in the current ELAB, but that isn't satisfying either.
            */
            youngSpaceEvacuator.initialize(2, oldSpace.freeSpace(), Size.fromInt(256), true);
            oldSpaceEvacuator.initialize(2, oldSpace.freeSpace(), Size.fromInt(256), true);
            initializeCoverage(firstUnusedByteAddress, oldSpace.highestAddress().minus(firstUnusedByteAddress).asSize());
            cardTableRSet.initializeXirStartupConstants();

            Address unusedReservedSpaceStart = cardTableRSet.memory().end().alignUp(pageSize);
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
            // Make the heap inspectable
            HeapScheme.Inspect.init(true);
            // HeapScheme.Inspect.notifyHeapRegions(youngSpace.space, oldSpace.space, oldSpace.fromSpace, cardTableRSet.memory());
        } finally {
            Heap.disableImmortalMemoryAllocation();
        }
    }

    @Override
    protected void reportTotalGCTimes() {
        // TODO Auto-generated method stub

    }

    /**
     * Allocate a chunk of memory of the specified size and refill a thread's TLAB with it.
     * @param etla the thread whose TLAB will be refilled
     * @param tlabSize the size of the chunk of memory used to refill the TLAB
     */
    private void allocateAndRefillTLAB(Pointer etla, Size tlabSize) {
        Pointer tlab = youngSpace.allocate(tlabSize);
        Size effectiveSize = tlabSize.minus(tlabHeadroom());
        refillTLAB(etla, tlab, effectiveSize);
    }

    @NEVER_INLINE
    @Override
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
        return ImmortalHeap.allocate(size, true);
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

    @HOSTED_ONLY
    @VMLoggerInterface(parent = HeapScheme.PhaseLogger.class)
    private interface PhaseLoggerInterface {
        void scanningThreadRoots(@VMLogParam(name = "vmThread") VmThread vmThread);
        void scanningRoots(@VMLogParam(name = "interval") Interval interval);
        void scanningBootHeap(@VMLogParam(name = "interval") Interval interval);
        void scanningCode(@VMLogParam(name = "interval") Interval interval);
        void scanningRSet(@VMLogParam(name = "interval") Interval interval);
        void evacuating(@VMLogParam(name = "interval") Interval interval);
        void processingSpecialReferences(@VMLogParam(name = "interval") Interval interval);
    }

    @HOSTED_ONLY
    @VMLoggerInterface(parent = HeapScheme.TimeLogger.class)
    private interface TimeLoggerInterface {
        void stackReferenceMapPreparationTime(
            @VMLogParam(name = "stackReferenceMapPreparationTime") long stackReferenceMapPreparationTime);

        void  gcTimes(
                        @VMLogParam(name = "invocationCount") int invocationCount,
                        @VMLogParam(name = "minorCollection") boolean minorCollection,
                        @VMLogParam(name = "gcTime") long gcTime
        );

        void phaseTimes(
                        @VMLogParam(name = "invocationCount") int invocationCount,
                        @VMLogParam(name = "rootScanTime") long rootScanTime,
                        @VMLogParam(name = "bootHeapScanTime") long bootHeapScanTime,
                        @VMLogParam(name = "codeScanTime") long codeScanTime,
                        @VMLogParam(name = "rsetTime") long rsetTime,
                        @VMLogParam(name = "evacTime") long evacTime,
                        @VMLogParam(name = "weakRefTime") long weakRefTime
        );
    }

    public  static final class PhaseLogger extends PhaseLoggerAuto {

        private static void tracePhase(String description, Interval interval) {
            Log.print(interval.name()); Log.print(": "); Log.println(description);
        }

        PhaseLogger() {
            super(null, null);
        }

        @Override
        protected void traceEvacuating(Interval interval) {
            tracePhase("Evacuating reachables", interval);
        }

        @Override
        protected void traceProcessingSpecialReferences(Interval interval) {
            tracePhase("Processing special references", interval);
        }

        @Override
        protected void traceScanningBootHeap(Interval interval) {
            tracePhase("Scanning boot heap", interval);
        }

        @Override
        protected void traceScanningCode(Interval interval) {
            tracePhase("Scanning code", interval);
        }

        @Override
        protected void traceScanningRSet(Interval interval) {
            tracePhase("Scanning remembered sets", interval);
        }

        @Override
        protected void traceScanningRoots(Interval interval) {
            tracePhase("Scanning roots", interval);
        }

        @Override
        protected void traceScanningThreadRoots(VmThread vmThread) {
            Log.print("Scanning thread local and stack roots for thread ");
            Log.printThread(vmThread, true);
        }

    }

    public static final class TimeLogger extends TimeLoggerAuto {
        private static final String HZ_SUFFIX = TimerUtil.getHzSuffix(HeapScheme.GC_TIMING_CLOCK);
        private static final String TIMINGS_LEAD = "Timings (" + HZ_SUFFIX + ") for ";

        TimeLogger() {
            super(null, null);
        }

        @Override
        protected void traceStackReferenceMapPreparationTime(long stackReferenceMapPreparationTime) {
            Log.print("Stack reference map preparation time: ");
            Log.print(stackReferenceMapPreparationTime);
            Log.println(HZ_SUFFIX);
        }

        @Override
        protected void tracePhaseTimes(int invocationCount,  long rootScanTime, long bootHeapScanTime, long codeScanTime, long rsetTime, long evacTime, long weakRefTime) {
            Log.print(TIMINGS_LEAD);
            if (invocationCount < 0) {
                Log.print("all GC");
            } else {
                Log.print(" GC #");
                Log.print(invocationCount);
            }
            Log.print(", root scan=");
            Log.print(rootScanTime);
            Log.print(", boot heap scan=");
            Log.print(bootHeapScanTime);
            Log.print(", code scan=");
            Log.print(codeScanTime);
            Log.print(", remembered set scan=");
            Log.print(rsetTime);
            Log.print(", copy=");
            Log.print(evacTime);
            Log.print(", weak refs=");
            Log.println(weakRefTime);
        }

        @Override
        protected void traceGcTimes(int invocationCount, boolean minorCollection, long gcTime) {
            Log.print(TIMINGS_LEAD);
            if (invocationCount < 0) {
                Log.print("all GC");
            } else {
                Log.print(" GC #");
                Log.print(invocationCount);
                if (!minorCollection) {
                    Log.print(" (Full) ");
                }
            }
            Log.print(" total=");
            Log.println(gcTime);
        }
    }

// START GENERATED CODE
    private static abstract class PhaseLoggerAuto extends com.sun.max.vm.heap.HeapScheme.PhaseLogger {
        public enum Operation {
            Evacuating, ProcessingSpecialReferences, ScanningBootHeap,
            ScanningCode, ScanningRSet, ScanningRoots, ScanningThreadRoots;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected PhaseLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logEvacuating(Interval interval) {
            log(Operation.Evacuating.ordinal(), intervalArg(interval));
        }
        protected abstract void traceEvacuating(Interval interval);

        @INLINE
        public final void logProcessingSpecialReferences(Interval interval) {
            log(Operation.ProcessingSpecialReferences.ordinal(), intervalArg(interval));
        }
        protected abstract void traceProcessingSpecialReferences(Interval interval);

        @INLINE
        public final void logScanningBootHeap(Interval interval) {
            log(Operation.ScanningBootHeap.ordinal(), intervalArg(interval));
        }
        protected abstract void traceScanningBootHeap(Interval interval);

        @INLINE
        public final void logScanningCode(Interval interval) {
            log(Operation.ScanningCode.ordinal(), intervalArg(interval));
        }
        protected abstract void traceScanningCode(Interval interval);

        @INLINE
        public final void logScanningRSet(Interval interval) {
            log(Operation.ScanningRSet.ordinal(), intervalArg(interval));
        }
        protected abstract void traceScanningRSet(Interval interval);

        @INLINE
        public final void logScanningRoots(Interval interval) {
            log(Operation.ScanningRoots.ordinal(), intervalArg(interval));
        }
        protected abstract void traceScanningRoots(Interval interval);

        @Override
        @INLINE
        public final void logScanningThreadRoots(VmThread vmThread) {
            log(Operation.ScanningThreadRoots.ordinal(), vmThreadArg(vmThread));
        }
        protected abstract void traceScanningThreadRoots(VmThread vmThread);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //Evacuating
                    traceEvacuating(toInterval(r, 1));
                    break;
                }
                case 1: { //ProcessingSpecialReferences
                    traceProcessingSpecialReferences(toInterval(r, 1));
                    break;
                }
                case 2: { //ScanningBootHeap
                    traceScanningBootHeap(toInterval(r, 1));
                    break;
                }
                case 3: { //ScanningCode
                    traceScanningCode(toInterval(r, 1));
                    break;
                }
                case 4: { //ScanningRSet
                    traceScanningRSet(toInterval(r, 1));
                    break;
                }
                case 5: { //ScanningRoots
                    traceScanningRoots(toInterval(r, 1));
                    break;
                }
                case 6: { //ScanningThreadRoots
                    traceScanningThreadRoots(toVmThread(r, 1));
                    break;
                }
            }
        }
    }

    private static abstract class TimeLoggerAuto extends com.sun.max.vm.heap.HeapScheme.TimeLogger {
        public enum Operation {
            GcTimes, PhaseTimes, StackReferenceMapPreparationTime;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected TimeLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logGcTimes(int invocationCount, boolean minorCollection, long gcTime) {
            log(Operation.GcTimes.ordinal(), intArg(invocationCount), booleanArg(minorCollection), longArg(gcTime));
        }
        protected abstract void traceGcTimes(int invocationCount, boolean minorCollection, long gcTime);

        @INLINE
        public final void logPhaseTimes(int invocationCount, long rootScanTime, long bootHeapScanTime, long codeScanTime, long rsetTime,
                long evacTime, long weakRefTime) {
            log(Operation.PhaseTimes.ordinal(), intArg(invocationCount), longArg(rootScanTime), longArg(bootHeapScanTime), longArg(codeScanTime), longArg(rsetTime),
                longArg(evacTime), longArg(weakRefTime));
        }
        protected abstract void tracePhaseTimes(int invocationCount, long rootScanTime, long bootHeapScanTime, long codeScanTime, long rsetTime,
                long evacTime, long weakRefTime);

        @Override
        @INLINE
        public final void logStackReferenceMapPreparationTime(long stackReferenceMapPreparationTime) {
            log(Operation.StackReferenceMapPreparationTime.ordinal(), longArg(stackReferenceMapPreparationTime));
        }
        protected abstract void traceStackReferenceMapPreparationTime(long stackReferenceMapPreparationTime);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //GcTimes
                    traceGcTimes(toInt(r, 1), toBoolean(r, 2), toLong(r, 3));
                    break;
                }
                case 1: { //PhaseTimes
                    tracePhaseTimes(toInt(r, 1), toLong(r, 2), toLong(r, 3), toLong(r, 4), toLong(r, 5), toLong(r, 6), toLong(r, 7));
                    break;
                }
                case 2: { //StackReferenceMapPreparationTime
                    traceStackReferenceMapPreparationTime(toLong(r, 1));
                    break;
                }
            }
        }
    }

// END GENERATED CODE

}
