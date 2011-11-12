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

import com.sun.max.unsafe.*;


public interface MaxMachineCode extends MaxEntity<MaxMachineCode> {

    /**
     * Create a new MaxExternalCode to represent a block of external native code in the VM that has not yet been registered,
     * and keep information about it in a registry for subsequent reference.
     *
     * @param codeStart starting address of the machine code in VM memory, not in any VM allocated memory
     * @param nBytes presumed size of the code in bytes
     * @param name an optional name to be assigned to the block of code; a simple address-based name used if null.
     * @return a newly created TeleExternalCode
     * @throws MaxVMBusyException if the VM is unavailable
     * @throws IllegalArgumentException if the range of memory overlaps in any way with a region already registered, or is in
     * a VM-allocated code region.
     * @throws MaxInvalidAddressException if he address cannot be read
     */
    MaxExternalCodeRoutine registerExternalCode(Address codeStart, long nBytes, String name) throws MaxVMBusyException, MaxInvalidAddressException;

    /**
     * Get the block of known external native code, if any, that contains a given address in the VM.
     *
     * @param address memory location in the VM
     * @return known external native code that includes the address, null if none
     */
    MaxExternalCodeRoutine findExternalCode(Address address);




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
