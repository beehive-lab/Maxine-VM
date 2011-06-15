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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public String signature;
            public Reply(String signature) {
                this.signature = signature;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                signature = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(signature);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("signature=" + signature);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.ClassLoaderID classLoader;
            public Reply(ID.ClassLoaderID classLoader) {
                this.classLoader = classLoader;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                classLoader = ID.read(ps.getInputStream(), ID.ClassLoaderID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                classLoader.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("classLoader=" + classLoader);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int modBits;
            public Reply(int modBits) {
                this.modBits = modBits;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                modBits = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(modBits);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("modBits=" + modBits);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class FieldInfo {

            public ID.FieldID fieldID;

            public String name;

            public String signature;

            public int modBits;
            public FieldInfo(ID.FieldID fieldID,
                String name,
                String signature,
                int modBits) {
                this.fieldID = fieldID;
                this.name = name;
                this.signature = signature;
                this.modBits = modBits;
            }
            public FieldInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                fieldID = ID.read(ps.getInputStream(), ID.FieldID.class);
                name = ps.readString();
                signature = ps.readString();
                modBits = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                fieldID.write(ps.getOutputStream());
                ps.write(name);
                ps.write(signature);
                ps.write(modBits);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("fieldID=" + fieldID);
                stringBuilder.append(", ");
                stringBuilder.append("name=" + name);
                stringBuilder.append(", ");
                stringBuilder.append("signature=" + signature);
                stringBuilder.append(", ");
                stringBuilder.append("modBits=" + modBits);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public FieldInfo[] declared;
            public Reply(FieldInfo[] declared) {
                this.declared = declared;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int declaredCount = ps.readInt();
                declared = new FieldInfo[declaredCount];
                for (int i = 0; i < declaredCount; i++) {
                    declared[i] = new FieldInfo();
                    declared[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(declared.length);
                for (int i = 0; i < declared.length; i++) {
                    declared[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("declared=[" + declared.length + "]{");
                for (int i = 0; i < declared.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("declared[i]=" + declared[i]);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class MethodInfo {

            public ID.MethodID methodID;

            public String name;

            public String signature;

            public int modBits;
            public MethodInfo(ID.MethodID methodID,
                String name,
                String signature,
                int modBits) {
                this.methodID = methodID;
                this.name = name;
                this.signature = signature;
                this.modBits = modBits;
            }
            public MethodInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
                name = ps.readString();
                signature = ps.readString();
                modBits = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                methodID.write(ps.getOutputStream());
                ps.write(name);
                ps.write(signature);
                ps.write(modBits);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("methodID=" + methodID);
                stringBuilder.append(", ");
                stringBuilder.append("name=" + name);
                stringBuilder.append(", ");
                stringBuilder.append("signature=" + signature);
                stringBuilder.append(", ");
                stringBuilder.append("modBits=" + modBits);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public MethodInfo[] declared;
            public Reply(MethodInfo[] declared) {
                this.declared = declared;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int declaredCount = ps.readInt();
                declared = new MethodInfo[declaredCount];
                for (int i = 0; i < declaredCount; i++) {
                    declared[i] = new MethodInfo();
                    declared[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(declared.length);
                for (int i = 0; i < declared.length; i++) {
                    declared[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("declared=[" + declared.length + "]{");
                for (int i = 0; i < declared.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("declared[i]=" + declared[i]);
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

            public ID.FieldID fieldID;
            public Field(ID.FieldID fieldID) {
                this.fieldID = fieldID;
            }
            public Field() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                fieldID = ID.read(ps.getInputStream(), ID.FieldID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                fieldID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("fieldID=" + fieldID);
                return stringBuilder.toString();
            }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ReferenceTypeID refType;

            public Field[] fields;
            public IncomingRequest(ID.ReferenceTypeID refType,
                Field[] fields) {
                this.refType = refType;
                this.fields = fields;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                final int fieldsCount = ps.readInt();
                fields = new Field[fieldsCount];
                for (int i = 0; i < fieldsCount; i++) {
                    fields[i] = new Field();
                    fields[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
                ps.write(fields.length);
                for (int i = 0; i < fields.length; i++) {
                    fields[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                stringBuilder.append(", ");
                stringBuilder.append("fields=[" + fields.length + "]{");
                for (int i = 0; i < fields.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("fields[i]=" + fields[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue[] values;
            public Reply(JDWPValue[] values) {
                this.values = values;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int valuesCount = ps.readInt();
                values = new JDWPValue[valuesCount];
                for (int i = 0; i < valuesCount; i++) {
                    values[i] = ps.readValue();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(values.length);
                for (int i = 0; i < values.length; i++) {
                    ps.write(values[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("values=[" + values.length + "]{");
                for (int i = 0; i < values.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("values[i]=" + values[i]);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public String sourceFile;
            public Reply(String sourceFile) {
                this.sourceFile = sourceFile;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                sourceFile = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(sourceFile);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sourceFile=" + sourceFile);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class TypeInfo {

            public byte refTypeTag;

            public ID.ReferenceTypeID typeID;
            public TypeInfo(byte refTypeTag,
                ID.ReferenceTypeID typeID) {
                this.refTypeTag = refTypeTag;
                this.typeID = typeID;
            }
            public TypeInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refTypeTag = ps.readByte();
                typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(refTypeTag);
                typeID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refTypeTag=" + refTypeTag);
                stringBuilder.append(", ");
                stringBuilder.append("typeID=" + typeID);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public TypeInfo[] classes;
            public Reply(TypeInfo[] classes) {
                this.classes = classes;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int classesCount = ps.readInt();
                classes = new TypeInfo[classesCount];
                for (int i = 0; i < classesCount; i++) {
                    classes[i] = new TypeInfo();
                    classes[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(classes.length);
                for (int i = 0; i < classes.length; i++) {
                    classes[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("classes=[" + classes.length + "]{");
                for (int i = 0; i < classes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("classes[i]=" + classes[i]);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int status;
            public Reply(int status) {
                this.status = status;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                status = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(status);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("status=" + status);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.InterfaceID[] interfaces;
            public Reply(ID.InterfaceID[] interfaces) {
                this.interfaces = interfaces;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int interfacesCount = ps.readInt();
                interfaces = new ID.InterfaceID[interfacesCount];
                for (int i = 0; i < interfacesCount; i++) {
                    interfaces[i] = ID.read(ps.getInputStream(), ID.InterfaceID.class);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(interfaces.length);
                for (int i = 0; i < interfaces.length; i++) {
                    interfaces[i].write(ps.getOutputStream());
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("interfaces=[" + interfaces.length + "]{");
                for (int i = 0; i < interfaces.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("interfaces[i]=" + interfaces[i]);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.ClassObjectID classObject;
            public Reply(ID.ClassObjectID classObject) {
                this.classObject = classObject;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                classObject = ID.read(ps.getInputStream(), ID.ClassObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                classObject.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("classObject=" + classObject);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public String extension;
            public Reply(String extension) {
                this.extension = extension;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                extension = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(extension);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("extension=" + extension);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public String signature;

            public String genericSignature;
            public Reply(String signature,
                String genericSignature) {
                this.signature = signature;
                this.genericSignature = genericSignature;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                signature = ps.readString();
                genericSignature = ps.readString();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(signature);
                ps.write(genericSignature);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("signature=" + signature);
                stringBuilder.append(", ");
                stringBuilder.append("genericSignature=" + genericSignature);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class FieldInfo {

            public ID.FieldID fieldID;

            public String name;

            public String signature;

            public String genericSignature;

            public int modBits;
            public FieldInfo(ID.FieldID fieldID,
                String name,
                String signature,
                String genericSignature,
                int modBits) {
                this.fieldID = fieldID;
                this.name = name;
                this.signature = signature;
                this.genericSignature = genericSignature;
                this.modBits = modBits;
            }
            public FieldInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                fieldID = ID.read(ps.getInputStream(), ID.FieldID.class);
                name = ps.readString();
                signature = ps.readString();
                genericSignature = ps.readString();
                modBits = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                fieldID.write(ps.getOutputStream());
                ps.write(name);
                ps.write(signature);
                ps.write(genericSignature);
                ps.write(modBits);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("fieldID=" + fieldID);
                stringBuilder.append(", ");
                stringBuilder.append("name=" + name);
                stringBuilder.append(", ");
                stringBuilder.append("signature=" + signature);
                stringBuilder.append(", ");
                stringBuilder.append("genericSignature=" + genericSignature);
                stringBuilder.append(", ");
                stringBuilder.append("modBits=" + modBits);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public FieldInfo[] declared;
            public Reply(FieldInfo[] declared) {
                this.declared = declared;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int declaredCount = ps.readInt();
                declared = new FieldInfo[declaredCount];
                for (int i = 0; i < declaredCount; i++) {
                    declared[i] = new FieldInfo();
                    declared[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(declared.length);
                for (int i = 0; i < declared.length; i++) {
                    declared[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("declared=[" + declared.length + "]{");
                for (int i = 0; i < declared.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("declared[i]=" + declared[i]);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class MethodInfo {

            public ID.MethodID methodID;

            public String name;

            public String signature;

            public String genericSignature;

            public int modBits;
            public MethodInfo(ID.MethodID methodID,
                String name,
                String signature,
                String genericSignature,
                int modBits) {
                this.methodID = methodID;
                this.name = name;
                this.signature = signature;
                this.genericSignature = genericSignature;
                this.modBits = modBits;
            }
            public MethodInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
                name = ps.readString();
                signature = ps.readString();
                genericSignature = ps.readString();
                modBits = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                methodID.write(ps.getOutputStream());
                ps.write(name);
                ps.write(signature);
                ps.write(genericSignature);
                ps.write(modBits);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("methodID=" + methodID);
                stringBuilder.append(", ");
                stringBuilder.append("name=" + name);
                stringBuilder.append(", ");
                stringBuilder.append("signature=" + signature);
                stringBuilder.append(", ");
                stringBuilder.append("genericSignature=" + genericSignature);
                stringBuilder.append(", ");
                stringBuilder.append("modBits=" + modBits);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public MethodInfo[] declared;
            public Reply(MethodInfo[] declared) {
                this.declared = declared;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int declaredCount = ps.readInt();
                declared = new MethodInfo[declaredCount];
                for (int i = 0; i < declaredCount; i++) {
                    declared[i] = new MethodInfo();
                    declared[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(declared.length);
                for (int i = 0; i < declared.length; i++) {
                    declared[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("declared=[" + declared.length + "]{");
                for (int i = 0; i < declared.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("declared[i]=" + declared[i]);
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
            public ID.ReferenceTypeID refType;

            public int maxInstances;
            public IncomingRequest(ID.ReferenceTypeID refType,
                int maxInstances) {
                this.refType = refType;
                this.maxInstances = maxInstances;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
                maxInstances = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
                ps.write(maxInstances);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                stringBuilder.append(", ");
                stringBuilder.append("maxInstances=" + maxInstances);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue[] instances;
            public Reply(JDWPValue[] instances) {
                this.instances = instances;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int instancesCount = ps.readInt();
                instances = new JDWPValue[instancesCount];
                for (int i = 0; i < instancesCount; i++) {
                    instances[i] = ps.readValue();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(instances.length);
                for (int i = 0; i < instances.length; i++) {
                    ps.write(instances[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("instances=[" + instances.length + "]{");
                for (int i = 0; i < instances.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("instances[i]=" + instances[i]);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int majorVersion;

            public int minorVersion;
            public Reply(int majorVersion,
                int minorVersion) {
                this.majorVersion = majorVersion;
                this.minorVersion = minorVersion;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                majorVersion = ps.readInt();
                minorVersion = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(majorVersion);
                ps.write(minorVersion);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("majorVersion=" + majorVersion);
                stringBuilder.append(", ");
                stringBuilder.append("minorVersion=" + minorVersion);
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
            public ID.ReferenceTypeID refType;
            public IncomingRequest(ID.ReferenceTypeID refType) {
                this.refType = refType;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refType = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                refType.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refType=" + refType);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public int count;

            public byte[] bytes;
            public Reply(int count,
                byte[] bytes) {
                this.count = count;
                this.bytes = bytes;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                count = ps.readInt();
                final int bytesCount = ps.readInt();
                bytes = new byte[bytesCount];
                for (int i = 0; i < bytesCount; i++) {
                    bytes[i] = ps.readByte();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(count);
                ps.write(bytes.length);
                for (int i = 0; i < bytes.length; i++) {
                    ps.write(bytes[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("count=" + count);
                stringBuilder.append(", ");
                stringBuilder.append("bytes=[" + bytes.length + "]{");
                for (int i = 0; i < bytes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("bytes[i]=" + bytes[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }
}
