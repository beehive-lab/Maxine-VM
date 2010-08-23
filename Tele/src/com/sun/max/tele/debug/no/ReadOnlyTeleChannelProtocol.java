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
package com.sun.max.tele.debug.no;

import java.nio.*;

import com.sun.max.program.*;
import com.sun.max.tele.channel.*;
import com.sun.max.tele.debug.*;


/**
 * @author Mick Jordan
 *
 */
public class ReadOnlyTeleChannelProtocol implements TeleChannelProtocol {

    @Override
    public long create(String pathName, String[] commandLineArguments, long extra1) {
        unexpected();
        return 0;
    }

    @Override
    public boolean attach(int id, int threadLocalsAreaSize, long extra1) {
        unexpected();
        return false;
    }

    @Override
    public boolean detach() {
        unexpected();
        return false;
    }

    @Override
    public long getBootHeapStart() {
        unexpected();
        return 0;
    }

    @Override
    public int maxByteBufferSize() {
        unexpected();
        return 0;
    }

    @Override
    public int readBytes(long src, byte[] dst, int dstOffset, int length) {
        unexpected();
        return 0;
    }

    @Override
    public int writeBytes(long dst, byte[] src, int srcOffset, int length) {
        unexpected();
        return 0;
    }

    @Override
    public boolean readRegisters(long threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        unexpected();
        return false;
    }

    @Override
    public int gatherThreads(long threadLocalsList, long primordialThreadLocals) {
        unexpected();
        return 0;
    }

    @Override
    public int readThreads(int size, byte[] gatherThreadsData) {
        unexpected();
        return 0;
    }

    @Override
    public boolean setInstructionPointer(long threadId, long ip) {
        unexpected();
        return false;
    }

    @Override
    public boolean singleStep(long threadId) {
        unexpected();
        return false;
    }

    @Override
    public boolean resumeAll() {
        unexpected();
        return false;
    }

    @Override
    public boolean suspendAll() {
        unexpected();
        return false;
    }

    @Override
    public boolean resume(long threadId) {
        unexpected();
        return false;
    }

    @Override
    public boolean suspend(long threadId) {
        unexpected();
        return false;
    }

    @Override
    public int waitUntilStoppedAsInt() {
        unexpected();
        return 0;
    }

    @Override
    public boolean kill() {
        unexpected();
        return false;
    }

    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        unexpected();
        return false;
    }

    @Override
    public boolean deactivateWatchpoint(long start, long size) {
        unexpected();
        return false;
    }

    @Override
    public long readWatchpointAddress() {
        unexpected();
        return 0;
    }

    @Override
    public int readWatchpointAccessCode() {
        unexpected();
        return 0;
    }

    @Override
    public int setTransportDebugLevel(int level) {
        unexpected();
        return 0;
    }

    @Override
    public int readBytes(long src, ByteBuffer dst, int dstOffset, int length) {
        unexpected();
        return 0;
    }

    @Override
    public int writeBytes(long dst, ByteBuffer src, int srcOffset, int length) {
        unexpected();
        return 0;
    }

    @Override
    public boolean gatherThreads(Object teleDomain, Object threadSequence, long threadLocalsList, long primordialThreadLocals) {
        unexpected();
        return false;
    }

    @Override
    public ProcessState waitUntilStopped() {
        unexpected();
        return null;
    }

    private static void unexpected() {
        ProgramError.unexpected("ReadOnlyTeleChannel method caalled unexpectedly");
    }

}
