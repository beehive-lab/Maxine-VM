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
package com.sun.max.tele.debug;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.tele.MaxThreadState.*;

import java.util.*;
import java.util.logging.*;

import com.sun.cri.ci.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.value.*;

/**
 * Represents a thread executing in a {@linkplain TeleProcess tele process}.
 */
public abstract class TeleNativeThread extends AbstractVmHolder
    implements TeleVMCache, Comparable<TeleNativeThread>, MaxThread, AllocationHolder, ThreadProvider {

    @Override
    protected String  tracePrefix() {
        return "[TeleNativeThread: " + Thread.currentThread().getName() + "] ";
    }

    private static final int TRACE_VALUE = 1;

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    private static final Logger LOGGER = Logger.getLogger(TeleNativeThread.class.getName());

    private final TeleProcess teleProcess;
    private int suspendCount;

    private final TeleRegisterSet teleRegisterSet;

    private final TeleStack teleStack;

    /**
     * A cached stack trace for this thread, never null.
     */
    private List<StackFrame> frames = Collections.emptyList();

    /**
     * The value of maxDepth when {@link #frames} was last updated.
     */
    private int framesMaxDepth = -1;

    /**
     * Only if this value is less than the {@linkplain TeleProcess#epoch() epoch} of this thread's tele process, does
     * the {@link #refreshFrames(boolean)} method do anything.
     */
    private long framesRefreshedEpoch;

    /**
     * The last epoch at which the structure of the stack changed, even if the contents of the top frame may have.
     */
    private long framesLastChangedEpoch;

    /**
     * Access to information about and contained in this thread's local storage area.
     * Holds a dummy if no thread local information is available:  i.e. either this is a non-Java thread, or
     * the thread is very early in its creation sequence.
     */
    private final TeleThreadLocalsBlock threadLocalsBlock;

    private MaxThreadState state = SUSPENDED;
    private VmTargetBreakpoint breakpoint;
    private FrameProvider[] frameCache;

    /**
     * This thread's {@linkplain VmThread#id() identifier}.
     */
    private final int id;

    private final long localHandle;
    private final long handle;
    private final String entityName;
    private final String entityDescription;

    /**
     * The parameters accepted by {@link TeleNativeThread#TeleNativeThread(TeleProcess, Params)}.
     */
    public static class Params {
        public int id;
        public long localHandle;
        public long handle;
        public TeleFixedMemoryRegion stackRegion;
        public TeleFixedMemoryRegion threadLocalsRegion;

        @Override
        public String toString() {
            return String.format("id=%d, localHandle=0x%08x, handle=%d, stackRegion=%s, tlasRegion=%s", id, localHandle, handle, MaxMemoryRegion.Util.asString(stackRegion), MaxMemoryRegion.Util.asString(threadLocalsRegion));
        }
    }

    protected TeleNativeThread(TeleProcess teleProcess, Params params) {
        super(teleProcess.vm());
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating id=" + params.id);
        tracer.begin();

        this.teleProcess = teleProcess;
        this.id = params.id;
        this.localHandle = params.localHandle;
        this.handle = params.handle;
        this.entityName = "Thread-" + this.localHandle;
        this.entityDescription = "The thread named " + this.entityName + " in the " + teleProcess.vm().entityName();
        this.teleRegisterSet = new TeleRegisterSet(teleProcess.vm(), this);
        if (params.threadLocalsRegion == null) {
            final String name = this.entityName + " Locals (NULL, not allocated)";
            this.threadLocalsBlock = new TeleThreadLocalsBlock(this, name);
        } else {
            final String name = this.entityName + " Locals";
            this.threadLocalsBlock = new TeleThreadLocalsBlock(this, name, params.threadLocalsRegion.start(), params.threadLocalsRegion.nBytes());
        }
        this.breakpointIsAtInstructionPointer = platform().isa == ISA.SPARC;
        final String stackName = this.entityName + " Stack";
        this.teleStack = new TeleStack(teleProcess.vm(), this, stackName, params.stackRegion.start(), params.stackRegion.nBytes());
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");

        tracer.end(null);
    }

    public void updateCache(long epoch) {
        if (epoch > lastUpdateEpoch) {
            Trace.line(TRACE_VALUE + 1, tracePrefix() + "refresh thread=" + this);
            if (state.allowsDataAccess()) {
                refreshBreakpoint();
                threadLocalsBlock.updateCache(epoch);
            }
            lastUpdateEpoch = epoch;
        } else {
            Trace.line(TRACE_VALUE, tracePrefix() + "redundant update epoch=" + epoch + ": " + this);
        }
    }

    public final String entityName() {
        return entityName;
    }

    public String entityDescription() {
        return entityDescription;
    }

    public final MaxEntityMemoryRegion<MaxThread> memoryRegion() {
        // The thread has no VM memory allocated for itself; it allocates stack and locals spaces from the OS.
        return null;
    }

    public final List<MaxMemoryRegion> memoryAllocations() {
        final List<MaxMemoryRegion> allocations = new ArrayList<MaxMemoryRegion>(2);
        if (teleStack.memoryRegion() != null) {
            allocations.add(teleStack.memoryRegion());
        }
        if (threadLocalsBlock.memoryRegion() != null) {
            allocations.add(threadLocalsBlock.memoryRegion());
        }
        return allocations;
    }

    public boolean contains(Address address) {
        return teleStack.contains(address) || (threadLocalsBlock != null && threadLocalsBlock.contains(address));
    }

    public final TeleObject representation() {
        return teleVmThread();
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

    public final boolean isPrimordial() {
        return id() == 0;
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

    public final boolean isLive() {
        return state != DEAD;
    }

    public final MaxThreadState state() {
        return state;
    }

    public final VmTargetBreakpoint breakpoint() {
        return breakpoint;
    }

    public final TeleThreadLocalsBlock localsBlock() {
        return threadLocalsBlock;
    }

    public final TeleRegisterSet registers() {
        return teleRegisterSet;
    }

    public final TeleStack stack() {
        return teleStack;
    }

    public final MachineCodeLocation ipLocation() {
        if (!isLive()) {
            return null;
        }
        // No need to refresh registers: the instruction pointer is updated by updateAfterGather() which
        // ensures that it is always in sync.
        return codeLocationFactory().createMachineCodeLocation(teleRegisterSet.instructionPointer(), "Instruction pointer");
    }

    public final TeleVmThread teleVmThread() {
        return threadLocalsBlock.teleVmThread();
    }

    public final String vmThreadName() {
        if (teleVmThread() != null) {
            return teleVmThread().name();
        }
        return null;
    }

    /**
     * @return a printable version of the thread's internal state that only shows key aspects
     */
    public final String toShortString() {
        final StringBuilder sb = new StringBuilder(100);
        sb.append(isPrimordial() ? "primordial" : (teleVmThread() == null ? "native" : teleVmThread().name()));
        sb.append("[id=").append(id());
        sb.append(",handle=").append(handleString());
        sb.append(",local handle=").append(localHandle());
        sb.append(",type=").append(isPrimordial() ? "primordial" : (isJava() ? "Java" : "native"));
        sb.append(",stat=").append(state.toString());
        sb.append("]");
        return sb.toString();
    }

    /**
     * Imposes a total ordering between threads based on their {@linkplain #id() identifiers}.
     */
    public final int compareTo(TeleNativeThread other) {
        return Longs.compare(handle(), other.handle());
    }

    /**
     * Immutable; thread-safe.
     *
     * @return the process in which this thread is running.
     */
    public TeleProcess teleProcess() {
        return teleProcess;
    }

    final StackFrame top() {
        StackFrame top = new TeleStackFrameWalker(vm(), this).frames(2).get(0);
        return top;
    }

    /**
     * @param maxDepth the maximum length of the returned frame list
     * @return the most currently refreshed frames on the thread's stack, never null.
     */
    final List<StackFrame> frames(int maxDepth) {
        final long epoch = teleProcess().epoch();
        if (framesRefreshedEpoch < epoch || maxDepth != this.framesMaxDepth) {
            Trace.line(TRACE_VALUE + 1, tracePrefix() + "refreshFrames (epoch=" + epoch + ") for " + this);
            threadLocalsBlock.updateCache(epoch);
            final List<StackFrame> newFrames = new TeleStackFrameWalker(vm(), this).frames(maxDepth);
            framesMaxDepth = maxDepth;
            assert !newFrames.isEmpty();
            // See if the new stack is structurally equivalent to its predecessor, even if the contents of the top
            // frame may have changed.
            if (newFrames.size() != this.frames.size()) {
                // Clear structural change; lengths are different
                framesLastChangedEpoch = epoch;
            } else {
                // Lengths are the same; see if any frames differ.
                final Iterator<StackFrame> oldFramesIterator = this.frames.iterator();
                final Iterator<StackFrame> newFramesIterator = newFrames.iterator();
                while (oldFramesIterator.hasNext()) {
                    final StackFrame oldFrame = oldFramesIterator.next();
                    final StackFrame newFrame = newFramesIterator.next();
                    if (!oldFrame.isSameFrame(newFrame)) {
                        framesLastChangedEpoch = epoch;
                        break;
                    }
                }
            }
            framesRefreshedEpoch = epoch;
            this.frames = newFrames;
        }
        return frames;
    }

    /**
     * Tracks when the structure of the stack changes, in any respect other than
     * the contents of the top frame.
     *
     * @return the last process epoch at which the structure of the stack changed.
     */
    final Long framesLastChangedEpoch() {
        return framesLastChangedEpoch;
    }

    /**
     * Gets the value of maxDepth when the frames cache was last updated.
     */
    final int framesMaxDepth() {
        return framesMaxDepth;
    }

    /**
     * Updates this thread with the information information made available while
     * {@linkplain TeleProcess#gatherThreads(List) gathering} threads. This information is made available
     * by the native tele layer as threads are discovered. Subsequent refreshing of cached thread state (such a
     * {@linkplain TeleRegisterSet#updateCache() registers}, {@linkplain #refreshFrames(boolean) stack frames} and
     * {@linkplain #refreshThreadLocals() VM thread locals}) depends on this information being available and up to date.
     *
     * @param state the state of the thread
     * @param instructionPointer the current value of the instruction pointer for the thread
     * @param threadLocalsRegion the memory region reported to be holding the thread local block; null if not available
     * @param tlaSize the size of each Thread Locals Area in the thread local block.
     */
    final void updateAfterGather(MaxThreadState state, Pointer instructionPointer, TeleFixedMemoryRegion threadLocalsRegion, int tlaSize) {
        this.state = state;
        teleRegisterSet.setInstructionPointer(instructionPointer);
        threadLocalsBlock.updateAfterGather(threadLocalsRegion, tlaSize);
    }

    /**
     * Marks the thread as having died in the process; flushes all state accordingly.
     */
    final void setDead() {
        state = DEAD;
        clearFrames();
        breakpoint = null;
        frameCache = null;
        threadLocalsBlock.clear();
    }

    /**
     * If this thread is currently at a {@linkplain #breakpoint() breakpoint} it is single stepped to the next
     * instruction.
     */
    void evadeBreakpoint() throws OSExecutionRequestException {
        if (breakpoint != null && !breakpoint.isTransient()) {
            assert !breakpoint.isActive() : "Cannot single step at an activated breakpoint";
            Trace.line(TRACE_VALUE + 1, tracePrefix() + "single step to evade breakpoint=" + breakpoint);
            teleProcess().singleStep(this, true);
        }
    }

    /**
     * Refreshes the information about the {@linkplain #breakpoint() breakpoint} this thread is currently stopped at (if
     * any). If this thread is stopped at a breakpoint, its instruction pointer is adjusted so that it is at the
     * instruction on which the breakpoint was set.
     */
    private void refreshBreakpoint() {
        final VmTargetBreakpoint.TargetBreakpointManager breakpointManager = teleProcess().targetBreakpointManager();
        VmTargetBreakpoint breakpoint = null;

        try {
            final Pointer breakpointAddress = breakpointAddressFromInstructionPointer();
            final RemoteCodePointer codePointer = vm().machineCode().makeCodePointer(breakpointAddress);
            if (codePointer != null) {
                breakpoint = breakpointManager.getTargetBreakpointAt(codePointer);
            }
        } catch (TerminatedProcessIOException terminatedProcessIOException) {
        } catch (DataIOError dataIOError) {
            // This is a catch for problems getting accurate state for threads that are not at breakpoints
        }

        if (breakpoint != null) {

            Trace.line(TRACE_VALUE + 1, tracePrefix() + "refreshingBreakpoint (epoch=" + teleProcess().epoch() + ") for " + this);

            state = BREAKPOINT;
            this.breakpoint = breakpoint;
            final Address address = this.breakpoint.codeLocation().address();
            if (updateInstructionPointer(address)) {
                teleRegisterSet.setInstructionPointer(address);
                Trace.line(TRACE_VALUE + 1, tracePrefix() + "refreshingBreakpoint (epoch=" + teleProcess().epoch() + ") IP updated for " + this);
            } else {
                TeleError.unexpected("Error updating instruction pointer to adjust thread after breakpoint at " + address + " was hit: " + this);
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
        frames = Collections.emptyList();
        framesLastChangedEpoch = teleProcess().epoch();
    }

    /**
     * Specifies whether or not the instruction pointer needs to be adjusted when this thread hits a breakpoint to
     * denote the instruction pointer for which the breakpoint was set. For example, on x86 architectures, the
     * instruction pointer is at the instruction following the breakpoint instruction whereas on SPARC, it's
     * at the instruction pointer for which the breakpoint was set.
     */
    private boolean breakpointIsAtInstructionPointer;

    /**
     * Updates the current value of the instruction pointer for this thread.
     *
     * @param address the address to which the instruction should be set
     * @return true if the instruction pointer was successfully updated, false otherwise
     */
    protected abstract boolean updateInstructionPointer(Address address);

    protected abstract boolean readRegisters(byte[] integerRegisters, byte[] floatingPointRegisters, byte[] stateRegisters);

    /**
     * Advances this thread to the next instruction. That is, makes this thread execute a single machine instruction.
     * Note that this method does not block waiting for the tele process to complete the step.
     *
     * @return true if the single step was issued successfully, false otherwise
     */
    protected abstract boolean singleStep();

    protected abstract boolean threadResume();

    protected abstract boolean threadSuspend();

    /**
     * Gets the address of the breakpoint instruction derived from the current instruction pointer. The current
     * instruction pointer is assumed to be at the architecture dependent location immediately after a breakpoint
     * instruction was executed.
     *
     * The implementation of this method in {@link TeleNativeThread} uses the convention for x86 architectures where the
     * the instruction pointer is at the instruction following the breakpoint instruction.
     */
    private Pointer breakpointAddressFromInstructionPointer() {
        final Pointer instructionPointer = teleRegisterSet.instructionPointer();
        if (breakpointIsAtInstructionPointer) {
            return instructionPointer;
        }
        return instructionPointer.minus(teleProcess().targetBreakpointManager().codeSize());
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

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder(100);
        sb.append(isPrimordial() ? "primordial" : (teleVmThread() == null ? "native" : teleVmThread().name()));
        sb.append("[id=").append(id());
        sb.append(",handle=").append(handleString());
        sb.append(",local handle=").append(localHandle());
        sb.append(",state=").append(state);
        sb.append(",type=").append(isPrimordial() ? "primordial" : (isJava() ? "Java" : "native"));
        if (isLive()) {
            sb.append(",ip=0x").append(teleRegisterSet.instructionPointer().toHexString());
            if (isJava()) {
                sb.append(",stack_start=0x").append(stack().memoryRegion().start().toHexString());
                sb.append(",stack_size=").append(stack().memoryRegion().nBytes());
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Class representing a Java-level frame. It implements the interface the JDWP protocol is programmed against.
     */
    private class FrameProviderImpl implements FrameProvider {

        private CiCodePos codePos;
        private ClassMethodActor classMethodActor;
        private int position;
        private StackFrame stackFrame;
        private TeleTargetMethod targetMethod;
        private long[] rawValues;
        private VMValue[] vmValues;
        private boolean isTopFrame;

        public FrameProviderImpl(boolean isTopFrame, TeleTargetMethod targetMethod, StackFrame stackFrame, CiCodePos codePos) {
            this(isTopFrame, targetMethod, stackFrame, codePos, (ClassMethodActor) codePos.method, 0); //descriptor.bytecodeLocation().());
        }

        public FrameProviderImpl(boolean isTopFrame, TeleTargetMethod targetMethod, StackFrame stackFrame, CiCodePos codePos, ClassMethodActor classMethodActor, int position) {
            this.stackFrame = stackFrame;
            this.codePos = codePos;
            this.classMethodActor = classMethodActor;
            this.position = position;
            this.targetMethod = targetMethod;
            this.isTopFrame = isTopFrame;

            if (classMethodActor.codeAttribute().lineNumberTable().entries().length > 0) {
                this.position = classMethodActor.codeAttribute().lineNumberTable().entries()[0].bci();
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
                vmValues[entry.slot()] = vm().maxineValueToJDWPValue(curValue);

                if (curValue.kind().isReference) {
                    values[entry.slot()] = curValue.asReference().toOrigin().toLong();
                } else if (curValue.kind().isWord) {
                    values[entry.slot()] = curValue.asWord().asPointer().toLong();
                }
            }

            this.vmValues = vmValues;
            rawValues = values;
        }

        private Value getValueImpl(int slot) {
            // TODO: Implement this!
            return null;
        }

        public TargetMethodAccess getTargetMethodProvider() {
            return targetMethod;
        }

        public JdwpCodeLocation getLocation() {
            return vm().vmAccess().createCodeLocation(vm().findTeleMethodActor(TeleClassMethodActor.class, classMethodActor), position, false);
        }

        public long getInstructionPointer() {
            // On top frame, the instruction pointer is incorrect, so take it from the thread!
            if (isTopFrame) {
                return teleRegisterSet.instructionPointer().asAddress().toLong();
            }
            return stackFrame.ip.asAddress().toLong();
        }

        public long getFramePointer() {
            return stackFrame.fp.asAddress().toLong();
        }

        public long getStackPointer() {
            return stackFrame.sp.asAddress().toLong();
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
            final CiValue targetLocation = codePos instanceof CiFrame ? ((CiFrame) codePos).getLocalValue(slot) : null;

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

    private List<StackFrame> oldFrames;

    public synchronized FrameProvider[] getFrames() {

        synchronized (teleProcess()) {

            if (oldFrames != frames) {
                oldFrames = frames;
            } else {
                return frameCache;
            }

            final List<FrameProvider> result = new LinkedList<FrameProvider>();
            int z = 0;
            for (final StackFrame stackFrame : frames(Integer.MAX_VALUE)) {
                z++;

                final Address address = stackFrame.ip;
                TeleCompilation compilation = vm().machineCode().findCompilation(address);
                if (compilation == null) {
                    if (stackFrame.targetMethod() == null) {
                        LOGGER.warning("Target method of stack frame (" + stackFrame + ") was null!");
                        continue;
                    }
                    final TargetMethod targetMethod = stackFrame.targetMethod();
                    final ClassMethodActor classMethodActor = targetMethod.classMethodActor();
                    final TeleClassMethodActor teleClassMethodActor = vm().findTeleMethodActor(TeleClassMethodActor.class, classMethodActor);
                    if (teleClassMethodActor == null) {
                        TeleWarning.message("Could not find tele class method actor for " + classMethodActor);
                        continue;
                    }
                    compilation = vm().machineCode().findCompilation(targetMethod.codeStart().toAddress());
                    if (compilation == null) {
                        TeleWarning.message("Could not find tele target method actor for " + classMethodActor);
                        continue;
                    }
                }

                LOGGER.info("Processing stackframe " + stackFrame);

                int index = -1;
                if (stackFrame.targetMethod() != null) {
                    index = stackFrame.targetMethod().findSafepointIndex(CodePointer.from(stackFrame.ip));
                }
                if (index != -1) {
                    final int stopIndex = index;
                    CiFrame frames = compilation.teleTargetMethod().getDebugInfoAtSafepointIndex(stopIndex).frame();

                    if (frames == null) {
                        LOGGER.info("WARNING: No Java frame descriptor found for Java stop " + stopIndex);

                        if (vm().findTeleMethodActor(TeleClassMethodActor.class, compilation.classMethodActor()) == null) {
                            LOGGER.warning("Could not find tele method!");
                        } else {
                            result.add(new FrameProviderImpl(z == 1, compilation.teleTargetMethod(), stackFrame, null, compilation.classMethodActor(), 0));
                        }
                    } else {

                        while (frames != null) {
                            final TeleClassMethodActor curTma = vm().findTeleMethodActor(TeleClassMethodActor.class, (ClassMethodActor) frames.method);

                            LOGGER.info("Found part frame " + frames + " tele method actor: " + curTma);
                            result.add(new FrameProviderImpl(z == 1, compilation.teleTargetMethod(), stackFrame, frames));
                            frames = frames.caller();
                        }
                    }
                } else {
                    LOGGER.info("Not at Java stop!");
                    if (vm().findTeleMethodActor(TeleClassMethodActor.class, compilation.classMethodActor()) == null) {
                        LOGGER.warning("Could not find tele method!");
                    } else {
                        result.add(new FrameProviderImpl(z == 1, compilation.teleTargetMethod(), stackFrame, null, compilation.classMethodActor(), 0));
                    }
                }
            }

            frameCache = result.toArray(new FrameProvider[result.size()]);
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
            vm().vmAccess().resume();
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
        return vm().vmAccess().getReferenceType(getClass());
    }

    public ThreadGroupProvider getThreadGroup() {
        return isJava() ? vm().javaThreadGroupProvider() : vm().nativeThreadGroupProvider();
    }

    public void doSingleStep() {
        LOGGER.info("Asked to do a single step!");
        vm().registerSingleStepThread(this);
    }

    public void doStepOut() {
        LOGGER.info("Asked to do a step out!");
        vm().registerStepOutThread(this);
    }

    public VMAccess getVM() {
        return vm().vmAccess();
    }

    public RegistersGroup getRegistersGroup() {
        final Registers[] registers = new Registers[]{teleRegisterSet.teleIntegerRegisters().getRegisters("Integer Registers"),
                        teleRegisterSet.teleStateRegisters().getRegisters("State Registers"),
                        teleRegisterSet.teleFloatingPointRegisters().getRegisters("Floating Point Registers")};
        return new RegistersGroup(registers);
    }
}
