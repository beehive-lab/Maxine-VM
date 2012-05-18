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
package com.sun.max.tele.reference.gen.semispace;

import static com.sun.max.vm.heap.ObjectStatus.*;

import com.sun.max.tele.heap.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

/**
 * Representation of a remote object reference in a generational heap with a non-aging nursery and a semi-space old generation.
 */
public class GenSSRemoteReference extends RemoteReference {
    /**
     * Address in a to space the reference maps to. To spaces are spaces where live objects reside.
     * During mutating phases and analysis phase of minor collection, a reference may map to an address in the non-aging nursery (young generation) or the to space of the old generation.
     * After a minor collection, references can only map to the to space of the old generation.
     * During analysis of a old collection, a reference may map to an address in to space of the old generation only.
     */
    private Address toOrigin;
    /**
     * Address in a from space the reference maps to.
     * A reference can only map to a from space location during the analyzing phase of a collection. Objects with an address in
     * a from space are either live, or their status is unknown.
     */
    private Address fromOrigin;
    /**
     * State of an object reference. The state indicates where the object is located and its liveness status,
     */
    private RefState refState = null;

    private final AbstractRemoteHeapScheme remoteScheme;

    /**
     * An enumeration of possible states of a remote reference for this kind of collector, based on the heap phase and
     * what is known at any given time.
     * <p>
     * Each member encapsulates the <em>behavior</em> associated with a state, including both the interpretation of
     * the data held by the reference and by allowable state transitions.
     */
    private static enum RefState {
        /**
         * Live young reference.
         * Valid only during the {@link #MUTATING} phase.
         */
        YOUNG_REF_LIVE("LIVE(young)") {
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
            Address origin(GenSSRemoteReference ref) {
                return ref.toOrigin;
            }
            @Override
            Address forwardedFrom(GenSSRemoteReference ref) {
                return Address.zero();
            }

            @Override
            void analysisBegins(GenSSRemoteReference ref, boolean minorCollection) {
                if (minorCollection) {
                    ref.fromOrigin = ref.toOrigin;
                    ref.toOrigin = Address.zero();
                    ref.refState = YOUNG_REF_FROM;
                    return;
                }
                TeleError.unexpected("Illegal state transition");
            }
       },
        /**
         * Young reference, not forwarded.
         * Valid only during the {@link #ANALYZING} phase.
         */
        YOUNG_REF_FROM("UNKNOWN(young)") {

            @Override
            ObjectStatus status() {
                return UNKNOWN;
            }

            @Override
            boolean isForwarded() {
                return false;
            }

            @Override
            Address origin(GenSSRemoteReference ref) {
                return ref.fromOrigin;
            }

            @Override
            Address forwardedFrom(GenSSRemoteReference ref) {
                return Address.zero();
            }

            @Override
            void analysisEnds(GenSSRemoteReference ref, boolean minorCollection) {
                if (minorCollection) {
                    ref.refState = REF_DEAD;
                    return;
                }
                TeleError.unexpected("Illegal state transition");
            }

            @Override
            void addToOrigin(GenSSRemoteReference ref, Address toOrigin, boolean minorCollection) {
                if (minorCollection) {
                    // FIXME: should also check the current heap phase!
                    ref.toOrigin = toOrigin;
                    ref.refState = PROMOTED_REF;
                    return;
                }
                TeleError.unexpected("Illegal state transition");
            }
        },

        OLD_PROMOTED_REF("LIVE(Analyzing: old only)") {

            @Override
            ObjectStatus status() {
                return LIVE;
            }

            @Override
            boolean isForwarded() {
                return true;
            }

            @Override
            Address origin(GenSSRemoteReference ref) {
                return ref.toOrigin;
            }

            @Override
            Address forwardedFrom(GenSSRemoteReference ref) {
                return Address.zero();
            }

            @Override
            void analysisEnds(GenSSRemoteReference ref, boolean minorCollection) {
                if (minorCollection) {
                    ref.refState = OLD_REF_LIVE;
                    return;
                }
                TeleError.unexpected("Illegal state transition");
            }
            @Override
            void addFromOrigin(GenSSRemoteReference ref, Address fromOrigin, boolean minorCollection) {
                if (minorCollection) {
                    ref.fromOrigin = fromOrigin;
                    ref.refState = PROMOTED_REF;
                    return;
                }
                TeleError.unexpected("Illegal state transition");
            }
        },

