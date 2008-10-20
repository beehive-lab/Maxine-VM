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
package com.sun.max.vm.thread;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.util.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.type.*;

/**
 * The predefined VM thread local variables and mechanisms for accessing them.
 *
 * The memory for these variables is allocated on the stack by the native code that starts a thread (see the function
 * 'thread_runJava' in Native/substrate/threads.c). All thread local variables occupy one word, except the last.
 * The safepoint latch must be first.
 * {@linkplain Safepoint safepoint} states for a thread:
 * <dl>
 * <dt>Enabled</dt>
 * <dd>Safepoints for the thread are {@linkplain Safepoint#enable() enabled}. The base address of this TLS area is
 * obtained by reading the {@link #SAFEPOINTS_ENABLED_THREAD_LOCALS} variable from any TLS area.</dd>
 * <dt>Disabled</dt>
 * <dd>Safepoints for the thread are {@linkplain Safepoint#disable() disabled}. The base address of this TLS area is
 * obtained by reading the {@link #SAFEPOINTS_DISABLED_THREAD_LOCALS} variable from any TLS area.</dd>
 * <dt>Triggered</dt>
 * <dd>Safepoints for the thread are {@linkplain Safepoint#trigger(Pointer, Word, Word) triggered}. The base address of
 * this TLS area is obtained by reading the {@link #SAFEPOINTS_TRIGGERED_THREAD_LOCALS} variable from any TLS area.</dd>
 * </dl>
 *
 * The memory for the three TLS areas is located on the stack as described in the thread stack layout
 * diagram {@linkplain VmThread here}.
 *
 * All thread local variables occupy one word, except the {@linkplain #REGISTERS last}.
 */
public enum VmThreadLocal {
    /**
     * Must be first as needed by {@link Safepoint#initialize(Pointer)}.
     */
    SAFEPOINT_LATCH(Kind.WORD),

    /**
     * The {@linkplain VmThread#currentVmThreadLocals() current} thread local storage when safepoints for the thread are
     * {@linkplain Safepoint#enable() enabled}.
     */
    SAFEPOINTS_ENABLED_THREAD_LOCALS(Kind.WORD),

    /**
     * The {@linkplain VmThread#currentVmThreadLocals() current} thread local storage when safepoints for the thread are
     * {@linkplain Safepoint#disable() disabled}.
     */
    SAFEPOINTS_DISABLED_THREAD_LOCALS(Kind.WORD),

    /**
     * The {@linkplain VmThread#currentVmThreadLocals() current} thread local storage when safepoints for the thread are
     * {@linkplain Safepoint#trigger(Pointer, Word, Word) triggered}.
     */
    SAFEPOINTS_TRIGGERED_THREAD_LOCALS(Kind.WORD),

    SAFEPOINT_EPOCH(Kind.WORD),
    ID(Kind.WORD),
    VM_THREAD(Kind.REFERENCE),
    NATIVE_THREAD(Kind.WORD),
    JNI_ENV(Kind.WORD),
    LAST_JAVA_CALLER_STACK_POINTER(Kind.WORD),
    LAST_JAVA_CALLER_FRAME_POINTER(Kind.WORD),

    /**
     * Records the instruction pointer in the Java frame for the call that transitioned to native code. If this value is
     * zero then, the thread is not in native code.
     */
    LAST_JAVA_CALLER_INSTRUCTION_POINTER(Kind.WORD),

    /**
     * Records information for the last Java caller for direct/C_FUNCTION calls.
     * This is only used by the Inspector for debugging
     */
    LAST_JAVA_CALLER_STACK_POINTER_FOR_C(Kind.WORD),

    /**
     * Records information for the last Java caller for direct/C_FUNCTION calls.
     * This is only used by the Inspector for debugging
     */
    LAST_JAVA_CALLER_FRAME_POINTER_FOR_C(Kind.WORD),

    /**
     * Records information for the last Java caller for direct/C_FUNCTION calls.
     * This is only used by the Inspector for debugging
     */
    LAST_JAVA_CALLER_INSTRUCTION_POINTER_FOR_C(Kind.WORD),

    /**
     * Holds the biased card table address. Card table writes are usually done using *(cardTableBase + reference-heap a.
     */
    ADJUSTED_CARDTABLE_BASE(Kind.WORD), TAG(Kind.WORD), SAFEPOINT_VENUE(Kind.REFERENCE), SAFEPOINT_NATIVE_STUB(Kind.WORD), SAFEPOINT_JAVA_STUB(Kind.WORD),

