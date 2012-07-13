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
import java.lang.ref.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;


/**
 * Access to the information in a dynamically allocated region of
 * code cache that is managed by SemiSpace collection.
 *
 * @see SemiSpaceCodeRegion
 * @see TeleSemiSpaceCodeRegion
 * @see VmCodeCacheAccess
 */
public final class VmSemiSpaceCodeCacheRegion extends VmCodeCacheRegion {

    private static final int TRACE_VALUE = 1;

    private final TimedTrace updateTracer;

    private final String entityDescription;

    /**
     * The object in the VM that describes this code region.
     */
    private final TeleSemiSpaceCodeRegion teleSemiSpaceCodeRegion;

    private final List<MaxObject> inspectableObjects;

    /**
     * Local manager of code regions.
     */
    private final VmCodeCacheAccess codeCache;

    private final RemoteCodePointerManager codePointerManager;

    // TODO (mlvdv)  check for and handle out-of-order additions in CodeManager.add()
    /**
     * A mirror of the array of {@link TargetMethod}s that represents the method compilations managed
     * in the VM code region represented by this code cache region.  We keep a linear mirror because
     * we assume that most additions to the array in {@link CodeRegion} are appended, making update
     * here easier.
     *
     * @see {@link CodeRegion}
     */
    private final List<MaxCompilation> compilations = new ArrayList<MaxCompilation>();

    /**
     * Known method compilations in the region, organized for efficient lookup by address.
     * Map:  Address --> TeleCompilation
     */
    private final AddressToCompilationMap addressToCompilationMap;

    /**
     * Known method compilations organized for lookup by {@link RemoteReference} to the {@link TargetMethod} in the VM; this
     * map doesn't get cleared in the case of eviction.  It serves as memory of compilations that have been seen
     * in this region, making it easier to track them when they have been relocated.
     * <p>
     * Map: Reference to VM {@link TargetMethod} --> the {@link TeleCompilation} representation of that method compilation.
     */
    private final Map<RemoteReference, WeakReference<TeleCompilation>> referenceToCompilationMap = new HashMap<RemoteReference, WeakReference<TeleCompilation>>();

    private long lastEvictionCompletedCount = 0;

    private final Object localStatsPrinter = new Object() {

        private int previousCompilationCount = 0;
        private long previousEvictionCount = 0;

        @Override
        public String toString() {
            final StringBuilder msg = new StringBuilder();
            final int compilationCount = compilations.size();
            final int newCompilationCount =  compilationCount - previousCompilationCount;
            if (lastEvictionCompletedCount > previousEvictionCount) {
                // We've just cleaned up after code eviction with this update
                msg.append("post-eviction #compilations=(");
                msg.append("before=").append(previousCompilationCount);
                msg.append(",survivors/new=").append(compilationCount).append(")");
                previousEvictionCount = lastEvictionCompletedCount;
            } else {
                msg.append("#compilations=(").append(compilationCount);
                msg.append(",new=").append(newCompilationCount).append(")");
            }
            previousCompilationCount = compilationCount;
            return msg.toString();
        }
    };

    private final RemoteObjectReferenceManager objectReferenceManager;

