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


import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
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
     * An alias type for accessing the fields in java.lang.ref.Reference without
     * having to use reflection.
     */
    public static class JDKRefAlias {
        @ALIAS(declaringClass = java.lang.ref.Reference.class)
        static java.lang.ref.Reference pending;

        @ALIAS(declaringClass = java.lang.ref.Reference.class)
        java.lang.ref.Reference next;

        /**
         * Next ref in a linked list used by the GC to communicate discovered references
         * from the {@linkplain SpecialReferenceManager#discoverSpecialReference(Pointer) discovery} phase
         * to the {@linkplain SpecialReferenceManager#processDiscoveredSpecialReferences(ReferenceForwarder) processing} phase.
         */
        @ALIAS(declaringClass = java.lang.ref.Reference.class)
        public java.lang.ref.Reference discovered;

        @ALIAS(declaringClass = java.lang.ref.Reference.class)
        Object referent;

        @ALIAS(declaringClass = java.lang.ref.Reference.class)
        public java.lang.ref.ReferenceQueue queue;
    }

    @INTRINSIC(UNSAFE_CAST)
    public static native JDKRefAlias asJDKRefAlias(Object o);

    @INTRINSIC(UNSAFE_CAST)
    static native java.lang.ref.Reference asJDKRef(Object o);

    /**
     * Processes the special reference objects that were
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

        java.lang.ref.Reference ref = discoveredList;
        java.lang.ref.Reference last = JDKRefAlias.pending;
        final boolean isForwardingGC = refForwarder.isForwarding();

        while (ref != sentinel) {
            JDKRefAlias refAlias = asJDKRefAlias(ref);
            final Reference referent = Reference.fromJava(refAlias.referent);
            if (referent.isZero()) {
                // Do not add 'ref' to the pending list as weak references
                // with already null referents are not added to ReferenceQueues
            } else if (!refForwarder.isReachable(referent)) {
                if (refAlias.queue == null) {
                    // This can only occur if there is a GC in the constructor for java.lang.ref.Reference
                    // between the initialization of 'referent' and 'queue'.
                    Log.println("WARNING: cannot add weak reference with null 'queue' field to pending list");
                } else {
                    refAlias.referent = null;
                    refAlias.next = last;
                    last = ref;
                }
            } else if (isForwardingGC) {
                // this object is reachable, however the "referent" field was not scanned.
                // we need to update this field manually
                refAlias.referent = refForwarder.getForwardRefence(referent).toJava();
            }

            JDKRefAlias r = refAlias;
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
                    Log.println(" was unreachable");
                } else {
                    Log.print(" moved to ");
                    Log.println(ObjectAccess.toOrigin(newReferent));
                }
                Log.unlock(lockDisabledSafepoints);
            }
        }
        JDKRefAlias.pending = last;
        FatalError.check(ref == sentinel, "end of discovered list should be sentinel");
        discoveredList = sentinel;

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

    /**
     * This method is called by the GC during heap exploration, when it finds a special
     * reference object. This method checks to see whether the object has been processed previously,
     * and if not, then adds it to the queue to be processed later.
     *
     * @param cell a pointer at the origin of the reference that has been discovered
     */
    public static void discoverSpecialReference(Pointer cell) {
        final Pointer origin = Layout.cellToOrigin(cell);
        java.lang.ref.Reference ref = asJDKRef(Reference.fromOrigin(origin));
        JDKRefAlias refAlias = asJDKRefAlias(ref);

        if (refAlias.discovered == null) {
            // the discovered field of this object is null, queue it for later processing
            if (MaxineVM.isDebug()) {
                if (ref == discoveredList) {
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.print("Discovered reference ");
                    Log.print(cell);
                    Log.print(" ");
                    Log.unlock(lockDisabledSafepoints);
                    FatalError.unexpected(": already discovered");
                }
            }
            refAlias.discovered = discoveredList;
            discoveredList = ref;
            if (TraceReferenceGC || Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Added ");
                Log.print(cell);
                Log.print(" ");
                final Hub hub = UnsafeCast.asHub(Layout.readHubReference(origin).toJava());
                Log.print(hub.classActor.name.string);
                Log.println(" to list of discovered references");
                Log.unlock(lockDisabledSafepoints);
            }
        }
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
            register(object);
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
            JDKRefAlias sentinelAlias = asJDKRefAlias(sentinel);
            sentinelAlias.discovered = null;
            sentinelAlias.next = null;
            sentinelAlias.queue = null;
            sentinelAlias.referent = null;
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
        VmThread.referenceHandlerThread.start0();
    }

    /**
     * Allocate and start a new thread to handle invocation of finalizers.
     */
    private static void startFinalizerThread() {
        if (FINALIZERS_SUPPORTED) {
            // The thread was built into the boot image. We simply need to start it:
            VmThread.finalizerThread.start0();
        }
    }
}
