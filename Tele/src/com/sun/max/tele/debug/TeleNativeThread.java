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
public abstract class TeleNativeThread implements Comparable<TeleNativeThread>, ThreadProvider {

    /**
     * The states a thread can be in.
     *
     * Note: This enum must be kept in sync the one of the same name in MaxineNative/inspector/teleNativeThread.h.
     */
    public enum ThreadState {
        /**
         * Denotes that a thread is waiting to acquire ownership of a monitor.
         */
        MONITOR_WAIT("Monitor"),

        /**
         * Denotes that a thread is waiting to be {@linkplain Object#notify() notified}.
         */
        NOTIFY_WAIT("Wait"),

        /**
         * Denotes that a thread is waiting for another
         * {@linkplain Thread#join(long, int) thread to die or a timer to expire}.
         */
        JOIN_WAIT("Join"),

        /**
         * Denotes that a thread is {@linkplain Thread#sleep(long) sleeping}.
         */
        SLEEPING("Sleeping"),

        /**
         * Denotes that a thread is suspended at a breakpoint.
         */
        BREAKPOINT("Breakpoint"),

        /**
         * Denotes that a thread is suspended for some reason other than {@link #MONITOR_WAIT}, {@link #NOTIFY_WAIT},
         * {@link #JOIN_WAIT}, {@link #SLEEPING} or {@link #BREAKPOINT}.
         */
        SUSPENDED("Suspended"),

        DEAD("Dead"),

        /**
         * Denotes that a thread is not suspended.
         */
        RUNNING("Running");

        public static final IndexedSequence<ThreadState> VALUES = new ArraySequence<ThreadState>(values());

        private final String _asString;

        ThreadState(String asString) {
            _asString = asString;
        }

