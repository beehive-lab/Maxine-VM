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
package com.sun.max.vm.runtime;

import java.lang.Thread.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.thread.*;

/**
 * The thread used to {@linkplain #submit(VmOperation) execute} {@linkplain VmOperation VM operations}.
 *
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 * @author Doug Simon
 * @author Paul Caprioli
 * @author Hannes Payer
 * @author Mick Jordan
 */
public class VmOperationThread extends Thread implements UncaughtExceptionHandler {

    /**
     * A special exception thrown when a non-VM operation thread {@linkplain VmOperationThread#submit(VmOperation)
     * submits} a VM operation while holding the {@linkplain VmThreadMap#THREAD_LOCK thread lock}. There is a single,
     * pre-allocated {@linkplain #INSTANCE instance} of this object so that raising this exception does not require any
     * allocation.
     *
     * @author Doug Simon
     */
    public static final class HoldsThreadLockError extends OutOfMemoryError {

        private HoldsThreadLockError() {
        }

        public static final HoldsThreadLockError INSTANCE = new HoldsThreadLockError();
    }

    private final VmOperationQueue queue;

    private boolean shouldTerminate;

    private boolean terminated;

    static boolean TraceVmOperations;

    public static VmOperationThread instance() {
        return (VmOperationThread) VmThread.vmOperationThread.javaThread();
    }

    static {
        VMOptions.addFieldOption("-XX:", "TraceVmOperations", VmOperationThread.class, "Trace VM operations.");
    }

    @HOSTED_ONLY
    public VmOperationThread(ThreadGroup group) {
        super(group, "VmOperationThread");
        queue = new VmOperationQueue();
        setDaemon(true);
        setUncaughtExceptionHandler(this);
    }

    private static final Object REQUEST_LOCK = JavaMonitorManager.newVmLock("VM_OPERATION_REQUEST_LOCK");
    private static final Object QUEUE_LOCK = JavaMonitorManager.newVmLock("VM_OPERATION_QUEUE_LOCK");
    private static final Object TERMINATE_LOCK = JavaMonitorManager.newVmLock("VM_OPERATION_THREAD_TERMINATION_LOCK");

    @Override
    public final void start() {
        synchronized (QUEUE_LOCK) {
            super.start();
            try {
                // Block until the VM operation thread is waiting for requests:
                QUEUE_LOCK.wait();
            } catch (InterruptedException interruptedException) {
                ProgramError.unexpected(interruptedException);
            }
        }
    }

    private VmOperation currentOperation;

    @Override
    public void run() {
        if (TraceVmOperations) {
            Log.println("Started VM operation thread");
        }

        synchronized (QUEUE_LOCK) {
            // Let the thread that started the VM operation thread now continue
            QUEUE_LOCK.notify();
        }

        while (true) {

            // Wait for VM operation

            synchronized (QUEUE_LOCK) {
                FatalError.check(currentOperation == null, "Polling operation queue while current operation is pending");
                currentOperation = queue.poll();

                while (!shouldTerminate && currentOperation == null) {
                    try {
                        QUEUE_LOCK.wait();
                        currentOperation = queue.poll();
                    } catch (InterruptedException e) {
                        Log.println("Caught InterruptedException while polling VM operation queue");
                    }
                }

                if (shouldTerminate) {
                    break;
                }
            }

            if (TraceVmOperations) {
                boolean lockDisabledSafepoints = Log.lock();
                Log.print("VM operation thread about to run operation ");
                Log.print(currentOperation.name);
                Log.print(" submitted by ");
                Log.printThread(currentOperation.callingThread(), true);
                Log.unlock(lockDisabledSafepoints);
            }

            // Execute VM operation
            if (currentOperation.disablesHeapAllocation()) {
                Heap.disableAllocationForCurrentThread();
            }
            try {
                currentOperation.run();
            } finally {
                if (currentOperation.disablesHeapAllocation()) {
                    Heap.enableAllocationForCurrentThread();
                }

                currentOperation.callingThread().decrementPendingOperations();

                synchronized (REQUEST_LOCK) {
                    if (TraceVmOperations) {
                        boolean lockDisabledSafepoints = Log.lock();
                        Log.print("VM operation thread finished operation ");
                        Log.print(currentOperation.name);
                        Log.println(" and is notifying submitters");
                        Log.unlock(lockDisabledSafepoints);
                    }
                    REQUEST_LOCK.notifyAll();
                }

                currentOperation = null;
            }
        }

        // Signal other threads that VM operation thread is gone
        synchronized (TERMINATE_LOCK) {
            terminated = true;
            TERMINATE_LOCK.notify();
        }

        if (TraceVmOperations) {
            Log.println("VM operation thread stopped");
        }
    }

