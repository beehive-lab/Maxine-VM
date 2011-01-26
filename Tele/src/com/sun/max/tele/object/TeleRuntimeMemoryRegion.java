/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for objects in the VM that represent a region of memory.
 *
 * @author Michael Van De Vanter
 */
public class TeleRuntimeMemoryRegion extends TeleTupleObject {

    private static final int TRACE_VALUE = 2;


    private Address regionStart = Address.zero();
    private Size regionSize = Size.zero();
    private String regionName = null;
    private MemoryUsage memoryUsage = null;

    private final Object localStatsPrinter = new Object() {

        @Override
        public String toString() {
            return "name=" + (regionName == null ? "<unassigned>" : regionName);
        }
    };

    TeleRuntimeMemoryRegion(TeleVM vm, Reference runtimeMemoryRegionReference) {
        super(vm, runtimeMemoryRegionReference);
        updateCache();
    }

    @Override
    protected void updateObjectCache(StatsPrinter statsPrinter) {
        super.updateObjectCache(statsPrinter);
        if (!isAllocated()) {
            statsPrinter.addStat("unallocated");
        }
        statsPrinter.addStat(localStatsPrinter);
        if (!isRelocatable() && isAllocated()) {
            // Optimization: if we know the region won't be moved by the VM, and
            // we already have the location information, then don't bother to refresh.
            statsPrinter.addStat("allocated, not relocatable");
            return;
        }
        try {
            final Size newRegionSize = vm().teleFields().MemoryRegion_size.readWord(reference()).asSize();

            final Reference regionNameStringReference = vm().teleFields().MemoryRegion_regionName.readReference(reference());
            final TeleString teleString = (TeleString) heap().makeTeleObject(regionNameStringReference);
            final String newRegionName = teleString == null ? "<null>" : teleString.getString();

            Address newRegionStart = vm().teleFields().MemoryRegion_start.readWord(reference()).asAddress();
            if (newRegionStart.isZero() && newRegionName != null) {
                if (newRegionName.equals(heap().bootHeapRegionName())) {
                    // Ugly special case:  the regionStart field of the static that defines the boot heap region
                    // is set at zero in the boot image, only set to the real value when the VM starts running.
                    // Lie about it.
                    newRegionStart = vm().bootImageStart();
                }
            }
            if (newRegionStart.isZero()) {
                Trace.line(TRACE_VALUE, tracePrefix() + "zero start address read from VM for region " + this);
            }
            this.regionStart = newRegionStart;
            this.regionSize = newRegionSize;
            this.regionName = newRegionName;
            final long sizeAsLong = this.regionSize.toLong();
            this.memoryUsage = new MemoryUsage(-1, sizeAsLong, sizeAsLong, -1);
        } catch (DataIOError dataIOError) {
            TeleWarning.message("TeleRuntimeMemoryRegion dataIOError:", dataIOError);
            dataIOError.printStackTrace();
            // No update; data unreadable for some reason
            // TODO (mlvdv)  replace this with a more general mechanism for responding to VM unavailable
        }
    }


    /**
     * @return the descriptive name assigned to the memory region object in the VM.
     */
    public final String getRegionName() {
        return regionName;
    }

    /**
     * @return starting location in VM memory of the region; zero if not yet allocated.
     */
    public final Address getRegionStart() {
        return regionStart;
    }

    /**
     * @return the size of the VM memory, as described by the memory region object in the VM.
     */
    public Size getRegionSize() {
        return regionSize;
    }

    /**
     * Computes the usage of the memory region, if available; default is to assume 100% utilized,
     * but specific subclasses may have more refined information available.
     */
    public MemoryUsage getUsage() {
        return memoryUsage;
    }

    /**
     * Determines whether an address is in the allocated portion of the memory region.
     * The default is to assume that all of the region is allocated, but
     * specific subclasses may have more refined information available.
     */
    public boolean containsInAllocated(Address address) {
        if (!isAllocated()) {
            return false;
        }
        // Default:  is the address anywhere in the region
        return address.greaterEqual(getRegionStart()) && address.lessThan(getRegionStart().plus(getRegionSize()));
    }

    /**
     * @return whether memory has been allocated yet in the VM for this region.
     */
    public final boolean isAllocated() {
        return !getRegionStart().isZero() && !getRegionSize().isZero();
    }

    /**
     * @return whether this region of VM memory might be relocated, once allocated.
     */
    public boolean isRelocatable() {
        return true;
    }

}
