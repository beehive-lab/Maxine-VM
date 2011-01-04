/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.jdwp.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import com.sun.max.jdwp.constants.Error;
import com.sun.max.jdwp.data.*;

/**
 * A JDWPServer object manages a command handler registry and can be started to listen for incoming JDWP connections.
 *
 * @author Thomas Wuerthinger
 */
public class JDWPServer {

    private static final Logger LOGGER = Logger.getLogger(JDWPServer.class.getName());
    private static final int TIMEOUT = 2000;

    private ServerSocket serverSocket;
    private boolean shutdown;

    /**
     * Shuts down the JDWP server. The server must be running.
     */
    public void shutdown() {
        assert serverSocket != null : "Not running!";
        assert !shutdown : "Already shutting down!";
        shutdown = true;
    }

    /**
     * Starts the thread that waits for incoming JDWP connections.
     *
     * @param serverSocket the server socket on which to listen for incoming connections
     * @throws IOException this exception is thrown, when the server socket could not be created
     */
    public void start(ServerSocket serverSocket) throws IOException {
        assert this.serverSocket == null : "Already started!";
        this.serverSocket = serverSocket;
        this.serverSocket.setSoTimeout(TIMEOUT);
        new Thread(waitForClientsThread).start();
    }

    /**
     * This command handler registry can be used to lookup or add JDWP command handlers.
     *
     * @return the command handler registry
     */
    public CommandHandlerRegistry commandHandlerRegistry() {
        return commandHandlerRegistry;
    }

    /**
     * Thread waiting for clients to connect to the JDWP server.
     */
    private Runnable waitForClientsThread = new Runnable() {
        public void run() {
            LOGGER.info("JDWPServer waiting for clients");
            try {
                while (!shutdown) {
                    try {
                        final Socket clientSocket = serverSocket.accept();
                        new ClientThread(clientSocket).start();
                    } catch (SocketTimeoutException e) {
                    }
                }
            } catch (IOException e) {
                LOGGER.severe("Exception occurred while waiting for clients: " + e.toString());
            } finally {
                LOGGER.info("JDWP server is shut down");
                serverSocket = null;
                shutdown = false;
            }
        }

    };

    /**
     * Registry that manages the set of command handlers.
     */
    private final CommandHandlerRegistry commandHandlerRegistry = new CommandHandlerRegistry() {

        private Map<Byte, Map<Byte, CommandHandler<? extends IncomingData, ? extends OutgoingData>>> commandHandlerCache = new HashMap<Byte, Map<Byte, CommandHandler<? extends IncomingData, ? extends OutgoingData>>>();

        public CommandHandler<? extends IncomingData, ? extends OutgoingData> findCommandHandler(byte commandSetId, byte commandId) {
            if (!commandHandlerCache.containsKey(commandSetId)) {
                return null;
            }
            final CommandHandler<? extends IncomingData, ? extends OutgoingData> result = commandHandlerCache.get(commandSetId).get(commandId);
            assert result == null || result.getCommandSetId() == commandSetId : "Command set ID must match.";
            assert result == null || result.getCommandId() == commandId : "Command ID must match.";
            return result;
        }

        public void addCommandHandler(CommandHandler<? extends IncomingData, ? extends OutgoingData> commandHandler) {
            if (!commandHandlerCache.containsKey(commandHandler.getCommandSetId())) {
                commandHandlerCache.put(commandHandler.getCommandSetId(), new HashMap<Byte, CommandHandler<? extends IncomingData, ? extends OutgoingData>>());
            }

            assert commandHandlerCache.containsKey(commandHandler.getCommandSetId());

            final Map<Byte, CommandHandler<? extends IncomingData, ? extends OutgoingData>> map = commandHandlerCache.get(commandHandler.getCommandSetId());

            if (map.containsKey(commandHandler.getCommandId())) {
                throw new IllegalArgumentException("Command handler with set id " + commandHandler.getCommandSetId() + " and command id " + commandHandler.getCommandId() + " is already installed.");
            }

            map.put(commandHandler.getCommandId(), commandHandler);

            LOGGER.info("added command handler " + commandHandler.getCommandSetId() + "/" + commandHandler.getCommandId());
        }
    };

    /**
     * Thread that is started for each client connection.
     */
    private class ClientThread extends Thread {

        private Socket socket;

        public ClientThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

            try {
                final JDWPStream stream = new JDWPStream(socket.getInputStream(), socket.getOutputStream());
                stream.handshake();
                LOGGER.info("Handshake passed successfully!");

                while (!shutdown) {
                    try {
                        final IncomingPacket<? extends IncomingData, ? extends OutgoingData> incomingPacket = stream.receive(commandHandlerRegistry);
                        try {
                            final ReplyPacket<? extends IncomingData, ? extends OutgoingData> replyPacket = incomingPacket.handle(stream);
                            if (replyPacket == null) {
                                LOGGER.warning("No handler found for command " + incomingPacket.getCommandSetId() + "/" + incomingPacket.getCommandId() + "!");
                                throw new JDWPNotImplementedException();
                            }
                            stream.send(replyPacket);
                        } catch (JDWPException e) {
                            LOGGER.warning("JDWP exception occured: " + e);
                            stream.send(incomingPacket.createErrorReply((short) e.errorCode()));
                        } catch (Throwable t) {
                            LOGGER.log(Level.SEVERE, "Severe generic exception occured while handling packet", t);
                            stream.send(incomingPacket.createErrorReply((short) Error.INTERNAL));

                        }
                    } catch (JDWPIncomingPacketException e) {
                        LOGGER.warning("JDWP exception occured while reading packet: " + e.innerException());
                        stream.send(e.packet().createErrorReply((short) e.innerException().errorCode()));
                    }
                }

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "IO exception in the client thread", e);
            } finally {

                try {
                    socket.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "IO exception when closing socket", e);
                }

                LOGGER.info("Client shutdown!");
            }
        }
    };
}
