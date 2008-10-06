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
/*VCSID=fac02261-1655-4de0-9728-c846b83c5387*/
package com.sun.max.tele.debug.guestvm.xen;

import com.sun.max.collect.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;

/**
 * This class encapsulates all interaction with the Xen db communication channel.
 *
 * @author Mick Jordan
 *
 */
public final class GuestVMXenDBChannel {
    private static GuestVMXenTeleDomain _teleDomain;
    private static int _maxByteBufferSize;

    public static synchronized void attach(GuestVMXenTeleDomain teleDomain, int domId) {
        _teleDomain = teleDomain;
        nativeAttach(domId);
        _maxByteBufferSize = nativeMaxByteBufferSize();
    }

    public static synchronized Pointer getBootHeapStart() {
        return Pointer.fromLong(nativeGetBootHeapStart());
    }

    public static synchronized void setTransportDebugLevel(int level) {
        nativeSetTransportDebugLevel(level);
    }

    public static synchronized int readBytes(Address address, byte[] buffer, int offset, int length) {
        int lengthLeft = length;
        int localOffset = offset;
        long localAddress = address.toLong();
        while (lengthLeft > 0) {
            final int toDo = lengthLeft > _maxByteBufferSize ? _maxByteBufferSize : lengthLeft;
            final int r = nativeReadBytes(localAddress, buffer, localOffset, toDo);
            if (r != toDo) {
                return -1;
            }
            lengthLeft -= toDo;
            localOffset += toDo;
            localAddress += toDo;
        }
        return length;
    }

    public static synchronized int writeBytes(byte[] buffer, int offset, int length, Address address) {
        int lengthLeft = length;
        int localOffset = offset;
        long localAddress = address.toLong();
        while (lengthLeft > 0) {
            final int toDo = lengthLeft > _maxByteBufferSize ? _maxByteBufferSize : lengthLeft;
            final int r = nativeWriteBytes(localAddress, buffer, localOffset, toDo);
            if (r != toDo) {
                return -1;
            }
            lengthLeft -= toDo;
            localOffset += toDo;
            localAddress += toDo;
        }
        return length;
    }


    public static synchronized boolean gatherThreads(AppendableSequence<TeleNativeThread> threads, int domainId) {
        return nativeGatherThreads(threads, domainId);
    }

    public static synchronized int resume(int domainId) {
        return nativeResume(domainId);
    }

    public static synchronized int readByte(long domainId, long address) {
        return nativeReadByte(domainId, address);
    }

    public static synchronized long readInt(int domainId, long address) {
        return nativeReadInt(domainId, address);
    }

    public static synchronized int readShort(int domainId, long address) {
        return nativeReadShort(domainId, address);
    }

    public static synchronized boolean writeByte(int domainId, long address, byte value) {
        return nativeWriteByte(domainId, address, value);
    }

    public static synchronized int setInstructionPointer(int threadId, long ip) {
        return nativeSetInstructionPointer(threadId, ip);
    }

    public static synchronized boolean readRegisters(int threadId,
                    byte[] integerRegisters, int integerRegistersSize,
                    byte[] floatingPointRegisters, int floatingPointRegistersSize,
                    byte[] stateRegisters, int stateRegistersSize) {
        return nativeReadRegisters(threadId, integerRegisters, integerRegistersSize, floatingPointRegisters, floatingPointRegistersSize, stateRegisters, stateRegistersSize);
    }

    public static synchronized boolean singleStep(int threadId) {
        return nativeSingleStep(threadId);
    }

    public static synchronized boolean suspend(int threadId) {
        return nativeSuspend(threadId);
    }

    /**
     * This is upcalled from nativeGatherThreads.
     *
     * @param threads
     * @param threadId
     * @param name
     * @param state
     * @param stackBase
     * @param stackSize
     */
    static void jniGatherThread(AppendableSequence<TeleNativeThread> threads, int threadId, String name, int state, long stackBase, long stackSize) {
        _teleDomain.jniGatherThread(threads, threadId, name, state, stackBase, stackSize);
    }

    private static native boolean nativeAttach(int domId);
    private static native long nativeGetBootHeapStart();
    private static native int nativeSetTransportDebugLevel(int level);
    private static native int nativeReadBytes(long address, byte[] buffer, int offset, int length);
    private static native int nativeWriteBytes(long address, byte[] buffer, int offset, int length);
    private static native int nativeMaxByteBufferSize();
    private static native boolean nativeGatherThreads(AppendableSequence<TeleNativeThread> threads, int domainId);
    private static native int nativeResume(int domainId);
    private static native int nativeReadByte(long domainId, long address);
    private static native long nativeReadInt(int domainId, long address);
    private static native int nativeReadShort(int domainId, long address);
    private static native boolean nativeWriteByte(int domainId, long address, byte value);
    private static native int nativeSetInstructionPointer(int threadId, long ip);
    private static native boolean nativeSingleStep(int threadId);
    private static native boolean nativeSuspend(int threadId);

    private static native boolean nativeReadRegisters(int threadId,
                    byte[] integerRegisters, int integerRegistersSize,
                    byte[] floatingPointRegisters, int floatingPointRegistersSize,
                    byte[] stateRegisters, int stateRegistersSize);

}
