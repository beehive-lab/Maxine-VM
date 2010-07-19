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

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.Log.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;

/**
 * VM thread local variables and mechanisms for accessing them. The majority of thread locals
 * are defined as static field of this class itself. However, thread locals can also be defined
 * in other {@linkplain VMScheme scheme}-specific classes.
 *
 * All thread local variables occupy one word and
 * the {@linkplain #SAFEPOINT_LATCH safepoint latch} must be first.
 * <p>
 * All thread locals are in a contiguous block of memory called a thread locals area (TLA) and there
 * are TLAs per thread, one for each of the {@linkplain Safepoint safepoint} states:
 * <dl>
 * <dt>Enabled</dt>
 * <dd>Safepoints for the thread are {@linkplain Safepoint#enable() enabled}. The base address of this TLA is
 * obtained by reading the {@link #SAFEPOINTS_ENABLED_THREAD_LOCALS} variable from any TLA.</dd>
 * <dt>Disabled</dt>
 * <dd>Safepoints for the thread are {@linkplain Safepoint#disable() disabled}. The base address of this TLA is
 * obtained by reading the {@link #SAFEPOINTS_DISABLED_THREAD_LOCALS} variable from any TLA.</dd>
 * <dt>Triggered</dt>
 * <dd>Safepoints for the thread are {@linkplain Safepoint#trigger(Pointer) triggered}. The base address of
 * this TLA is obtained by reading the {@link #SAFEPOINTS_TRIGGERED_THREAD_LOCALS} variable from any TLA.</dd>
 * </dl>
 *
 * The memory for each TLA is within a thread locals block allocated by the native code that starts a thread
 * (see the function 'thread_run' in Native/substrate/threads.c). A thread locals block contains not only
 * the three TLAs but other thread local data such as the stack
 * reference map. The format of the thread locals block is:
 *
 * <pre>
 * (low addresses)
 *
 *   page aligned --> +---------------------------------------------+
 *                    | X X X          unmapped page          X X X |
 *                    | X X X                                 X X X |
 *   page aligned --> +---------------------------------------------+
 *                    |        thread locals area (triggered)       |
 *                    +---------------------------------------------+
 *                    |        thread locals area (enabled)         |
 *                    +---------------------------------------------+
 *                    |        thread locals area (disabled)        |
 *                    +---------------------------------------------+
 *                    |           NativeThreadLocalsStruct          |
 *                    +---------------------------------------------+
 *                    |                                             |
 *                    |               reference map                 |
 *                    |                                             |
 *                    +---------------------------------------------+
 *
 * (high addresses)
 * </pre>
 *
 * The memory for the three TLAs is located on the stack as described in the thread stack layout
 * diagram {@linkplain VmThread here}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 * @author Paul Caprioli
 */
public class VmThreadLocal {

    private static final List<VmThreadLocal> VALUES = new ArrayList<VmThreadLocal>();

    /**
     * Must be first as needed by {@link Safepoint#initializePrimordial(Pointer)}.
     */
    public static final VmThreadLocal SAFEPOINT_LATCH = new VmThreadLocal("SAFEPOINT_LATCH", false, "memory location loaded by safepoint instruction");

    /**
     * The {@linkplain VmThread#currentVmThreadLocals() current} thread local storage when safepoints for the thread are
     * {@linkplain Safepoint#enable() enabled}.
     */
    public static final VmThreadLocal SAFEPOINTS_ENABLED_THREAD_LOCALS
        = new VmThreadLocal("SAFEPOINTS_ENABLED_THREAD_LOCALS", false, "points to TLS used when safepoints enabled");

    /**
     * The {@linkplain VmThread#currentVmThreadLocals() current} thread local storage when safepoints for the thread are
     * {@linkplain Safepoint#disable() disabled}.
     */
    public static final VmThreadLocal SAFEPOINTS_DISABLED_THREAD_LOCALS
        = new VmThreadLocal("SAFEPOINTS_DISABLED_THREAD_LOCALS", false, "points to TLS used when safepoints disabled");

