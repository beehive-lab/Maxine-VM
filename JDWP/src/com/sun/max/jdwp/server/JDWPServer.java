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

    private ServerSocket _serverSocket;
    private boolean _shutdown;

    /**
     * Shuts down the JDWP server. The server must be running.
     */
    public void shutdown() {
        assert _serverSocket != null : "Not running!";
        assert !_shutdown : "Already shutting down!";
        _shutdown = true;
    }

    /**
     * Starts the thread that waits for incoming JDWP connections.
     *
     * @param serverSocket the server socket on which to listen for incoming connections
     * @throws IOException this exception is thrown, when the server socket could not be created
     */
    public void start(ServerSocket serverSocket) throws IOException {
        assert _serverSocket == null : "Already started!";
        _serverSocket = serverSocket;
        _serverSocket.setSoTimeout(TIMEOUT);
        new Thread(_waitForClientsThread).start();
    }

    /**
     * This command handler registry can be used to lookup or add JDWP command handlers.
     *
     * @return the command handler registry
     */
    public CommandHandlerRegistry commandHandlerRegistry() {
        return _commandHandlerRegistry;
    }

    /**
     * Thread waiting for clients to connect to the JDWP server.
     */
    private Runnable _waitForClientsThread = new Runnable() {
        public void run() {
            LOGGER.info("JDWPServer waiting for clients");
            try {
                while (!_shutdown) {
                    try {
                        final Socket clientSocket = _serverSocket.accept();
                        new ClientThread(clientSocket).start();
                    } catch (SocketTimeoutException e) {
                    }
                }
            } catch (IOException e) {
                LOGGER.severe("Exception occurred while waiting for clients: " + e.toString());
            } finally {
                LOGGER.info("JDWP server is shut down");
                _serverSocket = null;
                _shutdown = false;
            }
        }

    };

    /**
     * Registry that manages the set of command handlers.
     */
    private final CommandHandlerRegistry _commandHandlerRegistry = new CommandHandlerRegistry() {

        private Map<Byte, Map<Byte, CommandHandler<? extends IncomingData, ? extends OutgoingData>>> _commandHandlerCache = new HashMap<Byte, Map<Byte, CommandHandler<? extends IncomingData, ? extends OutgoingData>>>();

        public CommandHandler<? extends IncomingData, ? extends OutgoingData> findCommandHandler(byte commandSetId, byte commandId) {
            if (!_commandHandlerCache.containsKey(commandSetId)) {
                return null;
            }
            final CommandHandler<? extends IncomingData, ? extends OutgoingData> result = _commandHandlerCache.get(commandSetId).get(commandId);
            assert result == null || result.getCommandSetId() == commandSetId : "Command set ID must match.";
            assert result == null || result.getCommandId() == commandId : "Command ID must match.";
            return result;
        }

        public void addCommandHandler(CommandHandler<? extends IncomingData, ? extends OutgoingData> commandHandler) {
            if (!_commandHandlerCache.containsKey(commandHandler.getCommandSetId())) {
                _commandHandlerCache.put(commandHandler.getCommandSetId(), new HashMap<Byte, CommandHandler<? extends IncomingData, ? extends OutgoingData>>());
            }

            assert _commandHandlerCache.containsKey(commandHandler.getCommandSetId());

            final Map<Byte, CommandHandler<? extends IncomingData, ? extends OutgoingData>> map = _commandHandlerCache.get(commandHandler.getCommandSetId());

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

        private Socket _socket;

        public ClientThread(Socket socket) {
            _socket = socket;
        }

        @Override
        public void run() {

            try {
                final JDWPStream stream = new JDWPStream(_socket.getInputStream(), _socket.getOutputStream());
                stream.handshake();
                LOGGER.info("Handshake passed successfully!");

                while (!_shutdown) {
                    try {
                        final IncomingPacket<? extends IncomingData, ? extends OutgoingData> incomingPacket = stream.receive(_commandHandlerRegistry);
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
                    _socket.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "IO exception when closing socket", e);
                }

                LOGGER.info("Client shutdown!");
            }
        }
    };
}
