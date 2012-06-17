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

import static com.sun.max.vm.heap.HeapPhase.*;

import java.io.*;
import java.lang.management.*;
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.TeleVM.InitializationListener;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.reference.semispace.*;
import com.sun.max.tele.reference.semispace.SemiSpaceRemoteReference.RefStateCount;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.semiSpace.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;


/**
 * Inspector support for working with VM sessions using the VM's simple
 * {@linkplain SemiSpaceHeapScheme semispace collector},
 * an implementation of the VM's {@link HeapScheme} interface.
 * <p>
 * <strong>Comments are out of date</strong>
 * <p>
 * The operation of the semispace collector in the VM is modeled using the
 * following techniques and invariants.
 * <p>
 * For each of the two regions, From-Space and To-Space, a reference map is maintained with the following structure:
 * <ul>
 * <li><strong>Key:</strong> an address (in the specific heap space of VM memory)
 * that has been discovered to be the location of an object origin</li>
 * <li><strong>Value:</strong> a weak reference to instance of {@link RemoteReference}
 * that has been created by this manager and whose origin is that address.</li>
 * </ul>
 * <p>
 * References in the maps in general are either {@link #LIVE} or {@link #UNKNOWN}, but the latter
 * is possible only when the heap phase is {@link #ANALYZING}.
 * References are removed from the maps as soon as they are discovered to be {@link #DEAD}.
 * Any reference whose origin is in the regions but which is not in the maps is {@link #DEAD}.
 * <p>
 * The two regions are allocated linearly; the well-defined area that is currently allocated in each
 * region is defined as the "live area"; no references in the maps point outside a live area.
 * <p>
 * When the heap phase is either {@link #MUTATING} or {@link #RECLAIMING}:
 * <ul>
 * <li>For every reference in the To-Space map:
 * <ul>
 * <li> {@link RemoteReference#origin()} is the object's origin in To-Space</li>
 * <li> {@link RemoteReference#status()} is {@link #LIVE} </li>
 * <li> {@link RemoteReference#isForwarded()} is {@code false}</li>
 * <li> {@link RemoteReference#forwardedFrom()} is {@link Address#zero()}</li>
 * </ul></li>
 * <li>The From-Space map is empty.</li>
 * </ul>
 * <p>
 * When the heap phase is {@link #ANALYZING}:
 * <ul>
 * <li>For every reference in the To-Space map:
 * <ul>
 * <li> {@link RemoteReference#origin()} is the origin in To-Space of the new
 * copy of a forwarded object</li>
 * <li> {@link RemoteReference#status()} is {@link #LIVE}</li>
 * <li> {@link RemoteReference#isForwarded()} is {@code true}</li>
 * <li> {@link RemoteReference#forwardedFrom()} is either:
 * <ul>
 * <li> the origin of the old copy in From-Space, in which case this reference
 * is <em>also</em> present in the From-Space map, keyed by the old origin in From-Space</li>
 * <li> {@link Address#zero()} if the origin of the old copy in From-Space has not been discovered,
 * in which case this reference is <em>not</em> present in the From-Space map</li>
 * </ul></li>
 * </ul></li>
 * <li>For every reference in the From-Space map, {@link RemoteReference#status()} is either:
 * <ul>
 * <li> {@link #UNKNOWN} meaning that the collector has not yet determined whether the object
 * in From-Space is reachable, in which case:
 * <ul>
 * <li> {@link RemoteReference#origin()} is the object's origin in From-Space</li>
 * <li> {@link RemoteReference#isForwarded()} is {@code false}</li>
 * <li> {@link RemoteReference#forwardedFrom()} is {@link Address#zero()}</li>
 * <li> this reference is <em>not</em> present in the To-Space map
 * </ul></li>
 * <li> {@link #LIVE} meaning that the collector has created a copy of the object in To-Space and
 * installed a <em>forwarding pointer</em>, in which case:
 * <ul>
 * <li> {@link RemoteReference#origin()} is the object's origin in To-Space</li>
 * <li> {@link RemoteReference#isForwarded()} is {@code true}</li>
 * <li> {@link RemoteReference#forwardedFrom()} is the object's origin in From-Space</li>
 * <li> this reference is in the Old-Space map, keyed by the object's origin in From-Space</li>
 * <li> this reference is <em>also</em> present in the To-Space map, keyed by the origin of the new copy</li>
 * </ul></li>
 * </ul></li>
 * </ul>
 *
 * @see SemiSpaceHeapScheme
 */
