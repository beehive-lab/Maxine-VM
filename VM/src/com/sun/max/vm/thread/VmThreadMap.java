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
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.monitor.modal.sync.nat.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.prototype.BootImage.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * The VmThreadMap class contains all the active threads in the MaxineVM.
 *
 * N.B. The {@link #ACTIVE} object is bound with a
 * special JavaMonitor that prevents a terminated thread's state from
 * changing from TERMINATED during removeThreadLocals.
 * It is therefore imperative that all synchronization in this class use the
 * ACTIVE object.
 *
 * @author Ben L. Titzer
 * @author Bernd Mathiske
 * @author Paul Caprioli
 * @author Doug Simon
 */
public final class VmThreadMap {

    /**
     * Specialized JavaMonitor intended to be bound to {@link VmThreadMap#ACTIVE} at image build time.
     *
     * MonitorEnter semantics are slightly modified to
     * halt a meta-circular regression arising from thread termination clean-up.
     * See VmThread.beTerminated().
     *
     * In addition, this object serves as the global GC lock and it is
     * {@linkplain VmThreadMap#nativeSetGlobalThreadAndGCLock(Pointer) exposed}
     * to the native code so that it can be locked when attaching a thread to the VM.
     *
     * @author Simon Wilkinson
     * @author Doug Simon
     */
    static final class VMThreadMapJavaMonitor extends StandardJavaMonitor {
        @Override
        public void allocate() {
            super.allocate();
            NativeMutex nativeMutex = (NativeMutex) mutex;
            nativeSetGlobalThreadAndGCLock(nativeMutex.asPointer());
        }

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

    /**
     * The {@code IDMap} class manages thread IDs and a mapping between thread IDs and
     * the corresponding {@code VmThread} instance.
     * The id 0 is reserved and never used to aid the modal monitor scheme ({@see ThinLockword64}).
     *
     * Note that callers of acquire or release must synchronize explicitly on ACTIVE to ensure that
     * the TERMINATED state is not disturbed during thread tear down.
     */
    private static final class IDMap {
        private int nextID = 1;
        private int[] freeList;
        private VmThread[] threads;

        IDMap(int initialSize) {
            freeList = new int[initialSize];
            threads = new VmThread[initialSize];
            for (int i = 0; i < freeList.length; i++) {
                freeList[i] = i + 1;
            }
        }

        /**
         * Acquires an ID for a VmThread.
         *
         * <b>NOTE: This method is not synchronized. It is required that the caller synchronizes on ACTIVE.</b>
         *
         * @param thread the VmThread for which an ID should be assigned
         * @return the ID assigned to {@code thread}
         */
        int acquire(VmThread thread) {
            FatalError.check(thread.id() == 0, "VmThread already has an ID");
            final int length = freeList.length;
            if (nextID >= length) {
                // grow the free list and initialize the new part
                final int[] newFreeList = Arrays.grow(freeList, length * 2);
                for (int i = length; i < newFreeList.length; i++) {
                    newFreeList[i] = i + 1;
                }
                freeList = newFreeList;

                // grow the threads list and copy
                final VmThread[] newVmThreads = new VmThread[length * 2];
                for (int i = 0; i < length; i++) {
                    newVmThreads[i] = threads[i];
                }
                threads = newVmThreads;
            }
            final int id = nextID;
            nextID = freeList[nextID];
            threads[id] = thread;
            thread.setID(id);
            return id;
        }

        /**
         * <b>NOTE: This method is not synchronized. It is required that the caller synchronizes on ACTIVE.</b>
         *
         * @param id
         */
        void release(int id) {
            freeList[id] = nextID;
            threads[id] = null;
            nextID = id;
        }

        @INLINE
        VmThread get(int id) {
            // this operation may be performance critical, so avoid the bounds check
            return UnsafeCast.asVmThread(ArrayAccess.getObject(threads, id));
        }
    }

    /**
     * Informs the native code of the mutex used to synchronize on {@link #ACTIVE}
     * which serves as a global thread creation and GC lock.
     *
     * @param mutex the address of a platform specific mutex
     */
    @C_FUNCTION
    private static native void nativeSetGlobalThreadAndGCLock(Pointer mutex);

    private final IDMap idMap = new IDMap(64);
    private volatile int threadStartCount;

    /**
     * The number of currently running non-daemon threads running, excluding
     * the {@linkplain VmThread#MAIN_VM_THREAD main} thread.
     */
    private volatile int nonDaemonThreads;

    /**
     * The head of the VM thread locals list.
     *
     * The address of this field in {@link #ACTIVE} is exposed to native code via
     * {@link Header#threadLocalsListHeadOffset}.
     * This allows a debugger attached to the VM to discover all Java threads without using
     * platform specific mechanisms (such as thread_db on Solaris and Linux or Mach APIs on Darwin).
     */
    private Pointer threadLocalsListHead = Pointer.zero();

    @INLINE
    private static Pointer getPrev(Pointer threadLocals) {
        return VmThreadLocal.BACKWARD_LINK.getConstantWord(threadLocals).asPointer();
    }

    @INLINE
    private static Pointer getNext(Pointer threadLocals) {
        return VmThreadLocal.FORWARD_LINK.getConstantWord(threadLocals).asPointer();
    }

    @INLINE
    private static void setPrev(Pointer threadLocals, Pointer prev) {
        if (!threadLocals.isZero()) {
            VmThreadLocal.BACKWARD_LINK.setConstantWord(threadLocals, prev);
        }
    }

    @INLINE
    private static void setNext(Pointer threadLocals, Pointer next) {
        if (!threadLocals.isZero()) {
            VmThreadLocal.FORWARD_LINK.setConstantWord(threadLocals, next);
        }
    }

    /**
     * Add the main thread (or an attached thread) to the thread map ACTIVE.
     *
     * @param thread the VmThread representing the main or attached thread
     * @param daemon specifies if {@code thread} is a daemon
     */
    public static void addAttachedThread(VmThread thread, boolean daemon) {
        ACTIVE.idMap.acquire(thread);
        if (!MaxineVM.isHosted() && !daemon && thread != VmThread.MAIN_VM_THREAD) {
            ACTIVE.nonDaemonThreads++;
        }

    }

    /**
     * Adds the specified thread locals to the ACTIVE thread map and initializes several of its
     * important values (such as its ID and VM thread reference).
     *
     * Note that this method does not perform synchronization on the thread map, because it must
     * only be executed in a newly created thread while the creating thread holds the lock on
     * the ACTIVE thread map.
     *
     * @param id the ID of the VM thread, which should match the ID of the VmThread
     * @param threadLocals a pointer to the VM thread locals for the thread
     * @return a reference to the VmThread for this thread
     */
    public static VmThread addThreadLocals(int id, Pointer threadLocals) {
        final VmThread thread = ACTIVE.idMap.get(id);
        addThreadLocals(thread, threadLocals);
        return thread;
    }

    /**
     * Adds the specified thread locals to the ACTIVE thread map and initializes several of its
     * important values (such as its ID and VM thread reference).
     *
     * Note that this method does not perform synchronization on the thread map, because it must
     * only be executed in a newly created thread while the creating thread holds the lock on
     * the ACTIVE thread map.
     *
     * @param thread the VmThread to add
     * @param threadLocals a pointer to the VM thread locals for the thread
     */
    public static void addThreadLocals(VmThread thread, Pointer threadLocals) {
        VmThreadLocal.VM_THREAD.setConstantReference(threadLocals, Reference.fromJava(thread));
        // insert this thread locals into the list
        setNext(threadLocals, ACTIVE.threadLocalsListHead);
        setPrev(ACTIVE.threadLocalsListHead, threadLocals);
        // at the head
        ACTIVE.threadLocalsListHead = threadLocals;
        // account for a non-daemon thread
        if (!thread.javaThread().isDaemon() && thread != VmThread.MAIN_VM_THREAD) {
            ACTIVE.nonDaemonThreads++;
        }
        // and signal that this thread has started up and joined the list
        ACTIVE.threadStartCount++;
    }

    /**
     * Remove the specified VM thread locals from this thread map.
     *
     * <b>NOTE: This method is not synchronized. It is required that the caller synchronizes on ACTIVE.</b>
     *
     * @param threadLocals the thread locals to remove from this map
     * @param daemon specifies if the thread is a daemon
     */
    public void removeThreadLocals(Pointer threadLocals, boolean daemon) {
        final int id = VmThreadLocal.ID.getConstantWord(threadLocals).asAddress().toInt();
        if (threadLocalsListHead == threadLocals) {
            // this vm thread locals is at the head of list
            threadLocalsListHead = getNext(threadLocalsListHead);
        } else {
            // this vm thread locals is somewhere in the middle
            final Pointer prev = getPrev(threadLocals);
            final Pointer next = getNext(threadLocals);
            setPrev(next, prev);
            setNext(prev, next);
        }
        // set this vm thread locals' links to zero
        setPrev(threadLocals, Pointer.zero());
        setNext(threadLocals, Pointer.zero());
        // release the ID for a later thread's use
        VmThread thread = idMap.get(id);
        idMap.release(id);

        if (!daemon && thread != VmThread.MAIN_VM_THREAD) {
            nonDaemonThreads--;
            ACTIVE.notify();
        }
    }

    private VmThreadMap() {
    }

    /**
     * Creates the native thread for a VM thread and start it running. This method acquires an ID
     * for the new thread and returns it to the caller.
     *
     * @param thread the VM thread to create
     * @param stackSize the requested stack size
     * @param priority the initial priority of the thread
     * @return the native thread created
     */
    public Word startThread(VmThread thread, Size stackSize, int priority) {
        synchronized (ACTIVE) {
            final int id = idMap.acquire(thread);
            final int count = threadStartCount;
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
                thread.beTerminated();
                throw new InternalError("waitForThreadStartup() failed");
            }
            return nativeThread;
        }
    }

    /**
     * Waits for all non-daemon threads to finish.
     *
     * This must only be called by the {@linkplain VmThread#MAIN_VM_THREAD main} thread.
     */
    public void joinAllNonDaemons() {
        FatalError.check(VmThread.current() == VmThread.MAIN_VM_THREAD, "Only the main thread should join non-daemon threads");
        while (nonDaemonThreads > 0) {
            if (VmThread.TRACE_THREADS_OPTION.getValue()) {
                boolean lockDisabledSafepoints = Log.lock();
                Log.print("Main thread waiting for ");
                Log.print(nonDaemonThreads);
                Log.println(" non-daemon threads to terminate");
                Log.unlock(lockDisabledSafepoints);
            }
            synchronized (ACTIVE) {
                try {
                    ACTIVE.wait();
                } catch (Exception exception) {
                    FatalError.unexpected("Error waiting for all non-daemon threads", exception);
                }
            }
        }
        if (VmThread.TRACE_THREADS_OPTION.getValue()) {
            Log.println("Main thread finished waiting for all non-daemon threads to terminate");
        }
    }

    private boolean waitForThreadStartup(int count) {
        int spin = 10000;
        while (threadStartCount == count) {
            // spin for a little while, waiting for other thread to start
            if (spin-- == 0) {
                spin = 100;
                while (threadStartCount == count) {
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
    public void forAllThreads(Predicate<VmThread> predicate, Procedure<VmThread> procedure) {
        Pointer threadLocals = threadLocalsListHead;
        while (!threadLocals.isZero()) {
            final VmThread thread = UnsafeCast.asVmThread(VmThreadLocal.VM_THREAD.getConstantReference(threadLocals).toJava());
            if (predicate == null || predicate.evaluate(thread)) {
                procedure.run(thread);
            }
            threadLocals = getNext(threadLocals);
        }
    }

    /**
     * Iterates over all the VM thread locals in this thread map and run the specified procedure.
     *
     * @param predicate a predicate to check on the VM thread locals
     * @param procedure the procedure to apply to each VM thread locals
     */
    public void forAllThreadLocals(Pointer.Predicate predicate, Pointer.Procedure procedure) {
        Pointer threadLocals = threadLocalsListHead;
        while (!threadLocals.isZero()) {
            if (predicate == null || predicate.evaluate(threadLocals)) {
                procedure.run(threadLocals);
            }
            threadLocals = getNext(threadLocals);
        }
    }

    public static final Pointer.Predicate isNotCurrent = new Pointer.Predicate() {
        public boolean evaluate(Pointer threadLocals) {
            return threadLocals != VmThread.current().vmThreadLocals();
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
}
