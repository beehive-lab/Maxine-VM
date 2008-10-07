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

public final class ObjectReferenceCommands {
    public static final int COMMAND_SET = 9;
    private ObjectReferenceCommands() { }  // hide constructor

    public static class ReferenceType {
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
            public ID.ObjectID _object;
            public IncomingRequest(ID.ObjectID object) {
                this._object = object;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _object = ID.read(ps.getInputStream(), ID.ObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _object.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_object=" + _object);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public byte _refTypeTag;

            public ID.ReferenceTypeID _typeID;
            public Reply(byte refTypeTag,
                ID.ReferenceTypeID typeID) {
                this._refTypeTag = refTypeTag;
                this._typeID = typeID;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refTypeTag = ps.readByte();
                _typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_refTypeTag);
                _typeID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refTypeTag=" + _refTypeTag);
                stringBuilder.append(", ");
                stringBuilder.append("_typeID=" + _typeID);
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

        public static class Field {

            public ID.FieldID _fieldID;
            public Field(ID.FieldID fieldID) {
                this._fieldID = fieldID;
            }
            public Field() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _fieldID = ID.read(ps.getInputStream(), ID.FieldID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _fieldID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_fieldID=" + _fieldID);
                return stringBuilder.toString();
            }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ObjectID _object;

            public Field[] _fields;
            public IncomingRequest(ID.ObjectID object,
                Field[] fields) {
                this._object = object;
                this._fields = fields;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _object = ID.read(ps.getInputStream(), ID.ObjectID.class);
                final int fieldsCount = ps.readInt();
                _fields = new Field[fieldsCount];
                for (int i = 0; i < fieldsCount; i++) {
                    _fields[i] = new Field();
                    _fields[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _object.write(ps.getOutputStream());
                ps.write(_fields.length);
                for (int i = 0; i < _fields.length; i++) {
                    _fields[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_object=" + _object);
                stringBuilder.append(", ");
                stringBuilder.append("_fields=[" + _fields.length + "]{");
                for (int i = 0; i < _fields.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_fields[i]=" + _fields[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue[] _values;
            public Reply(JDWPValue[] values) {
                this._values = values;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int valuesCount = ps.readInt();
                _values = new JDWPValue[valuesCount];
                for (int i = 0; i < valuesCount; i++) {
                    _values[i] = ps.readValue();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_values.length);
                for (int i = 0; i < _values.length; i++) {
                    ps.write(_values[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_values=[" + _values.length + "]{");
                for (int i = 0; i < _values.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_values[i]=" + _values[i]);
                }
                stringBuilder.append("}");
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
            public ID.ObjectID _object;

            public FieldValue[] _values;
            public IncomingRequest(ID.ObjectID object,
                FieldValue[] values) {
                this._object = object;
                this._values = values;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _object = ID.read(ps.getInputStream(), ID.ObjectID.class);
                final int valuesCount = ps.readInt();
                _values = new FieldValue[valuesCount];
                for (int i = 0; i < valuesCount; i++) {
                    _values[i] = new FieldValue();
                    _values[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _object.write(ps.getOutputStream());
                ps.write(_values.length);
                for (int i = 0; i < _values.length; i++) {
                    _values[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_object=" + _object);
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

    public static class MonitorInfo {
        public static final byte COMMAND = 5;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ObjectID _object;
            public IncomingRequest(ID.ObjectID object) {
                this._object = object;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _object = ID.read(ps.getInputStream(), ID.ObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _object.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_object=" + _object);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.ThreadID _owner;

            public int _entryCount;

            public ID.ThreadID[] _waiters;
            public Reply(ID.ThreadID owner,
                int entryCount,
                ID.ThreadID[] waiters) {
                this._owner = owner;
                this._entryCount = entryCount;
                this._waiters = waiters;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _owner = ID.read(ps.getInputStream(), ID.ThreadID.class);
                _entryCount = ps.readInt();
                final int waitersCount = ps.readInt();
                _waiters = new ID.ThreadID[waitersCount];
                for (int i = 0; i < waitersCount; i++) {
                    _waiters[i] = ID.read(ps.getInputStream(), ID.ThreadID.class);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _owner.write(ps.getOutputStream());
                ps.write(_entryCount);
                ps.write(_waiters.length);
                for (int i = 0; i < _waiters.length; i++) {
                    _waiters[i].write(ps.getOutputStream());
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_owner=" + _owner);
                stringBuilder.append(", ");
                stringBuilder.append("_entryCount=" + _entryCount);
                stringBuilder.append(", ");
                stringBuilder.append("_waiters=[" + _waiters.length + "]{");
                for (int i = 0; i < _waiters.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_waiters[i]=" + _waiters[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class InvokeMethod {
        public static final byte COMMAND = 6;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ObjectID _object;

            public ID.ThreadID _thread;

            public ID.ClassID _clazz;

            public ID.MethodID _methodID;

            public JDWPValue[] _arguments;

            public int _options;
            public IncomingRequest(ID.ObjectID object,
                ID.ThreadID thread,
                ID.ClassID clazz,
                ID.MethodID methodID,
                JDWPValue[] arguments,
                int options) {
                this._object = object;
                this._thread = thread;
                this._clazz = clazz;
                this._methodID = methodID;
                this._arguments = arguments;
                this._options = options;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _object = ID.read(ps.getInputStream(), ID.ObjectID.class);
                _thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                _clazz = ID.read(ps.getInputStream(), ID.ClassID.class);
                _methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
                final int argumentsCount = ps.readInt();
                _arguments = new JDWPValue[argumentsCount];
                for (int i = 0; i < argumentsCount; i++) {
                    _arguments[i] = ps.readValue();
                }
                _options = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _object.write(ps.getOutputStream());
                _thread.write(ps.getOutputStream());
                _clazz.write(ps.getOutputStream());
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
                stringBuilder.append("_object=" + _object);
                stringBuilder.append(", ");
                stringBuilder.append("_thread=" + _thread);
                stringBuilder.append(", ");
                stringBuilder.append("_clazz=" + _clazz);
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

    public static class DisableCollection {
        public static final byte COMMAND = 7;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ObjectID _object;
            public IncomingRequest(ID.ObjectID object) {
                this._object = object;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _object = ID.read(ps.getInputStream(), ID.ObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _object.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_object=" + _object);
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

    public static class EnableCollection {
        public static final byte COMMAND = 8;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ObjectID _object;
            public IncomingRequest(ID.ObjectID object) {
                this._object = object;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _object = ID.read(ps.getInputStream(), ID.ObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _object.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_object=" + _object);
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

    public static class IsCollected {
        public static final byte COMMAND = 9;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ObjectID _object;
            public IncomingRequest(ID.ObjectID object) {
                this._object = object;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _object = ID.read(ps.getInputStream(), ID.ObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _object.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_object=" + _object);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public boolean _isCollected;
            public Reply(boolean isCollected) {
                this._isCollected = isCollected;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _isCollected = ps.readBoolean();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_isCollected);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_isCollected=" + _isCollected);
                return stringBuilder.toString();
            }
        }
    }

    public static class ReferringObjects {
        public static final byte COMMAND = 10;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ObjectID _object;

            public int _maxReferrers;
            public IncomingRequest(ID.ObjectID object,
                int maxReferrers) {
                this._object = object;
                this._maxReferrers = maxReferrers;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _object = ID.read(ps.getInputStream(), ID.ObjectID.class);
                _maxReferrers = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _object.write(ps.getOutputStream());
                ps.write(_maxReferrers);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_object=" + _object);
                stringBuilder.append(", ");
                stringBuilder.append("_maxReferrers=" + _maxReferrers);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue[] _referringObjects;
            public Reply(JDWPValue[] referringObjects) {
                this._referringObjects = referringObjects;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int referringObjectsCount = ps.readInt();
                _referringObjects = new JDWPValue[referringObjectsCount];
                for (int i = 0; i < referringObjectsCount; i++) {
                    _referringObjects[i] = ps.readValue();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_referringObjects.length);
                for (int i = 0; i < _referringObjects.length; i++) {
                    ps.write(_referringObjects[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_referringObjects=[" + _referringObjects.length + "]{");
                for (int i = 0; i < _referringObjects.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_referringObjects[i]=" + _referringObjects[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }
}
