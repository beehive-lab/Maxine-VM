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
 * A cache of information about machine code (by methods and native routines) in the VM,
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

    private final OrderedMemoryRegionList<MaxEntityMemoryRegion<? extends MaxMachineCode>> machineCodeMemoryRegions =
        new OrderedMemoryRegionList<MaxEntityMemoryRegion<? extends MaxMachineCode>>();

    /**
     * Adds an entry to the code registry, indexed by code address, that represents a block
     * of external machine code about which little is known.
     *
     * @param externalCode the machine code whose memory region is to be added to this registry
     * @throws IllegalArgumentException when the code's memory overlaps one already in this registry.
     */
    public synchronized void add(TeleExternalCode externalCode) {
        machineCodeMemoryRegions.add(externalCode.memoryRegion());
    }

    /**
     * Adds an entry to the code registry, indexed by code address, that represents a method compilation.
     *
     * @param teleCompiledCode the compilation whose memory region is to be added to this registry
     * @throws IllegalArgumentException when the code's memory overlaps one already in this registry.
     */
    public synchronized void add(TeleCompiledCode teleCompiledCode) {
        final MaxEntityMemoryRegion<MaxCompiledCode> memoryRegion = teleCompiledCode.memoryRegion();
        ProgramError.check(!memoryRegion.start().isZero(), "Code registry zero location");
        machineCodeMemoryRegions.add(memoryRegion);
    }

    public synchronized TeleExternalCode getExternalCode(Address address) {
        final MaxEntityMemoryRegion< ? extends MaxMachineCode> machineCodeRegion = machineCodeMemoryRegions.find(address);
        if (machineCodeRegion != null) {
            final MaxMachineCode machineCode = machineCodeRegion.owner();
            if (machineCode instanceof TeleExternalCode) {
                return (TeleExternalCode) machineCode;
            }
        }
        return null;
    }

    public synchronized TeleCompiledCode getCompiledCode(Address address) {
        final MaxEntityMemoryRegion< ? extends MaxMachineCode> machineCodeRegion = machineCodeMemoryRegions.find(address);
        if (machineCodeRegion != null) {
            final MaxMachineCode machineCode = machineCodeRegion.owner();
            if (machineCode instanceof TeleCompiledCode) {
                return (TeleCompiledCode) machineCode;
            }
        }
        return null;
    }

    public void writeSummary(PrintStream printStream) {
        Address lastEndAddress = null;
        for (MaxEntityMemoryRegion<? extends MaxMachineCode> compiledMethodMemoryRegion : machineCodeMemoryRegions) {
            final MaxMachineCode maxMachineCode = compiledMethodMemoryRegion.owner();
            final String name = maxMachineCode.entityDescription();
            if (lastEndAddress != null && !lastEndAddress.equals(compiledMethodMemoryRegion.start())) {
                printStream.println(lastEndAddress.toHexString() + "--" + compiledMethodMemoryRegion.start().minus(1).toHexString() + ": ");
            }
            lastEndAddress = compiledMethodMemoryRegion.end();
            printStream.println(compiledMethodMemoryRegion.start().toHexString() + "--" + compiledMethodMemoryRegion.end().minus(1).toHexString() + ":  " + name);
        }
    }
}
