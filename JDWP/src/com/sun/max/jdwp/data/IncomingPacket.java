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
package com.sun.max.jdwp.data;

/**
 * Class representing an incoming JDWP packet. The two template parameters determine the types of the incoming and outgoing
 * data of the packet.
 *
 * @author Thomas Wuerthinger
 */
public final class IncomingPacket<IncomingData_Type extends IncomingData, OutgoingData_Type extends OutgoingData> {

    private int length;
    private int id;
    private byte commandSetId;
    private byte commandId;
    private byte flags;
    private IncomingData_Type data;
    private CommandHandler<IncomingData_Type, OutgoingData_Type> handler;

    public IncomingPacket(int length, int id, byte flags, byte commandSetId, byte commandId, IncomingData_Type data, CommandHandler<IncomingData_Type, OutgoingData_Type> handler) {
        this.length = length;
        this.id = id;
        this.commandSetId = commandSetId;
        this.commandId = commandId;
        this.flags = flags;
        this.data = data;
        this.handler = handler;

        assert handler == null || handler.getCommandId() == commandId;
        assert handler == null || handler.getCommandSetId() == commandSetId;
    }

    /**
     * Length of the packet, including the header length.
     * @return the length of the packet
     */
    public int getLength() {
        return length;
    }

    /**
     * Integer id uniquely identifying this packet for a certain client.
     * @return the unique id
     */
    public int getId() {
        return id;
    }

    /**
     * The id of the command set of the command, this packet is representing.
     * @return the id of the command set
     */
    public byte getCommandSetId() {
        return commandSetId;
    }

    /**
     * The id of the command, this packet is representing.
     * @return the id of the command
     */
    public byte getCommandId() {
        return commandId;
    }

    /**
     * Flags of the packet, currently flags are not used in JDWP for incoming packets.
     * @return the flags of the packet
     */
    public byte getFlags() {
        return flags;
    }

    /**
     * The handler responsible for processing this packet. The handler may be null, when there is no handler registered for a specific command.
     * @return the handler registered for the command
     */
    public CommandHandler<IncomingData_Type, OutgoingData_Type> getHandler() {
        return handler;
    }

    /**
     * Processes this packet using the registered handler. If the handler is null, then this function returns null.
     * @param replyChannel The reply channel for packets triggered by this packet that need to be sent back to the client.
     * @return a packet ready to be sent to the client that is a reply of the command handler to this incoming packet
     * @throws JDWPException this exception is thrown, when there is an error during processing the packet
     */
    public ReplyPacket<IncomingData_Type, OutgoingData_Type> handle(JDWPSender replyChannel) throws JDWPException {
        if (handler == null) {
            // No handler is responsible for this package!
            return null;
        }
        return new ReplyPacket<IncomingData_Type, OutgoingData_Type>(this, handler.handle(getData(), replyChannel));
    }

    /**
     * Typed data represending the data from the client as a Java object.
     * @return the incoming data
     */
    public IncomingData_Type getData() {
        return data;
    }

    /**
     * Creates an error reply packet as a response to this incoming packet.
     * @param errorCode the JDWP error code, see {@link com.sun.max.jdwp.constants.Error} for possible values.
     * @return an error reply packet that is ready to be sent to the client.
     */
    public ReplyPacket<IncomingData_Type, OutgoingData_Type> createErrorReply(short errorCode) {
        return new ReplyPacket<IncomingData_Type, OutgoingData_Type>(this, errorCode);
    }

    @Override
    public String toString() {
        return "IncomingPacket(" + getId() + "){" + getData() + "}";
    }
}
