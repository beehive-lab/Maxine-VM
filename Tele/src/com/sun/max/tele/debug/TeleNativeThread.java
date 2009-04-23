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

import static com.sun.max.tele.debug.TeleNativeThread.ThreadState.*;

import java.util.*;
import java.util.logging.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.TeleProcess.*;
import com.sun.max.tele.debug.TeleTargetBreakpoint.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.LocalVariableTable.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetLocation.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Represents a thread executing in a {@linkplain TeleProcess tele process}. The thread
 *
 * @author Bernd Mathiske
 * @author Aritra Bandyopadhyay
 * @author Doug Simon
 */
public abstract class TeleNativeThread implements Comparable<TeleNativeThread>, ThreadProvider, TeleVMHolder {

    protected String  tracePrefix() {
        return "[TeleNativeThread: " + Thread.currentThread().getName() + "] ";
    }

    private static final int REFRESH_TRACE_LEVEL = 2;

    /**
     * The states a thread can be in.
     * N.B. Many platforms will not be able to detect all these states, e.g., MONITOR_WAIT,
     * in which case the generic SUSPENDED is appropriate.
     *
     * Note: This enum must be kept in sync the one of the same name in MaxineNative/inspector/teleNativeThread.h.
     */
    public enum ThreadState {
        /**
         * Denotes that a thread is waiting to acquire ownership of a monitor.
         */
        MONITOR_WAIT("Monitor", true),

        /**
         * Denotes that a thread is waiting to be {@linkplain Object#notify() notified}.
         */
        NOTIFY_WAIT("Wait", true),

        /**
         * Denotes that a thread is waiting for another
         * {@linkplain Thread#join(long, int) thread to die or a timer to expire}.
         */
        JOIN_WAIT("Join", true),

        /**
         * Denotes that a thread is {@linkplain Thread#sleep(long) sleeping}.
         */
        SLEEPING("Sleeping", true),

        /**
         * Denotes that a thread is suspended at a breakpoint.
         */
        BREAKPOINT("Breakpoint", true),

        /**
         * A thread is suspended at a watchpoint.
         */
        WATCHPOINT("Watchpoint", true),

        /**
         * Denotes that a thread is suspended for some reason other than {@link #MONITOR_WAIT}, {@link #NOTIFY_WAIT},
         * {@link #JOIN_WAIT}, {@link #SLEEPING} or {@link #BREAKPOINT}.
         */
        SUSPENDED("Suspended", true),

        DEAD("Dead", false),

        /**
         * Denotes that a thread is not suspended.
         */
        RUNNING("Running", false);

        public static final IndexedSequence<ThreadState> VALUES = new ArraySequence<ThreadState>(values());

        private final String _asString;
        private final boolean _allowsDataAccess;

        ThreadState(String asString, boolean allowsDataAccess) {
            _asString = asString;
            _allowsDataAccess = allowsDataAccess;
        }

        @Override
        public String toString() {
            return _asString;
        }

