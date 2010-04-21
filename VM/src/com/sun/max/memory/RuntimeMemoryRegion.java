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
import com.sun.max.unsafe.*;

public class RuntimeMemoryRegion implements MemoryRegion {

    /**
     * An optional, short string that describes the role being played by the region, useful for debugging.
     */
    @INSPECTED
    private String regionName = "<unnamed>";

    @INSPECTED
    protected Address start;

    @INSPECTED
    protected Size size;

    public RuntimeMemoryRegion() {
        start = Address.zero();
        size = Size.zero();
    }

    public RuntimeMemoryRegion(Size size) {
        start = Address.zero();
        this.size = size;
    }

    public RuntimeMemoryRegion(Address start, Size size) {
        this.start = start;
        this.size = size;
    }

    public RuntimeMemoryRegion(MemoryRegion memoryRegion) {
        start = memoryRegion.start();
        size = memoryRegion.size();
    }

    public RuntimeMemoryRegion(String regionName) {
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
        return Util.equal(this, otherMemoryRegion);
    }

    public MemoryUsage getUsage() {
        return null;
    }

    @Override
    public String toString() {
        return "[" + start.toHexString() + " - " + end().minus(1).toHexString() + "]";
    }


}
