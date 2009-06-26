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
package com.sun.max.tele.debug;

import static com.sun.max.tele.debug.ProcessState.*;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

import com.sun.max.collect.*;
import com.sun.max.gui.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.TeleNativeThread.*;
import com.sun.max.tele.page.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Models the remote process being controlled.
 *
 * @author Bernd Mathiske
 * @author Aritra Bandyopadhyay
 * @author Doug Simon
 * @author Michael Van De Vanter
 * @author Hannes Payer
 */
public abstract class TeleProcess extends AbstractTeleVMHolder implements TeleIO {

    private static final int TRACE_VALUE = 2;

    private static final Sequence<TeleNativeThread> EMPTY_THREAD_SEQUENCE = Sequence.Static.empty(TeleNativeThread.class);

    public static final String[] NO_COMMAND_LINE_ARGUMENTS = {};

    @Override
    protected String  tracePrefix() {
        return "[TeleProcess: " + Thread.currentThread().getName() + "] ";
    }

    private TeleNativeThread lastSingleStepThread;

    public abstract DataAccess dataAccess();

    private final Platform platform;

    public Platform platform() {
        return platform;
    }

    private final TeleTargetBreakpoint.Factory targetBreakpointFactory;

    public TeleTargetBreakpoint.Factory targetBreakpointFactory() {
        return targetBreakpointFactory;
    }

    private final TeleWatchpoint.Factory watchpointFactory;

    public TeleWatchpoint.Factory watchpointFactory() {
        return watchpointFactory;
    }

    /**
     * The controller that controls access to this TeleProcess.
     */
    private final TeleProcessController controller;

    /**
     * The thread on which actions are performed when the process (or thread) stops after a request is issued to change
     * it's execution state.
     */
    public class RequestHandlingThread extends Thread {

        /**
         * The action to be performed when the process (or a thread in the process) stops as a result of the
         * currently executing process request.
         */
        private BlockingDeque<TeleEventRequest> requests = new LinkedBlockingDeque<TeleEventRequest>(10);

        RequestHandlingThread() {
            super("RequestHandlingThread");
            setDaemon(true);
        }

        /**
         * Waits until the tele process has stopped after it has been issued an execution request. The request's
         * post-execution action is then performed.
         * Finally, any threads waiting for an execution request to complete are notified.
         *
         * @param request
         */
        private void waitUntilProcessStopped(TeleEventRequest request) {
            assert requestHandlingThread == Thread.currentThread();
            Trace.begin(TRACE_VALUE, tracePrefix() + "waiting for execution to stop: " + request);
            try {
                boolean continuing;
                final AppendableSequence<TeleNativeThread> breakpointThreads = new LinkSequence<TeleNativeThread>();
                do {

                    continuing = false;
                    final boolean ok = waitUntilStopped();
                    if (!ok) {
                        Trace.end(TRACE_VALUE, tracePrefix() + "waiting for execution to stop: " + request + " (PROCESS TERMINATED)");
                        updateState(TERMINATED, EMPTY_THREAD_SEQUENCE);
                        return;
                    }

                    teleVM().refresh(epoch);
                    refreshThreads();
                    final Sequence<TeleTargetBreakpoint> deactivatedBreakpoints = targetBreakpointFactory().deactivateAll();
                    Trace.line(TRACE_VALUE, tracePrefix() + "Execution stopped: " + request);

                    for (TeleNativeThread thread : threads()) {
                        final TeleTargetBreakpoint breakpoint = thread.breakpoint();
                        if (breakpoint != null) {
                            // Check conditional breakpoint:
                            if (breakpoint.condition() != null && !breakpoint.condition().evaluate(TeleProcess.this, thread)) {
                                try {
                                    // Evade the breakpoint
                                    thread.evadeBreakpoint();

                                    // Re-activate all the de-activated breakpoints
                                    for (TeleTargetBreakpoint bp : deactivatedBreakpoints) {
                                        bp.activate();
                                    }

                                    //updateState(RUNNING);
                                    Trace.line(TRACE_VALUE, tracePrefix() + "continuing after hitting unsatisfied conditional breakpoint");
                                    TeleProcess.this.resume();
                                    continuing = true;
                                } catch (OSExecutionRequestException executionRequestException) {
                                    Trace.line(TRACE_VALUE, tracePrefix() + "process terminated while attempting to step over unsatisfied conditional breakpoint");
                                    updateState(TERMINATED, EMPTY_THREAD_SEQUENCE);
                                    return;
                                }
                            } else {
                                breakpointThreads.append(thread);
                            }
                        }
                    }
                } while (continuing);
                Trace.end(TRACE_VALUE, tracePrefix() + "waiting for execution to stop: " + request);
                Trace.begin(TRACE_VALUE, tracePrefix() + "firing execution post-request action: " + request);
                request.notifyProcessStopped();
                Trace.end(TRACE_VALUE, tracePrefix() + "firing execution post-request action: " + request);
                updateState(STOPPED, breakpointThreads);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                ThrowableDialog.showLater(throwable, null, tracePrefix() + "Uncaught exception while processing " + request);
            } finally {
                Trace.begin(TRACE_VALUE, tracePrefix() + "notifying completion of request: " + request);
                request.notifyOfCompletion();
                Trace.end(TRACE_VALUE, tracePrefix() + "notifying completion of request: " + request);
            }
        }