        /**
         * Determines whether a thread in this state allows thread specific data to be accessed in the remote process.
         * Thread specific data includes register values, stack memory, and {@linkplain VmThreadLocal VM thread locals}.
         */
        public final boolean allowsDataAccess() {
            return _allowsDataAccess;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(TeleNativeThread.class.getName());

    private final TeleProcess _teleProcess;
    private final TeleVM _teleVM;
    private TeleVmThread _teleVmThread;
    private int _suspendCount;

    private long _registersEpoch;
    private final TeleIntegerRegisters _integerRegisters;
    private final TeleStateRegisters _stateRegisters;
    private final TeleFloatingPointRegisters _floatingPointRegisters;

    /**
     * A cached stack trace for this thread.
     */
    private Sequence<StackFrame> _frames;

    /**
     * Only if this value is less than the {@linkplain TeleProcess#epoch() epoch} of this thread's tele process, does
     * the {@link #refreshFrames(boolean)} method do anything.
     */
    private long _framesEpoch;
    private boolean _framesChanged;

    /**
     * The stack of a Java thread; {@code null} if this is a non-Java thread.
     */
    private final TeleNativeStack _stack;

    private final Map<Safepoint.State, TeleVMThreadLocalValues> _teleVmThreadLocals;

    /**
     * Only if this value is less than the {@linkplain TeleProcess#epoch() epoch} of this thread's tele process, does
     * the {@link #refreshThreadLocals()} method do anything.
     */
    private long _teleVmThreadLocalsEpoch;

    private ThreadState _state = SUSPENDED;
    private TeleTargetBreakpoint _breakpoint;
    private FrameProvider[] _frameCache;

    /**
     * This thread's {@linkplain VmThread#id() identifier}.
     */
    private final int _id;

    private final long _handle;

    protected TeleNativeThread(TeleProcess teleProcess, int id, long handle, long stackBase, long stackSize) {
        _teleProcess = teleProcess;
        _teleVM = teleProcess.teleVM();
        _id = id;
        _handle = handle;
        final VMConfiguration vmConfiguration = _teleVM.vmConfiguration();
        _integerRegisters = new TeleIntegerRegisters(vmConfiguration);
        _floatingPointRegisters = new TeleFloatingPointRegisters(vmConfiguration);
        _stateRegisters = new TeleStateRegisters(vmConfiguration);

        _teleVmThreadLocals = id >= 0 ? new EnumMap<Safepoint.State, TeleVMThreadLocalValues>(Safepoint.State.class) : null;
        _stack = new TeleNativeStack(this, Address.fromLong(stackBase), Size.fromLong(stackSize));
        _breakpointIsAtInstructionPointer = vmConfiguration.platform().processorKind().instructionSet() == InstructionSet.SPARC;
    }

    /**
     * Gets the frames of this stack as produced by a {@linkplain StackFrameWalker stack walk} when the associated
     * thread last stopped.
     */
    public Sequence<StackFrame> frames() {
        refreshFrames(false);
        return _frames;
    }

    /**
     * Have the frames of this stack changed in the epoch that completed last time this thread stopped.
     * This may be true even if the objects representing the frames have changed.
     * @see StackFrame#isSameFrame(StackFrame)
     */
    public boolean framesChanged() {
        refreshFrames(false);
        return _framesChanged;
    }

    public TeleProcess teleProcess() {
        return _teleProcess;
    }

    public TeleVM teleVM() {
        return _teleVM;
    }

    public boolean hasThreadLocals() {
        return _teleVmThreadLocals != null;
    }

    /**
     * Gets the VM thread locals corresponding to a given safepoint state.
     */
    public TeleVMThreadLocalValues threadLocalsFor(Safepoint.State state) {
        assert hasThreadLocals();
        refreshThreadLocals();
        return _teleVmThreadLocals.get(state);
    }

    public TeleIntegerRegisters integerRegisters() {
        refreshRegisters();
        return _integerRegisters;
    }

    public TeleFloatingPointRegisters floatingPointRegisters() {
        refreshRegisters();
        return _floatingPointRegisters;
    }

    public TeleStateRegisters stateRegisters() {
        refreshRegisters();
        return _stateRegisters;
    }

    /**
     * Updates this thread's state. This does not cause a {@linkplain #refresh(long) refresh}.
     *
     * @param state the new state of this thread
     */
    public final void setState(ThreadState state) {
        _state = state;
    }

    /**
     * Updates this thread with the information information made available while
     * {@linkplain TeleProcess#gatherThreads(AppendableSequence) gathering} threads. This information is made available
     * by the native tele layer as threads are discovered. Subsequent refreshing of cached thread state (such a
     * {@linkplain #refreshRegisters() registers}, {@linkplain #refreshFrames(boolean) stack frames} and
     * {@linkplain #refreshThreadLocals() VM thread locals}) depends on this information being available and up to date.
     *
     * @param state the state of the thread
     * @param instructionPointer the current value of the instruction pointer for the thread
     * @param vmThreadLocals the address of the various VM thread locals storage areas
     */
    public final void updateAfterGather(ThreadState state, Pointer instructionPointer, Map<Safepoint.State, Pointer> vmThreadLocals) {
        _state = state;
        _stateRegisters.setInstructionPointer(instructionPointer);
        if (vmThreadLocals != null) {
            assert hasThreadLocals();
            for (Safepoint.State safepointState : Safepoint.State.CONSTANTS) {
                final Pointer vmThreadLocalsPointer = vmThreadLocals.get(safepointState);
                if (vmThreadLocalsPointer.isZero()) {
                    _teleVmThreadLocals.put(safepointState, null);
                } else {
                    // Only create a new TeleVMThreadLocalValues if the start address has changed which
                    // should only happen once going from 0 to a non-zero value.
                    final TeleVMThreadLocalValues teleVMThreadLocalValues = _teleVmThreadLocals.get(safepointState);
                    if (teleVMThreadLocalValues == null || !teleVMThreadLocalValues.start().equals(vmThreadLocalsPointer)) {
                        _teleVmThreadLocals.put(safepointState, new TeleVMThreadLocalValues(safepointState, vmThreadLocalsPointer));
                    }
                }
            }
        }
    }

    /**
     * Updates the addresses of this thread's state VM thread locals.
     * This does not cause a {@linkplain #refresh(long) refresh}.
     */
    public final void setVmThreadLocals(Map<Safepoint.State, Pointer> vmThreadLocals) {
        assert hasThreadLocals();
        for (Safepoint.State safepointState : Safepoint.State.CONSTANTS) {
            final Pointer vmThreadLocalsPointer = vmThreadLocals.get(safepointState);
            if (vmThreadLocalsPointer.isZero()) {
                _teleVmThreadLocals.put(safepointState, null);
            } else {
                // Only create a new TeleVMThreadLocalValues if the start address has changed which
                // should only happen once going from 0 to a non-zero value.
                final TeleVMThreadLocalValues teleVMThreadLocalValues = _teleVmThreadLocals.get(safepointState);
                if (teleVMThreadLocalValues == null || !teleVMThreadLocalValues.start().equals(vmThreadLocalsPointer)) {
                    _teleVmThreadLocals.put(safepointState, new TeleVMThreadLocalValues(safepointState, vmThreadLocalsPointer));
                }
            }
        }
    }

    /**
     * Refreshes the contents of this object to reflect the data of the corresponding thread in the tele process.
     * If {@code this.state() == DEAD}, then this object represents a thread that has died in the tele process and
     * all state is flushed accordingly.
     *
     * @param epoch the new epoch of this thread
     */
    public final void refresh(long epoch) {
        Trace.line(REFRESH_TRACE_LEVEL, tracePrefix() + "refresh(epoch=" + epoch + ") for " + this);
        if (_state == DEAD) {
            refreshFrames(true);
            _breakpoint = null;
            _teleVmThread = null;
            return;
        }

        if (_state.allowsDataAccess()) {
            refreshBreakpoint();
        }
    }

    /**
     * Refreshes the cached state of this thread's registers from the corresponding thread in the tele process.
     */
    private synchronized void refreshRegisters() {
        final long processEpoch = teleProcess().epoch();
        if (_registersEpoch < processEpoch) {
            _registersEpoch = processEpoch;
            Trace.line(REFRESH_TRACE_LEVEL, tracePrefix() + "refreshRegisters (epoch=" + processEpoch + ") for " + this);

            if (!readRegisters(_integerRegisters.registerData(), _floatingPointRegisters.registerData(), _stateRegisters.registerData())) {
                throw new TeleError("Error while updating registers for thread: " + this);
            }
            _integerRegisters.refresh();
            _floatingPointRegisters.refresh();
            _stateRegisters.refresh();
        }
    }

    /**
     * Refreshes the values of the cached VM thread local variables for this thread.
     * This method also updates the reference to the {@linkplain #teleVmThread() tele VmThread}.
     */
    private synchronized void refreshThreadLocals() {
        if (_teleVmThreadLocals == null) {
            return;
        }
        final long processEpoch = teleProcess().epoch();
        if (_teleVmThreadLocalsEpoch < processEpoch) {
            _teleVmThreadLocalsEpoch = processEpoch;

            Trace.line(REFRESH_TRACE_LEVEL, tracePrefix() + "refreshThreadLocals (epoch=" + processEpoch + ") for " + this);

            final DataAccess dataAccess = teleProcess().dataAccess();
            for (TeleVMThreadLocalValues teleVmThreadLocalValues : _teleVmThreadLocals.values()) {
                if (teleVmThreadLocalValues != null) {
                    teleVmThreadLocalValues.refresh(dataAccess);
                }
            }

            final TeleVMThreadLocalValues enabledVmThreadLocalValues = threadLocalsFor(Safepoint.State.ENABLED);
            _teleVmThread = null;
            if (enabledVmThreadLocalValues != null) {
                final Long threadLocalValue = enabledVmThreadLocalValues.get(VmThreadLocal.VM_THREAD);
                if (threadLocalValue != 0) {
                    final Reference vmThreadReference = _teleVM.wordToReference(Address.fromLong(threadLocalValue));
                    _teleVmThread = (TeleVmThread) _teleVM.makeTeleObject(vmThreadReference);
                }
            }
        }
    }

    /**
     * Refreshes the information about the {@linkplain #breakpoint() breakpoint} this thread is currently stopped at (if
     * any). If this thread is stopped at a breakpoint, its instruction pointer is adjusted so that it is at the
     * instruction on which the breakpoint was set.
     */
    private void refreshBreakpoint() {
        final Factory breakpointFactory = teleProcess().targetBreakpointFactory();
        TeleTargetBreakpoint breakpoint = null;



        try {
            breakpointFactory.registerBreakpointSetByVM(this);

            final Pointer breakpointAddress = breakpointAddressFromInstructionPointer();
            breakpoint = breakpointFactory.getBreakpointAt(breakpointAddress);
        } catch (DataIOError dataIOError) {
            // This is a catch for problems getting accurate state for threads that are not at breakpoints
        }
        if (breakpoint != null) {

            Trace.line(REFRESH_TRACE_LEVEL, tracePrefix() + "refreshingBreakpoint (epoch=" + teleProcess().epoch() + ") for " + this);

            _state = BREAKPOINT;
            _breakpoint = breakpoint;
            if (updateInstructionPointer(_breakpoint.address())) {
                _stateRegisters.setInstructionPointer(_breakpoint.address());
                Trace.line(REFRESH_TRACE_LEVEL, tracePrefix() + "refreshingBreakpoint (epoch=" + teleProcess().epoch() + ") IP updated for " + this);
            } else {
                throw new TeleError("Error updating instruction pointer to adjust thread after breakpoint at " + _breakpoint.address() + " was hit: " + this);
            }
        } else {
            _breakpoint = null;
            assert _state != BREAKPOINT;
        }
    }

    /**
     * Update the current list of frames.
     * As a side effect, set {@link #_framesChanged} to true if the identify of the stack frames has change,
     * even if the objects representing them are different.
     * @param clear the current list of frames should be cleared
     */
    private synchronized void refreshFrames(boolean clear) {
        if (clear) {
            _frames = Sequence.Static.empty(StackFrame.class);
            _framesChanged = true;
            return;
        }

        final long processEpoch = teleProcess().epoch();
        if (_framesEpoch < processEpoch) {
            _framesEpoch = processEpoch;

            Trace.line(REFRESH_TRACE_LEVEL, tracePrefix() + "refreshFrames (epoch=" + processEpoch + ") for " + this);

            // The stack walk requires the VM thread locals to be up to date
            //refreshThreadLocals();

            final TeleVM teleVM = _teleProcess.teleVM();
            final Sequence<StackFrame> frames = new TeleStackFrameWalker(teleVM, this).frames();
            assert !frames.isEmpty();
            if (_frames != null && frames.length() == _frames.length()) {
                _framesChanged = false;
                final Iterator<StackFrame> oldFrames = _frames.iterator();
                final Iterator<StackFrame> newFrames = frames.iterator();
                while (oldFrames.hasNext()) {
                    final StackFrame oldFrame = oldFrames.next();
                    final StackFrame newFrame = newFrames.next();
                    if (!oldFrame.isSameFrame(newFrame)) {
                        _framesChanged = true;
                        break;
                    }
                }
            } else {
                _framesChanged = true;
            }
            _frames = frames;
        }
    }

    /**
     * Gets the breakpoint this thread is currently stopped at (if any).
     *
     * @return null if this thread is not stopped at a breakpoint
     */
    public final TeleTargetBreakpoint breakpoint() {
        return _breakpoint;
    }

    /**
     * Specifies whether or not the instruction pointer needs to be adjusted when this thread hits a breakpoint to
     * denote the instruction pointer for which the breakpoint was set. For example, on x86 architectures, the
     * instruction pointer is at the instruction following the breakpoint instruction whereas on SPARC, it's
     * at the instruction pointer for which the breakpoint was set.
     */
    private boolean _breakpointIsAtInstructionPointer;

    public final ThreadState state() {
        return _state;
    }

    /**
     * Determines if this is the primordial VM thread. The primordial VM thread is the native thread on which the VM was
     * launched. It is not associated with a {@link VmThread} instance but does have {@linkplain #hasThreadLocals() VM
     * thread locals}.
     */
    public final boolean isPrimordial() {
        return id() == 0;
    }

    /**
     * Gets the identifier passed to {@link VmThread#run} when the thread was started. Note that this is different from
     * the platform dependent thread {@linkplain #handle() handle}.
     *
     * @return the id of this thread. A value less than, equal to or greater than 0 denotes a native thread, the
     *         primoridial thread or a Java thread respectively.
     */
    public final int id() {
        return _id;
    }

    /**
     * Gets the platform dependent handle to the native thread data structure. For example, on Solaris this will be
     * the LWP identifier for this thread. This value is guaranteed to be unique for any running thread.
     */
    public final long handle() {
        return _handle;
    }

    public final Pointer stackPointer() {
        refreshRegisters();
        return _integerRegisters.stackPointer();
    }

    public final Pointer framePointer() {
        refreshRegisters();
        return _integerRegisters.framePointer();
    }

    /**
     * Gets the stack for this thread.
     */
    public final TeleNativeStack stack() {
        return _stack;
    }

    public final Pointer instructionPointer() {
        // No need to call refreshRegisters(): the instruction pointer is updated by updateAfterGather() which
        // ensures that it is always in sync.
        return _stateRegisters.instructionPointer();
    }

    /**
     * Updates the current value of the instruction pointer for this thread.
     *
     * @param address the address to which the instruction should be set
     * @return true if the instruction pointer was successfully updated, false otherwise
     */
    protected abstract boolean updateInstructionPointer(Address address);

    /**
     * Gets the return address of the next-to-top frame on the stack. This will be null in the case where this thread is
     * in native code that was entered via a native method annotated with {@link C_FUNCTION}. The stub for such methods
     * do not leave the breadcrumbs on the stack that record how to find caller frames.
     */
    public Pointer getReturnAddress() {
        final StackFrame topFrame = frames().first();
        final StackFrame topFrameCaller = topFrame.callerFrame();
        return topFrameCaller == null ? null : topFrameCaller.instructionPointer();
    }

    /**
     * Gets the surrogate for the {@link VmThread} in the tele process denoted by this object.
     * This method returns {@code null} for any of the following reasons:
     *
     * 1. The thread is a non-Java thread
     * 2. The thread is a Java thread that has not yet reached the execution point (somewhere in {@link VmThread#run})
     *    that initializes {@link VmThreadLocal#VM_THREAD}.
     *
     * @return null if this thread is not (yet) associated with VmThread
     */
    public TeleVmThread teleVmThread() {
        if (_teleVmThread == null) {
            refreshThreadLocals();
        }
        return _teleVmThread;
    }

    protected abstract boolean readRegisters(byte[] integerRegisters, byte[] floatingPointRegisters, byte[] stateRegisters);

    /**
     * Advances this thread to the next instruction. That is, makes this thread execute a single machine instruction.
     * Note that this method does not block waiting for the tele process to complete the step.
     *
     * @return true if the single step was issued successfully, false otherwise
     */
    public abstract boolean singleStep();

    public abstract boolean threadResume();

    public abstract boolean threadSuspend();

    /**
     * If this thread is currently at a {@linkplain #breakpoint() breakpoint} it is single stepped to the next
     * instruction.
     */
    public void evadeBreakpoint() throws OSExecutionRequestException {
        if (_breakpoint != null && !_breakpoint.isTransient()) {
            assert !_breakpoint.isActivated() : "Cannot single step at an activated breakpoint";
            teleProcess().singleStep(this);
        }
    }

    /**
     * Gets the address of the breakpoint instruction derived from the current instruction pointer. The current
     * instruction pointer is assumed to be at the architecture dependent location immediately after a breakpoint
     * instruction was executed.
     *
     * The implementation of this method in {@link TeleNativeThread} uses the convention for x86 architectures where the
     * the instruction pointer is at the instruction following the breakpoint instruction.
     */
    public Pointer breakpointAddressFromInstructionPointer() {
        final Pointer instructionPointer = instructionPointer();
        if (_breakpointIsAtInstructionPointer) {
            return instructionPointer();
        }
        return instructionPointer.minus(teleProcess().targetBreakpointFactory().codeSize());
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof TeleNativeThread) {
            final TeleNativeThread teleNativeThread = (TeleNativeThread) other;
            return handle() == teleNativeThread.handle();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) handle();
    }

