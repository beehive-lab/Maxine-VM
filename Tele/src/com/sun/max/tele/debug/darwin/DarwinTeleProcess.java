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
import com.sun.max.vm.prototype.*;

/**
 * @author Bernd Mathiske
 */
public final class DarwinTeleProcess extends TeleProcess {

    private final PageDataAccess _pageDataAccess;

    @Override
    public PageDataAccess dataAccess() {
        return _pageDataAccess;
    }

    private void invalidateCache() {
        _pageDataAccess.invalidateCache();
    }

    private static native long nativeCreateChild(long argv, int vmAgentSocketPort);
    private static native void nativeKill(long task);
    private static native boolean nativeSuspend(long task);
    private static native boolean nativeResume(long task);
    private static native boolean nativeWait(long pid, long task);

    private final long _task;

    public long task() {
        return _task;
    }

    DarwinTeleProcess(TeleVM teleVM, Platform platform, File programFile, String[] commandLineArguments, TeleVMAgent agent) throws BootImageException {
        super(teleVM, platform);
        final Pointer commandLineArgumentsBuffer = TeleProcess.createCommandLineArgumentsBuffer(programFile, commandLineArguments);
        _task = nativeCreateChild(commandLineArgumentsBuffer.toLong(), agent.port());
        if (_task == -1) {
            ProgramError.unexpected(String.format("task_for_pid() permissions problem -- Need to run java as setgid procmod:%n%n" +
                "    chgrp procmod <java executable>;  chmod g+s <java executable>%n%n" +
                "where <java executable> is the platform dependent executable found under or relative to " + System.getProperty("java.home") + "."));
        }
        _pageDataAccess = new PageDataAccess(platform.processorKind().dataModel(), this);
        try {
            resume();
        } catch (OSExecutionRequestException e) {
            throw new BootImageException("Error resuming VM after starting it", e);
        }
    }

    @Override
    public void suspend() throws OSExecutionRequestException {
        if (!nativeSuspend(_task)) {
            throw new TeleError("could not suspend process");
        }
    }

    @Override
    public void resume() throws OSExecutionRequestException {
        if (!nativeResume(_task)) {
            throw new OSExecutionRequestException("Resume could not be completed");
        }
    }

    @Override
    protected boolean waitUntilStopped() {
        final boolean ok = nativeWait(_task, _task);
        invalidateCache();
        return ok;
    }

    @Override
    protected void kill() throws OSExecutionRequestException {
        nativeKill(_task);
    }

    private native void nativeGatherThreads(long task, AppendableSequence<TeleNativeThread> threads);

    @Override
    protected void gatherThreads(AppendableSequence<TeleNativeThread> threads) {
        nativeGatherThreads(_task, threads);
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

    private static native int nativeReadBytes(long task, long address, byte[] buffer, int offset, int length);

    @Override
    protected int read0(Address address, byte[] buffer, int offset, int length) {
        return nativeReadBytes(_task, address.toLong(), buffer, offset, length);
    }

    private static native int nativeWriteBytes(long task, long address, byte[] buffer, int offset, int length);

    @Override
    protected int write0(byte[] buffer, int offset, int length, Address address) {
        return nativeWriteBytes(_task, address.toLong(), buffer, offset, length);
    }
}
