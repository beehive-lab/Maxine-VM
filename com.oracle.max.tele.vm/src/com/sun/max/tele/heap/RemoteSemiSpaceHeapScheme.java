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
import com.sun.max.tele.heap.semispace.*;
import com.sun.max.tele.heap.semispace.SemiSpaceRemoteReference.RefStateCount;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
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
     */
    private WeakRemoteReferenceMap<SemiSpaceRemoteReference> toSpaceRefMap = new WeakRemoteReferenceMap<SemiSpaceRemoteReference>();

    /**
     * The VM object that describes the location of the collector's From-Space; the location changes when the spaces get swapped.
     */
    private TeleLinearAllocationMemoryRegion fromSpaceMemoryRegion = null;

    /**
     * Map:  VM address in From-Space --> a {@link SemiSpaceRemoteReference} that refers to the object whose origin is at that location.
     */
    private WeakRemoteReferenceMap<SemiSpaceRemoteReference> fromSpaceRefMap = new WeakRemoteReferenceMap<SemiSpaceRemoteReference>();

    private long collected = 0;
    private long forwarded = 0;

    private final ReferenceUpdateTracer referenceUpdateTracer;

    private final List<VmHeapRegion> heapRegions = new ArrayList<VmHeapRegion>(2);

    private final Object statsPrinter = new Object() {
        @Override
        public String toString() {
            final StringBuilder msg = new StringBuilder();
            msg.append("GC phase=").append(phase.label());
            msg.append(" #starts=").append(gcStartedCount);
            msg.append(", #complete=").append(gcCompletedCount);
            return msg.toString();
        }
    };

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
                 * Add a heap phase listener that will will force the VM to stop any time the heap transitions from
                 * analysis to the reclaiming phase of a GC. This is exactly the moment in a GC cycle when reference
                 * information must be updated. Unfortunately, the handler supplied with this listener will only be
                 * called after the VM state refresh cycle is complete. That would be too late since so many other parts
                 * of the refresh cycle depend on references. Consequently, the needed updates take place when this
                 * manager gets refreshed (early in the refresh cycle), not when this handler eventually gets called.
                 */
                try {
                    vm().addGCPhaseListener(new MaxGCPhaseListener() {

                        public void gcPhaseChange(HeapPhase phase) {
                            // Dummy handler; the actual updates must be done early during the refresh cycle.
                            final long phaseChangeEpoch = vm().teleProcess().epoch();
                            Trace.line(TRACE_VALUE, tracePrefix() + " VM stopped for reference updates, epoch=" + phaseChangeEpoch + ", gc cycle=" + gcStartedCount());
                            Trace.line(TRACE_VALUE, tracePrefix() + " Note: updates have long since been done by the time this (dummy) handler is called");
                        }
                    }, RECLAIMING);
                } catch (MaxVMBusyException e) {
                    TeleError.unexpected("Unable to add GC Phase Listener");
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
        }
        if (heapRegions.size() == 2 && epoch > lastUpdateEpoch) {

            heapUpdateTracer.begin();

            /*
             * This is a normal refresh. Immediately update information about the location of the heap regions; this
             * update must be forced because remote objects are otherwise not refreshed until later in the update
             * cycle.
             */
            toSpaceMemoryRegion.updateCache(epoch);
            fromSpaceMemoryRegion.updateCache(epoch);

            /*
             * We only need to check reference state when we're in a collection; otherwise they do not change state.
             */
            if (phase().isCollecting()) {

                /*
                 * Check first to see if a GC cycle has started since the last time we looked. If so, then the spaces have
                 * been swapped, and we must account for that before examining what has happened since the swap.
                 */
                if (lastAnalyzingPhaseCount < gcStartedCount()) {
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
                        fromSpaceRef.analysisBegins();
                    }
                    Trace.end(TRACE_VALUE, tracePrefix() + "first halt in GC cycle=" + gcStartedCount() + ", UNKNOWN refs=" + refs.size());
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
                    int forwarded = 0;
                    int live = 0;
                    for (SemiSpaceRemoteReference fromSpaceRef : fromSpaceRefMap.values()) {
                        switch (fromSpaceRef.status()) {
                            case UNKNOWN:
                                if (objects().hasForwardingAddressUnsafe(fromSpaceRef.origin())) {
                                    // A From-Space reference that has been forwarded since the last time we looked:
                                    // transition state and add to To-Space map.
                                    final Address toOrigin = objects().getForwardingAddressUnsafe(fromSpaceRef.origin());
                                    fromSpaceRef.addToOrigin(toOrigin);
                                    // After the state change, the official origin is now the one in To-Space.
                                    // Note that the reference is still in the From-Space map, indexed by what is now the "forwardedFrom" origin.
                                    toSpaceRefMap.put(toOrigin, fromSpaceRef);
                                    forwarded++;
                                }
                                break;
                            case LIVE:
                                // Do nothing; already has a forwarding pointer and is in the To-Space map
                                live++;
                                break;
                            case DEAD:
                                TeleError.unexpected(tracePrefix() + "DEAD reference found in From-Space map");
                                break;
                            default:
                                TeleError.unknownCase();
                        }
                    }
                    Trace.end(TRACE_VALUE, tracePrefix() + "checking From-Space refs, GC cycle=" + gcStartedCount()
                                    + " forwarded=" + (live + forwarded) + "(old=" + live + ", new=" + forwarded + ")");
                }

                if (phase().isReclaiming() && lastReclaimingPhaseCount < gcStartedCount()) {
                    /*
                     * The heap is in a GC cycle, and this is the first VM halt during that GC cycle where we know
                     * analysis is complete. This halt will usually be caused by the special breakpoint we've set at
                     * entry to the {@linkplain #RECLAIMING} phase.  This is the opportunity
                     * to update reference maps while full information is still available in the collector.
                     */
                    Trace.begin(TRACE_VALUE, tracePrefix() + "first halt in GC RECLAIMING, cycle=" + gcStartedCount() + "; clearing From-Space references");
                    assert lastReclaimingPhaseCount == gcStartedCount() - 1;
                    lastReclaimingPhaseCount = gcStartedCount();
                    for (SemiSpaceRemoteReference toSpaceRef : toSpaceRefMap.values()) {
                        switch (toSpaceRef.status()) {
                            case LIVE:
                                toSpaceRef.analysisEnds();
                                break;
                            case UNKNOWN:
                                TeleError.unexpected(tracePrefix() + "UNKNOWN reference found in To-Space map");
                                break;
                            case DEAD:
                                TeleError.unexpected(tracePrefix() + "DEAD reference found in To-Space map");
                                break;
                            default:
                                TeleError.unknownCase();
                        }
                    }
                    int forwarded = 0;
                    int died = 0;
                    for (SemiSpaceRemoteReference fromSpaceRef : fromSpaceRefMap.values()) {
                        switch (fromSpaceRef.status()) {
                            case LIVE:
                                // Do nothing; the reference will already be in the To-Space map.
                                forwarded++;
                                break;
                            case UNKNOWN:
                                fromSpaceRef.analysisEnds();
                                died++;
                                break;
                            case DEAD:
                                TeleError.unexpected(tracePrefix() + "DEAD reference found in From-Space map");
                                break;
                            default:
                                TeleError.unknownCase();
                        }
                    }
                    fromSpaceRefMap.clear();
                    Trace.end(TRACE_VALUE, tracePrefix() + "first halt in GC RECLAIMING, cycle=" + gcStartedCount() + ", forwarded=" + forwarded + ", died=" + died);

                }
            }
            lastUpdateEpoch = epoch;
            heapUpdateTracer.end(statsPrinter);
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

    public boolean isObjectOrigin(Address origin) throws TeleError {
        TeleError.check(contains(origin), "Location is outside semispace heap regions");
        switch(phase()) {
            case MUTATING:
            case RECLAIMING:
                return toSpaceMemoryRegion.containsInAllocated(origin) && objects().isPlausibleOriginUnsafe(origin);
            case ANALYZING:
                if (toSpaceMemoryRegion.containsInAllocated(origin)) {
                    return objects().isPlausibleOriginUnsafe(origin);
                }
                if (fromSpaceMemoryRegion.containsInAllocated(origin)) {
                    if (objects().hasForwardingAddressUnsafe(origin)) {
                        final Address forwardAddress = objects().getForwardingAddressUnsafe(origin);
                        return toSpaceMemoryRegion.containsInAllocated(forwardAddress) && objects().isPlausibleOriginUnsafe(forwardAddress);
                    } else {
                        return objects().isPlausibleOriginUnsafe(origin);
                    }
                }
                break;
            default:
                TeleError.unknownCase();
        }
        return false;
    }

    public RemoteReference makeReference(Address origin) throws TeleError {
        assert vm().lockHeldByCurrentThread();
        // It is an error to attempt creating a reference if the address is completely outside the managed region(s).
        TeleError.check(contains(origin), "Location is outside semispace heap regions");
        SemiSpaceRemoteReference remoteReference = null;
        switch(phase()) {
            case MUTATING:
            case RECLAIMING:
                // There are only To-Space references during this heap phase.
                remoteReference = toSpaceRefMap.get(origin);
                if (remoteReference != null) {
                    // A reference to the object is already in the To-Space map.
                    TeleError.check(remoteReference.status().isLive());
                } else if (toSpaceMemoryRegion.containsInAllocated(origin) && objects().isPlausibleOriginUnsafe(origin)) {
                    // A newly discovered object in the allocated area of To-Space; add a new reference to the To-Space map.
                    remoteReference = SemiSpaceRemoteReference.createLive(this, origin);
                    toSpaceRefMap.put(origin, remoteReference);
                }
                break;
            case ANALYZING:
                // In this heap phase, a reference can be in both maps at the same time.
                if (toSpaceMemoryRegion.containsInAllocated(origin)) {
                    remoteReference = toSpaceRefMap.get(origin);
                    if (remoteReference != null) {
                        // A reference to the object is already in the To-Space map.
                        TeleError.check(remoteReference.status().isLive());
                        TeleError.check(remoteReference.isForwarded());
                    } else if (objects().isPlausibleOriginUnsafe(origin)) {
                        /*
                         * A newly discovered object in the allocated area of To-Space, which means that it is the new
                         * copy of a forwarded object. There is no need to look in the From-Space map; if the origin of
                         * the original copy had been discovered, then it would already have been added to both the
                         * From-Space and To-Space maps.  Add a new reference to the To-Space map.
                         */
                        remoteReference =  SemiSpaceRemoteReference.createToOnly(this, origin);
                        toSpaceRefMap.put(origin, remoteReference);
                    }
                } else if (fromSpaceMemoryRegion.containsInAllocated(origin)) {
                    remoteReference = fromSpaceRefMap.get(origin);
                    if (remoteReference != null) {
                        // A reference to the object is already in the From-Space map
                        if (!remoteReference.isForwarded() && objects().hasForwardingAddressUnsafe(origin)) {
                            /*
                             * An object in From-Space that has been forwarded since the last time we checked:
                             * transition state and add the reference to the To-Space map.
                             */
                            final Address toOrigin = objects().getForwardingAddressUnsafe(origin);
                            remoteReference.addToOrigin(toOrigin);
                            toSpaceRefMap.put(toOrigin, remoteReference);
                        }
                    } else {
                        if (!objects().hasForwardingAddressUnsafe(origin) && objects().isPlausibleOriginUnsafe(origin)) {
                            /*
                             * A newly discovered object in the allocated area of From-Space that is not forwarded; add
                             * a new reference to the From-Space map, where it is indexed by its origin in From-Space.
                             */
                            remoteReference = SemiSpaceRemoteReference.createFromOnly(this, origin);
                            fromSpaceRefMap.put(origin, remoteReference);
                        } else if (objects().hasForwardingAddressUnsafe(origin)) {
                            /*
                             * A newly discovered object in the allocated area of From-Space that is forwarded.
                             * Check to see if we already know about the copy in To-Space.
                             */
                            final Address toOrigin = objects().getForwardingAddressUnsafe(origin);
                            remoteReference = toSpaceRefMap.get(toOrigin);
                            if (remoteReference != null) {
                                /*
                                 * We already have a reference to the new copy of the forwarded object in To-Space:
                                 * transition state and add it to the From-Space map (indexed by what is now its
                                 * "fowardedFrom" address).
                                 */
                                remoteReference.addFromOrigin(origin);
                                fromSpaceRefMap.put(origin, remoteReference);
                            } else if (objects().isPlausibleOriginUnsafe(toOrigin)) {
                                /*
                                 * A newly discovered object that is forwarded, but whose new copy in To-Space we
                                 * haven't seen yet; add the reference to both the From-Space map, indexed by
                                 * "forwardedFrom" origin, and to the To-Space map, where it is indexed by its new
                                 * origin.
                                 */
                                remoteReference = SemiSpaceRemoteReference.createFromTo(this, origin, toOrigin);
                                fromSpaceRefMap.put(origin, remoteReference);
                                toSpaceRefMap.put(toOrigin, remoteReference);
                            }
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
        int unknownRefs = 0;
        int deadRefs = 0;

        for (SemiSpaceRemoteReference ref : map.values()) {
            switch(ref.status()) {
                case LIVE:
                    liveRefs++;
                    break;
                case UNKNOWN:
                    unknownRefs++;
                    break;
                case DEAD:
                    deadRefs++;
                    break;
            }
        }
        totalRefs = liveRefs + unknownRefs + deadRefs;

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
            sb2.append(ObjectStatus.UNKNOWN.label()).append("=").append(formatter.format(unknownRefs));
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

            final NumberFormat formatter = NumberFormat.getInstance();

            // Line 0
            String indentation = Strings.times(' ', indent);
            final StringBuilder sb0 = new StringBuilder();
            sb0.append("Dynamic Heap:");
            if (verbose) {
                sb0.append("  VMScheme=").append(vm().heapScheme().name());
            }
            printStream.println(indentation + sb0.toString());

            // increase indentation
            indentation += Strings.times(' ', 4);

            // Line 1
            final StringBuilder sb1 = new StringBuilder();
            sb1.append("phase=").append(phase().label());
            sb1.append(", collections completed=").append(formatter.format(gcCompletedCount));
            sb1.append(", total object refs mapped=").append(formatter.format(toSpaceRefs + fromSpaceRefs));
            printStream.println(indentation + sb1.toString());

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
            final Reference fromReference = fields().SemiSpaceHeapScheme_fromSpace.readReference(getReference());
            return (TeleLinearAllocationMemoryRegion) objects().makeTeleObject(fromReference);
        }

        /**
         * @return surrogate for the semispace collector's "to" region
         */
        public TeleLinearAllocationMemoryRegion readTeleToRegion() {
            final Reference toReference = fields().SemiSpaceHeapScheme_toSpace.readReference(getReference());
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
