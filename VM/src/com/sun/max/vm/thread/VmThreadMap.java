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

import java.util.ArrayList;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.monitor.modal.sync.nat.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.prototype.BootImage.*;
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

    /**
     * Once true, no more threads can be started.
     */
    private volatile boolean mainThreadExited;

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
     * Adds a pre-allocated thread to the map. This reserves an ID for the thread
     * but does not add its thread locals to the global list of running threads.
     *
     * @param thread a pre-allocated thread
     */
    public static void addPreallocatedThread(VmThread thread) {
        ACTIVE.idMap.acquire(thread);
    }

    /**
     * Adds the specified thread locals to the ACTIVE thread map and initializes several of its
     * important values (such as its ID and VM thread reference).
     *
     * <b>NOTE: This method is not synchronized. It is required that the caller synchronizes on ACTIVE.</b>
     *
     * @param thread the VmThread to add
     * @param threadLocals a pointer to the VM thread locals for the thread
     * @param daemon specifies if {@code thread} is a daemon
     */
    public static void addThreadLocals(VmThread thread, Pointer threadLocals, boolean daemon) {
        setNext(threadLocals, ACTIVE.threadLocalsListHead);
        setPrev(ACTIVE.threadLocalsListHead, threadLocals);
        ACTIVE.threadLocalsListHead = threadLocals;
    }

    /**
     * Increments the number of active non-daemon threads by 1.
     *
     * <b>NOTE: This method is not synchronized. It is required that the caller synchronizes on ACTIVE.</b>
     *
     * @return {@code true} if the non-daemon thread can continue running; {@code false} if the main thread has already exited
     */
    static boolean incrementNonDaemonThreads() {
        if (ACTIVE.mainThreadExited) {
            return false;
        }
        if (VmThread.TRACE_THREADS_OPTION.getValue()) {
            boolean lockDisabledSafepoints = Log.lock();
            Log.print("Adding non-daemon thread - ");
            Log.print(ACTIVE.nonDaemonThreads + 1);
            Log.println(" non-daemon threads now running");
            Log.unlock(lockDisabledSafepoints);
        }
        ACTIVE.nonDaemonThreads++;
        return true;
    }

    /**
     * Decrements the number of active non-daemon threads by 1.
     *
     * <b>NOTE: This method is not synchronized. It is required that the caller synchronizes on ACTIVE.</b>
     */
    static void decrementNonDaemonThreads() {
        if (VmThread.TRACE_THREADS_OPTION.getValue()) {
            boolean lockDisabledSafepoints = Log.lock();
            Log.print("Removed non-daemon thread - ");
            Log.print(ACTIVE.nonDaemonThreads - 1);
            Log.println(" non-daemon threads remain");
            Log.unlock(lockDisabledSafepoints);
        }
        ACTIVE.nonDaemonThreads--;
        ACTIVE.notify();
    }

    /**
     * Remove the specified VM thread locals from this thread map.
     *
     * <b>NOTE: This method is not synchronized. It is required that the caller synchronizes on ACTIVE.</b>
     *
     * @param threadLocals the thread locals to remove from this map
     * @param daemon specifies if the thread is a daemon
     */
    public void removeThreadLocals(VmThread thread) {
        Pointer threadLocals = thread.vmThreadLocals();
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
        idMap.release(thread.id());
        if (!thread.daemon && thread != VmThread.MAIN_VM_THREAD) {
            decrementNonDaemonThreads();
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
    public void startThread(VmThread thread, Size stackSize, int priority) {
        synchronized (ACTIVE) {
            final int id = idMap.acquire(thread);
            thread.daemon = thread.javaThread().isDaemon();
            if (!thread.daemon) {
                if (!incrementNonDaemonThreads()) {
                    throw new IllegalStateException("Cannot start " + thread.javaThread() + " after the main thread has exited");
                }
            }

            final Word nativeThread = VmThread.nativeThreadCreate(id, stackSize, priority);
            if (nativeThread.isZero()) {
                /* This means that we did not create the native thread at all so there is nothing to
                 * terminate. Most likely we ran out of memory allocating the stack, so we throw
                 * an out of memory exception. There is a small possibility that the failure was in the
                 * actual OS thread creation but that would require a way to disambiguate.
                 */
                throw new OutOfMemoryError("Unable to create new native thread");
            }
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
        mainThreadExited = true;
    }

    /**
     * Iterates over all the VM threads in this thread map and run the specified procedure.
     * <b>NOTE: This method is not synchronized. It is recommended that the caller synchronizes on ACTIVE.</b>
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
     * <b>NOTE: This method is not synchronized. It is recommended that the caller synchronizes on ACTIVE.</b>
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

    public static Thread[] getThreads() {
        final ArrayList<Thread> threads = new ArrayList<Thread>();
        Procedure<VmThread> proc = new Procedure<VmThread>() {
            public void run(VmThread vmThread) {
                if (vmThread.javaThread() != null) {
                    threads.add(vmThread.javaThread());
                }
            }
        };
        synchronized (VmThreadMap.ACTIVE) {
            VmThreadMap.ACTIVE.forAllThreads(null, proc);
        }
        return threads.toArray(new Thread[threads.size()]);
    }

    public static StackTraceElement[][] dumpThreads(Thread[] threads) {
        FatalError.unimplemented();
        return null;
    }

}
