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

import static com.sun.max.vm.MaxineVM.Phase.*;
import static com.sun.max.vm.heap.gcx.EvacuationTimers.TIMED_OPERATION.*;

import java.lang.management.*;

import com.sun.cri.xir.*;
import com.sun.cri.xir.CiXirAssembler.XirOperand;
import com.sun.management.GarbageCollectorMXBean;
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
import com.sun.max.vm.heap.Heap.GCCallbackPhase;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.heap.gcx.rset.*;
import com.sun.max.vm.heap.gcx.rset.ctbl.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.management.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.ti.*;
/**
 * A heap scheme implementing a two-generations heap, where each generation implements a semi-space collector.
 *
 */
public final class GenSSHeapScheme extends HeapSchemeWithTLABAdaptor implements XirWriteBarrierSpecification, RSetCoverage, EvacuationBufferProvider {
    /**
     * Knob for the fixed ratio resizing policy.
     */
    static int YoungGenHeapPercent = 30;
    /**
     * Expected default percentage of survivors. Used to estimate old generation growth at minor collection and decide when to trigger a full GC.
     * Default value is arbitrary at the moment.
     */
    static private int minSurvivingPercent = 15;

    static {
        VMOptions.addFieldOption("-XX:", "YoungGenHeapPercent", GenSSHeapScheme.class, "Fixed percentage of heap size that must be used by young gen", Phase.PRISTINE);
    }

