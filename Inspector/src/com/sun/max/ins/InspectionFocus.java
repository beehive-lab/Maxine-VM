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
package com.sun.max.ins;

import com.sun.max.collect.*;
import com.sun.max.ins.debug.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.*;


/**
 * Holds the focus of user attention, expressed by user actions
 * that select something.
 *
 *  Implements a user model policy that changes
 *  focus as a side effect of some other user action.
 *
 * Other kinds of items could also have focus in the user model.
 *
 * @author Michael Van De Vanter
 */
public class InspectionFocus extends AbstractInspectionHolder {

    private static final int TRACE_VALUE = 2;

    public InspectionFocus(Inspection inspection) {
        super(inspection);
    }

    private IdentityHashSet<ViewFocusListener> _listeners = new IdentityHashSet<ViewFocusListener>();

    public void addListener(ViewFocusListener listener) {
        Trace.line(TRACE_VALUE, tracePrefix() + "adding listener: " + listener);
        _listeners.add(listener);
    }

    public void removeListener(ViewFocusListener listener) {
        Trace.line(TRACE_VALUE, tracePrefix() + " removing listener: " + listener);
        _listeners.remove(listener);
    }

    private TeleCodeLocation _codeLocation = maxVM().createCodeLocation(Address.zero());

    private final Object _codeLocationTracer = new Object() {
        @Override
        public String toString() {
            return tracePrefix() + "Focus (Code Location):  " + inspection().nameDisplay().longName(_codeLocation);
        }
    };

    public void clearAll() {
        _thread = null;
        _stackFrame = null;
        _memoryRegion = null;
        _breakpoint  = null;
        _heapObject = null;
    }

    /**
     * The current location of user interest in the code being inspected (view state).
     */
    public TeleCodeLocation codeLocation() {
        return _codeLocation;
    }

    /**
     * Is there a currently selected code location.
     */
    public boolean hasCodeLocation() {
        return _codeLocation != null;
    }

    /**
     * Selects a code location that is of immediate visual interest to the user.
     * This is view state only, not necessarily related to VM execution.
     */
    public void setCodeLocation(TeleCodeLocation teleCodeLocation, boolean interactiveForNative) {
        _codeLocation = teleCodeLocation;
        Trace.line(TRACE_VALUE, _codeLocationTracer);
        for (ViewFocusListener listener : _listeners.clone()) {
            listener.codeLocationFocusSet(teleCodeLocation, interactiveForNative);
        }
        // User Model Policy: when setting code location, if it happens to match a stack frame of the current thread then focus on that frame.
        if (_thread != null && _codeLocation.hasTargetCodeLocation()) {
            final Address address = _codeLocation.targetCodeInstructionAddresss();
            final Sequence<StackFrame> frames = _thread.frames();
            for (StackFrame stackFrame : frames) {
                if (stackFrame.instructionPointer().equals(address)) {
                    setStackFrame(_thread, stackFrame, false);
                    break;
                }
            }
        }
    }



    private TeleNativeThread _thread;

    private final Object _threadFocusTracer = new Object() {
        @Override
        public String toString() {
            final StringBuilder name = new StringBuilder();
            name.append(tracePrefix() + "Focus (Thread): ").append(inspection().nameDisplay().longName(_thread)).append(" {").append(_thread).append("}");
            return name.toString();
        }
    };

    /**
     * @return the {@link TeleNativeThread} that is the current user focus (view state); non-null once set.
     */
    public TeleNativeThread thread() {
        return _thread;
    }

    /**
     * Is there a currently selected thread.
     */
    public boolean hasThread() {
        return _thread != null;
    }

    /**
     * Shifts the focus of the Inspection to a particular thread; notify interested inspectors.
     * Sets the code location to the current InstructionPointer of the newly focused thread.
     * This is a view state change that can happen when there is no change to VM  state.
     */
    public void setThread(TeleNativeThread thread) {
        assert thread != null;
        if (!thread.equals(_thread)) {
            final TeleNativeThread oldThread = _thread;
            _thread = thread;
            Trace.line(TRACE_VALUE, _threadFocusTracer);
            for (ViewFocusListener listener : _listeners.clone()) {
                listener.threadFocusSet(oldThread, thread);
            }
            // User Model Policy:  when thread focus changes, restore an old frame focus if possible.
            // If no record of a prior choice and thread is at a breakpoint, focus there.
            // Else focus on the top frame.
            final StackFrame previousStackFrame = _frameSelections.get(thread);
            StackFrame newStackFrame = null;
            if (previousStackFrame != null) {
                for (StackFrame stackFrame : _thread.frames()) {
                    if (stackFrame.isSameFrame(previousStackFrame)) {
                        newStackFrame = stackFrame;
                        break;
                    }
                }
            }
            if (newStackFrame != null) {
                // Reset frame selection to one previously selected by the user
                setStackFrame(thread, newStackFrame, false);
            } else {
                // No prior frame selection
                final TeleTargetBreakpoint breakpoint = _thread.breakpoint();
                if (breakpoint != null) {
                    // thread is at a breakpoint; focus on the breakpoint, which should also cause focus on code and frame
                    setBreakpoint(breakpoint);
                } else {
                    // default is to focus on the top frame
                    setStackFrame(thread, thread.frames().first(), false);
                }
            }
            // User Model Policy:  when thread focus changes, also set the memory region focus to the thread's stack.
            setMemoryRegion(_thread.stack());

        }
    }

