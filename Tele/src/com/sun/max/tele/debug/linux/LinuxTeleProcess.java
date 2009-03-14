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
package com.sun.max.tele.debug.linux;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleNativeThread.*;
import com.sun.max.tele.page.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.prototype.*;

/**
 * @author Bernd Mathiske
 */
public final class LinuxTeleProcess extends TeleProcess {

    private final LinuxTask _task;

    LinuxTask task() {
        return _task;
    }

    private final PageDataAccess _pageDataAccess;

    @Override
    public PageDataAccess dataAccess() {
        return _pageDataAccess;
    }

    private void invalidateCache() {
        _pageDataAccess.invalidateCache();
    }

    LinuxTeleProcess(TeleVM teleVM, Platform platform, File programFile, String[] commandLineArguments, TeleVMAgent agent) throws BootImageException {
        super(teleVM, platform);
        final Pointer commandLineArgumentsBuffer = TeleProcess.createCommandLineArgumentsBuffer(programFile, commandLineArguments);
        _task = LinuxTask.createChild(commandLineArgumentsBuffer.toLong(), agent.port());
        if (_task == null) {
            throw new BootImageException("Error launching VM");
        }
        _pageDataAccess = new PageDataAccess(platform.processorKind().dataModel(), this);
        try {
            resume();
        } catch (OSExecutionRequestException e) {
            throw new BootImageException("Error resuming VM after starting it", e);
        }
    }

    @Override
    protected void kill() throws OSExecutionRequestException {
        _task.kill();
    }

    @Override
    protected void resume() throws OSExecutionRequestException {
        _task.resume();
        for (TeleNativeThread thread : threads()) {
            final LinuxTeleNativeThread linuxThread = (LinuxTeleNativeThread) thread;
            final LinuxTask threadTask = linuxThread.task();
            if (!threadTask.equals(_task)) {
                threadTask.resume();
            }
        }
    }

    @Override
    protected void suspend() throws OSExecutionRequestException {
        _task.suspend(true);
    }

    @Override
    protected boolean waitUntilStopped() {
        final boolean result = _task.waitUntilStopped();
        invalidateCache();
        return result;
    }

    private native void nativeGatherThreads(long pid, AppendableSequence<TeleNativeThread> threads);

    @Override
    protected void gatherThreads(final AppendableSequence<TeleNativeThread> threads) {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws IOException {
                    nativeGatherThreads(_task.tgid(), threads);
                    return null;
                }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Callback from JNI: creates new thread object or updates existing thread object with same thread ID.
     */
    void jniGatherThread(AppendableSequence<TeleNativeThread> threads, int tid, int state, long stackStart, long stackSize) {
        LinuxTeleNativeThread thread = (LinuxTeleNativeThread) idToThread(tid);
        if (thread == null) {
            thread = new LinuxTeleNativeThread(this, tid, stackStart, stackSize);
        }

        assert state >= 0 && state < ThreadState.VALUES.length() : state;
        thread.setState(ThreadState.VALUES.get(state));
        threads.append(thread);
    }

    @Override
    protected int read0(Address address, byte[] buffer, int offset, int length) {
        return _task.readBytes(address, buffer, offset, length);
    }

    @Override
    protected int write0(byte[] buffer, int offset, int length, Address address) {
        return _task.writeBytes(address, buffer, offset, length);
    }
}
