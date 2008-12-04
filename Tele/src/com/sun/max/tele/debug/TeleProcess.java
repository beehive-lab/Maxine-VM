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

import static com.sun.max.tele.debug.TeleProcess.State.*;

import java.io.*;
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

/**
 * Models the remote process being controlled.
 *
 * @author Bernd Mathiske
 * @author Aritra Bandyopadhyay
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public abstract class TeleProcess extends TeleVMHolder implements TeleIO {

    private static final int TRACE_VALUE = 2;

    @Override
    protected String  tracePrefix() {
        return "[TeleProcess: " + Thread.currentThread().getName() + "] ";
    }

    private TeleNativeThread _lastSingleStepThread;

    public abstract DataAccess dataAccess();

    private final Platform _platform;

    public Platform platform() {
        return _platform;
    }

    private final TeleTargetBreakpoint.Factory _targetBreakpointFactory;

    public TeleTargetBreakpoint.Factory targetBreakpointFactory() {
        return _targetBreakpointFactory;
    }

    public static final String[] NO_COMMAND_LINE_ARGUMENTS = {};

    private Pointer _commandLineBuffer;

    protected Pointer commandLineBuffer() {
        return _commandLineBuffer;
    }

    /**
     * The controller that controls access to this TeleProcess.
     */
    private final TeleProcessController _controller;

    /**
     * The thread on which actions are performed when the process (or thread) stops after a request is issued to change
     * it's execution state.
     */
    public class RequestHandlingThread extends Thread {

        /**
         * The action to be performed when the process (or a thread in the process) stops as a result of the
         * currently executing process request.
         */
        private BlockingDeque<TeleEventRequest> _requests = new LinkedBlockingDeque<TeleEventRequest>(10);

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
            assert _requestHandlingThread == Thread.currentThread();
            Trace.begin(TRACE_VALUE, tracePrefix() + "waiting for execution to stop: " + request);
            try {
                boolean continuing;
                final AppendableSequence<TeleNativeThread> breakpointThreads = new LinkSequence<TeleNativeThread>();
                do {

                    continuing = false;
                    final boolean ok = waitUntilStopped();
                    if (!ok) {
                        Trace.end(TRACE_VALUE, tracePrefix() + "waiting for execution to stop: " + request + " (PROCESS TERMINATED)");
                        updateState(TERMINATED);
                        return;
                    }

                    teleVM().refresh();
                    refreshThreads();
                    final Sequence<TeleTargetBreakpoint> deactivatedBreakpoints = targetBreakpointFactory().deactivateAll();
                    teleVM().fireThreadEvents();
                    Trace.line(TRACE_VALUE, tracePrefix() + "Execution stopped: " + request);

                    for (TeleNativeThread thread : teleProcess().threads()) {
                        final TeleTargetBreakpoint breakpoint = thread.breakpoint();
                        if (breakpoint != null) {
                            // Check conditional breakpoint:
                            if (breakpoint.condition() != null && !breakpoint.condition().evaluate(teleProcess(), thread)) {
                                try {
                                    // Evade the breakpoint
                                    thread.evadeBreakpoint();

                                    // Re-activate all the de-activated breakpoints
                                    for (TeleTargetBreakpoint bp : deactivatedBreakpoints) {
                                        bp.activate();
                                    }

                                    updateState(RUNNING);
                                    Trace.line(TRACE_VALUE, tracePrefix() + "continuing after hitting unsatisfied conditional breakpoint");
                                    TeleProcess.this.resume();
                                    continuing = true;
                                } catch (OSExecutionRequestException executionRequestException) {
                                    Trace.line(TRACE_VALUE, tracePrefix() + "process terminated while attempting to step over unsatisfied conditional breakpoint");
                                    updateState(TERMINATED);
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
                final TeleNativeThread singleStepThread = _lastSingleStepThread;
                updateState(STOPPED, breakpointThreads, singleStepThread);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                ThrowableDialog.showLater(throwable, null, tracePrefix() + "Uncaught exception while processing " + request);
            } finally {
                synchronized (_synchronizedWaitersLock) {
                    if (_synchronizedWaiters > 0) {
                        _synchronizedWaitersLock.notifyAll();
                    }
                }
            }
        }

        private int _synchronizedWaiters;
        private final Object _synchronizedWaitersLock = new Object();

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
                    _requests.putFirst(request);
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
            synchronized (_synchronizedWaitersLock) {
                _synchronizedWaiters++;
                Trace.begin(TRACE_VALUE, tracePrefix() + "waiting for synchronous request to complete: " + request);
                try {
                    // Wait for tele process event handling thread to handle this process request:
                    _synchronizedWaitersLock.wait();
                    _synchronizedWaiters--;
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                Trace.end(TRACE_VALUE, tracePrefix() + "waiting for synchronous request to complete: " + request);
            }
        }

        private void execute(TeleEventRequest request, boolean isNested) {
            if (!isNested && _state != STOPPED) {
                ProgramWarning.message(tracePrefix() + "Cannot execute \"" + request + "\" unless process state is " + STOPPED);
            } else {
                try {
                    _lastSingleStepThread = null;
                    Trace.begin(TRACE_VALUE, tracePrefix() + "executing request: " + request);
                    updateState(RUNNING);
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
                    final TeleEventRequest request = _requests.takeLast();
                    Trace.begin(TRACE_VALUE, tracePrefix() + "handling execution request: " + request);
                    execute(request, false);
                    Trace.end(TRACE_VALUE, tracePrefix() + "handling execution request: " + request);
                } catch (InterruptedException interruptedException) {
                    ProgramWarning.message(tracePrefix() + "Could not take request from sceduling queue: " + interruptedException);
                }
            }
        }
    }

    /**
     * Gets the thread on which all execution requests are executed.
     * @return the thread that is handling requests
     */
    public RequestHandlingThread requestHandlingThread() {
        return _requestHandlingThread;
    }

    private final RequestHandlingThread _requestHandlingThread;

    /**
     *
     * @return an object that gives access to process commands and state
     */
    public TeleProcessController controller() {
        return _controller;
    }

    protected TeleProcess(TeleVM teleVM, Platform platform, File programFile, String[] commandLineArguments) {
        super(teleVM);
        _platform = platform;
        _targetBreakpointFactory = new TeleTargetBreakpoint.Factory(this);
        _controller = new TeleProcessController(this);

        final String programPath = programFile.getAbsolutePath();

        final int argvSize = Word.size() * (2 + commandLineArguments.length);
        int bufferSize = argvSize + programPath.length() + 1;
        for (String commandLineArgument : commandLineArguments) {
            bufferSize += commandLineArgument.length() + 1;
        }
        _commandLineBuffer = Memory.mustAllocate(bufferSize);

        final Pointer argumentPointer = _commandLineBuffer;
        Pointer stringPointer = _commandLineBuffer.plus(argvSize);
        argumentPointer.setWord(0, stringPointer);
        stringPointer = CString.writeUtf8(programPath, stringPointer, programPath.length() + 1);
        int i = 1;
        while (i <= commandLineArguments.length) {
            argumentPointer.setWord(i, stringPointer);
            final String s = commandLineArguments[i - 1];
            /* FIXME: this will cause a buffer overflow since the total size of the command line arguments may be more than bufferSize
             *  Try a non-ascii command line strings  -- AZIZ*/
            stringPointer = CString.writeUtf8(s, stringPointer, s.length() + 1);
            i++;
        }
        argumentPointer.setWord(i, Word.zero());
        //Initiate the thread that continuously waits on the running process.
        _requestHandlingThread = new RequestHandlingThread();
        _requestHandlingThread.start();
    }


    /**
     * The possible states of a {@link TeleProcess}.
     */
    public enum State {
        STOPPED, RUNNING, TERMINATED
    }

    protected State _state = STOPPED;

    /**
     * Gets the current state of this process.
     * @return the current state of the process
     */
    public final State state() {
        return _state;
    }

    /**
     * @return true if the tele is ready to execute a command that puts it in the {@link State#RUNNING} state.
     */
    public boolean isReadyToRun() {
        return _state == State.STOPPED;
    }

    public interface StateTransitionListener {
        void handleStateTransition(StateTransitionEvent e);
    }

    public final class StateTransitionEvent {

        private State _oldState;
        private State _newState;
        private Sequence<TeleNativeThread> _breakpointThreads;
        private TeleNativeThread _singleStepThread;
        private long _epoch;

        private StateTransitionEvent(State oldState, State newState, Sequence<TeleNativeThread> breakpointThreads, TeleNativeThread singleStepThread, long epoch) {
            _oldState = oldState;
            _newState = newState;
            _breakpointThreads = breakpointThreads;
            _singleStepThread = singleStepThread;
            _epoch = epoch;
        }

        public TeleNativeThread singleStepThread() {
            return _singleStepThread;
        }

        public Sequence<TeleNativeThread> breakpointThreads() {
            return _breakpointThreads;
        }

        public State oldState() {
            return _oldState;
        }
        
        public State newState() {
            return _newState;
        }
        
        /**
         * @return the process epoch at the time of the state transition.
         */
        public long epoch() {
            return _epoch;
        }
        
        @Override
        public String toString() {
            return _oldState.name() + "-->" + _newState.name() + "(" + _epoch + ")";
        }
    }

    private VariableSequence<StateTransitionListener> _listeners = new ArrayListSequence<StateTransitionListener>();

    public void addStateListener(StateTransitionListener listener) {
        _listeners.append(listener);
    }

    public void removeStateListener(StateTransitionListener listener) {
        final int index = Sequence.Static.indexOfIdentical(_listeners, listener);
        if (index != -1) {
            _listeners.remove(index);
        }
    }

    private void notifyStateChange(StateTransitionEvent event) {
        for (final StateTransitionListener listener : _listeners) {
            listener.handleStateTransition(event);
        }
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
    public void scheduleRequest(TeleEventRequest request, boolean synchronous) {
        _requestHandlingThread.scheduleRequest(request, synchronous);
    }

    private void updateState(State newState, Sequence<TeleNativeThread> breakpointThreads, TeleNativeThread singleStepThread) {
        final State oldState = _state;
        if (oldState != newState) {
            _state = newState;
            notifyStateChange(new StateTransitionEvent(oldState, newState, breakpointThreads, singleStepThread, epoch()));
        }
    }

    private void updateState(State newState) {
        updateState(newState, new LinkSequence<TeleNativeThread>(), null);
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
     * @throws InvalidProcessRequestException if the current process state is not {@link State#RUNNING}
     * @throws OSExecutionRequestException if there was some problem in executing the suspension
     */
    public final void pause() throws InvalidProcessRequestException, OSExecutionRequestException {
        if (_state != RUNNING) {
            throw new InvalidProcessRequestException("Can only suspend a running tele process, not a tele process that is " + _state.toString().toLowerCase());
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
        this._lastSingleStepThread = teleNativeThread;
        singleStep(teleNativeThread);
    }


    public final void terminate() throws InvalidProcessRequestException, OSExecutionRequestException {
        if (_state == TERMINATED) {
            throw new InvalidProcessRequestException("Can only terminate a non-terminated tele process, not a tele process that is " + _state.toString().toLowerCase());
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

    private int _transportDebugLevel = 0;

    /**
     * A subclass should override this method to set the tracing level of the underlying
     * transport mechanism used for communication with the target. The override should call
     * super.setTransportDebugLevel to cache the value here.
     * @param level new level
     */
    public void setTransportDebugLevel(int level) {
        _transportDebugLevel = level;
    }

    public int transportDebugLevel() {
        return _transportDebugLevel;
    }

    private Set<Long> _instructionPointers = new HashSet<Long>();

    public final boolean isInstructionPointer(Address address) {
        return _instructionPointers.contains(address.toLong());
    }

    private SortedMap<Long, TeleNativeThread> _threadMap = new TreeMap<Long, TeleNativeThread>();

    private final Set<TeleNativeThread> _startedThreads = new TreeSet<TeleNativeThread>();

    private final Set<TeleNativeThread> _deadThreads = new TreeSet<TeleNativeThread>();

    protected abstract boolean gatherThreads(AppendableSequence<TeleNativeThread> threads);

    private long _epoch;

    /**
     * Gets the current epoch: the number of discrete execution steps of the process since it was created.
     * @return the current epoch
     */
    public long epoch() {
        return _epoch;
    }

    /**
     * Sets the {@linkplain #primordialThread() primordial thread} if it has already been set.
     *
     * @param thread the primordial thread
     */
    protected void initPrimordialThread(TeleNativeThread thread) {
        if (_primordialThread == null) {
            _primordialThread = thread;
        }
    }

    private void refreshThreads() {
    	_epoch++;
        final AppendableSequence<TeleNativeThread> threads = new ArrayListSequence<TeleNativeThread>(_threadMap.size());
        gatherThreads(threads);

        if (!threads.isEmpty()) {
            initPrimordialThread(threads.first());
        }

        final SortedMap<Long, TeleNativeThread> newThreadMap = new TreeMap<Long, TeleNativeThread>();
        final Set<Long> newInstructionPointers = new HashSet<Long>();

        _startedThreads.clear();
        _deadThreads.clear();
        _deadThreads.addAll(_threadMap.values());
        Trace.begin(TRACE_VALUE, tracePrefix() + "Refreshing remote threads:");
        for (TeleNativeThread thread : threads) {

            // Refresh the thread
            thread.refresh(epoch());

            newThreadMap.put(thread.id(), thread);
            final TeleNativeThread oldThread = _threadMap.get(thread.id());
            if (oldThread != null) {
                assert oldThread == thread;
                _deadThreads.remove(thread);
                Trace.line(TRACE_VALUE, "    "  + thread);
            } else {
                _startedThreads.add(thread);
                Trace.line(TRACE_VALUE, "    "  + thread + " STARTED");
            }
            final Pointer instructionPointer = thread.instructionPointer();
            if (!instructionPointer.isZero()) {
                newInstructionPointers.add(thread.instructionPointer().toLong());
            }
        }
        for (TeleNativeThread thread : _deadThreads) {
            thread.setState(ThreadState.DEAD);
            thread.refresh(-1);
            Trace.line(TRACE_VALUE, "    "  + thread + " DEAD");
        }
        Trace.end(TRACE_VALUE, tracePrefix() + "Refreshing remote threads:");
        _threadMap = newThreadMap;
        _instructionPointers = newInstructionPointers;
    }

    /**
     * Gets the set of all the threads in this process the last time it stopped.
     * The returned threads are sorted in ascending order of their {@linkplain TeleNativeThread#id() identifiers}.
     * @return the set of threads in the process
     */
    public final IterableWithLength<TeleNativeThread> threads() {
        return Iterables.toIterableWithLength(_threadMap.values());
    }

    /**
     * Gets the set of all the threads that died in the epoch that completed last time this process stopped.
     * @return the set of dead threads
     */
    public final IterableWithLength<TeleNativeThread> deadThreads() {
        return Iterables.toIterableWithLength(_deadThreads);
    }

    /**
     * Gets the set of all the threads that were started in the epoch that completed last time this process stopped.
     * @return the set of started threads
     */
    public final IterableWithLength<TeleNativeThread> startedThreads() {
        return Iterables.toIterableWithLength(_startedThreads);
    }

    public final TeleNativeThread idToThread(long id) {
        return _threadMap.get(id);
    }

    private TeleNativeThread _primordialThread;

    public final TeleNativeThread primordialThread() {
        return _primordialThread;
    }
    
    /**
     * Gets the thread whose stack contains the memory location, null if none.
     */
    public final TeleNativeThread threadContaining(Address address) {
    	for (TeleNativeThread thread : _threadMap.values()) {
    		if (thread.stack().contains(address)) {
    			return thread;
    		}
    	}
    	return null;
    }

    public final int pageSize() {
        return platform().pageSize();
    }

    public final int read(Address address, byte[] buffer, int offset, int length) {
        if (_state == TERMINATED) {
            throw new DataIOError(address, "Attempt to read the memory when the process is in state " + TERMINATED);
        }
        if (_state != STOPPED && _state != null && Thread.currentThread() != _requestHandlingThread) {
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
     * Reads bytes from an address into a given byte array.
     *
     * Precondition:
     * {@code buffer != null && offset >= 0 && offset < buffer.length && length >= 0 && offset + length <= buffer.length}
     *
     * @param address the address from which reading should start
     * @param buffer the array into which the bytes are read
     * @param offset the offset in {@code buffer} at which the bytes are read
     * @param length the number of bytes to be read
     * @return the number of bytes read into {@code buffer} or -1 if there was an error while trying to read the data
     */
    protected abstract int read0(Address address, byte[] buffer, int offset, int length);

    public final int write(byte[] buffer, int offset, int length, Address address) {
        if (_state == TERMINATED) {
            throw new DataIOError(address, "Attempt to write to memory when the process is in state " + TERMINATED);
        }
        if (_state != STOPPED && _state != null && Thread.currentThread() != _requestHandlingThread) {
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
     * Writes bytes from a given byte array to a given address.
     *
     * Precondition:
     * {@code buffer != null && offset >= 0 && offset < buffer.length && length >= 0 && offset + length <= buffer.length}
     *
     * @param address the address at which writing should start
     * @param buffer the array from which the bytes are written
     * @param offset the offset in {@code buffer} from which the bytes are written
     * @param length the maximum number of bytes to be written
     * @return the number of bytes written to {@code address} or -1 if there was an error while trying to write the data
     */
    protected abstract int write0(byte[] buffer, int offset, int length, Address address);
}
