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

import java.util.*;

import com.sun.cri.bytecode.Bytecodes.MemoryBarriers;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * A mechanism for stopping one or more threads (using {@linkplain Safepoint safepoints} if necessary) and
 * performing an {@linkplain #perform(Pointer, Pointer, Pointer, Pointer) operation} on the stopped thread(s).
 *
 * The threads are stopped "simultaneously", then {@link ThreadProcessor#processThread} is run on each thread serially.
 *
 * The basic stop/resume mechanism is reflected in the classes {@link Safepoint.ForceSafepoint},
 * {@link Safepoint.WaitUntilStopped}, {@link Safepoint.BlockOnACTIVE} and {@link Safepoint.ResetMutator}. N.B. this
 * code was re-factored from {@link StopTheWorldGCDaemon}, and uses the {@link VmThreadLocal#GC_STATE} field in
 * {@link VmThreadLocal} and related values. I.e., stopping threads is treated as if it were a GC request.
 *
 * The set of threads to be stopped and operated upon is specified either as a {@linkplain #StopThreadsForOperation(List) list of
 * threads} or as a {@linkplain #StopThreadsForOperation(com.sun.max.unsafe.Pointer.Predicate) predicate} on a thread locals
 * pointer. The latter variant is allocation free whereas the former variant is not.
 *
 * TODO Resolve the question of what should happen if allocation causes a GC request is issued while performing the operation.
 *
 * @author Mick Jordan
 * @author Doug Simon
 */
public class StopThreadsForOperation {

    /**
     * Adapts a {@link Thread} object based predicate to a thread-locals based predicate.
     */
    public abstract static class ThreadPredicate implements Pointer.Predicate {
        @Override
        public boolean evaluate(Pointer vmThreadLocals) {
            return evaluate(VmThread.fromVmThreadLocals(vmThreadLocals).javaThread());
        }

        /**
         * Determines if a given thread is matched by this predicate.
         *
         * @param the thread to test (which may be null)
         */
        public abstract boolean evaluate(Thread thread);
    }

    /**
     * Performs an operation on a stopped thread.
     *
     * The definition of this method in {@link StopThreadsForOperation} simply returns.
     *
     * @param threadLocals denotes the thread on which the operation is to be performed
     * @param ip instruction pointer at the point the thread was stopped
     * @param sp stack pointer at the point the thread was stopped
     * @param fp frame pointer at the point the thread was stopped
     */
    public void perform(Pointer threadLocals, Pointer ip, Pointer sp, Pointer fp) {
    }

    private static final Safepoint.BlockOnACTIVE afterSafepoint = new Safepoint.BlockOnACTIVE();
    private static final Safepoint.ForceSafepoint forceSafepoint = new Safepoint.ForceSafepoint(afterSafepoint);
    private static final Safepoint.WaitUntilStopped waitUntilStopped = new Safepoint.WaitUntilStopped();
    private static final Safepoint.ResetMutator resetMutator = new Safepoint.ResetMutator();

    /**
     * The predicate that determines if a thread denoted by a thread locals pointer is to
     * be included in the set of threads for which the {@linkplain #perform(Pointer, Pointer, Pointer, Pointer) operation}
     * is performed.
     */
    public final Pointer.Predicate predicate;

    /**
     * Adapter from {@link Pointer.Procedure#run(Pointer)} to {@linkplain #perform(Pointer, Pointer, Pointer, Pointer)}.
     */
    private final Pointer.Procedure performAdapter;

    /**
     * Creates an operation that will be applied to one or more stopped threads.
     *
     * @param predicate specifies which threads are to be operated upon
     */
    public StopThreadsForOperation(Pointer.Predicate predicate) {
        this.predicate = predicate;
        performAdapter = new Pointer.Procedure() {
            public void run(Pointer vmThreadLocals) {
                Pointer instructionPointer;
                Pointer stackPointer;
                Pointer framePointer;
                Pointer frameAnchor = JavaFrameAnchor.from(vmThreadLocals);
                assert !frameAnchor.isZero();
                instructionPointer = JavaFrameAnchor.PC.get(frameAnchor);
                stackPointer = JavaFrameAnchor.SP.get(frameAnchor);
                framePointer = JavaFrameAnchor.FP.get(frameAnchor);
                perform(vmThreadLocals, instructionPointer, stackPointer, framePointer);
            }
        };
    }

    /**
     * Creates an operation that will be applied to one or more stopped threads.
     * The non-null entries in {@code threads} corresponding to live threads when
     * {@link #run()} is called are processed.
     *
     * @param threads an explicit list of the threads to be operated upon
     */
    public StopThreadsForOperation(final List<Thread> threads) {
        this(new ThreadPredicate() {
            @Override
            public boolean evaluate(Thread thread) {
                return threads.contains(thread);
            }
        });
    }

    /**
     * Stops the threads that match the {@link #predicate} and once they are all stopped,
     * {@linkplain #perform(Pointer, Pointer, Pointer, Pointer) performs}
     * the operation represented by this object on each stopped thread.
     *
     * Note that the current thread must not be matched by the predicate.
     */
    public void run() {
        assert !predicate.evaluate(VmThread.currentVmThreadLocals()) : "threads to be processed must not include the current thread";
        synchronized (VmThreadMap.ACTIVE) {
            VmThreadMap.ACTIVE.forAllThreadLocals(predicate, forceSafepoint);

            // Ensures any safepoint-related control variables are visible for each thread
            // before the current thread reads such variables updated by a thread
            MemoryBarriers.storeLoad();

            // wait for threads to reach safepoint
            VmThreadMap.ACTIVE.forAllThreadLocals(predicate, waitUntilStopped);

            VmThreadMap.ACTIVE.forAllThreadLocals(predicate, performAdapter);

            VmThreadMap.ACTIVE.forAllThreadLocals(predicate, resetMutator);
        }
    }
}
