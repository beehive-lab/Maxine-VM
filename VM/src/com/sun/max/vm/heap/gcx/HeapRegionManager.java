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
package com.sun.max.vm.heap.gcx;

import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * The Heap Region Manager organize heap memory into fixed-size regions.
 * It provides an interface to create multiple "heap accounts", each with a guaranteed
 * reserve of space (an integral number of regions). Heaps allocate space from their
 * heap accounts, return free space to it, and may grow or shrink their accounts.
 * The heap region manager may also request a heap account to trade or free some specific
 * regions.
 *
 *
 * @author Laurent Daynes
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
     * Total number of unreserved regions.
     */
    private int unreserved;

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

    // One way to make this a throw away object is to allocate it in some far region that
    // we free afterward. An alternative is to make it the heap manager's heap.
    private class BootstrapAllocator {
        private Address top;
        private Address end;

        void initialize(Address top, Address end) {
            this.top = top;
            this.end = end;
        }
        @INLINE
        private Pointer allocate(Size size) {
            Address cell = top;
            top = cell.plus(size).asPointer();
            if (top.greaterThan(end)) {
                FatalError.unexpected("Not enough memory to initialize heap manager");
            }
            return cell.asPointer();
        }

        public final Object createTuple(Hub hub) {
            return Cell.plantTuple(allocate(hub.tupleSize), hub);
        }

        final <T> T createTuple(Class<T> tupleClass) {
            return tupleClass.cast(createTuple(ClassActor.fromJava(tupleClass).dynamicHub()));
        }

        public final Object createArray(DynamicHub dynamicHub, int length) {
            final Size size = Layout.getArraySize(dynamicHub.classActor.componentClassActor().kind, length);
            return Cell.plantArray(allocate(size), size, dynamicHub, length);
        }
    }

    BootstrapAllocator bootstrapAllocator;


    private HeapRegionManager() {
        regionAllocator = new FixedSizeRegionAllocator("Heap Backing Storage");
        bootstrapAllocator = new BootstrapAllocator();
        bootHeapAccount = new HeapAccount<HeapRegionManager>(this);
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
        final Address endOfManagedSpace = reservedSpace.plus(reservedSpaceSize).roundedDownBy(regionSizeInBytes);
        final Address startOfManagedSpace = reservedSpace.roundedUpBy(regionSizeInBytes);
        final Size managedSpaceSize = endOfManagedSpace.minus(startOfManagedSpace).asSize();
        final int numRegions = managedSpaceSize.unsignedShiftedRight(log2RegionSizeInBytes).toInt();

        // FIXME: have we committed the space that is going to be used by the boot allocator ?

        unreserved = numRegions;
        // Estimate conservatively what the heap manager needs initially. This is to commit
        // enough memory to get started.
        // FIXME: initial size should be made to correspond to some notion of initial heap.

        // 1. The region info table:
        Size initialSize = tupleSize(regionInfoClass).plus(tupleSize(RegionTable.class));
        // 2. The backing storage for the heap region lists
        initialSize = initialSize.plus(Layout.getArraySize(Kind.INT, numRegions * 2)).times(2);

        // Round this to an integral number of regions.
        initialSize = initialSize.roundedUpBy(regionSizeInBytes);
        final int initialNumRegions = initialSize.unsignedShiftedRight(log2RegionSizeInBytes).toInt();

        bootstrapAllocator.initialize(startOfManagedSpace, startOfManagedSpace.plus(initialSize));

        // Commit space and initialize the bootstrap allocator
        regionAllocator.initialize(startOfManagedSpace, numRegions, initialNumRegions);


        // FIXME: Here, ideally, we should have some mechanism to makes the standard allocation mechanism
        // tapping directly on the bootstrap linear allocator over the start of heap space.
        // Unclear how to do that while the heap scheme is not initialized yet.
        // If we do, this code could migrate to the RegionTable class
        // Where we'd do the allocation of the region info in the constructor!

        // The region manager lays its data out at the beginning of the heap space as follows:

        final RegionTable regionTable = bootstrapAllocator.createTuple(RegionTable.class);
        for (int i = 0; i < numRegions; i++) {
            bootstrapAllocator.createTuple(regionInfoClass);
        }
        RegionTable.initialize(regionTable, regionInfoClass, startOfManagedSpace, numRegions);
        // Allocate the backing storage for the region list.
        int [] listStorage = (int[]) bootstrapAllocator.createArray(ClassRegistry.INT_ARRAY.dynamicHub(), numRegions);
        HeapRegionList.initializeListStorage(HeapRegionList.RegionListUse.ACCOUNTING, listStorage);
        listStorage = (int[]) bootstrapAllocator.createArray(ClassRegistry.INT_ARRAY.dynamicHub(), numRegions);
        HeapRegionList.initializeListStorage(HeapRegionList.RegionListUse.OWNERSHIP, listStorage);

        FatalError.check(bootstrapAllocator.end.roundedUpBy(regionSizeInBytes).lessEqual(startOfManagedSpace.plus(initialSize)), "");

        // Ready to open bootstrap heap accounts now.
        // Start with opening the boot heap account to set the records straight after bootstrap.
        // FIXME: initialSize may not be the reserve we want here. Need to adjust that to the desired "immortal" size.
        bootHeapAccount.open(initialNumRegions);
        // Allocate the region after the fact. This will straightened the data structures for the boot heap account and the region allocator.
        bootHeapAccount.allocate();
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

        return 0;
    }

    /**
     * Free contiguous regions.
     * @param firstRegionId identifier of the first region
     * @param numRegions
     */
    void free(int firstRegionId, int numRegions) {
        // TODO: error handling
        regionAllocator.free(firstRegionId, numRegions);
    }

    void commit(int firstRegionId, int numRegions) {
        // TODO: error handling
        regionAllocator.commit(firstRegionId, numRegions);
    }

    void uncommit(int firstRegionId, int numRegions) {
        // TODO: error handling
        regionAllocator.uncommit(firstRegionId, numRegions);
    }

    public void verifyAfterInitialization() {
        HeapRegionConstants.validate();
    }
}