        PROMOTED_REF("LIVE(Analyzing: young+old)") {

            @Override
            ObjectStatus status() {
                return LIVE;
            }

            @Override
            boolean isForwarded() {
                return true;
            }

            @Override
            Address origin(GenSSRemoteReference ref) {
                return ref.toOrigin;
            }

            @Override
            Address forwardedFrom(GenSSRemoteReference ref) {
                return ref.fromOrigin;
            }
            @Override
            void analysisEnds(GenSSRemoteReference ref, boolean minorCollection) {
                if (minorCollection) {
                    ref.fromOrigin = Address.zero();
                    ref.refState = OLD_REF_LIVE;
                    return;
                }
                TeleError.unexpected("Illegal state transition");
            }
        },

        /**
         * Live old object.
         * Valid during the {@link #MUTATING}, and {@link HeapPhase#ANALYZING} of minor collections.
         */
        OLD_REF_LIVE("LIVE(old)") {

            @Override
            ObjectStatus status() {
                return LIVE;
            }

            @Override
            boolean isForwarded() {
                return false;
            }

            @Override
            Address origin(GenSSRemoteReference ref) {
                return ref.toOrigin;
            }

            @Override
            Address forwardedFrom(GenSSRemoteReference ref) {
                return Address.zero();
            }

            @Override
            void analysisBegins(GenSSRemoteReference ref, boolean minorCollection) {
                if (minorCollection) {
                    // nothing to do.
                    return;
                }
                ref.fromOrigin = ref.toOrigin;
                ref.toOrigin = Address.zero();
                ref.refState = OLD_REF_FROM;
            }
        },

        OLD_REF_FROM("UNKNOWN (Analyzing: old from-only)") {
            @Override
            ObjectStatus status() {
                return UNKNOWN;
            }

            @Override
            boolean isForwarded() {
                return false;
            }

            @Override
            Address origin(GenSSRemoteReference ref) {
                return ref.fromOrigin;
            }

            @Override
            Address forwardedFrom(GenSSRemoteReference ref) {
                return Address.zero();
            }

            @Override
            void analysisEnds(GenSSRemoteReference ref, boolean minorCollection) {
                if (!minorCollection) {
                    ref.refState = REF_DEAD;
                    return;
                }
                TeleError.unexpected("Illegal state transition");
            }

            @Override
            void addToOrigin(GenSSRemoteReference ref, Address toOrigin, boolean minorCollection) {
                if (!minorCollection) {
                    ref.toOrigin = toOrigin;
                    ref.refState = OLD_REF_FROM_TO;
                    return;
                }
                TeleError.unexpected("Illegal state transition");
            }

        },

        OLD_REF_TO("LIVE (Analyzing: old to-only)") {

            @Override
            ObjectStatus status() {
                return LIVE;
            }

            @Override
            boolean isForwarded() {
                return true;
            }

            @Override
            Address origin(GenSSRemoteReference ref) {
                return ref.toOrigin;
            }

            @Override
            Address forwardedFrom(GenSSRemoteReference ref) {
                return Address.zero();
            }

            @Override
            void analysisEnds(GenSSRemoteReference ref, boolean minorCollection) {
                if (!minorCollection) {
                    ref.refState = OLD_REF_LIVE;
                    return;
                }
                TeleError.unexpected("Illegal state transition");
            }
            @Override
            void addFromOrigin(GenSSRemoteReference ref, Address fromOrigin, boolean minorCollection) {
                if (!minorCollection) {
                    ref.fromOrigin = fromOrigin;
                    ref.refState = OLD_REF_FROM_TO;
                    return;
                }
                TeleError.unexpected("Illegal state transition");
            }
       },

        /**
         * Reference in from space of old generation (not in promotion space). Must only be seen during full collection's {@link #ANALYZING} phase.
         */
        OLD_REF_FROM_TO("LIVE (Analyzing: old from+to)") {
            @Override
            ObjectStatus status() {
                return LIVE;
            }

            @Override
            boolean isForwarded() {
                return true;
            }

            @Override
            Address origin(GenSSRemoteReference ref) {
                return ref.toOrigin;
            }

            @Override
            Address forwardedFrom(GenSSRemoteReference ref) {
                return ref.fromOrigin;
            }

            @Override
            void analysisEnds(GenSSRemoteReference ref, boolean minorCollection) {
                if (!minorCollection) {
                    ref.fromOrigin = Address.zero();
                    ref.refState = OLD_REF_LIVE;
                    return;
                }
                TeleError.unexpected("Illegal state transition");
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
            Address origin(GenSSRemoteReference ref) {
                return ref.fromOrigin;
            }
            @Override
            Address forwardedFrom(GenSSRemoteReference ref) {
                return Address.zero();
            }
        };

        protected final String label;

        RefState(String label) {
            this.label = label;
        }

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
        abstract Address origin(GenSSRemoteReference ref);

        /**
         * @see RemoteReference#forwardedFrom()
         */
        abstract Address forwardedFrom(GenSSRemoteReference ref);

        String gcDescription(GenSSRemoteReference ref) {
            return label;
        }

        /**
         * Apply state transition to a reference upon beginning of an analysis phase.
         * @param ref reference the state transition applies to
         * @param minorCollection true if the analysis phase if for a minor collection.
         */
        void analysisBegins(GenSSRemoteReference ref, boolean minorCollection) {
            TeleError.unexpected("Illegal state transition");
        }
        /**
         * Apply state transition to a reference upon end an analysis phase.
         *
         * @param ref reference the state transition applies to
         * @param minorCollection true if the analysis phase if for a minor collection.
         */
        void analysisEnds(GenSSRemoteReference ref, boolean minorCollection) {
            TeleError.unexpected("Illegal state transition");
        }

        void addFromOrigin(GenSSRemoteReference ref, Address fromOrigin, boolean minorCollection) {
            TeleError.unexpected("Illegal state transition");
        }
        void addToOrigin(GenSSRemoteReference ref, Address toOrigin, boolean minorCollection) {
            TeleError.unexpected("Illegal state transition");
        }
    }

