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
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.TeleVM.InitializationListener;
import com.sun.max.tele.field.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.reference.gen.semispace.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.gen.semiSpace.*;
import com.sun.max.vm.reference.*;

/**
 * Inspector support for working with VM sessions using the VM's simple
* {@linkplain GenSSHeapScheme generational collector},
* an implementation of the VM's {@link HeapScheme} interface.
* WORK IN PROGRESS.
*/
public final class RemoteGenSSHeapScheme extends AbstractRemoteHeapScheme implements RemoteObjectReferenceManager {
    private static final int TRACE_VALUE = 1;
    /**
     * Number of contiguous VM regions for dynamic heap space.
     */
    private static final int NUM_DYN_HEAP_REGIONS = 3;
    /**
     * Total number of VM regions for the heap. This adds to the regions of the dynamic heap the region for the remembered set (card table and FOT).
     */
    private static final int MAX_VM_HEAP_REGIONS = NUM_DYN_HEAP_REGIONS + 1;
    private TeleGenSSHeapScheme scheme;
    private final List<VmHeapRegion> heapRegions = new ArrayList<VmHeapRegion>(MAX_VM_HEAP_REGIONS);
    private final TimedTrace heapUpdateTracer;
    private long lastUpdateEpoch = -1L;
    private long lastAnalyzingPhaseCount = 0L;
    private long lastReclaimingPhaseCount = 0L;

    /**
     * Track full completed collection count. Updated only when entering the reclaiming phase of a full GC.
     */
    private long lastCompletedFullCollectionCount = 0L;
    private boolean fullGC = false;
    private boolean initialized = false;

    private boolean isFullGC() {
        return fullGC;
    }

    private void updateFullGCStatus() {
        int fullCollectionCount = scheme.fullCollectionCount();
        fullGC = fullCollectionCount > lastCompletedFullCollectionCount;
    }

    private TeleCardTableRSet teleCardTableRSet;
    private TeleContiguousHeapSpace nursery;
    private TeleContiguousHeapSpace oldFrom;
    private TeleContiguousHeapSpace oldTo;
    private TeleCardTableRSet cardTableRSet;
    private TeleBaseAtomicBumpPointerAllocator nurseryAllocator;
    private TeleBaseAtomicBumpPointerAllocator oldAllocator;

    /**
     * Allocation mark in the to space of the old generation at last {@link #ANALYZING} phase.
     */
    Address firstEvacuatedMark = Address.zero();

    /**
     * Map VM addresses in the nursery to {@link GenSSRemoteReference} that refer to the object whose origin is at that location.
     */
    private WeakRemoteReferenceMap<GenSSRemoteReference> nurseryRefMap = new WeakRemoteReferenceMap<GenSSRemoteReference>();
    /**
     * Map VM addresses in the To-Space of the old generation to {@link GenSSRemoteReference} that refer to objects whose origin is at that location.
     */
    private WeakRemoteReferenceMap<GenSSRemoteReference> oldToSpaceRefMap = new WeakRemoteReferenceMap<GenSSRemoteReference>();
    /**
     * Map VM addresses in the From-Space of the old generation to {@link GenSSRemoteReference} that refer to objects whose origin is at that location.
     */
    private WeakRemoteReferenceMap<GenSSRemoteReference> oldFromSpaceRefMap = new WeakRemoteReferenceMap<GenSSRemoteReference>();

    /**
     * Map VM addresses in the promotion area of the old generation to {@link GenSSRemoteReference} that refer to objects whose origin is at that location.
     */
    private WeakRemoteReferenceMap<GenSSRemoteReference> promotedRefMap = new WeakRemoteReferenceMap<GenSSRemoteReference>();

