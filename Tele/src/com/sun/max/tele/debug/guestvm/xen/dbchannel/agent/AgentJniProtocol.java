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
package com.sun.max.tele.debug.guestvm.xen.dbchannel.agent;

import java.io.*;

import static com.sun.max.tele.debug.guestvm.xen.dbchannel.dataio.DataIOProtocol.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.guestvm.xen.dbchannel.*;
import com.sun.max.tele.debug.guestvm.xen.dbchannel.jni.*;

/**
 * A {@link SimpleProtocol} implementation that is called reflectively by {@link ProtocolAgent}
 * and delegates to the standard {@link JniProtocol}, save the {@link SimpleProtocol#gatherThreads(long, long)}
 * and {@link SimpleProtocol#readThreads} methods, which are implemented here.
 *
 * @author Mick Jordan
 *
 */

public class AgentJniProtocol extends RIProtocolAdaptor implements SimpleProtocol {
    private JniProtocol impl;

    public AgentJniProtocol() {
        this.impl = new JniProtocol();
        setArrayMode("readBytes", 1, ArrayMode.OUT);
        setArrayMode("writeBytes", 1, ArrayMode.IN);
        setArrayMode("readRegisters", 1, ArrayMode.OUT);
        setArrayMode("readRegisters", 3, ArrayMode.OUT);
        setArrayMode("readRegisters", 5, ArrayMode.OUT);
        setArrayMode("readThreads", 1, ArrayMode.OUT);
    }

    private static native void teleThreadLocalsInitialize(int threadLocalsSize);

    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        return impl.activateWatchpoint(start, size, after, read, write, exec);
    }

    @Override
    public boolean attach(int domId, int threadLocalsAreaSize) {
        teleThreadLocalsInitialize(threadLocalsAreaSize);
        return impl.attach(domId, threadLocalsAreaSize);
    }

    @Override
    public boolean detach() {
        return impl.detach();
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
    public boolean readRegisters(int threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
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
    public int resume() {
        return impl.resume();
    }

    @Override
    public int setInstructionPointer(int threadId, long ip) {
        return impl.setInstructionPointer(threadId, ip);
    }

    @Override
    public int setTransportDebugLevel(int level) {
        return impl.setTransportDebugLevel(level);
    }

    @Override
    public boolean singleStep(int threadId) {
        return impl.singleStep(threadId);
    }

    @Override
    public boolean suspend(int threadId) {
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
    public int gatherThreads(long threadLocalsList, long primordialThreadLocals) {
        GuestVMXenTeleDomain teleDomain = new GuestVMXenTeleDomain();
        AppendableSequence<TeleNativeThread> threads = new ArrayListSequence<TeleNativeThread>();
        impl.gatherThreads(teleDomain, threads, threadLocalsList, primordialThreadLocals);
        numThreads = threads.length();
        SimpleProtocol.GatherThreadData[] data = new SimpleProtocol.GatherThreadData[numThreads];
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
            ProgramError.unexpected(getClass().getName() + ".gatherThreads unexpected I/O error: ", ex);
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