    protected GenSSRemoteReference(AbstractRemoteHeapScheme remoteScheme, Address fromOrigin, Address toOrigin) {
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

    public void analysisBegins(boolean minorCollection) {
        refState.analysisBegins(this, minorCollection);
    }

    public void analysisEnds(boolean minorCollection) {
        refState.analysisEnds(this, minorCollection);
    }

    public void addFromOrigin(Address fromOrigin, boolean minorCollection) {
        refState.addFromOrigin(this, fromOrigin, minorCollection);
    }
    public void addToOrigin(Address toOrigin, boolean minorCollection) {
        refState.addToOrigin(this, toOrigin, minorCollection);
    }

    /**
     * Creates a reference to a live object, discovered when the heap is <em>not</em> {@link #ANALYZING}.
     * @param remoteScheme
     * @param toOrigin the physical location of the object in virtual memory.
     * @param isYoung true if the object is located in the nursery. Otherwise the object is in the old To-space.
     * @return a remote reference to an live object
     */
    public static GenSSRemoteReference createLive(AbstractRemoteHeapScheme remoteScheme, Address toOrigin, boolean isYoung) {
        TeleError.check(((RemoteGenSSHeapScheme) remoteScheme).canCreateLive());
        final GenSSRemoteReference ref = new GenSSRemoteReference(remoteScheme, Address.zero(), toOrigin);
        ref.refState = isYoung ? RefState.YOUNG_REF_LIVE : RefState.OLD_REF_LIVE;
        return ref;
    }

    public static GenSSRemoteReference createOldTo(AbstractRemoteHeapScheme remoteScheme, Address toOrigin, boolean isPromoted) {
        final GenSSRemoteReference ref = new GenSSRemoteReference(remoteScheme, Address.zero(), toOrigin);
        ref.refState = isPromoted ? RefState.OLD_PROMOTED_REF : RefState.OLD_REF_TO;
        return ref;
    }

    public static GenSSRemoteReference createFromOnly(AbstractRemoteHeapScheme remoteScheme, Address fromOrigin, boolean isYoung) {
        final GenSSRemoteReference ref = new GenSSRemoteReference(remoteScheme, fromOrigin, Address.zero());
        ref.refState = isYoung ? RefState.YOUNG_REF_FROM : RefState.OLD_REF_FROM;
        return ref;
    }

    public static GenSSRemoteReference createFromTo(AbstractRemoteHeapScheme remoteScheme, Address fromOrigin, Address toOrigin, boolean isYoung) {
        final GenSSRemoteReference ref = new GenSSRemoteReference(remoteScheme, fromOrigin, toOrigin);
        ref.refState = isYoung ? RefState.PROMOTED_REF  : RefState.OLD_REF_FROM_TO;
        return ref;
    }

    public static void checkNoLiveRef(WeakRemoteReferenceMap<GenSSRemoteReference> map, boolean isYoung) {
        final RefState prohibitedRefState =  isYoung ? RefState.YOUNG_REF_LIVE : RefState.OLD_REF_LIVE;
        for (GenSSRemoteReference ref : map.values()) {
            TeleError.check(ref.refState != prohibitedRefState);
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(refState.label);
        sb.append(" to: ");
        sb.append(toOrigin == null ? "<null>" : toOrigin.to0xHexString());
        sb.append(" from: ");
        sb.append(fromOrigin == null ? "<null>" : fromOrigin.to0xHexString());
        return sb.toString();
    }
}
