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
package com.sun.max.tele.channel.iostream;

import java.io.*;

import com.sun.max.program.*;
import com.sun.max.tele.channel.*;
import com.sun.max.tele.channel.agent.*;
import com.sun.max.tele.util.*;

/**
 * An implementation of {@link TeleChannelDataIOProtocol} that communicates using {@link DataInputStream} and {@link DataOutputStream) and is
 * essentially a custom remote method invocation system.
 *
 * Byte arrays are supported and, obviously, require special treatment. Arrays are tagged as {@link ArrayMode.#IN in}, {@link ArrayMode#OUT out}
 * or {@link ArrayMode#INOUT inout}. That is, when an array is passed as parameter, the ordinal value of the {@link ArrayMode} is written first,
 * followed by the length of the array, followed by the bytes of the array, except in the case of {@link ArrayMode#OUT}, where no bytes are sent.
 * The contents of the array are "returned" as an auxiliary result if the mode is {@link ArrayMode#OUT} or {@link ArrayMode#INOUT}; the bytes
 * precede the method result value.
 *
 * For this to work, the {@link ArrayMode} of a parameter must be registered with {@link RemoteInvocationProtocolAdaptor} by the target end of the communication..
 *
* @author Mick Jordan
 *
 */

public class TeleChannelDataIOProtocolImpl implements TeleChannelDataIOProtocol {
    public enum ArrayMode {
        IN,
        OUT,
        INOUT
    };

    protected DataInputStream in;
    protected DataOutputStream out;

    protected TeleChannelDataIOProtocolImpl() {
    }

    protected void setStreams(InputStream in, OutputStream out) {
        this.in = new DataInputStream(in);
        this.out = new DataOutputStream(out);
    }

    @Override
    public boolean initialize(int tlaSize, boolean bigEndian) {
        try {
            out.writeUTF("initialize");
            out.writeInt(tlaSize);
            out.writeBoolean(bigEndian);
            out.flush();
            return in.readBoolean();
        } catch (Exception ex) {
            Trace.line(1, ex);
            return false;
        }
    }

    @Override
    public long create(String pathName, String[] commandLineArguments) {
        try {
            out.writeUTF("create");
            out.writeUTF(pathName);
            outStringArray(ArrayMode.IN, commandLineArguments);
            out.flush();
            return in.readLong();
        } catch (Exception ex) {
            Trace.line(1, ex);
            return -1;
        }
    }

