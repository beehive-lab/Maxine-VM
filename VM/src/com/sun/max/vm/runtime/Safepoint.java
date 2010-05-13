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

import java.lang.reflect.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.*;
import com.sun.max.vm.heap.StopTheWorldGCDaemon.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * The platform specific details of the mechanism by which a thread can be suspended via
 * polling at prudently chosen execution points. The polling has the effect of causing
 * a trap.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Hannes Payer
 * @author Paul Caprioli
 */
public abstract class Safepoint {

    /**
     * Flag indicating which mechanism is to be used for synchronizing mutator and GC threads. The mechanisms are
     * described below.
     * <dl>
     * <dt>CAS</dt>
     * <dd>Atomic compare-and-swap (CAS) instructions are used to enforce transitions through the following state
     * machine:
     *
     * <pre>
     *
     *     +------+                            +--------+                             +---------+
     *     |      |--- M:JNI-Prolog{STORE} --->|        |--- GC:WaitForNative{CAS} -->|         |
     *     | JAVA |                            | NATIVE |                             |   GC    |
     *     |      |<--- M:JNI-Epilog{CAS} -----|        |<----- GC:Reset{STORE} ------|         |
     *     +------+                            +--------+                             +---------+
     * </pre>
     *
     * The states pertain to the mutator thread.
     * Each transition describes which thread makes the transition ({@code M} == mutator thread, {@code GC} == GC
     * thread), the VM code implementing the transition ({@linkplain NativeCallPrologue#nativeCallPrologue() JNI-Prolog},
     * {@linkplain NativeCallEpilogue#nativeCallEpilogue(Pointer) JNI-Epilog}, {@linkplain WaitUntilNonMutating
     * WaitForNative} and {@linkplain ResetMutator Reset}) and the instruction used to update state ({@code CAS} ==
     * atomic compare-and-swap, {@code STORE} == normal memory store). The state is stored in the
     * {@link VmThreadLocal#MUTATOR_STATE} thread local variable of the mutator thread.</dd>
     *
     * <dt>FENCE</dt>
     * <dd>Memory fences are used to implement Dekkers algorithm to ensure that a thread is never
     * mutating during a GC. This mechanism uses both the {@link VmThreadLocal#MUTATOR_STATE} and
     * {@link VmThreadLocal#GC_STATE} thread local variables of the mutator thread. The operations
     * that access these variables are in {@link NativeCallPrologue#nativeCallPrologue()},
     * {@link NativeCallEpilogue#nativeCallEpilogue(Pointer)}, {@link WaitUntilNonMutating} and
     * {@link ResetMutator}.
     * </dd>
     * </dl>
     *
     * TODO: Make the choice for this value based on the mechanism proven to runs best on each platform.
     */
    public static final boolean UseCASBasedGCMutatorSynchronization = true;

    /**
     * Constant denoting a mutator thread is executing native code.
     */
    public static final Word THREAD_IN_NATIVE = Address.fromInt(0);

    /**
     * Constant denoting a mutator thread is executing Java code.
     */
    public static final Word THREAD_IN_JAVA = Address.fromInt(1);

    /**
     * Constant denoting a mutator thread is stopped for garbage collection.
     */
    public static final Word THREAD_IN_GC = Address.fromInt(2);

    /**
     * The three states a thread can be in with respect to safepoints.
     * Note that the order of these enum matches the layout of the three
     * {@linkplain VmThreadLocal thread local areas}.
     */
    public enum State implements PoolObject {
        TRIGGERED(SAFEPOINTS_TRIGGERED_THREAD_LOCALS),
        ENABLED(SAFEPOINTS_ENABLED_THREAD_LOCALS),
        DISABLED(SAFEPOINTS_DISABLED_THREAD_LOCALS);

        public static final IndexedSequence<State> CONSTANTS = new ArraySequence<State>(values());

        private final VmThreadLocal key;

        State(VmThreadLocal key) {
            this.key = key;
        }

        public int serial() {
            return ordinal();
        }

        public int offset() {
            return key.offset;
        }
    }

