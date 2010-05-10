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

import java.io.*;
import java.net.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.*;

/**
 * An implementation of {@link GuestVMXenDBChannelProtocol} that communicates via TCP
 * to an agent running in domain 0, that communicates to the target Guest VM domain.
 * This has more latency but allows the Inspector to run in a non-privileged domain (domU).
 *
 * @author Mick Jordan
 *
 */

public class GuestVMXenDBTCPNativeChannelProtocol extends GuestVMXenDBChannelProtocolAdaptor {
    static final int DEFAULT_PORT = 9125;
    private final int port;
    private final String host;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    GuestVMXenDBTCPNativeChannelProtocol(String hostAndPort) {
        final int sep = hostAndPort.indexOf(',');
        if (sep > 0) {
            port = Integer.parseInt(hostAndPort.substring(sep + 1));
            host = hostAndPort.substring(0, sep);
        } else {
            port = DEFAULT_PORT;
            host = hostAndPort;
        }
    }

    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        unimplemented("activateWatchpoint");
        return false;
    }

    @Override
    public boolean attach(int domId) {
        Trace.line(1, "connecting to agent on " + host + ":" + port);
        try {
            socket = new Socket(host, port);
            Trace.line(1, "connected");
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("attach");
            out.writeInt(domId);
            boolean result = in.readBoolean();
            return result;
        } catch (Exception ex) {
            Trace.line(1, ex);
            return false;
        }
    }

    @Override
    public boolean deactivateWatchpoint(long start, long size) {
        unimplemented("deactivateWatchpoint");
        return false;
    }

    @Override
    public boolean gatherThreads(GuestVMXenTeleDomain teleDomain, AppendableSequence<TeleNativeThread> threads, long threadLocalsList, long primordialThreadLocals) {
        unimplemented("gatherThreads");
        return false;
    }

    @Override
    public long getBootHeapStart() {
        try {
            out.writeUTF("getBootHeapStart");
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
            return in.readInt();
        } catch (IOException ex) {
            ProgramError.unexpected(ex);
            return 0;
        }
    }

    @Override
    public int readByte(long address) {
        unimplemented("readByte");
        return 0;
    }

    @Override
    public int readBytes(long src, Object dst, boolean isDirectByteBuffer, int dstOffset, int length) {
        unimplemented("readBytes");
        return 0;
    }

    @Override
    public long readInt(long address) {
        unimplemented("readInt");
        return 0;
    }

    @Override
    public boolean readRegisters(int threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        unimplemented("readRegisters");
        return false;
    }

    @Override
    public int readShort(long address) {
        unimplemented("readShort");
        return 0;
    }

    @Override
    public int readWatchpointAccessCode() {
        unimplemented("readWatchpointAccessCode");
        return 0;
    }

    @Override
    public long readWatchpointAddress() {
        unimplemented("readWatchpointAddress");
        return 0;
    }

    @Override
    public int resume() {
        unimplemented("resume");
        return 0;
    }

    @Override
    public int setInstructionPointer(int threadId, long ip) {
        unimplemented("setInstructionPointer");
        return 0;
    }

    @Override
    public int setTransportDebugLevel(int level) {
        unimplemented("setTransportDebugLevel");
        return 0;
    }

    @Override
    public boolean singleStep(int threadId) {
        unimplemented("singleStep");
        return false;
    }

    @Override
    public boolean suspend(int threadId) {
        unimplemented("suspend");
        return false;
    }

    @Override
    public boolean suspendAll() {
        unimplemented("suspendAll");
        return false;
    }

    @Override
    public boolean writeByte(long address, byte value) {
        unimplemented("writeByte");
        return false;
    }

    @Override
    public int writeBytes(long dst, Object src, boolean isDirectByteBuffer, int srcOffset, int length) {
        unimplemented("writeBytes");
        return 0;
    }

    private static void unimplemented(String name) {
        ProgramError.unexpected("GuestVMXenDBTCPNativeChannel." + name + " unimplemented");
    }

}
