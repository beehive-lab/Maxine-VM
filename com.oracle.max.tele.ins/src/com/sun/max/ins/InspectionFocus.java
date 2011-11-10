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
package com.sun.max.ins;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.util.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * Holds the focus of user attention, expressed by user actions
 * that select something.
 *
 *  Implements a user model policy that changes
 *  focus as a side effect of some other user action.
 *
 * Other kinds of items could also have focus in the user model.
 */
public class InspectionFocus extends AbstractInspectionHolder {

    private static final int TRACE_VALUE = 2;
    private boolean settingCodeLocation = false;

    public InspectionFocus(Inspection inspection) {
        super(inspection);
    }

    private Set<ViewFocusListener> listeners = CiUtil.newIdentityHashSet();

    public void addListener(ViewFocusListener listener) {
        Trace.line(TRACE_VALUE, tracePrefix() + "adding listener: " + listener);
        listeners.add(listener);
    }

    public void removeListener(ViewFocusListener listener) {
        Trace.line(TRACE_VALUE, tracePrefix() + " removing listener: " + listener);
        listeners.remove(listener);
    }

    public ViewFocusListener[] copyListeners() {
        return listeners.toArray(new ViewFocusListener[listeners.size()]);
    }

    private MaxCodeLocation codeLocation = null;

    private final Object codeLocationTracer = new Object() {
        @Override
        public String toString() {
            return tracePrefix() + "Focus (Code Location):  " + inspection().nameDisplay().longName(codeLocation);
        }
    };

    public void clearAll() {
        thread = null;
        stackFrame = null;
        memoryRegion = null;
        breakpoint  = null;
        heapObject = null;
    }

    /**
     * The current location of user interest in the code being inspected (view state).
     */
    public MaxCodeLocation codeLocation() {
        return codeLocation;
    }

    /**
     * Is there a currently selected code location.
     */
    public boolean hasCodeLocation() {
        return codeLocation != null;
    }

    /**
     * Selects a code location that is of immediate visual interest to the user.
     * This is view state only, not necessarily related to VM execution.
     *
     * @param codeLocation a location in code in the VM, possibly null which means no code location
     * @param interactiveForNative should user be prompted interactively if in native code without
     * any meta information?
     */
    public void setCodeLocation(MaxCodeLocation codeLocation, boolean interactiveForNative) {
        // Terminate any loops in focus setting.
        if (!settingCodeLocation) {
            settingCodeLocation = true;
            try {
                this.codeLocation = codeLocation;
                Trace.line(TRACE_VALUE, codeLocationTracer);
                for (ViewFocusListener listener : copyListeners()) {
                    listener.codeLocationFocusSet(codeLocation, interactiveForNative);
                }
                // User Model Policy: when setting code location, if it happens to match a stack frame of the current thread then focus on that frame.
                if (thread != null && codeLocation != null && codeLocation.hasAddress()) {
                    for (MaxStackFrame maxStackFrame : thread.stack().frames(StackView.DEFAULT_MAX_FRAMES_DISPLAY)) {
                        if (codeLocation.isSameAs(maxStackFrame.codeLocation())) {
                            setStackFrame(stackFrame, false);
                            break;
                        }
                    }
                }
            } finally {
                settingCodeLocation = false;
            }
        }
    }

    /**
     * Selects a code location that is of immediate visual interest to the user.
     * This is view state only, not necessarily related to VM execution.
     *
     * @param codeLocation a location in code in the VM
     */
    public void setCodeLocation(MaxCodeLocation codeLocation) {
        setCodeLocation(codeLocation, TeleVM.promptForNativeCodeView);
    }

    private MaxThread thread;

    private final Object threadFocusTracer = new Object() {
        @Override
        public String toString() {
            final StringBuilder name = new StringBuilder();
            name.append(tracePrefix() + "Focus (Thread): ").append(inspection().nameDisplay().longName(thread)).append(" {").append(thread).append("}");
            return name.toString();
        }
    };

    /**
     * @return the {@link MaxThread} that is the current user focus (view state); non-null once set.
     */
    public MaxThread thread() {
        return thread;
    }

