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
 * JDWP exception with the additional ability to store an incoming packet. Used when there is a JDWP exception while reading a packet.
 *
 * @author Thomas Wuerthinger
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
