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
     * @return size of the region.
     */
    Size size();

    /**
     * @return address just past the last location in the region.
     */
    Address end();

    /**
     * @return a description of the memory usage for this region, null if not available.
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
            return left.start().equals(right.start()) && left.size().equals(right.size());

        }

        /**
         * Gets a default description of usage information for the region, in which the region
         * is presumed to be fully utilized, and for which {@link MemoryUsage#getInit()} and
         * {@link MemoryUsage#getMax()} are "undefined".
         */
        public static MemoryUsage defaultUsage(MaxMemoryRegion memoryRegion) {
            return new MemoryUsage(-1, memoryRegion.size().toLong(), memoryRegion.size().toLong(), -1);
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