    /**
     * Creates an object that models a dynamically allocated region
     * in the VM's code cache that is managed by SemiSpace collection.
     *
     * @param vm the VM
     * @param teleSemiSpaceCodeRegion the VM object that describes the memory allocated
     * @param codeCache the manager for code cache regions
     */
    public VmSemiSpaceCodeCacheRegion(TeleVM vm, TeleSemiSpaceCodeRegion teleSemiSpaceCodeRegion, VmCodeCacheAccess codeCache) {
        super(vm, teleSemiSpaceCodeRegion);
        this.teleSemiSpaceCodeRegion = teleSemiSpaceCodeRegion;
        this.inspectableObjects = new ArrayList<MaxObject>();
        this.inspectableObjects.add(teleSemiSpaceCodeRegion);
        this.codeCache = codeCache;
        this.entityDescription = "The managed allocation area " + teleSemiSpaceCodeRegion.getRegionName() + " owned by the VM code cache";
        this.addressToCompilationMap = new AddressToCompilationMap(vm);
        this.objectReferenceManager = new SemispaceCodeCacheRemoteReferenceManager(vm, this);
        this.codePointerManager = new SemispaceCodeCacheRemoteCodePointerManager(vm, this);
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + "updating name=" + teleSemiSpaceCodeRegion.getRegionName());
        Trace.line(TRACE_VALUE, tracePrefix() + "code cache region created for " + teleSemiSpaceCodeRegion.getRegionName() + " with " + objectReferenceManager.getClass().getSimpleName());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Proactively attempt to discover every method compilation in the region upon
     * refresh.  It is slightly possible, however, that a {@link TeleTargetMethod}
     * might be created in some other way, before we locate it here.  For that reason,
     * new ones are registered by a call from the constructor for {@link TeleTargetMethod}.
     */
    public void updateCache(long epoch) {
        updateTracer.begin();
        // Ensure that we know the code region's current state by refreshing the object cache
        teleSemiSpaceCodeRegion.updateCacheIfNeeded();
        if (teleSemiSpaceCodeRegion.isAllocated()) {
            if (teleSemiSpaceCodeRegion.isInEviction()) {
                // Code eviction is currently underway in this region.
                // TODO (mlvdv) review what, if anything, should be done here
            } else {
                // Not currently in eviction
                if (teleSemiSpaceCodeRegion.evictionCount() > lastEvictionCompletedCount) {
                    // There has been an eviction since the last time we updated; clear out the old summary information.
                    compilations.clear();
                    addressToCompilationMap.clear();
                    lastEvictionCompletedCount = teleSemiSpaceCodeRegion.evictionCount();
                }
                // Check for compilations not yet summarized; this will comprise all compilations
                // present in the region if there has been an eviction since the last update.
                final int targetMethodCount = teleSemiSpaceCodeRegion.nTargetMethods();
                int index = compilations.size();
                while (index < targetMethodCount) {
                    RemoteReference targetMethodReference = teleSemiSpaceCodeRegion.getTargetMethodReference(index++);
                    // Have we seen this compilation before, independent of its location in the region?
                    TeleCompilation compilation = getCompilation(targetMethodReference);
                    if (compilation == null) {
                        // This element in the target method array refers to a compilation not yet seen.
                        TeleTargetMethod teleTargetMethod = (TeleTargetMethod) objects().makeTeleObject(targetMethodReference);
                        if (teleTargetMethod == null) {
                            vm().invalidReferencesLogger().record(targetMethodReference, TeleTargetMethod.class);
                            System.out.println("null");
                            continue;
                        }
                        // Creating a new {@link TeleTargetMethod} causes it to be registered by
                        // a call to register().
                        // It should now be in the map.
                        compilation = getCompilation(targetMethodReference);
                    } else {
                        // This is a reference to a previously seen compilation.
                        // This compilation has likely been relocated by eviction.
                        // Ensure that we know its new location before adding it to the map.
                        compilation.teleTargetMethod().updateCache(epoch);
                    }
                    compilations.add(compilation);
                    addressToCompilationMap.add(compilation);
                }
            }
        }
        updateTracer.end(localStatsPrinter);
    }

    public String entityDescription() {
        return entityDescription;
    }

    public int compilationCount() {
        return compilations.size();
    }

    public List<MaxCompilation> compilations() {
        return Collections.unmodifiableList(compilations);
    }

    @Override
    public int loadedCompilationCount() {
        int count = 0;
        for (MaxCompilation maxCompilation : compilations) {
            final TeleCompilation teleCompilation = (TeleCompilation) maxCompilation;
            if (teleCompilation == null) {
                System.out.println("null");
            }
            if (teleCompilation.teleTargetMethod() == null) {
                System.out.println(teleCompilation.toString() + "targetmethod=null");
            }
            if (teleCompilation.teleTargetMethod().isCacheLoaded()) {
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
        final TeleCompilation compilation = new TeleCompilation(vm(), teleTargetMethod, codeCache);
        referenceToCompilationMap.put(teleTargetMethod.reference(), new WeakReference<TeleCompilation>(compilation));
        addressToCompilationMap.add(compilation);
    }

    @Override
    public TeleCompilation findCompilation(Address address) {
        return addressToCompilationMap.find(address);
    }

    @Override
    public void writeSummary(PrintStream printStream) {
        addressToCompilationMap.writeSummary(printStream);
    }

    /**
     * Finds an existing instance of {@link TeleCompilation} that wraps a specific instance of
     * {@link TeleTargetMethod}, corresponding to a method compilation in the VM.
     *
     * @param teleTargetMethod local surrogate for a {@link TargetMethod} in the VM.
     * @return the existing instance of {@link TeleCompilation} that wraps the target method, null if none.
     */
    private TeleCompilation getCompilation(RemoteReference reference) {
        final WeakReference<TeleCompilation> weakReference = referenceToCompilationMap.get(reference);
        return (weakReference != null) ? weakReference.get() : null;
    }

    public RemoteObjectReferenceManager objectReferenceManager() {
        return objectReferenceManager;
    }

    public RemoteCodePointerManager codePointerManager() {
        return codePointerManager;
    }

    @Override
    public List<MaxObject> inspectableObjects() {
        return inspectableObjects;
    }

}
