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
package com.sun.max.tele.channel.natives;


/**
 * Defines a set of native methods that must be implemented for any Maxine VM implementation to support the Inspector.
 * These derive originally from two classes in the Inspector, {@link TeleProcess} and {@link TeleNativeThread}, although
 * in the current architecture they are invoked by {@link TeleNativeChannelProtocol}.
 *
 * These methods are not static to allow the native code to distinguish between multiple implementations that may co-exist.
 *
 * The exact interpretation of {@code processHandle} and {@code tid} in the method signatures is operating system independent,
 * but generically a  {@code processHandle} represents the process hosting the target VM and the {@code tid} denotes a thread.
 * For generality both of these are declared as {@code long}.
 *
 * @author Mick Jordan
 *
 */
public class TeleChannelNatives {
    // from TeleProcess
    public native void teleInitialize(int tlaSize);
    public native long createChild(long argv, int vmAgentPort);
    private native boolean attach(long processHandle);
    private native boolean detach(long processHandle);
    public native void kill(long processHandle);
    public native boolean suspend(long processHandle);
    public native boolean resume(long processHandle);
    public native int waitUntilStopped(long processHandle);
    public native void gatherThreads(long processHandle, Object teleProcess, Object threadList, long tlaList, long primordialETLA);
    public native int readBytes(long processHandle, long src, Object dst, boolean isDirectByteBuffer, int offset, int length);
    public native int writeBytes(long processHandle, long dst, Object src, boolean isDirectByteBuffer, int offset, int length);
    public native boolean activateWatchpoint(long processHandle, long start, long size, boolean after, boolean read, boolean write, boolean exec);
    public native boolean deactivateWatchpoint(long processHandle, long start, long size);
    public native long readWatchpointAddress(long processHandle);
    public native int readWatchpointAccessCode(long processHandle);

    // from TeleNativeThread
    public native boolean setInstructionPointer(long processHandle, long tid, long address);
    public native boolean readRegisters(long processHandle, long tid,
                    byte[] integerRegisters, int integerRegistersSize,
                    byte[] floatingPointRegisters, int floatingPointRegistersSize,
                    byte[] stateRegisters, int stateRegistersSize);
    public native boolean singleStep(long processHandle, long tid);
    public native boolean resume(long processHandle, long tid);
    public native boolean suspend(long processHandle, long tid);
}
