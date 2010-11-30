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
import com.sun.max.vm.runtime.*;


public class RegionTable< R extends HeapRegionHeader> {
    @CONSTANT_WHEN_NOT_ZERO
    private Pointer table = Pointer.zero();
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

    final int regionHeaderSize;

    final Class <R>regionClass;

    public RegionTable(Class<R> regionClass) {
        this.regionClass = regionClass;
        this.regionHeaderSize = ClassActor.fromJava(regionClass).dynamicTupleSize().toInt();
    }

    public void initialize(Pointer table, int length) {
        FatalError.check(table.isZero(), "must be initialized once only");
        this.table = table;
        this.length = length;
        this.regionSizeInBytes = HeapRegionHeader.regionSizeOption.getValue().toInt();
        log2RegionSizeInBytes = Integer.numberOfTrailingZeros(this.regionSizeInBytes);
    }

    R addressToRegion(Address addr) {
        final int rindex = addr.minus(regionBaseAddress).unsignedShiftedRight(log2RegionSizeInBytes).toInt();
        final Pointer raddr = table.plus(rindex * regionSizeInBytes);
        return regionClass.cast(Reference.fromOrigin(raddr).toJava());
    }
}
