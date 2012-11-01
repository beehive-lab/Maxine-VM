/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;

import com.sun.max.unsafe.*;

/**
 * Access to the VM's heap.
 * <p>
 * The current working assumption is that the heap consists of multiple regions, each of which may be managed
 * differently in terms of memory management and in the (closely related) management of object references.
 * <p>
 * For now, not all objects are considered to be in the heap; information stored as objects is also present
 * in the code cache.  This could all be unified at some point in the future.
 *
 * @see MaxObjects
 * @see MaxCodeCache
 */
public interface MaxHeap extends MaxEntity<MaxHeap> {

    // TODO (mlvdv) This interface as well as others related to memory management is evolving.

    /**
     * @return description of the special object heap region included in the binary image.
     */
    MaxHeapRegion bootHeapRegion();

    /**
     * @return description of the special non-collected object heap region.
     */
    MaxHeapRegion immortalHeapRegion();

    /**
     * Gets all currently allocated regions in the VM's heap, including boot and immortal.
     *
     * @return descriptions for all allocated regions in the VM.
     */
    List<MaxHeapRegion> heapRegions();

    /**
     * Finds a heap region by memory location.
     *
     * @param address a memory location in the VM.
     * @return the allocated heap region, if any, that includes that location
     */
    MaxHeapRegion findHeapRegion(Address address);

    /**
     * Determines whether an address is in one of the dynamically allocated heap regions,
     * excluding boot and immortal.
     *
     * @param address a memory address in the VM.
     * @return is the address within a dynamically allocated heap {@linkplain MaxMemoryRegion memory region}?
     */
    boolean containsInDynamicHeap(Address address);

    /**
     * Indicates whether heap management provides detailed heap region information.
     *
     * @return true if detailed heap region information can be provided by heap management.
     */
    boolean providesHeapRegionInfo();

    /**
     * Return heap-specific memory management information that the heap maintains about the region of memory containing the specified location,
     * or null if the specified address is not under the control of the HeapScheme.
     *
     * @param address a location in VM process memory
     * @return whatever information the heap scheme knows about the status of the location
     * with respect to memory management, non-null.
     */
    MaxMemoryManagementInfo getMemoryManagementInfo(Address address);

    /**
     * @return whether the current heap implementation relocates objects and leaves forwarders behind.
     */
    boolean hasForwarders();

    /**
     * @return whether the current heap implementation uses a {@link MaxBitMarkmap}.
     */
    boolean hasMarkBitmap();

    /**
     * Return heap-specific implementation of {@link MaxMarkBitmap} that the inspector can use to display mark-bit information for heap
     * scheme using a mark-bitmap for trace-based collection, if {@link #hasMarkBitmap()} {@code == true} and if the bitmap has been
     * created.  {@code null} otherwise
     *
     * @return the heap's {@link MaxMarkBitmap}, if one is available, {@code null} otherwise.
     */
    MaxMarkBitmap markBitmap();

    /**
     * @return whether the current heap implementation uses a {@link MaxCardTable}.
     */
    boolean hasCardTable();

    /**
     * Return heap-specific implementation of {@link MaxCardTable} that the inspector can use to display card marking
     * information for the heap scheme, if {@link #hasCardTable()} {@code == true} and if the bitmap has been created.
     * {@code null} otherwise
     *
     * @return the heap's {@link MaxCardTable}, if one is available, {@code null} otherwise.
     */
    MaxCardTable cardTable();

    /**
     * Writes current statistics concerning inspection of the VM's heap.
     *
     * @param printStream stream to which to write
     * @param indent number of spaces to indent each line
     * @param verbose possibly write extended information when true
     */
    void printSessionStats(PrintStream printStream, int indent, boolean verbose);

    /**
     * Determines whether a VM memory address, presumed to be in the boot heap, is marked in boot heap's built-in reference map as a mutable reference field.
     */
    boolean isBootHeapRefMapMarked(Address address);

}
