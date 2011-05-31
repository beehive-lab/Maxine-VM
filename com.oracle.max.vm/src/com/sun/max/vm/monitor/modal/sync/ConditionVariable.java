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
package com.sun.max.vm.monitor.modal.sync;

/**
 * Abstract class defining the interface to a condition variable as used by JavaMonitors.
 */
public abstract class ConditionVariable {
    /**
     * Perform any setup on the condition variable.
     */
    public abstract ConditionVariable init();

    /**
     * Causes the current thread to wait until this {@code ConditionVariable} is signaled via
     * {@link #threadNotify(boolean all) threadNotify()}. If a timeout > 0 is specified, then the thread may return
     * after this timeout has elapsed without being signaled.
     *
     * The current thread must own the given mutex when calling this method, otherwise the results are undefined. Before
     * blocking, the current thread will release the mutex. On return, the current thread is guaranteed to own the
     * mutex.
     *
     * The thread may return early if it is {@linkplain java.lang.Thread#interrupt() interrupted} whilst
     * blocking. In this case, the {@linkplain VmThread#isInterrupted(boolean) interrupted} flag of the thread
     * will have been {@linkplain VmThread#setInterrupted() set} to true by the trap handler on the
     * interrupted thread.
     *
     * @param mutex the mutex on which to block
     * @param timeoutMilliSeconds the maximum time to block. No timeout is used if timeoutMilliSeconds == 0.
     * @return true if no error occurred whilst blocking; false otherwise
     */
    public abstract boolean threadWait(Mutex mutex, long timeoutMilliSeconds);

    /**
     * Causes one or all of the threads {@link #threadWait(Mutex mutex, long timeoutMilliSeconds) waiting} on this
     * {@code ConditionVariable} to wake-up.
     *
     * @param all notify all threads
     * @return true if no error occurred in native code; false otherwise
     */
    public abstract boolean threadNotify(boolean all);

    /**
     * Return an id suitable for logging purposes.
     */
    public abstract long logId();
}
