/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * JDWP exception with the additional ability to store an incoming packet. Used when there is a JDWP exception while reading a packet.
 */
public class JDWPIncomingPacketException extends JDWPException {

    private final JDWPException innerException;
    private final IncomingPacket<? extends IncomingData, ? extends OutgoingData> packet;

    public JDWPIncomingPacketException(JDWPException innerException, IncomingPacket<? extends IncomingData, ? extends OutgoingData> packet) {
        this.innerException = innerException;
        this.packet = packet;
    }

    /**
     * The inner JDWP exception that caused this exception to be thrown.
     * @return the inner JDWP exception
     */
    public JDWPException innerException() {
        return innerException;
    }

    /**
     * Packet that was parsed when the exception occurred. The data of the packet may be incomplete, but the header must be complete.
     * @return the packet whose parsing caused the exception
     */
    public IncomingPacket<? extends IncomingData, ? extends OutgoingData> packet() {
        return packet;
    }
}
