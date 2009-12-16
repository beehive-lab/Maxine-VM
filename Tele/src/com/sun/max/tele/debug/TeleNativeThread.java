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

import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
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
 * Represents a thread executing in a {@linkplain TeleProcess tele process}.
 *
 * @author Bernd Mathiske
 * @author Aritra Bandyopadhyay
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public abstract class TeleNativeThread implements Comparable<TeleNativeThread>, MaxThread, ThreadProvider, TeleVMHolder {

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

        private final String asString;
        private final boolean allowsDataAccess;

        ThreadState(String asString, boolean allowsDataAccess) {
            this.asString = asString;
            this.allowsDataAccess = allowsDataAccess;
        }

        @Override
        public String toString() {
            return asString;
        }

        /**
         * Determines whether a thread in this state allows thread specific data to be accessed in the remote process.
         * Thread specific data includes register values, stack memory, and {@linkplain VmThreadLocal VM thread locals}.
         */
        public final boolean allowsDataAccess() {
            return allowsDataAccess;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(TeleNativeThread.class.getName());

    private final TeleProcess teleProcess;
    private final TeleVM teleVM;
    private TeleVmThread teleVmThread;
    private int suspendCount;

    private long registersEpoch;
    private TeleIntegerRegisters integerRegisters;
    private TeleStateRegisters stateRegisters;
    private TeleFloatingPointRegisters floatingPointRegisters;

    /**
     * A cached stack trace for this thread.
     */
    private Sequence<StackFrame> frames;

    /**
     * Only if this value is less than the {@linkplain TeleProcess#epoch() epoch} of this thread's tele process, does
     * the {@link #refreshFrames(boolean)} method do anything.
     */
    private long framesEpoch;
    private boolean framesChanged;

    /**
     * The stack of a Java thread; {@code null} if this is a non-Java thread.
     */
    private final TeleNativeStack stack;

    /**
     * The thread locals block of a Java thread; {@code null} if this is a non-Java thread.
     */
    private final TeleThreadLocalsBlock threadLocalsBlock;

    private final Map<Safepoint.State, TeleThreadLocalValues> teleVmThreadLocals;

    /**
     * Only if this value is less than the {@linkplain TeleProcess#epoch() epoch} of this thread's tele process, does
     * the {@link #refreshThreadLocals()} method do anything.
     */
    private long teleVmThreadLocalsEpoch;

    private ThreadState state = SUSPENDED;
    private TeleTargetBreakpoint breakpoint;
    private FrameProvider[] frameCache;

    /**
     * This thread's {@linkplain VmThread#id() identifier}.
     */
    private final int id;

    private final long localHandle;

    private final long handle;

    /**
     * The parameters accepted by {@link TeleNativeThread#TeleNativeThread(TeleProcess, Params)}.
     */
    public static class Params {
        public int id;
        public long localHandle;
        public long handle;
        public MemoryRegion stack;
        public MemoryRegion threadLocalsBlock;

        @Override
        public String toString() {
            return String.format("id=%d, localHandle=0x%08x, handle=%d, stack=%s, threadLocalsBlock=%s", id, localHandle, handle, MemoryRegion.Util.asString(stack), MemoryRegion.Util.asString(threadLocalsBlock));
        }
    }

    protected TeleNativeThread(TeleProcess teleProcess, Params params) {
        this.teleProcess = teleProcess;
        this.teleVM = teleProcess.teleVM();
        this.id = params.id;
        this.localHandle = params.localHandle;
        this.handle = params.handle;
        final VMConfiguration vmConfiguration = teleVM.vmConfiguration();
        this.integerRegisters = new TeleIntegerRegisters(vmConfiguration);
        this.floatingPointRegisters = new TeleFloatingPointRegisters(vmConfiguration);
        this.stateRegisters = new TeleStateRegisters(vmConfiguration);

        this.teleVmThreadLocals = !params.threadLocalsBlock.start().isZero() ? new EnumMap<Safepoint.State, TeleThreadLocalValues>(Safepoint.State.class) : null;
        this.stack = new TeleNativeStack(this, params.stack.start(), params.stack.size());
        this.threadLocalsBlock = new TeleThreadLocalsBlock(this, params.threadLocalsBlock.start(), params.threadLocalsBlock.size());
        this.breakpointIsAtInstructionPointer = vmConfiguration.platform().processorKind.instructionSet == InstructionSet.SPARC;
    }

    public Sequence<StackFrame> frames() {
        refreshFrames();
        return frames;
    }

    public boolean framesChanged() {
        refreshFrames();
        return framesChanged;
    }

    /**
     * Immutable; thread-safe.
     *
     * @return the process in which this thread is running.
     */
    public TeleProcess teleProcess() {
        return teleProcess;
    }

    public TeleVM teleVM() {
        return teleVM;
    }

    public TeleThreadLocalValues threadLocalsFor(Safepoint.State state) {
        refreshThreadLocals();
        if (teleVmThreadLocals == null) {
            return null;
        }
        return teleVmThreadLocals.get(state);
    }

    public TeleIntegerRegisters integerRegisters() {
        refreshRegisters();
        return integerRegisters;
    }

    public TeleFloatingPointRegisters floatingPointRegisters() {
        refreshRegisters();
        return floatingPointRegisters;
    }

    public TeleStateRegisters stateRegisters() {
        refreshRegisters();
        return stateRegisters;
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
    final void updateAfterGather(ThreadState state, Pointer instructionPointer, Map<Safepoint.State, Pointer> vmThreadLocals) {
        this.state = state;
        stateRegisters.setInstructionPointer(instructionPointer);
        if (vmThreadLocals != null) {
            assert teleVmThreadLocals != null;
            for (Safepoint.State safepointState : Safepoint.State.CONSTANTS) {
                final Pointer vmThreadLocalsPointer = vmThreadLocals.get(safepointState);
                if (vmThreadLocalsPointer.isZero()) {
                    teleVmThreadLocals.put(safepointState, null);
                } else {
                    // Only create a new TeleThreadLocalValues if the start address has changed which
                    // should only happen once going from 0 to a non-zero value.
                    final TeleThreadLocalValues teleVMThreadLocalValues = teleVmThreadLocals.get(safepointState);
                    if (teleVMThreadLocalValues == null || !teleVMThreadLocalValues.start().equals(vmThreadLocalsPointer)) {
                        teleVmThreadLocals.put(safepointState, new TeleThreadLocalValues(this, safepointState, vmThreadLocalsPointer));
                    }
                }
            }
        }
    }

    /**
     * Refreshes the contents of this object to reflect the data of the corresponding thread in the tele process.
     *
     * @param epoch the new epoch of this thread
     */
    void refresh(long epoch) {
        Trace.line(REFRESH_TRACE_LEVEL, tracePrefix() + "refresh(epoch=" + epoch + ") for " + this);
        if (state.allowsDataAccess()) {
            refreshBreakpoint();
        }
    }

    /**
     * Refreshes the cached state of this thread's registers from the corresponding thread in the tele process.
     */
    private synchronized void refreshRegisters() {
        final long processEpoch = teleProcess().epoch();
        if (registersEpoch < processEpoch && isLive()) {
            registersEpoch = processEpoch;
            Trace.line(REFRESH_TRACE_LEVEL, tracePrefix() + "refreshRegisters (epoch=" + processEpoch + ") for " + this);

            if (!readRegisters(integerRegisters.registerData(), floatingPointRegisters.registerData(), stateRegisters.registerData())) {
                ProgramError.unexpected("Error while updating registers for thread: " + this);
            }
            integerRegisters.refresh();
            floatingPointRegisters.refresh();
            stateRegisters.refresh();
        }
    }

    /**
     * Refreshes the values of the cached VM thread local variables for this thread.
     * This method also updates the reference to the {@linkplain #maxVMThread() maxVMThread}.
     */
    private synchronized void refreshThreadLocals() {
        if (teleVmThreadLocals == null) {
            return;
        }
        final long processEpoch = teleProcess().epoch();
        if (teleVmThreadLocalsEpoch < processEpoch) {
            teleVmThreadLocalsEpoch = processEpoch;

            Trace.line(REFRESH_TRACE_LEVEL, tracePrefix() + "refreshThreadLocals (epoch=" + processEpoch + ") for " + this);

            final DataAccess dataAccess = teleProcess().dataAccess();
            for (TeleThreadLocalValues teleVmThreadLocalValues : teleVmThreadLocals.values()) {
                if (teleVmThreadLocalValues != null) {
                    teleVmThreadLocalValues.refresh(dataAccess);
                }
            }

            final TeleThreadLocalValues enabledVmThreadLocalValues = threadLocalsFor(Safepoint.State.ENABLED);
            teleVmThread = null;
            if (enabledVmThreadLocalValues != null) {
                final Long threadLocalValue = enabledVmThreadLocalValues.get(VmThreadLocal.VM_THREAD);
                if (threadLocalValue != 0) {
                    final Reference vmThreadReference = teleVM.wordToReference(Address.fromLong(threadLocalValue));
                    teleVmThread = (TeleVmThread) teleVM.makeTeleObject(vmThreadReference);
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
            breakpoint = breakpointFactory.getTargetBreakpointAt(breakpointAddress);
        } catch (DataIOError dataIOError) {
            // This is a catch for problems getting accurate state for threads that are not at breakpoints
        }
        if (breakpoint != null) {

            Trace.line(REFRESH_TRACE_LEVEL, tracePrefix() + "refreshingBreakpoint (epoch=" + teleProcess().epoch() + ") for " + this);

            state = BREAKPOINT;
            this.breakpoint = breakpoint;
            final Address address = this.breakpoint.teleCodeLocation().targetCodeInstructionAddress();
            if (updateInstructionPointer(address)) {
                stateRegisters.setInstructionPointer(address);
                Trace.line(REFRESH_TRACE_LEVEL, tracePrefix() + "refreshingBreakpoint (epoch=" + teleProcess().epoch() + ") IP updated for " + this);
            } else {
                ProgramError.unexpected("Error updating instruction pointer to adjust thread after breakpoint at " + address + " was hit: " + this);
            }
        } else {
            this.breakpoint = null;
            assert state != BREAKPOINT;
        }
    }

    /**
     * Clears the current list of frames.
     */
    private synchronized void clearFrames() {
        frames = Sequence.Static.empty(StackFrame.class);
        framesChanged = true;
    }

    /**
     * Update the current list of frames.
     * As a side effect, set {@link #framesChanged} to true if the identify of the stack frames has change,
     * even if the objects representing them are different.
     */
    private synchronized void refreshFrames() {
        final long processEpoch = teleProcess().epoch();
        if (framesEpoch < processEpoch) {
            framesEpoch = processEpoch;

            Trace.line(REFRESH_TRACE_LEVEL, tracePrefix() + "refreshFrames (epoch=" + processEpoch + ") for " + this);

            // The stack walk requires the VM thread locals to be up to date
            //refreshThreadLocals();

            final TeleVM teleVM = teleProcess.teleVM();
            final Sequence<StackFrame> frames = new TeleStackFrameWalker(teleVM, this).frames();
            assert !frames.isEmpty();
            if (this.frames != null && frames.length() == this.frames.length()) {
                framesChanged = false;
                final Iterator<StackFrame> oldFrames = this.frames.iterator();
                final Iterator<StackFrame> newFrames = frames.iterator();
                while (oldFrames.hasNext()) {
                    final StackFrame oldFrame = oldFrames.next();
                    final StackFrame newFrame = newFrames.next();
                    if (!oldFrame.isSameFrame(newFrame)) {
                        framesChanged = true;
                        break;
                    }
                }
            } else {
                framesChanged = true;
            }
            this.frames = frames;
        }
    }

    public final TeleTargetBreakpoint breakpoint() {
        return breakpoint;
    }

    /**
     * Specifies whether or not the instruction pointer needs to be adjusted when this thread hits a breakpoint to
     * denote the instruction pointer for which the breakpoint was set. For example, on x86 architectures, the
     * instruction pointer is at the instruction following the breakpoint instruction whereas on SPARC, it's
     * at the instruction pointer for which the breakpoint was set.
     */
    private boolean breakpointIsAtInstructionPointer;

    public final ThreadState state() {
        return state;
    }

    public final boolean isPrimordial() {
        return id() == 0;
    }

    public final boolean isLive() {
        return state != DEAD;
    }

    /**
     * Marks the thread as having died in the process; flushes all state accordingly.
     */
    final void setDead() {
        state = DEAD;
        clearFrames();
        breakpoint = null;
        teleVmThread = null;
        frameCache = null;
        if (teleVmThreadLocals != null) {
            teleVmThreadLocals.clear();
        }
        integerRegisters = null;
        stateRegisters = null;
        floatingPointRegisters = null;
    }

    public final int id() {
        return id;
    }

    public final long handle() {
        return handle;
    }

    public final String handleString() {
        return "0x" + Long.toHexString(handle);
    }

    public final long localHandle() {
        return localHandle;
    }

    public final Pointer stackPointer() {
        if (!isLive()) {
            return Pointer.zero();
        }
        refreshRegisters();
        return integerRegisters.stackPointer();
    }

    final Pointer framePointer() {
        refreshRegisters();
        return integerRegisters.framePointer();
    }

    /**
     * @see com.sun.max.tele.MaxThread#stack()
     */
    public final TeleNativeStack stack() {
        return stack;
    }

    /**
     * @see com.sun.max.tele.MaxThread#stack()
     */
    public final TeleThreadLocalsBlock threadLocalsBlock() {
        return threadLocalsBlock;
    }

    public final Pointer instructionPointer() {
        if (!isLive()) {
            return Pointer.zero();
        }
        // No need to call refreshRegisters(): the instruction pointer is updated by updateAfterGather() which
        // ensures that it is always in sync.
        return stateRegisters.instructionPointer();
    }

    /**
     * Updates the current value of the instruction pointer for this thread.
     *
     * @param address the address to which the instruction should be set
     * @return true if the instruction pointer was successfully updated, false otherwise
     */
    protected abstract boolean updateInstructionPointer(Address address);

    public Pointer getReturnAddress() {
        final StackFrame topFrame = frames().first();
        final StackFrame topFrameCaller = topFrame.callerFrame();
        return topFrameCaller == null ? null : teleVM.getCodeAddress(topFrameCaller).asPointer();
    }

    public MaxVMThread maxVMThread() {
        if (teleVmThread == null) {
            refreshThreadLocals();
        }
        return teleVmThread;
    }

    protected abstract boolean readRegisters(byte[] integerRegisters, byte[] floatingPointRegisters, byte[] stateRegisters);

    /**
     * Advances this thread to the next instruction. That is, makes this thread execute a single machine instruction.
     * Note that this method does not block waiting for the tele process to complete the step.
     *
     * @return true if the single step was issued successfully, false otherwise
     */
    protected abstract boolean singleStep();

    protected abstract boolean threadResume();

    public abstract boolean threadSuspend();

    /**
     * If this thread is currently at a {@linkplain #breakpoint() breakpoint} it is single stepped to the next
     * instruction.
     */
    void evadeBreakpoint() throws OSExecutionRequestException {
        if (breakpoint != null && !breakpoint.isTransient()) {
            assert !breakpoint.isActivated() : "Cannot single step at an activated breakpoint";
            teleProcess().singleStep(this, true);
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
    Pointer breakpointAddressFromInstructionPointer() {
        final Pointer instructionPointer = instructionPointer();
        if (breakpointIsAtInstructionPointer) {
            return instructionPointer();
        }
        return instructionPointer.minus(teleProcess().targetBreakpointFactory().codeSize());
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof TeleNativeThread) {
            final TeleNativeThread teleNativeThread = (TeleNativeThread) other;
            return localHandle() == teleNativeThread.localHandle() && id() == teleNativeThread.id();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) localHandle();
    }

    /**
     * Imposes a total ordering between threads based on their {@linkplain #id() identifiers}.
     */
    public int compareTo(TeleNativeThread other) {
        return Longs.compare(handle(), other.handle());
    }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder(100);
        sb.append(isPrimordial() ? "primordial" : (teleVmThread == null ? "native" : teleVmThread.name()));
        sb.append("[id=").append(id());
        sb.append(",handle=").append(handleString());
        sb.append(",local handle=").append(localHandle());
        sb.append(",state=").append(state);
        sb.append(",type=").append(isPrimordial() ? "primordial" : (isJava() ? "Java" : "native"));
        if (isLive()) {
            sb.append(",ip=0x").append(instructionPointer().toHexString());
            if (isJava()) {
                sb.append(",stack_start=0x").append(stack().start().toHexString());
                sb.append(",stack_size=").append(stack().size().toLong());
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * @return a printable version of the thread's internal state that only shows key aspects
     */
    public final String toShortString() {
        final StringBuilder sb = new StringBuilder(100);
        sb.append(isPrimordial() ? "primordial" : (teleVmThread == null ? "native" : teleVmThread.name()));
        sb.append("[id=").append(id());
        sb.append(",handle=").append(handleString());
        sb.append(",local handle=").append(localHandle());
        sb.append(",type=").append(isPrimordial() ? "primordial" : (isJava() ? "Java" : "native"));
        sb.append("]");
        return sb.toString();
    }

    /**
     * Determines if this thread is associated with a {@link VmThread} instance. Note that even if this method returns
     * {@code true}, the {@link #maxVMThread()} method will return {@code null} if the thread has not reached the
     * execution point in {@link VmThread#run} where the {@linkplain VmThreadLocal#VM_THREAD reference} to the
     * {@link VmThread} object has been initialized.
     */
    public final boolean isJava() {
        return id > 0;
    }

    /**
     * Class representing a Java-level frame. It implements the interface the JDWP protocol is programmed against.
     */
    private class FrameProviderImpl implements FrameProvider {

        private TargetJavaFrameDescriptor frameDescriptor;
        private ClassMethodActor classMethodActor;
        private int position;
        private StackFrame stackFrame;
        private TeleTargetMethod targetMethod;
        private long[] rawValues;
        private VMValue[] vmValues;
        private boolean isTopFrame;

        public FrameProviderImpl(boolean isTopFrame, TeleTargetMethod targetMethod, StackFrame stackFrame, TargetJavaFrameDescriptor descriptor) {
            this(isTopFrame, targetMethod, stackFrame, descriptor, descriptor.classMethodActor, 0); //descriptor.bytecodeLocation().());
        }

        public FrameProviderImpl(boolean isTopFrame, TeleTargetMethod targetMethod, StackFrame stackFrame, TargetJavaFrameDescriptor descriptor, ClassMethodActor classMethodActor, int position) {
            this.stackFrame = stackFrame;
            this.frameDescriptor = descriptor;
            this.classMethodActor = classMethodActor;
            this.position = position;
            this.targetMethod = targetMethod;
            this.isTopFrame = isTopFrame;

            if (classMethodActor.codeAttribute().lineNumberTable().entries().length > 0) {
                this.position = classMethodActor.codeAttribute().lineNumberTable().entries()[0].position();
            } else {
                LOGGER.warning("No line number table information for method " + classMethodActor.name.toString());
                this.position = -1;
            }
        }

        private void initValues() {
            final int length = classMethodActor.codeAttribute().localVariableTable().entries().length;
            final long[] values = new long[length];
            final VMValue[] vmValues = new VMValue[length];

            for (LocalVariableTable.Entry entry : classMethodActor.codeAttribute().localVariableTable().entries()) {
                final Value curValue = getValueImpl(entry.slot());
                vmValues[entry.slot()] = teleVM.maxineValueToJDWPValue(curValue);

                if (curValue.kind() == Kind.REFERENCE) {
                    values[entry.slot()] = curValue.asReference().toOrigin().toLong();
                } else if (curValue.kind() == Kind.WORD) {
                    values[entry.slot()] = curValue.asWord().asPointer().toLong();
                }
            }

            this.vmValues = vmValues;
            rawValues = values;
        }

        private Value getValueImpl(int slot) {
            TargetLocation l = null;

            if (frameDescriptor == null) {
                final TargetLocation[] targetLocations = stackFrame.targetMethod().abi().getParameterTargetLocations(stackFrame.targetMethod().classMethodActor().getParameterKinds());
                if (slot >= targetLocations.length) {
                    return IntValue.from(0xbadbabe);
                }
                l = targetLocations[slot];
            } else {

                if (slot >= frameDescriptor.locals.length) {
                    return IntValue.from(0xbadbabe);
                }
                l = frameDescriptor.locals[slot];
            }

            System.out.println("STACKFRAME ACCESS at " + slot + ", target=" + l);

            final Entry entry = classMethodActor.codeAttribute().localVariableTable().findLocalVariable(slot, position);
            if (entry == null) {
                return LongValue.from(0xbadbabe);
            }
            final TypeDescriptor descriptor = entry.descriptor(classMethodActor.codeAttribute().constantPool);
            final Kind kind = descriptor.toKind();

            if (l instanceof LocalStackSlot) {
                final LocalStackSlot localStackSlot = (LocalStackSlot) l;
                final int index = localStackSlot.index();
                final Pointer slotBase = stackFrame.slotBase();
                final int offset = index * Word.size();

                return teleVM.readValue(kind, slotBase, offset);

            } else if (l instanceof ParameterStackSlot) {

                final ParameterStackSlot parameterStackSlot = (ParameterStackSlot) l;
                final int index = parameterStackSlot.index();
                final Pointer slotBase = stackFrame.slotBase();

                // TODO: Resolve this hack that uses a special function in the Java stack frame layout.

                final JavaStackFrame javaStackFrame = (JavaStackFrame) stackFrame;
                int offset = index * Word.size() + javaStackFrame.layout.frameSize();
                offset += javaStackFrame.layout.isReturnAddressPushedByCall() ? Word.size() : 0;

                return teleVM.readValue(kind, slotBase, offset);

            } else if (l instanceof IntegerRegister) {
                final IntegerRegister integerRegister = (IntegerRegister) l;
                final int integerRegisterIndex = integerRegister.index();
                final Address address = integerRegisters().get(integerRegisterIndex);

                if (kind == Kind.REFERENCE) {
                    return TeleReferenceValue.from(teleVM, Reference.fromOrigin(address.asPointer()));
                }
                return LongValue.from(address.toLong());
            }

            return IntValue.from(5);
        }

        public TargetMethodAccess getTargetMethodProvider() {
            return targetMethod;
        }

        public CodeLocation getLocation() {
            return teleVM.vmAccess().createCodeLocation(teleVM.findTeleMethodActor(TeleClassMethodActor.class, classMethodActor), position, false);
        }

        public long getInstructionPointer() {
            // On top frame, the instruction pointer is incorrect, so take it from the thread!
            if (isTopFrame) {
                return instructionPointer().asAddress().toLong();
            }
            return stackFrame.instructionPointer.asAddress().toLong();
        }

        public long getFramePointer() {
            return stackFrame.framePointer.asAddress().toLong();
        }

        public long getStackPointer() {
            return stackFrame.stackPointer.asAddress().toLong();
        }

        public ThreadProvider getThread() {
            return TeleNativeThread.this;
        }

        public VMValue getValue(int slot) {

            if (vmValues == null) {
                initValues();
            }

            return vmValues[slot];
        }

        public void setValue(int slot, VMValue value) {
            final TargetLocation targetLocation = frameDescriptor.locals[slot];

            // TODO: Implement writing to stack frames.
            LOGGER.warning("Stackframe write at " + slot + ", targetLocation=" + targetLocation + ", doing nothing");
        }

        public ObjectProvider thisObject() {
            // TODO: Add a way to access the "this" object.
            LOGGER.warning("Trying to access THIS object, returning null");
            return null;
        }

        public long[] getRawValues() {
            if (rawValues == null) {
                initValues();
            }
            return rawValues;
        }
    }

    public FrameProvider getFrame(int depth) {
        return getFrames()[depth];
    }

    private Sequence<StackFrame> oldFrames;

    public synchronized FrameProvider[] getFrames() {

        synchronized (teleProcess()) {

            if (oldFrames != frames) {
                oldFrames = frames;
            } else {
                return frameCache;
            }

            final AppendableSequence<FrameProvider> result = new LinkSequence<FrameProvider>();
            int z = 0;
            for (final StackFrame stackFrame : frames()) {
                z++;

                final Address address = stackFrame.instructionPointer;
                TeleTargetMethod teleTargetMethod = TeleTargetMethod.make(teleVM, address);
                if (teleTargetMethod == null) {
                    if (stackFrame.targetMethod() == null) {
                        LOGGER.warning("Target method of stack frame (" + stackFrame + ") was null!");
                        continue;
                    }
                    final TargetMethod targetMethod = stackFrame.targetMethod();
                    final ClassMethodActor classMethodActor = targetMethod.classMethodActor();
                    final TeleClassMethodActor teleClassMethodActor = teleVM.findTeleMethodActor(TeleClassMethodActor.class, classMethodActor);
                    if (teleClassMethodActor == null) {
                        ProgramWarning.message("Could not find tele class method actor for " + classMethodActor);
                        continue;
                    }
                    teleTargetMethod = teleVM.findTeleTargetRoutine(TeleTargetMethod.class, targetMethod.codeStart().asAddress());
                    if (teleTargetMethod == null) {
                        ProgramWarning.message("Could not find tele target method actor for " + classMethodActor);
                        continue;
                    }
                }

                LOGGER.info("Processing stackframe " + stackFrame);

                int index = -1;
                if (stackFrame.targetMethod() != null && stackFrame.targetMethod() instanceof CPSTargetMethod) {
                    index = ((CPSTargetMethod) stackFrame.targetMethod()).findClosestStopIndex(stackFrame.instructionPointer);
                }
                if (index != -1) {
                    final int stopIndex = index;
                    TargetJavaFrameDescriptor descriptor = teleTargetMethod.getJavaFrameDescriptor(stopIndex);

                    if (descriptor == null) {
                        LOGGER.info("WARNING: No Java frame descriptor found for Java stop " + stopIndex);

                        if (teleVM.findTeleMethodActor(TeleClassMethodActor.class, teleTargetMethod.classMethodActor()) == null) {
                            LOGGER.warning("Could not find tele method!");
                        } else {
                            result.append(new FrameProviderImpl(z == 1, teleTargetMethod, stackFrame, null, teleTargetMethod.classMethodActor(), 0));
                        }
                    } else {

                        while (descriptor != null) {
                            final TeleClassMethodActor curTma = teleVM.findTeleMethodActor(TeleClassMethodActor.class, descriptor.classMethodActor);

                            LOGGER.info("Found part frame " + descriptor + " tele method actor: " + curTma);
                            result.append(new FrameProviderImpl(z == 1, teleTargetMethod, stackFrame, descriptor));
                            descriptor = descriptor.parent();
                        }
                    }
                } else {
                    LOGGER.info("Not at Java stop!");
                    if (teleVM.findTeleMethodActor(TeleClassMethodActor.class, teleTargetMethod.classMethodActor()) == null) {
                        LOGGER.warning("Could not find tele method!");
                    } else {
                        result.append(new FrameProviderImpl(z == 1, teleTargetMethod, stackFrame, null, teleTargetMethod.classMethodActor(), 0));
                    }
                }
            }

            frameCache = Sequence.Static.toArray(result, FrameProvider.class);
            return frameCache;
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
        if (suspendCount > 0) {
            suspendCount--;
        }
        if (suspendCount == 0) {
            LOGGER.info("Asked to RESUME THREAD " + this + " we are resuming silently the whole VM for now");
            teleVM.vmAccess().resume();
        }
    }

    public void stop(ObjectProvider exception) {
        // TODO: Consider implementing stopping a thread by throwing an exception.
        LOGGER.warning("A thread was asked to stop over JDWP with the exception " + exception + ", doing nothing.");
    }

    public final void suspend() {
        suspendCount++;
    }

    public int suspendCount() {
        // TODO: Implement the suspend count according to the JDWP rules. The current very simple implementation seems to work however fine with NetBeans.
        if (teleProcess().processState() == ProcessState.STOPPED) {
            return 1;
        }
        return suspendCount;
    }

    public ReferenceTypeProvider getReferenceType() {
        return this.teleVM.vmAccess().getReferenceType(getClass());
    }

    public ThreadGroupProvider getThreadGroup() {
        return isJava() ? this.teleVM.javaThreadGroupProvider() : this.teleVM.nativeThreadGroupProvider();
    }

    public void doSingleStep() {
        LOGGER.info("Asked to do a single step!");
        teleVM.registerSingleStepThread(this);
    }

    public void doStepOut() {
        LOGGER.info("Asked to do a step out!");
        teleVM.registerStepOutThread(this);
    }

    public VMAccess getVM() {
        return teleVM.vmAccess();
    }

    public RegistersGroup getRegistersGroup() {
        final Registers[] registers = new Registers[]{integerRegisters().getRegisters("Integer Registers"), stateRegisters().getRegisters("State Registers"), floatingPointRegisters().getRegisters("Floating Point Registers")};
        return new RegistersGroup(registers);
    }
}