    @Override
    public boolean kill() {
        try {
            out.writeUTF("kill");
            out.flush();
            return in.readBoolean();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return false;
        }
    }

    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        unimplemented("activateWatchpoint");
        return false;
    }

    @Override
    public boolean attach(int id) {
        try {
            out.writeUTF("attach");
            out.writeInt(id);
            out.flush();
            boolean result = in.readBoolean();
            return result;
        } catch (Exception ex) {
            Trace.line(1, ex);
            return false;
        }
    }

    @Override
    public boolean detach() {
        try {
            out.writeUTF("detach");
            out.flush();
            return in.readBoolean();
        } catch (Exception ex) {
            Trace.line(1, ex);
            return false;
        }
    }

    @Override
    public int waitUntilStoppedAsInt() {
        try {
            out.writeUTF("waitUntilStoppedAsInt");
            out.flush();
            return in.readInt();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return 0;
        }
    }

    @Override
    public long getBootHeapStart() {
        try {
            out.writeUTF("getBootHeapStart");
            out.flush();
            return in.readLong();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return 0;
        }
    }

    @Override
    public int maxByteBufferSize() {
        try {
            out.writeUTF("maxByteBufferSize");
            out.flush();
            return in.readInt();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return 0;
        }
    }

    @Override
    public int readBytes(long src, byte[] dst, int dstOffset, int length) {
        try {
            out.writeUTF("readBytes");
            out.writeLong(src);
            outByteArray(ArrayMode.OUT, dst);
            out.writeInt(dstOffset);
            out.writeInt(length);
            out.flush();
            inByteArray(dst, dstOffset, length);
            final int result = in.readInt();
            return result;
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return 0;
        }
    }

    @Override
    public boolean readRegisters(long threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        try {
            out.writeUTF("readRegisters");
            out.writeLong(threadId);
            outByteArray(ArrayMode.OUT, integerRegisters);
            out.writeInt(integerRegistersSize);
            outByteArray(ArrayMode.OUT, floatingPointRegisters);
            out.writeInt(floatingPointRegistersSize);
            outByteArray(ArrayMode.OUT, stateRegisters);
            out.writeInt(stateRegistersSize);
            out.flush();
            inByteArray(integerRegisters, 0, integerRegistersSize);
            inByteArray(floatingPointRegisters, 0, floatingPointRegistersSize);
            inByteArray(stateRegisters, 0, stateRegistersSize);
            return in.readBoolean();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return false;
        }
    }

    @Override
    public int readWatchpointAccessCode() {
        try {
            out.writeUTF("readWatchpointAccessCode");
            out.flush();
            return in.readInt();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return 0;
        }
    }

    @Override
    public long readWatchpointAddress() {
        try {
            out.writeUTF("readWatchpointAddress");
            out.flush();
            return in.readLong();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return 0;
        }
    }

    @Override
    public boolean deactivateWatchpoint(long start, long size) {
        try {
            out.writeUTF("deactivateWatchpoint");
            out.writeLong(start);
            out.writeLong(size);
            out.flush();
            return in.readBoolean();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return false;
        }
    }

    @Override
    public boolean resume(long threadId) {
        try {
            out.writeUTF("resume");
            out.writeLong(threadId);
            out.flush();
            return in.readBoolean();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return false;
        }
    }

    @Override
    public boolean setInstructionPointer(long threadId, long ip) {
        try {
            out.writeUTF("setInstructionPointer");
            out.writeLong(threadId);
            out.writeLong(ip);
            out.flush();
            return in.readBoolean();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return false;
        }
    }

    @Override
    public int setTransportDebugLevel(int level) {
        try {
            out.writeUTF("setTransportDebugLevel");
            out.flush();
            return in.readInt();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return 0;
        }
    }

    @Override
    public boolean singleStep(long threadId) {
        try {
            out.writeUTF("singleStep");
            out.writeLong(threadId);
            out.flush();
            return in.readBoolean();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return false;
        }
    }

    @Override
    public boolean suspend(long threadId) {
        try {
            out.writeUTF("suspend");
            out.writeLong(threadId);
            out.flush();
            return in.readBoolean();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return false;
        }
    }

    @Override
    public boolean suspendAll() {
        try {
            out.writeUTF("suspendAll");
            out.flush();
            return in.readBoolean();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return false;
        }
    }

    @Override
    public boolean resumeAll() {
        try {
            out.writeUTF("resumeAll");
            out.flush();
            return in.readBoolean();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return false;
        }
    }

    @Override
    public int writeBytes(long dst, byte[] src, int srcOffset, int length) {
        try {
            out.writeUTF("writeBytes");
            out.writeLong(dst);
            outByteArray(ArrayMode.IN, src);
            out.writeInt(srcOffset);
            out.writeInt(length);
            out.flush();
            return in.readInt();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return 0;
        }
    }

    @Override
    public int gatherThreads(long tlaList, long primordialETLA) {
        try {
            out.writeUTF("gatherThreads");
            out.writeLong(tlaList);
            out.writeLong(primordialETLA);
            out.flush();
            return in.readInt();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return 0;
        }
    }

    @Override
    public int readThreads(int size, byte[] gatherThreadData) {
        try {
            out.writeUTF("readThreads");
            out.writeInt(size);
            outByteArray(ArrayMode.OUT, gatherThreadData);
            out.flush();
            inByteArray(gatherThreadData, 0, size);
            return in.readInt();
        } catch (IOException ex) {
            TeleError.unexpected(ex);
            return 0;
        }
    }

    private void outByteArray(ArrayMode mode, byte[] array) throws IOException {
        out.writeInt(mode.ordinal());
        // write b.length first so callee can allocate
        out.writeInt(array.length);
        if (mode != ArrayMode.OUT) {
            out.write(array);
        }
    }

    private void inByteArray(byte[] dst, int dstOffset, int length) throws IOException {
        byte[] result = new byte[dst.length];
        in.readFully(result);
        System.arraycopy(result, dstOffset, dst, dstOffset, length);
    }


    private void outStringArray(ArrayMode mode, String[] array) throws IOException {
        out.writeInt(mode.ordinal());
        // write b.length first so callee can allocate
        out.writeInt(array.length);
        if (mode != ArrayMode.OUT) {
            for (String s : array) {
                out.writeUTF(s);
            }
        }
    }

    private void unimplemented(String name) {
        TeleError.unimplemented(getClass().getName() + "." + name);
    }

}