    /**
     * The {@linkplain VmThread#currentVmThreadLocals() current} thread local storage when safepoints for the thread are
     * {@linkplain Safepoint#trigger(Pointer) triggered}.
     */
    public static final VmThreadLocal SAFEPOINTS_TRIGGERED_THREAD_LOCALS
        = new VmThreadLocal("SAFEPOINTS_TRIGGERED_THREAD_LOCALS", false, "points to TLS used when safepoints triggered");

    public static final VmThreadLocal NATIVE_THREAD_LOCALS = new VmThreadLocal("NATIVE_THREAD_LOCALS", false, "pointer to a NativeThreadLocalsStruct");

    /**
     * Next link for a doubly-linked list of all thread locals for active threads.
     */
    public static final VmThreadLocal FORWARD_LINK = new VmThreadLocal("FORWARD_LINK", false, "points to next thread locals in list of all active");

    /**
     * Previous link for a doubly-linked list of all thread locals for active threads.
     */
    public static final VmThreadLocal BACKWARD_LINK = new VmThreadLocal("BACKWARD_LINK", false, "points to previous thread locals in list of all active");

    /**
     * The procedure to run when a safepoint has been triggered.
     */
    public static final VmThreadLocal SAFEPOINT_PROCEDURE
        = new VmThreadLocal("SAFEPOINT_PROCEDURE", true, "Procedure to run when a safepoint is triggered");

    /**
     * Holds the exception object for the exception currently being raised. This value will only be non-null very briefly.
     */
    public static final VmThreadLocal EXCEPTION_OBJECT
        = new VmThreadLocal("EXCEPTION_OBJECT", true, "The exception being raised");

    /**
     * The identifier used to identify the thread in the {@linkplain VmThreadMap thread map}.
     *
     *   0: denotes the primordial thread
     *  >0: denotes a VmThread
     *  <0: denotes a native thread
     *
     * @see VmThread#id()
     */
    public static final VmThreadLocal ID = new VmThreadLocal("ID", false, "Native ID of VM thread holding these locals");

    /**
     * Reference to the {@link VmThread} associated with a set of thread locals.
     */
    public static final VmThreadLocal VM_THREAD = new VmThreadLocal("VM_THREAD", true, "VM thread holding these locals") {
        @Override
        public void log(LogPrintStream out, Pointer vmThreadLocals, boolean prefixName) {
            super.log(out, vmThreadLocals, prefixName);
            final VmThread vmThread = VmThread.fromVmThreadLocals(vmThreadLocals);
            if (vmThread != null) {
                out.print(' ');
                out.printThread(vmThread, false);
            }
        }
    };

    /**
     * The address of the table of {@linkplain NativeInterfaces#jniEnv() JNI functions}.
     */
    public static final VmThreadLocal JNI_ENV = new VmThreadLocal("JNI_ENV", false, "points to table of JNI functions");

    /**
     * The address of the current {@linkplain JavaFrameAnchor anchor list} for a thread.
     */
    public static final VmThreadLocal LAST_JAVA_FRAME_ANCHOR = new VmThreadLocal("LAST_JAVA_FRAME_ANCHOR", false, "");

    /**
     * The state of this thread with respect to GC. This will be one of the {@code THREAD_IN_...} constants defined in {@link Safepoint}.
     */
    public static final VmThreadLocal MUTATOR_STATE = new VmThreadLocal("MUTATOR_STATE", false, "Thread state wrt GC");

    /**
     * A boolean denoting whether a GC is in progress. A non-zero value means true, a zero value means false.
     */
    public static final VmThreadLocal GC_STATE = new VmThreadLocal("GC_STATE", false, "Non-zero if GC in progress");

    /**
     * The number of the trap (i.e. signal) that occurred.
     */
    public static final VmThreadLocal TRAP_NUMBER = new VmThreadLocal("TRAP_NUMBER", false, "Number of the trap (signal) that occurred");

    /**
     * The value of the instruction pointer when the last trap occurred.
     * NOTE: like other trap information, this is ONLY VALID for a short time and should only be
     * used by the C trap handler and the prologue of the trap stub to pass information.
     */
    public static final VmThreadLocal TRAP_INSTRUCTION_POINTER
        = new VmThreadLocal("TRAP_INSTRUCTION_POINTER", false, "IP when last trap occurred; short-lived");

    /**
     * The fault address causing the trap.
     */
    public static final VmThreadLocal TRAP_FAULT_ADDRESS
        = new VmThreadLocal("TRAP_FAULT_ADDRESS", false, "Fault address causing last trap; short-lived");

