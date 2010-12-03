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

import com.sun.max.annotate.*;
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
public class HeapRegionManager {
    /**
     * Backing storage for the space managed by the region manager.
     * Provides interface to commit/uncommit space to regions.
     */
    private ContiguousHeapSpace backingStorage;

    private RegionTable regionTable;

    /**
     * Heap account serving the needs of the heap region manager.
     */
    private HeapAccount<HeapRegionManager> bootHeapAccount;

    /**
     * Total number of regions.
     */
    private int capacity;

    /**
     * Total number of unreserved regions.
     */
    private int unreserved;

    // Region Management interface private to Heap Account.
    // May want to revisit how the two interacts to better control
    // use of these sensitive operations.

    /**
     * Reserve exactly the number of regions requested, or fail.
     * @param numRegions number of region requested
     * @return true if the number of regions requested was reserved
     */
    boolean reserveOrFail(int numRegions) {
        if (numRegions > unreserved) {
            return false;
        }
        unreserved -= numRegions;
        return true;
    }

    void release(int numRegions) {
        FatalError.check((unreserved + numRegions) <= capacity, "invalid request");
        unreserved += numRegions;
    }

    /**
     * Backing storage for the heap account lists tracking region ownership.
     */
    private int [] heapAccountListStorage;


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


    HeapRegionManager() {
        backingStorage = new ContiguousHeapSpace();
        bootstrapAllocator = new BootstrapAllocator();
    }

    private Size tupleSize(Class tupleClass) {
        return ClassActor.fromJava(tupleClass).dynamicTupleSize();
    }

    /**
     * Initialize the region manager with the supplied space.
     * As many regions as possible are carved out from this space, while preserving alignment constraints.
     * The region size is obtained from the HeapRegionInfo class.
     *
     * @param reservedSpace address to the first byte of the virtual memory reserved for the heap space
     * @param reservedSpaceSize size in byte of the heap space
     * @param regionInfoClass the sub-class of HeapRegionInfo used for region management.
     */
    public void initialize(Address reservedSpace, Size reservedSpaceSize, Class<HeapRegionInfo> regionInfoClass) {
        // Initialize region constants (size and log constants).
        HeapRegionInfo.initializeConstants();
        // Adjust reserved space to region boundaries.
        final Address endOfHeapSpace = reservedSpace.plus(reservedSpaceSize).roundedDownBy(HeapRegionInfo.regionSizeInBytes);
        final Address startOfHeapSpace = reservedSpace.roundedUpBy(HeapRegionInfo.regionSizeInBytes);
        final Size heapSpaceSize = endOfHeapSpace.minus(endOfHeapSpace).asSize();
        final int numRegions = heapSpaceSize.unsignedShiftedRight(HeapRegionInfo.log2RegionSizeInBytes).toInt();

        backingStorage.setReserved(startOfHeapSpace, heapSpaceSize);

        // Estimate conservatively what the heap manager needs initially. This is to commit
        // enough memory to get started.
        // FIXME: initial size should be made to correspond to some notion of initial heap.

        // 1. The region info table:
        Size initialSize = tupleSize(regionInfoClass).plus(tupleSize(RegionTable.class));
        // 2. The backing storage for the accounts' region lists
        initialSize = initialSize.plus(Layout.getArraySize(Kind.INT, numRegions * 2));
        // Round this to an integral number of regions.
        initialSize = initialSize.roundedUpBy(HeapRegionInfo.regionSizeInBytes);

        // Commit space and initialize the bootstrap allocator
        backingStorage.growCommittedSpace(initialSize);
        bootstrapAllocator.initialize(startOfHeapSpace, backingStorage.committedEnd().asPointer());
        // FIXME: Here, ideally, we should have some mechanism to makes the standard allocation mechanism
        // tapping directly on the bootstrap linear allocator over the start of heap space.

        // The region manager lays its data out at the beginning of the heap space as follows:

        regionTable = bootstrapAllocator.createTuple(RegionTable.class);
        for (int i = 0; i < numRegions; i++) {
            bootstrapAllocator.createTuple(regionInfoClass);
        }
        regionTable.initialize(numRegions);
        // Allocate the backing storage for account allocation lists.
        heapAccountListStorage = (int[]) bootstrapAllocator.createArray(ClassRegistry.INT_ARRAY.dynamicHub(), numRegions);
        // Ready to open heap accounts now.
    }

    /**
     * Opening a heap account for the specified owner.
     * @param <Owner>
     * @param owner
     * @return
     */
    public <Owner> HeapAccount<Owner> openHeapAccount(Owner owner) {
        Size initialReserve = Size.zero();
        // Problem starts here: where should we allocate this ? We don't want this to be
        // on the heap account itself as we don't want references from heap management to
        // leak to other heaps.
        HeapAccount<Owner> account = new HeapAccount<Owner>(owner, initialReserve);
        return account;
    }

    public <Owner> void close(HeapAccount<Owner> account) {
        // TODO
    }

}

