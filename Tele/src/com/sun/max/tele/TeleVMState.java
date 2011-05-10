/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele;

import java.io.*;
import java.util.*;

import com.sun.max.tele.debug.*;

/**
 * Implements the (mostly) immutable history of Maxine VM states during a debugging sessions.
 *
 * @author Michael Van De Vanter
 */
public final class TeleVMState implements MaxVMState {

    private static final List<TeleNativeThread> EMPTY_THREAD_LIST = Collections.emptyList();
    private static final List<MaxThread> EMPTY_MAXTHREAD_LIST  = Collections.emptyList();
    private static final List<TeleBreakpointEvent> EMPTY_BREAKPOINTEVENT_LIST = Collections.emptyList();
    private static final List<MaxBreakpointEvent> EMPTY_MAXBREAKPOINTEVENT_LIST = Collections.emptyList();
    private static final List<MaxMemoryRegion> EMPTY_MAXMEMORYREGION_LIST = Collections.emptyList();

    private final MaxInspectionMode inspectionMode;
    private final MaxProcessState processState;
    private final long serialID;
    private final long epoch;
    private final List<MaxMemoryRegion> memoryAllocations;
    private final List<MaxThread> threads;
    private final MaxThread singleStepThread;
    private final List<MaxThread> threadsStarted;
    private final List<MaxThread> threadsDied;
    private final List<MaxBreakpointEvent> breakpointEvents;
    private final MaxWatchpointEvent maxWatchpointEvent;
    private final boolean isInGC;
    private final TeleVMState previous;

    /**
     * @param inspectionMode TODO
     * @param processState current state of the VM
     * @param epoch current process epoch counter
     * @param memoryAllocations memory regions the VM has allocated from the OS
     * @param threads threads currently active in the VM
     * @param singleStepThread thread just single-stepped, null if none
     * @param threadsStarted threads created since the previous state
     * @param threadsDied threads died since the previous state
     * @param breakpointEvents information about threads currently at breakpoints, empty if none
     * @param teleWatchpointEvent information about a thread currently at a watchpoint, null if none
     * @param isInGC is the VM, when paused, in a GC
     * @param previous previous state
     */
    public TeleVMState(
                    MaxInspectionMode inspectionMode,
                    ProcessState processState,
                    long epoch,
                    List<MaxMemoryRegion> memoryAllocations,
                    Collection<TeleNativeThread> threads,
                    TeleNativeThread singleStepThread,
                    List<TeleNativeThread> threadsStarted,
                    List<TeleNativeThread> threadsDied,
                    List<TeleBreakpointEvent> breakpointEvents,
                    TeleWatchpointEvent teleWatchpointEvent, boolean isInGC, TeleVMState previous) {
        this.inspectionMode = inspectionMode;
        switch(processState) {
            case RUNNING:
                this.processState = MaxProcessState.RUNNING;
                break;
            case STOPPED:
                this.processState = MaxProcessState.STOPPED;
                break;
            case TERMINATED:
                this.processState = MaxProcessState.TERMINATED;
                break;
            default:
                this.processState = MaxProcessState.UNKNOWN;
        }
        this.serialID = previous == null ? 0 : previous.serialID() + 1;
        this.epoch = epoch;

        // Reuse old list of memory regions if unchanged
        if (previous != null && previous.memoryAllocations.equals(memoryAllocations)) {
            this.memoryAllocations = previous.memoryAllocations;
        } else {
            this.memoryAllocations = Collections.unmodifiableList(memoryAllocations);
        }

        this.singleStepThread = singleStepThread;

        if (threadsStarted.size() == 0) {
            this.threadsStarted = EMPTY_MAXTHREAD_LIST;
        } else {
            this.threadsStarted = Collections.unmodifiableList(new ArrayList<MaxThread>(threadsStarted));
        }

        if (threadsDied.size() == 0) {
            this.threadsDied = EMPTY_MAXTHREAD_LIST;
        } else {
            this.threadsDied = Collections.unmodifiableList(new ArrayList<MaxThread>(threadsDied));
        }

        if (breakpointEvents.isEmpty()) {
            this.breakpointEvents = EMPTY_MAXBREAKPOINTEVENT_LIST;
        } else {
            this.breakpointEvents = Collections.unmodifiableList(new ArrayList<MaxBreakpointEvent>(breakpointEvents));
        }

        this.maxWatchpointEvent = teleWatchpointEvent;
        this.isInGC = isInGC;
        this.previous = previous;

        // Compute the current active thread list.
        if (previous == null) {
            // First state transition in the history.
            this.threads = Collections.unmodifiableList(new ArrayList<MaxThread>(threadsStarted));
        } else if (threadsStarted.size() + threadsDied.size() == 0)  {
            // No changes since predecessor; share the thread list.  This is the most common case.
            this.threads = previous.threads();
        } else {
            // There have been some thread changes; make a new (immutable) sequence for the new state
            this.threads = Collections.unmodifiableList(new ArrayList<MaxThread>(threads));
        }
    }

