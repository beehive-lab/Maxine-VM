/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.heap.gcx.ms;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.StopTheWorldGCDaemon.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * A simple mark-sweep collector, without TLABs support. Only used for testing / debugging
 * marking and sweeping algorithms. Allocation is a simple linear allocation out of a linked list
 * of free space, initially set to a single memory area that equals the heap initial size, then
 * a linked list of free space threaded over the heap by the sweeper.
 *
 * @author Laurent Daynes
 */
public class MSHeapScheme extends HeapSchemeWithTLAB {
    private static final int WORDS_COVERED_PER_BIT = 1;

    /**
     * Size to reserve at the end of a TLABs to guarantee that a dead object can always be
     * appended to a TLAB to fill unused space before a TLAB refill.
     * The headroom is used to compute a soft limit that'll be used as the tlab's top.
     */
    @CONSTANT_WHEN_NOT_ZERO
    static Size TLAB_HEADROOM;

    /**
     * A marking algorithm for the MSHeapScheme.
     */
    final TricolorHeapMarker heapMarker;

    /**
     * Free Space Manager. Also implement sweeping.
     */
    final FreeHeapSpaceManager freeSpace;

    Size totalUsedSpace;

    private final Collect collect = new Collect();

    private StopTheWorldGCDaemon collectorThread;