    /**
     * The value of the instruction pointer when the last trap occurred.
     *
     * @see #IN_TRAP_STUB_AFTER_RECORDING_TRAP_FRAME
     */
    TRAP_INSTRUCTION_POINTER(Kind.WORD),

    /**
     * The value of the stack pointer when the last trap occurred.
     *
     * @see #IN_TRAP_STUB_AFTER_RECORDING_TRAP_FRAME
     */
    TRAP_STACK_POINTER(Kind.WORD),

    /**
     * The value of the frame pointer when the last trap occurred.
     *
     * @see #IN_TRAP_STUB_AFTER_RECORDING_TRAP_FRAME
     */
    TRAP_FRAME_POINTER(Kind.WORD),

    /**
     * When a trap occurs, the {@link #TRAP_INSTRUCTION_POINTER}, {@link #TRAP_STACK_POINTER} and
     * {@link #TRAP_FRAME_POINTER} values are only guaranteed to reflect the trap frame info once the appropriate Java
     * trap handler (i.e. any of the methods in {@link Trap} annotated with {@link C_FUNCTION} with the
     * {@link C_FUNCTION#isSignalHandler()} element set to true) has returned. While execution is in a trap handler,
     * this guarantee only holds when this value is not zero.
     *
     * Note that this guarantee is somewhat conservative and is only used by the inspector as means for making a "best
     * effort" to show the stack when inspecting a thread currently executing in an exception handler. This guarantee is
     * conservative is because this thread local variable must be reset to zero before returning from the Java trap
     * handler and so there will be a short sequence of instructions between the zeroing of the variable and the return
     * instruction (e.g. restoring callee saved registers).
     */
    TRAP_HANDLER_HAS_RECORDED_TRAP_FRAME(Kind.WORD),

    /**
     * @see Deoptimizer.ReferenceOccurrences
     */
    DEOPTIMIZER_REFERENCE_OCCURRENCES(Kind.REFERENCE),

    /**
     * In case of Deoptimizer.ReferenceOccurrences.SAFEPOINT, when deoptimization occurs, we remember the instruction
     * where it was triggered via an illegal instruction trap. This is used in case there is a GC during deoptimization
     * to enable the stack map preparer to find the register reference map to be applied to the register copies in the
     * disabled thread local space. The latter is where the trap saved register values.
     */
    DEOPTIMIZER_INSTRUCTION_POINTER(Kind.WORD),

    METHOD_TRACE_COUNT(Kind.WORD),

    /**
     * The address of the stack slot with the highest address that is covered by the {@linkplain #STACK_REFERENCE_MAP
     * stack reference map}. This value is set so that it covers all thread locals, and the thread's stack.
     * Once initialized, this value does not change for the lifetime of the thread.
     */
    HIGHEST_STACK_SLOT_ADDRESS(Kind.WORD),

    /**
     * The address of the stack slot with the lowest address that is covered by the {@linkplain #STACK_REFERENCE_MAP
     * stack reference map}. This value is set so that it covers all thread locals. Once initialized, this value does
     * not change for the lifetime of the thread.
     */
    LOWEST_STACK_SLOT_ADDRESS(Kind.WORD),

    /**
     * The address of the active stack slot with the lowest address that is covered by the
     * {@linkplain #STACK_REFERENCE_MAP stack reference map}. This value is set during stack reference map preparation
     * and then used by stack reference map scanning. That is, this value indicates how much of the stack represents
     * live data at any given garbage collection point.
     */
    LOWEST_ACTIVE_STACK_SLOT_ADDRESS(Kind.WORD),

    /**
     * The address of the stack reference map. This reference map has sufficient capacity to store a bit for each
     * word-aligned address in the (inclusive) range {@code [STACK_LIMIT_BOTTOM_FOR_REFERENCE_MAP ..
     * STACK_BOTTOM_FOR_REFERENCE_MAP]}. During any given garbage collection, the first bit in the reference map (i.e.
     * bit 0) denotes the address given by {@code STACK_TOP_FOR_REFERENCE_MAP}.
     */
    STACK_REFERENCE_MAP(Kind.WORD),

    /**
     * Links for a doubly-linked list of all thread locals for active threads.
     */
    FORWARD_LINK(Kind.WORD),
    BACKWARD_LINK(Kind.WORD),

