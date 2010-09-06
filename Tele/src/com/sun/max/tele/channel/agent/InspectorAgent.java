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
import com.sun.max.tele.channel.TeleChannelProtocol;
import com.sun.max.tele.channel.tcp.TCPTeleChannelProtocol;
import com.sun.max.tele.debug.ProcessState;
import com.sun.max.vm.hosted.*;

import static com.sun.max.tele.channel.agent.RemoteInvocationProtocolAdaptor.*;

/**
 * An agent that handles the target side of the Maxine Inspector debug communication channel.
 *
 * @author Mick Jordan
 *
 */
public class InspectorAgent {

    private static int port = TCPTeleChannelProtocol.DEFAULT_PORT;
    private static int dbtLevel = 0;
    private static boolean oneShot = false;
    private static OperatingSystem target;
    private static String targetSpecific = "";

    /**
     * @param args
     */
    public static void main(String[] args) {
        int traceLevel = 0;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("-port")) {
                port = Integer.parseInt(args[++i]);
            } else if (arg.equals("-host.os")) {
                System.setProperty(Platform.OPERATING_SYSTEM_PROPERTY, args[++i]);
            } else if (arg.equals("-trace")) {
                traceLevel = Integer.parseInt(args[++i]);
            } else if (arg.equals("-dbtlevel")) {
                dbtLevel = Integer.parseInt(args[++i]);
            } else  if (arg.equals("-host.os.sub")) {
                targetSpecific = args[++i].toUpperCase();
            } else if (arg.equals("-qd")) {
            	oneShot = true;
            }
        }
        // Checkstyle: resume modified control variable check
        if (traceLevel > 0) {
            Trace.on(traceLevel);
        }

        target = OperatingSystem.fromName(System.getProperty(Platform.OPERATING_SYSTEM_PROPERTY, OperatingSystem.current().name()));

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
                    if (oneShot) {
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
            final String protocolClassName = "com.sun.max.tele.channel.agent." + target.asPackageName() + ".Agent" + target.asClassName() + targetSpecific + "NativeTeleChannelProtocol";
            protocol = (RemoteInvocationProtocolAdaptor) Class.forName(protocolClassName).newInstance();
            if (dbtLevel > 0) {
                ((TeleChannelProtocol) protocol).setTransportDebugLevel(dbtLevel);
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
