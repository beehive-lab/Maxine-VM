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

import java.io.*;

/**
 *
 * Objects implementing this interface are capable of sending JDWP output data as a command to the client. The object is
 * responsible for creating the packet that encapsulate the outgoing data and coming up with a new packet identifier.
 *
 * @author Thomas Wuerthinger
 *
 */
public interface JDWPSender {

    /**
     * Sends the outgoing data object to the client.
     * @param outgoingData the data to be sent
     * @throws IOException this exception is thrown when there is an exception while writing the data
     */
    void sendCommand(OutgoingData outgoingData) throws IOException;
}
