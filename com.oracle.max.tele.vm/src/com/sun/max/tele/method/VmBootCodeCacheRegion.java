/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.reference.*;


/**
 * Access to the information in the code cache region that
 * is part of the boot image.
 *
 * @see CodeRegion
 * @see VmCodeCacheAccess
 * @see TargetMethod
 */
public final class VmBootCodeCacheRegion extends VmCodeCacheRegion {

    private static final int TRACE_VALUE = 1;

    private final TimedTrace updateTracer;

    private final String entityDescription;

    /**
     * The object in the VM that describes this code region.
     */
    private final TeleCodeRegion teleCodeRegion;

    /**
     * Local manager of code regions.
     */
    private final VmCodeCacheAccess codeCache;

    private final RemoteCodePointerManager codePointerManager;

    /**
     * Known method compilations in the region, organized for efficient lookup by address.
     * Map:  Address --> TeleCompilation
     */
    private final AddressToCompilationMap addressToCompilationMap;

    private final List<MaxCompilation> compilations = new ArrayList<MaxCompilation>();

    private final List<TeleTargetMethod> teleTargetMethods = new ArrayList<TeleTargetMethod>();

    private final RemoteObjectReferenceManager remoteObjectReferenceManager;

    private final UnmanagedCodeCacheRegionStatsPrinter localStatsPrinter;

    /**
     * Creates an object that models the region of the VM's code cache that is in the boot image.
     * @param vm the VM
     * @param teleCodeRegion the VM object that describes the memory allocated
     * @param codeCache the manager for code cache regions
     */
    public VmBootCodeCacheRegion(TeleVM vm, TeleCodeRegion teleCodeRegion, VmCodeCacheAccess codeCache) {
        super(vm, teleCodeRegion);
        this.teleCodeRegion = teleCodeRegion;
        this.codeCache = codeCache;
        this.entityDescription = "The allocation area for pre-compiled methods in the " + vm.entityName() + " boot image";
        this.addressToCompilationMap = new AddressToCompilationMap(vm);
        this.remoteObjectReferenceManager = new UnmanagedCodeCacheRemoteReferenceManager(vm, this);
        this.codePointerManager = new UnmanagedRemoteCodePointerManager(vm, this);
        this.localStatsPrinter = new UnmanagedCodeCacheRegionStatsPrinter();
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + "updating name=" + teleCodeRegion.getRegionName());
        Trace.line(TRACE_VALUE, tracePrefix() + "code cache region created for " + teleCodeRegion.getRegionName() + " with " + remoteObjectReferenceManager.getClass().getSimpleName());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Proactively attempt to discover every method compilation in the region upon
     * refresh.  It is slightly possible, however, that a {@link TeleTargetMethod}
     * might be created in some other way, before we locate it here.  For that reason,
     * new ones are registered by a call from the constructor for {@link TeleTargetMethod}.
     * <p>
     * This implementation exploits the representation of {@link TargetMethod}s held in
     * a VM instance of {@link CodeRegion} as an array of {@link TargetMethod}
     * references.  In theory, the contents of the boot code cache should not grow, but
     * we check anyway by checking the compilation count.
     */
    public void updateCache(long epoch) {
        updateTracer.begin();
        // Ensure any updating of the object's state is done.
        teleCodeRegion.updateCache(epoch);
        if (teleCodeRegion.isAllocated()) {
            final int vmTargetMethodCount = teleCodeRegion.nTargetMethods();
            int registeredTargetMethodCount = teleTargetMethods.size();
            while (registeredTargetMethodCount < vmTargetMethodCount) {
                Reference targetMethodReference = teleCodeRegion.getTargetMethodReference(registeredTargetMethodCount++);
                // Creating a {@link TeleTargetMethod} causes it to be added to the code registry
                TeleTargetMethod teleTargetMethod = (TeleTargetMethod) objects().makeTeleObject(targetMethodReference);
                if (teleTargetMethod == null) {
                    vm().invalidReferencesLogger().record(targetMethodReference, TeleTargetMethod.class);
                    continue;
                }
                teleTargetMethods.add(teleTargetMethod);
            }
        }
        updateTracer.end(localStatsPrinter);
    }

    public String entityDescription() {
        return entityDescription;
    }

    public int compilationCount() {
        return teleTargetMethods.size();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Assume that compilations in the boot code cache are allocated linearly, and that they are never relocated or evicted.
     */
    public List<MaxCompilation> compilations() {
        TeleError.check(compilations.size() <= teleTargetMethods.size(), "Compilations in boot code cache appear to have disappeared");
        if (compilations.size() < teleTargetMethods.size()) {
            for (int index = compilations.size(); index < teleTargetMethods.size(); index++) {
                compilations.add(find(teleTargetMethods.get(index).getRegionStart()));
            }
        }
        return Collections.unmodifiableList(compilations);
    }

    @Override
    public int loadedCompilationCount() {
        int count = 0;
        for (TeleTargetMethod teleTargetMethod : teleTargetMethods) {
            if (teleTargetMethod.isLoaded()) {
                count++;
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method only gets called when a new instance of {@link TeleTargetMethod} gets created,
     * so assume that there has not yet been a {@link TeleCompilation} created for it.
     */
    @Override
    public void register(TeleTargetMethod teleTargetMethod) {
        TeleError.check(contains(teleTargetMethod.getRegionStart()), "Attempt to register TargetMethod in the wrong region");
        addressToCompilationMap.add(new TeleCompilation(vm(), teleTargetMethod, codeCache, true));
    }

    @Override
    public TeleCompilation find(Address address) {
        return addressToCompilationMap.find(address);
    }


    @Override
    public void writeSummary(PrintStream printStream) {
        addressToCompilationMap.writeSummary(printStream);
    }

    public RemoteObjectReferenceManager objectReferenceManager() {
        return remoteObjectReferenceManager;
    }

    public RemoteCodePointerManager codePointerManager() {
        return codePointerManager;
    }
}
