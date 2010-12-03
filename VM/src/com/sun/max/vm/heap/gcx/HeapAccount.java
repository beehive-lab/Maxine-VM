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

import com.sun.max.unsafe.*;

/**
 * Heap space is managed via heap accounts created on demand by the {@link HeapRegionManager}.
 * A heap account provide a guaranteed reserve of space a heap can allocate regions from.
 * A heap may grow its account, which increase its guaranteed reserve, or shrink it by a
 * number of regions less or equal to the number of free region in its account.
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
     * Guaranteed reserve of space for this account.
     */
    private Size reserve;
    /**
     * List of regions allocated to the account owner.
     */
    private HeapRegionList allocated;
    /**
     * List of committed regions (i.e., allocated backing storage in real memory),
     * but not allocated.
     */
    private HeapRegionList committed;

    HeapAccount(Owner owner, Size reserve) {
        this.owner = owner;
        this.reserve = reserve;

    }

    public Owner owner() { return owner; }

}
