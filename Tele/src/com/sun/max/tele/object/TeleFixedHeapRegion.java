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

import com.sun.max.collect.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.unsafe.*;

public class TeleFixedHeapRegion extends AbstractTeleVMHolder implements MaxHeapRegion {

    private static final int TRACE_LEVEL = 2;

    /**
     * Description of a simple memory region allocated by the VM heap, where
     * the description is known completely.
     * <br>
     * This region has no parent; it is allocated from the OS. <br>
     * This region has no children. We could decompose the region into sub-regions representing individual objects, but
     * we don't do that at this time.
     */
    private static final class FixedHeapRegionMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxHeapRegion> {

        private static final IndexedSequence<MaxEntityMemoryRegion< ? extends MaxEntity>> EMPTY =
            new ArrayListSequence<MaxEntityMemoryRegion< ? extends MaxEntity>>(0);

        private final TeleFixedHeapRegion owner;
        private final boolean isBootRegion;

        protected FixedHeapRegionMemoryRegion(TeleVM teleVM, TeleFixedHeapRegion owner, String regionName, Address start, Size size, boolean isBootRegion) {
            super(teleVM, regionName, start, size);
            this.owner = owner;
            this.isBootRegion = isBootRegion;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            // Heap regions are allocated from the OS, not part of any other region
            return null;
        }

        public IndexedSequence<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            // We don't break a heap memory region into any smaller entities, but could.
            return EMPTY;
        }

        public TeleFixedHeapRegion owner() {
            return owner;
        }

        public boolean isBootRegion() {
            return isBootRegion;
        }
    }

    private final String entityDescription;
    private final FixedHeapRegionMemoryRegion heapRegionMemoryRegion;

    /**
     * Creates an object that models an allocation region in the VM that is used by the heap, in situations
     * where the descriptors for the region are known and fixed.
     * <br>
     * This variation is only needed during the startup sequence, when we can deduce the characteristics
     * of the boot heap region, but there isn't yet a well-formed object in the VM that describes the region.
     *
     * @param teleVM the VM
     * @param name a short description of the region
     * @param start starting location of the region in the VM
     * @param size size of the region in the VM
     * @param isBootRegion whether this region is in the boot image.
     */
    public TeleFixedHeapRegion(TeleVM teleVM, String name, Address start, Size size, boolean isBootRegion) {
        super(teleVM);
        this.entityDescription = "A unit of allocation area for a heap in the " + vm().entityName();
        this.heapRegionMemoryRegion = new FixedHeapRegionMemoryRegion(teleVM, this, name, start, size, isBootRegion);
    }

    public String entityName() {
        return heapRegionMemoryRegion.regionName();
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxHeapRegion> memoryRegion() {
        return heapRegionMemoryRegion;
    }

    public boolean contains(Address address) {
        return heapRegionMemoryRegion.contains(address);
    }

    public boolean isBootRegion() {
        return heapRegionMemoryRegion.isBootRegion();
    }

}
