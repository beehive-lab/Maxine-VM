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

public final class ReferenceTypeCommands {
    public static final int COMMAND_SET = 2;
    private ReferenceTypeCommands() { }  // hide constructor

    public static class Signature {
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
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public String _signature;
            public Reply(String signature) {
                this._signature = signature;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _signature = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_signature);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_signature=" + _signature);
                return stringBuilder.toString();
            }
        }
    }

    public static class ClassLoader {
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
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.ClassLoaderID _classLoader;
            public Reply(ID.ClassLoaderID classLoader) {
                this._classLoader = classLoader;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _classLoader = ID.read(ps.getInputStream(), ID.ClassLoaderID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _classLoader.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_classLoader=" + _classLoader);
                return stringBuilder.toString();
            }
        }
    }

    public static class Modifiers {
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
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int _modBits;
            public Reply(int modBits) {
                this._modBits = modBits;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _modBits = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_modBits);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_modBits=" + _modBits);
                return stringBuilder.toString();
            }
        }
    }

    public static class Fields {
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
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class FieldInfo {

            public ID.FieldID _fieldID;

            public String _name;

            public String _signature;

            public int _modBits;
            public FieldInfo(ID.FieldID fieldID,
                String name,
                String signature,
                int modBits) {
                this._fieldID = fieldID;
                this._name = name;
                this._signature = signature;
                this._modBits = modBits;
            }
            public FieldInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _fieldID = ID.read(ps.getInputStream(), ID.FieldID.class);
                _name = ps.readString();
                _signature = ps.readString();
                _modBits = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _fieldID.write(ps.getOutputStream());
                ps.write(_name);
                ps.write(_signature);
                ps.write(_modBits);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_fieldID=" + _fieldID);
                stringBuilder.append(", ");
                stringBuilder.append("_name=" + _name);
                stringBuilder.append(", ");
                stringBuilder.append("_signature=" + _signature);
                stringBuilder.append(", ");
                stringBuilder.append("_modBits=" + _modBits);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public FieldInfo[] _declared;
            public Reply(FieldInfo[] declared) {
                this._declared = declared;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int declaredCount = ps.readInt();
                _declared = new FieldInfo[declaredCount];
                for (int i = 0; i < declaredCount; i++) {
                    _declared[i] = new FieldInfo();
                    _declared[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_declared.length);
                for (int i = 0; i < _declared.length; i++) {
                    _declared[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_declared=[" + _declared.length + "]{");
                for (int i = 0; i < _declared.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_declared[i]=" + _declared[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class Methods {
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
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class MethodInfo {

            public ID.MethodID _methodID;

            public String _name;

            public String _signature;

            public int _modBits;
            public MethodInfo(ID.MethodID methodID,
                String name,
                String signature,
                int modBits) {
                this._methodID = methodID;
                this._name = name;
                this._signature = signature;
                this._modBits = modBits;
            }
            public MethodInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
                _name = ps.readString();
                _signature = ps.readString();
                _modBits = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _methodID.write(ps.getOutputStream());
                ps.write(_name);
                ps.write(_signature);
                ps.write(_modBits);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_methodID=" + _methodID);
                stringBuilder.append(", ");
                stringBuilder.append("_name=" + _name);
                stringBuilder.append(", ");
                stringBuilder.append("_signature=" + _signature);
                stringBuilder.append(", ");
                stringBuilder.append("_modBits=" + _modBits);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public MethodInfo[] _declared;
            public Reply(MethodInfo[] declared) {
                this._declared = declared;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int declaredCount = ps.readInt();
                _declared = new MethodInfo[declaredCount];
                for (int i = 0; i < declaredCount; i++) {
                    _declared[i] = new MethodInfo();
                    _declared[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_declared.length);
                for (int i = 0; i < _declared.length; i++) {
                    _declared[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_declared=[" + _declared.length + "]{");
                for (int i = 0; i < _declared.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_declared[i]=" + _declared[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class GetValues {
        public static final byte COMMAND = 6;
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
            public ID.ReferenceTypeID _refType;

            public Field[] _fields;
            public IncomingRequest(ID.ReferenceTypeID refType,
                Field[] fields) {
                this._refType = refType;
                this._fields = fields;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                final int fieldsCount = ps.readInt();
                _fields = new Field[fieldsCount];
                for (int i = 0; i < fieldsCount; i++) {
                    _fields[i] = new Field();
                    _fields[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
                ps.write(_fields.length);
                for (int i = 0; i < _fields.length; i++) {
                    _fields[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
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

    public static class SourceFile {
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
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public String _sourceFile;
            public Reply(String sourceFile) {
                this._sourceFile = sourceFile;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _sourceFile = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_sourceFile);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_sourceFile=" + _sourceFile);
                return stringBuilder.toString();
            }
        }
    }

    public static class NestedTypes {
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
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class TypeInfo {

            public byte _refTypeTag;

            public ID.ReferenceTypeID _typeID;
            public TypeInfo(byte refTypeTag,
                ID.ReferenceTypeID typeID) {
                this._refTypeTag = refTypeTag;
                this._typeID = typeID;
            }
            public TypeInfo() {
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

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public TypeInfo[] _classes;
            public Reply(TypeInfo[] classes) {
                this._classes = classes;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int classesCount = ps.readInt();
                _classes = new TypeInfo[classesCount];
                for (int i = 0; i < classesCount; i++) {
                    _classes[i] = new TypeInfo();
                    _classes[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_classes.length);
                for (int i = 0; i < _classes.length; i++) {
                    _classes[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_classes=[" + _classes.length + "]{");
                for (int i = 0; i < _classes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_classes[i]=" + _classes[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class Status {
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
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int _status;
            public Reply(int status) {
                this._status = status;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _status = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_status);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_status=" + _status);
                return stringBuilder.toString();
            }
        }
    }

    public static class Interfaces {
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
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.InterfaceID[] _interfaces;
            public Reply(ID.InterfaceID[] interfaces) {
                this._interfaces = interfaces;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int interfacesCount = ps.readInt();
                _interfaces = new ID.InterfaceID[interfacesCount];
                for (int i = 0; i < interfacesCount; i++) {
                    _interfaces[i] = ID.read(ps.getInputStream(), ID.InterfaceID.class);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_interfaces.length);
                for (int i = 0; i < _interfaces.length; i++) {
                    _interfaces[i].write(ps.getOutputStream());
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_interfaces=[" + _interfaces.length + "]{");
                for (int i = 0; i < _interfaces.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_interfaces[i]=" + _interfaces[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class ClassObject {
        public static final byte COMMAND = 11;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.ClassObjectID _classObject;
            public Reply(ID.ClassObjectID classObject) {
                this._classObject = classObject;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _classObject = ID.read(ps.getInputStream(), ID.ClassObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _classObject.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_classObject=" + _classObject);
                return stringBuilder.toString();
            }
        }
    }

    public static class SourceDebugExtension {
        public static final byte COMMAND = 12;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public String _extension;
            public Reply(String extension) {
                this._extension = extension;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _extension = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_extension);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_extension=" + _extension);
                return stringBuilder.toString();
            }
        }
    }

    public static class SignatureWithGeneric {
        public static final byte COMMAND = 13;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public String _signature;

            public String _genericSignature;
            public Reply(String signature,
                String genericSignature) {
                this._signature = signature;
                this._genericSignature = genericSignature;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _signature = ps.readString();
                _genericSignature = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_signature);
                ps.write(_genericSignature);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_signature=" + _signature);
                stringBuilder.append(", ");
                stringBuilder.append("_genericSignature=" + _genericSignature);
                return stringBuilder.toString();
            }
        }
    }

    public static class FieldsWithGeneric {
        public static final byte COMMAND = 14;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class FieldInfo {

            public ID.FieldID _fieldID;

            public String _name;

            public String _signature;

            public String _genericSignature;

            public int _modBits;
            public FieldInfo(ID.FieldID fieldID,
                String name,
                String signature,
                String genericSignature,
                int modBits) {
                this._fieldID = fieldID;
                this._name = name;
                this._signature = signature;
                this._genericSignature = genericSignature;
                this._modBits = modBits;
            }
            public FieldInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _fieldID = ID.read(ps.getInputStream(), ID.FieldID.class);
                _name = ps.readString();
                _signature = ps.readString();
                _genericSignature = ps.readString();
                _modBits = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _fieldID.write(ps.getOutputStream());
                ps.write(_name);
                ps.write(_signature);
                ps.write(_genericSignature);
                ps.write(_modBits);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_fieldID=" + _fieldID);
                stringBuilder.append(", ");
                stringBuilder.append("_name=" + _name);
                stringBuilder.append(", ");
                stringBuilder.append("_signature=" + _signature);
                stringBuilder.append(", ");
                stringBuilder.append("_genericSignature=" + _genericSignature);
                stringBuilder.append(", ");
                stringBuilder.append("_modBits=" + _modBits);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public FieldInfo[] _declared;
            public Reply(FieldInfo[] declared) {
                this._declared = declared;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int declaredCount = ps.readInt();
                _declared = new FieldInfo[declaredCount];
                for (int i = 0; i < declaredCount; i++) {
                    _declared[i] = new FieldInfo();
                    _declared[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_declared.length);
                for (int i = 0; i < _declared.length; i++) {
                    _declared[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_declared=[" + _declared.length + "]{");
                for (int i = 0; i < _declared.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_declared[i]=" + _declared[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class MethodsWithGeneric {
        public static final byte COMMAND = 15;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class MethodInfo {

            public ID.MethodID _methodID;

            public String _name;

            public String _signature;

            public String _genericSignature;

            public int _modBits;
            public MethodInfo(ID.MethodID methodID,
                String name,
                String signature,
                String genericSignature,
                int modBits) {
                this._methodID = methodID;
                this._name = name;
                this._signature = signature;
                this._genericSignature = genericSignature;
                this._modBits = modBits;
            }
            public MethodInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
                _name = ps.readString();
                _signature = ps.readString();
                _genericSignature = ps.readString();
                _modBits = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _methodID.write(ps.getOutputStream());
                ps.write(_name);
                ps.write(_signature);
                ps.write(_genericSignature);
                ps.write(_modBits);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_methodID=" + _methodID);
                stringBuilder.append(", ");
                stringBuilder.append("_name=" + _name);
                stringBuilder.append(", ");
                stringBuilder.append("_signature=" + _signature);
                stringBuilder.append(", ");
                stringBuilder.append("_genericSignature=" + _genericSignature);
                stringBuilder.append(", ");
                stringBuilder.append("_modBits=" + _modBits);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public MethodInfo[] _declared;
            public Reply(MethodInfo[] declared) {
                this._declared = declared;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int declaredCount = ps.readInt();
                _declared = new MethodInfo[declaredCount];
                for (int i = 0; i < declaredCount; i++) {
                    _declared[i] = new MethodInfo();
                    _declared[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_declared.length);
                for (int i = 0; i < _declared.length; i++) {
                    _declared[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_declared=[" + _declared.length + "]{");
                for (int i = 0; i < _declared.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_declared[i]=" + _declared[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class Instances {
        public static final byte COMMAND = 16;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ReferenceTypeID _refType;

            public int _maxInstances;
            public IncomingRequest(ID.ReferenceTypeID refType,
                int maxInstances) {
                this._refType = refType;
                this._maxInstances = maxInstances;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                _maxInstances = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
                ps.write(_maxInstances);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                stringBuilder.append(", ");
                stringBuilder.append("_maxInstances=" + _maxInstances);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue[] _instances;
            public Reply(JDWPValue[] instances) {
                this._instances = instances;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int instancesCount = ps.readInt();
                _instances = new JDWPValue[instancesCount];
                for (int i = 0; i < instancesCount; i++) {
                    _instances[i] = ps.readValue();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_instances.length);
                for (int i = 0; i < _instances.length; i++) {
                    ps.write(_instances[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_instances=[" + _instances.length + "]{");
                for (int i = 0; i < _instances.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_instances[i]=" + _instances[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }

    public static class ClassFileVersion {
        public static final byte COMMAND = 17;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int _majorVersion;

            public int _minorVersion;
            public Reply(int majorVersion,
                int minorVersion) {
                this._majorVersion = majorVersion;
                this._minorVersion = minorVersion;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _majorVersion = ps.readInt();
                _minorVersion = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_majorVersion);
                ps.write(_minorVersion);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_majorVersion=" + _majorVersion);
                stringBuilder.append(", ");
                stringBuilder.append("_minorVersion=" + _minorVersion);
                return stringBuilder.toString();
            }
        }
    }

    public static class ConstantPool {
        public static final byte COMMAND = 18;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ReferenceTypeID _refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this._refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                _refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_refType=" + _refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int _count;

            public byte[] _bytes;
            public Reply(int count,
                byte[] bytes) {
                this._count = count;
                this._bytes = bytes;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                _count = ps.readInt();
                final int bytesCount = ps.readInt();
                _bytes = new byte[bytesCount];
                for (int i = 0; i < bytesCount; i++) {
                    _bytes[i] = ps.readByte();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(_count);
                ps.write(_bytes.length);
                for (int i = 0; i < _bytes.length; i++) {
                    ps.write(_bytes[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_count=" + _count);
                stringBuilder.append(", ");
                stringBuilder.append("_bytes=[" + _bytes.length + "]{");
                for (int i = 0; i < _bytes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("_bytes[i]=" + _bytes[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }
}
