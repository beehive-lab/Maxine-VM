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
package com.sun.max.vm.monitor.modal.modehandlers;

import static com.sun.max.vm.thread.VmThread.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.schemes.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Basic services for a ModeHandler.
 */
public abstract class AbstractModeHandler implements ModeHandler {

    private static final boolean EXPLICIT_NULL_CHECKS = true;

    @CONSTANT_WHEN_NOT_ZERO
    private ModalMonitorScheme monitorScheme;

    private final ModeDelegate delegate;

    protected AbstractModeHandler(ModeDelegate delegate) {
        this.delegate = delegate;
    }

    @INLINE
    public final ModeDelegate delegate() {
        return delegate;
    }

    /**
     * Returns the current VM monitor scheme.
     *
     * @return the current VM monitor scheme.
     */
    protected ModalMonitorScheme monitorScheme() {
        return monitorScheme;
    }

    public void setMonitorScheme(ModalMonitorScheme monitorScheme) {
        this.monitorScheme = monitorScheme;
    }

    @INLINE
    protected final void nullCheck(Object object) {
        if (EXPLICIT_NULL_CHECKS && object == null) {
            Throw.nullPointerException();
        }
    }

    /**
     * Returns the current thread's id (its VMThreadMap id) in an encoding suitable for
     * lockwords defined by the ModalLockword64 hierarchy.
     *
     * @return the encoded thread ID
     */
    @INLINE
    protected static final int encodeCurrentThreadIDForLockword() {
        return VmThreadLocal.ID.load(currentTLA()).toInt();
    }

    /**
     * Decodes the given lockword thread id into a VMThreadMap id.
     * Now that thread ids are always >= 1 this is a no-op.
     *
     * @param lockwordThreadID the lockword thread id
     * @return the VMThreadMap id
     */
    @INLINE
    protected static final int decodeLockwordThreadID(int lockwordThreadID) {
        return lockwordThreadID;
    }

    public void initialize(MaxineVM.Phase phase) {
    }

    /**
     * Runtime entry points into the fastest path of the locking-mode hierarchy.
     */
    public interface MonitorSchemeEntry extends ModeHandler {

        /**
         * Acquires or recursively locks the given object's monitor for the current thread.
         *
         * @param object the Object being acquired
         */
        void monitorEnter(Object object);

        /**
         * Releases or recursively unlocks the given object's monitor for the current thread.
         *
         * @param object the Object being released
         */
        void monitorExit(Object object);

        /**
         * Performs Object.notify() / notifyAll() for the given Object.
         *
         * @param object the Object to notify
         * @param all true if all threads should be notified; false if only one should be notified
         */
        void monitorNotify(Object object, boolean all);

        /**
         * Performs Object.wait() for the given Object.
         *
         * @param object the Object on which to wait
         * @param timeout the wait timeout
         * @throws InterruptedException if the current thread was interrupted via Thread.interrupt() whilst waiting.
         */
        void monitorWait(Object object, long timeout) throws InterruptedException;

        /**
         * Returns the given Object's hashcode.
         *
         * @return the given Object's hashcode.
         */
        int makeHashCode(Object object);

        /**
         * Returns an image build-time misc word for the given Object.
         *
         * @param object the Object
         * @return the Object's image build-time misc word
         */
        Word createMisc(Object object);

        /**
         * Tests if the given VmThread owns the given Object's monitor.
         *
         * @param object the Object to test
         * @param thread the VmThread to test
         * @return true if the thread owns the Object's monitor; false otherwise
         */
        boolean threadHoldsMonitor(Object object, VmThread thread);

        /**
         *  Notification that we are at a global safe-point, pre-collection.
         */
        void beforeGarbageCollection();

        /**
         *  Notification that we are at a global safe-point, post-collection.
         */
        void afterGarbageCollection();
    }

    /**
     * Standard interface between nodes in a locking mode hierarchy to allow
     * mode transitions and delegation.
     */
    public interface ModeDelegate extends ModeHandler {
        ModalLockword64 prepareModalLockword(Object object, ModalLockword64 currentLockword);
        ModalLockword64 reprepareModalLockword(ModalLockword64 preparedLockword, ModalLockword64 currentLockword);
        void cancelPreparedModalLockword(ModalLockword64 preparedLockword);
        boolean delegateMonitorEnter(Object object, ModalLockword64 lockword, int lockwordThreadID);
        void delegateMonitorExit(Object object, ModalLockword64 lockword);
        void delegateMonitorNotify(Object object, boolean all, ModalLockword64 lockword);
        void delegateMonitorWait(Object object, long timeout, ModalLockword64 lockword) throws InterruptedException;
        DelegatedThreadHoldsMonitorResult delegateThreadHoldsMonitor(Object object, ModalLockword64 lockword, VmThread thread, int lockwordThreadID);
        int delegateMakeHashcode(Object object, ModalLockword64 lockword);
        void delegateBeforeGarbageCollection();
        void delegateAfterGarbageCollection();
        /**
         * A tri-state boolean for the return value of
         * {@link ModeDelegate#delegateThreadHoldsMonitor(Object, ModalLockword64, VmThread, int)}.
         * This deals with the situation where the locking mode changes while determining
         * whether the current thread owns the lock.
         *
         */
        enum DelegatedThreadHoldsMonitorResult {TRUE, FALSE, NOT_THIS_MODE}
    }
}
