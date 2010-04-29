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

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * Access to the cache of compiled code in the VM.
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
    IndexedSequence<MaxCompiledCodeRegion> compiledCodeRegions();

    /**
     * Finds a code cache region by location.
     *
     * @param address a memory location in the VM.
     * @return the code cache region, if any, that includes that location
     */
    MaxCompiledCodeRegion findCompiledCodeRegion(Address address);

    /**
     * Get the TeleTargetMethod, newly created if needed, that contains a given address in the VM.
     *
     * @param address address in target code memory in the VM
     * @return a possibly newly created target method whose code contains the address.
     */
    TeleTargetMethod makeTeleTargetMethod(Address address);

    /**
     * Create a new TeleCompiledNativeCode for a block of native code in the VM that has not yet been registered.
     *
     * @param codeStart starting address of the code in VM memory
     * @param codeSize presumed size of the code
     * @param name an optional name to be assigned to the block of code; a simple address-based name used if null.
     * @return a newly created TeleCompiledNativeCode
     */
    TeleCompiledNativeCode createTeleNativeTargetRoutine(Address codeStart, Size codeSize, String name);

    /**
     * Gets the existing MaxCompiledCode, if registered, that contains a given address in the VM, possibly filtering by subtype.
     *
     * @param <TeleTargetRoutine_Type> the type of the requested MaxCompiledCode
     * @param teleTargetRoutineType the {@link Class} instance representing {@code TeleTargetRoutine_Type}
     * @param address the look up address
     * @return the tele target routine of type {@code TeleTargetRoutine_Type} in this registry that contains {@code
     *         address} or null if no such tele target routine of the requested type exists
     */
    <TeleTargetRoutine_Type extends MaxCompiledCode> TeleTargetRoutine_Type findTeleTargetRoutine(Class<TeleTargetRoutine_Type> teleTargetRoutineType, Address address);

    /**
     * Writes a textual summary describing all instances of {@link MaxCompiledCode} known to the VM.
     */
    void writeSummary(PrintStream printStream);

}
