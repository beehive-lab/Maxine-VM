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

import com.sun.max.memory.*;
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

/**
 * Inspector support for working with VM sessions using the VM's simple
* {@linkplain GenSSHeapScheme generational collector},
* an implementation of the VM's {@link HeapScheme} interface.
* WORK IN PROGRESS.
*/
public final class RemoteGenSSHeapScheme extends AbstractRemoteHeapScheme implements RemoteObjectReferenceManager, VmCardTableHeap, VmRelocatingHeap {
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
    /**
     * Range of old to-space holding the survivors of the latest old generation collection.
     * Updated once per update cycle.
     */
    private final MemoryRegion oldSurvivorRange = new MemoryRegion();
    /**
     * Range of young space used when a full GC overflow occurred to hold the survivors of the latest old generation collection.
     * Updated once per update cycle.
     */
    private final MemoryRegion oldOverflowRange = new MemoryRegion();
    /**
     * Range of old to-space holding the survivors of the latest young generation collection.
     * Updated once per update cycle.
     */
    private final MemoryRegion youngSurvivorRange = new MemoryRegion();
    /**
     * Range of old from-space used when a minor GC overflow occurred to hold the survivors of the latest young generation collection.
     * Updated once per update cycle.
     */
    private final MemoryRegion youngOverflowRange = new MemoryRegion();

    private TeleContiguousHeapSpace nursery;
    private TeleContiguousHeapSpace oldFrom;
    private TeleContiguousHeapSpace oldTo;
    private TeleCardTableRSet cardTableRSet;
    private TeleBaseAtomicBumpPointerAllocator nurseryAllocator;
    private TeleBaseAtomicBumpPointerAllocator oldAllocator;

    /**
     * Track whether a minor evacuation overflowed into from space.
     */
    boolean minorEvacuationOverflow = false;
    /**
     * Track whether a old evacuation overflowed into young space.
     */
    boolean oldEvacuationOverflow = false;

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

    /**
     *  Map VM addresses to unallocated space. These may be heap free chunk or dark matter.
     * <p>
     * <strong>Invariant</strong>: the map holds only objects with status {@linkplain ObjectStatus#FREE FREE} or {@linkplain ObjectStatus#DARK DARK}.
     */
    private WeakRemoteReferenceMap<GenSSRemoteReference> unallocatedRefMap = new WeakRemoteReferenceMap<GenSSRemoteReference>();

    /**
     * Map VM addresses in the from space area of the old generation to {@link GenSSRemoteReference} that refer to objects whose origin is at that location.
     * This maps is used only when a minor evacuation overflow to the from space.
     * The overflowRefMap is merged in the to space refmap when full GC begins.
     */
    private WeakRemoteReferenceMap<GenSSRemoteReference> overflowRefMap = new WeakRemoteReferenceMap<GenSSRemoteReference>();

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
        int live = 0;

