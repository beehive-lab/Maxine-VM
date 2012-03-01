/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx.mse;

import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.heap.gcx.HeapRegionManager.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.heap.gcx.rset.*;
import com.sun.max.vm.jvmti.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Region-based Mark Sweep + Evacuation-based defragmentation Heap Scheme.
 * Used for testing region-based support.
 */
public final class MSEHeapScheme extends HeapSchemeWithTLABAdaptor implements HeapAccountOwner {
    /**
     * Number of heap words covered by a single mark.
     */
    private static final int WORDS_COVERED_PER_BIT = 1;
    static boolean DumpFragStatsAfterGC = false;
    static boolean DumpFragStatsAtGCFailure = false;
    static boolean DoImpreciseSweep = false;
    static {
        VMOptions.addFieldOption("-XX:", "DumpFragStatsAfterGC", MSEHeapScheme.class, "Dump region fragmentation stats after GC", Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "DumpFragStatsAtGCFailure", MSEHeapScheme.class, "Dump region fragmentation when GC failed to reclaim enough space", Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "DoImpreciseSweep", MSEHeapScheme.class, "Control whether to do precise or imprecise sweep", Phase.PRISTINE);
    }

    /**
     * Marking algorithm used to trace the heap.
     */
    private final TricolorHeapMarker heapMarker;

    /**
     * Space where objects are allocated from by default.
     */
    private final FirstFitMarkSweepSpace<MSEHeapScheme> markSweepSpace;

    final MarkSweepCollection collect = new MarkSweepCollection();

    /**
     * An instance of an after mark sweep verifier to use for heap verification after a mark sweep.
     * @see Sweeper
     */
    final AfterMarkSweepVerifier afterGCVerifier;

    private HeapRegionStatistics fragmentationStats;

    /**
     * The application heap. Currently, where all dynamic allocation takes place.
     */

    @HOSTED_ONLY
    public MSEHeapScheme() {
        super();
        final HeapAccount<MSEHeapScheme> heapAccount = new HeapAccount<MSEHeapScheme>(this);

        final ChunkListAllocator<RegionChunkListRefillManager> tlabAllocator =
            new ChunkListAllocator<RegionChunkListRefillManager>(new RegionChunkListRefillManager());
        final AtomicBumpPointerAllocator<RegionOverflowAllocatorRefiller> overflowAllocator =
            new AtomicBumpPointerAllocator<RegionOverflowAllocatorRefiller>(new RegionOverflowAllocatorRefiller());
        markSweepSpace = new FirstFitMarkSweepSpace<MSEHeapScheme>(heapAccount, tlabAllocator, overflowAllocator, false, NullDeadSpaceListener.nullDeadSpaceListener(), 0);
        heapMarker = new TricolorHeapMarker(WORDS_COVERED_PER_BIT, new HeapAccounRootCellVisitor(this));
        afterGCVerifier = new AfterMarkSweepVerifier(heapMarker, markSweepSpace, AfterMarkSweepBootHeapVerifier.makeVerifier(heapMarker, this));
        pinningSupportFlags = PIN_SUPPORT_FLAG.makePinSupportFlags(true, false, true);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
    }

    @Override
    protected void reportTotalGCTimes() {
        collect.reportTotalGCTimes();
    }

