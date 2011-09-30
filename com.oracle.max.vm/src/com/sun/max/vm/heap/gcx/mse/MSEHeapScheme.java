/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

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
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Region-based Mark Sweep + Evacuation-based defragmentation Heap Scheme.
 * Used for testing region-based support.
 */
public class MSEHeapScheme extends HeapSchemeWithTLAB {
    /**
     * Number of heap words covered by a single mark.
     */
    private static final int WORDS_COVERED_PER_BIT = 1;
    static boolean VerifyAfterGC = false;
    static {
        VMOptions.addFieldOption("-XX:", "VerifyAfterGC", MSEHeapScheme.class, "Verify heap after GC", Phase.PRISTINE);
    }
   /**
     * Size to reserve at the end of a TLABs to guarantee that a dead object can always be
     * appended to a TLAB to fill unused space before a TLAB refill.
     * The headroom is used to compute a soft limit that'll be used as the tlab's top.
     */
    @CONSTANT_WHEN_NOT_ZERO
    static Size TLAB_HEADROOM;

    private static void fillTLABWithDeadObject(Pointer tlabAllocationMark, Pointer tlabEnd) {
        // Need to plant a dead object in the leftover to make the heap parseable (required for sweeping).
        Pointer hardLimit = tlabEnd.plus(TLAB_HEADROOM);
        if (tlabAllocationMark.greaterThan(tlabEnd)) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("TLAB_MARK = ");
            Log.print(tlabAllocationMark);
            Log.print(", TLAB end = ");
            Log.println(tlabEnd);
            FatalError.check(hardLimit.equals(tlabAllocationMark), "TLAB allocation mark cannot be greater than TLAB End");
            Log.unlock(lockDisabledSafepoints);
            return;
        }
        fillWithDeadObject(tlabAllocationMark, hardLimit);
    }

    /**
     * Marking algorithm used to trace the heap.
     */
    final TricolorHeapMarker heapMarker;

    /**
     * Space where objects are allocated from by default.
     */
    final FirstFitMarkSweepHeap theHeap;

    private final Collect collect = new Collect();

    final AfterMarkSweepVerifier afterGCVerifier;

    // For debugging purposes only.
    final private AtomicPinnedCounter pinnedCounter;

    /**
     * The application heap. Currently, where all dynamic allocation takes place.
     */

    @HOSTED_ONLY
    public MSEHeapScheme() {
        theHeap = new FirstFitMarkSweepHeap();
        heapMarker = new TricolorHeapMarker(WORDS_COVERED_PER_BIT, new HeapAccounRootCellVisitor(theHeap));
        afterGCVerifier = new AfterMarkSweepVerifier(heapMarker, theHeap, AfterMarkSweepBootHeapVerifier.makeVerifier(heapMarker, theHeap));
        pinningSupportFlags = PIN_SUPPORT_FLAG.makePinSupportFlags(true, false, true);
        pinnedCounter = MaxineVM.isDebug() ? new AtomicPinnedCounter() : null;
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isHosted() && phase == MaxineVM.Phase.BOOTSTRAPPING) {
            // VM-generation time initialization.
            TLAB_HEADROOM = MIN_OBJECT_SIZE;
            AtomicBumpPointerAllocator.hostInitialize();
            if (MaxineVM.isDebug()) {
                AtomicPinnedCounter.hostInitialize();
            }
        } else if (phase == MaxineVM.Phase.PRISTINE) {
            allocateHeapAndGCStorage();
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            if (Heap.traceGCTime()) {
                collect.reportTotalGCTimes();
            }
        }

    }

    /**
     * Allocate memory for both the heap and the GC's data structures (mark bitmaps, marking stacks, etc.).
     */
    private void allocateHeapAndGCStorage() {
        final Size reservedSpace = Size.K.times(reservedVirtualSpaceKB());
        final Size initSize = Heap.initialSize();
        final Size maxSize = Heap.maxSize();
        final int pageSize = Platform.platform().pageSize;

        // Verify that the constraint of the heap scheme are met:
        FatalError.check(Heap.bootHeapRegion.start() == Heap.startOfReservedVirtualSpace(),
            "Boot heap region must be mapped at start of reserved virtual space");

        final Address endOfCodeRegion = Code.getCodeManager().getRuntimeCodeRegion().end();
        final Address endOfReservedSpace = Heap.bootHeapRegion.start().plus(reservedSpace);

        // Initialize the heap region manager.
        final Address  firstUnusedByteAddress = endOfCodeRegion;

        theHeapRegionManager().initialize(firstUnusedByteAddress, maxSize, HeapRegionInfo.class);
        try {
            enableCustomAllocation(theHeapRegionManager().bootAllocator());
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

            theHeap.initialize(initSize, applicationHeapMaxSize);
            // FIXME (ld) We should uncommit what hasn't been committed yet!

            // Initialize the heap marker's data structures. Needs to make sure it is outside of the heap reserved space.
            if (!VirtualMemory.allocatePageAlignedAtFixedAddress(heapMarkerDataStart, heapMarkerDatasize,  VirtualMemory.Type.DATA)) {
                MaxineVM.reportPristineMemoryFailure("heap marker data", "allocate", heapMarkerDatasize);
            }

            heapMarker.initialize(heapBounds.start(), heapBounds.end(), heapMarkerDataStart, heapMarkerDatasize);

            // Free reserved space we will not be using.
            Size leftoverSize = endOfReservedSpace.minus(unusedReservedSpaceStart).asSize();

            // First, uncommit range we want to free (this will create a new mapping that can then be deallocated)
            if (!VirtualMemory.uncommitMemory(unusedReservedSpaceStart, leftoverSize,  VirtualMemory.Type.DATA)) {
                MaxineVM.reportPristineMemoryFailure("reserved space leftover", "uncommit", leftoverSize);
            }

            if (VirtualMemory.deallocate(unusedReservedSpaceStart, leftoverSize, VirtualMemory.Type.DATA).isZero()) {
                MaxineVM.reportPristineMemoryFailure("reserved space leftover", "deallocate", leftoverSize);
            }
        } finally {
            disableCustomAllocation();
        }
        theHeapRegionManager().verifyAfterInitialization(heapMarker);
    }

    @Override
    public int reservedVirtualSpaceKB() {
        // 2^30 Kb = 1 TB of reserved virtual space.
        // This will be truncated as soon as we taxed what we need at initialization time.
        return Size.G.toInt();
    }

    public boolean collectGarbage(Size requestedFreeSpace) {
        collect.requestedSize = requestedFreeSpace;
        boolean forcedGC = requestedFreeSpace.toInt() == 0;
        if (forcedGC) {
            collect.submit();
            return true;
        }
        // We may reach here after a race. Don't run GC if request can be satisfied.

        // TODO (ld) might be better to try allocate the requested space and save the result for the caller.
        // This may avoid starvation case where in concurrent threads allocate the requested space
        // in after this method returns but before the caller allocated the space..
        if (theHeap.canSatisfyAllocation(requestedFreeSpace)) {
            return true;
        }
        VmOperationThread.submit(collect);
        return theHeap.canSatisfyAllocation(requestedFreeSpace);
    }

    public boolean contains(Address address) {
        return  theHeapRegionManager().contains(address);
    }

    public boolean isGcThread(Thread thread) {
        return thread instanceof VmOperationThread;
    }

    public Size reportFreeSpace() {
        return theHeap.freeSpace();
    }

    public Size reportUsedSpace() {
        return theHeap.usedSpace();
    }

    @INLINE(override = true)
    public void writeBarrier(Reference from, Reference to) {
    }

    @INLINE(override = true)
    public boolean pin(Object object) {
        // Objects never relocate. So this is always safe.
        if (MaxineVM.isDebug()) {
            pinnedCounter.increment();
        }
        return true;
    }

    @INLINE(override = true)
    public void unpin(Object object) {
        if (MaxineVM.isDebug()) {
            pinnedCounter.decrement();
        }
    }

    @Override
    protected void doBeforeTLABRefill(Pointer tlabAllocationMark, Pointer tlabEnd) {
        fillTLABWithDeadObject(tlabAllocationMark, tlabEnd);
    }

    static class TLABFiller extends ResetTLAB {
        @Override
        protected void doBeforeReset(Pointer etla, Pointer tlabMark, Pointer tlabTop) {
            if (tlabMark.greaterThan(tlabTop)) {
                // Already filled-up (mark is at the limit).
                return;
            }
            // Before filling the current TLAB chunk, save link to next pointer.
            final Pointer nextChunk = tlabTop.getWord().asPointer();
            fillTLABWithDeadObject(tlabMark, tlabTop);
            // FIXME: we shouldn't have to do the following. Heap walker should be able to walk over HeapFreeChunk.
            HeapFreeChunk.makeParsable(nextChunk);
        }
    }

    @Override
    protected void tlabReset(Pointer tla) {
        collect.tlabFiller.run(tla);
    }


    /**
     * Class implementing the garbage collection routine.
     * This is the {@link VmOperationThread}'s entry point to garbage collection.
     */
    final class Collect extends GCOperation {
        private long collectionCount = 0;
        private TLABFiller tlabFiller = new TLABFiller();


        public Collect() {
            super("Collect");
        }

        private Size requestedSize;
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

        private void reportTotalGCTimes() {
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
        public void collect(int invocationCount) {
            traceGCTimes = Heap.traceGCTime();
            startTimer(totalPauseTime);
            VmThreadMap.ACTIVE.forAllThreadLocals(null, tlabFiller);

            HeapScheme.Inspect.notifyGCStarted();

            vmConfig().monitorScheme().beforeGarbageCollection();
            theHeap.doBeforeGC();

            collectionCount++;
            if (MaxineVM.isDebug() && Heap.traceGCPhases()) {
                Log.print("Begin mark-sweep #");
                Log.println(collectionCount);
            }

            theHeapRegionManager().checkOutgoingReferences();

            theHeap.mark(heapMarker);
            startTimer(reclaimTimer);
            theHeap.sweep(heapMarker);
           // Size freeSpaceAfterGC = theHeap.freeSpaceAfterSweep();
            stopTimer(reclaimTimer);
            if (VerifyAfterGC) {
                afterGCVerifier.run();
            }
            vmConfig().monitorScheme().afterGarbageCollection();

       /*     if (heapResizingPolicy.resizeAfterCollection(theHeap.totalSpace(), freeSpaceAfterGC, theHeap)) {
                // Heap was resized.
                // Update heapMarker's coveredArea.
                ContiguousHeapSpace markedSpace = theHeap.committedHeapSpace();
                heapMarker.setCoveredArea(markedSpace.start(), markedSpace.committedEnd());
            }*/
            if (MaxineVM.isDebug() && Heap.traceGCPhases()) {
                Log.print("End mark-sweep #");
                Log.println(collectionCount);
            }
            theHeap.doAfterGC();
            HeapScheme.Inspect.notifyGCCompleted();
            stopTimer(totalPauseTime);

            if (traceGCTimes) {
                reportLastGCTimes();
            }
        }
    }

    private Size setNextTLABChunk(Pointer chunk) {
        if (MaxineVM.isDebug()) {
            if (FirstFitMarkSweepHeap.DebugMSE) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("setNextTLABChunk(");
                Log.print(chunk);
                if (!chunk.isZero()) {
                    Log.print(" [");
                    Log.print(HeapFreeChunk.getFreechunkSize(chunk).toInt());
                    Log.print(" bytes ]");
                }
                Log.println(")");
                Log.unlock(lockDisabledSafepoints);
            }
            FatalError.check(!chunk.isZero(), "TLAB chunk must not be null");
            FatalError.check(HeapFreeChunk.getFreechunkSize(chunk).greaterEqual(theHeap.minReclaimableSpace()), "TLAB chunk must be greater than min reclaimable space");
        }
        Size chunkSize =  HeapFreeChunk.getFreechunkSize(chunk);
        Size effectiveSize = chunkSize.minus(TLAB_HEADROOM);
        Address nextChunk = HeapFreeChunk.getFreeChunkNext(chunk);
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
            return theHeap.allocate(size);
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

    /**
     * Allocate a chunk of memory of the specified size and refill a thread's TLAB with it.
     * @param etla the thread whose TLAB will be refilled
     * @param tlabSize the size of the chunk of memory used to refill the TLAB
     */
    private void allocateAndRefillTLAB(Pointer etla, Size tlabSize) {
        Pointer tlab = theHeap.allocateTLAB(tlabSize);
        Size effectiveSize = setNextTLABChunk(tlab);

        if (Heap.traceAllocation()) {
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


    @INTRINSIC(UNSAFE_CAST)
    private static native BaseAtomicBumpPointerAllocator asBumpPointerAllocator(Object object);

    @Override
    protected Pointer customAllocate(Pointer customAllocator, Size size, boolean adjustForDebugTag) {
        return asBumpPointerAllocator(Reference.fromOrigin(Layout.cellToOrigin(customAllocator)).toJava()).allocateCleared(size);
    }

    @Override
    protected Pointer handleTLABOverflow(Size size, Pointer etla, Pointer tlabMark, Pointer tlabEnd) {      // Should we refill the TLAB ?
        final TLABRefillPolicy refillPolicy = TLABRefillPolicy.getForCurrentThread(etla);
        if (refillPolicy == null) {
            // No policy yet for the current thread. This must be the first time this thread uses a TLAB (it does not have one yet).
            ProgramError.check(tlabMark.isZero(), "thread must not have a TLAB yet");
            if (!usesTLAB()) {
                // We're not using TLAB. So let's assign the never refill tlab policy.
                TLABRefillPolicy.setForCurrentThread(etla, NEVER_REFILL_TLAB);
                return theHeap.allocate(size);
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
            return theHeap.allocate(size);
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
                return theHeap.allocate(size);
            }
        }
        // Refill TLAB and allocate (we know the request can be satisfied with a fresh TLAB and will therefore succeed).
        allocateAndRefillTLAB(etla, nextTLABSize);
        return tlabAllocate(size);
    }

    @INLINE(override = true)
    @Override
    public boolean supportsTagging() {
        return false;
    }

    @Override
    protected void releaseUnusedReservedVirtualSpace() {
        // Do nothing. This heap scheme has its own way of doing this.
    }
}