    /**
     * The value of the latch register when the last trap occurred.
     */
    public static final VmThreadLocal TRAP_LATCH_REGISTER
        = new VmThreadLocal("TRAP_LATCH_REGISTER", false, "Value of latch register at last trap; short-lived");

    /**
     * The address of the stack slot with the highest address that is covered by the {@linkplain #STACK_REFERENCE_MAP
     * stack reference map}. This value is set so that it covers all thread locals, and the thread's stack.
     * Once initialized, this value does not change for the lifetime of the thread.
     */
    public static final VmThreadLocal HIGHEST_STACK_SLOT_ADDRESS
        = new VmThreadLocal("HIGHEST_STACK_SLOT_ADDRESS", false, "points to stack slot with highest address covered by stack reference map");

    /**
     * The address of the stack slot with the lowest address that is covered by the {@linkplain #STACK_REFERENCE_MAP
     * stack reference map}. This value is set so that it covers all thread locals. Once initialized, this value does
     * not change for the lifetime of the thread.
     */
    public static final VmThreadLocal LOWEST_STACK_SLOT_ADDRESS
        = new VmThreadLocal("LOWEST_STACK_SLOT_ADDRESS", false, "points to stack slot with lowest address covered by stack reference map");

    /**
     * The address of the active stack slot with the lowest address that is covered by the
     * {@linkplain #STACK_REFERENCE_MAP stack reference map}. This value is set during stack reference map preparation
     * and then used by stack reference map scanning. That is, this value indicates how much of the stack represents
     * live data at any given garbage collection point. The value is only non-zero for a Java thread that was stopped
     * for GC <b>and</b> has partially prepared its stack reference map. That is, if the GC sees a zero value for a thread
     * that has been stopped (with respect to object graph mutation), then it infers said thread is in native code and
     * needs to have its <b>complete</b> stack reference map prepared on its behalf.
     */
    public static final VmThreadLocal LOWEST_ACTIVE_STACK_SLOT_ADDRESS
        = new VmThreadLocal("LOWEST_ACTIVE_STACK_SLOT_ADDRESS", false, "points to active stack slot with lowest address covered by stack reference map");

    /**
     * The address of the stack reference map. This reference map has sufficient capacity to store a bit for each
     * word-aligned address in the (inclusive) range {@code [LOWEST_STACK_SLOT_ADDRESS .. HIGHEST_STACK_SLOT_ADDRESS]}..
     */
    public static final VmThreadLocal STACK_REFERENCE_MAP
        = new VmThreadLocal("STACK_REFERENCE_MAP", false, "points to stack reference map");

    public static final VmThreadLocal STACK_REFERENCE_MAP_SIZE
        = new VmThreadLocal("STACK_REFERENCE_SIZE", false, "size of stack reference map");

    public static final VmThreadLocal IMMORTAL_ALLOCATION_ENABLED
        = new VmThreadLocal("IMMORTAL_ALLOCATION_ENABLED", false, "Non-zero if thread is allocating on the immortal heap");

    /**
     * Used to pass a method from an {@link InterpreterStub} to the interpreter.
     */
    public static final VmThreadLocal INTERPRETED_METHOD = new VmThreadLocal("INTERPRETED_METHOD", true, "ClassMethodActor being interpreted");

    /**
     * Records the size of the native call stack.
     */
    public static final VmThreadLocal NATIVE_CALL_STACK_SIZE = new VmThreadLocal("NATIVE_CALL_STACK_SIZE", false, "Size of the native call stack");

    private static VmThreadLocal[] valuesNeedingInitialization;

    /**
     * Specifies if this variable is a reference.
     */
    public final boolean isReference;

    /**
     * The name of this variable.
     */
    public final String name;

    /**
     * The index of this variable in the array returned by {@link #values()}.
     */
    public final int index;

    /**
     * The offset of this variable from the base of a thread locals.
     */
    public final int offset;

    /**
     * A very short string describing the variable, useful for debugging.
     */
    public final String description;

    /**
     * The stack trace element describing where this thread local is declared.
     */
    public final StackTraceElement declaration;

