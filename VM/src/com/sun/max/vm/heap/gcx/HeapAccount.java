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
import static com.sun.max.vm.heap.gcx.HeapRegionManager.*;

/**
 * Backing storage for a heap is managed via a heap account created on demand by the {@link HeapRegionManager}.
 * A heap account provides a guaranteed reserve of space, corresponding to the maximum space required
 * by the account owner. Space is expressed in terms of number of heap regions, whose size is defined in {@link HeapRegionConstants}.
 * The account owner can allocate regions on demand up to the account's reserve.
 *
 *
 * @author Laurent Daynes
 */
public class HeapAccount<Owner>{
    /**
     * Owner of the account. Typically, some heap implementation.
     */
    private Owner owner;
    /**
     * Guaranteed reserve of regions for this account.
     */
    private int reserve;

    /**
     * List of regions allocated to the account owner. All allocated regions are committed
     */
    private HeapRegionList allocated;

    HeapAccount(Owner owner, int reserve) {
        this.owner = owner;
        this.reserve = reserve;

    }

    /**
     * Number of regions in the reserve.
     * @return a number of regions.
     */
    int reserve() {
        return reserve;
    }
    /**
     * The owner of the heap account.
     * @return an object
     */
    public Owner owner() { return owner; }

    /**
     *
     * @return
     */
    public int allocate() {
        if (allocated.size() < reserve) {
            int regionID = theHeapRegionManager.regionAllocator().allocate();
            if (regionID != INVALID_REGION_ID) {
                allocated.prepend(regionID);
            }
        }
        return INVALID_REGION_ID;
    }

}
