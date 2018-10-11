/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Canonical surrogate for an object of type {@link CodeRegion} object in the VM, which describes a VM memory region
 * in which method compilations are allocated.
 *
 * @see CodeRegion
 */
public class TeleCodeRegion extends TeleLinearAllocationMemoryRegion {

    private static final int TRACE_VALUE = 2;

    private boolean initialized = false;
    private boolean isBootCodeRegion = false;

    /**
     * Reference to the array of references to {@link TargetMethod}s allocated in the code cache region.
     */
    private RemoteReference targetMethodsReference = vm().referenceManager().zeroReference();

    // These two counters tell us when an eviction has taken place since the last time
    // we checked, and whether we are currently in an eviction.
    protected long evictionStartedCount = 0;
    protected long evictionCompletedCount = 0;

    // These two counters tell us whether an addition to the array of target methods
    // in the code region has happened since the last time we checked, which has
    // implications for efficient refreshing our model of the array's contents.
    // They also tell us whether we an addition is currently underway,
    // in which case the state of the array is indeterminate.
    protected int additionStartedCount = 0;
    protected int additionCompletedCount = 0;

    TeleCodeRegion(TeleVM vm, RemoteReference codeRegionReference) {
        super(vm, codeRegionReference);
    }

    /**
     * @return whether the {@link CodeRegion} object describes the "boot code" region,
     * which is contained in the boot image of the VM.
     */
    private boolean isBootCodeRegion() {
        initialize();
        return isBootCodeRegion;
    }

    private void initialize() {
        if (!initialized) {
            isBootCodeRegion = getRegionName().equals(vm().codeCache().bootCodeRegionName());
            initialized = true;
        }
    }

    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        // We may get updated before initialization; if we don't force it now, we may step
        // into a very nasty infinite regress.
        initialize();
        // The target methods array might change if the array is grown, or via eviction.
        targetMethodsReference = fields().CodeRegion_targetMethods.readRemoteReference(reference());
        additionStartedCount = fields().CodeRegion_additionStartedCount.readInt(reference());
        additionCompletedCount = fields().CodeRegion_additionCompletedCount.readInt(reference());
        if (isManaged()) {
            evictionStartedCount = fields().CodeRegion_evictionStartedCount.readLong(reference());
            evictionCompletedCount = fields().CodeRegion_evictionCompletedCount.readLong(reference());
        }
        return true;
    }

    /**
     * Gets a reference to a {@link TargetMethod} from the code cache region, traversing a forwarder if present.
     *
     * @param index identifies the desired target method.  Must be less than {@link #nTargetMethods()}.
     * @return a reference to a {@link TargetMethod} in the VM.
     */
    public final RemoteReference getTargetMethodReference(int index) {
        return targetMethodsReference.readArrayAsRemoteReference(index);
    }

    /**
     * @return the number of {@link TargetMethod}s currently in the code cache region in the VM.
     */
    public final int nTargetMethods() {
        return fields().CodeRegion_length.readInt(reference());
    }

    @Override
    public final long getRegionNBytes() {
        initialize();
        if (isBootCodeRegion) {
            // The explicit representation of the boot {@link CodeRegion} gets "trimmed" by setting its size
            // to the amount allocated within the region.  Other regions don't have this happen.
            // We lie about this and return the size allocated for the whole region, as recorded in the boot image.
            return vm().bootImage().header.codeSize;
        }
        return super.getRegionNBytes();
    }

    /**
     * @return whether code eviction ever takes place in this code region
     */
    public boolean isManaged() {
        return false;
    }

    /**
     * @return whether eviction is underway in this code region
     */
    public final boolean isInEviction() {
        return evictionCompletedCount < evictionStartedCount;
    }

    /**
     * @return the number of code evictions that have been completed in this code region
     */
    public final long evictionCount() {
        return evictionCompletedCount;
    }

}
