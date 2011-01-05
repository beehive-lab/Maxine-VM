/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.thread.*;

/**
 * A JavaMonitor provides Java monitor semantics on behalf of another object (the <b>bound</b> object).
 *
 * @author Simon Wilkinson
 */
public interface JavaMonitor {

    /**
     * Causes the current thread to enter the monitor.
     */
    void monitorEnter();

    /**
     * Causes the current thread to exit the monitor.
     */
    void monitorExit();

    /**
     * Implements {@link Object#wait()} for this monitor's bound object.
     */
    void monitorWait(long timeoutMilliSeconds) throws InterruptedException;

    /**
     * Implements {@link Object#notify()} and {@link Object#notifyAll()} for
     * this monitor's bound object.
     */
    void monitorNotify(boolean all);

    /**
     * Tests if this {@code JavaMonitor} is owned by the given thread.
     *
     * @param thread the thread to test for ownership or {@code null} to test if this monitor is not currently locked by any thread
     * @return true if {@code thread} owns this monitor or {@code thread == null} and this monitor has no owner, false otherwise
     */
    boolean isOwnedBy(VmThread thread);

    /**
     * Returns the displaced misc header word of this {@code JavaMonitor}'s bound object.
     * This must be previously set by {@link #setDisplacedMisc(Word) setDisplacedMisc()}.
     *
     * @return the displaced misc header word
     */
    Word displacedMisc();

    /**
     * Stores a copy of the given header misc word.
     *
     * This is intended to store the misc header word of this monitor's
     * bound object, if it needs to be displaced in order to complete the binding.
     *
     * @param lockword the misc header word
     */
    void setDisplacedMisc(Word lockword);

    /**
     * Compares and swaps the the displaced misc header word of this monitor's
     * bound object.
     * @see  #setDisplacedMisc(Word lockword)
     * @param expectedValue the suspected current displaced misc word
     * @param newValue the new displaced misc word
     * @return expectedValue if successful, the current displaced misc word if unsuccessful
     */
    Word compareAndSwapDisplacedMisc(Word expectedValue, Word newValue);

    /**
     * Sets this monitor to be owned by the given thread.
     *
     * Note: This method should only be called when this monitor
     * is not currently owned, and cannot be acquired by any other thread.
     *
     * @param owner the new owner thread
     * @param lockQty the number of recursive locks to acquire
     */
    void monitorPrivateAcquire(VmThread owner, int lockQty);

    /**
     * Set this monitor to be not owned.
     *
     * Note: This method should only be called when this monitor
     * has been acquired via
     * {@link #monitorPrivateAcquire(VmThread owner, int lockQty) monitorPrivateAcquire()},
     * and cannot be acquired by any other thread.
     */
    void monitorPrivateRelease();

    /**
     * Prints the details of this monitor to the {@linkplain Log VM log stream}.
     */
    void log();
}
