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
package com.sun.max.tele.heap;

import java.io.*;
import java.lang.management.*;
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;

/**
 * Description of an allocation area in the VM owned by the heap.
 * <p>
 * Interaction with objects in the area is delegated to an instance of {@link RemoteObjectReferenceManager}, which permits
 * specialized implementations of {@link RemoteReference}s to be created that embody knowledge of specific heap implementations in the VM.
 * <p>
 * If no {@link RemoteObjectReferenceManager} is specified, the default is an instance of {@link FixedObjectRemoteReferenceManager},
 * whose implementation assumes that it is managing a single allocated region, that the allocation never
 * moves and that it is unmanaged:  objects, once created, are never moved or collected.
 *
 * @see RemoteObjectReferenceManager
 */
public final class VmHeapRegion extends AbstractVmHolder implements MaxHeapRegion, VmObjectHoldingRegion<MaxHeapRegion> {

    private static final int TRACE_VALUE = 1;
    private static final List<MaxEntityMemoryRegion< ? extends MaxEntity>> EMPTY_REGION_LIST = Collections.emptyList();

    private final String entityDescription;
    private final TeleMemoryRegion teleMemoryRegion;
    private final MaxEntityMemoryRegion<MaxHeapRegion> memoryRegion;
    private final RemoteObjectReferenceManager objectReferenceManager;

    /**
     * Creates a description of a heap allocation region in the VM, with information drawn from
     * a VM object describing the memory region.  The region is assumed to be at a fixed location, and
     * it is assumed to be unmanaged: objects, once created are never moved or collected.
     */
    public VmHeapRegion(TeleVM vm, TeleMemoryRegion teleMemoryRegion) {
        super(vm);
        this.teleMemoryRegion = teleMemoryRegion;
        this.memoryRegion = new DelegatedHeapRegionMemoryRegion(vm, teleMemoryRegion);
        this.objectReferenceManager = new FixedObjectRemoteReferenceManager(vm, this);
        this.entityDescription = "The allocation area " + memoryRegion.regionName() + " owned by the VM heap";
        Trace.line(TRACE_VALUE, tracePrefix() + "heap region created for " + memoryRegion.regionName() + " with " + objectReferenceManager.getClass().getSimpleName());
    }

    /**
     * Creates a description of a heap allocation region in the VM, with information about the
     * memory region described explicitly.  The region is assumed to be at a fixed location, and
     * it is assumed to be unmanaged: objects, once created are never moved or collected.
     */
    public VmHeapRegion(TeleVM vm, String name, Address start, long nBytes) {
        super(vm);
        this.teleMemoryRegion = null;
        this.memoryRegion = new FixedHeapRegionMemoryRegion(vm, name, start, nBytes);
        this.objectReferenceManager = new FixedObjectRemoteReferenceManager(vm, this);
        this.entityDescription = "The allocation area " + memoryRegion.regionName() + " owned by the VM heap";
        Trace.line(TRACE_VALUE, tracePrefix() + "heap region created for " + memoryRegion.regionName() + " with " + objectReferenceManager.getClass().getSimpleName());
    }

    /**
     * Creates a description of a heap allocation region in the VM, with information drawn from
     * a VM object describing the memory region.  The region is assumed to be at a fixed location, and
     * it is assumed to be unmanaged: objects, once created are never moved or collected.
     */
    public VmHeapRegion(TeleVM vm, TeleMemoryRegion teleMemoryRegion, RemoteObjectReferenceManager objectReferenceManager) {
        super(vm);
        this.teleMemoryRegion = teleMemoryRegion;
        this.memoryRegion = new DelegatedHeapRegionMemoryRegion(vm, teleMemoryRegion);
        this.objectReferenceManager = objectReferenceManager;
        this.entityDescription = "The allocation area " + memoryRegion.regionName() + " owned by the VM heap";
        Trace.line(TRACE_VALUE, tracePrefix() + "heap region created for " + memoryRegion.regionName() + " with " + objectReferenceManager.getClass().getSimpleName());
    }

    public String entityName() {
        return memoryRegion().regionName();
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxHeapRegion> memoryRegion() {
        return memoryRegion;
    }

    public boolean contains(Address address) {
        return memoryRegion().contains(address);
    }

    public TeleObject representation() {
        return teleMemoryRegion;
    }

    public RemoteObjectReferenceManager objectReferenceManager() {
        return objectReferenceManager;
    }

    public void printSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        final NumberFormat formatter = NumberFormat.getInstance();
        // Line 1
        printStream.println(indentation + entityName());
        // Line 2
        final StringBuilder sb2 = new StringBuilder();
        sb2.append("region: ");
        final MemoryUsage usage = memoryRegion().getUsage();
        final long size = usage.getCommitted();
        sb2.append("size=" + formatter.format(size));
        if (size > 0) {
            final long used = usage.getUsed();
            sb2.append(", usage=" + (Long.toString(100 * used / size)) + "%");
        }
        printStream.println(indentation + "    " + sb2.toString());
        // Line 3
        // TODO (mlvdv)  change this, since the manager can manage multiple regions.
 //       objectReferenceManager.printSessionStats(printStream, indent + 4, verbose);
    }

    @Override
    public void printObjectSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        printStream.println(indentation + "Object session stats for: " + entityName());
    }


    public void updateStatus(long epoch) {
        teleMemoryRegion.updateCache(epoch);
    }

    /**
     * Description of an ordinary memory region allocated by the VM heap, as described by a VM object.
     * <p>
     * This region has no parent; it is allocated from the OS.
     * <p>
     * This region has no children. We could decompose the region into sub-regions representing individual objects, but
     * we don't do that at this time.
     */
    private final class DelegatedHeapRegionMemoryRegion extends TeleDelegatedMemoryRegion implements MaxEntityMemoryRegion<MaxHeapRegion> {

        protected DelegatedHeapRegionMemoryRegion(MaxVM vm, TeleMemoryRegion teleMemoryRegion) {
            super(vm, teleMemoryRegion);
            assert teleMemoryRegion != null;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            // Heap regions are allocated from the OS, not part of any other region
            return null;
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            // We don't break a heap memory region into any smaller entities, but could.
            return EMPTY_REGION_LIST;
        }

        public VmHeapRegion owner() {
            return VmHeapRegion.this;
        }

    }

    /**
     * Description of a memory region allocated by the VM heap, where the description is known completely
     * without reference to a VM object.
     * <p>
     * This region has no parent; it is allocated from the OS.
     * <p>
     * This region has no children. We could decompose the region into sub-regions representing individual objects, but
     * we don't do that at this time.
     */
    private final class FixedHeapRegionMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxHeapRegion> {

        protected FixedHeapRegionMemoryRegion(MaxVM vm, String regionName, Address start, long nBytes) {
            super(vm, regionName, start, nBytes);
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            // Heap regions are allocated from the OS, not part of any other region
            return null;
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            // We don't break a heap memory region into any smaller entities, but could.
            return EMPTY_REGION_LIST;
        }

        public VmHeapRegion owner() {
            return VmHeapRegion.this;
        }

    }


}
