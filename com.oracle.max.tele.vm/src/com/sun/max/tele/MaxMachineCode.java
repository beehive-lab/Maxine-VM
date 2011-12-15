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
 * Access to machine code in the VM, consisting of either {@linkplain MaxCompilation method compilations} in the
 * {@linkplain MaxCodeCache code cache} or {@linkplain MaxNativeFunction external code} that has been identified by
 * various means.
 */
public interface MaxMachineCode extends MaxEntity<MaxMachineCode> {

    /**
     * Gets the existing machine code, if known, that contains a given address in the VM; the result could be either a
     * VM method compilation or a block of external native code about which little is known.
     * <p>
     * A result is returned <em>only</em> if there is machine code at the location. If the memory location falls within
     * the code cache memory allocated to a method compilation, but does <em>not</em> point to machine code in that
     * allocation, then {@code null} is returned.
     *
     * @param address a memory location in the VM
     * @return the machine code, if any is known, that includes the address
     */
    MaxMachineCodeRoutine< ? extends MaxMachineCodeRoutine> findMachineCode(Address address);

    /**
     * Get the method compilation, if any, whose memory containing machine code includes a given address in the VM.
     * <p>
     * A result is returned <em>only</em> if there is machine code at the location. A memory location might fall within
     * the code cache memory allocated to a method compilation, but if there is <em>not</em> point machine code at the
     * memory location, then {@code null} is returned.
     *
     * @param address memory location in the VM
     * @return a compiled method whose code includes the address, null if none
     */
    MaxCompilation findCompilation(Address address);

    /**
     * @return gets all compilations of a method in the VM, empty if none
     */
    List<MaxCompilation> compilations(TeleClassMethodActor teleClassMethodActor);

    /**
     * Gets the most recent compilation of a method in the VM, null if none.
     *
     * @throws MaxVMBusyException if the VM is unavailable
     */
    MaxCompilation latestCompilation(TeleClassMethodActor teleClassMethodActor) throws MaxVMBusyException;

    /**
     * Create a new MaxNativeFunction to represent a block of external native code in the VM that has not yet been
     * registered, and keep information about it in a registry for subsequent reference.
     *
     * @param codeStart starting address of the machine code in VM memory, not in any VM allocated memory
     * @param nBytes presumed size of the code in bytes
     * @param name an optional name to be assigned to the block of code; a simple address-based name used if null.
     * @return a newly created MaxNativeFunction
     * @throws MaxVMBusyException if the VM is unavailable
     * @throws IllegalArgumentException if the range of memory overlaps in any way with a region already registered, or
     *             is in a VM-allocated code region.
     * @throws MaxInvalidAddressException if he address cannot be read
     */
    MaxNativeFunction registerExternalCode(Address codeStart, long nBytes, String name) throws MaxVMBusyException, MaxInvalidAddressException;

    /**
     * Get the block of known external native code, if any, that contains a given address in the VM.
     *
     * @param address memory location in the VM
     * @return known external native code that includes the address, null if none
     */
    MaxNativeFunction findExternalCode(Address address);

    /**
     * Writes a textual summary describing all instances of {@link MaxMachineCodeRoutine} known to the VM, including
     * compilations created by the VM and external blocks of native code about which less is known.
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