    /**
     * Imposes a total ordering between threads based on their {@linkplain #id() identifiers}.
     */
    public int compareTo(TeleNativeThread other) {
        return Longs.compare(handle(), other.handle());
    }

    @Override
    public final String toString() {
        String name = isPrimordial() ? "primordial" : (_teleVmThread == null ? "native" : _teleVmThread.name());
        name +=
            "[id=" + id() +
            ",handle=" + handle() +
            ",state=" + _state +
            ",type=" + (isPrimordial() ? "primordial" : (isJava() ? "Java" : "native")) +
            ",ip=0x" + instructionPointer().toHexString();
        if (isJava()) {
            name +=
                ",stack_start=0x" + stack().start().toHexString() +
                ",stack_size=" + stack().size().toLong();
        }
        return name + "]";
    }

    /**
     * Determines if this thread is associated with a {@link VmThread} instance. Note that even if this method returns
     * {@code true}, the {@link #teleVmThread()} method will return {@code null} if the thread has not reached the
     * execution point in {@link VmThread#run} where the {@linkplain VmThreadLocal#VM_THREAD reference} to the
     * {@link VmThread} object has been initialized.
     */
    public final boolean isJava() {
        return _id > 0;
    }

    /**
     * Class representing a Java-level frame. It implements the interface the JDWP protocol is programmed against.
     */
    private class FrameProviderImpl implements FrameProvider {

