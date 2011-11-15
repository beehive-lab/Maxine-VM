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
import com.sun.max.unsafe.*;

/**
 * <p>
 * A (mostly) immutable summary of the Maxine VM state at some point in time, thread-safe and suitable for safe sharing between GUI and execution threads.</p>
 * <p>
 * The typical progression of process states is
 * { {@linkplain MaxProcessState.STOPPED STOPPED} -> {@linkplain MaxProcessState.RUNNING RUNNING} }* -> {@linkplain MaxProcessState.TERMINATED TERMINATED}.
 * When there is no live process being used, then the state is always {@linkplain MaxProcessState.NONE NONE}.</p>
 * <p>
 * The serial id counter is at 0 for the first state in the history; it has no predecessor.
 * States are linked backwards; a new summary is prepended (and the serial id incremented) each time a new state instance is created.</p>
 * <p>
 * <b>Note:</b> Although all state concerning the identity of threads is immutable, the internal state of those threads may not be.</p>
  */
public interface MaxVMState  {

    /**
     * The new state of the VM at this state transition.
     *
     * @return VM process state.
     */
    MaxProcessState processState();

    /**
     * Generation counter in the history of VM state transitions.
     * <br>
     * The oldest state, which has no predecessor, is set to 0,
     * and each subsequent state transition in the history is one
     * greater than its predecessor.
     *
     * @return the generation count of this state transition.
     */
    long serialID();

    /**
     * Process epoch in the VM at this state transition, counting the number
     * of times that the VM process has been executed.  This is generally
     * much higher than the number of VM state changes, as counted by
     * {@link #serialID()}, since the VM may be started and stopped many
     * times in service of a single client request.
     *
     * @return the number of VM state generations since started,
     * @see TeleProcess#epoch()
     * @see #serialID()
     */
    long epoch();

    /**
     * Enumerates all memory regions in the VM that have been allocated from the OS
     * at this state transition.
     * <br>
     * <b>Note</b>: the internal state of a memory region identified here is
     * not necessarily immutable, for example if an allocation mark has changed
     * since this object was created.
     *
     * @return the regions of memory that the VM has allocated from the OS.
     */
    List<MaxMemoryRegion> memoryAllocations();

    /**
     * Finds the allocated region of memory in the VM, if any, that includes an address.
     *
     * @param address a memory location in the VM
     * @return the allocated {@link MaxMemoryRegion} containing the address, null if not in any known region.
     */
    MaxMemoryRegion findMemoryRegion(Address address);

    /**
     * Enumerates all threads live in the VM at this state transition.
     * <br>
     *  <b>Note</b> : the internal state of a thread identified here is not necessarily immutable,
     * for example by the time a reader examines the set of live threads, one or more
     * of them may have died and will be reported as having died in a subsequent
     * state transition.
     *
     * @return the active (live) threads
     * @see #threadsStarted()
     * @see #threadsDied()
     */
    List<MaxThread> threads();

    /**
     * When the VM stops, the thread, if any, that was single stepped.
     * <br>
     * <b>Note</b>: the internal state of a thread identified here is not necessarily immutable,
     * for example a thread reported here (which is live at this state transition)
     * may have died by the time a reader examines this state transition.
     *
     * @return the thread just single stepped; null if none.
     * @see #threads()
     */
    MaxThread singleStepThread();

    /**
     * @return threads created since the previous state in the history; empty if none.
     * Creation of a new thread is announced in exactly one state;
     * it can be assumed to be live until a state in which it is announced
     * as having died.
     * <br>
     *  <b>Note</b>  the internal state of a thread identified here is not necessarily immutable,
     *  for example a thread reported here (which is live at this state transition)
     *  may have died by the time a reader examines this state transition.
     *
     * @see #threads()
     * @see #threadsDied()
     */
    List<MaxThread> threadsStarted();

    /**
     * Threads that have died since the last state transition.
     * <br>
     * The death of a thread is announced in exactly one state transition, which
     * must be later than the state in which it was created.  The thread
     * can be assumed to be dead in all subsequent states, and its
     * contents are not defined.
     * <br>
     *  <b>Note</b>: the internal state of a thread identified here is not necessarily immutable.
     *
     * @return threads died since the previous state in the history; empty if none.
     * @see #threads()
     * @see #threadsStarted()
     */
    List<MaxThread> threadsDied();

    /**
     * When the VM stops, describes threads that are currently at breakpoints.
     * <br>
     * Note that only client-visible breakpoints are reported, so for example, when
     * a target code breakpoint created for a bytecode breakpoint is triggered, what
     * gets reported is the bytecode breakpoint.
     *
     * @return descriptions of threads currently stopped at breakpoints, empty if none.
     */
    List<MaxBreakpointEvent> breakpointEvents();

    /**
     * When the VM has stopped because a thread hit a memory watchpoint,
     * contains a description of the event; null otherwise.
     * @return description of a thread currently at a memory watchpoint; null if none.
     */
    MaxWatchpointEvent watchpointEvent();

    /**
     * Is the VM in the midst of a Garbage Collection at this state transition?
     *
     * @return whether the VM, when paused, is in the middle of a GC.
     */
    boolean isInGC();

    /**
     * Is the VM in the midst of a code eviction at this state transition?
     *
     * @return whether the VM, when paused, is in the middle of a code eviction.
     */
    boolean isInEviction();

    /**
     * @return previous state summary in the history; null in the first element in the history.
     */
    MaxVMState previous();

    /**
     * @return whether the serial id of this state transition is strictly newer than another.
     * Any state transition is strictly newer than null.
     */
    boolean newerThan(MaxVMState maxVMState);


    /**
     * Writes a textual summary describing this and all predecessor states.
     * <br>
     * Thread-safe.
     */
    void writeSummary(PrintStream printStream);
}
