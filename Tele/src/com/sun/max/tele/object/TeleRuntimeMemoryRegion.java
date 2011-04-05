/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.object;

import java.lang.management.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for a {@link MemoryRegion} object in the VM, which represents a region of VM memory.
 * <br>
 * Usage defaults to 100%.
 *
 * @author Michael Van De Vanter
 */
public class TeleRuntimeMemoryRegion extends TeleTupleObject {

    private static final int TRACE_VALUE = 2;

    private Address regionStartCache = Address.zero();
    private long nBytesCache = 0L;
    private String regionNameCache = null;
    private MemoryUsage memoryUsageCache = MaxMemoryRegion.Util.NULL_MEMORY_USAGE;

    private final Object localStatsPrinter = new Object() {

        @Override
        public String toString() {
            return "region name=" + (regionNameCache == null ? "<unassigned>" : regionNameCache);
        }
    };

    TeleRuntimeMemoryRegion(TeleVM vm, Reference runtimeMemoryRegionReference) {
        super(vm, runtimeMemoryRegionReference);
        TimedTrace timedTrace = new TimedTrace(TRACE_VALUE, tracePrefix() + "Initializing");
        timedTrace.begin();
        // Exception to general policy of not doing too much in constructors; some subclasses
        // need to have the basic location information available at construction time.
        updateRegionInfoCache();
        timedTrace.end(localStatsPrinter);
    }

    /** {@inheritDoc}
     * <br>
     * Optimized for certain kinds of regions that describe non-relocatable memory
     * allocations in the VM; in those cases, once location and name information
     * are read, then further cache updates are skipped.
     */
    @Override
    protected void updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        super.updateObjectCache(epoch, statsPrinter);
        statsPrinter.addStat(localStatsPrinter);
        if (!isRelocatable() && isAllocated() && regionNameCache != null) {
            statsPrinter.addStat("allocated & not relocatable, no location refresh");
        } else {
            updateRegionInfoCache();
        }
    }

    /**
     * Attempts to read information about the region from the {@link MemoryRegion} object in VM memory.
     */
    private void updateRegionInfoCache() {
        try {
            final long nBytes = vm().teleFields().MemoryRegion_size.readWord(reference()).asSize().toLong();

            final Reference regionNameStringReference = vm().teleFields().MemoryRegion_regionName.readReference(reference());
            final TeleString teleString = (TeleString) heap().makeTeleObject(regionNameStringReference);
            final String regionName = teleString == null ? "<null>" : teleString.getString();

            Address regionStart = vm().teleFields().MemoryRegion_start.readWord(reference()).asAddress();
            if (regionStart.isZero() && regionName != null && regionName.equals(heap().bootHeapRegionName())) {
                // Ugly special case: the regionStart field of the static that defines the boot heap region
                // is set at zero in the boot image and only gets set to the real value when the VM starts running.
                // We lie in this situation.
                regionStart = vm().bootImageStart();
            }
            if (regionStart.isZero()) {
                Trace.line(TRACE_VALUE, tracePrefix() + "zero start address read from VM for region " + this);
            }
            // Wait until everything read before updating (crude atomicity).
            this.regionStartCache = regionStart;
            this.nBytesCache = nBytes;
            this.regionNameCache = regionName;
            if (nBytesCache != memoryUsageCache.getUsed()) {
                this.memoryUsageCache =  MaxMemoryRegion.Util.defaultUsage(nBytesCache);
            }
        } catch (DataIOError dataIOError) {
            TeleWarning.message("TeleRuntimeMemoryRegion dataIOError:", dataIOError);
            dataIOError.printStackTrace();
            // No update; data unreadable for some reason
            // TODO (mlvdv)  replace this with a more general mechanism for responding to VM unavailable
        }
    }

    /**
     * @return the descriptive name assigned to the region described by a {@link MemoryRegion} object in the VM;
     */
    public final String getRegionName() {
        return regionNameCache;
    }

    /**
     * @return starting location of the region described by a {@link MemoryRegion}  object in the VM;
     * zero if not yet allocated.
     */
    public final Address getRegionStart() {
        return regionStartCache;
    }

    /**
     * @return the size of the VM memory in bytes described by a {@link MemoryRegion} object in the VM.
     */
    public long getRegionNBytes() {
        return nBytesCache;
    }

    /**
     * @return the end location of the VM memory region described by a {@link MemoryRegion} object in the VM.
     */
    public final Address getRegionEnd() {
        return getRegionStart().plus(getRegionNBytes());
    }

    /**
     * Computes the usage of the the memory region described by a {@link MemoryRegion} object in the VM.
     * <br>
     * The default is to assume 100% utilized,
     * but specific subclasses may have more refined information available.
     * <br>
     * Returns {@link MaxMemoryRegion.Util.NULL_MEMORY_USAGE} if no information available.
     */
    public MemoryUsage getUsage() {
        return memoryUsageCache;
    }

    /**
     * Determines whether an address is in the allocated portion of the {@link MemoryRegion}
     * described by a memory region object in the VM.
     * <br>
     * The default is to assume that all of the region is allocated, but
     * specific subclasses may have more refined information available.
     */
    public boolean containsInAllocated(Address address) {
        return isAllocated() ? address.greaterEqual(getRegionStart()) && address.lessThan(getRegionEnd()) : false;
    }

    /**
     * @return whether memory has been allocated yet for the {@link MemoryRegion}
     * described by a memory region object in the VM.
     */
    public final boolean isAllocated() {
        return !getRegionStart().isZero() && getRegionNBytes() > 0;
    }

    /**
     * @return whether the memory region described by a {@link MemoryRegion} object in the VM
     * can be relocated once allocated.
     */
    public boolean isRelocatable() {
        return true;
    }

}
