/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.thread;

import static com.sun.max.vm.intrinsics.Infopoints.*;
import static com.sun.max.vm.runtime.VMRegister.*;
import static com.sun.max.vm.stack.StackReferenceMapPreparer.*;
import static com.sun.max.vm.thread.VmThread.*;

import java.lang.reflect.*;
import java.util.*;

import com.sun.cri.ci.CiKind.*;
import com.sun.max.annotate.*;
import com.sun.max.config.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.Log.LogPrintStream;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;

/**
 * VM thread local variables and mechanisms for accessing them. The majority of thread locals
 * are defined as static field of this class itself. However, thread locals can also be defined
 * in other {@linkplain VMScheme scheme}-specific classes.
 * <p>
 * All {@link VmThreadLocal} objects must be instantiated before
 * {@link #tlaSize()} or {@link #values()} is called. The recommended way to ensure this is
 * to explicitly reference these instances from the constructor of a {@link BootImagePackage}
 * subclass. A set of {@code registerThreadLocal()} methods are provided in
 * {@link BootImagePackage} to support this.
 * <p>
 * All thread local variables occupy one word and have a constant {@linkplain Nature nature}.
 * <p>
 * All thread locals are in a contiguous block of memory called a thread locals area (TLA) and there
 * are three TLAs per thread, one for each of the {@linkplain SafepointPoll safepoint} states:
 * <dl>
 * <dt>Enabled</dt>
 * <dd>Safepoints for the thread are {@linkplain SafepointPoll#enable() enabled}. The base address of this TLA is
 * obtained by reading the {@link #ETLA} variable from any TLA.</dd>
 * <dt>Disabled</dt>
 * <dd>Safepoints for the thread are {@linkplain SafepointPoll#disable() disabled}. The base address of this TLA is
 * obtained by reading the {@link #DTLA} variable from any TLA.</dd>
 * <dt>Triggered</dt>
 * <dd>Safepoints for the thread are {@linkplain SafepointPoll#isTriggered() triggered}. The base address of
 * this TLA is obtained by reading the {@link #TTLA} variable from any TLA.</dd>
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
 *                    |               TLA (triggered)               |
 *                    +---------------------------------------------+
 *                    |               TLA (enabled)                 |
 *                    +---------------------------------------------+
 *                    |               TLA (disabled)                |
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
 * The thread local block layout for each thread is traced when a thread starts up if
 * the {@link VmThread#TraceThreads -XX:+TraceThreads} VM option is used.
 */
public class VmThreadLocal implements FormatWithToString {

    /**
     * Constants describing the read and write protocol for a thread local.
     *
     */
    public enum Nature {
        /**
         * Denotes a thread local that has it's value maintained only in the {@linkplain VmThreadLocal#ETLA safepoints-enabled} TLA.
         * This should be used for thread locals that need to be written atomically (e.g. {@link VmThreadLocal#MUTATOR_STATE}) and/or in a fast path
         * (e.g. {@link VmThreadLocal#LAST_JAVA_FRAME_ANCHOR} which is written in JNI stubs).
         *
         * Note that the TLA variable used to access this variable must be the ETLA value. This
         * invariant is tested in a {@linkplain MaxineVM#isDebug() debug} VM.
         */
        Single,

        /**
         * Denotes a thread local that has it's value maintained in all three TLAs for a thread.
         * This is used for thread locals that are initialized only once or whose update is not performance critical.
         * These local variables must be updated using one of the {@code store3(...)} methods in {@link VmThreadLocal}.
         */
        Triple;
    }

    @INSPECTED
    private static final List<VmThreadLocal> VALUES = new ArrayList<VmThreadLocal>();

    /**
     * Must be first.
     */
    public static final VmThreadLocal SAFEPOINT_LATCH = new VmThreadLocal("SAFEPOINT_LATCH", false, "memory location loaded by safepoint instruction", Nature.Single);

    /**
     * The {@linkplain VmThread#currentTLA() current} thread local storage when safepoints for the thread are
     * {@linkplain SafepointPoll#enable() enabled}.
     */
    public static final VmThreadLocal ETLA
        = new VmThreadLocal("ETLA", false, "points to TLA used when safepoints enabled");