    /**
     * Is there a currently selected thread.
     */
    public boolean hasThread() {
        return thread != null;
    }

    /**
     * Shifts the focus of the Inspection to a particular thread; notify interested views.
     * Sets the code location to the current InstructionPointer of the newly focused thread.
     * This is a view state change that can happen when there is no change to VM  state.
     */
    public void setThread(MaxThread thread) {
        assert thread != null;
        if (!thread.equals(this.thread)) {
            final MaxThread oldThread = this.thread;
            this.thread = thread;
            Trace.line(TRACE_VALUE, threadFocusTracer);
            for (ViewFocusListener listener : copyListeners()) {
                listener.threadFocusSet(oldThread, thread);
            }
            // User Model Policy:  when thread focus changes, restore an old frame focus if possible.
            // If no record of a prior choice and thread is at a breakpoint, focus there.
            // Else focus on the top frame.
            final MaxStackFrame previousStackFrame = frameSelections.get(thread);
            MaxStackFrame newStackFrame = null;
            if (previousStackFrame != null) {
                for (MaxStackFrame stackFrame : this.thread.stack().frames(StackView.DEFAULT_MAX_FRAMES_DISPLAY)) {
                    if (stackFrame.isSameFrame(previousStackFrame)) {
                        newStackFrame = stackFrame;
                        break;
                    }
                }
            }
            if (newStackFrame != null) {
                // Reset frame selection to one previously selected by the user
                setStackFrame(newStackFrame, false);
            } else {
                // No prior frame selection
                final MaxBreakpoint breakpoint = this.thread.breakpoint();
                if (breakpoint != null && !breakpoint.isTransient()) {
                    // thread is at a breakpoint; focus on the breakpoint, which should also cause focus on code and frame
                    setBreakpoint(breakpoint);
                } else {
                    // default is to focus on the top frame
                    setStackFrame(thread.stack().top(), false);
                }
            }
            // User Model Policy:  when thread focus changes, also set the memory region focus to the thread's stack memory.
            setMemoryRegion(this.thread.stack().memoryRegion());

        }
    }

    // Remember most recent frame selection per thread, and restore this selection (if possible) when thread focus changes.
    private final Map<MaxThread, MaxStackFrame> frameSelections = new HashMap<MaxThread, MaxStackFrame>();

    private MaxStackFrame stackFrame;

    private final Object stackFrameFocusTracer = new Object() {
        @Override
        public String toString() {
            return tracePrefix() + "Focus (StackFrame):  " + stackFrame;
        }
    };

    /**
     * @return the {@link MaxStackFrame} that is current user focus (view state).
     */
    public MaxStackFrame stackFrame() {
        return stackFrame;
    }

    /**
     * Is there a currently selected stack frame.
     */
    public boolean hasStackFrame() {
        return stackFrame != null;
    }

    /**
     * Shifts the focus of the Inspection to a particular stack frame in a particular thread; notify interested views.
     * Sets the current thread to be the thread of the frame.
     * This is a view state change that can happen when there is no change to VM state.
     * @param newStackFrame the frame on which to focus.
     * @param interactiveForNative whether (should a side effect be to land in a native method) the user should be consulted if unknown.
     */
    public void setStackFrame(MaxStackFrame newStackFrame, boolean interactiveForNative) {

        if (this.stackFrame != newStackFrame) {
            final MaxStackFrame oldStackFrame = this.stackFrame;
            this.stackFrame = newStackFrame;
            if (newStackFrame != null) {
                final MaxThread newThread = newStackFrame.stack().thread();
                // For consistency, be sure we're in the right thread context before doing anything with the stack frame.
                setThread(newThread);
                frameSelections.put(newThread, newStackFrame);
            }
            Trace.line(TRACE_VALUE, stackFrameFocusTracer);
            for (ViewFocusListener listener : copyListeners()) {
                listener.frameFocusChanged(oldStackFrame, newStackFrame);
            }
        }
        if (newStackFrame != null) {
            // User Model Policy:  When a stack frame becomes the focus, then also focus on the code at the frame's instruction pointer
            // or call return location.
            // Update code location, even if stack frame is the "same", where same means at the same logical position in the stack as the old one.
            // Note that the old and new stack frames are not identical, and in fact may have different instruction pointers.
            final MaxCodeLocation newCodeLocation = newStackFrame.codeLocation();
            if (this.codeLocation == null || !this.codeLocation.isSameAs(newCodeLocation)) {
                setCodeLocation(newCodeLocation, interactiveForNative);
            }
        }
    }

