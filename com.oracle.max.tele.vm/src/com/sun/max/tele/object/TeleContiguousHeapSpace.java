/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for a {@link ContiguousHeapSpace} object in the VM.
 *
 * @see ContiguousHeapSpace
 */
public class TeleContiguousHeapSpace extends TeleMemoryRegion {

    private static final int TRACE_VALUE = 1;

    /**
     * Cached contents of the mark field from the {@link LinearAllocationMemoryRegion} object in the VM.
     */
    private Address committedEndCache;

    private MemoryUsage usageCache = MaxMemoryRegion.Util.NULL_MEMORY_USAGE;

    private final Object localStatsPrinter = new Object() {

        @Override
        public String toString() {
            return "committedEnd=" + committedEndCache.to0xHexString();
        }
    };

    public TeleContiguousHeapSpace(TeleVM vm, Reference contiguousHeapSpaceReference) {
        super(vm, contiguousHeapSpaceReference);
        // Initialize committed end to region end: default 100% utilization
        committedEndCache = super.getRegionEnd();
        updateCommittedEndCache();
    }

    /** {@inheritDoc}
     * <p>
     * Preempt upward the priority for tracing on {@link ContiguousHeapSpace} objects,
     * since they usually represent very significant regions.
     */
    @Override
    protected int getObjectUpdateTraceValue(long epoch) {
        return TRACE_VALUE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Reads the committed end address.
     */
    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        statsPrinter.addStat(localStatsPrinter);
        return updateCommittedEndCache();
    }

    /**
     * Attempts to read information about the committed end of the space in VM memory.
     */
    private boolean updateCommittedEndCache() {
        try {
            committedEndCache = fields().ContiguousHeapSpace_committedEnd.readWord(reference()).asAddress();
        } catch (DataIOError dataIOError) {
            // No update; data read failed for some reason other than VM availability
            TeleWarning.message("TeleContiguousHeapSpace: ", dataIOError);
            dataIOError.printStackTrace();
            return false;
            // TODO (mlvdv)  replace this with a more general mechanism for responding to VM unavailable
        }
        return true;
    }

    /** {@inheritDoc}
     * <p>
     * This override reports "used memory" to be the memory that is "committed".
     */
    @Override
    public final MemoryUsage getUsage() {
        if (isAllocated()) {
            long used = committedEndCache.minus(getRegionStart()).toLong();
            long committed = getRegionNBytes();
            if (used != usageCache.getUsed() || committed != usageCache.getCommitted()) {
                usageCache = new MemoryUsage(-1L, used, committed, -1L);
            }
        }
        return usageCache;
    }

    /** {@inheritDoc}
     * <p>
     * By default, assume that everything up to the committed end mark is allocated.
     */
    @Override
    public boolean containsInAllocated(Address address) {
        return isAllocated() ? address.greaterEqual(getRegionStart()) && address.lessThan(committedEndCache) : false;
    }

}
