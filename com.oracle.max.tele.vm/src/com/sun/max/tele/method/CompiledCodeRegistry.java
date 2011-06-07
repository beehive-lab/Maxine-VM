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
 * A cache of information about method compilations in the VM,
 * organized for efficient lookup by memory address.
 * <p>
 * Membership in this cache does not necessarily imply that the compilation itself has been copied
 * into the inspection memory.
 * <p>
 * In the case where the the representation of a compilation exists, but has not yet been allocated
 * memory space in the VM's code cache (distinguished by a starting address equal to zero), the
 * entries are set aside and checked upon each refresh to see if they have since been allocated
 * and can be inserted into the registry.
 */
final class CompiledCodeRegistry extends AbstractTeleVMHolder implements TeleVMCache {

    private static final int TRACE_VALUE = 1;

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    private final OrderedMemoryRegionList<MaxEntityMemoryRegion<MaxCompilation>> compiledCodeMemoryRegions =
        new OrderedMemoryRegionList<MaxEntityMemoryRegion<MaxCompilation>>();

    private final Set<MaxEntityMemoryRegion<MaxCompilation>> unallocatedCompiledCodeMemoryRegions =
        new HashSet<MaxEntityMemoryRegion<MaxCompilation>>();

    private final Object statsPrinter = new Object() {

        private int previousEntryCount = 0;

        @Override
        public String toString() {
            final int entryCount = compiledCodeMemoryRegions.size();
            final int newEntryCount =  entryCount - previousEntryCount;
            final StringBuilder msg = new StringBuilder();
            msg.append("#entries=(").append(entryCount);
            msg.append(",new=").append(newEntryCount);
            msg.append(",unallocated=").append(compiledCodeMemoryRegions.size()).append(")");
            previousEntryCount = entryCount;
            return msg.toString();
        }
    };

    CompiledCodeRegistry(TeleVM teleVM) {
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
            for (MaxEntityMemoryRegion<MaxCompilation> memoryRegion : unallocatedCompiledCodeMemoryRegions) {
                if (!memoryRegion.start().isZero()) {
                    unallocatedCompiledCodeMemoryRegions.remove(memoryRegion);
                    Trace.line(TRACE_VALUE, tracePrefix() + " formerly unallocated code memory region promoted to registry: " + memoryRegion.owner().entityName());
                    unallocatedCompiledCodeMemoryRegions.add(memoryRegion);
                }
            }
            lastUpdateEpoch = epoch;
            updateTracer.end(statsPrinter);
        } else {
            Trace.line(TRACE_VALUE, tracePrefix() + "redundant update epoch=" + epoch + ": " + this);
        }
    }

    /**
     * Adds an entry to the code registry, indexed by code address, that represents a VM compilation.
     *
     * @param teleCompiledCode the compilation whose memory region is to be added to this registry
     * @throws IllegalArgumentException when the code's memory overlaps one already in this registry.
     */
    synchronized void register(TeleCompilation teleCompiledCode) {
        final MaxEntityMemoryRegion<MaxCompilation> memoryRegion = teleCompiledCode.memoryRegion();
        if (memoryRegion.start().isZero()) {
            // The code has not been allocated any memory in the code cache yet; set aside for now.
            unallocatedCompiledCodeMemoryRegions.add(memoryRegion);
            Trace.line(TRACE_VALUE, tracePrefix() + " unallocated code memory region pending for registry: " + teleCompiledCode.entityName());
        } else {
            compiledCodeMemoryRegions.add(memoryRegion);
        }
    }

    synchronized TeleCompilation find(Address address) {
        final MaxEntityMemoryRegion<MaxCompilation> compiledCodeRegion = compiledCodeMemoryRegions.find(address);
        if (compiledCodeRegion != null) {
            final MaxCompilation compiledCode = compiledCodeRegion.owner();
            if (compiledCode != null) {
                return (TeleCompilation) compiledCode;
            }
        }
        return null;
    }

    /**
     * Gets the current count of compiled methods whose location in VM memory is known.
     * @return the number of registered compiled code regions in VM memory
     */
    int size() {
        return compiledCodeMemoryRegions.size();
    }

    void writeSummary(PrintStream printStream) {
        Address lastEndAddress = null;
        for (MaxEntityMemoryRegion<MaxCompilation> compiledCodeMemoryRegion : compiledCodeMemoryRegions) {
            final MaxCompilation maxCompiledCode = compiledCodeMemoryRegion.owner();
            final String name = maxCompiledCode.entityDescription();
            if (lastEndAddress != null && !lastEndAddress.equals(compiledCodeMemoryRegion.start())) {
                printStream.println(lastEndAddress.toHexString() + "--" + compiledCodeMemoryRegion.start().minus(1).toHexString() + ": ");
            }
            lastEndAddress = compiledCodeMemoryRegion.end();
            printStream.println(compiledCodeMemoryRegion.start().toHexString() + "--" + compiledCodeMemoryRegion.end().minus(1).toHexString() + ":  " + name);
        }
    }
}