        private TargetJavaFrameDescriptor _frameDescriptor;
        private ClassMethodActor _classMethodActor;
        private int _position;
        private StackFrame _stackFrame;
        private TeleTargetMethod _targetMethod;
        private long[] _rawValues;
        private VMValue[] _vmValues;
        private boolean _isTopFrame;

        public FrameProviderImpl(boolean isTopFrame, TeleTargetMethod targetMethod, StackFrame stackFrame, TargetJavaFrameDescriptor descriptor) {
            this(isTopFrame, targetMethod, stackFrame, descriptor, descriptor.classMethodActor(), 0); //descriptor.bytecodeLocation().());
        }

        public FrameProviderImpl(boolean isTopFrame, TeleTargetMethod targetMethod, StackFrame stackFrame, TargetJavaFrameDescriptor descriptor, ClassMethodActor classMethodActor, int position) {
            _stackFrame = stackFrame;
            _frameDescriptor = descriptor;
            _classMethodActor = classMethodActor;
            _position = position;
            _targetMethod = targetMethod;
            _isTopFrame = isTopFrame;

            if (classMethodActor.codeAttribute().lineNumberTable().entries().length > 0) {
                _position = classMethodActor.codeAttribute().lineNumberTable().entries()[0].position();
            } else {
                LOGGER.warning("No line number table information for method " + classMethodActor.name().toString());
                _position = -1;
            }
        }

