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
package com.sun.max.tele.method;

import java.io.*;
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Singleton access to compiled code in the VM.
 * <p>
 * Much of the important state in the VM about the code cache is contained
 * in a singleton heap object, an instance of {@linkplain com.sun.max.vm.code.CodeManager CodeManager}.
 * <p>
 * The VM's cache allocates memory in one or more memory regions, above and beyond the boot code cache
 * that already exists in the boot image.
 * <p>
 * We attempt to track every method compilation, as soon as it can be discovered.  This is usually done
 * by various concrete subclasses of {@link VmCodeCacheRegion}, each of which specializes in one of the
 * code cache implementations in the VM.  At refresh time, each of those classes interrogates the VM
 * object that manages that code cache region in the VM, and loads summary information about any new
 * compilations instances of {@link TeleTargetMethod}.  Each newly created instance of {@link TeleTargetRegion}
 * registers itself here, so that it can be looked up by address.
 * <p>
 * Not every instance may be created in the usual way. There might be cases where we are inspecting an
 * instance of {@link TeleTargetMethod} as it is being created, which means that it may not yet have been
 * allocated memory in any code cache region. If a compilation is registered before allocation,
 * we set it aside and eventually register when the location information becomes known.
 *
 * @see com.sun.max.vm.code.CodeManager
 * @see TeleTargetMethod
 */
public final class VmCodeCacheAccess extends AbstractVmHolder implements MaxCodeCache, VmAllocationHolder<MaxCodeCache> {

    private static final int TRACE_VALUE = 1;

    private static VmCodeCacheAccess vmCodeCacheAccess;

    public static VmCodeCacheAccess make(TeleVM vm) {
        if (vmCodeCacheAccess == null) {
            vmCodeCacheAccess = new VmCodeCacheAccess(vm);
        }
        return vmCodeCacheAccess;
    }

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    private final String entityName = "Code Cache";

    private final String entityDescription;

    /**
     * The object in the VM that manages cached compiled code.
     */
    private TeleCodeManager teleCodeManager;

    private final String bootCodeCacheRegionName;

    // The three code cache regions known to be allocated by the VM
    private VmCodeCacheRegion bootCodeCacheRegion = null;
    private VmSemiSpaceCodeCacheRegion dynamicBaselineCodeCacheRegion = null;
    private VmCodeCacheRegion dynamicOptCodeCacheRegion = null;

    /**
     * Unmodifiable list of all regions in which compiled methods are stored.
     */
    private List<MaxCodeCacheRegion> maxCodeCacheRegions = Collections.emptyList();

    /**
     * Regions in which compiled methods are stored, held as the implementation type.
     */
    private List<VmCodeCacheRegion> vmCodeCacheRegions = Collections.emptyList();

    /**
     * Keep track of memory regions allocated from the OS that are <em>owned</em> by the code cache.
     */
    private List<MaxEntityMemoryRegion<? extends MaxEntity> > allocations = new ArrayList<MaxEntityMemoryRegion<? extends MaxEntity> >();

