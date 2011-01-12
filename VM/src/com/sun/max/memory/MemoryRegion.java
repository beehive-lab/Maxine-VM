/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.unsafe.*;

/**
 * A named region of memory in the VM.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 * @author Doug Simon
 */
public class MemoryRegion {

    /**
     * An optional, short string that describes the role being played by the region, useful for debugging.
     */
    @INSPECTED
    private String regionName = "<unnamed>";

    @INSPECTED
    protected Address start;

    @INSPECTED
    protected Size size;

    public MemoryRegion() {
        start = Address.zero();
        size = Size.zero();
    }

    public MemoryRegion(Size size) {
        start = Address.zero();
        this.size = size;
    }

    public MemoryRegion(Address start, Size size) {
        this.start = start;
        this.size = size;
    }

    public MemoryRegion(MemoryRegion memoryRegion) {
        start = memoryRegion.start();
        size = memoryRegion.size();
    }

    public MemoryRegion(String regionName) {
        this.regionName = regionName;
        start = Address.zero();
        size = Size.zero();
    }

    @INLINE
    public final Address start() {
        return start;
    }

    public void setStart(Address start) {
        this.start = start;
    }

    public final Size size() {
        return size;
    }

    public final void setSize(Size size) {
        this.size = size;
    }

    public final void setEnd(Address end) {
        size = end.minus(start).asSize();
    }

    public final String regionName() {
        return regionName;
    }

    /**
     * Sets the name that describes role being played by this region.
     */
    public final void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public final Address end() {
        return start().plus(size());
    }

    public final boolean contains(Address address) {
        return address.greaterEqual(start()) && address.lessThan(end());
    }

    public final boolean overlaps(MemoryRegion memoryRegion) {
        return start().lessThan(memoryRegion.end()) && end().greaterThan(memoryRegion.start());
    }

    public final boolean sameAs(MemoryRegion otherMemoryRegion) {
        if (otherMemoryRegion == null) {
            return false;
        }
        return start.equals(otherMemoryRegion.start) && size.equals(otherMemoryRegion.size);
    }

    public MemoryUsage getUsage() {
        return null;
    }

    @Override
    public String toString() {
        return "[" + start.toHexString() + " - " + end().minus(1).toHexString() + "]";
    }
}
