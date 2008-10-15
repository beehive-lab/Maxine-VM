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
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
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

        public static final IndexedSequence<State> VALUES = new ArraySequence<State>(values());
    }

    public static Safepoint create(VMConfiguration vmConfiguration) {
        try {
            final String isa = vmConfiguration.platform().processorKind().instructionSet().name();
            final Class<?> safepointClass = Class.forName(MaxPackage.fromClass(Safepoint.class).subPackage(isa.toLowerCase()).name() + "." + isa + Safepoint.class.getSimpleName());
            final Constructor<?> constructor = safepointClass.getConstructor(VMConfiguration.class);
            return (Safepoint) constructor.newInstance(vmConfiguration);
        } catch (Exception exception) {
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

    /**
     * Returns the number of integer registers that needs to be saved in {@linkplain VmThreadLocal VM thread locals} by
     * safepoint traps. This may not match the total number of integer registers of the ISA, depending on operating
     * system ABI.
     *
     * @return the number of integer registers that needs to be saved in VM thread locals by safepoint traps
     */
    public abstract int numberOfIntegerRegisters();

    public abstract int numberOfFloatingPointRegisters();

    public abstract int latchRegisterIndex();

    @INLINE
    public static Pointer getLatchRegister() {
        return VMRegister.getSafepointLatchRegister();
    }

    @INLINE
    public static void setLatchRegister(Pointer latch) {
        VMRegister.setSafepointLatchRegister(latch);
    }

    public static void trigger(Pointer latch, Word nativeStub, Word javaStub) {
        VmThreadLocal.SAFEPOINT_NATIVE_STUB.setConstantWord(latch, nativeStub);
        VmThreadLocal.SAFEPOINT_JAVA_STUB.setConstantWord(latch, javaStub);
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

    public static void without(Runnable runnable) {
        try {
            Safepoint.disable();
            runnable.run();
        } finally {
            Safepoint.enable();
        }
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
     * Creates a machine code sequence to be run after the {@linkplain Trap Java trap handler} for a safepoint returns.
     * The stub is responsible for executing the code for which the safepoint was triggered (e.g. suspending a thread
     * for GC). Once that code returns, then the stub then resumes execution at the safepoint instruction.
     *
     * @param safepointEntry the code executing the post-safepoint action
     * @param venue specifies if the stub is to handle taking a safepoint when the thread is executing in Java code or
     *            in native code
     * @return the safepoint stub
     */
    public abstract SafepointStub createSafepointStub(CriticalMethod safepointEntry, Venue venue);

    private final int _latchRegisterIndex = latchRegister().value();

    public static Address trapHandler(Pointer vmThreadLocals) {
        final Address epoch = VmThreadLocal.SAFEPOINT_EPOCH.getVariableWord().asAddress().plus(1);
        VmThreadLocal.SAFEPOINT_EPOCH.setVariableWord(epoch);

        if (VmThreadLocal.inJava(vmThreadLocals)) {
            // We have reached a safepoint from normal Java code.
            VmThreadLocal.SAFEPOINT_VENUE.setVariableReference(vmThreadLocals, Reference.fromJava(Venue.JAVA));

            // Reenable safepoints when the stub will have returned:
            final Pointer enabledVmThreadLocals = VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            final Safepoint safepoint = VMConfiguration.hostOrTarget().safepoint();
            enabledVmThreadLocals.setWord(VmThreadLocal.REGISTERS.index() + safepoint._latchRegisterIndex, enabledVmThreadLocals);

            return VmThreadLocal.SAFEPOINT_JAVA_STUB.getConstantWord(vmThreadLocals).asAddress();
        }

        // ATTENTION: GC may be ongoing already - we must not use any references to objects that are not in the boot image!

        // We have reached a safepoint on exit from native code.
        VmThreadLocal.SAFEPOINT_VENUE.setVariableReference(vmThreadLocals, Reference.fromJava(Venue.NATIVE));
        return VmThreadLocal.SAFEPOINT_NATIVE_STUB.getConstantWord(vmThreadLocals).asAddress();
    }

}
