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

import java.lang.ref.*;
import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.value.*;

/**
 * This class implements supports for collecting and processing special references
 * (i.e. instances of {@link Reference} and its subclasses) which
 * implement weak references and finalizers.
 * The routines in this class are called by the GC as it discovers reachable special
 * references, and after live objects have been processed.
 *
 * @author Ben L. Titzer
 */
public class SpecialReferenceManager {

    private static final boolean FINALIZERS_SUPPORTED = true;

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
    private static final ReferenceFieldActor _lockField = getReferenceClassField("lock");
    // this method should be available and compiled
    private static final Object _lock = getLockObject();
    private static final CriticalMethod _registerMethod = new CriticalMethod(JDK.java_lang_ref_Finalizer.javaClass(), "register");
    static {
        MaxineVM.registerImageInvocationStub(_registerMethod.classMethodActor());
    }

    private static Grip _discoveredList;

    /**
     * This method processes the special reference objects that were
     * {@linkplain #discoverSpecialReference(Grip) discovered} during the
     * GC's exploration of the heap. These live reference objects must be checked to see whether
     * their "referent" objects have been collected. If so, they must be enqueued as "pending"
     * so that the {@link Reference.ReferenceHandler} thread can pick them up
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
            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Processed ");
                Log.print(ObjectAccess.readClassActor(ref).name().string());
                Log.print(" at ");
                Log.print(ObjectAccess.toOrigin(ref));
                Log.print(" whose referrent ");
                Log.print(referrent.toOrigin());
                final Object newReferrent = _referentField.readObject(ref);
                if (newReferrent == null) {
                    Log.println(" was unreachable");
                } else {
                    Log.print(" moved to ");
                    Log.println(ObjectAccess.toOrigin(newReferrent));
                }
                Log.unlock(lockDisabledSafepoints);
            }
            ref = UnsafeLoophole.cast(_discoveredField.readObject(ref));
        }
        _pendingField.writeStatic(last);
        _discoveredList = Grip.fromOrigin(Pointer.zero());
        if (last != null) {
            // if there are pending special references, notify the reference handler thread.
            // (note that the GC must already hold the lock object)
            getLockObject().notifyAll();
        }
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
            grip.writeGrip(_discoveredField.offset(), _discoveredList);
            _discoveredList = grip;
            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Added ");
                final Hub hub = UnsafeLoophole.cast(Layout.readHubReference(grip).toJava());
                Log.print(hub.classActor().name().string());
                Log.println(" to list of discovered references");
                Log.unlock(lockDisabledSafepoints);
            }
        }
    }

    /**
     * Registers an object that has a finalizer with the special reference manager.
     * A call to this method is inserted after allocation of such objects.
     * @param object the object that has a finalizers
     */
    public static void registerFinalizee(Object object) {
        if (FINALIZERS_SUPPORTED) {
            try {
                final ClassMethodActor methodActor = _registerMethod.classMethodActor();
                methodActor.invoke(ReferenceValue.from(object));
            } catch (Exception e) {
                throw ProgramError.unexpected(e);
            }
        }
    }

    /**
     * Initialize the SpecialReferenceManager when starting the VM. Normally, on the host
     * VM, the {@link java.lang.Reference} and {@link java.lang.Finalizer} classes create
     * threads in their static initializers to handle weak references and finalizable objects.
     * However, in the target VM, these classes have already been initialized and these
     * threads need to be started manually.
     *
     * @param phase the phase in which the VM is in
     */
    public static void initialize(Phase phase) {
        if (phase == Phase.STARTING) {
            startReferenceHandlerThread();
            startFinalizerThread();
        }
    }

    /**
     * This method gets the lock object associated with managing special references. This lock must
     * be held by the GC when it is updating the list of pending special references. This lock object
     * is allocated in {@link java.lang.Reference} and stored in its <code>lock</code> field.
     * Like {@link VmThread.ACTIVE}, this lock is special and requires a sticky monitor.
     *
     * @return the object that must be held by the GC
     */
    public static Object getLockObject() {
        if (MaxineVM.isPrototyping()) {
            try {
                final Field lockField = JDK.java_lang_ref_Reference.javaClass().getDeclaredField("lock");
                lockField.setAccessible(true);
                return lockField.get(null);
            } catch (Exception e) {
                throw ProgramError.unexpected(e);
            }
        }
        return _lock;
    }

    @PROTOTYPE_ONLY
    private static ReferenceFieldActor getReferenceClassField(String name) {
        final ClassActor referenceClass = ClassActor.fromJava(java.lang.ref.Reference.class);
        FieldActor fieldActor = referenceClass.findLocalStaticFieldActor(name);
        if (fieldActor == null) {
            fieldActor = referenceClass.findLocalInstanceFieldActor(name);
        }
        return (ReferenceFieldActor) fieldActor;
    }

    /**
     * Allocate and start a new thread to handle enqueuing weak references.
     */
    private static void startReferenceHandlerThread() {
        // Note: this code is stolen from java.lang.Reference <clinit>
        // we cannot simply rerun static initialization because it would allocate a new lock object
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        for (ThreadGroup tgn = tg; tgn != null; tg = tgn, tgn = tg.getParent()) {
            // do nothing; get the root thread group
        }
        final Class<?> javaClass = JDK.java_lang_ref_Reference$ReferenceHandler.javaClass();
        try {
            final Constructor constructor = javaClass.getDeclaredConstructor(ThreadGroup.class, String.class);
            constructor.setAccessible(true);
            final Thread handler = (Thread) constructor.newInstance(tg, "Reference Handler");
            /* If there were a special system-only priority greater than
             * MAX_PRIORITY, it would be used here
             */
            handler.setPriority(Thread.MAX_PRIORITY);
            handler.setDaemon(true);
            handler.start();
        } catch (Exception e) {
            throw ProgramError.unexpected(e);
        }
    }

    /**
     * Allocate and start a new thread to handle invocation of finalizers.
     */
    private static void startFinalizerThread() {
        if (FINALIZERS_SUPPORTED) {
            // it is sufficient just to reinitialize the finalizer class
            JDK.java_lang_ref_Finalizer.classActor().callInitializer();
        }
    }
}