    public MaxProcessState processState() {
        return processState;
    }

    public long serialID() {
        return serialID;
    }

    public long epoch() {
        return epoch;
    }

    public List<MaxMemoryRegion> memoryAllocations() {
        return memoryAllocations;
    }

    public List<MaxThread> threads() {
        return threads;
    }

    public MaxThread singleStepThread() {
        return singleStepThread;
    }

    public List<MaxThread> threadsStarted() {
        return threadsStarted;
    }

    public  List<MaxThread> threadsDied() {
        return threadsDied;
    }

    public  List<MaxBreakpointEvent> breakpointEvents() {
        return breakpointEvents;
    }

    public MaxWatchpointEvent watchpointEvent() {
        return maxWatchpointEvent;
    }

    public boolean isInGC() {
        return isInGC;
    }

    public MaxVMState previous() {
        return previous;
    }

    public boolean newerThan(MaxVMState maxVMState) {
        return maxVMState == null || serialID > maxVMState.serialID();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(50);
        sb.append(getClass().getSimpleName()).append("(");
        sb.append(Long.toString(serialID)).append(", ");
        sb.append(processState.toString()).append(", ");
        sb.append("epoch=").append(Long.toString(epoch)).append(", ");
        sb.append("gc=").append(Boolean.toString(isInGC)).append(", ");
        sb.append("prev=");
        if (previous == null) {
            sb.append("null");
        } else {
            sb.append(previous.processState().label());
        }
        sb.append(")");
        return sb.toString();
    }

    public void writeSummary(PrintStream printStream) {
        MaxVMState state = this;
        while (state != null) {
            final StringBuilder sb = new StringBuilder(100);
            sb.append(Long.toString(state.serialID())).append(":  ");
            sb.append("proc=(").append(state.processState().label()).append(", epoch=").append(Long.toString(state.epoch())).append(") ");
            sb.append("gc=").append(state.isInGC()).append(" ");
            printStream.println(sb.toString());
            if (state.singleStepThread() != null) {
                printStream.println("\tsingle-stepped=" + state.singleStepThread().toShortString());
            }
            if (state.previous() != null && state.memoryAllocations() == state.previous().memoryAllocations()) {
                printStream.println("\tmemory regions: <unchanged>");
            } else {
                printStream.println("\tmemory regions:");
                for (MaxMemoryRegion memoryRegion : state.memoryAllocations()) {
                    printStream.println("\t\t" + memoryRegion.getClass().getName() + "(\"" + memoryRegion.regionName() + "\" @ 0x" + memoryRegion.start().toHexString() + ")");
                }
            }
            if (state.previous() != null && state.threads() == state.previous().threads()) {
                printStream.println("\tthreads active: <unchanged>");
            } else if (state.threads().size() == 0) {
                printStream.println("\tthreads active: <empty>");
            } else {
                printStream.println("\tthreads active:");
                for (MaxThread thread : state.threads()) {
                    printStream.println("\t\t" + thread.toShortString());
                }
            }
            if (state.threadsStarted().size() > 0) {
                printStream.println("\tthreads newly started:");
                for (MaxThread thread : state.threadsStarted()) {
                    printStream.println("\t\t" + thread.toShortString());
                }
            }
            if (state.threadsDied().size() > 0) {
                printStream.println("\tthreads newly died:");
                for (MaxThread thread : state.threadsDied()) {
                    printStream.println("\t\t" + thread.toShortString());
                }
            }
            if (state.breakpointEvents().size() > 0) {
                printStream.println("\tbreakpoint events");
                for (MaxBreakpointEvent breakpointEvent : state.breakpointEvents()) {
                    printStream.println("\t\t" + breakpointEvent.toString());
                }
            }
            if (state.watchpointEvent() != null) {
                printStream.println("\tthread at watchpoint=" + state.watchpointEvent());
            }
            state = state.previous();
        }
    }

    /**
     * Creates a null state that is designed to precede any real states in
     * a state history.
     *
     * @param mode the mode of the inspection in which the history is being
     * recorded.
     * @return a null state with no predecessor, unknown process state, and no thread information.
     */
    public static TeleVMState nullState(MaxInspectionMode mode) {
        return new TeleVMState(
                       mode,
                       ProcessState.UNKNOWN,
                       -1L,
                       EMPTY_MAXMEMORYREGION_LIST,
                       EMPTY_THREAD_LIST,
                       (TeleNativeThread) null,
                       EMPTY_THREAD_LIST,
                       EMPTY_THREAD_LIST,
                       EMPTY_BREAKPOINTEVENT_LIST,
                       (TeleWatchpointEvent) null, false, (TeleVMState) null);
    }

}
