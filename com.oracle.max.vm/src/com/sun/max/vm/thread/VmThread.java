/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.actor.member.InjectedReferenceFieldActor.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;
import static com.sun.max.vm.type.ClassRegistry.*;

import java.security.*;

import sun.misc.*;

import com.sun.max.annotate.*;
import com.sun.max.atomic.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.bytecode.refmaps.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.value.*;

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
 *      page aligned --> +---------------------------------------------+ <-- yellowZone
 *                       | X X X     Stack overflow detection    X X X |
 *                       | X X X           (red zone)            X X X |
 *      page aligned --> +---------------------------------------------+ <-- redZone
 *
 * Low addresses
 * </pre>
 *
 * The stack layout for each thread is traced when a thread starts up if
 * the {@link #TraceThreads -XX:+TraceThreads} VM option is used.
 *
 * @see VmThreadLocal
 */
public class VmThread {

    static boolean TraceThreads;
    static {
        VMOptions.addFieldOption("-XX:", "TraceThreads",  VmThread.class, "Trace thread start-up and shutdown.", MaxineVM.Phase.PRISTINE);
    }

    private static final Size DEFAULT_STACK_SIZE = Size.K.times(256);

    private static final VMSizeOption STACK_SIZE_OPTION = register(new VMSizeOption("-Xss", DEFAULT_STACK_SIZE, "Stack size of new threads."), MaxineVM.Phase.PRISTINE);

    @HOSTED_ONLY
    private static final ThreadLocal<CompactReferenceMapInterpreter> HOSTED_COMPACT_REFERENCE_MAP_INTERPRETER = new ThreadLocal<CompactReferenceMapInterpreter>() {
        @Override
        protected CompactReferenceMapInterpreter initialValue() {
            return new CompactReferenceMapInterpreter();
        }
    };

    public static final ThreadGroup systemThreadGroup;
    public static final ThreadGroup mainThreadGroup;
    public static final VmThread referenceHandlerThread;
    public static final VmThread finalizerThread;

    /**
     * Single instance of {@link VmOperationThread}.
     */
    public static final VmThread vmOperationThread;

    /**
     * Single instance of {@link SignalDispatcher}.
     */
    public static final VmThread signalDispatcherThread;

    /**
     * The main thread created by the primordial thread at runtime.
     */
    public static final VmThread mainThread;

    @HOSTED_ONLY public static ThreadGroup hostSystemThreadGroup;
    @HOSTED_ONLY public static ThreadGroup hostMainThreadGroup;
    @HOSTED_ONLY public static Thread hostReferenceHandlerThread;
    @HOSTED_ONLY public static Thread hostFinalizerThread;
    @HOSTED_ONLY public static Thread hostMainThread;

    static {
        hostSystemThreadGroup = null;
        hostMainThreadGroup = null;
        hostReferenceHandlerThread = null;
        hostFinalizerThread = null;
        hostMainThread = null;

        hostSystemThreadGroup = Thread.currentThread().getThreadGroup();
        for (ThreadGroup parent = hostSystemThreadGroup.getParent(); parent != null; parent = hostSystemThreadGroup.getParent()) {
            hostSystemThreadGroup = parent;
        }
        for (Thread thread : getThreads(hostSystemThreadGroup)) {
            if (thread.getClass().equals(JDK.java_lang_ref_Reference$ReferenceHandler.javaClass())) {
                hostReferenceHandlerThread = thread;
            } else if (thread.getClass().equals(JDK.java_lang_ref_Finalizer$FinalizerThread.javaClass())) {
                hostFinalizerThread = thread;
            }
        }

        for (ThreadGroup group : getThreadGroups(hostSystemThreadGroup)) {
            if (group.getName().equals("main")) {
                hostMainThreadGroup = group;
                for (Thread thread : getThreads(group)) {
                    if (thread.getName().equals("main")) {
                        hostMainThread = thread;
                    }
                }
            }
        }

        assert hostSystemThreadGroup != null;
        assert hostMainThreadGroup != null;
        assert hostReferenceHandlerThread != null;
        assert hostFinalizerThread != null;
        assert hostMainThread != null;


        systemThreadGroup = new ThreadGroup(hostSystemThreadGroup.getName());
        systemThreadGroup.setMaxPriority(hostSystemThreadGroup.getMaxPriority());
        WithoutAccessCheck.setInstanceField(systemThreadGroup, "parent", null);
        ReferenceValue systemThreadGroupRef = ReferenceValue.from(systemThreadGroup);
        mainThreadGroup = new ThreadGroup(systemThreadGroup, hostMainThreadGroup.getName());

        mainThread = initVmThread(copyProps(hostMainThread, new Thread(mainThreadGroup, hostMainThread.getName())));
        vmOperationThread = initVmThread(new VmOperationThread(systemThreadGroup));
        signalDispatcherThread = initVmThread(new SignalDispatcher(systemThreadGroup));

        try {
            referenceHandlerThread = initVmThread(copyProps(hostReferenceHandlerThread, (Thread) ReferenceHandler_init.invokeConstructor(systemThreadGroupRef, ReferenceValue.from(hostReferenceHandlerThread.getName())).asObject()));
            finalizerThread = initVmThread(copyProps(hostFinalizerThread, (Thread) FinalizerThread_init.invokeConstructor(systemThreadGroupRef).asObject()));
        } catch (Exception e) {
            throw FatalError.unexpected("Error initializing VM threads", e);
        }
    }

    @HOSTED_ONLY
    static Thread[] getThreads(ThreadGroup group) {
        Thread[] list = new Thread[group.activeCount()];
        group.enumerate(list);
        return list;
    }

    @HOSTED_ONLY
    static ThreadGroup[] getThreadGroups(ThreadGroup group) {
        ThreadGroup[] list = new ThreadGroup[group.activeGroupCount()];
        group.enumerate(list);
        return list;
    }

    @HOSTED_ONLY
    static VmThread initVmThread(Thread javaThread) {
        VmThread vmThread = VmThreadFactory.create(javaThread);
        VmThreadMap.addPreallocatedThread(vmThread);
        return vmThread;
    }

    @HOSTED_ONLY
    static Thread copyProps(Thread src, Thread dst) {
        dst.setDaemon(src.isDaemon());
        dst.setPriority(src.getPriority());
        return dst;
    }


    @CONSTANT_WHEN_NOT_ZERO
    private Thread javaThread;

    private volatile Thread.State state = Thread.State.NEW;
    private volatile boolean interrupted = false;
    private Throwable terminationCause;
    private int id;
    private int parkState;

    /**
     * Denotes if this thread was started as a daemon. This property is only set once a thread
     * is about to run (for a VM created thread) or is running (for an attached thread)
     * and never changes thereafter.
     */
    boolean daemon;

    /**
     * The stack guard page(s) used to detect recoverable stack overflow.
     */
    private Pointer yellowZone = Pointer.zero();

    /**
     * The thread locals associated with this thread.
     */
    private Pointer tla = Pointer.zero();

    /**
     * The name of this thread which is kept in sync with the name of the associated {@link #javaThread()}.
     */
    @INSPECTED
    private String name;

    @CONSTANT
    protected Word nativeThread = Word.zero();

    private final VmStackFrameWalker stackFrameWalker = new VmStackFrameWalker(Pointer.zero());

    private final VmStackFrameWalker stackDumpStackFrameWalker = new VmStackFrameWalker(Pointer.zero());

    private final StackReferenceMapPreparer stackReferenceMapPreparer = new StackReferenceMapPreparer(true, true);

    private final StackReferenceMapPreparer stackReferenceMapVerifier = new StackReferenceMapPreparer(true, false);

    private final CompactReferenceMapInterpreter compactReferenceMapInterpreter = new CompactReferenceMapInterpreter();

    public JavaMonitor protectedMonitor;

    private ConditionVariable waitingCondition = ConditionVariableFactory.create();

    /**
     * Holds the exception object for the exception currently being raised. This value will only be
     * non-null during the unwinding process between calls to {@link #storeExceptionForHandler(Throwable, TargetMethod, int)}
     * and {@link #loadExceptionForHandler()}.
     */
    private Throwable exception;

    private boolean yellowZoneUnprotected;

    /**
     * Number of shadow zone pages for overflow checking.
     */
    public static final int STACK_SHADOW_PAGES = 2;

    /**
     * Records the fact that native code has unprotected the yellow zone.
     */
    public void nativeTrapHandlerUnprotectedYellowZone() {
        VmStackFrameWalker sfw = stackFrameWalker;
        if (sfw.isInUse()) {
            Log.println("stack overflow occurred while raising another exception or reference map preparing");
            Log.println("may need to increase safetyMargin in VmThread.checkYellowZoneForRaisingException()");
            FatalError.unexpected("stack overflow occurred while raising another exception or reference map preparing");
        }
        yellowZoneUnprotected = true;
    }

    /**
     * Determines if execution on this thread is "too close" to the yellow zone
     * for safely unwinding to an exception handler without running the risk
     * of "banging" the yellow zone. Too close is determined to be within
     * ({@link #STACK_SHADOW_PAGES} + 1) pages of the yellow zone.
     * If it is, then the yellow zone is unguarded to mitigate the
     * chance of exception raising itself causing stack overflow. The exception
     * handler will reset the guard pages when it calls {@link #loadExceptionForHandler()}.
     */
    public void checkYellowZoneForRaisingException() {
        if (platform().isa == ISA.AMD64) {
            if (!yellowZoneUnprotected) {
                Pointer sp = VMRegister.getCpuStackPointer();
                int safetyMargin = (1 + STACK_SHADOW_PAGES) * platform().pageSize;
                if (sp.minus(yellowZoneEnd()).toInt() < safetyMargin) {
                    VirtualMemory.unprotectPages(yellowZone, YELLOW_ZONE_PAGES);
                    yellowZoneUnprotected = true;
                }
            } else {
                // Yellow zone was unprotected in the native trap handler - see nativeTrapHandlerUnprotectedYellowZone()
            }
        } else {
            throw FatalError.unimplemented();
        }
    }

    /**
     * Stores the exception object being raised. This will subsequently be {@linkplain #loadExceptionForHandler() loaded}
     * by the handler when the stack is unwound.
     *
     * @param e the exception being raised
     * @param handler the target method that will handle the exception
     * @param pos the position in {@code handler} of the handler entry point
     */
    public void storeExceptionForHandler(Throwable e, TargetMethod handler, int pos) {
        if (exception != null) {
            FatalError.unexpected("Previous exception never loaded", exception);
        }
        if (Throw.TraceExceptions > 0) {
            boolean lockDisabledSafepoints = Log.lock();
            Log.printThread(VmThread.current(), false);
            Log.print(": ");
            Throw.logFrame("Caught in ", handler, handler.codeStart().plus(pos));
            Log.unlock(lockDisabledSafepoints);
        }
        exception = e;
    }

    /**
     * Loads the exception object being raised.
     *
     * This method also:
     * <ol>
     * <li>Re-enables safepoints (they were disabled in {@link Throw#raise(Throwable, Pointer, Pointer, Pointer)}).</li>
     * <li>Executes a safepoint.</li>
     * <li>Reprotects the yellow zone if the raising process unprotected it.</li>
     * </ol>
     */
    public Throwable loadExceptionForHandler() {
        Safepoint.enable();
        Safepoint.safepoint();
        Throwable e = exception;
        exception = null;
        FatalError.check(e != null, "Exception object lost during unwinding");
        // Re-protect yellow page if is unprotected AND we're no longer too close to it
        if (yellowZoneUnprotected) {
            Pointer sp = VMRegister.getCpuStackPointer();
            int safetyMargin = (1 + STACK_SHADOW_PAGES) * platform().pageSize;
            if (sp.minus(yellowZoneEnd()).toInt() >= safetyMargin) {
                VirtualMemory.protectPages(yellowZone, VmThread.YELLOW_ZONE_PAGES);
                yellowZoneUnprotected = false;
            }
        }
        return e;
    }

    /**
     * Gets the exception currently in flight.
     *
     * @return {@code null} if there is not exception in flight
     */
    public Throwable pendingException() {
        return exception;
    }

    /**
     * Exception thrown by a {@linkplain JniFunctions JNI function}.
     */
    private Throwable jniException;

    /**
     * The number of VM operations {@linkplain VmOperationThread#submit(VmOperation) submitted}
     * by this thread for execution that have not yet completed.
     */
    private volatile int pendingOperations;

    /**
     * The pool of JNI local references allocated for this thread.
     */
    private JniHandles jniHandles;

    private boolean isGCThread;

    /**
     * Next thread waiting on the same monitor this thread is {@linkplain Object#wait() waiting} on.
     * Any thread can only be waiting on at most one monitor.
     * This thread is {@linkplain #isOnWaitersList() not} on a monitor's waiting thread list if
     * the value of this field is the thread itself.
     *
     * @see StandardJavaMonitor#monitorWait(long)
     * @see StandardJavaMonitor#monitorNotify(boolean)
     */
    public VmThread nextWaitingThread = this;

    /**
     * Determines if this thread is on a monitor's list of waiting threads.
     */
    public boolean isOnWaitersList() {
        return nextWaitingThread != this;
    }

    public void unlinkFromWaitersList() {
        nextWaitingThread = this;
    }

    /**
     * A stack of elements that support  {@link AccessController#doPrivileged(PrivilegedAction)} calls.
     */
    private PrivilegedElement privilegedStackTop;

    @HOSTED_ONLY
    public static Size stackSize() {
        return DEFAULT_STACK_SIZE;
    }

    public static VmThread fromJava(Thread javaThread) {
        return (VmThread) Thread_vmThread.getObject(javaThread);
    }

    @C_FUNCTION
    protected static native Word nativeThreadCreate(int id, Size stackSize, int priority);

    /**
     * Gets the current {@linkplain VmThreadLocal TLA}.
     *
     * @return the value of the safepoint {@linkplain Safepoint#latchRegister() latch} register.
     */
    @INLINE
    public static Pointer currentTLA() {
        return Safepoint.getLatchRegister();
    }

    /**
     * Gets a pointer to the JNI environment data structure for the current thread.
     *
     * @return a value of C type JNIEnv*
     */
    public static Pointer jniEnv() {
        if (MaxineVM.isHosted()) {
            return Pointer.zero();
        }
        return JNI_ENV.addressIn(currentTLA());
    }

    @INLINE
    public static VmThread current() {
        if (MaxineVM.isHosted()) {
            return mainThread;
        }
        return UnsafeCast.asVmThread(VM_THREAD.loadRef(currentTLA()).toJava());
    }

    /**
     * Determines if the current thread is still attaching to the VM. In this state,
     * synchronization and garbage collection are disabled.
     */
    public static boolean isAttaching() {
        return current() == null;
    }

    private static void executeRunnable(VmThread vmThread) throws Throwable {
        try {
            if (vmThread == mainThread) {
                vmConfig().runScheme().run();
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
     * A pre-allocated thread object used to service JNI AttachCurrentThread requests.
     * The need for such an object arises from the fact that synchronization and allocation
     * are not allowed when {@linkplain #add(int, boolean, Address, Pointer, Pointer, Pointer, Pointer) adding}
     * a thread to the global list of running threads.
     *
     * As part of the call to {@link #attach(Pointer, JniHandle, boolean, Pointer, Pointer, Pointer)}
     * made when attaching thread, a new pre-allocated thread object is created.
     */
    private static final AtomicReference threadForAttach = new AtomicReference();
    static {
        VmThread thread = VmThreadFactory.create(null);
        threadForAttach.set(thread);
        VmThreadMap.addPreallocatedThread(thread);

    }

    /**
     * Adds the current thread to the global list of running threads.
     *
     * This method must perform no synchronization or heap allocation and must disable safepoints. In addition, the
     * native caller must hold the global GC and thread list lock for the duration of this call.
     *
     * After returning a value of {@code 0} or {@code 1} this thread is now in a state where it can safely participate in a GC cycle.
     * A {@code -1} return value can only occur when this is a native thread being attached to the VM and it loses the
     * race to acquire the pre-allocated {@linkplain #threadForAttach thread-for-attach} object.
     *
     * @param id if {@code id > 0}, then it's an identifier reserved in the thread map for the thread being started.
     *            Otherwise, {@code id} must be negative and is a temporary identifier (derived from the native thread
     *            handle) of a thread that is being attached to the VM.
     * @param daemon indicates if the thread being added is a daemon thread. This value is ignored if {@code id > 0}.
     * @param nativeThread a handle to the native thread data structure (e.g. a pthread_t value)
     * @param etla the address of the safepoints-enabled TLA
     * @param stackBase the lowest address (inclusive) of the stack (i.e. the stack memory range is {@code [stackBase ..
     *            stackEnd)})
     * @param stackEnd the highest address (exclusive) of the stack (i.e. the stack memory range is {@code [stackBase ..
     *            stackEnd)})
     * @param yellowZone the stack page(s) that have been protected to detect stack overflow
     * @return {@code 1} if the thread was successfully added and it is the {@link VmOperationThread},
     *         {@code 0} if the thread was successfully added, {@code -1} if this attaching thread needs to try again
     *         and {@code -2} if this attaching thread cannot be added because the main thread has exited
     */
    @VM_ENTRY_POINT
    private static int add(int id,
                    boolean daemon,
                    Address nativeThread,
                    Pointer etla,
                    Pointer stackBase,
                    Pointer stackEnd,
                    Pointer yellowZone) {

        // Disable safepoints:
        Safepoint.setLatchRegister(DTLA.load(etla));

        JNI_ENV.store3(etla, NativeInterfaces.jniEnv());

        // Add the VM thread locals to the active map
        VmThread thread;
        boolean isAttaching = id < 0;
        if (isAttaching) {
            thread = UnsafeCast.asVmThread(threadForAttach.get());
            if (thread == null || !threadForAttach.compareAndSet(thread, null)) {
                // We could not exclusively get the pre-allocated thread-for-attach object.
                // So we have to return to native code, release the global GC and thread list lock
                // which gives the winner a chance to allocate and register the next available
                // thread-for-attach object. Then we try again...
                return -1;
            }
            if (!daemon && !VmThreadMap.incrementNonDaemonThreads()) {
                return -2;
            }
            ID.store3(etla, Address.fromLong(thread.id));
        } else {
            thread = VmThreadMap.ACTIVE.getVmThreadForID(id);
            daemon = thread.javaThread().isDaemon();
            thread.daemon = daemon;
        }

        for (VmThreadLocal threadLocal : VmThreadLocal.valuesNeedingInitialization()) {
            threadLocal.initialize();
        }

        HIGHEST_STACK_SLOT_ADDRESS.store3(etla, stackEnd);
        LOWEST_STACK_SLOT_ADDRESS.store3(etla, yellowZone.plus(platform().pageSize));

        thread.nativeThread = nativeThread;
        thread.tla = etla;
        thread.stackFrameWalker.setTLA(etla);
        thread.stackDumpStackFrameWalker.setTLA(etla);
        thread.yellowZone = yellowZone;

        VM_THREAD.store3(etla, Reference.fromJava(thread));
        VmThreadMap.addThreadLocals(thread, etla, daemon);

        return thread.isVmOperationThread() ? 1 : 0;
    }

    /**
     * Calls the {@link Runnable} associated with the current thread. This is called from native
     * thread startup code <b>after</b> the current thread has been {@linkplain #add(int, boolean, Address, Pointer, Pointer, Pointer, Pointer) added}
     * to the global list of running threads.
     *
     * ATTENTION: this signature must match 'VmThreadRunMethod' in "com.oracle.max.vm.native/substrate/threads.h".
     *
     * @param etla the address of the safepoints-enabled TLA
     * @param stackBase the lowest address (inclusive) of the stack (i.e. the stack memory range is {@code [stackBase .. stackEnd)})
     * @param stackEnd the highest address (exclusive) of the stack (i.e. the stack memory range is {@code [stackBase .. stackEnd)})
     */
    @VM_ENTRY_POINT
    @INSPECTED
    private static void run(Pointer etla,
                    Pointer stackBase,
                    Pointer stackEnd) {

        // Enable safepoints:
        Pointer anchor = JniFunctions.prologue(JNI_ENV.addressIn(etla), null);

        final VmThread thread = VmThread.current();

        thread.initializationComplete();

        thread.traceThreadAfterInitialization(stackBase, stackEnd);

        // If this is the main thread, then start up the VM operation thread and other special VM threads
        if (thread == mainThread) {
            // NOTE:
            // The main thread must now bring the VM to the pristine state so as to
            // provide basic services (most importantly, heap allocation) before starting the other "system" threads.
            //
            // Code manager initialization must happen after parsing of pristine options
            // It must also be performed before pristine initialization of the heap scheme.
            // This is a temporary issue due to all code managers being instances of
            // FixedAddressCodeManager and assuming to be allocated directly after the boot region.
            // If the heap scheme is initialized first, it might take this address first, causing failure.
            // In the future, code manager initialization will be dictated by the heap scheme directly,
            // and this issue will disappear.
            Code.initialize();

            vmConfig().initializeSchemes(MaxineVM.Phase.PRISTINE);

            // We can now start the other system threads.
            VmThread.vmOperationThread.start0();
            SpecialReferenceManager.initialize(MaxineVM.Phase.PRISTINE);
            VmThread.signalDispatcherThread.start0();
        }

        try {
            executeRunnable(thread);
        } catch (Throwable throwable) {
            thread.traceThreadForUncaughtException(throwable);

            final Thread javaThread = thread.javaThread();
            // Uncaught exception should be passed by the VM to the uncaught exception handler defined for the thread.
            // Exception thrown by this one should be ignored by the VM.
            try {
                javaThread.getUncaughtExceptionHandler().uncaughtException(javaThread, throwable);
            } catch (Throwable ignoreMe) {
            }
            thread.terminationCause = throwable;
        }
        // If this is the main thread terminating, initiate shutdown hooks after waiting for other non-daemons to terminate
        if (thread == mainThread) {
            VmThreadMap.ACTIVE.joinAllNonDaemons();
            invokeShutdownHooks();
            // This prevents further thread creation
            VmThreadMap.ACTIVE.setVMTerminating();
            SignalDispatcher.terminate();
            // scheme-specific termination
            vmConfig().initializeSchemes(MaxineVM.Phase.TERMINATING);
            VmOperationThread.terminate();

            // Drop back to PRIMORDIAL
            MaxineVM vm = vm();
            vm.phase = MaxineVM.Phase.PRIMORDIAL;
        }

        JniFunctions.epilogue(anchor, null);
    }

    /**
     * ATTENTION: this signature must match 'VmThreadAttachMethod' in "com.oracle.max.vm.native/substrate/threads.h".
     *
     * @param id the unique identifier assigned to this thread when it was {@linkplain #start0() started}. This
     *            identifier is only bound to this thread until it is {@linkplain #beTerminated() terminated}. That is,
     *            only active threads have unique identifiers.
     * @param nativeThread a handle to the native thread data structure (e.g. a pthread_t value)
     * @param stackBase the lowest address (inclusive) of the stack (i.e. the stack memory range is {@code [stackBase .. stackEnd)})
     * @param stackEnd the highest address (exclusive) of the stack (i.e. the stack memory range is {@code [stackBase .. stackEnd)})
     * @param tla the address of a thread locals area
     * @param refMap the native memory to be used for the stack reference map
     * @param yellowZone the stack page(s) that have been protected to detect stack overflow
     */
    @VM_ENTRY_POINT
    private static int attach(
                    Pointer nameCString,
                    JniHandle groupHandle,
                    boolean daemon,
                    Pointer stackBase,
                    Pointer stackEnd,
                    Pointer tla) {

        // Enable safepoints:
        Pointer anchor = JniFunctions.prologue(JNI_ENV.addressIn(ETLA.load(tla)), null);

        VmThread thread = VmThread.current();

        // Create next thread-for-attach
        FatalError.check(threadForAttach.get() == null, "thread-for-attach should be null");
        try {
            VmThread newThread = VmThreadFactory.create(null);
            synchronized (VmThreadMap.THREAD_LOCK) {
                VmThreadMap.addPreallocatedThread(newThread);
            }
            threadForAttach.set(newThread);
        } catch (OutOfMemoryError oome) {
        }

        // Synchronization can only be performed on this thread after the above two
        // statements have been executed.
        try {
            String name = nameCString.isZero() ? null : CString.utf8ToJava(nameCString);
            ThreadGroup group = (ThreadGroup) groupHandle.unhand();
            if (group == null) {
                group = mainThread.javaThread.getThreadGroup();
            }
            JDK_java_lang_Thread.createThreadForAttach(thread, name, group, daemon);

            thread.initializationComplete();

            thread.traceThreadAfterInitialization(stackBase, stackEnd);
            return JniFunctions.JNI_OK;

        } catch (OutOfMemoryError oome) {
            return JniFunctions.JNI_ENOMEM;
        } catch (Throwable throwable) {
            throwable.printStackTrace(Log.out);
            return JniFunctions.JNI_ERR;
        } finally {
            JniFunctions.epilogue(anchor, null);
        }
    }


    @INSPECTED
    @NEVER_INLINE
    private static void detached() {

    }

    /**
     * Cleans up a thread that is terminating.
     *
     * This method is called from the destructor
     * function (i.e. 'threadLocalsBlock_destroy()' in threadLocals.c) associated with the
     * key used to access the thread specifics of the native thread. This function will be
     * called for both threads created by the VM as well as threads attached to the VM
     * allowing a single mechanism to be used for both types of threads.
     *
     * ATTENTION: this signature must match 'VmThreadDetachMethod' in "com.oracle.max.vm.native/substrate/threads.h".
     */
    @VM_ENTRY_POINT
    private static void detach(Pointer tla) {
        // Disable safepoints:
        Pointer anchor = JniFunctions.prologue(JNI_ENV.addressIn(DTLA.load(tla)), null);

        VmThread thread = VmThread.current();

        // Report and clear any pending JNI exception
        Throwable jniException = thread.jniException();
        if (jniException != null) {
            thread.setJniException(null);
            System.err.print("Exception in thread \"" + thread.getName() + "\" ");
            jniException.printStackTrace();
        }

        thread.terminationPending();

        synchronized (thread.javaThread) {
            // Must set TERMINATED before the notify in case a joiner is already waiting
            thread.state = Thread.State.TERMINATED;
            thread.javaThread.notifyAll();
        }

        thread.traceThreadAfterTermination();

        // GC may now reclaim or prepare any of its resources before the thread vanishes forever.
        vmConfig().heapScheme().notifyCurrentThreadDetach();

        synchronized (VmThreadMap.THREAD_LOCK) {
            // It is the monitor scheme's responsibility to ensure that this thread isn't
            // reset to RUNNABLE if it blocks here.
            VmThreadMap.ACTIVE.removeThreadLocals(thread);
        }
        if (MaxineVM.isDebug()) {
            detached();
        }
        // Monitor acquisition after point this MUST NOT HAPPEN as it may reset state to RUNNABLE
        thread.nativeThread = Address.zero();
        thread.tla = Pointer.zero();
        thread.id = -1;
        thread.waitingCondition = null;

        JniFunctions.epilogue(anchor, null);
    }

    @ALIAS(declaringClassName = "java.lang.Shutdown")
    private static native void shutdown();

    private static void invokeShutdownHooks() {
        VMOptions.beforeExit();
        if (TraceThreads) {
            Log.println("invoking Shutdown hooks");
        }
        shutdown();
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

    public static VmThread fromJniEnv(Pointer jniEnv) {
        final Pointer tla = jniEnv.minus(JNI_ENV.offset);
        return fromTLA(tla);
    }

    @INLINE
    public static VmThread fromTLA(Pointer tla) {
        if (MaxineVM.isHosted()) {
            return mainThread;
        }
        return UnsafeCast.asVmThread(VM_THREAD.loadRef(tla).toJava());
    }

    public static void yield() {
        nativeYield();
    }

    private static native void nativeYield();

    private static native void nativeSetPriority(Word nativeThread, int newPriority);

    /**
     * This exists for the benefit of the primordial thread.
     *
     * The primordial thread cannot (currently) safely call JNI functions because it is not a "real" Java thread. This
     * is a workaround - obviously it would be better to find a way to relax this restriction.
     *
     * @param numberOfMilliSeconds
     */
    public static void nonJniSleep(long numberOfMilliSeconds) {
        nonJniNativeSleep(numberOfMilliSeconds);
    }

    @C_FUNCTION
    private static native void nonJniNativeSleep(long numberOfMilliSeconds);

    private static native boolean nativeSleep(long numberOfMilliSeconds);

    public static void sleep(long millis) throws InterruptedException {
        final VmThread current = current();
        boolean interrupted = current.sleep0(millis);
        if (interrupted) {
            current.interrupted = false;
            throw new InterruptedException();
        }
    }

    public static native void nativeInterrupt(Word nativeThread);

    /**
     * Number of yellow zone pages used for detecting recoverable stack overflow.
     * This space must also accommodate the execution of stack over handling from
     * the trap stub all the way until the stack is unwound for the {@link StackOverflowError}
     * exception. If this is too little, the red stack zone will be hit.
     * The maximum space required is very dependent on the compiler but 2 pages
     * seems to be enough on AMD64.
     */
    public static final int YELLOW_ZONE_PAGES = 2;

    /**
     * Number of red zone pages used for detecting unrecoverable stack overflow.
     */
    public static final int RED_ZONE_PAGES = 1;

    /**
     * Gets the size of the yellow stack guard zone.
     */
    public static int yellowZoneSize() {
        return YELLOW_ZONE_PAGES * platform().pageSize;
    }

    /**
     * Gets the size of the red stack guard zone.
     */
    public static int redZoneSize() {
        return RED_ZONE_PAGES * platform().pageSize;
    }

    private static Address traceRegion(String label, Address areaStart, Address regionStart, Address regionEnd, Address lastRegionStart, int areaSize) {
        return traceRegion(label, areaStart, regionStart, regionEnd.minus(regionStart).toInt(), lastRegionStart, areaSize);
    }

    private static Address traceRegion(String label, Address areaStart, Address regionStart, int regionSize, Address lastRegionStart, int areaSize) {
        FatalError.check(lastRegionStart.isZero() || regionStart.lessEqual(lastRegionStart), "Overlapping regions");
        if (regionSize > 0) {
            final Address regionEnd = regionStart.plus(regionSize);
            FatalError.check(lastRegionStart.isZero() || regionEnd.lessEqual(lastRegionStart), "Overlapping regions");
            final int startOffset = regionStart.minus(areaStart).toInt();
            final int endOffset = startOffset + regionSize;
            if (lastRegionStart.isZero() || !lastRegionStart.equals(regionEnd)) {
                Log.print("  +--------- ");
                Log.print(regionEnd);
                Log.print("  [");
                Log.print(endOffset >= 0 ? "+" : "");
                Log.print(endOffset);
                Log.println("]");
            }
            Log.println("  |");
            Log.print("  | ");
            Log.print(label);
            Log.print(" [");
            Log.print(regionSize);
            Log.print(" bytes, ");
            Log.print(((float) regionSize * 100) / areaSize);
            Log.println("%]");
            Log.println("  |");
            Log.print("  +--------- ");
            Log.print(regionStart);
            Log.print(" [");
            Log.print(startOffset >= 0 ? "+" : "");
            Log.print(startOffset);
            Log.println("]");
        }
        return regionStart;
    }

    /**
     * Creates an unbound VmThread that will be bound later.
     */
    public VmThread() {
    }

    /**
     * Create a VmThread.
     *
     * @param javaThread if not {@code null}, then the created VmThread is bound to {@code javaThread}
     */
    public VmThread(Thread javaThread) {
        if (javaThread != null) {
            setJavaThread(javaThread, javaThread.getName());
        }
    }

    /**
     * Gets a preallocated, thread local object that can be used to walk the frames in this thread's stack.
     *
     * <b>This must only be used when {@linkplain Throw#raise(Throwable, Pointer, Pointer, Pointer)} throwing an exception}.
     * Allocation must not occur in this context.</b>
     *
     * @param throwable the exception being raised. If {@code null}, then a StackOverflowError is about to be raised.
     */
    public VmStackFrameWalker unwindingStackFrameWalker(Throwable throwable) {
        FatalError.check(stackFrameWalker != null, "Thread-local stack frame walker cannot be null for a running thread");
        VmStackFrameWalker sfw = stackFrameWalker;
        if (sfw.isInUse()) {
            if (throwable != null) {
                Log.println("exception thrown while raising another exception or reference map preparing");
            } else {
                Log.println("stack overflow occurred while raising another exception or reference map preparing");
                Log.println("may need to increase safetyMargin in VmThread.checkYellowZoneForRaisingException()");
            }
            if (!sfw.isDumpingFatalStackTrace()) {
                sfw.reset();
                sfw.setIsDumpingFatalStackTrace(true);
                Throw.stackDumpWithException(throwable);
                sfw.setIsDumpingFatalStackTrace(false);
            }
            FatalError.unexpected("exception thrown while raising another exception");
        }

        return stackFrameWalker;
    }

    /**
     * Gets a preallocated, thread local object that can be used to walk the frames in this thread's stack.
     *
     * <b>This must only be used when {@linkplain StackReferenceMapPreparer preparing} a stack reference map.
     * Allocation must not occur in this context.</b>
     */
    public VmStackFrameWalker referenceMapPreparingStackFrameWalker() {
        FatalError.check(stackFrameWalker != null, "Thread-local stack frame walker cannot be null for a running thread");
        return stackFrameWalker;
    }

    /**
     * Gets a preallocated, thread local object that can be used to walk the frames in this thread's stack
     * and check their reference maps.
     */
    public VmStackFrameWalker referenceMapVerifyingStackFrameWalker() {
        FatalError.check(stackDumpStackFrameWalker != null, "Thread-local stack frame walker cannot be null for a running thread");
        return stackDumpStackFrameWalker;
    }

    /**
     * Gets a preallocated, thread local object that can be used to log a stack dump without incurring any allocation.
     */
    public VmStackFrameWalker stackDumpStackFrameWalker() {
        FatalError.check(stackDumpStackFrameWalker != null, "Thread-local stack frame walker cannot be null for a running thread");
        return stackDumpStackFrameWalker;
    }

    /**
     * Gets the thread-local object used to prepare the reference map for this stack's thread during garbage collection.
     */
    public StackReferenceMapPreparer stackReferenceMapPreparer() {
        return stackReferenceMapPreparer;
    }

    /**
     * Gets the thread-local object used to verify the reference map for this stack's thread.
     */
    public StackReferenceMapPreparer stackReferenceMapVerifier() {
        return stackReferenceMapVerifier;
    }

    public CompactReferenceMapInterpreter compactReferenceMapInterpreter() {
        if (MaxineVM.isHosted()) {
            return HOSTED_COMPACT_REFERENCE_MAP_INTERPRETER.get();
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
    public final int id() {
        return id;
    }

    /**
     * @see #id()
     */
    final void setID(int id) {
        this.id = id;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    @INLINE
    public final ConditionVariable waitingCondition() {
        return waitingCondition;
    }

    /**
     * Sets the interrupted status of this thread to true.
     */
    public final void setInterrupted() {
        this.interrupted = true;
    }

    /**
     * Determines if this is the single {@link VmOperationThread}.
     */
    public final boolean isVmOperationThread() {
        return vmOperationThread == this;
    }

    /**
     * Determines if this thread is owned by the garbage collector.
     */
    public final boolean isGCThread() {
        return isGCThread;
    }

    /**
     * Bind the given {@code Thread} to this VmThread.
     * @param javaThread thread to be bound
     * @param name the name of the thread
     */
    public final VmThread setJavaThread(Thread javaThread, String name) {
        this.isGCThread = Heap.isGcThread(javaThread);
        this.javaThread = javaThread;
        this.name = name;
        return this;
    }

    private void traceThreadAfterInitialization(Pointer stackBase, Pointer stackEnd) {
        if (TraceThreads) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Initialization completed for thread[id=");
            Log.print(id);
            Log.print(", name=\"");
            Log.print(name);
            Log.print("\", native id=");
            Log.print(nativeThread);
            Log.println("]:");
            Log.println("Stack layout:");
            Address lastRegionStart = Address.zero();
            final int stackSize = stackEnd.minus(stackBase).toInt();
            final Pointer stackPointer = VMRegister.getCpuStackPointer();
            lastRegionStart = traceRegion("OS thread specific data and native frames", stackBase, stackPointer, stackEnd, lastRegionStart, stackSize);
            lastRegionStart = traceRegion("Frame of Java methods, native stubs and native functions", stackBase, yellowZoneEnd(), stackPointer, lastRegionStart, stackSize);
            lastRegionStart = traceRegion("Stack yellow zone", stackBase, yellowZone, yellowZoneEnd(), lastRegionStart, stackSize);
            lastRegionStart = traceRegion("Stack red zone", stackBase, redZone(), redZoneEnd(), lastRegionStart, stackSize);

            lastRegionStart = Address.zero();
            Address ntl = NATIVE_THREAD_LOCALS.load(currentTLA());
            Pointer ttla = TTLA.load(currentTLA());
            Pointer etla = ETLA.load(currentTLA());
            Pointer dtla = DTLA.load(currentTLA());
            Pointer tlb = ttla.roundedDownBy(platform().pageSize);
            Address refMap = STACK_REFERENCE_MAP.load(currentTLA());
            Address tlbEnd = refMap.plus(STACK_REFERENCE_MAP_SIZE.load(currentTLA()).asAddress());
            int tlbSize = tlbEnd.minus(tlb).toInt();
            Log.println();
            Log.println("Thread locals block layout:");
            lastRegionStart = traceRegion("reference map", tlb, refMap, tlbEnd, lastRegionStart, tlbSize);
            lastRegionStart = traceRegion("native thread locals", tlb, ntl, refMap, lastRegionStart, tlbSize);
            lastRegionStart = traceRegion("safepoints-disabled TLA", tlb, dtla, ntl, lastRegionStart, tlbSize);
            lastRegionStart = traceRegion("safepoints-enabled TLA", tlb, etla, dtla, lastRegionStart, tlbSize);
            lastRegionStart = traceRegion("safepoints-triggered TLA", tlb, ttla, etla, lastRegionStart, tlbSize);
            lastRegionStart = traceRegion("unmapped page", tlb, tlb, ttla, lastRegionStart, tlbSize);

            Log.println();
            Log.printThreadLocals(tla, true);
            Log.unlock(lockDisabledSafepoints);
        }
    }

    private void traceThreadForUncaughtException(Throwable throwable) {
        if (TraceThreads) {
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
        if (TraceThreads) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Thread terminated [id=");
            Log.print(id);
            Log.print(", name=\"");
            Log.print(name);
            Log.println("\"]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    public final boolean join() {
        return nativeJoin(nativeThread);
    }

    // Only used by the EIR interpreter(s)
    @HOSTED_ONLY
    public void setTLA(Address address) {
        tla = address.asPointer();
    }

    @INLINE
    public final Pointer tla() {
        return tla;
    }

    public final JniHandle createLocalHandle(Object object) {
        if (jniHandles == null) {
            jniHandles = new JniHandles();
        }
        return JniHandles.createLocalHandle(jniHandles, object);
    }

    /**
     * Gets the JNI handles for this thread. This should only be called when the caller expects
     */
    @INLINE
    public final JniHandles jniHandles() {
        return jniHandles;
    }

    /**
     * Return the "top" (i.e. current size) of JNI handles for this thread
     *
     * NOTE: This code is called from a {@linkplain NativeStubGenerator JNI stub}
     *
     * @return -1 if the JNI handles have not been allocated for this thread
     */
    @INLINE
    public final int jniHandlesTop() {
        return jniHandles == null ? -1 : jniHandles.top();
    }

    /**
     * Resets the "top" (i.e. current size) of JNI handles for this thread
     *
     * NOTE: This code is called from a {@linkplain NativeStubGenerator JNI stub}
     *
     * @param newTop the value to which the top should be reset or -1 if no resetting is to occur
     */
    @INLINE
    public final void resetJniHandlesTop(int newTop) {
        if (newTop != -1) {
            jniHandles.resetTop(newTop);
        }
    }

    /**
     * Gets the JNI handles for this thread, creating them first if necessary.
     */
    @INLINE
    public final JniHandles makeJniHandles() {
        if (jniHandles == null) {
            jniHandles = new JniHandles();
        }
        return jniHandles;
    }

    /**
     * Sets or clears the exception to be thrown once this thread returns from the native function most recently entered
     * via a {@linkplain NativeStubGenerator native stub}. This mechanism is used to propagate exceptions through native
     * frames and should not be used for any other purpose.
     *
     * @param exception if non-null, this exception will be raised upon returning from the closest native function on
     *            this thread's stack. Otherwise, the pending JNI exception is cleared.
     */
    public final void setJniException(Throwable exception) {
        this.jniException = exception;
    }

    /**
     * Gets the exception thrown by the most recently called JNI function. This will be re-thrown once this
     * thread returns from the native function most recently entered via a {@linkplain NativeStubGenerator native stub}.
     *
     * @return the exception that will be raised upon returning from the closest native function on this thread's stack
     *         or null if there is no such pending exception
     */
    public final Throwable jniException() {
        return jniException;
    }

    /**
     * Raises the pending JNI exception on this thread (if any) and clears it.
     * Called from a {@linkplain NativeStubGenerator JNI stub} after a native function returns.
     */
    public final void throwJniException() throws Throwable {
        final Throwable pendingException = this.jniException;
        if (pendingException != null) {
            this.jniException = null;
            throw pendingException;
        }
    }

    /**
     * Gets the number of VM operations {@linkplain VmOperationThread#submit(VmOperation) submitted}
     * by this thread for execution that have not yet completed.
     */
    public int pendingOperations() {
        return pendingOperations;
    }

    /**
     * Increments the {@linkplain #pendingOperations() pending operations} count.
     */
    public void incrementPendingOperations() {
        ++pendingOperations;
    }

    /**
     * Decrements the {@linkplain #pendingOperations() pending operations} count.
     */
    public void decrementPendingOperations() {
        --pendingOperations;
        FatalError.check(pendingOperations >= 0, "pendingOperations should never be negative");
    }

    /**
     * Causes this thread to begin execution.
     */
    public final void start0() {
        assert state == Thread.State.NEW;
        state = Thread.State.RUNNABLE;
        VmThreadMap.ACTIVE.startThread(this, STACK_SIZE_OPTION.getValue().aligned(platform().pageSize).asSize(), javaThread.getPriority());
    }

    public final boolean isInterrupted(boolean clearInterrupted) {
        final boolean interrupted = this.interrupted;
        if (clearInterrupted) {
            this.interrupted = false;
        }
        return interrupted;
    }

    /**
     * This can be called in two contexts: 1. During the creation of a thread (i.e. during the Thread constructor) 2.
     * During the execution of a thread. In case 1 the native thread does not exist as it is not created until the
     * {@link #start0()} method is called, so there is nothing to do (the priority is passed down by start0). In case 2
     * we call a native function to change the priority.
     *
     * @param newPriority the new thread priority
     */
    public final void setPriority0(int newPriority) {
        if (nativeThread.isZero()) {
            // native thread does not exist yet
        } else {
            nativeSetPriority(nativeThread, newPriority);
        }
    }

    /*
     * use protected member method so that Maxine VE's SchedThread is able to implement its own sleep method
     */
    protected boolean sleep0(long numberOfMilliSeconds) {
        return VmThread.nativeSleep(numberOfMilliSeconds);
    }

    public final void stop0(Object throwable) {
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

    public final void interrupt0() {
        interrupted = true;
        if (!nativeThread.isZero()) {
            // Set to true as default. Will be cleared on this VmThread's
            // native thread if an InterruptedException is thrown after the
            // interruption.
            nativeInterrupt(nativeThread);
        }
    }

    @Override
    public String toString() {
        return "VM" + javaThread;
    }

    /**
     * Gets the address of the stack zone used to detect recoverable stack overflow.
     * This zone covers the range {@code [yellowZone() .. yellowZoneEnd())}.
     */
    public final Address yellowZone() {
        return yellowZone;
    }

    /**
     * Gets the end address of the stack zone used to detect recoverable stack overflow.
     * This zone covers the range {@code [yellowZone() .. yellowZoneEnd())}.
     */
    public final Address yellowZoneEnd() {
        return yellowZone.plus(yellowZoneSize());
    }

    /**
     * Gets the address of the stack zone used to detect unrecoverable stack overflow.
     * This zone is immediately below the {@linkplain #yellowZone yellow} zone and covers
     * the range {@code [redZone() .. redZoneEnd())}.
     */
    public final Address redZone() {
        return yellowZone.minus(redZoneSize());
    }

    /**
     * Gets the end address of the stack zone used to detect unrecoverable stack overflow.
     * This zone is immediately below the {@linkplain #yellowZone yellow} zone and covers
     * the range {@code [redZone() .. redZoneEnd())}.
     */
    public final Address redZoneEnd() {
        return yellowZone;
    }

    /**
     * This method is called when the VmThread initialization is complete.
     * A subclass can override this method to do whatever subclass-specific
     * initialization that depends on that invariant.
     */
    protected void initializationComplete() {
    }

    /**
     * This method is called when the VmThread termination is pending.
     * A subclass can override this method to do whatever subclass-specific
     * termination that depends on that invariant.
     * N.B. In order for this callback to be usable, the state is not
     * set to TERMINATED until after this method returns.
     */
    protected void terminationPending() {
    }

    /**
     * This method parks the current thread according to the semantics of {@link Unsafe#park()}.
     * @throws InterruptedException
     */
    public final void park() throws InterruptedException {
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
    public final void park(long wait) throws InterruptedException {
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
    public final void unpark() {
        synchronized (this) {
            parkState = 1;
            notifyAll();
        }
    }

    public void pushPrivilegedElement(ClassActor classActor, long frameId, AccessControlContext context) {
        privilegedStackTop = new PrivilegedElement(classActor, frameId, context, privilegedStackTop);
    }

    public void popPrivilegedElement() {
        privilegedStackTop = privilegedStackTop.next;
    }

    public PrivilegedElement getTopPrivilegedElement() {
        return privilegedStackTop;
    }

    public static class PrivilegedElement {
        private PrivilegedElement next;
        public ClassActor classActor;
        public long frameId;
        public AccessControlContext context;

        PrivilegedElement(ClassActor classActor, long frameId, AccessControlContext context, PrivilegedElement next) {
            this.classActor = classActor;
            this.frameId = frameId;
            this.context = context;
            this.next = next;
        }

    }
}