    @HOSTED_ONLY
    public static Safepoint create(VMConfiguration vmConfiguration) {
        try {
            final String isa = vmConfiguration.platform().processorKind.instructionSet.name();
            final Class<?> safepointClass = Class.forName(MaxPackage.fromClass(Safepoint.class).subPackage(isa.toLowerCase()).name() + "." + isa + Safepoint.class.getSimpleName());
            final Constructor<?> constructor = safepointClass.getConstructor(VMConfiguration.class);
            return (Safepoint) constructor.newInstance(vmConfiguration);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw ProgramError.unexpected("could not create safepoint: " + exception);
        }
    }

    public final byte[] code = createCode();

    protected Safepoint() {
    }

    /**
     * Gets the current value of the safepoint latch register.
     */
    @INLINE
    public static Pointer getLatchRegister() {
        return VMRegister.getSafepointLatchRegister();
    }

    /**
     * Updates the current value of the safepoint latch register.
     *
     * @param vmThreadLocals a pointer to a copy of the thread locals from which the base of the safepoints-enabled
     *            thread locals can be obtained
     */
    @INLINE
    public static void setLatchRegister(Pointer vmThreadLocals) {
        VMRegister.setSafepointLatchRegister(vmThreadLocals);
    }

    /**
     * Sets the value of the {@linkplain VmThreadLocal#SAFEPOINT_LATCH safepoint latch} in the safepoints-enabled VM
     * thread locals to point to the safepoints-triggered VM thread locals. This will cause a safepoint trap the next
     * time a safepoint instruction is executed while safepoints are enabled.
     *
     * @param vmThreadLocals a pointer to a copy of the thread locals from which the base of the safepoints-enabled
     *            thread locals can be obtained
     */
    public static void trigger(Pointer vmThreadLocals) {
        SAFEPOINT_LATCH.setVariableWord(vmThreadLocals, SAFEPOINTS_TRIGGERED_THREAD_LOCALS.getConstantWord(vmThreadLocals));
    }

