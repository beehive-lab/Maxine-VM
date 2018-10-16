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

import java.io.*;
import java.util.*;

/**
 * Stream class for reading JDWP values. It has a child input stream to which the actual reads are delegated.
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
