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

import com.sun.max.unsafe.*;


/**
 * Access to dynamically loaded libraries of native code, external to the VM.
 */
public interface MaxExternalCode extends MaxEntity<MaxExternalCode> {

    /**
     * Gets descriptions all currently allocated code cache regions in the VM's compiled code cache, including the boot
     * code cache.
     *
     * @return descriptions for all code cache regions in the VM.
     */
    List<MaxNativeLibrary> nativeLibraries();

    /**
     * Finds a code cache region by location, where the location could be anywhere in the code
     * cache's memory, even if unallocated or not pointing at machine code.
     *
     * @param address a memory location in the VM.
     * @return the code cache region, if any, that contains the specified location
     */
    MaxNativeLibrary findNativeLibrary(Address address);

    /**
     * Writes current statistics concerning inspection of VM's code cache.
     *
     * @param printStream stream to which to write
     * @param indent number of spaces to indent each line
     * @param verbose possibly write extended information when true
     */
    void printSessionStats(PrintStream printStream, int indent, boolean verbose);

}
