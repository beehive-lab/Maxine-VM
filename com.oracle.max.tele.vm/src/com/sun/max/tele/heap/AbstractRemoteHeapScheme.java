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
import com.sun.max.tele.type.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.tele.*;


public abstract class AbstractRemoteHeapScheme extends AbstractVmHolder implements RemoteHeapScheme {

    protected HeapPhase phase = HeapPhase.ALLOCATING;
    protected long gcStartedCount = -1;
    protected long gcCompletedCount = -1;

    protected AbstractRemoteHeapScheme(TeleVM vm) {
        super(vm);
    }

    public Class schemeClass() {
        return HeapScheme.class;
    }

    public List<MaxCodeLocation> inspectableMethods() {
        return Collections.emptyList();
    }

    public MaxMarkBitsInfo markBitInfo() {
        return null;
    }

    /**
     * @return surrogate for the VM object that implements the {@link HeapScheme} interface
     */
    public TeleHeapScheme teleHeapScheme() {
        final TeleVMConfiguration vmConfiguration = vm().teleVMConfiguration();
        return vmConfiguration == null ? null : vmConfiguration.teleHeapScheme();
    }

    public void updateMemoryStatus(long epoch) {
        // Check what phase the heap is in with respect to GC.
        phase = HeapPhase.values()[fields().InspectableHeapInfo_heapPhaseOrdinal.readInt(vm())];

        // Check GC status and update references if a GC has completed since last time we checked
        final long oldGcStartedCount = gcStartedCount;
        gcStartedCount = fields().InspectableHeapInfo_gcStartedCounter.readLong(vm());
        gcCompletedCount = fields().InspectableHeapInfo_gcCompletedCounter.readLong(vm());
        // Invariant:  oldGcStartedCount <= gcCompletedCount <= gcStartedCount
        if (gcStartedCount != gcCompletedCount) {
            // A GC is in progress, local cache is out of date by definition but can't update yet
            // Sanity check; collection count increases monotonically
            assert  gcCompletedCount < gcStartedCount;
        } else if (oldGcStartedCount != gcStartedCount) {
            // GC is not in progress, but a GC has completed since the last time
            // we checked, so cached reference data is out of date
            // Sanity check; collection count increases monotonically
            assert oldGcStartedCount < gcStartedCount;
            // vm().referenceManager().updateCache(epoch);
        } else {
            // oldGcStartedCount == gcStartedCount == gcCompletedCount
            // GC is not in progress, and no new GCs have happened, so cached reference data is up to date
        }
    }

    public HeapPhase phase() {
        return phase;
    }

    protected long gcStartedCount() {
        return gcStartedCount;
    }

    /**
     * Creates a representation of the contents of the {@linkplain InspectableHeapInfo inspectable list} of dynamic heap
     * regions in the VM, using low level mechanisms and performing no checking that the location or objects are valid.
     * <p>
     * The intention is to provide a way to read this data without needing any of the usual type-based mechanisms for
     * reading data, all of which rely on a populated {@link VmClassAccess}. This is needed when attaching to a process
     * or reading a dump, where a description of the dynamic heap must be determined before the {@link VmClassAccess}
     * can be built.  Once those parts of the inspection state are in place, safer methods should be used.
     * <p>
     * <strong>Unsafe:</strong> this method depends on knowledge of the implementation of arrays.
     *
     * @return a list of objects, each of which describes a dynamically allocated heap region in the VM, empty array if
     *         no such heap regions
     *
     * @see InspectableHeapInfo
     */
    protected List<MaxMemoryRegion> getDynamicHeapRegionsUnsafe() {
        // Work only with temporary references that are unsafe across GC
        // Do no testing to determine if the reference points to a valid object in live memory of the correct types.

        final List<MaxMemoryRegion> regions = new ArrayList<MaxMemoryRegion>();

        // Location of the inspectable field that might point to an array of dynamically allocated heap regions
        final Pointer dynamicHeapRegionsArrayFieldPointer = vm().bootImageStart().plus(vm().bootImage().header.dynamicHeapRegionsArrayFieldOffset);

        // Value of the field, possibly a pointer to an array of dynamically allocated heap regions
        final Word fieldValue = memory().readWord(dynamicHeapRegionsArrayFieldPointer.asAddress());

        if (fieldValue.isNotZero()) {
            // Assert that this points to an array of references, read as words
            final RemoteReference wordArrayRef = referenceManager().makeUnsafeRemoteReference(fieldValue.asAddress());
            final int wordArrayLength = objects().unsafeReadArrayLength(wordArrayRef);

            // Read the references as words to avoid using too much machinery
            for (int index = 0; index < wordArrayLength; index++) {
                // Read an entry from the array
                final Word regionReferenceWord = Layout.getWord(wordArrayRef, index);
                // Assert that this points to an object of type {@link MemoryRegion} in the VM
                RemoteReference memoryRegionRef = referenceManager().makeUnsafeRemoteReference(regionReferenceWord.asAddress());
                // Read the field MemoryRegion.start
                final Address regionStartAddress = memoryRegionRef.readWord(fields().MemoryRegion_start.fieldActor().offset()).asAddress();
                // Read the field MemoryRegion.size
                final int regionSize = memoryRegionRef.readInt(fields().MemoryRegion_size.fieldActor().offset());
                regions.add(new TeleFixedMemoryRegion(vm(), "Fake", regionStartAddress, regionSize));
            }
        }
        return regions;
    }

}