        private void initValues() {
            final int length = _classMethodActor.codeAttribute().localVariableTable().entries().length;
            final long[] values = new long[length];
            final VMValue[] vmValues = new VMValue[length];

            for (LocalVariableTable.Entry entry : _classMethodActor.codeAttribute().localVariableTable().entries()) {
                final Value curValue = getValueImpl(entry.slot());
                vmValues[entry.slot()] = _teleVM.maxineValueToJDWPValue(curValue);

                if (curValue.kind() == Kind.REFERENCE) {
                    values[entry.slot()] = curValue.asReference().toOrigin().toLong();
                } else if (curValue.kind() == Kind.WORD) {
                    values[entry.slot()] = curValue.asWord().asPointer().toLong();
                }
            }

            _vmValues = vmValues;
            _rawValues = values;
        }


        private Value getValueImpl(int slot) {
            TargetLocation l = null;

            if (_frameDescriptor == null) {
                final TargetLocation[] targetLocations = _stackFrame.targetMethod().abi().getParameterTargetLocations(_stackFrame.targetMethod().classMethodActor().getParameterKinds());
                if (slot >= targetLocations.length) {
                    return IntValue.from(0xbadbabe);
                }
                l = targetLocations[slot];
            } else {

                if (slot >= _frameDescriptor.locals().length) {
                    return IntValue.from(0xbadbabe);
                }
                l = _frameDescriptor.locals()[slot];
            }

            System.out.println("STACKFRAME ACCESS at " + slot + ", target=" + l);

            final Entry entry = _classMethodActor.codeAttribute().localVariableTable().findLocalVariable(slot, _position);
            if (entry == null) {
                return LongValue.from(0xbadbabe);
            }
            final TypeDescriptor descriptor = entry.descriptor(_classMethodActor.codeAttribute().constantPool());
            final Kind kind = descriptor.toKind();

            if (l instanceof LocalStackSlot) {
                final LocalStackSlot localStackSlot = (LocalStackSlot) l;
                final int index = localStackSlot.index();
                final Pointer slotBase = _stackFrame.slotBase();
                final int offset = index * Word.size();

                return _teleVM.readValue(kind, slotBase, offset);

            } else if (l instanceof ParameterStackSlot) {

                final ParameterStackSlot parameterStackSlot = (ParameterStackSlot) l;
                final int index = parameterStackSlot.index();
                final Pointer slotBase = _stackFrame.slotBase();

                // TODO: Resolve this hack that uses a special function in the Java stack frame layout.

                final JavaStackFrame javaStackFrame = (JavaStackFrame) _stackFrame;
                int offset = index * Word.size() + javaStackFrame.layout().frameSize();
                offset += javaStackFrame.layout().isReturnAddressPushedByCall() ? Word.size() : 0;

                return _teleVM.readValue(kind, slotBase, offset);

            } else if (l instanceof IntegerRegister) {
                final IntegerRegister integerRegister = (IntegerRegister) l;
                final int integerRegisterIndex = integerRegister.index();
                final Address address = integerRegisters().get(integerRegisterIndex);

                if (kind == Kind.REFERENCE) {
                    return TeleReferenceValue.from(_teleVM, Reference.fromOrigin(address.asPointer()));
                }
                return LongValue.from(address.toLong());
            }

            return IntValue.from(5);
        }

