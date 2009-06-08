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
 * Implements the immutable history of Maxine VM states during a debugging sessions.
 *
 * @author Michael Van De Vanter
 */
public class TeleVMState implements MaxVMState {

    private final ProcessState _processState;
    private final long _serialID;
    private final long _epoch;
    private final TeleNativeThread _singleStepThread;
    private final Sequence<TeleNativeThread> _breakpointThreads;
    private final boolean _isInGC;
    private final TeleVMState _previous;

    /**
     * @param processState current state of the VM
     * @param epoch current process epoch counter
     * @param singleStepThread thread just single-stepped, null if none
     * @param breakpointThreads threads currently at a breakpoint, empty if none
     * @param isInGC is the VM, when paused, in a GC
     * @param previous previous state
     */
    public TeleVMState(ProcessState processState, long epoch, TeleNativeThread singleStepThread, Sequence<TeleNativeThread> breakpointThreads, boolean isInGC, TeleVMState previous) {
        _processState = processState;
        _serialID = previous == null ? 0 : previous.serialID() + 1;
        _epoch = epoch;
        _singleStepThread = singleStepThread;
        assert breakpointThreads != null;
        _breakpointThreads = breakpointThreads;
        _isInGC = isInGC;
        _previous = previous;
    }

    public ProcessState processState() {
        return _processState;
    }

    public long serialID() {
        return _serialID;
    }

    public long epoch() {
        return _epoch;
    }

    public TeleNativeThread singleStepThread() {
        return _singleStepThread;
    }

    public Sequence<TeleNativeThread> breakpointThreads() {
        return _breakpointThreads;
    }

    public boolean isInGC() {
        return _isInGC;
    }

    public MaxVMState previous() {
        return _previous;
    }

    public boolean newerThan(MaxVMState maxVMState) {
        return maxVMState == null || _serialID > maxVMState.serialID();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(50);
        sb.append(getClass().getSimpleName()).append("(");
        sb.append(Long.toString(_serialID)).append(", ");
        sb.append(_processState.toString()).append(", ");
        sb.append(Long.toString(_epoch)).append(", ");
        sb.append(Boolean.toString(_isInGC)).append(", ");
        if (_previous == null) {
            sb.append("null");
        } else {
            sb.append("(").append(_previous.processState().toString()).append(",");
            sb.append(Long.toString(_previous.serialID())).append(",");
            sb.append(Long.toString(_previous.epoch())).append(")");
        }
        sb.append(")");
        return sb.toString();
    }

}
