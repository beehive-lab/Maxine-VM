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
package com.sun.max.vm.heap.gcx;

import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;
import static com.sun.max.vm.heap.gcx.RegionTable.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.type.*;

/**
 * The Heap Region Manager organize heap memory into fixed-size regions.
 * It provides an interface to create multiple "heap accounts", each with a guaranteed
 * reserve of space (an integral number of regions). Heaps allocate space from their
 * heap accounts, return free space to it, and may grow or shrink their accounts.
 * The heap region manager may also request a heap account to trade or free some specific
 * regions.
 */
public final class HeapRegionManager implements HeapAccountOwner {
    /**
     * The single instance of the heap region manager.
     */
    static final HeapRegionManager theHeapRegionManager = new HeapRegionManager();

    /**
     * Region allocator used by the heap manager.
     */
    private final FixedSizeRegionAllocator regionAllocator;

    FixedSizeRegionAllocator regionAllocator() {
        return regionAllocator;
    }
    /**
     * Heap account serving the needs of the heap region manager.
     */
    private HeapAccount<HeapRegionManager> bootHeapAccount;

    public HeapAccount<HeapRegionManager> heapAccount() {
        return bootHeapAccount;
    }

    /**
     * Size of the space reserved by the heap region manager.
     * @return a size in byte
     */
    public Size size() {
        return Size.fromInt(heapAccount().reserve()).shiftedLeft(log2RegionSizeInBytes);
    }
    final AtomicBumpPointerAllocator bootAllocator;

    public Address bootAllocator() {
        return Reference.fromJava(bootAllocator).toOrigin();
    }

    /**
     * Total number of unreserved regions.
     */
    private int unreserved;

    /**
     *
     */
    private OutgoingReferenceChecker outgoingReferenceChecker;

    // Region reservation management interface. Should be private to heap account.
    // May want to revisit how the two interacts to better control
    // use of these sensitive operations (ideally, the transfer of unreserved to a
    // heap account region should be atomic.

    /**
     * Reserve the specified number of regions.
     *
     * @param numRegions number of region requested
     * @return true if the number of regions requested was reserved
     */
    boolean reserve(int numRegions) {
        if (numRegions > unreserved) {
            return false;
        }
        unreserved -= numRegions;
        return true;
    }
    /**
     * Release reserved regions (i.e., "unreserved" them).
     *
     * @param numRegions
     */
    void release(int numRegions) {
        FatalError.check((unreserved + numRegions) <= regionAllocator.capacity(), "invalid request");
        unreserved += numRegions;
    }

    public boolean contains(Address address) {
        return regionAllocator.contains(address);
    }

    public MemoryRegion bounds() {
        return regionAllocator.bounds();
    }

    boolean isValidRegionID(int regionID) {
        return regionAllocator.isValidRegionId(regionID);
    }

    private HeapRegionManager() {
        regionAllocator = new FixedSizeRegionAllocator("Heap Backing Storage");
        bootHeapAccount = new HeapAccount<HeapRegionManager>(this);
        bootAllocator = new AtomicBumpPointerAllocator<RefillManager>(new RefillManager() {

            @Override
            boolean shouldRefill(Size requestedSpace, Size spaceLeft) {
                return true;
            }

            @Override
            Address allocateRefill(Pointer startOfSpaceLeft, Size spaceLeft) {
                FatalError.unimplemented();
                return Address.zero();
            }

            @Override
            Address allocateOverflow(Size size) {
                FatalError.unimplemented();
                return Address.zero();
            }

            @Override
            Address allocateLarge(Size size) {
                FatalError.unimplemented();
                return Address.zero();
            }

            @Override
            void makeParsable(Pointer start, Pointer end) {
                // Boot linear allocator should not span multiple regions.
                RegionTable rt = theRegionTable();
                if (MaxineVM.isDebug()) {
                    final Pointer regionEnd = rt.regionInfo(start).regionStart().asPointer().plus(regionSizeInBytes);
                    FatalError.check(regionEnd.lessThan(end), "must be at region boundary");
                }
                HeapSchemeAdaptor.fillWithDeadObject(start, end);
                rt.regionInfo(start).setIterable();
            }
        });
    }

    private Size tupleSize(Class tupleClass) {
        return ClassActor.fromJava(tupleClass).dynamicTupleSize();
    }

    @INLINE
    public static HeapRegionManager theHeapRegionManager() {
        return theHeapRegionManager;
    }