    // Remember most recent frame selection per thread, and restore this selection (if possible) when thread focus changes.
    private final VariableMapping<TeleNativeThread, StackFrame> _frameSelections = HashMapping.<TeleNativeThread, StackFrame>createVariableEqualityMapping();

    private StackFrame _stackFrame;
    // Since frames don't record what stack they're in, we must keep a reference to the thread of the frame.
    private TeleNativeThread _threadForStackFrame;

    private final Object _stackFrameFocusTracer = new Object() {
        @Override
        public String toString() {
            return tracePrefix() + "Focus (StackFrame):  " + _stackFrame;
        }
    };

    /**
     * @return the {@link StackFrame} that is current user focus (view state).
     */
    public StackFrame stackFrame() {
        return _stackFrame;
    }

    /**
     * Shifts the focus of the Inspection to a particular stack frame in a particular thread; notify interested inspectors.
     * Sets the current thread to be the thread of the frame.
     * This is a view state change that can happen when there is no change to VM state.
     *
     * @param teleNativeThread the thread in whose stack the frame resides
     * @param stackFrame the frame on which to focus.
     * @param interactiveForNative whether (should a side effect be to land in a native method) the user should be consulted if unknown.
     */
    public void setStackFrame(TeleNativeThread teleNativeThread, StackFrame stackFrame, boolean interactiveForNative) {
        if (!teleNativeThread.equals(_threadForStackFrame) || !stackFrame.isSameFrame(_stackFrame)) {
            final StackFrame oldStackFrame = _stackFrame;
            _threadForStackFrame = teleNativeThread;
            _stackFrame = stackFrame;
            _frameSelections.put(teleNativeThread, stackFrame);
            Trace.line(TRACE_VALUE, _stackFrameFocusTracer);
            // For consistency, be sure we're in the right thread context before doing anything with the stack frame.
            setThread(teleNativeThread);
            for (ViewFocusListener listener : _listeners.clone()) {
                listener.stackFrameFocusChanged(oldStackFrame, teleNativeThread, stackFrame);
            }
        }
        // User Model Policy:  When a stack frame becomes the focus, then also focus on the code at the frame's instruction pointer.
        // Update code location, even if stack frame is the "same", where same means at the same logical position in the stack as the old one.
        // Note that the old and new stack frames are not identical, and in fact may have different instruction pointers.
        if (!_codeLocation.hasTargetCodeLocation() || !_codeLocation.targetCodeInstructionAddresss().equals(stackFrame.instructionPointer())) {
            setCodeLocation(maxVM().createCodeLocation(stackFrame.instructionPointer()), interactiveForNative);
        }
    }


    // never null, zero if none set
    private Address _address = Address.zero();

    private final Object _addressFocusTracer = new Object() {
        @Override
        public String toString() {
            final StringBuilder name = new StringBuilder();
            name.append(tracePrefix()).append("Focus (Address): ").append(_address.toHexString());
            return name.toString();
        }
    };

    /**
     * @return the {@link Address} that is the current user focus (view state), {@link Address#zero()} if none.
     */
    public Address address() {
        return _address;
    }

    /**
     * Is there a currently selected {@link Address}.
     */
    public boolean hasAddress() {
        return  !_address.isZero();
    }

    /**
     * Shifts the focus of the Inspection to a particular {@link Address}; notify interested inspectors.
     * This is a view state change that can happen when there is no change to the VM state.
     */
    public void setAddress(Address address) {
        ProgramError.check(address != null, "setAddress(null) should use zero Address instead");
        if ((address.isZero() && hasAddress()) || (!address.isZero() && !address.equals(_address))) {
            final Address oldAddress = _address;
            _address = address;
            Trace.line(TRACE_VALUE, _addressFocusTracer);
            for (ViewFocusListener listener : _listeners.clone()) {
                listener.addressFocusChanged(oldAddress, address);
            }
            // User Model Policy:  select the memory region that contains the newly selected address; clears if not known.
            // If
            setMemoryRegion(maxVM().memoryRegionContaining(address));
        }
    }


    private MemoryRegion _memoryRegion;

    private final Object _memoryRegionFocusTracer = new Object() {
        @Override
        public String toString() {
            final StringBuilder name = new StringBuilder();
            name.append(tracePrefix()).append("Focus (MemoryRegion): ").append(_memoryRegion.description());
            return name.toString();
        }
    };