    /**
     * Sets the value of the {@linkplain VmThreadLocal#SAFEPOINT_LATCH safepoint latch} in the safepoints-enabled VM
     * thread locals to point to itself. This means that subsequent executions of a safepoint instruction will not cause a trap
     * until safepoints are once again {@linkplain #trigger(Pointer) triggered}.
     *
     * @param vmThreadLocals a pointer to a copy of the thread locals from which the base of the safepoints-enabled
     *            thread locals can be obtained
     */
    public static void reset(Pointer vmThreadLocals) {
        SAFEPOINT_LATCH.setVariableWord(vmThreadLocals, SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals));
    }

    /**
     * Determines if safepoints are disabled for the current thread.
     * @return {@code true} if safepoints are disabled
     */
    public static boolean isDisabled() {
        return getLatchRegister().equals(SAFEPOINTS_DISABLED_THREAD_LOCALS.getConstantWord().asPointer());
    }

    /**
     * Determines if safepoints are triggered for the current thread.
     * @return {@code true} if safepoints are triggered
     */
    @INLINE
    public static boolean isTriggered() {
        return !MaxineVM.isHosted() && SAFEPOINT_LATCH.getVariableWord().equals(SAFEPOINTS_TRIGGERED_THREAD_LOCALS.getConstantWord());
    }

    /**
     * Disables safepoints for the current thread. To temporarily disable safepoints on a thread, a call to this method
     * should paired with a call to {@link #enable()}, passing the value returned by the former as the single
     * parameter to the later. That is:
     * <pre>
     *     boolean wasDisabled = Safepoint.disable();
     *     // perform action with safepoints disabled
     *     if (!wasDisabled) {
     *         Safepoints.enable();
     *     }
     * </pre>
     *
     * @return true if this call caused safepoints to be disabled (i.e. they were enabled upon entry to this method)
     */
    @INLINE
    public static boolean disable() {
        final boolean wasDisabled = getLatchRegister().equals(SAFEPOINTS_DISABLED_THREAD_LOCALS.getConstantWord().asPointer());
        setLatchRegister(SAFEPOINTS_DISABLED_THREAD_LOCALS.getConstantWord().asPointer());
        return wasDisabled;
    }

    /**
     * Enables safepoints for the current thread, irrespective of whether or not they are currently enabled.
     *
     * @see #disable()
     */
    @INLINE
    public static void enable() {
        setLatchRegister(SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord().asPointer());
    }

    public static void initializePrimordial(Pointer primordialVmThreadLocals) {
        primordialVmThreadLocals.setWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index, primordialVmThreadLocals);
        primordialVmThreadLocals.setWord(SAFEPOINTS_DISABLED_THREAD_LOCALS.index, primordialVmThreadLocals);
        primordialVmThreadLocals.setWord(SAFEPOINTS_TRIGGERED_THREAD_LOCALS.index, primordialVmThreadLocals);
        primordialVmThreadLocals.setWord(SAFEPOINT_LATCH.index, primordialVmThreadLocals);
        Safepoint.setLatchRegister(primordialVmThreadLocals);
    }

    /**
     * Emits a safepoint at the call site.
     */
    @INLINE
    public static void safepoint() {
        SafepointBuiltin.safepointBuiltin();
    }

    public abstract Symbol latchRegister();

    @HOSTED_ONLY
    protected abstract byte[] createCode();

    public boolean isAt(Pointer instructionPointer) {
        return Memory.equals(instructionPointer, code);
    }

    /**
     * An enum with constants denoting the type of code that a thread was executing when a safepoint occurred.
     */
    public static enum Venue {
        /**
         * Indicates that a safepoint has been reached on exit from native code
         * after the safepoint has been triggered.
         */
        NATIVE,
        /**
         * Indicates that a safepoint has been reached from Java code
         * without entering any native code since the safepoint was triggered.
         */
        JAVA
    }

    /**
     * Runs a given procedure on the thread denoted by {@code vmThreadLocals},
     * when that thread is at a safepoint and safepoints are disabled.
     * This method allows the VM to stop a thread at a safepoint and then run the specified procedure (e.g. garbage collection
     * or biased monitor revocation). Note that this method returns when the procedure is successfully
     * queued to be run on that thread, but the procedure may not actually have run yet.
     * Note also that this method will spin
     * if that thread is already executing or is scheduled to execute another procedure.
     *
     * @param vmThreadLocals the thread locals on which to push the procedure
     * @param procedure the procedure to run on this thread
     */
    public static void runProcedure(Pointer vmThreadLocals, Procedure procedure) {
        // spin until the SAFEPOINT_PROCEDURE field is null
        final Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
        while (true) {
            if (enabledVmThreadLocals.getReference(SAFEPOINT_PROCEDURE.index).isZero()) {
                if (enabledVmThreadLocals.compareAndSwapReference(SAFEPOINT_PROCEDURE.offset, null, Reference.fromJava(procedure)).isZero()) {
                    Safepoint.trigger(vmThreadLocals);
                    return;
                }
            }
            Thread.yield();
        }
    }

    /**
     * Cancel a procedure that may be outstanding for a thread. If the thread has not yet executed the procedure,
     * then it will be canceled (i.e. {@link VmThreadLocal#SAFEPOINT_PROCEDURE} will be set to {@code null}).
     *
     * @param vmThreadLocals the thread for which to cancel the procedure
     */
    public static void cancelProcedure(Pointer vmThreadLocals) {
        SAFEPOINT_PROCEDURE.setVariableReference(vmThreadLocals, null);
    }

    /**
     * A procedure to run at a safepoint.
     *
     * @author Ben L. Titzer
     */
    public interface Procedure {
        /**
         * Runs this procedure.
         *
         * @param trapState the thread state recorded by the trap
         */
        void run(Pointer trapState);
    }

    /**
     * A procedure that resets safepoints for a given thread and {@linkplain Safepoint#cancelProcedure(Pointer) cancels} the safepoint procedure.
     */
    public static class ResetSafepoints implements Pointer.Procedure {
        /**
         * Resets safepoints for the thread associated with the given thread locals.
         */
        public void run(Pointer vmThreadLocals) {
            cancelProcedure(vmThreadLocals);
            reset(vmThreadLocals);
        }
    }
}
