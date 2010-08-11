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
package com.sun.max.vm.heap;

import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.sync.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * A daemon thread that hangs around, waiting, then executes a given GC procedure when requested, then waits again.
 *
 * All other VM threads are forced into a non-mutating state while a request is being serviced. This can be used to
 * implement stop-the-world GC.
 *
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 * @author Doug Simon
 * @author Paul Caprioli
 * @author Hannes Payer
 * @author Mick Jordan
 */
public class StopTheWorldGCDaemon extends BlockingServerDaemon {

    /**
     * The procedure that is run on a mutator thread that has been stopped by a safepoint for the
     * purpose of performing a stop-the-world garbage collection.
     */
    static final class AfterSafepoint extends FreezeThreads.AtSafepoint {

        /**
         * Stops the current mutator thread for a garbage collection. Just before stopping, the
         * thread prepares its own stack reference map up to the trap frame. The remainder of the
         * stack reference map is prepared by a GC thread once this thread has stopped by blocking
         * on the global GC and thread lock ({@link VmThreadMap#ACTIVE}).
         */
        @Override
        public void beforeBlocking(Pointer trapState) {
            // note that this procedure always runs with safepoints disabled
            final Pointer vmThreadLocals = Safepoint.getLatchRegister();
            final Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            Heap.disableAllocationForCurrentThread();

            if (!enabledVmThreadLocals.getWord(LOWEST_ACTIVE_STACK_SLOT_ADDRESS.index).isZero()) {
                FatalError.unexpected("Stack reference map preparer should be cleared before GC");
            }

            VmThreadLocal.prepareStackReferenceMapFromTrap(vmThreadLocals, trapState);
        }

        @Override
        public void afterBlocking(Pointer trapState) {
            final Pointer vmThreadLocals = Safepoint.getLatchRegister();
            final Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            if (!enabledVmThreadLocals.getWord(LOWEST_ACTIVE_STACK_SLOT_ADDRESS.index).isZero()) {
                FatalError.unexpected("Stack reference map preparer should be cleared after GC");
            }

            Heap.enableAllocationForCurrentThread();
        }
        @Override
        public String toString() {
            return "SuspendMutatorForGC";
        }
    }

    /**
     * A VM option for triggering a GC at fixed intervals.
     */
    static final VMIntOption excessiveGCFrequency = register(new VMIntOption("-XX:ExcessiveGCFrequency=", 0,
        "Run a garbage collection every <n> milliseconds. A value of 0 disables this mechanism."), MaxineVM.Phase.STARTING);

    @Override
    protected long serverWaitTimeout() {
        return excessiveGCFrequency.getValue();
    }

    /**
     * The procedure that is run on the GC thread to reset the GC relevant state of a mutator thread
     * once GC is complete.
     */
    public static final class ResetMutator extends FreezeThreads.ThawThread {
        @Override
        public void preRun(Pointer vmThreadLocals) {
            // Indicates that the stack reference map for the thread is once-again unprepared.
            LOWEST_ACTIVE_STACK_SLOT_ADDRESS.setVariableWord(vmThreadLocals, Address.zero());
        }
    }

    public static class WaitUntilNonMutating extends FreezeThreads.WaitUntilFrozen {

        long stackReferenceMapPreparationTime;

        @Override
        public void postRun(Pointer vmThreadLocals) {

            final boolean threadWasInNative = LOWEST_ACTIVE_STACK_SLOT_ADDRESS.getVariableWord(vmThreadLocals).isZero();
            if (threadWasInNative) {
                // Since this thread is in native code it did not get an opportunity to prepare any of its stack reference map,
                // so we will take care of that for it now:
                stackReferenceMapPreparationTime += VmThreadLocal.prepareStackReferenceMap(vmThreadLocals);
            } else {
                // Threads that hit a safepoint in Java code have prepared *most* of their stack reference map themselves.
                // The part of the stack between the trap stub frame and the frame of the JNI stub that enters into the
                // native code for blocking on VmThreadMap.ACTIVE's monitor is not yet prepared. Do it now:
                final VmThread vmThread = VmThread.fromVmThreadLocals(vmThreadLocals);
                final StackReferenceMapPreparer stackReferenceMapPreparer = vmThread.stackReferenceMapPreparer();
                stackReferenceMapPreparer.completeStackReferenceMap(vmThreadLocals);
                stackReferenceMapPreparationTime += stackReferenceMapPreparer.preparationTime();
            }
        }
    }