    /**
     * @return the {@link MemoryRegion} that is the current user focus (view state).
     */
    public MemoryRegion memoryRegion() {
        return _memoryRegion;
    }

    /**
     * Is there a currently selected {@link MemoryRegion}.
     */
    public boolean hasMemoryRegion() {
        return _memoryRegion != null;
    }

    /**
     * Shifts the focus of the Inspection to a particular {@link MemoryRegion}; notify interested inspectors.
     * If the region is a  stack, then set the current thread to the thread owning the stack.
     * This is a view state change that can happen when there is no change to the VM state.
     */
    public void setMemoryRegion(MemoryRegion memoryRegion) {
        // TODO (mlvdv) see about setting to null if a thread is observed to have died, or mark the region as dead?
        if ((memoryRegion == null && _memoryRegion != null) || (memoryRegion != null && !memoryRegion.sameAs(_memoryRegion))) {
            final MemoryRegion oldMemoryRegion = _memoryRegion;
            _memoryRegion = memoryRegion;
            Trace.line(TRACE_VALUE, _memoryRegionFocusTracer);
            for (ViewFocusListener listener : _listeners.clone()) {
                listener.memoryRegionFocusChanged(oldMemoryRegion, memoryRegion);
            }
            // User Model Policy:  When a stack memory region gets selected for focus, also set focus to the thread owning the stack.
//            if (_memoryRegion != null) {
//                final TeleNativeThread teleNativeThread = teleVM().threadContaining(_memoryRegion.start());
//                if (teleNativeThread != null) {
//                    setThread(teleNativeThread);
//                }
//            }
        }
    }


    private TeleBreakpoint _breakpoint;

    private final Object _breakpointFocusTracer = new Object() {
        @Override
        public String toString() {
            return tracePrefix() + "Focus(Breakpoint):  " + (_breakpoint == null ? "null" : inspection().nameDisplay().longName(_breakpoint.teleCodeLocation()));
        }
    };

    /**
     * Currently selected breakpoint, typically controlled by the {@link BreakpointsInspector}.
     * May be null.
     */
    public TeleBreakpoint breakpoint() {
        return _breakpoint;
    }

    /**
     * Is there a currently selected breakpoint in the BreakpointsInspector.
     */
    public boolean hasBreakpoint() {
        return _breakpoint != null;
    }

    /**
     * Selects a breakpoint that is of immediate visual interest to the user, possibly null.
     * This is view state only, not necessarily related to VM execution.
     */
    public void setBreakpoint(TeleBreakpoint teleBreakpoint) {
        if (_breakpoint != teleBreakpoint) {
            final TeleBreakpoint oldTeleBreakpoint = _breakpoint;
            _breakpoint = teleBreakpoint;
            Trace.line(TRACE_VALUE, _breakpointFocusTracer);
            for (ViewFocusListener listener : _listeners.clone()) {
                listener.breakpointFocusSet(oldTeleBreakpoint, teleBreakpoint);
            }
        }
        if (teleBreakpoint != null) {
            TeleNativeThread threadAtBreakpoint = null;
            for (TeleNativeThread teleNativeThread : maxVM().threads()) {
                if (teleNativeThread.breakpoint() == teleBreakpoint) {
                    threadAtBreakpoint = teleNativeThread;
                    break;
                }
            }
            // User Model Policy:  when a breakpoint acquires focus, also set focus to the
            // thread, if any, that is stopped at the breakpoint.  If no thread stopped,
            // then just focus on the code location.
            if (threadAtBreakpoint != null) {
                setStackFrame(threadAtBreakpoint, threadAtBreakpoint.frames().first(), false);
            } else {
                setCodeLocation(teleBreakpoint.teleCodeLocation(), false);
            }
        }
    }

    private TeleObject _heapObject;

    private final Object _objectFocusTracer = new Object() {
        @Override
        public String toString() {
            return tracePrefix() + "Focus(Heap Object):  " + (_heapObject == null ? "null" : _heapObject.toString());
        }
    };

    /**
     * Currently selected object in the tele VM heap; may be null.
     */
    public TeleObject heapObject() {
        return _heapObject;
    }

    /**
     * Whether there is a currently selected heap object.
     */
    public boolean hasHeapObject() {
        return _heapObject != null;
    }

    /**
     * Shifts the focus of the Inspection to a particular heap object in the VM; notify interested inspectors.
     * This is a view state change that can happen when there is no change to VM state.
     */
    public void setHeapObject(TeleObject heapObject) {
        if (_heapObject != heapObject) {
            final TeleObject oldTeleObject = _heapObject;
            _heapObject = heapObject;
            Trace.line(TRACE_VALUE, _objectFocusTracer);
            for (ViewFocusListener listener : _listeners.clone()) {
                listener.heapObjectFocusChanged(oldTeleObject, heapObject);
            }
        }
    }


}
