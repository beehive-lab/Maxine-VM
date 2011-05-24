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

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Canonical surrogate for a {@link CodeRegion} object in the VM, which describes a VM memory region
 * that is used to allocate compiled code.
 * <br>
 * This implementation eagerly caches descriptions of every {@TargetMethod} object allocated
 * in the region.
 *
 * @author Michael Van De Vanter
 * @see CodeRegion
 */
public final class TeleCodeRegion extends TeleLinearAllocationMemoryRegion {

    private static final int TRACE_VALUE = 2;

    private boolean initialized = false;
    private boolean isBootCodeRegion = false;
    private final List<TeleTargetMethod> teleTargetMethods = new ArrayList<TeleTargetMethod>();

    private final Object localStatsPrinter = new Object() {

        private int previousMethodCount = 0;

        @Override
        public String toString() {
            final int methodCount = teleTargetMethods.size();
            final int newMethodCount =  methodCount - previousMethodCount;
            final StringBuilder msg = new StringBuilder();
            msg.append("#methods=(").append(methodCount);
            msg.append(",new=").append(newMethodCount).append(")");
            previousMethodCount = methodCount;
            return msg.toString();
        }
    };

    TeleCodeRegion(TeleVM vm, Reference codeRegionReference) {
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

    /**
     * {@inheritDoc}
     * <p>
     * Register any new compiled methods that have appeared since the previous successful refresh.
     */
    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        // Don't try until the code cache is ready, which it isn't early in the startup sequence.
        // Also make sure that the region has actually been allocated before trying.
        if (!vm().codeCache().isInitialized() || !isAllocated()) {
            return false;
        }
        int length = vm().teleFields().CodeRegion_length.readInt(reference());
        Reference targetMethodsReference = vm().teleFields().CodeRegion_targetMethods.readReference(reference());
        int index = teleTargetMethods.size();
        while (index < length) {
            Reference targetMethodReference = vm().getElementValue(Kind.REFERENCE, targetMethodsReference, index++).asReference();
            // Creating a {@link TeleTargetMethod} causes it to be added to the code registry
            TeleTargetMethod teleTargetMethod = (TeleTargetMethod) heap().makeTeleObject(targetMethodReference);
            if (teleTargetMethod == null) {
                vm().invalidReferencesLogger().record(targetMethodReference, TeleTargetMethod.class);
                continue;
            }
            teleTargetMethods.add(teleTargetMethod);
        }
        statsPrinter.addStat(localStatsPrinter);
        return true;
    }

    @Override
    public long getRegionNBytes() {
        if (isBootCodeRegion()) {
            // The explicit representation of the boot {@link CodeRegion} gets "trimmed" by setting its size
            // to the amount allocated within the region.  Other regions don't have this happen.
            // We lie about this and return the size allocated for the whole region, as recorded in the boot image.
            return vm().bootImage().header.codeSize;
        }
        return super.getRegionNBytes();
    }

    /**
     * Gets all method compilations; assumes no eviction, no reordering.
     *
     * @return all compiled methods known to be in the region.
     */
    public List<TeleTargetMethod> teleTargetMethods() {
        return teleTargetMethods;
    }

    /**
     * @return the number of method compilations that have been copied from
     * the VM and cached locally.
     */
    public int methodLoadedCount() {
        int count = 0;
        for (TeleTargetMethod teleTargetMethod : teleTargetMethods) {
            if (teleTargetMethod.isLoaded()) {
                count++;
            }
        }
        return count;
    }
}
