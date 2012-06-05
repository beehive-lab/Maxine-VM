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
package com.sun.max.tele.reference.semispace;

import static com.sun.max.tele.object.RemoteObjectStatus.*;

import java.util.*;

import com.sun.max.tele.heap.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

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
         * Live reference in To-Space, heap not {@link #ANALYZING}, not forwarded.
         */
        REF_LIVE ("LIVE (not Analyzing)"){

            // Properties
            @Override
            RemoteObjectStatus status() {
                return LIVE;
            }
            @Override
            Address origin(SemiSpaceRemoteReference ref) {
                return ref.origin;
            }
            @Override
            Address forwardedFrom(SemiSpaceRemoteReference ref) {
                return Address.zero();
            }
            @Override
            Address forwardedTo(SemiSpaceRemoteReference ref) {
                return Address.zero();
            }

            // Transitions
            @Override
            void beginAnalyzing(SemiSpaceRemoteReference ref) {
                ref.refState = REF_FROM;
            }
        },

        /**
         * Reference in From-Space, heap {@link #ANALYZING}, not forwarded.
         */
        REF_FROM ("LIVE (Analyzing: From-only), not forwarded"){

            // Properties
            @Override RemoteObjectStatus status() {
                return LIVE;
            }
            @Override
            Address origin(SemiSpaceRemoteReference ref) {
                return ref.origin;
            }
            @Override
            Address forwardedFrom(SemiSpaceRemoteReference ref) {
                return Address.zero();
            }
            @Override
            Address forwardedTo(SemiSpaceRemoteReference ref) {
                return Address.zero();
            }

            // Transitions
            @Override
            void discoverForwarded(SemiSpaceRemoteReference ref, Address newOrigin) {
                ref.alternateOrigin = ref.origin;
                ref.origin = newOrigin;
                ref.refState = REF_FROM_TO;
            }
            @Override
            void endAnalyzing(SemiSpaceRemoteReference ref) {
                ref.refState = REF_DEAD;
            }
        },

        /**
         * Reference in To-Space, heap {@link #ANALYZING}, presumed to be forwarded new copy, location of original copy is unknown.
         */
        REF_TO("LIVE (Analyzing: To only)") {

            // Properties
            @Override RemoteObjectStatus status() {
                return LIVE;
            }
            @Override
            Address origin(SemiSpaceRemoteReference ref) {
                return ref.origin;
            }
            @Override
            Address forwardedFrom(SemiSpaceRemoteReference ref) {
                return Address.zero();
            }
            @Override
            Address forwardedTo(SemiSpaceRemoteReference ref) {
                return Address.zero();
            }

            // Transitions
            @Override
            void discoverOldOrigin(SemiSpaceRemoteReference ref, Address oldOrigin) {
                ref.alternateOrigin = oldOrigin;
                ref.refState = REF_FROM_TO;
            }
            @Override
            void endAnalyzing(SemiSpaceRemoteReference ref) {
                ref.refState = REF_LIVE;
            }
        },

        REF_FROM_TO ("LIVE (Analyzing: From+To)") {

            // Properties
            @Override RemoteObjectStatus status() {
                return LIVE;
            }
            @Override
            Address origin(SemiSpaceRemoteReference ref) {
                return ref.origin;
            }
            @Override
            Address forwardedFrom(SemiSpaceRemoteReference ref) {
                return ref.alternateOrigin;
            }
            @Override
            Address forwardedTo(SemiSpaceRemoteReference ref) {
                return Address.zero();
            }

            // Transitions
            @Override
            void endAnalyzing(SemiSpaceRemoteReference ref) {
                ref.alternateOrigin = Address.zero();
                ref.refState = REF_LIVE;
            }
        },

        REF_FORWARDER ("FORWARDER (Quasi object, only during Analyzing)") {

            // Properties
            @Override RemoteObjectStatus status() {
                return FORWARDER;
            }
            @Override
            Address origin(SemiSpaceRemoteReference ref) {
                return ref.origin;
            }
            @Override
            Address forwardedFrom(SemiSpaceRemoteReference ref) {
                return Address.zero();
            }
            @Override
            Address forwardedTo(SemiSpaceRemoteReference ref) {
                return ref.alternateOrigin;
            }

            // Transitions
            @Override
            void endAnalyzing(SemiSpaceRemoteReference ref) {
                ref.alternateOrigin = Address.zero();
                ref.refState = REF_DEAD;
            }
        },

        REF_DEAD ("Dead") {

            // Properties
            @Override RemoteObjectStatus status() {
                return DEAD;
            }
            @Override
            Address origin(SemiSpaceRemoteReference ref) {
                return ref.origin;
            }
            @Override
            Address forwardedFrom(SemiSpaceRemoteReference ref) {
                return Address.zero();
            }
            @Override
            Address forwardedTo(SemiSpaceRemoteReference ref) {
                return Address.zero();
            }

            // Transitions (none: death is final)

        };

        protected final String label;

        RefState(String label) {
            this.label = label;
        }

        // Properties.  For clarity in the description of each state, no defaults specified.

        /**
         * @see RemoteReference#status()
         */
        abstract RemoteObjectStatus status();

        /**
         * @see RemoteReference#origin()
         */
        abstract Address origin(SemiSpaceRemoteReference ref);

        /**
         * @see RemoteReference#forwardedFrom()
         */
        abstract Address forwardedFrom(SemiSpaceRemoteReference ref);

        /**
         * @see RemoteReference#forwardedTo()
         */
        abstract Address forwardedTo(SemiSpaceRemoteReference ref);

        String gcDescription(SemiSpaceRemoteReference ref) {
            return label;
        }

        // Transitions: default is Illegal

        /**
         * @see SemiSpaceRemoteReference#beginAnalyzing()
         */
        void beginAnalyzing(SemiSpaceRemoteReference ref) {
            TeleError.unexpected("Illegal state transition");
        }

        /**
         * @see SemiSpaceRemoteReference#addFromOrigin()
         */
        void discoverOldOrigin(SemiSpaceRemoteReference ref, Address fromOrigin) {
            TeleError.unexpected("Illegal state transition");
        }

        /**
         * @see SemiSpaceRemoteReference#forward()
         */
        void discoverForwarded(SemiSpaceRemoteReference ref, Address toOrigin) {
            TeleError.unexpected("Illegal state transition");
        }

        /**
         * @see SemiSpaceRemoteReference#endAnalyzing()
         */
        void endAnalyzing(SemiSpaceRemoteReference ref) {
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
     * @param origin the location of the object in VM To-Space.
     * @return a remote reference to an ordinary live object
     */
    public static SemiSpaceRemoteReference createLive(AbstractRemoteHeapScheme remoteScheme, Address origin) {
        return new SemiSpaceRemoteReference(remoteScheme, RefState.REF_LIVE, origin);
    }

    /**
     * Creates a reference to an object discovered in From-Space when the heap is {@link #ANALYZING}, but
     * without a forwarding pointer.
     * In this situation, the object is presumed to be in an unknown state with respect to reachability.
     *
     * @param origin the location of the object in VM From-Space.
     * @return a remote reference to the an object in From-Space with unknown reachability
     */
    public static SemiSpaceRemoteReference createInFromOnly(AbstractRemoteHeapScheme remoteScheme, Address origin) {
        return new SemiSpaceRemoteReference(remoteScheme, RefState.REF_FROM, origin);
    }

    /**
     * Creates a reference to an object discovered in To-Space when the heap is {@link #ANALYZING}.
     * In this situation, the object is presumed to be a forwarded copy of an object in From-Space, even though
     * the location of the old copy is not (yet) known.
     *
     * @param origin the location of the object in VM To-Space.
     * @return a remote reference to the new copy of a forwarded object in To-Space
     */
    public static SemiSpaceRemoteReference createInToOnly(AbstractRemoteHeapScheme remoteScheme, Address origin) {
        return new SemiSpaceRemoteReference(remoteScheme, RefState.REF_TO, origin);
    }

    /**
     * Creates a reference to a forwarded object discovered when the heap is {@link #ANALYZING}.
     * @param fromSpaceOrigin the location of the old copy in VM From-Space.
     * @param toSpaceOrigin the location of the new, live copy of the object in VM To-Space.
     *
     * @return a remote reference to the new copy of the forwarded object
     */
    public static SemiSpaceRemoteReference createInFromTo(AbstractRemoteHeapScheme remoteScheme, Address fromSpaceOrigin, Address toSpaceOrigin) {
        return new SemiSpaceRemoteReference(remoteScheme, RefState.REF_FROM_TO, toSpaceOrigin, fromSpaceOrigin);
    }

    /**
     * Creates a quasi-reference to the old copy of a forwarded object.
     *
     * @param fromSpaceOrigin the location of the forwarding object in VM From-Space
     * @param toSpaceOrigin the location of the new, live copy in VM To-Space
     * @return a quasi remote reference to the forwarding object in From-Space, which will only live until the heap
     * is finished {@link #ANALYZING}.
     */
    public static SemiSpaceRemoteReference createForwarder(AbstractRemoteHeapScheme remoteScheme, Address fromSpaceOrigin, Address toSpaceOrigin) {
        return new SemiSpaceRemoteReference(remoteScheme, RefState.REF_FORWARDER, fromSpaceOrigin, toSpaceOrigin);
    }

    /**
     * The origin of the object, usually in To-Space but possibly in From-Space
     * during ANALYZING.
     */
    private Address origin;

    /**
     * During ANALYZING, when the object is known to have been forwarded, the location of the <em>other</em> copy of the object.
     */
    private Address alternateOrigin;

    /**
     * The current state of the reference with respect to
     * where the object is located, what heap phase we might
     * be in, and whether the object is live, forwarded, or dead.
     */
    private RefState refState = null;

    private final AbstractRemoteHeapScheme remoteScheme;

    private SemiSpaceRemoteReference(AbstractRemoteHeapScheme remoteScheme, RefState refState, Address origin, Address alternateOrigin) {
        super(remoteScheme.vm());
        this.remoteScheme = remoteScheme;
        this.refState = refState;
        this.origin = origin;
        this.alternateOrigin = alternateOrigin;
    }

    private SemiSpaceRemoteReference(AbstractRemoteHeapScheme remoteScheme, RefState refState, Address origin) {
        this(remoteScheme, refState, origin, Address.zero());
    }

    @Override
    public RemoteObjectStatus status() {
        return refState.status();
    }

    @Override
    public Address origin() {
        return refState.origin(this);
    }

    @Override
    public Address forwardedFrom() {
        return refState.forwardedFrom(this);
    }

    @Override
    public Address forwardedTo() {
        return refState.forwardedTo(this);
    }

    @Override
    public String gcDescription() {
        return remoteScheme.heapSchemeClass().getSimpleName() + " state=" + refState.gcDescription(this);
    }

    /**
     * State transition on an ordinary live reference in To-Space when an {@link #ANALYZING} phase is discovered to
     * have begun, and in particular when the heap spaces have been swapped. This transition happens before any
     * attempt is made to discover the outcome of any tracing and possible forwarding; it models the state of a
     * live reference immediately after the spaces are swapped.
     * <p>
     * <strong>Pre:</strong> An ordinary {@link #LIVE} object in To-Space, not forwarded, with the heap phase
     * {@link #MUTATING} (or possibly in the latter part of {@link #RECLAIMING}).
     * <p>
     * <strong>Post:</strong> An object whose origin is in From-Space and presumed to be {@link lIVE} during
     * an {@link #ANALYZING} heap phase in which the object's reachability has not yet been determined.
     */
    public void beginAnalyzing() {
        refState.beginAnalyzing(this);
    }

    /**
     * State transition during {@link #ANALYZING} when the previously unknown original copy of a
     * forwarded object is discovered.
     * <p>
     * <strong>Pre:</strong> A reference to an object in To-Space, during {@link #ANALYZING}, that
     * is assumed to be a forwarded copy of some object, but the location of the original copy
     * in From-Space is unknown.
     * <p>
     * <strong>Post:</strong> A forwarded object in To-Space, during {@link #ANALYZING}, whose original
     * location in From-Space is known.
     *
     * @param oldOrigin newly discovered old copy in From-Space of a forwarded object
     */
    public void discoverOldOrigin(Address oldOrigin) {
        refState.discoverOldOrigin(this, oldOrigin);
    }

    /**
     * State transition during {@link #ANALYZING} when an object in From-Space is first discovered to be forwarded.
     * <p>
     * <strong>Pre:</strong> A reference to an object in From-Space, during {@link #ANALYZING}, that has not
     * yet been discovered to be forwarded, i.e. in an unknown state of reachability.
     * <p>
     * <strong>Post:</strong> A reference to an object in To-Space (by definition forwarded and thus reachable), whose
     * original location (the previously value of the origin) is also known.
     *
     * @param newOrigin location of the new copy of the forwarded object in To-Space
     */
    public void discoverForwarded(Address newOrigin) {
        refState.discoverForwarded(this, newOrigin);
    }

    /**
     * State transition on a reference at the end of an {@link #ANALYZING} phase, when
     * all the tracing and forwarding is complete.
     * <p>
     * <strong>Pre:</strong> A reference held during an {@link #ANALYZING} heap phase.
     * <p>
     * <strong>Post:</strong> A reference that is either {@link #LIVE} or {@link #DEAD},
     * depending on its reachability discovered during {@link #ANALYZING}
     */
    public void endAnalyzing() {
        refState.endAnalyzing(this);
    }

}
