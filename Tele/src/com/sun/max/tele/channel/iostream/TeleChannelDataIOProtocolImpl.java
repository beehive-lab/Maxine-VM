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
package com.sun.max.tele.channel.iostream;

import java.io.*;
import com.sun.max.program.*;
import com.sun.max.tele.channel.TeleChannelDataIOProtocol;

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
    public long create(String pathName, String[] commandLineArguments, long extra1) {
        unimplemented("create");
        /*
        try {
            out.writeUTF("create");
            out.writeUTF(pathName);
        } catch (Exception ex) {
            Trace.line(1, ex);
            return -1;
        }
        */
        return -1;
    }

    @Override
    public boolean kill() {
        try {
            out.writeUTF("kill");
            out.flush();
            return in.readBoolean();
        } catch (IOException ex) {
            ProgramError.unexpected(ex);
            return false;
        }
    }

    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        unimplemented("activateWatchpoint");
        return false;
    }

    @Override
    public boolean attach(int id, int threadLocalsAreaSize, long extra1) {
        try {
            out.writeUTF("attach");
            out.writeInt(id);
            out.writeInt(threadLocalsAreaSize);
            out.writeLong(extra1);
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
            ProgramError.unexpected(ex);
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
            ProgramError.unexpected(ex);
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
            ProgramError.unexpected(ex);
            return 0;
        }
    }

    @Override
    public int readBytes(long src, byte[] dst, int dstOffset, int length) {
        try {
            out.writeUTF("readBytes");
            out.writeLong(src);
            outArray(ArrayMode.OUT, dst);
            out.writeInt(dstOffset);
            out.writeInt(length);
            out.flush();
            inArray(dst, dstOffset, length);
            final int result = in.readInt();
            return result;
        } catch (IOException ex) {
            ProgramError.unexpected(ex);
            return 0;
        }
    }

    @Override
    public boolean readRegisters(long threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        try {
            out.writeUTF("readRegisters");
            out.writeLong(threadId);
            outArray(ArrayMode.OUT, integerRegisters);
            out.writeInt(integerRegistersSize);
            outArray(ArrayMode.OUT, floatingPointRegisters);
            out.writeInt(floatingPointRegistersSize);
            outArray(ArrayMode.OUT, stateRegisters);
            out.writeInt(stateRegistersSize);
            out.flush();
            inArray(integerRegisters, 0, integerRegistersSize);
            inArray(floatingPointRegisters, 0, floatingPointRegistersSize);
            inArray(stateRegisters, 0, stateRegistersSize);
            return in.readBoolean();
        } catch (IOException ex) {
            ProgramError.unexpected(ex);
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
            ProgramError.unexpected(ex);
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
            ProgramError.unexpected(ex);
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
            ProgramError.unexpected(ex);
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
            ProgramError.unexpected(ex);
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
            ProgramError.unexpected(ex);
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
            ProgramError.unexpected(ex);
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
            ProgramError.unexpected(ex);
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
            ProgramError.unexpected(ex);
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
            ProgramError.unexpected(ex);
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
            ProgramError.unexpected(ex);
            return false;
        }
    }

    @Override
    public int writeBytes(long dst, byte[] src, int srcOffset, int length) {
        try {
            out.writeUTF("writeBytes");
            out.writeLong(dst);
            outArray(ArrayMode.IN, src);
            out.writeInt(srcOffset);
            out.writeInt(length);
            out.flush();
            return in.readInt();
        } catch (IOException ex) {
            ProgramError.unexpected(ex);
            return 0;
        }
    }

    @Override
    public int gatherThreads(long threadLocalsList, long primordialThreadLocals) {
        try {
            out.writeUTF("gatherThreads");
            out.writeLong(threadLocalsList);
            out.writeLong(primordialThreadLocals);
            out.flush();
            return in.readInt();
        } catch (IOException ex) {
            ProgramError.unexpected(ex);
            return 0;
        }
    }

    @Override
    public int readThreads(int size, byte[] gatherThreadData) {
        try {
            out.writeUTF("readThreads");
            out.writeInt(size);
            outArray(ArrayMode.OUT, gatherThreadData);
            out.flush();
            inArray(gatherThreadData, 0, size);
            return in.readInt();
        } catch (IOException ex) {
            ProgramError.unexpected(ex);
            return 0;
        }
    }

    private void outArray(ArrayMode mode, byte[] b) throws IOException {
        out.writeInt(mode.ordinal());
        // write b.length first so callee can allocate
        out.writeInt(b.length);
        if (mode != ArrayMode.OUT) {
            out.write(b);
        }
    }

    private void inArray(byte[] dst, int dstOffset, int length) throws IOException {
        byte[] result = new byte[dst.length];
        in.readFully(result);
        System.arraycopy(result, dstOffset, dst, dstOffset, length);
    }


    private void unimplemented(String name) {
        ProgramError.unexpected(getClass().getName() + "." + name + " unimplemented");
    }

}
