/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * The singleton manager for representations of machine code locations in the VM.
 * <p>
 * This implementation is incomplete.
 */
public class VmMachineCodeAccess extends AbstractVmHolder implements MaxMachineCode, TeleVMCache {

    private static final int TRACE_VALUE = 1;

    private static VmMachineCodeAccess vmCodeLocationManager;

    public static VmMachineCodeAccess make(TeleVM vm) {
        if (vmCodeLocationManager == null) {
            vmCodeLocationManager = new VmMachineCodeAccess(vm);
        }
        return vmCodeLocationManager;
    }

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    private final String entityName = "Machine Code";

    private final String entityDescription;

    /**
     * Contains regions of machine code discovered in the VM process that
     * do not belong to the VM.
     *
     * Information about external machine code regions discovered in the VM process.
     * Presumed invariants:
     * <ul>
     * <li>The external code regions do not intersect any memory regions allocated by the VM.</li>
     * <li>The external code regions do not intersect any other registered external code regions.</li>
     * <li>The number of transactions against the collection is small.</li>
     * <li>The number of registered regions is small, so linear lookup suffices</li>
     * <ul>
     */
    private final List<TeleExternalCodeRoutine> externalCodeRegions = new ArrayList<TeleExternalCodeRoutine>();


    private final Object statsPrinter = new Object() {
        @Override
        public String toString() {
            final StringBuilder msg = new StringBuilder();

            return msg.toString();
        }
    };

    public VmMachineCodeAccess(TeleVM vm) {
        super(vm);
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();

        this.entityDescription = "Remote code pointer creation and management for the " + vm.entityName();
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");


        tracer.end(statsPrinter);
    }

    public void updateCache(long epoch) {
        lastUpdateEpoch = epoch;
    }

    public String entityName() {
        return entityName;
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxMachineCode> memoryRegion() {
        // This represents no memory allocation; code resides in regions managed by the code cache.
        return null;
    }

    public boolean contains(Address address) {
        // There's no real notion of containing an address here.
        return false;
    }

    public TeleObject representation() {
        // No distinguished object in VM runtime represents the heap.
        return null;
    }

    public TeleExternalCodeRoutine registerExternalCode(Address codeStart, long nBytes, String name) throws MaxVMBusyException, IllegalArgumentException, MaxInvalidAddressException {
        if (codeStart == null || codeStart.isZero()) {
            throw new MaxInvalidAddressException(codeStart, "Null or zero address");
        }
        final TeleFixedMemoryRegion newCodeRegion = new TeleFixedMemoryRegion(vm(), "temp", codeStart, nBytes);
        for (MaxMemoryRegion vmAllocation : vm().state().memoryAllocations()) {
            if (newCodeRegion.overlaps(vmAllocation)) {
                throw new IllegalArgumentException("proposed external code region overlaps VM region: " + vmAllocation.regionName());
            }
        }
        for (TeleExternalCodeRoutine registeredCode : externalCodeRegions) {
            if (newCodeRegion.overlaps(registeredCode.memoryRegion())) {
                throw new IllegalArgumentException("proposed external code region overlaps one already registered");
            }
        }
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        try {
            final TeleExternalCodeRoutine teleExternalCode = new TeleExternalCodeRoutine(vm(), codeStart, nBytes, name);
            externalCodeRegions.add(teleExternalCode);
            return teleExternalCode;
        } finally {
            vm().unlock();
        }
    }

    public MaxExternalCodeRoutine findExternalCode(Address address) {
        for (TeleExternalCodeRoutine registeredCode : externalCodeRegions) {
            if (registeredCode.memoryRegion().contains(address)) {
                return registeredCode;
            }
        }
        return null;
    }

    public void printSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        //final NumberFormat formatter = NumberFormat.getInstance();

        printStream.print(indentation + "External machine code regions registered: " + externalCodeRegions.size() + "\n");
    }

    public void writeSummary(PrintStream printStream) {

        Address lastEndAddress = null;
        for (TeleExternalCodeRoutine registeredCode : externalCodeRegions) {
            final String name = registeredCode.entityDescription();
            final MaxEntityMemoryRegion<MaxExternalCodeRoutine> externalCodeMemoryRegion = registeredCode.memoryRegion();
            if (lastEndAddress != null && !lastEndAddress.equals(externalCodeMemoryRegion.start())) {
                printStream.println(lastEndAddress.toHexString() + "--" + externalCodeMemoryRegion.start().minus(1).toHexString() + ": ");
            }
            lastEndAddress = externalCodeMemoryRegion.end();
            printStream.println(externalCodeMemoryRegion.start().toHexString() + "--" + externalCodeMemoryRegion.end().minus(1).toHexString() + ":  " + name);
        }
    }









    // TODO (mlvdv)  Can we now enforce the precondition that we know about the region containing the address?
    // this might be an issue of startup timing, or more likely about native code.
    /**
     * Creates a code location in VM specified as the memory address of a compiled machine code instruction.
     * <p>
     * Thread-safe
     *
     * @param address a non-zero address in VM memory that represents the beginning of a compiled machine code instruction
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a newly created location
     * @throws TeleError if the address is null or zero
     */
    public MachineCodeLocation createMachineCodeLocation(Address address, String description) throws TeleError {
//        if (vm().codeCache() != null) {
//            final VmCodeCacheRegion codeCacheRegion = vm().codeCache().findCompiledCodeRegion(address);
//            if (codeCacheRegion != null) {
//
//            }
//        }

        return null;
    }





}
