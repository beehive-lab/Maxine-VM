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
package com.sun.max.tele.method;

import java.io.*;

import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;


/**
 * A manager for pointers to machine code in a VM memory region.
 * Each implementation will likely need to take into account
 * specific code management implementations in the VM.
 */
public interface RemoteCodePointerManager {

    /**
     * Gets the region contain the machine code locations to be managed.
     */
    CodeHoldingRegion codeRegion();

    /**
     * Determines whether there is machine code in the VM memory region
     * at the specified location.  Does <em>not</em> check whether an
     * instructions starts at the location.
     *
     * @throws TeleError if the location is not in the memory region being managed.
     */
    boolean isValidCodePointer(Address address) throws TeleError;

    /**
     * Creates a canonical pointer to a location in VM memory containing machine code,
     * null if there is no machine code at that location.
     * <p>
     * The absolute address of the code pointer may change over time, for example
     * if the code is a managed code cache region.
     * <p>
     * Remote  code pointers are collected when no longer used, or when the code
     * to which they point ceases to be live in the VM (as well as can be determined).
     *
     * @throws TeleError if the location is not in the memory region being managed.
     */
    RemoteCodePointer makeCodePointer(Address address) throws TeleError;

    /**
     * Returns the total number of remote code pointers being held by the manager.
     */
    int totalPointerCount();

    /**
     * Returns the number of remote code pointers being held that are no longer inactive use.
     */
    int activePointerCount();

    /**
     * Prints a summary of information about code pointers being managed.
     */
    void printSessionStats(PrintStream printStream, int indent, boolean verbose);

}
