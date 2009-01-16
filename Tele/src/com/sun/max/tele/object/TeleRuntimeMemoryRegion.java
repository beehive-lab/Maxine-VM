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
package com.sun.max.tele.object;

import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for objects in the {@link TeleVM} that represent a region of memory.
 *
 * @author Michael Van De Vanter
 */
public class TeleRuntimeMemoryRegion extends TeleTupleObject implements MemoryRegion {

    TeleRuntimeMemoryRegion(TeleVM teleVM, Reference runtimeMemoryRegionReference) {
        super(teleVM, runtimeMemoryRegionReference);
    }

    /**
     * Reads from the {@link TeleVM} the start field of the {@link RuntimeMemoryRegion}.
     */
    private Address readStart() {
        return teleVM().fields().RuntimeMemoryRegion_start.readWord(reference()).asAddress();
    }

    public Address start() {
        // No caching for now
        Address start = readStart();
        if (start.isZero() && this == teleVM().teleHeapManager().teleBootHeapRegion()) {
            // Ugly special case:  the start field of the static that defines the boot heap region
            // is set at zero in the boot image, only set to the real value when the VM starts running.
            // Lie about it.
            start = teleVM().bootImageStart();
        }
        return start;
    }

    /**
     * @return whether memory has been allocated yet in the {@link TeleVM} for this region.
     */
    public boolean isAllocated() {
        return !start().isZero();
    }

    /**
     * Reads from the {@link TeleVM} the mark field of the {@link RuntimeMemoryRegion}.
     */
    public Address mark() {
        return teleVM().fields().RuntimeMemoryRegion_mark.readWord(reference()).asAddress();
    }

    /**
     * @return how much memory in region has been allocated to objects, {@link Size#zero()) if memory for region not allocated.
     */
    public Size allocatedSize() {
        if (isAllocated()) {
            final Address mark = mark();
            if (!mark.isZero()) {
                return mark.minus(start()).asSize();
            }
        }
        return Size.zero();
    }

    /**
     * Reads from the {@link TeleVM} the size field of the {@link RuntimeMemoryRegion}.
     */
    public Size size() {
        return teleVM().fields().RuntimeMemoryRegion_size.readWord(reference()).asSize();
    }

    public Address end() {
        return start().plus(size());
    }

    public String description() {
        final Reference descriptionStringReference = teleVM().fields().RuntimeMemoryRegion_description.readReference(reference());
        final TeleString teleString = (TeleString) makeTeleObject(descriptionStringReference);
        return teleString.getString();
    }


    public boolean contains(Address address) {
        return address.greaterEqual(start()) && address.lessThan(end());
    }

    public boolean overlaps(MemoryRegion memoryRegion) {
        final Address start = start();
        final Address end = end();
        return start.greaterEqual(memoryRegion.start()) && start.lessThan(memoryRegion.end()) ||
            end.greaterEqual(memoryRegion.start()) && end.lessThan(memoryRegion.end());
    }

    public boolean sameAs(MemoryRegion otherMemoryRegion) {
        return otherMemoryRegion != null && start().equals(otherMemoryRegion.start()) && size().equals(otherMemoryRegion.size());
    }
}
