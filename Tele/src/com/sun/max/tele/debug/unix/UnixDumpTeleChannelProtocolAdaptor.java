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
import java.nio.*;

import com.sun.max.tele.channel.*;
import com.sun.max.tele.debug.*;
import com.sun.max.vm.runtime.*;


public class UnixDumpTeleChannelProtocolAdaptor implements TeleChannelProtocol {

    protected UnixDumpTeleChannelProtocolAdaptor(File vm, File dump) {
        FatalError.unexpected("core dump access not supported");
    }

    @Override
    public boolean initialize(int threadLocalsAreaSize) {
        return true;
    }

    @Override
    public long create(String pathName, String[] commandLineArguments) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean attach(int id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean detach() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long getBootHeapStart() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int maxByteBufferSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int readBytes(long src, byte[] dst, int dstOffset, int length) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int writeBytes(long dst, byte[] src, int srcOffset, int length) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean readRegisters(long threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int gatherThreads(long threadLocalsList, long primordialThreadLocals) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int readThreads(int size, byte[] gatherThreadsData) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean setInstructionPointer(long threadId, long ip) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean singleStep(long threadId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean resumeAll() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean suspendAll() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean resume(long threadId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean suspend(long threadId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int waitUntilStoppedAsInt() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean kill() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deactivateWatchpoint(long start, long size) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long readWatchpointAddress() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int readWatchpointAccessCode() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int setTransportDebugLevel(int level) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int readBytes(long src, ByteBuffer dst, int dstOffset, int length) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int writeBytes(long dst, ByteBuffer src, int srcOffset, int length) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean gatherThreads(Object teleDomain, Object threadSequence, long threadLocalsList, long primordialThreadLocals) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ProcessState waitUntilStopped() {
        // TODO Auto-generated method stub
        return null;
    }

}
