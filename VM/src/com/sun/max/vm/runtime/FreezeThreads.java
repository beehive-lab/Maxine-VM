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

import java.util.*;

import com.sun.cri.bytecode.Bytecodes.MemoryBarriers;
import com.sun.max.unsafe.*;
import com.sun.max.unsafe.Pointer.Predicate;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.NativeCallEpilogue;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.NativeCallPrologue;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * A mechanism for a <i>freezer</i> thread to freeze one or more <i>mutator</i> threads,
 * perform some {@linkplain #perform() operation} and finally unfreeze the mutator threads.
 * A frozen mutator thread is blocked in native
 * code (typically on an OS-level lock) and cannot (re)enter compiled/interpreted
 * Java code without being {@linkplain ThawThread thawed} by the freezer thread.
 * Every frame of a compiled/interpreted method on a frozen thread's stack is guaranteed to be
 * at an execution point where the complete frame state of the method is available.
 * <p>
 * Freezing a thread is a co-operative action involving the freezer thread and the thread being frozen.
 * There are two alternative implementations of this mechanism provided. The first uses atomic instructions
 * and the second uses memory fences. They are named "CAS" and "FENCE" and are described further below.
 * <dl>
 * <dt>CAS</dt>
 * <dd>Atomic compare-and-swap (CAS) instructions are used to enforce transitions through the following state
 * machine:
 *
 * <pre>
 *     +------+                            +--------+                               +---------+
 *     |      |--- M:JNI-Prolog{STORE} --->|        |--- F:WaitUntilFrozen{CAS} --->|         |
 *     | JAVA |                            | NATIVE |                               | FROZEN  |
 *     |      |<--- M:JNI-Epilog{CAS} -----|        |<----- F:ThawThread{STORE} ----|         |
 *     +------+                            +--------+                               +---------+
 * </pre>
 * The syntax for each transition operation is:
 * <pre>
 *       thread ':' code '{' update-instruction '}'
 * </pre>
 *
 * The states pertain to the mutator thread and is recorded in the
 * {@link VmThreadLocal#MUTATOR_STATE} thread local variable of the mutator thread.
 * Each transition describes which thread makes the transition ({@code M} == mutator thread, {@code F} == freezer
 * thread), the VM code implementing the transition ({@linkplain NativeCallPrologue#nativeCallPrologue() JNI-Prolog},
 * {@linkplain NativeCallEpilogue#nativeCallEpilogue() JNI-Epilog}, {@linkplain WaitUntilFrozen
 * WaitUntilFrozen} and {@linkplain ThawThread ThawThread}) and the instruction used to update state ({@code CAS} ==
 * atomic compare-and-swap, {@code STORE} == normal memory store).</dd>
 *
 * <dt>FENCE</dt>
 * <dd>Memory fences are used to implement Dekkers algorithm to ensure that a thread is never
 * mutating during a GC. This mechanism uses both the {@link VmThreadLocal#MUTATOR_STATE} and
 * {@link VmThreadLocal#FROZEN} thread local variables of the mutator thread. The operations
 * that access these variables are in {@link NativeCallPrologue#nativeCallPrologue()},
 * {@link NativeCallEpilogue#nativeCallEpilogue()}, {@link WaitUntilFrozen} and
 * {@link ThawThread}.
 * </dd>
 * </dl>
 * The choice of which synchronization mechanism to use is specified by the {@link #UseCASBasedThreadFreezing} variable.
 * TODO: Make the choice for this value based on the mechanism proven to runs best on each platform.
 * <p>
 * Freezing a thread requires making it enter native code. For threads already in native code, this is trivial (i.e.
 * there's nothing to do except to transition them to the frozen state). For threads executing in Java code,
 * {@linkplain Safepoint safepoints} are employed.
 * Safepoints are small polling code sequences injected by the compiler at prudently chosen execution points.
 * The effect of executing a triggered safepoint is for the thread to trap. The trap handler will then call
 * a specified {@linkplain AtSafepoint} procedure. This procedure synchronizes
 * on the global GC and thread lock. Since the freezer thread holds this lock, a trapped thread will
 * eventually enter native code to block on the native monitor associated with the lock.
 * <p>
 * This mechanism is similar to but not exactly the same as the {@code VM_Operation} facility in HotSpot.
 * Some of the (current) differences are:
 * <ul>
 * <li>{@code FreezeThreads} can freeze a partial set of the running threads as Maxine has per-thread safepoints (HotSpot doesn't).</li>
 * <li>Any {@code FreezeThreads} operation that allocates inside {@link #perform()} should be aware that if GC is triggered, the
 * operation will be aborted via a {@link Heap.HoldsGCLockError} being raised. This is required so that the operation
 * relinquishes the {@linkplain VmThreadMap#ACTIVE GC lock} allowing GC to proceed.
 * TODO This constraint will be removed once a non-GC-heap-allocation facility is implemented for use in these
 * kind of operations. This will be something like the allocation of "resource" memory in HotSpot.
 * </ul>
 * <p>
 *
 * Implementation note:
 * It is simplest for a mutator thread to be blocked this way. Only under this condition can the
 * GC find every reference on a slave thread's stack.
 * If the mutator thread blocked in a spin loop instead, finding the references in the frame of
 * the spinning method is hard (what refmap would be used?). Even if the freezer thread
 * is not the GC thread, it may want to walk the stack of the mutator thread. Doing
 * so requires the freezer thread to be able to find the starting point for the stack walk
 * and this can only reliably be done (through use of the Java frame anchors) when the mutator
 * thread is blocked in native code.
 *
 * @author Mick Jordan
 * @author Doug Simon
 */
public class FreezeThreads {

    /**
     * Flag indicating which mechanism is to be used for synchronizing mutator and freezer threads.
     *
     * @see FreezeThreads
     */
    public static final boolean UseCASBasedThreadFreezing = true;

    /**
     * Constant denoting a mutator thread is executing/blocked in native code and is
     * allowed to transition back to {@link #THREAD_IN_JAVA}. A thread enters this
     * state on every JNI transition to native code.
     */
    public static final Word THREAD_IN_NATIVE = Address.fromInt(0);

    /**
     * Constant denoting a mutator thread is executing Java code.
     */
    public static final Word THREAD_IN_JAVA = Address.fromInt(1);

    /**
     * Constant denoting a mutator thread is executing/blocked in native code and it
     * cannot transition back to {@link #THREAD_IN_JAVA} without the controlling
     * thread allowing it to do so by setting its state to {@link #THREAD_IN_NATIVE}.
     */
    public static final Word THREAD_IS_FROZEN = Address.fromInt(2);

    /**
     * Encapsulates the procedure to be run by a thread just before it freezes
     * as a result of executing a safepoint. This allows each freezing thread
     * to perform part of the operation for which threads are being frozen
     * in the first place.
     *
     * @author Ben L. Titzer
     * @author Doug Simon
     */
    public static class AtSafepoint {

        /**
         * The default action performed by a thread that hits a safepoint for the purpose of freezing.
         */
        public static final AtSafepoint DEFAULT = new AtSafepoint();

        /**
         * Called by the trap handler when a safepoint is hit on the current thread.
         *
         * @param trapState the thread state recorded by the trap
         */
        public final void run(Pointer trapState) {
            // note that this procedure always runs with safepoints disabled
            final Pointer vmThreadLocals = Safepoint.getLatchRegister();
            if (!VmThreadLocal.inJava(vmThreadLocals)) {
                FatalError.unexpected("Freezing thread trapped while in native code");
            }

            if (TraceFreezing) {
                boolean lockDisabledSafepoints = Log.lock();
                Log.printCurrentThread(false);
                Log.println(": Froze at safepoint");
                Log.unlock(lockDisabledSafepoints);
            }

            // This thread must only transition to native code as a result of calling block().
            // Such a transition will be interpreted by the freezer thread to
            // mean that this thread is now blocked on the global GC and thread lock.
            // This invariant is enforced by disabling the ability to call native methods prior
            // to the call to block().
            NativeStubSnippet.disableNativeCallsForCurrentThread();

            beforeBlocking(trapState);
            block();
            afterBlocking(trapState);

            if (TraceFreezing) {
                boolean lockDisabledSafepoints = Log.lock();
                Log.printCurrentThread(false);
                Log.println(": Thawed from safepoint");
                Log.unlock(lockDisabledSafepoints);
            }
        }

        /**
         * Called just before the current thread freezes.
         *
         * @param trapState the thread state recorded by the trap
         */
        protected void beforeBlocking(Pointer trapState) {
        }

        /**
         * Called once a thread is {@linkplain ThawThread thawed} before it returns to the trap handler.
         *
         * @param trapState the thread state recorded by the trap, possibly modified by the freezer thread
         */
        protected void afterBlocking(Pointer trapState) {
        }

        private void block() {
            // Now re-enable the ability to call native code
            NativeStubSnippet.enableNativeCallsForCurrentThread();

            synchronized (VmThreadMap.ACTIVE) {
                // block on lock held by controlling thread
            }
        }
    }

    /**
     * Encapsulates the procedure to co-operatively freeze a thread.
     */
    class FreezeThread implements Pointer.Procedure {

        /**
         * Initiates the process to freeze a given thread.
         *
         * @param vmThreadLocals denotes the thread to be frozen
         */
        public final void run(Pointer vmThreadLocals) {

            // Freeze a thread already in native code
            if (!UseCASBasedThreadFreezing) {
                FROZEN.setVariableWord(vmThreadLocals, Address.fromInt(1));
            }

            // spin until the SAFEPOINT_PROCEDURE field is null
            final Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            while (true) {
                if (enabledVmThreadLocals.getReference(AT_SAFEPOINT_PROCEDURE.index).isZero()) {
                    if (enabledVmThreadLocals.compareAndSwapReference(AT_SAFEPOINT_PROCEDURE.offset, null, Reference.fromJava(atSafepoint)).isZero()) {
                        /*
                         * Set the value of the safepoint latch in the safepoints-enabled VM
                         * thread locals to point to the safepoints-triggered VM thread locals.
                         * This will cause a safepoint trap the next time a safepoint
                         * instruction is executed while safepoints are enabled.
                         */
                        SAFEPOINT_LATCH.setVariableWord(vmThreadLocals, SAFEPOINTS_TRIGGERED_THREAD_LOCALS.getConstantWord(vmThreadLocals));
                        return;
                    }
                }
                Thread.yield();
            }
        }

    }

    /**
     * Encapsulates the procedure run by a freezer thread to block until a thread {@linkplain FreezeThread freezes}.
     * The {@linkplain WaitUntilFrozen#run(Pointer) default} procedure can be extended by sub-classing {@link WaitUntilFrozen}
     * and overriding {@link #postRun(Pointer)}.
     */
    public static class WaitUntilFrozen implements Pointer.Procedure {

        public static final FreezeThreads.WaitUntilFrozen DEFAULT = new FreezeThreads.WaitUntilFrozen();

        /**
         * Called by {@link #run(Pointer)}. Subclasses can use this to perform extra actions
         * on a thread once it is frozen.
         *
         * @param vmThreadLocals thread locals of the thread that is now frozen
         */
        protected void postRun(Pointer vmThreadLocals) {
        }

        /**
         * Blocks until a given thread is frozen.
         *
         * @param vmThreadLocals thread locals of the thread to wait for
         */
        public final void run(Pointer vmThreadLocals) {
            if (UseCASBasedThreadFreezing) {
                final Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
                while (true) {
                    if (enabledVmThreadLocals.getWord(MUTATOR_STATE.index).equals(THREAD_IN_NATIVE)) {
                        if (enabledVmThreadLocals.compareAndSwapWord(MUTATOR_STATE.offset, THREAD_IN_NATIVE, THREAD_IS_FROZEN).equals(THREAD_IN_NATIVE)) {
                            // Transitioned thread into frozen state
                            break;
                        }
                    }
                    Thread.yield();
                }
            } else {
                while (MUTATOR_STATE.getVariableWord(vmThreadLocals).equals(THREAD_IN_JAVA)) {
                    // Wait for thread to be in native code, either as a result of a safepoint or because
                    // that's where it was when its FROZEN flag was set to true.
                    Thread.yield();
                }
            }

            postRun(vmThreadLocals);
        }
    }

    /**
     * Encapsulates the procedure run by a freezer thread to thaw a {@linkplain FreezeThreads frozen} thread.
     * The {@linkplain ThawThread#run(Pointer) default} procedure can be extended by sub-classing {@link ThawThread}
     * and overriding {@link #preRun(Pointer)}.
     */
    public static class ThawThread implements Pointer.Procedure {

        public static final FreezeThreads.ThawThread DEFAULT = new FreezeThreads.ThawThread();

        /**
         * Called by {@link #run(Pointer)}. Subclasses can use this to perform extra actions
         * on a thread before it is thawed.
         *
         * @param vmThreadLocals thread locals of the thread about to be thawed
         */
        protected void preRun(Pointer vmThreadLocals) {
        }

        /**
         * Thaws a frozen thread.
         *
         * @param vmThreadLocals thread locals of the thread about to be thawed
         */
        public final void run(Pointer vmThreadLocals) {

            preRun(vmThreadLocals);

            /*
             * Set the value of the safepoint latch in the safepoints-enabled VM
             * thread locals to point to itself. This means that subsequent executions
             * of a safepoint instruction will not cause a trap until safepoints
             * are once again triggered.
             */
            SAFEPOINT_LATCH.setVariableWord(vmThreadLocals, SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals));

            AT_SAFEPOINT_PROCEDURE.setVariableReference(vmThreadLocals, null);

            if (UseCASBasedThreadFreezing) {
                MUTATOR_STATE.setVariableWord(vmThreadLocals, THREAD_IN_NATIVE);
            } else {
                // This must be last so that a frozen thread trying to return out of native code stays
                // frozen until its safepoint related state has been completely reset
                FROZEN.setVariableWord(vmThreadLocals, Address.zero());
            }
        }
    }

    /**
     * Performs the operation represented by this object. This is called once all requested
     * threads have been frozen.
     *
     * The implementation of this method in {@link FreezeThreads} applies a {@linkplain #doThread thread specific operation}
     * to each frozen thread by calling {@link doAllThreads}.
     */
    protected void perform() {
        doAllThreads();
    }

    /**
     * Traverses over all frozen threads, applying {@link #doThread(Pointer, Pointer, Pointer, Pointer)} to each one.
     */
    protected final void doAllThreads() {
        if (predicate != null) {
            VmThreadMap.ACTIVE.forAllThreadLocals(predicate, doThreadAdapter);
        } else {
            doThreadAdapter.run(singleThread);
        }
    }

    /**
     * Performs an operation on a frozen thread.
     *
     * The definition of this method in {@link FreezeThreads} simply returns.
     *
     * @param threadLocals denotes the thread on which the operation is to be performed
     * @param ip instruction pointer at which the thread was frozen
     * @param sp stack pointer at which the thread was frozen
     * @param fp frame pointer at which the thread was frozen
     */
    protected void doThread(Pointer threadLocals, Pointer ip, Pointer sp, Pointer fp) {
    }

    /**
     * A descriptive name of the {@linkplain #perform() operation} represented by this object. This value is only used for tracing.
     */
    public final String name;

    /**
     * The procedure used to thaw the mutator threads.
     */
    public final ThawThread thawThread;

    public final WaitUntilFrozen waitUntilFrozen;

    /**
     * The predicate that determines if a thread denoted by a thread locals pointer is to be frozen.
     */
    private Pointer.Predicate predicate;

    private Pointer singleThread;

    public final FreezeThread freezeThread;

    public final AtSafepoint atSafepoint;

    /**
     * Adapter from {@link Pointer.Procedure#run(Pointer)} to {@linkplain #doThread(Pointer, Pointer, Pointer, Pointer)}.
     */
    private final Pointer.Procedure doThreadAdapter;

    /**
     * Creates an operation that will be performed while one or more frozen threads are frozen.
     *
     * @param name descriptive name of the {@linkplain #perform() operation}. This value is only used for tracing.
     * @param predicateOrThread a {@link Predicate} instance specifying which threads are to be frozen, a
     *            {@link VmThread} instance specifying a single thread to be frozen, or {@code null} if
     *            this information will be specified when the operation is {@linkplain #run(Object) run}
     * @param atSafepoint
     * @param waitUntilFrozen
     * @param thawThread
     */
    public FreezeThreads(String name, Object predicateOrThread, AtSafepoint atSafepoint, WaitUntilFrozen waitUntilFrozen, ThawThread thawThread) {
        this.name = name;
        if (predicateOrThread instanceof VmThread) {
            this.predicate = null;
            this.singleThread = ((VmThread) predicateOrThread).vmThreadLocals();
        } else {
            // This also handles the case when 'predicateOrThread' is null
            this.predicate = (Pointer.Predicate) predicateOrThread;
            this.singleThread = null;
        }
        this.atSafepoint = atSafepoint;
        this.freezeThread = new FreezeThread();
        this.waitUntilFrozen = waitUntilFrozen;
        this.thawThread = thawThread;

        doThreadAdapter = new Pointer.Procedure() {
            public void run(Pointer vmThreadLocals) {
                Pointer instructionPointer;
                Pointer stackPointer;
                Pointer framePointer;
                Pointer frameAnchor = JavaFrameAnchor.from(vmThreadLocals);
                assert !frameAnchor.isZero();
                instructionPointer = JavaFrameAnchor.PC.get(frameAnchor);
                stackPointer = JavaFrameAnchor.SP.get(frameAnchor);
                framePointer = JavaFrameAnchor.FP.get(frameAnchor);
                doThread(vmThreadLocals, instructionPointer, stackPointer, framePointer);
            }
        };
    }

    /**
     * Creates an operation that will be performed while one or more frozen threads are frozen.
     *
     * @param name descriptive name of the {@linkplain #perform() operation}. This value is only used for tracing.
     * @param predicateOrThread a {@link Predicate} instance specifying which threads are to be frozen upon or a
     *            {@link VmThread} instance specifying the single thread to be frozen
     */
    public FreezeThreads(String name, Object predicateOrThread) {
        this(name, predicateOrThread, AtSafepoint.DEFAULT, WaitUntilFrozen.DEFAULT, ThawThread.DEFAULT);
    }

    /**
     * Adapts a thread list to be a predicate on a thread locals pointer.
     *
     * @author Doug Simon
     */
    public static class ThreadListPredicate implements Pointer.Predicate {

        private final List<Thread> threads;

        /**
         * Creates a predicate that will return true for a thread locals pointer denoting
         * a non-null thread in {@code threads}.
         */
        public ThreadListPredicate(List<Thread> threads) {
            this.threads = threads;
        }

        /**
         * Returns true if {@code vmThreadLocals} denotes a thread in the list encapsulated by this predicate.
         */
        public boolean evaluate(Pointer vmThreadLocals) {
            VmThread vmThread = VmThread.fromVmThreadLocals(vmThreadLocals);
            return threads.contains(vmThread.javaThread());
        }
    }

    /**
     * Alternative version of {@link FreezeThreads#run()} that allows the thread(s) to be
     * explicitly specified. This means a single {@link FreezeThreads} operation being
     * target different threads each time it is run.
     *
     * @param predicateOrThread
     */
    public void run(Object predicateOrThread) {
        if (predicateOrThread instanceof VmThread) {
            this.predicate = null;
            this.singleThread = ((VmThread) predicateOrThread).vmThreadLocals();
        } else {
            this.predicate = (Pointer.Predicate) predicateOrThread;
            this.singleThread = null;
        }
        run();
    }

    /**
     * Executes the process to freeze one or more threads, perform the relevant
     * {@linkplain #perform() operation} once they are frozen and finally thaw/unfreeze the threads.
     * This method synchronizes on the global thread and GC lock (i.e. {@link VmThreadMap#ACTIVE}).
     * This means that:
     * <ul>
     * <li>The set of threads in the system does not change for the duration of the entire thread-freezing operation.</li>
     * <li>Thread-freezing operations are serialized.
     * </ul>
     * The current thread plays the role of the <i>freezer</i> thread and it cannot
     * intersect with the sets of threads that will be frozen.
     */
    public void run() {
        if (predicate == null) {
            assert singleThread != null;
            assert !singleThread.equals(VmThread.currentVmThreadLocals()) : "thread to be processed must not include the current thread";
        } else {
            assert !predicate.evaluate(VmThread.currentVmThreadLocals()) : "threads to be processed must not include the current thread";
        }

        Throwable error = null;
        synchronized (VmThreadMap.ACTIVE) {

            tracePhase("Freeze operation begin");

            freeze();

            // Ensures any safepoint-related control variables are visible for each thread
            // before the current thread reads such variables updated by a thread
            MemoryBarriers.storeLoad();

            waitUntilFrozen();

            try {
                performOperation();
            } catch (Throwable t) {
                if (TraceFreezing) {
                    boolean lockDisabledSafepoints = Log.lock();
                    Log.print(name);
                    Log.print(": error while running operation: ");
                    Log.println(ObjectAccess.readClassActor(t).name.string);
                    Log.unlock(lockDisabledSafepoints);
                }

                // Errors are propagated once the remaining phases of the operation are complete
                // otherwise frozen threads will neve be unfrozen
                error = t;
            }

            thaw();

            tracePhase("Freeze operation end");
        }

        if (error != null) {
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            } else if (error instanceof Error) {
                throw (Error) error;
            } else {
                throw (InternalError) new InternalError().initCause(error);
            }
        }
    }

    private void freeze() {
        tracePhase("Freezing thread(s)");
        if (predicate != null) {
            VmThreadMap.ACTIVE.forAllThreadLocals(predicate, freezeThread);
        } else {
            freezeThread.run(singleThread);
        }
    }

    private void waitUntilFrozen() {
        tracePhase("Waiting for thread(s) to freeze");
        if (predicate != null) {
            VmThreadMap.ACTIVE.forAllThreadLocals(predicate, waitUntilFrozen);
        } else {
            waitUntilFrozen.run(singleThread);
        }
    }

    private void performOperation() {
        tracePhase("Running operation");
        perform();
    }

    private void thaw() {
        tracePhase("Thawing thread(s)");
        if (predicate != null) {
            VmThreadMap.ACTIVE.forAllThreadLocals(predicate, thawThread);
        } else {
            thawThread.run(singleThread);
        }
    }

    private void tracePhase(String phaseMsg) {
        if (TraceFreezing) {
            boolean lockDisabledSafepoints = Log.lock();
            Log.print(name);
            Log.print(": ");
            Log.println(phaseMsg);
            Log.unlock(lockDisabledSafepoints);
        }
    }

    private static boolean TraceFreezing;

    static {
        VMOptions.addFieldOption("-XX:", "TraceFreezing", FreezeThreads.class, "Trace thread freezing phases.");
    }
}