    /**
     * Initialize the region manager with the supplied space.
     * As many regions as possible are carved out from this space, while preserving alignment constraints.
     * The region size is obtained from the HeapRegionInfo class.
     *
     * TODO: currently, footprint of the region manager is taxed-off the "reservedSpaceSize", which is the max heap size.
     * We should not do that, in order for our heap size to be comparable to another VM heap size which doesn't count this
     * but just the application heap. The fact that we also allocate the VM data structure, code, etc. make
     * this even more difficult.
     *
     * @param reservedSpace address to the first byte of the virtual memory reserved for the heap space
     * @param reservedSpaceSize size in byte of the heap space
     * @param regionInfoClass the sub-class of HeapRegionInfo used for region management.
     */
    public void initialize(Address reservedSpace, Size reservedSpaceSize, Class<HeapRegionInfo> regionInfoClass) {
        // Initialize region constants (size and log constants).
        HeapRegionConstants.initializeConstants();
        // Adjust reserved space to region boundaries.
        final Address startOfManagedSpace = reservedSpace.roundedUpBy(regionSizeInBytes);
        final Address endOfManagedSpace = startOfManagedSpace.plus(reservedSpaceSize).roundedDownBy(regionSizeInBytes);
        final Size managedSpaceSize = endOfManagedSpace.minus(startOfManagedSpace).asSize();
        final int numRegions = managedSpaceSize.unsignedShiftedRight(log2RegionSizeInBytes).toInt();
        final int regionListSize = numRegions << 1; // 2 entries per regions, one for each link (prev and next).

        // FIXME (ld) have we committed the space that is going to be used by the boot allocator ?

        unreserved = numRegions;
        // Estimate conservatively what the heap manager needs initially. This is to commit
        // enough memory to get started.
        // TODO (ld) initial size should be made to correspond to some notion of initial heap.

        // 1. The region info table:
        Size initialSize = tupleSize(regionInfoClass).plus(tupleSize(RegionTable.class));
        // 2. The backing storage for the heap region lists
        initialSize = initialSize.plus(Layout.getArraySize(Kind.INT, regionListSize)).times(2);

        // Round this to an integral number of regions.
        initialSize = initialSize.roundedUpBy(regionSizeInBytes);
        final int initialNumRegions = initialSize.unsignedShiftedRight(log2RegionSizeInBytes).toInt();

        // initialize the bootstrap allocator. The rest of the initialization code needs to allocate heap region management
        // object. We solve the bootstrapping problem this causes by using a linear allocator as a custom allocator for the current
        // thread. The contiguous set of regions consumed by the initialization will be accounted after the fact to the special
        // boot heap account.
        bootAllocator.initialize(startOfManagedSpace, initialSize, initialSize, HeapSchemeAdaptor.MIN_OBJECT_SIZE);
        try {
            VMConfiguration.vmConfig().heapScheme().enableCustomAllocation(Reference.fromJava(bootAllocator).toOrigin());

            // Commit space
            regionAllocator.initialize(startOfManagedSpace, numRegions, initialNumRegions);

            // enable early inspection.
            InspectableHeapInfo.init(false, regionAllocator.bounds());

            RegionTable.initialize(regionInfoClass, regionAllocator.bounds(), numRegions);
            // Allocate the backing storage for the region lists.
            HeapRegionList.initializeListStorage(HeapRegionList.RegionListUse.ACCOUNTING, new int[regionListSize]);
            HeapRegionList.initializeListStorage(HeapRegionList.RegionListUse.OWNERSHIP, new int[regionListSize]);

            FatalError.check(bootAllocator.end.roundedUpBy(regionSizeInBytes).lessEqual(startOfManagedSpace.plus(initialSize)), "");

            // Ready to open bootstrap heap accounts now.
            // Start with opening the boot heap account to set the records straight after bootstrap.
            // TODO (ld) initialNumRegions may not be the reserve we want here. Need to adjust that to the desired "immortal" size.
            FatalError.check(bootHeapAccount.open(initialNumRegions), "Failed to create boot heap account");
            if (MaxineVM.isDebug()) {
                outgoingReferenceChecker = new OutgoingReferenceChecker(bootHeapAccount);
            }

            // Now fix up the boot heap account to records the regions used up to now.
            bootHeapAccount.recordAllocated(0, initialNumRegions, null, false);
        } finally {
            VMConfiguration.vmConfig().heapScheme().disableCustomAllocation();
        }
    }

    /**
     * Request a number of contiguous regions.
     * @param numRegions
     * @return the identifier of the first region of the contiguous range allocated or {@link HeapRegionConstants#INVALID_REGION_ID} if the
     * request cannot be satisfied.
     */
    int allocate(int numRegions) {
        return regionAllocator.allocate(numRegions);
    }

    /**
     * Request a number of regions. The allocated regions are added at the head or tail of the list depending on the value
     * specified in the append parameter. The allocate does a best effort to provides contiguous regions.
     *
     * @param list list where the allocated regions are recorded
     * @param numRegions number of regions requested
     * @param append Append the allocated region to the list if true, otherwise, prepend it.
     * @param exact if true, fail if the number of requested regions cannot be satisfied, otherwise allocate
     * as many regions as possible
     * @return the number of regions allocated
     */
    int allocate(HeapRegionList list, int numRegions, boolean append, boolean exact) {
        FatalError.unimplemented();
        return 0;
    }

    /**
     * Free contiguous regions.
     * @param firstRegionId identifier of the first region
     * @param numRegions
     */
    void free(int firstRegionId, int numRegions) {
        // TODO(ld): error handling
        regionAllocator.free(firstRegionId, numRegions);
    }

    void commit(int firstRegionId, int numRegions) {
        // TODO(ld): error handling
        regionAllocator.commit(firstRegionId, numRegions);
    }

    void uncommit(int firstRegionId, int numRegions) {
        // TODO:(ld) error handling
        regionAllocator.uncommit(firstRegionId, numRegions);
    }

    public void checkOutgoingReferences() {
        if (MaxineVM.isDebug()) {
            bootAllocator.unsafeMakeParsable();
            bootHeapAccount.visitObjects(outgoingReferenceChecker);
            if (outgoingReferenceChecker.outgoingReferenceCount() != 0L) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Boot heap account has ");
                Log.print(outgoingReferenceChecker.outgoingReferenceCount());
                Log.println(" outgoing references.");
                Log.unlock(lockDisabledSafepoints);
                FatalError.crash("Must not happen");
            }
        }
    }

    public void verifyAfterInitialization() {
        HeapRegionConstants.validate();
        checkOutgoingReferences();
    }
}

