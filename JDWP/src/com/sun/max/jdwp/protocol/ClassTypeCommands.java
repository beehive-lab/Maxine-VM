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
            public ID.ClassID clazz;
            public IncomingRequest(ID.ClassID clazz) {
                this.clazz = clazz;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                clazz = ID.read(ps.getInputStream(), ID.ClassID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                clazz.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("clazz=" + clazz);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ID.ClassID superclass;
            public Reply(ID.ClassID superclass) {
                this.superclass = superclass;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                superclass = ID.read(ps.getInputStream(), ID.ClassID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                superclass.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("superclass=" + superclass);
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
            public ID.ClassID clazz;

            public FieldValue[] values;
            public IncomingRequest(ID.ClassID clazz,
                FieldValue[] values) {
                this.clazz = clazz;
                this.values = values;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                clazz = ID.read(ps.getInputStream(), ID.ClassID.class);
                final int valuesCount = ps.readInt();
                values = new FieldValue[valuesCount];
                for (int i = 0; i < valuesCount; i++) {
                    values[i] = new FieldValue();
                    values[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                clazz.write(ps.getOutputStream());
                ps.write(values.length);
                for (int i = 0; i < values.length; i++) {
                    values[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("clazz=" + clazz);
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
            public ID.ClassID clazz;

            public ID.ThreadID thread;

            public ID.MethodID methodID;

            public JDWPValue[] arguments;

            public int options;
            public IncomingRequest(ID.ClassID clazz,
                ID.ThreadID thread,
                ID.MethodID methodID,
                JDWPValue[] arguments,
                int options) {
                this.clazz = clazz;
                this.thread = thread;
                this.methodID = methodID;
                this.arguments = arguments;
                this.options = options;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                clazz = ID.read(ps.getInputStream(), ID.ClassID.class);
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
                final int argumentsCount = ps.readInt();
                arguments = new JDWPValue[argumentsCount];
                for (int i = 0; i < argumentsCount; i++) {
                    arguments[i] = ps.readValue();
                }
                options = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                clazz.write(ps.getOutputStream());
                thread.write(ps.getOutputStream());
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
                stringBuilder.append("clazz=" + clazz);
                stringBuilder.append(", ");
                stringBuilder.append("thread=" + thread);
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
            public ID.ClassID clazz;

            public ID.ThreadID thread;

            public ID.MethodID methodID;

            public JDWPValue[] arguments;

            public int options;
            public IncomingRequest(ID.ClassID clazz,
                ID.ThreadID thread,
                ID.MethodID methodID,
                JDWPValue[] arguments,
                int options) {
                this.clazz = clazz;
                this.thread = thread;
                this.methodID = methodID;
                this.arguments = arguments;
                this.options = options;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                clazz = ID.read(ps.getInputStream(), ID.ClassID.class);
                thread = ID.read(ps.getInputStream(), ID.ThreadID.class);
                methodID = ID.read(ps.getInputStream(), ID.MethodID.class);
                final int argumentsCount = ps.readInt();
                arguments = new JDWPValue[argumentsCount];
                for (int i = 0; i < argumentsCount; i++) {
                    arguments[i] = ps.readValue();
                }
                options = ps.readInt();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                clazz.write(ps.getOutputStream());
                thread.write(ps.getOutputStream());
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
                stringBuilder.append("clazz=" + clazz);
                stringBuilder.append(", ");
                stringBuilder.append("thread=" + thread);
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

            public JDWPValue newObject;

            public JDWPValue exception;
            public Reply(JDWPValue newObject,
                JDWPValue exception) {
                this.newObject = newObject;
                this.exception = exception;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                newObject = ps.readValue();
                exception = ps.readValue();
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(newObject);
                ps.write(exception);
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("newObject=" + newObject);
                stringBuilder.append(", ");
                stringBuilder.append("exception=" + exception);
                return stringBuilder.toString();
            }
        }
    }
}
