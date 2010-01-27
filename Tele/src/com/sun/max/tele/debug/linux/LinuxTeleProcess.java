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
import java.nio.*;

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

    private final LinuxTask task;

    LinuxTask task() {
        return task;
    }

    private final DataAccess dataAccess;

    @Override
    public DataAccess dataAccess() {
        return dataAccess;
    }

    LinuxTeleProcess(TeleVM teleVM, Platform platform, File programFile, String[] commandLineArguments, TeleVMAgent agent) throws BootImageException {
        super(teleVM, platform, ProcessState.STOPPED);
        final Pointer commandLineArgumentsBuffer = TeleProcess.createCommandLineArgumentsBuffer(programFile, commandLineArguments);
        task = LinuxTask.createChild(commandLineArgumentsBuffer.toLong(), agent.port());
        if (task == null) {
            throw new BootImageException("Error launching VM");
        }
        dataAccess = new PageDataAccess(this, platform.processorKind.dataModel);
        try {
            resume();
        } catch (OSExecutionRequestException e) {
            throw new BootImageException("Error resuming VM after starting it", e);
        }
    }

    @Override
    public int maximumWatchpointCount() {
        return 0;
    }

    @Override
    protected void kill() throws OSExecutionRequestException {
        task.kill();
    }

    @Override
    protected void resume() throws OSExecutionRequestException {
        task.resume(true);
    }

    @Override
    protected void suspend() throws OSExecutionRequestException {
        task.suspend(true);
    }

    @Override
    protected ProcessState waitUntilStopped() {
        final ProcessState result = task.waitUntilStopped(true);
        if (result != ProcessState.STOPPED) {
            task.close();
        }
        return result;
    }

    private native void nativeGatherThreads(long pid, AppendableSequence<TeleNativeThread> threads, long threadLocalsList, long primordialThreadLocals);

    @Override
    protected TeleNativeThread createTeleNativeThread(Params params) {
        return new LinuxTeleNativeThread(this, params);
    }

    @Override
    protected void gatherThreads(final AppendableSequence<TeleNativeThread> threads) {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws IOException {
                    final Word primordialThreadLocals = dataAccess().readWord(teleVM().bootImageStart().plus(teleVM().bootImage().header.primordialThreadLocalsOffset));
                    final Word threadLocalsList = dataAccess().readWord(teleVM().bootImageStart().plus(teleVM().bootImage().header.threadLocalsListHeadOffset));
                    nativeGatherThreads(task.tgid(), threads, threadLocalsList.asAddress().toLong(), primordialThreadLocals.asAddress().toLong());
                    return null;
                }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    protected int read0(Address address, ByteBuffer buffer, int offset, int length) {
        return task.readBytes(address, buffer, offset, length);
    }

    @Override
    protected int write0(ByteBuffer buffer, int offset, int length, Address address) {
        return task.writeBytes(address, buffer, offset, length);
    }
}
