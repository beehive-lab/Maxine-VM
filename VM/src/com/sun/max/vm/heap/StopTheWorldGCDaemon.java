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
import static com.sun.max.vm.runtime.Safepoint.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.c1x.bytecode.Bytecodes.*;
import com.sun.max.sync.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.snippet.*;
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
 */
public class StopTheWorldGCDaemon extends BlockingServerDaemon {

    /**
     * The procedure that is run on a mutator thread that has been stopped by a safepoint for the
     * purpose of performing a stop-the-world garbage collection.
     */
    static final class AfterSafepoint implements Safepoint.Procedure {

        /**
         * Stops the current mutator thread for a garbage collection. Just before stopping, the
         * thread prepares its own stack reference map up to the trap frame. The remainder of the
         * stack reference map is prepared by a GC thread once this thread has stopped by blocking
         * on the global GC lock ({@link VmThreadMap#ACTIVE}).
         */
        public void run(Pointer trapState) {
            // note that this procedure always runs with safepoints disabled
            final Pointer vmThreadLocals = Safepoint.getLatchRegister();
            final Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            Heap.disableAllocationForCurrentThread();
            if (!VmThreadLocal.inJava(vmThreadLocals)) {
                FatalError.unexpected("Mutator thread trapped while in native code");
            }

            if (!enabledVmThreadLocals.getWord(LOWEST_ACTIVE_STACK_SLOT_ADDRESS.index).isZero()) {
                FatalError.unexpected("Stack reference map preparer should be cleared before GC");
            }

            // Stack reference map preparation must never include a call to a native method (e.g. blocking
            // due to synchronization) as it such a transition will be interpreted by a GC thread to
            // mean that the thread is now blocked on the GC lock as a result of synchronizing on
            // VmThreadMap.ACTIVE (below). Given that stack reference map preparation involves an
            // extensive amount of Java code, the easiest way to detect a violation this invariant
            // is by disabling the ability to call native methods (as opposed to continually
            // auditing any code that may be involved in stack reference map preparation).
            NativeStubSnippet.disableNativeCallsForCurrentThread();
            VmThreadLocal.prepareStackReferenceMapFromTrap(vmThreadLocals, trapState);
            NativeStubSnippet.enableNativeCallsForCurrentThread();

            synchronized (VmThreadMap.ACTIVE) {
                // Stops this thread until GC is done.
            }

            if (!enabledVmThreadLocals.getWord(LOWEST_ACTIVE_STACK_SLOT_ADDRESS.index).isZero()) {
                FatalError.unexpected("Stack reference map preparer should be cleared after GC");
            }

            if (Heap.traceGCPhases()) {
                Log.printCurrentThread(false);
                Log.println(": Restarting after GC");
            }

            Heap.enableAllocationForCurrentThread();
        }
        @Override
        public String toString() {
            return "SuspendMutatorForGC";
        }
    }

    private static AfterSafepoint afterSafepoint = new AfterSafepoint();

    static final class IsNotGCOrCurrentThread implements Pointer.Predicate {

        public boolean evaluate(Pointer vmThreadLocals) {
            return vmThreadLocals != VmThread.current().vmThreadLocals() && !VmThread.fromVmThreadLocals(vmThreadLocals).isGCThread();
        }
    }

    /**
     * The procedure that is run on the GC thread to stop a mutator thread. This means triggering safepoints for a
     * mutator thread as well as changing the {@linkplain VmThreadLocal#GC_STATE thread local} denoting that
     * a GC is now in progress.
     */
    final class StopMutator implements Pointer.Procedure {

        /**
         * Triggers safepoints for the thread associated with the given thread locals.
         */
        public void run(Pointer vmThreadLocals) {
            if (vmThreadLocals.isZero()) {
                // Thread is still starting up.
                // Do not need to do anything, because it will try to lock 'VmThreadMap.ACTIVE' and thus block.
            } else {
                GC_STATE.setVariableWord(vmThreadLocals, Address.fromInt(1));
                Safepoint.runProcedure(vmThreadLocals, afterSafepoint);
            }
        }
    }

    private final StopMutator stopMutator = new StopMutator();

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
    public final class ResetMutator extends Safepoint.ResetSafepoints {
        @Override
        public void run(Pointer vmThreadLocals) {
            if (Heap.traceGCPhases()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("GCDaemon: Resetting mutator thread ");
                Log.printThread(VmThread.fromVmThreadLocals(vmThreadLocals), true);
                Log.unlock(lockDisabledSafepoints);
            }

            // Indicates that the stack reference map for the thread is once-again unprepared.
            LOWEST_ACTIVE_STACK_SLOT_ADDRESS.setVariableWord(vmThreadLocals, Address.zero());

            // Resets the safepoint latch and resets the safepoint procedure to null
            super.run(vmThreadLocals);

            if (UseCASBasedGCMutatorSynchronization) {
                MUTATOR_STATE.setVariableWord(vmThreadLocals, THREAD_IN_NATIVE);
            } else {
                // This must be last so that a mutator thread trying to return out of native code stays in the spin
                // loop until its GC & safepoint related state has been completely reset
                GC_STATE.setVariableWord(vmThreadLocals, Address.zero());
            }
        }
    }

