/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.tele;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.type.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.reference.*;


/**
 * Singleton class that caches information about the heap in the {@link TeleVM}.
 *
 * Initialization between this manager and {@link TeleClassRegistry} are mutually
 * dependent.  The cycle is broken by creating this manager in a partially initialized
 * state that only considers the boot heap region; the manager is only made fully functional
 * with a call to {@link #initialize()}, which requires that {@link TeleClassRegistry} be
 * fully initialized.
 *
 * @author Michael Van De Vanter
 *
 */
public final class TeleHeapManager extends AbstractTeleVMHolder {

    private static final int TRACE_VALUE = 2;

    private static TeleHeapManager teleHeapManager;

    public static TeleHeapManager make(TeleVM teleVM) {
        if (teleHeapManager ==  null) {
            teleHeapManager = new TeleHeapManager(teleVM);
        }
        return teleHeapManager;
    }

    private TeleRuntimeMemoryRegion teleBootHeapRegion = null;

    /**
     * Surrogates for each of the heap regions created by GC implementations in the {@link TeleVM}.
     */
    private TeleRuntimeMemoryRegion[] teleHeapRegions = new TeleRuntimeMemoryRegion[0];

    private TeleHeapManager(TeleVM teleVM) {
        super(teleVM);
    }

    /**
     * This class must function before being fully initialized in order to avoid an initialization
     * cycle with {@link TeleClassRegistry}; each depends on the other for full initialization.
     *
     * When not yet initialized, this manager assumes that there is a boot heap region but no
     * dynamically allocated regions.
     *
     * @return whether this manager has been fully initialized.
     *
     */
    private boolean isInitialized() {
        return teleBootHeapRegion != null;
    }

    /**
     * Lazy initialization; try to keep data reading out of constructor.
     */
    public void initialize(long processEpoch) {
        Trace.begin(1, tracePrefix() + "initializing");
        final long startTimeMillis = System.currentTimeMillis();
        final Reference bootHeapRegionReference = teleVM().fields().Heap_bootHeapRegion.readReference(teleVM());
        teleBootHeapRegion = (TeleRuntimeMemoryRegion) teleVM().makeTeleObject(bootHeapRegionReference);
        refreshMemoryRegions(processEpoch);
        Trace.end(1, tracePrefix() + "initializing", startTimeMillis);
    }

    private boolean updatingHeapMemoryRegions = false;

    /**
     * Updates local cache of information about dynamically allocated heap regions in the {@link TeleVM}.
     * During this update, any method calls to check heap containment are handled specially.
     */
    public void refreshMemoryRegions(long processEpoch) {
        if (isInitialized()) {
            Trace.begin(TRACE_VALUE, tracePrefix() + "refreshing");
            final long startTimeMillis = System.currentTimeMillis();
            updatingHeapMemoryRegions = true;
            final Reference runtimeHeapRegionsArrayReference = teleVM().fields().InspectableHeapInfo_memoryRegions.readReference(teleVM());
            if (!runtimeHeapRegionsArrayReference.isZero()) {
                final TeleArrayObject teleArrayObject = (TeleArrayObject) teleVM().makeTeleObject(runtimeHeapRegionsArrayReference);
                final Reference[] heapRegionReferences = (Reference[]) teleArrayObject.shallowCopy();
                if (teleHeapRegions.length != heapRegionReferences.length) {
                    teleHeapRegions = new TeleRuntimeMemoryRegion[heapRegionReferences.length];
                }
                int next = 0;
                for (int i = 0; i < heapRegionReferences.length; i++) {
                    final TeleRuntimeMemoryRegion region = (TeleRuntimeMemoryRegion) teleVM().makeTeleObject(heapRegionReferences[i]);
                    if (region != null) {
                        teleHeapRegions[next++] = region;
                    } else {
                        // This can happen when inspecting VM startup
                    }
                }
                // Remove any null memory regions from the list.
                if (next != teleHeapRegions.length) {
                    teleHeapRegions = Arrays.copyOf(teleHeapRegions, next);
                }
            }
            updatingHeapMemoryRegions = false;
            Trace.end(TRACE_VALUE, tracePrefix() + "refreshing", startTimeMillis);
        }
    }

    /**
     * @return surrogate for the special heap {@link RuntimeMemoryRegion} in the {@link BootImage} of the {@link TeleVM}.
     */
    public TeleRuntimeMemoryRegion teleBootHeapRegion() {
        return teleBootHeapRegion;
    }

