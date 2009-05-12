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
package com.sun.max.jdwp.data;

/**
 * Class representing an incoming JDWP packet. The two template parameters determine the types of the incoming and outgoing
 * data of the packet.
 *
 * @author Thomas Wuerthinger
 */
public final class IncomingPacket<IncomingData_Type extends IncomingData, OutgoingData_Type extends OutgoingData> {

    private int _length;
    private int _id;
    private byte _commandSetId;
    private byte _commandId;
    private byte _flags;
    private IncomingData_Type _data;
    private CommandHandler<IncomingData_Type, OutgoingData_Type> _handler;

    public IncomingPacket(int length, int id, byte flags, byte commandSetId, byte commandId, IncomingData_Type data, CommandHandler<IncomingData_Type, OutgoingData_Type> handler) {
        _length = length;
        _id = id;
        _commandSetId = commandSetId;
        _commandId = commandId;
        _flags = flags;
        _data = data;
        _handler = handler;

        assert handler == null || handler.getCommandId() == commandId;
        assert handler == null || handler.getCommandSetId() == commandSetId;
    }

    /**
     * Length of the packet, including the header length.
     * @return the length of the packet
     */
    public int getLength() {
        return _length;
    }

    /**
     * Integer id uniquely identifying this packet for a certain client.
     * @return the unique id
     */
    public int getId() {
        return _id;
    }

    /**
     * The id of the command set of the command, this packet is representing.
     * @return the id of the command set
     */
    public byte getCommandSetId() {
        return _commandSetId;
    }

    /**
     * The id of the command, this packet is representing.
     * @return the id of the command
     */
    public byte getCommandId() {
        return _commandId;
    }

    /**
     * Flags of the packet, currently flags are not used in JDWP for incoming packets.
     * @return the flags of the packet
     */
    public byte getFlags() {
        return _flags;
    }

    /**
     * The handler responsible for processing this packet. The handler may be null, when there is no handler registered for a specific command.
     * @return the handler registered for the command
     */
    public CommandHandler<IncomingData_Type, OutgoingData_Type> getHandler() {
        return _handler;
    }

    /**
     * Processes this packet using the registered handler. If the handler is null, then this function returns null.
     * @param replyChannel The reply channel for packets triggered by this packet that need to be sent back to the client.
     * @return a packet ready to be sent to the client that is a reply of the command handler to this incoming packet
     * @throws JDWPException this exception is thrown, when there is an error during processing the packet
     */
    public ReplyPacket<IncomingData_Type, OutgoingData_Type> handle(JDWPSender replyChannel) throws JDWPException {
        if (_handler == null) {
            // No handler is responsible for this package!
            return null;
        }
        return new ReplyPacket<IncomingData_Type, OutgoingData_Type>(this, _handler.handle(getData(), replyChannel));
    }

    /**
     * Typed data represending the data from the client as a Java object.
     * @return the incoming data
     */
    public IncomingData_Type getData() {
        return _data;
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