    /**
     * Saving area for integer registers live during a trap.
     * Used by trap handlers (and safepoints which operate as a special trap).
     */
    REGISTERS(Kind.WORD); // must be last, space for saved register values starts here

    private final Kind _kind;

    /**
     * The size of the storage required for a copy of the thread locals defined by this enum (including the registers).
     * Note that it is <b>not</b> simply the number of enum constants multiplied by the size of a word.
     * This value is guaranteed to be word-aligned
     */
    public static final Size THREAD_LOCAL_STORAGE_SIZE;

    public static final IndexedSequence<VmThreadLocal> VALUES = new ArraySequence<VmThreadLocal>(values());

    /**
     * A sequence with an entry for each word in a VM thread locals storage area. The entry at index {@code i} denotes the name for
     * the VM thread local whose {@linkplain #index() index} is also {@code i}. In addition, the entries at the end of the sequence
     * give the names for the saved registers (which do not have a unique {@link VmThreadLocal} constant value).
     */
    public static final IndexedSequence<String> NAMES;

    public static final int NUMBER_OF_INTEGER_REGISTERS;
    public static final int NUMBER_OF_FLOATING_POINT_REGISTERS;

    static {
        NUMBER_OF_INTEGER_REGISTERS = VMConfiguration.target().safepoint().numberOfIntegerRegisters();
        NUMBER_OF_FLOATING_POINT_REGISTERS = VMConfiguration.target().safepoint().numberOfFloatingPointRegisters();
        THREAD_LOCAL_STORAGE_SIZE = Size.fromInt((REGISTERS.index() + NUMBER_OF_INTEGER_REGISTERS) * Word.size() +
                            NUMBER_OF_FLOATING_POINT_REGISTERS * Doubles.SIZE);
        ProgramError.check(REGISTERS.ordinal() == values().length - 1);
        ProgramError.check(THREAD_LOCAL_STORAGE_SIZE.aligned().equals(THREAD_LOCAL_STORAGE_SIZE), "THREAD_LOCAL_STORAGE_SIZE is not word-aligned");

        final String[] names = new String[THREAD_LOCAL_STORAGE_SIZE.toInt() / Word.size()];
        for (VmThreadLocal vmThreadLocal : VALUES) {
            names[vmThreadLocal.index()] = vmThreadLocal.name();
        }
        int registerNameIndex = REGISTERS.index();
        for (int i = 0; i < NUMBER_OF_INTEGER_REGISTERS; ++i) {
            names[registerNameIndex] = "REG" + i;
            registerNameIndex++;
        }
        for (int i = 0; i < NUMBER_OF_FLOATING_POINT_REGISTERS; ++i) {
            names[registerNameIndex] = "FP_REG" + i;
            registerNameIndex++;
        }
        ProgramError.check(names.length == registerNameIndex);
        NAMES = new ArraySequence<String>(names);
    }

    VmThreadLocal(Kind kind) {
        assert kind.size() == Kind.WORD.size();
        _kind = kind;
    }

    @FOLD
    public final int index() {
        return ordinal();
    }

    public final Kind kind() {
        return _kind;
    }

    /**
     * Gets the offset of this variable from the base of a thread locals.
     *
     * @return the offset of this variable from the base of a thread locals
     */
    @FOLD
    public final int offset() {
        return index() * Word.size();
    }

    @INLINE
    public static Pointer fromJniEnv(Pointer jniEnv) {
        return jniEnv.minusWords(JNI_ENV.index());
    }

    /**
     * Gets the address of this thread local variable in a given version of thread local variables.
     *
     * @param vmThreadLocals the base address of the thread local variables
     */
    @INLINE
    public Pointer pointer(Pointer vmThreadLocals) {
        return vmThreadLocals.plusWords(index());
    }


