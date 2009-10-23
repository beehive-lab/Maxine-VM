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

import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.actor.member.InjectedReferenceFieldActor.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.lang.reflect.*;

import sun.misc.*;

import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.refmaps.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.type.*;

/**
 * The MaxineVM VM specific implementation of threads.
 *
 * A thread's stack layout is as follows:
 *
 * <pre>
 * High addresses
 *
 *                       +---------------------------------------------+ <-- highestSlotAddress
 *                       |          OS thread specific data            |
 *                       |           and native frames                 |
 *                       +---------------------------------------------+
 *                       |                                             |
 *                       |           Frames of Java methods,           |
 *     stack pointer --> |              native stubs, and              |
 *                       |              native functions               | <-- lowestActiveSlotAddress
 *                       |                                             |
 *                       +---------------------------------------------+
 *                       | X X X     Stack overflow detection    X X X |
 *                       | X X X          (yellow zone)          X X X |
 *      page aligned --> +---------------------------------------------+ <-- stackYellowZone
 *                       | X X X     Stack overflow detection    X X X |
 *                       | X X X           (red zone)            X X X |
 *      page aligned --> +---------------------------------------------+ <-- stackRedZone
 *                       |                                             |
 *                       |               reference map area            |
 *                       |                                             |
 *                       +---------------------------------------------+  <-- referenceMap
 *                       |               Java frame anchor             |
 *                       +---------------------------------------------+  <-- initial Java frame anchor
 *                       |           thread locals (disabled)          |
 *                       +---------------------------------------------+  <-- disabledThreadLocals
 *                       |           thread locals (enabled)           |
 *                       +---------------------------------------------+  <-- enabledThreadLocals
 *                       |           thread locals (triggered)         |
 *      page aligned --> +---------------------------------------------+
 *                       | X X X          unmapped page          X X X |  <-- triggeredThreadLocals / lowestSlotAddress
 *                       | X X X                                 X X X |
 *      page aligned --> +---------------------------------------------+
 *
 * Low addresses
 * </pre>
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public class VmThread {

    /**
     * The signature of {@link #run(int, Address, Pointer, Pointer, Pointer, Pointer, Pointer, Pointer, Pointer, Pointer)}.
     */
    public static final SignatureDescriptor RUN_METHOD_SIGNATURE;

    static {
        Method runMethod = null;
        for (Method method : VmThread.class.getDeclaredMethods()) {
            if (method.getName().equals("run")) {
                ProgramError.check(runMethod == null, "There must only be one method named \"run\" in " + MaxineVM.class);
                runMethod = method;
            }
        }
        RUN_METHOD_SIGNATURE = SignatureDescriptor.create(runMethod.getReturnType(), runMethod.getParameterTypes());
    }

    private static final VMBooleanXXOption traceThreadsOption = register(new VMBooleanXXOption("-XX:-TraceThreads", "Trace thread management activity for debugging purposes."), MaxineVM.Phase.PRISTINE);

    private static final Size DEFAULT_STACK_SIZE = Size.K.times(256);

    private static final VMSizeOption stackSizeOption = register(new VMSizeOption("-Xss", DEFAULT_STACK_SIZE, "Stack size of new threads."), MaxineVM.Phase.PRISTINE);

    @INLINE
    public static boolean traceThreads() {
        return traceThreadsOption.getValue();
    }


    @CONSTANT_WHEN_NOT_ZERO
    private Thread javaThread;
    private volatile Thread.State state = Thread.State.NEW;
    private volatile boolean interrupted = false;
    private Throwable terminationCause;
    private int id;
    private int parkState;

    private Address guardPage = Address.zero();

    @CONSTANT
    private Pointer vmThreadLocals = Pointer.zero();

    @INSPECTED
    private String name;

    @CONSTANT
    protected Word nativeThread = Word.zero();

    private final VmStackFrameWalker stackFrameWalker = new VmStackFrameWalker(Pointer.zero());

    /**
     * Gets a preallocated, thread local object that can be used to walk the frames in this thread's stack.
     *
     * <b>This must only be used when {@linkplain Throw#raise(Throwable, com.sun.max.unsafe.Pointer, com.sun.max.unsafe.Pointer, com.sun.max.unsafe.Pointer)} throwing an exception}
     * or {@linkplain StackReferenceMapPreparer preparing} a stack reference map.
     * Allocation must not occur in these contexts.</b>
     */
    public VmStackFrameWalker unwindingOrReferenceMapPreparingStackFrameWalker() {
        FatalError.check(stackFrameWalker != null, "Thread-local stack frame walker cannot be null for a running thread");
        return stackFrameWalker;
    }

    private final VmStackFrameWalker stackDumpStackFrameWalker = new VmStackFrameWalker(Pointer.zero());

    /**
     * Gets a preallocated, thread local object that can be used to log a stack dump without incurring any allocation.
     */
    public VmStackFrameWalker stackDumpStackFrameWalker() {
        FatalError.check(stackDumpStackFrameWalker != null, "Thread-local stack frame walker cannot be null for a running thread");
        return stackDumpStackFrameWalker;
    }

    private final StackReferenceMapPreparer stackReferenceMapPreparer = new StackReferenceMapPreparer(this);

    /**
     * Gets the thread-local object used to prepare the reference map for this stack's thread during garbage collection.
     */
    public StackReferenceMapPreparer stackReferenceMapPreparer() {
        return stackReferenceMapPreparer;
    }

    private final CompactReferenceMapInterpreter compactReferenceMapInterpreter = new CompactReferenceMapInterpreter();

    @HOSTED_ONLY
    private static final ThreadLocal<CompactReferenceMapInterpreter> hostedCompactReferenceMapInterpreter = new ThreadLocal<CompactReferenceMapInterpreter>() {

        @Override
        protected CompactReferenceMapInterpreter initialValue() {
            return new CompactReferenceMapInterpreter();
        }
    };

    public CompactReferenceMapInterpreter compactReferenceMapInterpreter() {
        if (MaxineVM.isHosted()) {
            return hostedCompactReferenceMapInterpreter.get();
        }
        return compactReferenceMapInterpreter;
    }

    public Thread.State state() {
        return state;
    }

    public void setState(Thread.State state) {
        this.state = state;
    }


    public final Thread javaThread() {
        return javaThread;
    }

    public static VmThread fromJava(Thread javaThread) {
        return (VmThread) TupleAccess.readObject(javaThread, Thread_vmThread.offset());
    }

    public Word nativeThread() {
        return nativeThread;
    }

    /**
     * Gets the identifier used to identify this thread in the {@linkplain VmThreadMap thread map}.
     * A thread that has not been added to the thread map, will have an identifier of 0 and
     * a thread that has been terminated and removed from the map will have an identifier
     * of -1.
     *
     * This value is identical to the {@link VmThreadLocal#ID} value of a running thread.
     */
    public int id() {
        return id;
    }

    /**
     * @see #id()
     */
    void setID(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private JavaMonitor protectedMonitor;

    @INLINE
    public final JavaMonitor protectedMonitor() {
        return protectedMonitor;
    }

    @INLINE
    public final void setProtectedMonitor(JavaMonitor protectedMonitor) {
        this.protectedMonitor = protectedMonitor;
    }


    private ConditionVariable waitingCondition;

    @INLINE
    public final ConditionVariable waitingCondition() {
        return waitingCondition;
    }

    /**
     * A link in a list of threads waiting on a monitor. If this field points to this thread, then the thread is not on
     * a list. If it is {@code null}, then this thread is at the end of a list. A thread can be on at most one list.
     *
     * @see StandardJavaMonitor#monitorWait(long)
     * @see StandardJavaMonitor#monitorNotify(boolean)
     */
    private VmThread nextWaitingThread = this;

    @INLINE
    public final VmThread nextWaitingThread() {
        return nextWaitingThread;
    }

    @INLINE
    public final void setNextWaitingThread(VmThread nextWaitingThread) {
        this.nextWaitingThread = nextWaitingThread;
    }

    public void setInterrupted() {
        this.interrupted = true;
    }

    /**
     * The pool of JNI local references allocated for this thread.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private JniHandles jniHandles;

    @CONSTANT_WHEN_NOT_ZERO
    private boolean isGCThread;

    /**
     * Determines if this thread is owned by the garbage collector.
     */
    public final boolean isGCThread() {
        return isGCThread;
    }

    /**
     * Create an unbound VmThread that will be bound later.
     */
    public VmThread() {
    }

    /**
     * Create a bound VmThread.
     * @param javaThread
     */
    public VmThread(Thread javaThread) {
        setJavaThread(javaThread);
    }

    /**
     * Bind the given {@code Thread} to this VmThread.
     * @param javaThread thread to be bound
     */
    public VmThread setJavaThread(Thread javaThread) {
        this.isGCThread = Heap.isGcThread(javaThread);
        this.waitingCondition = ConditionVariableFactory.create();
        this.javaThread = javaThread;
        this.name = javaThread.getName();
        this.jniHandles = new JniHandles();
        return this;
    }

    protected void beTerminated() {
        synchronized (javaThread) {
            // Must set TERMINATED before the notify in case a joiner is already waiting
            // @see Thread.join()
            state = Thread.State.TERMINATED;
            javaThread.notifyAll();
        }
        terminationComplete();
        // It is the monitor scheme's responsibility to ensure that this thread isn't reset to RUNNABLE if it blocks
        // here.
        VmThreadMap.ACTIVE.removeVmThreadLocals(vmThreadLocals);
        // Monitor acquisition after point this MUST NOT HAPPEN as it may reset state to RUNNABLE
        nativeThread = Address.zero();
        vmThreadLocals = Pointer.zero();
        id = -1;
        waitingCondition = null;
    }

    /**
     * This happens during bootstrapping. Then, 'Thread.currentThread()' refers to the "main" thread of the host VM. Since
     * there is no 'Thread' constructor that we could call without a valid parent thread, we hereby clone the host VM's
     * main thread.
     */
    @HOSTED_ONLY
    private static VmThread createMain() {
        final Thread thread = HostObjectAccess.mainThread();
        final VmThread vmThread = VmThreadFactory.create(thread);
        VmThreadMap.ACTIVE.addMainVmThread(vmThread);
        return vmThread;
    }

    private static final VmThread mainVMThread = createMain();

    public static VmThread main() {
        return mainVMThread;
    }

    @HOSTED_ONLY
    public static Size stackSize() {
        return DEFAULT_STACK_SIZE;
    }

    @C_FUNCTION
    protected static native Word nativeThreadCreate(int id, Size stackSize, int priority);

    /**
     * Initializes the VM thread system and starts the main Java thread.
     */
    public static void createAndRunMainThread() {
        final Size requestedStackSize = stackSizeOption.getValue().aligned(Platform.host().pageSize).asSize();

        final Word nativeThread = nativeThreadCreate(mainVMThread.id, requestedStackSize, Thread.NORM_PRIORITY);
        if (nativeThread.isZero()) {
            FatalError.unexpected("Could not start main native thread.");
        } else {
            nonJniNativeJoin(nativeThread);
        }
        // Drop back to PRIMORDIAL because we are now in the primordial thread
        MaxineVM vm = MaxineVM.host();
        vm.phase = MaxineVM.Phase.PRIMORDIAL;
    }

    /**
     * Gets the address of the {@linkplain VmThreadLocal thread local variable} storage area pointed to by the
     * safepoint {@linkplain Safepoint#latchRegister() latch} register.
     */
    @INLINE
    public static Pointer currentVmThreadLocals() {
        return Safepoint.getLatchRegister();
    }

    /**
     * Gets a pointer to the JNI environment data structure for the current thread.
     *
     * @return a value of C type JNIEnv*
     */
    public static Pointer currentJniEnvironmentPointer() {
        if (MaxineVM.isHosted()) {
            return Pointer.zero();
        }
        return JNI_ENV.pointer(currentVmThreadLocals());
    }

    @INLINE
    public static VmThread current() {
        if (MaxineVM.isHosted()) {
            return mainVMThread;
        }
        return UnsafeCast.asVmThread(VM_THREAD.getConstantReference().toJava());
    }

    private static void executeRunnable(VmThread vmThread) throws Throwable {
        try {
            if (vmThread == mainVMThread) {
                VMConfiguration.hostOrTarget().runScheme().run();
            } else {
                vmThread.javaThread.run();
            }
        } finally {
            // 'stop0()' support.
            if (vmThread.terminationCause != null) {
                // We arrive here because an uncatchable non-Throwable object has been propagated as an exception.
                throw vmThread.terminationCause;
            }
        }
    }

    /**
     * The entry point for the native thread startup code.
     *
     * ATTENTION: this signature must match 'VMThreadRunMethod' in "Native/substrate/threads.c".
     *
     * @param id the unique identifier assigned to this thread when it was {@linkplain #start0() started}. This
     *            identifier is only bound to this thread until it is {@linkplain #beTerminated() terminated}. That is,
     *            only active threads have unique identifiers.
     * @param nativeThread a handle to the native thread data structure (e.g. a pthread_t value)
     * @param enabledVmThreadLocals the address of the VM thread locals in effect when safepoints are
     *            {@linkplain Safepoint#enable() enabled} for this thread
     * @param disabledVmThreadLocals the address of the VM thread locals in effect when safepoints are
     *            {@linkplain Safepoint#disable() disabled} for this thread
     * @param triggeredVmThreadLocals the address of the VM thread locals in effect when safepoints are
     *            {@linkplain Safepoint#trigger(Pointer) triggered} for this thread
     */
    @C_FUNCTION
    private static void run(int id, Address nativeThread,
                    Pointer stackBase,
                    Pointer triggeredVmThreadLocals,
                    Pointer enabledVmThreadLocals,
                    Pointer disabledVmThreadLocals,
                    Pointer refMapArea,
                    Pointer stackRedZone,
                    Pointer stackYellowZone,
                    Pointer stackEnd) {
        // Disable safepoints:
        Safepoint.setLatchRegister(disabledVmThreadLocals);

        JNI_ENV.setConstantWord(enabledVmThreadLocals, JniNativeInterface.jniEnv());

        // Add the VM thread locals to the active map
        final VmThread vmThread = VmThreadMap.ACTIVE.addVmThreadLocals(id, enabledVmThreadLocals);

        for (VmThreadLocal threadLocal : VmThreadLocal.valuesNeedingInitialization()) {
            threadLocal.initialize();
        }

        vmThread.nativeThread = nativeThread;
        vmThread.vmThreadLocals = enabledVmThreadLocals;
        vmThread.stackFrameWalker.setVmThreadLocals(enabledVmThreadLocals);
        vmThread.stackDumpStackFrameWalker.setVmThreadLocals(enabledVmThreadLocals);

        HIGHEST_STACK_SLOT_ADDRESS.setConstantWord(triggeredVmThreadLocals, stackEnd);
        LOWEST_STACK_SLOT_ADDRESS.setConstantWord(triggeredVmThreadLocals, triggeredVmThreadLocals.plus(Word.size()));
        STACK_REFERENCE_MAP.setConstantWord(triggeredVmThreadLocals, refMapArea);

        vmThread.guardPage = stackYellowZone;

        vmThread.initializationComplete();

        // Enable safepoints:
        Safepoint.enable();

        vmThread.traceThreadAfterInitialization(stackBase, enabledVmThreadLocals);

        try {
            executeRunnable(vmThread);
        } catch (Throwable throwable) {

            vmThread.traceThreadForUncaughtException(throwable);

            final Thread javaThread = vmThread.javaThread();
            // Uncaught exception should be passed by the VM to the uncaught exception handler defined for the thread.
            // Exception thrown by this one should be ignore by the VM.
            try {
                javaThread.getUncaughtExceptionHandler().uncaughtException(javaThread, throwable);
            } catch (Throwable ignoreMe) {
            }
            vmThread.terminationCause = throwable;
        } finally {
            // If this is the main thread terminating, initiate shutdown hooks after waiting for other non-daemons to terminate
            if (vmThread == mainVMThread) {
                VmThreadMap.ACTIVE.joinAllNonDaemons();
                invokeShutdownHooks();
            }
            vmThread.beTerminated();

            vmThread.traceThreadAfterTermination();
        }
    }

    private void traceThreadAfterInitialization(Pointer stackBase, Pointer vmThreadLocals) {
        if (traceThreads()) {
            final Pointer triggeredVmThreadLocals = SAFEPOINTS_TRIGGERED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            final Pointer disabledVmThreadLocals = SAFEPOINTS_DISABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            final Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            final Pointer refMapArea = STACK_REFERENCE_MAP.getConstantWord(vmThreadLocals).asPointer();
            final Pointer anchor = LAST_JAVA_FRAME_ANCHOR.getVariableWord(vmThreadLocals).asPointer();
            final Pointer stackYellowZone = guardPage.asPointer();
            final Pointer stackRedZone = stackYellowZone.minus(guardPageSize());
            final Pointer stackEnd = HIGHEST_STACK_SLOT_ADDRESS.getConstantWord(enabledVmThreadLocals).asPointer();
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Initialization completed for thread[id=");
            Log.print(id);
            Log.print(", name=\"");
            Log.print(name);
            Log.print("\", native id=");
            Log.print(nativeThread);
            Log.println("]:");
            Log.println("  Stack layout:");
            Address lastRegionStart = Address.zero();
            final int stackSize = stackEnd.minus(stackBase).toInt();
            final Pointer stackPointer = SpecialBuiltin.getIntegerRegister(Role.CPU_STACK_POINTER);
            final Pointer stackYellowZoneEnd = stackYellowZone.plus(guardPageSize());
            lastRegionStart = traceStackRegion("OS thread specific data and native frames", stackBase, stackPointer, stackEnd, lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Frame of Java methods, native stubs and native functions", stackBase, stackYellowZoneEnd, stackPointer, lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Stack overflow guard (yellow zone)", stackBase, stackYellowZone, stackYellowZoneEnd, lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Stack overflow guard (red zone)", stackBase, stackRedZone, stackYellowZone, lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Reference map area", stackBase, refMapArea, stackRedZone, lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Java frame anchor", stackBase, anchor, refMapArea, lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Thread locals (disabled)", stackBase, disabledVmThreadLocals, threadLocalStorageSize().toInt(), lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Thread locals (enabled)", stackBase, enabledVmThreadLocals, threadLocalStorageSize().toInt(), lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Thread locals (triggered)", stackBase, triggeredVmThreadLocals, threadLocalStorageSize().toInt(), lastRegionStart, stackSize);
            if (stackBase.lessThan(triggeredVmThreadLocals)) {
                lastRegionStart = traceStackRegion("Unmapped page", stackBase, stackBase, triggeredVmThreadLocals, lastRegionStart, stackSize);
            }
            Log.printThreadLocals(vmThreadLocals, true);
            Log.unlock(lockDisabledSafepoints);
        }
    }

    private static Address traceStackRegion(String label, Address base, Address start, Address end, Address lastRegionStart, int usedStackSize) {
        return traceStackRegion(label, base, start, end.minus(start).toInt(), lastRegionStart, usedStackSize);
    }

    private static Address traceStackRegion(String label, Address base, Address start, int size, Address lastRegionStart, int usedStackSize) {
        FatalError.check(lastRegionStart.isZero() || start.lessEqual(lastRegionStart), "Overlapping stack regions");
        if (size > 0) {
            final Address end = start.plus(size);
            FatalError.check(lastRegionStart.isZero() || end.lessEqual(lastRegionStart), "Overlapping stack regions");
            final int startOffset = start.minus(base).toInt();
            final int endOffset = startOffset + size;
            if (lastRegionStart.isZero() || !lastRegionStart.equals(end)) {
                Log.print("  +----- ");
                Log.print(end);
                Log.print("  [");
                Log.print(endOffset >= 0 ? "+" : "");
                Log.print(endOffset);
                Log.println("]");
            }
            Log.println("  |");
            Log.print("  | ");
            Log.print(label);
            Log.print(" [");
            Log.print(size);
            Log.print(" bytes, ");
            Log.print(((float) size * 100) / usedStackSize);
            Log.println("%]");
            Log.println("  |");
            Log.print("  +----- ");
            Log.print(start);
            Log.print(" [");
            Log.print(startOffset >= 0 ? "+" : "");
            Log.print(startOffset);
            Log.println("]");
        }
        return start;
    }

    private void traceThreadForUncaughtException(Throwable throwable) {
        if (traceThreads()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("VmThread[id=");
            Log.print(id);
            Log.print(", name=\"");
            Log.print(name);
            Log.print("] Uncaught exception of type ");
            Log.println(ObjectAccess.readClassActor(throwable).name);
            Log.unlock(lockDisabledSafepoints);
        }
    }

    private void traceThreadAfterTermination() {
        if (traceThreads()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Thread terminated [id=");
            Log.print(id);
            Log.print(", name=\"");
            Log.print(name);
            Log.println("\"]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    private static void invokeShutdownHooks() {
        VMOptions.beforeExit();
        //Shutdown.shutdown(), but it's not visible
        if (traceThreads()) {
            Log.println("invoking Shutdown hooks");
        }
        try {
            final ClassActor classActor = ClassActor.fromJava(Class.forName("java.lang.Shutdown"));
            final StaticMethodActor shutdownMethod = classActor.findLocalStaticMethodActor("shutdown");
            shutdownMethod.invoke();
        } catch (Throwable throwable) {
            ProgramError.unexpected("error invoking Shutdown.shutdown", throwable);
        }
    }

    /*
     * This function exists for the benefit of the primordial thread, as per nonJniNativeSleep.
     */
    @C_FUNCTION
    private static native boolean nonJniNativeJoin(Word nativeThread);

    /*
     * This cannot be a C_FUNCTION as it blocks!
     */
    private static native boolean nativeJoin(Word nativeThread);

    public boolean join() {
        return nativeJoin(nativeThread);
    }

    // Access to thread local variables


    // Only used by the EIR interpreter(s)
    @HOSTED_ONLY
    public void setVmThreadLocals(Address address) {
        vmThreadLocals = address.asPointer();
    }

    @INLINE
    public final Pointer vmThreadLocals() {
        return vmThreadLocals;
    }

    // JNI support

    public static VmThread fromJniEnv(Pointer jniEnv) {
        final Pointer vmThreadLocals = jniEnv.minus(JNI_ENV.offset);
        return fromVmThreadLocals(vmThreadLocals);
    }

    @INLINE
    public static VmThread fromVmThreadLocals(Pointer vmThreadLocals) {
        if (MaxineVM.isHosted()) {
            return mainVMThread;
        }
        return UnsafeCast.asVmThread(VM_THREAD.getConstantReference(vmThreadLocals).toJava());
    }

    public JniHandle createLocalHandle(Object object) {
        return JniHandles.createLocalHandle(jniHandles, object);
    }

    public final JniHandles jniHandles() {
        return jniHandles;
    }

    private Throwable pendingException;

    /**
     * Sets or clears the exception to be thrown once this thread returns from the native function most recently entered
     * via a {@linkplain NativeStubGenerator native stub}. This mechanism is used to propogate exceptions through native
     * frames and should be used used for any other purpose.
     *
     * @param exception if non-null, this exception will be raised upon returning from the closest native function on
     *            this thread's stack. Otherwise, the pending exception is cleared.
     */
    public void setPendingException(Throwable exception) {
        this.pendingException = exception;
    }

    /**
     * Gets the exception that will be thrown once this thread returns from the native function most recently entered
     * via a {@linkplain NativeStubGenerator native stub}.
     *
     * @return the exception that will be raised upon returning from the closest native function on this thread's stack
     *         or null if there is no such pending exception
     */
    public Throwable pendingException() {
        return pendingException;
    }

    /**
     * Raises the pending exception on this thread (if any) and clears it.
     * Called from a {@linkplain NativeStubGenerator JNI stub} after a native function returns.
     */
    public void throwPendingException() throws Throwable {
        final Throwable pendingException = this.pendingException;
        if (pendingException != null) {
            this.pendingException = null;
            throw pendingException;
        }
    }

    // Support for JDK_java_lang_Thread:

    public static void yield() {
        nativeYield();
    }

    private static native void nativeYield();

    /**
     * Causes this thread to begin execution.
     */
    public void start0() {
        state = Thread.State.RUNNABLE;
        VmThreadMap.ACTIVE.startVmThread(this, stackSizeOption.getValue().aligned(Platform.host().pageSize).asSize(), javaThread.getPriority());
    }

    public boolean isInterrupted(boolean clearInterrupted) {
        final boolean interrupted = this.interrupted;
        if (clearInterrupted) {
            this.interrupted = false;
        }
        return interrupted;
    }

    public int countStackFrames() {
        FatalError.unimplemented();
        return -1;
    }

    public static StackTraceElement[][] dumpThreads(Thread[] threads) {
        FatalError.unimplemented();
        return null;
    }

    public static Thread[] getThreads() {
        FatalError.unimplemented();
        return null;
    }

    private static native void nativeSetPriority(Word nativeThread, int newPriority);

    /**
     * This can be called in two contexts: 1. During the creation of a thread (i.e. during the Thread constructor) 2.
     * During the execution of a thread. In case 1 the native thread does not exist as it is not created until the
     * {@link #start0()} method is called, so there is nothing to do (the priority is passed down by start0). In case 2
     * we call a native function to change the priority.
     *
     * @param newPriority the new thread priority
     */
    public void setPriority0(int newPriority) {
        if (nativeThread.isZero()) {
            // native thread does not exist yet
        } else {
            nativeSetPriority(nativeThread, newPriority);
        }
    }

    /**
     * This exists for the benefit of the primordial thread.
     *
     * The primordial thread cannot (currently) safely call JNI functions because it is not a "real" Java thread. This
     * is a workaround - obviously it would be better to find a way to relax this restriction.
     *
     * @param numberOfMilliSeconds
     */
    static void nonJniSleep(long numberOfMilliSeconds) {
        nonJniNativeSleep(numberOfMilliSeconds);
    }

    private static final CriticalNativeMethod nonJniNativeSleep = new CriticalNativeMethod(VmThread.class, "nonJniNativeSleep");
    private static final CriticalNativeMethod nativeSleep = new CriticalNativeMethod(VmThread.class, "nativeSleep");
    private static final CriticalNativeMethod nativeYield = new CriticalNativeMethod(VmThread.class, "nativeYield");

    @C_FUNCTION
    private static native void nonJniNativeSleep(long numberOfMilliSeconds);

    private static native boolean nativeSleep(long numberOfMilliSeconds);

    /*
     * use protected member method so that GuestVM's SchedThread is able to implement its own sleep method
     */
    protected boolean sleep0(long numberOfMilliSeconds) {
        return VmThread.nativeSleep(numberOfMilliSeconds);
    }

    public static void sleep(long millis) throws InterruptedException {
        boolean interrupted = current().sleep0(millis);
        if (interrupted) {
            interrupted = false;
            throw new InterruptedException();
        }
    }

    public void stop0(Object throwable) {
        terminationCause = (Throwable) throwable;
        FatalError.unimplemented();
        Throw.raise(this); // not a Throwable => uncatchable - see 'run()' above
    }

    public void suspend0() {
        FatalError.unimplemented();
    }

    public void resume0() {
        FatalError.unimplemented();
    }

    public static native void nativeInterrupt(Word nativeThread);

    public void interrupt0() {
        if (nativeThread.isZero()) {
            // Native thread does not exist yet
        } else {
            // Set to true as default. Will be cleared on this VmThread's
            // native thread if an InterruptedException is thrown after the
            // interruption.
            interrupted = true;
            nativeInterrupt(nativeThread);
        }
    }

    @Override
    public String toString() {
        return "VM" + javaThread;
    }

    public  Address guardPage() {
        return guardPage;
    }

    public Address guardPageEnd() {
        return guardPage.plus(guardPageSize());
    }

    public static int guardPageSize() {
        return VMConfiguration.target().platform().pageSize;
    }

    /**
     * This method is called when the VmThread initialization is complete.
     * A subclass can override this method to do whatever subclass-specific
     * initialization that depends on that invariant.
     */
    protected void initializationComplete() {

    }

    /**
     * This method is called when the VmThread termination is complete.
     * A subclass can override this method to do whatever subclass-specific
     * termination that depends on that invariant.
     */
    protected void terminationComplete() {

    }

    /**
     * This method parks the current thread according to the semantics of {@link Unsafe#park()}.
     * @throws InterruptedException
     */
    public void park() throws InterruptedException {
        synchronized (this) {
            if (parkState == 1) {
                parkState = 0;
            } else {
                parkState = 2;
                wait();
            }
        }
    }

    /**
     * This method parks the current thread according to the semantics of {@link Unsafe#park()}.
     * @throws InterruptedException
     */
    public void park(long wait) throws InterruptedException {
        synchronized (this) {
            if (parkState == 1) {
                parkState = 0;
            } else {
                parkState = 2;
                wait(wait / 1000000, (int) (wait % 1000000));
            }
        }
    }

    /**
     * This method unparks the current thread according to the semantics of {@link Unsafe#unpark()}.
     */
    public void unpark() {
        synchronized (this) {
            parkState = 1;
            notifyAll();
        }
    }
}
