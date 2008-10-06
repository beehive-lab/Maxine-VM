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
/*VCSID=86895649-07c2-4c4d-8e71-d9b1ba2b29d7*/

// WARNING: Generated file, do not modify!

// To use the original JDWP specification as a basis, some styleguides have to be turned off
// Checkstyle: stop field name check

package com.sun.max.jdwp.protocol;
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.constants.*;
@SuppressWarnings("unused")

public final class ArrayReferenceCommands {
    public static final int COMMAND_SET = 13;
    private ArrayReferenceCommands() { }  // hide constructor

    public static class Length {
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
            public ID.ArrayID _arrayObject;
            public IncomingRequest(ID.ArrayID arrayObject) {
                this._arrayObject = arrayObject;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _arrayObject = ID.read(ps.getInputStream(), ID.ArrayID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _arrayObject.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_arrayObject=" + _arrayObject);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int _arrayLength;
            public Reply(int arrayLength) {
                this._arrayLength = arrayLength;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _arrayLength = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_arrayLength);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_arrayLength=" + _arrayLength);
                return stringBuilder.toString();
            }
        }
    }

    public static class GetValues {
        public static final byte COMMAND = 2;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ArrayID _arrayObject;

            public int _firstIndex;

            public int _length;
            public IncomingRequest(ID.ArrayID arrayObject,
                int firstIndex,
                int length) {
                this._arrayObject = arrayObject;
                this._firstIndex = firstIndex;
                this._length = length;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _arrayObject = ID.read(ps.getInputStream(), ID.ArrayID.class);
                _firstIndex = ps.readInt();
                _length = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _arrayObject.write(ps.getOutputStream());
                ps.write(_firstIndex);
                ps.write(_length);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_arrayObject=" + _arrayObject);
                stringBuilder.append(", ");
                stringBuilder.append("_firstIndex=" + _firstIndex);
                stringBuilder.append(", ");
                stringBuilder.append("_length=" + _length);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public java.util.List<? extends JDWPValue> _values;
            public Reply(java.util.List<? extends JDWPValue> values) {
                this._values = values;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _values = ps.readArrayRegion();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_values);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_values=" + _values);
                return stringBuilder.toString();
            }
        }
    }

    public static class SetValues {
        public static final byte COMMAND = 3;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ArrayID _arrayObject;

            public int _firstIndex;

            public JDWPValue[] _values;
            public IncomingRequest(ID.ArrayID arrayObject,
                int firstIndex,
                JDWPValue[] values) {
                this._arrayObject = arrayObject;
                this._firstIndex = firstIndex;
                this._values = values;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _arrayObject = ID.read(ps.getInputStream(), ID.ArrayID.class);
                _firstIndex = ps.readInt();
                final int valuesCount = ps.readInt();
                _values = new JDWPValue[valuesCount];
                for (int i = 0; i < valuesCount; i++) {
                    _values[i] = ps.readUntaggedValue();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _arrayObject.write(ps.getOutputStream());
                ps.write(_firstIndex);
                ps.write(_values.length);
                for (int i = 0; i < _values.length; i++) {
                    ps.write(_values[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_arrayObject=" + _arrayObject);
                stringBuilder.append(", ");
                stringBuilder.append("_firstIndex=" + _firstIndex);
                stringBuilder.append(", ");
                stringBuilder.append("_values=[" + _values.length + "]{");
                for (int i = 0; i < _values.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_values[i]=" + _values[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                return stringBuilder.toString();
            }
        }
    }
}
