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
import java.nio.*;

import com.sun.max.collect.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.page.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.prototype.*;

/**
 * @author Bernd Mathiske
 */
public final class DarwinTeleProcess extends TeleProcess {

    private final DataAccess dataAccess;

    @Override
    public DataAccess dataAccess() {
        return dataAccess;
    }

    private static native long nativeCreateChild(long argv, int vmAgentSocketPort);
    private static native void nativeKill(long task);
    private static native boolean nativeSuspend(long task);
    private static native boolean nativeResume(long task);
    private static native boolean nativeWait(long pid, long task);

    private final long task;

    public long task() {
        return task;
    }

    DarwinTeleProcess(TeleVM teleVM, Platform platform, File programFile, String[] commandLineArguments, TeleVMAgent agent) throws BootImageException {
        super(teleVM, platform, ProcessState.STOPPED);
        final Pointer commandLineArgumentsBuffer = TeleProcess.createCommandLineArgumentsBuffer(programFile, commandLineArguments);
        task = nativeCreateChild(commandLineArgumentsBuffer.toLong(), agent.port());
        if (task == -1) {
            ProgramError.unexpected(String.format("task_for_pid() permissions problem -- Need to run java as setgid procmod:%n%n" +
                "    chgrp procmod <java executable>;  chmod g+s <java executable>%n%n" +
                "where <java executable> is the platform dependent executable found under or relative to " + System.getProperty("java.home") + "."));
        }
        dataAccess = new PageDataAccess(this, platform.processorKind.dataModel);
        try {
            resume();
        } catch (OSExecutionRequestException e) {
            throw new BootImageException("Error resuming VM after starting it", e);
        }
    }

    @Override
    public void suspend() throws OSExecutionRequestException {
        if (!nativeSuspend(task)) {
            ProgramError.unexpected("could not suspend process");
        }
    }

    @Override
    public void resume() throws OSExecutionRequestException {
        if (!nativeResume(task)) {
            throw new OSExecutionRequestException("Resume could not be completed");
        }
    }

    @Override
    protected boolean waitUntilStopped() {
        final boolean ok = nativeWait(task, task);
        return ok;
    }

    @Override
    protected void kill() throws OSExecutionRequestException {
        nativeKill(task);
    }

    private native void nativeGatherThreads(long task, AppendableSequence<TeleNativeThread> threads, long threadLocalsList, long primordialThreadLocals);

    @Override
    protected void gatherThreads(AppendableSequence<TeleNativeThread> threads) {
        final Word primordialThreadLocals = dataAccess().readWord(teleVM().bootImageStart().plus(teleVM().bootImage().header.primordialThreadLocalsOffset));
        final Word threadLocalsList = dataAccess().readWord(teleVM().bootImageStart().plus(teleVM().bootImage().header.threadLocalsListHeadOffset));
        nativeGatherThreads(task, threads, threadLocalsList.asAddress().toLong(), primordialThreadLocals.asAddress().toLong());
    }

    @Override
    protected TeleNativeThread createTeleNativeThread(int id, long machThread, long stackBase, long stackSize) {
        return new DarwinTeleNativeThread(this, id, machThread, stackBase, stackSize);
    }

    /**
     * Copies bytes from the tele process into a given {@linkplain ByteBuffer#isDirect() direct ByteBuffer} or byte
     * array.
     *
     * @param src the address in the tele process to copy from
     * @param dst the destination of the copy operation. This is a direct {@link ByteBuffer} or {@code byte[]}
     *            depending on the value of {@code isDirectByteBuffer}
     * @param isDirectByteBuffer
     * @param dstOffset the offset in {@code dst} at which to start writing
     * @param length the number of bytes to copy
     * @return the number of bytes copied or -1 if there was an error
     */
    private static native int nativeReadBytes(long task, long src, Object dst, boolean isDirectByteBuffer, int dstOffset, int length);

    @Override
    protected int read0(Address src, ByteBuffer dst, int offset, int length) {
        assert dst.limit() - offset >= length;
        if (dst.isDirect()) {
            return nativeReadBytes(task, src.toLong(), dst, true, offset, length);
        }
        assert dst.array() != null;
        return nativeReadBytes(task, src.toLong(), dst.array(), false, dst.arrayOffset() + offset, length);
    }

    /**
     * Copies bytes from a given {@linkplain ByteBuffer#isDirect() direct ByteBuffer} or byte array into the tele process.
     *
     * @param dst the address in the tele process to copy to
     * @param src the source of the copy operation. This is a direct {@link ByteBuffer} or {@code byte[]}
     *            depending on the value of {@code isDirectByteBuffer}
     * @param isDirectByteBuffer
     * @param srcOffset the offset in {@code src} at which to start reading
     * @param length the number of bytes to copy
     * @return the number of bytes copied or -1 if there was an error
     */
    private static native int nativeWriteBytes(long task, long dst, Object src, boolean isDirectByteBuffer, int srcOffset, int length);

    @Override
    protected int write0(ByteBuffer src, int offset, int length, Address dst) {
        assert src.limit() - offset >= length;
        if (src.isDirect()) {
            return nativeWriteBytes(task, dst.toLong(), src, true, offset, length);
        }
        assert src.array() != null;
        return nativeWriteBytes(task, dst.toLong(), src.array(), false, src.arrayOffset() + offset, length);
    }
}
