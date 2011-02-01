/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
