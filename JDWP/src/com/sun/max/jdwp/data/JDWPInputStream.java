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
import java.util.*;

/**
 * Stream class for reading JDWP values. It has a child input stream to which the actual reads are delegated.
 *
 * @author Thomas Wuerthinger
 */
public class JDWPInputStream {

    private DataInputStream dataInputStream;
    private CommandHandler<IncomingData, OutgoingData> commandHandler;
    private IncomingData incomingData;

    public JDWPInputStream(InputStream inputStream, CommandHandler<IncomingData, OutgoingData> handler, IncomingData incomingData) {
        this.dataInputStream = new DataInputStream(inputStream);
        this.commandHandler = handler;
        this.incomingData = incomingData;
    }

    public boolean readBoolean() throws IOException {
        return dataInputStream.readBoolean();
    }

    public byte readByte() throws IOException {
        return dataInputStream.readByte();
    }

    public int readInt() throws IOException {
        return dataInputStream.readInt();
    }

    public long readLong() throws IOException {
        return dataInputStream.readLong();
    }

    public InputStream getInputStream() {
        return dataInputStream;
    }

    /**
     * Reads a string according to JDWP syntax.
     * @return the string read from the JDWP stream
     * @throws IOException this exception is thrown, when there was a problem reading the raw bytes
     */
    public String readString() throws IOException {

        final int length = dataInputStream.readInt();

        final byte[] data = new byte[length];
        dataInputStream.read(data);

        final String s = new String(data);
        return s;
    }

    public JDWPLocation readLocation() throws IOException {
        return new JDWPLocation(dataInputStream);
    }

    /**
     * Reads a object identifier with a preceding tag (see {@link com.sun.max.jdwp.constants.Tag}).
     * @return the read object identifier
     * @throws IOException
     */
    public ID.ObjectID readTaggedObjectReference() throws IOException {

        // Read over tag, this information is currently not used.
        dataInputStream.readByte();

        return ID.read(dataInputStream, ID.ObjectID.class);
    }

    public JDWPValue readValue() throws IOException {
        return new JDWPValue(dataInputStream);
    }

    /**
     * Reads a JDWPValue that is not preceded with a tag (see {@link com.sun.max.jdwp.constants.Tag}). The registered command handler
     * is used to resolve this information based on the semantics of the command.
     *
     * @return the read JDWPValue object
     * @throws IOException this exception is thrown, when there was a problem reading the raw bytes of the value
     * @throws JDWPException this exception is thrown, when the command handler had a problem resolving the tag
     */
    public JDWPValue readUntaggedValue() throws IOException, JDWPException {
        return new JDWPValue(dataInputStream, commandHandler.helpAtDecodingUntaggedValue(incomingData));
    }

    /**
     * Reads a list of values from the stream. This function is not yet implemented, because it is currently not needed in the protocol.
     * @return a list of values read from the stream
     * @throws IOException this exception is thrown, when there was a problem reading the raw bytes of the value
     * @throws JDWPException this exception is thrown, when the command handler had a problem resolving tags
     */
    public List<? extends JDWPValue> readArrayRegion() throws IOException, JDWPException {
        throw new JDWPNotImplementedException();
    }
}