    // never null, zero if none set
    private Address address = Address.zero();

    private final Object addressFocusTracer = new Object() {
        @Override
        public String toString() {
            final StringBuilder name = new StringBuilder();
            name.append(tracePrefix()).append("Focus (Address): ").append(address.toHexString());
            return name.toString();
        }
    };

    /**
     * @return the {@link Address} that is the current user focus (view state), {@link Address#zero()} if none.
     */
    public Address address() {
        return address;
    }

    /**
     * Is there a currently selected {@link Address}.
     */
    public boolean hasAddress() {
        return  !address.isZero();
    }

    /**
     * Shifts the focus of the Inspection to a particular {@link Address}; notify interested views.
     * This is a view state change that can happen when there is no change to the VM state.
     */
    public void setAddress(Address address) {
        InspectorError.check(address != null, "setAddress(null) should use zero Address instead");
        if ((address.isZero() && hasAddress()) || (!address.isZero() && !address.equals(this.address))) {
            final Address oldAddress = this.address;
            this.address = address;
            Trace.line(TRACE_VALUE, addressFocusTracer);
            for (ViewFocusListener listener : copyListeners()) {
                listener.addressFocusChanged(oldAddress, address);
            }
            // User Model Policy:  select the memory region that contains the newly selected address; clears if not known.
            // If
            setMemoryRegion(vm().findMemoryRegion(address));
        }
    }

    private MaxMemoryRegion memoryRegion;

    private final Object memoryRegionFocusTracer = new Object() {
        @Override
        public String toString() {
            final StringBuilder name = new StringBuilder();
            name.append(tracePrefix()).append("Focus (MemoryRegion): ").append(memoryRegion.regionName());
            return name.toString();
        }
    };

    /**
     * @return the {@linkplain MaxMemoryRegion memory region} that is the current user focus (view state).
     */
    public MaxMemoryRegion memoryRegion() {
        return memoryRegion;
    }

    /**
     * Is there a currently selected {@linkplain MaxMemoryRegion memory region}.
     */
    public boolean hasMemoryRegion() {
        return memoryRegion != null;
    }

    /**
     * Shifts the focus of the Inspection to a particular {@linkplain MaxMemoryRegion memory region}; notify interested views.
     * If the region is a  stackRegion, then set the current thread to the thread owning the stack.
     * This is a view state change that can happen when there is no change to the VM state.
     */
    public void setMemoryRegion(MaxMemoryRegion memoryRegion) {
        // TODO (mlvdv) see about setting to null if a thread is observed to have died, or mark the region as dead?
        if ((memoryRegion == null && this.memoryRegion != null) || (memoryRegion != null && !memoryRegion.sameAs(this.memoryRegion))) {
            final MaxMemoryRegion oldMemoryRegion = this.memoryRegion;
            this.memoryRegion = memoryRegion;
            Trace.line(TRACE_VALUE, memoryRegionFocusTracer);
            for (ViewFocusListener listener : copyListeners()) {
                listener.memoryRegionFocusChanged(oldMemoryRegion, memoryRegion);
            }
            // User Model Policy:  When a stack memory region gets selected for focus, also set focus to the thread owning the stack.
//            if (_memoryRegion != null) {
//                final MaxThread thread = vm().threadContaining(_memoryRegion.start());
//                if (thread != null) {
//                    setThread(thread);
//                }
//            }
        }
    }

    private MaxBreakpoint breakpoint;

    private final Object breakpointFocusTracer = new Object() {
        @Override
        public String toString() {
            return tracePrefix() + "Focus(Breakpoint):  " + (breakpoint == null ? "null" : inspection().nameDisplay().longName(breakpoint.codeLocation()));
        }
    };

