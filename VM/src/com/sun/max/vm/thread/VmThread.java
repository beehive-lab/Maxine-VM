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

import static com.sun.max.vm.actor.member.InjectedReferenceFieldActor.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.graft.*;
import com.sun.max.vm.bytecode.refmaps.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.monitor.modal.schemes.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.monitor.modal.sync.nat.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.*;
import com.sun.max.vm.stack.*;

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
     * The amount of stack required to {@linkplain #reprotectGuardPage(Throwable) reset} the
     * {@linkplain #guardPage() guard page} after unwinding a stack while raising a {@code StackOverflowError}.
     */
    public static final int MIN_STACK_SPACE_FOR_GUARD_PAGE_RESETTING = 200;

    @INLINE
    public static boolean traceThreads() {
        return _traceThreadsOption.isPresent();
    }

    private static final VMOption _traceThreadsOption = new VMOption("-XX:TraceThreads", "Trace thread management activity for debugging purposes.", MaxineVM.Phase.PRISTINE);

    private static final Size DEFAULT_STACK_SIZE = Size.M;

    private static final VMSizeOption _stackSizeOption = new VMSizeOption("-Xss", DEFAULT_STACK_SIZE, "Stack size of new threads.", MaxineVM.Phase.PRISTINE);

    private final Thread _javaThread;
    private volatile Thread.State _state = Thread.State.NEW;
    private volatile boolean _interrupted = false;
    private Throwable _terminationCause;
    private int _threadMapID;

    private  TLAB _tlab = new TLAB();

    /**
     * Gets the number of bytes that would be used to cover the usual case when allocating an alternate stack area.
     */
    @C_FUNCTION
    private static native int nativeGetDefaultThreadSignalStackSize();

    private Address _guardPage = Address.zero();

    @CONSTANT
    private Pointer _vmThreadLocals = Pointer.zero();

    @INSPECTED
    private final String _name;

    @INSPECTED
    private final long _serial;

    @CONSTANT
    protected Word _nativeThread = Word.zero();

    private final VmStackFrameWalker _stackFrameWalker = new VmStackFrameWalker(Pointer.zero());

    /**
     * Gets an object that can be used to walk the frames in this thread's stack.
     *
     * <b>This must only be used when {@linkplain Throw#raise(Object, Pointer, Pointer, Pointer) throwing an exception}
     * or {@linkplain StackReferenceMapPreparer preparing} a stack reference map. These are the only contexts in which
     * allocation must not occur.</b> All other stack walks must create a new stack walker instance.
     */
    public VmStackFrameWalker stackFrameWalker() {
        FatalError.check(_stackFrameWalker != null, "Thread-local stack frame walker cannot be null for a running thread");
        return _stackFrameWalker;
    }

    public StackFrameWalker newStackFrameWalker() {
        return new VmStackFrameWalker(vmThreadLocals());
    }

    private final StackReferenceMapPreparer _stackReferenceMapPreparer = new StackReferenceMapPreparer(this);

    public StackReferenceMapPreparer stackReferenceMapPreparer() {
        return _stackReferenceMapPreparer;
    }

    private final CompactReferenceMapInterpreter _compactReferenceMapInterpreter = new CompactReferenceMapInterpreter();

    @PROTOTYPE_ONLY
    private static final ThreadLocal<CompactReferenceMapInterpreter> _prototypeCompactReferenceMapInterpreter = new ThreadLocal<CompactReferenceMapInterpreter>() {

        @Override
        protected CompactReferenceMapInterpreter initialValue() {
            return new CompactReferenceMapInterpreter();
        }
    };

    public CompactReferenceMapInterpreter compactReferenceMapInterpreter() {
        if (MaxineVM.isPrototyping()) {
            return _prototypeCompactReferenceMapInterpreter.get();
        }
        return _compactReferenceMapInterpreter;
    }

    private static long _counter = 0;

    public Thread.State state() {
        return _state;
    }

    public void setState(Thread.State state) {
        _state = state;
    }

    public void setTLAB(TLAB tlab) {
        _tlab = tlab;
    }

    /**
     * Gets the unique identifier for this thread. This identifier
     * is unique for the lifetime of this thread.
     */
    public long serial() {
        return _serial;
    }

    public Thread javaThread() {
        return _javaThread;
    }

    public static VmThread fromJava(Thread javaThread) {
        return Thread_vmThread.read(javaThread);
    }

    public Word nativeThread() {
        return _nativeThread;
    }

    /**
     * Gets the identifier used to identify this thread in the {@linkplain VmThreadMap thread map}.
     * A thread that has not been added to the thread map, will have an identifier of 0 and
     * a thread that has been terminated and removed from the map will have an identifier
     * of -1.
     */
    int threadMapID() {
        return _threadMapID;
    }

    void setThreadMapID(int id) {
        _threadMapID = id;
    }

    public String getName() {
        return _name;
    }

    private JavaMonitor _protectedMonitor;

    @INLINE
    public final JavaMonitor protectedMonitor() {
        return _protectedMonitor;
    }

    @INLINE
    public final void setProtectedMonitor(JavaMonitor protectedMonitor) {
        _protectedMonitor = protectedMonitor;
    }


    private ConditionVariable _waitingCondition;

    @INLINE
    public final ConditionVariable waitingCondition() {
        return _waitingCondition;
    }

    @INLINE
    public final void setWaitingCondition(ConditionVariable waitingCondition) {
        _waitingCondition = waitingCondition;
    }

    private VmThread _nextWaitingThread;

    @INLINE
    public final VmThread nextWaitingThread() {
        return _nextWaitingThread;
    }

    @INLINE
    public final void setNextWaitingThread(VmThread nextWaitingThread) {
        _nextWaitingThread = nextWaitingThread;
    }

    public void setInterrupted() {
        _interrupted = true;
    }

    /**
     * The pool of JNI local references allocated for this thread.
     */
    private final JniHandles _jniHandles;

    public VmThread(Thread javaThread) {
        _waitingCondition = new ConditionVariable();
        synchronized (VmThread.class) {
            _serial = _counter++;
        }
        _javaThread = javaThread;
        _name = javaThread.getName();
        _jniHandles = new JniHandles();
    }

    private static final boolean _useUnsafeBeTerminated = !(VMConfiguration.target().monitorScheme() instanceof ModalMonitorScheme);

    private void beTerminatedUnsafe() {
        VmThreadMap.ACTIVE.removeVmThreadLocals(_vmThreadLocals);
        synchronized (_javaThread) {
            // Must set TERMINATED before the notify in case a joiner is already waiting
            // @see Thread.join()
            _state = Thread.State.TERMINATED;
            _javaThread.notifyAll();
        }
        // Monitor acquisition after point this MUST NOT HAPPEN as it may reset _state to RUNNABLE
        _nativeThread = Address.zero();
        _vmThreadLocals = Pointer.zero();
        _threadMapID = -1;
    }

    private void beTerminatedSafe() {
        synchronized (_javaThread) {
            // Must set TERMINATED before the notify in case a joiner is already waiting
            // @see Thread.join()
            _state = Thread.State.TERMINATED;
            _javaThread.notifyAll();
        }
        // It is the monitor scheme's responsibility to ensure that this thread isn't reset to RUNNABLE if it blocks
        // here.
        VmThreadMap.ACTIVE.removeVmThreadLocals(_vmThreadLocals);
        // Monitor acquisition after point this MUST NOT HAPPEN as it may reset _state to RUNNABLE
        _nativeThread = Address.zero();
        _vmThreadLocals = Pointer.zero();
        _threadMapID = -1;
    }

    protected void beTerminated() {
        if (_useUnsafeBeTerminated) {
            beTerminatedUnsafe();
        } else {
            beTerminatedSafe();
        }
    }

    /**
     * This happens during prototyping. Then, 'Thread.currentThread()' refers to the "main" thread of the host VM. Since
     * there is no 'Thread' constructor that we could call without a valid parent thread, we hereby clone the host VM's
     * main thread.
     */
    @PROTOTYPE_ONLY
    private static VmThread createMain() {
        final Thread thread = HostObjectAccess.mainThread();
        final VmThread vmThread = VmThreadFactory.create(thread);
        VmThreadMap.ACTIVE.addMainVmThread(vmThread);
        return vmThread;
    }

    private static final VmThread _mainVMThread = createMain();

    public static VmThread main() {
        return _mainVMThread;
    }

    @PROTOTYPE_ONLY
    public static Size stackSize() {
        return DEFAULT_STACK_SIZE;
    }

    @C_FUNCTION
    protected static native Word nativeThreadCreate(int id, Size stackSize, int priority);

    private static final CriticalNativeMethod nonJniNativeSleep = new CriticalNativeMethod(VmThread.class, "nonJniNativeSleep");

    /**
     * Initializes the VM thread system and starts the main Java thread.
     */
    public static void createAndRunMainThread() {
        final Size requestedStackSize = _stackSizeOption.getValue().aligned(Platform.host().pageSize()).asSize();

        final Word nativeThread = nativeThreadCreate(_mainVMThread._threadMapID, requestedStackSize, Thread.NORM_PRIORITY);
        if (nativeThread.isZero()) {
            FatalError.unexpected("Could not start main native thread.");
        } else {
            nativeJoin(nativeThread);
            VmThreadMap.ACTIVE.joinAllNonDaemons();
        }
        // Drop back to PRIMORDIAL because we are now in the primordial thread
        MaxineVM.host().setPhase(MaxineVM.Phase.PRIMORDIAL);
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
        if (MaxineVM.isPrototyping()) {
            return Pointer.zero();
        }
        return JNI_ENV.pointer(currentVmThreadLocals());
    }

    @INLINE
    public static VmThread current() {
        if (MaxineVM.isPrototyping()) {
            return _mainVMThread;
        }
        return UnsafeLoophole.cast(VmThread.class, VM_THREAD.getConstantReference().toJava());
    }

    @INLINE
    public static VmThread current(Pointer vmThreadLocals) {
        if (MaxineVM.isPrototyping()) {
            return _mainVMThread;
        }
        return UnsafeLoophole.cast(VmThread.class, VmThreadLocal.VM_THREAD.getConstantReference(vmThreadLocals).toJava());
    }

    @INLINE
    private static Word currentNativeThread() {
        if (MaxineVM.isPrototyping()) {
            return Pointer.zero();
        }
        return NATIVE_THREAD.getConstantWord();
    }

    public static final Address TAG = Address.fromLong(0xbabacafecabafebaL);

    private static void executeRunnable(VmThread vmThread) throws Throwable {
        try {
            if (vmThread == _mainVMThread) {
                VMConfiguration.hostOrTarget().runScheme().run();
            } else {
                vmThread._javaThread.run();
            }
        } finally {
            // 'stop0()' support.
            if (vmThread._terminationCause != null) {
                // We arrive here because an uncatchable non-Throwable object has been propagated as an exception.
                throw vmThread._terminationCause;
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
     * @param nativeThread the address of the native thread data structure (e.g. a pointer to a pthread_t value)
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
        disabledVmThreadLocals.setWord(SAFEPOINT_LATCH.index(), disabledVmThreadLocals);
        Safepoint.setLatchRegister(disabledVmThreadLocals);

        enabledVmThreadLocals.setWord(SAFEPOINT_LATCH.index(), enabledVmThreadLocals);

        // set up references to all three locals in all three locals
        enabledVmThreadLocals.setWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index(), enabledVmThreadLocals);
        enabledVmThreadLocals.setWord(SAFEPOINTS_DISABLED_THREAD_LOCALS.index(), disabledVmThreadLocals);
        enabledVmThreadLocals.setWord(SAFEPOINTS_TRIGGERED_THREAD_LOCALS.index(), triggeredVmThreadLocals);

        disabledVmThreadLocals.setWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index(), enabledVmThreadLocals);
        disabledVmThreadLocals.setWord(SAFEPOINTS_DISABLED_THREAD_LOCALS.index(), disabledVmThreadLocals);
        disabledVmThreadLocals.setWord(SAFEPOINTS_TRIGGERED_THREAD_LOCALS.index(), triggeredVmThreadLocals);

        triggeredVmThreadLocals.setWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index(), enabledVmThreadLocals);
        triggeredVmThreadLocals.setWord(SAFEPOINTS_DISABLED_THREAD_LOCALS.index(), disabledVmThreadLocals);
        triggeredVmThreadLocals.setWord(SAFEPOINTS_TRIGGERED_THREAD_LOCALS.index(), triggeredVmThreadLocals);

        NATIVE_THREAD.setConstantWord(enabledVmThreadLocals, nativeThread);
        JNI_ENV.setConstantWord(enabledVmThreadLocals, JniNativeInterface.pointer());
        VmThreadLocal.TAG.setConstantWord(enabledVmThreadLocals, TAG);
        SAFEPOINT_VENUE.setVariableReference(enabledVmThreadLocals, Reference.fromJava(Safepoint.Venue.NATIVE));

        // enable write barriers by setting the adjusted card table address
        if (MaxineVM.isRunning() || MaxineVM.isStarting()) {
            // use the normal card table
            ADJUSTED_CARDTABLE_BASE.setConstantWord(enabledVmThreadLocals, CardRegion.getAdjustedCardTable());
        } else {
            // use the primordial card table
            ADJUSTED_CARDTABLE_BASE.setConstantWord(enabledVmThreadLocals, ADJUSTED_CARDTABLE_BASE.getConstantWord(MaxineVM.primordialVmThreadLocals()));
        }

        // Add the VM thread locals to the active map
        final VmThread vmThread = VmThreadMap.ACTIVE.addVmThreadLocals(id, enabledVmThreadLocals);

        vmThread._nativeThread = nativeThread;
        vmThread._vmThreadLocals = enabledVmThreadLocals;
        vmThread._stackFrameWalker.setVmThreadLocals(enabledVmThreadLocals);

        HIGHEST_STACK_SLOT_ADDRESS.setConstantWord(triggeredVmThreadLocals, stackEnd);
        LOWEST_STACK_SLOT_ADDRESS.setConstantWord(triggeredVmThreadLocals, triggeredVmThreadLocals.plus(Word.size()));
        STACK_REFERENCE_MAP.setConstantWord(triggeredVmThreadLocals, refMapArea);

        vmThread._guardPage = stackYellowZone;

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
            vmThread._terminationCause = throwable;
        } finally {
            // If this is the main thread terminating, initiate shutdown hooks
            if (vmThread == _mainVMThread) {
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
            final Pointer stackYellowZone = _guardPage.asPointer();
            final Pointer stackRedZone = stackYellowZone.minus(guardPageSize());
            final Pointer stackEnd = HIGHEST_STACK_SLOT_ADDRESS.getConstantWord(enabledVmThreadLocals).asPointer();
            final boolean lockDisabledSafepoints = Debug.lock();
            Debug.print("Initialization completed for thread[serial=");
            Debug.print(_serial);
            Debug.print(", name=\"");
            Debug.print(_name);
            Debug.println("\"]:");
            Debug.print("  Adjusted card table address: ");
            Debug.println(ADJUSTED_CARDTABLE_BASE.getConstantWord());
            Debug.println("  Stack layout:");
            Address lastRegionStart = Address.zero();
            final int stackSize = stackEnd.minus(stackBase).toInt();
            final Pointer stackPointer = SpecialBuiltin.getIntegerRegister(Role.CPU_STACK_POINTER);
            final Pointer stackYellowZoneEnd = stackYellowZone.plus(guardPageSize());
            lastRegionStart = traceStackRegion("OS thread specific data and native frames", stackBase, stackPointer, stackEnd, lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Frame of Java methods, native stubs and native functions", stackBase, stackYellowZoneEnd, stackPointer, lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Stack overflow guard (yellow zone)", stackBase, stackYellowZone, stackYellowZoneEnd, lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Stack overflow guard (red zone)", stackBase, stackRedZone, stackYellowZone, lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Reference map area", stackBase, refMapArea, stackRedZone, lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Thread locals (disabled)", stackBase, disabledVmThreadLocals, THREAD_LOCAL_STORAGE_SIZE.toInt(), lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Thread locals (enabled)", stackBase, enabledVmThreadLocals, THREAD_LOCAL_STORAGE_SIZE.toInt(), lastRegionStart, stackSize);
            lastRegionStart = traceStackRegion("Thread locals (triggered)", stackBase, triggeredVmThreadLocals, THREAD_LOCAL_STORAGE_SIZE.toInt(), lastRegionStart, stackSize);
            if (stackBase.lessThan(triggeredVmThreadLocals)) {
                lastRegionStart = traceStackRegion("Unmapped page", stackBase, stackBase, triggeredVmThreadLocals, lastRegionStart, stackSize);
            }
            Debug.unlock(lockDisabledSafepoints);
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
                Debug.print("  +----- ");
                Debug.print(end);
                Debug.print("  [");
                Debug.print(endOffset >= 0 ? "+" : "");
                Debug.print(endOffset);
                Debug.println("]");
            }
            Debug.println("  |");
            Debug.print("  | ");
            Debug.print(label);
            Debug.print(" [");
            Debug.print(size);
            Debug.print(" bytes, ");
            Debug.print(((float) size * 100) / usedStackSize);
            Debug.println("%]");
            Debug.println("  |");
            Debug.print("  +----- ");
            Debug.print(start);
            Debug.print(" [");
            Debug.print(startOffset >= 0 ? "+" : "");
            Debug.print(startOffset);
            Debug.println("]");
        }
        return start;
    }

    private void traceThreadForUncaughtException(Throwable throwable) {
        if (traceThreads()) {
            final boolean lockDisabledSafepoints = Debug.lock();
            Debug.print("VmThread[serial=");
            Debug.print(_serial);
            Debug.print(", name=\"");
            Debug.print(_name);
            Debug.print("] Uncaught exception of type ");
            Debug.println(ObjectAccess.readClassActor(throwable).name());
            Debug.unlock(lockDisabledSafepoints);
        }
    }

    private void traceThreadAfterTermination() {
        if (traceThreads()) {
            final boolean lockDisabledSafepoints = Debug.lock();
            Debug.print("Thread terminated [serial=");
            Debug.print(_serial);
            Debug.print(", name=\"");
            Debug.print(_name);
            Debug.println("\"]");
            Debug.unlock(lockDisabledSafepoints);
        }
    }

    private static void invokeShutdownHooks() {
        //Shutdown.shutdown(), but it's not visible
        if (traceThreads()) {
            Debug.println("invoking Shutdown hooks");
        }
        try {
            final ClassActor classActor = ClassActor.fromJava(Class.forName("java.lang.Shutdown"));
            final StaticMethodActor shutdownMethod = classActor.findLocalStaticMethodActor("shutdown");
            shutdownMethod.invoke();
        } catch (Throwable throwable) {
            ProgramError.unexpected("error invoking Shutdown.shutdown", throwable);
        }
    }

    /**
     * Determines if this thread is in a state that implies it is executing native code. A thread is guaranteed to never
     * mutate any object references when in this state. If this thread is in the 'in native' state (which is determined
     * by the value of {@link VmThreadLocal#LAST_JAVA_CALLER_INSTRUCTION_POINTER}), it is most likely actually
     * executing native code. However, it may also be in Java code that is executing:
     * <ul>
     * <li>a {@linkplain NativeCallPrologue#nativeCallPrologue() native call prologue} and is after the instruction that
     * sets the 'in native' flag.</li>
     * <li>a {@linkplain NativeCallEpilogue#nativeCallEpilogue(Pointer) native call epilogue} and is before the
     * instruction that resets the 'in native' flag.</li>
     * <li>a {@linkplain JniFunctionWrapper JNI function wrapper} and is before the
     * {@linkplain JniFunctionWrapper#exitThreadInNative(Pointer) instruction} that resets the 'in native' flag or after
     * the {@linkplain JniFunctionWrapper#reenterThreadInNative(Pointer, Word) instruction} that sets the 'in native'
     * flag.</li>
     * <li>code in or called from a {@linkplain C_FUNCTION#isSignalHandler() Java trap handler} when a trap occurs with
     * the 'in native' already set. This mostly cause of this is a trap occuring while executing native code called via
     * a JNI stub.</li>
     * <li>code in or called from a {@linkplain C_FUNCTION#isSignalHandlerStub() Java trap stub} when a trap occurs with
     * the 'in native' already set. This mostly cause of this is a trap occuring while executing native code called via
     * a JNI stub.</li>
     * </ul>
     * <p>
     * ATTENTION: only call this when there is no race with thread termination!
     */
    @INLINE
    public final boolean isInNative() {
        MemoryBarrier.storeLoad();
        return !_vmThreadLocals.isZero() && LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(_vmThreadLocals).isZero();
    }

    @C_FUNCTION
    private static native boolean nativeJoin(Word nativeThread);

    public boolean join() {
        return nativeJoin(_nativeThread);
    }

    // Access to thread local variables

    // Only used by the EIR interpreter(s)
    @PROTOTYPE_ONLY
    public void setVmThreadLocals(Address address) {
        _vmThreadLocals = address.asPointer();
    }

    @INLINE
    public final Pointer vmThreadLocals() {
        return _vmThreadLocals;
    }

    // JNI support

    public static VmThread fromJniEnv(Pointer jniEnv) {
        final Pointer vmThreadLocals = jniEnv.minus(JNI_ENV.index() * Word.size());
        return UnsafeLoophole.cast(VM_THREAD.getConstantReference(vmThreadLocals).toJava());
    }

    public JniHandle createLocalHandle(Object object) {
        return JniHandles.createLocalHandle(_jniHandles, object);
    }

    public JniHandles jniHandles() {
        return _jniHandles;
    }

    private Throwable _pendingException;

    /**
     * Sets or clears the exception to be thrown once this thread returns from the native function most recently entered
     * via a {@linkplain NativeStubGenerator native stub}. This mechanism is used to propogate exceptions through native
     * frames and should be used used for any other purpose.
     *
     * @param exception if non-null, this exception will be raised upon returning from the closest native function on
     *            this thread's stack. Otherwise, the pending exception is cleared.
     */
    public void setPendingException(Throwable exception) {
        _pendingException = exception;
    }

    /**
     * Gets the exception that will be thrown once this thread returns from the native function most recently entered
     * via a {@linkplain NativeStubGenerator native stub}.
     *
     * @return the exception that will be raised upon returning from the closest native function on this thread's stack
     *         or null if there is no such pending exception
     */
    public Throwable pendingException() {
        return _pendingException;
    }

    /**
     * Raises the pending exception on this thread (if any) and clears it.
     * Called from a {@linkplain NativeStubGenerator JNI stub} after a native function returns.
     */
    public void throwPendingException() throws Throwable {
        final Throwable pendingException = _pendingException;
        if (pendingException != null) {
            _pendingException = null;
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
        _state = Thread.State.RUNNABLE;
        VmThreadMap.ACTIVE.startVmThread(this, _stackSizeOption.getValue().aligned(Platform.host().pageSize()).asSize(), _javaThread.getPriority());
    }

    public boolean isInterrupted(boolean clearInterrupted) {
        final boolean interrupted = _interrupted;
        if (clearInterrupted) {
            _interrupted = false;
        }
        return interrupted;
    }

    public int countStackFrames() {
        Problem.unimplemented();
        return -1;
    }

    public static StackTraceElement[][] dumpThreads(Thread[] threads) {
        Problem.unimplemented();
        return null;
    }

    public static Thread[] getThreads() {
        Problem.unimplemented();
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
        if (_nativeThread.isZero()) {
            // native thread does not exist yet
        } else {
            nativeSetPriority(_nativeThread, newPriority);
        }
    }

    /**
     * This exists for the benefit of the primordial thread.
     *
     * @see VmThreadMap#findAnyNonDaemon()  The primordial thread cannot (currently) safely call JNI functions because it
     *      is not a "real" Java thread. This is a workaround - obviously it would be better to find a way to relax this
     *      restriction.
     * @param numberOfMilliSeconds
     */
    static void nonJniSleep(long numberOfMilliSeconds) {
        nonJniNativeSleep(numberOfMilliSeconds);
    }

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
        _terminationCause = (Throwable) throwable;
        Problem.unimplemented();
        Throw.raise(this); // not a Throwable => uncatchable - see 'run()' above
    }

    public void suspend0() {
        Problem.unimplemented();
    }

    public void resume0() {
        Problem.unimplemented();
    }

    public static native void nativeInterrupt(Word nativeThread);

    public void interrupt0() {
        // Problem.unimplemented();
        if (_nativeThread.isZero()) {
            // Native thread does not exist yet
        } else {
            // Set to true as default. Will be cleared on this VmThread's
            // native thread if an InterruptedException is thrown after the
            // interruption.
            _interrupted = true;
            nativeInterrupt(_nativeThread);
        }
    }

    @Override
    public String toString() {
        return "VM" + _javaThread;
    }

    // GC support:

    public TLAB getTLAB() {
        return _tlab;
    }

    public  Address guardPage() {
        return _guardPage;
    }

    public Address guardPageEnd() {
        return _guardPage.plus(guardPageSize());
    }

    public static int guardPageSize() {
        return VMConfiguration.target().platform().pageSize();
    }

    /**
     * Determines if a given exception is a {@link StackOverflowError} and resets the protection access of the guard page
     * used to detect stack overflow. This method is called by each {@linkplain ExceptionDispatcher exception dispatcher}.
     *
     * @param throwable the exception being dispatched
     */
    private static void reprotectGuardPage(Throwable throwable) {
        if (throwable instanceof StackOverflowError) {
            VirtualMemory.protectPage(current().guardPage());
        }
    }

    /**
     * Determines if there is enough space left on this thread's stack to execute the code that resets the stack guard
     * page.
     *
     * @param stackPointer the stack pointer that this thread's stack will be unwound to before executing the code that
     *            resets the stack guard page
     */
    public boolean hasSufficentStackToReprotectGuardPage(Pointer stackPointer) {
        final Pointer limit = stackPointer.minus(VmThread.MIN_STACK_SPACE_FOR_GUARD_PAGE_RESETTING);
        return limit.greaterThan(guardPageEnd());
    }
}
