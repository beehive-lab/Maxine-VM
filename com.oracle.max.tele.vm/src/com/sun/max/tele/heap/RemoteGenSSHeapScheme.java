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
import com.sun.max.tele.heap.gen.semispace.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
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
    private static final int MAX_VM_HEAP_REGIONS = 3;
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

    private void addHeapRegion(TeleContiguousHeapSpace contiguousHeapSpace) {
        if (contiguousHeapSpace != null) {
            final VmHeapRegion vmHeapRegion = new VmHeapRegion(vm(), contiguousHeapSpace, this);
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
            oldTo = scheme.readTeleOldFromSpace();
            addHeapRegion(oldTo);
        }
        if (cardTableRSet == null) {
            cardTableRSet = scheme.readTeleCardTableRSet();
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

    private void analysisBegin(String action, WeakRemoteReferenceMap<GenSSRemoteReference> refMap) {
        final boolean minorCollection = !isFullGC();
        final String prefix =  tracePrefix() + "first halt in " + (minorCollection ? "MINOR" : "FULL") + " GC cycle=" + gcStartedCount() + ", ";
        Trace.begin(TRACE_VALUE,  prefix + action);
        for (GenSSRemoteReference ref : refMap.values()) {
            ref.analysisBegins(minorCollection);
        }
        Trace.end(TRACE_VALUE, prefix +  "UNKNOWN refs=" + refMap.size());
    }

    private void updateForwardedReferences(WeakRemoteReferenceMap<GenSSRemoteReference> fromSpaceMap, WeakRemoteReferenceMap<GenSSRemoteReference> toSpaceMap) {
        final boolean minorCollection = !isFullGC();
        final String prefix =  tracePrefix() + "checking forwarding refs, " + (minorCollection ? "MINOR" : "FULL") +  " GC cycle=" + gcStartedCount();
        int forwarded = 0;
        int live = 0;

        Trace.begin(TRACE_VALUE, prefix);
        for (GenSSRemoteReference ref : fromSpaceMap.values()) {
            switch(ref.status()) {
                case UNKNOWN:
                    if (objects().hasForwardingAddressUnsafe(ref.origin())) {
                        // A From-Space reference (either to the nursery if doing minor collection, or old from if doing full collection)  has been forwarded since the last time we looked:
                        // transition state and add to To-Space map (i.e., either the promoted map or the old to space map)..
                        final Address toOrigin = objects().getForwardingAddressUnsafe(ref.origin());
                        ref.addToOrigin(toOrigin, minorCollection);
                        toSpaceMap.put(toOrigin, ref);
                        forwarded++;
                    }
                    break;
                case LIVE:
                    live++;
                    break;
                case DEAD:
                    TeleError.unexpected(tracePrefix() + "DEAD reference found in From-Space map");
                    break;
                default:
                    TeleError.unknownCase();
            }
        }
        Trace.end(TRACE_VALUE, prefix + " forwarded=" + forwarded + "(before=" + live + ",  after=" + (live + forwarded) + ")");
    }

    private void updatedReclaimedReference(WeakRemoteReferenceMap<GenSSRemoteReference> fromSpaceMap, WeakRemoteReferenceMap<GenSSRemoteReference> toSpaceMap) {
        final boolean minorCollection = !isFullGC();
        final String prefix =  tracePrefix() + "first halt in  RECLAIMING phase of " + (minorCollection ? "MINOR" : "FULL") +  " GC cycle=" + gcStartedCount();
        Trace.begin(TRACE_VALUE, prefix + " -- clear forwarded & remove dead references");
        for (GenSSRemoteReference ref : toSpaceMap.values()) {
            switch (ref.status()) {
                case LIVE:
                    ref.analysisEnds(minorCollection);
                    break;
                case UNKNOWN:
                    TeleError.unexpected(tracePrefix() + "UNKNOWN reference found in " + (minorCollection ? "promoted space" : "old to-space") + " map");
                    break;
                case DEAD:
                    TeleError.unexpected(tracePrefix() + "DEAD reference found in  " +  (minorCollection ? "promoted space" : "old to-space") + " map");
                    break;
                default:
                    TeleError.unknownCase();
            }
        }
        int forwarded = 0;
        int died = 0;
        for (GenSSRemoteReference ref : fromSpaceMap.values()) {
            switch (ref.status()) {
                case LIVE:
                    forwarded++;
                    break;
                case UNKNOWN:
                    ref.analysisEnds(minorCollection);
                    died++;
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
    }

    @Override
    public List<VmHeapRegion> heapRegions() {
        return heapRegions;
    }

    @Override
    public void updateMemoryStatus(long epoch) {
        super.updateMemoryStatus(epoch);
        // Can't do anything until we have the VM object that represents the scheme implementation
        if (scheme == null) {
            return;
        }
        // FIXME: will need to take region(s) of the card table RSet into account
        if (heapRegions.size() < MAX_VM_HEAP_REGIONS) {
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
                    if (isFullGC()) {
                        final WeakRemoteReferenceMap<GenSSRemoteReference> tempRefMap = oldToSpaceRefMap;
                        oldFromSpaceRefMap = oldToSpaceRefMap;
                        oldToSpaceRefMap = tempRefMap;
                        // Transition the state of all references that are now in the old from-Space
                        analysisBegin("flip old generation semi spaces", oldFromSpaceRefMap);
                    } else {
                        analysisBegin("make all young refs unknown", nurseryRefMap);
                    }
                    lastAnalyzingPhaseCount = gcStartedCount();
                }
                /*
                 * If we're still analyzing, we need to update references to objects that may have been forwarded during this GC cycle.
                 */
                if (lastReclaimingPhaseCount < gcStartedCount()) {
                    if (isFullGC()) {
                        updateForwardedReferences(oldFromSpaceRefMap, oldToSpaceRefMap);
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
                        updatedReclaimedReference(nurseryRefMap, promotedRefMap);
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

    @Override
    public boolean isObjectOrigin(Address origin) throws TeleError {
        TeleError.check(contains(origin), "Location is outside GenSSHeapScheme regions");
        switch(phase()) {
            case MUTATING:
                return  (oldAllocator.containsInAllocated(origin) || nurseryAllocator.containsInAllocated(origin)) && objects().isPlausibleOriginUnsafe(origin);
            case ANALYZING:
                if (fullGC) {
                    if (oldFrom.containsInAllocated(origin)) {
                        if (objects().hasForwardingAddressUnsafe(origin)) {
                            final Address forwardAddress = objects().getForwardingAddressUnsafe(origin);
                            return oldAllocator.containsInAllocated(forwardAddress) && objects().isPlausibleOriginUnsafe(forwardAddress);
                        }
                        return objects().isPlausibleOriginUnsafe(origin);
                    }
                } else if (nursery.containsInAllocated(origin)) {
                    if (objects().hasForwardingAddressUnsafe(origin)) {
                        final Address forwardAddress = objects().getForwardingAddressUnsafe(origin);
                        return oldAllocator.containsInAllocated(forwardAddress) && objects().isPlausibleOriginUnsafe(forwardAddress);
                    }
                    return objects().isPlausibleOriginUnsafe(origin);
                }
                // Wasn't a forwarded object, or an object in either of the from space (nursery, old from).
                // The to-space for both minor and full collection is the old to space. So check it now.
                if (oldAllocator.containsInAllocated(origin)) {
                    return objects().isPlausibleOriginUnsafe(origin);
                }
                break;
            case RECLAIMING:
                // Whether full or minor collection, the nursery and old from space are always empty during the reclaiming phase.
                return oldAllocator.containsInAllocated(origin) && objects().isPlausibleOriginUnsafe(origin);
            default:
                TeleError.unknownCase();
        }
        return false;
    }

    public boolean isFreeSpaceOrigin(Address origin) throws TeleError {
        // FIXME: do we use HeapFreeChunk for anything ?
        return false;
    }

    @Override
    public RemoteReference makeReference(Address origin) throws TeleError {
        assert vm().lockHeldByCurrentThread();
        TeleError.check(contains(origin), "Location is outside of " + heapSchemeClass().getSimpleName() + " heap");
        GenSSRemoteReference ref = null;
        switch(phase()) {
            case MUTATING:
                ref = nurseryRefMap.get(origin);
                if (ref == null) {
                    ref = oldToSpaceRefMap.get(origin);
                }
                if (ref != null) {
                    // A reference to the object is already in one of the live map.
                    TeleError.check(ref.status().isLive());
                    return ref;
                }
                if (nurseryAllocator.containsInAllocated(origin)) {
                }
                if (oldAllocator.containsInAllocated(origin)) {

                }
                break;
            case ANALYZING:
            case RECLAIMING:

                break;
            default:
                TeleError.unknownCase();
        }
        return ref == null ? vm().referenceManager().zeroReference() : ref;
    }

    @Override
    public void printObjectSessionStats(PrintStream printStream, int indent, boolean verbose) {
        // TODO Auto-generated method stub

    }

    public static class TeleGenSSHeapScheme extends TeleHeapScheme {
        public TeleGenSSHeapScheme(TeleVM vm, Reference reference) {
            super(vm, reference);
        }

        public TeleCardTableRSet readTeleCardTableRSet() {
            return null;
        }

        public TeleContiguousHeapSpace readTeleYoungSpace() {
            final Reference youngSpaceReference = fields().GenSSHeapScheme_youngSpace.readReference(getReference());
            if (youngSpaceReference.isZero()) {
                return null;
            }
            return (TeleContiguousHeapSpace) objects().makeTeleObject(fields().ContiguousAllocatingSpace_space.readReference(youngSpaceReference));
        }

        public TeleContiguousHeapSpace readTeleOldToSpace() {
            final Reference oldSpaceReference = fields().GenSSHeapScheme_oldSpace.readReference(getReference());
            if (oldSpaceReference.isZero()) {
                return null;
            }
            return (TeleContiguousHeapSpace) objects().makeTeleObject(fields().ContiguousAllocatingSpace_space.readReference(oldSpaceReference));
        }

        public TeleContiguousHeapSpace readTeleOldFromSpace() {
            final Reference oldSpaceReference = fields().GenSSHeapScheme_oldSpace.readReference(getReference());
            if (oldSpaceReference.isZero()) {
                return null;
            }
            return (TeleContiguousHeapSpace) objects().makeTeleObject(fields().ContiguousSemiSpace_fromSpace.readReference(oldSpaceReference));
        }

        private TeleBaseAtomicBumpPointerAllocator readTeleBumpAllocator(TeleInstanceReferenceFieldAccess spaceFieldAccess) {
            Reference spaceReference = spaceFieldAccess.readReference(getReference());
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
            return fields().GenSSHeapScheme_fullCollectionCount.readInt(getReference());
        }
    }
}