    /**
     * Refiller for the OldSpace allocator.
     * The old space allocator is primarily used for promoting objects from the young space at minor collection.
     * Large objects that doesn't fit in the young generation may be allocated directly in the old space as well.
     * Refill to the old space only happen in exceptional cases. If refill is triggered by a failed large object allocation, it means the old space is exhausted
     * and a full GC is warranted.  All other refill occurrences take place during GC. If a refill occurs during a minor collection, it means that the
     * estimation of surviving objects was incorrect and there isn't enough space in the to-space of the old generation to allocate all survivors.
     * In this case, we overflow over to the old generation's from-space, and mark down for an immediate full GC. The overflow into from space
     * will be considered live and root of full GC collection.
     * Overflow during full GC may subsequently occur (e.g., because minor collection overflow generate more live data than the current semi-space size).
     * There two sub-cases: there enough room to grow the current semi-space to absorb the overflow, in which case the heap is resized. Or there isn't enough space.
     * In this case, we overflow into the young generation and raise a OOM.
     *
     */
    final class OldSpaceRefiller extends Refiller {
        @Override
        public Address allocateRefill(Pointer startOfSpaceLeft, Size spaceLeft) {
            // Force full collection.
            Heap.collectGarbage(Size.zero());
            // The current thread hold the refill lock and will do the refill of the allocator.
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
     * VM Operation implementing a generational collection. Delegates to the GenSSHeapScheme.
     */
    final class GenCollection extends GCOperation {
        GenCollection() {
            super("GenCollection");
        }

        @Override
        protected void collect(int invocationCount) {
            doCollect(invocationCount);
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
    @INSPECTED
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

    private final Evacuator.PhaseLogger phaseLogger = new Evacuator.PhaseLogger();

    /**
     * Support for {@link #maxObjectInspectionAge()}.
     * Keeps track of last time a full GC completed.
     */
    private long lastFullGCTime = 0L;


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
        youngSpaceEvacuator = new NoAgingNurseryEvacuator(youngSpace, oldSpace, this, cardTableRSet, "Young");
        oldSpaceEvacuator = new  EvacuatorToCardSpace(oldSpace.fromSpace, oldSpace, this, cardTableRSet, "Old");
        noFromSpaceReferencesVerifiers = new NoEvacuatedSpaceReferenceVerifier(cardTableRSet, youngSpace);
        fotVerifier = new FOTVerifier(cardTableRSet);
        genCollection = new GenCollection();
        youngSpaceEvacuator.setTimers(evacTimers);
        oldSpaceEvacuator.setTimers(evacTimers);
        youngSpaceEvacuator.setPhaseLogger(phaseLogger);
        oldSpaceEvacuator.setPhaseLogger(phaseLogger);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        cardTableRSet.initialize(phase);
        if (phase == PRISTINE) {
            lastFullGCTime = System.currentTimeMillis();
        }
        if (phase == TERMINATING) {
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
        VMTI.handler().beginGC();
        if ((requestedFreeSpace.isZero() && !DisableExplicitGC) || youngSpace.freeSpace().lessThan(requestedFreeSpace)) {
            if (!Heap.gcDisabled()) {
                genCollection.submit();
            }
        }
        final boolean result = oldSpace.freeSpace().plus(youngSpace.freeSpace()).greaterThan(requestedFreeSpace);
        VMTI.handler().endGC();
        if (resizingPolicy.outOfMemory()) {
            throw new OutOfMemoryError();
        }
        return result;
    }
    private void verifyAfterMinorCollection() {
        // Verify that:
        // 1. offset table is correctly setup
        // 2. there are no pointer from old to young.
        oldSpace.visit(fotVerifier);
        noFromSpaceReferencesVerifiers.setEvacuatedSpace(youngSpace);
        if (resizingPolicy.minorEvacuationOverflow()) {
            // Have to visit both the old gen's to space and the overflow in the old gen from space (i.e., the bound of the oldSpace's allocator.
            final ContiguousHeapSpace oldToSpace = oldSpace.space;
            final BaseAtomicBumpPointerAllocator oldSpaceAllocator = oldSpace.allocator;
            noFromSpaceReferencesVerifiers.visitCells(oldToSpace.start(), oldToSpace.committedEnd());
            noFromSpaceReferencesVerifiers.visitCells(oldSpaceAllocator.start(), oldSpaceAllocator.unsafeTop());
        } else {
            oldSpace.visit(noFromSpaceReferencesVerifiers);
        }
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
        // Gather information needed in case we overflowed.
        // We need these because the flipSpace method refill the allocator from the start of the to-space.
        final boolean minorEvacuationOverflow = resizingPolicy.minorEvacuationOverflow();
        final Address oldAllocatorTop =  oldSpace.allocator.unsafeTop();
        oldSpace.flipSpaces();
        if (minorEvacuationOverflow) {
            final Address startRange =  oldSpace.allocator.start();
            resizingPolicy.notifyMinorEvacuationOverflowRange(startRange, oldAllocatorTop);
            oldSpace.allocator.unsafeSetTop(oldAllocatorTop);
            oldSpaceEvacuator.prefillSurvivorRanges(startRange, oldAllocatorTop);
        }
        oldSpaceEvacuator.setGCOperation(genCollection);
        oldSpaceEvacuator.setEvacuationSpace(oldSpace.fromSpace, oldSpace);
        oldSpaceEvacuator.evacuate(Heap.logGCPhases());
        final CardFirstObjectTable fot = cardTableRSet.cfoTable;
        final int startIndex = fot.tableEntryIndex(oldSpace.fromSpace.start());
        final int endIndex = fot.tableEntryIndex(oldSpace.fromSpace.committedEnd());
        fot.clear(startIndex, endIndex);
        cardTableRSet.cardTable.clean(startIndex, endIndex);
        youngSpaceEvacuator.doAfterGC();
        oldSpaceEvacuator.setGCOperation(null);
    }

    @Override
    public Address refillEvacuationBuffer() {
        final CardSpaceAllocator<OldSpaceRefiller> allocator = oldSpace.allocator();
        Size spaceLeft = allocator.freeSpace();
        Address startOfSpaceLeft = allocator.unsafeSetTopToLimit();
        FatalError.check(VmThread.current().isVmOperationThread(), "must only be called by VmOperation");
        // First, make sure we're doing minor collection here.
        if (youngSpaceEvacuator.getGCOperation() != null) {
            FatalError.check(!resizingPolicy.minorEvacuationOverflow(), "Must not have recursive overflow of old space during minor collection");
            if (youngSpaceEvacuator.evacuatedBytes().isNotZero()) {
                // This is not a refill before minor evacuation start, but an overflow situation.
                // Refill using the from space.
                final ContiguousHeapSpace fromSpace = oldSpace.fromSpace;
                // Left-over in allocator is not formated.
                fillWithDeadObject(startOfSpaceLeft, allocator.hardLimit());
                // Notify that we need to run a full GC immediately after this overflowing minor collection.
                resizingPolicy.notifyMinorEvacuationOverflow();
                // Refill the allocator with the old from space.
                spaceLeft = fromSpace.committedSize();
                allocator.refill(fromSpace.start(), spaceLeft);
                startOfSpaceLeft = allocator.unsafeSetTopToLimit();
            }
            HeapFreeChunk.format(startOfSpaceLeft, spaceLeft);
            return startOfSpaceLeft;
        } else if (oldSpaceEvacuator.getGCOperation() != null) {
            if (oldSpaceEvacuator.evacuatedBytes().isZero()) {
                // We haven't started evacuating.
                HeapFreeChunk.format(startOfSpaceLeft, spaceLeft);
                return startOfSpaceLeft;
            }
            // Try growing the heap (mostly the old space)
            if (resizingPolicy.canIncreaseSizeDuringFullGC(youngSpaceEvacuator.evacuatedBytes(), spaceLeft)) {
                final ContiguousHeapSpace space = oldSpace.space;
                resize(youngSpace, resizingPolicy.youngGenSize());
                resize(oldSpace, resizingPolicy.oldGenSize());
                final Address endOfRefill = space.committedEnd();
                final Address startOfRefill = allocator.unsafeSetTopToLimit();
                FatalError.check(startOfSpaceLeft.plus(spaceLeft).equals(startOfRefill), "");
                HeapFreeChunk.format(startOfSpaceLeft, endOfRefill.minus(startOfSpaceLeft).asSize());
                return startOfSpaceLeft;
            }
            // Need to refill old gen allocator with young gen space.
            resizingPolicy.notifyOutOfMemory();
            oldSpace.allocator.refill(youngSpace.space.start(), youngSpace.space.committedSize());
            FatalError.unimplemented();
        } else {
            FatalError.unexpected("Shouldn't refill evacuation buffer outside of GC operations");
        }
        return Address.zero();
    }

    @Override
    public void retireEvacuationBuffer(Address startOfSpaceLeft, Address endOfSpaceLeft) {
        oldSpace.allocator().retireTop(startOfSpaceLeft, endOfSpaceLeft.minus(startOfSpaceLeft).asSize());
    }

    private Size estimatedNextEvac() {
        final Size min = youngSpace.totalSpace().dividedBy(100).times(minSurvivingPercent);
        final Size lastSurvivorCount = youngSpaceEvacuator.evacuatedBytes();
        return lastSurvivorCount.greaterThan(min) ? lastSurvivorCount : min;
    }

    private void resize(HeapSpace space, Size newSize) {
        if (newSize.lessThan(space.totalSpace())) {
            Size delta = space.totalSpace().minus(newSize);
            space.decreaseSize(delta);
        } else if (newSize.greaterThan(space.totalSpace())) {
            Size delta = newSize.minus(space.totalSpace());
            space.increaseSize(delta);
        }
    }

    /**
     * Implement logic for garbage collecting at safetpoint.
     * Always start with a minor collection.
     * If space left after the minor collection in the old generation is less than estimated space for survivors of the next minor collection,
     * a full GC is performed immediately.
     * It is possible for the minor collection to overflow the old generation because of under-estimated survivor space at the last minor collection.
     * This is caught by the refiller of the old generation allocator, which in this case allocate space directly in the second semi-space.
     */
    private void doCollect(int invocationCount) {
        evacTimers.resetTrackTime();

        VmThreadMap.ACTIVE.forAllThreadLocals(null, tlabFiller);
        Heap.invokeGCCallbacks(GCCallbackPhase.BEFORE);
        if (MaxineVM.isDebug() && Heap.verbose()) {
            Log.println("--Begin nursery evacuation");
        }
        final long startGCTime = System.currentTimeMillis();
        evacTimers.start(TOTAL);
        youngSpaceEvacuator.setGCOperation(genCollection);
        youngSpaceEvacuator.evacuate(Heap.logGCPhases());
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
            }
            evacTimers.stop(TOTAL);
            lastFullGCTime = System.currentTimeMillis() - lastFullGCTime;

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
        accumulatedGCTime = System.currentTimeMillis() - startGCTime;
        Heap.invokeGCCallbacks(GCCallbackPhase.AFTER);
        HeapScheme.Inspect.notifyHeapPhaseChange(HeapPhase.MUTATING);
    }

    @Override
    public Size reportFreeSpace() {
        return oldSpace.freeSpace().plus(youngSpace.freeSpace());
    }

    @Override
    public Size reportUsedSpace() {
        return oldSpace.usedSpace().plus(youngSpace.usedSpace());
    }

    @Override
    public boolean pin(Object object) {
        return false;
    }

    @Override
    public void unpin(Object object) {
        throw new UnsupportedOperationException();
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
    public long maxObjectInspectionAge() {
        return lastFullGCTime;
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
        final Address immortalStart = endOfCodeRegion.alignUp(pageSize);
        // Relocate immortal memory immediately after the end of the code region.
        ImmortalMemoryRegion immortalRegion = ImmortalHeap.getImmortalHeap();
        FatalError.check(immortalRegion.used().isZero(), "Immortal heap must be unused");
        VirtualMemory.deallocate(immortalRegion.start(), immortalRegion.size(), VirtualMemory.Type.HEAP);
        immortalRegion.setStart(immortalStart);
        immortalRegion.mark.set(immortalStart);
        final Address firstUnusedByteAddress = immortalRegion.end();

        try {
            // Use immortal memory for now.
            Heap.enableImmortalMemoryAllocation();
            resizingPolicy.initialize(initSize, maxSize, YoungGenHeapPercent, log2Alignment);
            youngSpace.initialize(firstUnusedByteAddress, resizingPolicy.maxYoungGenSize(), resizingPolicy.initialYoungGenSize());
            Address startOfOldSpace = youngSpace.space.end().alignUp(pageSize);
            oldSpace.initialize(startOfOldSpace, resizingPolicy.maxOldGenSize(), resizingPolicy.initialOldGenSize());
            initializeCoverage(firstUnusedByteAddress, oldSpace.highestAddress().minus(firstUnusedByteAddress).asSize());
            cardTableRSet.initializeXirStartupConstants();

            /*
             * The evacuators include their own local allocation buffer, refilled via the EvacuationBufferProvider interface implemented by the GenSSHeapScheme.
             * The evacuators are initialized with the retireAfterEvacuation parameter to true. This allows the GenSSHeapScheme to
             * use the entire old free space as evacuation allocation buffer (EAB)  when doing a minor evacuation,
             * and retire the entire left over after the evacuation. Setting retireAfterEvacuation guarantees that the evacuators (especially the oldSpaceEvacuators)
             * doesn't keep it's evacuation buffers across minor collections. This is necessery for two reasons:
             * (i) so that oldSpace.freeSpace() reports the free space accurately independently
             * of the youngSpaceEvacuator (otherwise, we'd have to include the evacuator's EAB in the calculation);
             * (2) if we need mutators to allocate directly in the old gen, then evacuation buffer need to be retired so the old gen doesn't look full (otherwise, we
             * have to make allocation routine aware of the caching performed by the old space evacuator, which would be really messy and prevent reuse of
             * allocators across heap scheme.
             *
             * We also set the alwaysRefill argument to true: this makes refilling not conditional to space left over in the private evacuation buffer.
             * Since the evacuation buffer is refilled before evacuation with the entire free space, the first allocation failure correspond to an overflow situation
             */
            youngSpaceEvacuator.initialize(2, true, Size.zero(), true);
            oldSpaceEvacuator.initialize(2, true, Size.zero(), true);

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

    @Override
    protected boolean logTLABEvents(Address tlabStart) {
        return MaxineVM.isDebug() && TLABLog.TraceTLABAllocation;
    }

    /**
     * Allocate a chunk of memory of the specified size and refill a thread's TLAB with it.
     * @param etla the thread whose TLAB will be refilled
     * @param tlabSize the size of the chunk of memory used to refill the TLAB
     */
    private void allocateAndRefillTLAB(Pointer etla, Size tlabSize) {
        Pointer tlab = youngSpace.allocate(tlabSize);
        if (MaxineVM.isDebug() && logTLABEvents(tlab)) {
            TLABLog.doOnRefillTLAB(etla, tlabSize, true);
        }
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

    @Override
    public GarbageCollectorMXBean getGarbageCollectorMXBean() {
      return new GenSSGarbageCollectorMXBean();
    }

    private final class GenSSGarbageCollectorMXBean extends HeapSchemeAdaptor.GarbageCollectorMXBeanAdaptor {
        private GenSSGarbageCollectorMXBean() {
            super("GenSS");
            add(new GenSSMemoryPoolMXBean(oldSpace.space, this));
            add(new GenSSMemoryPoolMXBean(oldSpace.fromSpace, this));
            add(new GenSSMemoryPoolMXBean(youngSpace.space, this));
        }
    }

    private final class GenSSMemoryPoolMXBean extends MemoryPoolMXBeanAdaptor {
        GenSSMemoryPoolMXBean(MemoryRegion region, MemoryManagerMXBean manager) {
            super(MemoryType.HEAP, region, manager);
        }
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
