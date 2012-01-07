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

/**
 * A manager for remote references to objects allocated in a region of VM memory where:
 * <ul>
 * <li>The region is assumed not to move during its lifetime; and</li>
 * <li>Objects, once created never move and are never collected/evicted.</li>
 * </ul>
 */
public final class FixedObjectRemoteReferenceManager extends AbstractRemoteReferenceManager {

    private static final int TRACE_VALUE = 1;

    private final MaxObjectHoldingRegion objectRegion;

    /**
     * Map:  address in VM --> a {@link TeleReference} that refers to the object whose origin is at that location.
     */
    private Map<Long, WeakReference<RemoteTeleReference>> originToReference = new HashMap<Long, WeakReference<RemoteTeleReference>>();

    /**
     * Creates a manager for remote references to objects in a particular region
     * of memory in the VM, presumed to be an unmanaged region in which object
     * never move and are never collected.
     */
    public FixedObjectRemoteReferenceManager(TeleVM vm, MaxObjectHoldingRegion objectRegion) {
        super(vm);
        this.objectRegion = objectRegion;
    }

    public MaxObjectHoldingRegion objectRegion() {
        return objectRegion;
    }

    public boolean isObjectOrigin(Address origin) throws TeleError {
        TeleError.check(objectRegion.memoryRegion().contains(origin), "Location is outside region");
        // The only way we can tell in general is with the heuristic.
        return objects().isObjectOriginHeuristic(origin);
    }

    public TeleReference makeReference(Address origin) {
        TeleError.check(objectRegion.memoryRegion().contains(origin), "Attempt to make reference at location outside region");
        RemoteTeleReference teleReference = null;
        final WeakReference<RemoteTeleReference> existingRef = originToReference.get(origin.toLong());
        if (existingRef != null) {
            teleReference = existingRef.get();
        }
        if (teleReference == null && objects().isObjectOriginHeuristic(origin)) {
            teleReference = new UnmanagedCanonicalTeleReference(vm(), origin);
            originToReference.put(origin.toLong(), new WeakReference<RemoteTeleReference>(teleReference));
        }
        return teleReference;
    }

    public int activeReferenceCount() {
        int count = 0;
        for (WeakReference<RemoteTeleReference> weakRef : originToReference.values()) {
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