    static {
        ProgramError.check(SAFEPOINT_LATCH.index == 0);
        // The C code in trap.c relies on the following relationships:
        ProgramError.check(TRAP_NUMBER.index + 1 == TRAP_INSTRUCTION_POINTER.index);
        ProgramError.check(TRAP_NUMBER.index + 2 == TRAP_FAULT_ADDRESS.index);
        ProgramError.check(TRAP_NUMBER.index + 3 == TRAP_LATCH_REGISTER.index);
    }

    /**
     * Gets the complete set of declared VM thread locals.
     */
    public static List<VmThreadLocal> values() {
        return VALUES;
    }

    /**
     * Gets the array of VM thread locals whose class overrides {@link #initialize(com.sun.max.vm.MaxineVM.Phase)}.
     */
    static VmThreadLocal[] valuesNeedingInitialization() {
        return valuesNeedingInitialization;
    }

    /**
     * A bit map denoting the thread locals that are GC roots. Bit {@code n} is set in
     * this map if the thread local with {@link #index} {@code n} is a reference.
     *
     * If there is ever a reference-type thread local with an index > 63, then an
     * encoding larger than a long will be required.
     */
    private static long REFERENCE_MAP;

    /**
     * Performs various initialization that can only be done once all the VM thread locals have
     * been created and registered with this class.
     */
    @HOSTED_ONLY
    public static void completeInitialization() {
        assert valuesNeedingInitialization == null : "Cannot call completeInitialization() more than once";
        try {
            final List<VmThreadLocal> valuesNeedingInitialization = new ArrayList<VmThreadLocal>();
            final Method emptyInitializeMethod = VmThreadLocal.class.getMethod("initialize");
            for (VmThreadLocal value : VALUES) {
                if (!emptyInitializeMethod.equals(value.getClass().getMethod("initialize"))) {
                    valuesNeedingInitialization.add(value);
                }
                if (value.isReference) {
                    assert value.index <= 63 : "Need larger reference map for thread locals";
                    REFERENCE_MAP |= 1L << value.index;
                }
            }
            VmThreadLocal.valuesNeedingInitialization = valuesNeedingInitialization.toArray(new VmThreadLocal[valuesNeedingInitialization.size()]);
        } catch (NoSuchMethodException e) {
            throw ProgramError.unexpected(e);
        }
    }

