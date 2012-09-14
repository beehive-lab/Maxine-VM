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
import com.sun.max.tele.heap.region.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.reference.ms.*;
import com.sun.max.tele.reference.ms.MSRemoteReference.RefStateCount;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.heap.gcx.ms.*;

/**
 * Inspection support specialized for the basic mark-sweep implementation of {@link HeapScheme}
 * in the VM.
 * <p>
 * This support will not function correctly unless the VM is built in DEBUG mode, in which the GC will
 * {@linkplain Memory#ZAPPED_MARKER zap} all memory in free space, with the exception of the two word
 * header used to describe each segment of free space as a pseudo-object instance of {@link HeapFreeChunk}.
 * It is further assumed that precise scanning is used during {@linkplain HeapPhase#RECLAIMING RECLAIMING},
 * in which implementation DarkMatter objects are specially zapped for convenient recognition of these
 * unreachable objects.</p>
 * <p>
 * For the purpose of exposition, assume two collections of references, indexed by memory location, which we'll
 * call <em>Maps</em>: one for <em>Objects</em> and one for <em>Free Space</em>.
 * These maps contain no {@linkplain ObjectStatus#DEAD DEAD} references except during a
 * GC {@linkplain HeapPhase#RECLAIMING RECLAIMING} phase.</p>
 * <p>
 * A reference encapsulates the <em>identity</em> of an object (whether legitimate or quasi-), so there may be
 * no more than one reference in the maps at any memory location.  This identity relation permits the distinction
 * between object and reference to be overlooked for brevity in the following description.</p>
 * <p>
 * <b>Implementation note:</b> the division of the references into two maps is purely conceptual for the purpose
 * of this description.  Since the reference {@linkplain MSRemoteReference states} are all distinct,
 * they could be kept in a single map, two maps, or even a separate map for each reference state.
 * <p>
 *
 * <b>{@link HeapPhase#MUTATING} Summary</b>
 * <p>
 * In this phase new objects are allocated by splitting (if needed) instances of the pseudo-object {@link HeapFreeChunk}.
 * New objects appear, and instances of {@link HeapFreeChunk} both appear and disappear.</p>
 * <ul>
 * <li><b>Objects:</b>
 * <ol>
 * <li>The Object Map contains only object references that are {@linkplain ObjectStatus#LIVE LIVE}.</li>
 * <li>New {@linkplain ObjectStatus#LIVE LIVE} objects may be discovered and added to the Object Map.</li>
 * <li>No other changes to the Object Map take place during the {@linkplain HeapPhase#MUTATING MUTATING} phase.</li>
 * </ol></li>
 * <li><b>Free Space:</b>
 * <ol>
 * <li>The Free Space map contains only contain pseudo-object references that are {@linkplain ObjectStatus#LIVE LIVE}.
 * Some are pseudo-object instances of {@link HeapFreeChunk}; the rest are unreachable ordinary objects that
 * are unmodified, but implicitly considered <em>Dark Matter</em> by the GC.</li>
 * <li>New instances of {@link HeapFreeChunk} may be discovered and added to the Free Space Map.</li>
 * <li>New unreachable objects marked as <em>DarkMatter</em> may be discovered and added to the Free Space Map.</li>
 * <li>Existing instances of {@link HeapFreeChunk} in the map may be discovered, by observing that their hub pointer
 * has changed, to have been <em>released</em>: replaced by an object allocation. Such references become
 * {@linkplain ObjectStatus#DEAD DEAD} and are removed from the Free Space Map.</li>
 * <li>Existing instances of {@link HeapFreeChunk} in the map will not change in size, on the assumption that allocation
 * out of a {@link HeapFreeChunk} always happens at the beginning.</li>
 * <li>Existing instances of <em>Dark Matter</em> in the map may be discovered, by observing that they are no longer
 * specially marked, to have been <em>released</em>: replaced by an object allocation.
 * Such references become {@linkplain ObjectStatus#DEAD DEAD} and are removed from the Free Space Map.</li>
 * <li>No other changes to the Free Space Map take place during the {@linkplain HeapPhase#MUTATING MUTATING} phase.</li>
 * </ol></li></ul>
 * <p>
 *
 * <b>{@link HeapPhase#ANALYZING} Summary</b>
 * <p>
 * In this phase the only observable change is the transition of some heap locations from <em>unmarked</em> to
 * <em>marked</em>.
 * <ul>
 * <li><b>Objects:</b>
 * <ol>
 * <li>The Object Map contains only object references that are either {@linkplain ObjectStatus#LIVE LIVE} or
 * {@linkplain ObjectStatus#UNKNOWN UNKNOWN}.</li>
 * <li>New objects may be discovered and added to the Object Map; if they are <em>marked</em> then they are
 * {@linkplain ObjectStatus#LIVE LIVE} and if not they are {@linkplain ObjectStatus#UNKNOWN UNKNOWN}.</li>
 * <li>Existing {@linkplain ObjectStatus#UNKNOWN UNKNOWN} objects in the map may be discovered, by checking
 * their <em>marks</em> to have been determined <em>reachable</em>.
 * Such references become {@linkplain ObjectStatus#LIVE LIVE}.</li>
 * <li>No other changes to the Object Map take place during the {@linkplain HeapPhase#ANALYZING ANALYZING} phase.</li>
 * </ol></li>
 * <li><b>Free Space:</b>
 * <ol>
 * <li>The Free Space map contains only pseudo-object references that are {@linkplain ObjectStatus#LIVE LIVE}.
 * Some are pseudo-object instances of {@link HeapFreeChunk}; the rest are unreachable ordinary objects that
 * are unmodified, but implicitly considered <em>Dark Matter</em> by the GC.</li>
 * <li>New instances of {@link HeapFreeChunk} may be discovered and added to the Free Space Map.</li>
 * <li>New unreachable objects marked as <em>Dark Matter</em> maybe be discovered and added to the Free Space Map.</li>
 * <li>No other changes to the Free Space Map take place during the {@linkplain HeapPhase#ANALYZING ANALYZING} phase.</li>
 * </ol></li></ul>
 * <p>
 *
 * <b>{@link HeapPhase#RECLAIMING} Summary</b>
 * <p>
 * In this phase unreachable (<em>unmarked</em>) objects <em>larger</em> than maximum size threshold for <em>Dark Matter</em>
 * are either converted to instances of {@link HeapFreeChunk} or consolidated with adjacent instances of {@link HeapFreeChunk}.
 * Some objects disappear, and instances of {@link HeapFreeChunk} both appear and disappear.
 * Unreachable (<em>unmarked</em>) objects <em>smaller</em> than maximum size threshold for <em>Dark Matter</em>
 * are ignored and implicitly become <em>Dark Matter</em>.</p>
 * <ul>
 * <li><b>Objects:</b>
 * <ol>
 * <li>The Object Map contains only object references that are either {@linkplain ObjectStatus#LIVE LIVE} or
 * {@linkplain ObjectStatus#DEAD DEAD}, the latter case occurring <em>only</em> when a previously {@linkplain ObjectStatus#LIVE LIVE}
 * object was discovered to be unreachable during the immediately preceding {@linkplain HeapPhase#ANALYZING ANALYZING} phase.</li>
 * <li>New objects may be discovered and added to the Object Map as {@linkplain ObjectStatus#LIVE LIVE}, but only
 * if they are <em>marked</em>; an <em>unmarked</em> location is not considered to be an object.</li>
 * <li>An existing {@linkplain ObjectStatus#DEAD DEAD} object in the Object Map might be discovered, by observing zapped memory,
 * to have been <em>released</em> through consolidation with another instance of {@link HeapFreeChunk}.
 * Such a reference is removed from the Object Map.</li>
 * <li>An existing {@linkplain ObjectStatus#DEAD DEAD} object in the Object Map might be discovered, by observing its hub pointer,
 * to have been <em>released</em> through <em>conversion</em> to an instance of {@link HeapFreeChunk}.
 * Such a reference is removed from the Object Map.</li>
 * <li>At the end of the {@linkplain HeapPhase#RECLAIMING RECLAIMING} phase, every {@linkplain ObjectStatus#DEAD DEAD}
 * object in the Object Map is presumed to be <em>Dark Space</em>.
 * Such a reference is removed from the Object Map and added to the Free Space Map.</li>
 * <li>No other changes to the Object Map take place during the {@linkplain HeapPhase#RECLAIMING RECLAIMING} phase.</li>
 * </ol></li>
 * <li><b>Free Space:</b>
 * <ol>
 * <li>The Free Space map contains only pseudo-object references that are {@linkplain ObjectStatus#LIVE LIVE}.
 * Some are pseudo-object instances of {@link HeapFreeChunk}; the rest are unreachable ordinary objects that
 * are unmodified, but implicitly considered <em>Dark Matter</em> by the GC.</li>
 * <li>New instances of {@link HeapFreeChunk} may be discovered and added to the Free Space Map.</li>
 * <li>New unreachable objects marked as <em>Dark Matter</em> maybe be discovered and added to the Free Space Map.</li>
 * <li>An existing instance of {@link HeapFreeChunk} in the map may be discovered to have changed in size through
 * consolidation with following space.</li>
 * <li>An existing instance of {@link HeapFreeChunk} in the map might be discovered, by observing zapped memory,
 * to have been <em>released</em> through consolidation with another instance of {@link HeapFreeChunk}.
 * Such a reference is removed from the Free Space Map.</li>
 * <li>No other changes to the Free Space Map take place during the {@linkplain HeapPhase#ANALYZING ANALYZING} phase.</li>
 * <li><b>Note:</b>  it might be useful to force the <em>Dark Space</em> references to be kept, even if the WeakReference map
 * might otherwise lose them; they contain information that cannot be reconstructed.</li>
 * </ol></li></ul>
 * <p>

 * @see HeapFreeChunk
 * @see Memory#ZAPPED_MARKER
 * @see Sweeper#minReclaimableSpace()
 * @see MSHeapScheme
 */
