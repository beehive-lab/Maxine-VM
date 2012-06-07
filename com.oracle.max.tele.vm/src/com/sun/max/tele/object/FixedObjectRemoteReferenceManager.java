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

import java.io.*;
import java.lang.management.*;
import java.lang.ref.*;
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
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
public final class FixedObjectRemoteReferenceManager extends AbstractVmHolder implements RemoteObjectReferenceManager {

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
    public  HeapPhase phase() {
        return HeapPhase.MUTATING;
    }

    public ObjectStatus objectStatusAt(Address origin) throws TeleError {
        TeleError.check(objectRegion.memoryRegion().contains(origin), "Location is outside region");
        final WeakReference<RemoteReference> weakRef = originToReference.get(origin);
        if (weakRef != null) {
            final RemoteReference knownReference = weakRef.get();
            if (knownReference != null) {
                return knownReference.status();
            }
        }
        // The only way we can tell in general is with the heuristic.
        return objects().isPlausibleOriginUnsafe(origin) ? ObjectStatus.LIVE : ObjectStatus.DEAD;
    }

    public RemoteReference makeReference(Address origin) {
        assert vm().lockHeldByCurrentThread();
        TeleError.check(objectRegion.memoryRegion().contains(origin), "Attempt to make reference at location outside region");
        RemoteReference remoteReference = null;
        final WeakReference<RemoteReference> weakRef = originToReference.get(origin.toLong());
        if (weakRef != null) {
            remoteReference = weakRef.get();
        }
        if (remoteReference == null && objects().isPlausibleOriginUnsafe(origin)) {
            remoteReference = new UnmanagedCanonicalTeleReference(vm(), origin);
            originToReference.put(origin.toLong(), new WeakReference<RemoteReference>(remoteReference));
        }
        return remoteReference == null ? vm().referenceManager().zeroReference() : remoteReference;
    }

    private int activeReferenceCount() {
        int count = 0;
        for (WeakReference<RemoteReference> weakRef : originToReference.values()) {
            if (weakRef.get() != null) {
                count++;
            }
        }
        return count;
    }

    private int totalReferenceCount() {
        return originToReference.size();
    }

    public void printObjectSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final NumberFormat formatter = NumberFormat.getInstance();

        // Line 0
        String indentation = Strings.times(' ', indent);
        final StringBuilder sb0 = new StringBuilder();
        sb0.append(objectRegion.entityName());
        printStream.println(indentation + sb0.toString());

        // increase indentation
        indentation += Strings.times(' ', 4);

       // Line 1
        final StringBuilder sb1 = new StringBuilder();
        sb1.append("memory: ");
        final MaxEntityMemoryRegion memoryRegion = objectRegion.memoryRegion();
        final MemoryUsage usage = memoryRegion.getUsage();
        final long size = usage.getCommitted();
        if (size > 0) {
            sb1.append("size=" + formatter.format(size));
            final long used = usage.getUsed();
            sb1.append(", usage=" + (Long.toString(100 * used / size)) + "%");
        } else {
            sb1.append(" <unallocated>");
        }
        sb1.append(", unmanaged");
        printStream.println(indentation + sb1.toString());

        // Line 2, indented
        final StringBuilder sb2 = new StringBuilder();
        final int activeReferenceCount = activeReferenceCount();
        final int totalReferenceCount = totalReferenceCount();
        sb2.append("mapped object refs:  active=" + formatter.format(activeReferenceCount));
        sb2.append(", inactive=" + formatter.format(totalReferenceCount - activeReferenceCount));
        if (verbose) {
            sb2.append(", ref. mgr=" + getClass().getSimpleName());
        }
        printStream.println(indentation + sb2.toString());
    }


    /**
     * A canonical remote object reference pointing into a region of VM memory that is unmanaged:
     * objects, once allocated, never move and are never collected/evicted.
     */
    private class UnmanagedCanonicalTeleReference extends ConstantRemoteReference {

        UnmanagedCanonicalTeleReference(TeleVM vm, Address origin) {
            super(vm, origin);
        }

        @Override
        public ObjectStatus status() {
            return ObjectStatus.LIVE;
        }
    }

}