public class RemoteSemiSpaceHeapScheme extends AbstractRemoteHeapScheme implements RemoteObjectReferenceManager {

    private static final int TRACE_VALUE = 1;

    private final TimedTrace heapUpdateTracer;

    private long lastUpdateEpoch = -1L;
    private long lastAnalyzingPhaseCount = 0L;
    private long lastReclaimingPhaseCount = 0L;

    /**
     * The VM object that implements the {@link HeapScheme} in the current configuration.
     */
    private TeleSemiSpaceHeapScheme scheme;

    /**
     * The VM object that describes the location of the collector's To-Space; the location changes when the spaces get swapped.
     */
    private TeleLinearAllocationMemoryRegion toSpaceMemoryRegion = null;

    /**
     * Map:  VM address in To-Space --> a {@link SemiSpaceRemoteReference} that refers to the object whose origin is at that location.
     * There may be only live references in the map.
     */
    private WeakRemoteReferenceMap<SemiSpaceRemoteReference> toSpaceRefMap = new WeakRemoteReferenceMap<SemiSpaceRemoteReference>();

    /**
     * The VM object that describes the location of the collector's From-Space; the location changes when the spaces get swapped.
     */
    private TeleLinearAllocationMemoryRegion fromSpaceMemoryRegion = null;

    /**
     * Map:  VM address in From-Space --> a {@link SemiSpaceRemoteReference} that refers to the object whose origin is at that location.
     * There may be live or quasi references in the map, but never dead.
     */
    private WeakRemoteReferenceMap<SemiSpaceRemoteReference> fromSpaceRefMap = new WeakRemoteReferenceMap<SemiSpaceRemoteReference>();

    private long collected = 0;
    private long forwarded = 0;

    private final ReferenceUpdateTracer referenceUpdateTracer;

    private final List<VmHeapRegion> heapRegions = new ArrayList<VmHeapRegion>(2);

    protected RemoteSemiSpaceHeapScheme(TeleVM vm) {
        super(vm);
        this.heapUpdateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + "updating");
        this.referenceUpdateTracer = new ReferenceUpdateTracer();