        private String traceSuffix(boolean synchronous) {
            return " (" + (synchronous ? "synchronous" : "asynchronous") + ")";
        }

        /**
         * Schedule a request.
         * @param request the request
         * @param isSynchronous true if the request should be performed synchronously
         * @see TeleProcess#scheduleRequest(TeleEventRequest, boolean)
         */
        void scheduleRequest(TeleEventRequest request, boolean isSynchronous) {
            final Thread currentThread = Thread.currentThread();
            if (currentThread == this) {
                Trace.begin(TRACE_VALUE, tracePrefix() + "immediate execution request: " + traceSuffix(isSynchronous));
                execute(request, true);
                Trace.end(TRACE_VALUE, tracePrefix() + "immediate execution request: " + request + traceSuffix(isSynchronous));
            } else {
                try {
                    Trace.begin(TRACE_VALUE, tracePrefix() + "scheduled execution request: " + request + traceSuffix(isSynchronous));
                    requests.putFirst(request);
                    Trace.end(TRACE_VALUE, tracePrefix() + "scheduled execution request: " + request + traceSuffix(isSynchronous));
                } catch (InterruptedException interruptedException) {
                    ProgramWarning.message(tracePrefix() + "Could not schedule " + request + ": " + interruptedException);
                    return;
                }

                if (isSynchronous) {
                    waitForSynchronousRequestToComplete(request);
                }
            }
        }

        private void waitForSynchronousRequestToComplete(TeleEventRequest request) {
            Trace.begin(TRACE_VALUE, tracePrefix() + "waiting for synchronous request to complete: " + request);
            request.waitUntilComplete();
            Trace.end(TRACE_VALUE, tracePrefix() + "waiting for synchronous request to complete: " + request);
        }

