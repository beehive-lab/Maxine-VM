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

import static com.sun.max.vm.heap.ObjectStatus.*;

import java.io.*;
import java.lang.management.*;
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.TeleVM.InitializationListener;
import com.sun.max.tele.heap.RemoteMSHeapScheme.MSRemoteReference.RefStateCount;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.ms.*;
import com.sun.max.vm.reference.*;

/**
 * Inspection support specialized for the basic mark-sweep implementation of {@link HeapScheme}
 * in the VM.
 *
 * @see MSHeapScheme
 */
public final class RemoteMSHeapScheme extends AbstractRemoteHeapScheme implements RemoteObjectReferenceManager {

    private static final int TRACE_VALUE = 1;

    private final TimedTrace heapUpdateTracer;

    private long lastUpdateEpoch = -1L;

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
    private WeakRemoteReferenceMap<MSRemoteReference> objectSpaceRefMap = new WeakRemoteReferenceMap<MSRemoteReference>();

    /**
     * Map:  VM address in Object-Space --> a {@link MSRemoteReference} that refers to the free space chunk whose origin is at that location.
     */
    private WeakRemoteReferenceMap<MSRemoteReference> freeSpaceRefMap = new WeakRemoteReferenceMap<MSRemoteReference>();

    private final List<VmHeapRegion> heapRegions = new ArrayList<VmHeapRegion>(1);

