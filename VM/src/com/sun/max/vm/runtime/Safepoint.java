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
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * The platform specific details of the mechanism by which a thread can be suspended via
 * polling at prudently chosen execution points. The polling has the effect of causing
 * a trap.
 *
 * The <i>trap state</i> of the thread is recorded at such a trap and is made available
 * via these methods:
 * <ul>
 * <li>{@link #getInstructionPointer(Pointer)}</li>
 * <li>{@link #getStackPointer(Pointer, TargetMethod)}</li>
 * <li>{@link #getFramePointer(Pointer, TargetMethod)}</li>
 * <li>{@link #getSafepointLatch(Pointer)}</li>
 * <li>{@link #getRegisterState(Pointer)}</li>
 * <li>{@link #getTrapNumber(Pointer)}</li>
 * <li>{@link #setSafepointLatch(Pointer, Pointer)}</li>
 * </ul>
 *
 * @author Bernd Mathiske
 */
public abstract class Safepoint {

    public enum State implements PoolObject {
        ENABLED(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS),
        DISABLED(VmThreadLocal.SAFEPOINTS_DISABLED_THREAD_LOCALS),
        TRIGGERED(VmThreadLocal.SAFEPOINTS_TRIGGERED_THREAD_LOCALS);

        private final VmThreadLocal _key;

        State(VmThreadLocal key) {
            _key = key;
        }

        /**
         * Gets the address of VM thread locals storage area corresponding to this safepoint state.
         *
         * @param vmThreadLocals a pointer to any one of the thread locals storage areas
         */
        public Pointer vmThreadLocalsArea(Pointer vmThreadLocals) {
            return _key.getConstantWord(vmThreadLocals).asPointer();
        }

        @Override
        public int serial() {
            return ordinal();
        }

        public int offset() {
            return _key.offset();
        }

        public static final IndexedSequence<State> VALUES = new ArraySequence<State>(values());
    }

    public static Safepoint create(VMConfiguration vmConfiguration) {
        try {
            final String isa = vmConfiguration.platform().processorKind().instructionSet().name();
            final Class<?> safepointClass = Class.forName(MaxPackage.fromClass(Safepoint.class).subPackage(isa.toLowerCase()).name() + "." + isa + Safepoint.class.getSimpleName());
            final Constructor<?> constructor = safepointClass.getConstructor(VMConfiguration.class);
            return (Safepoint) constructor.newInstance(vmConfiguration);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw ProgramError.unexpected("could not create safepoint: " + exception);
        }
    }

    private VMConfiguration _vmConfiguration;

    public VMConfiguration vmConfiguration() {
        return _vmConfiguration;
    }

    protected Safepoint(VMConfiguration vmConfiguration) {
        _vmConfiguration = vmConfiguration;
    }

    @INLINE
    public static Pointer getLatchRegister() {
        return VMRegister.getSafepointLatchRegister();
    }

    @INLINE
    public static void setLatchRegister(Pointer latch) {
        VMRegister.setSafepointLatchRegister(latch);
    }

    public static void trigger(Pointer latch) {
        VmThreadLocal.SAFEPOINT_LATCH.setVariableWord(latch, VmThreadLocal.SAFEPOINTS_TRIGGERED_THREAD_LOCALS.getConstantWord(latch));
    }

    public static void reset(Pointer latch) {
        VmThreadLocal.SAFEPOINT_VENUE.setVariableReference(latch, Reference.fromJava(Safepoint.Venue.NATIVE));
        VmThreadLocal.SAFEPOINT_LATCH.setVariableWord(latch, VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(latch));
    }

    public static boolean isDisabled() {
        return getLatchRegister().equals(VmThreadLocal.SAFEPOINTS_DISABLED_THREAD_LOCALS.getConstantWord().asPointer());
    }

    @INLINE
    public static void disable() {
        setLatchRegister(VmThreadLocal.SAFEPOINTS_DISABLED_THREAD_LOCALS.getConstantWord().asPointer());
    }

    @INLINE
    public static void enable() {
        setLatchRegister(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord().asPointer());
    }

    public static void initializePrimordial(Pointer primordialVmThreadLocals) {
        primordialVmThreadLocals.setWord(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.index(), primordialVmThreadLocals);
        primordialVmThreadLocals.setWord(VmThreadLocal.SAFEPOINTS_DISABLED_THREAD_LOCALS.index(), primordialVmThreadLocals);
        primordialVmThreadLocals.setWord(VmThreadLocal.SAFEPOINTS_TRIGGERED_THREAD_LOCALS.index(), primordialVmThreadLocals);
        primordialVmThreadLocals.setWord(VmThreadLocal.SAFEPOINT_LATCH.index(), primordialVmThreadLocals);
        Safepoint.setLatchRegister(primordialVmThreadLocals);
    }

    @INLINE
    public static void soft() {
        SafepointBuiltin.softSafepoint();
    }

    /**
     *  A "hard" safepoint must stay in place no matter what optimization level.
     *  Furthermore, sufficient safepoint instructions and memory barriers must be emitted to guarantee stopping any traversal.
     *  This ensures that execution cannot run away from monitor blocking, wait() and language transitions,
     *  but can be caught and suspended right after any of those.
     */
    @INLINE
    public static void hard() {
        // Ensure that safepoint triggering is serialized:
        MemoryBarrier.storeLoad();

        SafepointBuiltin.hardSafepoint();
        SafepointBuiltin.hardSafepoint();

        // Ensure that stores to {@link VmThreadLocals.LAST_JAVA_CALLER_INSTRUCTION_POINTER} can only happen after GC:
        MemoryBarrier.loadStore();
    }

    @INLINE(override = true)
    public abstract Symbol latchRegister();

    protected abstract byte[] createCode();

    private final byte[] _code = createCode();

    public final byte[] code() {
        return _code;
    }

    public boolean isAt(Pointer instructionPointer) {
        return Memory.equals(instructionPointer, _code);
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
        JAVA;
    }

    /**
     * Run a given procedure on the thread corresponding to the specified {@code VmThread} instance,
     * when that thread is at a safepoint and safepoints are disabled.
     * This method allows the VM to stop a thread at a safepoint and then run the specified procedure (e.g. garbage collection
     * or biased monitor revocation). Note that this method returns when the procedure is successfully
     * queued to be run on that thread, but the procedure may not actually have run yet.
     * Note also that this method will spin
     * if that thread is already executing or is scheduled to execute another procedure.
     *
     * @param runnable the procedure to run on this thread
     */
    public static void runProcedure(Pointer vmThreadLocals, Procedure procedure) {
        // spin until the SAFEPOINT_PROCEDURE field is null
        while (true) {
            if (VmThreadLocal.SAFEPOINT_PROCEDURE.pointer(vmThreadLocals).compareAndSwapReference(null, Reference.fromJava(procedure)).isZero()) {
                Safepoint.trigger(vmThreadLocals);
                return;
            }
            Thread.yield();
        }
    }

    /**
     * Cancel a procedure that may be outstanding for a thread. If the thread has not yet executed the procedure,
     * then it will be cancelled (i.e. {@link VmThreadLocal#SAFEPOINT_PROCEDURE} will be set to {@code null}).
     *
     * @param vmThread the VMThread for which to cancel the procedure
     * @param procedure the procedure to cancel
     */
    public static void cancelProcedure(Pointer vmThreadLocals, Procedure procedure) {
        VmThreadLocal.SAFEPOINT_PROCEDURE.pointer(vmThreadLocals).compareAndSwapReference(Reference.fromJava(procedure), null);
    }

    /**
     * A procedure to run at a safepoint.
     *
     * @author Ben L. Titzer
     */
    public interface Procedure {
        void run(Pointer trapState);
    }

    /**
     * Reads the value of the instruction pointer saved in a given trap state area.
     *
     * @param trapState the block of memory holding the trap state
     * @return the value of the instruction pointer saved in {@code trapState}
     */
    public abstract Pointer getInstructionPointer(Pointer trapState);
    public abstract Pointer getStackPointer(Pointer trapState, TargetMethod targetMethod);
    public abstract Pointer getFramePointer(Pointer trapState, TargetMethod targetMethod);
    public abstract Pointer getSafepointLatch(Pointer trapState);
    public abstract void setSafepointLatch(Pointer trapState, Pointer value);
    public abstract int getTrapNumber(Pointer trapState);
    public abstract Pointer getRegisterState(Pointer trapState);
}
