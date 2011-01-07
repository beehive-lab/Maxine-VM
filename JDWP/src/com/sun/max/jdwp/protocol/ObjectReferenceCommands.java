/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
            public ID.ObjectID object;
            public IncomingRequest(ID.ObjectID object) {
                this.object = object;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                object = ID.read(ps.getInputStream(), ID.ObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                object.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("object=" + object);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public byte refTypeTag;

            public ID.ReferenceTypeID typeID;
            public Reply(byte refTypeTag,
                ID.ReferenceTypeID typeID) {
                this.refTypeTag = refTypeTag;
                this.typeID = typeID;
            }
            public Reply() {
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
            public ID.ObjectID object;

            public Field[] fields;
            public IncomingRequest(ID.ObjectID object,
                Field[] fields) {
                this.object = object;
                this.fields = fields;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                object = ID.read(ps.getInputStream(), ID.ObjectID.class);
                final int fieldsCount = ps.readInt();
                fields = new Field[fieldsCount];
                for (int i = 0; i < fieldsCount; i++) {
                    fields[i] = new Field();
                    fields[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                object.write(ps.getOutputStream());
                ps.write(fields.length);
                for (int i = 0; i < fields.length; i++) {
                    fields[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("object=" + object);
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

            public ID.FieldID fieldID;

            public JDWPValue value;
            public FieldValue(ID.FieldID fieldID,
                JDWPValue value) {
                this.fieldID = fieldID;
                this.value = value;
            }
            public FieldValue() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                fieldID = ID.read(ps.getInputStream(), ID.FieldID.class);
                value = ps.readUntaggedValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                fieldID.write(ps.getOutputStream());
                ps.write(value);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("fieldID=" + fieldID);
                stringBuilder.append(", ");
                stringBuilder.append("value=" + value);
                return stringBuilder.toString();
            }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ObjectID object;

            public FieldValue[] values;
            public IncomingRequest(ID.ObjectID object,
                FieldValue[] values) {
                this.object = object;
                this.values = values;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                object = ID.read(ps.getInputStream(), ID.ObjectID.class);
                final int valuesCount = ps.readInt();
                values = new FieldValue[valuesCount];
                for (int i = 0; i < valuesCount; i++) {
                    values[i] = new FieldValue();
                    values[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                object.write(ps.getOutputStream());
                ps.write(values.length);
                for (int i = 0; i < values.length; i++) {
                    values[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("object=" + object);
                stringBuilder.append(", ");
                stringBuilder.append("values=[" + values.length + "]{");
                for (int i = 0; i < values.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("values[i]=" + values[i]);
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
            public ID.ObjectID object;
            public IncomingRequest(ID.ObjectID object) {
                this.object = object;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                object = ID.read(ps.getInputStream(), ID.ObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                object.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("object=" + object);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.ThreadID owner;

            public int entryCount;

            public ID.ThreadID[] waiters;
            public Reply(ID.ThreadID owner,
                int entryCount,
                ID.ThreadID[] waiters) {
                this.owner = owner;
                this.entryCount = entryCount;
                this.waiters = waiters;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                owner = ID.read(ps.getInputStream(), ID.ThreadID.class);
                entryCount = ps.readInt();
                final int waitersCount = ps.readInt();
                waiters = new ID.ThreadID[waitersCount];
                for (int i = 0; i < waitersCount; i++) {
                    waiters[i] = ID.read(ps.getInputStream(), ID.ThreadID.class);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                owner.write(ps.getOutputStream());
                ps.write(entryCount);
                ps.write(waiters.length);
                for (int i = 0; i < waiters.length; i++) {
                    waiters[i].write(ps.getOutputStream());
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("owner=" + owner);
                stringBuilder.append(", ");
                stringBuilder.append("entryCount=" + entryCount);
                stringBuilder.append(", ");
                stringBuilder.append("waiters=[" + waiters.length + "]{");
                for (int i = 0; i < waiters.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("waiters[i]=" + waiters[i]);
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
            public ID.ObjectID object;

            public ID.ThreadID thread;

            public ID.ClassID clazz;

            public ID.MethodID methodID;

            public JDWPValue[] arguments;

            public int options;
            public IncomingRequest(ID.ObjectID object,
                ID.ThreadID thread,
                ID.ClassID clazz,
                ID.MethodID methodID,
                JDWPValue[] arguments,
                int options) {
                this.object = object;
                this.thread = thread;
                this.clazz = clazz;
                this.methodID = methodID;
                this.arguments = arguments;
                this.options = options;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                object = ID.read(ps.getInputStream(), ID.ObjectID.class);
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                clazz = ID.read(ps.getInputStream(), ID.ClassID.class);
                methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
                final int argumentsCount = ps.readInt();
                arguments = new JDWPValue[argumentsCount];
                for (int i = 0; i < argumentsCount; i++) {
                    arguments[i] = ps.readValue();
                }
                options = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                object.write(ps.getOutputStream());
                thread.write(ps.getOutputStream());
                clazz.write(ps.getOutputStream());
                methodID.write(ps.getOutputStream());
                ps.write(arguments.length);
                for (int i = 0; i < arguments.length; i++) {
                    ps.write(arguments[i]);
                }
                ps.write(options);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("object=" + object);
                stringBuilder.append(", ");
                stringBuilder.append("thread=" + thread);
                stringBuilder.append(", ");
                stringBuilder.append("clazz=" + clazz);
                stringBuilder.append(", ");
                stringBuilder.append("methodID=" + methodID);
                stringBuilder.append(", ");
                stringBuilder.append("arguments=[" + arguments.length + "]{");
                for (int i = 0; i < arguments.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("arguments[i]=" + arguments[i]);
                }
                stringBuilder.append("}");
                stringBuilder.append(", ");
                stringBuilder.append("options=" + options);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue returnValue;

            public JDWPValue exception;
            public Reply(JDWPValue returnValue,
                JDWPValue exception) {
                this.returnValue = returnValue;
                this.exception = exception;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                returnValue = ps.readValue();
                exception = ps.readValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(returnValue);
                ps.write(exception);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("returnValue=" + returnValue);
                stringBuilder.append(", ");
                stringBuilder.append("exception=" + exception);
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
            public ID.ObjectID object;
            public IncomingRequest(ID.ObjectID object) {
                this.object = object;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                object = ID.read(ps.getInputStream(), ID.ObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                object.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("object=" + object);
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
            public ID.ObjectID object;
            public IncomingRequest(ID.ObjectID object) {
                this.object = object;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                object = ID.read(ps.getInputStream(), ID.ObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                object.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("object=" + object);
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
            public ID.ObjectID object;
            public IncomingRequest(ID.ObjectID object) {
                this.object = object;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                object = ID.read(ps.getInputStream(), ID.ObjectID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                object.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("object=" + object);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public boolean isCollected;
            public Reply(boolean isCollected) {
                this.isCollected = isCollected;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                isCollected = ps.readBoolean();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(isCollected);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isCollected=" + isCollected);
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
            public ID.ObjectID object;

            public int maxReferrers;
            public IncomingRequest(ID.ObjectID object,
                int maxReferrers) {
                this.object = object;
                this.maxReferrers = maxReferrers;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                object = ID.read(ps.getInputStream(), ID.ObjectID.class);
                maxReferrers = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                object.write(ps.getOutputStream());
                ps.write(maxReferrers);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("object=" + object);
                stringBuilder.append(", ");
                stringBuilder.append("maxReferrers=" + maxReferrers);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public JDWPValue[] referringObjects;
            public Reply(JDWPValue[] referringObjects) {
                this.referringObjects = referringObjects;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int referringObjectsCount = ps.readInt();
                referringObjects = new JDWPValue[referringObjectsCount];
                for (int i = 0; i < referringObjectsCount; i++) {
                    referringObjects[i] = ps.readValue();
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(referringObjects.length);
                for (int i = 0; i < referringObjects.length; i++) {
                    ps.write(referringObjects[i]);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("referringObjects=[" + referringObjects.length + "]{");
                for (int i = 0; i < referringObjects.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("referringObjects[i]=" + referringObjects[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }
}
