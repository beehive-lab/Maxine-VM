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
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleNativeThread.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;

/**
 * @author Bernd Mathiske
 */
public final class LinuxTeleProcess extends TeleProcess {

    private final Ptrace _ptrace;

    Ptrace ptrace() {
        return _ptrace;
    }

    public int processID() {
        return _ptrace.processID();
    }

    private final DataAccess _dataAccess;

    @Override
    public DataAccess dataAccess() {
        return _dataAccess;
    }

    private long _agent;

    private static native long nativeCreateAgent(int processID);

    public void initializeDebugging() {
        SingleThread.execute(new Runnable() {
            public void run() {
                _agent = nativeCreateAgent(_ptrace.processID());
            }
        });
        if (_agent == 0L) {
            throw new TeleError("could not create thread_db agent");
        }
    }

    LinuxTeleProcess(TeleVM teleVM, Platform platform, File programFile, String[] commandLineArguments) {
        super(teleVM, platform, programFile, commandLineArguments);
        _ptrace = Ptrace.createChild(programFile.getAbsolutePath());
        _dataAccess = new StreamDataAccess(new PtraceDataStreamFactory(_ptrace), platform.processorKind().dataModel());
    }

    public void waitForNextSyscall() throws IOException {
        _ptrace.syscall();
    }

    private static native boolean nativeFreeAgent(long agent);

    @Override
    protected void kill() throws OSExecutionRequestException {
        try {
            SingleThread.execute(new Runnable() {
                public void run() {
                    nativeFreeAgent(_agent);
                }
            });
            _agent = 0;
            _ptrace.kill();
        } catch (IOException ioException) {
            throw new TeleError(ioException);
        }
    }

    @Override
    protected void resume() throws OSExecutionRequestException {
        Problem.unimplemented();
    }

    @Override
    protected void suspend() throws OSExecutionRequestException {
        Problem.unimplemented();
    }

    @Override
    protected boolean waitUntilStopped() {
        throw Problem.unimplemented();
    }

    private native boolean nativeGatherThreads(long agent, AppendableSequence<TeleNativeThread> threads);

    @Override
    protected boolean gatherThreads(final AppendableSequence<TeleNativeThread> threads) {
        try {
            return SingleThread.executeWithException(new Function<Boolean>() {
                public Boolean call() throws IOException {
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

        assert state >= 0 && state < ThreadState.VALUES.length();
        thread.setState(ThreadState.VALUES.get(state));
        threads.append(thread);
    }

    @Override
    protected int read0(Address address, byte[] buffer, int offset, int length) {
        throw Problem.unimplemented();
    }

    @Override
    protected int write0(byte[] buffer, int offset, int length, Address address) {
        throw Problem.unimplemented();
    }

}
