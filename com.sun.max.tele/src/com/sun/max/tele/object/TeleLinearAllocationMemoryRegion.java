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
package com.sun.max.tele.object;

import java.lang.management.*;

import com.sun.max.atomic.*;
import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * Canonical surrogate for a {@link LinearAllocationMemoryRegion} object in the VM,
 * which represents a region of memory
 * that is allocated linearly from the beginning, and which holds an
 * <i>allocation mark</i> that represents the limit of the current allocation.
 * <br>
 * Usage defaults to 100% if nothing can be determined about the allocation mark.
 */
public class TeleLinearAllocationMemoryRegion extends TeleMemoryRegion {

    private static final int TRACE_VALUE = 1;

    /**
     * Cached contents of the mark field from the {@link LinearAllocationMemoryRegion} object in the VM.
     */
    private Address markCache;

    private MemoryUsage usageCache = MaxMemoryRegion.Util.NULL_MEMORY_USAGE;

    private final Object localStatsPrinter = new Object() {

        @Override
        public String toString() {
            return "mark=" + markCache.to0xHexString();
        }
    };

    public TeleLinearAllocationMemoryRegion(TeleVM vm, RemoteReference linearAllocationMemoryRegionReference) {
        super(vm, linearAllocationMemoryRegionReference);
        // Initialize mark to region end: default 100% utilization
        markCache = super.getRegionEnd();
        updateMarkCache();
    }

    /** {@inheritDoc}
     * <p>
     * Preempt upward the priority for tracing on {@link LinearAllocationMemoryRegion} objects,
     * since they usually represent very significant regions.
     */
    @Override
    protected int getObjectUpdateTraceValue(long epoch) {
        return TRACE_VALUE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Reads the allocation marker and estimates usage in a memory region that
     * is allocated linearly.
     */
    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        statsPrinter.addStat(localStatsPrinter);
        return updateMarkCache();
    }

    /**
     * Attempts to read information about the {@link LinearAllocationMemoryRegion}
     * region's mark from the object in VM memory.
     */
    private boolean updateMarkCache() {
        try {
            final RemoteReference markReference = fields().LinearAllocationMemoryRegion_mark.readRemoteReference(reference());
            markCache = markReference.readWord(AtomicWord.valueOffset()).asPointer();
            // This essentially marks the usage cache as dirty.
            usageCache = MaxMemoryRegion.Util.NULL_MEMORY_USAGE;
        } catch (DataIOError dataIOError) {
            // No update; data read failed for some reason other than VM availability
            TeleWarning.message("TeleLinearAllocationMemoryRegion: ", dataIOError);
            dataIOError.printStackTrace();
            return false;
            // TODO (mlvdv)  replace this with a more general mechanism for responding to VM unavailable
        }
        return true;
    }

    /** {@inheritDoc}
     * <p>
     * This override reports "used memory" to be between region start and the allocation mark.
     */
    @Override
    public final MemoryUsage getUsage() {
        if (isAllocated()) {
            long used = markCache.minus(getRegionStart()).toLong();
            long committed = getRegionNBytes();
            if (used != usageCache.getUsed() || committed != usageCache.getCommitted()) {
                usageCache = new MemoryUsage(-1L, used, committed, -1L);
            }
        }
        return usageCache;
    }

    /**
     * Gets most recently read value from the mark field of the {@link LinearAllocationMemoryRegion} in the VM.
     */
    @Override
    public Address mark() {
        return markCache;
    }

    /** {@inheritDoc}
     * <p>
     * In a {@link LinearAllocationMemoryRegion}, the allocated region is assumed to be between
     * the region's start and its "mark" location.
     */
    @Override
    public boolean containsInAllocated(Address address) {
        return isAllocated() ? address.greaterEqual(getRegionStart()) && address.lessThan(markCache) : false;
    }

}