    /**
     * Allocate memory for both the heap and the GC's data structures (mark bitmaps, marking stacks, etc.).
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

        theHeapRegionManager().initialize(firstUnusedByteAddress, endOfReservedSpace, maxSize, HeapRegionInfo.class, 0);
        // All reserved space (but the one used by the heap region manager) is now uncommitted.
        try {
            enableCustomAllocation(theHeapRegionManager().allocator());
            final MemoryRegion heapBounds = theHeapRegionManager().bounds();
            final Size applicationHeapMaxSize = heapBounds.size().minus(theHeapRegionManager().size());

            // Compute space needed by the heap marker. This is proportional to the size of the space traced by the heap marker.
            // The boot image isn't traced (it is assumed a permanent root of collection).
            final Size heapMarkerDatasize = heapMarker.memoryRequirement(heapBounds.size());

            // Heap Marker Data are allocated at end of the space reserved to the heap regions.
            final Address heapMarkerDataStart = heapBounds.end().roundedUpBy(pageSize);
            // Address to the first reserved byte unused by the heap scheme.
            final Address unusedReservedSpaceStart = heapMarkerDataStart.plus(heapMarkerDatasize).roundedUpBy(pageSize);

            if (!unusedReservedSpaceStart.greaterThan(Heap.startOfReservedVirtualSpace())) {
                MaxineVM.reportPristineMemoryFailure("heap marker data", "reserve", heapMarkerDatasize);
            }

            if (!markSweepSpace.heapAccount().open(HeapRegionConstants.numberOfRegions(applicationHeapMaxSize))) {
                FatalError.unexpected("Failed to create application heap");
            }

            markSweepSpace.initialize(initSize, applicationHeapMaxSize);
            if (!VirtualMemory.commitMemory(heapMarkerDataStart, heapMarkerDatasize,  VirtualMemory.Type.DATA)) {
                MaxineVM.reportPristineMemoryFailure("heapMarkerDataStart", "commit", heapMarkerDatasize);
            }
            heapMarker.initialize(heapBounds.start(), heapBounds.end(), heapMarkerDataStart, heapMarkerDatasize);

            if (DumpFragStatsAfterGC || DumpFragStatsAtGCFailure) {
                fragmentationStats = new HeapRegionStatistics(markSweepSpace.minReclaimableSpace());
            }
            // Free leftover of reserved space we will not be using.
            Size leftoverSize = endOfReservedSpace.minus(unusedReservedSpaceStart).asSize();
            if (VirtualMemory.deallocate(unusedReservedSpaceStart, leftoverSize, VirtualMemory.Type.DATA).isZero()) {
                MaxineVM.reportPristineMemoryFailure("reserved space leftover", "deallocate", leftoverSize);
            }
            // Make the heap inspectable
            HeapScheme.Inspect.init(false);
            HeapScheme.Inspect.notifyHeapRegions(heapBounds, heapMarker.memory());

        } finally {
            disableCustomAllocation();
        }
        theHeapRegionManager().checkOutgoingReferences();
    }

    private void reportFragmentationStats(boolean reclaimedEnoughSpace) {
        if (DumpFragStatsAfterGC || (!reclaimedEnoughSpace && DumpFragStatsAtGCFailure)) {
            fragmentationStats.reportStats(heapAccount());
        }
    }

    /**
     * Log of recent amount of allocated space since last GC.
     * For debugging.
     */
    private final long [] allocatedSinceLastGC = new long[16];
    private long usedSpaceAfterLastGC;

    public boolean collectGarbage(Size requestedFreeSpace) {
        final Size usedSpaceBefore = markSweepSpace.usedSpace();
        if (MaxineVM.isDebug()) {
            final int logCursor = (int) (collectionCount % 16);
            allocatedSinceLastGC[logCursor] = usedSpaceBefore.minus(usedSpaceAfterLastGC).toLong();
            if (logCursor == 15) {
                long c = collectionCount - 15;
                Log.print("#GC, bytes reclaimed: ");
                for (int i = 0; i <= 15; i++) {
                    Log.print(c + i); Log.print(' '); Log.print(allocatedSinceLastGC[i]); Log.print(' ');
                }
                Log.println();
            }
        }
        if (requestedFreeSpace.isZero()) {
            // This is a forced GC.
            collect.submit();
            reportFragmentationStats(true);
            return true;
        }
        collect.submit();
        if (MaxineVM.isDebug()) {
            usedSpaceAfterLastGC = markSweepSpace.usedSpace().toLong();
        }
        boolean result =  markSweepSpace.usedSpace().minus(usedSpaceBefore).greaterThan(requestedFreeSpace);
        reportFragmentationStats(result);
        return result;
    }

