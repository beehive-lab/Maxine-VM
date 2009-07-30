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
package com.sun.max.vm.monitor.modal.modehandlers;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.schemes.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Basic services for a ModeHandler.
 *
 * @author Simon Wilkinson
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
     * lockwords defined by the ModalLockWord64 hierarchy.
     *
     * @return the encoded thread ID
     */
    @INLINE
    protected static final int encodeCurrentThreadIDForLockword() {
        return VmThreadLocal.ID.getConstantWord().asAddress().toInt();
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
        ModalLockWord64 prepareModalLockWord(Object object, ModalLockWord64 currentlockWord);
        ModalLockWord64 rePrepareModalLockWord(ModalLockWord64 preparedLockWord, ModalLockWord64 currentlockWord);
        void cancelPreparedModalLockWord(ModalLockWord64 preparedLockWord);
        boolean delegateMonitorEnter(Object object, ModalLockWord64 lockWord, int lockwordThreadID);
        void delegateMonitorExit(Object object, ModalLockWord64 lockWord);
        void delegateMonitorNotify(Object object, boolean all, ModalLockWord64 lockWord);
        void delegateMonitorWait(Object object, long timeout, ModalLockWord64 lockWord) throws InterruptedException;
        boolean delegateThreadHoldsMonitor(Object object, ModalLockWord64 lockWord, VmThread thread, int lockwordThreadID, boolean[] result);
        int delegateMakeHashcode(Object object, ModalLockWord64 lockWord);
        void delegateBeforeGarbageCollection();
        void delegateAfterGarbageCollection();
    }
}
