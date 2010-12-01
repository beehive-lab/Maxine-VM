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
