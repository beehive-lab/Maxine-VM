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

    private final PTracedProcess _ptrace;

    PTracedProcess ptrace() {
        return _ptrace;
    }

    public int processID() {
        return _ptrace.processID();
    }

    private final PageDataAccess _pageDataAccess;

    @Override
    public PageDataAccess dataAccess() {
        return _pageDataAccess;
    }

    private void invalidateCache() {
        _pageDataAccess.invalidateCache();
    }

    private long _agent;

    private static native long nativeCreateAgent(int processID);

    LinuxTeleProcess(TeleVM teleVM, Platform platform, File programFile, String[] commandLineArguments, TeleVMAgent agent) throws BootImageException {
        super(teleVM, platform);
        final Pointer commandLineArgumentsBuffer = TeleProcess.createCommandLineArgumentsBuffer(programFile, commandLineArguments);
        _ptrace = PTracedProcess.createChild(commandLineArgumentsBuffer.toLong(), agent.port());
        if (_ptrace == null) {
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
        _ptrace.kill();
    }

    @Override
    protected void resume() throws OSExecutionRequestException {
        _ptrace.resume();
    }

    @Override
    protected void suspend() throws OSExecutionRequestException {
        _ptrace.suspend();
    }

    @Override
    protected boolean waitUntilStopped() {
        final boolean result = _ptrace.waitUntilStopped();
        invalidateCache();
        return result;
    }

    private native boolean nativeGatherThreads(long agent, AppendableSequence<TeleNativeThread> threads);

    @Override
    protected boolean gatherThreads(final AppendableSequence<TeleNativeThread> threads) {
        try {
            return SingleThread.executeWithException(new Function<Boolean>() {
                public Boolean call() throws IOException {
                    if (_agent == 0L) {
                        _agent = nativeCreateAgent(_ptrace.processID());
                        if (_agent == 0L) {
                            throw new TeleError("could not create thread_db agent");
                        }
                    }
                    return nativeGatherThreads(_agent, threads);
                }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    /**
     * Callback from JNI: creates new thread object or updates existing thread object with same thread ID.
     */
    void jniGatherThread(AppendableSequence<TeleNativeThread> threads, long threadID, int lwpId, int state, long stackStart, long stackSize) {
        LinuxTeleNativeThread thread = (LinuxTeleNativeThread) idToThread(threadID);
        if (thread == null) {
            thread = new LinuxTeleNativeThread(this, threadID, lwpId, stackStart, stackSize);
        }

        assert state >= 0 && state < ThreadState.VALUES.length() : state;
        thread.setState(ThreadState.VALUES.get(state));
        threads.append(thread);
    }

    @Override
    protected int read0(Address address, byte[] buffer, int offset, int length) {
        return _ptrace.readBytes(address, buffer, offset, length);
    }

    @Override
    protected int write0(byte[] buffer, int offset, int length, Address address) {
        return _ptrace.writeBytes(address, buffer, offset, length);
    }
}
