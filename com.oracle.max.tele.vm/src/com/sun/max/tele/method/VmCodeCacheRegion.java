/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.method;

import java.io.*;
import java.lang.management.*;
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Access to an <em>area</em> of the VM's code cache, described in the VM by a (heap) instance of {@link CodeRegion}.
 * All of the allocated data in an area of a code cache region is <em>owned</em>
 * by a method compilation, represented in the VM as a (heap) instance of {@link TargetMethod}.
 * <p>
 * Allocated data is stored in the same <em>object format</em> as are objects in the VM's heap.
 * Interaction with objects in the area is delegated to an instance of {@link RemoteObjectReferenceManager}, which permits
 * specialized implementations of {@link Reference} to be created that embody knowledge of how objects are managed in each region.
 *
 * @see CodeRegion
 * @see TargetMethod
 * @see VmCodeCacheAccess
 */
public abstract class VmCodeCacheRegion extends AbstractVmHolder
    implements TeleVMCache, MaxCodeCacheRegion, VmObjectHoldingRegion<MaxCodeCacheRegion>, MaxCodeHoldingRegion<MaxCodeCacheRegion> {

    private static final int TRACE_VALUE = 1;

    /**
     * Representation of a VM memory region used as part of the compiled code cache.
     * <p>
     * This region has no parent; it is either in the boot image or is allocated dynamically from the OS
     * <p>
     * This region has no children.
     * We could decompose it into sub-regions containing a method compilation each, but we don't
     * do that at this time.
     */
    private static final class CodeCacheMemoryRegion extends TeleDelegatedMemoryRegion implements MaxEntityMemoryRegion<MaxCodeCacheRegion> {

        private static final List<MaxEntityMemoryRegion< ? extends MaxEntity>> EMPTY = Collections.emptyList();

        private final MaxCodeCacheRegion owner;

        /**
         * Creates access to a VM memory object that describes part of the VM's code cache.
         * @param owner the object that models the code allocation area
         * @param teleCodeRegion the VM object that describes this region of VM memory
         */
        public CodeCacheMemoryRegion(MaxVM vm, MaxCodeCacheRegion owner, TeleCodeRegion teleCodeRegion) {
            super(vm, teleCodeRegion);
            this.owner = owner;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            // Compiled code regions are allocated from the OS, not part of any other region
            return null;
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            // We don't break a compiled code memory region into any smaller entities.
            return EMPTY;
        }

        public MaxCodeCacheRegion owner() {
            return owner;
        }

    }

    private final TeleCodeRegion teleCodeRegion;

    /**
     * Representation of the VM memory region holding this part of the code cache.
     */
    private final CodeCacheMemoryRegion codeCacheMemoryRegion;

    public VmCodeCacheRegion(TeleVM vm, TeleCodeRegion teleCodeRegion) {
        super(vm);
        this.teleCodeRegion = teleCodeRegion;
        this.codeCacheMemoryRegion = new CodeCacheMemoryRegion(vm, this, teleCodeRegion);
    }

    public String entityName() {
        return codeCacheMemoryRegion.regionName();
    }

    public MaxEntityMemoryRegion<MaxCodeCacheRegion> memoryRegion() {
        return codeCacheMemoryRegion;
    }

    public boolean contains(Address address) {
        return codeCacheMemoryRegion.contains(address);
    }

    public TeleObject representation() {
        return teleCodeRegion;
    }

    /**
     * Refresh enough state to understand the state of this region in the code cache, for example, whether
     * eviction is underway, without necessarily doing any other cache updates.
     * <p>
     * This must be done before heap objects are refreshed, in particular so that the
     * code cache region status can be used to determine refresh behavior in instances of
     * {@link TargetMethod}.
     */
    public final void updateStatus(long epoch) {
        teleCodeRegion.updateCache(epoch);
    }

    /**
     * @return whether code eviction ever takes place in this code region
     */
    public boolean isManaged() {
        return teleCodeRegion.isManaged();
    }

    /**
     * @return whether code eviction is currently underway in this code region
     */
    public boolean isInEviction() {
        return teleCodeRegion.isInEviction();
    }

    /**
     * @return the number of code evictions that have been completed in this code region
     */
    public long evictionCount() {
        return teleCodeRegion.evictionCount();
    }

    /**
     * @see MaxVM#inspectableObjects()
     */
    public abstract List<MaxObject> inspectableObjects();

    /**
     * Notifies the code cache region that a target method has been discovered
     * in the region and that an instance of {@link TeleTargetMethod} has
     * been created to represent it.
     *
     * @param teleTargetMethod a newly created {@link TeleTargetMethod}
     */
    public abstract void register(TeleTargetMethod teleTargetMethod);

    public abstract TeleCompilation findCompilation(Address address);

    /**
     * @return the number of method compilations that have been copied from
     * the VM and cached locally.  This is not done automatically when a
     * {@link TeleTargetMethod} is created corresponding to a compilation,
     * but rather done lazily when detailed information about the compilation is needed.
     */
    public abstract int loadedCompilationCount();

    public abstract void writeSummary(PrintStream printStream);

    public void printSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        final NumberFormat formatter = NumberFormat.getInstance();
        // Line 1
        final StringBuilder sb1 = new StringBuilder();
        sb1.append(indentation + entityName());
        if (isManaged()) {
            if (isInEviction()) {
                sb1.append("  (managed, EVICTION UNDERWAY)");
            } else {
                sb1.append(" (managed)");
            }
        }
        printStream.println(sb1.toString());
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
        final StringBuilder sb3 = new StringBuilder();
        sb3.append("compilations:  registered=").append(formatter.format(compilationCount()));
        sb3.append(", code loaded=").append(formatter.format(loadedCompilationCount()));
        if (teleCodeRegion.isManaged()) {
            sb3.append(", completed evictions=").append(formatter.format(teleCodeRegion.evictionCount()));
        }
        printStream.println(indentation + "    " + sb3.toString());
        // Line 4
        codePointerManager().printSessionStats(printStream, indent + 4, verbose);
        // Line 5
        // Was objectReferenceManager().printSessionStats(printStream, indent + 4, verbose);
    }

    @Override
    public void printObjectSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        printStream.println(indentation + "Object session stats for: " + entityName());
    }

    /**
     * A simple status printer to be used after updating state concerning
     * a VM area of code cache that is not managed, which is to say, the
     * registered contents can only grow monotonically, if at all.
     */
    protected final class UnmanagedCodeCacheRegionStatsPrinter {

        private int previousCompilationCount = 0;

        @Override
        public String toString() {
            final int compilationCount = compilationCount();
            final int newCompilationCount =  compilationCount - previousCompilationCount;
            final StringBuilder msg = new StringBuilder();
            msg.append("#compilations=(").append(compilationCount);
            msg.append(",new=").append(newCompilationCount).append(")");
            previousCompilationCount = compilationCount;
            return msg.toString();
        }
    }


}