        public TargetMethodAccess getTargetMethodProvider() {
            return _targetMethod;
        }

        public CodeLocation getLocation() {
            return _teleVM.vmAccess().createCodeLocation(_teleVM.findTeleMethodActor(TeleClassMethodActor.class, _classMethodActor), _position, false);
        }

        public long getInstructionPointer() {
            // On top frame, the instruction pointer is incorrect, so take it from the thread!
            if (_isTopFrame) {
                return instructionPointer().asAddress().toLong();
            }
            return _stackFrame.instructionPointer().asAddress().toLong();
        }

        public long getFramePointer() {
            return _stackFrame.framePointer().asAddress().toLong();
        }

        public long getStackPointer() {
            return _stackFrame.stackPointer().asAddress().toLong();
        }

        public ThreadProvider getThread() {
            return TeleNativeThread.this;
        }

        public VMValue getValue(int slot) {

            if (_vmValues == null) {
                initValues();
            }

            return _vmValues[slot];
        }

        public void setValue(int slot, VMValue value) {
            final TargetLocation targetLocation = _frameDescriptor.locals()[slot];

            // TODO: Implement writing to stack frames.
            LOGGER.warning("Stackframe write at " + slot + ", targetLocation=" + targetLocation + ", doing nothing");
        }

        public ObjectProvider thisObject() {
            // TODO: Add a way to access the "this" object.
            LOGGER.warning("Trying to access THIS object, returning null");
            return null;
        }

