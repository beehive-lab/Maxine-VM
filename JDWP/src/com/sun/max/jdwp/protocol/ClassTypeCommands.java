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

public final class ClassTypeCommands {
    public static final int COMMAND_SET = 3;
    private ClassTypeCommands() { }  // hide constructor

    public static class Superclass {
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
            public ID.ClassID _clazz;
            public IncomingRequest(ID.ClassID clazz) {
                this._clazz = clazz;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _clazz = ID.read(ps.getInputStream(), ID.ClassID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _clazz.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_clazz=" + _clazz);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.ClassID _superclass;
            public Reply(ID.ClassID superclass) {
                this._superclass = superclass;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _superclass = ID.read(ps.getInputStream(), ID.ClassID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _superclass.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_superclass=" + _superclass);
                return stringBuilder.toString();
            }
        }
    }

    public static class SetValues {
        public static final byte COMMAND = 2;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class FieldValue {

            public ID.FieldID _fieldID;

            public JDWPValue _value;
            public FieldValue(ID.FieldID fieldID,
                JDWPValue value) {
                this._fieldID = fieldID;
                this._value = value;
            }
            public FieldValue() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _fieldID = ID.read(ps.getInputStream(), ID.FieldID.class);
                _value = ps.readUntaggedValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _fieldID.write(ps.getOutputStream());
                ps.write(_value);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_fieldID=" + _fieldID);
                stringBuilder.append(", ");
                stringBuilder.append("_value=" + _value);
                return stringBuilder.toString();
            }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ClassID _clazz;

            public FieldValue[] _values;
            public IncomingRequest(ID.ClassID clazz,
                FieldValue[] values) {
                this._clazz = clazz;
                this._values = values;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _clazz = ID.read(ps.getInputStream(), ID.ClassID.class);
                final int valuesCount = ps.readInt();
                _values = new FieldValue[valuesCount];
                for (int i = 0; i < valuesCount; i++) {
                    _values[i] = new FieldValue();
                    _values[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _clazz.write(ps.getOutputStream());
                ps.write(_values.length);
                for (int i = 0; i < _values.length; i++) {
                    _values[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_clazz=" + _clazz);
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

    public static class InvokeMethod {
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
            public ID.ClassID _clazz;

            public ID.ThreadID _thread;

            public ID.MethodID _methodID;

            public JDWPValue[] _arguments;

            public int _options;
            public IncomingRequest(ID.ClassID clazz,
                ID.ThreadID thread,
                ID.MethodID methodID,
                JDWPValue[] arguments,
                int options) {
                this._clazz = clazz;
                this._thread = thread;
                this._methodID = methodID;
                this._arguments = arguments;
                this._options = options;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _clazz = ID.read(ps.getInputStream(), ID.ClassID.class);
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                _methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
                final int argumentsCount = ps.readInt();
                _arguments = new JDWPValue[argumentsCount];
                for (int i = 0; i < argumentsCount; i++) {
                    _arguments[i] = ps.readValue();
                }
                _options = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _clazz.write(ps.getOutputStream());
                _thread.write(ps.getOutputStream());
                _methodID.write(ps.getOutputStream());
                ps.write(_arguments.length);
                for (int i = 0; i < _arguments.length; i++) {
                    ps.write(_arguments[i]);
                }
                ps.write(_options);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_clazz=" + _clazz);
                stringBuilder.append(", ");
                stringBuilder.append("_thread=" + _thread);
                stringBuilder.append(", ");
                stringBuilder.append("_methodID=" + _methodID);
                stringBuilder.append(", ");
                stringBuilder.append("_arguments=[" + _arguments.length + "]{");
                for (int i = 0; i < _arguments.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_arguments[i]=" + _arguments[i]);
                }
                stringBuilder.append("}");
                stringBuilder.append(", ");
                stringBuilder.append("_options=" + _options);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue _returnValue;

            public JDWPValue _exception;
            public Reply(JDWPValue returnValue,
                JDWPValue exception) {
                this._returnValue = returnValue;
                this._exception = exception;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _returnValue = ps.readValue();
                _exception = ps.readValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_returnValue);
                ps.write(_exception);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_returnValue=" + _returnValue);
                stringBuilder.append(", ");
                stringBuilder.append("_exception=" + _exception);
                return stringBuilder.toString();
            }
        }
    }

    public static class NewInstance {
        public static final byte COMMAND = 4;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ClassID _clazz;

            public ID.ThreadID _thread;

            public ID.MethodID _methodID;

            public JDWPValue[] _arguments;

            public int _options;
            public IncomingRequest(ID.ClassID clazz,
                ID.ThreadID thread,
                ID.MethodID methodID,
                JDWPValue[] arguments,
                int options) {
                this._clazz = clazz;
                this._thread = thread;
                this._methodID = methodID;
                this._arguments = arguments;
                this._options = options;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _clazz = ID.read(ps.getInputStream(), ID.ClassID.class);
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                _methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
                final int argumentsCount = ps.readInt();
                _arguments = new JDWPValue[argumentsCount];
                for (int i = 0; i < argumentsCount; i++) {
                    _arguments[i] = ps.readValue();
                }
                _options = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _clazz.write(ps.getOutputStream());
                _thread.write(ps.getOutputStream());
                _methodID.write(ps.getOutputStream());
                ps.write(_arguments.length);
                for (int i = 0; i < _arguments.length; i++) {
                    ps.write(_arguments[i]);
                }
                ps.write(_options);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_clazz=" + _clazz);
                stringBuilder.append(", ");
                stringBuilder.append("_thread=" + _thread);
                stringBuilder.append(", ");
                stringBuilder.append("_methodID=" + _methodID);
                stringBuilder.append(", ");
                stringBuilder.append("_arguments=[" + _arguments.length + "]{");
                for (int i = 0; i < _arguments.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_arguments[i]=" + _arguments[i]);
                }
                stringBuilder.append("}");
                stringBuilder.append(", ");
                stringBuilder.append("_options=" + _options);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue _newObject;

            public JDWPValue _exception;
            public Reply(JDWPValue newObject,
                JDWPValue exception) {
                this._newObject = newObject;
                this._exception = exception;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _newObject = ps.readValue();
                _exception = ps.readValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_newObject);
                ps.write(_exception);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_newObject=" + _newObject);
                stringBuilder.append(", ");
                stringBuilder.append("_exception=" + _exception);
                return stringBuilder.toString();
            }
        }
    }
}