    private final ResetMutator resetMutator = new ResetMutator();

    public static class WaitUntilNonMutating implements Pointer.Procedure {
        long stackReferenceMapPreparationTime;
        public void run(Pointer vmThreadLocals) {
            if (Safepoint.UseCASBasedGCMutatorSynchronization) {
                final Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
                while (true) {
                    if (enabledVmThreadLocals.getWord(MUTATOR_STATE.index).equals(THREAD_IN_NATIVE)) {
                        if (enabledVmThreadLocals.compareAndSwapWord(MUTATOR_STATE.offset, THREAD_IN_NATIVE, THREAD_IN_GC).equals(THREAD_IN_NATIVE)) {
                            // Transitioned thread into GC
                            break;
                        }
                    }
                    Thread.yield();
                }
            } else {
                while (MUTATOR_STATE.getVariableWord(vmThreadLocals).equals(THREAD_IN_JAVA)) {
                    // Wait for thread to be in native code, either as a result of a safepoint or because
                    // that's where it was when its GC_STATE flag was set to true.
                    Thread.yield();
                }
            }

            final boolean threadWasInNative = LOWEST_ACTIVE_STACK_SLOT_ADDRESS.getVariableWord(vmThreadLocals).isZero();

            if (Heap.traceGCPhases()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("GCDaemon: Stopped mutator thread ");
                Log.printThread(VmThread.fromVmThreadLocals(vmThreadLocals), false);
                if (threadWasInNative) {
                    Log.println(" which was in native");
                } else {
                    Log.println(" which was in Java");
                }
                Log.unlock(lockDisabledSafepoints);
            }

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

    private final WaitUntilNonMutating waitUntilNonMutating = new WaitUntilNonMutating();

    private static final IsNotGCOrCurrentThread isNotGCOrCurrentThread = new IsNotGCOrCurrentThread();

    /**
     * The procedure that is run by the GC thread to perform a garbage collection.
     */
    class GCRequest implements Runnable {

        /**
         * A procedure supplied by the {@link HeapScheme} that implements a GC algorithm the request will execute.
         * This may be set to a different routine at every GC request.
         */
        private Collector collector;

        public void run() {
            // The lock for the special reference manager must be held before starting GC
            synchronized (SpecialReferenceManager.LOCK) {
                synchronized (VmThreadMap.ACTIVE) {
                    waitUntilNonMutating.stackReferenceMapPreparationTime = 0;

                    if (Heap.traceGCPhases()) {
                        Log.println("GCDaemon: Triggering safepoints for all mutators");
                    }

                    VmThreadMap.ACTIVE.forAllThreadLocals(isNotGCOrCurrentThread, stopMutator);

                    // Ensures the GC_STATE variable is visible for each thread before the GC thread reads
                    // the MUTATOR_STATE variable for each thread.
                    MemoryBarriers.storeLoad();

                    if (Heap.traceGCPhases()) {
                        Log.println("GCDaemon: Waiting for all mutators to stop");
                    }

                    VmThreadMap.ACTIVE.forAllThreadLocals(isNotGCOrCurrentThread, waitUntilNonMutating);

                    if (Heap.traceGCPhases()) {
                        Log.println("GCDaemon: Running GC algorithm");
                    }

                    // The next 2 statements *must* be adjacent as the reference map for this frame must
                    // be the same at both calls.
                    final long time = VmThreadLocal.prepareCurrentStackReferenceMap();
                    collector.run();

                    if (Heap.traceGCPhases()) {
                        Log.println("GCDaemon: Resetting mutators");
                    }

                    VmThreadMap.ACTIVE.forAllThreadLocals(isNotGCOrCurrentThread, resetMutator);
                    if (Heap.traceGCTime()) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("Stack reference map preparation time: ");
                        Log.print(time + waitUntilNonMutating.stackReferenceMapPreparationTime);
                        Log.println(TimerUtil.getHzSuffix(HeapScheme.GC_TIMING_CLOCK));
                        Log.unlock(lockDisabledSafepoints);
                    }

                    if (Heap.traceGCPhases()) {
                        Log.println("GCDaemon: Completed GC request");
                    }
                }
            }
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
     * Execute GC request with previously configured GC logic.
     */
    public void execute() {
        super.service(gcRequest);
    }

    /**
     * The garbage collection algorithm.
     */
    public abstract static class Collector implements Runnable {
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
        }

        /**
         * Executes a single garbage collection.
         *
         * @param invocationCount the number of previous executions
         */
        protected abstract void collect(int invocationCount);
    }

    /**
     * Executes GC request with some given GC algorithm.
     */
    public void execute(Collector collector) {
        gcRequest.collector = collector;
        super.service(gcRequest);
    }
}