public final class RemoteMSHeapScheme extends AbstractRemoteHeapScheme implements RemoteObjectReferenceManager {

    private static final int TRACE_VALUE = 1;

    private final TimedTrace heapUpdateTracer;

    private long lastUpdateEpoch = -1L;
    private long lastAnalyzingPhaseCount = 0L;
    private long lastReclaimingPhaseCount = 0L;

    /**
    * The VM object that implements the {@link HeapScheme} in the current configuration.
    */
    private TeleMSHeapScheme scheme;

    /**
     * The VM object that describes the location of the collector's Object-Space.
     */
    private TeleContiguousHeapSpace objectSpaceMemoryRegion = null;

    /**
     * Map:  VM address in Object-Space --> a {@link MSRemoteReference} that refers to the object whose origin is at that location.
     */
    private WeakRemoteReferenceMap<MSRemoteReference> objectRefMap = new WeakRemoteReferenceMap<MSRemoteReference>();

    /**
     * Map:  VM address in Object-Space --> a {@link MSRemoteReference} that refers to the free space chunk whose origin is at that location.
     */
    private WeakRemoteReferenceMap<MSRemoteReference> freeSpaceRefMap = new WeakRemoteReferenceMap<MSRemoteReference>();

    private final List<VmHeapRegion> heapRegions = new ArrayList<VmHeapRegion>(1);

