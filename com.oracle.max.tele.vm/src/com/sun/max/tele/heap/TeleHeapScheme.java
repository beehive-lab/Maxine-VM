/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Implementation details about a specific implementation of {@link HeapScheme} in the VM.
 */
public interface TeleHeapScheme extends TeleScheme {

    List<MaxCodeLocation> EMPTY_METHOD_LIST = Collections.emptyList();

    /**
     * Gets whatever is known about a particular location in VM memory with respect
     * to the current state of memory management.
     *
     * @param address a location in VM memory
     * @return non-null information about the location with respect to memory management in the VM,
     *
     */
    MaxMemoryManagementInfo getMemoryManagementInfo(Address address);

    /**
     * Location, relative to object origin, of the word used by GC to store a forwarding pointer,
     * -1 if unknown or if GC doesn't forward.
     *
     * @return offset from object origin to word that might contain a forwarding pointer,
     * -1 if no forwarding done or if unknown.
     */
    int gcForwardingPointerOffset();

    /**
     * Determines whether an object formerly at a particular location
     * has been relocated.
     *
     * @param origin an object location in the VM
     * @return whether the object at the location has been relocated.
     */
    boolean isObjectForwarded(Pointer origin);

    /**
     * Determines if a pointer is a GC forwarding pointer.
     *
     * @param pointer a pointer to VM memory
     * @return true iff the pointer is a GC forwarding pointer
     */
    boolean isForwardingPointer(Pointer pointer);

    /**
     * Get where a pointer actually points, even if it is a forwarding pointer.
     *
     * @param forwardingPointer a pointer that might be a forwarding pointer
     * @return where the pointers points, whether or not it is a forwarding pointer.
     */
    Pointer getTrueLocationFromPointer(Pointer pointer);

    /**
     * Returns the true location of an object that might have been forwarded, either
     * the current location (if forwarded) or the same location (if not forwarded).
     *
     * @param objectPointer the origin of an object in VM memory
     * @return the current, possibly forwarded, origin of the object
     */
    Pointer getForwardedOrigin(Pointer origin);

    /**
     * Return heap-specific implementation of {@link MaxMarkBitsInfo} that the inspector can use to display mark-bit information for heap
     * scheme using a mark-bitmap for trace-based collection.
     * @return an implementation of MaxMarBitsInfo or null
     */
    MaxMarkBitsInfo markBitInfo();
}
