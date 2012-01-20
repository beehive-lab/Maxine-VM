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

import java.lang.ref.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

/**
 * A manager for remote references to objects allocated in a single region of VM memory where:
 * <ul>
 * <li>The region is assumed not to move during its lifetime; and</li>
 * <li>Objects, once created never move and are never collected/evicted.</li>
 * </ul>
 */
public final class FixedObjectRemoteReferenceManager extends AbstractRemoteReferenceManager {

    private static final int TRACE_VALUE = 1;

    private final VmObjectHoldingRegion objectRegion;

    /**
     * Map:  address in VM --> a {@link RemoteReference} that refers to the object whose origin is at that location.
     */
    private Map<Long, WeakReference<RemoteReference>> originToReference = new HashMap<Long, WeakReference<RemoteReference>>();

    /**
     * Creates a manager for remote references to objects in a single region
     * of memory in the VM, presumed to be an unmanaged region in which object
     * never move and are never collected.
     */
    public FixedObjectRemoteReferenceManager(TeleVM vm, VmObjectHoldingRegion objectRegion) {
        super(vm);
        this.objectRegion = objectRegion;
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no GC cycle for an unmanaged code cache; object
     * are neither relocated nor collected.
     */
    public  HeapPhase heapPhase() {
        return HeapPhase.ALLOCATING;
    }

    public boolean isObjectOrigin(Address origin) throws TeleError {
        TeleError.check(objectRegion.memoryRegion().contains(origin), "Location is outside region");
        // The only way we can tell in general is with the heuristic.
        return objects().isObjectOriginHeuristic(origin);
    }

    public RemoteReference makeReference(Address origin) {
        TeleError.check(objectRegion.memoryRegion().contains(origin), "Attempt to make reference at location outside region");
        RemoteReference teleReference = null;
        final WeakReference<RemoteReference> existingRef = originToReference.get(origin.toLong());
        if (existingRef != null) {
            teleReference = existingRef.get();
        }
        if (teleReference == null && objects().isObjectOriginHeuristic(origin)) {
            teleReference = new UnmanagedCanonicalTeleReference(vm(), origin);
            originToReference.put(origin.toLong(), new WeakReference<RemoteReference>(teleReference));
        }
        return teleReference;
    }

    public int activeReferenceCount() {
        int count = 0;
        for (WeakReference<RemoteReference> weakRef : originToReference.values()) {
            if (weakRef.get() != null) {
                count++;
            }
        }
        return count;
    }

    public int totalReferenceCount() {
        return originToReference.size();
    }

    /**
     * A canonical remote object reference pointing into a region of VM memory that is unmanaged:
     * objects, once allocated, never move and are never collected/evicted.
     */
    private class UnmanagedCanonicalTeleReference extends ConstantTeleReference {

        UnmanagedCanonicalTeleReference(TeleVM vm, Address origin) {
            super(vm, origin);
        }

        @Override
        public ObjectMemoryStatus memoryStatus() {
            return ObjectMemoryStatus.LIVE;
        }
    }

}