        final VmAddressSpace addressSpace = vm().addressSpace();
        // There might already be dynamically allocated regions in a dumped image or when attaching to a running VM
        // TODO (mlvdv)  Update; won't work now; important for attach mode
        for (MaxMemoryRegion dynamicHeapRegion : getDynamicHeapRegionsUnsafe()) {
            final VmHeapRegion fakeDynamicHeapRegion =
                new VmHeapRegion(vm, dynamicHeapRegion.regionName(), dynamicHeapRegion.start(), dynamicHeapRegion.nBytes());
            heapRegions.add(fakeDynamicHeapRegion);
            addressSpace.add(fakeDynamicHeapRegion.memoryRegion());
        }
    }

    public Class heapSchemeClass() {
        return SemiSpaceHeapScheme.class;
    }

    public void initialize(long epoch) {

        vm().addInitializationListener(new InitializationListener() {

            public void initialiationComplete(final long initializationEpoch) {
                objects().registerTeleObjectType(SemiSpaceHeapScheme.class, TeleSemiSpaceHeapScheme.class);
                // Get the VM object that represents the heap implementation; can't do this any sooner during startup.
                scheme = (TeleSemiSpaceHeapScheme) teleHeapScheme();
                assert scheme != null;
                updateMemoryStatus(initializationEpoch);
                /*
                 * Add a heap phase listener that will  force the VM to stop any time the heap transitions from
                 * analysis to the reclaiming phase of a GC. This is exactly the moment in a GC cycle when reference
                 * information must be updated. Unfortunately, the handler supplied with this listener will only be
                 * called after the VM state refresh cycle is complete. That would be too late since so many other parts
                 * of the refresh cycle depend on references. Consequently, the needed updates take place when this
                 * manager gets refreshed (early in the refresh cycle), not when this handler eventually gets called.
                 */
                try {
                    vm().addGCPhaseListener(RECLAIMING, new MaxGCPhaseListener() {

                        public void gcPhaseChange(HeapPhase phase) {
                            // Dummy handler; the actual updates must be done early during the refresh cycle.
                            final long phaseChangeEpoch = vm().teleProcess().epoch();
                            Trace.line(TRACE_VALUE, tracePrefix() + " VM stopped for reference updates, epoch=" + phaseChangeEpoch + ", gc cycle=" + gcStartedCount());
                            Trace.line(TRACE_VALUE, tracePrefix() + " Note: updates have long since been done by the time this (dummy) handler is called");
                        }
                    });
                } catch (MaxVMBusyException e) {
                    TeleError.unexpected(tracePrefix() + "Unable to add GC Phase Listener");
                }
            }
        });
    }

    public List<VmHeapRegion> heapRegions() {
        return heapRegions;
    }

    /**
     * {@inheritDoc}
     * <p>
     * It is unnecessary to check for heap regions that disappear for this collector.
     * Once allocated, the two regions never change, but we force a refresh on the
     * memory region descriptions, since their locations change when swapped.
     * <p>
     * This gets called more than once during the startup sequence.
     */
    @Override
    public void updateMemoryStatus(long epoch) {

        super.updateMemoryStatus(epoch);
        // Can't do anything until we have the VM object that represents the scheme implementation
        if (scheme == null) {
            return;
        }
        if (heapRegions.size() < 2) {
            Trace.begin(TRACE_VALUE, tracePrefix() + "looking for heap regions");
            /*
             * The two heap regions have not yet been discovered. Don't check the epoch, since this check may
             * need to be run more than once during the startup sequence, as the information needed for access to
             * the information is incrementally established.  Information about the two heap regions, other then
             * their locations (subject to change when swapped) need not be checked once established.
             */
            if (toSpaceMemoryRegion == null) {
                toSpaceMemoryRegion = scheme.readTeleToRegion();
                if (toSpaceMemoryRegion != null) {
                    final VmHeapRegion toVmHeapRegion = new VmHeapRegion(vm(), toSpaceMemoryRegion, this);
                    heapRegions.add(toVmHeapRegion);
                    vm().addressSpace().add(toVmHeapRegion.memoryRegion());
                }
            }
            if (fromSpaceMemoryRegion == null) {
                fromSpaceMemoryRegion = scheme.readTeleFromRegion();
                if (fromSpaceMemoryRegion != null) {
                    final VmHeapRegion fromVmHeapRegion = new VmHeapRegion(vm(), fromSpaceMemoryRegion, this);
                    heapRegions.add(fromVmHeapRegion);
                    vm().addressSpace().add(fromVmHeapRegion.memoryRegion());
                }
            }
            Trace.end(TRACE_VALUE, tracePrefix() + "looking for heap regions, " + heapRegions.size() + " found");
            if (heapRegions.size() < 2) {
                // don't bother with the rest.
                return;
            }
        }
        if (epoch > lastUpdateEpoch) {

            heapUpdateTracer.begin();

            /*
             * This is a normal refresh. Immediately update information about the location of the heap regions; this
             * update must be forced because remote objects are otherwise not refreshed until later in the update
             * cycle.
             */
            toSpaceMemoryRegion.updateCache(epoch);
            fromSpaceMemoryRegion.updateCache(epoch);


            /*
             * For this collector, we only need an overall review of reference state when we're actually collecting.
             */
            if (phase().isCollecting()) {

                // Has a GC cycle has started since the last time we looked?
                if (lastAnalyzingPhaseCount < gcStartedCount()) {
                    /*
                     * A GC cycle has started since the last time we checked.  That means that the spaces have been swapped by the GC
                     * and we must account for that first, before examining what else has happened since the swap.
                     */
                    Trace.begin(TRACE_VALUE, tracePrefix() + "first halt in GC cycle=" + gcStartedCount() + ", swapping semispace heap regions");
                    assert lastAnalyzingPhaseCount == gcStartedCount() - 1;
                    assert fromSpaceRefMap.isEmpty();
                    // Swap the maps to reflect the swapped locations of the two regions in the heap
                    final WeakRemoteReferenceMap<SemiSpaceRemoteReference> tempRefMap = toSpaceRefMap;
                    toSpaceRefMap = fromSpaceRefMap;
                    fromSpaceRefMap = tempRefMap;

                    // Transition the state of all references that are now in From-Space
                    final List<SemiSpaceRemoteReference> refs = fromSpaceRefMap.values();
                    for (SemiSpaceRemoteReference fromSpaceRef : refs) {
                        // A former To-Space reference that is now in From-Space: transition state
                        fromSpaceRef.beginAnalyzing();
                    }
                    Trace.end(TRACE_VALUE, tracePrefix() + "first halt in GC cycle=" + gcStartedCount() + ", total refs=" + refs.size());
                    lastAnalyzingPhaseCount = gcStartedCount();
                }

                /*
                 * Check now to see if any From-Space references have been forwarded since we last looked.  This can happen
                 * at any time during the analyzing phase, but we have to check one more time when we hit the reclaiming
                 * phase.
                 */
                if (lastReclaimingPhaseCount < gcStartedCount()) {
                    // The transition to reclaiming hasn't yet been processed, so check for any newly forwarded references.
                    Trace.begin(TRACE_VALUE, tracePrefix() + "checking From-Space refs, GC cycle=" + gcStartedCount());
                    int newForwarded = 0;
                    int oldForwarded = 0;
                    for (SemiSpaceRemoteReference fromSpaceRef : fromSpaceRefMap.values()) {
                        switch (fromSpaceRef.status()) {
                            case LIVE:
                                final Address fromSpaceOrigin = fromSpaceRef.origin();
                                if (objects().hasForwardingAddressUnsafe(fromSpaceOrigin)) {
                                    // A known origin in From-Space that has been forwarded since the last time we checked.
                                    // The object is now live in To-Space; transition the existing reference to reflect this.
                                    // Remove the reference from the From-Space map.
                                    fromSpaceRefMap.remove(fromSpaceOrigin);
                                    // Get new address in To-Space
                                    final Address toSpaceOrigin = objects().getForwardingAddressUnsafe(fromSpaceOrigin);
                                    // Relocate the existing reference and add it to the To-Space Map
                                    fromSpaceRef.discoverForwarded(toSpaceOrigin);
                                    toSpaceRefMap.put(toSpaceOrigin, fromSpaceRef);

                                    // Create a forwarder quasi reference to retain information about the old location for the duration of the analysis phase
                                    final SemiSpaceRemoteReference forwarderReference = SemiSpaceRemoteReference.createForwarder(this, fromSpaceOrigin, toSpaceOrigin);
                                    fromSpaceRefMap.put(fromSpaceOrigin, forwarderReference);
                                    newForwarded++;
                                }
                                break;
                            case FORWARDER:
                                // Do nothing; already forwarded and a separate reference is in the To-Space map
                                oldForwarded++;
                                break;
                            case DEAD:
                                TeleError.unexpected(tracePrefix() + "DEAD reference found in From-Space map");
                                break;
                            default:
                                TeleError.unknownCase();
                        }
                    }
                    Trace.end(TRACE_VALUE, tracePrefix() + "checking From-Space refs, GC cycle=" + gcStartedCount()
                                    + " forwarded=" + (oldForwarded + newForwarded) + "(old=" + oldForwarded + ", new=" + newForwarded + ")");
                }

                if (phase().isReclaiming() && lastReclaimingPhaseCount < gcStartedCount()) {
                    /*
                     * The heap is in a GC cycle, and this is the first VM halt during that GC cycle where we know
                     * analysis is complete. This halt will usually be caused by the special breakpoint we've set at
                     * entry to the {@linkplain #RECLAIMING} phase.
                     */
                    Trace.begin(TRACE_VALUE, tracePrefix() + "first halt in GC RECLAIMING, cycle=" + gcStartedCount() + "; clearing From-Space references");
                    assert lastReclaimingPhaseCount == gcStartedCount() - 1;
                    lastReclaimingPhaseCount = gcStartedCount();
                    for (SemiSpaceRemoteReference toSpaceRef : toSpaceRefMap.values()) {
                        switch (toSpaceRef.status()) {
                            case LIVE:
                                toSpaceRef.endAnalyzing();
                                break;
                            default:
                                TeleError.unexpected(tracePrefix() + toSpaceRef.status().name() + " reference found in To-Space map");
                                break;
                        }
                    }
                    int objectsDied = 0;
                    int forwardersDied = 0;
                    for (SemiSpaceRemoteReference fromSpaceRef : fromSpaceRefMap.values()) {
                        switch (fromSpaceRef.status()) {
                            case LIVE:
                                fromSpaceRef.endAnalyzing();
                                objectsDied++;
                                break;
                            case FORWARDER:
                                fromSpaceRef.endAnalyzing();
                                forwardersDied++;
                                break;
                            default:
                                TeleError.unexpected(tracePrefix() + fromSpaceRef.status().name() + " reference found in From-Space map");
                                break;
                        }
                    }
                    fromSpaceRefMap.clear();
                    Trace.end(TRACE_VALUE, tracePrefix() + "first halt in GC RECLAIMING, cycle=" + gcStartedCount() + ", died=(objects=" + objectsDied + ", fowarders=" + forwardersDied + ")");

                }
            }
            lastUpdateEpoch = epoch;
            heapUpdateTracer.end(heapUpdateStatsPrinter);
        }
    }

    public MaxMemoryManagementInfo getMemoryManagementInfo(final Address address) {
        return new MaxMemoryManagementInfo() {

            public MaxMemoryStatus status() {
                final MaxHeapRegion heapRegion = heap().findHeapRegion(address);
                if (heapRegion == null) {
                    // The location is not in any memory region allocated by the heap.
                    return MaxMemoryStatus.UNKNOWN;
                }
                if (heap().phase().isCollecting()) {
                    // Don't quibble if we're in a GC, as long as the address is in either the To or From regions.
                    return MaxMemoryStatus.LIVE;
                }
                if (heapRegion.entityName().equals(SemiSpaceHeapScheme.FROM_REGION_NAME)) {
                    // When not in GC, everything in from-space is dead
                    return MaxMemoryStatus.DEAD;
                }
                if (!heapRegion.memoryRegion().containsInAllocated(address)) {
                    // everything in to-space after the global allocation mark is dead
                    return MaxMemoryStatus.FREE;
                }
                for (TeleNativeThread teleNativeThread : vm().teleProcess().threads()) { // iterate over threads in check in case of tlabs if objects are dead or live
                    TeleThreadLocalsArea teleThreadLocalsArea = teleNativeThread.localsBlock().tlaFor(SafepointPoll.State.ENABLED);
                    if (teleThreadLocalsArea != null) {
                        Word tlabDisabledWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_DISABLED_THREAD_LOCAL_NAME);
                        Word tlabMarkWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_MARK_THREAD_LOCAL_NAME);
                        Word tlabTopWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_TOP_THREAD_LOCAL_NAME);
                        if (tlabDisabledWord.isNotZero() && tlabMarkWord.isNotZero() && tlabTopWord.isNotZero()) {
                            if (address.greaterEqual(tlabMarkWord.asAddress()) && tlabTopWord.asAddress().greaterThan(address)) {
                                return MaxMemoryStatus.FREE;
                            }
                        }
                    }
                }
                // Everything else should be live.
                return MaxMemoryStatus.LIVE;
            }

            public String terseInfo() {
                // Provide text to be displayed in display cell
                return "";
            }

            public String shortDescription() {
                // More information could be added here
                return vm().heapScheme().name();
            }

            public Address address() {
                return address;
            }
            public TeleObject tele() {
                return null;
            }
        };
    }

    /**
     * Finds an existing heap region, if any, that has been created using the
     * remote object describing it.
     */
    private VmHeapRegion find(TeleMemoryRegion runtimeMemoryRegion) {
        for (VmHeapRegion heapRegion : heapRegions) {
            if (runtimeMemoryRegion == heapRegion.representation()) {
                return heapRegion;
            }
        }
        return null;
    }

    public ObjectStatus objectStatusAt(Address origin) throws TeleError {
        TeleError.check(contains(origin), "Location is outside semispace heap regions");
        switch(phase()) {
            case MUTATING:
            case RECLAIMING:
                if (toSpaceMemoryRegion.containsInAllocated(origin)) {
                    final SemiSpaceRemoteReference knownToSpaceReference = toSpaceRefMap.get(origin);
                    if (knownToSpaceReference != null) {
                        return knownToSpaceReference.status();
                    }
                    if (objects().isPlausibleOriginUnsafe(origin)) {
                        return ObjectStatus.LIVE;
                    }
                }
                break;
            case ANALYZING:
                if (toSpaceMemoryRegion.containsInAllocated(origin)) {
                    final SemiSpaceRemoteReference knownToSpaceReference = toSpaceRefMap.get(origin);
                    if (knownToSpaceReference != null) {
                        return knownToSpaceReference.status();
                    }
                    if (objects().isPlausibleOriginUnsafe(origin)) {
                        return ObjectStatus.LIVE;
                    }
                } else if (fromSpaceMemoryRegion.containsInAllocated(origin)) {
                    final SemiSpaceRemoteReference knownFromSpaceReference = fromSpaceRefMap.get(origin);
                    if (knownFromSpaceReference != null) {
                        return knownFromSpaceReference.status();
                    }
                    if (objects().isPlausibleOriginUnsafe(origin)) {
                        // An object in From-Space that hasn't been forwarded
                        return ObjectStatus.LIVE;
                    }
                    if (objects().hasForwardingAddressUnsafe(origin)) {
                        final Address forwardAddress = objects().getForwardingAddressUnsafe(origin);
                        if (toSpaceMemoryRegion.containsInAllocated(forwardAddress) && objectStatusAt(forwardAddress).isLive()) {
                            return ObjectStatus.FORWARDER;
                        }
                    }
                }
                break;
            default:
                TeleError.unknownCase();
        }
        return ObjectStatus.DEAD;
    }


    public boolean isForwardingAddress(Address forwardingAddress) {
        if (phase() == HeapPhase.ANALYZING && toSpaceMemoryRegion.contains(forwardingAddress)) {
            final Address possibleOrigin = objects().forwardingPointerToOriginUnsafe(forwardingAddress);
            if (possibleOrigin.isNotZero() && objectStatusAt(possibleOrigin).isLive()) {
                return true;
            }
        }
        return false;
    }

    public RemoteReference makeReference(Address origin) throws TeleError {
        assert vm().lockHeldByCurrentThread();
        TeleError.check(contains(origin), "Location is outside of " + heapSchemeClass().getSimpleName() + " heap");
        final RemoteReference reference = internalMakeRef(origin);
        return reference != null && reference.status().isLive() ? reference : null;
    }

    public RemoteReference makeQuasiReference(Address origin) throws TeleError {
        assert vm().lockHeldByCurrentThread();
        TeleError.check(contains(origin), "Location is outside of " + heapSchemeClass().getSimpleName() + " heap");
        final RemoteReference reference = internalMakeRef(origin);
        return reference != null && reference.status().isQuasi() ? reference : null;
    }

    /**
     * Creates a reference of the appropriate kind if there is an object or <em>quasi</em>
     * object at the specified origin in VM memory.
     * <p>
     * Using this shared internal method has advantage that the logic is all in one place.
     * The disadvantage is that references will be created unnecessarily.  That effect should
     * be small, though.  The most common use case is to look for a live object, and the number
     * of live objects dominates the number of <em>quasi</em> objects.  Looking for <em>quasi</em>
     * objects only will probably succeed most of the time anyway, because callers are likely to
     * have checked the object's status before making the reference.
     */
    private RemoteReference internalMakeRef(Address origin) {
        SemiSpaceRemoteReference remoteReference = null;
        switch(phase()) {
            case MUTATING:
            case RECLAIMING:
                /*
                 * In this phase there are only live objects in To-Space.  There are no quasi objects.
                 */
                remoteReference = toSpaceRefMap.get(origin);
                if (remoteReference != null) {
                    // An object origin in To-Space already seen.
                    TeleError.check(remoteReference.status().isLive());
                } else if (toSpaceMemoryRegion.containsInAllocated(origin) && objects().isPlausibleOriginUnsafe(origin)) {
                    // An object origin in To-Space not yet seen.
                    remoteReference = SemiSpaceRemoteReference.createLive(this, origin);
                    toSpaceRefMap.put(origin, remoteReference);
                }
                break;
            case ANALYZING:
                /*
                 * In this heap phase, there can be objects and references in both maps: live objects in the To-Space map,
                 * and live objects (not yet forwarded) or quasi objects (forwarders) in the From-Space map.
                 */
                if (toSpaceMemoryRegion.containsInAllocated(origin)) {
                    // A location in the allocated area of To-Space
                    final Address toSpaceOrigin = origin;
                    remoteReference = toSpaceRefMap.get(toSpaceOrigin);
                    if (remoteReference != null) {
                        // A known origin in To-Space
                        TeleError.check(remoteReference.status().isLive());
                    } else if (objects().isPlausibleOriginUnsafe(toSpaceOrigin)) {
                        /*
                         * An object origin in To-Space not yet seen.
                         * This must be the new copy of a forwarded object, but we don't know the location of its the old
                         * copy, which is now a forwarder. If we had previously seen the forwarder, then we would also have
                         * seen this new object as well, in which case a reference for it would already be in the To-Space Map.
                         */
                        remoteReference = SemiSpaceRemoteReference.createInToOnly(this, toSpaceOrigin);
                        toSpaceRefMap.put(toSpaceOrigin, remoteReference);
                    }
                } else if (fromSpaceMemoryRegion.containsInAllocated(origin)) {
                    // A location in the allocated area of From-Space
                    final Address fromSpaceOrigin = origin;
                    remoteReference = fromSpaceRefMap.get(fromSpaceOrigin);
                    if (remoteReference != null) {
                        // A known object origin in From-Space
                        TeleError.check(remoteReference.status().isLive() || remoteReference.status().isForwarder());
                    } else if (objects().isPlausibleOriginUnsafe(fromSpaceOrigin)) {
                        // An origin in From-Space not yet seen and not forwarded.
                        // This will be treated a live reference in From-Space for the time being.
                        remoteReference = SemiSpaceRemoteReference.createInFromOnly(this, fromSpaceOrigin);
                        fromSpaceRefMap.put(fromSpaceOrigin, remoteReference);
                    } else if (objects().hasForwardingAddressUnsafe(fromSpaceOrigin)) {
                        // A possible forwarder origin in From-Space not yet seen.
                        // Check to see if the forwarder really points to an object in To-Space
                        final Address toSpaceOrigin = objects().getForwardingAddressUnsafe(fromSpaceOrigin);
                        SemiSpaceRemoteReference toSpaceReference = toSpaceRefMap.get(toSpaceOrigin);
                        if (toSpaceReference != null) {
                            // A forwarder in From-Space, not yet seen, whose new copy is already known
                            // Create a forwarder quasi reference to retain information about the old location for the duration of the analysis phase
                            remoteReference = SemiSpaceRemoteReference.createForwarder(this, fromSpaceOrigin, toSpaceOrigin);
                            fromSpaceRefMap.put(fromSpaceOrigin, remoteReference);
                            // Update the existing To-Space reference with newly discovered location of its old copy
                            toSpaceReference.discoverOldOrigin(fromSpaceOrigin);
                        } else if (toSpaceMemoryRegion.contains(toSpaceOrigin) && objectStatusAt(toSpaceOrigin).isLive()) {
                            // A forwarder in From-Space, not yet seen, whose new copy in To-Space has not yet been seen
                            // Add a reference for the new copy to the To-Space map
                            final SemiSpaceRemoteReference newCopyReference = SemiSpaceRemoteReference.createInFromTo(this, fromSpaceOrigin, toSpaceOrigin);
                            toSpaceRefMap.put(toSpaceOrigin, newCopyReference);
                            // Create a forwarder quasi reference and add to the From-Space map
                            remoteReference = SemiSpaceRemoteReference.createForwarder(this, fromSpaceOrigin, toSpaceOrigin);
                            fromSpaceRefMap.put(fromSpaceOrigin, remoteReference);
                        }
                    }
                }
                break;
            default:
                TeleError.unknownCase();
        }
        return remoteReference == null ? vm().referenceManager().zeroReference() : remoteReference;
    }

    /**
     * Do either of the heap regions contain the address anywhere in their extents (ignoring the allocation marker)?
     */
    private boolean contains(Address address) {
        return (toSpaceMemoryRegion != null && toSpaceMemoryRegion.contains(address))
            || (fromSpaceMemoryRegion != null && fromSpaceMemoryRegion.contains(address));
    }

    /**
     * Is the address in an area where an object could be either
     * {@linkplain ObjectStatus#LIVE LIVE} or
     * {@linkplain ObjectStatus#UNKNOWN UNKNOWN}.
     */
    private boolean inLiveArea(Address address) {
        switch(phase()) {
            case MUTATING:
            case RECLAIMING:
                return toSpaceMemoryRegion.containsInAllocated(address);
            case ANALYZING:
                return fromSpaceMemoryRegion.containsInAllocated(address) || toSpaceMemoryRegion.containsInAllocated(address);
            default:
                TeleError.unknownCase();
        }
        return false;
    }

    private void printRegionObjectStats(PrintStream printStream, int indent, boolean verbose, TeleLinearAllocationMemoryRegion region, WeakRemoteReferenceMap<SemiSpaceRemoteReference> map) {
        final NumberFormat formatter = NumberFormat.getInstance();
        int totalRefs = 0;
        int liveRefs = 0;
        int deadRefs = 0;

        for (SemiSpaceRemoteReference ref : map.values()) {
            switch(ref.status()) {
                case LIVE:
                    liveRefs++;
                    break;
                case DEAD:
                    deadRefs++;
                    break;
            }
        }
        totalRefs = liveRefs + deadRefs;

        // Line 0
        String indentation = Strings.times(' ', indent);
        final StringBuilder sb0 = new StringBuilder();
        sb0.append("heap region: ");
        sb0.append(find(region).entityName());
        if (verbose) {
            sb0.append(", ref. mgr=").append(getClass().getSimpleName());
        }
        printStream.println(indentation + sb0.toString());

        // increase indentation
        indentation += Strings.times(' ', 4);

        // Line 1
        final StringBuilder sb1 = new StringBuilder();
        sb1.append("memory: ");
        final MemoryUsage usage = region.getUsage();
        final long size = usage.getCommitted();
        if (size > 0) {
            sb1.append("size=" + formatter.format(size));
            final long used = usage.getUsed();
            sb1.append(", used=" + formatter.format(used) + " (" + (Long.toString(100 * used / size)) + "%)");
        } else {
            sb1.append(" <unallocated>");
        }
        printStream.println(indentation + sb1.toString());

        // Line 2, indented
        final StringBuilder sb2 = new StringBuilder();
        sb2.append("mapped object refs=").append(formatter.format(totalRefs));
        if (totalRefs > 0) {
            sb2.append(", object status: ");
            sb2.append(ObjectStatus.LIVE.label()).append("=").append(formatter.format(liveRefs)).append(", ");
        }
        printStream.println(indentation + sb2.toString());
        if (deadRefs > 0) {
            printStream.println(indentation + "ERROR: " + formatter.format(deadRefs) + " DEAD refs in map");
        }

        // Line 3, optional
        if (verbose && totalRefs > 0) {
            printStream.println(indentation +  "Mapped object ref states:");
            final String stateIndentation = indentation + Strings.times(' ', 4);
            for (RefStateCount refStateCount : SemiSpaceRemoteReference.getStateCounts(map.values())) {
                if (refStateCount.count > 0) {
                    printStream.println(stateIndentation + refStateCount.stateName + ": " + formatter.format(refStateCount.count));
                }
            }
        }
    }

    @Override
    public void printObjectSessionStats(PrintStream printStream, int indent, boolean verbose) {
        if (!heapRegions.isEmpty()) {
            int toSpaceRefs = toSpaceRefMap.values().size();
            int fromSpaceRefs = fromSpaceRefMap.values().size();
            printObjectSessionStatsHeader(printStream, indent, verbose, toSpaceRefs + fromSpaceRefs);
            printRegionObjectStats(printStream, indent + 4, verbose, toSpaceMemoryRegion, toSpaceRefMap);
            printRegionObjectStats(printStream, indent + 4, verbose, fromSpaceMemoryRegion, fromSpaceRefMap);
        }
    }

    /**
     * Surrogate object for the scheme instance in the VM.
     */
    public static class TeleSemiSpaceHeapScheme extends TeleHeapScheme {

        public TeleSemiSpaceHeapScheme(TeleVM vm, Reference reference) {
            super(vm, reference);
        }

        /**
         * @return surrogate for the semispace collector's "from" region
         */
        public TeleLinearAllocationMemoryRegion readTeleFromRegion() {
            final Reference fromReference = fields().SemiSpaceHeapScheme_fromSpace.readReference(reference());
            return (TeleLinearAllocationMemoryRegion) objects().makeTeleObject(fromReference);
        }

        /**
         * @return surrogate for the semispace collector's "to" region
         */
        public TeleLinearAllocationMemoryRegion readTeleToRegion() {
            final Reference toReference = fields().SemiSpaceHeapScheme_toSpace.readReference(reference());
            return (TeleLinearAllocationMemoryRegion) objects().makeTeleObject(toReference);
        }

    }

    /**
     * Delayed evaluation of a trace message.
     */
    private class ReferenceUpdateTracer {

        final NumberFormat formatter = NumberFormat.getInstance();

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Phase=" + phase().label());
            sb.append(":  forwarded=").append(formatter.format(forwarded));
            sb.append(", collected=").append(formatter.format(collected));
            return sb.toString();
        }
    }


}
