/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.debug.no;

import java.nio.*;

import com.sun.max.tele.channel.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.util.*;


/**
 * @author Mick Jordan
 *
 */
public class ReadOnlyTeleChannelProtocol implements TeleChannelProtocol {

    @Override
    public boolean initialize(int tlaSize, boolean bigEndian) {
        return true;
    }

    @Override
    public long create(String pathName, String[] commandLineArguments) {
        unexpected();
        return 0;
    }

    @Override
    public boolean attach(int id) {
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
    public int gatherThreads(long tlaList, long primordialETLA) {
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
    public boolean gatherThreads(Object teleDomain, Object threadSequence, long tlaList, long primordialETLA) {
        unexpected();
        return false;
    }

    @Override
    public ProcessState waitUntilStopped() {
        unexpected();
        return null;
    }

    private static void unexpected() {
        TeleError.unexpected("ReadOnlyTeleChannel method caalled unexpectedly");
    }

}
