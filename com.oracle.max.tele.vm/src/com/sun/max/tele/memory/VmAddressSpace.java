/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.memory;

import java.io.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;


/**
 * Maintains a map of memory allocated in the VM's address space.  It is updated
 * incrementally during the refresh cycle.
 * <p>
 * Managers for other entities in the VM are expected to register/unregister
 * top level memory allocations when they are identified during the refresh cycle.
 */
public class VmAddressSpace extends AbstractVmHolder implements MaxAddressSpace {

    private static final int TRACE_VALUE = 1;

    private static final String ADDRESS_SPACE_REGION_NAME = "Address Space";

    private static VmAddressSpace vmAddressSpace;

    public static VmAddressSpace make(TeleVM vm) {
        if (vmAddressSpace == null) {
            vmAddressSpace = new VmAddressSpace(vm);
        }
        return vmAddressSpace;
    }

    private final MaxEntityMemoryRegion<MaxAddressSpace> addressSpaceMemoryRegion;

    private final SortedMemoryRegionSet<MaxEntityMemoryRegion<? extends MaxEntity>> map
        = new SortedMemoryRegionSet<MaxEntityMemoryRegion<? extends MaxEntity>>();

    private VmAddressSpace(TeleVM vm) {
        super(vm);
        addressSpaceMemoryRegion = new VmAddressSpaceMemoryRegion(vm);
    }

    public String entityName() {
        return "Address Space";
    }

    public String entityDescription() {
        return "A map of the memory regions allocated in the VM's address space";
    }

    public MaxEntityMemoryRegion<MaxAddressSpace> memoryRegion() {
        return addressSpaceMemoryRegion;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Does any of the top level memory allocations contain the address?
     */
    public boolean contains(Address address) {
        return find(address) != null;
    }

    public TeleObject representation() {
        // TODO (mlvdv) is there any equivalent implementation object in the VM?  Will there be?
        return null;
    }

    /**
     * All known memory allocations, both those allocated by the VM and
     * those discovered from the OS, for example thread allocation areas and loaded
     * native libraries.
     * <p>
     * This collection is updated incrementally during the VM refresh cycle.  This means
     * that it may be inconsistent (unlike the list of regions recorded in the current
     * {@link MaxVMState}, which is not assigned until the conclusion of the refresh cycle.
     * However, it always reflects the most current information available, which is necessary
     * to have available <em>within</em> the refresh cycle.
     * <p>
     * Should never contain {@code null}
     *
     * @return an unmodifiable list of the current allocations, sorted by start location
     */
    public List<MaxEntityMemoryRegion<? extends MaxEntity> > allocations() {
        return map.regions();
    }

    /**
     * Gets the top level allocated memory region, if any, that includes the
     * specified memory location in the VM.
     * <p>
     * This method relies on the collection of allocated regions that is
     * updated incrementally during the update cycle.  It therefore has
     * the latest information, but may not be consistent.  For example, during
     * the update cycle the VM's direct allocations may have been refreshed, but
     * the allocations corresponding to threads may have not yet been refreshed.
     */
    public MaxEntityMemoryRegion<? extends MaxEntity> find(Address address) {
        return map.find(address);
    }

    /**
     * Registers a previously unknown top level memory allocation.
     * <p>
     * It is permitted for the location and size of an already registered memory region to change.
     * <p>
     * Registration of an <em>unallocated</em> (i.e. with {@code start=0}) is permitted.
     * An unallocated memory region will be treated as <em>empty</em> regardless of
     * the specified {@code size}, which should be 0.
     * <p>
     * Adding an already registered memory region (based on <em>object identity</em>, not location) has no effect.
     * <p>
     * Adding a registered region that overlaps an already existing region will succeed, but a
     * warning message will be generated the next time that the registered regions are
     * iterated.
     * <p>
     * Should the location and size of a registered memory region change in such a way that there
     * is an overlap between it an another registered memory region, a warning message will be
     * generated the next time that the registered regions are iterated.
     */
    public void add(MaxEntityMemoryRegion<? extends MaxEntity> allocation) {
        assert allocation != null;
        map.add(allocation);
    }

    /**
     * De-registers a previously registered top level memory allocation, matched based
     * on <em>object identity</em>, not location.
     *
     * @param allocation a previously registered top level memory allocation
     * @return {@code true} if removed, {@code false} if not found
     */
    public boolean remove(MaxEntityMemoryRegion<? extends MaxEntity> allocation) {
        assert allocation != null;
        return map.remove(allocation);
    }

    public void printSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        for (MaxEntityMemoryRegion<? extends MaxEntity> memoryRegion : this.map) {
            final StringBuilder line = new StringBuilder();
            if (memoryRegion.isAllocated()) {
                line.append(memoryRegion.start().to0xHexString());
                line.append(" - ");
                line.append((memoryRegion.end().minus(1)).to0xHexString());
            } else {
                line.append("<Unallocated>");
            }
            line.append(": " + memoryRegion.owner().entityName());
            printStream.println(indentation + line.toString());
        }
    }

    /**
     * Description of the address space occupied by the VM.
     * This region has no parent.
     * <p>
     * Children of this region are the top level extents of memory allocated from the OS, both
     * by the VM explicitly, and by native OS behavior:  thread stacks, native libraries, etc.
     */
    private final class VmAddressSpaceMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxAddressSpace> {

