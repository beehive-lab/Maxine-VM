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

import java.util.*;

import com.sun.max.vm.thread.*;

/**
 * The states a thread can be in.
 * N.B. Many platforms will not be able to detect all these states, e.g., MONITOR_WAIT,
 * in which case the generic SUSPENDED is appropriate.
 *
 * Note: This enum must be kept in sync with the enum {@code ThreadState}  in MaxineNative/inspector/teleNativeThread.h.
 * @see MaxThread
 */
public enum MaxThreadState {

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

    public static final List<MaxThreadState> VALUES = Collections.unmodifiableList(Arrays.asList(values()));

    private final String asString;
    private final boolean allowsDataAccess;

    MaxThreadState(String asString, boolean allowsDataAccess) {
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