    /**
     * Updates the value of this variable in all three ({@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled},
     * {@linkplain #SAFEPOINTS_TRIGGERED_THREAD_LOCALS triggered} and {@linkplain #SAFEPOINTS_DISABLED_THREAD_LOCALS disabled}) thread local
     * variable storage areas.
     *
     * @param vmThreadLocals a pointer to a copy of the thread locals from which the base of all the
     *            thread local variable storage areas can be obtained
     * @param value the new value for this variable
     */
    @INLINE
    public void setConstantWord(Pointer vmThreadLocals, Word value) {
        vmThreadLocals.getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index()).asPointer().setWord(index(), value);
        vmThreadLocals.getWord(SAFEPOINTS_DISABLED_THREAD_LOCALS.index()).asPointer().setWord(index(), value);
        vmThreadLocals.getWord(SAFEPOINTS_TRIGGERED_THREAD_LOCALS.index()).asPointer().setWord(index(), value);
    }

    /**
     * Updates the value of this variable in all three ({@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled},
     * {@linkplain #SAFEPOINTS_TRIGGERED_THREAD_LOCALS triggered} and {@linkplain #SAFEPOINTS_DISABLED_THREAD_LOCALS disabled}) thread local
     * variable storage areas.
     *
     * @param value the new value for this variable
     */
    @INLINE
    public void setConstantWord(Word value) {
        setConstantWord(VmThread.currentVmThreadLocals(), value);
    }

    /**
     * Gets the value of this variable from a specified copy of thread locals.
     *
     * @param vmThreadLocals a pointer to one of the thread locals from which the value of this
     *            variable should be retrieved
     * @return value the value of this variable in the thread locals denoted by {@code
     *         vmThreadLocals}
     */
    @INLINE
    public Word getConstantWord(Pointer vmThreadLocals) {
        return vmThreadLocals.getWord(index());
    }

    /**
     * Gets the value of this variable from the thread locals denoted by the
     * safepoint {@linkplain Safepoint#latchRegister() latch} register.
     *
     * @return value the value of this variable in the current thread locals
     */
    @INLINE
    public Word getConstantWord() {
        return getConstantWord(VmThread.currentVmThreadLocals());
    }

    /**
     * Updates the value of this variable in all three ({@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled},
     * {@linkplain #SAFEPOINTS_TRIGGERED_THREAD_LOCALS triggered} and {@linkplain #SAFEPOINTS_DISABLED_THREAD_LOCALS disabled}) thread local
     * variable storage areas.
     *
     * @param vmThreadLocals a pointer to a copy of the thread locals from which the base of all the
     *            thread localss can be obtained
     * @param value the new value for this variable
     */
    @INLINE
    public void setConstantReference(Pointer vmThreadLocals, Reference value) {
        vmThreadLocals.getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index()).asPointer().setReference(index(), value);
        vmThreadLocals.getWord(SAFEPOINTS_DISABLED_THREAD_LOCALS.index()).asPointer().setReference(index(), value);
        vmThreadLocals.getWord(SAFEPOINTS_TRIGGERED_THREAD_LOCALS.index()).asPointer().setReference(index(), value);
    }

    /**
     * Gets the value of this variable from a specified copy of thread locals.
     *
     * @param vmThreadLocals a pointer to one of the thread locals from which the value of this
     *            variable should be retrieved
     * @return value the value of this variable in the thread locals denoted by {@code
     *         vmThreadLocals}
     */
    @INLINE
    public Reference getConstantReference(Pointer vmThreadLocals) {
        return vmThreadLocals.getReference(index());
    }

    /**
     * Updates the value of this variable in all three ({@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled},
     * {@linkplain #SAFEPOINTS_TRIGGERED_THREAD_LOCALS triggered} and {@linkplain #SAFEPOINTS_DISABLED_THREAD_LOCALS disabled}) thread local
     * variable storage areas.
     *
     * @param value the new value for this variable
     */
    @INLINE
    public void setConstantReference(Reference value) {
        setConstantReference(VmThread.currentVmThreadLocals(), value);
    }

    /**
     * Gets the value of this variable from the thread locals denoted by the
     * safepoint {@linkplain Safepoint#latchRegister() latch} register.
     *
     * @return value the value of this variable in the current thread locals
     */
    @INLINE
    public Reference getConstantReference() {
        return getConstantReference(VmThread.currentVmThreadLocals());
    }

    @INLINE
    public static VmThread getVmThread(Pointer vmThreadLocals) {
        return UnsafeLoophole.cast(VM_THREAD.getConstantReference(vmThreadLocals));
    }

    /**
     * Updates the value of this variable in the {@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled} thread locals.
     *
     * @param vmThreadLocals a pointer to a copy of the thread locals from which the base of the
     *            safepoints-enabled thread locals can be obtained
     * @param value the new value for this variable
     */
    @INLINE
    public void setVariableWord(Pointer vmThreadLocals, Word value) {
        vmThreadLocals.getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index()).asPointer().setWord(index(), value);
    }

    /**
     * Updates the value of this variable in the {@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled} thread locals.
     *
     * @param value the new value for this variable
     */
    @INLINE
    public void setVariableWord(Word value) {
        setVariableWord(VmThread.currentVmThreadLocals(), value);
    }

    /**
     * Gets the value of this variable from the {@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled} thread locals.
     *
     * @param vmThreadLocals a pointer to a copy of the thread locals from which the base of the
     *            safepoints-enabled thread locals can be obtained
     * @return value the value of this variable in the safepoints-enabled thread locals
     */
    @INLINE
    public Word getVariableWord(Pointer vmThreadLocals) {
        return vmThreadLocals.getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index()).asPointer().getWord(index());
    }

    /**
     * Gets the value of this variable from the {@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled} thread locals.
     *
     * @return value the value of this variable in the safepoints-enabled thread locals
     */
    @INLINE
    public Word getVariableWord() {
        return getVariableWord(VmThread.currentVmThreadLocals());
    }

    /**
     * Updates the value of this variable in the {@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled} thread locals.
     *
     * @param vmThreadLocals a pointer to a copy of the thread locals from which the base of the
     *            safepoints-enabled thread locals can be obtained
     * @param value the new value for this variable
     */
    @INLINE
    public void setVariableReference(Pointer vmThreadLocals, Reference value) {
        vmThreadLocals.getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index()).asPointer().setReference(index(), value);
    }

    /**
     * Gets the value of this variable from the {@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled} thread locals.
     *
     * @param vmThreadLocals a pointer to a copy of the thread locals from which the base of the
     *            safepoints-enabled thread locals can be obtained
     * @return value the value of this variable in the safepoints-enabled thread locals
     */
    @INLINE
    public Reference getVariableReference(Pointer vmThreadLocals) {
        return vmThreadLocals.getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index()).asPointer().getReference(index());
    }

    /**
     * Updates the value of this variable in the {@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled} thread locals.
     *
     * @param value the new value for this variable
     */
    @INLINE
    public void setVariableReference(Reference value) {
        setVariableReference(VmThread.currentVmThreadLocals(), value);
    }

    /**
     * Gets the value of this variable from the {@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled} thread locals.
     *
     * @return value the value of this variable in the safepoints-enabled thread locals
     */
    @INLINE
    public Reference getVariableReference() {
        return getVariableReference(VmThread.currentVmThreadLocals());
    }

    public static boolean inJava(Pointer latch) {
        return LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(latch).isZero();
    }

    // GC support:

    /**
     * Prepares a reference map for the entire stack of a VM thread including all of VM thread locals (which include
     * saved register values iff at a Java safepoint) while the GC has not changed anything yet.
     *
     * @param vmThreadLocals a pointer to the VM thread locals denoting the thread stack whose reference map is to be prepared
     */
    public static void prepareStackReferenceMap(Pointer vmThreadLocals) {
        VmThread.current().stackReferenceMapPreparer().prepareStackReferenceMap(vmThreadLocals);
    }

    /**
     * The purpose of calling this below is to ensure that there is a stop position to be found near the current
     * instruction pointer. Stack reference map
     * {@linkplain TargetMethod#prepareFrameReferenceMap(StackReferenceMapPreparer, Pointer, Pointer, Pointer)
     * preparation} relies on that to be the case - a call constitutes a stop position and {@link NEVER_INLINE} ensures
     * that the compiler generates one.
     */
    @NEVER_INLINE
    private static void fakeNearbyStopPosition() {
    }

    /**
     * ATTENTION: must not use object references in this method, because its frame will be scanned after having returned
     * from it.
     */
    @NEVER_INLINE
    static void prepareCurrentStackReferenceMap() {
        fakeNearbyStopPosition();
        VmThread.current().stackReferenceMapPreparer().prepareStackReferenceMap(VmThread.currentVmThreadLocals(), VMRegister.getInstructionPointer(), VMRegister.getAbiStackPointer(),
                        VMRegister.getAbiFramePointer());
    }

    /**
     * Scan all references on the stack, including the VM thread locals, including stored register values.
     *
     * This assumes that prepareStackReferenceMap() has been run for the same stack and that no mutator execution
     * affecting this stack has occurred in between.
     */
    public static void scanReferences(Pointer vmThreadLocals, BeltWayPointerIndexVisitor wordPointerIndexVisitor, RuntimeMemoryRegion fromSpace, RuntimeMemoryRegion toSpace) {
        final Pointer lastJavaCallerStackPointer = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals).asPointer();
        final Pointer lowestActiveSlot = LOWEST_ACTIVE_STACK_SLOT_ADDRESS.getVariableWord(vmThreadLocals).asPointer();
        final Pointer highestSlot = HIGHEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();
        final Pointer lowestSlot = LOWEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();

        final VmThread vmThread = UnsafeLoophole.cast(VmThread.class, VM_THREAD.getConstantReference(vmThreadLocals));
        if (!(vmThread.javaThread() instanceof StopTheWorldDaemon) && lastJavaCallerStackPointer.lessThan(lowestActiveSlot)) {
            Debug.print("The stack for thread \"");
            Debug.printVmThread(vmThread, false);
            Debug.print("\" has slots between ");
            Debug.print(lastJavaCallerStackPointer);
            Debug.print(" and ");
            Debug.print(lowestActiveSlot);
            Debug.println(" are not covered by the reference map.");
            FatalError.unexpected("Stack reference map does not cover all active slots");
        }

        boolean lockDisabledSafepoints = false;
        if (Heap.traceGC()) {
            lockDisabledSafepoints = Debug.lock(); // Note: This lock basically serializes stack reference map scanning
            Debug.print("Scanning stack reference map for thread ");
            Debug.printVmThread(vmThread, false);
            Debug.println(":");
            Debug.print("  Highest slot: ");
            Debug.println(highestSlot);
            Debug.print("  Lowest active slot: ");
            Debug.println(lowestActiveSlot);
            Debug.print("  Lowest slot: ");
            Debug.println(lowestSlot);
        }

        StackReferenceMapPreparer.scanReferenceMapRange(vmThreadLocals, lowestSlot, vmThreadLocalsEnd(vmThreadLocals), wordPointerIndexVisitor, fromSpace, toSpace);
        StackReferenceMapPreparer.scanReferenceMapRange(vmThreadLocals, lowestActiveSlot, highestSlot, wordPointerIndexVisitor, fromSpace, toSpace);

        if (Heap.traceGC()) {
            Debug.unlock(lockDisabledSafepoints);
        }
    }

    /**
     * Scan all references on the stack, including the VM thread locals, including stored register values.
     *
     * This assumes that prepareStackReferenceMap() has been run for the same stack and that no mutator execution
     * affecting this stack has occurred in between.
     */
    public static void scanReferences(Pointer vmThreadLocals, PointerIndexVisitor wordPointerIndexVisitor) {
        final Pointer lowestActiveSlot = LOWEST_ACTIVE_STACK_SLOT_ADDRESS.getVariableWord(vmThreadLocals).asPointer();
        final Pointer highestSlot = HIGHEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();
        final Pointer lowestSlot = LOWEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();

        boolean lockDisabledSafepoints = false;
        if (Heap.traceGC()) {
            final VmThread vmThread = UnsafeLoophole.cast(VmThread.class, VM_THREAD.getConstantReference(vmThreadLocals));
            lockDisabledSafepoints = Debug.lock(); // Note: This lock basically serializes stack reference map scanning
            Debug.print("Scanning stack reference map for thread ");
            Debug.printVmThread(vmThread, false);
            Debug.println(":");
            Debug.print("  Highest slot: ");
            Debug.println(highestSlot);
            Debug.print("  Lowest active slot: ");
            Debug.println(lowestActiveSlot);
            Debug.print("  Lowest slot: ");
            Debug.println(lowestSlot);
        }

        StackReferenceMapPreparer.scanReferenceMapRange(vmThreadLocals, lowestSlot, vmThreadLocalsEnd(vmThreadLocals), wordPointerIndexVisitor);
        StackReferenceMapPreparer.scanReferenceMapRange(vmThreadLocals, lowestActiveSlot, highestSlot, wordPointerIndexVisitor);

        if (Heap.traceGC()) {
            Debug.unlock(lockDisabledSafepoints);
        }
    }

    public static Pointer vmThreadLocalsEnd(Pointer vmThreadLocals) {
        final Pointer lowestSlot = LOWEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();
        return lowestSlot.plus(THREAD_LOCAL_STORAGE_SIZE.times(3));
    }
}