    /**
     * The {@linkplain VmThread#currentTLA() current} thread local storage when safepoints for the thread are
     * {@linkplain SafepointPoll#disable() disabled}.
     */
    public static final VmThreadLocal DTLA
        = new VmThreadLocal("DTLA", false, "points to TLA used when safepoints disabled");

    /**
     * The {@linkplain VmThread#currentTLA() current} thread local storage when safepoints for the thread are
     * triggered.
     */
    public static final VmThreadLocal TTLA
        = new VmThreadLocal("TTLA", false, "points to TLA used when safepoints triggered");

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
     * The {@link VmOperation} whose {@link VmOperation#doAtSafepoint(Pointer)} is invoked on a thread when it traps at a {@linkplain SafepointPoll safepoint}.
     */
    public static final VmThreadLocal VM_OPERATION
        = new VmThreadLocal("VM_OPERATION", true, "Procedure to run when a safepoint is triggered", Nature.Single);

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
        public void log(LogPrintStream out, Pointer tla, boolean prefixName) {
            super.log(out, tla, prefixName);
            final VmThread vmThread = VmThread.fromTLA(tla);
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
     * The address of the current {@linkplain JavaFrameAnchor Java frame anchor list} for a thread.
     */
    public static final VmThreadLocal LAST_JAVA_FRAME_ANCHOR = new VmThreadLocal("LAST_JAVA_FRAME_ANCHOR", false, "", Nature.Single);

    /**
     * The state of this thread with respect to {@linkplain VmOperation freezing}.
     * This will be one of the {@code THREAD_IN_...} constants defined in {@link VmOperation}.
     */
    public static final VmThreadLocal MUTATOR_STATE = new VmThreadLocal("MUTATOR_STATE", false, "Thread state wrt freezing", Nature.Single);

    /**
     * A boolean denoting whether this thread has been {@linkplain VmOperation frozen}.
     * A non-zero value means true, a zero value means false.
     * This variable is only used when {@link VmOperation#UseCASBasedThreadFreezing} is {@code false}.
     */
    public static final VmThreadLocal FROZEN = new VmThreadLocal("FROZEN", false, "Non-zero if frozen", Nature.Single);

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

    /**
     * Threads allocate primarily via a TLAB, which is refilled by default from a default heap.
     * Occasionally, a thread may need to allocate outside of this allocator.
     * This thread local is used to indicate that a custom allocator is to be used on next TLAB overflow
     * events. These can be provoked artificially to force bypassing TLABs and route execution to the custom allocator.
     * The value stored in the thread local is specific to a heap scheme.
     */
    public static final VmThreadLocal CUSTOM_ALLOCATION_ENABLED
        = new VmThreadLocal("CUSTOM_ALLOCATION_ENABLED", false, "Non-zero to bypass TLAB allocation and use an alternate allocator", Nature.Single);

    /**
     * Set by {@link VmOperation} when a thread is being forced to suspend. This is a bit mask, bit 0
     * is the request to suspend and bit 1 determines whether thread was in Java or native when the
     * request was made.
     */
    public static final VmThreadLocal SUSPEND
        = new VmThreadLocal("SUSPEND", false, "Bitset for thread suspension", Nature.Single);

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
     * The offset of this variable in a TLA.
     */
    public final int offset;

    /**
     * The nature of this variable.
     */
    public final Nature nature;

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
        assert valuesNeedingInitialization != null : "Need to call completeInitialization() first";
        return VALUES;
    }

    /**
     * Gets the array of VM thread locals whose class overrides {@link #initialize()}.
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
     *
     * N.B. javac will not compile the following without explicit package names
     */
    @com.sun.max.annotate.HOSTED_ONLY
    static class InitializationCompleteCallback implements com.sun.max.vm.hosted.JavaPrototype.InitializationCompleteCallback {

        public void initializationComplete() {
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
    }

    static {
        JavaPrototype.registerInitializationCompleteCallback(new InitializationCompleteCallback());
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
    public static Size tlaSize() {
        assert valuesNeedingInitialization != null : "Need to call completeInitialization() first";
        return Size.fromInt(VALUES.size() * Word.size());
    }

    public static boolean inJava(Pointer tla) {
        return JavaFrameAnchor.inJava(JavaFrameAnchor.from(tla));
    }

    // GC support:

    /**
     * Prepares a reference map for the stack of a VM thread executing or blocked in native code.
     *
     * @param tla a pointer to the VM thread locals denoting the thread stack whose reference map is to be prepared
     * @return the amount of time taken to prepare the reference map
     */
    public static long prepareStackReferenceMap(Pointer tla) {
        final VmThread vmThread = VmThread.fromTLA(tla);
        final StackReferenceMapPreparer stackReferenceMapPreparer = vmThread.stackReferenceMapPreparer();
        stackReferenceMapPreparer.prepareStackReferenceMap(tla);
        return stackReferenceMapPreparer.preparationTime();
    }

    /**
     * Prepares a reference map for the entire stack of a VM thread starting from a trap.
     *
     * @param tla a pointer to the VM thread locals denoting the thread stack whose reference map is to be prepared
     * @param trapFrame a pointer to the trap frame
     */
    public static void prepareStackReferenceMapFromTrap(Pointer tla, Pointer trapFrame) {
        VmThread.current().stackReferenceMapPreparer().prepareStackReferenceMapFromTrap(tla, trapFrame);
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
        return VmThread.current().stackReferenceMapPreparer().prepareStackReferenceMap(VmThread.currentTLA(),
                                                                                CodePointer.from(here()),
                                                                                getAbiStackPointer(),
                                                                                getAbiFramePointer(), true);
    }

    /**
     * Scan all references in the VM thread locals.
     *
     * @param tla
     * @param wordPointerIndexVisitor
     */
    private static void scanThreadLocals(Pointer tla, PointerIndexVisitor wordPointerIndexVisitor) {
        Pointer etla = ETLA.load(tla);
        Pointer dtla = DTLA.load(tla);
        Pointer ttla = TTLA.load(tla);

        if (logStackRootScanning()) {
            StackReferenceMapPreparer.stackRootScanLogger.logStartThreadLocals();
        }
        int index = 0;
        long map = REFERENCE_MAP;
        while (map != 0) {
            if ((map & 1) != 0) {
                wordPointerIndexVisitor.visit(etla, index);
                wordPointerIndexVisitor.visit(dtla, index);
                wordPointerIndexVisitor.visit(ttla, index);
                if (logStackRootScanning()) {
                    traceReferenceThreadLocal(etla, index, " (enabled)");
                    traceReferenceThreadLocal(dtla, index, " (disabled)");
                    traceReferenceThreadLocal(ttla, index, " (triggered)");
                }
            }
            index++;
            map = map >>> 1;
        }
    }

    private static void traceReferenceThreadLocal(Pointer tla, int index, String categorySuffix) {
        Pointer address = tla.plus(index * Word.size());
        StackReferenceMapPreparer.stackRootScanLogger.logReferenceThreadLocal(index,
                        address, address.readWord(0), VALUES.get(index).name, categorySuffix);
    }

    /**
     * Scan all references on the stack, including the VM thread locals, including stored register values.
     *
     * This assumes that prepareStackReferenceMap() has been run for the same stack and that no mutator execution
     * affecting this stack has occurred in between.
     */
    public static void scanReferences(Pointer tla, PointerIndexVisitor wordPointerIndexVisitor) {
        final VmThread thread = VmThread.fromTLA(tla);
        boolean isVmOperationThread = thread.isVmOperationThread();

        // Note: as a side effect, this lock serializes stack reference map scanning
        boolean tracing = logStackRootScanning();
        boolean lockDisabledSafepoints = tracing && stackRootScanLogger.lock();

        if (tracing) {
            StackReferenceMapPreparer.stackRootScanLogger.logScanThread(thread);
        }

        // After this call, the thread object may have been forwarded which means
        // that vtable dispatch will no longer work for the object
        scanThreadLocals(tla, wordPointerIndexVisitor);

        VMLog.scanLogs(tla, wordPointerIndexVisitor);

        Pointer anchor = JavaFrameAnchor.from(tla);
        if (!anchor.isZero()) {
            final Pointer lastJavaCallerStackPointer = JavaFrameAnchor.SP.get(anchor);
            final Pointer lowestActiveSlot = LOWEST_ACTIVE_STACK_SLOT_ADDRESS.load(tla);
            final Pointer highestSlot = HIGHEST_STACK_SLOT_ADDRESS.load(tla);
            final Pointer lowestSlot = LOWEST_STACK_SLOT_ADDRESS.load(tla);

            if (!isVmOperationThread && lastJavaCallerStackPointer.lessThan(lowestActiveSlot)) {
                Log.print("The stack has slots between ");
                Log.print(lastJavaCallerStackPointer);
                Log.print(" and ");
                Log.print(lowestActiveSlot);
                Log.println(" are not covered by the reference map.");
                Throw.stackDump("Stack trace for thread:", JavaFrameAnchor.PC.get(anchor), lastJavaCallerStackPointer, JavaFrameAnchor.FP.get(anchor));
                FatalError.unexpected("Stack reference map does not cover all active slots");
            }
            if (tracing) {
                StackReferenceMapPreparer.stackRootScanLogger.logThreadSlotRange(highestSlot, lowestActiveSlot, lowestSlot);
            }
            StackReferenceMapPreparer.scanReferenceMapRange(tla, lowestActiveSlot, highestSlot, wordPointerIndexVisitor);
        } else {
            if (tracing) {
                StackReferenceMapPreparer.stackRootScanLogger.logThreadSlotRange(Pointer.zero(), Pointer.zero(), Pointer.zero());
            }
        }

        if (tracing) {
            Log.unlock(lockDisabledSafepoints);
        }
    }

    @HOSTED_ONLY
    public VmThreadLocal(String name, boolean isReference, String description) {
        this(name, isReference, description, Nature.Triple);
    }

    /**
     * Creates a new thread local variable and adds a slot for it to the map.
     *
     * @param name name of the variable
     * @param isReference specifies if this variable is a reference
     * @param description a very short textual description, useful for debugging
     */
    @HOSTED_ONLY
    public VmThreadLocal(String name, boolean isReference, String description, Nature nature) {
        this.isReference = isReference;
        this.name = name;
        this.nature = nature;
        this.index = VALUES.size();
        this.offset = index * Word.size();
        VALUES.add(this);
        this.description = description;
        this.declaration = findDeclaration(name, new Throwable().getStackTrace());

        if (valuesNeedingInitialization != null) {
            FatalError.unexpected("Cannot register " + name + " after " + VmThreadLocal.class.getSimpleName() + ".completeInitialization() was called.\n" +
                            "Make sure " + BootImagePackage.class.getSimpleName() + ".registerThreadLocal() is called for this thread local in the constructor of the " +
                            "Package class responsible for including " + declaration.getClassName() + " in the image.");
        }
    }

    /**
     * Gets the address of this thread local variable in a given version of thread local variables.
     *
     * @param tla the base address of the thread local variables
     */
    @INLINE
    public final Pointer addressIn(Pointer tla) {
        return tla.plus(offset);
    }

    /**
     * Prints the value of this thread local to a given log stream.
     *
     * @param out the log stream to which the value will be printed
     * @param tla the TLA from which to read the value of this thread local
     * @param prefixName if true, then the name of this thread local plus ": " will be printed before the value
     */
    public void log(LogPrintStream out, Pointer tla, boolean prefixName) {
        if (prefixName) {
            out.print(name);
            out.print(": ");
        }
        if (this == SAFEPOINT_LATCH && TTLA.load(tla).equals(tla)) {
            out.print("<trigger latch>");
        } else {
            out.print(tla.getWord(index));
        }
    }

    /**
     * Performs any initialization required for this thread local at thread startup time.
     * This is called before any heap allocation or object stores are executed on the thread.
     *
     * The initialization logic should not perform any synchronization or heap allocation.
     *
     * The set of VM thread locals that override this method can be obtained via {@link #valuesNeedingInitialization()}.
     */
    public void initialize() {
    }

    /**
     * Stores the value of this {@linkplain Nature#Single single} variable.
     *
     * This operation is a single store.
     *
     * @param etla this must be the value of the {@link #ETLA} thread local
     * @param value the value to store
     */
    @INLINE
    public final void store(Pointer etla, Reference value) {
        if (MaxineVM.isDebug()) {
            if (nature != Nature.Single || !etla.readWord(ETLA.offset).equals(etla)) {
                Log.print("Triple thread local being stored to as a single: ");
                Log.println(name);
                FatalError.unexpected("Triple thread local updated as a single");
            }
        }
        etla.setReference(index, value);
    }

    /**
     * Stores the value of this {@linkplain Nature#Single single} variable.
     *
     * This operation is a single store.
     *
     * @param etla this must be the value of the {@link #ETLA} thread local
     * @param value the value to store
     */
    @INLINE
    public final void store(Pointer etla, Word value) {
        if (MaxineVM.isDebug()) {
            if (nature != Nature.Single || !etla.readWord(ETLA.offset).equals(etla)) {
                Log.print("Triple thread local being stored to as a single: ");
                Log.println(name);
                FatalError.unexpected("Triple thread local updated as a single");
            }
        }
        etla.setWord(index, value);
    }

    @Fold
    private static int tlaOffset(int n) {
        return tlaSize().toInt() * n;
    }

    /**
     * Stores the value of this variable in the TLAs denoted by a given TLA.
     *
     * This operation is composed of 1 load and 3 stores.
     */
    @INLINE
    public final void store3(Pointer tla, Word value) {
        Pointer ttla = TTLA.load(tla);
        ttla.writeWord(offset, value);
        ttla.writeWord(offset + tlaOffset(1), value);
        ttla.writeWord(offset + tlaOffset(2), value);
    }

    /**
     * Stores the value of this variable in the TLAs denoted by the {@linkplain VmThread#currentTLA() current TLA}.
     *
     * This operation is composed of 3 loads and 3 stores.
     */
    @INLINE
    public final void store3(Word value) {
        store3(currentTLA(), value);
    }

    /**
     * Stores the value of this variable in the TLAs denoted by a given TLA.
     *
     * This operation is composed of 1 load and 3 stores.
     */
    @INLINE
    public final void store3(Pointer tla, Reference value) {
        Pointer ttla = TTLA.load(tla);
        ttla.writeReference(offset, value);
        ttla.writeReference(offset + tlaOffset(1), value);
        ttla.writeReference(offset + tlaOffset(2), value);
    }

    /**
     * Stores the value of this variable in the TLAs denoted by the {@linkplain VmThread#currentTLA() current TLA}.
     *
     * This operation is composed of 3 loads and 3 stores.
     */
    @INLINE
    public final void store3(Reference value) {
        store3(currentTLA(), value);
    }

    private void checkLoad(Pointer tla) {
        if (nature == Nature.Single) {
            if (!tla.readWord(ETLA.offset).equals(tla)) {
                Log.print("Single thread local must be loaded from ETLA: ");
                Log.println(name);
                FatalError.unexpected("Single thread local must be loaded from ETLA", false, null, Pointer.zero());
            }
        }
    }

    /**
     * Loads the value of this variable from a given TLA.
     *
     * This operation is a single load.
     *
     * @param tla this must be the value of the {@link #ETLA} thread local if this is a {@linkplain Nature#Single single} thread local
     * @return the value of this variable in {@code tla} as a pointer
     */
    @INLINE
    public final Pointer load(Pointer tla) {
  /*      if (MaxineVM.isDebug()) {
            checkLoad(tla);
        }*/
        return tla.readWord(offset).asPointer();
    }

    /**
     * Loads the value of this variable from a given TLA.
     *
     * This operation is a single load.
     *
     * @param tla this must be the value of the {@link #ETLA} thread local if this is a {@linkplain Nature#Single single} thread local
     * @return the value of this variable in {@code tla} as a reference
     */
    @INLINE
    public final Reference loadRef(Pointer tla) {
        if (MaxineVM.isDebug()) {
            checkLoad(tla);
        }
        return tla.readReference(offset);
    }

    @Override
    public String toString() {
        return name;
    }
}
