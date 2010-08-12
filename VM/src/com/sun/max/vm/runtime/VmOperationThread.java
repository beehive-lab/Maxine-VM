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

import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.thread.*;

/**
 * The thread used to {@linkplain #execute(VmOperation) execute} {@linkplain VmOperation VM operations}.
 *
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 * @author Doug Simon
 * @author Paul Caprioli
 * @author Hannes Payer
 * @author Mick Jordan
 */
public class VmOperationThread extends Thread {

    private final VmOperationQueue queue;

    private boolean shouldTerminate;

    private boolean terminated;

    static boolean TraceVmOperations;

    private static VmOperationThread instance;

    public static VmOperationThread instance() {
        return instance;
    }

    /**
     * Creates and starts the single thread used to execute VM operations.
     */
    public static void initialize() {
        new VmOperationThread().start();
    }

    static {
        VMOptions.addFieldOption("-XX:", "TraceVmOperations", VmOperationThread.class, "Trace VM operations.");
    }

    public VmOperationThread() {
        super("VmOperationThread");
        queue = new VmOperationQueue();
        setDaemon(true);
    }

    private static final Object REQUEST_LOCK = new Object();
    private static final Object QUEUE_LOCK = new Object();
    private static final Object TERMINATE_LOCK = new Object();
    static {
        JavaMonitorManager.bindStickyMonitor(REQUEST_LOCK);
        JavaMonitorManager.bindStickyMonitor(QUEUE_LOCK);
        JavaMonitorManager.bindStickyMonitor(TERMINATE_LOCK);
    }

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
        assert instance == null;
        instance = this;
        assert currentOperation == null;
        Heap.disableAllocationForCurrentThread();

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
                assert currentOperation == null;
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

            // Execute VM operation
            assert currentOperation != null;
            currentOperation.run();
            currentOperation.callingThread().decrementVmOperationCount();
            synchronized (REQUEST_LOCK) {
                REQUEST_LOCK.notifyAll();
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
     * Notifies the VM operation thread that it should stop. The current thread
     * is blocked until the VM thread stops.
     */
    public static void terminate() {
        VmOperationThread vmOperationThread = instance;
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

    public static void execute(VmOperation operation) {
        VmThread vmThread = VmThread.current();
        VmOperationThread vmOperationThread = instance;

        if (vmThread.isVmOperationThread()) {

            if (!operation.doItPrologue()) {
                return;
            }

            operation.setCallingThread(vmThread);

            vmThread.incrementVmOperationCount();

            // Add operation to queue
            synchronized (QUEUE_LOCK) {
                vmOperationThread.queue.add(operation);
                QUEUE_LOCK.notify();
            }

            // Wait until operation completes
            synchronized (REQUEST_LOCK) {
                while (vmThread.vmOperationCount() > 0) {
                    try {
                        REQUEST_LOCK.wait();
                    } catch (InterruptedException e) {
                        Log.println("Caught InterruptedException while waiting for VM operation to complete");
                    }
                }
            }

            operation.doItEpilogue();

        } else {
            // Invoked by VM operation thread: usually nested VM operation
            FatalError.unimplemented();
        }
    }
}
