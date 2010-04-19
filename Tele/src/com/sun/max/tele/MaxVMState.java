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
package com.sun.max.tele;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.tele.debug.*;

/**
 * <p>
 * A (mostly) immutable summary of the Maxine VM state at some point in time, thread-safe and suitable for safe sharing between GUI and execution threads.</p>
 * <p>
 * The typical progression of process states is
 * { {@linkplain ProcessState.STOPPED STOPPED} -> {@linkplain ProcessState.RUNNING RUNNING} }* -> {@linkplain ProcessState.TERMINATED TERMINATED}.
 * When there is no live process being used, then the state is always {@linkplain ProcessState.NO_PROCESS NO_PROCESS}.</p>
 * <p>
 * The serial id counter is at 0 for the first state in the history; it has no predecessor.
 * States are linked backwards; a new summary is prepended (and the serial id incremented) each time a new state instance is created.</p>
 * <p>
 * <b>Note:</b> Although all state concerning the identity of threads is immutable, the internal state of those threads may not be.</p>
 *
 * @author Michael Van De Vanter
  */
public interface MaxVMState  {

    /**
     * The new state of the VM at this state transition.
     *
     * @return VM process state.
     * @see TeleProcess#processState().
     */
    ProcessState processState();

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
    Sequence<MaxMemoryRegion> memoryRegions();

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
    Sequence<MaxThread> threads();

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
     *  @see #threads()
     * @see #threadsDied()
     */
    Sequence<MaxThread> threadsStarted();

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
     *  @see #threadsStarted()
     */
    Sequence<MaxThread> threadsDied();

    /**
     * When the VM stops, describes threads that are currently at breakpoints.
     * <br>
     * Note that only client-visible breakpoints are reported, so for example, when
     * a target code breakpoint created for a bytecode breakpoint is triggered, what
     * gets reported is the bytecode breakpoint.
     *
     * @return descriptions of threads currently stopped at breakpoints, empty if none.
     */
    Sequence<MaxBreakpointEvent> breakpointEvents();

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
