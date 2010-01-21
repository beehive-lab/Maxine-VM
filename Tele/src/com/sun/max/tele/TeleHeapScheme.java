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

import com.sun.max.unsafe.*;

/**
 * Implementation details about a specific implementation of {@link HeapScheme} in the VM.
 *
 * @author Michael Van De Vanter
 */
public interface TeleHeapScheme extends TeleScheme {

    /**
     * Location, relative to object origin, of the word used by GC to store a forwarding pointer.
     *
     * @return offset from object origin to word that might contain a forwarding pointer.
     */
    Offset gcForwardingPointerOffset();

    /**
     * Checks whether a location is in a live area of the VM's heap.
     *
     * @param address a memory location in the VM
     * @return whether the location is defined to be "live" by the implementation.
     */
    boolean isInLiveMemory(Address address);

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
    Pointer getForwardedObject(Pointer origin);

}
