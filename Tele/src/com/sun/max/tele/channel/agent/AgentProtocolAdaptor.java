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
package com.sun.max.tele.channel.agent;

import java.io.*;
import java.util.*;

import com.sun.max.tele.channel.*;
import com.sun.max.tele.channel.iostream.TeleChannelDataIOProtocolImpl.ArrayMode;
import com.sun.max.tele.util.*;

/**
 * Base class for agent-side implementations of {@link TeleChannelProtocol}.
 *
 * @author Mick Jordan
 *
 */
public abstract class AgentProtocolAdaptor extends RemoteInvocationProtocolAdaptor implements TeleChannelDataIOProtocol {
    private TeleChannelProtocol impl;
    private TeleProcess teleProcess;

    /**
     * Create an {@code AgentProtocolAdaptor}.
     * @param teleProcess the appropriate subclass of the agent-side variant of {@link TeleProcess}
     * @param impl the {@code TeleChannelProtocol} to delegate to
     */
    protected AgentProtocolAdaptor(TeleProcess teleProcess, TeleChannelProtocol impl) {
        this.impl = impl;
        this.teleProcess = teleProcess;
        setArrayMode("create", 1, ArrayMode.IN);
        setArrayMode("readBytes", 1, ArrayMode.OUT);
        setArrayMode("writeBytes", 1, ArrayMode.IN);
        setArrayMode("readRegisters", 1, ArrayMode.OUT);
        setArrayMode("readRegisters", 3, ArrayMode.OUT);
        setArrayMode("readRegisters", 5, ArrayMode.OUT);
        setArrayMode("readThreads", 1, ArrayMode.OUT);
    }

    @Override
    public boolean initialize(int tlaSize, boolean bigEndian) {
        return impl.initialize(tlaSize, bigEndian);
    }
    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        return impl.activateWatchpoint(start, size, after, read, write, exec);
    }

    @Override
    public long create(String programFile, String[] commandLineArguments) {
        return impl.create(programFile, commandLineArguments);
    }

    @Override
    public boolean attach(int id) {
        return impl.attach(id);
    }

    @Override
    public boolean detach() {
        return impl.detach();
    }

    @Override
    public boolean kill() {
        return impl.kill();
    }

    @Override
    public int waitUntilStoppedAsInt() {
        return impl.waitUntilStoppedAsInt();
    }

    @Override
    public boolean deactivateWatchpoint(long start, long size) {
        return impl.deactivateWatchpoint(start, size);
    }

    @Override
    public long getBootHeapStart() {
        return impl.getBootHeapStart();
    }

    @Override
    public int maxByteBufferSize() {
        return impl.maxByteBufferSize();
    }

    @Override
    public int readBytes(long src, byte[] dst, int dstOffset, int length) {
        return impl.readBytes(src, dst, dstOffset, length);
    }

    @Override
    public boolean readRegisters(long threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        return impl.readRegisters(threadId, integerRegisters, integerRegistersSize, floatingPointRegisters, floatingPointRegistersSize, stateRegisters, stateRegistersSize);
    }

    @Override
    public int readWatchpointAccessCode() {
        return impl.readWatchpointAccessCode();
    }

    @Override
    public long readWatchpointAddress() {
        return impl.readWatchpointAddress();
    }

    @Override
    public boolean resume(long threadId) {
        return impl.resume(threadId);
    }

    @Override
    public boolean resumeAll() {
        return impl.resumeAll();
    }

    @Override
    public boolean setInstructionPointer(long threadId, long ip) {
        return impl.setInstructionPointer(threadId, ip);
    }

    @Override
    public int setTransportDebugLevel(int level) {
        return impl.setTransportDebugLevel(level);
    }

    @Override
    public boolean singleStep(long threadId) {
        return impl.singleStep(threadId);
    }

    @Override
    public boolean suspend(long threadId) {
        return impl.suspend(threadId);
    }

    @Override
    public boolean suspendAll() {
        return impl.suspendAll();
    }

    @Override
    public int writeBytes(long dst, byte[] src, int srcOffset, int length) {
        return impl.writeBytes(dst, src, srcOffset, length);
    }

    private byte[] threadData;
    private int numThreads;

    @Override
    public int gatherThreads(long tlaList) {
        List<TeleNativeThread> threads = new ArrayList<TeleNativeThread>();
        impl.gatherThreads(teleProcess, threads, tlaList);
        numThreads = threads.size();
        TeleChannelDataIOProtocol.GatherThreadData[] data = new TeleChannelDataIOProtocol.GatherThreadData[numThreads];
        int index = 0;
        for (TeleNativeThread tnt : threads) {
            data[index++] = tnt.getThreadData();
        }
        final ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bs);
            oos.writeObject(data);
            threadData = bs.toByteArray();
            return bs.size();
        } catch (IOException ex) {
            TeleError.unexpected(getClass().getName() + ".gatherThreads unexpected I/O error: ", ex);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                    bs.close();
                } catch (IOException ex) {
                }
            }
        }
        return 0;
    }

    @Override
    public int readThreads(int size, byte[] gatherThreadsData) {
        assert gatherThreadsData.length >= threadData.length;
        System.arraycopy(threadData, 0, gatherThreadsData, 0, threadData.length);
        return numThreads;
    }

}
