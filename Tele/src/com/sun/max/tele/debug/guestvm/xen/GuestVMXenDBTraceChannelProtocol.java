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
package com.sun.max.tele.debug.guestvm.xen;

import com.sun.max.collect.*;
import com.sun.max.tele.debug.*;
import com.sun.max.program.*;

public class GuestVMXenDBTraceChannelProtocol extends GuestVMXenDBChannelProtocolAdaptor {

    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        return false;
    }

    @Override
    public boolean attach(int domId) {
        Trace.line(1, "attach " + domId);
        return true;
    }

    @Override
    public boolean deactivateWatchpoint(long start, long size) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean gatherThreads(GuestVMXenTeleDomain teleDomain, AppendableSequence<TeleNativeThread> threads, long threadLocalsList, long primordialThreadLocals) {
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
    public int readByte(long address) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int readBytes(long src, Object dst, boolean isDirectByteBuffer, int dstOffset, int length) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long readInt(long address) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean readRegisters(int threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int readShort(long address) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int readWatchpointAccessCode() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long readWatchpointAddress() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int resume() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int setInstructionPointer(int threadId, long ip) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int setTransportDebugLevel(int level) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean singleStep(int threadId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean suspend(int threadId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean suspendAll() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean writeByte(long address, byte value) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int writeBytes(long dst, Object src, boolean isDirectByteBuffer, int srcOffset, int length) {
        // TODO Auto-generated method stub
        return 0;
    }

}
