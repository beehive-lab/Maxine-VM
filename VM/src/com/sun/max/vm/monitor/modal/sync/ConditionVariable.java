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
package com.sun.max.vm.monitor.modal.sync;

/**
 * Abstract class defining the interface to a condition variable as used by JavaMonitors.
 *
 * @author Simon Wilkinson
 * @author Mick Jordan
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
