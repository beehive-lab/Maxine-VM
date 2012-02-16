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

import java.io.*;
import java.lang.management.*;
import java.lang.ref.*;
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
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.semiSpace.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.runtime.*;


/**
 * Inspector support for working with VM sessions using the VM's simple semispace collector.
 *
 * @see SemiSpaceHeapScheme
 */
public class RemoteSemiSpaceHeapScheme extends AbstractRemoteHeapScheme implements RemoteObjectReferenceManager {

    private static final int TRACE_VALUE = 1;

    private final TimedTrace heapUpdateTracer;

    private long lastUpdateEpoch = -1L;
    private long lastGCStartedCount = 0L;

    /**
     * The VM object that implements the {@link HeapScheme} in the current configuration.
     */
    private com.sun.max.tele.object.TeleSemiSpaceHeapScheme scheme;

    /**
     * The VM object that describes the collector's "To-Space".
     */
    private TeleLinearAllocationMemoryRegion toSpaceMemoryRegion = null;

    /**
     * The VM object that describes the collector's "From-Space".
     */
    private TeleLinearAllocationMemoryRegion fromSpaceMemoryRegion = null;

    private final int gcForwardingPointerOffset;

    private long collected = 0;
    private List<SemiSpaceRemoteReference> forwardedReferences = new ArrayList<SemiSpaceRemoteReference>();

    private final ReferenceUpdateTracer referenceUpdateTracer;

    private final List<VmHeapRegion> heapRegions = new ArrayList<VmHeapRegion>(2);

    private final Object statsPrinter = new Object() {
        @Override
        public String toString() {
            final StringBuilder msg = new StringBuilder();
            msg.append("phase =").append(phase.label());
            msg.append(" #starts=").append(gcStartedCount);
            msg.append(", #complete=").append(gcCompletedCount);
            return msg.toString();
        }
    };

