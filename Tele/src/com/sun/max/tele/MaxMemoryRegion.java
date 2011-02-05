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
package com.sun.max.tele;

import java.lang.management.*;
import java.util.*;

import com.sun.max.unsafe.*;

/**
 * Description of a region of memory in the VM.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public interface MaxMemoryRegion {

    /**
     * @return the VM
     */
    MaxVM vm();

    /**
     * @return a short human-readable name for this memory region that helps explain
     * the role it plays; suitable for appearance in a table
     * cell or menu item.
     */
    String regionName();

    /**
     * @return address of the first location in the region.
     */
    Address start();

    /**
     * @return number of bytes in the region.
     */
    long nBytes();

    /**
     * @return address just past the last location in the region.
     */
    Address end();

    /**
     * @return a description of the memory usage for this region, NULL_USAGE if not allocated or no information available.
     */
    MemoryUsage getUsage();

    /**
     * @return does the region contain the address.
     */
    boolean contains(Address address);

    /**
     * Checks whether a VM memory address is within the allocated
     * area of a VM memory region.  If the VM memory region has no
     * notion of internal allocation, or if internal allocation cannot
     * reasonably be determined, then this is equivalent to {@link #contains(Address)}.
     *
     * @param address a VM memory location
     * @return whether the location is within the allocated part of this VM
     * memory region.
     */
    boolean containsInAllocated(Address address);

    /**
     * @return does the region have any locations in common with another region.
     */
    boolean overlaps(MaxMemoryRegion memoryRegion);

    /**
     * @return does the region have the same bounds as another region.
     * @see Util#equal(MemoryRegion, MemoryRegion)
     */
    boolean sameAs(MaxMemoryRegion memoryRegion);

    public static final class Util {

        public static MemoryUsage  NULL_MEMORY_USAGE = new MemoryUsage(-1L, 0L, 0L, -1L);

        private static final Comparator<MaxMemoryRegion> START_COMPARATOR = new Comparator<MaxMemoryRegion>() {

            public int compare(MaxMemoryRegion mr1, MaxMemoryRegion mr2) {
                return mr1.start().compareTo(mr2.start());
            }
        };

        /**
         * @return a comparator that operates on the start location of two non-null memory regions
         */
        public static Comparator<MaxMemoryRegion> startComparator() {
            return START_COMPARATOR;
        }

        private static final Comparator<MaxMemoryRegion> NAME_COMPARATOR = new Comparator<MaxMemoryRegion>() {

            public int compare(MaxMemoryRegion mr1, MaxMemoryRegion mr2) {
                return mr1.regionName().compareTo(mr2.regionName());
            }
        };

        /**
         * @return a comparator that operates on the start location of two non-null memory regions
         */
        public static Comparator<MaxMemoryRegion> nameComparator() {
            return NAME_COMPARATOR;
        }

        /**
         * @return whether the VM memory region contains a specific address
         */
        public static boolean contains(MaxMemoryRegion memoryRegion, Address address) {
            return address.greaterEqual(memoryRegion.start()) && address.lessThan(memoryRegion.end());
        }

        /**
         * @return whether two VM memory regions contain any location in common
         */
        public static boolean overlaps(MaxMemoryRegion left, MaxMemoryRegion right) {
            return left.start().lessThan(right.end()) && right.start().lessThan(left.end());
        }

        /**
         * @return whether two descriptions describe the exact same region of VM memory
         */
        public static boolean equal(MaxMemoryRegion left, MaxMemoryRegion right) {
            if (left == null) {
                return right == null;
            }
            if (right == null) {
                return false;
            }
            return left.start().equals(right.start()) && left.nBytes() == right.nBytes();

        }

        /**
         * Gets a default description of usage information for a fixed size region,
         * is presumed to be fully utilized, and for which {@link MemoryUsage#getInit()} and
         * {@link MemoryUsage#getMax()} are "undefined".
         *
         * @param nBytes the number of bytes in the region
         * @return a default usage descriptor of a fully utilized region of the specified size.
         */
        public static MemoryUsage defaultUsage(long nBytes) {
            return nBytes == 0L ? NULL_MEMORY_USAGE :  new MemoryUsage(-1L, nBytes, nBytes, -1L);
        }

        /**
         * Gets a default description of usage information for the region, in which the region
         * is presumed to be fully utilized, and for which {@link MemoryUsage#getInit()} and
         * {@link MemoryUsage#getMax()} are "undefined".
         *
         * @param memoryRegion a region of VM memory
         * @return a default usage descriptor indicating full utilization fo the region
         */
        public static MemoryUsage defaultUsage(MaxMemoryRegion memoryRegion) {
            return defaultUsage(memoryRegion.nBytes());
        }

        /**
         * Gets a string representation for a memory region composed of its {@linkplain MaxMemoryRegion#regionName() description}
         * and {@linkplain MaxMemoryRegion#start()} - {@linkplain MaxMemoryRegion#end()} address range.
         */
        public static String asString(MaxMemoryRegion memoryRegion) {
            if (memoryRegion == null) {
                return "null";
            }
            final StringBuilder sb = new StringBuilder();
            if (memoryRegion.regionName() != null) {
                sb.append(memoryRegion.regionName()).append(":");
            }
            sb.append("[").append(memoryRegion.start().toHexString()).append(" - ").append(memoryRegion.end().minus(1).toHexString()).append("]");
            return sb.toString();
        }
    }

}
