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

import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Access to objects in the VM, not all of which may be in the heap.
 * <p>
 * Objects reside in the heap (which consists of multiple regions under possibly different management)
 * as well as in the code cache.  At some point this may all be unified, but for now they are considered separate.
 *
 * @see MaxHeap
 * @see MaxCodeCache
 */
public interface MaxObjects extends MaxEntity<MaxObjects> {

    // TODO (mlvdv) This interface as well as others related to memory management is evolving.

    /**
     * Examines the contents of VM memory and determines what kind of object (live, quasi, etc.),
     * if any, is represented at that location, using only low-level mechanisms and creating
     * no {@link Reference}s.
     *
     * @param origin an absolute memory location in the VM.
     * @return an enum identifying the kind of object, if any, that is represented at the location in VM memory.
     */
    ObjectStatus objectStatusAt(Address origin);

    /**
     * Locator for TeleObjects, which
     * provide access to object contents and specialized methods that encapsulate
     * knowledge of the heap's design.
     * Special subclasses are created for Maxine implementation objects of special interest,
     *  and for other objects for which special treatment is needed.
     *
     * @param reference a heap object in the VM;
     * @return a canonical local surrogate for the object, {@code null} for the distinguished zero {@link Reference}.
     * @throws MaxVMBusyException if data cannot be read from the VM at this time
     */
    MaxObject findObject(Reference reference) throws MaxVMBusyException;

    /**
     * @param id an id assigned to each heap object in the VM as needed, unique for the duration of a VM execution.
     * @return an accessor for the specified heap object.
     */
    MaxObject findObjectByOID(long id);

    /**
     * Finds a live object whose origin is at the specified address, if one exists.
     *
     * @param origin memory location in the VM
     * @return surrogate for a VM object, {@code null} if none found or if the VM is busy
     * @throws MaxVMBusyException if data cannot be read from the VM at this time
     */
    MaxObject findObjectAt(Address origin);

    /**
     * Finds a quasi object whose origin is at the specified address, if one exists.
     *
     * @param origin memory location in the VM
     * @return surrogate for a VM quasi object, {@code null} if none found or if the VM is busy
     * @throws MaxVMBusyException if data cannot be read from the VM at this time
     */
    MaxObject findQuasiObjectAt(Address origin);

    /**
     * Finds a live or quasi object whose origin is at the specified address, if one exists.
     *
     * @param origin memory location in the VM
     * @return surrogate for a VM live or quasi object, {@code null} if none found or if the VM is busy
     * @throws MaxVMBusyException if data cannot be read from the VM at this time
     */
    MaxObject findAnyObjectAt(Address origin);

    /**
     * Finds a live object whose location is encoded as a forwarding address.
     *
     * @param possibly encoded origin of a newly forwarded object in the VM
     * @return surrogate for a VM object, {@code null} if none found or if the VM is busy
     * @throws MaxVMBusyException if data cannot be read from the VM at this time
     */
    MaxObject findForwardedObjectAt(Address forwardingAddress);

    /**
     * Scans VM memory backwards (smaller address) for a live object whose cell begins at the specified address.
     *
     * @param cellAddress search starts with word preceding this address
     * @param maxSearchExtent maximum number of bytes to search, unbounded if 0.
     * @return surrogate for a VM object, {@code null} if none found
     */
    MaxObject findObjectPreceding(Address cellAddress, long maxSearchExtent);

    /**
     * Scans VM memory forward (larger address) for a live object whose cell begins at the specified address.
     *
     * @param cellAddress search starts with word following this address
     * @param maxSearchExtent maximum number of bytes to search, unbounded if 0.
     * @return surrogate for a VM object, {@code null} if none found
     */
    MaxObject findObjectFollowing(Address cellAddress, long maxSearchExtent);

    /**
     * @return the {@link ClassRegistry} object in the boot heap of the VM.
     */
    TeleObject vmClassRegistry() throws MaxVMBusyException;

    /**
     * Writes current statistics concerning inspection of the VM's heap.
     *
     * @param printStream stream to which to write
     * @param indent number of spaces to indent each line
     * @param verbose possibly write extended information when true
     */
    void printSessionStats(PrintStream printStream, int indent, boolean verbose);

}
