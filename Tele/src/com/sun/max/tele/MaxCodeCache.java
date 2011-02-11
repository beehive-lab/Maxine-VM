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

/**
 * Access to the cache of machine code in the VM.
 * The code cache consists of a part of the boot image
 * and one or more dynamically allocated regions
 *
 * @author Michael Van De Vanter
 */
public interface MaxCodeCache extends MaxEntity<MaxCodeCache> {

    /**
     * @return description of the special code cache region included in the binary image.
     */
    MaxCompiledCodeRegion bootCodeRegion();

    /**
     * Gets all currently allocated regions in the VM's compiled code cache, including boot.
     *
     * @return descriptions for all compiled code regions in the VM.
     */
    List<MaxCompiledCodeRegion> compiledCodeRegions();

    /**
     * Finds a code cache region by location.
     *
     * @param address a memory location in the VM.
     * @return the code cache region, if any, that includes that location
     */
    MaxCompiledCodeRegion findCompiledCodeRegion(Address address);

    /**
     * Gets the existing machine code, if known, that contains a given address in the VM;
     * the result could be a compiled method or a block of external native code about which little is known.
     *
     * @param address a memory location in the VM
     * @return the code, if any is known, that includes the address
     */
    MaxMachineCode< ? extends MaxMachineCode> findMachineCode(Address address);

    /**
     * Get the method compilation, if any, that contains a given address in the VM.
     *
     * @param address memory location in the VM
     * @return a compiled method whose code includes the address, null if none
     */
    MaxCompiledCode findCompiledCode(Address address);

    /**
     * Get the block of known external native code, if any, that contains a given address in the VM.
     *
     * @param address memory location in the VM
     * @return known external native code that includes the address, null if none
     */
    MaxExternalCode findExternalCode(Address address);

    /**
     * @return gets all compilations of a method in the VM, empty if none
     */
    List<MaxCompiledCode> compilations(TeleClassMethodActor teleClassMethodActor);

    /**
     * Gets the most recent compilation of a method in the VM, null if none.
     * @throws MaxVMBusyException  if the VM is unavailable
     */
    MaxCompiledCode latestCompilation(TeleClassMethodActor teleClassMethodActor) throws MaxVMBusyException;

    /**
     * Create a new MaxExternalCode for a block of external native code in the VM that has not yet been registered.
     *
     * @param codeStart starting address of the machine code in VM memory
     * @param nBytes presumed size of the code in bytes
     * @param name an optional name to be assigned to the block of code; a simple address-based name used if null.
     * @return a newly created TeleExternalCode
     * @throws MaxVMBusyException if the VM is unavailable
     * @throws MaxInvalidAddressException if he address cannot be read
     */
    MaxExternalCode createExternalCode(Address codeStart, long nBytes, String name) throws MaxVMBusyException, MaxInvalidAddressException;

    /**
     * Writes a textual summary describing all instances of {@link MaxMachineCode} known to the VM.
     */
    void writeSummary(PrintStream printStream);

    /**
     * Writes current statistics concerning inspection of VM's code cache.
     *
     * @param printStream stream to which to write
     * @param indent number of spaces to indent each line
     * @param verbose possibly write extended information when true
     */
    void printSessionStats(PrintStream printStream, int indent, boolean verbose);

}