    protected RemoteMSHeapScheme(TeleVM vm) {
        super(vm);
        this.heapUpdateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + "updating");

        if (!vm.bootImage().vmConfiguration.debugging()) {
            final StringBuilder sb = new StringBuilder();
            sb.append(tracePrefix());
            sb.append(": specialized inspector heap support for " + heapSchemeClass().getSimpleName());
            sb.append(" will not work correctly; DEBUG boot image required");
            TeleWarning.message(sb.toString());
        }
        // TODO (mlvdv) handle attach mode, where the memory region will already be allocated and we need to know about it right away
    }

    public Class heapSchemeClass() {
        return MSHeapScheme.class;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Include the array of longs that holds the mark bitmap data.
     */
    @Override
    public List<MaxObject> inspectableObjects() {
        List<MaxObject> inspectableObjects = new ArrayList<MaxObject>();
        inspectableObjects.addAll(super.inspectableObjects());
        if (scheme.markBitmap != null) {
            final MaxObject representation = scheme.markBitmap.representation();
            if (representation != null) {
                inspectableObjects.add(representation);
            }
        }
        return inspectableObjects;
    }

    public void initialize(long epoch) {
        vm().addInitializationListener(new InitializationListener() {

            public void initialiationComplete(final long initializationEpoch) {
                objects().registerTeleObjectType(MSHeapScheme.class, TeleMSHeapScheme.class);
                // Get the VM object that represents the heap implementation; can't do this any sooner during startup.
                scheme = (TeleMSHeapScheme) teleHeapScheme();
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


    // TODO (mlvdv) Consider whether we should periodically purge the maps of weak references to references that have been collected.

    // TODO (mlvdv) Consider whether to replace the objectRefMap with separate maps:  liveObjectRefMap and unknownObjectRefMap

    /**
     * {@inheritDoc}
     * <p>
     * This gets called more than once during the startup sequence.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void updateMemoryStatus(long epoch) {

        super.updateMemoryStatus(epoch);
        updateHeapFreeChunkHubOrigin();
        if (scheme == null) {
            // Can't do anything until we have the VM object that represents the scheme implementation
            return;
        }
        scheme.updateCacheIfNeeded();
        if (objectSpaceMemoryRegion == null) {
            Trace.begin(TRACE_VALUE, tracePrefix() + "looking for heap region");
            /*
             * The heap region has not yet been discovered. Don't check the epoch, since this check may need to be run
             * more than once during the startup sequence, as the information needed for access to the information is
             * incrementally established. Information about the region, other then location need not be checked once
             * established.
             */
            objectSpaceMemoryRegion = scheme.objectSpace.contiguousHeapSpace();
            if (objectSpaceMemoryRegion != null) {
                final VmHeapRegion vmHeapRegion = new VmHeapRegion(vm(), objectSpaceMemoryRegion, this);
                heapRegions.add(vmHeapRegion);
                vm().addressSpace().add(vmHeapRegion.memoryRegion());
            }

            Trace.end(TRACE_VALUE, tracePrefix() + "looking for heap region: " + (heapRegions.isEmpty() ? "not" : "") + " found");
        }
        if (objectSpaceMemoryRegion != null && epoch > lastUpdateEpoch) {

            heapUpdateTracer.begin();

            /*
             * This is a normal refresh. Immediately update information about the location/size/allocation of the heap region; this
             * update must be forced because remote objects are otherwise not refreshed until later in the update
             * cycle.
             */
            objectSpaceMemoryRegion.updateCache(epoch);

            /*
             * Before doing anything else, update the free space references to see if any have become dead
             */
            // TODO (mlvdv) is this needed during the ANALYZING phase?
            for (MSRemoteReference freeSpaceRef : freeSpaceRefMap.values()) {
                if (!freeSpaceRef.isFreeSpace()) {
                    // The reference no longer points at a free space chunk
                    assert freeSpaceRefMap.remove(freeSpaceRef.origin()) != null;
                    freeSpaceRef.die();
                }
            }
            if (phase().isCollecting()) {

                // TODO (mlvdv) in the most common case, where we only take the pre-defined break during each GC cycle,
                // then we iterate over the object references three times.  This might be reduced to two, or even one,
                // using a kind of fast-path approach but it would make the logic less clear.  Debug first, then revisit this possibility.

                /*
                 * Check first to see if a GC cycle has started since the last time we looked. If so, then any live objects
                 * must transition state before before examining what has happened since the the cycle started.
                 */
                if (lastAnalyzingPhaseCount < gcStartedCount()) {
                    Trace.begin(TRACE_VALUE, tracePrefix() + "first halt in GC cycle=" + gcStartedCount());
                    assert lastAnalyzingPhaseCount == gcStartedCount() - 1;
                    final List<MSRemoteReference> values = objectRefMap.values();
                    for (MSRemoteReference objectRef : values) {
                        objectRef.analyzingBegins();
                    }
                    Trace.end(TRACE_VALUE, tracePrefix() + "first halt in GC cycle=" + gcStartedCount() + ", UNKNOWN refs=" + values.size());
                    lastAnalyzingPhaseCount = gcStartedCount();
                }

                /*
                 * Check to see if any objects have been marked since we last looked.  This can happen
                 * at any time during the analyzing phase, but we have to check one more time when we hit the reclaiming
                 * phase.
                 */
                if (lastReclaimingPhaseCount < gcStartedCount()) {
                    // The transition to reclaiming hasn't yet been processed, so check for any newly marked references.
                    Trace.begin(TRACE_VALUE, tracePrefix() + "checking Object refs, GC cycle=" + gcStartedCount());
                    int live = 0;
                    int markedLive = 0;
                    for (MSRemoteReference objectRef : objectRefMap.values()) {
                        switch (objectRef.status()) {
                            case UNKNOWN:
                                if (true) { // TODO (mlvdv) if object is marked in the MBM, not implemented yet
                                    // An object has been marked since the last time we looked, transition back to LIVE
                                    objectRef.discoveredReachable();
                                    markedLive++;
                                }
                                break;
                            case LIVE:
                                // Do nothing; already live and in the objectMap
                                live++;
                                break;
                            case DEAD:
                                TeleError.unexpected(tracePrefix() + "DEAD reference found in Object map");
                                break;
                            default:
                                TeleError.unknownCase();
                        }
                    }
                    Trace.end(TRACE_VALUE, tracePrefix() + "checking Object refs, GC cycle=" + gcStartedCount()
                                    + " live=" + (live + markedLive) + "(old=" + live + ", new=" + markedLive + ")");
                }

                if (phase().isReclaiming() && lastReclaimingPhaseCount < gcStartedCount()) {
                    /*
                     * The heap is in a GC cycle, and this is the first VM halt during that GC cycle where we know
                     * analysis is complete. This halt will usually be caused by the special breakpoint we've set at
                     * entry to the {@linkplain #RECLAIMING} phase.  This is the opportunity
                     * to update reference maps while full information is still available in the collector.
                     */
                    Trace.begin(TRACE_VALUE, tracePrefix() + "first halt in GC RECLAIMING, cycle=" + gcStartedCount());
                    assert lastReclaimingPhaseCount == gcStartedCount() - 1;
                    lastReclaimingPhaseCount = gcStartedCount();
                    for (MSRemoteReference objectRef : objectRefMap.values()) {
                        switch (objectRef.status()) {
                            case UNKNOWN:
                                // The object is unreachable.
                                assert objectRefMap.remove(objectRef.origin()) != null;
                                objectRef.die();
                                break;
                            case LIVE:
                                // Do nothing; already live and in the objectMap
                                break;
                            case DEAD:
                                TeleError.unexpected(tracePrefix() + "DEAD reference found in Object map");
                                break;
                            default:
                                TeleError.unknownCase();
                        }
                    }
                }
                lastUpdateEpoch = epoch;
                heapUpdateTracer.end(heapUpdateStatsPrinter);
            }
        }
    }


    // TODO (mlvdv)  fix
    public MaxMemoryManagementInfo getMemoryManagementInfo(final Address address) {
        return new MaxMemoryManagementInfo() {

            public MaxMemoryStatus status() {
                // TODO (mlvdv) ensure the location is in one of the regions being managed.
                final MaxHeapRegion heapRegion = heap().findHeapRegion(address);
                if (heapRegion == null) {
                    // The location is not in any memory region allocated by the heap.
                    return MaxMemoryStatus.UNKNOWN;
                }

                // Unclear what the semantics of this should be during GC.
                // We should be able to tell past the marking phase if an address point to a live object.
                // But what about during the marking phase ? The only thing that can be told is that
                // what was dead before marking begin should still be dead during marking.

                // TODO (ld) This requires the inspector to know intimately about the heap structures.
                // The current MS scheme  linearly allocate over chunk of free space discovered during the past MS.
                // However, it doesn't maintain these as "linearly allocating memory region". This could be done by formatting
                // all reusable free space as such (instead of the chunk of free list as is done now). in any case.

                return MaxMemoryStatus.LIVE;
            }

            public String terseInfo() {
                return "";
            }

            public String shortDescription() {
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

    @Override
    public boolean hasMarkBitmap() {
        return true;
    }

    @Override
    public MaxMarkBitmap markBitMap() {
        return scheme.markBitmap;
    }

    // TODO (mlvdv) refine
    public ObjectStatus objectStatusAt(Address origin) throws TeleError {
        TeleError.check(contains(origin), "Location is outside MS heap region");

        switch(phase()) {
            case MUTATING:
            case RECLAIMING:
            case ANALYZING:
                if (objectSpaceMemoryRegion.containsInAllocated(origin) && objects().isPlausibleOriginUnsafe(origin)) {
                    if (isHeapFreeChunkOrigin(origin)) {
                        return ObjectStatus.FREE;
                    }
                    return ObjectStatus.LIVE;
                }
                break;
            default:
                TeleError.unknownCase();
        }
        return ObjectStatus.DEAD;
    }

    public boolean isForwardingAddress(Address forwardingAddress) {
        // TODO Auto-generated method stub
        return false;
    }

    // TODO (mlvdv) refine; only handles live object now, doesn't support collection.
    public RemoteReference makeReference(Address origin) throws TeleError {
        assert vm().lockHeldByCurrentThread();
        // It is an error to attempt creating a reference if the address is completely outside the managed region(s).
        TeleError.check(contains(origin), "Location is outside MS heap region");
        MSRemoteReference remoteReference = null;
        switch(phase()) {
            case MUTATING:
            case RECLAIMING:
            case ANALYZING:
                remoteReference = objectRefMap.get(origin);
                if (remoteReference != null) {
                    // A reference to the object is already in the Object-Space map.
                    TeleError.check(remoteReference.status().isLive());
                } else if (objectSpaceMemoryRegion.containsInAllocated(origin)
                                && objects().isPlausibleOriginUnsafe(origin)
                                && !isHeapFreeChunkOrigin(origin)) {
                    // A newly discovered object in the allocated area of To-Space; add a new reference to the To-Space map.
                    remoteReference = MSRemoteReference.createLive(this, origin);
                    objectRefMap.put(origin, remoteReference);
                }
                break;
            default:
                TeleError.unknownCase();
        }
        return remoteReference;
    }

    // TODO (mlvdv) refine, e.g. for HeapFreeChunks and possibly others.
    public RemoteReference makeQuasiReference(Address origin) throws TeleError {
        assert vm().lockHeldByCurrentThread();
        // It is an error to attempt creating a reference if the address is completely outside the managed region(s).
        TeleError.check(contains(origin), "Location is outside MS heap region");
        MSRemoteReference remoteReference = null;
        switch(phase()) {
            case MUTATING:
            case RECLAIMING:
            case ANALYZING:
                remoteReference = objectRefMap.get(origin);
                if (remoteReference != null) {
                    // A reference to the object is already in the Object-Space map.
                    TeleError.check(remoteReference.status().isQuasi());
                } else if (objectSpaceMemoryRegion.containsInAllocated(origin)
                                && objects().isPlausibleOriginUnsafe(origin)
                                && isHeapFreeChunkOrigin(origin)) {
                    // A newly discovered object in the allocated area of To-Space; add a new reference to the To-Space map.
                    remoteReference = MSRemoteReference.createFree(this, origin);
                    objectRefMap.put(origin, remoteReference);
                }
                break;
            default:
                TeleError.unknownCase();
        }
        return remoteReference;
    }

    /**
     * Does the heap region contain the address anywhere?
     */
    private boolean contains(Address address) {
        return objectSpaceMemoryRegion != null && objectSpaceMemoryRegion.contains(address);
    }

    /**
     * Is the address in an area where an object could be either
     * {@linkplain ObjectStatus#LIVE LIVE} or
     * {@linkplain ObjectStatus#UNKNOWN UNKNOWN}.
     */
    private boolean inLiveArea(Address address) {
        // TODO (mlvdv) refine
        return objectSpaceMemoryRegion.containsInAllocated(address);
    }

    /**
     * Checks whether the origin of a reference is currently the origin of an instance
     * of {@link HeapFreeChunk} in VM memory.
     *
     * @param ref a reference, presumably once known to be the origin of a {@link HeapFreeChunk}
     * @return whether the reference points at a {@link HeapFreeChunk}
     */
    protected boolean isFreeSpace(RemoteReference ref) {
        final Address hubOrigin = ref.readHubAsWord().asAddress();
        return heapFreeChunkHubOrigin.isNotZero() && hubOrigin.equals(heapFreeChunkHubOrigin);
    }

    @SuppressWarnings("deprecation")
    private void printRegionObjectStats(PrintStream printStream, int indent, boolean verbose, TeleMemoryRegion region, WeakRemoteReferenceMap<MSRemoteReference> map) {
        final NumberFormat formatter = NumberFormat.getInstance();
        int totalRefs = 0;
        int liveRefs = 0;
        int unknownRefs = 0;
        int deadRefs = 0;

        for (MSRemoteReference ref : map.values()) {
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
        sb0.append(heapRegions.get(0).entityName());
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
            for (RefStateCount refStateCount : MSRemoteReference.getStateCounts(map.values())) {
                if (refStateCount.count > 0) {
                    printStream.println(stateIndentation + refStateCount.stateName + ": " + formatter.format(refStateCount.count));
                }
            }
        }
    }


    public void printObjectSessionStats(PrintStream printStream, int indent, boolean verbose) {
        if (objectSpaceMemoryRegion != null) {
            final NumberFormat formatter = NumberFormat.getInstance();

            final int totalRefs = objectRefMap.values().size();

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
            sb1.append(", total object refs mapped=").append(formatter.format(totalRefs));
            printStream.println(indentation + sb1.toString());

            printRegionObjectStats(printStream, indent + 4, verbose, objectSpaceMemoryRegion, objectRefMap);

        }
    }

    /**
     * Surrogate object for the scheme instance in the VM.
     */
    public static class TeleMSHeapScheme extends TeleHeapScheme {

        private TeleFreeHeapSpaceManager objectSpace;
        private TeleTricolorHeapMarker remoteHeapMarker;

        private VmMarkBitmap markBitmap = null;

        public TeleMSHeapScheme(TeleVM vm, RemoteReference reference) {
            super(vm, reference);
        }

        @Override
        protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
            if (!super.updateObjectCache(epoch, statsPrinter)) {
                return false;
            }

            if (objectSpace == null) {
                final RemoteReference freeHeapSpaceManagerRef = fields().MSHeapScheme_objectSpace.readRemoteReference(reference());
                objectSpace = (TeleFreeHeapSpaceManager) objects().makeTeleObject(freeHeapSpaceManagerRef);
            } else {
                objectSpace.updateCacheIfNeeded();
            }

            if (remoteHeapMarker == null) {
                // Allocated in boot heap, so it exists the first time we check.
                final RemoteReference heapMarkerRef = fields().MSHeapScheme_heapMarker.readRemoteReference(reference());
                remoteHeapMarker = (TeleTricolorHeapMarker) objects().makeTeleObject(heapMarkerRef);
            } else {
                remoteHeapMarker.updateCacheIfNeeded();
            }
            // assert remoteHeapmarker != null
            if (markBitmap == null && remoteHeapMarker.isAllocated()) {
                markBitmap = new VmMarkBitmap(vm(), remoteHeapMarker);
                vm().addressSpace().add(markBitmap.memoryRegion());
            }
            return true;
        }
    }

}