        @Override
        public String toString() {
            return _asString;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(TeleNativeThread.class.getName());

    private final TeleProcess _teleProcess;
    private TeleVmThread _teleVmThread;
    private int _suspendCount;

    private Sequence<StackFrame> _frames;
    private Sequence<StackFrame> _oldFrames;

    private final TeleIntegerRegisters _integerRegisters;
    private final TeleStateRegisters _stateRegisters;
    private final TeleFloatingPointRegisters _floatingPointRegisters;

    private boolean _framesChanged;
    private final TeleNativeStack _stack;
    private ThreadState _state = SUSPENDED;
    private TeleTargetBreakpoint _breakpoint;
    private final long _id;
    private FrameProvider[] _frameCache;

    protected TeleNativeThread(TeleProcess teleProcess, long stackBase, long stackSize, long id) {
        final VMConfiguration vmConfiguration = teleProcess.teleVM().vmConfiguration();
        _teleProcess = teleProcess;
        _id = id;
        _integerRegisters = new TeleIntegerRegisters(vmConfiguration);
        _floatingPointRegisters = new TeleFloatingPointRegisters(vmConfiguration);
        _stateRegisters = new TeleStateRegisters(vmConfiguration);
        _stack = new TeleNativeStack(Address.fromLong(stackBase), Size.fromLong(stackSize));
        _breakpointIsAtInstructionPointer = teleProcess().teleVM().vmConfiguration().platform().processorKind().instructionSet() == InstructionSet.SPARC;
    }

    /**
     * Gets the frames of this stack as produced by a {@linkplain StackFrameWalker stack walk} when the associated
     * thread last stopped.
     */
    public Sequence<StackFrame> frames() {
        return _frames;
    }

    /**
     * Determines if the frames of this stack changed in the epoch that completed last time this thread stopped.
     */
    public boolean framesChanged() {
        return _framesChanged;
    }

    public TeleProcess teleProcess() {
        return _teleProcess;
    }

    public TeleIntegerRegisters integerRegisters() {
        return _integerRegisters;
    }

    public TeleFloatingPointRegisters floatingPointRegisters() {
        return _floatingPointRegisters;
    }

    public TeleStateRegisters stateRegisters() {
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
     * Refreshes the contents of this object to reflect the data of the corresponding thread in the tele process.
     * If {@code this.state() == DEAD}, then this object represents a thread that has died in the tele process and
     * all state is flushed accordingly.
     *
     * @param epoch the new epoch of this thread
     */
    public final void refresh(long epoch) {
        if (_state == DEAD) {
            _stack.refresh(null);
            refreshFrames(true);
            _breakpoint = null;
            _teleVmThread = null;
            return;
        }

        refreshRegisters();
        refreshStack();
        refreshBreakpoint();
        refreshFrames(false);
    }

    /**
     * Refreshes the cached state of this thread's registers from the corresponding thread in the tele process.
     */
    private void refreshRegisters() {
        if (!readRegisters(_integerRegisters.registerData(), _floatingPointRegisters.registerData(), _stateRegisters.registerData())) {
            throw new TeleError("Error while updating registers for thread: " + this);
        }
        _integerRegisters.refresh();
        _floatingPointRegisters.refresh();
        _stateRegisters.refresh();
    }

    /**
     * Refreshes the cached state of this thread's {@linkplain #stack() stack} from the corresponding thread in the tele process.
     * This method also updates the reference to the {@linkplain #teleVmThread() tele VmThread}.
     */
    private void refreshStack() {
        _stack.refresh(this);
        final Map<VmThreadLocal, Long> threadLocalValues = _stack.threadLocalValues();
        if (threadLocalValues != null) {
            final Long threadLocalValue = threadLocalValues.get(VmThreadLocal.VM_THREAD);
            final Reference vmThreadReference = _teleProcess.teleVM().wordToReference(Address.fromLong(threadLocalValue));
            _teleVmThread = (TeleVmThread) TeleObject.make(_teleProcess.teleVM(), vmThreadReference);
        } else {
            _teleVmThread = null;
        }
    }

    /**
     * Refreshes the information about the {@linkplain #breakpoint() breakpoint} this thread is currently stopped at (if
     * any). If this thread is stopped at a breakpoint, its instruction pointer is adjusted so that it is at the
     * instruction on which the breakpoint was set.
     */
    private void refreshBreakpoint() {
        final Factory breakpointFactory = teleProcess().targetBreakpointFactory();
        breakpointFactory.registerBreakpointSetByVM(this);

        final Pointer breakpointAddress = breakpointAddressFromInstructionPointer();
        final TeleTargetBreakpoint breakpoint = breakpointFactory.getBreakpointAt(breakpointAddress);
        if (breakpoint != null) {
            _state = BREAKPOINT;
            _breakpoint = breakpoint;
            if (updateInstructionPointer(_breakpoint.address())) {
                _stateRegisters.setInstructionPointer(_breakpoint.address());
            } else {
                throw new TeleError("Error updating instruction pointer to adjust thread after breakpoint at " + _breakpoint.address() + " was hit: " + this);
            }
        } else {
            _breakpoint = null;
            assert _state != BREAKPOINT;
        }
    }

    private void refreshFrames(boolean clear) {
        if (clear) {
            _frames = Sequence.Static.empty(StackFrame.class);
            _framesChanged = true;
            return;
        }

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

    public final long id() {
        return _id;
    }

    public final Pointer stackPointer() {
        return _integerRegisters.stackPointer();
    }

    public final Pointer framePointer() {
        return _integerRegisters.framePointer();
    }

    /**
     * Gets the stack for this thread.
     */
    public final TeleNativeStack stack() {
        return _stack;
    }

    public final Pointer instructionPointer() {
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
     *
     * @return null if this thread is not associated with VmThread (e.g. it's a non-Java thread created by native code called by JNI)
     */
    public TeleVmThread teleVmThread() {
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
    public void evadeBreakpoint() throws ExecutionRequestException {
        if (_breakpoint != null) {
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
            return id() == teleNativeThread.id();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) id();
    }

    /**
     * Imposes a total ordering between threads based on their {@linkplain #id() identifiers}.
     */
    public int compareTo(TeleNativeThread other) {
        return Longs.compare(id(), other.id());
    }

    @Override
    public final String toString() {
        final String name = _teleVmThread == null ? "native" : _teleVmThread.name();
        return name +
            "[id=" + id() +
            ",state=" + _state +
            ",type=" + (isJava() ? "Java" : "native") +
            ",ip=0x" + instructionPointer().toHexString() +
            ",stack_start=0x" + stack().start().toHexString() +
            ",stack_size=" + stack().size().toLong() +
            "]";
    }

    public final boolean isJava() {
        return _stack.threadLocalValues() != null;
    }

    /**
     * Class representing a Java-level frame. It implements the interface the JDWP protocol is programmed against.
     *
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
            this(isTopFrame, targetMethod, stackFrame, descriptor, descriptor.bytecodeLocation().classMethodActor(), 0); //descriptor.bytecodeLocation().());
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
                vmValues[entry.slot()] = teleProcess().teleVM().convertToVirtualMachineValue(curValue);

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

                return teleProcess().teleVM().readValue(kind, slotBase, offset);

            } else if (l instanceof ParameterStackSlot) {

                final ParameterStackSlot parameterStackSlot = (ParameterStackSlot) l;
                final int index = parameterStackSlot.index();
                final Pointer slotBase = _stackFrame.slotBase();

                // TODO: Resolve this hack that uses a special function in the Java stack frame layout.

                final JavaStackFrame javaStackFrame = (JavaStackFrame) _stackFrame;
                int offset = index * Word.size() + javaStackFrame.layout().frameSize();
                offset += javaStackFrame.layout().isReturnAddressPushedByCall() ? Word.size() : 0;

                return teleProcess().teleVM().readValue(kind, slotBase, offset);

            } else if (l instanceof IntegerRegister) {
                final IntegerRegister integerRegister = (IntegerRegister) l;
                final int integerRegisterIndex = integerRegister.index();
                final Address address = integerRegisters().get(integerRegisterIndex);

                if (kind == Kind.REFERENCE) {
                    return TeleReferenceValue.from(teleProcess().teleVM(), Reference.fromOrigin(address.asPointer()));
                }
                return LongValue.from(address.toLong());
            }

            return IntValue.from(5);
        }

        @Override
        public TargetMethodAccess getTargetMethodProvider() {
            return _targetMethod;
        }

        @Override
        public CodeLocation getLocation() {
            return teleProcess().teleVM().createCodeLocation(teleProcess().teleVM().teleClassRegistry().findTeleMethodActor(_classMethodActor), _position, false);
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

        @Override
        public ThreadProvider getThread() {
            return TeleNativeThread.this;
        }

        @Override
        public VMValue getValue(int slot) {

            if (_vmValues == null) {
                initValues();
            }

            return _vmValues[slot];
        }

        @Override
        public void setValue(int slot, VMValue value) {
            final TargetLocation targetLocation = _frameDescriptor.locals()[slot];

            // TODO: Implement writing to stack frames.
            LOGGER.warning("Stackframe write at " + slot + ", targetLocation=" + targetLocation + ", doing nothing");
        }

        @Override
        public ObjectProvider thisObject() {
            // TODO: Add a way to access the "this" object.
            LOGGER.warning("Trying to access THIS object, returning null");
            return null;
        }

        @Override
        public long[] getRawValues() {
            if (_rawValues == null) {
                initValues();
            }
            return _rawValues;
        }
    }

    @Override
    public FrameProvider getFrame(int depth) {
        return getFrames()[depth];
    }

    @Override
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
                TeleTargetMethod teleTargetMethod = TeleTargetMethod.make(teleProcess().teleVM(), address);
                if (teleTargetMethod == null) {

                    if (stackFrame.targetMethod() == null) {
                        LOGGER.warning("Target method of stack frame (" + stackFrame + ") was null!");
                        continue;
                    }

                    final TargetMethod targetMethod = stackFrame.targetMethod();
                    final ClassMethodActor classMethodActor = targetMethod.classMethodActor();
                    final TeleClassMethodActor teleClassMethodActor = (TeleClassMethodActor) teleProcess().teleVM().teleClassRegistry().findTeleMethodActor(classMethodActor);
                    if (teleClassMethodActor == null) {
                        ProgramWarning.message("Could not find tele class method actor for " + classMethodActor);
                        continue;
                    }

                    // TODO: Check if this cast is always safe!
                    final TeleTargetMethod foundMethod = (TeleTargetMethod) teleProcess().teleVM().findTeleTargetRoutine(targetMethod.codeStart().asAddress());
                    if (foundMethod == null) {
                        ProgramWarning.message("Could not find tele target method actor for " + classMethodActor);
                        continue;
                    }
                    teleTargetMethod = foundMethod;
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

                        if (teleProcess().teleVM().teleClassRegistry().findTeleMethodActor(teleTargetMethod.classMethodActor()) == null) {
                            LOGGER.warning("Could not find tele method!");
                        } else {
                            result.append(new FrameProviderImpl(z == 1, teleTargetMethod, stackFrame, null, teleTargetMethod.classMethodActor(), 0));
                        }
                    } else {

                        while (descriptor != null) {
                            final TeleClassMethodActor curTma = (TeleClassMethodActor) teleProcess().teleVM().teleClassRegistry().findTeleMethodActor(descriptor.bytecodeLocation().classMethodActor());

                            LOGGER.info("Found part frame " + descriptor + " tele method actor: " + curTma);
                            result.append(new FrameProviderImpl(z == 1, teleTargetMethod, stackFrame, descriptor));
                            descriptor = descriptor.parent();
                        }
                    }
                } else {
                    LOGGER.info("Not at Java stop!");
                    if (teleProcess().teleVM().teleClassRegistry().findTeleMethodActor(teleTargetMethod.classMethodActor()) == null) {
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

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public void interrupt() {
        // TODO: Implement the possibility to interrupt threads.
        LOGGER.warning("Thread " + this + " was asked to interrupt, doing nothing");
        assert false : "Not implemented.";
    }

    @Override
    public final void resume() {
        if (_suspendCount > 0) {
            _suspendCount--;
        }
        if (_suspendCount == 0) {
            LOGGER.info("Asked to RESUME THREAD " + this + " we are resuming silently the whole VM for now");
            teleProcess().teleVM().resume();
        }
    }

    @Override
    public void stop(ObjectProvider exception) {
        // TODO: Consider implementing stopping a thread by throwing an exception.
        LOGGER.warning("A thread was asked to stop over JDWP with the exception " + exception + ", doing nothing.");
    }

    @Override
    public final void suspend() {
        _suspendCount++;
    }

    @Override
    public int suspendCount() {
        // TODO: Implement the suspend count according to the JDWP rules. The current very simple implementation seems to work however fine with NetBeans.
        if (teleProcess().state() == State.STOPPED) {
            return 1;
        }
        return _suspendCount;
    }

    @Override
    public ReferenceTypeProvider getReferenceType() {
        return this.teleProcess().teleVM().getReferenceType(getClass());
    }

    @Override
    public ThreadGroupProvider getThreadGroup() {
        return isJava() ? this.teleProcess().teleVM().javaThreads() : this.teleProcess().teleVM().nativeThreads();
    }

    @Override
    public void doSingleStep() {
        LOGGER.info("Asked to do a single step!");
        teleProcess().teleVM().registerSingleStepThread(this);
    }

    @Override
    public void doStepOut() {
        LOGGER.info("Asked to do a step out!");
        teleProcess().teleVM().registerStepOutThread(this);
    }

    @Override
    public VMAccess getVM() {
        return teleProcess().teleVM();
    }

    @Override
    public RegistersGroup getRegistersGroup() {
        final Registers[] registers = new Registers[]{integerRegisters().getRegisters("Integer Registers"), stateRegisters().getRegisters("State Registers"), floatingPointRegisters().getRegisters("Floating Point Registers")};
        return new RegistersGroup(registers);
    }
}