    /**
     * Currently selected breakpoint, typically controlled by the {@link BreakpointsView}.
     * May be null.
     */
    public MaxBreakpoint breakpoint() {
        return breakpoint;
    }

    /**
     * Is there a currently selected breakpoint in the BreakpointsInspector.
     */
    public boolean hasBreakpoint() {
        return breakpoint != null;
    }

    /**
     * Selects a breakpoint that is of immediate visual interest to the user, possibly null.
     * This is view state only, not necessarily related to VM execution.
     */
    public void setBreakpoint(MaxBreakpoint maxBreakpoint) {
        if (breakpoint != maxBreakpoint) {
            final MaxBreakpoint oldMaxBreakpoint = breakpoint;
            breakpoint = maxBreakpoint;
            Trace.line(TRACE_VALUE, breakpointFocusTracer);
            for (ViewFocusListener listener : copyListeners()) {
                listener.breakpointFocusSet(oldMaxBreakpoint, maxBreakpoint);
            }
        }
        if (maxBreakpoint != null) {
            MaxThread threadAtBreakpoint = null;
            for (MaxThread thread : vm().state().threads()) {
                if (thread.breakpoint() == maxBreakpoint) {
                    threadAtBreakpoint = thread;
                    break;
                }
            }
            // User Model Policy:  when a breakpoint acquires focus, also set focus to the
            // thread, if any, that is stopped at the breakpoint.  If no thread stopped,
            // then just focus on the code location.
            if (threadAtBreakpoint != null) {
                setStackFrame(threadAtBreakpoint.stack().top(), false);
            } else {
                setCodeLocation(maxBreakpoint.codeLocation());
            }
        }
    }

    private MaxWatchpoint watchpoint;

    private final Object watchpointFocusTracer = new Object() {
        @Override
        public String toString() {
            return tracePrefix() + "Focus(Watchpoint):  " + (watchpoint == null ? "null" : watchpoint.toString());
        }
    };

    /**
     * Currently selected watchpoint, typically controlled by the {@link WatchpointsView}.
     * May be null.
     */
    public MaxWatchpoint watchpoint() {
        return watchpoint;
    }

    /**
     * Is there a currently selected watchpoint in the WatchpointsInspector.
     */
    public boolean hasWatchpoint() {
        return watchpoint != null;
    }

    /**
     * Selects a watchpoint that is of immediate visual interest to the user, possibly null.
     * This is view state only, not necessarily related to VM execution.
     */
    public void setWatchpoint(MaxWatchpoint watchpoint) {
        if (this.watchpoint != watchpoint) {
            final MaxWatchpoint oldWatchpoint = this.watchpoint;
            this.watchpoint = watchpoint;
            Trace.line(TRACE_VALUE, watchpointFocusTracer);
            for (ViewFocusListener listener : copyListeners()) {
                listener.watchpointFocusSet(oldWatchpoint, watchpoint);
            }
        }
    }


    private TeleObject heapObject;

    private final Object objectFocusTracer = new Object() {
        @Override
        public String toString() {
            return tracePrefix() + "Focus(Heap Object):  " + (heapObject == null ? "null" : heapObject.toString());
        }
    };

    /**
     * Currently selected object in the VM heap; may be null.
     */
    public TeleObject heapObject() {
        return heapObject;
    }

    /**
     * Whether there is a currently selected heap object.
     */
    public boolean hasHeapObject() {
        return heapObject != null;
    }

    /**
     * Shifts the focus of the Inspection to a particular heap object in the VM; notify interested views.
     * This is a view state change that can happen when there is no change to VM state.
     */
    public void setHeapObject(TeleObject heapObject) {
        if (this.heapObject != heapObject) {
            final TeleObject oldTeleObject = this.heapObject;
            this.heapObject = heapObject;
            Trace.line(TRACE_VALUE, objectFocusTracer);
            for (ViewFocusListener listener : copyListeners()) {
                listener.heapObjectFocusChanged(oldTeleObject, heapObject);
            }
        }
    }

}
