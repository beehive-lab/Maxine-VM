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
package com.sun.max.vm.heap.gcx.ms;

import static com.sun.max.vm.VMConfiguration.*;

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
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;

/**
 * A simple mark-sweep collector. Only used for testing / debugging
 * marking and sweeping algorithms.
 * Implements TLAB over a linked list of free chunk provided by an object space manager.
 *
 * @see FreeHeapSpaceManager.
 *
 * @author Laurent Daynes
 */
public class MSHeapScheme extends HeapSchemeWithTLAB {
    /**
     * Number of heap words covered by a single mark.
     */
    private static final int WORDS_COVERED_PER_BIT = 1;

    static boolean UseLOS = true;
    static {
        VMOptions.addFieldOption("-XX:", "UseLOS", MSHeapScheme.class, "Use a large object space", Phase.PRISTINE);
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
            FatalError.check(hardLimit.equals(tlabAllocationMark), "TLAB allocation mark cannot be greater than TLAB End");
            return;
        }
        fillWithDeadObject(tlabAllocationMark, hardLimit);
    }

    /**
     * A marking algorithm for the MSHeapScheme.
     */
    final TricolorHeapMarker heapMarker;

    /**
     * Space where objects are allocated from by default.
     * Implements the {@link Sweepable} interface to be notified by a sweeper of
     * free space.
     */
    final FreeHeapSpaceManager objectSpace;

    /**
     * Space where large object are allocated from if {@link MSHeapScheme#UseLOS} is true.
     * Implements the {@link Sweepable} interface to be notified by a sweeper of
     * free space.
     */
    final LargeObjectSpace largeObjectSpace;

    private final Collect collect = new Collect();

    final AfterMarkSweepVerifier afterGCVerifier;

    @HOSTED_ONLY
    public MSHeapScheme() {
        heapMarker = new TricolorHeapMarker(WORDS_COVERED_PER_BIT);
        objectSpace = new FreeHeapSpaceManager();
        largeObjectSpace = new LargeObjectSpace();
        afterGCVerifier = new AfterMarkSweepVerifier(heapMarker, objectSpace);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isHosted() && phase == MaxineVM.Phase.BOOTSTRAPPING) {
            // VM-generation time initialization.
            TLAB_HEADROOM = MIN_OBJECT_SIZE;
            LinearSpaceAllocator.hostInitialize();
        } else  if (phase == MaxineVM.Phase.PRISTINE) {
            allocateHeapAndGCStorage();
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            if (Heap.traceGCTime()) {
                collect.reportTotalGCTimes();
            }
        }

    }

    @HOSTED_ONLY
    @Override
    public CodeManager createCodeManager() {
        switch (Platform.platform().os) {
            case LINUX: {
                return new LowAddressCodeManager();
            }
            case MAXVE:
            case DARWIN:
            case SOLARIS: {
                return new FixedAddressCodeManager();
            }
            default: {
                FatalError.unimplemented();
                return null;
            }
        }
    }

    /**
     * Allocate memory for both the heap and the GC's data structures (mark bitmaps, marking stacks, etc.).
     */
    private void allocateHeapAndGCStorage() {
        final Size reservedSpace = Size.K.times(reservedVirtualSpaceSize());
        final Size initSize = Heap.initialSize();
        final Size maxSize = Heap.maxSize();
        final int pageSize = Platform.platform().pageSize;

        // Verify that the constraint of the heap scheme are met:
        FatalError.check(Heap.bootHeapRegion.start() == Heap.startOfReservedVirtualSpace(),
                        "Boot heap region must be mapped at start of reserved virtual space");

        final Address endOfBootCodeRegion = Code.bootCodeRegion().end().roundedUpBy(pageSize);
        final Address endOfCodeRegion = Code.getCodeManager().getRuntimeCodeRegion().end();
        final Address endOfReservedSpace = Heap.bootHeapRegion.start().plus(reservedSpace);

        final Address  heapLowerBound = endOfCodeRegion.greaterEqual(endOfBootCodeRegion) ? endOfCodeRegion : endOfBootCodeRegion;
        final Size heapMarkerDatasize = heapMarker.memoryRequirement(maxSize);


        final Address heapStart = heapLowerBound.roundedUpBy(pageSize);
        final Address heapMarkerDataStart = heapStart.plus(maxSize).roundedUpBy(pageSize);
        final Address leftoverStart = heapMarkerDataStart.plus(heapMarkerDatasize).roundedUpBy(pageSize);

        objectSpace.initialize(this, heapStart, initSize, maxSize);
        ContiguousHeapSpace markedSpace = objectSpace.committedHeapSpace();

        // Initialize the heap marker's data structures. Needs to make sure it is outside of the heap reserved space.

        if (!VirtualMemory.allocatePageAlignedAtFixedAddress(heapMarkerDataStart, heapMarkerDatasize,  VirtualMemory.Type.DATA)) {
            MaxineVM.reportPristineMemoryFailure("heap marker data", "allocate", heapMarkerDatasize);
        }

        heapMarker.initialize(markedSpace.start(), markedSpace.committedEnd(), heapMarkerDataStart, heapMarkerDatasize);

        // Free reserved space we will not be using.
        Size leftoverSize = endOfReservedSpace.minus(leftoverStart).asSize();

        // First, uncommit range we want to free (this will create a new mapping that can then be deallocated)
        if (!VirtualMemory.uncommitMemory(leftoverStart, leftoverSize,  VirtualMemory.Type.DATA)) {
            MaxineVM.reportPristineMemoryFailure("reserved space leftover", "uncommit", leftoverSize);
        }

        if (VirtualMemory.deallocate(leftoverStart, leftoverSize, VirtualMemory.Type.DATA).isZero()) {
            MaxineVM.reportPristineMemoryFailure("reserved space leftover", "deallocate", leftoverSize);
        }

        // From now on, we can allocate. The following does this because of the var-arg arguments.
        InspectableHeapInfo.init(true, markedSpace);
    }

    @Override
    public int reservedVirtualSpaceSize() {
        // 2^30 Kb = 1 TB of reserved virtual space.
        // This will be truncated as soon as we taxed what we need at initialization time.
        return Size.G.toInt();
    }

    @Override
    public BootRegionMappingConstraint bootRegionMappingConstraint() {
        return BootRegionMappingConstraint.AT_START;
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
        if (objectSpace.canSatisfyAllocation(requestedFreeSpace)) {
            return true;
        }
        VmOperationThread.submit(collect);
        return objectSpace.canSatisfyAllocation(requestedFreeSpace);
    }

    public boolean contains(Address address) {
        return objectSpace.committedHeapSpace().inCommittedSpace(address);
    }

    public boolean isGcThread(Thread thread) {
        return thread instanceof VmOperationThread;
    }

    @INLINE(override = true)
    public boolean isPinned(Object object) {
        return false;
    }

    @INLINE(override = true)
    public boolean pin(Object object) {
        return false; // no supported
    }

    public Size reportFreeSpace() {
        return objectSpace.freeSpaceLeft();
    }

    public Size reportUsedSpace() {
        return objectSpace.committedHeapSpace().committedSize().minus(reportFreeSpace());
    }

    public void runFinalization() {
    }

    @INLINE(override = true)
    public void unpin(Object object) {
    }

    @INLINE(override = true)
    public void writeBarrier(Reference from, Reference to) {
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
        private final TimerMetric weakRefTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
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
            Log.print(", weak refs=");
            Log.print(weakRefTimer.getLastElapsedTime());
            Log.print(", sweeping=");
            Log.print(reclaimTimer.getLastElapsedTime());
            Log.print(", total=");
            Log.println(totalPauseTime.getLastElapsedTime());
            Log.unlock(lockDisabledSafepoints);
        }

        private void reportTotalGCTimes() {
            final boolean lockDisabledSafepoints = Log.lock();
            heapMarker.reportTotalElapsedTimes();
            Log.print(", weak refs=");
            Log.print(weakRefTimer.getElapsedTime());
            Log.print(", sweeping=");
            Log.print(reclaimTimer.getElapsedTime());
            Log.print(", total=");
            Log.println(totalPauseTime.getElapsedTime());
            Log.unlock(lockDisabledSafepoints);
        }

        private Size reclaim() {
            objectSpace.beginSweep();
            heapMarker.sweep(objectSpace);
            return objectSpace.endSweep();
        }

        private HeapResizingPolicy heapResizingPolicy = new HeapResizingPolicy();

        @Override
        public void collect(int invocationCount) {
            traceGCTimes = Heap.traceGCTime();
            startTimer(totalPauseTime);
            VmThreadMap.ACTIVE.forAllThreadLocals(null, tlabFiller);

            HeapScheme.Inspect.notifyGCStarted();

            vmConfig().monitorScheme().beforeGarbageCollection();

            collectionCount++;
            if (MaxineVM.isDebug() && Heap.traceGCPhases()) {
                Log.print("Begin mark-sweep #");
                Log.println(collectionCount);
            }
            objectSpace.makeParsable();
            heapMarker.markAll();
            startTimer(weakRefTimer);
            SpecialReferenceManager.processDiscoveredSpecialReferences(heapMarker.getSpecialReferenceGC());
            stopTimer(weakRefTimer);
            startTimer(reclaimTimer);
            Size freeSpaceAfterGC = reclaim();
            stopTimer(reclaimTimer);
            if (MaxineVM.isDebug()) {
                afterGCVerifier.run();
            }
            vmConfig().monitorScheme().afterGarbageCollection();

            if (heapResizingPolicy.resizeAfterCollection(objectSpace.totalSpace(), freeSpaceAfterGC, objectSpace)) {
                // Heap was resized.
                // Update heapMarker's coveredArea.
                ContiguousHeapSpace markedSpace = objectSpace.committedHeapSpace();
                heapMarker.setCoveredArea(markedSpace.start(), markedSpace.committedEnd());
            }
            if (MaxineVM.isDebug() && Heap.traceGCPhases()) {
                Log.print("End mark-sweep #");
                Log.println(collectionCount);
            }
            HeapScheme.Inspect.notifyGCCompleted();
            stopTimer(totalPauseTime);

            if (traceGCTimes) {
                reportLastGCTimes();
            }
        }
    }

    private Size setNextTLABChunk(Pointer chunk) {
        if (MaxineVM.isDebug()) {
            FatalError.check(!chunk.isZero(), "TLAB chunk must not be null");
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
     * @param tlabHardLimit hard limit of the current TLAB
     * @param chunk next chunk of this TLAB
     * @param size requested amount of memory
     * @return a pointer to the allocated memory
     */
    private Pointer changeTLABChunkOrAllocate(Pointer etla, Pointer tlabMark, Pointer tlabHardLimit, Pointer chunk, Size size) {
        Size chunkSize =  HeapFreeChunk.getFreechunkSize(chunk);
        Size effectiveSize = chunkSize.minus(TLAB_HEADROOM);
        if (size.greaterThan(effectiveSize))  {
            // Don't bother with searching another TLAB chunk that fits. Allocate out of TLAB.
            return objectSpace.allocate(size);
        }
        Address nextChunk = HeapFreeChunk.getFreeChunkNext(chunk);
        fillWithDeadObject(tlabMark, tlabHardLimit);
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
        Pointer tlab = objectSpace.allocateTLAB(tlabSize);
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

    @Override
    protected Pointer customAllocate(Pointer customAllocator, Size size, boolean adjustForDebugTag) {
        // Default is to use the immortal heap.
        return ImmortalHeap.allocate(size, true);
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
                return objectSpace.allocate(size);
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
        final Size nextTLABSize = refillPolicy.nextTlabSize();
        if (size.greaterThan(nextTLABSize)) {
            // This couldn't be allocated in a TLAB, so go directly to direct allocation routine.
            return objectSpace.allocate(size);
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
                return objectSpace.allocate(size);
            }
            // Leave as is. Parsability of the TLAB will be taken care by what follows.
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

}

