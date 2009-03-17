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
package com.sun.max.tele.debug.solaris;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleNativeThread.*;
import com.sun.max.tele.page.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.prototype.*;

/**
 * @author Bernd Mathiske
 * @author Aritra Bandyopadhyay
 * @author Doug Simon
 */
public final class SolarisTeleProcess extends TeleProcess {

    private final PageDataAccess _pageDataAccess;

    @Override
    public PageDataAccess dataAccess() {
        return _pageDataAccess;
    }

    public void invalidateCache() {
        _pageDataAccess.invalidateCache();
    }

    private static native long nativeCreateChild(long argv, int vmAgentPort);

    private long _processHandle;

    long processHandle() {
        return _processHandle;
    }

    /**
     * Creates a handle to a native Solaris process by launching a new process with a given set of command line arguments.
     *
     * @param teleVM
     * @param platform
     * @param programFile
     * @param commandLineArguments
     * @param agent TODO
     * @throws BootImageException
     */
    SolarisTeleProcess(TeleVM teleVM, Platform platform, File programFile, String[] commandLineArguments, TeleVMAgent agent) throws BootImageException {
        super(teleVM, platform);
        final Pointer commandLineArgumentsBuffer = TeleProcess.createCommandLineArgumentsBuffer(programFile, commandLineArguments);
        _processHandle = nativeCreateChild(commandLineArgumentsBuffer.toLong(), agent.port());
        _pageDataAccess = new PageDataAccess(platform.processorKind().dataModel(), this);
        try {
            resume();
        } catch (OSExecutionRequestException e) {
            throw new BootImageException("Error resuming VM after starting it", e);
        }
    }

    private static native void nativeKill(long processHandle);

    @Override
    public void kill() throws OSExecutionRequestException {
        nativeKill(_processHandle);
    }

    private static native boolean nativeSuspend(long processHandle);

    @Override
    public void suspend() throws OSExecutionRequestException {
        if (!nativeSuspend(_processHandle)) {
            throw new OSExecutionRequestException("Could not suspend process");
        }
    }

    private static native boolean nativeResume(long processHandle);

    @Override
    protected void resume() throws OSExecutionRequestException {
        if (!nativeResume(_processHandle)) {
            throw new OSExecutionRequestException("The VM could not be resumed");
        }
    }

    /**
     * Waits until this process is stopped.
     */
    private static native boolean nativeWait(long processHandle);

    @Override
    protected boolean waitUntilStopped() {
        final boolean result = nativeWait(_processHandle);
        invalidateCache();
        return result;
    }

    private native boolean nativeGatherThreads(long processHandle, AppendableSequence<TeleNativeThread> threads);

    @Override
    protected boolean gatherThreads(AppendableSequence<TeleNativeThread> threads) {
        return nativeGatherThreads(_processHandle, threads);
    }

    /**
     * Callback from JNI: creates new thread object or updates existing thread object with same thread ID.
     */
    void jniGatherThread(AppendableSequence<TeleNativeThread> threads, long lwpID, int state, long stackStart, long stackSize) {
        SolarisTeleNativeThread thread = (SolarisTeleNativeThread) idToThread(lwpID);
        if (thread == null) {
            thread = new SolarisTeleNativeThread(this, lwpID, stackStart, stackSize);
        }

        assert state >= 0 && state < ThreadState.VALUES.length();
        thread.setState(ThreadState.VALUES.get(state));
        threads.append(thread);
    }

    private static native int nativeReadBytes(long processHandle, long address, byte[] buffer, int offset, int length);

    @Override
    protected int read0(Address address, byte[] buffer, int offset, int length) {
        return nativeReadBytes(_processHandle, address.toLong(), buffer, offset, length);
    }

    private static native int nativeWriteBytes(long processHandle, long address, byte[] buffer, int offset, int length);

    @Override
    protected int write0(byte[] buffer, int offset, int length, Address address) {
        return nativeWriteBytes(_processHandle, address.toLong(), buffer, offset, length);
    }

    private native boolean nativeActivateWatchpoint(long processHandle, long start, long size);

    @Override
    protected boolean activateWatchpoint(MemoryRegion memoryRegion) {
        return nativeActivateWatchpoint(_processHandle, memoryRegion.start().toLong(), memoryRegion.size().toLong());
    }
}
