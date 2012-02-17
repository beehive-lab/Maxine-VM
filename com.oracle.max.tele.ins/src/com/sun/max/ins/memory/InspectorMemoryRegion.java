/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.memory;

import java.lang.management.*;

import com.sun.max.tele.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * Representation of a span of memory in the VM.
 */
public class InspectorMemoryRegion implements MaxMemoryRegion {

    private final MaxVM vm;
    private String regionName;

    /**
     * Default start location, as of creation time.
     */
    private final Address start;

    /**
     * Default size, as of creation time.
     */
    private final long nBytes;

    private MemoryUsage memoryUsage = null;

    public InspectorMemoryRegion(MaxVM maxVM, String regionName, Address start, long nBytes) {
        TeleError.check(nBytes == 0 || start.isNotZero(), "Non-empty memory regions may not start address 0");
        this.vm = maxVM;
        this.start = start;
        this.nBytes = nBytes;
        this.regionName = regionName;
    }

    public final MaxVM vm() {
        return vm;
    }

    public final String regionName() {
        return regionName;
    }

    public final Address start() {
        return start;
    }

    public final long nBytes() {
        return nBytes;
    }

    public final Address end() {
        return start().plus(nBytes);
    }

    public final boolean isAllocated() {
        return MaxMemoryRegion.Util.isAllocated(this);
    }

    public final void setDescription(String regionName) {
        this.regionName = regionName;
    }

    public final boolean contains(Address address) {
        return MaxMemoryRegion.Util.contains(this, address);
    }

    public Address mark() {
        // By default, assume that the region is not linearly allocated.
        return null;
    }

    public boolean containsInAllocated(Address address) {
        // By default, assume that the whole region is allocated.
        // Override this for specific region that have internal
        // allocation that can be checked.
        return contains(address);
    }

    public final boolean overlaps(MaxMemoryRegion memoryRegion) {
        return MaxMemoryRegion.Util.overlaps(this, memoryRegion);
    }

    public final boolean sameAs(MaxMemoryRegion otherMemoryRegion) {
        return MaxMemoryRegion.Util.equal(this, otherMemoryRegion);
    }

    public final MemoryUsage getUsage() {
        if (memoryUsage == null) {
            // Lazy initialization to avoid object creation circularities
            // The default usage is 100%, i.e. the region is completely used.
            this.memoryUsage = MaxMemoryRegion.Util.defaultUsage(this);
        }
        return memoryUsage;
    }
}