    public boolean contains(Address address) {
        return  theHeapRegionManager().contains(address);
    }

    public Size reportFreeSpace() {
        return markSweepSpace.freeSpace();
    }

    public Size reportUsedSpace() {
        return markSweepSpace.usedSpace();
    }

    @INLINE
    public void writeBarrier(Reference from, Reference to) {
    }

    /**
     * Class implementing the garbage collection routine.
     * This is the {@link VmOperationThread}'s entry point to garbage collection.
     */
    final class MarkSweepCollection extends GCOperation {
        public MarkSweepCollection() {
            super("MarkSweepCollection");
        }

        private final TimerMetric reclaimTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
        private final TimerMetric totalPauseTime = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));

        private boolean traceGCTimes = false;

        private void startTimer(Timer timer) {
            if (traceGCTimes) {
                timer.start();
            }
        }
        private void stopTimer(Timer timer) {
            if (traceGCTimes) {
                timer.stop();
            }
        }

        private void reportLastGCTimes() {
            final boolean lockDisabledSafepoints = Log.lock();
            heapMarker.reportLastElapsedTimes();
            Log.print(", sweeping=");
            Log.print(reclaimTimer.getLastElapsedTime());
            Log.print(", total=");
            Log.println(totalPauseTime.getLastElapsedTime());
            Log.unlock(lockDisabledSafepoints);
        }

        void reportTotalGCTimes() {
            final boolean lockDisabledSafepoints = Log.lock();
            heapMarker.reportTotalElapsedTimes();
            Log.print(", sweeping=");
            Log.print(reclaimTimer.getElapsedTime());
            Log.print(", total=");
            Log.println(totalPauseTime.getElapsedTime());
            Log.unlock(lockDisabledSafepoints);
        }

        private HeapResizingPolicy heapResizingPolicy = new HeapResizingPolicy();

        @Override
        protected void collect(int invocationCount) {
            final boolean traceGCPhases = Heap.logGCPhases();
            traceGCTimes = Heap.logGCTime();
            startTimer(totalPauseTime);
            VmThreadMap.ACTIVE.forAllThreadLocals(null, tlabFiller);

            JVMTI.event(JVMTIEvent.GARBAGE_COLLECTION_START);
            HeapScheme.Inspect.notifyHeapPhaseChange(HeapPhase.ANALYZING);

            vmConfig().monitorScheme().beforeGarbageCollection();
            markSweepSpace.doBeforeGC();
            collectionCount++;

            theHeapRegionManager().checkOutgoingReferences();

            markSweepSpace.mark(heapMarker);

            HeapScheme.Inspect.notifyHeapPhaseChange(HeapPhase.RECLAIMING);

            if (traceGCPhases) {
                Log.println("BEGIN: Sweeping");
            }
            startTimer(reclaimTimer);
            markSweepSpace.sweep(heapMarker, DoImpreciseSweep);
            Size freeSpaceAfterGC = markSweepSpace.freeSpace();
            stopTimer(reclaimTimer);
            if (traceGCPhases) {
                Log.println("END: Sweeping");
            }

            if (VerifyAfterGC) {
                afterGCVerifier.run();
            }
            vmConfig().monitorScheme().afterGarbageCollection();

            heapResizingPolicy.resizeAfterCollection(freeSpaceAfterGC, markSweepSpace);
            markSweepSpace.doAfterGC();

            JVMTI.event(JVMTIEvent.GARBAGE_COLLECTION_FINISH);
            HeapScheme.Inspect.notifyHeapPhaseChange(HeapPhase.ALLOCATING);
            stopTimer(totalPauseTime);

            if (traceGCTimes) {
                reportLastGCTimes();
            }
        }
    }

    private Size setNextTLABChunk(Pointer chunk) {
        if (MaxineVM.isDebug()) {
            FatalError.check(!chunk.isZero(), "TLAB chunk must not be null");
            FatalError.check(HeapFreeChunk.getFreechunkSize(chunk).greaterEqual(markSweepSpace.minReclaimableSpace()), "TLAB chunk must be greater than min reclaimable space");
        }
        Size chunkSize =  HeapFreeChunk.getFreechunkSize(chunk);
        Address nextChunk = HeapFreeChunk.getFreeChunkNext(chunk);
        Size effectiveSize = chunkSize.minus(TLAB_HEADROOM);
        // Zap chunk data to leave allocation area clean.
        Memory.clearWords(chunk, effectiveSize.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt());
        chunk.plus(effectiveSize).setWord(nextChunk);
        return effectiveSize;
    }

    @INLINE
    private Size setNextTLABChunk(Pointer etla, Pointer nextChunk) {
        Size nextChunkEffectiveSize = setNextTLABChunk(nextChunk);
        fastRefillTLAB(etla, nextChunk, nextChunkEffectiveSize);
        return nextChunkEffectiveSize;
    }

    /**
     * Check if changing TLAB chunks may satisfy the allocation request. If not, allocated directly from the underlying free space manager,
     * otherwise, refills the TLAB with the next TLAB chunk and allocated from it.
     *
     * @param etla Pointer to enabled VMThreadLocals
     * @param tlabMark current mark of the TLAB
     * @param tlabHardLimit soft end of the current TLAB
     * @param chunk next chunk of this TLAB
     * @param size requested amount of memory
     * @return a pointer to the allocated memory
     */
    private Pointer changeTLABChunkOrAllocate(Pointer etla, Pointer tlabMark, Pointer tlabHardLimit, Pointer chunk, Size size) {
        Size chunkSize =  HeapFreeChunk.getFreechunkSize(chunk);
        if (size.greaterThan(chunkSize.minus(MIN_OBJECT_SIZE)))  {
            // Don't bother with searching another TLAB chunk that fits. Allocate directly in the heap.
            return markSweepSpace.allocate(size);
        }
        // Otherwise, the chunk can accommodate the request AND
        // we'll have enough room left in the chunk to format a dead object or to store the next chunk pointer.
        Address nextChunk = HeapFreeChunk.getFreeChunkNext(chunk);
        fillWithDeadObject(tlabMark,  tlabHardLimit);
        Size effectiveSize = chunkSize.minus(TLAB_HEADROOM);
        // Zap chunk data to leave allocation area clean.
        Memory.clearWords(chunk, effectiveSize.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt());
        chunk.plus(effectiveSize).setWord(nextChunk);
        fastRefillTLAB(etla, chunk, effectiveSize);
        return tlabAllocate(size);
    }

    @Override
    protected boolean logTLABEvents(Address tlabStart) {
        return RegionTable.inDebuggedRegion(tlabStart);
    }

    /**
     * Allocate a chunk of memory of the specified size and refill a thread's TLAB with it.
     * @param etla the thread whose TLAB will be refilled
     * @param tlabSize the size of the chunk of memory used to refill the TLAB
     */
    protected void allocateAndRefillTLAB(Pointer etla, Size tlabSize) {
        Pointer tlab = markSweepSpace.allocateTLAB(tlabSize);
        if (MaxineVM.isDebug() && logTLABEvents(tlab)) {
            TLABLog.doOnRefillTLAB(etla, tlabSize, true);
        }
        Size effectiveSize = setNextTLABChunk(tlab);

        if (traceTLAB()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Size realTLABSize = effectiveSize.plus(TLAB_HEADROOM);
            Log.printCurrentThread(false);
            Log.print(": Allocated TLAB at ");
            Log.print(tlab);
            Log.print(" [TOP=");
            Log.print(tlab.plus(effectiveSize));
            Log.print(", end=");
            Log.print(tlab.plus(realTLABSize));
            Log.print(", size=");
            Log.print(realTLABSize.toInt());
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
        refillTLAB(etla, tlab, effectiveSize);
    }

    @Override
    protected Pointer customAllocate(Pointer customAllocator, Size size) {
        return BaseAtomicBumpPointerAllocator.asBumpPointerAllocator(Reference.fromOrigin(Layout.cellToOrigin(customAllocator)).toJava()).allocateCleared(size);
    }

    @Override
    @NEVER_INLINE
    protected Pointer handleTLABOverflow(Size size, Pointer etla, Pointer tlabMark, Pointer tlabEnd) {      // Should we refill the TLAB ?
        final TLABRefillPolicy refillPolicy = TLABRefillPolicy.getForCurrentThread(etla);
        if (refillPolicy == null) {
            // No policy yet for the current thread. This must be the first time this thread uses a TLAB (it does not have one yet).
            ProgramError.check(tlabMark.isZero(), "thread must not have a TLAB yet");
            if (!usesTLAB()) {
                // We're not using TLAB. So let's assign the never refill tlab policy.
                TLABRefillPolicy.setForCurrentThread(etla, NEVER_REFILL_TLAB);
                return markSweepSpace.allocate(size);
            }
            // Allocate an initial TLAB and a refill policy. For simplicity, this one is allocated from the TLAB (see comment below).
            final Size tlabSize = initialTlabSize();
            allocateAndRefillTLAB(etla, tlabSize);
            // Let's do a bit of dirty meta-circularity. The TLAB is refilled, and no-one except the current thread can use it.
            // So the tlab allocation is going to succeed here
            TLABRefillPolicy.setForCurrentThread(etla, new SimpleTLABRefillPolicy(tlabSize));
            // Now, address the initial request. Note that we may recurse down to handleTLABOverflow again here if the
            // request is larger than the TLAB size. However, this second call will succeed and allocate outside of the tlab.
            return tlabAllocate(size);
        }
        // FIXME (ld) Want to first test against size of next chunk of this TLAB (if any).
        final Size nextTLABSize = refillPolicy.nextTlabSize();
        if (size.greaterThan(nextTLABSize)) {
            // This couldn't be allocated in a TLAB, so go directly to direct allocation routine.
            return markSweepSpace.allocate(size);
        }
        // TLAB may have been wiped out by a previous direct allocation routine.
        if (!tlabEnd.isZero()) {
            final Pointer hardLimit = tlabEnd.plus(TLAB_HEADROOM);
            final Pointer nextChunk = tlabEnd.getWord().asPointer();

            final Pointer cell = tlabMark;
            if (cell.plus(size).equals(hardLimit)) {
                // Can actually fit the object in space left.
                // zero-fill the headroom we left.
                Memory.clearWords(tlabEnd, TLAB_HEADROOM.unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt());
                if (nextChunk.isZero()) {
                    // Zero-out TLAB top and mark.
                    fastRefillTLAB(etla, Pointer.zero(), Size.zero());
                } else {
                    // TLAB has another chunk of free space. Set it.
                    setNextTLABChunk(etla, nextChunk);
                }
                return cell;
            } else if (!(cell.equals(hardLimit) || nextChunk.isZero())) {
                // We have another chunk, and we're not to limit yet. So we may change of TLAB chunk to satisfy the request.
                return changeTLABChunkOrAllocate(etla, tlabMark, hardLimit, nextChunk, size);
            }

            if (!refillPolicy.shouldRefill(size, tlabMark)) {
                // Size would fit in a new tlab, but the policy says we shouldn't refill the tlab yet, so allocate directly in the heap.
                return markSweepSpace.allocate(size);
            }
        }
        if (MaxineVM.isDebug() && RegionTable.inDebuggedRegion(tlabMark)) {
            TLABLog.doOnRetireTLAB(etla);
        }
        // Refill TLAB and allocate (we know the request can be satisfied with a fresh TLAB and will therefore succeed).
        allocateAndRefillTLAB(etla, nextTLABSize);
        return tlabAllocate(size);
    }

    @Override
    public HeapAccount<MSEHeapScheme> heapAccount() {
        return markSweepSpace.heapAccount();
    }

}

