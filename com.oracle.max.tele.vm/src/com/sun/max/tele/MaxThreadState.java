/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
