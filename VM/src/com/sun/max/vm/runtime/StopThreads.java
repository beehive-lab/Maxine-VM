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

import com.sun.cri.bytecode.Bytecodes.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * A common mechanism for stopping a set of threads using the safepoint mechanism, and running some subclass-specific
 * operation, specified via {@link #processThread}.
 *
 * The threads are stopped "simultaneously", then {@link ThreadProcessor#processThread} is run on each thread serially.
 *
 * The basic stop/resume mechanism is reflected in the nested classes {@link Safepoint.ForceSafepoint},
 * {@link Safepoint.WaitUntilStopped}, {@link Safepoint.BlockOnACTIVE} and {@link Safepoint.ResetMutator}. N.B. this
 * code was re-factored from {@link StopTheWorldGCDaemon}, and uses the {@link VmThreadLocal#GC_STATE} field in
 * {@link VmThreadLocal} and related values. I.e., stopping threads is treated as if it were a GC request.
 *
 * There are two ways to specify the set of threads to be stopped. One {@link FromArray} is via an array of
 * {@link Thread threads}, since this is the API used by the {@link java.lang.management} framework. The other
 * {@link ByPredicate} is based on a {@link Pointer.Predicate} as, for example, used in {@link StopTheWorldGCDaemon}.
 * The {@link ByPredicate} variant is allocation free, at some cost in the complexity of the interface, whereas the
 * {@link FromArray} variant is not. It is TODO how the allocation meta-circularity is resolved if, say, a GC is
 * requested during processing.
 *
 * @author Mick Jordan
 */
public class StopThreads {
    /**
     * {@link Pointer.Predicate} that ignores the current thread.
     */
    public static final IsNotCurrentThread isNotCurrentThread = new IsNotCurrentThread();

    public static final class IsNotCurrentThread implements Pointer.Predicate {

        public boolean evaluate(Pointer vmThreadLocals) {
            return vmThreadLocals != VmThread.current().vmThreadLocals();
        }
    }

    /**
     * {@link Pointer.Procedure} that runs a given {@link ThreadProcessor#processThread} method.
     */
    public static class ProcessProcedure implements Pointer.Procedure {
        private ThreadProcessor threadProcessor;
        public ProcessProcedure(ThreadProcessor threadProcessor) {
            this.threadProcessor = threadProcessor;
        }

        public void run(Pointer vmThreadLocals) {
            Pointer instructionPointer;
            Pointer stackPointer;
            Pointer framePointer;
            Pointer frameAnchor = JavaFrameAnchor.from(vmThreadLocals);
            assert !frameAnchor.isZero();
            instructionPointer = JavaFrameAnchor.PC.get(frameAnchor);
            stackPointer = JavaFrameAnchor.SP.get(frameAnchor);
            framePointer = JavaFrameAnchor.FP.get(frameAnchor);
            threadProcessor.processThread(vmThreadLocals, instructionPointer, stackPointer, framePointer);

        }
    }

    /**
     * Specifies the processing that takes place on a thread after it is stopped.
     */
    public interface ThreadProcessor {
        /**
         * Subclass-specific behaviour after a thread has stopped.
         *
         * @param threadLocals equivalent to {@code VmThread.fromJava(threads[i]).vmThreadLocals()}
         * @param instructionPointer at the point the thread was stopped
         * @param stackPointer at the point the thread was stopped
         * @param framePointer at the point the thread was stopped
         */
        void processThread(Pointer threadLocals, Pointer instructionPointer, Pointer stackPointer, Pointer framePointer);

    }

    /**
     * Stop a set of threads specified by a {@link Pointer.Predicate}.
     */
    public static class ByPredicate {

        private static final Safepoint.BlockOnACTIVE afterSafepoint = new Safepoint.BlockOnACTIVE();
        private static final Safepoint.ForceSafepoint forceSafepoint = new Safepoint.ForceSafepoint(afterSafepoint);
        private static final Safepoint.WaitUntilStopped waitUntilStopped = new Safepoint.WaitUntilStopped();
        private static final Safepoint.ResetMutator resetMutator = new Safepoint.ResetMutator();

        private Pointer.Predicate predicate;
        private ProcessProcedure processProcedure;

        public ByPredicate(Pointer.Predicate predicate, ProcessProcedure processProcedure) {
            this.predicate = predicate;
            this.processProcedure = processProcedure;
        }

        /**
         * Stops the threads that match the {@link #predicate} and invoke the {@link #processProcedure#run} method on each
         * thread. N.B. It is the responsibility of the caller to ensure that the thread invoking this method does not
         * match the predicate!
         */
        public void process() {
            synchronized (VmThreadMap.ACTIVE) {
                VmThreadMap.ACTIVE.forAllThreadLocals(predicate, forceSafepoint);
                MemoryBarriers.storeLoad();
                // wait for threads to reach safepoint
                VmThreadMap.ACTIVE.forAllThreadLocals(predicate, waitUntilStopped);

                VmThreadMap.ACTIVE.forAllThreadLocals(predicate, processProcedure);

                VmThreadMap.ACTIVE.forAllThreadLocals(predicate, resetMutator);
            }
        }
    }

    /**
     * Stop a set of threads specified by an array of {@link Thread threads}.
     */
    public static class FromArray extends ByPredicate {

        /**
         * This value will be non-negative if and only if the current thread at the time that
         * {@link StopThreads#FromArray} called was in {@link threads}.
         */
        protected int currentIndex = -1;

        private Thread[] threads;

        private static class ArrayPredicate implements Pointer.Predicate {
            private Thread[] threads;

            ArrayPredicate(Thread[] threads) {
                this.threads = threads;

            }

            public boolean evaluate(Pointer threadLocals) {
                for (int i = 0; i < threads.length; i++) {
                    if (threadLocals != VmThread.current().vmThreadLocals() && threadLocals == VmThread.fromJava(threads[i]).vmThreadLocals()) {
                        return true;
                    }
                }
                return false;
            }

        }

        public FromArray(Thread[] threads, ProcessProcedure processProcedure) {
            super(new ArrayPredicate(threads), processProcedure);
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
         *
         * @return index of the current thread of -1 if it was not in the request set.
         */
        public int currentIndex() {
            return currentIndex;
        }

        /**
         * Get index in {@code threads} array for thread identified by {@code threadLocals}.
         * @param threadLocals
         * @return index in array
         */
        public static int indexOf(Thread[] threads, Pointer threadLocals) {
            for (int i = 0; i < threads.length; i++) {
                if (VmThread.fromJava(threads[i]).vmThreadLocals() == threadLocals) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Get index in {@link #threads} array for thread identified by {@code threadLocals}.
         * @param threadLocals
         * @return index in array
         */
        public int indexOf(Pointer threadLocals) {
            return indexOf(threads, threadLocals);
        }
    }
}

