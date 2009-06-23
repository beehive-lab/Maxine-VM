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

import java.io.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.tele.debug.*;


/**
 * Implements the immutable history of Maxine VM states during a debugging sessions.
 *
 * @author Michael Van De Vanter
 */
public final class TeleVMState implements MaxVMState {

    private static final Sequence<MaxThread> EMPTY_THREAD_SEQUENCE = Sequence.Static.empty(MaxThread.class);

    private final ProcessState _processState;
    private final long _serialID;
    private final long _epoch;
    private final Sequence<MaxThread> _threads;
    private final MaxThread _singleStepThread;
    private final Sequence<MaxThread> _breakpointThreads;
    private final Sequence<MaxThread> _threadsStarted;
    private final Sequence<MaxThread> _threadsDied;
    private final boolean _isInGC;
    private final TeleVMState _previous;

    /**
     * @param processState current state of the VM
     * @param epoch current process epoch counter
     * @param singleStepThread thread just single-stepped, null if none
     * @param breakpointThreads threads currently at a breakpoint, empty if none
     * @param threads TODO
     * @param threadsStarted threads created since the previous state
     * @param threadsDied threads died since the previous state
     * @param isInGC is the VM, when paused, in a GC
     * @param previous previous state
     */
    public TeleVMState(ProcessState processState,
                    long epoch,
                    TeleNativeThread singleStepThread,
                    Sequence<TeleNativeThread> breakpointThreads,
                    Collection<TeleNativeThread> threads,
                    Sequence<TeleNativeThread> threadsStarted,
                    Sequence<TeleNativeThread> threadsDied,
                    boolean isInGC, TeleVMState previous) {
        _processState = processState;
        _serialID = previous == null ? 0 : previous.serialID() + 1;
        _epoch = epoch;
        _singleStepThread = singleStepThread;
        _breakpointThreads = breakpointThreads.length() == 0 ? EMPTY_THREAD_SEQUENCE : new VectorSequence<MaxThread>(breakpointThreads);
        _threadsStarted = threadsStarted.length() == 0 ? EMPTY_THREAD_SEQUENCE : new VectorSequence<MaxThread>(threadsStarted);
        _threadsDied = threadsDied.length() == 0 ? EMPTY_THREAD_SEQUENCE : new VectorSequence<MaxThread>(threadsDied);
        _isInGC = isInGC;
        _previous = previous;

        // Compute the current active thread list.
        if (previous == null) {
            // First state transition in the history.
            _threads = new VectorSequence<MaxThread>(threadsStarted);
        } else if (threadsStarted.length() + threadsDied.length() == 0)  {
            // No changes since predecessor; share the thread list.
            _threads = previous.threads();
        } else {
            // There have been some thread changes; make a new (immutable) sequence for the new state
            _threads = new VectorSequence<MaxThread>(threads);
        }
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

    public Sequence<MaxThread> threads() {
        return _threads;
    }

    public MaxThread singleStepThread() {
        return _singleStepThread;
    }

    public Sequence<MaxThread> breakpointThreads() {
        return _breakpointThreads;
    }

    public Sequence<MaxThread> threadsStarted() {
        return _threadsStarted;
    }

    public  Sequence<MaxThread> threadsDied() {
        return _threadsDied;
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

    public void writeSummaryToStream(PrintStream printStream) {
        MaxVMState state = this;
        while (state != null) {
            final StringBuilder sb = new StringBuilder(100);
            sb.append(Long.toString(state.serialID())).append(":  ");
            sb.append("proc=(").append(state.processState().toString()).append(", ").append(Long.toString(state.epoch())).append(") ");
            sb.append("gc=").append(state.isInGC()).append(" ");
            printStream.println(sb.toString());
            if (state.singleStepThread() != null) {
                printStream.println("\tstep=" + state.singleStepThread().toShortString());
            }
            for (MaxThread thread : state.breakpointThreads()) {
                printStream.println("\t@breakpoint=" + thread.toShortString());
            }
            if (state.previous() != null && state.threads() == state.previous().threads()) {
                printStream.println("\tthreads: <unchanged>");
            } else if (state.threads().length() == 0) {
                printStream.println("\tthreads: <empty>");
            } else {
                printStream.println("\tthreads:");
                for (MaxThread thread : state.threads()) {
                    printStream.println("\t\t" + thread.toShortString());
                }
            }
            for (MaxThread thread : state.threadsStarted()) {
                printStream.println("\tstarted=" + thread.toShortString());
            }
            for (MaxThread thread : state.threadsDied()) {
                printStream.println("\tdied=" + thread.toShortString());
            }
            state = state.previous();
        }
    }

}
