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

// WARNING: Generated file, do not modify!

// To use the original JDWP specification as a basis, some styleguides have to be turned off
// Checkstyle: stop field name check

package com.sun.max.jdwp.protocol;
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.constants.*;
@SuppressWarnings("unused")

public final class ArrayTypeCommands {
    public static final int COMMAND_SET = 4;
    private ArrayTypeCommands() { }  // hide constructor

    public static class NewInstance {
        public static final byte COMMAND = 1;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ArrayTypeID _arrType;

            public int _length;
            public IncomingRequest(ID.ArrayTypeID arrType,
                int length) {
                this._arrType = arrType;
                this._length = length;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _arrType = ID.read(ps.getInputStream(), ID.ArrayTypeID.class);
                _length = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _arrType.write(ps.getOutputStream());
                ps.write(_length);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_arrType=" + _arrType);
                stringBuilder.append(", ");
                stringBuilder.append("_length=" + _length);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue _newArray;
            public Reply(JDWPValue newArray) {
                this._newArray = newArray;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _newArray = ps.readValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_newArray);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_newArray=" + _newArray);
                return stringBuilder.toString();
            }
        }
    }
}
