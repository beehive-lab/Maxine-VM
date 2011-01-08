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
