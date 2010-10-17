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
package com.sun.max.memory;

import java.lang.management.*;

import com.sun.max.annotate.*;
import com.sun.max.atomic.*;
import com.sun.max.unsafe.*;


/**
 * A runtime allocated region of memory in the VM with an allocation mark.
 *
 * @author Michael Van De Vanter
 */
public class LinearAllocationMemoryRegion extends MemoryRegion {

    public LinearAllocationMemoryRegion() {
        super();
    }

    public LinearAllocationMemoryRegion(Address start, Size size) {
        super(start, size);
    }

    public LinearAllocationMemoryRegion(MemoryRegion memoryRegion) {
        super(memoryRegion);
    }

    public LinearAllocationMemoryRegion(Size size) {
        super(size);
    }

    public LinearAllocationMemoryRegion(String description) {
        super(description);
    }

    /**
     * The current allocation mark. This is an atomic word so that it can be updated
     * atomically if necessary.
     */
    @INSPECTED
    public final AtomicWord mark = new AtomicWord();

    /**
     * Gets the address just past the last allocated/used location in the region.
     *
     * If this region is not used for allocation, then the value returned by this
     * method will be identical to the value returned by {@link #end()}.
     */
    @INLINE
    public final Pointer mark() {
        return mark.get().asPointer();
    }

    @Override
    public MemoryUsage getUsage() {
        final long sizeAsLong = size.toLong();
        return new MemoryUsage(sizeAsLong, getAllocationMark().minus(start).toLong(), sizeAsLong, sizeAsLong);
    }

    @INLINE
    public final Address getAllocationMark() {
        return mark.get().asAddress();
    }

    protected Size used() {
        return getAllocationMark().minus(start).asSize();
    }

}