    public RemoteGenSSHeapScheme(TeleVM vm) {
        super(vm);
        this.heapUpdateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + "updating");
    }

    @Override
    public Class heapSchemeClass() {
        return GenSSHeapScheme.class;
    }

    @Override
    public void initialize(long epoch) {
        vm().addInitializationListener(new InitializationListener() {
            public void initialiationComplete(final long initializationEpoch) {
                objects().registerTeleObjectType(GenSSHeapScheme.class, TeleGenSSHeapScheme.class);
                // Get the VM object that represents the heap implementation; can't do this any sooner during startup.
                scheme = (TeleGenSSHeapScheme) teleHeapScheme();
                assert scheme != null;
                updateMemoryStatus(initializationEpoch);
                /*
                 * Add a heap phase listener that will  force the VM to stop any time the heap transitions from
                 * analysis to the reclaiming phase of a GC. This is exactly the moment in a GC cycle when reference
                 * information must be updated. Unfortunately, the handler supplied with this listener will only be
                 * called after the VM state refresh cycle is complete. That would be too late since so many other parts
                 * of the refresh cycle depend on references. Consequently, the needed updates take place when this
                 * manager gets refreshed (early in the refresh cycle), not when this handler eventually gets called.
                 * The needed manager knows about the systematic stop at this phase transition and take this into account
                 * when taking action at phase transition.
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

    private void addHeapRegion(TeleMemoryRegion memoryRegion) {
        if (memoryRegion != null) {
            final VmHeapRegion vmHeapRegion = new VmHeapRegion(vm(), memoryRegion, this);
            heapRegions.add(vmHeapRegion);
            vm().addressSpace().add(vmHeapRegion.memoryRegion());
        }
    }

    private void initializeHeapRegions() {
        Trace.begin(TRACE_VALUE, tracePrefix() + "looking for heap regions");
        if (nursery == null) {
            nursery = scheme.readTeleYoungSpace();
            addHeapRegion(nursery);
        }
        if (oldFrom == null) {
            oldFrom = scheme.readTeleOldFromSpace();
            addHeapRegion(oldFrom);
        }
        if (oldTo == null) {
            oldTo = scheme.readTeleOldToSpace();
            addHeapRegion(oldTo);
        }
        if (cardTableRSet == null) {
            cardTableRSet = scheme.readTeleCardTableRSet();
            heapRegions.add(cardTableRSet.vmHeapRegion);
            vm().addressSpace().add(cardTableRSet.vmHeapRegion.memoryRegion());
        }
        Trace.end(TRACE_VALUE, tracePrefix() + "looking for heap regions, " + heapRegions.size() + " found");
    }

    private boolean contains(Address address) {
        try {
            return nursery.contains(address) || oldFrom.contains(address) || oldTo.contains(address);
        } catch (NullPointerException enull) {
            return (nursery != null && nursery.contains(address)) || (oldFrom != null &&  oldFrom.contains(address)) || (oldTo != null && oldTo.contains(address));
        }
    }

    private void beginAnalyzing(String action, WeakRemoteReferenceMap<GenSSRemoteReference> refMap) {
        final boolean minorCollection = !isFullGC();
        final String prefix =  tracePrefix() + "first halt in " + (minorCollection ? "MINOR" : "FULL") + " GC cycle=" + gcStartedCount() + ", ";
        Trace.begin(TRACE_VALUE,  prefix + action);
        List<GenSSRemoteReference> references = refMap.values();
        for (GenSSRemoteReference ref : references) {
            ref.beginAnalyzing(minorCollection);
        }
        Trace.end(TRACE_VALUE, prefix +  "ASSUMED LIVE refs=" + references.size());
    }


    private void updateForwardedReferences(WeakRemoteReferenceMap<GenSSRemoteReference> fromSpaceMap, WeakRemoteReferenceMap<GenSSRemoteReference> toSpaceMap) {
        final boolean minorCollection = !isFullGC();
        final String prefix =  tracePrefix() + "checking forwarding refs, " + (minorCollection ? "MINOR" : "FULL") +  " GC cycle=" + gcStartedCount();
        int newlyForwarded = 0;
        int alreadyForwarded = 0;
        int live = 0;
        int nullOriginCount = 0;

        Trace.begin(TRACE_VALUE, prefix);
        for (GenSSRemoteReference ref : fromSpaceMap.values()) {
            switch(ref.status()) {
                case FORWARDER:
                    alreadyForwarded++;
                    break;
                case LIVE:
                    final Address origin = ref.origin();
                    if (origin.isZero()) {
                        nullOriginCount++;
                        break;
                    }
                    if (objects().hasForwardingAddressUnsafe(origin)) {
                        // A From-Space reference (either to the nursery if doing minor collection, or to the old from-space if doing full collection)  has been forwarded since the last time we looked.
                        final Address toOrigin = objects().getForwardingAddressUnsafe(origin);
                        // We need to:
                        // 1. remove the reference from the from space map
                        fromSpaceMap.remove(origin);
                        // 2. transition it to a forwarded state
                        ref.discoverForwarded(toOrigin, minorCollection);
                        // 3. move it to the to-space map (i.e., either the promoted map or the old to space map).
                        toSpaceMap.put(toOrigin, ref);
                        // 4. create a forwarder reference. We currently enter it in the fromSpaceMap. A better approach might be to move it to a dedicated forwarder map so
                        // these updatedForwardReference only iterate over unforwarded from reference.
                        fromSpaceMap.put(origin, GenSSRemoteReference.createForwarder(this, ref));
                        newlyForwarded++;
                    }
                    live++;
                    break;
                case DEAD:
                    TeleError.unexpected(tracePrefix() + "DEAD reference found in From-Space map");
                    break;
                default:
                    TeleError.unknownCase();
            }
        }
        Trace.end(TRACE_VALUE, prefix + " forwarded=" + newlyForwarded + "(before=" + live + ",  after=" + (live + newlyForwarded) + ")");
    }

    private void updatedReclaimedReference(WeakRemoteReferenceMap<GenSSRemoteReference> fromSpaceMap, WeakRemoteReferenceMap<GenSSRemoteReference> toSpaceMap) {
        final boolean minorCollection = !isFullGC();
        final String prefix =  tracePrefix() + "first halt in  RECLAIMING phase of " + (minorCollection ? "MINOR" : "FULL") +  " GC cycle=" + gcStartedCount();
        Trace.begin(TRACE_VALUE, prefix + " -- clear forwarded & remove dead references");
        for (GenSSRemoteReference ref : toSpaceMap.values()) {
            switch (ref.status()) {
                case LIVE:
                    ref.endAnalyzing(minorCollection);
                    break;
                case FORWARDER:
                    TeleError.unexpected(tracePrefix() + "FORWARDER reference found in " + (minorCollection ? "promoted space" : "old to-space") + " map");
                    break;
                case DEAD:
                    TeleError.unexpected(tracePrefix() + "DEAD reference found in  " +  (minorCollection ? "promoted space" : "old to-space") + " map");
                    break;
                default:
                    TeleError.unknownCase();
            }
        }
        int died = 0;
        int forwarded = 0;
        for (GenSSRemoteReference ref : fromSpaceMap.values()) {
            switch (ref.status()) {
                case LIVE:
                    ref.endAnalyzing(minorCollection);
                    died++;
                    break;
                case FORWARDER:
                    ref.endAnalyzing(minorCollection);
                    forwarded++;
                    break;
                case DEAD:
                    TeleError.unexpected(tracePrefix() + "DEAD reference found in " + (minorCollection ? "nursery" : "old from-space") + " map");
                    break;
                default:
                    TeleError.unknownCase();
            }
        }
        fromSpaceMap.clear();
        Trace.end(TRACE_VALUE, prefix + ", forwarded cleared =" + forwarded + ", died=" + died);
        Trace.end(TRACE_VALUE, tracePrefix() + "first halt in GC RECLAIMING, cycle=" + gcStartedCount() + ", reclaimed=(objects=" + died + ", fowarders=" + forwarded + ")");
    }

    public List<VmHeapRegion> heapRegions() {
        return heapRegions;
    }

    public boolean canCreateLive() {
        return !(isFullGC() && lastReclaimingPhaseCount < gcStartedCount());
    }

    private boolean checkNoOverlap(WeakRemoteReferenceMap<GenSSRemoteReference> map1, WeakRemoteReferenceMap<GenSSRemoteReference> map2) {
        for (GenSSRemoteReference ref : map1.values()) {
            GenSSRemoteReference found = map2.get(ref.toOrigin());
            if (found != null) {
                System.err.print("ref : " + ref + " duplicated in other map : " + found);
                return false;
            }
        }
        return true;
    }

    @Override
    public void updateMemoryStatus(long epoch) {
        super.updateMemoryStatus(epoch);
        // Can't do anything until we have the VM object that represents the scheme implementation
        if (scheme == null) {
            return;
        }
        if (heapRegions.size() < MAX_VM_HEAP_REGIONS) {
            updateHeapFreeChunkHubOrigin();
            initializeHeapRegions();
            if (heapRegions.size() < MAX_VM_HEAP_REGIONS) {
                return;
            }
            nurseryAllocator = scheme.readYoungSpaceAllocator();
            oldAllocator = scheme.readOldSpaceAllocator();
        }

        if (epoch > lastUpdateEpoch) {
            heapUpdateTracer.begin();
            /*
             * This is a normal refresh. Immediately update information about the location of the heap regions; this
             * update must be forced because remote objects are otherwise not refreshed until later in the update
             * cycle.
             */
            nursery.updateCache(epoch);
            oldTo.updateCache(epoch);
            oldFrom.updateCache(epoch);
            nurseryAllocator.updateCache(epoch);
            oldAllocator.updateCache(epoch);
            cardTableRSet.updateCache(epoch);

            /*
             * For this collector, we only need an overall review of reference state when we're actually collecting.
             */
            if (phase().isCollecting()) {
                updateFullGCStatus();
                /*
                 * Check first if a GC cycle has started since the last time we looked.
                 */
                if (lastAnalyzingPhaseCount < gcStartedCount()) {
                    assert lastAnalyzingPhaseCount == gcStartedCount() - 1;
                    assert oldFromSpaceRefMap.isEmpty();
                    assert promotedRefMap.isEmpty();
                    firstEvacuatedMark = scheme.firstEvacuatedMark();
                    if (isFullGC()) {
                        final TeleContiguousHeapSpace tempHeapSpace = oldTo;
                        oldTo = oldFrom;
                        oldFrom = tempHeapSpace;
                        final WeakRemoteReferenceMap<GenSSRemoteReference> tempRefMap = oldToSpaceRefMap;
                        oldToSpaceRefMap = oldFromSpaceRefMap;
                        oldFromSpaceRefMap = tempRefMap;
                        // Transition the state of all references that are now in the old from-Space
                        beginAnalyzing("flip old generation semi spaces", oldFromSpaceRefMap);
                        GenSSRemoteReference.checkNoLiveRef(oldToSpaceRefMap, false);
                    } else {
                        beginAnalyzing("turn all young refs into young from refs ", nurseryRefMap);
                    }
                    lastAnalyzingPhaseCount = gcStartedCount();
                }
                /*
                 * If we're still analyzing, we need to update references to objects that may have been forwarded during this GC cycle.
                 */
                if (lastReclaimingPhaseCount < gcStartedCount()) {
                    if (isFullGC()) {
                        updateForwardedReferences(oldFromSpaceRefMap, oldToSpaceRefMap);
                        GenSSRemoteReference.checkNoLiveRef(oldToSpaceRefMap, false);
                    } else {
                        updateForwardedReferences(nurseryRefMap, promotedRefMap);
                    }
                }
                if (phase().isReclaiming() && lastReclaimingPhaseCount < gcStartedCount()) {
                    /*
                     * The heap is in a GC cycle, and this is the first VM halt during that GC cycle where we know
                     * analysis is complete. This halt will usually be caused by the special breakpoint we've set at
                     * entry to the {@linkplain #RECLAIMING} phase.  This is the opportunity
                     * to update reference maps while full information is still available in the collector.
                     */
                    assert lastReclaimingPhaseCount == gcStartedCount() - 1;
                    lastReclaimingPhaseCount = gcStartedCount();
                    if (isFullGC()) {
                        updatedReclaimedReference(oldFromSpaceRefMap, oldToSpaceRefMap);
                        lastCompletedFullCollectionCount = scheme.fullCollectionCount();
                    } else {
                        checkNoOverlap(promotedRefMap, oldToSpaceRefMap);
                        updatedReclaimedReference(nurseryRefMap, promotedRefMap);
                        checkNoOverlap(promotedRefMap, oldToSpaceRefMap);
                        for (GenSSRemoteReference ref : promotedRefMap.values()) {
                            oldToSpaceRefMap.put(ref.toOrigin(), ref);
                        }
                        promotedRefMap.clear();
                    }
                }
            }
            lastUpdateEpoch = epoch;
            heapUpdateTracer.end(heapUpdateStatsPrinter);
        }
    }

    @Override
    public MaxMemoryManagementInfo getMemoryManagementInfo(Address address) {
        // TODO Auto-generated method stub
        return null;
    }

    public ObjectStatus objectStatusAt(Address origin) {
        TeleError.check(contains(origin), "Location is outside GenSSHeapScheme regions");
        if (isHeapFreeChunkOrigin(origin)) {
            return ObjectStatus.FREE;
        }
        switch(phase()) {
            case MUTATING:
                if ((oldAllocator.containsInAllocated(origin) || nurseryAllocator.containsInAllocated(origin)) && objects().isPlausibleOriginUnsafe(origin)) {
                    return ObjectStatus.LIVE;
                }
                break;
            case ANALYZING:
                if (fullGC) {
                    if (oldFrom.containsInAllocated(origin)) {
                        if (objects().hasForwardingAddressUnsafe(origin)) {
                            final Address forwardAddress = objects().getForwardingAddressUnsafe(origin);
                            if (oldAllocator.containsInAllocated(forwardAddress) && objects().isPlausibleOriginUnsafe(forwardAddress)) {
                                return ObjectStatus.FORWARDER;
                            }
                        } else if (objects().isPlausibleOriginUnsafe(origin)) {
                            return ObjectStatus.LIVE;
                        }
                    }
                } else if (nursery.containsInAllocated(origin)) {
                    if (objects().hasForwardingAddressUnsafe(origin)) {
                        final Address forwardAddress = objects().getForwardingAddressUnsafe(origin);
                        if (oldAllocator.containsInAllocated(forwardAddress) && objects().isPlausibleOriginUnsafe(forwardAddress)) {
                            return ObjectStatus.FORWARDER;
                        }
                    } else if (objects().isPlausibleOriginUnsafe(origin)) {
                        return ObjectStatus.LIVE;
                    }
                }
                // Wasn't a forwarded object, or an object in either of the from space (nursery, old from).
                // The to-space for both minor and full collection is the old to space. So check it now.
                if (oldAllocator.containsInAllocated(origin)) {
                    if (objects().isPlausibleOriginUnsafe(origin)) {
                        return ObjectStatus.LIVE;
                    }
                }
                break;
            case RECLAIMING:
                // Whether full or minor collection, the nursery and old from space are always empty during the reclaiming phase.
                if (oldAllocator.containsInAllocated(origin) && objects().isPlausibleOriginUnsafe(origin)) {
                    return ObjectStatus.LIVE;
                }
                break;
            default:
                TeleError.unknownCase();
        }
        return ObjectStatus.DEAD;
    }

    public boolean isForwardingAddress(Address forwardingAddress) {
        if (phase() == HeapPhase.ANALYZING) {
            Address possibleOrigin = Address.zero();
            if (fullGC) {
                if (oldTo.containsInAllocated(forwardingAddress)) {
                    possibleOrigin = objects().forwardingPointerToOriginUnsafe(forwardingAddress);
                }
            } else if (oldTo.containsInAllocated(forwardingAddress) && forwardingAddress.greaterEqual(firstEvacuatedMark)) {
                possibleOrigin = objects().forwardingPointerToOriginUnsafe(forwardingAddress);
            }
            return possibleOrigin.isNotZero() && objectStatusAt(possibleOrigin).isLive();
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
        // return reference != null && reference.status().isLive() ? reference : null;
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
     * have already checked the object's status before making the reference.
     */
    private RemoteReference internalMakeRef(Address origin) {
        GenSSRemoteReference ref = null;
        switch(phase()) {
            case MUTATING:
                if (nurseryAllocator.containsInAllocated(origin)) {
                    ref = nurseryRefMap.get(origin);
                    if (ref != null) {
                        // A reference to the object is already in one of the live map.
                        TeleError.check(ref.status().isLive());
                    } else {
                        ref = GenSSRemoteReference.createLive(this, origin, true);
                        nurseryRefMap.put(origin, ref);
                    }
                    return ref;
                }
                // Fall-off and check live old objects. Same logic as for the reclaiming phase for old objects.
            case RECLAIMING:
                // There are no young object while in the reclaiming phase.
                if (oldAllocator.containsInAllocated(origin)) {
                    ref = oldToSpaceRefMap.get(origin);
                    if (ref != null) {
                        // A reference to the object is already in one of the live map.
                        TeleError.check(ref.status().isLive());
                    } else {
                        ref = GenSSRemoteReference.createLive(this, origin, false);
                        oldToSpaceRefMap.put(origin, ref);
                    }
                    return ref;
                }
                break;
            case ANALYZING:
                if (isFullGC()) {
                    if (oldAllocator.containsInAllocated(origin)) {
                        /*
                         * Full GC. Nursery is empty, all objects in ToSpace are forwarded.
                         */
                        ref = oldToSpaceRefMap.get(origin);
                        if (ref != null) {
                            // A reference to the object is already in one of the live map.
                            TeleError.check(ref.status().isLive());
                        } else if (objects().isPlausibleOriginUnsafe(origin)) {
                            /*
                             * A newly discovered object in the old To-Space.  In the analyzing phase of a full GC, the object must be a
                             * copy of a forwarded object.
                             */
                            ref = GenSSRemoteReference.createOldTo(this, origin, false);
                            oldToSpaceRefMap.put(origin, ref);
                        }
                    } else if (oldFrom.containsInAllocated(origin)) {
                        ref = makeForwardedReference(origin, oldFromSpaceRefMap, oldToSpaceRefMap, false);
                    }
                } else {
                    if (oldAllocator.containsInAllocated(origin)) {
                        /*
                         *  Minor collection: the object may be a live object, or a freshly promoted one. Check both maps depending of where the origin is.
                         */
                        if (origin.lessThan(firstEvacuatedMark)) {
                            ref = oldToSpaceRefMap.get(origin);
                            if (ref != null) {
                                // A reference to the object is already in old-to live map.
                                TeleError.check(ref.status().isLive() && !ref.status().isForwarder());
                            } else if (objects().isPlausibleOriginUnsafe(origin)) {
                                ref = GenSSRemoteReference.createLive(this, origin, false);
                                oldToSpaceRefMap.put(origin, ref);
                            }
                        } else {
                            ref = promotedRefMap.get(origin);
                            if (ref != null) {
                                TeleError.check(ref.status().isLive() && !ref.status().isForwarder());
                            } else if (objects().isPlausibleOriginUnsafe(origin)) {
                                ref = GenSSRemoteReference.createOldTo(this, origin, true);
                                promotedRefMap.put(origin, ref);
                            }
                        }
                    } else if (nursery.containsInAllocated(origin)) {
                        ref = makeForwardedReference(origin, nurseryRefMap, promotedRefMap, true);
                    }
                }
                break;
            default:
                TeleError.unknownCase();
        }
        return  ref == null ? vm().referenceManager().zeroReference() : ref;
    }

    /**
     * Implement the logic for making a forwarding reference. The logic is the same for minor and full collection and is parameterized with
     * just the from and to space reference maps used.
     * The map for the from space is queried, and a new reference is created if none is found.
     * The existing reference may be updated if the status of the object at the specified origin has changed (e.g., it has become a forwarder), and the maps may be
     * updated accordingly.
     *
     * @param fromOrigin the address in the From Space
     * @param fromRefMap the map holding the reference to objects of the space being evacuated (either the nursery or the old From space).
     * @param toRefMap the map holding the reference to evacuated objects (either the promoted map, or the old To space map)
     * @param isMinorCollection true if this is  minor collection
     * @return the reference corresponding to the origin.
     */
    private GenSSRemoteReference makeForwardedReference(
                    Address fromOrigin,
                    WeakRemoteReferenceMap<GenSSRemoteReference> fromRefMap,
                    WeakRemoteReferenceMap<GenSSRemoteReference> toRefMap,
                    boolean isMinorCollection) {
        final boolean isForwarder = objects().hasForwardingAddressUnsafe(fromOrigin);
        GenSSRemoteReference ref = null;
        ref = fromRefMap.get(fromOrigin);
        if (ref != null) {
            // A reference to the object is already in the from-space map. Check if it was forwarded and if so, update the ref and maps accordingly.
            if (!ref.status().isForwarder() && isForwarder) {
                final Address toOrigin = objects().getForwardingAddressUnsafe(fromOrigin);
                fromRefMap.remove(fromOrigin);
                ref.discoverForwarded(toOrigin, isMinorCollection);
                toRefMap.put(toOrigin, ref);
                // Create a forwarder and add it to the from-space map.
                fromRefMap.put(fromOrigin, GenSSRemoteReference.createForwarder(this, ref));
            }
        } else {
            if (isForwarder) {
                /*
                 * A newly discovered object in the old From-Space that is forwarded.
                 * Check to see if we already know about the copy in To-Space.
                 */
                final Address toOrigin = objects().getForwardingAddressUnsafe(fromOrigin);
                GenSSRemoteReference forwardedRef = toRefMap.get(toOrigin);
                if (forwardedRef != null) {
                    if (forwardedRef.forwardedFrom().isZero()) {
                        /*
                         * We already have a reference to the new copy of the forwarded object in the old To-Space:
                         * transition state and add it to the From-Space map (indexed by its fromOrigin).
                         */
                        forwardedRef.discoverForwarder(fromOrigin, isMinorCollection);
                    } else {
                        // This may occur when the forwarder ref previously stored in the from map was collected by the GC, leaving
                        // only the weak ref with no referent.
                        TeleError.check(forwardedRef.forwardedFrom().equals(fromOrigin));
                    }
                    ref = GenSSRemoteReference.createForwarder(this, forwardedRef);
                    fromRefMap.put(fromOrigin, ref);
                } else if (objects().isPlausibleOriginUnsafe(toOrigin)) {
                    /*
                     * A newly discovered object that is forwarded, but whose new copy in To-Space we
                     * haven't seen yet; add the reference to both the From-Space map, indexed by
                     * "forwardedFrom" origin, and to the To-Space map, where it is indexed by its new
                     * origin.
                     */
                    forwardedRef = GenSSRemoteReference.createFromTo(this, fromOrigin, toOrigin, isMinorCollection);
                    toRefMap.put(toOrigin, forwardedRef);
                    // Create a forwarder and add it to the from-space map.
                    ref = GenSSRemoteReference.createForwarder(this, forwardedRef);
                    fromRefMap.put(fromOrigin, ref);
                }
            } else if (objects().isPlausibleOriginUnsafe(fromOrigin)) {
                /*
                 * A newly discovered object in the old From-Space that is not forwarded; add
                 * a new reference to the old From-Space map, where it is indexed by its origin in From-Space.
                 */
                ref = GenSSRemoteReference.createFromOnly(this, fromOrigin, isMinorCollection);
                fromRefMap.put(fromOrigin, ref);
            }
        }
        return ref;
    }

    @Override
    public void printObjectSessionStats(PrintStream printStream, int indent, boolean verbose) {
        // TODO Auto-generated method stub

    }

    public static class TeleGenSSHeapScheme extends TeleHeapScheme {
        private Reference oldSpaceReference = Reference.zero();
        public TeleGenSSHeapScheme(TeleVM vm, Reference reference) {
            super(vm, reference);
        }

        public TeleCardTableRSet readTeleCardTableRSet() {
            final Reference cardTableRSetReference = fields().GenSSHeapScheme_cardTableRSet.readReference(reference());
            if (cardTableRSetReference.isZero()) {
                return null;
            }
            return  (TeleCardTableRSet) objects().makeTeleObject(cardTableRSetReference);
        }

        public TeleContiguousHeapSpace readTeleYoungSpace() {
            final Reference youngSpaceReference = fields().GenSSHeapScheme_youngSpace.readReference(reference());
            if (youngSpaceReference.isZero()) {
                return null;
            }
            return (TeleContiguousHeapSpace) objects().makeTeleObject(fields().ContiguousAllocatingSpace_space.readReference(youngSpaceReference));
        }

        public TeleContiguousHeapSpace readTeleOldToSpace() {
            if (oldSpaceReference.isZero()) {
                oldSpaceReference = fields().GenSSHeapScheme_oldSpace.readReference(reference());
                if (oldSpaceReference.isZero()) {
                    return null;
                }
            }
            return (TeleContiguousHeapSpace) objects().makeTeleObject(fields().ContiguousAllocatingSpace_space.readReference(oldSpaceReference));
        }

        public TeleContiguousHeapSpace readTeleOldFromSpace() {
            if (oldSpaceReference.isZero()) {
                oldSpaceReference = fields().GenSSHeapScheme_oldSpace.readReference(reference());
                if (oldSpaceReference.isZero()) {
                    return null;
                }
            }
            return (TeleContiguousHeapSpace) objects().makeTeleObject(fields().ContiguousSemiSpace_fromSpace.readReference(oldSpaceReference));
        }

        private TeleBaseAtomicBumpPointerAllocator readTeleBumpAllocator(TeleInstanceReferenceFieldAccess spaceFieldAccess) {
            Reference spaceReference = spaceFieldAccess.readReference(reference());
            if (spaceReference.isZero()) {
                return null;
            }
            return (TeleBaseAtomicBumpPointerAllocator)  objects().makeTeleObject(fields().ContiguousAllocatingSpace_allocator.readReference(spaceReference));
        }

        public TeleBaseAtomicBumpPointerAllocator readOldSpaceAllocator() {
            return readTeleBumpAllocator(fields().GenSSHeapScheme_oldSpace);
        }

        public TeleBaseAtomicBumpPointerAllocator readYoungSpaceAllocator() {
            return readTeleBumpAllocator(fields().GenSSHeapScheme_youngSpace);
        }

        public int fullCollectionCount() {
            return fields().GenSSHeapScheme_fullCollectionCount.readInt(reference());
        }

        /**
         * Return the address of the first evacuated object of most recent collection.
         * @return an address in the To-space of the old generation.
         */
        public Address firstEvacuatedMark() {
            return  fields().EvacuatorToCardSpace_allocatedRangeStart.readWord(fields().GenSSHeapScheme_youngSpaceEvacuator.readReference(reference())).asAddress();
        }
    }
}
