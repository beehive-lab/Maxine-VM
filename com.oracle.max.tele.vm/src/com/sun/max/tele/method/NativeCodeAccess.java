/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;


/**
 * The singleton manager for managing information about native function code in libraries external to the VM.
 */
public final class NativeCodeAccess extends AbstractVmHolder implements TeleVMCache, MaxNativeCode, AllocationHolder {

    private static final int TRACE_VALUE = 1;

    private static NativeCodeAccess nativeCodeAccess;

    static NativeCodeAccess make(TeleVM vm) {
        if (nativeCodeAccess == null) {
            nativeCodeAccess = new NativeCodeAccess(vm);
        }
        return nativeCodeAccess;
    }

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    private final String entityName = "Native function code";

    private final String entityDescription;


    /**
     * Information about native function code regions discovered in the VM process.
     * Presumed invariants:
     * <ul>
     * <li>The native function regions do not intersect any memory regions allocated by the VM.</li>
     * <li>The native function code regions do not intersect any other registered native function code regions.</li>
     * <li>The number of transactions against the collection is small.</li>
     * <li>The number of registered regions is small, so linear lookup suffices</li>
     * <ul>
     */
    private final List<MaxEntityMemoryRegion<? extends MaxEntity> > nativeFunctionMemoryRegions = new ArrayList<MaxEntityMemoryRegion<? extends MaxEntity> >();

    private final List<MaxNativeLibrary> libraries = new ArrayList<MaxNativeLibrary>();

    private final Object statsPrinter = new Object() {

        private int oldLoadedLibraryCount = 0;
        @Override
        public String toString() {
            final int newLoadedLibraryCount = libraries.size();
            final StringBuilder msg = new StringBuilder();
            msg.append("#libraries=(").append(newLoadedLibraryCount);
            msg.append(", new=").append(newLoadedLibraryCount - oldLoadedLibraryCount).append(")");
            oldLoadedLibraryCount = newLoadedLibraryCount;
            return msg.toString();
        }
    };

    public NativeCodeAccess(TeleVM vm) {
        super(vm);
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();
        this.entityDescription = "Native functions in dynamically loaded libraries";
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");
        tracer.end(null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * See if any new native libraries have been loaded since the last refresh.
     */
    public void updateCache(long epoch) {
        if (epoch > lastUpdateEpoch) {
            updateTracer.begin();
            try {
                TeleNativeLibraries.update(vm());
                for (TeleNativeLibrary teleNativeLibrary : TeleNativeLibraries.libs()) {
                    if (teleNativeLibrary.functions() != null && !libraries.contains(teleNativeLibrary)) {
                        libraries.add(teleNativeLibrary);
                        nativeFunctionMemoryRegions.add(teleNativeLibrary.memoryRegion());
                        Trace.line(TRACE_VALUE, tracePrefix() + "adding dynamically loaded library: " + teleNativeLibrary.entityName());
                    }
                }
                lastUpdateEpoch = epoch;
            } catch (Exception e) {
                TeleError.unexpected("Native library update failure", e);
            }
            updateTracer.end(statsPrinter);
        }
    }

    public String entityName() {
        return entityName;
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxNativeCode> memoryRegion() {
        // The native function code access has no memory allocation of its own, but
        // rather "owns" the memory regions occupied by loaded native libraries.
        return null;
    }


    public boolean contains(Address address) {
        return findNativeLibrary(address) != null;
    }

    public TeleObject representation() {
        // There is no VM object that represents the native function libraries.
        return null;
    }

    public List<MaxNativeLibrary> nativeLibraries() {
        return libraries;
    }

    public TeleNativeLibrary findNativeLibrary(Address address) {
        for (MaxNativeLibrary  maxNativeLibrary : libraries) {
            if (maxNativeLibrary.memoryRegion().contains(address)) {
                return (TeleNativeLibrary) maxNativeLibrary;
            }
        }
        return null;
    }

    public List<MaxEntityMemoryRegion<? extends MaxEntity> > memoryAllocations() {
        return nativeFunctionMemoryRegions;
    }

    TeleNativeFunction registerNativeFunction(Address codeStart, long nBytes, String name) throws MaxVMBusyException, IllegalArgumentException, MaxInvalidAddressException {
        if (codeStart == null || codeStart.isZero()) {
            throw new MaxInvalidAddressException(codeStart, "Null or zero address");
        }
        final TeleFixedMemoryRegion newCodeRegion = new TeleFixedMemoryRegion(vm(), "temp", codeStart, nBytes);
        for (MaxMemoryRegion vmAllocation : vm().state().memoryAllocations()) {
            if (newCodeRegion.overlaps(vmAllocation)) {
                throw new IllegalArgumentException("proposed native function region overlaps VM region: " + vmAllocation.regionName());
            }
        }
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        try {
            final TeleNativeFunction teleNativeFunction = new TeleNativeFunction(vm(), name, codeStart, nBytes);
            nativeFunctionMemoryRegions.add(teleNativeFunction.memoryRegion());
            return teleNativeFunction;
        } finally {
            vm().unlock();
        }
    }

    TeleNativeFunction findNativeFunction(Address address) {
        //look in registered native libraries
        for (TeleNativeLibrary teleNativeLibrary : TeleNativeLibraries.libs()) {
            if (teleNativeLibrary.contains(address)) {
                return teleNativeLibrary.findNativeFunction(address);
            }
        }
        return null;
    }

    public void printSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        final NumberFormat formatter = NumberFormat.getInstance();
        printStream.print(indentation + "Native function code regions registered: " + formatter.format(nativeFunctionMemoryRegions.size()) + "\n");
    }

    public void writeSummary(PrintStream printStream) {
        Address lastEndAddress = null;
        for (MaxMemoryRegion maxMemoryRegion : nativeFunctionMemoryRegions) {
            final String name = maxMemoryRegion.regionName();
            if (lastEndAddress != null && !lastEndAddress.equals(maxMemoryRegion.start())) {
                printStream.println(lastEndAddress.toHexString() + "--" + maxMemoryRegion.start().minus(1).toHexString() + ": ");
            }
            lastEndAddress = maxMemoryRegion.end();
            printStream.println(maxMemoryRegion.start().toHexString() + "--" + maxMemoryRegion.end().minus(1).toHexString() + ":  " + name);
        }
    }


}
