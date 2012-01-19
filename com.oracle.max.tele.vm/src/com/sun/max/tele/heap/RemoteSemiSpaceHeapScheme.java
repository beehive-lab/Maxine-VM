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
import com.sun.max.tele.debug.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.semiSpace.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;


public class RemoteSemiSpaceHeapScheme extends AbstractRemoteHeapScheme {

    List<VmHeapRegion> dynamicHeapRegions = new ArrayList<VmHeapRegion>(2);

    protected RemoteSemiSpaceHeapScheme(TeleVM vm) {
        super(vm);
        final VmAddressSpace addressSpace = vm().addressSpace();
        // There might already be dynamically allocated regions in a dumped image or when attaching to a running VM
        for (MaxMemoryRegion dynamicHeapRegion : getDynamicHeapRegionsUnsafe()) {
            final VmHeapRegion fakeDynamicHeapRegion =
                new VmHeapRegion(vm, dynamicHeapRegion.regionName(), dynamicHeapRegion.start(), dynamicHeapRegion.nBytes());
            dynamicHeapRegions.add(fakeDynamicHeapRegion);
            addressSpace.add(fakeDynamicHeapRegion.memoryRegion());
        }
    }

    public Class heapSchemeClass() {
        return SemiSpaceHeapScheme.class;
    }

    public List<VmHeapRegion> dynamicHeapRegions() {
        return dynamicHeapRegions;
    }

    /**
     * {@inheritDoc}
     * <p>
     * No need to look for heap regions that disappear for this collector.
     * <p>
     * Once allocated, the two regions never change, but we force a refresh on the memory region descriptions.
     */
    public void updateMemoryStatus(long epoch) {
        final Reference runtimeHeapRegionsArrayReference = fields().InspectableHeapInfo_dynamicHeapMemoryRegions.readReference(vm());
        if (!runtimeHeapRegionsArrayReference.isZero()) {
            final TeleArrayObject teleArrayObject = (TeleArrayObject) objects().makeTeleObject(runtimeHeapRegionsArrayReference);
            final Reference[] heapRegionReferences = (Reference[]) teleArrayObject.shallowCopy();
            for (int i = 0; i < heapRegionReferences.length; i++) {
                final TeleRuntimeMemoryRegion dynamicHeapMemoryRegion = (TeleRuntimeMemoryRegion) objects().makeTeleObject(heapRegionReferences[i]);
                if (dynamicHeapMemoryRegion != null) {
                    final VmHeapRegion oldHeapRegion = find(dynamicHeapMemoryRegion);
                    if (oldHeapRegion != null) {
                        // We've seen this VM heap region object before and already have an entity that models the state
                        // Force an early update of the cached data about the region
                        oldHeapRegion.updateStatus(epoch);
                    } else {
                        final VmHeapRegion newVmHeapRegion = new VmHeapRegion(vm(), dynamicHeapMemoryRegion);
                        dynamicHeapRegions.add(newVmHeapRegion);
                        vm().addressSpace().add(newVmHeapRegion.memoryRegion());
                    }
                }
            }
        }
    }

    public MaxMemoryManagementInfo getMemoryManagementInfo(final Address address) {
        return new MaxMemoryManagementInfo() {

            public MaxMemoryStatus status() {
                final MaxHeapRegion heapRegion = heap().findHeapRegion(address);
                if (heapRegion == null) {
                    // The location is not in any memory region allocated by the heap.
                    return MaxMemoryStatus.UNKNOWN;
                }
                if (heap().phase().isCollecting()) {
                    // Don't quibble if we're in a GC, as long as the address is in either the To or From regions.
                    return MaxMemoryStatus.LIVE;
                }
                if (heapRegion.entityName().equals(SemiSpaceHeapScheme.FROM_REGION_NAME)) {
                    // When not in GC, everything in from-space is dead
                    return MaxMemoryStatus.DEAD;
                }
                if (!heapRegion.memoryRegion().containsInAllocated(address)) {
                    // everything in to-space after the global allocation mark is dead
                    return MaxMemoryStatus.FREE;
                }
                for (TeleNativeThread teleNativeThread : vm().teleProcess().threads()) { // iterate over threads in check in case of tlabs if objects are dead or live
                    TeleThreadLocalsArea teleThreadLocalsArea = teleNativeThread.localsBlock().tlaFor(SafepointPoll.State.ENABLED);
                    if (teleThreadLocalsArea != null) {
                        Word tlabDisabledWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_DISABLED_THREAD_LOCAL_NAME);
                        Word tlabMarkWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_MARK_THREAD_LOCAL_NAME);
                        Word tlabTopWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_TOP_THREAD_LOCAL_NAME);
                        if (tlabDisabledWord.isNotZero() && tlabMarkWord.isNotZero() && tlabTopWord.isNotZero()) {
                            if (address.greaterEqual(tlabMarkWord.asAddress()) && tlabTopWord.asAddress().greaterThan(address)) {
                                return MaxMemoryStatus.FREE;
                            }
                        }
                    }
                }
                // Everything else should be live.
                return MaxMemoryStatus.LIVE;
            }

            public String terseInfo() {
                // Provide text to be displayed in display cell
                return "";
            }

            public String shortDescription() {
                // More information could be added here
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
    private VmHeapRegion find(TeleRuntimeMemoryRegion runtimeMemoryRegion) {
        for (VmHeapRegion heapRegion : dynamicHeapRegions) {
            if (runtimeMemoryRegion == heapRegion.representation()) {
                return heapRegion;
            }
        }
        return null;
    }
}