    protected RemoteSemiSpaceHeapScheme(TeleVM vm) {
        super(vm);
        this.heapUpdateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");
        this.referenceUpdateTracer = new ReferenceUpdateTracer();
        // The collector stores forwarding pointers in the Hub field of the header
        gcForwardingPointerOffset = Layout.generalLayout().getOffsetFromOrigin(HeaderField.HUB).toInt();

        final VmAddressSpace addressSpace = vm().addressSpace();
        // There might already be dynamically allocated regions in a dumped image or when attaching to a running VM
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

            public void initialiationComplete(long epoch) {
                // Get the VM object that represents the heap implementation.
                scheme = (com.sun.max.tele.object.TeleSemiSpaceHeapScheme) teleHeapScheme();
                updateMemoryStatus(epoch);
                // Force the VM to stop any time the heap transitions from analysis to reclaiming
                // Note, however that the actual work that must be done at the transition doesn't
                // get done in the hander, because it must happen very early in the refresh cycle.
                // The specified handler only gets run after the refresh cycle is complete.
                try {
                    vm().addGCPhaseListener(new MaxGCPhaseListener() {

                        public void gcPhaseChange(HeapPhase phase) {
                            Trace.line(TRACE_VALUE, tracePrefix() + " VM stopped for reference updates");
                        }
                    }, HeapPhase.RECLAIMING);
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
     * No need to look for heap regions that disappear for this collector.
     * <p>
     * Once allocated, the two regions never change, but we force a refresh on the
     * memory region descriptions, since their locations change when swapped.
     */
    @Override
    public void updateMemoryStatus(long epoch) {
        heapUpdateTracer.begin();
        super.updateMemoryStatus(epoch);
        if (heapRegions.isEmpty()) {
            if (scheme != null) {
                toSpaceMemoryRegion = scheme.teleToRegion();
                if (toSpaceMemoryRegion != null) {
                    final VmHeapRegion toVmHeapRegion = new VmHeapRegion(vm(), toSpaceMemoryRegion, this);
                    heapRegions.add(toVmHeapRegion);
                    vm().addressSpace().add(toVmHeapRegion.memoryRegion());
                    fromSpaceMemoryRegion = scheme.teleFromRegion();
                    final VmHeapRegion fromVmHeapRegion = new VmHeapRegion(vm(), fromSpaceMemoryRegion, this);
                    heapRegions.add(fromVmHeapRegion);
                    vm().addressSpace().add(fromVmHeapRegion.memoryRegion());
                }
            }
        } else {
            // TODO (mlvdv) What if first time we see the regions we're at the reclaiming halt?
            if (epoch > lastUpdateEpoch) {
                // Only need to do this once per epoch, unlike the startup sequence
                // where the infrastructure needed to find the regions may be
                // assembled gradually during one epoch.
                toSpaceMemoryRegion.updateCache(epoch);
                fromSpaceMemoryRegion.updateCache(epoch);


                final HeapPhase phase = phase();
                if (phase == HeapPhase.RECLAIMING && gcStartedCount() > lastGCStartedCount) {
                    // We are guaranteed to stop at least once during each RECLAIMING PHASE.  If it is the first
                    // time, then we need to update references.
                    updateReferences(epoch);
                }
                lastGCStartedCount = gcStartedCount();
            }
        }
        lastUpdateEpoch = epoch;
        heapUpdateTracer.end(statsPrinter);
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
    private VmHeapRegion find(TeleRuntimeMemoryRegion runtimeMemoryRegion) {
        for (VmHeapRegion heapRegion : heapRegions) {
            if (runtimeMemoryRegion == heapRegion.representation()) {
                return heapRegion;
            }
        }
        return null;
    }

    /**
     * Map:  address in VM --> a {@link SemiSpaceRemoteReference} that refers to the object whose origin is at that location.
     */
    private Map<Long, WeakReference<SemiSpaceRemoteReference>> originToReference = new HashMap<Long, WeakReference<SemiSpaceRemoteReference>>();

    public boolean isObjectOrigin(Address origin) throws TeleError {
        TeleError.check(contains(origin), "Location is outside semispace heap regions");
        switch(phase()) {
            case ALLOCATING:
            case RECLAIMING:
                return toSpaceMemoryRegion.containsInAllocated(origin)
                    && objects().isPlausibleOriginUnsafe(origin);
            case ANALYZING:
                return (toSpaceMemoryRegion.containsInAllocated(origin) || fromSpaceMemoryRegion.containsInAllocated(origin))
                    && objects().isPlausibleOriginUnsafe(origin);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Using only low-level mechanisms, return a plausible location for a forwarded
     * copy of the object whose presumed location is specified.  This is unsafe, but it
     * does apply sanity checks that should minimize false positives:
     * checks:
     * <ol>
     * <li>Heap must be in the {@link HeapPhase#ANALYZING} phase.</li>
     * <li>The presumed object origin must be in the live area of the {@code From} space.</li>
     * <li>The word where forwarding pointers are stored must be <em>tagged</em> as if a forwarding pointer.</li>
     * <li>The derived forwarding address must be in the live area of the {@code To} space.</li>
     * </ol>
     */
    public Address getForwardingAddressUnsafe(Address origin) throws TeleError {
        TeleError.check(contains(origin), "Location is outside semispace heap regions");
        if (phase() == HeapPhase.ANALYZING) {
            Word forwardingWord = readForwardWord(origin);
            if (isForwardPointer(forwardingWord)) {
                final Address forwardedCopyOrigin = forwardAddress(forwardingWord);
                if (toSpaceMemoryRegion.containsInAllocated(forwardedCopyOrigin)) {
                    return forwardedCopyOrigin;
                }
            }
        }
        return null;
    }

    @Override
    public RemoteReference makeReference(Address origin) throws TeleError {
        TeleError.check(contains(origin), "Location is outside semispace heap regions");
        SemiSpaceRemoteReference remoteReference = null;
        final WeakReference<SemiSpaceRemoteReference> existingRef = originToReference.get(origin.toLong());
        if (existingRef != null) {
            remoteReference = existingRef.get();
        }
        if (remoteReference == null && isObjectOrigin(origin)) {
            remoteReference = new SemiSpaceRemoteReference(vm(), origin);
            originToReference.put(origin.toLong(), new WeakReference<SemiSpaceRemoteReference>(remoteReference));
        }
        return remoteReference;
    }

    private void updateReferences(long epoch) {

        // TODO (mlvdv)  check for duplicates,  when change forwarded to live, you may collide with
        // a reference pointing to the new copy of it (which space is it in?)

        forwardedReferences.clear();
        collected = 0;
        for (WeakReference<SemiSpaceRemoteReference> weakRef : originToReference.values()) {
            if (weakRef != null) {
                final SemiSpaceRemoteReference remoteReference = weakRef.get();
                if (remoteReference != null) {
                    if (hasForwardPointer(remoteReference.origin)) {
                        forwardedReferences.add(remoteReference);
                    } else {
                        remoteReference.markedDead = true;
                        collected++;
                    }

                }
            }
        }
        Trace.line(TRACE_VALUE, tracePrefix() + referenceUpdateTracer.toString());
        for (SemiSpaceRemoteReference remoteReference : forwardedReferences) {
            final Address oldOrigin = remoteReference.origin;
            final WeakReference<SemiSpaceRemoteReference> weakRef = originToReference.get(oldOrigin.toLong());
            assert weakRef != null;
            assert originToReference.remove(oldOrigin.toLong()) != null;
            final Address newOrigin = getForward(oldOrigin);
            remoteReference.origin = newOrigin;
            originToReference.put(newOrigin.toLong(), new WeakReference<SemiSpaceRemoteReference>(remoteReference));
        }
    }


    private void printRegionObjectStats(PrintStream printStream, int indent, boolean verbose, TeleLinearAllocationMemoryRegion region) {
        final NumberFormat formatter = NumberFormat.getInstance();

        int totalRefs = 0;
        int liveRefs = 0;
        int unknownRefs = 0;
        int forwardedRefs = 0;
        int deadRefs = 0;

        for (WeakReference<SemiSpaceRemoteReference> weakRef : originToReference.values()) {
            final SemiSpaceRemoteReference remoteReference = weakRef.get();
            if (remoteReference != null && region.contains(remoteReference.raw())) {
                totalRefs++;
                switch(remoteReference.status()) {
                    case LIVE:
                        liveRefs++;
                        break;
                    case UNKNOWN:
                        unknownRefs++;
                        break;
                    case FORWARDED:
                        forwardedRefs++;
                        break;
                    case DEAD:
                        deadRefs++;
                        break;
                }
            }
        }

        // Line 0
        String indentation = Strings.times(' ', indent);
        final StringBuilder sb0 = new StringBuilder();
        sb0.append("Dynamic region: ");
        sb0.append(find(region).entityName());
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
            sb1.append(", usage=" + (Long.toString(100 * used / size)) + "%");
        } else {
            sb1.append(" <unallocated>");
        }
        printStream.println(indentation + sb1.toString());

        // Line 2, indented
        final StringBuilder sb2 = new StringBuilder();
        sb2.append("refs=").append(formatter.format(totalRefs));
        sb2.append(" (");
        sb2.append(ObjectStatus.LIVE.label()).append("=").append(formatter.format(liveRefs)).append(", ");
        sb2.append(ObjectStatus.UNKNOWN.label()).append("=").append(formatter.format(unknownRefs)).append(", ");
        sb2.append(ObjectStatus.FORWARDED.label()).append("=").append(formatter.format(forwardedRefs)).append(", ");
        sb2.append(ObjectStatus.DEAD.label()).append("=").append(formatter.format(deadRefs));
        sb2.append(")");
        if (verbose) {
            sb2.append(", ref. mgr=").append(getClass().getSimpleName());
        }
        printStream.println(indentation + sb2.toString());
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
        // TODO (mlvdv)  what about FORWARDED?
        if (fromSpaceMemoryRegion.contains(address)) {
            switch(phase()) {
                case ALLOCATING:
                    return false;
                case ANALYZING:
                case RECLAIMING:
                    // TODO (mlvdv) is the allocation mark valid here?
                    return true;
            }
        } else if (toSpaceMemoryRegion.contains(address)) {
            return toSpaceMemoryRegion.containsInAllocated(address);
        }
        return false;
    }

    /**
     * Assuming that the argument is the location of an object in managed
     * memory that could legitimately be forwarded, read the word that would
     * hold the forwarding pointer.
     */
    private Word readForwardWord(Address origin) {
        return memory().readWord(origin.plus(gcForwardingPointerOffset));
    }

    /**
     * Does a word appear to represent a forwarding pointer,
     * as used by this GC implementation.
     */
    private boolean isForwardPointer(Word word) {
        return word.asAddress().and(1).toLong() == 1;
    }

    /**
     * Assumes the address is a valid object origin; returns whether
     * the object holds a forwarding pointer, without checking whether
     * the result is a legitimate address or a plausible location
     * for a forwarded object copy.
     */
    private boolean hasForwardPointer(Address origin) {
        return isForwardPointer(readForwardWord(origin));
    }

    /**
     * Assumes that the argument is a valid object origin and that
     * it holds a forwarding pointer; return the origin at which
     * forwarding points without any sanity checks.
     */
    private Address getForward(Address origin) {
        return forwardAddress(readForwardWord(origin));
    }

    /**
     * Gets the actual address of a forwarding pointer, assuming
     * that the argument is a forwarding address.
     */
    private Address forwardAddress(Word word) {
        Address newCellAddress = word.asAddress().minus(1);
        return Layout.generalLayout().cellToOrigin(newCellAddress.asPointer());
    }

    @Override
    public void printObjectSessionStats(PrintStream printStream, int indent, boolean verbose) {
        if (!heapRegions.isEmpty()) {
            int toSpaceRefs = 0;
            int fromSpaceRefs = 0;

            for (WeakReference<SemiSpaceRemoteReference> weakRef : originToReference.values()) {
                if (weakRef != null) {
                    final SemiSpaceRemoteReference remoteReference = weakRef.get();
                    if (remoteReference != null) {
                        if (toSpaceMemoryRegion.contains(remoteReference.raw())) {
                            toSpaceRefs++;
                        } else if (fromSpaceMemoryRegion.contains(remoteReference.raw())) {
                            fromSpaceRefs++;
                        }
                    }
                }
            }
            final NumberFormat formatter = NumberFormat.getInstance();

            // Line 0
            String indentation = Strings.times(' ', indent);
            final StringBuilder sb0 = new StringBuilder();
            sb0.append("Dynamic Heap:");
            if (verbose) {
                sb0.append("  mgr=").append(vm().heapScheme().name());
            }
            printStream.println(indentation + sb0.toString());

            // increase indentation
            indentation += Strings.times(' ', 4);

            // Line 1
            final StringBuilder sb1 = new StringBuilder();
            sb1.append("phase=").append(phase().label());
            sb1.append(", collections completed=").append(formatter.format(gcCompletedCount));
            printStream.println(indentation + sb1.toString());

            // Line 2
            final StringBuilder sb2 = new StringBuilder();
            sb2.append("total object refs:");
            sb2.append(" fromSpace=").append(formatter.format(fromSpaceRefs));
            sb2.append(", toSpace=").append(formatter.format(toSpaceRefs));
            printStream.println(indentation + sb2.toString());


            printRegionObjectStats(printStream, indent + 4, verbose, toSpaceMemoryRegion);
            printRegionObjectStats(printStream, indent + 4, verbose, fromSpaceMemoryRegion);
        }
    }

    private class SemiSpaceRemoteReference extends RemoteReference {

        private Address origin;
        private boolean markedDead = false;

        protected SemiSpaceRemoteReference(TeleVM vm, Address origin) {
            super(vm);
            assert origin.isNotZero();
            this.origin = origin;
        }

        @Override
        public Address raw() {
            return origin;
        }

        @Override
        public ObjectStatus status() {
            if (markedDead) {
                return ObjectStatus.DEAD;
            } else {
                switch(phase()) {
                    case ALLOCATING:
                    case RECLAIMING:
                        // There are only LIVE and DEAD objects
                        if (toSpaceMemoryRegion.containsInAllocated(origin)) {
                            // The only live objects are here.
                            return ObjectStatus.LIVE;
                        }
                        return ObjectStatus.DEAD;
                    case ANALYZING:
                        // There are only UNKNOWN, FORWARDED, and DEAD objects
                        if (toSpaceMemoryRegion.containsInAllocated(origin)) {
                            // Newly copied objects
                            return ObjectStatus.LIVE;
                        }
                        if (fromSpaceMemoryRegion.containsInAllocated(origin)) {
                            if (hasForwardPointer(origin)) {
                                return ObjectStatus.FORWARDED;
                            }
                            return ObjectStatus.UNKNOWN;
                        }
                        break;
                }
            }
            TeleError.unknownCase("unknown HeapPhase");
            return null;
        }

        @Override
        public RemoteReference getForwardReference() {
            if (phase() == HeapPhase.ANALYZING && hasForwardPointer(origin)) {
                return RemoteSemiSpaceHeapScheme.this.vm().referenceManager().makeReference(getForward(origin));
            }
            return null;
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
            sb.append(":  forwarded=").append(formatter.format(forwardedReferences.size()));
            sb.append(", collected=").append(formatter.format(collected));
            return sb.toString();
        }
    }


}
