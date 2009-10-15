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
 * This class defines outgoing JDWP data. The object can serialize itself to a {@link JDWPOutputStream} and identify the type of command.
 *
 * @author Thomas Wuerthinger
 */
public interface OutgoingData {

    /**
     * Writes this outgoing data to the given {@link JDWPOutputStream} object.
     * @param outputStream the stream to write the data on
     * @throws IOException this exception should be thrown, when there was a problem writing the data
     */
    void write(JDWPOutputStream outputStream) throws IOException;

    /**
     * Identifies the command set of the command that this outgoing data is part of.
     * @return the command set identifier
     */
    byte getCommandSetId();

    /**
     * Identifies the command that this outgoing data is part of.
     * @return the command identifier
     */
    byte getCommandId();
}
