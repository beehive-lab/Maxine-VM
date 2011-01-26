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

import java.io.*;
import java.util.*;

import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;

/**
 * Access to the heap in the VM.
 *
 * @author Michael Van De Vanter
 */
public interface MaxHeap extends MaxEntity<MaxHeap> {

    /**
     * @return description of the special heap region included in the binary image.
     */
    MaxHeapRegion bootHeapRegion();

    /**
     * @return description of the special non-collected heap region.
     */
    MaxHeapRegion immortalHeapRegion();

    /**
     * Gets all currently allocated regions in the VM's heap, including boot and immortal.
     *
     * @return descriptions for all allocated regions in the VM.
     */
    List<MaxHeapRegion> heapRegions();

    /**
     * Finds a heap region by location.
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
     * A memory region used for holding references a surrogates for those held
     * during remote inspection.
     * <br>
     * This region is not part of the heap.
     *
     * @return description for the special memory region allocated for holding
     * remote copies of addresses being held by references.
     */
    MaxMemoryRegion rootsMemoryRegion();

    /**
     * Locator for TeleObjects, which
     * provide access to object contents and specialized methods that encapsulate
     * knowledge of the heap's design.
     * Special subclasses are created for Maxine implementation objects of special interest,
     *  and for other objects for which special treatment is needed.
     *
     * @param reference a heap object in the VM;
     * @return a canonical local surrogate for the object, null for the distinguished zero {@link Reference}.
     * @throws MaxVMBusyException if data cannot be read from the VM at this time
     */
    TeleObject findTeleObject(Reference reference) throws MaxVMBusyException;

    /**
     * @param id an id assigned to each heap object in the VM as needed, unique for the duration of a VM execution.
     * @return an accessor for the specified heap object.
     */
    TeleObject findObjectByOID(long id);

    /**
     * Finds an object whose origin is at the specified address.
     *
     * @param origin memory location in the VM
     * @return surrogate for a VM object, null if none found
     */
    TeleObject findObjectAt(Address origin);

    /**
     * Scans VM memory backwards (smaller address) for an object whose cell begins at the specified address.
     *
     * @param cellAddress search starts with word preceding this address
     * @param maxSearchExtent maximum number of bytes to search, unbounded if 0.
     * @return surrogate for a VM object, null if none found
     */
    TeleObject findObjectPreceding(Address cellAddress, long maxSearchExtent);

    /**
     * Scans VM memory forward (larger address) for an object whose cell begins at the specified address.
     *
     * @param cellAddress search starts with word following this address
     * @param maxSearchExtent maximum number of bytes to search, unbounded if 0.
     * @return surrogate for a VM object, null if none found
     */
    TeleObject findObjectFollowing(Address cellAddress, long maxSearchExtent);

    /**
     * Writes current statistics concerning inspection of the VM's heap.
     *
     * @param printStream stream to which to write
     * @param indent number of spaces to indent each line
     * @param verbose possibly write extended information when true
     */
    void printStats(PrintStream printStream, int indent, boolean verbose);

}
