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


import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;
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

    private static boolean TraceReferenceGC;
    static {
        VMOptions.addFieldOption("-XX:", "TraceReferenceGC", "Trace Handling of soft/weak/final/phantom references.");
    }

    private static final boolean FINALIZERS_SUPPORTED = true;

    /**
     * This interface forms a contract between the GC algorithm and the implementation of special
     * references. The special reference implementation must be able to query the GC about
     * a particular reference as well as get its updated value, if it has one.
     *
     * @author Ben L. Titzer
     */
    public interface ReferenceForwarder {
        boolean isReachable(Reference ref);
        boolean isForwarding();
        Reference getForwardRefence(Reference ref);
    }

    private static final FieldActor nextField = getReferenceClassField("next");
    private static final FieldActor discoveredField = getReferenceClassField("discovered");
    private static final FieldActor referentField = getReferenceClassField("referent");
    private static final FieldActor pendingField = getReferenceClassField("pending");

    /**
     * The lock object associated with managing special references. This lock must
     * be held by the GC when it is updating the list of pending special references.
     * This value is a reference to the static {@code lock} field in {@link java.lang.ref.Reference}.
     * Like {@link com.sun.max.vm.thread.VmThreadMap#THREAD_LOCK}, this lock is special and
     * requires a sticky monitor.
     */
    public static final Object REFERENCE_LOCK = WithoutAccessCheck.getStaticField(JDK.java_lang_ref_Reference.javaClass(), "lock");
    static {
        JavaMonitorManager.bindStickyMonitor(REFERENCE_LOCK);
    }

    private static Reference discoveredList;

    /**
     * This method processes the special reference objects that were
     * {@linkplain #discoverSpecialReference(Reference) discovered} during the
     * GC's exploration of the heap. These live reference objects must be checked to see whether
     * their "referent" objects have been collected. If so, they must be enqueued as "pending"
     * so that the {@link Reference.ReferenceHandler} thread can pick them up
     * and add them to their respective queues later.
     * The reference handler lock is notified by the thread that {@linkplain VmOperationThread#submit(VmOperation) submitted}
     * the GC operation as it holds the lock. See {@link GCOperation#doItEpilogue(boolean)}.
     *
     * @param refForwarder an object from the GC algorithm that can detect whether a ref
     * is live and can also return a forwarded version of the ref
     */
    public static void processDiscoveredSpecialReferences(ReferenceForwarder refForwarder) {
        // the first pass over the list finds the references that have referents that are no longer reachable
        java.lang.ref.Reference ref = UnsafeCast.asJDKReference(discoveredList.toJava());
        java.lang.ref.Reference last = UnsafeCast.asJDKReference(pendingField.getObject(null));
        final boolean isForwardingGC = refForwarder.isForwarding();

        while (ref != null) {
            final Reference referent = Reference.fromJava(ref).readReference(referentField.offset());
            if (referent.isZero()) {
                TupleAccess.writeObject(ref, nextField.offset(), last);
                last = ref;
            } else if (!refForwarder.isReachable(referent)) {
                TupleAccess.writeObject(ref, referentField.offset(), null);
                TupleAccess.writeObject(ref, nextField.offset(), last);
                last = ref;
            } else if (isForwardingGC) {
                // this object is reachable, however the "referent" field was not scanned.
                // we need to update this field manually
                TupleAccess.writeObject(ref, referentField.offset(), refForwarder.getForwardRefence(referent));
            }

            java.lang.ref.Reference r = ref;
            ref = UnsafeCast.asJDKReference(TupleAccess.readObject(ref, discoveredField.offset()));
            TupleAccess.writeObject(r, discoveredField.offset(), null);

            if (TraceReferenceGC) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Processed ");
                Log.print(ObjectAccess.readClassActor(r).name.string);
                Log.print(" at ");
                Log.print(ObjectAccess.toOrigin(r));
                if (MaxineVM.isDebug()) {
                    Log.print(" [next discovered = ");
                    Log.print(ObjectAccess.toOrigin(ref));
                    Log.print("]");
                }
                Log.print(" whose referent ");
                Log.print(referent.toOrigin());
                final Object newReferent = TupleAccess.readObject(r, referentField.offset());
                if (newReferent == null) {
                    Log.println(" was unreachable");
                } else {
                    Log.print(" moved to ");
                    Log.println(ObjectAccess.toOrigin(newReferent));
                }
                Log.unlock(lockDisabledSafepoints);
            }
        }
        TupleAccess.writeObject(pendingField.holder().staticTuple(), pendingField.offset(), last);
        discoveredList = Reference.fromOrigin(Pointer.zero());

        // Special reference map of Inspector
        if (Inspectable.isVmInspected()) {
            processInspectableWeakReferencesMemory(refForwarder);
        }
    }

    private static void processInspectableWeakReferencesMemory(ReferenceForwarder refForwarder) {
        final RootTableMemoryRegion rootsMemoryRegion = InspectableHeapInfo.rootsMemoryRegion();
        assert rootsMemoryRegion != null;
        final Pointer rootsPointer = rootsMemoryRegion.start().asPointer();
        long wordsUsedCounter = 0;
        assert !rootsPointer.isZero();
        for (int i = 0; i < InspectableHeapInfo.MAX_NUMBER_OF_ROOTS; i++) {
            final Pointer rootPointer = rootsPointer.getWord(i).asPointer();
            if (!rootPointer.isZero()) {
                final Reference referent = Reference.fromOrigin(rootPointer);
                if (refForwarder.isReachable(referent)) {
                    rootsPointer.setWord(i, refForwarder.getForwardRefence(referent).toOrigin());
                    wordsUsedCounter++;
                } else {
                    rootsPointer.setWord(i, Pointer.zero());
                }
                if (TraceReferenceGC) {
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.print("Processed root table entry ");
                    Log.print(i);
                    Log.print(": set ");
                    Log.print(rootPointer);
                    Log.print(" to ");
                    Log.println(rootsPointer.getWord(i));
                    Log.unlock(lockDisabledSafepoints);
                }
            }
        }
        rootsMemoryRegion.setWordsUsed(wordsUsedCounter);
    }

    /**
     * This method is called by the GC during heap exploration, when it finds a special
     * reference object. This method checks to see whether the object has been processed previously,
     * and if not, then adds it to the queue to be processed later.
     *
     * @param ref the reference that has been discovered
     */
    public static void discoverSpecialReference(Reference ref) {
        if (ref.readReference(nextField.offset()).isZero()) {
            // the "next" field of this object is null, queue it for later processing
            if (MaxineVM.isDebug()) {
                boolean hasNullDiscoveredField = ref.readReference(discoveredField.offset()).isZero();
                boolean isHeadOfDiscoveredList = ref.equals(discoveredList);
                if (!(hasNullDiscoveredField && !isHeadOfDiscoveredList)) {
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.print("Discovered reference ");
                    Log.print(ref.toOrigin());
                    Log.print(" ");
                    Log.unlock(lockDisabledSafepoints);
                    FatalError.unexpected(": already discovered");
                }
            }
            ref.writeReference(discoveredField.offset(), discoveredList);
            discoveredList = ref;
            if (TraceReferenceGC) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Added ");
                Log.print(ref.toOrigin());
                Log.print(" ");
                final Hub hub = UnsafeCast.asHub(Layout.readHubReference(ref).toJava());
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
                ClassRegistry.Finalizer_register_Object.invoke(ReferenceValue.from(object));
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
        if (phase == Phase.PRISTINE) {
            startReferenceHandlerThread();
            startFinalizerThread();
        }
    }

    @HOSTED_ONLY
    private static FieldActor getReferenceClassField(String name) {
        final ClassActor referenceClass = ClassActor.fromJava(java.lang.ref.Reference.class);
        FieldActor fieldActor = referenceClass.findLocalStaticFieldActor(name);
        if (fieldActor == null) {
            fieldActor = referenceClass.findLocalInstanceFieldActor(name);
        }
        return fieldActor;
    }

    /**
     * Start the thread to handle enqueuing weak references.
     */
    private static void startReferenceHandlerThread() {
        if (VmThread.referenceHandlerThread != null) {
            // The thread was built into the boot image. We simply need to start it:
            VmThread.referenceHandlerThread.start0();
        } else {
            // Note: this code is partially copied from java.lang.Reference <clinit>
            // We cannot simply rerun static initialization because it would allocate a new lock object.
            ThreadGroup tg = VmThread.systemThreadGroup;
            try {
                final Thread handler = (Thread) ClassRegistry.ReferenceHandler_init.invokeConstructor(ReferenceValue.from(tg), ReferenceValue.from("Reference Handler")).asObject();
                /* If there were a special system-only priority greater than
                 * MAX_PRIORITY, it would be used here
                 */
                handler.setPriority(Thread.MAX_PRIORITY);
                handler.setDaemon(true);
                handler.start();
            } catch (Exception e) {
                throw FatalError.unexpected("Could not start ReferenceHandler thread", e);
            }
        }
    }

    /**
     * Allocate and start a new thread to handle invocation of finalizers.
     */
    private static void startFinalizerThread() {
        if (FINALIZERS_SUPPORTED) {
            if (VmThread.finalizerThread != null) {
                // The thread was built into the boot image. We simply need to start it:
                VmThread.finalizerThread.start0();
            } else {
                // it is sufficient just to reinitialize the finalizer class
                JDK.callInitializer(JDK.java_lang_ref_Finalizer.classActor());
            }
        }
    }
}
