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
package com.sun.max.tele.heap.semispace;

import static com.sun.max.vm.heap.ObjectStatus.*;

import java.util.*;

import com.sun.max.tele.heap.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

/**
 * Representation of a remote object reference in a heap region managed by a semispace GC. The states of the reference
 * represent possible states of knowledge about the particular object, especially those relevant during the
 * {@link #ANALYZING} phase of the heap.
 *
 * @see <a href="http://en.wikipedia.org/wiki/State_pattern">"State" design pattern</a>
 */
public class SemiSpaceRemoteReference extends RemoteReference {

    /**
     * An enumeration of possible states of a remote reference for this kind of collector, based on the heap phase and
     * what is known at any given time.
     * <p>
     * Each member encapsulates the <em>behavior</em> associated with a state, including both the interpretation of
     * the data held by the reference and by allowable state transitions.
     */
    private static enum RefState {

        /**
         * Live reference in To-Space, heap not {@link #ANALYZING}.
         */
        REF_LIVE ("LIVE (not Analyzing)"){

            // Properties
            @Override
            ObjectStatus status() {
                return LIVE;
            }
            @Override
            boolean isForwarded() {
                return false;
            }
            @Override
            Address origin(SemiSpaceRemoteReference ref) {
                return ref.toOrigin;
            }
            @Override
            Address forwardedFrom(SemiSpaceRemoteReference ref) {
                return Address.zero();
            }

            // Transitions
            @Override
            void analysisBegins(SemiSpaceRemoteReference ref) {
                ref.fromOrigin = ref.toOrigin;
                ref.toOrigin = Address.zero();
                ref.refState = REF_FROM;
            }
        },

        /**
         * Reference in From-Space, heap {@link #ANALYZING}, not forwarded.
         */
        REF_FROM ("UNKNOWN (Analyzing: From-only)"){

            // Properties
            @Override ObjectStatus status() {
                return UNKNOWN;
            }
            @Override boolean isForwarded() {
                return false;
            }
            @Override
            Address origin(SemiSpaceRemoteReference ref) {
                return ref.fromOrigin;
            }
            @Override
            Address forwardedFrom(SemiSpaceRemoteReference ref) {
                return Address.zero();
            }

            // Transitions
            @Override
            void analysisEnds(SemiSpaceRemoteReference ref) {
                // For consistency, when dead leave the last real origin in toOrigin.
                ref.toOrigin = ref.fromOrigin;
                ref.fromOrigin = Address.zero();
                ref.refState = REF_DEAD;
            }
            @Override
            void addToOrigin(SemiSpaceRemoteReference ref, Address toOrigin) {
                ref.toOrigin = toOrigin;
                ref.refState = REF_FROM_TO;
            }
        },

        /**
         * Reference in To-Space, heap {@link #ANALYZING}, presumed to be forwarded new copy, old copy not discovered.
         */
        REF_TO("LIVE (Analyzing: To only)") {

            // Properties
            @Override ObjectStatus status() {
                return LIVE;
            }
            @Override boolean isForwarded() {
                return true;
            }
            @Override
            Address origin(SemiSpaceRemoteReference ref) {
                return ref.toOrigin;
            }
            @Override
            Address forwardedFrom(SemiSpaceRemoteReference ref) {
                return Address.zero();
            }

            // Transitions
            @Override
            void analysisEnds(SemiSpaceRemoteReference ref) {
                ref.refState = REF_LIVE;
            }
            @Override
            void addFromOrigin(SemiSpaceRemoteReference ref, Address fromOrigin) {
                ref.fromOrigin = fromOrigin;
                ref.refState = REF_FROM_TO;
            }
        },

        REF_FROM_TO ("LIVE (Analyzing: From+To)") {

            // Properties
            @Override ObjectStatus status() {
                return LIVE;
            }
            @Override boolean isForwarded() {
                return true;
            }
            @Override
            Address origin(SemiSpaceRemoteReference ref) {
                return ref.toOrigin;
            }
            @Override
            Address forwardedFrom(SemiSpaceRemoteReference ref) {
                return ref.fromOrigin;
            }

            // Transitions
            @Override
            void analysisEnds(SemiSpaceRemoteReference ref) {
                ref.fromOrigin = Address.zero();
                ref.refState = REF_LIVE;
            }
        },

