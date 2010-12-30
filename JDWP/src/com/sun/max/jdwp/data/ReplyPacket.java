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
 * This class represents a JDWP reply packet. It is always associated with the incoming packet for which this packet is a reply.
 *
 * @author Thomas Wuerthinger
 */
public final class ReplyPacket<IncomingData_Type extends IncomingData, OutgoingData_Type extends OutgoingData> {

    private IncomingPacket<IncomingData_Type, OutgoingData_Type> incomingPacket;
    private short errorCode;
    private OutgoingData_Type data;

    public ReplyPacket(IncomingPacket<IncomingData_Type, OutgoingData_Type> incomingPacket, short errorCode) {
        this.incomingPacket = incomingPacket;
        this.errorCode = errorCode;
    }

    public ReplyPacket(IncomingPacket<IncomingData_Type, OutgoingData_Type> incomingPacket, OutgoingData_Type data) {
        this.incomingPacket = incomingPacket;
        this.data = data;
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
        return data;
    }

    public int getId() {
        return incomingPacket.getId();
    }

    /**
     * The error code of the packet, if this is an error reply packet, or zero otherwise.
     * @return the error code
     */
    public short getErrorCode() {
        return errorCode;
    }

    public IncomingPacket<IncomingData_Type, OutgoingData_Type> getOriginalPacket() {
        return incomingPacket;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ReplyPacket(" + getId() + ")[flags=" + getFlags() + ", error=" + getErrorCode() + ", data=" + getData() + "]");
        return sb.toString();
    }
}