        protected VmAddressSpaceMemoryRegion(MaxVM vm) {
            super(vm, ADDRESS_SPACE_REGION_NAME, Address.fromInt(1), Long.MAX_VALUE);
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            // This is the root of the memory hierarchy
            return null;
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            final List<MaxEntityMemoryRegion< ? extends MaxEntity>> children
                = new ArrayList<MaxEntityMemoryRegion< ? extends MaxEntity>>();
            for (MaxEntityMemoryRegion<? extends MaxEntity> region : map) {
                children.add(region);
            }
            return children;
        }

        public MaxAddressSpace owner() {
            return VmAddressSpace.this;
        }

    }


    /**
     * A set of memory regions sorted by location, with some special properties.
     * <ul>
     * <li>Adding a duplicate memory region (by object identity) will have no effect</li>
     * <li>Adding a region that overlaps with another will succeed with a warning message, although this is not likely to be discovered immediately</li>
     * <li>Any region with {@code start() == Address.zero())} is considered <em>unallocated</em> and <em>empty</em></li>
     * <li>Any number of unallocated regions may be in the list; they will sort by name at the beginning of the list</li>
     * <li>Adding an unallocated region with {@code size > 0} will succeed with a warning message, but it will be treated as <em>empty</em></li>
     * <li>The memory location of a set member might on occasion change, most typically when an <em>unallocated</em> region becomes <em>allocated</em></li>
     * <ul>
     * This is a very simple implementation, not designed for large sets.
     */
    private final class SortedMemoryRegionSet<Region_Type extends MaxEntityMemoryRegion> implements Iterable<Region_Type> {

        /**
         * Compares first by starting address, then by entity name (which should only be needed for unallocated regions).
         */
        final Comparator<Region_Type> regionComparator = new Comparator<Region_Type>() {
            @Override
            public int compare(Region_Type o1, Region_Type o2) {
                final int result = o1.start().compareTo(o2.start());
                return result == 0 ?  o1.owner().entityName().compareTo(o2.owner().entityName()) : result;
            }
        };

        /**
         * The set of regions, sorted lazily by starting address.
         * <p>
         * Checking for overlaps takes place at sort time.
         */
        private Region_Type[] regions;
        private int size;
        private boolean sorted;

        private SortedMemoryRegionSet() {
            Class<Region_Type[]> type = null;
            regions = Utils.cast(type, new MaxEntityMemoryRegion[20]);
            size = 0;
            sorted = true;
        }

        int size() {
            return size;
        }

        List<Region_Type> regions() {
            return Collections.unmodifiableList(Arrays.asList(Arrays.copyOf(regions, size)));
        }

        /**
         * Searches for an element in this list based on an address.
         */
        synchronized Region_Type find(Address address) {
            for (Region_Type region : this) {
                if (region.contains(address)) {
                    return region;
                }
            }
            return null;
        }

        /**
         * Adds an element to this list.
         */
        synchronized void add(Region_Type newRegion) {
            // Linear search to discover duplicates; not necessarily sorted.
            for (int index = 0; index < size; index++) {
                if (regions[index] == newRegion) {
                    Trace.line(TRACE_VALUE, tracePrefix() + "Add ignoring duplicate: " + newRegion.owner().entityName());
                    return;
                }
            }
            if (regions.length == size) {
                int newCapacity = (regions.length * 3) / 2 + 1;
                regions = Arrays.copyOf(regions, newCapacity);
            }
            regions[size] = newRegion;
            sorted = false;
            size++;
            Trace.line(TRACE_VALUE, tracePrefix() + "Adding new: " + newRegion.owner().entityName());
        }

        synchronized boolean remove(Region_Type oldRegion) {
            // Linear search to discover duplicates; not necessarily sorted.
            for (int index = 0; index < size; index++) {
                if (regions[index] == oldRegion) {
                    if (index < size - 1) {
                        System.arraycopy(regions, index + 1, regions, index, size - (index + 1));
                    }
                    size--;
                    Trace.line(TRACE_VALUE, tracePrefix() + "Removing: " + oldRegion.owner().entityName());
                    return true;
                }
            }
            Trace.line(TRACE_VALUE, tracePrefix() + "Removal failed: " + oldRegion.owner().entityName());
            return false;
        }

        @Override
        public synchronized Iterator<Region_Type> iterator() {
            if (!sorted) {
                Arrays.sort(regions, 0, size, regionComparator);
                sorted = true;
            }
            Region_Type[] listCopy = Arrays.copyOf(regions, size);
            return Arrays.asList(listCopy).iterator();
        }

        /**
         * Sort by starting address.
         */
        private void ensureSorted() {
            if (!sorted) {
                Arrays.sort(regions, 0, size, regionComparator);
                sorted = true;
                for (Region_Type region : regions) {
                    Region_Type previous = null;
                    if (previous != null && previous.start().isNotZero() &&  previous.end().greaterThan(region.start())) {
                        TeleWarning.message("Overlapping regions discovered: " + previous.owner().entityName());
                    }
                }
            }
        }
    }

}
