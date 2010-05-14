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

    private final OrderedMemoryRegionList<MaxEntityMemoryRegion<? extends MaxCompiledCode>> compiledCodeMemoryRegions =
        new OrderedMemoryRegionList<MaxEntityMemoryRegion<? extends MaxCompiledCode>>();

    /**
     * Adds an entry to the code registry, indexed by code address, that represents a block
     * of native machine code about which little is known.
     *
     * @param teleCompiledNativeCode the compiled code whose memory region is to be added to this registry
     * @throws IllegalArgumentException when the code's memory overlaps one already in this registry.
     */
    public synchronized void add(MaxCompiledNativeCode teleCompiledNativeCode) {
        compiledCodeMemoryRegions.add(teleCompiledNativeCode.memoryRegion());
    }

    /**
     * Adds an entry to the code registry, indexed by code address, that represents a method compilation.
     *
     * @param teleCompiledMethod the method compilation whose memory region is to be added to this registry
     * @throws IllegalArgumentException when the code's memory overlaps one already in this registry.
     */
    public synchronized void add(TeleCompiledMethod teleCompiledMethod) {
        final MaxEntityMemoryRegion<MaxCompiledMethod> memoryRegion = teleCompiledMethod.memoryRegion();
        ProgramError.check(!memoryRegion.start().isZero(), "Code registry zero location");
        compiledCodeMemoryRegions.add(memoryRegion);
    }

    public synchronized TeleCompiledNativeCode getCompiledNativeCode(Address address) {
        final MaxEntityMemoryRegion< ? extends MaxCompiledCode> compiledCodeRegion = compiledCodeMemoryRegions.find(address);
        if (compiledCodeRegion != null) {
            final MaxCompiledCode compiledCode = compiledCodeRegion.owner();
            if (compiledCode instanceof TeleCompiledNativeCode) {
                return (TeleCompiledNativeCode) compiledCode;
            }
        }
        return null;
    }

    public synchronized TeleCompiledMethod getCompiledMethod(Address address) {
        final MaxEntityMemoryRegion< ? extends MaxCompiledCode> compiledCodeRegion = compiledCodeMemoryRegions.find(address);
        if (compiledCodeRegion != null) {
            final MaxCompiledCode compiledCode = compiledCodeRegion.owner();
            if (compiledCode instanceof TeleCompiledMethod) {
                return (TeleCompiledMethod) compiledCode;
            }
        }
        return null;
    }

    public void writeSummary(PrintStream printStream) {
        Address lastEndAddress = null;
        for (MaxEntityMemoryRegion<? extends MaxCompiledCode> compiledMethodMemoryRegion : compiledCodeMemoryRegions) {
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