        private void execute(TeleEventRequest request, boolean isNested) {
            if (!isNested && processState != STOPPED) {
                ProgramWarning.message(tracePrefix() + "Cannot execute \"" + request + "\" unless process state is " + STOPPED);
            } else {
                try {
                    lastSingleStepThread = null;
                    Trace.begin(TRACE_VALUE, tracePrefix() + "executing request: " + request);
                    updateState(RUNNING, EMPTY_THREAD_SEQUENCE);
                    request.execute();
                    Trace.end(TRACE_VALUE, tracePrefix() + "executing request: " + request);
                } catch (OSExecutionRequestException executionRequestException) {
                    executionRequestException.printStackTrace();
                    return;
                }
                waitUntilProcessStopped(request);
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    final TeleEventRequest request = requests.takeLast();
                    Trace.begin(TRACE_VALUE, tracePrefix() + "handling execution request: " + request);
                    execute(request, false);
                    Trace.end(TRACE_VALUE, tracePrefix() + "handling execution request: " + request);
                } catch (InterruptedException interruptedException) {
                    ProgramWarning.message(tracePrefix() + "Could not take request from sceduling queue: " + interruptedException);
                }
            }
        }
    }

    private final RequestHandlingThread requestHandlingThread;

    /**
     *
     * @return an object that gives access to process commands and state
     */
    public TeleProcessController controller() {
        return controller;
    }

    /**
     * Allocates and initializes a buffer in native memory to hold a given set of command line arguments. De-allocating
     * the memory for the buffer is the responsibility of the caller.
     *
     * @param programFile the executable that will be copied into element 0 of the returned buffer
     * @param commandLineArguments
     * @return a native buffer than can be cast to the C type {@code char**} and used as the first argument to a C
     *         {@code main} function
     */
    public static Pointer createCommandLineArgumentsBuffer(File programFile, String[] commandLineArguments) {
        final String[] strings = new String[commandLineArguments.length + 1];
        strings[0] = programFile.getAbsolutePath();
        System.arraycopy(commandLineArguments, 0, strings, 1, commandLineArguments.length);
        return CString.utf8ArrayFromStringArray(strings, true);
    }

    protected ProcessState processState;

    /**
     * @return the current state of the process
     */
    public final ProcessState processState() {
        return processState;
    }

    private long epoch;

    /**
     * Gets the current epoch: the number of requested execution steps of the process since it was created.
     *
     * @return the current epoch
     */
    public final long epoch() {
        return epoch;
    }

    /**
     * @param teleVM the VM with which the Process will be associated
     * @param platform
     * @param initialState Initial state of the process at creation.
     */
    protected TeleProcess(TeleVM teleVM, Platform platform, ProcessState initialState) {
        super(teleVM);
        this.platform = platform;
        this.processState = initialState;
        epoch = 0;
        this.targetBreakpointFactory = new TeleTargetBreakpoint.Factory(teleVM);
        this.watchpointFactory = new TeleWatchpoint.Factory(this);
        this.controller = new TeleProcessController(this);

        //Initiate the thread that continuously waits on the running process.
        this.requestHandlingThread = new RequestHandlingThread();
        this.requestHandlingThread.start();
    }

    /**
     * @return true if the tele is ready to execute a command that puts it in the {@link ProcessState#RUNNING} state.
     */
    public final boolean isReadyToRun() {
        return processState == STOPPED;
    }

    /**
     * Accepts a tele process execution request and schedules it for execution on the
     * {@linkplain #requestHandlingThread() request handling thread}. The request is executed immediately if the
     * {@linkplain Thread#currentThread() current} thread is the request handling thread. Otherwise, it will be executed
     * once any pending request has completed.
     *
     * @param request an execution request to schedule
     * @param synchronous if this value is true or the {@linkplain Thread#currentThread() current thread} is the
     *            {@linkplain #requestHandlingThread() request handling thread}, this method will block until the
     *            request's post execution action has
     *            completed. Otherwise, this method returns after scheduling the request and notifying the request
     *            handling thread.
     */
    public final void scheduleRequest(TeleEventRequest request, boolean synchronous) {
        requestHandlingThread.scheduleRequest(request, synchronous);
    }

    /**
     * Initializes the state history for the process.  Must be called after process created, but before
     * any requests.
     */
    public void initializeState() {
        teleVM().refresh(epoch);
//        refreshThreads();
//        assert _handleToThreadMap.size() == 1;
//        final TeleNativeThread teleNativeThread = _handleToThreadMap.get(_handleToThreadMap.firstKey());
//        assert teleNativeThread.breakpoint() != null;
//        final Sequence<TeleNativeThread> breakpointThreads = new ArrayListSequence<TeleNativeThread>(teleNativeThread);
//        updateState(_processState, breakpointThreads);
        updateState(processState, EMPTY_THREAD_SEQUENCE);
    }

    private void updateState(ProcessState newState, Sequence<TeleNativeThread> breakpointThreads) {
        processState = newState;
        if (newState == TERMINATED) {
            this.threadsDied.addAll(handleToThreadMap.values());
        }
        for (TeleNativeThread thread : this.threadsDied) {
            thread.setDead();
            Trace.line(TRACE_VALUE, "    "  + thread.toShortString() + " DEAD");
        }
        final Sequence<TeleNativeThread> threadsStarted =
            this.threadsStarted.isEmpty() ? EMPTY_THREAD_SEQUENCE : new ArrayListSequence<TeleNativeThread>(this.threadsStarted);
        final Sequence<TeleNativeThread> threadsDied =
            this.threadsDied.isEmpty() ? EMPTY_THREAD_SEQUENCE : new ArrayListSequence<TeleNativeThread>(this.threadsDied);
        this.threadsStarted.clear();
        this.threadsDied.clear();
        teleVM().notifyStateChange(processState, epoch, lastSingleStepThread, breakpointThreads, handleToThreadMap.values(), threadsStarted, threadsDied);
    }

    /**
     * Resumes this process.
     *
     * @throws OSExecutionRequestException if there was some problem while resuming this process
     */
    protected abstract void resume() throws OSExecutionRequestException;

    /**
     * Suspends this process.
     *
     * @throws InvalidProcessRequestException if the current process state is not {@link ProcessState#RUNNING}
     * @throws OSExecutionRequestException if there was some problem in executing the suspension
     */
    public final void pause() throws InvalidProcessRequestException, OSExecutionRequestException {
        if (processState != RUNNING) {
            throw new InvalidProcessRequestException("Can only suspend a running tele process, not a tele process that is " + processState.toString().toLowerCase());
        }
        suspend();
    }

    /**
     * Suspends this process.
     * @throws OSExecutionRequestException if the request could not be performed
     */
    protected abstract void suspend() throws OSExecutionRequestException;

    /**
     * Single steps a given thread.
     * @param thread the thread to single step
     * @throws OSExecutionRequestException if there was a problem issuing the single step
     */
    protected final void singleStep(TeleNativeThread thread) throws OSExecutionRequestException {
        if (!thread.singleStep()) {
            throw new OSExecutionRequestException("Error while single stepping thread " + thread);
        }
    }

    /**
     * Single steps a given thread.
     * @param teleNativeThread the thread to step
     * @throws OSExecutionRequestException if there was a problem issuing the single step
     */
    protected void performSingleStep(TeleNativeThread teleNativeThread) throws OSExecutionRequestException {
        this.lastSingleStepThread = teleNativeThread;
        singleStep(teleNativeThread);
    }


    public final void terminate() throws InvalidProcessRequestException, OSExecutionRequestException {
        if (processState == TERMINATED) {
            throw new InvalidProcessRequestException("Can only terminate a non-terminated tele process, not a tele process that is " + processState.toString().toLowerCase());
        }
        kill();
    }

    protected abstract void kill() throws OSExecutionRequestException;

    /**
     * Waits for this process to stop.
     *
     * @return true if the process stopped, false if there was an error
     */
    protected abstract boolean waitUntilStopped();

    private int transportDebugLevel = 0;

    /**
     * A subclass should override this method to set the tracing level of the underlying
     * transport mechanism used for communication with the target. The override should call
     * super.setTransportDebugLevel to cache the value here.
     * @param level new level
     */
    public void setTransportDebugLevel(int level) {
        transportDebugLevel = level;
    }

    public int transportDebugLevel() {
        return transportDebugLevel;
    }

    private Set<Long> instructionPointers = new HashSet<Long>();

    public final boolean isInstructionPointer(Address address) {
        return instructionPointers.contains(address.toLong());
    }

    private SortedMap<Long, TeleNativeThread> handleToThreadMap = new TreeMap<Long, TeleNativeThread>();

    private final Set<TeleNativeThread> threadsStarted = new TreeSet<TeleNativeThread>();

    private final Set<TeleNativeThread> threadsDied = new TreeSet<TeleNativeThread>();

    protected abstract void gatherThreads(AppendableSequence<TeleNativeThread> threads);

    protected abstract TeleNativeThread createTeleNativeThread(int id, long handle, long stackBase, long stackSize);

    /**
     * Callback from JNI: creates new thread object or updates existing thread object with same thread ID.
     *
     * @param threads the sequence being used to collect the threads
     * @param id the {@link VmThread#id() id} of the thread. If {@code id > 0}, then this thread corresponds to a
     *            {@link VmThread Java thread}. If {@code id == 0}, then this is the primordial thread. Otherwise, this
     *            is a native thread.
     * @param handle the native thread library {@linkplain TeleNativeThread#handle() handle} to this thread (e.g. the
     *            LWP of a Solaris thread)
     * @param state
     * @param instructionPointer the current value of the instruction pointer
     * @param stackBase the lowest known address of the stack. This may be 0 for an native thread that has not been
     *            attached to the VM via the AttachCurrentThread function that is part of the JNI Invocation API. .
     * @param stackSize the size of the stack in bytes
     * @param triggeredVmThreadLocals the address of the native memory holding the safepoints-triggered VM thread locals
     * @param enabledVmThreadLocals the address of the native memory holding the safepoints-enabled VM thread locals
     * @param disabledVmThreadLocals the address of the native memory holding the safepoints-disabled VM thread locals
     */
    public final void jniGatherThread(AppendableSequence<TeleNativeThread> threads, int id, long handle, int state, long instructionPointer, long stackBase, long stackSize,
                    long triggeredVmThreadLocals, long enabledVmThreadLocals, long disabledVmThreadLocals) {
        assert state >= 0 && state < ThreadState.VALUES.length() : state;
        TeleNativeThread thread = handleToThreadMap.get(handle);
        if (thread == null) {
            thread = createTeleNativeThread(id, handle, stackBase, stackSize);
        }

        final Map<Safepoint.State, Pointer> vmThreadLocals;
        if (id >= 0) {
            vmThreadLocals = new EnumMap<Safepoint.State, Pointer>(Safepoint.State.class);
            vmThreadLocals.put(Safepoint.State.ENABLED, Pointer.fromLong(enabledVmThreadLocals));
            vmThreadLocals.put(Safepoint.State.DISABLED, Pointer.fromLong(disabledVmThreadLocals));
            vmThreadLocals.put(Safepoint.State.TRIGGERED, Pointer.fromLong(triggeredVmThreadLocals));
        } else {
            vmThreadLocals = null;
        }

        thread.updateAfterGather(ThreadState.VALUES.get(state), Pointer.fromLong(instructionPointer), vmThreadLocals);
        threads.append(thread);
    }


    private void refreshThreads() {
        Trace.begin(TRACE_VALUE, tracePrefix() + "Refreshing remote threads:");
        final long startTimeMillis = System.currentTimeMillis();
        epoch++;
        final AppendableSequence<TeleNativeThread> currentThreads = new ArrayListSequence<TeleNativeThread>(handleToThreadMap.size());
        gatherThreads(currentThreads);

        final SortedMap<Long, TeleNativeThread> newHandleToThreadMap = new TreeMap<Long, TeleNativeThread>();
        final Set<Long> newInstructionPointers = new HashSet<Long>();

        // List all previously live threads as possibly dead; remove the ones discovered to be current.
        threadsDied.addAll(handleToThreadMap.values());

        for (TeleNativeThread thread : currentThreads) {

            // Refresh the thread
            thread.refresh(epoch());

            newHandleToThreadMap.put(thread.handle(), thread);
            final TeleNativeThread oldThread = handleToThreadMap.get(thread.handle());
            if (oldThread != null) {
                assert oldThread == thread;
                threadsDied.remove(thread);
                Trace.line(TRACE_VALUE, "    "  + thread);
            } else {
                threadsStarted.add(thread);
                Trace.line(TRACE_VALUE, "    "  + thread + " STARTED");
            }
            final Pointer instructionPointer = thread.instructionPointer();
            if (!instructionPointer.isZero()) {
                newInstructionPointers.add(thread.instructionPointer().toLong());
            }
        }
        Trace.end(TRACE_VALUE, tracePrefix() + "Refreshing remote threads:", startTimeMillis);
        handleToThreadMap = newHandleToThreadMap;
        instructionPointers = newInstructionPointers;
    }

    /**
     * Gets the set of all the threads in this process the last time it stopped.
     * The returned threads are sorted in ascending order of their {@linkplain TeleNativeThread#id() identifiers}.
     * @return the set of threads in the process
     */
    public final IterableWithLength<TeleNativeThread> threads() {
        return Iterables.toIterableWithLength(handleToThreadMap.values());
    }

    public final int pageSize() {
        return platform().pageSize;
    }

    public final int read(Address address, ByteBuffer buffer, int offset, int length) {
        if (processState == TERMINATED) {
            throw new DataIOError(address, "Attempt to read the memory when the process is in state " + TERMINATED);
        }
        if (processState != STOPPED && processState != null && Thread.currentThread() != requestHandlingThread) {
        //    ProgramWarning.message("Reading from process memory while processed not stopped [thread: " + Thread.currentThread().getName() + "]");
        }
        DataIO.Static.checkRead(buffer, offset, length);
        final int bytesRead = read0(address, buffer, offset, length);
        if (bytesRead < 0) {
            throw new DataIOError(address);
        }
        return bytesRead;
    }

    /**
     * Reads bytes from an address into a given byte buffer.
     *
     * Precondition:
     * {@code buffer != null && offset >= 0 && offset < buffer.capacity() && length >= 0 && offset + length <= buffer.capacity()}
     *
     * @param address the address from which reading should start
     * @param buffer the buffer into which the bytes are read
     * @param offset the offset in {@code buffer} at which the bytes are read
     * @param length the number of bytes to be read
     * @return the number of bytes read into {@code buffer} or -1 if there was an error while trying to read the data
     */
    protected abstract int read0(Address address, ByteBuffer buffer, int offset, int length);

    public final int write(ByteBuffer buffer, int offset, int length, Address address) {
        if (processState == TERMINATED) {
            throw new DataIOError(address, "Attempt to write to memory when the process is in state " + TERMINATED);
        }
        if (processState != STOPPED && processState != null && Thread.currentThread() != requestHandlingThread) {
            //ProgramWarning.message("Writing to process memory while processed not stopped [thread: " + Thread.currentThread().getName() + "]");
            /* Uncomment to trace the culprit
            try {
                throw new Exception();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            */
        }
        DataIO.Static.checkWrite(buffer, offset, length);
        return write0(buffer, offset, length, address);
    }

    /**
     * Writes bytes from a given byte buffer to a given address.
     *
     * Precondition:
     * {@code buffer != null && offset >= 0 && offset < buffer.capacity() && length >= 0 && offset + length <= buffer.capacity()}
     *
     * @param address the address at which writing should start
     * @param buffer the buffer from which the bytes are written
     * @param offset the offset in {@code buffer} from which the bytes are written
     * @param length the maximum number of bytes to be written
     * @return the number of bytes written to {@code address} or -1 if there was an error while trying to write the data
     */
    protected abstract int write0(ByteBuffer buffer, int offset, int length, Address address);

    public int maximumWatchpointCount() {
        return 0;
    }

    protected boolean activateWatchpoint(TeleWatchpoint teleWatchpoint) {
        return false;
    }

    protected boolean deactivateWatchpoint(MemoryRegion memoryRegion) {
        return false;
    }

    protected long readWatchpointAddress() {
        return 0;
    }

    protected int readWatchpointAccessCode() {
        return 0;
    }
}
