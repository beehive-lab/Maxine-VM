/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap;


import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;
import static com.sun.max.vm.jdk.JDK_java_lang_ref_ReferenceQueue.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;

/**
 * This class implements support for collecting and processing special references
 * (i.e. instances of {@link Reference} and its subclasses) which
 * implement weak references and finalizers.
 * The routines in this class are called by the GC as it discovers reachable special
 * references, and after live objects have been processed.
 */
public class SpecialReferenceManager {

    public static boolean TraceReferenceGC;
    static {
        VMOptions.addFieldOption("-XX:", "TraceReferenceGC", "Trace Handling of soft/weak/final/phantom references.");
    }

    private static final boolean FINALIZERS_SUPPORTED = true;

    /**
     * This interface forms a contract between the GC algorithm and the implementation of special references.
     */
    public interface GC {
        /**
         * Determines if an object is currently in the set of objects that
         * will survive the current collection.
         *
         * @param ref a reference to an object
         */
        boolean isReachable(Reference ref);

        /**
         * Ensures that the object graph rooted at a given reference survives the current GC.
         *
         * @param ref the root of the object graph to be preserved
         * @return a reference to the root of the preserved object graph. Whether or not this is equal to {@code ref}
         *         depends on the specific GC algorithm (e.g. mark-sweep vs copying)
         */
        Reference preserve(Reference ref);

        /**
         * Indicates whether the GC relocates live objects. If true and a reference object is live, the special reference manager must
         * invoke its {@link #preserve(Reference)} method to update the referent field.
         * @return true if live objects may have relocated.
         */
        boolean mayRelocateLiveObjects();
    }

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

    /**
     * The head of the list of discovered references.
     * This field must only be used by the GC. Accessing it should not trigger any read/write barriers.
     */
    private static java.lang.ref.Reference discoveredList;

    /**
     * An alias type for accessing the fields in java.lang.ref.Reference without having to use reflection.
     * <p>
     * The comment below is from the JDK source for java.lang.ref.Reference and explains
     * the meaning of the {@link #next} and {@link #queue} as pertaining to the state of a reference.
     * <p>
     * A Reference instance is in one of four possible internal states:
     * <p>
     *     Active: Subject to special treatment by the garbage collector.  Some
     *     time after the collector detects that the reachability of the
     *     referent has changed to the appropriate state, it changes the
     *     instance's state to either Pending or Inactive, depending upon
     *     whether or not the instance was registered with a queue when it was
     *     created.  In the former case it also adds the instance to the
     *     pending-Reference list.  Newly-created instances are Active.
     * <p>
     *     Pending: An element of the pending-Reference list, waiting to be
     *     enqueued by the Reference-handler thread.  Unregistered instances
     *     are never in this state.
     * <p>
     *     Enqueued: An element of the queue with which the instance was
     *     registered when it was created.  When an instance is removed from
     *     its ReferenceQueue, it is made Inactive.  Unregistered instances are
     *     never in this state.
     * <p>
     *     Inactive: Nothing more to do.  Once an instance becomes Inactive its
     *     state will never change again.
     * <p>
     * The state is encoded in the queue and next fields as follows:
     * <p>
     *     Active: queue = ReferenceQueue with which instance is registered, or
     *     ReferenceQueue.NULL if it was not registered with a queue; next =
     *     null.
     * <p>
     *     Pending: queue = ReferenceQueue with which instance is registered;
     *     next = Following instance in queue, or this if at end of list.
     * <p>
     *     Enqueued: queue = ReferenceQueue.ENQUEUED; next = Following instance
     *     in queue, or this if at end of list.
     * <p>
     *     Inactive: queue = ReferenceQueue.NULL; next = this.
     * <p>
     * With this scheme the collector need only examine the next field in order
     * to determine whether a Reference instance requires special treatment: If
     * the next field is null then the instance is active; if it is non-null,
     * then the collector should treat the instance normally.
     * <p>
     * To ensure that concurrent collector can discover active Reference
     * objects without interfering with application threads that may apply
     * the enqueue() method to those objects, collectors should link
     * discovered objects through the discovered field.
     */
    public static class JLRRAlias {
        @ALIAS(declaringClass = java.lang.ref.Reference.class)
        static java.lang.ref.Reference pending;

        @ALIAS(declaringClass = java.lang.ref.Reference.class)
        public java.lang.ref.Reference next;

        /**
         * Next ref in a linked list used by the GC to communicate discovered references
         * from the {@linkplain SpecialReferenceManager#discoverSpecialReference(Pointer) discovery} phase
         * to the {@linkplain SpecialReferenceManager#processDiscoveredSpecialReferences(GC) processing} phase.
         */
        @ALIAS(declaringClass = java.lang.ref.Reference.class)
        public java.lang.ref.Reference discovered;

        @ALIAS(declaringClass = java.lang.ref.Reference.class)
        Object referent;