        REF_DEAD ("Dead") {

            // Properties
            @Override ObjectStatus status() {
                return DEAD;
            }
            @Override boolean isForwarded() {
                return false;
            }
            @Override
            Address origin(SemiSpaceRemoteReference ref) {
                return ref.toOrigin;
            }
            @Override
            Address forwardedFrom(SemiSpaceRemoteReference ref) {
                return Address.zero();
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
         * @see RemoteReference#isForwarded()
         */
        abstract boolean isForwarded();

        /**
         * @see RemoteReference#origin()
         */
        abstract Address origin(SemiSpaceRemoteReference ref);

        /**
         * @see RemoteReference#forwardedFrom()
         */
        abstract Address forwardedFrom(SemiSpaceRemoteReference ref);

        String gcDescription(SemiSpaceRemoteReference ref) {
            return label;
        }

        // Transitions

        /**
         * @see SemiSpaceRemoteReference#analysisBegins()
         */
        void analysisBegins(SemiSpaceRemoteReference ref) {
            TeleError.unexpected("Illegal state transition");
        }

        /**
         * @see SemiSpaceRemoteReference#addFromOrigin()
         */
        void addFromOrigin(SemiSpaceRemoteReference ref, Address fromOrigin) {
            TeleError.unexpected("Illegal state transition");
        }

        /**
         * @see SemiSpaceRemoteReference#addToOrigin()
         */
        void addToOrigin(SemiSpaceRemoteReference ref, Address toOrigin) {
            TeleError.unexpected("Illegal state transition");
        }

        /**
         * @see SemiSpaceRemoteReference#analysisEnds()
         */
        void analysisEnds(SemiSpaceRemoteReference ref) {
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

    public static List<RefStateCount> getStateCounts(List<SemiSpaceRemoteReference> refs) {
        final long[] refCounts = new long[RefState.values().length];
        final List<RefStateCount> stateCounts = new ArrayList<RefStateCount>();
        for (SemiSpaceRemoteReference ref : refs) {
            refCounts[ref.refState.ordinal()]++;
        }
        for (int i = 0; i < RefState.values().length; i++) {
            stateCounts.add(new RefStateCount(RefState.values()[i], refCounts[i]));
        }
        return stateCounts;
    }

    /**
     * Creates a reference to an object, discovered in To-Space when the heap is <em>not</em> {@link #ANALYZING}.
     *
     * @param toOrigin the location of the object in VM To-Space.
     * @return a remote reference to an ordinary live object
     */
    public static SemiSpaceRemoteReference createLive(AbstractRemoteHeapScheme remoteScheme, Address toOrigin) {
        final SemiSpaceRemoteReference ref = new SemiSpaceRemoteReference(remoteScheme, Address.zero(), toOrigin);
        ref.refState = RefState.REF_LIVE;
        return ref;
    }

    /**
     * Creates a reference to an object discovered in From-Space when the heap is {@link #ANALYZING}, but
     * without a forwarding pointer.
     * In this situation, the object is presumed to be in an unknown state with respect to reachability.
     *
     * @param fromOrigin the location of the object in VM From-Space.
     * @return a remote reference to the an object in From-Space with unknown reachability
     */
    public static SemiSpaceRemoteReference createFromOnly(AbstractRemoteHeapScheme remoteScheme, Address fromOrigin) {
        final SemiSpaceRemoteReference ref = new SemiSpaceRemoteReference(remoteScheme, fromOrigin, Address.zero());
        ref.refState = RefState.REF_FROM;
        return ref;
    }

    /**
     * Creates a reference to an object discovered in To-Space when the heap is {@link #ANALYZING}.
     * In this situation, the object is presumed to be a forwarded copy of an object in From-Space, even though
     * the location of the old copy is not (yet) known.
     *
     * @param toOrigin the location of the object in VM To-Space.
     * @return a remote reference to the new copy of a forwarded object in To-Space
     */
    public static SemiSpaceRemoteReference createToOnly(AbstractRemoteHeapScheme remoteScheme, Address toOrigin) {
        final SemiSpaceRemoteReference ref = new SemiSpaceRemoteReference(remoteScheme, Address.zero(), toOrigin);
        ref.refState = RefState.REF_TO;
        return ref;
    }

    /**
     * Creates a reference to both copies of a forwarded object discovered when the heap is {@link #ANALYZING}.
     *
     * @param fromOrigin the location of the old object in VM From-Space.
     * @param toOrigin the location of the newly copied object in VM To-Space.
     * @return a remote reference to both copies of a forwarded object
     */
    public static SemiSpaceRemoteReference createFromTo(AbstractRemoteHeapScheme remoteScheme, Address fromOrigin, Address toOrigin) {
        final SemiSpaceRemoteReference ref = new SemiSpaceRemoteReference(remoteScheme, fromOrigin, toOrigin);
        ref.refState = RefState.REF_FROM_TO;
        return ref;
    }

    /**
     * The origin of the object when it is in To-Space (or when it is {@link #DEAD}).
     * It can only be zero during and {@link #ANALYZING} heap phase when it has not
     * been discovered to have been forwarded, in which case the only known origin
     * is in From-Space.
     */
    private Address toOrigin;

    /**
     * The origin of the object when it is in From-Space.  It can only be non-zero
     * during an {@link #ANALYZING} heap phase.  When it has been discovered to
     * be forwarded during this phase, a copy of the object can also be in To-Space.
     */
    private Address fromOrigin;

    /**
     * The current state of the reference with respect to
     * where the object is located, what heap phase we might
     * be in, and whether the object is live, forwarded, or dead.
     */
    private RefState refState = null;

    private final AbstractRemoteHeapScheme remoteScheme;

    private SemiSpaceRemoteReference(AbstractRemoteHeapScheme remoteScheme, Address fromOrigin, Address toOrigin) {
        super(remoteScheme.vm());
        this.fromOrigin = fromOrigin;
        this.toOrigin = toOrigin;
        this.remoteScheme = remoteScheme;
    }

    @Override
    public ObjectStatus status() {
        return refState.status();
    }

    @Override
    public Address origin() {
        return refState.origin(this);
    }

    @Override
    public boolean isForwarded() {
        return refState.isForwarded();
    }

    @Override
    public Address forwardedFrom() {
        return refState.forwardedFrom(this);
    }

    @Override
    public String gcDescription() {
        return remoteScheme.heapSchemeClass().getSimpleName() + " state=" + refState.gcDescription(this);
    }

    /**
     * State transition on an ordinary live reference in To-Space when an {@link #ANALYZING} phase is discovered to
     * have begun, and in particular when the heap spaces have been swapped. This transition happens before any
     * attempt is made to discover the outcome of any tracing and possible forwarding; it models the state of a
     * reference immediately after the spaces are swapped.
     * <p>
     * <strong>Pre:</strong> An ordinary {@link #LIVE} object in To-Space, not forwarded, with the heap phase
     * {@link #MUTATING} (or possibly in the latter part of {@link #RECLAIMING}).
     * <p>
     * <strong>Post:</strong> An object whose origin is in From-Space and whose reachability has not yet been
     * determined during an {@link #ANALYZING} heap phase.
     */
    public void analysisBegins() {
        refState.analysisBegins(this);
    }

    /**
     * State transition when the previously unknown old copy of a forwarded object is discovered.
     * <p>
     * <strong>Pre:</strong> A reference to an object in To-Space, during {@link #ANALYZING}, that
     * is assumed to be a forwarded new copy, but whose old copy in From-Space is unknown.
     * <p>
     * <strong>Post:</strong> A forwarded object, during {@link #ANALYZING}, both of whose copies are known.
     *
     * @param fromOrigin newly discovered old copy in From-Space of a forwarded object
     */
    public void addFromOrigin(Address fromOrigin) {
        refState.addFromOrigin(this, fromOrigin);
    }

    /**
     * State transition during {@link #ANALYZING} when an object in From-Space is discovered to be forwarded.
     * <p>
     * <strong>Pre:</strong> A reference to an object in From-Space, during {@link #ANALYZING}, that has not
     * yet been discovered to be forwarded, which is to say, in an unknown state of reachability.
     * <p>
     * <strong>Post:</strong> A reference to an object that is forwarded, and thus reachable, about which
     * both copies are known.
     *
     * @param toOrigin location of the new copy of the forwarded object in To-Space
     */
    public void addToOrigin(Address toOrigin) {
        refState.addToOrigin(this, toOrigin);
    }

    /**
     * State transition on a reference at the end of an {@link #ANALYZING} phase, when
     * all the tracing and forwarding is complete.
     * <p>
     * <strong>Pre:</strong> A reference held during an {@link #ANALYZING} heap phase.
     * <p>
     * <strong>Post:</strong> A reference that is either {@link #LIVE} or {@link #DEAD},
     * depending on what was learned about it while the heap was {@link #ANALYZING}
     */
    public void analysisEnds() {
        refState.analysisEnds(this);
    }

}
