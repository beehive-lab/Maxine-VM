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
package com.sun.max.tele.heap;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

/**
 *  Remote inspection support for a particular implementation of the {@link HeapScheme} in the VM.
 *
 *  @see HeapScheme
 */
public interface RemoteHeapScheme extends RemoteScheme {

    /**
     * @return the VM class implementing the heap scheme
     */
    Class heapSchemeClass();

    /**
     * Causes this scheme to perform any additional initializations that cannot be
     * done until most of the Inspector services are up and running.
     */
    void initialize(long epoch);

    /**
     * Causes this scheme support to refresh state related to memory allocations and
     * memory management status.
     *
     * @param epoch the number of times the VM process has run so far.
     */
    void updateMemoryStatus(long epoch);

    /**
     * Gets the current GC phase for the heap.
     */
    HeapPhase phase();

    /**.
     * Reports on the currently known dynamic heap regions.
     * <p>
     * These are assume to have already been reported as top level
     * allocations in the {@linkplain VmAddressSpace address space}.
     *
     * @return the list of dynamic heap regions currently known....
     */
    List<VmHeapRegion> heapRegions();


    // TODO (mlvdv) what to do if address not in heap?
    /**
     * Gets whatever is known about a particular location in VM heap memory with respect
     * to the current state of memory management.
     *
     * @param address a location in VM heap memory
     * @return non-null information about the location with respect to memory management in the VM,
     */
    MaxMemoryManagementInfo getMemoryManagementInfo(Address address);

    /**
     * Return heap-specific implementation of {@link MaxMarkBitsInfo} that the inspector can use to display mark-bit information for heap
     * scheme using a mark-bitmap for trace-based collection.
     * @return an implementation of MaxMarBitsInfo or null
     */
    MaxMarkBitsInfo markBitInfo();
}
