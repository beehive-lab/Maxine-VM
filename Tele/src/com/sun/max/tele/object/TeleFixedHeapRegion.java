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
package com.sun.max.tele.object;

import java.util.*;

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

        private static final List<MaxEntityMemoryRegion< ? extends MaxEntity>> EMPTY = Collections.emptyList();

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

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
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
