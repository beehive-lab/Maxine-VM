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
package com.sun.max.vm.heap.gcx.gen.mse;
import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;
import static com.sun.max.vm.heap.gcx.HeapRegionManager.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;


/**
 * Generational Heap Scheme. WORK IN PROGRESS.
 */
final public class GenMSEHeapScheme extends HeapSchemeWithTLAB  implements HeapAccountOwner {
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

    /**
     * Size to reserve at the end of a TLABs to guarantee that a dead object can always be
     * appended to a TLAB to fill unused space before a TLAB refill.
     * The headroom is used to compute a soft limit that'll be used as the tlab's top.
     */
    @CONSTANT_WHEN_NOT_ZERO
    static Size TLAB_HEADROOM;

    /**
     * Account for the application's generational heap. Both old and young generations tap in this account for their storage.
     */
    private final HeapAccount<GenMSEHeapScheme> heapAccount;

    // FIXME: the interface for a generations abstraction not fully designed yet, so for now we use spaces directly here.
    /**
     * Young generation.
     */
    private final NoAgingNursery youngSpace;
    /**
     * Tenured generation.
     */
    private final FirstFitMarkSweepSpace<GenMSEHeapScheme> oldSpace;

    /**
     * Policy for resizing the heap after each GC.
     */
    private GenHeapSizingPolicy heapResizingPolicy;

    /**
     * Card-table based remembered set for the nursery.
     */
    private CardTableRSet cardTableRSet;

    /**
     * Implementation of young space evacuation. Used by minor collection operations.
     */
    private Evacuator youngSpaceEvacuator;

    /**
     * Operation to submit to the {@link VmOperationThread} to perform a minor collection.
     */
    private MinorCollection minorCollection;

    /**
     * Operation to submit to the {@link VmOperationThread} to perform a full collection.
     */
    private MajorCollection fullCollection;

    /**
     * Marking algorithm used to trace the heap.
     */
    private final TricolorHeapMarker heapMarker;

