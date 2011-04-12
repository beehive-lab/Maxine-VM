/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.max.jdwp.data.CommandHandler;
import com.sun.max.jdwp.data.CommandHandlerRegistry;
import com.sun.max.jdwp.data.IncomingData;
import com.sun.max.jdwp.data.IncomingPacket;
import com.sun.max.jdwp.data.JDWPException;
import com.sun.max.jdwp.data.JDWPIncomingPacketException;
import com.sun.max.jdwp.data.JDWPInputStream;
import com.sun.max.jdwp.data.JDWPOutputStream;
import com.sun.max.jdwp.data.JDWPSender;
import com.sun.max.jdwp.data.OutgoingData;
import com.sun.max.jdwp.data.ReplyPacket;

/**
 *
 * @author Thomas Wuerthinger
 */
class JDWPStream implements JDWPSender {

    private static final Logger LOGGER = Logger.getLogger(JDWPStream.class.getName());
    private static final String HANDSHAKE = "JDWP-Handshake";
    private static final int HEADER_SIZE = 11;

    private DataInputStream in;
    private DataOutputStream out;

    // Counter that is increased for each sent outgoing command.
    private int outgoingID;

    JDWPStream(InputStream is, OutputStream os) {
        in = new DataInputStream(is);
        out = new DataOutputStream(os);
    }

    public synchronized void sendCommand(OutgoingData outgoingData) throws IOException {
        outgoingID++;
        send(outgoingID, outgoingData);
    }

    private void send(int id, OutgoingData outgoingData) throws IOException {

        LOGGER.info("***************************************************************************");
        LOGGER.info("Sending eventPacket with id=" + id);
        LOGGER.info(outgoingData.toString());

        final byte[] dataBytes = toByteArray(outgoingData);
        final int length = HEADER_SIZE + dataBytes.length;
        out.writeInt(length);
        out.writeInt(id);
        out.writeByte(0);
        out.writeByte(outgoingData.getCommandSetId());
        out.writeByte(outgoingData.getCommandId());
        out.write(dataBytes);
    }

    private byte[] toByteArray(OutgoingData outgoingData) {
        if (outgoingData == null) {
            return new byte[0];
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            outgoingData.write(new JDWPOutputStream(out));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return out.toByteArray();
    }

    /**
     * Sends a reply packet to the client.
     *
     * @param packet the packet to be sent
     * @throws IOException this exception is thrown when a problem occurred while writing the packet bytes
     */
    public synchronized <IncomingData_Type extends IncomingData, OutgoingData_Type extends OutgoingData> void send(ReplyPacket<IncomingData_Type, OutgoingData_Type> packet) throws IOException {

        LOGGER.info("Sending reply packet: " + packet);
        final byte[] dataBytes = toByteArray(packet.getData());
        final int length = HEADER_SIZE + dataBytes.length;
        out.writeInt(length);
        out.writeInt(packet.getId());
        out.writeByte(packet.getFlags());
        out.writeShort(packet.getErrorCode());
        out.write(dataBytes);
    }

    /**
     * Performs a JDWP handshake and throws an exception if the handshake fails.
     *
     * @throws IOException this exception is thrown if the handshake fails
     */
    public void handshake() throws IOException {
        if (readAndCheckStringAsBytes(HANDSHAKE)) {
            writeStringAsBytes(HANDSHAKE);
        } else {
            throw new IOException("JDWP handshake failed");
        }
    }

    private void writeStringAsBytes(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            assert ((byte) s.charAt(i)) == s.charAt(i) : "String may only consist of ASCII characters";
            out.writeByte((byte) s.charAt(i));
        }
    }

    private boolean readAndCheckStringAsBytes(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            assert ((byte) s.charAt(i)) == s.charAt(i) : "String may only consist of ASCII characters";
            final byte b = in.readByte();
            if (b != ((byte) s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tries to receive a new JDWP packet and decode it by lookup up a command handler in the given command handler
     * registry.
     *
     * @param registry the registry that is used to lookup the command handler based on the bytes in the packet header
     * @return a newly created IncomingPacket object representing the read packet
     * @throws IOException this exception is thrown, when a problem occurred while reading the packet bytes
     * @throws JDWPIncomingPacketException this exception is thrown, when a problem occurred while translating the
     *             packet bytes
     */
    public IncomingPacket<? extends IncomingData, ? extends OutgoingData> receive(CommandHandlerRegistry registry) throws IOException, JDWPIncomingPacketException {

        final int length = in.readInt();
        final int id = in.readInt();
        final byte flags = in.readByte();
        final byte commandSetId = in.readByte();
        final byte commandId = in.readByte();
        final byte[] data = new byte[length - HEADER_SIZE];
        in.read(data);

        final CommandHandler<? extends IncomingData, ? extends OutgoingData> handler = registry.findCommandHandler(commandSetId, commandId);
        if (handler == null) {
            final IncomingPacket<? extends IncomingData, ? extends OutgoingData> result = createIncomingPacket(length, id, flags, commandSetId, commandId, null, null);
            return result;
        }

        assert handler.getCommandId() == commandId;
        assert handler.getCommandSetId() == commandSetId;

        try {
            final IncomingData incomingData = handler.createIncomingDataObject();
            @SuppressWarnings("unchecked")
            final CommandHandler<IncomingData, OutgoingData> handlerDownCast = (CommandHandler<IncomingData, OutgoingData>) handler;

            incomingData.read(new JDWPInputStream(new ByteArrayInputStream(data), handlerDownCast, incomingData));
            final IncomingPacket<? extends IncomingData, ? extends OutgoingData> p = createIncomingPacket(length, id, flags, commandSetId, commandId, incomingData, handler);

            LOGGER.info("#####################################################################################");
            LOGGER.info(CommandHandler.Static.getCommandName(handler) + ": " + p);
            return p;

        } catch (JDWPException e) {
            throw new JDWPIncomingPacketException(e, createIncomingPacket(length, id, flags, commandSetId, commandId, null, handler));
        }
    }

    @SuppressWarnings("unchecked")
    private IncomingPacket<? extends IncomingData, ? extends OutgoingData> createIncomingPacket(int length, int id, byte flags, byte commandSetId, byte commandId, IncomingData data,
                    CommandHandler handler) {
        final IncomingPacket incomingPacket = new IncomingPacket<IncomingData, OutgoingData>(length, id, flags, commandSetId, commandId, data, (CommandHandler<IncomingData, OutgoingData>) handler);
        final IncomingPacket<? extends IncomingData, ? extends OutgoingData> p = (IncomingPacket<? extends IncomingData, ? extends OutgoingData>) incomingPacket;
        return p;
    }
}