        Trace.begin(TRACE_VALUE, prefix);
        for (GenSSRemoteReference ref : fromSpaceMap.values()) {
            switch(ref.status()) {
                case FORWARDER:
                    break;
                case LIVE:
                    final Address origin = ref.origin();
                    if (origin.isZero()) {
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

    private void updateEvacuator(RemoteReference evacuator, MemoryRegion survivorRange, MemoryRegion overflowRange, boolean overflowOccurred) {
        Address initialMark = scheme.initialEvacuationMark(evacuator);
        Address ptop = scheme.ptop(evacuator);
        survivorRange.setStart(initialMark);
        if (overflowOccurred) {
            survivorRange.setEnd(oldTo.committedEnd());
            overflowRange.setStart(oldAllocator.start());
            overflowRange.setEnd(ptop);
        } else {
            survivorRange.setEnd(ptop);
            overflowRange.setStart(Address.zero());
            overflowRange.setSize(Size.zero());
        }
    }

    @Override
    public void updateMemoryStatus(long epoch) {
        super.updateMemoryStatus(epoch);
        updateFreeHubOrigins();
        // Can't do anything until we have the VM object that represents the scheme implementation
        if (scheme == null) {
            return;
        }
        if (heapRegions.size() < MAX_VM_HEAP_REGIONS) {
            updateFreeHubOrigins();
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
            minorEvacuationOverflow = scheme.minorEvacuationOverflow();
            oldEvacuationOverflow = scheme.oldEvacuationOverflow();
            /*
             * For this collector, we only need an overall review of reference state when we're actually collecting.
             */
            if (phase().isCollecting()) {
                updateFullGCStatus();
                updateEvacuator(scheme.youngSpaceEvacuator(), youngSurvivorRange, youngOverflowRange, minorEvacuationOverflow);
                if (fullGC) {
                    updateEvacuator(scheme.oldSpaceEvacuator(), oldSurvivorRange, oldOverflowRange, oldEvacuationOverflow);
                }
               /*
                 * Check first if a GC cycle has started since the last time we looked.
                 */
                if (lastAnalyzingPhaseCount < gcStartedCount()) {
                    assert lastAnalyzingPhaseCount == gcStartedCount() - 1 || TeleVM.mode != MaxInspectionMode.CREATE;
                    assert oldFromSpaceRefMap.isEmpty();
                    assert promotedRefMap.isEmpty();
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
    public MaxMemoryManagementInfo getMemoryManagementInfo(final Address address) {
        return new MaxMemoryManagementInfo() {
            @Override
            public MaxMemoryManagementStatus status() {
                if (address == null || address.isZero()) {
                    return MaxMemoryManagementStatus.NONE;
                }
                final MaxHeapRegion heapRegion = heap().findHeapRegion(address);
                if (heapRegion == null) {
                    // The location is not in any memory region allocated by the heap.
                    return MaxMemoryManagementStatus.NONE;
                }
                // TODO (mlvdv) using ObjectStatusAt() isn't correct, but unsure what to do
                if (contains(address)) {
                    switch(objectStatusAt(address)) {
                        case LIVE:
                            return MaxMemoryManagementStatus.LIVE;
                        case FORWARDER:
                            return MaxMemoryManagementStatus.LIVE;
                        case DEAD:
                            return MaxMemoryManagementStatus.DEAD;
                        case FREE:
                            return MaxMemoryManagementStatus.FREE;
                    }
                }
                return MaxMemoryManagementStatus.LIVE;
            }

            @Override
            public String terseInfo() {
                // Return card table index and whether the card is dirty or not.
                final int ci = cardTableRSet.cardIndex(address);
                return Integer.toString(ci);
            }

            @Override
            public String shortDescription() {
                return "Card #";
            }

            @Override
            public Address address() {
                return address;
            }

            @Override
            public MaxObject tele() {
                return null;
            }
        };
    }

    public MaxCardTable cardTable() {
        return null;
    }

    public ObjectStatus objectStatusAt(Address origin) {
        TeleError.check(contains(origin), "Location is outside GenSSHeapScheme dynamic heap regions");
        if (isHeapFreeChunkOrigin(origin)) {
            return ObjectStatus.FREE;
        }
        if (isDarkMatterOrigin(origin)) {
            return ObjectStatus.DARK;
        }
        switch(phase()) {
            case MUTATING:
                if ((oldTo.contains(origin) && origin.lessThan(oldAllocator.top()) || nurseryAllocator.containsInAllocated(origin)) &&
                                objects().isPlausibleOriginUnsafe(origin)) {
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
                    } else if ((oldSurvivorRange.contains(origin) || oldOverflowRange.contains(origin)) && objects().isPlausibleOriginUnsafe(origin)) {
                        return ObjectStatus.LIVE;
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
                } else if ((youngSurvivorRange.contains(origin) || youngOverflowRange.contains(origin)) && objects().isPlausibleOriginUnsafe(origin)) {
                    // A promoted young object
                    return ObjectStatus.LIVE;
                } else if (oldTo.contains(origin) && origin.lessThan(youngSurvivorRange.start()) && objects().isPlausibleOriginUnsafe(origin)) {
                    // An old live object
                    return ObjectStatus.LIVE;
                }
                break;
            case RECLAIMING:
                // Whether full or minor collection, the nursery and old from space are always empty during the reclaiming phase.
                if (fullGC && oldEvacuationOverflow) {
                    assert nursery.contains(oldAllocator.top());
                    // there might be live objects in the young generation which serves as overflow area to the old generation evacuator
                    if ((oldTo.contains(origin) || (nursery.contains(origin) && origin.lessThan(oldAllocator.top()))) && objects().isPlausibleOriginUnsafe(origin)) {
                        return ObjectStatus.LIVE;
                    }
                } else if (minorEvacuationOverflow && !fullGC) {
                    assert oldFrom.contains(oldAllocator.top());
                    // there might be live objects in the old from-space which serves as overflow area to the young generation evacuator
                    if ((oldTo.contains(origin) || (oldFrom.contains(origin) && origin.lessThan(oldAllocator.top()))) && objects().isPlausibleOriginUnsafe(origin)) {
                        return ObjectStatus.LIVE;
                    }
                } else if (oldTo.contains(origin) && origin.lessThan(oldAllocator.top()) && objects().isPlausibleOriginUnsafe(origin)) {
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
            boolean inSurvivorRange = fullGC ?
                 oldSurvivorRange.contains(forwardingAddress) || oldOverflowRange.contains(forwardingAddress) :
                 youngSurvivorRange.contains(forwardingAddress) || youngOverflowRange.contains(forwardingAddress);
            if (inSurvivorRange) {
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
    }

    public RemoteReference makeQuasiReference(Address origin) throws TeleError {
        assert vm().lockHeldByCurrentThread();
        TeleError.check(contains(origin), "Location is outside of " + heapSchemeClass().getSimpleName() + " heap");
        final RemoteReference reference = internalMakeRef(origin);
        return reference != null && reference.status().isQuasi() ? reference : null;
    }

    private int overflowReferenceCount = 0;
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
                    } else if (objects().isPlausibleOriginUnsafe(origin)) {
                        ref = GenSSRemoteReference.createLive(this, origin, true);
                        nurseryRefMap.put(origin, ref);
                    }
                    break;
                }
                // Fall-off and check live old objects. Same logic as for the reclaiming phase for old objects.
            case RECLAIMING:
                WeakRemoteReferenceMap<GenSSRemoteReference> oldAllocatorRefMap = oldToSpaceRefMap;
                if (minorEvacuationOverflow) {
                    // The oldAllocator doesn't point to the from space but to the overflow area. The from space is full so we can check against it directly.
                    if (oldTo.containsInAllocated(origin)) {
                        ref = oldToSpaceRefMap.get(origin);
                        if (ref != null) {
                            // A reference to the object is already in one of the live map.
                            TeleError.check(ref.status().isLive());
                        } else if (objects().isPlausibleOriginUnsafe(origin)) {
                            ref = GenSSRemoteReference.createLive(this, origin, false);
                            oldToSpaceRefMap.put(origin, ref);
                        }
                        break;
                    }
                    // We will check against the allocator, make sure we put the reference in the overflow map.
                    oldAllocatorRefMap = overflowRefMap;
                }
                // There are no young object while in the reclaiming phase.
                if (oldAllocator.containsInAllocated(origin)) {
                    ref = oldAllocatorRefMap.get(origin);
                    if (ref != null) {
                        // A reference to the object is already in one of the live map.
                        TeleError.check(ref.status().isLive());
                    } else if (objects().isPlausibleOriginUnsafe(origin)) {
                        ref = GenSSRemoteReference.createLive(this, origin, false);
                        oldAllocatorRefMap.put(origin, ref);
                    }
                }
                break;
            case ANALYZING:
                if (isFullGC()) {
                    /*
                     * Full GC. Nursery is empty.
                     */
                    if (oldSurvivorRange.contains(origin) || oldOverflowRange.contains(origin)) {
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
                    if (nursery.containsInAllocated(origin)) {
                        ref = makeForwardedReference(origin, nurseryRefMap, promotedRefMap, true);
                        break;
                    }
                    boolean isPromoted = youngSurvivorRange.contains(origin) || youngOverflowRange.contains(origin);
                    boolean isOldLive = oldTo.containsInAllocated(origin) && origin.lessThan(youngSurvivorRange.start());
                    if (isPromoted) {
                        ref = promotedRefMap.get(origin);
                        if (ref != null) {
                            TeleError.check(ref.status().isLive() && !ref.status().isForwarder());
                        } else if (objects().isPlausibleOriginUnsafe(origin)) {
                            ref = GenSSRemoteReference.createOldTo(this, origin, true);
                            promotedRefMap.put(origin, ref);
                        }
                    } else if (isOldLive) {
                        ref = oldToSpaceRefMap.get(origin);
                        if (ref != null) {
                            // A reference to the object is already in one of the live map.
                            TeleError.check(ref.status().isLive());
                        } else if (objects().isPlausibleOriginUnsafe(origin)) {
                            ref = GenSSRemoteReference.createLive(this, origin, false);
                            oldToSpaceRefMap.put(origin, ref);
                        }
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
        private RemoteReference oldSpaceReference = referenceManager().zeroReference();
        private RemoteReference resizingPolicyReference = referenceManager().zeroReference();

        public TeleGenSSHeapScheme(TeleVM vm, RemoteReference reference) {
            super(vm, reference);
        }

        public TeleCardTableRSet readTeleCardTableRSet() {
            final RemoteReference cardTableRSetReference = fields().GenSSHeapScheme_cardTableRSet.readRemoteReference(reference());
            if (cardTableRSetReference.isZero()) {
                return null;
            }
            return  (TeleCardTableRSet) objects().makeTeleObject(cardTableRSetReference);
        }

        public TeleContiguousHeapSpace readTeleYoungSpace() {
            final RemoteReference youngSpaceReference = fields().GenSSHeapScheme_youngSpace.readRemoteReference(reference());
            if (youngSpaceReference.isZero()) {
                return null;
            }
            return (TeleContiguousHeapSpace) objects().makeTeleObject(fields().ContiguousAllocatingSpace_space.readRemoteReference(youngSpaceReference));
        }

        public TeleContiguousHeapSpace readTeleOldToSpace() {
            if (oldSpaceReference.isZero()) {
                oldSpaceReference = fields().GenSSHeapScheme_oldSpace.readRemoteReference(reference());
                if (oldSpaceReference.isZero()) {
                    return null;
                }
            }
            return (TeleContiguousHeapSpace) objects().makeTeleObject(fields().ContiguousAllocatingSpace_space.readRemoteReference(oldSpaceReference));
        }

        public TeleContiguousHeapSpace readTeleOldFromSpace() {
            if (oldSpaceReference.isZero()) {
                oldSpaceReference = fields().GenSSHeapScheme_oldSpace.readRemoteReference(reference());
                if (oldSpaceReference.isZero()) {
                    return null;
                }
            }
            return (TeleContiguousHeapSpace) objects().makeTeleObject(fields().ContiguousSemiSpace_fromSpace.readRemoteReference(oldSpaceReference));
        }

        private TeleBaseAtomicBumpPointerAllocator readTeleBumpAllocator(TeleInstanceReferenceFieldAccess spaceFieldAccess) {
            RemoteReference spaceReference = spaceFieldAccess.readRemoteReference(reference());
            if (spaceReference.isZero()) {
                return null;
            }
            return (TeleBaseAtomicBumpPointerAllocator)  objects().makeTeleObject(fields().ContiguousAllocatingSpace_allocator.readRemoteReference(spaceReference));
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

        public RemoteReference oldSpaceEvacuator() {
            return fields().GenSSHeapScheme_oldSpaceEvacuator.readRemoteReference(reference());
        }

        public RemoteReference youngSpaceEvacuator() {
            return fields().GenSSHeapScheme_youngSpaceEvacuator.readRemoteReference(reference());
        }

        /**
         * Return the initial evacuation mark of evacuator.
         * @return an address in the To-space of the old generation
         */
        public Address initialEvacuationMark(RemoteReference evacuator) {
            return  fields().EvacuatorToCardSpace_initialEvacuationMark.readWord(evacuator).asAddress();
        }

        /**
         * Return top-most allocated mark within the current evacuation buffer of the evacuator.
         * @return an address in the evacuation buffer of an evacuator
         */
        public Address ptop(RemoteReference evacuator) {
            return  fields().EvacuatorToCardSpace_ptop.readWord(evacuator).asAddress();
        }

        private RemoteReference resizingPolicy() {
            if (resizingPolicyReference.isZero()) {
                resizingPolicyReference = fields().GenSSHeapScheme_resizingPolicy.readRemoteReference(reference());
            }
            return resizingPolicyReference;
        }

        public boolean oldEvacuationOverflow() {
            if (resizingPolicy().isZero()) {
                return false;
            }
            return fields().GenSSHeapSizingPolicy_oldEvacuationOverflow.readBoolean(resizingPolicyReference);
        }

        public boolean minorEvacuationOverflow() {
            if (resizingPolicy().isZero()) {
                return false;
            }
            return fields().GenSSHeapSizingPolicy_minorEvacuationOverflow.readBoolean(resizingPolicyReference);
        }
    }
}
