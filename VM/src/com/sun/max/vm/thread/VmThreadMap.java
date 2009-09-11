/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.thread;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.prototype.BootImage.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * The VmThreadMap class contains all the active threads in the MaxineVM.
 *
 * N.B. The (singleton) ACTIVE VmThreadMap object is bound with a
 * special JavaMonitor that prevents a terminated thread's state from
 * changing from TERMINATED during removeThreadLocals.
 * It is therefore imperative that all synchronization in this class use the
 * ACTIVE object.
 *
 * @author Ben L. Titzer
 * @author Bernd Mathiske
 */
public final class VmThreadMap {

    /**
     * Specialised JavaMonitor intended to be bound to the
     * VMThreadMap.ACTIVE object at image build time.
     *
     * MonitorEnter semantics are slightly modified to
     * halt a meta-circular regression arising from thread termination clean-up.
     * See VmThread.beTerminated().
     *
     * @author Simon Wilkinson
     */
    static class VMThreadMapJavaMonitor extends StandardJavaMonitor {

        @Override
        public void monitorEnter() {
            final VmThread currentThread = VmThread.current();
            if (currentThread.state() == Thread.State.TERMINATED) {
                if (ownerThread != currentThread) {
                    mutex.lock();
                    ownerThread = currentThread;
                    recursionCount = 1;
                } else {
                    recursionCount++;
                }
            } else {
                super.monitorEnter();
            }
        }
    }

    /**
     * The global thread map of active threads in the VM. This object also serves the role
     * of a global GC and thread creation lock.
     */
    public static final VmThreadMap ACTIVE = new VmThreadMap();
    static {
        JavaMonitorManager.bindStickyMonitor(ACTIVE, new VMThreadMapJavaMonitor());
    }

    private final IDMap idMap = new IDMap(64);
    private volatile int vmThreadStartCount;

    /**
     * The head of the VM thread locals list.
     *
     * The address of this field in {@link #ACTIVE} is exposed to native code via
     * {@link Header#threadLocalsListHeadOffset}.
     * This allows a debugger attached to the VM to discover all Java threads without using
     * platform specific mechanisms (such as thread_db on Solaris and Linux or Mach APIs on Darwin).
     */
    private Pointer threadLocalsListHead = Pointer.zero();

    private VmThreadMap() {
    }

    /**
     * Adds the specified thread locals to this thread map and initializes several of its
     * important values (such as its ID and VM thread reference).
     *
     * Note that this method does not perform synchronization on the thread map, because it must
     * only be executed in a newly created thread while the creating thread holds the lock on
     * this thread map.
     *
     * @param id the ID of the VM thread, which should match the ID of the VmThread
     * @param vmThreadLocals a pointer to the VM thread locals for the thread
     * @return a reference to the VmThread for this thread
     */
    public VmThread addVmThreadLocals(int id, Pointer vmThreadLocals) {
        final VmThread vmThread = idMap.get(id);
        VmThreadLocal.VM_THREAD.setConstantReference(vmThreadLocals, Reference.fromJava(vmThread));
        // insert this thread locals into the list
        setNext(vmThreadLocals, threadLocalsListHead);
        setPrev(threadLocalsListHead, vmThreadLocals);
        // at the head
        threadLocalsListHead = vmThreadLocals;
        // and signal that this thread has started up and joined the list
        vmThreadStartCount++;
        return vmThread;
    }

    /**
     * Remove the specified VM thread locals from this thread map.
     * @param vmThreadLocals the thread locals to remove from this map
     */
    public void removeVmThreadLocals(Pointer vmThreadLocals) {
        synchronized (this) {
            final int id = VmThreadLocal.ID.getConstantWord(vmThreadLocals).asAddress().toInt();
            if (threadLocalsListHead == vmThreadLocals) {
                // this vm thread locals is at the head of list
                threadLocalsListHead = getNext(threadLocalsListHead);
            } else {
                // this vm thread locals is somewhere in the middle
                final Pointer prev = getPrev(vmThreadLocals);
                final Pointer next = getNext(vmThreadLocals);
                setPrev(next, prev);
                setNext(prev, next);
            }
            // set this vm thread locals' links to zero
            setPrev(vmThreadLocals, Pointer.zero());
            setNext(vmThreadLocals, Pointer.zero());
            // release the ID for a later thread's use
            idMap.release(id);
        }
    }