    @HOSTED_ONLY
    public GenMSEHeapScheme() {
        heapAccount = new HeapAccount<GenMSEHeapScheme>(this);
        heapMarker = new TricolorHeapMarker(WORDS_COVERED_PER_BIT, new HeapAccounRootCellVisitor(this));
        youngSpace = new NoAgingNursery(heapAccount);
        oldSpace = new FirstFitMarkSweepSpace<GenMSEHeapScheme>(heapAccount);
        cardTableRSet = new CardTableRSet();
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isHosted() && phase == MaxineVM.Phase.BOOTSTRAPPING) {
            // VM-generation time initialization.
            TLAB_HEADROOM = MIN_OBJECT_SIZE;
            BaseAtomicBumpPointerAllocator.hostInitialize();
        } else if (phase == MaxineVM.Phase.PRISTINE) {
            allocateHeapAndGCStorage();
        }
    }

    /**
     * Allocate memory for both the heap and the GC's data structures (mark bitmaps, marking stacks, card & offset tables, etc.).
     */
    private void allocateHeapAndGCStorage() {
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

        theHeapRegionManager().initialize(firstUnusedByteAddress, endOfReservedSpace, maxSize, HeapRegionInfo.class);
        try {
            enableCustomAllocation(theHeapRegionManager().allocator());
            final MemoryRegion heapBounds = theHeapRegionManager().bounds();
            final Size applicationHeapMaxSize = heapBounds.size().minus(theHeapRegionManager().size());

            // Compute space needed by the heap marker. This is proportional to the size of the space traced by the heap marker.
            // The boot image isn't traced (it is assumed a permanent root of collection).
            final Size heapMarkerDatasize = heapMarker.memoryRequirement(heapBounds.size());

            // Heap Marker Data are allocated at end of the space reserved to the heap regions.
            final Address heapMarkerDataStart = heapBounds.end().roundedUpBy(pageSize);

            // Card Table Data are allocated at end of the space reserved to the heap regions.
            final Address cardTableDataStart =  heapMarkerDataStart.plus(heapMarkerDatasize).roundedUpBy(pageSize);
            final Size cardTableDataSize = cardTableRSet.memoryRequirement(heapBounds.size());

            // Address to the first reserved byte unused by the heap scheme.
            Address unusedReservedSpaceStart = cardTableDataStart.plus(cardTableDataSize).roundedUpBy(pageSize);

            if (!unusedReservedSpaceStart.greaterThan(Heap.startOfReservedVirtualSpace())) {
                MaxineVM.reportPristineMemoryFailure("out of heap data (heap marker + card table", "reserve", heapMarkerDatasize.plus(cardTableDataSize));
            }

            // Initialize card table as early as possible since bootstrapping code may modify reference.
            if (!VirtualMemory.commitMemory(cardTableDataStart, cardTableDataSize,  VirtualMemory.Type.DATA)) {
                MaxineVM.reportPristineMemoryFailure("card table space", "commit", heapMarkerDatasize);
            }
            cardTableRSet.initialize(heapBounds.start(), heapBounds.size(), cardTableDataStart, cardTableDataSize);

            if (!VirtualMemory.commitMemory(heapMarkerDataStart, heapMarkerDatasize,  VirtualMemory.Type.DATA)) {
                MaxineVM.reportPristineMemoryFailure("heap marker space", "commit", heapMarkerDatasize);
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

            heapResizingPolicy = new FixedRatioGenHeapSizingPolicy(initSize, maxSize, YoungGenHeapPercent, log2RegionSizeInBytes);

            if (!heapAccount().open(numberOfRegions(applicationHeapMaxSize))) {
                FatalError.unexpected("Failed to create application heap");
            }

            youngSpace.initialize(heapResizingPolicy);
            oldSpace.initialize(heapResizingPolicy.initialOldGenSize(), heapResizingPolicy.maxOldGenSize());

            // FIXME: the capacity of the survivor range queues should be dynamic. Its upper bound could be computed based on the
            // worst case evacuation and the number of fragments of old space available for allocation.
            // Same with the lab size. In non parallel evacuators, this should be all the space available for allocation in a region.

            youngSpaceEvacuator = new NoAgingEvacuator(youngSpace, oldSpace, cardTableRSet, oldSpace.minReclaimableSpace(),
                            new SurvivorRangesQueue(1000), ELABSize);

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

    @Override
    public boolean isGcThread(Thread thread) {
        // TODO Auto-generated method stub
        return false;
    }


    final class MinorCollection extends GCOperation {
        MinorCollection() {
            super("MinorCollection");
        }

        @Override
        protected void collect(int invocationCount) {
            // TODO Auto-generated method stub
            HeapScheme.Inspect.notifyGCStarted();
            // Evacuate young space
            youngSpaceEvacuator.evacuate();
            // TODO: this is where potentially you want
            //
            HeapScheme.Inspect.notifyGCCompleted();
        }
    }

    final class MajorCollection extends GCOperation {
        MajorCollection() {
            super("MajorCollection");
        }
        @Override
        protected void collect(int invocationCount) {
            // TODO Auto-generated method stub
            HeapScheme.Inspect.notifyGCStarted();
            HeapScheme.Inspect.notifyGCCompleted();
        }
    }

    @Override
    public boolean contains(Address address) {
        return theHeapRegionManager().contains(address);
    }

    @Override
    public boolean collectGarbage(Size requestedFreeSpace) {
        // TODO:
        // Right now, we do a minor collection just for testing the code.
        minorCollection.submit();

        return true;
    }

    @Override
    public Size reportFreeSpace() {
        // TODO Auto-generated method stub
        return Size.zero();
    }

    @Override
    public Size reportUsedSpace() {
        // TODO Auto-generated method stub
        return Size.zero();
    }

    @Override
    public void writeBarrier(Reference from, Reference to) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean pin(Object object) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void unpin(Object object) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void tlabReset(Pointer tla) {
        // TODO Auto-generated method stub

    }

    /**
     * Allocate a chunk of memory of the specified size and refill a thread's TLAB with it.
     * @param etla the thread whose TLAB will be refilled
     * @param tlabSize the size of the chunk of memory used to refill the TLAB
     */
    private void allocateAndRefillTLAB(Pointer etla, Size tlabSize) {
        Pointer tlab = youngSpace.allocate(tlabSize);
        Size effectiveSize = tlabSize.minus(TLAB_HEADROOM);
        if (Heap.traceAllocation()) {
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
    protected void doBeforeTLABRefill(Pointer tlabAllocationMark, Pointer tlabEnd) {
        // TODO Auto-generated method stub

    }

    @Override
    protected Pointer customAllocate(Pointer customAllocator, Size size) {
        return BaseAtomicBumpPointerAllocator.asBumpPointerAllocator(Reference.fromOrigin(Layout.cellToOrigin(customAllocator)).toJava()).allocateCleared(size);
    }

    @Override
    public HeapAccount<GenMSEHeapScheme> heapAccount() {
        return heapAccount;
    }

}
