/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

public class TeleHeapRegion extends AbstractTeleVMHolder implements MaxHeapRegion {

    private static final int TRACE_LEVEL = 2;

    /**
     * Description of an ordinary memory region allocated by the VM heap.
     * Extra information (for example, usage statistics and liveness map of specific addresses) is provided for
     * those VM heap types that make this information available.
     * <br>
     * This region has no parent; it is allocated from the OS.
     * <br>
     * This region has no children. We could decompose the region into sub-regions representing individual objects, but
     * we don't do that at this time.
     */
    private static final class DelegatedHeapRegionMemoryRegion extends TeleDelegatedMemoryRegion implements MaxEntityMemoryRegion<MaxHeapRegion> {

        private static final List<MaxEntityMemoryRegion< ? extends MaxEntity>> EMPTY = Collections.emptyList();

        private final TeleHeapRegion owner;

        private final boolean isBootRegion;

        protected DelegatedHeapRegionMemoryRegion(TeleVM teleVM, TeleHeapRegion owner, TeleRuntimeMemoryRegion teleRuntimeMemoryRegion, boolean isBootRegion) {
            super(teleVM, teleRuntimeMemoryRegion);
            this.owner = owner;
            assert teleRuntimeMemoryRegion != null;
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

        public TeleHeapRegion owner() {
            return owner;
        }

        public boolean isBootRegion() {
            return isBootRegion;
        }
    }

    private final String entityDescription = "An allocation area for a Maxine VM heap";
    private final TeleRuntimeMemoryRegion teleRuntimeMemoryRegion;
    private final DelegatedHeapRegionMemoryRegion heapRegionMemoryRegion;

    /**
     * Creates an object that models an allocation region in the VM that is used by the heap. The
     * description for the region is delegated to a VM object that holds the description.
     *
     * @param teleVM the VM
     * @param teleRuntimeMemoryRegion the VM object that describes the memory allocated
     * @param isBootRegion whether this region is in the boot image.
     */
    public TeleHeapRegion(TeleVM teleVM, TeleRuntimeMemoryRegion teleRuntimeMemoryRegion, boolean isBootRegion) {
        super(teleVM);
        this.teleRuntimeMemoryRegion = teleRuntimeMemoryRegion;
        this.heapRegionMemoryRegion = new DelegatedHeapRegionMemoryRegion(teleVM, this, teleRuntimeMemoryRegion, isBootRegion);
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

    public TeleObject representation() {
        return teleRuntimeMemoryRegion;
    }

    public boolean isBootRegion() {
        return heapRegionMemoryRegion.isBootRegion();
    }

}
