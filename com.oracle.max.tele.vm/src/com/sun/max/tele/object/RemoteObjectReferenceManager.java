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
package com.sun.max.tele.object;

import java.io.*;

import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * A manager for remote references to objects allocated in a
 * VM region. Each implementation will likely need to take into account
 * specific object management implementations in the VM.
 */
public interface RemoteObjectReferenceManager {

    /**
     * Gets the region whose remote object references are being managed.
     */
    ObjectHoldingRegion objectRegion();

    /**
     * Determines whether there is an object in the VM memory region
     * with specified origin.
     *
     * @throws TeleError if the origin is not in the memory region being managed.
     */
    boolean isObjectOrigin(Address origin) throws TeleError;

    /**
     * Creates a canonical remote reference to an object whose origin
     * in VM memory is at a specified address, null if there is no
     * object with that origin.
     * <p>
     * The origin of the object may change over time, for example if
     * a relocating collector is being used for the region by the VM.
     * <p>
     * Remote references are collected when no longer used, or when the
     * objects to which they point cease to be live in the VM (as well
     * as can be determined).
     *
     * @throws TeleError if the origin is not in the memory region being managed.
     */
    TeleReference makeReference(Address origin) throws TeleError;

    /**
     * Returns the total number of remote object references being held by
     * the manager.
     */
    int activeReferenceCount();

    /**
     * Returns the number of remote object references being held that are no
     * longer inactive use.
     */
    int totalReferenceCount();

    /**
     * Writes current statistics concerning references to objects in VM memory.
     *
     * @param printStream stream to which to write
     * @param indent number of spaces to indent each line
     * @param verbose possibly write extended information when true
     */
    void printSessionStats(PrintStream printStream, int indent, boolean verbose);

}
