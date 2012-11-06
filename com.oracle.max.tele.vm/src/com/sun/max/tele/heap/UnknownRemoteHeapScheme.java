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
package com.sun.max.tele.heap;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.tele.*;

/**
 * Implementation details about the dynamic heap in the VM, specialized
 * for situations where there is no specific support code available for the heap scheme.
 * <p>
 * Assumes:
 * <ul>
 * <li>Information about VM heap regions will be reported in {@link InspectableHeapInfo}</li>
 * <li>Any address in any of regions is valid</li>
 * <li>The heap is always in the {@link HeapPhase#MUTATING} phase.</li>
 * <ul>
 */
public final class UnknownRemoteHeapScheme extends AbstractRemoteHeapScheme {

    List<VmHeapRegion> heapRegions = new ArrayList<VmHeapRegion>();

    protected UnknownRemoteHeapScheme(TeleVM vm) {
        super(vm);
        final VmAddressSpace addressSpace = vm().addressSpace();
        // There might already be dynamically allocated regions in a dumped image or when attaching to a running VM
        for (MaxMemoryRegion dynamicHeapRegion : getDynamicHeapRegionsUnsafe()) {
            final VmHeapRegion fakeDynamicHeapRegion =
                new VmHeapRegion(vm, dynamicHeapRegion.regionName(), dynamicHeapRegion.start(), dynamicHeapRegion.nBytes());
            heapRegions.add(fakeDynamicHeapRegion);
            addressSpace.add(fakeDynamicHeapRegion.memoryRegion());
        }
    }

    public Class heapSchemeClass() {
        return null;
    }

    public void initialize(long epoch) {
    }

    public List<VmHeapRegion> heapRegions() {
        return heapRegions;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Use safe, object-based methods to read the array of references from {@link InspectableHeapInfo} in the VM
     * that point to descriptions of allocated heap regions.
     */
    @Override
    public void updateMemoryStatus(long epoch) {
        final RemoteReference runtimeHeapRegionsArrayReference = fields().InspectableHeapInfo_dynamicHeapMemoryRegions.readRemoteReference(vm());
        if (!runtimeHeapRegionsArrayReference.isZero()) {
            final TeleArrayObject teleArrayObject = (TeleArrayObject) objects().makeTeleObject(runtimeHeapRegionsArrayReference);
            final RemoteReference[] heapRegionReferences = (RemoteReference[]) teleArrayObject.shallowCopy();
            for (int i = 0; i < heapRegionReferences.length; i++) {
                final TeleMemoryRegion dynamicHeapMemoryRegion = (TeleMemoryRegion) objects().makeTeleObject(heapRegionReferences[i]);
                if (dynamicHeapMemoryRegion != null) {
                    final VmHeapRegion oldHeapRegion = find(dynamicHeapMemoryRegion);
                    if (oldHeapRegion != null) {
                        // We've seen this VM heap region object before and already have an entity that models the state
                        // Force an early update of the cached data about the region
                        oldHeapRegion.updateStatus(epoch);
                    } else {
                        final VmHeapRegion newVmHeapRegion = new VmHeapRegion(vm(), dynamicHeapMemoryRegion);
                        heapRegions.add(newVmHeapRegion);
                        vm().addressSpace().add(newVmHeapRegion.memoryRegion());
                    }
                }
            }
        }
    }

    public MaxMemoryManagementInfo getMemoryManagementInfo(final Address address) {
        return new MaxMemoryManagementInfo() {

            public MaxMemoryManagementStatus status() {
                final MaxHeapRegion heapRegion = heap().findHeapRegion(address);
                if (heapRegion == null) {
                    // The location is not in any memory region allocated by the heap.
                    return MaxMemoryManagementStatus.NONE;
                }
                if (heapRegion.memoryRegion().containsInAllocated(address)) {
                    return MaxMemoryManagementStatus.LIVE;
                }
                return MaxMemoryManagementStatus.FREE;
            }

            public String terseInfo() {
                return null;
            }

            public String shortDescription() {
                return vm().heapScheme().name();
            }

            public Address address() {
                return address;
            }
            public TeleObject tele() {
                return null;
            }
        };
    }

    /**
     * Finds an existing heap region, if any, that has been created using the
     * remote object describing it.
     */
    private VmHeapRegion find(TeleMemoryRegion runtimeMemoryRegion) {
        for (VmHeapRegion heapRegion : heapRegions) {
            if (runtimeMemoryRegion == heapRegion.representation()) {
                return heapRegion;
            }
        }
        return null;
    }

}
