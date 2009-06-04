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

import com.sun.max.collect.*;
import com.sun.max.tele.debug.*;


/**
 * An immutable summary of the Maxine VM state at some point in time, thread-safe and suitable for safe sharing between GUI and execution threads.
 * The typical progression is INITIAL -> {WAITING -> RUNNING}* -> TERMINATED.
 * The serial id counter is initially 0, when in the INITIAL STATE or the first time WAITING.
 * States are linked backwards; a new summary is prepended (and the serial id incremented) each time a new state instance is created..
 *
 * @author Michael Van De Vanter
  */
public interface MaxVMState  {

    /**
     * @return the state that the VM was in at the time of this object creation..
     */
    TeleProcess.ProcessState processState();

    /**
     * @return the generation count of this state instance; each id is one greater than
     * its predecessor, and the oldest state in the history is 0.
     */
    long serialID();

    /**
     * @return the number of VM state generations since started, counted as the number
     * of debugging steps requested.  Note that that some debugging actions cause the
     * VM to be run more than once, but each action increments the generation by one.
     */
    long epoch();

    /**
     * @return the thread just single stepped, if any.
     * Note that the internal state of a thread identified here is not necessarily immutable.
     */
    TeleNativeThread singleStepThread();

    /**
     * @return threads currently stopped at a breakpoint, empty if none.
     *  Note that the internal state of a thread identified here is not necessarily immutable.
     */
    Sequence<TeleNativeThread> breakpointThreads();

    /**
     * @return whether the VM, when paused, is in the middle of a GC; the value may be out of date when the VM is running.
     */
    boolean isInGC();

    /**
     * @return previous state summary in the history.
     */
    MaxVMState previous();

    /**
     * @return whether the serial id of this state is strictly newer than another, true if the other is null.
     */
    boolean newerThan(MaxVMState maxVMState);
}
