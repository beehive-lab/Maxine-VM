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
package com.sun.max.tele.debug.unix;

import java.io.*;
import java.net.*;
import java.nio.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.channel.*;
import com.sun.max.tele.channel.natives.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.runtime.*;

/**
 * Provides default implementations of the methods in {@link UnixAgentTeleChannelProtocol}.
 *
 * Makes use of {@link TeleChannelNatives} to invoke the corresponding native code.
 *
 * @author Mick Jordan
 *
 */
public class UnixNativeTeleChannelProtocolAdaptor implements TeleChannelProtocol {

    private long processHandle;
    private TeleChannelNatives natives;
    protected TeleVMAgent agent;

    public UnixNativeTeleChannelProtocolAdaptor(TeleChannelNatives natives) {
        this.natives = natives;
    }

    protected Pointer createBufferAndAgent(String programFile, String[] commandLineArguments) throws BootImageException {
        final Pointer commandLineArgumentsBuffer = TeleProcess.createCommandLineArgumentsBuffer(new File(programFile), commandLineArguments);
        this.agent = new TeleVMAgent();
        agent.start();
        return commandLineArgumentsBuffer;
    }

    @Override
    public long create(String programFile, String[] commandLineArguments, long extra) {
        final Pointer commandLineArgumentsBuffer;
        try {
            commandLineArgumentsBuffer = createBufferAndAgent(programFile, commandLineArguments);
        } catch (BootImageException ex) {
            return -1;
        }
        processHandle = natives.createChild(commandLineArgumentsBuffer.toLong(), agent.port());
        return processHandle;
    }

    @Override
    public boolean attach(int id, int threadLocalsAreaSize, long extra1) {
        return false;
    }

    @Override
    public boolean detach() {
        return false;
    }

    @Override
    public long getBootHeapStart() {
        try {
            final Socket socket = agent.waitForVM();
            final InputStream stream = socket.getInputStream();
            final Endianness endianness = TeleVM.vmConfigurationStatic().platform().processorKind.dataModel.endianness;
            final Pointer heap = Word.read(stream, endianness).asPointer();
            Trace.line(1, "Received boot image address from VM: 0x" + heap.toHexString());
            socket.close();
            agent.close();
            return heap.toLong();
        } catch (Exception ioException) {
            FatalError.unexpected("Error while reading boot image address from VM process", ioException);
            return 0;
        }
    }

    @Override
    public int maxByteBufferSize() {
        // no limit
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean readRegisters(long threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        return natives.readRegisters(processHandle, threadId,
                        integerRegisters, integerRegisters.length,
                        floatingPointRegisters, floatingPointRegisters.length,
                        stateRegisters, stateRegisters.length);
    }

    @Override
    public boolean setInstructionPointer(long threadId, long ip) {
        return natives.setInstructionPointer(processHandle, threadId, ip);
    }

    @Override
    public boolean singleStep(long threadId) {
        return natives.singleStep(processHandle, threadId);
    }

    @Override
    public int waitUntilStoppedAsInt() {
        return natives.waitUntilStopped(processHandle);
    }

    @Override
    public ProcessState waitUntilStopped() {
        final int state = waitUntilStoppedAsInt();
        return ProcessState.values()[state];
    }

    @Override
    public boolean suspendAll() {
        return natives.suspend(processHandle);
    }

    @Override
    public boolean resumeAll() {
        return natives.resume(processHandle);
    }

    @Override
    public boolean suspend(long threadId) {
        return natives.suspend(processHandle, threadId);
    }

    @Override
    public boolean resume(long threadId) {
        return natives.resume(threadId);
    }

    @Override
    public boolean kill() {
        natives.kill(processHandle);
        return true;
    }

    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        return natives.activateWatchpoint(processHandle, start, size, after, read, write, exec);
    }

    @Override
    public boolean deactivateWatchpoint(long start, long size) {
        return natives.deactivateWatchpoint(processHandle, start, size);
    }

    @Override
    public long readWatchpointAddress() {
        return natives.readWatchpointAddress(processHandle);
    }

    @Override
    public int readWatchpointAccessCode() {
        return natives.readWatchpointAccessCode(processHandle);
    }

    @Override
    public int setTransportDebugLevel(int level) {
        return 0;
    }

    @Override
    public int readBytes(long src, byte[] dst, int dstOffset, int length) {
        return natives.readBytes(processHandle, src, dst, false, dstOffset, length);
    }

    @Override
    public int writeBytes(long dst, byte[] src, int srcOffset, int length) {
        return natives.writeBytes(processHandle, dst, src, false, srcOffset, length);
    }

    @Override
    public int readBytes(long src, ByteBuffer dst, int dstOffset, int length) {
        if (dst.isDirect()) {
            return natives.readBytes(processHandle, src, dst, true, dstOffset, length);
        }
        return natives.readBytes(processHandle, src, dst.array(), false, dst.arrayOffset() + dstOffset, length);
    }

    @Override
    public int writeBytes(long dst, ByteBuffer src, int srcOffset, int length) {
        if (src.isDirect()) {
            return natives.writeBytes(processHandle, dst, src, true, srcOffset, length);
        }
        return natives.writeBytes(processHandle, dst, src.array(), false, src.arrayOffset() + srcOffset, length);
    }

    @Override
    public boolean gatherThreads(Object teleDomain, Object threadSequence, long threadLocalsList, long primordialThreadLocals) {
        natives.gatherThreads(processHandle, teleDomain, threadSequence, threadLocalsList, primordialThreadLocals);
        return true;
    }


    @Override
    public int gatherThreads(long threadLocalsList, long primordialThreadLocals) {
        ProgramError.unexpected("TeleChannelProtocol.gatherThreads(int, int) should not be called in this configuration");
        return 0;
    }

    @Override
    public int readThreads(int size, byte[] gatherThreadsData) {
        ProgramError.unexpected("TeleChannelProtocol.readThreads should not be called in this configuration");
        return 0;
    }


}