        @ALIAS(declaringClass = java.lang.ref.Reference.class)
        public java.lang.ref.ReferenceQueue queue;

        final boolean isActive() {
            return next == null;
        }

        final boolean isPending() {
            return next != null && queue != ENQUEUED && queue != NULL;
        }

        final boolean isEnqueued() {
            return next != null && queue == ENQUEUED;
        }

        final boolean isInactive() {
            return next != null && queue == NULL;
        }
    }

    @INTRINSIC(UNSAFE_CAST)
    public static native JLRRAlias asJLRRAlias(Object o);

    @INTRINSIC(UNSAFE_CAST)
    public static native java.lang.ref.Reference asJLRR(Object o);

    /**
     * This method is called by the GC during heap exploration, when it finds a special
     * reference object. This method checks to see whether the object has been processed previously,
     * and if not, then adds it to the queue to be processed later.
     *
     * @param cell a pointer at the origin of the reference that has been discovered
     */
    public static void discoverSpecialReference(Pointer cell) {
        final Pointer origin = Layout.cellToOrigin(cell);
        java.lang.ref.Reference ref = asJLRR(Reference.fromOrigin(origin));
        JLRRAlias refAlias = asJLRRAlias(ref);

        if (refAlias.discovered == null) {
            // the discovered field of this object is null, queue it for later processing
            if (ref == discoveredList) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Reference ");
                Log.print(ObjectAccess.readClassActor(ref).name.string);
                Log.print(" at ");
                Log.print(cell);
                Log.println(" is already on discovered list");
                Log.unlock(lockDisabledSafepoints);
                FatalError.unexpected("Duplicate on discovered list");
            }
            final Reference referent = Reference.fromJava(refAlias.referent);
            refAlias.discovered = discoveredList;
            discoveredList = ref;
            if (TraceReferenceGC || Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Added ");
                Log.print(cell);
                Log.print(' ');
                final Hub hub = UnsafeCast.asHub(Layout.readHubReference(origin).toJava());
                Log.print(hub.classActor.name.string);
                Log.print(" {referent=");
                Log.print(referent.toOrigin());
                Log.println("} to list of discovered references");
                Log.unlock(lockDisabledSafepoints);
            }
        }
    }

    /**
     * Processes the special reference objects that were {@linkplain #discoverSpecialReference(Pointer) discovered}
     * during heap scanning.
     * These live reference objects are checked to see whether
     * the reachability of their "referent" objects has changed according to the type of reference.
     * If so, they are enqueued as "pending" so that the {@link Reference.ReferenceHandler} thread can pick them up
     * and add them to their respective queues later.
     * The reference handler lock is notified by the thread that {@linkplain VmOperationThread#submit(VmOperation) submitted}
     * the GC operation as it holds the lock. See {@link GCOperation#doItEpilogue(boolean)}.
     *
     * @param gc interface to the GC implementation
     */
    public static void processDiscoveredSpecialReferences(GC gc) {
        java.lang.ref.Reference head = discoveredList;
        java.lang.ref.Reference end = sentinel;
        final boolean updateReachableReferent = gc.mayRelocateLiveObjects();

        if (TraceReferenceGC || Heap.traceGC()) {
            Log.print("ReferenceQueue.NULL = ");
            Log.println(Reference.fromJava(JDK_java_lang_ref_ReferenceQueue.NULL).toOrigin());
            Log.print("ReferenceQueue.ENQUEUED = ");
            Log.println(Reference.fromJava(JDK_java_lang_ref_ReferenceQueue.ENQUEUED).toOrigin());
        }

        // Process the discovered list until it is empty (new elements may be
        // prepended while processing).
        do {
            java.lang.ref.Reference ref = head;
            java.lang.ref.Reference pending = JLRRAlias.pending;

            while (ref != end) {
                boolean preserved = false;
                boolean addedToPending = false;
                JLRRAlias refAlias = asJLRRAlias(ref);
                final Reference referent = Reference.fromJava(refAlias.referent);
                if (referent.isZero()) {
                    // Do not add 'ref' to the pending list as weak references
                    // with already null referents are not added to ReferenceQueues
                } else if (!gc.isReachable(referent)) {
                    if (refAlias.queue == null) {
                        // This can only occur if there is a GC in the constructor for java.lang.ref.Reference
                        // between the initialization of 'referent' and 'queue'.
                        Log.println("WARNING: cannot add weak reference with null 'queue' field to pending list");
                    } else {
                        // Only soft and weak references have their referent cleared
                        if (ref instanceof java.lang.ref.SoftReference || ref instanceof java.lang.ref.WeakReference) {
                            refAlias.referent = null;
                        } else {
                            refAlias.referent = gc.preserve(referent).toJava();
                            preserved = true;
                        }

                        // Add active reference whose reachability has changed to pending list
                        if (refAlias.isActive()) {
                            if (pending == null) {
                                // 'ref' will be at the end of the pending list
                                refAlias.next = ref;
                            } else {
                                refAlias.next = pending;
                            }
                            pending = ref;
                            addedToPending = true;
                        }
                    }
                } else if (updateReachableReferent) {
                    // this object is reachable, however the "referent" field was not scanned.
                    // we need to update this field manually
                    refAlias.referent = gc.preserve(referent).toJava();
                }

                JLRRAlias r = refAlias;
                ref = refAlias.discovered;
                r.discovered = null;

                if (TraceReferenceGC || Heap.traceGC()) {
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
                    final Object newReferent = r.referent;
                    if (newReferent == null) {
                        Log.print(" was unreachable");
                        Log.print(" [queue: ");
                        Log.print(Reference.fromJava(r.queue).toOrigin());
                        Log.print("]");
                    } else if (preserved) {
                        Log.print(" was unreachable but preserved to ");
                        Log.print(ObjectAccess.toOrigin(newReferent));
                        Log.print(" [queue: ");
                        Log.print(Reference.fromJava(r.queue).toOrigin());
                        Log.print("]");
                    } else if (updateReachableReferent) {
                        Log.print(" moved to ");
                        Log.print(ObjectAccess.toOrigin(newReferent));
                    }
                    if (!addedToPending) {
                        Log.print(" {not added to Reference.pending list}");
                    }
                    Log.println();
                    Log.unlock(lockDisabledSafepoints);
                }
            }
            JLRRAlias.pending = pending;

            if (head == discoveredList) {
                // No further special references were discovered
                discoveredList = sentinel;
                break;
            }

            end = head;
            head = discoveredList;
        } while (true);

        // Special reference map of Inspector
        if (Inspectable.isVmInspected()) {
            processInspectableWeakReferencesMemory(gc);
        }
    }

    private static void processInspectableWeakReferencesMemory(GC gc) {
        final RootTableMemoryRegion rootsMemoryRegion = InspectableHeapInfo.rootsMemoryRegion();
        final boolean updateReachableReferent = gc.mayRelocateLiveObjects();
        assert rootsMemoryRegion != null;
        final Pointer rootsPointer = rootsMemoryRegion.start().asPointer();
        long wordsUsedCounter = 0;
        assert !rootsPointer.isZero();
        for (int i = 0; i < InspectableHeapInfo.MAX_NUMBER_OF_ROOTS; i++) {
            final Pointer rootPointer = rootsPointer.getWord(i).asPointer();
            if (!rootPointer.isZero()) {
                final Reference referent = Reference.fromOrigin(rootPointer);
                if (gc.isReachable(referent)) {
                    if (updateReachableReferent) {
                        rootsPointer.setWord(i, gc.preserve(referent).toOrigin());
                    }
                    wordsUsedCounter++;
                } else {
                    rootsPointer.setWord(i, Pointer.zero());
                }
                if (TraceReferenceGC || Heap.traceGC()) {
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

    @ALIAS(declaringClassName = "java.lang.ref.Finalizer")
    private static native void register(Object finalizee);

    /**
     * Registers an object that has a finalizer with the special reference manager.
     * A call to this method is inserted after allocation of such objects.
     * @param object the object that has a finalizers
     */
    public static void registerFinalizee(Object object) {
        if (FINALIZERS_SUPPORTED) {
            FatalError.check(ObjectAccess.readClassActor(object).hasFinalizer(), "cannot register object that has no finalizer");
            register(object);
            if (TraceReferenceGC || Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Registered finalizer for ");
                Log.print(Reference.fromJava(object).toOrigin());
                Log.print(" of type ");
                Log.println(object.getClass().getName());
                Log.unlock(lockDisabledSafepoints);
            }
        }
    }

    static final class SentinelReference extends java.lang.ref.WeakReference<Object> {
        public SentinelReference() {
            super(null);
        }
    }

    private static final SentinelReference sentinel = new SentinelReference();

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
            clock = System.currentTimeMillis();
            discoveredList = sentinel;
            JLRRAlias sentinelAlias = asJLRRAlias(sentinel);
            sentinelAlias.discovered = sentinel;
            sentinelAlias.next = sentinel;
            sentinelAlias.referent = null;
            assert sentinelAlias.isInactive();
            startReferenceHandlerThread();
            startFinalizerThread();
        }
    }

    @ALIAS(declaringClass = java.lang.ref.SoftReference.class)
    private static long clock;

    @HOSTED_ONLY
    private static FieldActor getReferenceClassField(String name, Class c) {
        final ClassActor referenceClass = ClassActor.fromJava(c);
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
        // The thread was built into the boot image. We simply need to start it:
        VmThread.referenceHandlerThread.startVmSystemThread();
    }

    /**
     * Allocate and start a new thread to handle invocation of finalizers.
     */
    private static void startFinalizerThread() {
        if (FINALIZERS_SUPPORTED) {
            // The thread was built into the boot image. We simply need to start it:
            VmThread.finalizerThread.startVmSystemThread();
        }
    }
}
