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
import com.sun.max.vm.thread.*;

/**
 * Basic services for a ModeHandler.
 *
 * @author Simon Wilkinson
 */
public abstract class AbstractModeHandler implements ModeHandler {

    private static final boolean _EXPLICIT_NULL_CHECKS = true;

    @CONSTANT_WHEN_NOT_ZERO
    private ModalMonitorScheme _monitorScheme;

    private final ModeDelegate _delegate;

    protected AbstractModeHandler(ModeDelegate delegate) {
        _delegate = delegate;
    }

    @INLINE
    @Override
    public final ModeDelegate getDelegate() {
        return _delegate;
    }

    protected ModalMonitorScheme monitorScheme() {
        return _monitorScheme;
    }

    public void setMonitorScheme(ModalMonitorScheme monitorScheme) {
        _monitorScheme = monitorScheme;
    }

    @INLINE
    protected final void nullCheck(Object object) {
        if (_EXPLICIT_NULL_CHECKS && object == null) {
            throw new NullPointerException();
        }
    }

    @INLINE
    protected static final int encodeCurrentForLockwordThreadID() {
        return VmThreadLocal.ID.getConstantWord().asAddress().toInt() + 1;
    }

    @INLINE
    protected static final int decodeLockwordThreadID(int lockwordThreadID) {
        return lockwordThreadID - 1;
    }

    public void initialize(MaxineVM.Phase phase) { }

    /**
     * An AbstractModeHandler subclass must implement this interface to
     * act as the entry-point for monitor operations via the MonitorScheme adapter.
     */
    public interface MonitorSchemeEntry extends ModeHandler {
        void monitorEnter(Object object);
        void monitorExit(Object object);
        void monitorNotify(Object object, boolean all);
        void monitorWait(Object object, long timeout) throws InterruptedException;
        int makeHashCode(Object object);
        Word createMisc(Object object);
        boolean threadHoldsMonitor(Object object, VmThread thread);
        void beforeGarbageCollection();
        void afterGarbageCollection();
    }

    /**
     * An AbstractModeHandler subclass must implement this interface to
     * act as a delegate for another AbstractModeHandler.
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