    /**
     * Creates the object that models the cache of machine code in the VM.
     * <p>
     * A subsequent call to {@link #initialize()} is required before this object is functional.
     */
    public VmCodeCacheAccess(TeleVM vm) {
        super(vm);
        assert vm().lockHeldByCurrentThread();
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + "creating");
        tracer.begin();
        this.entityDescription = "Storage managment in the " + vm().entityName() + " for method compilations";
        this.bootCodeCacheRegionName = vm().getString(fields().Code_CODE_BOOT_NAME.readReference(vm()));
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + "updating");
        tracer.end(null);
    }

    /**
     * Completes the initialization of this object:  identifies the VM's code regions, and preloads
     * the cache of information about method compilations in the boot code region.
     */
    public void initialize(long epoch) {
        assert vm().lockHeldByCurrentThread();
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + "initializing");
        tracer.begin();

        teleCodeManager = (TeleCodeManager) objects().makeTeleObject(fields().Code_codeManager.readReference(vm()));
        bootCodeCacheRegion = new VmBootCodeCacheRegion(vm(), teleCodeManager.teleBootCodeRegion(), this);
        vm().addressSpace().add(bootCodeCacheRegion.memoryRegion());
        dynamicBaselineCodeCacheRegion = new VmSemiSpaceCodeCacheRegion(vm(), teleCodeManager.teleRuntimeBaselineCodeRegion(), this);
        vm().addressSpace().add(dynamicBaselineCodeCacheRegion.memoryRegion());
        dynamicOptCodeCacheRegion = new VmUnmanagedCodeCacheRegion(vm(), teleCodeManager.teleRuntimeOptCodeRegion(), this);
        vm().addressSpace().add(dynamicOptCodeCacheRegion.memoryRegion());

        vmCodeCacheRegions = Arrays.asList(bootCodeCacheRegion, dynamicBaselineCodeCacheRegion, dynamicOptCodeCacheRegion);
        maxCodeCacheRegions = Collections.unmodifiableList(new ArrayList<MaxCodeCacheRegion>(vmCodeCacheRegions));

        for (VmCodeCacheRegion codeCacheRegion : vmCodeCacheRegions) {
            allocations.add(codeCacheRegion.memoryRegion());
        }

        // The VM's CodePointer class will be used to detect tagged code pointers in VM memory
        // This initialization assumes that the start of the boot code cache is the lowest address in
        // VM memory that can hold code, since that is used as the base in the VM for these pointers.
        CodePointer.initialize(bootCodeCacheRegion.memoryRegion().start());

        lastUpdateEpoch = epoch;
        tracer.end(null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Refresh only enough state to understand the general status of the code cache, for example whether new code cache
     * regions have been created and whether eviction is underway.  This all gets done without necessarily doing any
     * other cache updates, which happen later in the update cycle.
     * <p>
     * This must be done before heap objects are refreshed, in particular so that the code cache region status can be
     * used to determine refresh behavior in instances of {@link TargetMethod}.
     */
    public void updateMemoryStatus(long epoch) {
        updateTracer.begin();
        for (VmCodeCacheRegion region : vmCodeCacheRegions) {
            region.updateStatus(epoch);
        }
        updateTracer.end();
    }

    public String entityName() {
        return entityName;
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxCodeCache> memoryRegion() {
        // The code cache has no memory allocation of its own, but
        // rather owns one or more code regions that have memory
        // allocated from the OS.
        return null;
    }

    public boolean contains(Address address) {
        return findCodeCacheRegion(address) != null;
    }

    public TeleObject representation() {
        return teleCodeManager;
    }

    public VmCodeCacheRegion bootCodeRegion() {
        return bootCodeCacheRegion;
    }

    public List<MaxCodeCacheRegion> codeCacheRegions() {
        return maxCodeCacheRegions;
    }

    public VmCodeCacheRegion findCodeCacheRegion(Address address) {
        for (VmCodeCacheRegion codeCacheRegion : vmCodeCacheRegions) {
            if (codeCacheRegion.memoryRegion().contains(address)) {
                return codeCacheRegion;
            }
        }
        return null;
    }

    public List<MaxEntityMemoryRegion<? extends MaxEntity> > memoryAllocations() {
        return allocations;
    }

    public void printSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        final NumberFormat formatter = NumberFormat.getInstance();
        long totalSize = 0;
        for (VmCodeCacheRegion codeCacheRegion : vmCodeCacheRegions) {
            totalSize += codeCacheRegion.memoryRegion().nBytes();
        }
        printStream.println(indentation + "Total size: " + formatter.format(totalSize) + " bytes");
        printStream.print(indentation + "By region: \n");
        for (VmCodeCacheRegion codeCacheRegion : vmCodeCacheRegions) {
            codeCacheRegion.printSessionStats(printStream, indent + 4, verbose);
        }
    }

    /**
     * Gets the name used by the VM to identify the distinguished
     * boot code region, determined by static inspection of the field
     * that holds the value in the VM.
     *
     * @return the name assigned to the VM's boot code memory region
     * @see Code
     */
    public String bootCodeRegionName() {
        return bootCodeCacheRegionName;
    }

    public List<VmCodeCacheRegion> vmCodeCacheRegions() {
        return vmCodeCacheRegions;
    }

    /**
     * @return whether eviction is underway in any part of the code cache
     */
    public boolean isInEviction() {
        for (VmCodeCacheRegion codeCacheRegion : vmCodeCacheRegions) {
            if (codeCacheRegion.isInEviction()) {
                return true;
            }
        }
        return false;
    }

}
