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

import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * A registry of information about external machine code in the VM,
 * organized for efficient lookup by memory address.
 */
final class ExternalCodeRegistry extends AbstractTeleVMHolder implements TeleVMCache {

    private static final int TRACE_VALUE = 1;

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    private final OrderedMemoryRegionList<MaxEntityMemoryRegion<MaxExternalCode>> externalCodeMemoryRegions =
        new OrderedMemoryRegionList<MaxEntityMemoryRegion<MaxExternalCode>>();

    private final Object statsPrinter = new Object() {

        @Override
        public String toString() {
            return "#entries=(" + externalCodeMemoryRegions.size() + ")";
        }
    };

    ExternalCodeRegistry(TeleVM teleVM) {
        super(teleVM);
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");
        tracer.end(statsPrinter);
    }

    /**
     * {@inheritDoc}
     * <p>
     * External code regions are not discovered by any update on the part of the Inspector.
     * They are only added when discovered in other ways.
     * @see #register(TeleExternalCode)
     */
    public void updateCache(long epoch) {
        updateTracer.begin();
        updateTracer.end(statsPrinter);
    }

    /**
     * Adds an entry to the external code registry, indexed by code address, that represents a block
     * of external machine code about which little is known.
     *
     * @param externalCode the machine code whose memory region is to be added to this registry
     * @throws IllegalArgumentException when the code's memory overlaps one already in this registry.
     */
    synchronized void register(TeleExternalCode externalCode) {
        externalCodeMemoryRegions.add(externalCode.memoryRegion());
    }

    /**
     * Gets a previously discovered region of external machine code that includes a specified address.
     *
     * @param address a location in VM memory
     * @return a previously identified region of machine code in the VM that includes the address, null if none
     */
    synchronized TeleExternalCode find(Address address) {
        final MaxEntityMemoryRegion<MaxExternalCode> externalCodeRegion = externalCodeMemoryRegions.find(address);
        if (externalCodeRegion != null) {
            MaxExternalCode owner = externalCodeRegion.owner();
            if (owner != null) {
                return (TeleExternalCode) owner;
            }
        }
        return null;
    }

    /**
     * Gets the current count of external code regions whose location  is known.
     * @return the number of registered external code regions in VM memory
     */
    int size() {
        return externalCodeMemoryRegions.size();
    }

    void writeSummary(PrintStream printStream) {
        Address lastEndAddress = null;
        for (MaxEntityMemoryRegion<MaxExternalCode> externalCodeMemoryRegion : externalCodeMemoryRegions) {
            final MaxExternalCode maxExternalCode = externalCodeMemoryRegion.owner();
            final String name = maxExternalCode.entityDescription();
            if (lastEndAddress != null && !lastEndAddress.equals(externalCodeMemoryRegion.start())) {
                printStream.println(lastEndAddress.toHexString() + "--" + externalCodeMemoryRegion.start().minus(1).toHexString() + ": ");
            }
            lastEndAddress = externalCodeMemoryRegion.end();
            printStream.println(externalCodeMemoryRegion.start().toHexString() + "--" + externalCodeMemoryRegion.end().minus(1).toHexString() + ":  " + name);
        }
    }
}
