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
package com.sun.max.tele.method;

import java.io.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.unsafe.*;

/**
 * A cache of information about compiled code (by methods and native routines) in the VM,
 * organized for efficient lookup by memory address.
 *
 * @author Michael Van De Vanter
 */
final class CodeRegistry extends AbstractTeleVMHolder {

    private static final int TRACE_VALUE = 1;

    public CodeRegistry(TeleVM teleVM) {
        super(teleVM);
        Trace.begin(TRACE_VALUE, tracePrefix() + " initializing");
        final long startTimeMillis = System.currentTimeMillis();
        Trace.end(TRACE_VALUE, tracePrefix() + " initializing", startTimeMillis);
    }

    private final SortedMemoryRegionList<MaxEntityMemoryRegion<MaxCompiledCode>> compiledCodeMemoryRegions =
        new SortedMemoryRegionList<MaxEntityMemoryRegion<MaxCompiledCode>>();

    /**
     * Adds an entry to the code registry, indexed by code address.
     *
     * @param maxCompiledCode the compiled code whose {@linkplain MaxCompiledCode#memoryRegion() code
     *            region} is to be added to this registry
     * @throws IllegalArgumentException when the code's memory overlaps one already in this registry.
     */
    public synchronized void add(MaxCompiledCode maxCompiledCode) {
        compiledCodeMemoryRegions.add(maxCompiledCode.memoryRegion());
    }

    /**
     * Gets the {@link MaxCompiledCode} in this registry that contains a given address in the VM.
     *
     * @param <CompiledCode_Type> the type of the requested MaxCompiledCode
     * @param compiledCodeType the {@link Class} instance representing {@code TeleTargetRoutine_Type}
     * @param address the look up address
     * @return the tele target routine of type {@code TeleTargetRoutine_Type} in this registry that contains {@code
     *         address} or null if no such tele target routine of the requested type exists
     */
    public synchronized <CompiledCode_Type extends MaxCompiledCode> CompiledCode_Type get(Class<CompiledCode_Type> compiledCodeType, Address address) {
        final MaxEntityMemoryRegion<MaxCompiledCode> compiledCodeMemoryRegion = compiledCodeMemoryRegions.find(address);
        if (compiledCodeMemoryRegion != null) {
            final MaxCompiledCode maxCompiledCode = compiledCodeMemoryRegion.owner();
            if (compiledCodeType.isInstance(maxCompiledCode)) {
                return compiledCodeType.cast(maxCompiledCode);
            }
        }
        return null;
    }

    public void writeSummary(PrintStream printStream) {
        Address lastEndAddress = null;
        for (MaxEntityMemoryRegion<MaxCompiledCode> compiledMethodMemoryRegion : compiledCodeMemoryRegions) {
            final MaxCompiledCode maxCompiledCode = compiledMethodMemoryRegion.owner();
            final String name = maxCompiledCode.entityDescription();
            if (lastEndAddress != null && !lastEndAddress.equals(compiledMethodMemoryRegion.start())) {
                printStream.println(lastEndAddress.toHexString() + "--" + compiledMethodMemoryRegion.start().minus(1).toHexString() + ": ");
            }
            lastEndAddress = compiledMethodMemoryRegion.end();
            printStream.println(compiledMethodMemoryRegion.start().toHexString() + "--" + compiledMethodMemoryRegion.end().minus(1).toHexString() + ":  " + name);
        }
    }
}
