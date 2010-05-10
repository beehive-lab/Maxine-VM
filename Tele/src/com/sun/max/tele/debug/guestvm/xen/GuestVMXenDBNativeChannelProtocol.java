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

/**
 * An implementation of {@link GuestVMXenDBChannelProtocol} that links directly to native code
 * that communicates via the Xen ring mechanism to the target Guest VM domain.
 * This requires that the Inspector run with root privileges in (a 64-bit) dom0.
 *
 * The class is also used by {@link GuestVMXenDBNativeChannelAgent} when the
 * Inspector runs in a domU and connects via TCP.
 *
 * @author Mick Jordan
 *
 */

public class GuestVMXenDBNativeChannelProtocol extends GuestVMXenDBChannelProtocolAdaptor {

    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        return nativeActivateWatchpoint(start, size, after, read, write, exec);
    }

    @Override
    public boolean attach(int domId) {
        return nativeAttach(domId);
    }

    @Override
    public boolean deactivateWatchpoint(long start, long size) {
        return nativeDeactivateWatchpoint(start, size);
    }

    @Override
    public boolean gatherThreads(GuestVMXenTeleDomain teleDomain, AppendableSequence<TeleNativeThread> threads, long threadLocalsList, long primordialThreadLocals) {
        return nativeGatherThreads(teleDomain, threads, threadLocalsList, primordialThreadLocals);
    }

    @Override
    public long getBootHeapStart() {
        return nativeGetBootHeapStart();
    }

    @Override
    public int maxByteBufferSize() {
        return nativeMaxByteBufferSize();
    }

    @Override
    public int readBytes(long src, Object dst, boolean isDirectByteBuffer, int dstOffset, int length) {
        return nativeReadBytes(src, dst, isDirectByteBuffer, dstOffset, length);
    }

    @Override
    public boolean readRegisters(int threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        return nativeReadRegisters(threadId, integerRegisters, integerRegistersSize, floatingPointRegisters, floatingPointRegistersSize, stateRegisters, stateRegistersSize);
    }

    @Override
    public int readWatchpointAccessCode() {
        return nativeReadWatchpointAccessCode();
    }

    @Override
    public long readWatchpointAddress() {
        return nativeReadWatchpointAddress();
    }

    @Override
    public int resume() {
        return nativeResume();
    }

    @Override
    public int setInstructionPointer(int threadId, long ip) {
        return nativeSetInstructionPointer(threadId, ip);
    }

    @Override
    public int setTransportDebugLevel(int level) {
        return nativeSetTransportDebugLevel(level);
    }

    @Override
    public boolean singleStep(int threadId) {
        return nativeSingleStep(threadId);
    }

    @Override
    public boolean suspend(int threadId) {
        return nativeSuspend(threadId);
    }

    @Override
    public boolean suspendAll() {
        return nativeSuspendAll();
    }

    @Override
    public int writeBytes(long dst, Object src, boolean isDirectByteBuffer, int srcOffset, int length) {
        return nativeWriteBytes(dst, src, isDirectByteBuffer, srcOffset, length);
    }

    private static native boolean nativeAttach(int domId);
    private static native long nativeGetBootHeapStart();
    private static native int nativeSetTransportDebugLevel(int level);
    private static native int nativeReadBytes(long src, Object dst, boolean isDirectByteBuffer, int dstOffset, int length);
    private static native int nativeWriteBytes(long dst, Object src, boolean isDirectByteBuffer, int srcOffset, int length);
    private static native int nativeMaxByteBufferSize();
    private static native boolean nativeGatherThreads(GuestVMXenTeleDomain teleDomain, AppendableSequence<TeleNativeThread> threads, long threadLocalsList, long primordialThreadLocals);
    private static native int nativeResume();
    private static native int nativeSetInstructionPointer(int threadId, long ip);
    private static native boolean nativeSingleStep(int threadId);
    private static native boolean nativeSuspendAll();
    private static native boolean nativeSuspend(int threadId);
    private static native boolean nativeActivateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec);
    private static native boolean nativeDeactivateWatchpoint(long start, long size);
    private static native long nativeReadWatchpointAddress();
    private static native int nativeReadWatchpointAccessCode();

    private static native boolean nativeReadRegisters(int threadId,
                    byte[] integerRegisters, int integerRegistersSize,
                    byte[] floatingPointRegisters, int floatingPointRegistersSize,
                    byte[] stateRegisters, int stateRegistersSize);


}