    /**
     * A printer for statistics at the end of each update.
     */
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
    }

    public Class heapSchemeClass() {
        return MSHeapScheme.class;
    }

    public void initialize(long epoch) {
        vm().addInitializationListener(new InitializationListener() {

            public void initialiationComplete(final long initializationEpoch) {
                objects().registerTeleObjectType(MSHeapScheme.class, TeleMSHeapScheme.class);
                // Get the VM object that represents the heap implementation; can't do this any sooner during startup.
                scheme = (TeleMSHeapScheme) teleHeapScheme();
                assert scheme != null;

                updateMemoryStatus(initializationEpoch);
                // TODO (mlvdv) decide where to put the phase change notification so that the GC inspection will have all the needed information.
                /*
                 * Add a heap phase listener that will will force the VM to stop any time the heap transitions from
                 * analysis to the reclaiming phase of a GC. This is exactly the moment in a GC cycle when reference
                 * information must be updated. Unfortunately, the handler supplied with this listener will only be
                 * called after the VM state refresh cycle is complete. That would be too late since so many other parts
                 * of the refresh cycle depend on references. Consequently, the needed updates take place when this
                 * manager gets refreshed (early in the refresh cycle), not when this handler eventually gets called.
                 */
//                try {
//                    vm().addGCPhaseListener(new MaxGCPhaseListener() {
//
//                        public void gcPhaseChange(HeapPhase phase) {
//                            // Dummy handler; the actual updates must be done early during the refresh cycle.
//                            final long phaseChangeEpoch = vm().teleProcess().epoch();
//                            Trace.line(TRACE_VALUE, tracePrefix() + " VM stopped for reference updates, epoch=" + phaseChangeEpoch + ", gc cycle=" + gcStartedCount());
//                            Trace.line(TRACE_VALUE, tracePrefix() + " Note: updates have long since been done by the time this (dummy) handler is called");
//                        }
//                    }, RECLAIMING);
//                } catch (MaxVMBusyException e) {
//                    TeleError.unexpected("Unable to add GC Phase Listener");
//                }
            }
        });

    }

    public List<VmHeapRegion> heapRegions() {
        return heapRegions;
    }


    /**
     * {@inheritDoc}
     * <p>
     * This gets called more than once during the startup sequence.
     */
    @Override
    public void updateMemoryStatus(long epoch) {

        super.updateMemoryStatus(epoch);

        if (scheme == null) {
            // Can't do anything until we have the VM object that represents the scheme implementation
            return;
        }
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


            // TODO (mlvdv) based on where we are in the GC cycle, update remote references as needed.

            lastUpdateEpoch = epoch;
            heapUpdateTracer.end(statsPrinter);
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

    // TODO (mlvdv) refine
    public boolean isObjectOrigin(Address origin) throws TeleError {
        TeleError.check(contains(origin), "Location is outside MS heap region");
        switch(phase()) {
            case MUTATING:
            case RECLAIMING:
            case ANALYZING:
                return objectSpaceMemoryRegion.containsInAllocated(origin) && objects().isPlausibleOriginUnsafe(origin);
            default:
                TeleError.unknownCase();
        }
        return false;
    }

    // TODO (mlvdv) refine
    public RemoteReference makeReference(Address origin) throws TeleError {
        assert vm().lockHeldByCurrentThread();
        // It is an error to attempt creating a reference if the address is completely outside the managed region(s).
        TeleError.check(contains(origin), "Location is outside MS heap region");
        MSRemoteReference remoteReference = null;
        switch(phase()) {
            case MUTATING:
            case RECLAIMING:
            case ANALYZING:
                remoteReference = objectSpaceRefMap.get(origin);
                if (remoteReference != null) {
                    // A reference to the object is already in the Object-Space map.
                    TeleError.check(remoteReference.status().isLive());
                } else if (objectSpaceMemoryRegion.containsInAllocated(origin) && objects().isPlausibleOriginUnsafe(origin)) {
                    // A newly discovered object in the allocated area of To-Space; add a new reference to the To-Space map.
                    remoteReference = MSRemoteReference.createLive(this, origin);
                    objectSpaceRefMap.put(origin, remoteReference);
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

            final int totalRefs = objectSpaceRefMap.values().size();

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

            printRegionObjectStats(printStream, indent + 4, verbose, objectSpaceMemoryRegion, objectSpaceRefMap);

        }
    }


    /**
     * Surrogate object for the scheme instance in the VM.
     */
    public static class TeleMSHeapScheme extends TeleHeapScheme {

        private TeleFreeHeapSpaceManager objectSpace;

        public TeleMSHeapScheme(TeleVM vm, Reference reference) {
            super(vm, reference);
        }

        @Override
        protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
            if (!super.updateObjectCache(epoch, statsPrinter)) {
                return false;
            }
            if (objectSpace == null) {
                final Reference freeHeapSpaceManagerRef = fields().MSHeapScheme_objectSpace.readReference(reference());
                objectSpace = (TeleFreeHeapSpaceManager) objects().makeTeleObject(freeHeapSpaceManagerRef);
            }

            return true;
        }

    }

    /**
     * Representation of a remote object reference in a heap region managed by a mark sweep GC. The states of the reference
     * represent what can be known about the object at any given time, especially those relevant during the
     * {@link #ANALYZING} phase of the heap.
     *
     * @see <a href="http://en.wikipedia.org/wiki/State_pattern">"State" design pattern</a>
     */
    public static class MSRemoteReference extends RemoteReference {
        /**
         * An enumeration of possible states of a remote reference for this kind of collector, based on the heap phase and
         * what is known at any given time.
         * <p>
         * Each member encapsulates the <em>behavior</em> associated with a state, including both the interpretation of
         * the data held by the reference and by allowable state transitions.
         * <p>
         * This set of states actually defines two independent state models: one for ordinary object and one for
         * pseudo-objects (instances of {@link HeapFreeChunk}) used by the collector to represent unallocated
         * memory in a fashion similar to ordinary objects.  These can be treated as ordinary references for some
         * purposes, but not all.
         */
        private static enum RefState {

            /**
             * Reference to a reachable object.
             */
            OBJ_LIVE ("LIVE object"){

                // Properties
                @Override
                ObjectStatus status() {
                    return LIVE;
                }

                @Override
                boolean isFreeSpace() {
                    return false;
                }

                // Transitions
                @Override
                void makeUnknown(MSRemoteReference ref) {
                    ref.refState = OBJ_UNKNOWN;
                }
            },

            /**
             * Reference to an object whose reachability is unknown, heap {@link #ANALYZING}.
             */
            OBJ_UNKNOWN ("UNKNOWN object (Analyzing)") {

                // Properties
                @Override ObjectStatus status() {
                    return UNKNOWN;
                }

                @Override
                boolean isFreeSpace() {
                    return false;
                }

                // Transitions
                @Override
                void makeLive(MSRemoteReference ref) {
                    ref.refState = OBJ_LIVE;
                }
                @Override
                void makeDead(MSRemoteReference ref) {
                    ref.refState = OBJ_DEAD;
                }
            },

            /**
             * Reference to an object that has been determined unreachable;
             * no assumptions may be made about memory contents at the location.
             */
            OBJ_DEAD ("DEAD object") {

                // Properties
                @Override ObjectStatus status() {
                    return DEAD;
                }

                @Override
                boolean isFreeSpace() {
                    return false;
                }

                // Transitions (none: death is final)

            },

            FREE_LIVE ("LIVE free chunk") {

                // Properties;
                @Override
                ObjectStatus status() {
                    return LIVE;
                }

                @Override
                boolean isFreeSpace() {
                    return false;
                }

               // Transitions
                @Override
                void makeDead(MSRemoteReference ref) {
                    ref.refState = FREE_DEAD;
                }
            },

            FREE_DEAD ("DEAD free chunk") {

                // Properties;
                @Override
                ObjectStatus status() {
                    return DEAD;
                }

                @Override
                boolean isFreeSpace() {
                    return false;
                }

                // Transitions (none: death is final)
            };

            protected final String label;

            RefState(String label) {
                this.label = label;
            }

            // Properties

            /**
             * @see RemoteReference#status()
             */
            abstract ObjectStatus status();

            /**
             * @see RemoteReference#origin()
             */
            final Address origin(MSRemoteReference ref) {
                return ref.origin;
            }

            final String gcDescription(MSRemoteReference ref) {
                return label;
            }

            /**
             * @see RemoteReference#isFreeSpace()
             */
            abstract boolean isFreeSpace();

            // Transitions

            /**
             * @see MSRemoteReference#analysisBegins()
             */
            void makeUnknown(MSRemoteReference ref) {
                TeleError.unexpected("Illegal state transition");
            }

            /**
             * @see MSRemoteReference#addFromOrigin()
             */
            void makeLive(MSRemoteReference ref) {
                TeleError.unexpected("Illegal state transition");
            }

            /**
             * @see MSRemoteReference#analysisEnds()
             */
            void makeDead(MSRemoteReference ref) {
                TeleError.unexpected("Illegal state transition");
            }

        }

        public static final class RefStateCount {
            public final String stateName;
            public final long count;
            private RefStateCount(RefState state, long count) {
                this.stateName = state.label;
                this.count = count;
            }
        }

        public static List<RefStateCount> getStateCounts(List<MSRemoteReference> refs) {
            final long[] refCounts = new long[RefState.values().length];
            final List<RefStateCount> stateCounts = new ArrayList<RefStateCount>();
            for (MSRemoteReference ref : refs) {
                refCounts[ref.refState.ordinal()]++;
            }
            for (int i = 0; i < RefState.values().length; i++) {
                stateCounts.add(new RefStateCount(RefState.values()[i], refCounts[i]));
            }
            return stateCounts;
        }

        public static MSRemoteReference createLive(RemoteMSHeapScheme remoteScheme, Address origin) {
            final MSRemoteReference ref = new MSRemoteReference(remoteScheme, origin);
            ref.refState = RefState.OBJ_LIVE;
            return ref;
        }

        public static MSRemoteReference createFree(RemoteMSHeapScheme remoteScheme, Address origin) {
            final MSRemoteReference ref = new MSRemoteReference(remoteScheme, origin);
            ref.refState = RefState.FREE_LIVE;
            return ref;
        }

        /**
         * The origin of the object when it is in Object-Space.
         */
        private Address origin;

        /**
         * The current state of the reference with respect to
         * where the object is located, what heap phase we might
         * be in, and whether the object is live, forwarded, or dead.
         */
        private RefState refState = null;

        private final RemoteMSHeapScheme remoteScheme;

        private MSRemoteReference(RemoteMSHeapScheme remoteScheme, Address origin) {
            super(remoteScheme.vm());
            this.origin = origin;
            this.remoteScheme = remoteScheme;
        }

        @Override
        public ObjectStatus status() {
            return refState.status();
        }

        @Override
        public Address origin() {
            return origin;
        }

        @Override
        public boolean isForwarded() {
            return false;
        }

        @Override
        public Address forwardedFrom() {
            return Address.zero();
        }

        @Override
        public String gcDescription() {
            return remoteScheme.heapSchemeClass().getSimpleName() + " state=" + refState.gcDescription(this);
        }

        @Override
        public boolean isFreeSpace() {
            return refState.isFreeSpace();
        }

        void makeUnknown() {
            refState.makeUnknown(this);
        }


        void makeLive(MSRemoteReference ref) {
            refState.makeLive(this);
        }

        void makeDead(MSRemoteReference ref) {
            refState.makeDead(this);
        }
    }

}
