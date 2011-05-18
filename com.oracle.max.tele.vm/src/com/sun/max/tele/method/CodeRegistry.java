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

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * A cache of information about machine code (both compilations and external code) in the VM,
 * organized for efficient lookup by memory address.
 * <br>
 * Membership in this cache does not necessarily imply that the compilation itself has been copied
 * into the inspection memory.
 * <br>
 * In the case where the the representation of a compilation exists, but has not yet been allocated
 * memory space in the VM's code cache (distinguished by a starting address equal to zero), the
 * entries are set aside and checked upon each refresh to see if they have since been allocated
 * and can be inserted into the registry.
 *
 * @author Michael Van De Vanter
 */
final class CodeRegistry extends AbstractTeleVMHolder implements TeleVMCache {

    private static final int TRACE_VALUE = 1;

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    private final OrderedMemoryRegionList<MaxEntityMemoryRegion<? extends MaxMachineCode>> machineCodeMemoryRegions =
        new OrderedMemoryRegionList<MaxEntityMemoryRegion<? extends MaxMachineCode>>();

    private final Set<MaxEntityMemoryRegion<? extends MaxMachineCode>> unallocatedMachineCodeMemoryRegions =
        new HashSet<MaxEntityMemoryRegion<? extends MaxMachineCode>>();

    private final Object statsPrinter = new Object() {

        private int previousEntryCount = 0;

        @Override
        public String toString() {
            final int entryCount = machineCodeMemoryRegions.size();
            final int newEntryCount =  entryCount - previousEntryCount;
            final StringBuilder msg = new StringBuilder();
            msg.append("#entries=(").append(entryCount);
            msg.append(",new=").append(newEntryCount);
            msg.append(",unallocated=").append(unallocatedMachineCodeMemoryRegions.size()).append(")");
            previousEntryCount = entryCount;
            return msg.toString();
        }
    };

    public CodeRegistry(TeleVM teleVM) {
        super(teleVM);
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();

        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");

        tracer.end(statsPrinter);
    }

    /**
     * Looks for any previously unallocated code regions that have become allocated and updates
     * list of allocated code regions.
     */
    public void updateCache(long epoch) {
        if (epoch > lastUpdateEpoch) {
            updateTracer.begin();
            assert vm().lockHeldByCurrentThread();
            for (MaxEntityMemoryRegion< ? extends MaxMachineCode> memoryRegion : unallocatedMachineCodeMemoryRegions) {
                if (!memoryRegion.start().isZero()) {
                    unallocatedMachineCodeMemoryRegions.remove(memoryRegion);
                    Trace.line(TRACE_VALUE, tracePrefix() + " formerly unallocated code memory region promoted to registry: " + memoryRegion.owner().entityName());
                    machineCodeMemoryRegions.add(memoryRegion);
                }
            }
            lastUpdateEpoch = epoch;
            updateTracer.end(statsPrinter);
        } else {
            Trace.line(TRACE_VALUE, tracePrefix() + "redundant update epoch=" + epoch + ": " + this);
        }
    }


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
     * Adds an entry to the code registry, indexed by code address, that represents a VM compilation.
     *
     * @param teleCompiledCode the compilation whose memory region is to be added to this registry
     * @throws IllegalArgumentException when the code's memory overlaps one already in this registry.
     */
    public synchronized void add(TeleCompiledCode teleCompiledCode) {
        final MaxEntityMemoryRegion<MaxCompiledCode> memoryRegion = teleCompiledCode.memoryRegion();
        if (memoryRegion.start().isZero()) {
            // The code has not been allocated any memory in the code cache yet; set aside for now.
            unallocatedMachineCodeMemoryRegions.add(memoryRegion);
            Trace.line(TRACE_VALUE, tracePrefix() + " unallocated code memory region pending for registry: " + teleCompiledCode.entityName());
        } else {
            machineCodeMemoryRegions.add(memoryRegion);
        }
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

    /**
     * Gets the current count of compiled methods whose location in VM memory is known.
     * @return the number of registered compiled code regions in VM memory
     */
    public int size() {
        return machineCodeMemoryRegions.size();
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
