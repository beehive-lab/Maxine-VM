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
import com.sun.max.memory.*;
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
import com.sun.max.vm.heap.gcx.ms.*;
import com.sun.max.vm.heap.sequential.semiSpace.*;
import com.sun.max.vm.runtime.*;

/**
 * <p>
 * Inspection support specialized for the basic {@linkplain MSHeapScheme SemiSpaceHeapScheme semispace implementation}
 * of {@link HeapScheme} in the VM.
 * </p>
 * <p>
 * This support will function correctly whether or not the VM is built in DEBUG mode. When built in DEBUG mode, the GC
 * will {@linkplain Memory#ZAPPED_MARKER zap} all memory unallocated space.
 * </p>
 * <p>
 * This implementation maintains two <em>maps</em> from VM memory locations to {@link RemoteReference}s: one for each of
 * the two memory regions used by the scheme(<em>To-Space</em> and <em>From-Space</em>). When the collector
 * <em>swaps</em> at the beginning of {@linkplain HeapPhase#ANALYZING analyzing}, the two maps are likewise swapped.
 * Which is to say, the From-Space Map always contains references only into From-Space, wherever From-Space is located.
 * </p>
 * <p>
 * References in this implementation can refer to {@linkplain ObjectStatus#LIVE LIVE} objects in the VM heap or to
 * {@linkplain ObjectStatus#FORWARDER FORWARDER} quasi-objects. The term <em>quasi-object</em> refers to an area of
 * memory formatted as if it were an ordinary Maxine VM object, but which is
 * <ul>
 * <li>only known to the GC implementation;</li>
 * <li>is never given ordinary object behavior; and</li>
 * <li>is never reachable from any object roots.</li>
 * </ul>
 * </p>
 * <p>
 * A {@link RemoteReference} represents the <em>identity</em> of an object (whether legitimate or quasi-) in the VM.
 * Note that this perspective differs somewhat from that of the GC implementation.
 * </p>
 * <p>
 * General map invariants:
 * <ul>
 * <li>The <em>To-Space Map</em> only holds references that are {@linkplain ObjectStatus#LIVE LIVE}.</li>
 * <li>The <em>From-Space Map</em> only holds references that are {@linkplain ObjectStatus#LIVE LIVE} or
 * {@linkplain ObjectStatus#FORWARDER FORWARD}.</li>
 * <li>The maps never contain {@linkplain ObjectStatus#DEAD DEAD} references.</li>
 * <li>The maps always refer only to locations in disjoint regions of memory, so memory location never appears
 * simultaneously in both maps, which is to say there can only be one reference (and one kind of object) at a heap
 * location.</li>
 * <li>There are situations where a reference moves from one map to the other.</li>
 * </ul>
 * </p>
 * <p>
 * The following description enumerates more specifically what remote inspection can observe during each
 * {@link HeapPhase}. The <em>canonical</em> relationship between references and VM objects allows the distinction
 * between the two to be blurred for brevity in this description.
 * </p>
 * <p>
 * <b>{@link HeapPhase#MUTATING} Summary</b>
 * <p>
 * During this phase new objects are allocated linearly in To-Space; From-Space contains nothing.
 * </p>
 * <ul>
 * <li><b>To-Space Map:</b>
 * <ol>
 * <li>The To-Space Map contains only {@linkplain ObjectStatus#LIVE LIVE} object references.</li>
 * <li>New {@linkplain ObjectStatus#LIVE LIVE} objects may be discovered and added to the To-Space Map.</li>
 * <li>No other changes to the To-Space Map take place during the {@linkplain HeapPhase#MUTATING mutating} phase.</li>
 * </ol>
 * </li>
 * <li><b>From-Space Map:</b>
 * <ol>
 * <li>The From-Space Map is empty.</li>
 * <li>No objects appear in From-Space during the {@linkplain HeapPhase#MUTATING mutating} phase.</li>
 * <li>When build in DEBUG mode, the GC {@linkplain Memory#ZAPPED_MARKER zaps} the entire From-Space.</li>
 * </ol>
 * </li>
 * </ul>
 * <p>
 * <b>{@link HeapPhase#ANALYZING} Summary</b>
 * <p>
 * This phase begins by swapping the two spaces, so that From-Space contains all allocated objects, reachable or not.
 * The GC traces reachable objects, and as as they are discovered they are <em>copied</em> and stored by linear
 * allocation into To-Space. A <em>forwarding pointer</em> is stored into the old copy so that all references to the
 * object can eventually be redirected to the new copy by the end of the phase, at which time the contents of From-Space
 * are forgotten (and zapped if in DEBUG mode).
 * <ul>
 * <li><b>To-Space Map:</b>
 * <ol>
 * <li>The To-Space Map is empty at the beginning of the phase.</li>
 * <li>A previously unseen {@linkplain ObjectStatus#LIVE LIVE} object may be discovered in To-Space and added
 * to the To-Space Map.  Such an object is known to be the new copy of an object in From-Space, but in this event
 * the location of the old copy (now a <em>Forwarder</em> quasi-object) is unknown.</li>
 * <li>A previously unseen {@linkplain ObjectStatus#FORWARDER FORWARDER} might be discovered in From-Space that is
 * the old copy of a previously seen {@linkplain ObjectStatus#LIVE LIVE} object in the To-Space map.  In this event
 * the location of the old copy (a <em>Forwarder</em> quasi-object) is recorded in the To-Space reference.</li>
 * <li>A previously unseen {@linkplain ObjectStatus#FORWARDER FORWARDER} might be discovered in From-Space that is
 * the old copy of a previously unseen {@linkplain ObjectStatus#LIVE LIVE} object in To-Space.  In this event a new
 * {@linkplain ObjectStatus#LIVE LIVE} reference is added to the To-Space map, in which the location of its old copy
 * is recorded.</li>
 * <li>No other changes to the To-Space Map take place during the {@linkplain HeapPhase#ANALYZING analyzing} phase.</li>
 * </ol>
 * </li>
 * <li><b>From-Space Map:</b>
 * <ol>
 * <li>The From-Space Map at the beginning of the phase contains precisely the contents of the To-Space map just before
 * the beginning of the phase, at which point all contained references are {@linkplain ObjectStatus#LIVE LIVE}.  This
 * reflects the assumption that all objects are live until proven otherwise, which can only be done at the end of the phase.</li>
 * <li>A previously unseen {@linkplain ObjectStatus#LIVE LIVE} object may be discovered in From-Space and added
 * to the From-Space Map.
 * <li>A previously unseen {@linkplain ObjectStatus#FORWARDER FORWARDER} might be discovered in From-Space and added
 * to the From-Space Map.  If the new copy of forwarded object has already been seen (and thus is in the To-Space Map),
 * then the location of this old copy is recorded in that reference; if the new copy has not been seen, then a new
 * {@linkplain ObjectStatus#LIVE LIVE} reference is created, the location of the old copy is recorded, and it is added
 * to the To-Space Map.</li>
 * <li>A previously unseen {@linkplain ObjectStatus#LIVE LIVE} object in From-Space may be discovered to have been forwarded.
 * In this event, the reference is removed from the From-Space Map, assigned its new memory location, the location of this old
 * copy recorded in it, and the reference is added to the To-Space Map.  A <em>new</em> quasi-object
 * {@linkplain ObjectStatus#FORWARDER FORWARDER} is then created and added back into the From-Space Map.
 * <li>No other changes to the From-Space Map take place during the {@linkplain HeapPhase#ANALYZING analyzing} phase.</li>
 * </ol>
 * </li>
 * </ul>
 * <p>
 * <b>{@link HeapPhase#RECLAIMING} Summary</b>
 * <p>
 * At the beginning of this phase all reachable objects have been copied into To-Space and all references to them
 * revised to the new location.  The contents of From-Space (unreachable objects and forwarders) are forgotten.  In
 * DEBUG mode, the GC {@linkplain Memory#ZAPPED_MARKER zaps} the entire From-Space. This phase is extremely brief.</p>
 * <ul>
 * <li><b>To-Space Map:</b>
 * <ol>
 * <li>The To-Space Map contains only {@linkplain ObjectStatus#LIVE LIVE} object references.</li>
 * <li>No changes to the To-Space Map take place during the {@linkplain HeapPhase#RECLAIMING reclaiming} phase.</li>
 * </ol>
 * </li>
 * <li><b>From-Space Map:</b>
 * <ol>
 * <li>At the beginning of the phase, every entry in the From-Space map is removed and made {@linkplain ObjectStatus#DEAD DEAD}.
 * This includes {@linkplain ObjectStatus#LIVE LIVE} references, which are now known to be unreachable, and
 * {@linkplain ObjectStatus#FORWARDER FORWARDER} quasi-objects, which play no further role.</li>
 * <li>The From-Space Map remains empty during the (brief) remainder of the {@linkplain HeapPhase#RECLAIMING reclaiming} phase.</li>
 * </ol>
 * </li>
 * </ul>
 * @see SemiSpaceHeapScheme
 * @see Memory#ZAPPED_MARKER
 * @see SemiSpaceRemoteReference
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

    @Override
    public List<MaxObject> inspectableObjects() {
        List<MaxObject> inspectableObjects = new ArrayList<MaxObject>();
        inspectableObjects.addAll(super.inspectableObjects());
        if (fromSpaceMemoryRegion != null) {
            inspectableObjects.add(fromSpaceMemoryRegion);
        }
        if (toSpaceMemoryRegion != null) {
            inspectableObjects.add(toSpaceMemoryRegion);
        }
        return inspectableObjects;
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
        if (toSpaceMemoryRegion == null || fromSpaceMemoryRegion == null) {
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

    @Override
    public boolean hasMarkBitmap() {
        return false;
    }

    public MaxMemoryManagementInfo getMemoryManagementInfo(final Address address) {
        return new MaxMemoryManagementInfo() {

            public MaxMemoryManagementStatus status() {
                final MaxHeapRegion heapRegion = heap().findHeapRegion(address);
                final HeapPhase phase = heap().phase();
                if (heapRegion == null) {
                    // The location is not in any memory region allocated by the heap.
                    return MaxMemoryManagementStatus.NONE;
                }
                if (heapRegion.entityName().equals(SemiSpaceHeapScheme.FROM_REGION_NAME)) {
                    // From-Space is generally DEAD, unless we're collecting, in which case we treat
                    // the allocated portion of From-Space as LIVE for the time being.
                    if (phase.isCollecting() && fromSpaceMemoryRegion.containsInAllocated(address)) {
                        return MaxMemoryManagementStatus.LIVE;
                    }
                    return MaxMemoryManagementStatus.DEAD;
                }
                if (heapRegion.entityName().equals(SemiSpaceHeapScheme.TO_REGION_NAME)) {
                    if (phase.isCollecting()) {
                        // While collecting treat the whole allocated part of To-Space as LIVE
                        if (toSpaceMemoryRegion.containsInAllocated(address)) {
                            return MaxMemoryManagementStatus.LIVE;
                        }
                        return MaxMemoryManagementStatus.FREE;
                    }
                    // phase is MUTATING; look more closely at the allocations in each TLAB
                    if (!toSpaceMemoryRegion.containsInAllocated(address)) {
                        // everything in to-space after the global allocation mark is dead
                        return MaxMemoryManagementStatus.FREE;
                    }
                    // TODO (mlvdv) tighten up this loop to return LIVE when it finds the TLAB
                    // containing the address and it is before the mark.  I'm not sure of the exact math.
                    for (TeleNativeThread teleNativeThread : vm().teleProcess().threads()) { // iterate over threads in check in case of tlabs if objects are dead or live
                        TeleThreadLocalsArea teleThreadLocalsArea = teleNativeThread.localsBlock().tlaFor(SafepointPoll.State.ENABLED);
                        if (teleThreadLocalsArea != null) {
                            Word tlabDisabledWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_DISABLED_THREAD_LOCAL_NAME);
                            Word tlabMarkWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_MARK_THREAD_LOCAL_NAME);
                            Word tlabTopWord = teleThreadLocalsArea.getWord(HeapSchemeWithTLAB.TLAB_TOP_THREAD_LOCAL_NAME);
                            if (tlabDisabledWord.isNotZero() && tlabMarkWord.isNotZero() && tlabTopWord.isNotZero()) {
                                if (address.greaterEqual(tlabMarkWord.asAddress()) && tlabTopWord.asAddress().greaterThan(address)) {
                                    return MaxMemoryManagementStatus.FREE;
                                }
                            }
                        }
                    }
                    // Everything else should be live.
                    return MaxMemoryManagementStatus.LIVE;
                }
                // Some other heap region such as boot or immortal; use the simple test
                if (heapRegion.memoryRegion().containsInAllocated(address)) {
                    return MaxMemoryManagementStatus.LIVE;
                }
                return MaxMemoryManagementStatus.FREE;
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
        if (reference == null) {
            return null;
        }
        if (reference.status().isLive()) {
            return reference;
        }
        return reference.status().isForwarder() && phase() == HeapPhase.ANALYZING ? reference : null;
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
                        if (!remoteReference.status().isForwarder() && objects().hasForwardingAddressUnsafe(fromSpaceOrigin)) {
                            // LD: shouldn't we move the ref to the to-space map and create a forwarder if the reference has a forwarding address ?
                            // Added this check in case it happens.
                            TeleError.unexpected("Must not happen");
                        }
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
                            if (toSpaceReference.forwardedFrom().isZero()) {
                                // A forwarder in From-Space, not yet seen, whose new copy is already known
                                // Update the existing To-Space reference with newly discovered location of its old copy
                                toSpaceReference.discoverOldOrigin(fromSpaceOrigin);
                            } else {
                                // This may occur when the forwarder ref previously stored in the from map was collected by the GC, leaving
                                // only the weak ref with no referent.
                                TeleError.check(toSpaceReference.forwardedFrom().equals(fromSpaceOrigin));
                            }
                            // Create a forwarder quasi reference to retain information about the old location for the duration of the analysis phase
                            remoteReference = SemiSpaceRemoteReference.createForwarder(this, fromSpaceOrigin, toSpaceOrigin);
                            fromSpaceRefMap.put(fromSpaceOrigin, remoteReference);
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
     * Is the address in an area where an object could be
     * {@linkplain ObjectStatus#LIVE LIVE}.
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

        public TeleSemiSpaceHeapScheme(TeleVM vm, RemoteReference reference) {
            super(vm, reference);
        }

        /**
         * @return surrogate for the semispace collector's "from" region
         */
        public TeleLinearAllocationMemoryRegion readTeleFromRegion() {
            final RemoteReference fromReference = fields().SemiSpaceHeapScheme_fromSpace.readRemoteReference(reference());
            return (TeleLinearAllocationMemoryRegion) objects().makeTeleObject(fromReference);
        }

        /**
         * @return surrogate for the semispace collector's "to" region
         */
        public TeleLinearAllocationMemoryRegion readTeleToRegion() {
            final RemoteReference toReference = fields().SemiSpaceHeapScheme_toSpace.readRemoteReference(reference());
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
