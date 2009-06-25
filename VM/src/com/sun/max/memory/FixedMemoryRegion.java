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

import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 */
public class FixedMemoryRegion implements MemoryRegion {

    private final Address start;

    public Address start() {
        return start;
    }

    private final Size size;

    public Size size() {
        return size;
    }

    private final Address end;

    public Address end() {
        return end;
    }

    private String description;

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FixedMemoryRegion(Address start, Size size, String description) {
        this.start = start;
        this.size = size;
        this.end = start.plus(size);
        this.description = description;
    }

    public FixedMemoryRegion(MemoryRegion memoryRegion, String description) {
        start = memoryRegion.start();
        size = memoryRegion.size();
        end = memoryRegion.end();
        this.description = description;
    }

    public boolean contains(Address address) {
        return address.greaterEqual(start()) && address.lessThan(end());
    }

    public boolean overlaps(MemoryRegion memoryRegion) {
        return (start.greaterEqual(memoryRegion.start()) && start.lessThan(memoryRegion.end())) ||
               (end.greaterEqual(memoryRegion.start()) && end.lessThan(memoryRegion.end()));
    }

    public boolean sameAs(MemoryRegion otherMemoryRegion) {
        return otherMemoryRegion != null && start().equals(otherMemoryRegion.start()) && size().equals(otherMemoryRegion.size());
    }
}