    /**
     * @return surrogates for all {@link RuntimeMemoryRegion}s in the {@link Heap} of the {@link TeleVM}.
     * Sorted in order of allocation.  Does not include the boot heap region.
     */
    public IndexedSequence<TeleRuntimeMemoryRegion> teleHeapRegions() {
        return new ArraySequence<TeleRuntimeMemoryRegion>(teleHeapRegions);
    }

    /**
     * @return the allocated heap {@link RuntimeMemoryRegion} in the {@link TeleVM} that contains the address,
     * possibly the boot heap; null if none.
     */
    public TeleRuntimeMemoryRegion regionContaining(Address address) {
        if (teleBootHeapRegion.contains(address)) {
            return teleBootHeapRegion;
        }
        for (TeleRuntimeMemoryRegion teleHeapRegion : teleHeapRegions) {
            if (teleHeapRegion.contains(address)) {
                return teleHeapRegion;
            }
        }
        return null;
    }

    /**
     * @return whether any of the heap regions in the {@link TeleVM} contain the address; always returns true
     * in the context of a call in progress to {@link #refresh()}, in order to avoid a circularity.
     */
    public boolean contains(Address address) {
        if (!isInitialized()) {
            // Assume that there is only a boot heap region allocated, and  get the information
            // using only lower level mechanisms to avoid an initialization loop with {@link TeleClassRegistry}.
            // In particular, avoid any call to {@link TeleObject#make()}, which depends on {@link TeleClassRegistry}.
            final Pointer bootHeapStart = teleVM().bootImageStart();
            final Address bootHeapEnd = bootHeapStart.plus(teleVM().bootImage().header().bootHeapSize);
            return bootHeapStart.lessEqual(address) && address.lessThan(bootHeapEnd);
        }
        if (updatingHeapMemoryRegions) {
            // The call is nested within a call to {@link #refresh}, assume all is well.
            return true;
        }
        return regionContaining(address) != null;
    }

    /**
     * @return whether an of the dynamically allocated heap regions in the {@link TeleVM} contain the address; handle
     * specially in the context of a call in progress to {@link #refresh()}, in order to avoid a circularity.
     */
    public boolean dynamicHeapContains(Address address) {
        if (!isInitialized()) {
            // When not yet initialized with information about the dynamic heap, assume it doesn't exist yet.
            return false;
        }
        if (updatingHeapMemoryRegions) {
            // The call is nested within a call to {@link #refresh}; exclude
            // the case where it is in the boot region, otherwise assume all is well.
            return !teleBootHeapRegion.contains(address);
        }
        for (TeleRuntimeMemoryRegion teleHeapRegion : teleHeapRegions) {
            if (teleHeapRegion.contains(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads the collection table count; incremented each time a GC begins.
     * @return one greater than {@link #readRootEpoch()} during GC, otherwise equal
     */
    public long readCollectionEpoch() {
        return teleVM().fields().InspectableHeapInfo_collectionEpoch.readLong(teleVM());
    }

    /**
     * Reads the root table update count; incremented each time a GC completes.
     * @return number of times inspectable root table updated
     */
    public long readRootEpoch() {
        return teleVM().fields().InspectableHeapInfo_rootEpoch.readLong(teleVM());
    }

    /**
     * @ return is the VM in GC
     */
    public boolean isInGC() {
        return readCollectionEpoch() != readRootEpoch();
    }

    /**
     * Address of the field incremented each time a GC begins.
     * @return memory location of the field holding the collection epoch
     * @see #readCollectionEpoch()
     */
    public Address collectionEpochAddress() {
        final int offset = teleVM().fields().InspectableHeapInfo_collectionEpoch.fieldActor().offset();
        return teleVM().fields().InspectableHeapInfo_collectionEpoch.staticTupleReference(teleVM()).toOrigin().plus(offset);
    }

    /**
     * Address of the field incremented each time a GC completes.
     * @return memory location of the field holding the root epoch
     * @see #readRootEpoch()
     */
    public Address rootEpochAddress() {
        final int offset = teleVM().fields().InspectableHeapInfo_rootEpoch.fieldActor().offset();
        return teleVM().fields().InspectableHeapInfo_rootEpoch.staticTupleReference(teleVM()).toOrigin().plus(offset);
    }

}
