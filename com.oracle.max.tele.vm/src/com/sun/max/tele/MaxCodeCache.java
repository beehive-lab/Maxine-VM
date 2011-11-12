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
 * Access to the cache of compiled machine code in the VM.
 * <p>
 * The code cache consists of a single {@linkplain MaxCodeCacheRegion code cache region} in the boot image,
 * together with one or more dynamically allocated {@linkplain MaxCodeCacheRegion code cache regions}.
 * <p>
 * Each {@linkplain MaxCodeCacheRegion Code cache region}, managed by a (heap) instance of {@link CodeLocationFactory},
 * allocates an area of memory for each
 * compilation in the code cache.  Each allocation is described, along with other information
 * related to the compilation, by a (heap) instance of {@link TargetMethod}.
 */
public interface MaxCodeCache extends MaxEntity<MaxCodeCache> {

    /**
     * @return description of the special code cache region included in the binary boot image.
     */
    MaxCodeCacheRegion bootCodeRegion();

    /**
     * Gets descriptions all currently allocated code cache regions in the VM's compiled code cache, including the boot code cache.
     *
     * @return descriptions for all code cache regions in the VM.
     */
    List<MaxCodeCacheRegion> compiledCodeRegions();

    /**
     * Finds a code cache region by location, where the location could be anywhere in the code
     * cache's memory allocation, whether that location is actually allocated to a compilation
     * or is unallocated.  In particular, finding such a region does not guarantee that the
     * location is a valid code pointer.
     *
     * @param address a memory location in the VM.
     * @return the code cache region, if any, that contains the specified location
     */
    MaxCodeCacheRegion findCompiledCodeRegion(Address address);

    /**
     * Gets the existing machine code, if known, that contains a given address in the VM;
     * the result could be either a compiled method or a block of external native code about
     * which little is known.
     * <p>
     * A result is returned <em>only</em> if there is machine code at the location.  If the
     * memory location falls within the code cache memory allocated to a method compilation,
     * but does <em>not</em> point to machine code in that allocation, then {@code null} is
     * returned.
     *
     * @param address a memory location in the VM
     * @return the machine code, if any is known, that includes the address
     */
    MaxMachineCodeRoutine< ? extends MaxMachineCodeRoutine> findMachineCode(Address address);

    /**
     * Get the method compilation, if any, whose code cache allocation includes
     * a given address in the VM, whether or not there is target code at the
     * specific location.
     *
     * @param address memory location in the VM
     * @return a  method compilation whose code cache allocation includes the address, null if none
     */
    MaxCompilation findCompilation(Address address);

    /**
     * Get the method compilation, if any, whose memory containing machine code includes
     * a given address in the VM.
     * <p>
     * A result is returned <em>only</em> if there is machine code at the location.  A
     * memory location might fall within the code cache memory allocated to a method compilation,
     * but if there is <em>not</em> point machine code at the memory location, then {@code null} is
     * returned.
     *
     * @param address memory location in the VM
     * @return a compiled method whose code includes the address, null if none
     */
    MaxCompilation findCompiledCode(Address address);

    /**
     * @return gets all compilations of a method in the VM, empty if none
     */
    List<MaxCompilation> compilations(TeleClassMethodActor teleClassMethodActor);

    /**
     * Gets the most recent compilation of a method in the VM, null if none.
     * @throws MaxVMBusyException  if the VM is unavailable
     */
    MaxCompilation latestCompilation(TeleClassMethodActor teleClassMethodActor) throws MaxVMBusyException;

    /**
     * Writes a textual summary describing all instances of {@link MaxMachineCodeRoutine} known to the VM.
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
