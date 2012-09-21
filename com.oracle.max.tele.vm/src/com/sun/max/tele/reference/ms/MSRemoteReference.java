/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.reference.ms;

import static com.sun.max.tele.object.ObjectStatus.*;

import java.util.*;

import com.sun.max.tele.heap.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.*;

/**
 * Representation of a remote object reference in a heap region managed by a mark sweep GC. The states of the reference
 * represent what can be known about the object at any given time.
 *
 * @see <a href="http://en.wikipedia.org/wiki/State_pattern">"State" design pattern</a>
 */
public class MSRemoteReference extends  RemoteReference {
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
    static enum RefState {

        /**
         * Reference to a reachable object.
         */
        OBJ_LIVE ("LIVE object"){

            // Properties
            @Override
            ObjectStatus status() {
                return LIVE;
            }

            // Transitions

            @Override
            void discoveredUnreachable(MSRemoteReference ref) {
                ref.refState = OBJ_UNREACHABLE;
            }

            @Override
            void die(MSRemoteReference ref) {
                ref.refState = OBJ_DEAD;
            }
        },

        /**
         * Reference to an object found unreachable, heap {@link HeapPhase#RECLAIMING}, modeling
         * the state where the GC has not yet reclaimed the space.
         */
        OBJ_UNREACHABLE ("UNREACHABLE object (Reclaiming)") {

            // Properties
            @Override ObjectStatus status() {
                return UNREACHABLE;
            }

            // Transitions

            @Override
            void die(MSRemoteReference ref) {
                ref.refState = OBJ_DEAD;
            }
        },

        /**
         * Reference to an object that has been determined unreachable, and which should be forgotten;
         * no assumptions may be made about memory contents at the location.
         */
        OBJ_DEAD ("DEAD object") {

            // Properties
            @Override ObjectStatus status() {
                return DEAD;
            }

            // Transitions (none: death is final)

        },

        FREE_CHUNK ("Chunk of allocatable free memory") {

            // Properties;
            @Override
            ObjectStatus status() {
                return FREE;
            }

           // Transitions
            @Override
            void release(MSRemoteReference ref) {
                ref.refState = FREE_RELEASED;
            }
        },

        FREE_DARK ("Chunk of unallocatable free memory") {

            // Properties;
            @Override
            ObjectStatus status() {
                return DARK;
            }

           // Transitions
            @Override
            void release(MSRemoteReference ref) {
                ref.refState = FREE_RELEASED;
            }
        },

        FREE_RELEASED ("Released free space") {

            // Properties;
            @Override
            ObjectStatus status() {
                return DEAD;
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

        // Transitions

        /**
         * @see MSRemoteReference#discoveredReachable()
         */
        void discoveredUnreachable(MSRemoteReference ref) {
            TeleError.unexpected("Illegal state transition");
        }

        /**
         * @see MSRemoteReference#die()
         */
        void die(MSRemoteReference ref) {
            TeleError.unexpected("Illegal state transition");
        }


        /**
         * @see MSRemoteReference#release()
         */
        void release(MSRemoteReference ref) {
            TeleError.unexpected("Illegal state transition");
        }


    }

    public static final class RefStateCount {
        public final String stateName;
        public final long count;
        private RefStateCount(MSRemoteReference.RefState state, long count) {
            this.stateName = state.label;
            this.count = count;
        }
    }

    public static List<MSRemoteReference.RefStateCount> getStateCounts(List<MSRemoteReference> refs) {
        final long[] refCounts = new long[RefState.values().length];
        final List<MSRemoteReference.RefStateCount> stateCounts = new ArrayList<MSRemoteReference.RefStateCount>();
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
        ref.refState = RefState.FREE_CHUNK;
        return ref;
    }

    public static MSRemoteReference createDark(RemoteMSHeapScheme remoteScheme, Address origin) {
        final MSRemoteReference ref = new MSRemoteReference(remoteScheme, origin);
        ref.refState = RefState.FREE_DARK;
        return ref;
    }

    public static MSRemoteReference createUnreachable(RemoteMSHeapScheme remoteScheme, Address origin) {
        final MSRemoteReference ref = new MSRemoteReference(remoteScheme, origin);
        ref.refState = RefState.OBJ_UNREACHABLE;
        return ref;
    }

    /**
     * The origin of the object when it is in Object-Space.
     */
    private Address origin;

    private ObjectStatus priorStatus = null;

    /**
     * The current state of the reference with respect to
     * where the object is located, what heap phase we might
     * be in, and whether the object is live, forwarded, or dead.
     */
    private MSRemoteReference.RefState refState = null;

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
    public ObjectStatus priorStatus() {
        return priorStatus;
    }

    @Override
    public Address origin() {
        return origin;
    }

    @Override
    public Address forwardedFrom() {
        return Address.zero();
    }

    @Override
    public Address forwardedTo() {
        return Address.zero();
    }

    @Override
    public String gcDescription() {
        return remoteScheme.heapSchemeClass().getSimpleName() + " state=" + refState.gcDescription(this);
    }

    /**
     * State transition on an ordinary live object reference during {@link HeapPhase#ANALYZING} when an object is found to be reachable.
     */
    public void discoveredUnreachable() {
        priorStatus = refState.status();
        refState.discoveredUnreachable(this);
    }

    /**
     * State transition on any kind of object when it has been determined to be unreachable and should be forgotten.
     */
    public void die() {
        priorStatus = refState.status();
        refState.die(this);
    }

    /**
     * State transition on any kind of free space when it has been coalesced or allocated and should be forgotten.
     */
    public void release() {
        priorStatus = refState.status();
        refState.release(this);
    }
}
