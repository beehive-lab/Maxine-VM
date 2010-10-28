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
package com.sun.max.tele.channel.agent;

import java.io.*;
import java.net.*;

import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.channel.TeleChannelProtocol;
import com.sun.max.tele.channel.tcp.TCPTeleChannelProtocol;
import com.sun.max.tele.debug.ProcessState;
import com.sun.max.vm.hosted.*;

import static com.sun.max.tele.channel.agent.RemoteInvocationProtocolAdaptor.*;

/**
 * An agent that handles the target side of the Maxine Inspector {@link TeleChannelProtocol} communication channel.
 *
 * @author Mick Jordan
 *
 */
public class InspectorAgent {

    private static int port = TCPTeleChannelProtocol.DEFAULT_PORT;
    private static int pdbLevel = 0;
    private static boolean quitOnClose = false;
    private static OS os;
    private static String osSub = "";
    private static final OptionSet options = new OptionSet(true);
    private static final Option<Integer> portOption = options.newIntegerOption("port", TCPTeleChannelProtocol.DEFAULT_PORT,
                    "Port used for communication between Inspector and Agent");
    private static final Option<String> osOption = options.newStringOption("os", null,
                    "Operating system hosting the target VM");
    private static final Option<String> targetSubOption = options.newStringOption("os.sub", "",
                    "OS-specific channel protocol string");
    private static final Option<Boolean> quitOnCloseOption = options.newBooleanOption("xc", false,
                    "Exit when connection closed by remote Inspector");
    private static final Option<Integer> pdbLevelOption = options.newIntegerOption("tdblevel", 0,
                    "set protocol debug level");

    /**
     * @param args
     */
    public static void main(String[] args) {
        Trace.addTo(options);
        // parse the arguments
        options.parseArguments(args).getArguments();
        port = portOption.getValue();
        quitOnClose = quitOnCloseOption.getValue();
        osSub = targetSubOption.getValue();
        pdbLevel = pdbLevelOption.getValue();
        if (osOption.getValue() != null) {
            System.setProperty(Platform.OS_PROPERTY, osOption.getValue());
        }

        os = OS.fromName(System.getProperty(Platform.OS_PROPERTY, OS.current().name()));

        Prototype.loadLibrary(TeleVM.TELE_LIBRARY_NAME);
        listen();
    }

    public static void listen() {
        try {
            final ServerSocket server = new ServerSocket(port);
            for (;;) {
                try {
                    Trace.line(1, "waiting for connection");
                    final Socket sock = server.accept();
                    Trace.line(1, "connection accepted on " + sock.getLocalPort() + " from " + sock.getInetAddress());
                    final Handler handler = new Handler(sock);
                    handler.start();
                    // no concurrent connections, underlying native support cannot handle that at the moment
                    handler.join();
                    if (quitOnClose) {
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class Handler extends Thread {
        private DataInputStream in;
        private DataOutputStream out;
        private RemoteInvocationProtocolAdaptor protocol;

        Handler(Socket socket) throws Exception {
            try {
                in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            } catch (IOException ex) {
                close();
                throw ex;
            }
            final String protocolClassName = "com.sun.max.tele.channel.agent." + os.asPackageName() + ".Agent" + os.className + osSub + "NativeTeleChannelProtocol";
            protocol = (RemoteInvocationProtocolAdaptor) Class.forName(protocolClassName).newInstance();
            if (pdbLevel > 0) {
                ((TeleChannelProtocol) protocol).setTransportDebugLevel(pdbLevel);
            }
        }

        @Override
        public void run() {
            boolean terminated = false;
            while (!terminated) {
                try {
                    final String command = in.readUTF();
                    MethodInfo m = protocol.methodMap.get(command);
                    if (m == null) {
                        Trace.line(1, "command " + command + " not available");
                    } else {
                        final Object[] args = protocol.readArgs(in, m);
                        final Object result = m.method.invoke(protocol, args);
                        protocol.writeResult(out, m, result, args);
                        if (command.equals("waitUntilStopped") && ((Integer) result).intValue() == ProcessState.TERMINATED.ordinal()) {
                            terminated = true;
                        }
                    }
                } catch (EOFException ex) {
                    Trace.line(1, "client closed connection, terminating");
                    terminated = true;
                } catch (Exception ex) {
                    System.err.println(ex);
                    ex.printStackTrace();
                    Trace.line(1, "terminating connection");
                    terminated = true;
                }
            }
            close();
        }

        private void close() {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ex) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ex) {
                }
            }
        }
    }

}
