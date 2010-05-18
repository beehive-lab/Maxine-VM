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
package com.sun.max.tele.debug.guestvm.xen.dbchannel.tcp;

import java.io.*;
import java.net.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.guestvm.xen.dbchannel.*;
import com.sun.max.tele.debug.guestvm.xen.dbchannel.dataio.*;

/**
 * An implementation of {@link SimpleProtocol} that communicates via TCP
 * to an agent running in domain 0, that communicates to the target Guest VM domain.
 * This has more latency but allows the Inspector to run in a non-privileged domain (domU).
 * It uses the reflective, remote, invocation mechanism of {@link DataIOProtocol}
 *
 * @author Mick Jordan
 *
 */

public class TCPProtocol extends CompleteProtocolAdaptor {
    public static final int DEFAULT_PORT = 9125;
    private final int port;
    private final String host;
    private Socket socket;

    public TCPProtocol(String hostAndPort) {
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
    public boolean attach(int domId, int threadLocalsAreaSize) {
        Trace.line(1, "connecting to agent on " + host + ":" + port);
        try {
            socket = new Socket(host, port);
            Trace.line(1, "connected");
            setStreams(new BufferedInputStream(socket.getInputStream()), new BufferedOutputStream(socket.getOutputStream()));
            return super.attach(domId, threadLocalsAreaSize);
        } catch (Exception ex) {
            Trace.line(1, ex);
            return false;
        }
    }

    @Override
    public boolean detach() {
        try {
            return super.detach();
        } catch (Exception ex) {
            Trace.line(1, ex);
            return false;
        } finally {
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException ex) {
            }
        }
    }

}
