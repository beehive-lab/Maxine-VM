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

import java.lang.management.*;

import com.sun.max.collect.*;
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

        private static final IndexedSequence<MaxEntityMemoryRegion< ? extends MaxEntity>> EMPTY =
            new ArrayListSequence<MaxEntityMemoryRegion< ? extends MaxEntity>>(0);

        private final TeleHeapRegion owner;

        // The VM object that describes the region
        private final TeleRuntimeMemoryRegion teleRuntimeMemoryRegion;

        // The VM object that defines this region of memory, if it is a type
        // for which we know something about allocation.
        private final TeleLinearAllocationMemoryRegion teleLinearAllocationMemoryRegion;

        // The VM object that defines this region of memory, if it is a distinguished
        // type used for tracking inspection roots.
        private final TeleRootTableMemoryRegion teleRootTableMemoryRegion;

        private final boolean isBootRegion;

        protected DelegatedHeapRegionMemoryRegion(TeleVM teleVM, TeleHeapRegion owner, TeleRuntimeMemoryRegion teleRuntimeMemoryRegion, boolean isBootRegion) {
            super(teleVM, teleRuntimeMemoryRegion);
            this.owner = owner;
            assert teleRuntimeMemoryRegion != null;
            this.teleRuntimeMemoryRegion = teleRuntimeMemoryRegion;
            if (teleRuntimeMemoryRegion instanceof TeleLinearAllocationMemoryRegion) {
                this.teleLinearAllocationMemoryRegion = (TeleLinearAllocationMemoryRegion) teleRuntimeMemoryRegion;
            } else {
                this.teleLinearAllocationMemoryRegion = null;
            }
            if (teleRuntimeMemoryRegion instanceof TeleRootTableMemoryRegion) {
                this.teleRootTableMemoryRegion = (TeleRootTableMemoryRegion) teleRuntimeMemoryRegion;
            } else {
                this.teleRootTableMemoryRegion = null;
            }

            this.isBootRegion = isBootRegion;
        }

        @Override
        public MemoryUsage getUsage() {
            if (teleLinearAllocationMemoryRegion != null) {
                return new MemoryUsage(-1, teleLinearAllocationMemoryRegion.mark().minus(start()).toLong(), size().toLong(), -1);
            } else if (teleRootTableMemoryRegion != null) {
                return new MemoryUsage(-1, teleRootTableMemoryRegion.allocatedSize().toLong(), size().toLong(), -1);
            } else {
                return super.getUsage();
            }
        }

        @Override
        public boolean containsInAllocated(Address address) {
            if (teleLinearAllocationMemoryRegion == null) {
                return contains(address);
            }
            return address.greaterEqual(start()) && address.lessThan(teleLinearAllocationMemoryRegion.mark());
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            // Heap regions are allocated from the OS, not part of any other region
            return null;
        }

        public IndexedSequence<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
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

    public boolean isBootRegion() {
        return heapRegionMemoryRegion.isBootRegion();
    }

}
