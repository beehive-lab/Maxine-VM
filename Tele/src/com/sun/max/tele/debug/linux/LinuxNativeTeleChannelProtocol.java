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
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.tele.channel.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.unix.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.prototype.*;


/**
 * An implementation of {@link TeleChannelProtocol} for Linux that uses direct native method calls.
 *
 * Currently the Linux implementation does not use the {@link TeleChannelNatives} class owing to
 * the requirement to invoke the underlying native methods in the same thread, which is handed in
 * {@link LinuxTask}.
 *
 *
 * @author Mick Jordan
 *
 */
public class LinuxNativeTeleChannelProtocol extends UnixNativeTeleChannelProtocolAdaptor implements LinuxTeleChannelProtocol {

    public LinuxNativeTeleChannelProtocol() {
        super(null);
    }

    private LinuxTask leaderTask;
    private Map<Integer, LinuxTask> taskMap = new HashMap<Integer, LinuxTask>();;

    private LinuxTask task(long ltid) {
        final int tid = (int) ltid;
        LinuxTask result = taskMap.get(tid);
        if (result == null) {
            result = new LinuxTask(leaderTask, tid);
            taskMap.put(tid, result);
        }
        return result;
    }

    @Override
    public long create(String programFile, String[] commandLineArguments, int threadLocalsAreaSize) {
        final Pointer commandLineArgumentsBuffer;
        try {
            commandLineArgumentsBuffer = createBufferAndAgent(programFile, commandLineArguments, threadLocalsAreaSize);
        } catch (BootImageException ex) {
            return -1;
        }
        leaderTask = LinuxTask.createChild(commandLineArgumentsBuffer.toLong(), agent.port());
        if (leaderTask == null) {
            return -1;
        }
        return 1;
    }

    @Override
    public int readBytes(long src, byte[] dst, int dstOffset, int length) {
        return leaderTask.readBytes(src, dst, false, dstOffset, length);
    }

    @Override
    public int readBytes(long src, ByteBuffer dst, int dstOffset, int length) {
        if (dst.isDirect()) {
            return leaderTask.readBytes(src, dst, true, dstOffset, length);
        }
        return leaderTask.readBytes(src, dst.array(), false, dst.arrayOffset() + dstOffset, length);
    }

    @Override
    public int writeBytes(long dst, byte[] src, int srcOffset, int length) {
        return leaderTask.writeBytes(dst, src, false, srcOffset, length);
    }

    @Override
    public int writeBytes(long dst, ByteBuffer src, int srcOffset, int length) {
        if (src.isDirect()) {
            return leaderTask.writeBytes(dst, src, true, srcOffset, length);
        }
        return leaderTask.writeBytes(dst, src.array(), false, src.arrayOffset() + srcOffset, length);
    }

    @Override
    public boolean gatherThreads(final Object teleDomain, final Object threadSequence, final long threadLocalsList, final long primordialThreadLocals) {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws IOException {
                    nativeGatherThreads(leaderTask.tgid(), teleDomain, threadSequence, threadLocalsList, primordialThreadLocals);
                    return null;
                }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean readRegisters(long threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        return task(threadId).readRegisters(integerRegisters, floatingPointRegisters, stateRegisters);
    }

    @Override
    public boolean setInstructionPointer(long threadId, long ip) {
        return task(threadId).setInstructionPointer(ip);
    }

    @Override
    public boolean singleStep(long threadId) {
        return task(threadId).singleStep();
    }

    @Override
    public boolean resumeAll() {
        try {
            leaderTask.resume(true);
            return true;
        } catch (OSExecutionRequestException ex) {
            return false;
        }
    }

    @Override
    public boolean suspendAll() {
        try {
            leaderTask.suspend(true);
            return true;
        } catch (OSExecutionRequestException ex) {
            return false;
        }
    }

    @Override
    public boolean resume(long threadId) {
        return false;
    }

    @Override
    public boolean suspend(long threadId) {
        return false;
    }

    @Override
    public int waitUntilStoppedAsInt() {
        final ProcessState result = waitUntilStopped();
        return result.ordinal();
    }

    @Override
    public ProcessState waitUntilStopped() {
        final ProcessState result = leaderTask.waitUntilStopped(true);
        if (result != ProcessState.STOPPED) {
            leaderTask.close();
        }
        return result;
    }

    @Override
    public boolean kill() {
        try {
            leaderTask.kill();
            return true;
        } catch (OSExecutionRequestException ex) {
            return false;
        }
    }

    private static native void nativeGatherThreads(long pid, Object teleProcess, Object threadSequence, long threadLocalsList, long primordialThreadLocals);


}