    /**
     * Schedules an operation for execution on the VM operation thread. The caller is
     * blocked until the operation is completed or the scheduling is canceled by
     * {@link VmOperation#doItPrologue(boolean)}.
     *
     * @param operation a VM operation to be executed on the VM operation thread
     */
    public static void submit(VmOperation operation) {
        VmThread vmThread = VmThread.current();
        VmOperationThread vmOperationThread = instance();

        if (!vmThread.isVmOperationThread()) {

            if (!operation.doItPrologue(false)) {
                // Operation was canceled
                return;
            }

            if (Thread.holdsLock(VmThreadMap.THREAD_LOCK)) {
                // The VM operation thread requires this lock to proceed
                throw VmOperationThread.HoldsThreadLockError.INSTANCE;
            }

            operation.setCallingThread(vmThread);

            if (operation.mode.isBlocking()) {
                vmThread.incrementPendingOperations();
            }

            // Add operation to queue
            synchronized (QUEUE_LOCK) {
                vmOperationThread.queue.add(operation);
                if (TraceVmOperations) {
                    boolean lockDisabledSafepoints = Log.lock();
                    Log.print("VM operation ");
                    Log.print(operation.name);
                    Log.print(" submitted by ");
                    Log.printThread(vmThread, false);
                    Log.println(" - notifying VM operation thread");
                    Log.unlock(lockDisabledSafepoints);
                }
                QUEUE_LOCK.notify();
            }

            if (operation.mode.isBlocking()) {
                // Wait until operation completes
                synchronized (REQUEST_LOCK) {
                    while (vmThread.pendingOperations() > 0) {
                        try {
                            REQUEST_LOCK.wait();
                        } catch (InterruptedException e) {
                            Log.println("Caught InterruptedException while waiting for VM operation to complete");
                        }
                    }
                }
            }

            operation.doItEpilogue(false);

            if (TraceVmOperations) {
                boolean lockDisabledSafepoints = Log.lock();
                Log.print("VM operation ");
                Log.print(operation.name);
                Log.print(" submitted by ");
                Log.printThread(vmThread, false);
                Log.println(" - done");
                Log.unlock(lockDisabledSafepoints);
            }


        } else {
            // Invoked by VM operation thread
            VmOperation enclosingOperation = vmOperationThread.currentOperation;
            boolean nested = enclosingOperation != null;
            if (nested) {
                // Nested operation: check that it's allowed for the enclosing operation
                boolean fatal = !enclosingOperation.allowsNestedOperations(operation);
                if (TraceVmOperations || fatal) {
                    boolean lockDisabledSafepoints = Log.lock();
                    Log.print("Nested VM operation ");
                    Log.print(operation.name);
                    Log.print(" requested by operation ");
                    Log.println(enclosingOperation.name);
                    Log.unlock(lockDisabledSafepoints);
                }
                if (fatal) {
                    FatalError.unexpected("Nested VM operation requested when current operation doesn't allow it");
                }

                operation.enclosing = enclosingOperation;
            }
            if (!operation.doItPrologue(nested)) {
                // Operation was canceled
                return;
            }


            vmOperationThread.currentOperation = operation;
            try {
                operation.run();
            } finally {
                operation.doItEpilogue(nested);
                vmOperationThread.currentOperation = enclosingOperation;
                operation.enclosing = null;
            }
        }
    }

    /**
     * Notifies the VM operation thread that it should stop. The current thread
     * is blocked until the VM thread stops.
     */
    public static void terminate() {
        VmOperationThread vmOperationThread = instance();
        vmOperationThread.shouldTerminate = true;

        if (TraceVmOperations) {
            Log.println("Terminating VM operation thread");

        }
        synchronized (QUEUE_LOCK) {
            QUEUE_LOCK.notify();
        }

        synchronized (TERMINATE_LOCK) {
            while (!vmOperationThread.terminated) {
                try {
                    TERMINATE_LOCK.wait();
                } catch (InterruptedException e) {
                    Log.println("Caught InterruptedException while waiting for VM operation thread to stop");
                }
            }
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable e) {
        FatalError.unexpected("Uncaught exception on VM operation thread", e);
    }
}