    public MSHeapScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        heapMarker = new TricolorHeapMarker(WORDS_COVERED_PER_BIT);
        freeSpace = new FreeHeapSpaceManager();
        totalUsedSpace = Size.zero();
    }


    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isHosted()) {
            // VM-generation time initialization.
            TLAB_HEADROOM = MIN_OBJECT_SIZE;
            // The monitor for the collector must be allocated in the image
            JavaMonitorManager.bindStickyMonitor(this);
        } else  if (phase == MaxineVM.Phase.PRISTINE) {
            allocateHeapAndGCStorage();
        } else if (phase == MaxineVM.Phase.STARTING) {
            collectorThread = new StopTheWorldGCDaemon("GC", collect);
            collectorThread.start();
        }
    }

    /**
     * Allocate memory for both the heap and the GC's data structures (mark bitmaps, marking stacks, etc.).
     * We only require that the heap is contiguous with the
     */
    private void allocateHeapAndGCStorage() {
        final Size initSize = Heap.initialSize();
        final Size maxSize = Heap.maxSize();

        Address endOfCodeRegion = Code.bootCodeRegion.end().roundedUpBy(Platform.target().pageSize);
        CodeManager codeManager = Code.getCodeManager();
        if (codeManager instanceof FixedAddressCodeManager && codeManager.getRuntimeCodeRegion().start().equals(endOfCodeRegion)) {
            endOfCodeRegion = codeManager.getRuntimeCodeRegion().end();
        } else {
            FatalError.unimplemented();
        }

        if (!VirtualMemory.allocatePageAlignedAtFixedAddress(endOfCodeRegion, initSize, VirtualMemory.Type.HEAP)) {
            reportPristineMemoryFailure("object heap", initSize);
        }

        freeSpace.initialize(endOfCodeRegion, initSize);

        // Initialize the heap marker's data structures. Needs to make sure it is outside of the heap reserved space.
        final Address endOfHeap = endOfCodeRegion.plus(maxSize);
        final Size heapMarkerDatasize = heapMarker.memoryRequirement(maxSize);
        if (!VirtualMemory.allocatePageAlignedAtFixedAddress(endOfHeap, heapMarkerDatasize,  VirtualMemory.Type.DATA)) {
            reportPristineMemoryFailure("heap marker data", heapMarkerDatasize);
        }
        heapMarker.initialize(freeSpace.committedHeapSpace(), endOfHeap, heapMarkerDatasize);
    }


    public int auxiliarySpaceSize(int bootImageSize) {
        return 0;
    }

    public synchronized boolean collectGarbage(Size requestedFreeSpace) {
        // FIXME: need to revisit this.
        if (requestedFreeSpace.greaterThan(freeSpace.freeSpaceLeft())) {
            collectorThread.execute();
        }
        return true;
    }

    public boolean contains(Address address) {
        return freeSpace.committedHeapSpace().contains(address);
    }

    public final void initializeAuxiliarySpace(Pointer primordialVmThreadLocals, Pointer auxiliarySpace) {
    }

    public boolean isGcThread(Thread thread) {
        return thread instanceof StopTheWorldGCDaemon;
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
        return null;
    }

    public Size reportUsedSpace() {
        return totalUsedSpace;
    }

    public void runFinalization() {
    }

    @INLINE(override = true)
    public void unpin(Object object) {
    }

    @INLINE(override = true)
    public void writeBarrier(Reference from, Reference to) {
    }

    /**
     * Class implementing the garbage collection routine.
     * This is the {@link StopTheWorldGCDaemon}'s entry point to garbage collection.
     */
    final class Collect extends Collector {
        @Override
        public void collect(int invocationCount) {
            HeapScheme.Static.notifyGCStarted();
            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            heapMarker.markAll();
            freeSpace.reclaim(heapMarker);

            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
            HeapScheme.Static.notifyGCCompleted();
        }
    }

    @Override
    protected void doBeforeTLABRefill(Pointer tlabAllocationMark, Pointer tlabEnd) {
        // Need to plant a dead object in the leftover to make the heap parseable (required for sweeping).
        Pointer hardLimit = tlabEnd.plus(TLAB_HEADROOM);
        if (tlabAllocationMark.greaterThan(tlabEnd)) {
            FatalError.check(hardLimit.equals(tlabAllocationMark), "TLAB allocation mark cannot be greater than TLAB End");
            return;
        }
        fillWithDeadObject(tlabAllocationMark, hardLimit);
    }


    private ResetTLAB resetTLAB = new ResetTLAB() {
        @Override
        protected void doBeforeReset(Pointer enabledVmThreadLocals, Pointer tlabMark, Pointer tlabTop) {
            doBeforeTLABRefill(tlabMark, tlabTop);
        }
    };
    protected void resetTLABs() {
        VmThreadMap.ACTIVE.forAllThreadLocals(null, resetTLAB);
    }

    @INLINE
    private Size setNextTLABChunk(Pointer nextChunk) {
        Size chunkSize =  HeapFreeChunk.getFreechunkSize(nextChunk);
        Size effectiveSize = chunkSize.minus(TLAB_HEADROOM);
        nextChunk.setWord(effectiveSize.toInt(), HeapFreeChunk.getFreeChunkNext(nextChunk));
        return effectiveSize;
    }

    @INLINE
    private Size setNextTLABChunk(Pointer enabledVmThreadLocals, Pointer nextChunk) {
        Size nextChunkEffectiveSize = setNextTLABChunk(nextChunk);
        fastRefillTLAB(enabledVmThreadLocals, nextChunk, nextChunkEffectiveSize);
        return nextChunkEffectiveSize;
    }

    /**
     * Allocate a chunk of memory of the specified size and refill a thread's TLAB with it.
     * @param enabledVmThreadLocals the thread whose TLAB will be refilled
     * @param tlabSize the size of the chunk of memory used to refill the TLAB
     */
    private void allocateAndRefillTLAB(Pointer enabledVmThreadLocals, Size tlabSize) {
        Pointer tlab = freeSpace.allocateTLAB(tlabSize);
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
        refillTLAB(enabledVmThreadLocals, tlab, effectiveSize);
    }

    @Override
    protected Pointer handleTLABOverflow(Size size, Pointer enabledVmThreadLocals, Pointer tlabMark, Pointer tlabEnd) {      // Should we refill the TLAB ?
        final TLABRefillPolicy refillPolicy = TLABRefillPolicy.getForCurrentThread(enabledVmThreadLocals);
        if (refillPolicy == null) {
            // No policy yet for the current thread. This must be the first time this thread uses a TLAB (it does not have one yet).
            ProgramError.check(tlabMark.isZero(), "thread must not have a TLAB yet");
            if (!usesTLAB()) {
                // We're not using TLAB. So let's assign the never refill tlab policy.
                TLABRefillPolicy.setForCurrentThread(enabledVmThreadLocals, NEVER_REFILL_TLAB);
                return freeSpace.allocate(size);
            }
            // Allocate an initial TLAB and a refill policy. For simplicity, this one is allocated from the TLAB (see comment below).
            final Size tlabSize = initialTlabSize();
            allocateAndRefillTLAB(enabledVmThreadLocals, tlabSize);
            // Let's do a bit of dirty meta-circularity. The TLAB is refilled, and no-one except the current thread can use it.
            // So the tlab allocation is going to succeed here
            TLABRefillPolicy.setForCurrentThread(enabledVmThreadLocals, new SimpleTLABRefillPolicy(tlabSize));
            // Now, address the initial request. Note that we may recurse down to handleTLABOverflow again here if the
            // request is larger than the TLAB size. However, this second call will succeed and allocate outside of the tlab.
            return tlabAllocate(size);
        }
        final Size nextTLABSize = refillPolicy.nextTlabSize();
        if (size.greaterThan(nextTLABSize)) {
            // This couldn't be allocated in a TLAB, so go directly to direct allocation routine.
            return freeSpace.allocate(size);
        }
        final Pointer hardLimit = tlabEnd.plus(TLAB_HEADROOM);
        final Pointer nextChunk = tlabEnd.getWord().asPointer();

        final Pointer cell = DebugHeap.adjustForDebugTag(tlabMark);
        if (cell.plus(size).equals(hardLimit)) {
            // Can actually fit the object in space left.
            if (nextChunk.isZero()) {
                setTlabAllocationMark(enabledVmThreadLocals, hardLimit);
            } else {
                // TLAB has another chunk of free space. Set it.
                setNextTLABChunk(enabledVmThreadLocals, nextChunk);
            }
            // allocateAndRefillTLAB(enabledVmThreadLocals, nextTLABSize);
            return cell;
        } else if (!nextChunk.isZero()) {
            fillWithDeadObject(tlabMark, hardLimit);
            // TLAB has another chunk of free space. Set it.
            Size nextChunkEffectiveSize = setNextTLABChunk(enabledVmThreadLocals, nextChunk);
            if (size.lessEqual(nextChunkEffectiveSize)) {
                return tlabAllocate(size);
            }
            // Don't bother with searching another TLAB chunk that fits. Allocate out of TLAB.
            return freeSpace.allocate(size);
        }

        if (!refillPolicy.shouldRefill(size, tlabMark)) {
            // Size would fit in a new tlab, but the policy says we shouldn't refill the tlab yet, so allocate directly in the heap.
            return freeSpace.allocate(size);
        }
        // Refill TLAB and allocate (we know the request can be satisfied with a fresh TLAB and will therefore succeed).
        allocateAndRefillTLAB(enabledVmThreadLocals, nextTLABSize);
        return tlabAllocate(size);
    }

    @INLINE(override = true)
    @Override
    public boolean supportsTagging() {
        return false;
    }
}

