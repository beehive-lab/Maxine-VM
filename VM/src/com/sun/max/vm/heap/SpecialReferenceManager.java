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
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * This class implements support for collecting and processing special references
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

    private static final FieldActor nextField = getReferenceClassField("next");
    private static final FieldActor discoveredField = getReferenceClassField("discovered");
    private static final FieldActor referentField = getReferenceClassField("referent");
    private static final FieldActor pendingField = getReferenceClassField("pending");

    /**
     * The lock object associated with managing special references. This lock must
     * be held by the GC when it is updating the list of pending special references.
     * This value is a reference to the static {@code lock} field in {@link java.lang.ref.Reference}.
     * Like {@link com.sun.max.vm.thread.VmThreadMap#ACTIVE}, this lock is special and
     * requires a sticky monitor.
     */
    public static final Object LOCK = WithoutAccessCheck.getStaticField(JDK.java_lang_ref_Reference.javaClass(), "lock");
    static {
        JavaMonitorManager.bindStickyMonitor(LOCK, new StandardJavaMonitor());
    }

    // These methods and their invocation stubs must be compiled in the image
    private static final CriticalMethod registerMethod = new CriticalMethod(JDK.java_lang_ref_Finalizer.javaClass(), "register", SignatureDescriptor.create(Void.TYPE, Object.class));
    private static final CriticalMethod referenceHandlerConstructor = new CriticalMethod(JDK.java_lang_ref_Reference$ReferenceHandler.javaClass(), "<init>", SignatureDescriptor.create(Void.TYPE, ThreadGroup.class, String.class));
    static {
        MaxineVM.registerImageInvocationStub(registerMethod.classMethodActor);
        MaxineVM.registerImageInvocationStub(referenceHandlerConstructor.classMethodActor);
    }

    private static Grip discoveredList;

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
        // the first pass over the list finds the references that have referents that are no longer reachable
        java.lang.ref.Reference ref = UnsafeCast.asJDKReference(discoveredList.toJava());
        java.lang.ref.Reference last = UnsafeCast.asJDKReference(TupleAccess.readObject(pendingField.holder().staticTuple(), pendingField.offset()));
        while (ref != null) {
            final Grip referent = Grip.fromJava(ref).readGrip(referentField.offset());
            if (gripForwarder.isReachable(referent)) {
                // this object is reachable, however the "referent" field was not scanned.
                // we need to update this field manually
                TupleAccess.writeObject(ref, referentField.offset(), gripForwarder.getForwardGrip(referent));
            } else {
                TupleAccess.writeObject(ref, referentField.offset(), null);
                TupleAccess.writeObject(ref, nextField.offset(), last);
                last = ref;
            }
            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Processed ");
                Log.print(ObjectAccess.readClassActor(ref).name.string);
                Log.print(" at ");
                Log.print(ObjectAccess.toOrigin(ref));
                Log.print(" whose referent ");
                Log.print(referent.toOrigin());
                final Object newReferent = TupleAccess.readObject(ref, referentField.offset());
                if (newReferent == null) {
                    Log.println(" was unreachable");
                } else {
                    Log.print(" moved to ");
                    Log.println(ObjectAccess.toOrigin(newReferent));
                }
                Log.unlock(lockDisabledSafepoints);
            }
            ref = UnsafeCast.asJDKReference(TupleAccess.readObject(ref, discoveredField.offset()));
        }
        TupleAccess.writeObject(pendingField.holder().staticTuple(), pendingField.offset(), last);
        discoveredList = Grip.fromOrigin(Pointer.zero());
        if (last != null) {
            // if there are pending special references, notify the reference handler thread.
            // (note that the GC must already hold the lock object)
            LOCK.notifyAll();
        }

        // Special reference map of Inspector
        if (MaxineMessenger.isVmInspected()) {
            processInspectableWeakReferencesMemory(gripForwarder);
        }
    }

    private static void processInspectableWeakReferencesMemory(GripForwarder gripForwarder) {
        for (int i = 0; i < InspectableHeapInfo.MAX_NUMBER_OF_ROOTS; i++) {
            final Pointer rootPointer = InspectableHeapInfo.rootsPointer().getWord(i).asPointer();
            if (!rootPointer.isZero()) {
                final Grip referent = Grip.fromOrigin(rootPointer);
                if (gripForwarder.isReachable(referent)) {
                    InspectableHeapInfo.rootsPointer().setWord(i, gripForwarder.getForwardGrip(referent).toOrigin());
                } else {
                    InspectableHeapInfo.rootsPointer().setWord(i, Pointer.zero());
                }
                if (Heap.traceGC()) {
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.print("Processed root table entry ");
                    Log.print(i);
                    Log.print(": set ");
                    Log.print(rootPointer);
                    Log.print(" to ");
                    Log.println(InspectableHeapInfo.rootsPointer().getWord(i));
                    Log.unlock(lockDisabledSafepoints);
                }
            }
        }
    }

    /**
     * This method is called by the GC during heap exploration, when it finds a special
     * reference object. This method checks to see whether the object has been processed previously,
     * and if not, then adds it to the queue to be processed later.
     *
     * @param grip the grip that has been discovered
     */
    public static void discoverSpecialReference(Grip grip) {
        if (grip.readGrip(nextField.offset()).isZero()) {
            // the "next" field of this object is null, queue it for later processing
            grip.writeGrip(discoveredField.offset(), discoveredList);
            discoveredList = grip;
            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Added ");
                final Hub hub = UnsafeCast.asHub(Layout.readHubReference(grip).toJava());
                Log.print(hub.classActor.name.string);
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
                final ClassMethodActor methodActor = registerMethod.classMethodActor;
                methodActor.invoke(ReferenceValue.from(object));
            } catch (Exception e) {
                FatalError.unexpected("Could not register object for finalization", e);
            }
        }
    }

    /**
     * Initialize the SpecialReferenceManager when starting the VM. Normally, on the host
     * VM, the {@link java.lang.ref.Reference} and {@link java.lang.ref.Finalizer} classes create
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

    @PROTOTYPE_ONLY
    private static FieldActor getReferenceClassField(String name) {
        final ClassActor referenceClass = ClassActor.fromJava(java.lang.ref.Reference.class);
        FieldActor fieldActor = referenceClass.findLocalStaticFieldActor(name);
        if (fieldActor == null) {
            fieldActor = referenceClass.findLocalInstanceFieldActor(name);
        }
        return fieldActor;
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
        try {
            final Thread handler = (Thread) referenceHandlerConstructor.classMethodActor.invokeConstructor(ReferenceValue.from(tg), ReferenceValue.from("Reference Handler")).asObject();
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
