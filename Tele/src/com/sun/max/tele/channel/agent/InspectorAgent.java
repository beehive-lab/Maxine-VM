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
import java.util.*;
import com.sun.max.program.*;
import com.sun.max.tele.channel.TeleChannelProtocol;
import com.sun.max.tele.channel.tcp.TCPTeleChannelProtocol;
import com.sun.max.tele.debug.ProcessState;
import static com.sun.max.tele.channel.agent.RemoteInvocationProtocolAdaptor.*;

/**
 * An agent that handles the dom0 side of the Maxine Inspector debug communication channel.
 *
 * @author Mick Jordan
 *
 */
public class InspectorAgent {

    private static int port = TCPTeleChannelProtocol.DEFAULT_PORT;
    private static Protocol impl = Protocol.VE_DB;
    private static Map<String, Protocol> protocolMap = new HashMap<String, Protocol>();
    private static int dbtLevel = 0;
    private static boolean oneShot = false;

    enum Protocol {
        LINUX("linux", "com.sun.max.tele.channel.agent.linux", "LinuxNative"),
        VE_DB("ve_db", "com.sun.max.tele.channel.agent.guestvm.db", "GuestVMDB"),
        VE_XG("ve_xg", "com.sun.max.tele.channel.agent.guestvm.xg", "GuestVMXG");

        String key;
        String packageName;
        String classPrefix;

        Protocol(String key, String packageName, String classPrefix) {
            this.key = key;
            this.packageName = packageName;
            this.classPrefix = classPrefix;
        }
    }
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
            } else if (arg.equals("-impl")) {
                impl = getProtocol(args[++i]);
            } else if (arg.equals("-trace")) {
                traceLevel = Integer.parseInt(args[++i]);
            } else if (arg.equals("-dbtlevel")) {
                dbtLevel = Integer.parseInt(args[++i]);
            } else  if (arg.equals("-xg")) {
                impl = Protocol.VE_XG;
            } else if (arg.equals("-qc")) {
            	oneShot = true;
            }
        }
        // Checkstyle: resume modified control variable check
        if (traceLevel > 0) {
            Trace.on(traceLevel);
        }
        System.loadLibrary("tele");
        if (impl == Protocol.VE_DB) {
            System.loadLibrary("guk_db");
        }
        listen();
    }

    private static Protocol getProtocol(String key) {
        for (Protocol protocol : Protocol.values()) {
            if (protocol.key.equals(key)) {
                return protocol;
            }
        }
        System.err.println("unknown TeleChannelProtocol key: " + key);
        System.exit(1);
        return null;
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
            final String protocolClassName = impl.packageName + ".Agent" + impl.classPrefix + "TeleChannelProtocol";
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
                	System.out.println("client closed connection, terminating");
                	terminated = true;
                } catch (Exception ex) {
                    System.err.println(ex);
                    ex.printStackTrace();
                    System.err.println("terminating connection");
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