        public long[] getRawValues() {
            if (_rawValues == null) {
                initValues();
            }
            return _rawValues;
        }
    }

    public FrameProvider getFrame(int depth) {
        return getFrames()[depth];
    }

    private Sequence<StackFrame> _oldFrames;

    public synchronized FrameProvider[] getFrames() {

        synchronized (teleProcess()) {

            if (_oldFrames != _frames) {
                _oldFrames = _frames;
            } else {
                return _frameCache;
            }

            final AppendableSequence<FrameProvider> result = new LinkSequence<FrameProvider>();
            int z = 0;
            for (final StackFrame stackFrame : frames()) {
                z++;

                final Address address = stackFrame.instructionPointer();
                TeleTargetMethod teleTargetMethod = TeleTargetMethod.make(_teleVM, address);
                if (teleTargetMethod == null) {
                    if (stackFrame.targetMethod() == null) {
                        LOGGER.warning("Target method of stack frame (" + stackFrame + ") was null!");
                        continue;
                    }
                    final TargetMethod targetMethod = stackFrame.targetMethod();
                    final ClassMethodActor classMethodActor = targetMethod.classMethodActor();
                    final TeleClassMethodActor teleClassMethodActor = _teleVM.findTeleMethodActor(TeleClassMethodActor.class, classMethodActor);
                    if (teleClassMethodActor == null) {
                        ProgramWarning.message("Could not find tele class method actor for " + classMethodActor);
                        continue;
                    }
                    teleTargetMethod = _teleVM.findTeleTargetRoutine(TeleTargetMethod.class, targetMethod.codeStart().asAddress());
                    if (teleTargetMethod == null) {
                        ProgramWarning.message("Could not find tele target method actor for " + classMethodActor);
                        continue;
                    }
                }

                LOGGER.info("Processing stackframe " + stackFrame);

                int index = -1;
                if (stackFrame.targetMethod() != null) {
                    index = stackFrame.targetMethod().findClosestStopIndex(stackFrame.instructionPointer().minus(1));
                }
                if (index != -1) {
                    final int stopIndex = index; // foundMethod.getJavaStopIndex(sf.instructionPointer().minus(1));
                    TargetJavaFrameDescriptor descriptor = teleTargetMethod.getJavaFrameDescriptor(stopIndex);

                    if (descriptor == null) {
                        LOGGER.info("WARNING: No Java frame descriptor found for Java stop " + stopIndex);

                        if (_teleVM.findTeleMethodActor(TeleClassMethodActor.class, teleTargetMethod.classMethodActor()) == null) {
                            LOGGER.warning("Could not find tele method!");
                        } else {
                            result.append(new FrameProviderImpl(z == 1, teleTargetMethod, stackFrame, null, teleTargetMethod.classMethodActor(), 0));
                        }
                    } else {

                        while (descriptor != null) {
                            final TeleClassMethodActor curTma = _teleVM.findTeleMethodActor(TeleClassMethodActor.class, descriptor.classMethodActor());

                            LOGGER.info("Found part frame " + descriptor + " tele method actor: " + curTma);
                            result.append(new FrameProviderImpl(z == 1, teleTargetMethod, stackFrame, descriptor));
                            descriptor = descriptor.parent();
                        }
                    }
                } else {
                    LOGGER.info("Not at Java stop!");
                    if (_teleVM.findTeleMethodActor(TeleClassMethodActor.class, teleTargetMethod.classMethodActor()) == null) {
                        LOGGER.warning("Could not find tele method!");
                    } else {
                        result.append(new FrameProviderImpl(z == 1, teleTargetMethod, stackFrame, null, teleTargetMethod.classMethodActor(), 0));
                    }
                }
            }

            _frameCache = Sequence.Static.toArray(result, FrameProvider.class);
            return _frameCache;
        }
    }