    /**
     * Add the main thread to this thread map.
     *
     * @param vmThread the vmThread representing the main thread
     */
    public void addMainVmThread(VmThread vmThread) {
        vmThread.setID(idMap.acquire(vmThread));
    }

    // Helper routines to manipulate the linked list of vm thread locals

    @INLINE
    private Pointer getPrev(Pointer vmThreadLocals) {
        return VmThreadLocal.BACKWARD_LINK.getConstantWord(vmThreadLocals).asPointer();
    }

    @INLINE
    private Pointer getNext(Pointer vmThreadLocals) {
        return VmThreadLocal.FORWARD_LINK.getConstantWord(vmThreadLocals).asPointer();
    }

    @INLINE
    private void setPrev(Pointer vmThreadLocals, Pointer prev) {
        if (!vmThreadLocals.isZero()) {
            VmThreadLocal.BACKWARD_LINK.setConstantWord(vmThreadLocals, prev);
        }
    }

    @INLINE
    private void setNext(Pointer vmThreadLocals, Pointer next) {
        if (!vmThreadLocals.isZero()) {
            VmThreadLocal.FORWARD_LINK.setConstantWord(vmThreadLocals, next);
        }
    }

    /**
     * Creates the native thread for a VM thread and start it running. This method acquires an ID
     * for the new thread and returns it to the caller.
     *
     * @param vmThread the VM thread to create
     * @param stackSize the requested stack size
     * @param priority the initial priority of the thread
     * @return the native thread created
     */
    public Word startVmThread(VmThread vmThread, Size stackSize, int priority) {
        final int id = idMap.acquire(vmThread);
        synchronized (this) {
            final int count = vmThreadStartCount;
            final Word nativeThread = VmThread.nativeThreadCreate(id, stackSize, priority);
            if (nativeThread.isZero()) {
                /* This means that we did not create the native thread at all so there is nothing to
                 * terminate. Most likely we ran out of memory allocating the stack, so we throw
                 * an out of memory exception. There is a small possibility that the failure was in the
                 * actual OS thread creation but that would require a way to disambiguate.
                 */
                throw new OutOfMemoryError("Unable to create new native thread");
            }
            if (!waitForThreadStartup(count)) {
                vmThread.beTerminated();
                throw new InternalError("waitForThreadStartup() failed");
            }
            return nativeThread;
        }
    }

    /**
     * Waits for all non-daemon threads in the thread map to finish, except current.
     */
    public void joinAllNonDaemons() {
        while (true) {
            final VmThread vmThread = findNonDaemon();
            if (vmThread == null) {
                return;
            }
            vmThread.join();
        }
    }

    /**
     * Finds a non-daemon thread except current.
     * @return
     */
    private VmThread findNonDaemon() {
        Pointer vmThreadLocals = threadLocalsListHead;
        while (!vmThreadLocals.isZero()) {
            final VmThread vmThread = UnsafeLoophole.cast(VmThreadLocal.VM_THREAD.getConstantReference(vmThreadLocals).toJava());
            if (vmThread != VmThread.current() && !vmThread.javaThread().isDaemon()) {
                return vmThread;
            }
            vmThreadLocals = VmThreadLocal.FORWARD_LINK.getConstantWord(vmThreadLocals).asPointer();
        }
        return null;
    }

    private boolean waitForThreadStartup(int count) {
        int spin = 10000;
        while (vmThreadStartCount == count) {
            // spin for a little while, waiting for other thread to start
            if (spin-- == 0) {
                spin = 100;
                while (vmThreadStartCount == count) {
                    // wait for 100ms, 1ms at a time
                    if (spin-- == 0) {
                        return false;
                    }
                    VmThread.nonJniSleep(1);
                }
            }
        }
        return true;
    }

