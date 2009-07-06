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

import java.nio.*;

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
    private static GuestVMXenTeleDomain teleDomain;
    private static int maxByteBufferSize;

    public static synchronized void attach(GuestVMXenTeleDomain teleDomain, int domId) {
        GuestVMXenDBChannel.teleDomain = teleDomain;
        nativeAttach(domId);
        maxByteBufferSize = nativeMaxByteBufferSize();
    }

    public static synchronized Pointer getBootHeapStart() {
        return Pointer.fromLong(nativeGetBootHeapStart());
    }

    public static synchronized void setTransportDebugLevel(int level) {
        nativeSetTransportDebugLevel(level);
    }

    private static int readBytes0(long src, ByteBuffer dst, int dstOffset, int length) {
        assert dst.limit() - dstOffset >= length;
        if (dst.isDirect()) {
            return nativeReadBytes(src, dst, true, dstOffset, length);
        }
        assert dst.array() != null;
        return nativeReadBytes(src, dst.array(), false, dst.arrayOffset() + dstOffset, length);
    }

    public static synchronized int readBytes(Address src, ByteBuffer dst, int dstOffset, int length) {
        int lengthLeft = length;
        int localOffset = dstOffset;
        long localAddress = src.toLong();
        while (lengthLeft > 0) {
            final int toDo = lengthLeft > maxByteBufferSize ? maxByteBufferSize : lengthLeft;
            final int r = readBytes0(localAddress, dst, localOffset, toDo);
            if (r != toDo) {
                return -1;
            }
            lengthLeft -= toDo;
            localOffset += toDo;
            localAddress += toDo;
        }
        return length;
    }

    private static int writeBytes0(long dst, ByteBuffer src, int srcOffset, int length) {
        assert src.limit() - srcOffset >= length;
        if (src.isDirect()) {
            return nativeWriteBytes(dst, src, true, srcOffset, length);
        }
        assert src.array() != null;
        return nativeWriteBytes(dst, src.array(), false, src.arrayOffset() + srcOffset, length);

    }

    public static synchronized int writeBytes(ByteBuffer buffer, int offset, int length, Address address) {
        int lengthLeft = length;
        int localOffset = offset;
        long localAddress = address.toLong();
        while (lengthLeft > 0) {
            final int toDo = lengthLeft > maxByteBufferSize ? maxByteBufferSize : lengthLeft;
            final int r = writeBytes0(localAddress, buffer, localOffset, toDo);
            if (r != toDo) {
                return -1;
            }
            lengthLeft -= toDo;
            localOffset += toDo;
            localAddress += toDo;
        }
        return length;
    }


    public static synchronized void gatherThreads(AppendableSequence<TeleNativeThread> threads, int domainId, long threadSpecificsList) {
        nativeGatherThreads(teleDomain, threads, domainId, threadSpecificsList);
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

    private static native boolean nativeAttach(int domId);
    private static native long nativeGetBootHeapStart();
    private static native int nativeSetTransportDebugLevel(int level);
    private static native int nativeReadBytes(long src, Object dst, boolean isDirectByteBuffer, int dstOffset, int length);
    private static native int nativeWriteBytes(long dst, Object src, boolean isDirectByteBuffer, int srcOffset, int length);
    private static native int nativeMaxByteBufferSize();
    private static native boolean nativeGatherThreads(GuestVMXenTeleDomain teleDomain, AppendableSequence<TeleNativeThread> threads, int domainId, long threadSpecificsList);
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
