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
package com.sun.max.tele.method;

import java.io.*;
import java.lang.ref.*;
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;


/**
 * The singleton manager for managing information about machine code external to the VM.
 */
public final class ExternalMachineCodeAccess extends AbstractVmHolder {

    private static final int TRACE_VALUE = 1;

    private static ExternalMachineCodeAccess externalMachineCodeAccess;

    static ExternalMachineCodeAccess make(TeleVM vm) {
        if (externalMachineCodeAccess == null) {
            externalMachineCodeAccess = new ExternalMachineCodeAccess(vm);
        }
        return externalMachineCodeAccess;
    }

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

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

    private final RemoteCodePointerManager codePointerManager;

    private final Object statsPrinter = new Object() {
        @Override
        public String toString() {
            final StringBuilder msg = new StringBuilder();
            // TODO (mlvdv) add some stats?
            return msg.toString();
        }
    };

    ExternalMachineCodeAccess(TeleVM vm) {
        super(vm);
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();
        this.codePointerManager = new FakeRemoteCodePointerManager(vm);
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");
        tracer.end(statsPrinter);
    }

    TeleExternalCodeRoutine registerExternalCode(Address codeStart, long nBytes, String name) throws MaxVMBusyException, IllegalArgumentException, MaxInvalidAddressException {
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


    public RemoteCodePointerManager codePointerManager() {
        return codePointerManager;
    }

    MaxExternalCodeRoutine findExternalCode(Address address) {
        for (TeleExternalCodeRoutine registeredCode : externalCodeRegions) {
            if (registeredCode.memoryRegion().contains(address)) {
                return registeredCode;
            }
        }
        return null;
    }

    public void printSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        final NumberFormat formatter = NumberFormat.getInstance();
        printStream.print(indentation + "External machine code regions registered: " + formatter.format(externalCodeRegions.size()) + "\n");
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

    /**
     * A manager for pointers to machine code not in any known region, presumed to be constant.
     */
    private class FakeRemoteCodePointerManager extends AbstractRemoteCodePointerManager {

        /**
         * Map:  address in VM --> a {@link RemoteCodePointer} that refers to the machine code at that location.
         */
        private Map<Long, WeakReference<RemoteCodePointer>> addressToCodePointer = new HashMap<Long, WeakReference<RemoteCodePointer>>();

        public FakeRemoteCodePointerManager(TeleVM vm) {
            super(vm);
        }

        public CodeHoldingRegion codeRegion() {
            return null;
        }

        /**
         * {@inheritDoc}
         * <p>
         * Since we don't know anything about this code, all we can ensure is that it is really
         * external to anything else we know about.
         */
        public boolean isValidCodePointer(Address address) throws TeleError {
            return vm().findMemoryRegion(address) == null;
        }

        public RemoteCodePointer makeCodePointer(Address address) throws TeleError {
            TeleError.check(isValidCodePointer(address), "Location is in a VM allocation");
            RemoteCodePointer codePointer = null;
            final WeakReference<RemoteCodePointer> existingRef = addressToCodePointer.get(address.toLong());
            if (existingRef != null) {
                codePointer = existingRef.get();
            }
            if (codePointer == null && isValidCodePointer(address)) {
                codePointer = new ConstantRemoteCodePointer(address);
                addressToCodePointer.put(address.toLong(), new WeakReference<RemoteCodePointer>(codePointer));
            }
            return codePointer;
        }

        public int activePointerCount() {
            int count = 0;
            for (WeakReference<RemoteCodePointer> weakRef : addressToCodePointer.values()) {
                if (weakRef.get() != null) {
                    count++;
                }
            }
            return count;
        }

        public int totalPointerCount() {
            return addressToCodePointer.size();
        }
    }

}
