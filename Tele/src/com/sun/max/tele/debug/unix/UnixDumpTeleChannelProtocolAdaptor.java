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

import java.io.File;

import com.sun.max.program.*;
import com.sun.max.tele.channel.*;
import com.sun.max.tele.channel.iostream.*;
import com.sun.max.tele.debug.*;


public abstract class UnixDumpTeleChannelProtocolAdaptor extends TeleChannelDataIOProtocolAdaptor implements TeleChannelProtocol {

    protected int threadLocalsAreaSize;
    public boolean bigEndian;

    protected UnixDumpTeleChannelProtocolAdaptor(File vm, File dump) {
    }

    @Override
    public boolean initialize(int threadLocalsAreaSize, boolean bigEndian) {
        this.threadLocalsAreaSize = threadLocalsAreaSize;
        this.bigEndian = bigEndian;
        return true;
    }

    @Override
    public long create(String pathName, String[] commandLineArguments) {
        inappropriate("create");
        return -1;
    }

    @Override
    public boolean attach(int id) {
        return true;
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public abstract long getBootHeapStart();

    @Override
    public int maxByteBufferSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public abstract int readBytes(long src, byte[] dst, int dstOffset, int length);

    @Override
    public abstract int writeBytes(long dst, byte[] src, int srcOffset, int length);

    @Override
    public abstract boolean readRegisters(long threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize);

    @Override
    public int gatherThreads(long threadLocalsList, long primordialThreadLocals) {
        inappropriate("gatherThreads");
        return 0;
    }

    @Override
    public int readThreads(int size, byte[] gatherThreadsData) {
        inappropriate("readThreads");
        return 0;
    }

    @Override
    public boolean setInstructionPointer(long threadId, long ip) {
        inappropriate("setInstructionPointer");
        return false;
    }

    @Override
    public boolean singleStep(long threadId) {
        inappropriate("setInstructionPointer");
        return false;
    }

    @Override
    public boolean resumeAll() {
        inappropriate("resumeAll");
        return false;
    }

    @Override
    public boolean suspendAll() {
        inappropriate("suspendAll");
        return false;
    }

    @Override
    public boolean resume(long threadId) {
        inappropriate("resume");
        return false;
    }

    @Override
    public boolean suspend(long threadId) {
        inappropriate("suspend");
        return false;
    }

    @Override
    public int waitUntilStoppedAsInt() {
        inappropriate("waitUntilStoppedAsInt");
        return 0;
    }

    @Override
    public boolean kill() {
        return true;
    }

    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        inappropriate("activateWatchpoint");
        return false;
    }

    @Override
    public boolean deactivateWatchpoint(long start, long size) {
        inappropriate("deactivateWatchpoint");
        return false;
    }

    @Override
    public long readWatchpointAddress() {
        inappropriate("readWatchpointAddress");
        return 0;
    }

    @Override
    public int readWatchpointAccessCode() {
        inappropriate("readWatchpointAccessCode");
        return 0;
    }

    @Override
    public int setTransportDebugLevel(int level) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ProcessState waitUntilStopped() {
        inappropriate("waitUntilStoppedAsInt");
        return null;
    }

    private static void inappropriate(String methodName) {
        ProgramError.unexpected("method: " + methodName + " should not be called in dump mode");
    }

}
