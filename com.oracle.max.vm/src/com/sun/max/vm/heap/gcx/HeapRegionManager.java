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
    private HeapAccount<HeapRegionManager> managerHeapAccount;

    /**
     * Heap account of the region manager. This account is distinct from all other heap accounts
     * and in particular, heap accounts used by applications. It serves the need of the heap region manager only.
     */
    public HeapAccount<HeapRegionManager> heapAccount() {
        return managerHeapAccount;
    }

    /**
     * Size of the space reserved by the heap region manager.
     * @return a size in byte
     */
    public Size size() {
        return Size.fromInt(heapAccount().reserve()).shiftedLeft(log2RegionSizeInBytes);
    }

    /**
     * The allocator used by the HeapRegionManager to allocate its own objects.
     * A simple atomic bump allocator for now.
     */
    final AtomicBumpPointerAllocator managerAllocator;

    /**
     * Return the address of the HeapRegionManager's own allocator.
     * This is the allocator used to allocate the HeapRegionManager's objects at startup.
     * The address as an allocator identifier for custom allocation.
     * @see HeapScheme#enableCustomAllocation(Address)
     *
     * @return an address that can be used for setting up custom allocation
     */
    public Address allocator() {
        return Reference.fromJava(managerAllocator).toOrigin();
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
        managerHeapAccount = new HeapAccount<HeapRegionManager>(this);
        managerAllocator = new AtomicBumpPointerAllocator<RefillManager>(new RefillManager() {

            @Override
            public boolean shouldRefill(Size requestedSpace, Size spaceLeft) {
                return true;
            }

            @Override
            public Address allocateRefill(Pointer startOfSpaceLeft, Size spaceLeft) {
                FatalError.unimplemented();
                return Address.zero();
            }

            @Override
            public Address allocateOverflow(Size size) {
                FatalError.unimplemented();
                return Address.zero();
            }

            @Override
            public Address allocateLarge(Size size) {
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
                HeapRegionState.FULL_REGION.setState(rt.regionInfo(start));
            }

            @Override
            protected void doBeforeGC() {
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
     * Initialize the region manager with enough regions to satisfy a specified maximum of heap space.
     * The total number of regions should covers both the specified maximum heap space and the needs of the region manager.
     *
     * The region manager is provided with a contiguous range of virtual memory that should be large enough to cover the space needed both for the heap space and
     * the region manager's book-keeping data structure.
     * After initialization succeeds, the effective amount of space managed by the region manager can be obtained by invoking {@link #bounds()}.
     * The space left over from the supplied reserved space is uncommitted.
     * TODO: currently, the space is provided committed, and the heap manager is responsible for uncommitting what it doesn't use. It should be the other way around:
     * provide uncommitted space and the region manager commits what it needs.
     *
     * TODO: we need a better way to capture the extra space requirements a heap scheme needs for the boot heap account.
     * Or may be we just need to add an additional VM/GC heap account, so that the boot heap account is strictly for the heap region manager
     * while the GC (or each GC) create it's own.
     *
     * @param reservedSpace start of the virtual memory reserved for the managed space
     * @param endOfReservedSpace end of the reserved space
     * @param heapSpaceSize size in byte of the heap space
     * @param regionInfoClass the sub-class of HeapRegionInfo used for region management.
     *
     */
    public void initialize(Address reservedSpace, Address endOfReservedSpace, Size heapSpaceSize, Class<HeapRegionInfo> regionInfoClass) {
        // Initialize region constants (size and log constants).
        // The size of regions is computed from the requested heap size so as to keep the region table bounded and adapt region size to the heap size
        // (in particular, very large heap command large region size).
        HeapRegionConstants.initializeConstants(heapSpaceSize);
        // Adjust reserved space to region boundaries.
        final Address startOfManagedSpace = reservedSpace.alignUp(regionSizeInBytes);
        final Address endOfManagedSpace = startOfManagedSpace.plus(heapSpaceSize).alignUp(regionSizeInBytes);
        final int numHeapRegions = endOfManagedSpace.minus(startOfManagedSpace).asSize().unsignedShiftedRight(log2RegionSizeInBytes).toInt();
        // Always count 10K of extra space for the odd objects
        // (e.g., the OutgoingReferenceChecker instance, the MemoryRegion [] created by the var args of InspectableHeapInfo, etc...
        final Size extraSpace = Size.K.times(10);
        int numExtraBootRegions = extraSpace.alignUp(regionSizeInBytes).unsignedShiftedRight(log2RegionSizeInBytes).toInt();

       // We must add to this number of regions the regions to cover the space needed for the boot heap which allocate the region manager's data.
        // Per region book-keeping space: region descriptor plus links in region lists (2 links per region per list, two lists -- ownership and accounting).
        int perRegionSpaceRequirement = tupleSize(regionInfoClass).toInt() + 4 * Kind.INT.width.numberOfBytes;
        int numRegionsPerBootRegion = regionSizeInBytes / perRegionSpaceRequirement;
        int numTotalRegions = numHeapRegions + numExtraBootRegions;
        int numBootKeepingRegions =  (numTotalRegions * perRegionSpaceRequirement) /  regionSizeInBytes;

        while (numBootKeepingRegions > numRegionsPerBootRegion) {
            numTotalRegions += numBootKeepingRegions;
            numBootKeepingRegions = 1 + (numBootKeepingRegions * perRegionSpaceRequirement) /  regionSizeInBytes;
        }
        numTotalRegions += numBootKeepingRegions;

        // Final count of space needed for the VM startup heap: add the Empty region table plus empty region lists plus 1
        final Size bootHeapSize = extraSpace.plus(tupleSize(RegionTable.class).plus(Layout.getArraySize(Kind.INT, 0).times(2)).plus(perRegionSpaceRequirement * numTotalRegions)).alignUp(regionSizeInBytes);
        final Size managedSpaceSize = Size.fromInt(numTotalRegions).shiftedLeft(log2RegionSizeInBytes);
        FatalError.check(startOfManagedSpace.plus(managedSpaceSize).lessEqual(endOfReservedSpace),
                        "Not enough reserved space to initialize managed space");

        // Estimate conservatively how much space the heap manager needs initially. This is to commit
        // enough memory to get started.
        final int regionListSize = numTotalRegions << 1; // 2 entries per regions, one for each link (prev and next).

        final int initialNumRegions = bootHeapSize.unsignedShiftedRight(log2RegionSizeInBytes).toInt();
        if (MaxineVM.isDebug()) {
            Log.print("Initialize heap region manager's boot allocator with ");
            Log.print(initialNumRegions);
            Log.print(" regions (");
            Log.printToPowerOfTwoUnits(bootHeapSize);
            Log.println(" bytes)");
        }

        unreserved = numTotalRegions;
        final HeapScheme heapScheme = VMConfiguration.vmConfig().heapScheme();
        if (heapScheme instanceof RSetCoverage) {
            ((RSetCoverage) heapScheme).initializeCoverage(startOfManagedSpace, managedSpaceSize);
        }
        // initialize the bootstrap allocator. The rest of the initialization code needs to allocate heap region management
        // object. We solve the bootstrapping problem this causes by using a linear allocator as a custom allocator for the current
        // thread. The contiguous set of regions consumed by the initialization will be accounted after the fact to the special
        // boot heap account.
        managerAllocator.initialize(startOfManagedSpace, bootHeapSize, bootHeapSize);
        try {
            heapScheme.enableCustomAllocation(Reference.fromJava(managerAllocator).toOrigin());
            // Record initial space usage.
            regionAllocator.initialize(startOfManagedSpace, numTotalRegions, initialNumRegions);
            RegionTable.initialize(regionInfoClass, regionAllocator.bounds(), numTotalRegions);
            // Allocate the backing storage for the region lists.
            HeapRegionList.initializeListStorage(HeapRegionList.RegionListUse.ACCOUNTING, new int[regionListSize]);
            HeapRegionList.initializeListStorage(HeapRegionList.RegionListUse.OWNERSHIP, new int[regionListSize]);

            FatalError.check(managerAllocator.end.roundedUpBy(regionSizeInBytes).lessEqual(startOfManagedSpace.plus(bootHeapSize)), "");

            // Ready to open bootstrap heap accounts now.
            // Start with opening the boot heap account to set the records straight after bootstrap.
            // TODO (ld) initialNumRegions may not be the reserve we want here. Need to adjust that to the desired "immortal" size.
            FatalError.check(managerHeapAccount.open(initialNumRegions), "Failed to create boot heap account");
            if (MaxineVM.isDebug()) {
                outgoingReferenceChecker = new OutgoingReferenceChecker(managerHeapAccount);
            }

            // Now fix up the boot heap account to records the regions used up to now.
            // They are recorded committed
            HeapAccount.completeBootHeapAccountBootstrap(initialNumRegions);

            // First, uncommit reserved space after the fact. All the space between the end of the initial boot heap and the end of the provided reserved space should be uncommitted.
            final Address endOfInitialBootHeap = startOfManagedSpace.plus(bootHeapSize);
            final Address endOfRegions = bounds().end();
            Size uncommitedSpaceSize = endOfRegions.minus(endOfInitialBootHeap).asSize();
            if (!VirtualMemory.uncommitMemory(endOfInitialBootHeap, uncommitedSpaceSize,  VirtualMemory.Type.DATA)) {
                MaxineVM.reportPristineMemoryFailure("uncommitted regions", "uncommit", uncommitedSpaceSize);
            }
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

    /**
     * Verifies, in debug mode only (@see {@link MaxineVM#isDebug()}), that no references from this heap region manager's heap account escape.
     */
    public void checkOutgoingReferences() {
        if (MaxineVM.isDebug()) {
            managerAllocator.unsafeMakeParsable();
            managerHeapAccount.visitObjects(outgoingReferenceChecker);
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

    public boolean supportsTagging() {
        return false;
    }

    public boolean isGcThread(Thread thread) {
        return false;
    }
}

