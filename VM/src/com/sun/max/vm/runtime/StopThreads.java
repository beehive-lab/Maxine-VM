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

import static com.sun.max.vm.thread.VmThreadLocal.*;
import static com.sun.max.vm.runtime.Safepoint.*;

import com.sun.cri.bytecode.Bytecodes.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * A common mechanism for stopping a set of threads using the safepoint mechanism, and running some subclass-specific operation, specified via
 * {@link #processThread}.
 * Two variants of the stopping mechanism are provided (at present). The first stops one thread at a time runs {@link #processThread} and then
 * releases the thread. The second variant stops all threads "simultaneously", then runs {@link #processThread} on each thread serially.
 * The generic method {@link #process} chooses one of these based on the system property {@value STOP_MECHANISM_PROPERTY}.
 * If the current thread is in set, it is not stopped, but the {@link #currentIndex} variable is set to indicate its presence.
 *
 * The mechanism is very similar to that used by {@link StopTheWorldGCDaemon}, and sets the {@link VmThreadLocal#GC_STATE}
 * value. I.e., it is treated like a GC. TODO: Resolve interactions with GC during processing.
 *
 * @author Mick Jordan
 */
public abstract class StopThreads {
    private static final String STOP_MECHANISM_PROPERTY = "max.vm.stopthread.par";
    protected Thread[] threads;
    /**
     * This value will be non-negative iff the current thread was in {@link threads}.
     */
    protected int currentIndex = -1;

    protected StopThreads(Thread[] threads) {
        this.threads = threads;
        for (int i = 0; i < threads.length; i++) {
            if (Thread.currentThread() == threads[i]) {
                currentIndex = i;
                break;
            }
        }
    }

    /**
     * Gets the index of the current thread (at the time the instance was created) in the request set.
     * @return index of the current thread of -1 if it was not in the request set.
     */
    public int currentIndex() {
        return currentIndex;
    }

    /**
     * Stops the threads in the request set and invoke the {@link ProcessThread} method in each thread.
     */
    public void process() {
        if (System.getProperty(STOP_MECHANISM_PROPERTY) != null) {
            parallelProcess();
        } else {
            serialProcess();
        }
    }

    /*
     * Serially, for each thread in the request set, stop thread, run the given {@link #processThread} method, and then release.
     */
    private void serialProcess() {
        for (int i = 0; i < threads.length; i++) {
            Thread thread = threads[i];
            // do not stop the current thread!
            if (i == currentIndex) {
                continue;
            }
            final Pointer theThreadLocals = VmThread.fromJava(thread).vmThreadLocals();

            Pointer.Predicate matchThread = new Pointer.Predicate() {

                public boolean evaluate(Pointer threadLocals) {
                    return threadLocals == theThreadLocals;
                }
            };

            synchronized (VmThreadMap.ACTIVE) {
                VmThreadMap.ACTIVE.forAllThreadLocals(matchThread, stopThread);
                MemoryBarriers.storeLoad();
                // wait for thread to reach safepoint
                VmThreadMap.ACTIVE.forAllThreadLocals(matchThread, waitUntilStopped);
                final TrapStateAccess.MethodState methodState = new TrapStateAccess.MethodState();
                // We detect that the thread was stopped in native code by the SAFEPOINT_SCRATCH
                // field being zero since the safepoint procedure will not have run
                final Word safePointTrapState = SAFEPOINT_SCRATCH.getConstantWord(theThreadLocals);
                if (safePointTrapState.isZero()) {
                    Pointer frameAnchor = JavaFrameAnchor.from(theThreadLocals);
                    assert !frameAnchor.isZero();
                    methodState.instructionPointer = JavaFrameAnchor.PC.get(frameAnchor);
                    methodState.stackPointer = JavaFrameAnchor.SP.get(frameAnchor);
                    methodState.framePointer = JavaFrameAnchor.FP.get(frameAnchor);
                } else {
                    TrapStateAccess.getMethodState(safePointTrapState.asPointer(), methodState);
                }
                processThread(i, theThreadLocals, methodState);
                VmThreadMap.ACTIVE.forAllThreadLocals(matchThread, resetMutator);
            }
        }
    }

    /**
     * Stop the threads in parallel, then process them serially.
     */
    private void parallelProcess() {
        Pointer.Predicate matchThreads = new Pointer.Predicate() {

            public boolean evaluate(Pointer threadLocals) {
                for (int i = 0; i < threads.length; i++) {
                    if (i != currentIndex && VmThread.fromJava(threads[i]).vmThreadLocals() == threadLocals) {
                        return true;
                    }
                }
                return false;
            }
        };

        synchronized (VmThreadMap.ACTIVE) {
            VmThreadMap.ACTIVE.forAllThreadLocals(matchThreads, stopThread);
            MemoryBarriers.storeLoad();
            // wait for threads to reach safepoint
            VmThreadMap.ACTIVE.forAllThreadLocals(matchThreads, waitUntilStopped);
            for (int i = 0; i < threads.length; i++) {
                if (i == currentIndex) {
                    continue;
                }
                final TrapStateAccess.MethodState methodState = new TrapStateAccess.MethodState();
                Pointer theThreadLocals = VmThread.fromJava(threads[i]).vmThreadLocals();
                // We detect that the thread was stopped in native code by the SAFEPOINT_SCRATCH
                // field being zero since the safepoint procedure will not have run
                final Word safePointTrapState = SAFEPOINT_SCRATCH.getConstantWord(theThreadLocals);
                if (safePointTrapState.isZero()) {
                    Pointer frameAnchor = JavaFrameAnchor.from(theThreadLocals);
                    assert !frameAnchor.isZero();
                    methodState.instructionPointer = JavaFrameAnchor.PC.get(frameAnchor);
                    methodState.stackPointer = JavaFrameAnchor.SP.get(frameAnchor);
                    methodState.framePointer = JavaFrameAnchor.FP.get(frameAnchor);
                } else {
                    TrapStateAccess.getMethodState(safePointTrapState.asPointer(), methodState);
                }
                processThread(i, theThreadLocals, methodState);
            }
            VmThreadMap.ACTIVE.forAllThreadLocals(matchThreads, resetMutator);
        }
    }

    /**
     * Subclass-specific behavior after a thread has stopped.
     *
     * @param index index into {@link #threads} of thread being processed.
     * @param threadLocals equivalent to {@code VmThread.fromJava(threads[i]).vmThreadLocals()}
     * @param methodState the {@link TrapStateAccess.MethodState} at the point the thread was stopped
     */
    public abstract void processThread(int index, Pointer threadLocals, TrapStateAccess.MethodState methodState);

    private static class StopProcedure implements Pointer.Procedure {

        public void run(Pointer threadLocals) {
            GC_STATE.setVariableWord(threadLocals, Address.fromInt(1));
            SAFEPOINT_SCRATCH.setConstantWord(threadLocals, Address.zero());
            Safepoint.runProcedure(threadLocals, afterSafepoint);
        }

    }

    private static final StopProcedure stopThread = new StopProcedure();

    private static class AfterSafepoint implements Safepoint.Procedure {

        public void run(Pointer trapState) {
            SAFEPOINT_SCRATCH.setConstantWord(VmThread.currentVmThreadLocals(), trapState);
            synchronized (VmThreadMap.ACTIVE) {
                // block until initiator has done its processing
            }
        }
    }

    private static final AfterSafepoint afterSafepoint = new AfterSafepoint();

    private static class WaitUntilStopped implements Pointer.Procedure {
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
        }
    }

    private static final WaitUntilStopped waitUntilStopped = new WaitUntilStopped();

    private static final class ResetMutator extends Safepoint.ResetSafepoints {
        @Override
        public void run(Pointer vmThreadLocals) {
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

    private static final ResetMutator resetMutator = new ResetMutator();

}