    @HOSTED_ONLY
    private static StackTraceElement findDeclaration(String name, StackTraceElement[] stackTraceElements) {
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            if (!stackTraceElement.getMethodName().equals("<init>")) {
                ProgramWarning.check(stackTraceElement.getMethodName().equals("<clinit>"), "VM thread local " + name + " should be created in a static field initializer, not at " + stackTraceElement);
                return stackTraceElement;
            }
        }
        throw ProgramError.unexpected("Could not find non-constructor call in stack trace of a call to VmThreadLocal constructor");
    }

    /**
     * Gets the size of a {@linkplain VmThreadLocal thread locals area}.
     * This value is guaranteed to be word-aligned
     */
    public static Size threadLocalsAreaSize() {
        return Size.fromInt(VALUES.size() * Word.size());
    }

    @INLINE
    public static Pointer fromJniEnv(Pointer jniEnv) {
        return jniEnv.minusWords(JNI_ENV.index);
    }

    public static boolean inJava(Pointer vmThreadLocals) {
        return JavaFrameAnchor.inJava(JavaFrameAnchor.from(vmThreadLocals));
    }

    // GC support:

    /**
     * Prepares a reference map for the stack of a VM thread executing or blocked in native code.
     *
     * @param vmThreadLocals a pointer to the VM thread locals denoting the thread stack whose reference map is to be prepared
     * @return the amount of time taken to prepare the reference map
     */
    public static long prepareStackReferenceMap(Pointer vmThreadLocals) {
        final VmThread vmThread = VmThread.fromVmThreadLocals(vmThreadLocals);
        final StackReferenceMapPreparer stackReferenceMapPreparer = vmThread.stackReferenceMapPreparer();
        stackReferenceMapPreparer.prepareStackReferenceMap(vmThreadLocals);
        return stackReferenceMapPreparer.preparationTime();
    }

    /**
     * Prepares a reference map for the entire stack of a VM thread starting from a trap.
     *
     * @param vmThreadLocals a pointer to the VM thread locals denoting the thread stack whose reference map is to be prepared
     * @param trapState a pointer to the trap state
     */
    public static void prepareStackReferenceMapFromTrap(Pointer vmThreadLocals, Pointer trapState) {
        VmThread.current().stackReferenceMapPreparer().prepareStackReferenceMapFromTrap(vmThreadLocals, trapState);
    }

    /**
     * Prepares the stack reference map for the current thread. The prepared map ignores the frame of this
     * particular method as it will not be at a stop from the stack reference map preparer's perspective.
     * Due to the {@link NEVER_INLINE} annotation, this frame will be dead when the caller (i.e.
     * the GC thread) calls the scheme-specific collector.
     *
     * @return the amount of time taken to prepare the reference map
     */
    @NEVER_INLINE
    public static long prepareCurrentStackReferenceMap() {
        return VmThread.current().stackReferenceMapPreparer().prepareStackReferenceMap(VmThread.currentVmThreadLocals(),
                                                                                VMRegister.getInstructionPointer(),
                                                                                VMRegister.getAbiStackPointer(),
                                                                                VMRegister.getAbiFramePointer(), true);
    }

    /**
     * Scan all references in the VM thread locals.
     *
     * @param threadLocals
     * @param wordPointerIndexVisitor
     */
    private static void scanThreadLocals(Pointer threadLocals, PointerIndexVisitor wordPointerIndexVisitor) {
        Pointer enabledThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(threadLocals).asPointer();
        Pointer disabledThreadLocals = SAFEPOINTS_DISABLED_THREAD_LOCALS.getConstantWord(threadLocals).asPointer();
        Pointer triggeredThreadLocals = SAFEPOINTS_TRIGGERED_THREAD_LOCALS.getConstantWord(threadLocals).asPointer();

        if (Heap.traceRootScanning()) {
            Log.println("  Thread locals:");
        }
        int index = 0;
        long map = REFERENCE_MAP;
        while (map != 0) {
            if ((map & 1) != 0) {
                wordPointerIndexVisitor.visit(enabledThreadLocals, index);
                wordPointerIndexVisitor.visit(disabledThreadLocals, index);
                wordPointerIndexVisitor.visit(triggeredThreadLocals, index);
                if (Heap.traceRootScanning()) {
                    traceReferenceThreadLocal(enabledThreadLocals, index, " (enabled)");
                    traceReferenceThreadLocal(disabledThreadLocals, index, " (disabled)");
                    traceReferenceThreadLocal(triggeredThreadLocals, index, " (triggered)");
                }
            }
            index++;
            map = map >>> 1;
        }
    }

    private static void traceReferenceThreadLocal(Pointer vmThreadLocals, int index, String categorySuffix) {
        Log.print("    index=");
        Log.print(index);
        Log.print(", address=");
        Pointer address = vmThreadLocals.plus(index * Word.size());
        Log.print(address);
        Log.print(", value=");
        Log.print(address.readWord(0));
        Log.print(", name=");
        Log.print(values().get(index).name);
        Log.println(categorySuffix);
    }

    /**
     * Scan all references on the stack, including the VM thread locals, including stored register values.
     *
     * This assumes that prepareStackReferenceMap() has been run for the same stack and that no mutator execution
     * affecting this stack has occurred in between.
     */
    public static void scanReferences(Pointer vmThreadLocals, PointerIndexVisitor wordPointerIndexVisitor) {
        final VmThread thread = VmThread.fromVmThreadLocals(vmThreadLocals);
        boolean isGCThread = thread.isGCThread();

        // Note: as a side effect, this lock serializes stack reference map scanning
        boolean lockDisabledSafepoints = Heap.traceRootScanning() && Log.lock();

        if (Heap.traceRootScanning()) {
            Log.print("Scanning thread locals and stack for thread ");
            Log.printThread(thread, false);
            Log.println(":");
        }

        // After this call, the thread object may have been forwarded which means
        // that vtable dispatch will no longer work for the object
        scanThreadLocals(vmThreadLocals, wordPointerIndexVisitor);

        Pointer anchor = JavaFrameAnchor.from(vmThreadLocals);
        if (!anchor.isZero()) {
            final Pointer lastJavaCallerStackPointer = JavaFrameAnchor.SP.get(anchor);
            final Pointer lowestActiveSlot = LOWEST_ACTIVE_STACK_SLOT_ADDRESS.getVariableWord(vmThreadLocals).asPointer();
            final Pointer highestSlot = HIGHEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();
            final Pointer lowestSlot = LOWEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();

            if (!isGCThread && lastJavaCallerStackPointer.lessThan(lowestActiveSlot)) {
                Log.print("The stack has slots between ");
                Log.print(lastJavaCallerStackPointer);
                Log.print(" and ");
                Log.print(lowestActiveSlot);
                Log.println(" are not covered by the reference map.");
                Throw.stackDump("Stack trace for thread:", JavaFrameAnchor.PC.get(anchor), lastJavaCallerStackPointer, JavaFrameAnchor.FP.get(anchor));
                FatalError.unexpected("Stack reference map does not cover all active slots");
            }
            if (Heap.traceRootScanning()) {
                Log.print("  Highest slot: ");
                Log.println(highestSlot);
                Log.print("  Lowest active slot: ");
                Log.println(lowestActiveSlot);
                Log.print("  Lowest slot: ");
                Log.println(lowestSlot);
            }
            StackReferenceMapPreparer.scanReferenceMapRange(vmThreadLocals, lowestActiveSlot, highestSlot, wordPointerIndexVisitor);
        } else {
            if (Heap.traceRootScanning()) {
                Log.println("No Java stack frames");
            }
        }

        if (Heap.traceRootScanning()) {
            Log.unlock(lockDisabledSafepoints);
        }
    }

    /**
     * Creates a new thread local variable and adds a slot for it to the map.
     *
     * @param name name of the variable
     * @param isReference specifies if this variable is a reference
     * @param description a very short textual description, useful for debugging
     */
    @HOSTED_ONLY
    public VmThreadLocal(String name, boolean isReference, String description) {
        this.isReference = isReference;
        this.name = name;
        this.index = VALUES.size();
        this.offset = index * Word.size();
        VALUES.add(this);
        this.description = description;
        this.declaration = findDeclaration(name, new Throwable().getStackTrace());
    }

    /**
     * Gets the address of this thread local variable in a given version of thread local variables.
     *
     * @param vmThreadLocals the base address of the thread local variables
     */
    @INLINE
    public final Pointer pointer(Pointer vmThreadLocals) {
        return vmThreadLocals.plusWords(index);
    }

    /**
     * Prints the value of this thread local to a given log stream.
     *
     * @param out the log stream to which the value will be printed
     * @param vmThreadLocals the TLA from which to read the value of this thread local
     * @param prefixName if true, then the name of this thread local plus ": " will be printed before the value
     */
    public void log(LogPrintStream out, Pointer vmThreadLocals, boolean prefixName) {
        if (prefixName) {
            out.print(name);
            out.print(": ");
        }
        if (this == SAFEPOINT_LATCH && SAFEPOINTS_TRIGGERED_THREAD_LOCALS.getConstantWord(vmThreadLocals).equals(vmThreadLocals)) {
            out.print("<trigger latch>");
        } else {
            out.print(vmThreadLocals.getWord(index));
        }
    }

    /**
     * Performs any initialization required for this thread local at thread startup time.
     * This is called before any heap allocation or object stores are executed on the thread.
     *
     * The initialization logic should not perform any synchronization or heap allocation.
     *
     * The set of VM thread locals that override this method can be obtained via {@link #valuesNeedingInitialization()}.
     *
     * Note: this method is accessed via reflection in {@link #completeInitialization()}.
     */
    public void initialize() {
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
    public final void setConstantWord(Pointer vmThreadLocals, Word value) {
        vmThreadLocals.getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer().setWord(index, value);
        vmThreadLocals.getWord(SAFEPOINTS_DISABLED_THREAD_LOCALS.index).asPointer().setWord(index, value);
        vmThreadLocals.getWord(SAFEPOINTS_TRIGGERED_THREAD_LOCALS.index).asPointer().setWord(index, value);
    }

    /**
     * Updates the value of this variable in all three ({@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled},
     * {@linkplain #SAFEPOINTS_TRIGGERED_THREAD_LOCALS triggered} and {@linkplain #SAFEPOINTS_DISABLED_THREAD_LOCALS disabled}) thread local
     * variable storage areas.
     *
     * @param value the new value for this variable
     */
    @INLINE
    public final void setConstantWord(Word value) {
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
    public final Word getConstantWord(Pointer vmThreadLocals) {
        return vmThreadLocals.getWord(index);
    }

    /**
     * Gets the value of this variable from the thread locals denoted by the
     * safepoint {@linkplain Safepoint#latchRegister() latch} register.
     *
     * @return value the value of this variable in the current thread locals
     */
    @INLINE
    public final Word getConstantWord() {
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
    public final void setConstantReference(Pointer vmThreadLocals, Reference value) {
        vmThreadLocals.getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer().setReference(index, value);
        vmThreadLocals.getWord(SAFEPOINTS_DISABLED_THREAD_LOCALS.index).asPointer().setReference(index, value);
        vmThreadLocals.getWord(SAFEPOINTS_TRIGGERED_THREAD_LOCALS.index).asPointer().setReference(index, value);
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
    public final Reference getConstantReference(Pointer vmThreadLocals) {
        return vmThreadLocals.getReference(index);
    }

    /**
     * Updates the value of this variable in all three ({@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled},
     * {@linkplain #SAFEPOINTS_TRIGGERED_THREAD_LOCALS triggered} and {@linkplain #SAFEPOINTS_DISABLED_THREAD_LOCALS disabled}) thread local
     * variable storage areas.
     *
     * @param value the new value for this variable
     */
    @INLINE
    public final void setConstantReference(Reference value) {
        setConstantReference(VmThread.currentVmThreadLocals(), value);
    }

    /**
     * Gets the value of this variable from the thread locals denoted by the
     * safepoint {@linkplain Safepoint#latchRegister() latch} register.
     *
     * @return value the value of this variable in the current thread locals
     */
    @INLINE
    public final Reference getConstantReference() {
        return getConstantReference(VmThread.currentVmThreadLocals());
    }

    /**
     * Updates the value of this variable in the {@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled} thread locals.
     *
     * @param vmThreadLocals a pointer to a copy of the thread locals from which the base of the
     *            safepoints-enabled thread locals can be obtained
     * @param value the new value for this variable
     */
    @INLINE
    public final void setVariableWord(Pointer vmThreadLocals, Word value) {
        vmThreadLocals.getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer().setWord(index, value);
    }

    /**
     * Updates the value of this variable in the {@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled} thread locals.
     *
     * @param value the new value for this variable
     */
    @INLINE
    public final void setVariableWord(Word value) {
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
    public final Word getVariableWord(Pointer vmThreadLocals) {
        return vmThreadLocals.getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer().getWord(index);
    }

    /**
     * Gets the value of this variable from the {@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled} thread locals.
     *
     * @return value the value of this variable in the safepoints-enabled thread locals
     */
    @INLINE
    public final Word getVariableWord() {
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
    public final void setVariableReference(Pointer vmThreadLocals, Reference value) {
        vmThreadLocals.getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer().setReference(index, value);
    }

    /**
     * Gets the value of this variable from the {@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled} thread locals.
     *
     * @param vmThreadLocals a pointer to a copy of the thread locals from which the base of the
     *            safepoints-enabled thread locals can be obtained
     * @return value the value of this variable in the safepoints-enabled thread locals
     */
    @INLINE
    public final Reference getVariableReference(Pointer vmThreadLocals) {
        return vmThreadLocals.getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer().getReference(index);
    }

    /**
     * Updates the value of this variable in the {@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled} thread locals.
     *
     * @param value the new value for this variable
     */
    @INLINE
    public final void setVariableReference(Reference value) {
        setVariableReference(VmThread.currentVmThreadLocals(), value);
    }

    /**
     * Gets the value of this variable from the {@linkplain #SAFEPOINTS_ENABLED_THREAD_LOCALS safepoints-enabled} thread locals.
     *
     * @return value the value of this variable in the safepoints-enabled thread locals
     */
    @INLINE
    public final Reference getVariableReference() {
        return getVariableReference(VmThread.currentVmThreadLocals());
    }

    @Override
    public String toString() {
        return name;
    }
}
