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
/*VCSID=acf2b02d-bac7-48eb-b952-b8e56f89aab5*/
package com.sun.max.jdwp.data;

/**
 * This class represents a JDWP reply packet. It is always associated with the incoming packet for which this packet is a reply.
 *
 * @author Thomas Wuerthinger
 */
public final class ReplyPacket<IncomingData_Type extends IncomingData, OutgoingData_Type extends OutgoingData> {

    private IncomingPacket<IncomingData_Type, OutgoingData_Type> _incomingPacket;
    private short _errorCode;
    private OutgoingData_Type _data;

    public ReplyPacket(IncomingPacket<IncomingData_Type, OutgoingData_Type> incomingPacket, short errorCode) {
        _incomingPacket = incomingPacket;
        _errorCode = errorCode;
    }

    public ReplyPacket(IncomingPacket<IncomingData_Type, OutgoingData_Type> incomingPacket, OutgoingData_Type data) {
        _incomingPacket = incomingPacket;
        _data = data;
    }

    /**
     * Flags of the packet, a reply packet always has the same constant flags.
     * @return 0x80 according to the JDWP definition of the flags for a reply packet
     */
    public byte getFlags() {
        return (byte) 0x80;
    }

    /**
     * The outgoing data to be sent in reply or null, if it is an error packet.
     * @return the outgoing data
     */
    public OutgoingData_Type getData() {
        return _data;
    }

    public int getId() {
        return _incomingPacket.getId();
    }

    /**
     * The error code of the packet, if this is an error reply packet, or zero otherwise.
     * @return the error code
     */
    public short getErrorCode() {
        return _errorCode;
    }

    public IncomingPacket<IncomingData_Type, OutgoingData_Type> getOriginalPacket() {
        return _incomingPacket;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ReplyPacket(" + getId() + ")[flags=" + getFlags() + ", error=" + getErrorCode() + ", data=" + getData() + "]");
        return sb.toString();
    }
}