    public String getName() {
        return toString();
    }

    public void interrupt() {
        // TODO: Implement the possibility to interrupt threads.
        LOGGER.warning("Thread " + this + " was asked to interrupt, doing nothing");
        assert false : "Not implemented.";
    }

    public final void resume() {
        if (_suspendCount > 0) {
            _suspendCount--;
        }
        if (_suspendCount == 0) {
            LOGGER.info("Asked to RESUME THREAD " + this + " we are resuming silently the whole VM for now");
            _teleVM.vmAccess().resume();
        }
    }

    public void stop(ObjectProvider exception) {
        // TODO: Consider implementing stopping a thread by throwing an exception.
        LOGGER.warning("A thread was asked to stop over JDWP with the exception " + exception + ", doing nothing.");
    }

    public final void suspend() {
        _suspendCount++;
    }

    public int suspendCount() {
        // TODO: Implement the suspend count according to the JDWP rules. The current very simple implementation seems to work however fine with NetBeans.
        if (teleProcess().state() == State.STOPPED) {
            return 1;
        }
        return _suspendCount;
    }

    public ReferenceTypeProvider getReferenceType() {
        return this._teleVM.vmAccess().getReferenceType(getClass());
    }

    public ThreadGroupProvider getThreadGroup() {
        return isJava() ? this._teleVM.javaThreadGroupProvider() : this._teleVM.nativeThreadGroupProvider();
    }

    public void doSingleStep() {
        LOGGER.info("Asked to do a single step!");
        _teleVM.registerSingleStepThread(this);
    }

    public void doStepOut() {
        LOGGER.info("Asked to do a step out!");
        _teleVM.registerStepOutThread(this);
    }

    public VMAccess getVM() {
        return _teleVM.vmAccess();
    }

    public RegistersGroup getRegistersGroup() {
        final Registers[] registers = new Registers[]{integerRegisters().getRegisters("Integer Registers"), stateRegisters().getRegisters("State Registers"), floatingPointRegisters().getRegisters("Floating Point Registers")};
        return new RegistersGroup(registers);
    }
}
