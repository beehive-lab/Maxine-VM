/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.heap;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.grip.*;

/**
 * This class implements supports for collecting and processing special references
 * (i.e. instances of {@link java.lang.ref.Reference} and its subclasses) which
 * implement weak references and finalizers.
 * The routines in this class are called by the GC as it discovers reachable special
 * references, and after live objects have been processed.
 *
 * @author Ben L. Titzer
 */
public class SpecialReferenceManager {

    /**
     * This interface forms a contract between the GC algorithm and the implementation of special
     * references. The special reference implementation must be able to query the GC about
     * a particular reference as well as get its updated value, if it has one.
     *
     * @author Ben L. Titzer
     */
    public interface GripForwarder {
        boolean isReachable(Grip grip);
        Grip getForwardGrip(Grip grip);
    }

    private static final ReferenceFieldActor _nextField = getReferenceClassField("next");
    private static final ReferenceFieldActor _discoveredField = getReferenceClassField("discovered");
    private static final ReferenceFieldActor _referentField = getReferenceClassField("referent");
    private static final ReferenceFieldActor _pendingField = getReferenceClassField("pending");

    private static Grip _discoveredList;

    /**
     * This method processes the special reference objects that were discovered during the
     * GC's exploration of the heap. These live reference objects must be check to see whether
     * their "referent" objects have been collected. If so, they must be enqueued as "pending"
     * so that the {@link java.lang.ref.Reference.ReferenceHandler} thread can pick them up
     * and add them to their respective queues later.
     *
     * @param gripForwarder an object from the GC algorithm that can detect whether a grip
     * is live and can also return a forwarded version of the grip
     */
    public static void processDiscoveredSpecialReferences(GripForwarder gripForwarder) {
        // the first pass over the list finds the references that have referrents that are no longer reachable
        java.lang.ref.Reference ref = UnsafeLoophole.cast(_discoveredList.toJava());
        java.lang.ref.Reference last = UnsafeLoophole.cast(_pendingField.readStatic());
        while (ref != null) {
            final Grip referrent = Grip.fromJava(ref).readGrip(_referentField.offset());
            if (gripForwarder.isReachable(referrent)) {
                // this object is reachable, however the "referent" field was not scanned.
                // we need to update this field manually
                _referentField.writeObject(ref, gripForwarder.getForwardGrip(referrent));
            } else {
                _referentField.writeObject(ref, null);
                _nextField.writeObject(ref, last);
                last = ref;
            }
            ref = UnsafeLoophole.cast(_discoveredField.readObject(ref));
        }
        _pendingField.writeStatic(last);
        _discoveredList = Grip.fromOrigin(Pointer.zero());
    }

    /**
     * This method is called by the GC during heap exploration, when it finds a new special
     * reference object. This method checks to see whether the object has been processed previously,
     * and if not, then adds it to the queue to be processed later.
     *
     * @param grip
     */
    public static void discoverSpecialReference(Grip grip) {
        if (grip.readGrip(_nextField.offset()).isZero()) {
            // the "next" field of this object is null, queue it for later processing
            if (!_discoveredList.isZero()) {
                grip.writeGrip(_discoveredField.offset(), _discoveredList);
            }
            _discoveredList = grip;
        }
    }

    private static ReferenceFieldActor getReferenceClassField(String name) {
        final ClassActor referenceClass = ClassActor.fromJava(java.lang.ref.Reference.class);
        FieldActor fieldActor = referenceClass.findLocalStaticFieldActor(name);
        if (fieldActor == null) {
            fieldActor = referenceClass.findLocalInstanceFieldActor(name);
        }
        return (ReferenceFieldActor) fieldActor;
    }
}
