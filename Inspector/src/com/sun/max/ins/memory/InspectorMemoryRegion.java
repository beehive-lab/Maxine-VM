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
package com.sun.max.ins.memory;

import java.lang.management.*;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;

/**
 * Representation of a span of memory in the VM.
 *
 * @author Michael Van De Vanter
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
    private final Size size;

    private MemoryUsage memoryUsage = null;

    public InspectorMemoryRegion(MaxVM maxVM, String regionName, Address start, Size size) {
        this.vm = maxVM;
        this.start = start;
        this.size = size;
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

    public final Size size() {
        return size;
    }

    public final Address end() {
        return start().plus(size());
    }

    public final void setDescription(String regionName) {
        this.regionName = regionName;
    }

    public final boolean contains(Address address) {
        return MaxMemoryRegion.Util.contains(this, address);
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
        return Util.equal(this, otherMemoryRegion);
    }

    public MemoryUsage getUsage() {
        if (memoryUsage == null) {
            // Lazy initialization to avoid object creation circularities
            // The default usage is 100%, i.e. the region is completely used.
            this.memoryUsage = MaxMemoryRegion.Util.defaultUsage(this);
        }
        return memoryUsage;
    }
}
