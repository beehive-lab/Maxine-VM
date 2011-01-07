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
package com.sun.max.tele.channel.tcp;

import java.io.*;
import java.net.*;
import com.sun.max.program.*;
import com.sun.max.tele.channel.iostream.*;

/**
 * An implementation of {@link TeleChannelDataIOProtocol} that communicates via TCP to an agent on the machine running
 * the target VM. It uses the reflective, remote, invocation mechanism of {@link TeleChannelDataIOProtocolImpl}
 *
 * @author Mick Jordan
 *
 */

public class TCPTeleChannelProtocol extends TeleChannelDataIOProtocolAdaptor {
    public static final int DEFAULT_PORT = 9125;
    private final int port;
    private final String host;
    private Socket socket;

    public TCPTeleChannelProtocol(String host, int port) {
        this.host = host;
        this.port = port;
        Trace.line(1, "connecting to agent on " + host + ":" + port);
        try {
            socket = new Socket(host, port);
            Trace.line(1, "connected");
            setStreams(new BufferedInputStream(socket.getInputStream()), new BufferedOutputStream(socket.getOutputStream()));
        } catch (Exception ex) {
            Trace.line(1, ex);
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
