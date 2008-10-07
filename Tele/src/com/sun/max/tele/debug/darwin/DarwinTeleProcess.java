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
/*VCSID=5fb44493-430b-4da0-8dbf-f593b31c4f24*/
package com.sun.max.tele.debug.darwin;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleNativeThread.*;
import com.sun.max.tele.page.*;
import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 */
public final class DarwinTeleProcess extends TeleProcess {

    private final PageDataAccess _pageDataAccess;

    @Override
    public PageDataAccess dataAccess() {
        return _pageDataAccess;
    }

    public void invalidateCache() {
        _pageDataAccess.invalidateCache();
    }

    private static native long nativeCreateChild(long argv);
    private static native long nativePidToTask(long pid);
    private static native void nativeKill(long pid);
    private static native boolean nativeSuspend(long task);
    private static native boolean nativeResume(long pid);
    private static native boolean nativeWait(long pid, long task);

    private final long _pid;

    public long pid() {
        return _pid;
    }

    private final long _task;

    public long task() {
        return _task;
    }

    DarwinTeleProcess(TeleVM teleVM, Platform platform, File programFile, String[] commandLineArguments, int id) {
        super(teleVM, platform, programFile, commandLineArguments);
        _pid = nativeCreateChild(commandLineBuffer().toLong());
        _task = nativePidToTask(_pid);
        if (_task == -1) {
            ProgramError.unexpected(String.format("task_for_pid() permissions problem -- Need to run java as setgid procmod:%n%n    chgrp procmod `which java`;  chmod g+s `which java`%n"));
        }
        _pageDataAccess = new PageDataAccess(platform.processorKind().dataModel(), this);
    }


    @Override
    public void suspend() throws ExecutionRequestException {
        if (!nativeSuspend(_task)) {
            throw new TeleError("could not suspend process");
        }
    }

    @Override
    public void resume() throws ExecutionRequestException {
        if (!nativeResume(_pid)) {
            throw new ExecutionRequestException("Resume could not be completed");
        }
    }

    @Override
    protected boolean waitUntilStopped() {
        final boolean ok = nativeWait(_pid, _task);
        invalidateCache();
        return ok;
    }

    @Override
    protected void kill() throws ExecutionRequestException {
        nativeKill(_pid);
    }

    private native boolean nativeGatherThreads(long task, AppendableSequence<TeleNativeThread> threads);

    @Override
    protected boolean gatherThreads(AppendableSequence<TeleNativeThread> threads) {
        return nativeGatherThreads(_task, threads);
    }

    /**
     * Callback from JNI: creates new thread object or updates existing thread object with same thread ID.
     */
    void jniGatherThread(AppendableSequence<TeleNativeThread> threads, long threadID, int state, long stackBase, long stackSize) {
        DarwinTeleNativeThread thread = (DarwinTeleNativeThread) idToThread(threadID);
        if (thread == null) {
            thread = new DarwinTeleNativeThread(this, threadID, stackBase, stackSize);
        }

        assert state >= 0 && state < ThreadState.VALUES.length();
        thread.setState(ThreadState.VALUES.get(state));
        threads.append(thread);
    }

    @Override
    public synchronized int read0(Address address, byte[] buffer, int offset, int length) {
        return DarwinDataAccess.nativeReadBytes(_task, address.toLong(), buffer, offset, length);
    }

    @Override
    public synchronized int write0(byte[] buffer, int offset, int length, Address address) {
        return DarwinDataAccess.nativeWriteBytes(_task, address.toLong(), buffer, offset, length);
    }
}
