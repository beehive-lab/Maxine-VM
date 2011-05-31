/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
import com.sun.max.vm.hosted.*;


/**
 * An implementation of {@link TeleChannelProtocol} for Linux that uses direct native method calls.
 *
 * Currently the Linux implementation does not use the {@link TeleChannelNatives} class owing to
 * the requirement to invoke the underlying native methods in the same thread, which is handled in
 * {@link LinuxTask}.
 */
public class LinuxNativeTeleChannelProtocol extends UnixNativeTeleChannelProtocolAdaptor implements LinuxTeleChannelProtocol {

    public LinuxNativeTeleChannelProtocol() {
        super(null);
    }

    private LinuxTask leaderTask;
    private Map<Integer, LinuxTask> taskMap = new HashMap<Integer, LinuxTask>();

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
    public long create(String programFile, String[] commandLineArguments) {
        final Pointer commandLineArgumentsBuffer;
        try {
            commandLineArgumentsBuffer = createBufferAndAgent(programFile, commandLineArguments);
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
    public boolean gatherThreads(final Object teleDomain, final Object threadList, final long tlaList) {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws IOException {
                    nativeGatherThreads(leaderTask.tgid(), teleDomain, threadList, tlaList);
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

    private static native void nativeGatherThreads(long pid, Object teleProcess, Object threadList, long tlaList);


}
