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
import com.sun.max.tele.MaxMarkBitmap.MarkColor;
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
 * <p>
 * Inspection support specialized for the basic {@linkplain MSHeapScheme mark-sweep implementation} of {@link HeapScheme}
 * in the VM.</p>
 * <p>
 * This support will not function correctly unless the VM is built in DEBUG mode, in which case the GC will
 * {@linkplain Memory#ZAPPED_MARKER zap} all memory in free space with one exception.  The GC formats each segment
 * of free space as a <em>quasi-object</em>, for example an instance of {@link HeapFreeChunk} or as some
 * kind of <em>dark matter</em> with an ordinary two-word object header. This formatting is needed, among
 * other reasons, to allow the GC to sweep all of memory.</p>
 * <p>
 * The term <em>quasi-object</em> refers to an area of memory formatted as if it were an ordinary Maxine
 * VM object, but which is
 * <ul>
 * <li>only known to the GC implementation;</li>
 * <li>is never given ordinary object behavior; and </li>
 * <li>is never reachable from any object roots.</li>
 * </ul></p>
 * <p>
 * It is further assumed that <em>precise scanning</em> is used during the GC
 * {@linkplain HeapPhase#RECLAIMING reclaiming} phase,
 * in which implementation <em>dark matter</em> objects, which are unreachable, are specially zapped and
 * formatted for convenient recognition.</p>
 * <p>
 * For the purpose of this exposition, assume two collections of references indexed by memory location,
 * which we'll call <em>Maps</em>: one for <em>Objects</em> and one for <em>Free Space</em>.
 * These maps never contain {@linkplain ObjectStatus#DEAD DEAD} references.</p>
 * <p>
 * A {@link RemoteReference} encapsulates the <em>identity</em> of an object (whether legitimate or quasi-).
 * By invariant, there may be no more than one reference in the the maps (collectively) at any memory location.
 * This identity relation permits the distinction
 * between object and reference to be blurred for brevity in the following description.</p>
 * <p>
 * <b>Implementation note:</b> the division of the references into two maps is purely conceptual for the purpose
 * of this description.  Since the reference {@linkplain MSRemoteReference states} are all distinct,
 * they could be kept in a single map, two maps, or even a separate map for each reference state.
 * <p>
 *
 * <b>{@link HeapPhase#MUTATING} Summary</b>
 * <p>
 * During this phase new objects are allocated by splitting (if needed) {@linkplain ObjectStatus#FREE FREE} quasi-object instances.
 * Newly allocated objects appear, and {@linkplain ObjectStatus#FREE FREE} quasi-objects both appear and disappear (are released).</p>
 * <ul>
 * <li><b>Object Map:</b>
 * <ol>
 * <li>The Object Map contains only {@linkplain ObjectStatus#LIVE LIVE} object references.</li>
 * <li>New {@linkplain ObjectStatus#LIVE LIVE} objects may be discovered and added to the Object Map.</li>
 * <li>No other changes to the Object Map take place during the {@linkplain HeapPhase#MUTATING mutating} phase.</li>
 * </ol></li>
 * <li><b>Free Space Map:</b>
 * <ol>
 * <li>The Free Space map contains only contain quasi-object references that are {@linkplain ObjectStatus#FREE FREE}
 * or {@linkplain ObjectStatus#DARK DARK}.</li>
 * <li>Previously unseen {@linkplain ObjectStatus#FREE FREE} quasi-objects, possibly newly created, may be discovered
 * and added to the Free Space Map.</li>
 * <li>Previously unseen {@linkplain ObjectStatus#DARK DARK} quasi-objects may be discovered and added to the
 * Free Space Map, but they do not change during this phase.</li>
 * <li>A {@linkplain ObjectStatus#FREE FREE} quasi-object reference in the map may be discovered, by observing that its hub pointer
 * has changed, to have been <em>released</em>: i.e. replaced by an object allocation. This reference becomes
 * {@linkplain ObjectStatus#DEAD DEAD} and is removed from the Free Space Map.</li>
 * <li>{@linkplain ObjectStatus#FREE FREE} quasi-objects in the map do not change in size, on the assumption that allocation
 * out of a {@linkplain ObjectStatus#FREE FREE} quasi-object always happens at the beginning,
 * so the {@linkplain ObjectStatus#FREE FREE} quasi-object becomes {@linkplain ObjectStatus#DEAD DEAD} and
 * another is created with the remaining space (if there is enough).</li>
 * <li>No other changes to the Free Space Map take place during the {@linkplain HeapPhase#MUTATING mutating} phase.</li>
 * </ol></li></ul>
 * <p>
 *
 * <b>{@link HeapPhase#ANALYZING} Summary</b>
 * <p>
 * During this phase the only observable change is the transition of some heap object locations from <em>unmarked</em> to
 * <em>marked</em>.  A <em>mark</em> records that the GC has determined the object to be <em>reachable</em>,
 * whereas an <em>unmarked</em> location signified an object whose reachability is unknown.</p>
 * <ul>
 * <li><b>Object Map:</b>
 * <ol>
 * <li>The Object Map contains only object references that are {@linkplain ObjectStatus#LIVE LIVE}, whether or not
 * the objects are <em>marked</em>.</li>
 * <li>New {@linkplain ObjectStatus#LIVE LIVE} objects may be discovered and added to the Object Map.</li>
 * <li>Unmarked objects in the map may become marked.</li>
 * <li>No other changes to the Object Map take place during the {@linkplain HeapPhase#ANALYZING analyzing} phase.</li>
 * </ol></li>
 * <li><b>Free Space Map:</b>
 * <ol>
 * <li>The Free Space map contains only quasi-objects that are {@linkplain ObjectStatus#FREE FREE} or
 * {@linkplain ObjectStatus#DARK DARK}.</li>
 * <li>Previously unseen {@linkplain ObjectStatus#FREE FREE} quasi-objects may be discovered
 * and added to the Free Space Map, but they do not change during this phase.</li>
 * <li>Previously unseen {@linkplain ObjectStatus#DARK DARK} quasi-objects may be discovered and added to the
 * Free Space Map, but they do not change during this phase.</li>
 * <li>No other changes to the Free Space Map take place during the {@linkplain HeapPhase#ANALYZING analyzing} phase.</li>
 * </ol></li></ul>
 * <p>
 *
 * <b>{@link HeapPhase#RECLAIMING} Summary</b>
 * <p>
 * During this phase the memory holding unreachable (<em>unmarked</em>) objects is reclaimed as <em>free space</em>, either
 * as {@linkplain ObjectStatus#FREE FREE} quasi-objects or (if judged too small to be worth managing)
 * {@linkplain ObjectStatus#DARK DARK} quasi-objects (<em>dark matter</em>).
 * Unreachable objects disappear. {@linkplain ObjectStatus#FREE FREE} quasi-objects both appear and disappear as they are merged into
 * larger instances.  Previously created {@linkplain ObjectStatus#DARK DARK} quasi-objects may also be reclaimed and merged
 * with other {@linkplain ObjectStatus#FREE FREE} quasi-objects.
 * </p>
 * <ul>
 * <li><b>Object Map:</b>
 * <ol>
 * <li>The Object Map contains only object references that are {@linkplain ObjectStatus#LIVE LIVE} (if marked as reachable during
 * the preceding {@linkplain HeapPhase#ANALYZING analyzing} phase) or {@linkplain ObjectStatus#UNREACHABLE UNREACHABLE} (if unmarked).</li>
 * <li>Marking does not change during this phase.</li>
 * <li>A previously unseen marked object may be discovered and added to the Object Map as {@linkplain ObjectStatus#LIVE LIVE}.</li>
 * <li>A previously unseen unmarked object may be discovered and added to the Object Map as {@linkplain ObjectStatus#UNREACHABLE UNREACHABLE}.</li>
 * <li>An {@linkplain ObjectStatus#UNREACHABLE UNREACHABLE} object in the Object Map might be discovered to have been <em>released</em>
 * through merging with another instance {@linkplain ObjectStatus#FREE FREE} quasi-object (by observing that the header has been zapped).
 * This reference becomes {@linkplain ObjectStatus#DEAD DEAD} and is removed from the Object Map.</li>
 * <li>An {@linkplain ObjectStatus#UNREACHABLE UNREACHABLE} object in the Object Map might be discovered to have been <em>released</em>
 * through replacement by a {@linkplain ObjectStatus#FREE FREE} quasi-object (by observing that the header has been replaced).
 * This reference becomes {@linkplain ObjectStatus#DEAD DEAD} and is removed from the Object Map.
 * <ul>
 * <li><strong>Note:</strong> in this event we might proactively create a new quasi-reference for the newly discovered
 * {@linkplain ObjectStatus#FREE FREE} quasi-object and add it to the Free Space Map.</li>
 * <li><strong>Note:</strong> in this event we might convert an existing <em>view</em> on the
 * formerly {@linkplain ObjectStatus#UNREACHABLE UNREACHABLE} object to a view on the newly discovered {@linkplain ObjectStatus#FREE FREE} quasi-object
 * that replaced it (or should it be converted to a dead object view?).</li>
 * </ul></li>
 * <li>An {@linkplain ObjectStatus#UNREACHABLE UNREACHABLE} object in the Object Map might be discovered to have been <em>released</em>
 * through replacement by an instance of {@linkplain ObjectStatus#DARK DARK} matter (by observing that the header has been replaced).
 * This reference becomes {@linkplain ObjectStatus#DEAD DEAD} and is removed from the Object Map.
 * <ul>
 * <li><strong>Note:</strong> in this event we might proactively create a new quasi-reference for the newly discovered
 * {@linkplain ObjectStatus#DARK DARK} matter and add it to the Free Space Map.</li>
 * <li><strong>Note:</strong> in this event we might convert an existing <em>view</em> on the
 * formerly {@linkplain ObjectStatus#UNREACHABLE UNREACHABLE} object to a view on the newly discovered
 * {@linkplain ObjectStatus#DARK DARK} matter that replaced it (or should it be converted to a dead object view?).</li>
 * </ul></li>
 * <li>At the end of the {@linkplain HeapPhase#RECLAIMING reclaiming} phase, every {@linkplain ObjectStatus#UNREACHABLE UNREACHABLE}
 * object in the Object Map should have become {@linkplain ObjectStatus#DEAD DEAD}, either zapped, replaced in memory by a
 * {@linkplain ObjectStatus#FREE FREE} quasi-object or a {@linkplain ObjectStatus#DARK DARK} quasi-object.</li>
 * <li>No other changes to the Object Map take place during the {@linkplain HeapPhase#RECLAIMING reclaiming} phase.</li>
 * </ol></li>
 * <li><b>Free Space Map:</b>
 * <ol>
 * <li>The Free Space map contains only quasi-object references that are {@linkplain ObjectStatus#FREE FREE}
 * or {@linkplain ObjectStatus#DARK DARK}.</li>
 * <li>Previously unseen {@linkplain ObjectStatus#FREE FREE} quasi-objects may be discovered
 * and added to the Free Space Map, but they do not change during this phase.</li>
 * <li>Previously unseen {@linkplain ObjectStatus#DARK DARK} quasi-objects may be discovered and added to the
 * Free Space Map, but they do not change during this phase.</li>
 * <li>A {@linkplain ObjectStatus#FREE FREE} quasi-object reference in the Free Space Map may be discovered, by observing that its hub pointer
 * has changed, to have been <em>released</em> through merging with another {@linkplain ObjectStatus#FREE FREE} quasi-object. This reference becomes
 * {@linkplain ObjectStatus#DEAD DEAD} and is removed from the Free Space Map.</li>
 * <li>A {@linkplain ObjectStatus#FREE FREE} quasi-object reference in the Free Space Map may be discovered to have changed in size through
 * consolidation with following reclaimed space.</li>
 * <li>A {@linkplain ObjectStatus#DARK DARK} object in the Free Space Map maybe be discovered to have
 * been <em>reclaimed</em> through merging with another {@linkplain ObjectStatus#FREE FREE} quasi-object. This quasi-reference becomes
 * {@linkplain ObjectStatus#DEAD DEAD} and is removed from the FreeSpace Map.</li>
 * </ol></li></ul>
 * <p>
 * @see HeapFreeChunk
 * @see Memory#ZAPPED_MARKER
 * @see Sweeper#minReclaimableSpace()
 * @see MSHeapScheme
 * @see VmMarkBitmap
 */
public final class RemoteMSHeapScheme extends AbstractRemoteHeapScheme implements RemoteObjectReferenceManager {

    private static final int TRACE_VALUE = 1;

    private final TimedTrace heapUpdateTracer;

    private long lastUpdateEpoch = -1L;
    private long lastGCCompletedCount = 0L;
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
     * <p>
     * <strong>Invariant</strong>: the map holds only objects with status {@linkplain ObjectStatus#LIVE LIVE} or {@linkplain ObjectStatus#UNREACHABLE UNREACHABLE}
     */
    private WeakRemoteReferenceMap<MSRemoteReference> objectRefMap = new WeakRemoteReferenceMap<MSRemoteReference>();

    /**
     * Map:  VM address in Object-Space --> a {@link MSRemoteReference} that refers to the free space chunk whose origin is at that location.
     * <p>
     * <strong>Invariant</strong>: the map holds only objects with status {@linkplain ObjectStatus#FREE FREE} or {@linkplain ObjectStatus#DARK DARK}.
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
                 * analysis to the RECLAIMING phase of a GC. This is exactly the moment in a GC cycle when reference
                 * information must be updated. Unfortunately, the handler supplied with this listener will only be
                 * called after the VM state refresh cycle is complete. That would be too late since so many other parts
                 * of the refresh cycle depend on references. Consequently, the needed updates take place when this
                 * manager gets refreshed (early in the refresh cycle, see UpdateMemoryStatus()), not when this handler
                 * eventually gets called.
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

    /**
     * {@inheritDoc}
     * <p>
     * This gets called more than once during the startup sequence.
     * <p>
     * The sequence of things to be updated is based on an analysis of what might have happened since the last we
     * checked. In the limiting case, since the only forced halt is at the beginning of the
     * {@linkplain HeapPhase#RECLAIMING RECLAIMING} phase, we might find that we have halted at this point in the cycle
     * and we haven't updated since the last halt at the same point.
     */
    @Override
    public void updateMemoryStatus(long epoch) {

        super.updateMemoryStatus(epoch);
        updateFreeHubOrigins();
        if (scheme == null) {
            // Can't do anything until we have the VM object that represents the scheme implementation
            return;
        }

        // Ensure we have the latest information from the remote scheme object, since it may not yet have been touched in the refresh cycle.
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
             * This is a normal refresh. Immediately update information about the location/size/allocation of the heap
             * region, which is represented by a remote object; this update must be forced because remote objects are
             * otherwise not refreshed until later in the update cycle.
             */
            objectSpaceMemoryRegion.updateCache(epoch);

            // The order of the following reference updates is significant

            if (lastGCCompletedCount < gcCompletedCount()) {
                /*
                 * A GC, and in particular a RECLAIMING phase, has completed since the last time we checked. Find any
                 * references still marked UNREACHABLE and make them DEAD. It is important to clear them out before we
                 * handle the possible conclusion of the following ANALYZING phase that may also have happened since
                 * we last checked.
                 */
                for (MSRemoteReference objectRef : objectRefMap.values()) {
                    if (objectRef.status().isUnreachable()) {
                        objectRef.die();
                        assert objectRefMap.remove(objectRef.origin()) != null;
                    }
                }
                lastGCCompletedCount = gcCompletedCount();
            }

            if (phase().isReclaiming()) {
                if (lastReclaimingPhaseCount < gcStartedCount()) {
                    /*
                     * We are halted for the first time in this RECLAIMING phase, usually because of the phase change
                     * listener we registered. That means that an ANALYZING phase has concluded since the last time we
                     * checked. Find any LIVE object references that are unmarked and make them UNREACHABLE before
                     * anything else happens during this phase.
                     */
                    for (MSRemoteReference objectRef : objectRefMap.values()) {
                        if (objectRef.status().isLive()) {
                            if (markBitMap().getMarkColor(objectRef.origin()) != MarkColor.MARK_BLACK) {
                                objectRef.discoveredUnreachable();
                            }
                        } else {
                            // There shouldn't be any UNREACHABLE objects at this point; any lingering ones have been removed by the previous check.
                            TeleWarning.message(tracePrefix() + "Found unexpected ref status at RECLAIMING start=" + objectRef);
                        }
                    }
                    lastReclaimingPhaseCount = gcStartedCount();
                }
                /*
                 * Every time we stop while RECLAIMING, check to see if any unreachable objects have been reclaimed
                 * since we last checked. The memory that held an unreachable object might now hold a FREE chunk, DARK
                 * matter, or might be entirely zapped.
                 */
                for (MSRemoteReference objectRef : objectRefMap.values()) {
                    if (objectRef.status().isUnreachable()) {
                        final Address origin = objectRef.origin();
                        if (objectStatusAt(origin).isDead() || isHeapFreeChunkOrigin(origin) || isDarkMatterOrigin(origin)) {
                            objectRef.die();
                            assert objectRefMap.remove(objectRef.origin()) != null;
                        }
                    }
                }
            }

            /*
             * No matter what phase we're in, once everything else is taken care of, see if any free space has been
             * released. Do this last because nothing else in this part of the update is affected by it. Free space
             * isn't supposed to be released during the ANALYSIS phase, but we might not have checked since some time in
             * an earlier phase.
             */
            for (MSRemoteReference freeSpaceRef : freeSpaceRefMap.values()) {
                switch (freeSpaceRef.status()) {
                    case FREE:
                        // FREE chunks can be released during a MUTATING phase when the space is used to allocate a new object.
                        // FREE chunks can be released during a RECLAIMING phase when the space is merged with another FREE chunk.
                        if (!objectStatusAt(freeSpaceRef.origin()).isFree()) {
                            // The reference no longer points at a free space chunk, so it has been released.
                            freeSpaceRef.die();
                            assert freeSpaceRefMap.remove(freeSpaceRef.origin()) != null;
                        }
                        break;
                    case DARK:
                        // DARK chunks can be released during a RECLAIMING phase  when the space is merged with another FREE chunk.
                        if (!objectStatusAt(freeSpaceRef.origin()).isDark()) {
                            // The reference no longer points at dark matter, so it has been released.
                            freeSpaceRef.die();
                            assert freeSpaceRefMap.remove(freeSpaceRef.origin()) != null;
                        }
                        break;
                }
            }

            heapUpdateTracer.end(heapUpdateStatsPrinter);
            lastUpdateEpoch = epoch;
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

    public ObjectStatus objectStatusAt(Address origin) throws TeleError {
        TeleError.check(contains(origin), "Location is outside MS heap region");
        switch(phase()) {
            case MUTATING:
            case ANALYZING:
                if (objectSpaceMemoryRegion.containsInAllocated(origin) && objects().isPlausibleOriginUnsafe(origin)) {
                    if (isHeapFreeChunkOrigin(origin)) {
                        return ObjectStatus.FREE;
                    }
                    if (isDarkMatterOrigin(origin)) {
                        return ObjectStatus.DARK;
                    }
                    return ObjectStatus.LIVE;
                }
                break;
            case RECLAIMING:
                if (objectSpaceMemoryRegion.containsInAllocated(origin) && objects().isPlausibleOriginUnsafe(origin)) {
                    if (isHeapFreeChunkOrigin(origin)) {
                        return ObjectStatus.FREE;
                    }
                    if (isDarkMatterOrigin(origin)) {
                        return ObjectStatus.DARK;
                    }
                    switch(scheme.markBitmap.getMarkColorUnsafe(origin)) {
                        case MARK_BLACK:
                            return ObjectStatus.LIVE;
                        case MARK_WHITE:
                            return ObjectStatus.UNREACHABLE;
                        case MARK_GRAY:
                            TeleWarning.message("Gray Mark found during Reclaiming @ :" + origin.to0xHexString());
                    }
                }
                break;
            default:
                TeleError.unknownCase();
        }
        return ObjectStatus.DEAD;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The MS collector does not relocate objects.
     */
    public boolean isForwardingAddress(Address forwardingAddress) {
        return false;
    }

    public RemoteReference makeReference(Address origin) throws TeleError {
        assert vm().lockHeldByCurrentThread();
        // It is an error to attempt creating a reference if the address is completely outside the managed region(s).
        TeleError.check(contains(origin), "Location is outside MS heap region");
        final MSRemoteReference oldRef = objectRefMap.get(origin);
        if (oldRef != null) {
            // A live object or unreachable quasi-object is in the map at that location; only return if live.
            return oldRef.status().isLive() ? oldRef : null;
        }
        if (freeSpaceRefMap.get(origin) != null) {
            // A reference to a free space quasi object at that address already exists; nothing LIVE here.
            return null;
        }
        // Not in either map; might be a live object not yet seen.
        if (objectStatusAt(origin).isLive()) {
            final MSRemoteReference newLiveRef = MSRemoteReference.createLive(this, origin);
            if (newLiveRef != null) {
                objectRefMap.put(origin, newLiveRef);
                return newLiveRef;
            }
        }
        return null;
    }

    public RemoteReference makeQuasiReference(Address origin) throws TeleError {
        assert vm().lockHeldByCurrentThread();
        // It is an error to attempt creating a reference if the address is completely outside the managed region(s).
        TeleError.check(contains(origin), "Location is outside MS heap region");
        final MSRemoteReference oldFreeSpaceRef = freeSpaceRefMap.get(origin);
        if (oldFreeSpaceRef != null) {
            // A reference to some kind of quasi-object is already in the map
            return oldFreeSpaceRef;
        }
        final MSRemoteReference oldObjectRef = objectRefMap.get(origin);
        if (oldObjectRef != null) {
            // A live object or unreachable quasi-object is in the map at that location; only return if unreachable.
            return oldObjectRef.status().isUnreachable() ? oldObjectRef : null;
        }
        // Not in either map; might be a quasi-object not yet seen.
        switch (objectStatusAt(origin)) {
            case FREE:
                final MSRemoteReference newFreeRef = MSRemoteReference.createFree(this, origin);
                freeSpaceRefMap.put(origin, newFreeRef);
                return newFreeRef;
            case DARK:
                final MSRemoteReference newDarkRef = MSRemoteReference.createDark(this, origin);
                freeSpaceRefMap.put(origin, newDarkRef);
                break;
            case UNREACHABLE:
                final MSRemoteReference newUnreachableRef = MSRemoteReference.createUnreachable(this, origin);
                objectRefMap.put(origin, newUnreachableRef);
                return newUnreachableRef;
            default:
                TeleError.unexpected();
        }
        return null;
    }

    /**
     * Does the heap region contain the address anywhere (allocated or not)?
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

    @SuppressWarnings("deprecation")
    private void printRegionObjectStats(PrintStream printStream, int indent, boolean verbose, TeleMemoryRegion region, WeakRemoteReferenceMap<MSRemoteReference> map) {
        final NumberFormat formatter = NumberFormat.getInstance();
        int totalRefs = 0;
        int liveRefs = 0;
        int unreachableRefs = 0;
        int freeRefs = 0;
        int darkRefs = 0;
        int deadRefs = 0;

        for (MSRemoteReference ref : map.values()) {
            switch(ref.status()) {
                case LIVE:
                    liveRefs++;
                    break;
                case UNREACHABLE:
                    unreachableRefs++;
                    break;
                case FREE:
                    freeRefs++;
                    break;
                case DARK:
                    darkRefs++;
                    break;
                case DEAD:
                    deadRefs++;
                    break;
            }
        }
        totalRefs = liveRefs + unreachableRefs + deadRefs;

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
            sb2.append(ObjectStatus.UNREACHABLE.label()).append("=").append(formatter.format(unreachableRefs));
            sb2.append(ObjectStatus.FREE.label()).append("=").append(formatter.format(freeRefs));
            sb2.append(ObjectStatus.DARK.label()).append("=").append(formatter.format(darkRefs));
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