    /**
     * The procedure that is run by the GC thread to perform a garbage collection.
     */
    static class GCRequest extends FreezeThreads implements Runnable {

        long gcThreadStackReferenceMapPreparationTime;

        /**
         * A procedure supplied by the {@link HeapScheme} that implements a GC algorithm the request will execute.
         * This may be set to a different routine at every GC request.
         */
        Collector collector;

        public GCRequest() {
            super("GCDaemon", VmThreadMap.isNotGCOrCurrentThread, new AfterSafepoint(), new WaitUntilNonMutating(), new ResetMutator());
        }

        @Override
        public void run() {
            // The lock for the special reference manager must be held before starting GC
            synchronized (SpecialReferenceManager.LOCK) {

                super.run();

                if (Heap.traceGCTime()) {
                    WaitUntilNonMutating waitUntilNonMutating = (WaitUntilNonMutating) waitUntilFrozen;
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.print("Stack reference map preparation time: ");
                    Log.print(gcThreadStackReferenceMapPreparationTime + waitUntilNonMutating.stackReferenceMapPreparationTime);
                    Log.println(TimerUtil.getHzSuffix(HeapScheme.GC_TIMING_CLOCK));
                    Log.unlock(lockDisabledSafepoints);
                    waitUntilNonMutating.stackReferenceMapPreparationTime = 0;
                    gcThreadStackReferenceMapPreparationTime = 0;
                }

            }
        }

        @Override
        public void perform() {
            // The next 2 statements *must* be adjacent as the reference map for this frame must
            // be the same at both calls.
            gcThreadStackReferenceMapPreparationTime = VmThreadLocal.prepareCurrentStackReferenceMap();
            collector.run();
        }
    }

    private final GCRequest gcRequest = new GCRequest();

    /**
     * Set the daemon with initial GC logic. The GC logic can be subsequently changes on every request using the
     * {@link #execute(Runnable)} method.
     *
     * @param name name of the daemon
     * @param collector initial GC logic
     */
    public StopTheWorldGCDaemon(String name, Collector collector) {
        super(name);
        gcRequest.collector = collector;
    }

    /**
     * Set the daemon without initial GC logic. First request must be executed using the {@link #execute(Runnable)} method.
     *
     * @param name name of the daemon
     */
    public StopTheWorldGCDaemon(String name) {
        super(name);
    }

    @Override
    public void run() {
        Heap.disableAllocationForCurrentThread();
        super.run();
    }

    /**
     * The garbage collection algorithm.
     */
    public abstract static class Collector {
        private int invocationCount;

        public final void run() {
            if (Heap.verbose()) {
                Log.print("--Start GC ");
                Log.print(invocationCount);
                Log.println("--");
            }
            collect(invocationCount);
            if (Heap.verbose()) {
                Log.print("--End GC ");
                Log.print(invocationCount);
                Log.println("--");
            }
            invocationCount++;
            if (Heap.TraceGCSuppressionCount > 0) {
                Heap.TraceGCSuppressionCount--;
            }
        }

        /**
         * Executes a single garbage collection.
         *
         * @param invocationCount the number of previous executions
         */
        protected abstract void collect(int invocationCount);
    }

    /**
     * Execute GC request with previously configured GC logic.
     */
    public void execute() {
        super.service(gcRequest);
    }

    /**
     * Executes GC request with some given GC algorithm.
     */
    public void execute(Collector collector) {
        gcRequest.collector = collector;
        super.service(gcRequest);
    }
}
