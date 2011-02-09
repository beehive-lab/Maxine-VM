/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.debug.dump;

import static com.sun.max.tele.thread.NativeThreadLocal.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.nio.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.channel.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.thread.*;
import com.sun.max.tele.util.*;
import com.sun.max.vm.thread.*;

/**
 * Access to Maxine threads in a core dump or other similarly uncooperative environment,
 * where we have to troll memory to get the information.
 *
 * @author Mick Jordan
 *
 */
public abstract class ThreadAccess {
    protected TeleChannelDataIOProtocol protocol;
    private static final int MAXINE_THREAD_ID = 40;
    protected int tlaSize;
    protected List<ThreadInfo> currentThreadList;

    /**
     * Abstracts the critical information about a thread that is needed by the Inspector gatherThreads method.
     */
    public interface ThreadInfo {
        int getId();
        long getStackPointer();
        long getInstructionPointer();
        int getThreadState();
    }

    /**
     * An adaptor that provides register storage assuming canonical form (for the x64 architecture).
     * TODO Remove the explicit dependency on x64 architecture regarding register data size and location of IP/SP.
     */
    public abstract class ThreadInfoRegisterAdaptor implements ThreadInfo {
        public byte[] integerRegisters = new byte[128];
        public byte[] floatingPointRegisters = new byte[128];
        public byte[] stateRegisters = new byte[16];

        public long getStackPointer() {
            return ByteBuffer.wrap(integerRegisters).order(ByteOrder.LITTLE_ENDIAN).getLong(32);
        }

        public long getInstructionPointer() {
            return ByteBuffer.wrap(stateRegisters).order(ByteOrder.LITTLE_ENDIAN).getLong(0);
        }
    }

    /**
     * OS-specific gathering of all the candidate threads in {@link #currentThreadList}.
     */
    protected abstract void gatherOSThreads(List<ThreadInfo> threadList);

    protected ThreadAccess(TeleChannelDataIOProtocol protocol, int tlaSize) {
        this.protocol = protocol;
        this.tlaSize = tlaSize;
    }

    @SuppressWarnings("unchecked")
    public boolean gatherThreads(Object teleProcessObject, Object threadList, long tlaList) {
        final ByteBuffer tla = ByteBuffer.allocate(tlaSize).order(ByteOrder.LITTLE_ENDIAN);
        final ByteBuffer nativeThreadLocals = ByteBuffer.allocate(NativeThreadLocal.SIZE).order(ByteOrder.LITTLE_ENDIAN);

        currentThreadList = new ArrayList<ThreadInfo>();
        gatherOSThreads(currentThreadList);
        for (ThreadInfo threadInfo : currentThreadList) {
            final boolean found = findThreadLocals(tlaList, threadInfo.getStackPointer(), tla, nativeThreadLocals);
            int id = threadInfo.getId();
            if (!found) {
                /*
                 * Make id negative to indicate no thread locals were available for the thread. This will be the case
                 * for a native thread or a Java thread that has not yet executed past the point in VmThread.run() where
                 * it is added to the active thread list.
                 */
                id = id < 0 ? id : -id;
                setInStruct(tla, VmThreadLocal.ID.offset, id);
            }
            try {
                TeleChannelDataIOProtocol.GatherThreadData t = new TeleChannelDataIOProtocol.GatherThreadData((int) getFromStruct(tla, ID.offset), threadInfo.getId(), getFromStruct(
                                nativeThreadLocals, HANDLE.offset), threadInfo.getThreadState(), threadInfo.getInstructionPointer(), getFromStruct(nativeThreadLocals, STACKBASE.offset),
                                getFromStruct(nativeThreadLocals, STACKSIZE.offset), getFromStruct(nativeThreadLocals, TLBLOCK.offset), getFromStruct(nativeThreadLocals, TLBLOCKSIZE.offset),
                                tlaSize);
                Trace.line(2, "calling jniGatherThread id=" + t.id + ", lh=" + t.localHandle + ", h=" + Long.toHexString(t.handle) + ", st=" + t.state + ", ip=" +
                                                Long.toHexString(t.instructionPointer) + ", sb=" + Long.toHexString(t.stackBase) + ", ss=" + Long.toHexString(t.stackSize) + ", tlb=" +
                                                Long.toHexString(t.tlb) + ", tlbs=" + t.tlbSize + ", tlas=" + t.tlaSize);
                TeleProcess teleProcess = (TeleProcess) teleProcessObject;
                teleProcess.jniGatherThread((List<TeleNativeThread>) threadList, t.id, t.localHandle, t.handle, t.state, t.instructionPointer, t.stackBase, t.stackSize, t.tlb, t.tlbSize, t.tlaSize);
            } catch (Exception ex) {
                TeleError.unexpected("invoke failure on jniGatherThread", ex);
            }
        }
        return true;
    }

    public ThreadInfo getThreadInfo(int id) {
        for (ThreadInfo threadInfo : currentThreadList) {
            if (threadInfo.getId() == id) {
                return threadInfo;
            }
        }
        throw new IllegalArgumentException("cannot find thread id " + id);
    }

    private static void zeroBuffer(ByteBuffer bb) {
        byte[] b = bb.array();
        for (int i = 0; i < b.length; i++) {
            b[i] = 0;
        }
    }

    static long getFromStruct(ByteBuffer bb, int offset) {
        return bb.getLong(offset);
    }

    static void setInStruct(ByteBuffer bb, int offset, long value) {
        bb.putLong(offset, value);
    }

    boolean isThreadLocalsForStackPointer(long stackPointer, long tl, ByteBuffer tlCopy, ByteBuffer ntlCopy) {
        int n = protocol.readBytes(tl, tlCopy.array(), 0, tlaSize);
        assert n == tlaSize;
        final long ntl = getFromStruct(tlCopy, NATIVE_THREAD_LOCALS.offset);
        n = protocol.readBytes(ntl, ntlCopy.array(), 0, NativeThreadLocal.SIZE);
        final long stackBase = ntlCopy.getLong(STACKBASE.offset);
        final long stackSize = ntlCopy.getLong(STACKSIZE.offset);
        return stackBase <= stackPointer && stackPointer < (stackBase + stackSize);
    }



    /**
     * Searches the thread locals list in the VM's address space for an entry 'tl' such that:
     *
     *   tl.stackBase <= stackPointer && stackPointer < (tl.stackBase + tl.stackSize)
     *
     * If such an entry is found, then its contents are copied from the VM to the structs pointed to by 'tlCopy' and 'ntlCopy'.
     *
     * @param tlaList the head of the thread locals list in the VM's address space
     * @param stackPointer the stack pointer to search with
     * @param tlCopy pointer to storage for a set of thread locals into which the found entry
     *        (if any) will be copied from the VM's address space
     * @param ntlCopy pointer to storage for a NativeThreadLocalsStruct into which the native thread locals of the found entry
     *        (if any) will be copied from the VM's address space
     * @return {@code true} if the entry was found, {@code false} otherwise
     */
    boolean findThreadLocals(long tlaList, long stackPointer, ByteBuffer tlCopy, ByteBuffer ntlCopy) {
        zeroBuffer(tlCopy);
        zeroBuffer(ntlCopy);
        if (tlaList != 0) {
            long tl = tlaList;
            while (tl != 0) {
                if (isThreadLocalsForStackPointer(stackPointer, tl, tlCopy, ntlCopy)) {
                    return true;
                }
                tl = getFromStruct(tlCopy, FORWARD_LINK.offset);
            }
        }
        return false;
    }
}