    /**
     * Iterates over all the VM threads in this thread map and run the specified procedure.
     *
     * @param predicate a predicate to apply on each thread
     * @param procedure the procedure to apply to each VM thread
     */
    public void forAllVmThreads(Predicate<VmThread> predicate, Procedure<VmThread> procedure) {
        Pointer vmThreadLocals = threadLocalsListHead;
        while (!vmThreadLocals.isZero()) {
            final VmThread vmThread = UnsafeLoophole.cast(VmThreadLocal.VM_THREAD.getConstantReference(vmThreadLocals).toJava());
            if (predicate == null || predicate.evaluate(vmThread)) {
                procedure.run(vmThread);
            }
            vmThreadLocals = getNext(vmThreadLocals);
        }
    }

    /**
     * Iterates over all the VM thread locals in this thread map and run the specified procedure.
     *
     * @param predicate a predicate to check on the VM thread locals
     * @param procedure the procedure to apply to each VM thread locals
     */
    public void forAllVmThreadLocals(Pointer.Predicate predicate, Pointer.Procedure procedure) {
        Pointer vmThreadLocals = threadLocalsListHead;
        while (!vmThreadLocals.isZero()) {
            if (predicate == null || predicate.evaluate(vmThreadLocals)) {
                procedure.run(vmThreadLocals);
            }
            vmThreadLocals = getNext(vmThreadLocals);
        }
    }

    public static final Pointer.Predicate isNotCurrent = new Pointer.Predicate() {
        public boolean evaluate(Pointer vmThreadLocals) {
            return vmThreadLocals != VmThread.current().vmThreadLocals();
        }
    };

    /**
     * Gets the {@code VmThread} object associated with the specified thread id.
     *
     * @param id the thread id
     * @return a reference to the {@code VmThread} object for the specified id
     */
    @INLINE
    public VmThread getVmThreadForID(int id) {
        return idMap.get(id);
    }

    /**
     * The {@code IDMap} class manages thread ids and a mapping between thread ids and
     * the corresponding {@code VmThread} instance.
     * The id 0 is reserved and never used to aid the modal monitor scheme ({@see ThinLockWord64}).
     *
     * Note that we synchronize explicitly on ACTIVE to ensure that we don't disturb the TERMINATED state
     * during thread tear down.
     */
    private final class IDMap {
        private int nextID = 1;
        private int[] freeList;
        private VmThread[] vmThreads;

        IDMap(int initialSize) {
            freeList = new int[initialSize];
            vmThreads = new VmThread[initialSize];
            for (int i = 0; i < freeList.length; i++) {
                freeList[i] = i + 1;
            }
        }

        int acquire(VmThread vmThread) {
            synchronized (ACTIVE) {
                FatalError.check(vmThread.id() == 0, "VmThread already has an ID");
                final int length = freeList.length;
                if (nextID >= length) {
                    // grow the free list and initialize the new part
                    final int[] newFreeList = Arrays.grow(freeList, length * 2);
                    for (int i = length; i < newFreeList.length; i++) {
                        newFreeList[i] = i + 1;
                    }
                    freeList = newFreeList;

                    // grow the vmThreads list and copy
                    final VmThread[] newVmThreads = new VmThread[length * 2];
                    for (int i = 0; i < length; i++) {
                        newVmThreads[i] = vmThreads[i];
                    }
                    vmThreads = newVmThreads;
                }
                final int id = nextID;
                nextID = freeList[nextID];
                vmThreads[id] = vmThread;
                vmThread.setID(id);
                return id;
            }
        }

        void release(int id) {
            synchronized (ACTIVE) {
                freeList[id] = nextID;
                vmThreads[id] = null;
                nextID = id;
            }
        }

        @INLINE
        VmThread get(int id) {
            // this operation may be performance critical, so avoid the bounds check
            return UnsafeLoophole.cast(ArrayAccess.getObject(vmThreads, id));
        }
    }
}
