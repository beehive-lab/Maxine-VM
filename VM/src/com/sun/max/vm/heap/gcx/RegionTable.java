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
import com.sun.max.vm.reference.*;

/**
 * The heap region table centralizes all the descriptors for all regions in the heap space.
 * Region descriptors of all the regions are allocated in a single contiguous space in order
 * to enable a simple mapping from region address to region descriptor, which in turn, enables
 * direct retrieval of a region descriptor from an arbitrary pointer to the heap space.
 * The index in the region table forms a unique region identifier that can be simply maps to a region
 * descriptor and the region address in the heap space. The region table occupies the first bytes of
 * the heap space.
 *
 * @author Laurent Daynes
 */
public final class RegionTable {
    static final int TableOffset = ClassActor.fromJava(RegionTable.class).dynamicTupleSize().toInt();

    private Pointer table() {
        return Reference.fromJava(this).toOrigin().plus(TableOffset);
    }
    @CONSTANT_WHEN_NOT_ZERO
    private int length;

    /**
     * Region size in bytes.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private int regionSizeInBytes;

    @CONSTANT_WHEN_NOT_ZERO
    private int log2RegionSizeInBytes;

    /**
     * Base address of the contiguous space backing up the heap regions.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private Pointer regionBaseAddress;

    final int regionInfoSize;

    public RegionTable(Class<HeapRegionInfo> regionInfoClass) {
        this.regionInfoSize = ClassActor.fromJava(regionInfoClass).dynamicTupleSize().toInt();
    }

    public void initialize(int length) {
        this.length = length;
        this.regionSizeInBytes = HeapRegionInfo.regionSizeOption.getValue().toInt();
        log2RegionSizeInBytes = Integer.numberOfTrailingZeros(this.regionSizeInBytes);
    }

    HeapRegionInfo addressToRegion(Address addr) {
        final int rindex = addr.minus(regionBaseAddress).unsignedShiftedRight(log2RegionSizeInBytes).toInt();
        final Pointer raddr = table().plus(rindex * regionSizeInBytes);
        return HeapRegionInfo.toHeapRegionInfo(raddr);
    }
}
